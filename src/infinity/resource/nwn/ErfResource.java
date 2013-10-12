// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.nwn;

import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;
import infinity.util.NIFile;

import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ErfResource implements Resource, ActionListener, ListSelectionListener
{
  private final ERFKey keys[];
  private final LocalizedString strings[];
  private final ResourceEntry entry;
  private final byte buffer[];
  private JButton bView;
  private SortableTable table;

  public ErfResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    buffer = entry.getResourceData();

    // Read header
    String fileType = new String(buffer, 0, 4);
    String version = new String(buffer, 4, 4);
    int languageCount = DynamicArray.getInt(buffer, 8);
    int localizedStringSize = DynamicArray.getInt(buffer, 12);
    int entryCount = DynamicArray.getInt(buffer, 16);
    int offsetToLocalizedString = DynamicArray.getInt(buffer, 20);
    int offsetToKeyList = DynamicArray.getInt(buffer, 24);
    int offsetToResourceList = DynamicArray.getInt(buffer, 28);
    int buildYear = DynamicArray.getInt(buffer, 32);
    int buildDay = DynamicArray.getInt(buffer, 36);
    int descriptionStrRef = DynamicArray.getInt(buffer, 40);
    // 116 reserved bytes

    // Read localized strings
    strings = new LocalizedString[languageCount];
    int offset = offsetToLocalizedString;
    for (int i = 0; i < strings.length; i++) {
      strings[i] = new LocalizedString(buffer, offset);
      offset += 8 + strings[i].stringSize;
    }

    // Read resource info
    keys = new ERFKey[entryCount];
    for (int i = 0; i < keys.length; i++)
      keys[i] = new ERFKey(buffer, offsetToKeyList + i * 24, offsetToResourceList + i * 8);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bView) {
      ERFKey key = (ERFKey)table.getTableItemAt(table.getSelectedRow());
      ResourceEntry resourceEntry = new ERFResourceEntry(key);
      new ViewFrame(table.getTopLevelAncestor(), ResourceFactory.getResource(resourceEntry));
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    bView.setEnabled(table.getSelectedRowCount() == 1);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(2);
    colClasses.add(ImageIcon.class); colClasses.add(String.class);
    table = new SortableTable(Arrays.asList(new String[]{"", "Resource name"}),
                              colClasses, Arrays.asList(new Integer[]{5, 300}));

    for (final ERFKey key : keys)
      table.addTableItem(key);
    table.tableComplete(1);
    table.getSelectionModel().addListSelectionListener(this);
    table.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          ERFKey key = (ERFKey)table.getTableItemAt(table.getSelectedRow());
          ResourceEntry resourceEntry = new ERFResourceEntry(key);
          new ViewFrame(table.getTopLevelAncestor(), ResourceFactory.getResource(resourceEntry));
        }
      }
    });

    bView = new JButton("View", Icons.getIcon("Zoom16.gif"));
    bView.setMnemonic('v');
    bView.setEnabled(false);
    bView.addActionListener(this);

    JPanel centerpanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    centerpanel.setLayout(gbl);

    JLabel label = new JLabel("Contents of " + entry.toString());
    JScrollPane scroll = new JScrollPane(table);

    gbc.weightx = 0.0;
    gbc.weighty = 0.0;
    gbc.insets = new Insets(6, 0, 0, 0);
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(label, gbc);
    centerpanel.add(label);

    gbc.insets.top = 3;
    gbc.weightx = 0.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(scroll, gbc);
    centerpanel.add(scroll);

    gbc.weighty = 0.0;
    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bView);

    JPanel panel = new JPanel(new BorderLayout());
    panel.add(centerpanel, BorderLayout.CENTER);
    panel.add(bpanel, BorderLayout.SOUTH);
    centerpanel.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class LocalizedString
  {
    final int languageID;
    final int stringSize;
    final String string;

    private LocalizedString(byte buffer[], int offset)
    {
      languageID = DynamicArray.getInt(buffer, offset);
      stringSize = DynamicArray.getInt(buffer, offset + 4);
      string = DynamicArray.getString(buffer, offset + 8, stringSize);
    }
  }

  private static final class ERFKey implements TableItem
  {
    final String resRef;
    final int resID;
    final int resType;
    final int offsetToResource;
    final int resourceSize;

    private ERFKey(byte buffer[], int offset1, int offset2)
    {
      resRef = DynamicArray.getString(buffer, offset1, 16);
      resID = DynamicArray.getInt(buffer, offset1 + 16);
      resType = DynamicArray.getShort(buffer, offset1 + 20);
      // 2 unused bytes

      offsetToResource = DynamicArray.getInt(buffer, offset2);
      resourceSize = DynamicArray.getInt(buffer, offset2 + 4);
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return ResourceFactory.getKeyfile().getIcon(ResourceFactory.getKeyfile().getExtension(resType));
      return resRef + '.' + ResourceFactory.getKeyfile().getExtension(resType);
    }
  }

  private final class ERFResourceEntry extends ResourceEntry
  {
    private final ERFKey key;

    ERFResourceEntry(ERFKey key)
    {
      this.key = key;
    }

    public String getExtension()
    {
      String ext = ResourceFactory.getKeyfile().getExtension(key.resType);
      if (ext == null)
        return String.valueOf(key.resType);
      return ext;
    }

    public String getTreeFolder()
    {
      return null;
    }

    public int[] getResourceInfo(boolean ignoreoverride) throws Exception
    {
      return new int[] { key.resourceSize };
    }

    public byte[] getResourceData(boolean ignoreoverride) throws Exception
    {
      return Arrays.copyOfRange(buffer, key.offsetToResource, key.offsetToResource + key.resourceSize);
    }

    public boolean hasOverride()
    {
      return false;
    }

    protected InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception
    {
      return new ByteArrayInputStream(buffer, key.offsetToResource, key.resourceSize);
    }

    protected File getActualFile(boolean ignoreoverride)
    {
      return NIFile.getFile(ResourceFactory.getRootDirs(), toString());
    }

    public String toString()
    {
      return key.resRef + '.' + getExtension();
    }

    public String getResourceName()
    {
      return toString();
    }
  }
}

