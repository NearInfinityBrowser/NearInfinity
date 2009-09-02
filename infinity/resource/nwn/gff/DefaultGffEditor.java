// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn.gff;

import infinity.resource.ResourceFactory;
import infinity.resource.nwn.gff.field.*;
import infinity.icon.Icons;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import java.awt.event.*;
import java.awt.*;
import java.util.*;
import java.util.List;

final class DefaultGffEditor extends JPanel implements ActionListener
{
  private static final ImageIcon closed = Icons.getIcon("Forward16.gif");
  private static final ImageIcon open = Icons.getIcon("Down16.gif");
  private final GffResource resource;
  private final JButton bSave;
  private final JButton bExport;

  DefaultGffEditor(GffResource resource)
  {
    this.resource = resource;

    bExport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bSave = new JButton("Save", Icons.getIcon("Save16.gif"));
    bExport.addActionListener(this);
    bSave.addActionListener(this);

    JPanel bpanel = new JPanel();
    bpanel.setLayout(new GridLayout(1, 0, 6, 0));
    bpanel.add(bExport);
    bpanel.add(bSave);

    JPanel lowerpanel = new JPanel();
    lowerpanel.setLayout(new FlowLayout(FlowLayout.CENTER));
    lowerpanel.add(bpanel);

    JTable table = new JTable(new GffTableModel(resource));
    table.getColumnModel().getColumn(0).setMaxWidth(20);
    table.getColumnModel().getColumn(1).setPreferredWidth(100);
    table.getColumnModel().getColumn(2).setPreferredWidth(400);
    table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));
    table.addMouseListener(new GffTableMouseListener());

    setLayout(new BorderLayout());
    add(new JScrollPane(table), BorderLayout.CENTER);
    add(lowerpanel, BorderLayout.SOUTH);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bExport)
      ResourceFactory.getInstance().exportResource(resource.getResourceEntry(), getTopLevelAncestor());
    else if (event.getSource() == bSave)
      ResourceFactory.getInstance().saveResource(resource, getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class GffTableMouseListener extends MouseAdapter
  {
    public void mouseClicked(MouseEvent event)
    {
      JTable table = (JTable)event.getSource();
      int row = table.rowAtPoint(event.getPoint());
      int col = table.columnAtPoint(event.getPoint());
      Object value = table.getValueAt(row, col);
      if (value == closed)
        ((GffTableModel)table.getModel()).expandRow(row);
      else if (value == open)
        ((GffTableModel)table.getModel()).collapseRow(row);
    }
  }

  private static final class GffTableModel implements TableModel
  {
    private final List<TableModelListener> listeners = new ArrayList<TableModelListener>();
    private final List<GffField> shownFields = new ArrayList<GffField>();
    private final List<GffField> expandedFields = new ArrayList<GffField>();

    private GffTableModel(GffResource resource)
    {
      shownFields.addAll(resource.getTopStruct().getChildren());
    }

    private void expandRow(int row)
    {
      GffField field = shownFields.get(row);
      expandedFields.add(field);
      shownFields.addAll(row + 1, field.getChildren());
      TableModelEvent event = new TableModelEvent(this);
      for (int i = 0; i < listeners.size(); i++)
        listeners.get(i).tableChanged(event);
    }

    private void collapseRow(int row)
    {
      GffField field = shownFields.get(row);
      collapseRecursive(field);
      TableModelEvent event = new TableModelEvent(this);
      for (int i = 0; i < listeners.size(); i++)
        listeners.get(i).tableChanged(event);
    }

    private void collapseRecursive(GffField field)
    {
      expandedFields.remove(field);
      List children = field.getChildren();
      for (int i = 0; i < children.size(); i++) {
        Object o = children.get(i);
        shownFields.remove(o);
        if (expandedFields.contains(o))
          collapseRecursive((GffField)o);
      }
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
      GffField field = shownFields.get(rowIndex);
      if (columnIndex == 0) {
        if (field instanceof GffStruct || field instanceof GffList) {
          if (expandedFields.contains(field))
            return open;
          return closed;
        }
        return null;
      }
      else if (columnIndex == 1)
        return field.getLabel();
      return field.getValue();
    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex)
    {
      //To change body of implemented methods use File | Settings | File Templates.
    }

    public int getRowCount()
    {
      return shownFields.size();
    }

    public int getColumnCount()
    {
      return 3;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
      return columnIndex == 2;
    }

    public Class getColumnClass(int columnIndex)
    {
      if (columnIndex == 0)
        return ImageIcon.class;
      return Object.class;
    }

    public String getColumnName(int columnIndex)
    {
      if (columnIndex == 0)
        return " ";
      else if (columnIndex == 1)
        return "Label";
      return "Value";
    }

    public void addTableModelListener(TableModelListener l)
    {
      listeners.add(l);
    }

    public void removeTableModelListener(TableModelListener l)
    {
      listeners.remove(l);
    }
  }
}

