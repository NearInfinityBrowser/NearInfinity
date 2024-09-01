// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
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
import org.tinylog.Logger;

/**
 * This resource serves a similar purpose (and has a similar structure to) {@link StringTable TLK} files. The resource
 * version 1 work in conjunction with {@link TotResource TOT} files, and can be used to override text from
 * {@code dialog.tlk}. Version 2 of the TOH format combines the separate TOH/TOT files requried by version 1. The files
 * are contained in the {@link SavResource SAV} files in the savegame folders.
 * <p>
 * IWD: TOT files are used to edit the character biographies during the game (when the characters are not exported)
 * <p>
 * BG2: TOT files are used to store all custom players comments inside game e.g. custom waypoints on the area map,
 * custom notes in the journal and character biographies
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/toh.htm">
 *      https://gibberlings3.github.io/iesdp/file_formats/ie_formats/toh.htm</a>
 */
public final class TohResource extends AbstractStruct implements Resource {
  // TOH-specific field labels
  public static final String TOH_NUM_ENTRIES    = "# strref entries";
  public static final String TOH_OFFSET_ENTRIES = "Strref entries offset";
  public static final String TOH_LANGUAGE_TYPE  = "Language type";

  public TohResource(ResourceEntry entry) throws Exception {
    super(entry);
  }

  @Override
  public void close() throws Exception {
    // don't save changes
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
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
      if (entry.getOffset() + entry.getSize() > endoffset) {
        endoffset = entry.getOffset() + entry.getSize();
      }
    }
    return endoffset;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer) {
    // disabling 'Save' buttons
    JButton bSave = (JButton) viewer.getButtonPanel().getControlByType(ButtonPanel.Control.SAVE);
    if (bSave != null) {
      bSave.setEnabled(false);
    }
    JButton bSaveAs = (JButton) viewer.getButtonPanel().getControlByType(ButtonPanel.Control.SAVE_AS);
    if (bSaveAs != null) {
      bSave.setEnabled(false);
    }
  }

  /**
   * Returns the string for {@code strref} from the given string override resource entries.
   *
   * @param tohEntry {@link ResourceEntry} instance referencing a TOH resource. Required by both EE and non-EE games.
   * @param totEntry {@link ResourceEntry} instance referencing a TOT resource. Required only by non-EE games.
   * @param strref The string reference to look up.
   * @return String referenced by {@code strref} if found, {@code null} otherwise.
   */
  public static String getOverrideString(ResourceEntry tohEntry, ResourceEntry totEntry, int strref) {
    TohResource toh = null;
    TotResource tot = null;
    try {
      toh = (tohEntry != null) ? new TohResource(tohEntry) : null;
    } catch (Exception e) {
      Logger.error(e);
    }
    if (!Profile.isEnhancedEdition()) {
      try {
        tot = (totEntry != null) ? new TotResource(totEntry, toh) : null;
      } catch (Exception e) {
        Logger.error(e);
      }
    }
    return getOverrideString(toh, tot, strref);
  }

  /**
   * Returns the string for {@code strref} from the given string override resources.
   *
   * @param toh {@link TohResource} instance. Required by both EE and non-EE games.
   * @param tot {@link TotResource} instance. Required only by non-EE games.
   * @param strref The string reference to look up.
   * @return String referenced by {@code strref} if found, {@code null} otherwise.
   */
  public static String getOverrideString(TohResource toh, TotResource tot, int strref) {
    String retVal = null;

    if (strref < 0 || toh == null || (!Profile.isEnhancedEdition() && tot == null)) {
      return retVal;
    }

    if (Profile.isEnhancedEdition()) {
      // Only TOH resource is needed
      IsNumeric so = (IsNumeric) toh.getAttribute(TohResource.TOH_OFFSET_ENTRIES);
      IsNumeric sc = (IsNumeric) toh.getAttribute(TohResource.TOH_NUM_ENTRIES);
      if (so != null && sc != null && sc.getValue() > 0) {
        for (int i = 0, count = sc.getValue(), curOfs = so.getValue(); i < count; i++) {
          StrRefEntry2 strrefEntry = (StrRefEntry2) toh.getAttribute(curOfs, false);
          if (strrefEntry != null) {
            int v = ((IsNumeric) strrefEntry.getAttribute(StrRefEntry2.TOH_STRREF_OVERRIDDEN)).getValue();
            if (v == strref) {
              int sofs = ((IsNumeric) strrefEntry.getAttribute(StrRefEntry2.TOH_STRREF_OFFSET_STRING)).getValue();
              StringEntry2 se = (StringEntry2) toh.getAttribute(so.getValue() + sofs, false);
              if (se != null) {
                retVal = se.getAttribute(StringEntry2.TOH_STRING_TEXT).toString();
              }
              break;
            }
            curOfs += strrefEntry.getSize();
          }
        }
      }
    } else {
      // Utilizing both TOT and TOH
      IsNumeric sc = (IsNumeric) toh.getAttribute(TohResource.TOH_NUM_ENTRIES);
      if (sc != null && sc.getValue() > 0) {
        for (int i = 0, count = sc.getValue(), curOfs = 0x14; i < count; i++) {
          StrRefEntry strrefEntry = (StrRefEntry) toh.getAttribute(curOfs, false);
          if (strrefEntry != null) {
            int v = ((IsNumeric) strrefEntry.getAttribute(StrRefEntry.TOH_STRREF_OVERRIDDEN)).getValue();
            if (v == strref) {
              // string entry may consist of multiple segments
              retVal = "";
              int sofs = ((IsNumeric) strrefEntry.getAttribute(StrRefEntry.TOH_STRREF_OFFSET_TOT_STRING)).getValue();
              while (sofs >= 0) {
                StringEntry se = (StringEntry) tot.getAttribute(sofs, false);
                if (se != null) {
                  retVal += se.getAttribute(StringEntry.TOT_STRING_TEXT).toString();
                  sofs = ((IsNumeric) se.getAttribute(StringEntry.TOT_STRING_OFFSET_NEXT_ENTRY)).getValue();
                } else {
                  sofs = -1;
                }
              }
              break;
            }
            curOfs += strrefEntry.getSize();
          }
        }
      }
    }

    return retVal;
  }
}
