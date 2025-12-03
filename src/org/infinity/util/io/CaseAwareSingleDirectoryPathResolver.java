package org.infinity.util.io;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class CaseAwareSingleDirectoryPathResolver {

  public static class ResolutionResult {
    private Path resolvedPart;
    private Path unresolvedPart;

    public ResolutionResult(Path resolvedPart, Path unresolvedPart) {
      super();
      this.resolvedPart = resolvedPart;
      this.unresolvedPart = unresolvedPart;
    }

    public Path getResolvedPart() {
      return resolvedPart;
    }

    public Path getUnresolvedPart() {
      return unresolvedPart;
    }
  }

  private final Map<Path, CaseAwareSingleDirectoryPathResolver> subdirectoryResolvers = new ConcurrentHashMap<>();
  private final Path directory;

  private boolean caseSensitivityIsFiguredOut = false;
  private boolean caseSensitive = false;

  public CaseAwareSingleDirectoryPathResolver(Path resolveFrom) {
    this.directory = resolveFrom;
  }

  public ResolutionResult resolve(Path relativePath) {
    Path unresolvedPart = relativePath;
    Path currentLocation = directory;
    Path nextPathElement = unresolvedPart.subpath(0, 1);
    Path actualNextLocation = resolveSinglePathElementToActualLocation(currentLocation, nextPathElement);
    if (actualNextLocation == null) {
      return new ResolutionResult(currentLocation, unresolvedPart);
    }

    if (unresolvedPart.getNameCount() == 1) { // this was the last path element, nothing left to resolve
      return new ResolutionResult(currentLocation.resolve(actualNextLocation), null);
    } else {
      unresolvedPart = unresolvedPart.subpath(1, unresolvedPart.getNameCount());
      return subdirectoryResolvers
          .computeIfAbsent(actualNextLocation, (key) -> new CaseAwareSingleDirectoryPathResolver(key))
          .resolve(unresolvedPart);
    }

  }

  private Path resolveSinglePathElementToActualLocation(Path resolveFrom, Path elementToResolve) {
    Path directResolution = resolveFrom.resolve(elementToResolve);
    if (Files.exists(directResolution)) {
      return directResolution;
    }

    if (currentDirIsKnownToBeCaseInsensitive()) {
      return null;
    }

    String searchString = elementToResolve.getFileName().toString();
    try (DirectoryStream<Path> ds = Files.newDirectoryStream(resolveFrom)) {
      for (final Path dirPath : ds) {
        if (!caseSensitivityIsFiguredOut) {
          tryFigureOutCaseSensitivityUsingExistingPath(dirPath);
          if (currentDirIsKnownToBeCaseInsensitive()) {
            break;
          }
        }
        String dirString = dirPath.getFileName().toString();
        if (searchString.equalsIgnoreCase(dirString)) {
          return resolveFrom.resolve(dirPath);
        }
      }
    } catch (Throwable t) {
      throw new RuntimeException(t);
    }
    return null;
  }

  private boolean currentDirIsKnownToBeCaseInsensitive() {
    return caseSensitivityIsFiguredOut && !caseSensitive;
  }

  private boolean tryFigureOutCaseSensitivityUsingExistingPath(Path existingPath) throws IOException {
    Path baseDir = existingPath.getParent();
    String filename = existingPath.getFileName().toString();
    String lowerCaseFilename = filename.toLowerCase();
    String upperCaseFilename = filename.toUpperCase();
    if (Objects.equals(lowerCaseFilename, upperCaseFilename)) {
      return false;
    }

    try {
      Path lowercaseFilePath = baseDir.resolve(lowerCaseFilename);
      Path uppercaseFilePath = baseDir.resolve(upperCaseFilename);
      this.caseSensitive = !Files.isSameFile(lowercaseFilePath, uppercaseFilePath);
      this.caseSensitivityIsFiguredOut = true;
    } catch (NoSuchFileException e) {
      this.caseSensitive = true;
      this.caseSensitivityIsFiguredOut = true;
    }
    return true;
  }

}
