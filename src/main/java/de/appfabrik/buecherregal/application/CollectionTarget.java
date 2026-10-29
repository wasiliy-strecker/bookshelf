package de.appfabrik.buecherregal.application;

import java.nio.file.Path;
import java.time.Instant;

/** File identity captured before asking to replace an existing export. */
public record CollectionTarget(
    Path path, boolean exists, Object fileKey, long size, Instant modified) {}
