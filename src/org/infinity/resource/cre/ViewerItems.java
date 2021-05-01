// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ToolTipTableCellRenderer;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.util.Misc;

final class ViewerItems extends JPanel implements ActionListener, ListSelectionListener, TableModelListener
{
  private final InventoryTableModel tableModel = new InventoryTableModel();
  private final JButton bOpen;
  private final JTable table;
  private final List<StructEntry> slots = new ArrayList<>();

  ViewerItems(CreResource cre)
  {
    super(new BorderLayout(0, 3));
    final List<Item> items = new ArrayList<>();
    IsNumeric slots_offset = (IsNumeric)cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS);
    for (final StructEntry entry : cre.getFields()) {
      if (entry instanceof Item)
        items.add((Item)entry);
      else if (entry.getOffset() >= slots_offset.getValue() + cre.getOffset() &&
               entry instanceof DecNumber
               && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_SLOT)
               && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_ABILITY))
        slots.add(entry);
    }
    String selectedSlotName = getSelectedWeaponSlot(cre);
    slots.forEach((e) -> {
      IsNumeric slot = (IsNumeric)e;
      String slotName = e.getName().equals(selectedSlotName) ? "*" + e.getName() : e.getName();
      if (slot.getValue() >= 0 && slot.getValue() < items.size()) {
        Item item = items.get(slot.getValue());
        ResourceRef itemRef = (ResourceRef)item.getAttribute(Item.CRE_ITEM_RESREF);
        tableModel.addEntry(slotName, itemRef);
      } else {
        tableModel.addEntry(slotName, null);
      }
    });
    table = new JTable(tableModel);
    table.setDefaultRenderer(Object.class, new ToolTipTableCellRenderer());
    ((DefaultTableCellRenderer)table.getTableHeader().getDefaultRenderer()).setHorizontalAlignment(JLabel.LEFT);
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    // calculating optimal width of slot name column
    int maxWidth = 0;
    for (int i = 0, cnt = table.getModel().getRowCount(); i < cnt; i++) {
      String text = table.getModel().getValueAt(i, 0).toString();
      int width = table.getFontMetrics(table.getFont()).stringWidth(text);
      maxWidth = Math.max(width, maxWidth);
    }
    maxWidth = Math.max(maxWidth, table.getColumnModel().getColumn(0).getPreferredWidth());
    table.getColumnModel().getColumn(0).setPreferredWidth(maxWidth + 8);
    table.getColumnModel().getColumn(0).setMaxWidth(maxWidth + 100);
    table.setRowHeight(Misc.getFontHeight(table.getGraphics(), table.getFont()));
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
    JScrollPane scroll = new JScrollPane(table);
    bOpen = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
    bOpen.addActionListener(this);
//    bOpen.setEnabled(tableModel.getRowCount() > 0);
    table.getSelectionModel().setSelectionInterval(0, 0);
    add(new JLabel("Items"), BorderLayout.NORTH);
    add(scroll, BorderLayout.CENTER);
    add(bOpen, BorderLayout.SOUTH);
    cre.addTableModelListener(this);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOpen) {
      ResourceRef ref = (ResourceRef)tableModel.getValueAt(table.getSelectedRow(), 1);
      if (ref != null) {
        Resource res = ResourceFactory.getResource(
                ResourceFactory.getResourceEntry(ref.getResourceName()));
        new ViewFrame(getTopLevelAncestor(), res);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (table.getSelectedRow() == -1)
      bOpen.setEnabled(false);
    else
      bOpen.setEnabled(tableModel.getValueAt(table.getSelectedRow(), 1) != null);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface TableModelListener ---------------------

  @Override
  public void tableChanged(TableModelEvent event)
  {
    if (event.getType() == TableModelEvent.UPDATE) {
      CreResource cre = (CreResource)event.getSource();
      final StructEntry changed = cre.getFields().get(event.getFirstRow());
      if (slots.contains(changed)) {
        final List<Item> items = new ArrayList<>();
        for (final StructEntry entry : cre.getFields()) {
          if (entry instanceof Item)
            items.add((Item)entry);
        }
        table.clearSelection();
        tableModel.clear();
        String selectedSlotName = getSelectedWeaponSlot(cre);
        slots.forEach((e) -> {
          IsNumeric slot = (IsNumeric)e;
          String slotName = e.getName().equals(selectedSlotName) ? "*" + e.getName() : e.getName();
          if (slot.getValue() >= 0 && slot.getValue() < items.size()) {
            Item item = items.get(slot.getValue());
            ResourceRef itemRef = (ResourceRef)item.getAttribute(Item.CRE_ITEM_RESREF);
            tableModel.addEntry(slotName, itemRef);
          } else {
            tableModel.addEntry(slotName, null);
          }
        });
        tableModel.fireTableDataChanged();
        table.getSelectionModel().setSelectionInterval(0, 0);
      }
    }
  }

// --------------------- End Interface TableModelListener ---------------------

  private String getSelectedWeaponSlot(CreResource cre)
  {
    String retVal = "";
    int slotSelected = ((IsNumeric)cre.getAttribute(CreResource.CRE_SELECTED_WEAPON_SLOT)).getValue();
    if (slotSelected >= 0 && slotSelected < 1000) {
      StructEntry e = cre.getAttribute(String.format(CreResource.CRE_ITEM_SLOT_WEAPON_FMT, slotSelected + 1));
      if (e != null) {
        retVal = e.getName();
      }
    }
    return retVal;
  }


// -------------------------- INNER CLASSES --------------------------

  private static final class InventoryTableModel extends AbstractTableModel
  {
    private final List<InventoryTableEntry> list = new ArrayList<>();

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

    @Override
    public int getRowCount()
    {
      return list.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      InventoryTableEntry entry = list.get(rowIndex);
      return (columnIndex == 0) ? entry.slot : entry.item;
    }

    @Override
    public int getColumnCount()
    {
      return 2;
    }

    @Override
    public String getColumnName(int column)
    {
      return (column == 0) ? "Slot" : "Item";
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
