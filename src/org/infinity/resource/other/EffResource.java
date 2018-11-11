// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.other;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;

import org.infinity.datatype.EffectType;
import org.infinity.datatype.TextString;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Effect2;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.SearchOptions;

/**
 * This resource describes an effect (opcode) and its parameters. The resource of
 * version 1 is only ever found embedded in other files, but resource of version 2
 * is an extended version of that found embedded in creatures, items and spells,
 * and is a replacement for the version 1 embedded effects used in BG1.
 * <p>
 * The engine appears to roll a probability for each valid target type, rather than
 * one probability per attack.
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/eff_v1.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/eff_v1.htm</a>
 */
public final class EffResource extends AbstractStruct implements Resource, HasViewerTabs
{
  // EFF-specific field labels
  public static final String EFF_SIGNATURE_2  = "Signature 2";
  public static final String EFF_VERSION_2    = "Version 2";

  private StructHexViewer hexViewer;

  public EffResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    addField(new TextString(buffer, offset + 4, 4, COMMON_VERSION));
    addField(new TextString(buffer, offset + 8, 4, EFF_SIGNATURE_2));
    addField(new TextString(buffer, offset + 12, 4, EFF_VERSION_2));
    EffectType type = new EffectType(buffer, offset + 16, 4);
    addField(type);
    List<StructEntry> list = new ArrayList<>();
    offset = type.readAttributes(buffer, offset + 20, list);
    addFields(getFields().size() - 1, list);

    list.clear();
    Effect2.readCommon(list, buffer, offset);
    addFields(getFields().size() - 1, list);

    return offset + 216;
  }

//--------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (hexViewer == null) {
      hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

//--------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  /**
   * Called by "Extended Search"
   * Checks whether the specified resource entry matches all available search options.
   */
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        EffResource eff = new EffResource(entry);
        boolean retVal = true;
        String key;
        Object o;

        String[] keyList = new String[]{SearchOptions.EFF_Effect, SearchOptions.EFF_Param1,
                                        SearchOptions.EFF_Param2, SearchOptions.EFF_TimingMode,
                                        SearchOptions.EFF_Duration};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct;
            if (SearchOptions.isResourceByOffset(key)) {
              int ofs = SearchOptions.getResourceIndex(key);
              struct = eff.getAttribute(ofs, false);
            } else {
              struct = eff.getAttribute(SearchOptions.getResourceName(key), false);
            }
            retVal &= SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        if (retVal) {
          key = SearchOptions.EFF_SaveType;
          o = searchOptions.getOption(key);
          StructEntry struct = eff.getAttribute(SearchOptions.getResourceName(key), false);
          retVal &= SearchOptions.Utils.matchFlags(struct, o);
        }

        keyList = new String[]{SearchOptions.EFF_Resource1, SearchOptions.EFF_Resource2,
                               SearchOptions.EFF_Resource3};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct;
            if (SearchOptions.isResourceByOffset(key)) {
              int ofs = SearchOptions.getResourceIndex(key);
              struct = eff.getAttribute(ofs, false);
            } else {
              struct = eff.getAttribute(SearchOptions.getResourceName(key), false);
            }
            retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.EFF_Custom1, SearchOptions.EFF_Custom2,
                               SearchOptions.EFF_Custom3, SearchOptions.EFF_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(eff, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}
