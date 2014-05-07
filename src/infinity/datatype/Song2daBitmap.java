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

public final class Song2daBitmap extends Datatype implements Editable
{
  private static final LongIntegerHashMap<SonglistEntry> songNumber = new LongIntegerHashMap<SonglistEntry>();
  private TextListPanel list;
  private long value;

  private static void parseSonglist()
  {
    try {
      PlainTextResource songlist = new PlainTextResource(
              ResourceFactory.getInstance().getResourceEntry("SONGLIST.2DA"));
      StringTokenizer st = new StringTokenizer(songlist.getText(), "\n");
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      if (st.hasMoreTokens())
        st.nextToken();
      while (st.hasMoreTokens())
        parseSonglistLine(st.nextToken());
    } catch (Exception e) {
      e.printStackTrace();
    }

    songNumber.put(new Long(0xFFFFFFFE), new SonglistEntry((long)-2, "Continue area music"));
    songNumber.put(new Long(0xFFFFFFFF), new SonglistEntry((long)-1, "Continue outside music"));
  }

  private static void parseSonglistLine(String s)
  {
    StringTokenizer st = new StringTokenizer(s);
    int number = Integer.parseInt(st.nextToken());
    String name = st.nextToken();
    songNumber.put((long)number, new SonglistEntry((long)number, name));
  }

  public static void resetSonglist()
  {
    songNumber.clear();
  }

  public Song2daBitmap(byte buffer[], int offset, int length)
  {
    super(offset, length, "Song");
    if (songNumber.size() == 0)
      parseSonglist();

    if (length == 4)
      value = DynamicArray.getInt(buffer, offset);
    else if (length == 2)
      value = (long)DynamicArray.getShort(buffer, offset);
    else if (length == 1)
      value = (long)DynamicArray.getByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

  public Song2daBitmap(byte buffer[], int offset, int length, String name)
  {
    super(offset, length, name);
    if (songNumber.size() == 0)
      parseSonglist();

    if (length == 4)
      value = DynamicArray.getInt(buffer, offset);
    else if (length == 2)
      value = (long)DynamicArray.getShort(buffer, offset);
    else if (length == 1)
      value = (long)DynamicArray.getByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

// --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(final ActionListener container)
  {
    LongIntegerHashMap<SonglistEntry> idsmap = songNumber;
    if (list == null) {
      long[] keys = idsmap.keys();
      List<SonglistEntry> items = new ArrayList<SonglistEntry>(keys.length);
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
    SonglistEntry selected = (SonglistEntry)list.getSelectedValue();
    value = selected.number;
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.writeLong(os, value);
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  public String toString()
  {
    Object o = songNumber.get(value);
    if (o == null)
      return "Unknown - " + value;
    else
      return o.toString();
  }

  public long getValue()
  {
    return value;
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class SonglistEntry
  {
    private final long number;
    private final String name;

    private SonglistEntry(long number, String name)
    {
      this.number = number;
      this.name = name;
    }

    @Override
    public String toString()
    {
      return name + " - " + number;
    }
  }
}

