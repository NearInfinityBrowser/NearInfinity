// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.JProgressBar;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.IsTextual;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceHitFrame;

/**
 * Worker class for performing a full match against a resource entry.
 */
public class AdvancedSearchWorker implements Runnable
{
  private final List<ReferenceHitFrame.ReferenceHit> matched;
  private final AdvancedSearch.FilterMode filterOp;
  private final List<SearchOptions> searchOptions;
  private final ResourceEntry entry;
  private final JProgressBar progress;

  /**
   * @param matched List containing search results.
   * @param searchOptions List of search options.
   * @param entry Resource entry to match.
   */
  public AdvancedSearchWorker(List<ReferenceHitFrame.ReferenceHit> matched, AdvancedSearch.FilterMode filterOp,
                              List<SearchOptions> searchOptions, ResourceEntry entry, JProgressBar progress)
  {
    this.matched = matched;
    this.filterOp = (filterOp != null) ? filterOp : AdvancedSearch.FilterMode.MatchAll;
    this.searchOptions = searchOptions;
    this.entry = entry;
    this.progress = progress;
  }

  @Override
  public void run()
  {
    if (matched == null || searchOptions == null || entry == null)
      return;

    Resource res = ResourceFactory.getResource(entry);
    if (res instanceof AbstractStruct) {
      AbstractStruct structRoot = (AbstractStruct)res;
      List<ReferenceHitFrame.ReferenceHit> entryMatches = new ArrayList<>();
      Map<AbstractStruct, List<Class<? extends AbstractStruct>>> entryStructures = new HashMap<>();

      int matches = 0;
      for (int filterIdx = 0; filterIdx < searchOptions.size(); filterIdx++) {
        SearchOptions so = searchOptions.get(filterIdx);

        // list of structures to search
        List<AbstractStruct> structs = collectStructures(structRoot, so, 0);
        for (AbstractStruct struct : structs) {
          if (findMatches(entryMatches, struct, so) &&
              isGroupedMatch(entryStructures, struct, so)) {
            matches++;
            break;
          }
        }
      }

      // evaluating filter mode
      switch (filterOp) {
        case MatchAll:
          if (matches == searchOptions.size())
            matched.addAll(entryMatches);
          break;
        case MatchAny:
          if (matches > 0)
            matched.addAll(entryMatches);
          break;
        case MatchOne:
          if (matches == 1)
            matched.addAll(entryMatches);
          break;
      }
    }

    if (progress != null) {
      synchronized (progress) {
        progress.setValue(progress.getValue() + 1);
      }
    }
  }

  // Search for matching structures recursively
  private List<AbstractStruct> collectStructures(AbstractStruct struct, SearchOptions so, int index)
  {
    final List<AbstractStruct> list = new ArrayList<>();

    if (index < so.getStructure().size()) {
      // Field name may contain additional indices
      Pattern pattern;
      if (so.isStructureRegex()) {
        pattern = Pattern.compile(so.getStructure().get(index), Pattern.CASE_INSENSITIVE);
      } else {
        pattern = Pattern.compile(Pattern.quote(so.getStructure().get(index)) + "(\\s*[0-9]+)?", Pattern.CASE_INSENSITIVE);
      }
      struct
      .getFields()
      .stream()
      .filter(se -> se instanceof AbstractStruct && pattern.matcher(se.getName()).find())
      .forEachOrdered(se -> {
        // processing only matching AbstractStruct fields
        AbstractStruct as = (AbstractStruct)se;
        if (index + 1 < so.getStructure().size()) {
          // traverse more substructures?
          list.addAll(collectStructures(as, so, index + 1));
        } else {
          // leaf structure
          list.add(as);
          // search more substructures recursively?
          if (so.isStructureRecursive()) {
            as
            .getFields()
            .stream()
            .filter(se2 -> se2 instanceof AbstractStruct)
            .forEachOrdered(se2 -> list.addAll(collectStructures((AbstractStruct)se2, so, index + 1)));
          }
        }
      });
    } else {
      list.add(struct);
      if (so.isStructureRecursive()) {
        struct
        .getFields()
        .stream()
        .filter(se -> se instanceof AbstractStruct)
        .forEachOrdered(se -> list.addAll(collectStructures((AbstractStruct)se, so, index + 1)));
      }
    }

    return list;
  }

  // Return if a group match exists
  private boolean isGroupedMatch(Map<AbstractStruct, List<Class<? extends AbstractStruct>>> mapCache, AbstractStruct struct, SearchOptions so)
  {
    boolean retVal = true;
    if (so.isStructureGroup()) {
      // getting current struct chain
      List<Class<? extends AbstractStruct>> curPath = new ArrayList<>();
      for (AbstractStruct as = struct; as != null; as = as.getParent()) {
        curPath.add(0, (Class<? extends AbstractStruct>)as.getClass());
      }

      // getting mapped struct chain
      for (AbstractStruct as : mapCache.keySet()) {
        if (as.getClass().equals(struct.getClass())) {
          List<Class<? extends AbstractStruct>> existingPath = mapCache.get(as);
          // checking group
          if (curPath.equals(existingPath) && !struct.equals(as)) {
            retVal = false;
            break;
          }
        }
      }

      // adding current struct chain to map
      mapCache.put(struct, curPath);
    }
    return retVal;
  }

  // Search for matching fields in specified structure
  private boolean findMatches(List<ReferenceHitFrame.ReferenceHit> matchList, AbstractStruct struct, SearchOptions so)
  {
    if (struct != null && so != null) {
      if (so.getSearchType() == SearchOptions.FieldMode.ByName) {
        // search by name
        Pattern pattern;
        int flags = so.isSearchNameCaseSensitive() ? 0 : Pattern.CASE_INSENSITIVE;
        if (so.isSearchNameRegex()) {
          pattern = Pattern.compile(so.getSearchName(), flags);
        } else {
          pattern = Pattern.compile(Pattern.quote(so.getSearchName()), flags);
        }
        boolean result = false;
        for (final StructEntry se : struct.getFields()) {
          if (pattern.matcher(se.getName()).find()) {
            result |= isMatch(matchList, se, so);
          }
        }
        return result;
      } else {
        // search by offset (rel -> abs)
        int offset = so.getSearchOffset();
        if (so.getSearchType() == SearchOptions.FieldMode.ByRelativeOffset) {
          offset += struct.getOffset();
        }
        return isMatch(matchList, struct.getAttribute(offset), so);
      }
    }
    return false;
  }

  // Match value against search options
  private boolean isMatch(List<ReferenceHitFrame.ReferenceHit> matchList, StructEntry se, SearchOptions so)
  {
    boolean retVal = false;
    if (se != null && so != null) {
      switch (so.getValueType()) {
        case Text:
          retVal = isMatchText(se, so.getValueText(), so.isValueTextCaseSensitive(), so.isValueTextRegex());
          break;
        case Number:
          retVal = isMatchNumber(se, so.getValueNumberMin(), so.getValueNumberMax());
          break;
        case Resource:
          retVal = isMatchResource(se, so.getValueResourceRef(), so.getValueResourceType());
          break;
        case Bitfield:
          retVal = isMatchBitfield(se, so.getValueBitfield(), so.getBitfieldMode());
          break;
      }

      if (so.isInvertMatch())
          retVal = !retVal;

      if (retVal)
        matchList.add(new ReferenceHitFrame.ReferenceHit(entry, entry.getSearchString(), se));
    }
    return retVal;
  }

  // Match value textually
  private boolean isMatchText(StructEntry se, String text, boolean caseSensitive, boolean regex)
  {
    // check numeric values as well
    boolean isNumber = false;
    int number = 0;
    try {
      if (text.trim().startsWith("0x") || text.trim().startsWith("0X"))
        number = Integer.parseInt(text.trim().substring(2), 16);
      else if (text.trim().endsWith("h"))
        number = Integer.parseInt(text.trim().substring(0, text.length() - 1).trim(), 16);
      else
        number = Integer.parseInt(text.trim());
      isNumber = true;
    } catch (NumberFormatException e) {
    }

    Pattern pattern;
    int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
    if (regex) {
      pattern = Pattern.compile(text, flags);
    } else {
      pattern = Pattern.compile(Pattern.quote(text), flags);
    }

    if (se instanceof IsTextual) {
      IsTextual textEntry = (IsTextual)se;
      if (pattern.matcher(textEntry.getText()).find())
        return true;
    }

    if (se instanceof IsReference) {
      IsReference refEntry = (IsReference)se;
      String resName = refEntry.getResourceName();
      if (resName.equalsIgnoreCase("None"))
        resName = "";
      if (pattern.matcher(resName).find())
        return true;
      ResourceEntry resEntry = ResourceFactory.getResourceEntry(resName);
      if (resEntry != null) {
        String searchName = resEntry.getSearchString();
        if (searchName != null) {
          if (pattern.matcher(searchName).find())
            return true;
        }
      }
    }

    if (se instanceof IsNumeric && isNumber) {
      IsNumeric numberEntry = (IsNumeric)se;
      if (numberEntry.getValue() == number)
        return true;
    }

    // "catch all" check
    if (pattern.matcher(se.toString()).find())
      return true;

    return false;
  }

  // Match value numerically
  private boolean isMatchNumber(StructEntry se, int valueMin, int valueMax)
  {
    if (se instanceof IsNumeric) {
      IsNumeric entry = (IsNumeric)se;
      return (entry.getValue() >= valueMin && entry.getValue() <= valueMax);
    }
    return false;
  }

  // Match value by resource
  private boolean isMatchResource(StructEntry se, String resref, String ext)
  {
    if (se instanceof IsReference) {
      IsReference entry = (IsReference)se;
      String resName = entry.getResourceName();
      if (resName.equalsIgnoreCase("None"))
        resName = "";
      if (resref.isEmpty())
        return (resref.equalsIgnoreCase(resName));
      else
        return (resref + "." + ext).equalsIgnoreCase(resName);
    }
    return false;
  }

  // Match value as bitfield
  private boolean isMatchBitfield(StructEntry se, int value, SearchOptions.BitFieldMode mode)
  {
    if (se instanceof Flag) {
      Flag flag = (Flag)se;
      int bits = flag.getValue();
      switch (mode) {
        case Exact:
          return bits == value;
        case And:
          return (bits & value) == value;
        case Or:
          return (bits & value) != 0;
        case Xor:
        {
          bits = bits & value;
          int cnt = 0;
          while (bits != 0 && cnt < 2) {
            if ((bits & 1) != 0)
              cnt++;
            bits >>>= 1;
          }
          return cnt == 1;
        }
      }
    }
    return false;
  }

//-------------------------- INNER CLASSES --------------------------
}
