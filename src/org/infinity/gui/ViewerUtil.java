// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.Viewable;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.graphics.MosResource;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.SimpleListModel;

public final class ViewerUtil
{
  public static void addLabelFieldPair(JPanel panel, StructEntry entry, GridBagLayout gbl,
                                       GridBagConstraints gbc, boolean endline)
  {
    if (entry == null)
      return;
    JLabel label = new JLabel(entry.getName());
    JComponent text;
    if (entry instanceof ResourceRef) {
      text = new LinkButton((ResourceRef)entry);
    } else {
      if (entry instanceof StringRef) {
        text = new JLabel(((StringRef)entry).toString(BrowserMenuBar.getInstance().showStrrefs()));
      } else {
        text = new JLabel(entry.toString());
      }
      text.setFont(text.getFont().deriveFont(Font.PLAIN));
    }

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.gridwidth = 1;
    gbc.anchor = GridBagConstraints.WEST;
    gbl.setConstraints(label, gbc);
    panel.add(label);

    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    if (endline)
      gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(text, gbc);
    panel.add(text);
  }

  public static void addLabelFieldPair(JPanel panel, String name, String field, GridBagLayout gbl,
                                       GridBagConstraints gbc, boolean endline)
  {
    if (name != null) {
      JLabel label = new JLabel(name);
      JComponent text = new JLabel((field != null) ? field : "");
      text.setFont(text.getFont().deriveFont(Font.PLAIN));

      gbc.weightx = 0.0;
      gbc.fill = GridBagConstraints.NONE;
      gbc.gridwidth = 1;
      gbc.anchor = GridBagConstraints.WEST;
      gbl.setConstraints(label, gbc);
      panel.add(label);

      gbc.weightx = 1.0;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      if (endline)
        gbc.gridwidth = GridBagConstraints.REMAINDER;
      gbl.setConstraints(text, gbc);
      panel.add(text);
    }
  }

  public static JLabel makeBamPanel(ResourceRef iconRef, int frameNr)
  {
    ResourceEntry iconEntry = ResourceFactory.getResourceEntry(iconRef.getResourceName());
    if (iconEntry != null) {
      try {
        BamDecoder decoder = BamDecoder.loadBam(iconEntry);
        BamControl ctrl = decoder.createControl();
        JLabel label = new JLabel(iconRef.getName(), JLabel.CENTER);
        frameNr = Math.min(frameNr, decoder.frameCount() - 1);
        label.setIcon(new ImageIcon(decoder.frameGet(ctrl, frameNr)));
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        return label;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return new JLabel("No " + iconRef.getName().toLowerCase(Locale.ENGLISH), JLabel.CENTER);
  }

  public static JComponent makeBamPanel(ResourceRef iconRef, int animNr, int frameNr)
  {
    ResourceEntry iconEntry = ResourceFactory.getResourceEntry(iconRef.getResourceName());
    if (iconEntry != null) {
      try {
        BamDecoder decoder = BamDecoder.loadBam(iconEntry);
        BamControl ctrl = decoder.createControl();
        JLabel label = new JLabel(iconRef.getName(), JLabel.CENTER);
        int frameIdx = -1;
        for (int curAnimIdx = animNr; curAnimIdx >= 0 && frameIdx < 0; curAnimIdx--) {
          for (int curFrameIdx = frameNr; curFrameIdx >= 0 && frameIdx < 0; curFrameIdx--) {
            frameIdx = ctrl.cycleGetFrameIndexAbsolute(curAnimIdx, curFrameIdx);
          }
        }
        label.setIcon(new ImageIcon(decoder.frameGet(ctrl, frameIdx)));
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        return label;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return new JLabel("No " + iconRef.getName().toLowerCase(Locale.ENGLISH), JLabel.CENTER);
  }

  public static JComponent makeCheckLabel(StructEntry entry, String yes)
  {
    JLabel check = new JLabel(entry.getName());
    if (entry.toString().equalsIgnoreCase(yes))
      check.setIcon(Icons.getIcon(Icons.ICON_CHECK_16));
    else
      check.setIcon(Icons.getIcon(Icons.ICON_CHECK_NOT_16));
    return check;
  }

  public static JPanel makeCheckPanel(Flag flag, int rows)
  {
    JPanel panel = new JPanel(new GridLayout(0, rows, 3, 3));
    for (int i = 0; i < flag.getSize() << 3; i++) {
      String s = flag.getString(i);
      if (s != null && !s.equals("") && !s.startsWith("Unknown")) {
        JLabel check = new JLabel(flag.getString(i));
        if (flag.isFlagSet(i))
          check.setIcon(Icons.getIcon(Icons.ICON_CHECK_16));
        else
          check.setIcon(Icons.getIcon(Icons.ICON_CHECK_NOT_16));
        panel.add(check);
      }
    }
    panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(flag.getName()),
                                                       BorderFactory.createEmptyBorder(3, 3, 3, 3)));
    return panel;
  }

  public static JLabel makeImagePanel(ResourceRef imageRef)
  {
    ResourceEntry imageEntry = ResourceFactory.getResourceEntry(imageRef.getResourceName());
    if (imageEntry != null) {
      Resource resource = ResourceFactory.getResource(imageEntry);
      if (resource != null) {
        JLabel label = new JLabel(imageRef.getName(), JLabel.CENTER);
        label.setVerticalTextPosition(SwingConstants.BOTTOM);
        label.setHorizontalTextPosition(SwingConstants.CENTER);
        if (resource instanceof GraphicsResource) {
          label.setIcon(new ImageIcon(((GraphicsResource)resource).getImage()));
        } else if (resource instanceof MosResource) {
          label.setIcon(new ImageIcon(((MosResource)resource).getImage()));
        }
        return label;
      }
    }
    return new JLabel("No " + imageRef.getName().toLowerCase(Locale.ENGLISH), JLabel.CENTER);
  }

  public static JPanel makeListPanel(String title, AbstractStruct struct,
                                     Class<? extends StructEntry> listClass, String attrName)
  {
    return new StructListPanel(title, struct, listClass, attrName, null, null);
  }

  public static JPanel makeListPanel(String title, AbstractStruct struct,
                                     Class<? extends StructEntry> listClass,
                                     String attrName, ListCellRenderer<Object> renderer)
  {
    return new StructListPanel(title, struct, listClass, attrName, renderer, null);
  }

  public static JPanel makeListPanel(String title, AbstractStruct struct,
                                     Class<? extends StructEntry> listClass, String attrName,
                                     ListCellRenderer<Object> renderer, ListSelectionListener listener)
  {
    return new StructListPanel(title, struct, listClass, attrName, renderer, listener);
  }

  public static JPanel makeTextAreaPanel(StructEntry entry)
  {
    String text;
    if (entry instanceof StringRef) {
      text = ((StringRef)entry).toString(BrowserMenuBar.getInstance().showStrrefs());
    } else {
      text = entry.toString();
    }
    InfinityTextArea ta = new InfinityTextArea(text, true);
    ta.setCaretPosition(0);
    ta.setHighlightCurrentLine(false);
    ta.setEditable(false);
    ta.setLineWrap(true);
    ta.setWrapStyleWord(true);
    InfinityScrollPane scroll = new InfinityScrollPane(ta, true);
    scroll.setLineNumbersEnabled(false);
    ta.setMargin(new Insets(3, 3, 3, 3));
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(new JLabel(entry.getName()), BorderLayout.NORTH);
    panel.add(scroll, BorderLayout.CENTER);
    panel.setPreferredSize(new Dimension(5, 5));
    return panel;
  }

  /** Initializes a {@link GridBagConstraints} instance. */
  public static GridBagConstraints setGBC(GridBagConstraints gbc, int gridX, int gridY,
                                          int gridWidth, int gridHeight, double weightX, double weightY,
                                          int anchor, int fill, Insets insets, int iPadX, int iPadY)
  {
    if (gbc == null) gbc = new GridBagConstraints();

    gbc.gridx = gridX;
    gbc.gridy = gridY;
    gbc.gridwidth = gridWidth;
    gbc.gridheight = gridHeight;
    gbc.weightx = weightX;
    gbc.weighty = weightY;
    gbc.anchor = anchor;
    gbc.fill = fill;
    gbc.insets = (insets == null) ? new Insets(0, 0, 0, 0) : insets;
    gbc.ipadx = iPadX;
    gbc.ipady = iPadY;

    return gbc;
  }

  /** Returns a JLabel control containing a clickable link. */
  public static JLabel createUrlLabel(String url)
  {
    return createUrlLabel(url, url, SwingConstants.LEADING);
  }

  /** Returns a JLabel control containing a clickable link. */
  public static JLabel createUrlLabel(String text, String url)
  {
    return createUrlLabel(text, url, SwingConstants.LEADING);
  }

  /** Returns a JLabel control containing a clickable link. */
  public static JLabel createUrlLabel(String text, String url, int horizontalAlignment)
  {
    JLabel l = new JLabel("<html><a href=\"" + url + "\">" + text + "</a></html>", horizontalAlignment);
    l.addMouseListener(new UrlBrowser(url));
    l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    return l;
  }

  private ViewerUtil(){}

// -------------------------- INNER CLASSES --------------------------

  public static final class StructListPanel extends JPanel implements TableModelListener, ActionListener
  {
    private final AbstractStruct struct;
    private final Class<? extends StructEntry> listClass;
    private final JList<Object> list;
    private final SimpleListModel<Object> listModel = new SimpleListModel<Object>();
    private final JButton bOpen = new JButton("View/Edit", Icons.getIcon(Icons.ICON_ZOOM_16));
    private Comparator<AbstractStruct> comp;

    private StructListPanel(String title, AbstractStruct struct,
                            Class<? extends StructEntry> listClass, String attrName,
                            ListCellRenderer<Object> renderer, ListSelectionListener listener)
    {
      super(new BorderLayout(0, 3));
      this.struct = struct;
      this.listClass = listClass;
      struct.addTableModelListener(this);
      list = new JList<>(listModel);
      if (listener != null)
        list.addListSelectionListener(listener);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      if (renderer != null)
        list.setCellRenderer(renderer);
      if (attrName == null) {
        for (int i = 0; i < struct.getFieldCount(); i++) {
          StructEntry o = struct.getField(i);
          if (o.getClass() == listClass)
            listModel.addElement(o);
        }
      }
      else {
        if (renderer == null)
          list.setCellRenderer(new StructListRenderer(attrName));
        List<AbstractStruct> templist = new ArrayList<AbstractStruct>();
        for (int i = 0; i < struct.getFieldCount(); i++) {
          StructEntry o = struct.getField(i);
          if (o.getClass() == listClass)
            templist.add((AbstractStruct)o);
        }
        comp = new StructListComparator(attrName);
        Collections.sort(templist, comp);
        for (int i = 0; i < templist.size(); i++)
          listModel.addElement(templist.get(i));
      }

      final JPanel parent = this;
      list.addMouseListener(new MouseAdapter()
      {
        @Override
        public void mouseClicked(MouseEvent e)
        {
          if (e.getClickCount() == 2 && list.getSelectedValue() instanceof Viewable) {
            new ViewFrame(parent.getTopLevelAncestor(), (Viewable)list.getSelectedValue());
          }
        }
      });
      if (listModel.size() > 0)
        list.setSelectedIndex(0);
      bOpen.addActionListener(this);
      bOpen.setEnabled(listModel.size() > 0 && listModel.get(0) instanceof Viewable);

      add(new JLabel(title), BorderLayout.NORTH);
      add(new JScrollPane(list), BorderLayout.CENTER);
      add(bOpen, BorderLayout.SOUTH);
      setPreferredSize(new Dimension(5, 5));
    }

    /** Provides access to the list component of the panel. */
    public JList<Object> getList() { return list; }

    @Override
    public void actionPerformed(ActionEvent event)
    {
      new ViewFrame(getTopLevelAncestor(), (Viewable)list.getSelectedValue());
    }

    @Override
    public void tableChanged(TableModelEvent event)
    {
      if (event.getType() == TableModelEvent.DELETE) {

        // go through the list and find what was deleted
        List<StructEntry> structlist = struct.getList();
        for (int i = 0; i < listModel.size(); i++) {
          if (!structlist.contains(listModel.get(i))) {
            listModel.remove(i);
            i--;
          }
        }
        /*
        // Ineffective - any better solutions?
        if (comp == null) {
          listModel.clear();
          for (int i = 0; i < struct.getRowCount(); i++) {
            StructEntry o = struct.getStructEntryAt(i);
            if (o.getClass() == listClass)
              listModel.addElement(o);
          }
        }
        else {

          List<AbstractStruct> templist = new ArrayList<AbstractStruct>();
          for (int i = 0; i < struct.getRowCount(); i++) {
            StructEntry o = struct.getStructEntryAt(i);
            if (o.getClass() == listClass)
              templist.add((AbstractStruct)o);
          }
          Collections.sort(templist, comp);
          listModel.clear();
          for (int i = 0; i < templist.size(); i++) {
            listModel.addElement(templist.get(i));
          }
        }
        */

        if (listModel.size() > 0)
          list.setSelectedIndex(0);
        bOpen.setEnabled(listModel.size() > 0 && listModel.get(0) instanceof Viewable);
      }
      else if (event.getType() == TableModelEvent.INSERT) {
        for (int i = event.getFirstRow(); i <= event.getLastRow(); i++) {
          if (i >= struct.getFieldCount()) {
            break;
          }
          Object o = struct.getField(i);
          if (o.getClass() == listClass) {
            listModel.addElement(o);    // Not sorted properly after this...
            if (!bOpen.isEnabled() && listModel.get(0) instanceof Viewable) {
              bOpen.setEnabled(true);
              list.setSelectedIndex(0);
            }
          }
        }
      }
    }
  }

  private static final class StructListRenderer extends DefaultListCellRenderer
  {
    private final String attrName;

    private StructListRenderer(String attrName)
    {
      this.attrName = attrName;
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      AbstractStruct effect = (AbstractStruct)value;
      StructEntry entry = effect.getAttribute(attrName);
      if (entry instanceof ResourceRef) {
        ResourceRef resref = (ResourceRef)entry;
        label.setText(resref.getSearchName() + " (" + resref.getResourceName() + ')');
      }
      else if (entry == null || entry.toString().trim().equals(""))
        label.setText(effect.toString());
      else
        label.setText(entry.toString());
      return label;
    }
  }

  private static final class StructListComparator implements Comparator<AbstractStruct>
  {
    private final String attrName;

    private StructListComparator(String attrName)
    {
      this.attrName = attrName;
    }

    @Override
    public int compare(AbstractStruct as1, AbstractStruct as2)
    {
      return as1.getAttribute(attrName).toString().compareTo(as2.getAttribute(attrName).toString());
    }
  }
}

