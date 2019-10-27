// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.text;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.infinity.datatype.StringRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.InfinityScrollPane;
import org.infinity.gui.InfinityTextArea;
import org.infinity.resource.text.QuestsResource.Check;
import org.infinity.resource.text.QuestsResource.Quest;
import org.infinity.util.StringTable;

/**
 * <code><pre>
 * .-------. [Quest title                   ]
 * |       |:.--------------..--------------.
 * | quests|:| descAssigned ||descCompleted |
 * |       |:'--------------''--------------'
 * | table |:.--------------..--------------.
 * |       |:|assignedChecks||completeChecks|
 * '-------' '--------------''--------------'
 * </pre></code>
 * @author Mingun
 */
final class QuestsPanel extends JPanel implements ListSelectionListener
{
  private static final ConditionModel EMPTY_MODEL = new ConditionModel(Collections.emptyList());
  private static final String NO_QUEST_SELECTED = "No quest selected";
  private static final String DESCRIPTION = "Description";

  private final JTable quests;
  private final JLabel title = new JLabel(NO_QUEST_SELECTED);
  private final InfinityTextArea descAssigned  = new InfinityTextArea(true);
  private final InfinityTextArea descCompleted = new InfinityTextArea(true);
  private final JTable assignedChecks = new JTable();
  private final JTable completeChecks = new JTable();

  //<editor-fold defaultstate="collapsed" desc="Internal classes">
  private static final class QuestsModel extends AbstractTableModel
  {
    private static final String[] COLUMNS = {
      "#",
      "Quest Name",
    };
    private final List<Quest> quests;

    QuestsModel(List<Quest> quests) { this.quests = quests; }

    @Override
    public int getRowCount() { return quests.size(); }

    @Override
    public int getColumnCount() { return COLUMNS.length; }

    @Override
    public String getColumnName(int columnIndex) { return COLUMNS[columnIndex]; }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      switch (columnIndex) {
        case 0: return Integer.class;
        case 1: return String.class;
      }
      throw new IndexOutOfBoundsException("Invalid column " + columnIndex);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      final Quest quest = quests.get(rowIndex);
      switch (columnIndex) {
        case 0: return quest.number;
        case 1: return quest.toString();
      }
      throw new IndexOutOfBoundsException("Invalid column " + columnIndex);
    }
  }
  private static final class ConditionModel extends AbstractTableModel
  {
    private static final String[] COLUMNS = {
      "Variable Name",
      "Condition",
      "Value",
    };
    private final List<Check> conditions;

    public ConditionModel(List<Check> conditions)
    {
      this.conditions = conditions;
    }

    @Override
    public int getRowCount() { return conditions.size(); }

    @Override
    public int getColumnCount() { return COLUMNS.length; }

    @Override
    public String getColumnName(int columnIndex) { return COLUMNS[columnIndex]; }

    @Override
    public Class<?> getColumnClass(int columnIndex) { return String.class; }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      final Check cond = conditions.get(rowIndex);
      switch (columnIndex) {
        case 0: return cond.var;
        case 1: return cond.getHumanizedCondition();
        case 2: return cond.value;
      }
      throw new IndexOutOfBoundsException("Invalid column " + columnIndex);
    }
  }
  //</editor-fold>

  public QuestsPanel(List<Quest> quests)
  {
    super(new BorderLayout());
    this.quests = new JTable(new QuestsModel(quests));
    final int size = this.quests.getFontMetrics(this.quests.getFont()).charWidth('w') * 4;
    this.quests.getColumnModel().getColumn(0).setMaxWidth(size);
    this.quests.getSelectionModel().addListSelectionListener(this);

    final JPanel questInfo = new JPanel();
    questInfo.setLayout(new BoxLayout(questInfo, BoxLayout.X_AXIS));
    questInfo.add(createPanel(descAssigned , assignedChecks, "Assigned"));
    questInfo.add(createPanel(descCompleted, completeChecks, "Completed"));

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(title, BorderLayout.NORTH);
    panel.add(questInfo, BorderLayout.CENTER);

    final JScrollPane scroll = new JScrollPane(this.quests);
    scroll.setBorder(BorderFactory.createTitledBorder(String.format("Quests (%d)", quests.size())));
    add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll, panel), BorderLayout.CENTER);
  }

  //<editor-fold defaultstate="collapsed" desc="ListSelectionListener">
  @Override
  public void valueChanged(ListSelectionEvent e)
  {
    final int index = quests.getSelectedRow();
    if (index >= 0) {
      final Quest quest = ((QuestsModel)quests.getModel()).quests.get(index);

      final StringTable.Format fmt = BrowserMenuBar.getInstance().showStrrefs() ? StringTable.Format.STRREF_SUFFIX
                                                                                : StringTable.Format.NONE;
      title.setText(quest.toString(fmt));

      setText(descAssigned , quest.descAssigned);
      setText(descCompleted, quest.descCompleted);

      assignedChecks.setModel(new ConditionModel(quest.assignedChecks));
      completeChecks.setModel(new ConditionModel(quest.completeChecks));
    } else {
      title.setText(NO_QUEST_SELECTED);

      setText(descAssigned , null);
      setText(descCompleted, null);

      assignedChecks.setModel(EMPTY_MODEL);
      completeChecks.setModel(EMPTY_MODEL);
    }
  }
  //</editor-fold>

  private JPanel createPanel(InfinityTextArea area, JTable checks, String title)
  {
    final JPanel panel = new JPanel(new GridBagLayout());
    panel.setBorder(BorderFactory.createTitledBorder(title));

    final GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1;

    gbc.weighty = 2;
    panel.add(wrap(area, DESCRIPTION), gbc);

    gbc.gridy = 1;
    gbc.weighty = 1;
    panel.add(wrap(checks, "Conditions"), gbc);

    return panel;
  }

  private static JScrollPane wrap(InfinityTextArea area, String title)
  {
    area.setLineWrap(true);
    area.setEditable(false);
    final InfinityScrollPane scroll = new InfinityScrollPane(area, true);
    scroll.setBorder(BorderFactory.createTitledBorder(title));
    return scroll;
  }

  private static JScrollPane wrap(JTable table, String title)
  {
    final JScrollPane scroll = new JScrollPane(table);
    scroll.setBorder(BorderFactory.createTitledBorder(title));
    return scroll;
  }

  private static void setText(InfinityTextArea area, StringRef value)
  {
    // Direct parent of area - JViewport
    final JScrollPane scroll = (JScrollPane)area.getParent().getParent();
    final TitledBorder border = (TitledBorder)scroll.getBorder();
    if (value != null) {
      border.setTitle(StringTable.Format.STRREF_SUFFIX.format(DESCRIPTION, value.getValue()));
      area.setText(value.getText());
    } else {
      border.setTitle(DESCRIPTION);
      area.setText(null);
    }
    // To repaint TitledBorder. Swing have a bug and do not doing this automatically
    scroll.repaint();
  }
}
