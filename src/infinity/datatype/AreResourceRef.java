// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.resource.ResourceFactory;
import infinity.resource.are.AreResource;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;

public final class AreResourceRef extends ResourceRef
{
  private final String legalBIFs[];
  // BG1: SFXSound.BIF, CRESound.BIF
  // BG2: AMBSound.BIF 25AmbSnd.BIF
  // IDW1: SNDgen.BIF eSFXamb.BIF
  // IDW2: SNDsfx.BIF
  // PST: sound.BIF

  public AreResourceRef(byte h_buffer[], int offset, String name, AreResource are)
  {
    super(h_buffer, offset, name, "WAV");
    ResourceEntry res = ResourceFactory.getInstance().getResourceEntry(
            ((ResourceRef)are.getAttribute("WED resource")).getResourceName());
    String wedBIFF = "_dummy";
    if (res instanceof BIFFResourceEntry)
      wedBIFF = ((BIFFResourceEntry)res).getBIFFEntry().toString();
    int gameID = ResourceFactory.getGameID();
    if (gameID == ResourceFactory.ID_BG1 || gameID == ResourceFactory.ID_BG1TOTSC)
      legalBIFs = new String[]{wedBIFF, "data/sfxsound.bif", "data/cresound.bif"};
    else if (gameID == ResourceFactory.ID_BG2 || gameID == ResourceFactory.ID_BG2TOB)
      legalBIFs = new String[]{wedBIFF, "data/ambsound.bif", "data/25ambsnd.bif", "data/sfxsound.bif"};
    else if (gameID == ResourceFactory.ID_ICEWIND || gameID == ResourceFactory.ID_ICEWINDHOW ||
             gameID == ResourceFactory.ID_ICEWINDHOWTOT)
      legalBIFs = new String[]{wedBIFF, "data/sndgen.bif", "data/esfxamb.bif"};
    else if (gameID == ResourceFactory.ID_ICEWIND2)
      legalBIFs = new String[]{wedBIFF, "data/sndsfx.bif"};
    else if (gameID == ResourceFactory.ID_TORMENT)
      legalBIFs = new String[]{wedBIFF, "sound.bif"};
    else
      legalBIFs = new String[]{wedBIFF};
//    ResourceEntry entry = ResourceFactory.getInstance().getResourceEntry(getResourceName());
//    if (!isLegalEntry(entry)) {
//      System.out.println("Illegal: " + entry + " from " + entry.getActualFile());
//      System.out.println("In: " + are.getResourceEntry() + " from " + are.getResourceEntry().getActualFile());
//    }
  }

  public boolean isLegalEntry(ResourceEntry entry)
  {
    if (!(entry instanceof BIFFResourceEntry))
      return true;
    if (entry.hasOverride())
      return true;
    String bifName = ((BIFFResourceEntry)entry).getBIFFEntry().toString();
//    if (bifName.length() > 8 && bifName.substring(0, 9).equalsIgnoreCase("data/AREA"))
//      return true;
    for (final String legalBIF : legalBIFs) {
      if (bifName.equalsIgnoreCase(legalBIF))
        return true;
    }
    return false;
  }
}

