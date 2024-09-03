// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sav.SavResource;
import org.infinity.util.Logger;
import org.infinity.util.StringTable;
import org.infinity.util.io.FileManager;

/**
 * This resource serves a similar purpose (and has a similar structure to) {@link StringTable TLK} files. The files can
 * be used to override text from {@code dialog.tlk}, and consist of string entries that are referenced from
 * {@link TohResource TOH} files by the offset number. The files are contained in the {@link SavResource SAV} files in
 * the savegame folders.
 * <p>
 * IWD: TOT files are used to edit the character biographies during the game (when the characters are not exported).
 * <p>
 * BG2: TOT files are used to store all custom players comments inside game e.g. custom waypoints on the area map,
 * custom notes in the journal and character biographies
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/tot.htm">
 *      https://gibberlings3.github.io/iesdp/file_formats/ie_formats/tot.htm</a>
 */
public final class TotResource extends AbstractStruct implements Resource {
  // TOT-specific field labels
  public static final String TOT_EMPTY = "(empty)";

  public TotResource(ResourceEntry entry) throws Exception {
    this(entry, null);
  }

  public TotResource(ResourceEntry entry, TohResource toh) throws Exception {
    super(entry, toh);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception {
    if (buffer != null && buffer.limit() > 0) {
      final TohResource toh = loadAssociatedToh(getResourceEntry());
      if (toh != null) {
        // fetching valid string entries from associated TOH resource
        final List<StructEntry> tohEntries = new ArrayList<>(toh.getFields(StrRefEntry.class));
        tohEntries.sort((a, b) -> {
          StructEntry e1 = ((StrRefEntry) a).getAttribute(StrRefEntry.TOH_STRREF_OFFSET_TOT_STRING);
          StructEntry e2 = ((StrRefEntry) b).getAttribute(StrRefEntry.TOH_STRREF_OFFSET_TOT_STRING);
          return ((IsNumeric) e1).getValue() - ((IsNumeric) e2).getValue();
        });

        // handling unmapped region of data
        if (!tohEntries.isEmpty() && tohEntries.get(0).getOffset() > 0) {
          final StrRefEntry entry = (StrRefEntry) tohEntries.get(0);
          final int size = ((IsNumeric) entry.getAttribute(StrRefEntry.TOH_STRREF_OFFSET_TOT_STRING)).getValue();
          if (size > 0) {
            addField(new Unknown(buffer, offset, size));
          }
        }

        // handling string entries defined in TOH resource
        int idx = 0;
        for (final StructEntry se : tohEntries) {
          final StrRefEntry tohEntry = (StrRefEntry) se;
          offset = ((IsNumeric) tohEntry.getAttribute(StrRefEntry.TOH_STRREF_OFFSET_TOT_STRING)).getValue();
          // looping through entries to consider split text segments
          StringEntry stringEntry = new StringEntry(this, buffer, offset, idx);
          while (stringEntry != null) {
            if (!isStringEntryValid(stringEntry)) {
              throw new Exception(String.format("Invalid string section found at offset 0x%x", stringEntry.getOffset()));
            }
            offset = stringEntry.getEndOffset();
            addField(stringEntry);
            idx++;
            int ofsNextEntry = ((IsNumeric) stringEntry.getAttribute(StringEntry.TOT_STRING_OFFSET_NEXT_ENTRY))
                .getValue();
            if (ofsNextEntry != -1) {
              stringEntry = new StringEntry(this, buffer, ofsNextEntry, idx);
            } else {
              stringEntry = null;
            }
          }
        }
      } else {
        // guessing string entries (most likely using incorrect offsets)
        for (int i = 0; offset + 524 <= buffer.limit(); i++) {
          final StringEntry entry = new StringEntry(this, buffer, offset, i);
          offset = entry.getEndOffset();
          addField(entry);
        }
      }
    } else {
      addField(new Unknown(buffer, offset, 0, TOT_EMPTY)); // Placeholder for empty structure
    }

    int endoffset = offset;
    for (final StructEntry entry : getFields()) {
      if (entry.getOffset() + entry.getSize() > endoffset) {
        endoffset = entry.getOffset() + entry.getSize();
      }
    }

    return endoffset;
  }

  /**
   * Analyzes a TOT {@code StringEntry} to determine whether the entry contains valid data.
   *
   * @param entry a {@link StringEntry}
   * @return {@code true} if the entry is valid, {@code false} otherwise.
   */
  public static boolean isStringEntryValid(StringEntry entry) {
    boolean retVal = false;

    if (entry != null) {
      try {
        int ofsPrev = ((IsNumeric) entry.getAttribute(StringEntry.TOT_STRING_OFFSET_PREV_ENTRY)).getValue();
        int ofsNext = ((IsNumeric) entry.getAttribute(StringEntry.TOT_STRING_OFFSET_NEXT_ENTRY)).getValue();
        retVal = (ofsPrev == -1 && ofsNext == -1) || (ofsPrev != ofsNext);
      } catch (Exception e) {
        Logger.error(e);
      }
    }

    return retVal;
  }

  /**
   * Attempts to find the associated TOH resource in the same directory as the current TOT resource and loads it if
   * available.
   *
   * @param totResource The current TOT resource.
   * @return {@code TohResource} instance if loaded successfully, {@code null} otherwise.
   */
  private TohResource loadAssociatedToh(ResourceEntry totResource) {
    TohResource toh = (getExtraData() instanceof TohResource) ? (TohResource) getExtraData() : null;

    if (toh == null && totResource != null) {
      final Path totPath = totResource.getActualPath();
      if (totPath != null) {
        String fileName = totPath.getName(totPath.getNameCount() - 1).toString();
        char ch = fileName.charAt(fileName.length() - 1); // last character of file extension (TOT)
        ch -= 12; // TOT -> TOH (considers case)
        fileName = fileName.substring(0, fileName.length() - 1) + String.valueOf(ch);
        final Path tohPath = FileManager.queryExisting(totPath.getParent(), fileName);
        try {
          toh = new TohResource(new FileResourceEntry(tohPath));
        } catch (Exception e) {
          Logger.error(e);
        }
      }
    }

    return toh;
  }
}
