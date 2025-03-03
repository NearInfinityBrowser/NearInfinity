// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Locale;
import java.util.regex.PatternSyntaxException;

/**
 * This class provides information about the current architecture and operating system.
 */
public class Platform {
  /** Enumeration of supported operating systems. */
  public enum OS {
    WINDOWS("^.*win.*$"),
    UNIX("^.*(nix|nux|aix|hpux|bsd).*$"),
    MAC_OS("^.*(mac|osx|darwin).*$"),
    SOLARIS("^.*(sunos|solaris).*$"),
    UNKNOWN(null),
    ;

    /** Returns the {@link OS} value that matches the current system architecture.  */
    public static OS getCurrentOS() {
      for (final OS os : OS.values()) {
        if (os.isOS()) {
          return os;
        }
      }
      return UNKNOWN;
    }

    private final boolean check;

    OS(String regex) {
      final String osName = System.getProperty("os.name").toLowerCase(Locale.ROOT);
      this.check = regex != null && osName.matches(regex);
    }

    /** Returns whether this {@link OS} enum matches the current operating system. */
    public boolean isOS() {
      return check;
    }
  }

  /** Enumeration of supported system architectures. */
  public enum Arch {
    X86_64(64, "^(x8664|amd64|ia32e|em64t|x64)$"),
    X86_32(32, "^(x86(32)?|i[3-6]86|ia32|x32)$"),
    ITANIUM_64(64, "^(ia64w?|itanium64)$"),
    ITANIUM_32(32, "^ia64n$"),
    ARM_32(32, "^arm(32)?$"),
    AARCH_64(64, "^aarch64$"),
    PPC_32(32, "^ppc(32)?$"),
    PPC_64(64, "^ppc64$"),
    PPCLE_32(32, "^ppc(32)?le$"),
    PPCLE_64(64, "^ppc64le$"),
    UNKNOWN(0, null),
    ;

    /** Returns the {@link Arch} value that matches the current system architecture.  */
    public static Arch getCurrentArchitecture() {
      for (final Arch arch : Arch.values()) {
        if (arch.isArch()) {
          return arch;
        }
      }
      return UNKNOWN;
    }

    private final int bitness;
    private final boolean check;

    Arch(int bitness, String regex) {
      final String arch = System.getProperty("os.arch").toLowerCase(Locale.ROOT);
      this.check = regex != null && arch.matches(regex);
      this.bitness = bitness;
    }

    /**
     * Returns the bitness of the {@link Arch} value.
     * Returns {@code 0} if the value does not provide a specific bitness.
     */
    public int getBitness() {
      return bitness;
    }

    /** Returns whether this {@link Arch} enum value matches the current system architecture. */
    public boolean isArch() {
      return check;
    }
  }

  /** Whether this a Microsoft Windows system. */
  public final static boolean IS_WINDOWS = OS.WINDOWS.isOS();
  /** Whether this GNU/Linux or another Unix-like operating system. */
  public final static boolean IS_UNIX = OS.UNIX.isOS();
  /** Whether this an Apple Mac OS X system. */
  public final static boolean IS_MACOS = OS.MAC_OS.isOS();
  /** Whether this a Sun OS or Solaris system. */
  public final static boolean IS_SOLARIS = OS.SOLARIS.isOS();

  /** Whether the system is a 32-bit architecture. */
  public final static boolean IS_32BIT = (Arch.getCurrentArchitecture().bitness == 32);
  /** Whether the system is a 64-bit architecture. */
  public final static boolean IS_64BIT = (Arch.getCurrentArchitecture().bitness == 64);

  /** Returns the symbol used to separate individual path strings from each other for the current platform. */
  public final static String PATH_SEPARATOR = File.pathSeparator;

  /** Returns the system-dependent name-separator character as string for the current platform. */
  public final static String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

  /** Returns the default file extension for executables on the current system. */
  public final static String EXECUTABLE_EXT = OS.WINDOWS.isOS() ? ".exe" : "";

  /** Returns the "null" file that can be used to discard I/O output of external processes. */
  public final static File NULL_FILE = new File(System.getProperty("os.name").startsWith("Windows") ? "NUL" : "/dev/null");

  /** Returns the major version number of the active Java Runtime. */
  public final static int JAVA_VERSION = getJavaVersion();

  /**
   * Returns the Java Runtime major version.
   */
  public static int getJavaVersion() {
    int retVal = 8;

    String[] versionString = System.getProperty("java.specification.version").split("\\.");
    try {
      int major = Integer.parseInt(versionString[0]);
      if (major <= 1 && versionString.length > 1) {
        major = Integer.parseInt(versionString[1]);
      }
      retVal = major;
    } catch (NumberFormatException e) {
      Logger.trace(e);
    }

    return retVal;
  }

  /**
   * Resolves the full path of the given binary based on the environment variable {@code PATH}.
   *
   * @param binary Name of the binary.
   * @return Absolute {@link Path} of the binary if available, {@code null} otherwise.
   */
  public static Path resolveSystemPath(String binary) {
    if (binary == null || binary.trim().isEmpty()) {
      return null;
    }

    try {
      final String sysPath = System.getenv("PATH");
      if (sysPath != null) {
        final String[] paths = sysPath.split(Platform.PATH_SEPARATOR);
        for (final String path : paths) {
          if (!path.trim().isEmpty()) {
            try {
              Path p = Paths.get(path, binary);
              if (Files.isRegularFile(p)) {
                if (!p.isAbsolute()) {
                  p = p.toAbsolutePath();
                }
                return p;
              }
            } catch (InvalidPathException | IOError e) {
              Logger.debug(e);
            }
          }
        }
      }
    } catch (PatternSyntaxException e) {
      Logger.debug(e);
    }

    return null;
  }

  /**
   * Returns the path of the system's temporary directory.
   *
   * @return Absolute {@link Path} of the temporary directory.
   * @throws UnsupportedOperationException if the temporary directory does not exist.
   */
  public static Path getTempDirectory() {
    Path retVal = Paths.get(System.getProperty("java.io.tmpdir"));
    if (retVal == null) {
      throw new NullPointerException("Temp directory is null");
    }

    if (!retVal.isAbsolute()) {
      try {
        retVal = retVal.toAbsolutePath();
      } catch (IOError e) {
        Logger.debug(e);
      }
    }

    if (!Files.isDirectory(retVal)) {
      throw new UnsupportedOperationException("Temporary directory does not exist");
    }

    return retVal;
  }

  /**
   * Creates the specified subfolders in the system's temporary directory and adds them to the {@link FileDeletionHook}
   * for automatic removal.
   *
   * @param folders one or more subfolders that are recursively created in the temporary directory.
   * @return Absolute {@link Path} of the temporary directory.
   * @throws IOException if the directory could not be created.
   */
  public static Path createTempDirectory(String... folders) throws IOException {
    Path retVal = getTempDirectory();

    if (folders.length > 0) {
      for (final String folder : folders) {
        if (folder != null && !folder.trim().isEmpty()) {
          retVal = retVal.resolve(folder);
          if (!Files.isDirectory(retVal)) {
            try {
              Files.createDirectory(retVal);
              FileDeletionHook.getInstance().registerFile(retVal);
            } catch (UnsupportedOperationException e) {
              throw new IOException(e);
            }
          }
        }
      }
    }

    return retVal;
  }

  /**
   * Ensures that the specified file path can be executed.
   *
   * @param file The file {@link Path}.
   * @return {@code true} if {@code file} exists and is executable, {@code false} otherwise. On Windows system returns
   *         {@code true} only if the specified path is a file with the {@code .exe} file extension.
   */
  public static boolean makeExecutable(Path file) {
    if (file == null || !Files.exists(file)) {
      return false;
    }

    if (Platform.IS_WINDOWS) {
      return (file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe"));
    } else if (!Files.isExecutable(file)) {
      final PosixFilePermission[] xperms = {
          PosixFilePermission.OWNER_EXECUTE, PosixFilePermission.GROUP_EXECUTE, PosixFilePermission.OTHERS_EXECUTE,
          PosixFilePermission.OWNER_READ, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ
      };
      boolean setPerms = false;
      try {
        HashSet<PosixFilePermission> perms = new HashSet<>(Files.getPosixFilePermissions(file));
        for (final PosixFilePermission xperm : xperms) {
          if (!perms.contains(xperm)) {
            perms.add(xperm);
            setPerms = true;
          }
        }
        if (setPerms) {
          Files.setPosixFilePermissions(file, perms);
        }
      } catch (UnsupportedOperationException | IOException e) {
        Logger.debug(e);
        return false;
      }
    }
    return true;
  }

  private Platform() {
  }
}
