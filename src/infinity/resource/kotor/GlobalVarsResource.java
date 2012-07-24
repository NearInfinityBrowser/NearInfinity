// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.kotor;

import infinity.resource.nwn.gff.GffResource;
import infinity.resource.nwn.gff.field.*;
import infinity.resource.key.ResourceEntry;
import infinity.resource.ViewableContainer;
import infinity.resource.ResourceFactory;
import infinity.icon.Icons;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;

public final class GlobalVarsResource extends GffResource implements ActionListener
{
  private final GffList catBoolean, catNumber, catLocation, catString, valString;
  private final GffVoid valBoolean, valNumber, valLocation;
  private JButton bSave, bExport;
  private JPanel panel;

  public GlobalVarsResource(ResourceEntry entry) throws Exception
  {
    super(entry);
    GffStruct topStruct = getTopStruct();
    catBoolean = (GffList)topStruct.getField("CatBoolean");
    catNumber = (GffList)topStruct.getField("CatNumber");
    catLocation = (GffList)topStruct.getField("CatLocation");
    catString = (GffList)topStruct.getField("CatString");
    valBoolean = (GffVoid)topStruct.getField("ValBoolean");
    valNumber = (GffVoid)topStruct.getField("ValNumber");
    valLocation = (GffVoid)topStruct.getField("ValLocation");
    valString = (GffList)topStruct.getField("ValString");
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bExport)
      ResourceFactory.getInstance().exportResource(getResourceEntry(), panel.getTopLevelAncestor());
    else if (event.getSource() == bSave)
      ResourceFactory.getInstance().saveResource(this, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
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

    JTable tableBoolean = new JTable(new BooleanTableModel(catBoolean, valBoolean));
    JTable tableNumber = new JTable(new NumberTableModel(catNumber, valNumber));
    JTable tableLocation = new JTable();
    JTable tableString = new JTable(new StringTableModel(catString, valString));

    JPanel panel1 = new JPanel(new BorderLayout());
    panel1.add(new JLabel("Boolean values"), BorderLayout.NORTH);
    panel1.add(new JScrollPane(tableBoolean), BorderLayout.CENTER);

    JPanel panel2 = new JPanel(new BorderLayout());
    panel2.add(new JLabel("Numberic values"), BorderLayout.NORTH);
    panel2.add(new JScrollPane(tableNumber), BorderLayout.CENTER);

    JPanel panel3 = new JPanel(new BorderLayout());
    panel3.add(new JLabel("Locations"), BorderLayout.NORTH);
    panel3.add(new JScrollPane(tableLocation), BorderLayout.CENTER);

    JPanel panel4 = new JPanel(new BorderLayout());
    panel4.add(new JLabel("Strings"), BorderLayout.NORTH);
    panel4.add(new JScrollPane(tableString), BorderLayout.CENTER);

    JPanel centerPanel = new JPanel(new GridLayout(2,2,3,3));
    centerPanel.add(panel1);
    centerPanel.add(panel2);
    centerPanel.add(panel3);
    centerPanel.add(panel4);
    centerPanel.setBorder(BorderFactory.createEmptyBorder(3,3,3,3));

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(centerPanel, BorderLayout.CENTER);
    panel.add(lowerpanel, BorderLayout.SOUTH);

    return panel;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class BooleanTableModel extends DefaultTableModel
  {
    private final GffList catBoolean;
    private final GffVoid valBoolean;

    private BooleanTableModel(GffList catBoolean, GffVoid valBoolean)
    {
      super(new String[] { "Variable", "True?" }, catBoolean.getChildren().size());
      this.catBoolean = catBoolean;
      this.valBoolean = valBoolean;
    }

    public Object getValueAt(int row, int column)
    {
      if (column == 0) {
        GffStruct struct = (GffStruct)catBoolean.getChildren().get(row);
        GffExoString string = (GffExoString)struct.getField("Name");
        return string.getValue();
      }
      else {
        int bit = 7 - (row - row / 8 * 8);
        byte data[] = (byte[])valBoolean.getValue();
        if ((data[row / 8] & (int)Math.pow(2, bit)) == Math.pow(2, bit))
          return Boolean.TRUE;
        return Boolean.FALSE;
      }
    }

    public void setValueAt(Object aValue, int row, int column)
    {
      if (column == 1) {
        int bit = 7 - (row - row / 8 * 8);
        byte data[] = (byte[])valBoolean.getValue();
        data[row / 8] = (byte)(data[row / 8] ^ (int)Math.pow(2, bit));
      }
    }

    public boolean isCellEditable(int row, int column)
    {
      return column == 1;
    }

    public Class getColumnClass(int columnIndex)
    {
      if (columnIndex == 0)
        return String.class;
      return Boolean.class;
    }
  }

  private static final class NumberTableModel extends DefaultTableModel
  {
    private final GffList catNumber;
    private final GffVoid valNumber;

    private NumberTableModel(GffList catNumber, GffVoid valNumber)
    {
      super(new String[] { "Variable", "Value" }, catNumber.getChildren().size());
      this.catNumber = catNumber;
      this.valNumber = valNumber;
    }

    public Object getValueAt(int row, int column)
    {
      if (column == 0) {
        GffStruct struct = (GffStruct)catNumber.getChildren().get(row);
        GffExoString string = (GffExoString)struct.getField("Name");
        return string.getValue();
      }
      else {
        byte data[] = (byte[])valNumber.getValue();
        return new Byte(data[row]);
      }
    }

    public void setValueAt(Object aValue, int row, int column)
    {
      if (column == 1) {
        byte data[] = (byte[])valNumber.getValue();
        data[row] = ((Integer)aValue).byteValue();
      }
    }

    public boolean isCellEditable(int row, int column)
    {
      return column == 1;
    }

    public Class getColumnClass(int columnIndex)
    {
      if (columnIndex == 0)
        return String.class;
      return Integer.class;
    }
  }

  private static final class StringTableModel extends DefaultTableModel
  {
    private final GffList catString, valString;

    private StringTableModel(GffList catString, GffList valString)
    {
      super(new String[] { "Variable", "Value" }, catString.getChildren().size());
      this.catString = catString;
      this.valString = valString;
    }

    public Object getValueAt(int row, int column)
    {
      if (column == 0) {
        GffStruct struct = (GffStruct)catString.getChildren().get(row);
        GffExoString string = (GffExoString)struct.getField("Name");
        return string.getValue();
      }
      else {
        GffStruct struct = (GffStruct)valString.getChildren().get(row);
        GffExoString string = (GffExoString)struct.getField("String");
        return string.getValue();
      }
    }

    public boolean isCellEditable(int row, int column)
    {
      return column == 1;
    }
  }
}

