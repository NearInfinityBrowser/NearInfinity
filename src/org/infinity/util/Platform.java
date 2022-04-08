// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * This class can be used to determine the operating system where this Java application is running.
 */
public class Platform {
  public enum OS {
    WINDOWS, UNIX, MAC_OS, SOLARIS, UNKNOWN,
  }

  /** Whether this a Microsoft Windows system. */
  public final static boolean IS_WINDOWS = (getPlatform() == OS.WINDOWS);
  /** Whether this GNU/Linux or another Unix-like operating system. */
  public final static boolean IS_UNIX = (getPlatform() == OS.UNIX);
  /** Whether this an Apple Mac OS X system. */
  public final static boolean IS_MACOS = (getPlatform() == OS.MAC_OS);
  /** Whether this a Sun OS or Solaris system. */
  public final static boolean IS_SOLARIS = (getPlatform() == OS.SOLARIS);

  /** Returns the symbol used to separate individual path strings from each other for the current platform. */
  public final static String PATH_SEPARATOR = System.getProperty("path.separator");

  /** Returns the system-dependent name-separator character as string for the current platform. */
  public final static String SEPARATOR = System.getProperty("file.separator");

  /**
   * Determines the current operating system.
   */
  public static OS getPlatform() {
    String osName = System.getProperty("os.name").toLowerCase();
    if (osName.contains("mac") || osName.contains("darwin")) {
      return OS.MAC_OS;
    } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
      return OS.UNIX;
    } else if (osName.contains("win")) {
      return OS.WINDOWS;
    } else if (osName.contains("sunos") || osName.contains("solaris")) {
      return OS.SOLARIS;
    } else {
      return OS.UNKNOWN;
    }
  }

  private Platform() {
  }
}
