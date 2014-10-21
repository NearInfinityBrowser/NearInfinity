// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import infinity.datatype.EffectType;
import infinity.datatype.TextString;
import infinity.resource.AbstractStruct;
import infinity.resource.Effect2;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;

public final class EffResource extends AbstractStruct implements Resource
{
  public EffResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    list.add(new TextString(buffer, offset + 4, 4, "Version"));
    list.add(new TextString(buffer, offset + 8, 4, "Signature 2"));
    list.add(new TextString(buffer, offset + 12, 4, "Version 2"));
    EffectType type = new EffectType(buffer, offset + 16, 4);
    list.add(type);
    offset = type.readAttributes(buffer, offset + 20, list);

    Effect2.readCommon(list, buffer, offset);

    return offset + 216;
  }


  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
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
              struct = eff.getAttribute(ofs);
            } else {
              struct = eff.getAttribute(SearchOptions.getResourceName(key));
            }
            retVal &= SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        if (retVal) {
          key = SearchOptions.EFF_SaveType;
          o = searchOptions.getOption(key);
          StructEntry struct = eff.getAttribute(SearchOptions.getResourceName(key));
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
              struct = eff.getAttribute(ofs);
            } else {
              struct = eff.getAttribute(SearchOptions.getResourceName(key));
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

