// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

/**
 * Global enum to indicate a specific script type.
 */
public enum ScriptType {
  /** Indicates that code is a complete BAF source. */
  BAF,
  /** Indicates that code is a complete BCS resource. */
  BCS,
  /** Indicates that code is a sequence of script triggers. */
  TRIGGER,
  /** Indicates that code is a sequence of script actions. */
  ACTION,
  /** Indicates a special meaning. */
  CUSTOM
}
