param(
    [string]$Config = "",
    [string]$MakeAppx = ""
)
$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest
$Root = Split-Path -Parent $PSScriptRoot
if ($env:OS -ne "Windows_NT") { throw "MSIX muss auf Windows gebaut werden." }
if (-not $Config) { $Config = Join-Path $Root "packaging\windows\store.local.json" }
if (-not (Test-Path -LiteralPath $Config)) {
    throw "store.example.json nach store.local.json kopieren und echte Partner-Center-Werte eintragen."
}
$Settings = Get-Content -LiteralPath $Config -Raw -Encoding UTF8 | ConvertFrom-Json
foreach ($Field in @("IdentityName", "Publisher", "PublisherDisplayName", "DisplayName",
        "PackageVersion", "MinVersion", "MaxVersionTested")) {
    if (-not $Settings.PSObject.Properties[$Field] -or
        [string]::IsNullOrWhiteSpace([string]$Settings.$Field)) {
        throw "Fehlender Konfigurationswert: $Field. Keine Beispielidentitaet verwenden."
    }
}
if ($Settings.IdentityName -notmatch '^[A-Za-z0-9.-]{3,50}$' -or
    $Settings.Publisher -notmatch '^CN=') {
    throw "IdentityName und Publisher unveraendert aus Partner Center uebernehmen."
}
function Read-FourPartVersion([string]$Value) {
    if ($Value -notmatch '^\d+\.\d+\.\d+\.\d+$') { throw "Vierteilige Version erforderlich: $Value" }
    foreach ($Part in $Value.Split('.')) {
        if ([long]$Part -gt 65535) { throw "Versionsbestandteil ausserhalb 0..65535: $Value" }
    }
    return [version]$Value
}
$Version = Read-FourPartVersion $Settings.PackageVersion
if ($Version.Major -lt 1 -or $Version.Revision -ne 0) {
    throw "Store-Paketversion: erster Teil mindestens 1, vierter Teil 0."
}
$Minimum = Read-FourPartVersion $Settings.MinVersion
$Maximum = Read-FourPartVersion $Settings.MaxVersionTested
if ($Minimum -lt [version]"10.0.19041.0" -or $Maximum -lt $Minimum) {
    throw "Windows-Basis muss mindestens 10.0.19041.0 sein. MaxVersionTested darf nicht kleiner sein."
}
if (-not $env:JAVA_HOME) { throw "JAVA_HOME auf Temurin 21.0.13+9 fuer Windows x64 setzen." }
$JavaRelease = Get-Content -LiteralPath "$env:JAVA_HOME\release" -Raw
if ($JavaRelease -notmatch '(?m)^OS_ARCH="(amd64|x86_64)"' -or
    $JavaRelease -notmatch '(?m)^OS_NAME="Windows"') {
    throw "Diese Paketierung erwartet ein Windows-x64-JDK."
}
if (-not $MakeAppx) {
    $SdkRoot = Join-Path ${env:ProgramFiles(x86)} "Windows Kits\10\bin"
    $Candidates = @(Get-ChildItem -Path "$SdkRoot\*\x64\makeappx.exe" -ErrorAction SilentlyContinue |
        Sort-Object { [version]$_.Directory.Parent.Name } -Descending)
    if ($Candidates.Count -eq 0) { throw "MakeAppx fehlt. Windows SDK installieren oder -MakeAppx angeben." }
    $MakeAppx = $Candidates[0].FullName
}
if (-not (Test-Path -LiteralPath $MakeAppx -PathType Leaf)) { throw "MakeAppx nicht gefunden: $MakeAppx" }
$Output = Join-Path $Root "build\releases\windows\$Version"
if (Test-Path -LiteralPath $Output) { throw "Vorhandene Lieferung wird nicht ersetzt: $Output" }
$SourceRevision = "not-recorded"
$SourceState = "not-recorded"
if (Test-Path -LiteralPath (Join-Path $Root ".git")) {
    $SourceRevision = (& git -C $Root rev-parse HEAD)
    if ($LASTEXITCODE -ne 0) { throw "Git-Quellstand konnte nicht ermittelt werden." }
    $SourceChanges = (& git -C $Root status --porcelain --untracked-files=normal)
    if ($LASTEXITCODE -ne 0) { throw "Git-Status konnte nicht ermittelt werden." }
    $SourceState = if ($SourceChanges) { "working-tree-has-changes" } else { "clean" }
} elseif (Test-Path -LiteralPath (Join-Path $Root "source-revision.txt")) {
    $SourceRevision = (Get-Content (Join-Path $Root "source-revision.txt") -Raw).Trim()
    $SourceState = "snapshot-revision-only-local-edits-not-detected"
}

# Always rebuild on Windows. No tests, launcher, install, signing or upload.
& (Join-Path $PSScriptRoot "package.ps1") -PackageType app-image -SkipLauncherCheck
$AppImage = Join-Path $Root "target\app-image\Bookshelf"
foreach ($File in @("Bookshelf.exe", "runtime\bin\server\jvm.dll", "app\legal\BUILD_JAVA_RELEASE.txt")) {
    if (-not (Test-Path -LiteralPath (Join-Path $AppImage $File))) {
        throw "Unvollstaendiges Windows-App-Image: $File"
    }
}
$Staging = Join-Path $Root ("target\msix-" + [guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $Staging | Out-Null
try {
    Copy-Item -LiteralPath $AppImage -Destination (Join-Path $Staging "App") -Recurse
    Copy-Item -LiteralPath (Join-Path $Root "packaging\windows\Assets") -Destination $Staging -Recurse
    [xml]$Manifest = Get-Content (Join-Path $Root "packaging\windows\AppxManifest.xml.template") -Raw -Encoding UTF8
    $Ns = New-Object System.Xml.XmlNamespaceManager($Manifest.NameTable)
    $Ns.AddNamespace("p", "http://schemas.microsoft.com/appx/manifest/foundation/windows10")
    $Ns.AddNamespace("uap", "http://schemas.microsoft.com/appx/manifest/uap/windows10")
    $Identity = $Manifest.SelectSingleNode("/p:Package/p:Identity", $Ns)
    $Identity.SetAttribute("Name", [string]$Settings.IdentityName)
    $Identity.SetAttribute("Publisher", [string]$Settings.Publisher)
    $Identity.SetAttribute("Version", $Version.ToString())
    $Manifest.SelectSingleNode("/p:Package/p:Properties/p:DisplayName", $Ns).InnerText = $Settings.DisplayName
    $Manifest.SelectSingleNode("/p:Package/p:Properties/p:PublisherDisplayName", $Ns).InnerText = $Settings.PublisherDisplayName
    $Visuals = $Manifest.SelectSingleNode("//uap:VisualElements", $Ns)
    $Visuals.SetAttribute("DisplayName", [string]$Settings.DisplayName)
    $Family = $Manifest.SelectSingleNode("/p:Package/p:Dependencies/p:TargetDeviceFamily", $Ns)
    $Family.SetAttribute("MinVersion", $Minimum.ToString())
    $Family.SetAttribute("MaxVersionTested", $Maximum.ToString())
    $Manifest.Save((Join-Path $Staging "AppxManifest.xml"))

    $PackageName = "buecherregal-$Version-windows-x64.msix"
    $BuiltPackage = Join-Path $Root ("target\" + [guid]::NewGuid().ToString("N") + ".msix")
    & $MakeAppx pack /d $Staging /p $BuiltPackage /h SHA256 /no
    if ($LASTEXITCODE -ne 0) { throw "MakeAppx fehlgeschlagen. Keine fertige Lieferung erzeugt." }
    New-Item -ItemType Directory -Path $Output | Out-Null
    Move-Item -LiteralPath $BuiltPackage -Destination (Join-Path $Output $PackageName)
    Copy-Item -LiteralPath (Join-Path $Staging "AppxManifest.xml") -Destination $Output
    Copy-Item -LiteralPath "$env:JAVA_HOME\release" -Destination (Join-Path $Output "BUILD_JAVA_RELEASE.txt")
    Get-ChildItem -LiteralPath $Staging -Recurse -File | Sort-Object FullName | ForEach-Object {
        [pscustomobject]@{
            Path = $_.FullName.Substring($Staging.Length + 1)
            Bytes = $_.Length
            SHA256 = (Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant()
        }
    } | Export-Csv -LiteralPath (Join-Path $Output "payload-inventory.csv") -NoTypeInformation -Encoding UTF8
    $Hash = (Get-FileHash -LiteralPath (Join-Path $Output $PackageName) -Algorithm SHA256).Hash.ToLowerInvariant()
    "$Hash  $PackageName" | Set-Content (Join-Path $Output "SHA256SUMS.txt") -Encoding ASCII
    [ordered]@{
        packageVersion = $Version.ToString()
        applicationVersion = [string]([xml](Get-Content (Join-Path $Root "pom.xml") -Raw)).project.version
        sourceCommit = $SourceRevision
        sourceState = $SourceState
        createdUtc = [DateTime]::UtcNow.ToString("o")
        signing = "unsigned-for-store"
        testsExecuted = $false
        windowsAcceptance = "pending"
        storeReady = $false
        makeAppx = (Get-Item -LiteralPath $MakeAppx).VersionInfo.FileVersion
    } | ConvertTo-Json | Set-Content (Join-Path $Output "build-info.json") -Encoding UTF8
    Write-Host "Unsigniertes MSIX erstellt: $Output"
    Write-Host "Keine Tests, Installation, Signierung oder Veroeffentlichung ausgefuehrt."
    Write-Host "Windows-Abnahme, WACK und Store-Identitaetsabgleich bleiben erforderlich."
} finally {
    if (Test-Path -LiteralPath $Staging) { Remove-Item -LiteralPath $Staging -Recurse -Force }
}
