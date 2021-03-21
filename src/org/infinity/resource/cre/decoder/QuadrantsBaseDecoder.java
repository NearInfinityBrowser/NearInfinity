// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.util.IniMap;

/**
 * Common base for processing segmented creature animations.
 */
public abstract class QuadrantsBaseDecoder extends SpriteDecoder
{
  public static final DecoderAttribute KEY_QUADRANTS  = DecoderAttribute.with("quadrants", DecoderAttribute.DataType.INT);

  /** Returns the number of segments this creature animation has been split into. */
  public int getQuadrants() { return getAttribute(KEY_QUADRANTS); }
  protected void setQuadrants(int v) { setAttribute(KEY_QUADRANTS, v); }

  public QuadrantsBaseDecoder(AnimationInfo.Type type, int animationId, IniMap ini) throws Exception
  {
    super(type, animationId, ini);
  }

  protected QuadrantsBaseDecoder(AnimationInfo.Type type, CreResource cre) throws Exception
  {
    super(type, cre);
  }

  @Override
  protected void init() throws Exception
  {
    // setting properties
    initDefaults(getAnimationInfo());
    setDetectedByInfravision(true);
  }
}
