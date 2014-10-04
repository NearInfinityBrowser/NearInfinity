// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.to;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import infinity.datatype.DecNumber;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.ButtonPanel;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

public final class TohResource extends AbstractStruct implements Resource
{
  public TohResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public void close() throws Exception
  {
    // don't save changes
  }

  @Override
  protected int read(byte[] buffer, int offset) throws Exception
  {
    int startOffset = offset;
    boolean isEnhanced = (ResourceFactory.isEnhancedEdition()) &&
                         (DynamicArray.getInt(buffer, offset + 4) == 2);
    list.add(new TextString(buffer, offset, 4, "Signature"));
    if (isEnhanced) {
      list.add(new DecNumber(buffer, offset + 4, 4, "Version"));
    } else {
      list.add(new TextString(buffer, offset + 4, 4, "Version"));
    }
    list.add(new Unknown(buffer, offset + 8, 4));
    SectionCount scStrref = new SectionCount(buffer, offset + 12, 4, "# strref entries", StrRefEntry.class);
    list.add(scStrref);
    SectionOffset soStrref = null;
    if (isEnhanced) {
      soStrref = new SectionOffset(buffer, offset + 16, "Strref entries offset", StrRefEntry.class);
      list.add(soStrref);
    } else {
      list.add(new Unknown(buffer, offset + 16, 4));
    }

    List<Integer> ofsList = null;
    offset = 20;
    if (isEnhanced) {
      offset = soStrref.getValue();
      ofsList = new ArrayList<Integer>(scStrref.getValue());
    }
    for (int i = 0; i < scStrref.getValue(); i++) {
      if (isEnhanced) {
        // storing string offset for later
        int ofs = soStrref.getValue() + DynamicArray.getInt(buffer, offset + 4);
        ofsList.add(ofs);
        // adding strref entries structure
        StrRefEntry2 entry = new StrRefEntry2(this, buffer, offset, i);
        offset = entry.getEndOffset();
        list.add(entry);
      } else {
        StrRefEntry entry = new StrRefEntry(this, buffer, offset, i);
        offset = entry.getEndOffset();
        list.add(entry);
      }
    }

    if (isEnhanced) {
      for (int i = 0; i < scStrref.getValue(); i++) {
        StringEntry2 entry = new StringEntry2(this, buffer, startOffset + ofsList.get(i), i);
        list.add(entry);
        offset += entry.getEndOffset();
      }
    }

    int endoffset = offset;
    for (int i = 0; i < list.size(); i++) {
      StructEntry entry = list.get(i);
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    // disabling 'Save' button
    JButton bSave = (JButton)viewer.getButtonPanel().getControlByType(ButtonPanel.Control.Save);
    if (bSave != null) {
      bSave.setEnabled(false);
    }
  }
}
