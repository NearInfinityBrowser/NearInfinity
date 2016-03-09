// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.sto;

import org.infinity.datatype.Bitmap;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Profile;
import org.infinity.resource.itm.ItmResource;

public final class Purchases extends Bitmap implements AddRemovable
{
  // STO/Purchases-specific field labels
  public static final String STO_PURCHASES = "Store purchases";

  Purchases()
  {
    super(new byte[4], 0, 4, STO_PURCHASES,
          ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_STO_V11)) ? ItmResource.s_categories11
                                                                       : ItmResource.s_categories);
  }

  Purchases(byte buffer[], int offset, int number)
  {
    super(buffer, offset, 4, STO_PURCHASES + " " + number,
          ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_STO_V11)) ? ItmResource.s_categories11
                                                                       : ItmResource.s_categories);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------
}

