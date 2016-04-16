// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ToolTipTableCellRenderer;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;

final class ViewerSpells extends JPanel implements ActionListener
{
  private final JButton bOpen = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
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
      @Override
      public void mouseClicked(MouseEvent e)
      {
        if (e.getClickCount() == 2 && table.getSelectedRowCount() == 1) {
          ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 2);
          if (ref != null) {
            Resource res = ResourceFactory.getResource(
                    ResourceFactory.getResourceEntry(ref.getResourceName()));
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

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen) {
      ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 2);
      if (ref != null) {
        Resource res = ResourceFactory.getResource(
                ResourceFactory.getResourceEntry(ref.getResourceName()));
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
      for (int i = 0; i < cre.getFieldCount(); i++) {
        StructEntry o = cre.getField(i);
        if (o instanceof SpellMemorization) {
          SpellMemorization inf = (SpellMemorization)o;
          int lvl = ((DecNumber)inf.getAttribute(SpellMemorization.CRE_MEMORIZATION_LEVEL)).getValue();
          for (int j = 0; j < inf.getFieldCount(); j++) {
            StructEntry p = inf.getField(j);
            if (p instanceof MemorizedSpells) {
              MemorizedSpells spell = (MemorizedSpells)p;
              addSpell(lvl, (ResourceRef)spell.getAttribute(MemorizedSpells.CRE_MEMORIZED_RESREF));
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

    @Override
    public void tableChanged(TableModelEvent e)
    {
      updateTable();
      fireTableStructureChanged();
    }

    @Override
    public int getRowCount()
    {
      return list.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      MemSpellTableEntry entry = list.get(rowIndex);
      switch (columnIndex) {
        case 0:  return new Integer(entry.lvl + 1);
        case 1:  return new Integer(entry.count + 1);
        default: return entry.spell;
      }
    }

    @Override
    public int getColumnCount()
    {
      return 3;
    }

    @Override
    public String getColumnName(int column)
    {
      switch (column) {
        case 0:  return "Level";
        case 1:  return "#";
        default: return "Spell";
      }
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

