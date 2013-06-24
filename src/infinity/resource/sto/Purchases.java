// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sto;

import infinity.datatype.Bitmap;
import infinity.resource.AddRemovable;
import infinity.resource.ResourceFactory;
import infinity.resource.itm.ItmResource;

final class Purchases extends Bitmap implements AddRemovable
{
  Purchases()
  {
    super(new byte[4], 0, 4, "Store purchases",
          ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ? ItmResource.s_categories11 : ItmResource.s_categories);
  }

  Purchases(byte buffer[], int offset)
  {
    super(buffer, offset, 4, "Store purchases",
          ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ? ItmResource.s_categories11 : ItmResource.s_categories);
  }

//--------------------- Begin Interface AddRemovable ---------------------

  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------
}

