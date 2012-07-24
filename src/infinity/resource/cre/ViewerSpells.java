// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.gui.ToolTipTableCellRenderer;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

final class ViewerSpells extends JPanel implements ActionListener
{
  private final JButton bOpen = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
  private final JTable table;
  private final MemSpellTableModel tableModel;

  ViewerSpells(CreResource cre)
  {
    super(new BorderLayout(0, 3));
    tableModel = new MemSpellTableModel(cre);
    table = new JTable(tableModel);
    table.setDefaultRenderer(Object.class, new ToolTipTableCellRenderer());
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getColumnModel().getColumn(0).setMaxWidth(45);
    table.getColumnModel().getColumn(1).setMaxWidth(5);
    table.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2 && table.getSelectedRowCount() == 1) {
          ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 2);
          if (ref != null) {
            Resource res = ResourceFactory.getResource(
                    ResourceFactory.getInstance().getResourceEntry(ref.getResourceName()));
            new ViewFrame(getTopLevelAncestor(), res);
          }
        }
      }
    });
    table.getSelectionModel().setSelectionInterval(0, 0);
    add(new JLabel("Memorized spells"), BorderLayout.NORTH);
    add(new JScrollPane(table), BorderLayout.CENTER);
    bOpen.addActionListener(this);
    bOpen.setEnabled(tableModel.getRowCount() > 0);
    add(bOpen, BorderLayout.SOUTH);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen) {
      ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 2);
      if (ref != null) {
        Resource res = ResourceFactory.getResource(
                ResourceFactory.getInstance().getResourceEntry(ref.getResourceName()));
        new ViewFrame(getTopLevelAncestor(), res);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class MemSpellTableModel extends AbstractTableModel implements TableModelListener
  {
    private final List<MemSpellTableEntry> list = new ArrayList<MemSpellTableEntry>();
    private final CreResource cre;

    private MemSpellTableModel(CreResource cre)
    {
      this.cre = cre;
      updateTable();
      cre.addTableModelListener(this);
    }

    private void updateTable()
    {
      list.clear();
      for (int i = 0; i < cre.getRowCount(); i++) {
        StructEntry o = cre.getStructEntryAt(i);
        if (o instanceof SpellMemorization) {
          SpellMemorization inf = (SpellMemorization)o;
          int lvl = ((DecNumber)inf.getAttribute("Spell level")).getValue();
          for (int j = 0; j < inf.getRowCount(); j++) {
            StructEntry p = inf.getStructEntryAt(j);
            if (p instanceof MemorizedSpells) {
              MemorizedSpells spell = (MemorizedSpells)p;
              addSpell(lvl, (ResourceRef)spell.getAttribute("Spell"));
            }
          }
        }
      }
    }

    private void addSpell(int lvl, ResourceRef spell)
    {
      for (int i = 0; i < list.size(); i++) {
        MemSpellTableEntry entry = list.get(i);
        if (spell.getResourceName().equalsIgnoreCase(entry.spell.getResourceName())) {
          entry.count++;
          return;
        }
      }
      MemSpellTableEntry entry = new MemSpellTableEntry(lvl, spell);
      list.add(entry);
    }

    public void tableChanged(TableModelEvent e)
    {
      updateTable();
      fireTableStructureChanged();
    }

    public int getRowCount()
    {
      return list.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      MemSpellTableEntry entry = list.get(rowIndex);
      if (columnIndex == 0)
        return new Integer(entry.lvl + 1);
      else if (columnIndex == 1)
        return new Integer(entry.count + 1);
      else
        return entry.spell;
    }

    public int getColumnCount()
    {
      return 3;
    }

    public String getColumnName(int column)
    {
      if (column == 0)
        return "Level";
      else if (column == 1)
        return "#";
      return "Spell";
    }
  }

  private static final class MemSpellTableEntry
  {
    private final int lvl;
    private int count;
    private final ResourceRef spell;

    private MemSpellTableEntry(int lvl, ResourceRef spell)
    {
      this.lvl = lvl;
      this.spell = spell;
    }
  }
}

