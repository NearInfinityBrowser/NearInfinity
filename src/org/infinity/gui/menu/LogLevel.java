// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import org.tinylog.Level;

/** Wrapper for the {@link Level} enum from the tinylog logging framework. */
public enum LogLevel {
  TRACE(Level.TRACE, "Trace (fine-grained)"),
  DEBUG(Level.DEBUG, "Debug (diagnostic)"),
  INFO(Level.INFO, "Information (recommended)"),
  WARN(Level.WARN, "Warning"),
  ERROR(Level.ERROR, "Error"),
  OFF(Level.OFF, "Disabled (not recommended)"),
  ;

  private final Level level;
  private final String title;

  LogLevel(Level level, String title) {
    this.level = level;
    this.title = title;
  }

  /** Log {@link Level} associated with this enum. */
  public Level getLevel() {
    return level;
  }

  /** Title of the log level option in the Preferences dialog. */
  public String getTitle() {
    return title;
  }

  @Override
  public String toString() {
    return getTitle();
  }
}
