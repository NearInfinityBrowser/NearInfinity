// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.kotor;

import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;
import infinity.util.ArrayUtil;
import infinity.util.Byteconvert;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public final class RimResource implements Resource, ActionListener, ListSelectionListener
{
  private final ResourceEntry entry;
  private final RimKeyEntry keys[];
  private final byte buffer[];
  private JButton bView;
  private SortableTable table;

  public RimResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    buffer = entry.getResourceData();

    String fileType = new String(buffer, 0, 4);
    String fileVersion = new String(buffer, 4, 4);
    int unknown1 = Byteconvert.convertInt(buffer, 8);
    int entryCount = Byteconvert.convertInt(buffer, 12);
    int offsetToKeyList = Byteconvert.convertInt(buffer, 16);
    // unused bytes ?

    keys = new RimKeyEntry[entryCount];
    for (int i = 0; i < keys.length; i++)
      keys[i] = new RimKeyEntry(buffer, offsetToKeyList + i * 32);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bView) {
      RimKeyEntry key = (RimKeyEntry)table.getTableItemAt(table.getSelectedRow());
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
    table = new SortableTable(new String[] { "", "Resource name" },
                              new Class[] { ImageIcon.class, String.class },
                              new int[] { 5, 300 });
    for (final RimKeyEntry key : keys)
      table.addTableItem(key);
    table.tableComplete(1);
    table.getSelectionModel().addListSelectionListener(this);
    table.addMouseListener(new MouseAdapter()
    {
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          RimKeyEntry key = (RimKeyEntry)table.getTableItemAt(table.getSelectedRow());
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

  private static final class RimKeyEntry implements TableItem
  {
    final String resRef;
    final int resType;
    final int resID;
    final int offsetToResource;
    final int resourceSize;

    private RimKeyEntry(byte buffer[], int offset)
    {
      resRef = Byteconvert.convertString(buffer, offset, 16);
      resType = Byteconvert.convertInt(buffer, offset + 16);
      resID = Byteconvert.convertInt(buffer, offset + 20);
      offsetToResource = Byteconvert.convertInt(buffer, offset + 24);
      resourceSize = Byteconvert.convertInt(buffer, offset + 28);
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
    private final RimKeyEntry key;

    ERFResourceEntry(RimKeyEntry key)
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
      return ArrayUtil.getSubArray(buffer, key.offsetToResource, key.resourceSize);
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
      return new File(ResourceFactory.getRootDir(), toString());
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

