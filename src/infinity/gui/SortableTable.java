// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.icon.Icons;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public final class SortableTable extends JTable
{
  private final SortableTableModel tableModel;
  private boolean sortAscending;
  private int sortByColumn;

  public SortableTable(String[] columnNames, Class[] columnClasses, int[] columnWidths)
  {
    tableModel = new SortableTableModel(columnNames, columnClasses);
    setModel(tableModel);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setDefaultRenderer(Object.class, new ToolTipTableCellRenderer());
    getTableHeader().setDefaultRenderer(new TableHeaderRenderer());
    getTableHeader().addMouseListener(new MouseAdapter()
    {
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
    for (int i = 0; i < columnWidths.length; i++)
      getColumnModel().getColumn(i).setPreferredWidth(columnWidths[i]);
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
    private final Class[] columnClasses;
    private final String[] columnNames;

    private SortableTableModel(String[] columnNames, Class[] columnClasses)
    {
      this.columnNames = columnNames;
      this.columnClasses = columnClasses;
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

    public Class getColumnClass(int columnIndex)
    {
      return columnClasses[columnIndex];
    }

    public int getColumnCount()
    {
      return columnClasses.length;
    }

    public String getColumnName(int columnIndex)
    {
      return columnNames[columnIndex];
    }

    public int getRowCount()
    {
      return tableItems.size();
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      return tableItems.get(rowIndex).getObjectAt(columnIndex);
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return false;
    }

    public void addTableModelListener(TableModelListener l)
    {
      listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l)
    {
      listeners.remove(l);
    }

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

