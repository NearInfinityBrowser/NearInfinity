// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.StringResource;

public final class Decompiler
{
  /** Indicates how to decompile script code. */
  public enum ScriptType {
    /** Treat code as full BCS resource. */
    BCS,
    /** Treat code as script trigger only. */
    TRIGGER,
    /** Treat code as script action only. */
    ACTION,
    /** Do not decompile automatically. */
    CUSTOM
  }

  private final Set<Integer> stringrefsUsed = new HashSet<Integer>();
  private final Set<ResourceEntry> resourcesUsed = new HashSet<ResourceEntry>();
  private final SortedMap<Integer, String> idsErrors = new TreeMap<Integer, String>();
  private String code;    // script byte code
  private String source;  // decompiled script source
  private ScriptType scriptType;
  private String indent = "\t";
  private boolean generateErrors;
  private int lineNr;

  public Decompiler(ResourceEntry bcsEntry, boolean generateErrors) throws Exception
  {
    this(bcsEntry, ScriptType.BCS, generateErrors);
  }

  public Decompiler(ResourceEntry bcsEntry, ScriptType type, boolean generateErrors) throws Exception
  {
    if (bcsEntry == null) {
      throw new NullPointerException();
    }
    if (BrowserMenuBar.getInstance() != null) {
      if (BrowserMenuBar.getInstance().getBcsAutoIndentEnabled()) {
        indent = BrowserMenuBar.getInstance().getBcsIndent();
      } else {
        indent = "";
      }
    }
    this.scriptType = type;
    this.generateErrors = generateErrors;
    byte[] data = bcsEntry.getResourceData();
    this.code = (data.length > 0) ? new String(data) : "";
  }

  public Decompiler(String code, boolean generateErrors)
  {
    this(code, ScriptType.BCS, generateErrors);
  }

  public Decompiler(String code, ScriptType type, boolean generateErrors)
  {
    if (BrowserMenuBar.getInstance() != null) {
      if (BrowserMenuBar.getInstance().getBcsAutoIndentEnabled()) {
        indent = BrowserMenuBar.getInstance().getBcsIndent();
      } else {
        indent = "";
      }
    }
    this.scriptType = type;
    this.generateErrors = generateErrors;
    this.code = (code != null) ? code : "";
  }

  /** Returns unprocessed BCS byte code. */
  public String getCode()
  {
    return code;
  }

  /** Set new BCS byte code to decompile. */
  public void setCode(String code)
  {
    this.code = (code != null) ? code : "";
    reset();
  }

  /** Load new BCS byte code from the specified resource entry to decompile. */
  public void setCode(ResourceEntry bcsEntry) throws Exception
  {
    if (bcsEntry == null) {
      throw new NullPointerException();
    }
    byte[] data = bcsEntry.getResourceData();
    this.code = (data.length > 0) ? new String(data) : "";
    reset();
  }

  /** Returns the decompiled script source. Executes decompile process if needed. */
  public String getSource()
  {
    if (source == null) {
      decompile();
    }
    return source;
  }

  /** Returns currently used script type. */
  public ScriptType getScriptType()
  {
    return scriptType;
  }

  /**
   * Specify new script type.
   * <b>Note:</b> Automatically invalidates previously decompiled script code.
   */
  public void setScriptType(ScriptType type)
  {
    if (type != scriptType) {
      reset();
      this.scriptType = type;
    }
  }

  /** Returns whether to generate decompile errors. */
  public boolean isGenerateErrors()
  {
    return generateErrors;
  }

  /**
   * Specify whether to generate decompile errors.
   * <b>Note:</b> Automatically invalidates previously decompiled script code.
   */
  public void setGenerateErrors(boolean flag)
  {
    if (flag != generateErrors) {
      reset();
      generateErrors = flag;
    }
  }

  /** Returns currently used string for a single level of intendation. */
  public String getIndent()
  {
    return indent;
  }

  /** Applies the indentation string defined in the currently selected item in the Options menu. */
  public void setIndent()
  {
    if (BrowserMenuBar.getInstance() != null) {
      if (BrowserMenuBar.getInstance().getBcsAutoIndentEnabled()) {
        indent = BrowserMenuBar.getInstance().getBcsIndent();
      } else {
        indent = "";
      }
    }
  }

  /** Applies the specified string for a single level of indentation. */
  public void setIndent(String newIndent)
  {
    if (newIndent != null && !indent.equals(newIndent)) {
      indent = newIndent;
    }
  }

  public SortedMap<Integer, String> getIdsErrors()
  {
    return idsErrors;
  }

  public Set<ResourceEntry> getResourcesUsed()
  {
    return resourcesUsed;
  }

  public Set<Integer> getStringRefsUsed()
  {
    return stringrefsUsed;
  }

  /**
   * Decompiles the currently loaded script code into human-readable source.
   * Uses {@link #getScriptType()} to determine the correct decompile action.
   * @return The decompiled script source.
   * @throws Exception Thrown if script type is {@code Custom}.
   */
  public String decompile()
  {
    switch (scriptType) {
      case BCS: return decompileScript();
      case TRIGGER: return decompileTrigger();
      case ACTION: return decompileAction();
      default: throw new IllegalArgumentException("Could not determine script type");
    }
  }

  /**
   * Decompiles the current script code as if defined as {@code ScriptType.BCS}.
   * @return The decompiled script source. Also available via {@link #getSource()}.
   */
  public String decompileScript()
  {
    reset();
    StringBuilder sb = new StringBuilder(code.length() * 2);
    StringTokenizer st = new StringTokenizer(code);
    while (st.hasMoreTokens()) {
      if (st.nextToken().equalsIgnoreCase("CR"))
        decompileCR(sb, st);
    }
    source = sb.toString();
    return source;
  }

  /**
   * Decompiles the current script code as if defined as {@code ScriptType.Trigger}.
   * @return The decompiled script source. Also available via {@link #getSource()}.
   */
  public String decompileTrigger()
  {
    String curIndent = indent;
    indent = "";  // ignore indentation for dialog script actions
    try {
      reset();
      StringBuilder sb = new StringBuilder(code.length() * 2);
      StringTokenizer st = new StringTokenizer(code);
      while (st.hasMoreTokens()) {
        if (st.nextToken().equalsIgnoreCase("TR"))
          sb.append(decompileTR(st));
      }
      source = sb.toString();
      return source;
    } finally {
      indent = curIndent;
    }
  }

  /**
   * Decompiles the current script code as if defined as {@code ScriptType.Action}.
   * @return The decompiled script source. Also available via {@link #getSource()}.
   */
  public String decompileAction()
  {
    String curIndent = indent;
    indent = "";  // ignore indentation for dialog script actions
    try {
      reset();
      StringBuilder sb = new StringBuilder(code.length() * 2);
      StringTokenizer st = new StringTokenizer(code);
      while (st.hasMoreTokens()) {
        if (st.nextToken().equalsIgnoreCase("AC"))
          decompileAC(sb, st);
      }
      source = sb.toString();
      return source;
    } finally {
      indent = curIndent;
    }
  }

  public static String[] getResRefType(String function)
  {
    if (function.equalsIgnoreCase("DropItem") ||
        function.equalsIgnoreCase("EquipItem") ||
        function.equalsIgnoreCase("GetItem") ||
        function.equalsIgnoreCase("GiveItem") ||
        function.equalsIgnoreCase("UseItem") ||
        function.equalsIgnoreCase("HasItem") ||
        function.equalsIgnoreCase("Contains") ||
        function.equalsIgnoreCase("NumItems") ||
        function.equalsIgnoreCase("NumItemsGT") ||
        function.equalsIgnoreCase("NumItemsLT") ||
        function.equalsIgnoreCase("NumItemsParty") ||
        function.equalsIgnoreCase("NumItemsPartyGT") ||
        function.equalsIgnoreCase("NumItemsPartyLT") ||
        function.equalsIgnoreCase("HasItemEquiped") ||
        function.equalsIgnoreCase("PartyHasItem") ||
        function.equalsIgnoreCase("PartyHasItemIdentified") ||
        function.equalsIgnoreCase("HasItemEquipedReal") ||
        function.equalsIgnoreCase("Acquired") ||
        function.equalsIgnoreCase("Unusable") ||
        function.equalsIgnoreCase("CreateItem") ||
        function.equalsIgnoreCase("GiveItemCreate") ||
        function.equalsIgnoreCase("DestroyItem") ||
        function.equalsIgnoreCase("TakePartyItemNum") ||
        function.equalsIgnoreCase("CreateItemNumGlobal") ||
        function.equalsIgnoreCase("CreateItemGlobal") ||
        function.equalsIgnoreCase("PickUpItem")) {
      return new String[] {".ITM"};
    }
    else if (function.equalsIgnoreCase("ChangeAnimation") ||
             function.equalsIgnoreCase("ChangeAnimationNoEffect") ||
             function.equalsIgnoreCase("CreateCreature") ||
             function.equalsIgnoreCase("CreateCreatureObject") ||
             function.equalsIgnoreCase("CreateCreatureImpassable") ||
             function.equalsIgnoreCase("CreateCreatureDoor") ||
             function.equalsIgnoreCase("CreateCreatureObjectDoor") ||
             function.equalsIgnoreCase("CreateCreatureObjectOffScreen") ||
             function.equalsIgnoreCase("CreateCreatureOffScreen") ||
             function.equalsIgnoreCase("CreateCreatureAtLocation") ||
             function.equalsIgnoreCase("CreateCreatureObjectCopy") ||
             function.equalsIgnoreCase("CreateCreatureObjectOffset") ||
             function.equalsIgnoreCase("CreateCreatureCopyPoint") ||
             function.equalsIgnoreCase("CreateCreatureImpassableAllowOverlap")) {
      return new String[] {".CRE"};
    }
    else if (function.equalsIgnoreCase("AreaCheck") ||
             function.equalsIgnoreCase("AreaCheckObject") ||
             function.equalsIgnoreCase("RevealAreaOnMap") ||
             function.equalsIgnoreCase("HideAreaOnMap") ||
             function.equalsIgnoreCase("CopyGroundPilesTo") ||
             function.equalsIgnoreCase("EscapeAreaObjectMove")) {
      return new String[] {".ARE"};
    }
    else if (function.equalsIgnoreCase("G") ||
             function.equalsIgnoreCase("GGT") ||
             function.equalsIgnoreCase("GLT")) {
      return new String[] {};
    }
    else if (function.equalsIgnoreCase("IncrementChapter") ||
             function.equalsIgnoreCase("TakeItemListParty") ||
             function.equalsIgnoreCase("TakeItemListPartyNum")) {
      return new String[] {".2DA"};
    }
    else if (function.equalsIgnoreCase("StartMovie")) {
      if (Profile.isEnhancedEdition()) {
        return new String[] {".WBM", ".MVE"};
      } else {
        return new String[] {".MVE"};
      }
    }
    else if (function.equalsIgnoreCase("AddSpecialAbility")) {
      return new String[] {".SPL"};
    }
    else if (function.equalsIgnoreCase("CreateVisualEffect")) {
      return new String[] {".VEF", ".VVC", ".BAM"};
    }
    return new String[] {".CRE", ".ITM", ".ARE", ".2DA", ".BCS",
                         ".MVE", ".SPL", ".DLG", ".VEF", ".VVC", ".BAM"};
  }

  private void reset()
  {
    resourcesUsed.clear();
    stringrefsUsed.clear();
    idsErrors.clear();
    lineNr = 1;
    source = null;
  }

  private void decompileAC(StringBuilder code, StringTokenizer st)
  {
    int numbers[] = new int[3];
    String objects[] = new String[2];
    String strings[] = null;
    String actionString = st.nextToken();
    int actioncode;
    if (actionString.endsWith("OB"))
      actioncode = Integer.parseInt(actionString.substring(0, actionString.length() - 2));
    else
      actioncode = Integer.parseInt(st.nextToken());
    String overrideObject = decompileOB(st);
    st.nextToken(); // OB
    objects[0] = decompileOB(st);
    st.nextToken(); // OB
    objects[1] = decompileOB(st);
    numbers[0] = Integer.parseInt(st.nextToken());
    int x = Integer.parseInt(st.nextToken());
    int y = Integer.parseInt(st.nextToken());
    numbers[1] = Integer.parseInt(st.nextToken());
    String string1 = st.nextToken();
    if (string1.endsWith("AC"))
      numbers[2] = Integer.parseInt(string1.substring(0, string1.length() - 2));
    else {
      int i = string1.indexOf((int)'"');
      if (i != -1) {
        numbers[2] = Integer.parseInt(string1.substring(0, i));
        string1 = string1.substring(i);
        while (string1.charAt(0) == '"' && string1.charAt(string1.length() - 1) != '"')
          string1 += ' ' + st.nextToken();
      }
      else {
        numbers[2] = Integer.parseInt(string1);
        string1 = st.nextToken();
        while (string1.charAt(0) == '"' && string1.charAt(string1.length() - 1) != '"')
          string1 += ' ' + st.nextToken();
      }
      String string2 = st.nextToken();
      while (string2.charAt(0) == '"' && string2.charAt(string2.length() - 1) != '"')
        string2 += ' ' + st.nextToken();
      strings = modifyStrings(string1, string2);
      st.nextToken(); // AC
    }

    IdsMapEntry action = IdsMapCache.get("ACTION.IDS").getValue((long)actioncode);
    if (action == null) {
      if (generateErrors)
        idsErrors.put(new Integer(lineNr), actioncode + " not found in ACTION.IDS");
      code.append("Error - Could not find actionString ").append(actioncode).append('\n');
      lineNr++;
      return;
    }
    StringTokenizer defParam = new StringTokenizer(action.getParameters(), ",");

    IdsMapEntry action2 = IdsMapCache.get("ACTION.IDS").getOverflowValue((long)actioncode);
    if (action2 != null) {
      if (useOverflowCommand(defParam, numbers, x, y, objects, strings))
        action = action2;
      defParam = new StringTokenizer(action.getParameters(), ",");
    }

    if (strings != null) {
      int count_s = 0;
      while (defParam.hasMoreTokens()) {
        String p = defParam.nextToken();
        if (p.substring(0, 2).equals("S:"))
          count_s++;
      }
      if (count_s > 0 && count_s < 4 && strings[count_s] != null && !strings[count_s].equals("\"\""))
        strings[count_s - 1] =
        strings[count_s].substring(0, strings[count_s].length() - 1) + strings[count_s - 1].substring(1);

      defParam = new StringTokenizer(action.getParameters(), ",");
    }

    String comment = null;
    if (overrideObject.equals("[ANYONE]"))
      code.append(action.getString());
    else
      code.append("ActionOverride(").append(overrideObject).append(',').append(action.getString());

    int index_i = 0, index_o = 0, index_s = 0;
    boolean first = true;
    while (defParam.hasMoreTokens()) {
      if (!first)
        code.append(',');
      String p = defParam.nextToken();
      if (p.substring(0, 2).equals("S:")) {
        String newp;
        if (strings == null)
          newp = objects[index_o++];
        else
          newp = strings[index_s++];
        code.append(newp);
        if (p.equalsIgnoreCase("S:Spells*")) {
          String spellNumbers = newp.substring(1, newp.length() - 1);
          int index = 4;
          IdsMap map = IdsMapCache.get("SPELL.IDS");
          while (index <= spellNumbers.length()) {
            long spellNumber = Long.parseLong(spellNumbers.substring(index - 4, index));
            IdsMapEntry entry = map.getValue(spellNumber);
            if (comment == null)
              comment = entry.toString();
            else
              comment += ", " + entry.toString();
            index += 4;
          }
        }
        else if (newp != null) {
          String function = action.getString().substring(0, action.getString().length() - 1);
          comment = getResourceName(function, p, newp.substring(1, newp.length() - 1));
        }
      }
      else if (p.substring(0, 2).equals("O:")) {
        while (objects[index_o++].equals("[ANYONE]") && index_o < 2)
          ;
        code.append(objects[index_o - 1]);
      }
      else if (p.substring(0, 2).equals("I:")) {
        int nr = numbers[index_i++];
        decompileInteger(code, (long)nr, p);
        if ((p.length() >= 8 && p.substring(0, 8).equalsIgnoreCase("I:StrRef")) ||
            (p.length() >= 7 && p.substring(0, 7).equalsIgnoreCase("I:Entry"))) {
          comment = StringResource.getStringRef(nr);
          if (generateErrors)
            stringrefsUsed.add(new Integer(nr));
        } else {
          StringBuilder sb = new StringBuilder();
          decompileInteger(sb, (long)nr, p);
          String s = getResourceFileName(p, sb.toString());
          if (s != null) {
            comment = s;
          }
        }
      }
      else if (p.substring(0, 2).equals("P:"))
        code.append('[').append(x).append('.').append(y).append(']').toString();
      first = false;
    }

    if (!overrideObject.equals("[ANYONE]"))
      code.append("))");
    else
      code.append(')');
    if (comment != null)
      code.append(" // ").append(comment.replace('\n', ' '));
    code.append('\n');
    lineNr++;
  }

  private void decompileCO(StringBuilder code, StringTokenizer st)
  {
    code.append("IF\n");
    lineNr++;
    String token = st.nextToken();
    int orcount = 0;
    while (!token.equalsIgnoreCase("CO")) {
      if (token.equalsIgnoreCase("TR")) {
        String trigger = decompileTR(st);
        if (orcount > 0) {
          // NextTriggerObject doesn't count as separate trigger
          if (!trigger.startsWith("NextTriggerObject")) {
            orcount--;
          }
          code.append(indent);
        }
        else if (trigger.substring(0, 3).equalsIgnoreCase("OR(")) {
          orcount = Integer.parseInt(trigger.substring(3, trigger.indexOf(")")));
        }
        code.append(indent).append(trigger);
      }
      token = st.nextToken();
    }
  }

  private void decompileCR(StringBuilder code, StringTokenizer st)
  {
    String token = st.nextToken();
    while (st.hasMoreTokens() && !token.equalsIgnoreCase("CR")) {
      if (token.equalsIgnoreCase("CO"))
        decompileCO(code, st);
      else if (token.equalsIgnoreCase("RS"))
        decompileRS(code, st);
      if (st.hasMoreTokens())
        token = st.nextToken();
    }
    code.append("END\n\n");
    lineNr += 2;
  }

  private void decompileInteger(StringBuilder code, long nr, String p)
  {
    int pIndex = p.indexOf((int)'*');
    if (pIndex != -1 && pIndex != p.length() - 1) {
//      if (nr < 0)
//        nr += 4294967296L;
      String idsFile = p.substring(pIndex + 1).toUpperCase(Locale.ENGLISH) + ".IDS";
      IdsMap map = IdsMapCache.get(idsFile);
      if (map != null) {
        IdsMapEntry entry = map.getValue(nr);
        if (entry != null) {
          code.append(entry.getString());
        }
        else if (nr != 0 && (map.toString().equalsIgnoreCase("AREATYPE.IDS") ||
                             map.toString().equalsIgnoreCase("BITS.IDS") ||
                             map.toString().equalsIgnoreCase("SPLCAST.IDS") ||
                             map.toString().equalsIgnoreCase("STATE.IDS"))) {
          if (nr < 0) {
            nr += 4294967296L;
          }
          StringBuilder temp = new StringBuilder();
          for (int bit = 0; nr > 0 && bit < 32; bit++) {
            long bitnr = (long)Math.pow((double)2, (double)bit);
            if ((nr & bitnr) == bitnr) {
              entry = map.getValue(bitnr);
              if (entry != null) {
                if (temp.length() > 0) {
                  temp.append(" | ");
                }
                temp.append(entry.getString());
                nr ^= bitnr;
              }
            }
          }
          if (nr > 0) {
            code.append(nr);
            if (generateErrors) {
              idsErrors.put(new Integer(lineNr), nr + " not found in " + map.toString());
            }
          }
          else {
            code.append(temp);
          }
        }
        else {
          code.append(nr);
          if (generateErrors)
            idsErrors.put(new Integer(lineNr), nr + " not found in " + map.toString());
        }
      }
      else {
        code.append(nr);
        if (generateErrors) {
          idsErrors.put(new Integer(lineNr), "Could not find " + idsFile);
        }
      }
    }
    else {
      code.append(nr);
    }
  }

  private String decompileOB(StringTokenizer st)
  {
    int numbers[] = new int[15];
    int numbersIndex = 0;
    String value = st.nextToken();
    while (value.charAt(0) != '"' && value.charAt(0) != '[') {
      numbers[numbersIndex++] = Integer.parseInt(value);
      value = st.nextToken();
    }
    while (value.charAt(0) == '"' && !(value.endsWith("\"") || value.endsWith("OB")))
      value = value + ' ' + st.nextToken();

    String coord = null;
    String name = value;
    if (name.charAt(0) == '[') { // Object Coordinate
      StringTokenizer coordst = new StringTokenizer(name.substring(1, name.length() - 1), ".");
      while (coordst.hasMoreTokens())
        if (!coordst.nextToken().equals("-1")) {
          coord = name;
          break;
        }
      name = st.nextToken(); // ToDo: IWD can't handle spaces in objstring
      // this opens a fat can of stupid with Icewind2 (below); IWD2 still has
      // problems decompiling from "dirty" BCS source (in source view)
    }
    if (name.endsWith("OB"))
      name = name.substring(0, name.length() - 2);
    else {
      value = st.nextToken();
      if (!value.equalsIgnoreCase("OB")) { // Icewind2
        numbers[numbersIndex++] = Integer.parseInt(value);
        numbers[numbersIndex++] = Integer.parseInt(st.nextToken());
        st.nextToken(); // OB
      }
    }

    String ids[] = new String[numbersIndex - 5];
    int index = 0;
    ids[index] = lookup(IdsMapCache.get("EA.IDS"), numbers[index++]);
    if (numbersIndex == 14) {
      ids[index] = lookup(IdsMapCache.get("FACTION.IDS"), numbers[index++]);
      ids[index] = lookup(IdsMapCache.get("TEAM.IDS"), numbers[index++]);
    }
    ids[index] = lookup(IdsMapCache.get("GENERAL.IDS"), numbers[index++]);
    ids[index] = lookup(IdsMapCache.get("RACE.IDS"), numbers[index++]);
    ids[index] = lookup(IdsMapCache.get("CLASS.IDS"), numbers[index++]);
    ids[index] = lookup(IdsMapCache.get("SPECIFIC.IDS"), numbers[index++]);
    ids[index] = lookup(IdsMapCache.get("GENDER.IDS"), numbers[index++]);
    if (numbersIndex == 15) {
      ids[index] = lookup(IdsMapCache.get("ALIGNMNT.IDS"), numbers[index++]);
      ids[index] = lookup(IdsMapCache.get("SUBRACE.IDS"), numbers[index++]);
    }
    else {
      ids[index] = lookup(IdsMapCache.get("ALIGN.IDS"), numbers[index++]);
    }

    IdsMap objectMap = IdsMapCache.get("OBJECT.IDS");
    String obj[] = {
      lookup(objectMap, numbers[index++]),
      lookup(objectMap, numbers[index++]),
      lookup(objectMap, numbers[index++]),
      lookup(objectMap, numbers[index++]),
      lookup(objectMap, numbers[index++])};

    if (numbersIndex == 15) {
      ids[index - 5] = lookup(IdsMapCache.get("CLASS.IDS"), numbers[index++]);
      ids[index - 5] = lookup(IdsMapCache.get("CLASSMSK.IDS"), numbers[index++]);
    }

    StringBuilder code = new StringBuilder();
    StringBuilder endcode = new StringBuilder();
    for (int i = 4; i > 0; i--) {
      if (obj[i] != null) {
        code.append(obj[i]).append('(');
        endcode.append(')');
      }
    }
    if (obj[0] != null)
      code.append(obj[0]);

    if (!name.trim().equals("\"\"")) {
      if (code.length() == 0)
        code.append(name);
      else {
        code.append('(').append(name);
        endcode.append(')');
      }
    }
    else {
      int maxids = ids.length - 1;
      while (maxids >= 0 && ids[maxids] == null)
        maxids--;
      if (maxids == -1 && code.length() == 0)
        code.append("[ANYONE]");
      else if (maxids >= 0) {
        if (code.length() > 0) {
          code.append('(');
          endcode.append(')');
        }
        code.append('[');
        if (ids[0] == null)
          code.append('0');
        else
          code.append(ids[0]);
        for (int i = 1; i <= maxids; i++) {
          if (ids[i] == null)
            code.append(".0");
          else
            code.append('.').append(ids[i]);
        }
        code.append(']');
      }
      if (coord != null)
        code.append(coord);
    }
    return code.append(endcode).toString();
  }

  private void decompileRE(StringBuilder code, StringTokenizer st)
  {
    String token = st.nextToken();
    int i = token.indexOf("AC");
    if (i != -1) {
      code.append(indent).append("RESPONSE #").append(token.substring(0, i)).append('\n');
      lineNr++;
      token = token.substring(i);
    }
    else if (token.indexOf("RE") != -1) {
      code.append(indent).append("RESPONSE #").append(token.substring(0, token.indexOf("RE"))).append('\n');
      lineNr++;
      return;
    }
    else {
      code.append(indent).append("RESPONSE #").append(token).append('\n');
      lineNr++;
      token = st.nextToken();
    }
    while (token.equalsIgnoreCase("AC")) {
      code.append(indent).append(indent);
      decompileAC(code, st);
      token = st.nextToken();
    }
  }

  private void decompileRS(StringBuilder code, StringTokenizer st)
  {
    code.append("THEN\n");
    lineNr++;
    String token = st.nextToken();
    while (st.hasMoreTokens() && !token.equalsIgnoreCase("RS")) {
      if (token.equalsIgnoreCase("RE"))
        decompileRE(code, st);
      token = st.nextToken();
    }
  }

  private static ResourceEntry decompileStringCheck(String value, String[] fileTypes)
  {
    for (final String fileType : fileTypes) {
      if (ResourceFactory.resourceExists(value + fileType, true)) {
        return ResourceFactory.getResourceEntry(value + fileType, true);
      }
    }
    return null;
  }

  private String decompileTR(StringTokenizer st)
  {
    int triggercode = Integer.parseInt(st.nextToken());
    IdsMapEntry trigger = IdsMapCache.get("TRIGGER.IDS").getValue((long)triggercode);

    if (trigger == null) {
      trigger = IdsMapCache.get("TRIGGER.IDS").getValue((long)(0x4000 + triggercode));
      triggercode += 0x4000;
    }
    if (trigger == null) {
      while (!st.nextToken().equals("TR"))
        ;
      if (generateErrors)
        idsErrors.put(new Integer(lineNr), triggercode - 0x4000 + " not found in TRIGGER.IDS");
      lineNr++;
      return "Error - Could not find trigger " + (triggercode - 0x4000) + '\n';
    }

    String object, coord = null;
    int numbers[] = new int[3];
    String strings[] = null;

    StringBuilder code = new StringBuilder();
    String comment = null;
    String token = st.nextToken();
    if (token.endsWith("OB")) {
      numbers[0] = Integer.parseInt(token.substring(0, token.length() - 2));
      object = decompileOB(st);
      st.nextToken(); // TR
    }
    else {
      numbers[0] = Integer.parseInt(token);
      if ((Integer.parseInt(st.nextToken()) & 1) == 1) // Not flag
        code.append('!');
      numbers[1] = Integer.parseInt(st.nextToken());
      numbers[2] = Integer.parseInt(st.nextToken());
      String string1 = st.nextToken();
      if (string1.charAt(0) == '[') {
        coord = string1;
        string1 = st.nextToken();
      }
      while (string1.charAt(0) == '"' && string1.charAt(string1.length() - 1) != '"')
        string1 += ' ' + st.nextToken();
      String string2 = st.nextToken();
      while (string2.charAt(0) == '"' && string2.charAt(string2.length() - 1) != '"')
        string2 += ' ' + st.nextToken();
      st.nextToken(); // OB
      object = decompileOB(st);
      st.nextToken(); // TR

      strings = modifyStrings(string1, string2);
    }

    StringTokenizer defParam = new StringTokenizer(trigger.getParameters(), ",");

    IdsMapEntry trigger2 = IdsMapCache.get("TRIGGER.IDS").getOverflowValue((long)triggercode);
    if (trigger2 != null) {
      if (useOverflowCommand(defParam, numbers, 0, 0, new String[]{object}, strings))
        trigger = trigger2;
      defParam = new StringTokenizer(trigger.getParameters(), ",");
    }
    code.append(trigger.getString());

    int index_i = 0, index_s = 0;
    boolean first = true;
    while (defParam.hasMoreTokens()) {
      if (!first)
        code.append(',');
      String p = defParam.nextToken();
      if (p.substring(0, 2).equals("S:")) {
        String newp = strings[index_s++];
        String function = trigger.getString().substring(0, trigger.getString().length() - 1);
        comment = getResourceName(function, p, newp.substring(1, newp.length() - 1));
        code.append(newp);
      }
      else if (p.substring(0, 2).equals("O:"))
        code.append(object);
      else if (p.substring(0, 2).equals("P:"))
        code.append(coord.replaceFirst(",", "."));   // for WeiDU compatability
      else if (p.substring(0, 2).equals("I:")) {
        int nr = numbers[index_i++];
        decompileInteger(code, (long)nr, p);
        StringBuilder sb = new StringBuilder();
        decompileInteger(sb, (long)nr, p);
        comment = getResourceFileName(p, sb.toString());
      }
      first = false;
    }

    lineNr++;
    if (comment != null) {
      return code.append(") // ").append(comment.replace('\n', ' ')).append('\n').toString();
    } else {
      return code.append(")\n").toString();
    }
  }

  private String getResourceName(String function, String definition, String value)
  {
    if (definition.startsWith("S:") && value.length() > 8)
      return null;
    ResourceEntry entry = null;
    if (definition.equalsIgnoreCase("S:DialogFile*"))
      entry = decompileStringCheck(value, new String[]{".DLG", ".VEF", ".VVC", ".BAM"});
    else if (definition.equalsIgnoreCase("S:CutScene*") || definition.equalsIgnoreCase("S:ScriptFile*")
             || definition.equalsIgnoreCase("S:Script*"))
      entry = decompileStringCheck(value, new String[]{".BCS"});
    else if (definition.equalsIgnoreCase("S:Item*") || definition.equalsIgnoreCase("S:Take*")
             || definition.equalsIgnoreCase("S:Give*") || definition.equalsIgnoreCase("S:OldObject*"))
      entry = decompileStringCheck(value, new String[]{".ITM"});
    else if (definition.equalsIgnoreCase("S:Sound*") || definition.equalsIgnoreCase("S:Voice*"))
      entry = decompileStringCheck(value, new String[]{".WAV"});
    else if (definition.equalsIgnoreCase("S:TextList*"))
      entry = decompileStringCheck(value, new String[]{".2DA"});
    else if (definition.equalsIgnoreCase("S:Effect*"))
      entry = decompileStringCheck(value, new String[]{".VEF", ".VVC", ".BAM"});
    else if (definition.equalsIgnoreCase("S:Parchment*"))
      entry = decompileStringCheck(value, new String[]{".MOS"});
    else if (definition.equalsIgnoreCase("S:Spell*") || definition.equalsIgnoreCase("S:Res*"))
      entry = decompileStringCheck(value, new String[]{".SPL"});
    else if (definition.equalsIgnoreCase("S:Store*"))
      entry = decompileStringCheck(value, new String[]{".STO"});
    else if (definition.equalsIgnoreCase("S:ToArea*") || definition.equalsIgnoreCase("S:Areaname*")
             || definition.equalsIgnoreCase("S:FromArea*") || definition.equalsIgnoreCase("S:Area*")
             || definition.equalsIgnoreCase("S:Area1*") || definition.equalsIgnoreCase("S:Area2*"))
      entry = decompileStringCheck(value, new String[]{".ARE"});
    else if (definition.equalsIgnoreCase("S:BamResRef*"))
      entry = decompileStringCheck(value, new String[]{".BAM"});
    else if (definition.equalsIgnoreCase("S:Pool*"))
      entry = decompileStringCheck(value, new String[]{".SRC"});
    else if (definition.equalsIgnoreCase("S:Palette*"))
      entry = decompileStringCheck(value, new String[]{".BMP"});
    else if (definition.equalsIgnoreCase("S:ResRef*")) {
      entry = decompileStringCheck(value, getResRefType(function));
    }
    else if (definition.equalsIgnoreCase("S:Object*")) {
      entry = decompileStringCheck(value, getResRefType(function));
    }
    else if (definition.equalsIgnoreCase("S:NewObject*")) {
      entry = decompileStringCheck(value, getResRefType(function));
    }
    else if (definition.equalsIgnoreCase("I:Spell*Spell")) {
      String refValue = org.infinity.resource.spl.Viewer.getResourceName(value, false);
      if (refValue != null) {
        entry = decompileStringCheck(refValue, new String[]{".SPL"});
      }
    }
//    else
//      System.out.println("Decompiler.getResourceName: " + definition + " - " + value);
    if (entry != null) {
      if (generateErrors) {
        resourcesUsed.add(entry);
      }
      return entry.getSearchString();
    }
    return null;
  }

  private String getResourceFileName(String definition, String value)
  {
    if (!definition.startsWith("I:")) {
      return null;
    }
    ResourceEntry entry = null;
    if (definition.equalsIgnoreCase("I:Spell*Spell")) {
      String refName = org.infinity.resource.spl.Viewer.getResourceName(value, false);
      if (refName != null) {
        entry = decompileStringCheck(refName, new String[]{".SPL"});
      }
    }

    String retVal = null;
    if (entry != null) {
      if (generateErrors) {
        resourcesUsed.add(entry);
      }
      retVal = String.format("%1$s (%2$s)", entry.getResourceName(), entry.getSearchString());
    }
    return retVal;
  }

  private String lookup(IdsMap idsmap, int code)
  {
    if (code == 0) return null;
    IdsMapEntry entry = idsmap.getValue((long)code);
    if (entry != null)
      return entry.getString();
    else {
      if (generateErrors)
        idsErrors.put(new Integer(lineNr), code + " not found in " + idsmap.toString());
      return String.valueOf(code);
    }
  }

  private static String[] modifyStrings(String string1, String string2)
  {
    String newStrings[] = new String[4];
    int index = 0;

    if (string1.length() > 9
        && (Compiler.isPossibleNamespace(string1.substring(0, 7) + '\"')
//        && (!string1.substring(0, 3).equalsIgnoreCase("\"AR")
            || ResourceFactory.resourceExists(string1.substring(1, 7) + ".ARE"))) {
      newStrings[index++] = '\"' + string1.substring(7);
      newStrings[index++] = string1.substring(0, 7) + '\"';
    }
    else
      newStrings[index++] = string1;

    if (string2.length() > 9
        && (Compiler.isPossibleNamespace(string2.substring(0, 7) + '\"')
//        && (!string2.substring(0, 3).equalsIgnoreCase("\"AR")
            || ResourceFactory.resourceExists(string2.substring(1, 7) + ".ARE"))) {
      newStrings[index++] = '\"' + string2.substring(7);
      newStrings[index++] = string2.substring(0, 7) + '\"';
    }
    else {
      String[] splitted = splitString(string2);
      for (int i = 0; i < splitted.length; i++) {
        newStrings[index++] = splitted[i];
      }
    }

    return newStrings;
  }

  private static String[] splitString(String string)
  {
    String[] values = string.split(":");
    String[] retVal = new String[values.length];
    for (int i = 0; i < values.length; i++) {
      StringBuilder sb = new StringBuilder(values[i].length() + 2);
      if (values[i].length() == 0 || values[i].charAt(0) != '"') {
        sb.append('"');
      }
      sb.append(values[i]);
      if (values[i].length() < 2 || values[i].charAt(values[i].length() - 1) != '"') {
        sb.append('"');
      }
      retVal[i] = sb.toString();
    }

    return retVal;
  }

  private static boolean useOverflowCommand(StringTokenizer defParam, int numbers[], int x, int y,
                                            String objects[], String strings[])
  {
    // Count definition parameters
    int i_c1 = 0, p_c1 = 0, o_c1 = 0, s_c1 = 0;
    while (defParam.hasMoreTokens()) {
      String param = defParam.nextToken().substring(0, 2);
      if (param.equals("I:"))
        i_c1++;
      else if (param.equals("P:"))
        p_c1++;
      else if (param.equals("O:"))
        o_c1++;
      else if (param.equals("S:"))
        s_c1++;
    }

    // Count supplied parameters
    int i_count = 0, p_count = 0, o_count = 0, s_count = 0;
    for (final int number : numbers)
      if (number != 0)
        i_count++;
    if (x != 0 || y != 0)
      p_count = 1;
    for (final String object : objects)
      if (!object.equals("[ANYONE]"))
        o_count++;
    for (final String string : strings)
      if (string != null && !string.equals("\"\""))
        s_count++;

    // Decide...
    if (i_count > i_c1 || p_count > p_c1 || o_count > o_c1 || s_count > s_c1)
      return true;
    return false; // Don't use overflow action/trigger
  }
}

