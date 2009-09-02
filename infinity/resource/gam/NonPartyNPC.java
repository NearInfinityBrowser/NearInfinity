// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.gam;

import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;

final class NonPartyNPC extends PartyNPC
{
  NonPartyNPC() throws Exception
  {
    super(null, "Non-party character",
          ResourceFactory.getGameID() == ResourceFactory.ID_BG1 ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG1TOTSC ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
          ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
          ResourceFactory.getGameID() == ResourceFactory.ID_TUTU ? new byte[352] :
          ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ? new byte[360] :
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2 ? new byte[832] : new byte[384],
          0);
  }

  NonPartyNPC(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, "Non-party character " + nr, buffer, offset);
  }
}