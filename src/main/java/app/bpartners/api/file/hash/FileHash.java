package app.bpartners.api.file.hash;

import app.bpartners.api.PojaGenerated;

@PojaGenerated
public record FileHash(FileHashAlgorithm algorithm, String value) {}
