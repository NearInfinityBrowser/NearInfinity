// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.bcs;

import infinity.NearInfinity;
import infinity.gui.StatusBar;
import infinity.gui.BrowserMenuBar;
import infinity.resource.ResourceFactory;
import infinity.resource.are.AreResource;
import infinity.resource.bcs.Decompiler;
import infinity.resource.cre.CreResource;
import infinity.resource.key.ResourceEntry;
import infinity.util.IdsMap;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.SwingWorker;

public final class Compiler
{
  private static Compiler compiler;
  private final IdsMap[] itype;
  private final Map<String, Set<ResourceEntry>> scriptNamesCre =
    new HashMap<String, Set<ResourceEntry>>();
  private final Set<String> scriptNamesAre = new HashSet<String>();
  private final SortedMap<Integer, String> errors = new TreeMap<Integer, String>();
  private final SortedMap<Integer, String> warnings = new TreeMap<Integer, String>();
  private final String emptyObject;
  private int linenr;
  private boolean scriptNamesValid = false;

  public static Compiler getInstance()
  {
    if (compiler == null)
      compiler = new Compiler();
    return compiler;
  }

  static boolean isPossibleNamespace(String string)
  {
    if (string.equalsIgnoreCase("\"GLOBAL\"") ||
        string.equalsIgnoreCase("\"LOCALS\"") ||
        string.equalsIgnoreCase("\"MYAREA\"") ||
        string.equalsIgnoreCase("\"KAPUTZ\"") || // PS:T
        string.length() == 8 &&
        (string.substring(0, 3).equalsIgnoreCase("\"AR") && Character.isDigit(string.charAt(3)) &&
         Character.isDigit(string.charAt(4)) &&
         Character.isDigit(string.charAt(5)) &&
         Character.isDigit(string.charAt(6)) ||
         ResourceFactory.getInstance().resourceExists(string.substring(1, 7) + ".ARE")))
      return true;
    return false;
  }

  public static void restartCompiler()
  {
    compiler = null;
  }

  private Compiler()
  {
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      itype = new IdsMap[]{
        IdsMapCache.get("EA.IDS"),
        IdsMapCache.get("FACTION.IDS"),
        IdsMapCache.get("TEAM.IDS"),
        IdsMapCache.get("GENERAL.IDS"),
        IdsMapCache.get("RACE.IDS"),
        IdsMapCache.get("CLASS.IDS"),
        IdsMapCache.get("SPECIFIC.IDS"),
        IdsMapCache.get("GENDER.IDS"),
        IdsMapCache.get("ALIGN.IDS")
      };
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
      itype = new IdsMap[]{
        IdsMapCache.get("EA.IDS"),
        IdsMapCache.get("GENERAL.IDS"),
        IdsMapCache.get("RACE.IDS"),
        IdsMapCache.get("CLASS.IDS"),
        IdsMapCache.get("SPECIFIC.IDS"),
        IdsMapCache.get("GENDER.IDS"),
        IdsMapCache.get("ALIGNMNT.IDS"),
        IdsMapCache.get("SUBRACE.IDS"),
        IdsMapCache.get("CLASS.IDS"),
        IdsMapCache.get("CLASSMSK.IDS")
      };
    else
      itype = new IdsMap[]{
        IdsMapCache.get("EA.IDS"),
        IdsMapCache.get("GENERAL.IDS"),
        IdsMapCache.get("RACE.IDS"),
        IdsMapCache.get("CLASS.IDS"),
        IdsMapCache.get("SPECIFIC.IDS"),
        IdsMapCache.get("GENDER.IDS"),
        IdsMapCache.get("ALIGN.IDS")
      };
    emptyObject = compileObject(null, "");

    if (BrowserMenuBar.getInstance().checkScriptNames())
        setupScriptNames();
  }

  private void setupScriptNames()
  {
    final StatusBar statusBar = NearInfinity.getInstance().getStatusBar();
    final String oldMessage = statusBar.getMessage();
    final String notification = "Gathering creature and area names ...";

    // This can take some time, so its moved into a background job
    SwingWorker<Object, Object> task = new SwingWorker<Object, Object>() {
      protected Object doInBackground() {
        scriptNamesCre.clear();
        scriptNamesAre.clear();

        List<ResourceEntry> files = ResourceFactory.getInstance().getResources("CRE");
        for (int i = 0; i < files.size(); i++) {
          ResourceEntry resourceEntry = files.get(i);
          try {
            CreResource.addScriptName(scriptNamesCre, resourceEntry);
          }
          catch (Exception e) {}

        }

        files = ResourceFactory.getInstance().getResources("ARE");
        for (int i = 0; i < files.size(); i++) {
          ResourceEntry resourceEntry = files.get(i);
          try {
            AreResource.addScriptNames(scriptNamesAre, resourceEntry.getResourceData());
          }
          catch (Exception e) {}
        }

        return null;
      }

      protected void done() {
        if (statusBar.getMessage().startsWith(notification)) {
          statusBar.setMessage(oldMessage.trim());
        }
        scriptNamesValid = true;
      }
    };

    statusBar.setMessage(notification);
    task.execute();
  }

  public boolean hasValidScriptNames() {
    return scriptNamesValid;
  }

  public boolean hasScriptName(String scriptName) {
    if (scriptNamesValid &&
        scriptNamesCre.containsKey(scriptName.toLowerCase().replaceAll(" ", ""))) {
      return true;
    }
    return false;
  }

  public Set<ResourceEntry> getResForScriptName(String scriptName) {
    return scriptNamesCre.get(scriptName.toLowerCase().replaceAll(" ", ""));
  }

  public String compile(String source)
  {
    StringBuilder code = new StringBuilder("SC\n");
    StringTokenizer st = new StringTokenizer(source, "\n", true);
    linenr = 0;
    errors.clear();
    warnings.clear();

    String line = null;
    if (st.hasMoreTokens())
      line = getNextLine(st);
    while (st.hasMoreTokens()) {
      if (line == null || !line.equalsIgnoreCase("IF")) {
        String error = "Missing IF";
        errors.put(new Integer(linenr), error);
        return "Error - " + error;
      }

      code.append("CR\n");
      compileCondition(code, st);
      compileResponseSet(code, st);
      code.append("CR\n");

      line = getNextLine(st);
      while (line.length() == 0 && st.hasMoreTokens())
        line = getNextLine(st);
    }
    code.append("SC\n");
    return code.toString();
  }

  public String compileDialogCode(String source, boolean isAction)
  {
//    source = source.replaceAll(" ", ""); // ToDo: Replace with something better
    StringBuilder code = new StringBuilder();
    linenr = 0;
    errors.clear();
    warnings.clear();
    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
        ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
        ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT ||
        ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ||
        ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
      StringTokenizer st = new StringTokenizer(source, ")");
      while (st.hasMoreTokens()) {
        String line = st.nextToken().trim() + ')';
        linenr++;
        int index = line.indexOf("//");
        if (index != -1)
          line = line.substring(0, index);
        if (line.length() > 0 && !line.equals(")")) {
          if (isAction)
            compileAction(code, line);
          else
            compileTrigger(code, line);
        }
      }
    }
    else {
      StringTokenizer st = new StringTokenizer(source + "\n\n", "\n", true);
      String line = null;
      if (st.hasMoreTokens())
        line = getNextLine(st);
      while (st.hasMoreTokens()) {
        if (isAction)
          compileAction(code, line);
        else
          compileTrigger(code, line);
        line = getNextLine(st);
        while (line.length() == 0 && st.hasMoreTokens())
          line = getNextLine(st);
      }
    }
    return code.toString();
  }

  public SortedMap<Integer, String> getErrors()
  {
    return errors;
  }

  public SortedMap<Integer, String> getWarnings()
  {
    return warnings;
  }

  private void checkObjectString(String definition, String value)
  {
    String name = value.substring(1, value.length() - 1).toLowerCase().replaceAll(" ", "");
    if (scriptNamesValid) {
        if (name.equals("") || !(scriptNamesCre.containsKey(name) || scriptNamesAre.contains(name)))
          warnings.put(new Integer(linenr), "Script name not found: " + definition + " - " + value);
//        else {
//          System.out.println(definition + " - " + value + " OK");
//        }
    }
  }

  private void checkString(String function, String definition, String value)
  {
//    if (!definition.endsWith("*"))
//      System.out.println("Compiler.checkString: " + function + " " + definition + " " + value);
    if (value.equals("\"\"")) // ToDo: "" due to IWD2 decompiler bug?
      return;
    if (value.substring(1, value.length() - 1).length() > 32)
      warnings.put(new Integer(linenr), "Invalid string length: " + definition + " - " + value);
    else if (definition.equalsIgnoreCase("S:Area*") ||
             definition.equalsIgnoreCase("S:Area1*") ||
             definition.equalsIgnoreCase("S:Area2*")) {
      if (!isPossibleNamespace(value)) {
        String error = "Invalid area string: " + definition + " - " + value;
        errors.put(new Integer(linenr), error);
      }
    }
    else if (definition.equalsIgnoreCase("S:Name*")) { // ToDo: need CalledByName()?
      if (scriptNamesValid) {
        if (function.equalsIgnoreCase("Dead(") ||
            function.equalsIgnoreCase("IsScriptName(") ||
            function.equalsIgnoreCase("Name(") ||
            function.equalsIgnoreCase("NumDead(") ||
            function.equalsIgnoreCase("NumDeadGT(") ||
            function.equalsIgnoreCase("NumDeadLT(")) {
          if (!scriptNamesCre.containsKey(value.substring(1, value.length() - 1).toLowerCase().replaceAll(" ", "")) &&
              IdsMapCache.get("OBJECT.IDS").lookup(value) == null)
            warnings.put(new Integer(linenr), "Script name not found: " + definition + " - " + value);
        }
        else if (function.equalsIgnoreCase("SetCorpseEnabled(")) {
          if (!scriptNamesAre.contains(value.substring(1, value.length() - 1).toLowerCase().replaceAll(" ", "")) &&
              IdsMapCache.get("OBJECT.IDS").lookup(value) == null)
            warnings.put(new Integer(linenr), "Script name not found: " + definition + " - " + value);
        }
      }
    }
    else if (function.equalsIgnoreCase("AttachTransitionToDoor(") && scriptNamesValid) {
        if (!scriptNamesAre.contains(value.substring(1, value.length() - 1).toLowerCase().replaceAll(" ", "")) &&
            IdsMapCache.get("OBJECT.IDS").lookup(value) == null)
          warnings.put(new Integer(linenr), "Script name not found: " + definition + " - " + value);
    }
//    else if (definition.equalsIgnoreCase("S:Name*") || definition.equalsIgnoreCase("S:Column*")
//             || definition.equalsIgnoreCase("S:Entry*") || definition.equalsIgnoreCase("S:Global*")
//             || definition.equalsIgnoreCase("S:Name1*") || definition.equalsIgnoreCase("S:Name2*")
//             || definition.equalsIgnoreCase("S:Message*") || definition.equalsIgnoreCase("S:String1*")
//             || definition.equalsIgnoreCase("S:String2*") || definition.equalsIgnoreCase("S:String*")
//             || definition.equalsIgnoreCase("S:String3*") || definition.equalsIgnoreCase("S:String4*")
//             || definition.equalsIgnoreCase("S:VarTableEntry*") || definition.equalsIgnoreCase("S:Spells*")
//             || definition.equalsIgnoreCase("S:ScriptName*") || definition.equalsIgnoreCase("S:Sound1*")) {
//      // Not a resource
//    }
    else {                                                          // Resource checks
      String resourceTypes[] = new String[0];
      if (definition.equalsIgnoreCase("S:DialogFile*"))
        resourceTypes = new String[] {".DLG", ".VVC"};
      else if (definition.equalsIgnoreCase("S:CutScene*") ||
               definition.equalsIgnoreCase("S:ScriptFile*") ||
               definition.equalsIgnoreCase("S:Script*"))
        resourceTypes = new String[]{".BCS"};
      else if (definition.equalsIgnoreCase("S:Item*") ||
               definition.equalsIgnoreCase("S:Take*") ||
               definition.equalsIgnoreCase("S:Give*") ||
               definition.equalsIgnoreCase("S:Item") ||
               definition.equalsIgnoreCase("S:OldObject*"))
        resourceTypes = new String[]{".ITM"};
      else if (definition.equalsIgnoreCase("S:Sound*") ||
               definition.equalsIgnoreCase("S:Voice*"))
        resourceTypes = new String[]{".WAV"};
      else if (definition.equalsIgnoreCase("S:TextList*"))
        resourceTypes = new String[]{".2DA"};
      else if (definition.equalsIgnoreCase("S:Effect*"))
        resourceTypes = new String[]{".BAM", ".VVC"};
      else if (definition.equalsIgnoreCase("S:Parchment*"))
        resourceTypes = new String[]{".MOS"};
      else if (definition.equalsIgnoreCase("S:Spell*") ||
               definition.equalsIgnoreCase("S:Res*"))
        resourceTypes = new String[]{".SPL"};
      else if (definition.equalsIgnoreCase("S:Store*"))
        resourceTypes = new String[]{".STO"};
      else if (definition.equalsIgnoreCase("S:ToArea*") ||
               definition.equalsIgnoreCase("S:Areaname*") ||
               definition.equalsIgnoreCase("S:FromArea*"))
        resourceTypes = new String[]{".ARE"};
      else if (definition.equalsIgnoreCase("S:BamResRef*"))
        resourceTypes = new String[]{".BAM"};
      else if (definition.equalsIgnoreCase("S:Pool*"))
        resourceTypes = new String[]{".SRC"};
      else if (definition.equalsIgnoreCase("S:Palette*"))
        resourceTypes = new String[]{".BMP"};
      else if (definition.equalsIgnoreCase("S:ResRef*"))
        resourceTypes = Decompiler.getResRefType(function.substring(0, function.length() - 1));
      else if (definition.equalsIgnoreCase("S:Object*")) // ToDo: Better check possible?
        resourceTypes = new String[]{".ITM", ".VVC", ".BAM"};
      else if (definition.equalsIgnoreCase("S:NewObject*")) // ToDo: Better check possible?
        resourceTypes = new String[]{".CRE", ".DLG", ".BCS", ".ITM"};

      if (resourceTypes.length > 0) {
        for (final String resourceType : resourceTypes)
          if (ResourceFactory.getInstance().resourceExists(value.substring(1, value.length() - 1) + resourceType))
            return;
        warnings.put(new Integer(linenr), "Resource not found: " + definition + " - " + value);
      }
    }
//    else
//      System.out.println(definition + " - " + value);
  }

  private void compileAction(StringBuilder code, String line)
  {
    int i = line.indexOf((int)'(');
    int j = line.lastIndexOf((int)')');
    if (i == -1 || j == -1) {
      String error = "Missing parenthesis";
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
      return;
    }
    String s_action = line.substring(0, i + 1);
    String s_param = line.substring(i + 1, j);
    while (s_action.endsWith(" ("))
      s_action = s_action.substring(0, s_action.length() - 2) + '(';

    IdsMapEntry idsEntry = IdsMapCache.get("ACTION.IDS").lookup(s_action);
    if (idsEntry == null) {
      String error = s_action + " not found in ACTION.IDS";
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
      return;
    }

    String list_i[] = {"0", "0", "0"};
    String list_o[] = {emptyObject, emptyObject, emptyObject};
    String list_s[] = {"\"\"", "\"\"", "\"\"", "\"\""}; // Might be more than two because of ModifyStrings
    String list_p[] = {"0 0"};
    int index_i = 0, index_o = 0, index_s = 0, index_p = 0;

    if (s_action.equalsIgnoreCase("ActionOverride(")) {
      list_o[index_o++] = compileObject("O:Actor*", s_param.substring(0, s_param.indexOf(',')).trim());
      line = s_param.substring(s_param.indexOf(',') + 1).trim();
      i = line.indexOf((int)'(');
      j = line.lastIndexOf((int)')');
      if (i == -1 || j == -1) {
        String error = "Missing parenthesis";
        errors.put(new Integer(linenr), error);
        code.append("Error - ").append(error).append('\n');
        return;
      }
      s_action = line.substring(0, i + 1);
      s_param = line.substring(i + 1, j).trim();
      while (s_action.endsWith(" ("))
        s_action = s_action.substring(0, s_action.length() - 2) + '(';

      idsEntry = IdsMapCache.get("ACTION.IDS").lookup(s_action);
      if (idsEntry == null) {
        String error = s_action + " not found in ACTION.IDS";
        errors.put(new Integer(linenr), error);
        code.append("Error - ").append(error).append('\n');
        return;
      }
    }
    else
      index_o++;

    code.append("AC\n").append(idsEntry.getID());

    StringTokenizer actParam = new StringTokenizer(s_param, ",");
    StringTokenizer defParam = new StringTokenizer(idsEntry.getParameters(), ",");
    int defParamCount = defParam.countTokens();
    while (actParam.hasMoreTokens()) {
      String parameter = actParam.nextToken().trim();
      if (parameter.charAt(0) == '[' && parameter.charAt(parameter.length() - 1) != ']') {
        if (actParam.hasMoreTokens())
          parameter += ',' + actParam.nextToken();
        else {
          String error = "Missing end bracket - " + parameter;
          errors.put(new Integer(linenr), error);
          code.append("Error - ").append(error).append('\n');
        }
      }
      if (parameter.charAt(0) == '"' && parameter.charAt(parameter.length() - 1) != '"') {
        if (actParam.hasMoreTokens())
          parameter += ',' + actParam.nextToken();
        else {
          String error = "Missing end quote - " + parameter;
          errors.put(new Integer(linenr), error);
          code.append("Error - ").append(error).append('\n');
        }
      }
      if (!defParam.hasMoreTokens()) {
        String error = "Too many arguments - (" + idsEntry.getParameters() + ')';
        errors.put(new Integer(linenr), error);
        code.append("Error - ").append(error).append('\n');
        return;
      }
      String definition = defParam.nextToken();
      if (definition.startsWith("I:") && actParam.hasMoreTokens() && !defParam.hasMoreTokens()) // Ugly fix - commas in IDS-files
        parameter = parameter + ',' + actParam.nextToken();
      if (definition.startsWith("S:")) {
        if (index_s == 2 && parameter.charAt(0) != '"')
          list_o[index_o++] = compileObject(definition, parameter);
        else
          list_s[index_s++] = compileString(s_action, definition, parameter);
      }
      else if (definition.startsWith("I:"))
        list_i[index_i++] = compileInteger(definition, parameter);
      else if (definition.startsWith("O:"))
        list_o[index_o++] = compileObject(definition, parameter);
      else if (definition.startsWith("P:"))
        list_p[index_p++] = compilePoint(definition, parameter);
    }
    if (defParamCount > index_s + index_i + (index_o - 1) + index_p) {
      String error = "Too few arguments - (" + idsEntry.getParameters() + ')';
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
      return;
    }
    list_s = modifyStrings(list_s, (long)-1);

    code.append(list_o[0]).append('\n');
    code.append(list_o[1]).append('\n');
    code.append(list_o[2]).append('\n');
    code.append(list_i[0]).append(' ');
    code.append(list_p[0]).append(' ');
    code.append(list_i[1]).append(' ');
    code.append(list_i[2]);
    code.append(list_s[0]).append(' ');
    code.append(list_s[1]).append(' ');

    code.append("AC\n");
  }

  private void compileCondition(StringBuilder code, StringTokenizer st)
  {
    // IF last read token
    code.append("CO\n");
    String line = getNextLine(st);
    int orCount = 0;
    while (!line.equalsIgnoreCase("THEN") && line.length() > 0) {
      int newOrCount = compileTrigger(code, line);
      if (newOrCount == 0 && orCount > 0)
        orCount--;
      else if (newOrCount > 0 && orCount > 0) {
        String error = "Nested ORs not allowed";
        errors.put(new Integer(linenr), error);
        code.append("Error - ").append(error).append('\n');
      }
      else
        orCount = newOrCount;
      line = getNextLine(st);
    }
    if (orCount > 0) {
      String error = "Missing " + orCount + " trigger(s) in order to match OR()";
      errors.put(new Integer(linenr - 1), error);
      code.append("Error - ").append(error).append('\n');
    }
    if (line.length() == 0) {
      String error = "Missing THEN";
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
    }
    code.append("CO\n");
  }

  private String compileInteger(String definition, String value)
  {
    try {
      if (value.length() > 2 && value.substring(0, 2).equalsIgnoreCase("0x")) {
        long nr = Long.parseLong(value.substring(2), 16);
        if (nr >= 2147483648L)
          nr -= 4294967296L;
        return String.valueOf(nr);
      }
      else {
      // XXX: What is the purpose of this?
      // Maybe unsigned -> signed conversion?
      // Why not simply cast it to (int)?
        long nr = Long.parseLong(value);
        if (nr >= 2147483648L) {
          nr -= 4294967296L;
          return String.valueOf(nr);
        }
        else
          return value;
      }
    } catch (NumberFormatException e) {
    }
    int i = definition.lastIndexOf((int)'*');
    if (i == -1 || definition.substring(i + 1).length() == 0) {
      String error = "Expected " + definition + " but found " + value;
      errors.put(new Integer(linenr), error);
      return "Error - " + error;
    }
    IdsMap idsmap = IdsMapCache.get(definition.substring(i + 1).toUpperCase() + ".IDS");
    String code = idsmap.lookupID(value);
    if (code != null)
      return code;
    else if (value.indexOf("|") != -1) {
      long nr = (long)0;
      StringTokenizer st = new StringTokenizer(value, "|");
      while (st.hasMoreTokens()) {
        String svalue = st.nextToken().trim();
        IdsMapEntry idsentry = idsmap.lookup(svalue);
        if (idsentry == null) {
          String error = svalue + " not found in " + idsmap;
          errors.put(new Integer(linenr), error);
          return "Error - " + error;
        }
        nr += idsentry.getID();
      }
      if (nr >= 2147483648L)
        nr -= 4294967296L;
      return String.valueOf(nr);
    }
    else {
      String error = value + " not found in " + idsmap;
      errors.put(new Integer(linenr), error);
      return "Error - " + error;
    }
  }

  private String compileObject(String definition, String value)
  {
    long identifiers[] = {0, 0, 0, 0, 0};
    int firstIdentifier = -1;
    if (value.length() > 0 && value.charAt(0) != '"') { // Not straight string
      int i = value.indexOf((int)'(');
      while (i != -1) {
        if (value.charAt(value.length() - 1) != ')') {
          String error = "Missing end parenthesis " + value;
          errors.put(new Integer(linenr), error);
          return "Error - " + error;
        }
        IdsMapEntry idsEntry = IdsMapCache.get("OBJECT.IDS").lookup(value.substring(0, i));
        if (idsEntry == null) {
          String error = value.substring(0, i) + " not found in OBJECT.IDS";
          errors.put(new Integer(linenr), error);
          return "Error - " + error;
        }
        value = value.substring(i + 1, value.length() - 1);
        identifiers[++firstIdentifier] = idsEntry.getID();
        i = value.indexOf((int)'(');
      }
      if (value.length() == 0 && firstIdentifier >= 0)
        value = "MYSELF";
    }

    StringBuilder code = new StringBuilder("OB\n");

    if (value.length() > 0 && value.charAt(0) == '[') { // Coordinate/ObjectType
      String coord = "[-1.-1.-1.-1]";
      String iwd2 = " 0 0 ";
      while (value.charAt(0) == '[') {
        int endIndex = value.indexOf((int)']');
        if (endIndex == -1) {
          String error = "Missing end bracket";
          errors.put(new Integer(linenr), error);
          return "Error - " + error + '\n';
        }
        String rest = value.substring(endIndex);
        if (endIndex == 1) // Enable [] shortcut
          value = "ANYONE";
        else
          value = value.substring(1, endIndex);
        if (value.equalsIgnoreCase("ANYONE")) {
          if (itype.length == 7)
            code.append("0 0 0 0 0 0 0 ");
          else if (itype.length == 9)
            code.append("0 0 0 0 0 0 0 0 0 ");
          else if (itype.length == 10)
            code.append("0 0 0 0 0 0 0 0 ");
        }
        else {
          StringTokenizer st = new StringTokenizer(value, ".");
          boolean possiblecoord = true;
          StringBuilder temp = new StringBuilder();
          for (final IdsMap idsMap : itype) {
            if (st.countTokens() > 4)
              possiblecoord = false;
            if (st.hasMoreTokens()) {
              String objType = st.nextToken();
              IdsMapEntry idsEntry = idsMap.lookup(objType);
              if (idsEntry == null) {
                try {
                  temp.append(Long.parseLong(objType)).append(' ');
                } catch (NumberFormatException e) {
                  String error = objType + " not found in " + idsMap.toString().toUpperCase();
                  errors.put(new Integer(linenr), error);
                  return "Error - " + error;
                }
              }
              else {
                temp.append(idsEntry.getID()).append(' ');
                possiblecoord = false;
              }
            }
            else
              temp.append("0 ");
          }
          if (possiblecoord && (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ||
                                ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
                                ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
                                ResourceFactory.getGameID() ==
                                ResourceFactory.ID_ICEWINDHOWTOT ||
                                ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)) {
            if (code.toString().equals("OB\n")) {
              if (itype.length == 7)
                code.append("0 0 0 0 0 0 0 ");
              else if (itype.length == 9)
                code.append("0 0 0 0 0 0 0 0 0 ");
              else if (itype.length == 10)
                code.append("0 0 0 0 0 0 0 0 ");
            }
            coord = '[' + value + ']';
          }
          else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2) {
            int space = temp.lastIndexOf(" ");
            space = temp.substring(0, space).lastIndexOf(" ");
            space = temp.substring(0, space).lastIndexOf(" ");
            code.append(temp.substring(0, space + 1));
            iwd2 = temp.substring(space);
          }
          else
            code.append(temp);
        }
        int index = rest.indexOf((int)'[');
        if (index != -1)
          value = rest.substring(index);
      }
      for (int i = firstIdentifier; i >= 0; i--)
        code.append(identifiers[i]).append(' ');
      for (int i = firstIdentifier + 1; i < identifiers.length; i++)
        code.append(identifiers[i]).append(' ');
      if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT)
        code.append(coord).append(" \"\"OB");
      else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
        code.append(coord).append(" \"\"").append(iwd2).append("OB");
      else
        code.append("\"\"OB");
    }

    else if (value.length() > 0 && value.charAt(0) == '"') { // String
      if (value.charAt(value.length() - 1) != '"') {
        String error = "Missing end quote - " + value;
        errors.put(new Integer(linenr), error);
        return "Error - " + error;
      }
      if (itype.length == 7)
        code.append("0 0 0 0 0 0 0 ");
      else if (itype.length == 9)
        code.append("0 0 0 0 0 0 0 0 0 ");
      else if (itype.length == 10)
        code.append("0 0 0 0 0 0 0 0 ");
      for (int i = firstIdentifier; i >= 0; i--)
        code.append(identifiers[i]).append(' ');
      for (int i = firstIdentifier + 1; i < identifiers.length; i++)
        code.append(identifiers[i]).append(' ');
      if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT)
        code.append("[-1.-1.-1.-1] ").append(value).append("OB");
      else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
        code.append("[-1.-1.-1.-1] ").append(value).append(" 0 0 OB");
      else
        code.append(value).append("OB");
      checkObjectString(definition, value);
    }

    else {
      if (itype.length == 7)
        code.append("0 0 0 0 0 0 0 ");
      else if (itype.length == 9)
        code.append("0 0 0 0 0 0 0 0 0 ");
      else if (itype.length == 10)
        code.append("0 0 0 0 0 0 0 0 ");
      String coord;
      if (value.endsWith("]")) {
        coord = value.substring(value.indexOf((int)'['));
        value = value.substring(0, value.indexOf((int)'['));
      }
      else
        coord = "[-1.-1.-1.-1]";
      IdsMapEntry idsEntry = IdsMapCache.get("OBJECT.IDS").lookup(value);
      if (idsEntry == null)
        identifiers[++firstIdentifier] = (long)0;
      else
        identifiers[++firstIdentifier] = idsEntry.getID();
      if (coord.equals("[-1.-1.-1.-1]") && idsEntry == null && !value.equals("")) {
        String error = "Unknown symbol - " + value;
        errors.put(new Integer(linenr), error);
        return "Error - " + error;
      }
      for (int i = firstIdentifier; i >= 0; i--)
        code.append(identifiers[i]).append(' ');
      for (int i = firstIdentifier + 1; i < identifiers.length; i++)
        code.append(identifiers[i]).append(' ');
      if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
          ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT)
        code.append("[-1.-1.-1.-1] \"\"OB");
      else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
        code.append(coord).append(" \"\" 0 0 OB");
      else {
        code.append("\"\"OB");
        if (!coord.equals("[-1.-1.-1.-1]")) {
          String error = "Missing parenthesis?";
          errors.put(new Integer(linenr), error);
          return "Error - " + error;
        }
      }
    }
    return code.toString();
  }

  private String compilePoint(String definition, String value)
  {
    if (value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']') {
      value = value.substring(1, value.length() - 1); // Remove '[' and ']'
      StringTokenizer st = new StringTokenizer(value, ".");
      StringBuilder code = new StringBuilder();
      int countPeriod = 0;
      for (int i = 0; i < value.length(); i++)
        if (value.charAt(i) == '.')
          countPeriod++;
      if (countPeriod != st.countTokens() - 1) {
        String error = '[' + value + "] - arguments missing";
        errors.put(new Integer(linenr), error);
        return "Error - " + error;
      }
      try {
        while (st.hasMoreTokens()) {
          String s = st.nextToken();
          if (code.length() > 0)
            code.append(' ');
          code.append(Integer.parseInt(s));
        }
        return code.toString();
      } catch (NumberFormatException e) {
        String error = '[' + value + "] must contain numbers only";
        errors.put(new Integer(linenr), error);
        return "Error - " + error;
      }
    }
    String error = "Expected " + definition + " but found " + value;
    errors.put(new Integer(linenr), error);
    return "Error - " + error;
  }

  private void compileResponseSet(StringBuilder code, StringTokenizer st)
  {
    // THEN last read token
    code.append("RS\n");
    String line = getNextLine(st);
    boolean firstresponse = true;
    while (!line.equalsIgnoreCase("END") && line.length() > 0) {
      if (line.length() > 7 && line.substring(0, 8).equalsIgnoreCase("RESPONSE")) {
        if (!firstresponse)
          code.append("RE\n");
        code.append("RE\n");
        int i = line.indexOf((int)'#');
        if (i == -1) {
          String error = "Missing # in RESPONSE";
          errors.put(new Integer(linenr), error);
          code.append("Error - ").append(error).append('\n');
          return;
        }
        code.append(line.substring(i + 1));
        firstresponse = false;
      }
      else
        compileAction(code, line);
      line = getNextLine(st);
    }
    if (line.length() == 0) {
      String error = "Missing END";
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
    }
    code.append("RE\nRS\n");
  }

  private String compileString(String function, String definition, String value)
  {
    checkString(function, definition, value);
    return value;
  }

  private int compileTrigger(StringBuilder code, String line) // returns n if trigger = OR(n)
  {
    String flag;
    if (line.charAt(0) == '!') {
      flag = "1";
      line = line.substring(1, line.length());
    }
    else
      flag = "0";

    int i = line.indexOf((int)'(');
    int j = line.lastIndexOf((int)')');
    if (i == -1 || j == -1) {
      String error = "Missing parenthesis";
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
      return 0;
    }
    String s_trigger = line.substring(0, i + 1);
    String s_param = line.substring(i + 1, j);
    while (s_trigger.endsWith(" ("))
      s_trigger = s_trigger.substring(0, s_trigger.length() - 2) + '(';

    IdsMapEntry idsEntry = IdsMapCache.get("TRIGGER.IDS").lookup(s_trigger);
    if (idsEntry == null) {
      String error = s_trigger + " not found in TRIGGER.IDS";
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
      return 0;
    }

    code.append("TR\n").append(idsEntry.getID()).append(' ');
    String integers[] = {"0", "0", "0"};
    String object = null;
    String strings[] = {"\"\"", "\"\"", "\"\"", "\"\""};
    String point = "[0,0]";
    int indexI = 0, indexS = 0;

    StringTokenizer actParam = new StringTokenizer(s_param, ",");
    StringTokenizer defParam = new StringTokenizer(idsEntry.getParameters(), ",");
    int defParamCount = defParam.countTokens(), actParamCount = 0;
    while (actParam.hasMoreTokens()) {
      String parameter = actParam.nextToken().trim();
      if (parameter.charAt(0) == '"' && parameter.charAt(parameter.length() - 1) != '"') {
        if (actParam.hasMoreTokens())
          parameter += ',' + actParam.nextToken();
        else {
          String error = "Missing end quote - " + parameter;
          errors.put(new Integer(linenr), error);
          code.append("Error - ").append(error).append('\n');
          return 0;
        }
      }
      if (parameter.charAt(0) == '[' && parameter.charAt(parameter.length() - 1) != ']') {
        if (actParam.hasMoreTokens())
          parameter += ',' + actParam.nextToken();
        else {
          String error = "Missing end bracket - " + parameter;
          errors.put(new Integer(linenr), error);
          code.append("Error - ").append(error).append('\n');
          return 0;
        }
      }
      if (!defParam.hasMoreTokens()) {
        String error = "Too many arguments - (" + idsEntry.getParameters() + ')';
        errors.put(new Integer(linenr), error);
        code.append("Error - ").append(error).append('\n');
        return 0;
      }
      String definition = defParam.nextToken();
      if (definition.startsWith("I:") && actParam.hasMoreTokens() && !defParam.hasMoreTokens()) // Ugly fix - commas in IDS-files
        parameter = parameter + ',' + actParam.nextToken();
      if (definition.startsWith("S:"))
        strings[indexS++] = compileString(s_trigger, definition, parameter);
      else if (definition.startsWith("I:"))
        integers[indexI++] = compileInteger(definition, parameter);
      else if (definition.startsWith("O:"))
        object = compileObject(definition, parameter);
      else if (definition.startsWith("P:"))
        point = parameter.replaceFirst("\\.", ",");     // be consistent with WeiDU
      actParamCount++;
    }
    if (defParamCount > actParamCount) {
      String error = "Too few arguments - (" + idsEntry.getParameters() + ')';
      errors.put(new Integer(linenr), error);
      code.append("Error - ").append(error).append('\n');
      return 0;
    }
    if (object == null)
      object = emptyObject;

    strings = modifyStrings(strings, idsEntry.getID());

    code.append(integers[0]).append(' ');
    code.append(flag).append(' ');
    code.append(integers[1]).append(' ');
    code.append(integers[2]).append(' ');
    if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      code.append(point).append(' ');
    code.append(strings[0]).append(' ');
    code.append(strings[1]).append(' ');
    code.append(object).append('\n');

    code.append("TR\n");
    if (s_trigger.equalsIgnoreCase("OR("))
      return Integer.parseInt(integers[0]);
    return 0;
  }

  private String getNextLine(StringTokenizer st)
  {
    if (!st.hasMoreTokens())
      return "";
    String line = st.nextToken();
    if (!line.equals("\n") && st.hasMoreTokens())
      st.nextToken();
    linenr++;
    int i = line.indexOf("//");
    if (i != -1)
      line = line.substring(0, i);
    line = line.trim();
    if (line.length() == 0)
      line = getNextLine(st);
    return line;
  }

  private String[] modifyStrings(String strings[], long id)
  {
    String newStrings[] = new String[strings.length];
    int newIndex = 0;
    for (final String string : strings) {
      if (string.charAt(0) == '"' && string.charAt(string.length() - 1) != '"')
        errors.put(new Integer(linenr), "Missing end quote - " + string);
      else if (string.charAt(0) != '"' && string.charAt(string.length() - 1) == '"')
        errors.put(new Integer(linenr), "Missing begin quote - " + string);
      else if (string.charAt(0) != '"' && string.charAt(string.length() - 1) != '"')
        errors.put(new Integer(linenr), "Missing quotes - " + string);
//        strings[i] = '"' + strings[i] + '"';
      if (newIndex == 0 || id == 16449 || id == 16566 || id == 16448) // Don't ask me why...
        newStrings[newIndex++] = string;
      else if (isPossibleNamespace(string) && !isPossibleNamespace(newStrings[newIndex - 1]))
        newStrings[newIndex - 1] = string.substring(0, 7) + newStrings[newIndex - 1].substring(1);
      else
        newStrings[newIndex++] = string;
    }
    while (newIndex < newStrings.length)
      newStrings[newIndex++] = "\"\"";
    return newStrings;
  }
}

