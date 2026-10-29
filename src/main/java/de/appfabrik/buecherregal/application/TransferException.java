package de.appfabrik.buecherregal.application;

/** File errors remain independent of the UI language. */
public final class TransferException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public enum Code {
    READ,
    FORMAT,
    VERSION,
    LIMIT,
    TARGET,
    CHANGED,
    CONFIRM,
    WRITE,
    IMPORT
  }

  private final Code code;

  public TransferException(Code code) {
    this(code, null);
  }

  public TransferException(Code code, Throwable cause) {
    super(code.name(), cause);
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
