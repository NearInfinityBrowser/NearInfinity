// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.StructViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sav.SavResource;
import org.infinity.util.StringTable;

/**
 * This resource serves a similar purpose (and has a similar structure to)
 * {@link StringTable TLK} files. The resource version 1 work in conjunction with
 * {@link TotResource TOT} files, and can be used to override text from {@code dialog.tlk}.
 * Version 2 of the TOH format combines the separate TOH/TOT files requried by version 1.
 * The files are contained in the {@link SavResource SAV} files in the savegame folders.
 * <p>
 * IWD: TOT files are used to edit the character biographies during the game
 * (when the characters are not exported)
 * <p>
 * BG2: TOT files are used to store all custom players comments inside game e.g.
 * custom waypoints on the area map, custom notes in the journal and character biographies
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/toh.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/toh.htm</a>
 */
public final class TohResource extends AbstractStruct implements Resource
{
  // TOH-specific field labels
  public static final String TOH_NUM_ENTRIES    = "# strref entries";
  public static final String TOH_OFFSET_ENTRIES = "Strref entries offset";
  public static final String TOH_LANGUAGE_TYPE  = "Language type";

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
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    final int startOffset = offset;
    final boolean isEnhanced = Profile.isEnhancedEdition() && (buffer.getInt(offset + 4) == 2);
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new DecNumber(buffer, offset + 4, 4, COMMON_VERSION));
    addField(new DecNumber(buffer, offset + 8, 4, TOH_LANGUAGE_TYPE));
    SectionCount scStrref = new SectionCount(buffer, offset + 12, 4, TOH_NUM_ENTRIES, StrRefEntry.class);
    addField(scStrref);
    SectionOffset soStrref = null;
    if (isEnhanced) {
      soStrref = new SectionOffset(buffer, offset + 16, TOH_OFFSET_ENTRIES, StrRefEntry.class);
      addField(soStrref);
    } else {
      addField(new Unknown(buffer, offset + 16, 4));
    }

    List<Integer> ofsList = null;
    offset = 20;
    if (isEnhanced) {
      offset = soStrref.getValue();
      ofsList = new ArrayList<>(scStrref.getValue());
    }
    for (int i = 0; i < scStrref.getValue(); i++) {
      if (isEnhanced) {
        // storing string offset for later
        int ofs = soStrref.getValue() + buffer.getInt(offset + 4);
        ofsList.add(ofs);
        // adding strref entries structure
        StrRefEntry2 entry = new StrRefEntry2(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      } else {
        StrRefEntry entry = new StrRefEntry(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      }
    }

    if (isEnhanced) {
      for (int i = 0; i < scStrref.getValue(); i++) {
        StringEntry2 entry = new StringEntry2(this, buffer, startOffset + ofsList.get(i), i);
        addField(entry);
        offset += entry.getEndOffset();
      }
    }

    int endoffset = offset;
    for (final StructEntry entry : getFields()) {
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }
    return endoffset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    // disabling 'Save' button
    JButton bSave = (JButton)viewer.getButtonPanel().getControlByType(ButtonPanel.Control.SAVE);
    if (bSave != null) {
      bSave.setEnabled(false);
    }
  }
}
