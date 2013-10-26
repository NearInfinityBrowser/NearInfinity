// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn;

import infinity.resource.Resource;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

@Deprecated
public final class TableTextResource implements Resource
{
  private final List<String> columnNames = new ArrayList<String>();
  private final ResourceEntry entry;
  private final String fileTypeVersion;
  private final String stringData[][];
  private final byte buffer[];

  public TableTextResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    buffer = entry.getResourceData();

    fileTypeVersion = new String(buffer, 0, 8);
    int index = 9;
    while (buffer[index] != 0) {
      int endIndex = index + 1;
      while (buffer[endIndex] != 0x09)
        endIndex++;
      columnNames.add(new String(buffer, index, endIndex - index));
      index = endIndex + 1;
    }

    int rowCount = DynamicArray.getInt(buffer, ++index);
    stringData = new String[columnNames.size() + 1][rowCount];
    index += 4;

    for (int row = 0; row < rowCount; row++) {
      int endIndex = index + 1;
      while (buffer[endIndex] != 0x09)
        endIndex++;
      stringData[0][row] = new String(buffer, index, endIndex - index);
      index = endIndex + 1;
    }

    int stringOffsets[][] = new int[columnNames.size()][rowCount];
    for (int row = 0; row < rowCount; row++)
      for (int col = 0; col < columnNames.size(); col++)
        stringOffsets[col][row] = DynamicArray.getShort(buffer, index + row * columnNames.size() * 2 + col * 2);

    index += rowCount * columnNames.size() * 2;
    int unknown1 = DynamicArray.getShort(buffer, index);
    index += 2;

    for (int row = 0; row < rowCount; row++)
      for (int col = 1; col <= columnNames.size(); col++) {
        int startIndex = index + stringOffsets[col-1][row];
        if (buffer[startIndex] == 0x00)
          stringData[col][row] = "";
        else {
          int endIndex = startIndex + 1;
          while (buffer[endIndex] != 0x00)
            endIndex++;
          stringData[col][row] = new String(buffer, startIndex, endIndex - startIndex);
        }
      }
  }

// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    TextTableModel tableModel = new TextTableModel();
    JTable table = new JTable(tableModel);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
    return new JScrollPane(table);
  }

// --------------------- End Interface Viewable ---------------------


// -------------------------- INNER CLASSES --------------------------

  private final class TextTableModel extends AbstractTableModel
  {
    @Override
    public String getColumnName(int column)
    {
      if (column == 0)
        return fileTypeVersion;
      return columnNames.get(column - 1);
    }

    @Override
    public int getColumnCount()
    {
      return columnNames.size() + 1;
    }

    @Override
    public int getRowCount()
    {
      return stringData[0].length;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex)
    {
      return stringData[columnIndex][rowIndex];
    }
  }
}

