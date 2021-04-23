// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;

import org.infinity.datatype.StringRef;
import org.infinity.icon.Icons;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.StringTable;
import org.infinity.util.Variables;

/**
 * Resource, that represents PS:T {@code "quests.ini"} file - special resource
 * with list of quests.
 *
 * @author Mingun
 */
public class QuestsResource extends PlainTextResource implements ChangeListener
{
  /** Special resource name {@code "quests.ini"}. */
  public static final String RESOURCE_NAME = "quests.ini";

  /**
   * Flag, that indicates, that {@link #quests is not synchronized with current
   * {@link #getText() resource text} and reloading is required.
   */
  private boolean dirty = false;

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

    /**
     * Evaluates execution status of this quest. Firstly checks completed conditions
     * and return {@link State#Completed} if all conditions met. If not, run
     * assigned conditions check and return {@link State#Assigned} if all conditions
     * met. Otherwise return {@link State#Unassigned}.
     *
     * @param vars Container with variables, containing the status of quest.
     *
     * @return Execution status of the quest
     */
    public State evaluate(Variables vars)
    {
      if (evaluateAnd(vars, completeChecks)) {
        return State.Completed;
      }
      if (evaluateAnd(vars, assignedChecks)) {
        return State.Assigned;
      }
      return State.Unassigned;
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

    /**
     * Evaluate all checks againist specified variables combining results with AND
     * logical operator.
     *
     * @param vars Container with values of variables
     * @param checks List of checks to run
     *
     * @return {@code true} if all checks evaluated to {@code true}, {@code false}
     *         otherwise
     */
    private static boolean evaluateAnd(Variables vars, List<Check> checks)
    {
      for (final Check check : checks) {
        if (!check.evaluate(vars)) return false;
      }
      return true;
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

    /**
     * Evaluates check condition.
     *
     * @param vars Container with values of variables
     * @return {@code true} if condition met, {@code false} otherwise
     *
     * @throws NumberFormatException If condition {@link #value} is not integer number
     * @throws IllegalArgumentException If {@link #condition} is not one of the
     *         {@link Condition known conditions}
     */
    public boolean evaluate(Variables vars)
    {
      final int val = Integer.parseInt(value);
      final int var = vars.getInt(this.var);
      final Condition cond = Condition.valueOf(condition);
      switch (cond) {
        case EQ: return var == val;
        case NE: return var != val;
        case LT: return var <  val;
        case GT: return var >  val;
      }
      throw new InternalError("Unknown enum variant: " + cond);
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
  /** Quest execution status. */
  public enum State
  {
    /** Quest not yet taken by player and unknown to him. */
    Unassigned,
    /** Quest taken by player but not yet completed, active quest. */
    Assigned,
    /** Quest is finished. */
    Completed;
  }

  public QuestsResource() throws Exception
  {
    this(ResourceFactory.getResourceEntry(RESOURCE_NAME));
  }

  public QuestsResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    final JComponent textPage = super.makeViewer(container);
    final QuestsPanel details = new QuestsPanel(readQuests(), null);
    final JTabbedPane pane = new JTabbedPane();
    pane.addTab("Quest List", details);
    pane.addTab("Text", Icons.getIcon(Icons.ICON_EDIT_16), textPage);
    pane.addChangeListener(this);
    return pane;
  }

  @Override
  public void changedUpdate(DocumentEvent event)
  {
    super.changedUpdate(event);
    dirty = true;
  }

  @Override
  public void insertUpdate(DocumentEvent event)
  {
    super.insertUpdate(event);
    dirty = true;
  }

  @Override
  public void removeUpdate(DocumentEvent event)
  {
    super.removeUpdate(event);
    dirty = true;
  }

  @Override
  public void stateChanged(ChangeEvent e)
  {
    final JTabbedPane pane = (JTabbedPane)e.getSource();
    final Component selected = pane.getSelectedComponent();
    if (selected instanceof QuestsPanel && dirty) {
      ((QuestsPanel)selected).setQuests(readQuests());
      dirty = false;
    }
  }

  public List<Quest> readQuests()
  {
    final IniMap ini = new IniMap(editor == null ? text : editor.getText());
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
