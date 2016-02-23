// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;

import org.infinity.resource.graphics.BamDecoder;

/**
 * A structure to hold a unique identifier and the associated BAM animation.
 * @author argent77
 */
public class ResourceAnimation extends BasicResource
{
  private final BamDecoder bam;

  /**
   * Creates a new animation object that consists of a resource name (any unique name will do) and
   * the associated BAM animation.
   * @param key A unique keyword that can be used to identify the animation.
   * @param bam The BAM animation object.
   */
  public ResourceAnimation(String key, BamDecoder bam)
  {
    super(key);
    this.bam = bam;
  }

  @Override
  public BamDecoder getData()
  {
    return bam;
  }
}
