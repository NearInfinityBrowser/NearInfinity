// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

/**
 * This class can be used to determine the operating system where this Java application is running.
 */
public class Platform
{
  public enum OS {
    Windows,
    Unix,
    MacOS,
    Solaris,
    Unknown,
  }

  /** Whether this a Microsoft Windows system. */
  public final static boolean IS_WINDOWS  = (getPlatform() == OS.Windows);
  /** Whether this GNU/Linux or another Unix-like operating system. */
  public final static boolean IS_UNIX     = (getPlatform() == OS.Unix);
  /** Whether this an Apple Mac OS X system. */
  public final static boolean IS_MACOS    = (getPlatform() == OS.MacOS);
  /** Whether this a Sun OS or Solaris system. */
  public final static boolean IS_SOLARIS  = (getPlatform() == OS.Solaris);

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
      return OS.MacOS;
    } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
      return OS.Unix;
    } else if (osName.contains("win")) {
      return OS.Windows;
    } else if (osName.contains("sunos") || osName.contains("solaris")) {
      return OS.Solaris;
    } else {
      return OS.Unknown;
    }
  }

  private Platform() { }
}
