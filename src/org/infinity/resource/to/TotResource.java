// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.to;

import java.nio.ByteBuffer;

import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sav.SavResource;
import org.infinity.util.StringTable;

/**
 * This resource serves a similar purpose (and has a similar structure to)
 * {@link StringTable TLK} files. The files can be used to override text from
 * {@code dialog.tlk}, and consist of string entries that are referenced from
 * {@link TohResource TOH} files by the offset number. The files are contained
 * in the {@link SavResource SAV} files in the savegame folders.
 * <p>
 * IWD: TOT files are used to edit the character biographies during the game
 * (when the characters are not exported).
 * <p>
 * BG2: TOT files are used to store all custom players comments inside game e.g.
 * custom waypoints on the area map, custom notes in the journal and character biographies
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/tot.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/tot.htm</a>
 */
public final class TotResource extends AbstractStruct implements Resource
{
  // TOT-specific field labels
  public static final String TOT_EMPTY = "(empty)";

  public TotResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    if (buffer != null && buffer.limit() > 0) {
      // TODO: fetch number of valid string entries from associated TOH resource
      for (int i = 0; offset + 524 <= buffer.limit(); i++) {
        StringEntry entry = new StringEntry(this, buffer, offset, i);
        offset = entry.getEndOffset();
        addField(entry);
      }
    } else {
      addField(new Unknown(buffer, offset, 0, TOT_EMPTY));  // Placeholder for empty structure
    }

    int endoffset = offset;
    for (final StructEntry entry : getFields()) {
      if (entry.getOffset() + entry.getSize() > endoffset)
        endoffset = entry.getOffset() + entry.getSize();
    }

    return endoffset;
  }
}
