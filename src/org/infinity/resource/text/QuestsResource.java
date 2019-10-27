// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;

import org.infinity.datatype.StringRef;
import org.infinity.icon.Icons;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.StringTable;

/**
 * Resource, that represents PS:T {@code "quests.ini"} file - special resource
 * with list of quests.
 *
 * @author Mingun
 */
public class QuestsResource extends PlainTextResource
{
  /** Special resource name {@code "quests.ini"}. */
  public static final String RESOURCE_NAME = "quests.ini";

  //<editor-fold defaultstate="collapsed" desc="Internal classes">
  /** Represents one quest in the file. */
  public static final class Quest
  {
    /** Positional number of quest in the ini file. */
    final int number;
    /** StringRef with name of quest. */
    final StringRef title;
    /** StringRef, that displayed in assigned, but not completed quests area. */
    final StringRef descAssigned;
    /** StringRef, that displayed in completed (succesfully or not) quests area. */
    final StringRef descCompleted;
    /**
     * List of checks that determines, when this quest must be shown in the
     * "Assigned" quests page. All checks combined by {@code AND}.
     */
    final List<Check> assignedChecks;
    /**
     * List of checks that determines, when this quest must be shown in the
     * "Completed" quests page. All checks combined by {@code AND}.
     */
    final List<Check> completeChecks;

    public Quest(IniMapSection section)
    {
      number         = Integer.parseInt(section.getName());
      title          = section.getAsStringRef("title");
      descAssigned   = section.getAsStringRef("descAssigned");
      descCompleted  = section.getAsStringRef("descCompleted");

      assignedChecks = readConditions(section, "assignedChecks", 'a');
      completeChecks = readConditions(section, "completeChecks", 'c');
    }

    public String toString(StringTable.Format fmt)
    {
      return title == null ? "<no title>" : title.toString(fmt);
    }

    @Override
    public String toString()
    {
      return toString(StringTable.Format.NONE);
    }

    private static List<Check> readConditions(IniMapSection section, String countVar, char prefix)
    {
      final int count = section.getAsInteger(countVar, 0);
      final ArrayList<Check> result = new ArrayList<>(count);
      for (int i = 1; i <= count; ++i) {
        result.add(new Check(section, prefix, i));
      }
      return result;
    }
  }
  /** Class, that represent one variable check condition for quest. */
  public static final class Check
  {
    /** Variable to check. */
    final String var;
    /** Value to check againist. */
    final String value;
    /**
     * Condition for check. Must be one of the {@link Condition} constants, but
     * stored as a string to be able to save invalid conditions.
     */
    final String condition;

    public Check(IniMapSection section, char prefix, int number)
    {
      var       = section.getAsString(prefix + "Var" + number);
      value     = section.getAsString(prefix + "Value" + number);
      condition = section.getAsString(prefix + "Condition" + number);
    }

    public String getHumanizedCondition()
    {
      try {
        return Condition.valueOf(condition).title;
      } catch (IllegalArgumentException ex) {
        return condition;
      }
    }
  }
  public enum Condition
  {
    /** Variable must be equal ({@code ==}) to condition constant. */
    EQ("=="),
    /** Variable must be unequal ({@code !=}) to condition constant. */
    NE("!="),
    /** Variable must be strictly less ({@code <}) then condition constant. */
    LT("<"),
    /** Variable must be strictly more ({@code >}) then condition constant. */
    GT(">");

    /** Humanized name оf condition operator. */
    final String title;

    private Condition(String title) { this.title = title; }
  }
  //</editor-fold>

  public QuestsResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    final JTabbedPane pane = new JTabbedPane();
    pane.addTab("Quest List", new QuestsPanel(readQuests()));
    pane.addTab("Text", Icons.getIcon(Icons.ICON_EDIT_16), super.makeViewer(container));
    return pane;
  }

  private List<Quest> readQuests()
  {
    final IniMap ini = new IniMap(getResourceEntry());
    final IniMapSection init = ini.getSection("init");
    final int questCount = init == null ? -1 : init.getAsInteger("questcount", -1);
    final ArrayList<Quest> quests = new ArrayList<>(questCount < 0 ? ini.getSectionCount() : questCount);
    for (final IniMapSection quest : ini) {
      try {
        quests.add(new Quest(quest));
      } catch (NumberFormatException ex) {
        // Skip non-number question name
      }
    }
    return quests;
  }
}
