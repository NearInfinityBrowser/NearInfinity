// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.gam;

import java.nio.ByteBuffer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.HexNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.resource.AbstractStruct;

public final class Familiar extends AbstractStruct {
  // GAM/Familiar-specific field labels
  public static final String GAM_FAMILIAR                   = "Familiar info";
  public static final String GAM_FAMILIAR_LG                = "Lawful good";
  public static final String GAM_FAMILIAR_LN                = "Lawful neutral";
  public static final String GAM_FAMILIAR_LE                = "Lawful evil";
  public static final String GAM_FAMILIAR_NG                = "Neutral good";
  public static final String GAM_FAMILIAR_TN                = "True neutral";
  public static final String GAM_FAMILIAR_NE                = "Neutral evil";
  public static final String GAM_FAMILIAR_CG                = "Chaotic good";
  public static final String GAM_FAMILIAR_CN                = "Chaotic neutral";
  public static final String GAM_FAMILIAR_CE                = "Chaotic evil";
  public static final String GAM_FAMILIAR_OFFSET_RESOURCES  = "Familiar resources offset";
  public static final String GAM_FAMILIAR_COUNT_FMT         = "%s level %d familiar count";
  public static final String GAM_FAMILIAR_RESOURCE_FMT      = "Familiar resource %d";

  public static final String[] ALIGNMENT_LABELS = { "LG", "LN", "CG", "NG", "TN", "NE", "LE", "CN", "CE" };

  Familiar(AbstractStruct superStruct, ByteBuffer buffer, int offset) throws Exception {
    super(superStruct, GAM_FAMILIAR, buffer, offset);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    addField(new ResourceRef(buffer, offset, GAM_FAMILIAR_LG, "CRE"));
    addField(new ResourceRef(buffer, offset + 8, GAM_FAMILIAR_LN, "CRE"));
    addField(new ResourceRef(buffer, offset + 16, GAM_FAMILIAR_LE, "CRE"));
    addField(new ResourceRef(buffer, offset + 24, GAM_FAMILIAR_NG, "CRE"));
    addField(new ResourceRef(buffer, offset + 32, GAM_FAMILIAR_TN, "CRE"));
    addField(new ResourceRef(buffer, offset + 40, GAM_FAMILIAR_NE, "CRE"));
    addField(new ResourceRef(buffer, offset + 48, GAM_FAMILIAR_CG, "CRE"));
    addField(new ResourceRef(buffer, offset + 56, GAM_FAMILIAR_CN, "CRE"));
    addField(new ResourceRef(buffer, offset + 64, GAM_FAMILIAR_CE, "CRE"));
    HexNumber offEOS = new HexNumber(buffer, offset + 72, 4, GAM_FAMILIAR_OFFSET_RESOURCES);
    addField(offEOS);
    offset += 76;
    // To be confirmed: I've never seen these fields in use
    int numFamiliarExtra = 0;
    for (final String align : ALIGNMENT_LABELS) {
      for (int i = 1; i < 10; i++) {
        DecNumber familiarCount = new DecNumber(buffer, offset, 4, String.format(GAM_FAMILIAR_COUNT_FMT, align, i));
        numFamiliarExtra += familiarCount.getValue();
        addField(familiarCount);
        offset += 4;
      }
    }
    if (numFamiliarExtra > 0) {
      int curOffset = offEOS.getValue();
      for (int i = 0; i < numFamiliarExtra; i++) {
        addField(new ResourceRef(buffer, curOffset, String.format(GAM_FAMILIAR_RESOURCE_FMT, i), "CRE"));
        curOffset += 8;
      }
    }
    return offset;
  }

  void updateFilesize(DecNumber filesize) {
    DecNumber fs = (DecNumber) getAttribute(GAM_FAMILIAR_OFFSET_RESOURCES);
    fs.setValue(filesize.getValue());
  }
}
