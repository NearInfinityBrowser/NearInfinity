// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.cre;

import infinity.datatype.*;
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

final class ViewerItems extends JPanel implements ActionListener, ListSelectionListener, TableModelListener
{
  private final InventoryTableModel tableModel = new InventoryTableModel();
  private final JButton bOpen;
  private final JTable table;
  private final List<StructEntry> slots = new ArrayList<StructEntry>();

  ViewerItems(CreResource cre)
  {
    super(new BorderLayout(0, 3));
    List<Item> items = new ArrayList<Item>();
    HexNumber slots_offset = (HexNumber)cre.getAttribute("Item slots offset");
    for (int i = 0; i < cre.getRowCount(); i++) {
      StructEntry entry = cre.getStructEntryAt(i);
      if (entry instanceof Item)
        items.add((Item)entry);
      else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals("Weapon slot selected")
               && !entry.getName().equals("Weapon ability selected"))
        slots.add(entry);
    }
    for (int i = 0; i < slots.size(); i++) {
      DecNumber slot = (DecNumber)slots.get(i);
      if (slot.getValue() >= 0 && slot.getValue() < items.size()) {
        Item item = items.get(slot.getValue());
        ResourceRef itemRef = (ResourceRef)item.getAttribute("Item");
        tableModel.addEntry(slot.getName(), itemRef);
      }
      else
        tableModel.addEntry(slot.getName(), null);
    }
    table = new JTable(tableModel);
    table.setDefaultRenderer(Object.class, new ToolTipTableCellRenderer());
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    table.getColumnModel().getColumn(0).setMaxWidth(175);
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
    JScrollPane scroll = new JScrollPane(table);
    bOpen = new JButton("View/Edit", Icons.getIcon("Zoom16.gif"));
    bOpen.addActionListener(this);
//    bOpen.setEnabled(tableModel.getRowCount() > 0);
    table.getSelectionModel().setSelectionInterval(0, 0);
    add(new JLabel("Items"), BorderLayout.NORTH);
    add(scroll, BorderLayout.CENTER);
    add(bOpen, BorderLayout.SOUTH);
    cre.addTableModelListener(this);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen) {
      ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 1);
      if (ref != null) {
        Resource res = ResourceFactory.getResource(
                ResourceFactory.getInstance().getResourceEntry(ref.getResourceName()));
        new ViewFrame(getTopLevelAncestor(), res);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (table.getSelectedRow() == -1)
      bOpen.setEnabled(false);
    else
      bOpen.setEnabled(tableModel.getValueAt(table.getSelectedRow(), 1) != null);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  public void tableChanged(TableModelEvent event)
  {
    if (event.getType() == TableModelEvent.UPDATE) {
      CreResource cre = (CreResource)event.getSource();
      Object changed = cre.getStructEntryAt(event.getFirstRow());
      if (slots.contains(changed)) {
        List<Item> items = new ArrayList<Item>();
        for (int i = 0; i < cre.getRowCount(); i++) {
          StructEntry entry = cre.getStructEntryAt(i);
          if (entry instanceof Item)
            items.add((Item)entry);
        }
        table.clearSelection();
        tableModel.clear();
        for (int i = 0; i < slots.size(); i++) {
          DecNumber slot = (DecNumber)slots.get(i);
          if (slot.getValue() >= 0 && slot.getValue() < items.size()) {
            Item item = items.get(slot.getValue());
            ResourceRef itemRef = (ResourceRef)item.getAttribute("Item");
            tableModel.addEntry(slot.getName(), itemRef);
          }
          else
            tableModel.addEntry(slot.getName(), null);
        }
        tableModel.fireTableDataChanged();
        table.getSelectionModel().setSelectionInterval(0, 0);
      }
    }
  }

// --------------------- End Interface TableModelListener ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class InventoryTableModel extends AbstractTableModel
  {
    private final List<InventoryTableEntry> list = new ArrayList<InventoryTableEntry>();

    private InventoryTableModel()
    {
    }

    private void clear()
    {
      list.clear();
    }

    private void addEntry(String slot, ResourceRef item)
    {
      list.add(new InventoryTableEntry(slot, item));
    }

    public int getRowCount()
    {
      return list.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      InventoryTableEntry entry = list.get(rowIndex);
      if (columnIndex == 0)
        return entry.slot;
      return entry.item;
    }

    public int getColumnCount()
    {
      return 2;
    }

    public String getColumnName(int column)
    {
      if (column == 0)
        return "Slot";
      return "Item";
    }
  }

  private static final class InventoryTableEntry
  {
    private final String slot;
    private final ResourceRef item;

    private InventoryTableEntry(String slot, ResourceRef item)
    {
      this.slot = slot;
      this.item = item;
    }
  }
}

