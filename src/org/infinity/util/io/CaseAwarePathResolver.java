package org.infinity.util.io;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.infinity.util.io.CaseAwareSingleDirectoryPathResolver.ResolutionResult;

public class CaseAwarePathResolver {

  private final Map<Path, CaseAwareSingleDirectoryPathResolver> roots = new ConcurrentHashMap<>();

  public Path resolve(Path path, boolean requireFullyResolvedPath) {
    if (!path.isAbsolute()) {
      throw new IllegalArgumentException("Provided path should be absolute, got " + path + " instead");
    }
    if (Files.exists(path)) {
      return path;
    }

    ResolutionResult resolutionResult = roots
        .computeIfAbsent(path.getRoot(), (root) -> new CaseAwareSingleDirectoryPathResolver(root))
        .resolve(path.subpath(0, path.getNameCount()));

    if (resolutionResult.getUnresolvedPart() == null) {
      return resolutionResult.getResolvedPart();
    } else if (requireFullyResolvedPath) {
      return null;
    } else {
      return resolutionResult.getResolvedPart().resolve(resolutionResult.getUnresolvedPart());
    }
  }
}
