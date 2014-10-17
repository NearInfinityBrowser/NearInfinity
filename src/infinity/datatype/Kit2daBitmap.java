// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.text.PlainTextResource;
import infinity.util.DynamicArray;
import infinity.util.LongIntegerHashMap;
import infinity.util.io.FileWriterNI;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

public final class Kit2daBitmap extends Datatype implements Editable, Readable
{
  private static final LongIntegerHashMap<KitlistEntry> kitsNumber = new LongIntegerHashMap<KitlistEntry>();
  private static final LongIntegerHashMap<KitlistEntry> kitsUnusable = new LongIntegerHashMap<KitlistEntry>();
  private TextListPanel list;
  private boolean useUnusable;
  private long value;

  private static void parseKitlist()
  {
    try {
      PlainTextResource kitlist = new PlainTextResource(
              ResourceFactory.getInstance().getResourceEntry("KITLIST.2DA"));
      StringTokenizer st = new StringTokenizer(kitlist.getText(), "\n");
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      while (st.hasMoreTokens())
        parseKitlistLine(st.nextToken());
    } catch (Exception e) {
      e.printStackTrace();
    }
    kitsNumber.put((long)0, new KitlistEntry((long)0, "NO_KIT"));
    kitsUnusable.put((long)0, new KitlistEntry((long)0, "NO_KIT"));
  }

  private static void parseKitlistLine(String s)
  {
    StringTokenizer st = new StringTokenizer(s);
    int number = Integer.parseInt(st.nextToken());
    String name = st.nextToken();
    st.nextToken();
    st.nextToken();
    st.nextToken();
    st.nextToken();
    st.nextToken();
    String unusableSt = st.nextToken();
    long unusable;
    if (unusableSt.substring(0, 2).equalsIgnoreCase("0x"))
      unusable = Long.parseLong(unusableSt.substring(2), 16);
    else
      unusable = Long.parseLong(unusableSt);
    kitsNumber.put((long)number, new KitlistEntry((long)number, name));
    kitsUnusable.put(unusable, new KitlistEntry(unusable, name));
  }

  public static void resetKitlist()
  {
    kitsNumber.clear();
    kitsUnusable.clear();
  }

  public Kit2daBitmap(byte buffer[], int offset)
  {
    this(buffer, offset, true);
  }

  public Kit2daBitmap(byte buffer[], int offset, boolean useUnusable)
  {
    super(offset, 4, "Kit");
    this.useUnusable = useUnusable;
    if (kitsNumber.size() == 0)
      parseKitlist();
    read(buffer, offset);
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(final ActionListener container)
  {
    LongIntegerHashMap<KitlistEntry> idsmap = kitsNumber;
    if (useUnusable)
      idsmap = kitsUnusable;
    if (list == null) {
      long[] keys = idsmap.keys();
      List<KitlistEntry> items = new ArrayList<KitlistEntry>(keys.length);
      for (long id : keys) {
        items.add(idsmap.get(id));
      }
      list = new TextListPanel(items);
      list.addMouseListener(new MouseAdapter()
      {
        @Override
        public void mouseClicked(MouseEvent event)
        {
          if (event.getClickCount() == 2)
            container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      });
    }
    Object selected = idsmap.get(value);
    if (selected != null)
      list.setSelectedValue(selected, true);

    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbl.setConstraints(list, gbc);
    panel.add(list);

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.insets.left = 6;
    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    panel.setMinimumSize(DIM_MEDIUM);
    panel.setPreferredSize(DIM_MEDIUM);
    return panel;
  }

  @Override
  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    KitlistEntry selected = (KitlistEntry)list.getSelectedValue();
    value = selected.number;
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    if (useUnusable) {
      if (value > 2147483648L)
        value -= 4294967296L;
      byte buffer[] = DynamicArray.convertInt((int)value);
      os.write((int)buffer[2]);
      os.write((int)buffer[3]);
      os.write((int)buffer[0]);
      os.write((int)buffer[1]);
    }
    else
      FileWriterNI.writeBytes(os, new byte[]{0x00, 0x00, (byte)value, 0x40});
  }

// --------------------- End Interface Writeable ---------------------

//--------------------- Begin Interface Readable ---------------------

  @Override
  public void read(byte[] buffer, int offset)
  {
    if (buffer[offset + 3] == 0x40) {
      this.useUnusable = false;
      value = (long)buffer[offset + 2];
    }
    else {
      value = (long)(DynamicArray.getUnsignedShort(buffer, offset + 2) +
          0x10000 * DynamicArray.getUnsignedShort(buffer, offset));
      value &= 0xffffffff;
    }
  }

//--------------------- End Interface Readable ---------------------

  @Override
  public String toString()
  {
    Object o;
    if (useUnusable)
      o = kitsUnusable.get(value);
    else
      o = kitsNumber.get(value);
    if (o == null)
      return "Unknown - " + value;
    else
      return o.toString();
  }

  public long getValue()
  {
    return value;
  }

  public KitlistEntry getKit(int index)
  {
    if (index >= 0 && index < kitsNumber.size()) {
      return kitsNumber.get(kitsNumber.keys()[index]);
    }
    return null;
  }

  public int getKitsCount()
  {
    return kitsNumber.size();
  }

// -------------------------- INNER CLASSES --------------------------

  public static final class KitlistEntry
  {
    private final long number;
    private final String name;

    private KitlistEntry(long number, String name)
    {
      this.number = number;
      this.name = name;
    }

    @Override
    public String toString()
    {
      return name + " - " + number;
    }

    public long getValue()
    {
      return number;
    }

    public String getName()
    {
      return name;
    }
  }
}

