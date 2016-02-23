// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.are.viewer;


/**
 * The base class for structures that can hold a unique identifier and their data.
 * @author argent77
 */
public abstract class BasicResource
{
  private final String key;

  protected BasicResource(String key)
  {
    if (key == null || key.isEmpty()) {
      throw new NullPointerException();
    }
    this.key = key;
  }

  /**
   * Returns the key value of the resource structure.
   */
  public String getKey()
  {
    return key;
  }

  /**
   * Returns the data associated with the key.
   */
  public abstract Object getData();
}
