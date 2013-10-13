// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.kotor;

import infinity.gui.SortableTable;
import infinity.gui.TableItem;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;
import infinity.util.NIFile;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

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
    int unknown1 = DynamicArray.getInt(buffer, 8);
    int entryCount = DynamicArray.getInt(buffer, 12);
    int offsetToKeyList = DynamicArray.getInt(buffer, 16);
    // unused bytes ?

    keys = new RimKeyEntry[entryCount];
    for (int i = 0; i < keys.length; i++)
      keys[i] = new RimKeyEntry(buffer, offsetToKeyList + i * 32);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
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

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bView.setEnabled(table.getSelectedRowCount() == 1);
  }

// --------------------- End Interface ListSelectionListener ---------------------


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
    List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(3);
    colClasses.add(ImageIcon.class); colClasses.add(String.class);
    table = new SortableTable(Arrays.asList(new String[]{"", "Resource name"}),
                              colClasses, Arrays.asList(new Integer[]{5, 300}));

    for (final RimKeyEntry key : keys)
      table.addTableItem(key);
    table.tableComplete(1);
    table.getSelectionModel().addListSelectionListener(this);
    table.addMouseListener(new MouseAdapter()
    {
      @Override
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
      resRef = DynamicArray.getString(buffer, offset, 16);
      resType = DynamicArray.getInt(buffer, offset + 16);
      resID = DynamicArray.getInt(buffer, offset + 20);
      offsetToResource = DynamicArray.getInt(buffer, offset + 24);
      resourceSize = DynamicArray.getInt(buffer, offset + 28);
    }

    @Override
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

    @Override
    public String getExtension()
    {
      String ext = ResourceFactory.getKeyfile().getExtension(key.resType);
      if (ext == null)
        return String.valueOf(key.resType);
      return ext;
    }

    @Override
    public String getTreeFolder()
    {
      return null;
    }

    @Override
    public int[] getResourceInfo(boolean ignoreoverride) throws Exception
    {
      return new int[] { key.resourceSize };
    }

    @Override
    public byte[] getResourceData(boolean ignoreoverride) throws Exception
    {
      return Arrays.copyOfRange(buffer, key.offsetToResource, key.offsetToResource + key.resourceSize);
    }

    @Override
    public boolean hasOverride()
    {
      return false;
    }

    @Override
    protected InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception
    {
      return new ByteArrayInputStream(buffer, key.offsetToResource, key.resourceSize);
    }

    @Override
    protected File getActualFile(boolean ignoreoverride)
    {
      return NIFile.getFile(ResourceFactory.getRootDirs(), toString());
    }

    @Override
    public String toString()
    {
      return key.resRef + '.' + getExtension();
    }

    @Override
    public String getResourceName()
    {
      return toString();
    }
  }
}

