// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.effects;

import java.util.HashMap;

/**
 * A generic opcode implementation intended for representation of unknown or invalid opcodes.
 */
public class DefaultOpcode extends BaseOpcode {
  private static final HashMap<Integer, DefaultOpcode> OPCODE_CACHE = new HashMap<>();

  /** Removes all accumulated default opcode instances from the cache. */
  public static void clearCache() {
    OPCODE_CACHE.clear();
  }

  /** Returns the name for an unknown opcode. */
  public static String getDefaultName() {
    return "Unknown";
  }

  /**
   * Returns the default opcode instance for the specified opcode number. For performance reasons opcode instances
   * will be recycled if possible.
   *
   * @param id Requested opcode number.
   * @return {@code DefaultOpcode} instance for the given opcode number.
   */
  public static DefaultOpcode get(int id) {
    DefaultOpcode retVal = OPCODE_CACHE.get(id);
    if (retVal == null) {
      retVal = new DefaultOpcode(id);
      OPCODE_CACHE.put(id, retVal);
    }
    return retVal;
  }

  @Override
  public boolean isAvailable() {
    return false;
  }

  private DefaultOpcode(int id) {
    super(id, getDefaultName());
  }
}
