// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.vef;

import infinity.resource.AbstractStruct;

public final class PrimaryComponent extends AbstractComponent
{
  // VEF/Component1-specific field labels
  public static final String VEF_COMP_PRI = "Primary component";

  PrimaryComponent() throws Exception
  {
    super(VEF_COMP_PRI);
  }

  PrimaryComponent(AbstractStruct superStruct, byte[] buffer, int offset) throws Exception
  {
    super(superStruct, buffer, offset, VEF_COMP_PRI);
  }
}
