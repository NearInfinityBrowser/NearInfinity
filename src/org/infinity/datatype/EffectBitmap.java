// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;

import org.infinity.resource.effects.BaseOpcode;

/**
 * Specialized {@link Bitmap} type that represents a list of available effect opcodes.
 */
public class EffectBitmap extends Bitmap {
  public static final String EFFECT_FX = "Effect";

  public EffectBitmap(ByteBuffer buffer, int offset, int length) {
    this(buffer, offset, length, EFFECT_FX);
  }

  public EffectBitmap(ByteBuffer buffer, int offset, int length, String name) {
    super(buffer, offset, length, name, BaseOpcode.getEffectNames());
  }

  public EffectBitmap(ByteBuffer buffer, int offset, int length, boolean signed) {
    this(buffer, offset, length, EFFECT_FX, signed);
  }

  public EffectBitmap(ByteBuffer buffer, int offset, int length, String name, boolean signed) {
    super(buffer, offset, length, name, BaseOpcode.getEffectNames(), signed);
  }

  public EffectBitmap(ByteBuffer buffer, int offset, int length, boolean signed,
      boolean showAsHex) {
    this(buffer, offset, length, EFFECT_FX, signed, showAsHex);
  }

  public EffectBitmap(ByteBuffer buffer, int offset, int length, String name, boolean signed,
      boolean showAsHex) {
    super(buffer, offset, length, name, BaseOpcode.getEffectNames(), signed, showAsHex);
  }
}
