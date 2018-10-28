// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

public interface IsReference
{
  /**
   * Gets full resource name: name dot extension.
   *
   * @return String {@code "None"} or empty string, if object not contains
   *         reference, reference to resource otherwise.
   */
  String getResourceName();//FIXME: Replace special value to null
}
