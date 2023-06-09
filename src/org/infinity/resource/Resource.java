// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource;

import org.infinity.resource.key.ResourceEntry;

public interface Resource extends Viewable {
  ResourceEntry getResourceEntry();
}
