// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.infinity.icon.Icons;

public final class SortableTable extends JTable
{
  private final SortableTableModel tableModel;
  private boolean sortAscending;
  private int sortByColumn;

  public SortableTable(List<String> columnNames, List<Class<? extends Object>> columnClasses, List<Integer> columnWidths)
  {
    tableModel = new SortableTableModel(columnNames, columnClasses);
    setModel(tableModel);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setDefaultRenderer(Object.class, new ToolTipTableCellRenderer());
    getTableHeader().setDefaultRenderer(new TableHeaderRenderer());
    getTableHeader().addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        TableColumnModel columnModel = getColumnModel();
        int viewColumn = columnModel.getColumnIndexAtX(event.getX());
        int column = convertColumnIndexToModel(viewColumn);
        if (column == sortByColumn)
          sortAscending = !sortAscending;
        else {
          sortByColumn = column;
          sortAscending = false;
        }
        getTableHeader().repaint();
        tableModel.sort();
      }
    });
    for (int i = 0; i < columnWidths.size(); i++)
      getColumnModel().getColumn(i).setPreferredWidth(columnWidths.get(i));
  }

  public void addTableItem(TableItem item)
  {
    tableModel.addTableItem(item);
  }

  public void clear()
  {
    tableModel.clear();
  }

  public TableItem getTableItemAt(int rowIndex)
  {
    return tableModel.getTableItemAt(rowIndex);
  }

  public void tableComplete()
  {
    tableModel.sort();
  }

  public void tableComplete(int sortByColumn)
  {
    this.sortByColumn = sortByColumn;
    tableModel.sort();
  }

// -------------------------- INNER CLASSES --------------------------

  private final class TableHeaderRenderer extends DefaultTableCellRenderer
  {
    private TableHeaderRenderer()
    {
      setHorizontalTextPosition(DefaultTableCellRenderer.LEFT);
      setBorder(UIManager.getBorder("TableHeader.cellBorder"));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column)
    {
      if (table != null) {
        JTableHeader header = table.getTableHeader();
        if (header != null) {
          setForeground(header.getForeground());
          setBackground(header.getBackground());
          setFont(header.getFont());
          setText(' ' + table.getModel().getColumnName(column));
        }
      }

      if (sortByColumn == column)
        setIcon(sortAscending ? Icons.getIcon("Up16.gif") : Icons.getIcon("Down16.gif"));
      else
        setIcon(null);
      return this;
    }
  }

  private final class SortableTableModel implements TableModel, Comparator<TableItem>
  {
    private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();
    private final List<TableItem> tableItems = new ArrayList<TableItem>();
    private final List<Class<? extends Object>> columnClasses;
    private final List<String> columnNames;

    private SortableTableModel(List<String> columnNames, List<Class<? extends Object>> columnClasses)
    {
      if (columnNames != null) {
        this.columnNames = columnNames;
      } else {
        this.columnNames = new ArrayList<String>();
      }
      if (columnClasses != null) {
        this.columnClasses = columnClasses;
      } else {
        this.columnClasses = new ArrayList<Class<? extends Object>>();
      }
    }

    private void addTableItem(TableItem item)
    {
      tableItems.add(item);
    }

    private TableItem getTableItemAt(int rowIndex)
    {
      return tableItems.get(rowIndex);
    }

    public void sort()
    {
      Collections.sort(tableItems, this);
      TableModelEvent event = new TableModelEvent(this);
      for (int i = 0; i < listeners.size(); i++)
        listeners.get(i).tableChanged(event);
    }

    private void clear()
    {
      tableItems.clear();
      TableModelEvent event = new TableModelEvent(this);
      for (int i = 0; i < listeners.size(); i++)
        listeners.get(i).tableChanged(event);
    }

    @Override
    public Class<? extends Object> getColumnClass(int columnIndex)
    {
      return columnClasses.get(columnIndex);
    }

    @Override
    public int getColumnCount()
    {
      return columnClasses.size();
    }

    @Override
    public String getColumnName(int columnIndex)
    {
      return columnNames.get(columnIndex);
    }

    @Override
    public int getRowCount()
    {
      return tableItems.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      return tableItems.get(rowIndex).getObjectAt(columnIndex);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return false;
    }

    @Override
    public void addTableModelListener(TableModelListener l)
    {
      listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l)
    {
      listeners.remove(l);
    }

    @Override
    public int compare(TableItem o1, TableItem o2)
    {
      int res;
      if (getColumnClass(sortByColumn) == Integer.class) {
        Integer int1 = (Integer)o1.getObjectAt(sortByColumn);
        Integer int2 = (Integer)o2.getObjectAt(sortByColumn);
        res = int1.compareTo(int2);
      }
      else {
        String string1 = o1.getObjectAt(sortByColumn).toString();
        String string2 = o2.getObjectAt(sortByColumn).toString();
        res = string1.compareToIgnoreCase(string2);
      }
      if (sortAscending)
        res = -res;
      return res;
    }
  }
}

