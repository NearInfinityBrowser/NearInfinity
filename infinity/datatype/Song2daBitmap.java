// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.gui.TextListPanel;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.other.PlainTextResource;
import infinity.util.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.List;

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

    songNumber.put(0xFFFFFFFE, new SonglistEntry((long)-2, "Continue area music"));
    songNumber.put(0xFFFFFFFF, new SonglistEntry((long)-1, "Continue outside music"));
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
      value = Byteconvert.convertInt(buffer, offset);
    else if (length == 2)
      value = (long)Byteconvert.convertShort(buffer, offset);
    else if (length == 1)
      value = (long)Byteconvert.convertByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

  public Song2daBitmap(byte buffer[], int offset, int length, String name)
  {
    super(offset, length, name);
    if (songNumber.size() == 0)
      parseSonglist();

    if (length == 4)
      value = Byteconvert.convertInt(buffer, offset);
    else if (length == 2)
      value = (long)Byteconvert.convertShort(buffer, offset);
    else if (length == 1)
      value = (long)Byteconvert.convertByte(buffer, offset);
    else
      throw new IllegalArgumentException();
  }

// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(final ActionListener container)
  {
    LongIntegerHashMap idsmap = songNumber;
    if (list == null) {
      long keys[] = idsmap.keys();
      List<SonglistEntry> items = new ArrayList<SonglistEntry>(keys.length);
      for (long id : keys) {
        items.add((SonglistEntry)idsmap.get(id));
      }
      list = new TextListPanel(items);
      list.addMouseListener(new MouseAdapter()
      {
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

  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  public boolean updateValue(AbstractStruct struct)
  {
    SonglistEntry selected = (SonglistEntry)list.getSelectedValue();
    value = selected.number;
    return true;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeLong(os, value);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    Object o = songNumber.get(value);
    if (o == null)
      return "Unknown - " + value;
    else
      return o.toString();
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

    public String toString()
    {
      return name + " - " + number;
    }
  }
}

