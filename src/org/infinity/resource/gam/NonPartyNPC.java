// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;

public final class NonPartyNPC extends PartyNPC
{
  // GAM/NonPartyNPC-specific field labels
  public static final String GAM_EXNPC = "Non-party character";

  NonPartyNPC() throws Exception
  {
    super(null, GAM_EXNPC,
          (Profile.getEngine() == Profile.Engine.BG1 ||
          Profile.getEngine() == Profile.Engine.BG2 ||
          Profile.isEnhancedEdition()) ? new byte[352] :
          (Profile.getEngine() == Profile.Engine.PST) ? new byte[360] :
          (Profile.getEngine() == Profile.Engine.IWD2) ? new byte[832] : new byte[384],
          0);
  }

  NonPartyNPC(AbstractStruct superStruct, byte buffer[], int offset, int nr) throws Exception
  {
    super(superStruct, GAM_EXNPC + " " + nr, buffer, offset);
  }
}
