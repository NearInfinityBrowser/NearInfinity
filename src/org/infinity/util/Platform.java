// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import org.tinylog.Logger;

import java.io.File;
import java.nio.file.FileSystems;
import java.util.Locale;

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

    private OS(String regex) {
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

    private Arch(int bitness, String regex) {
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

  private Platform() {
  }
}
