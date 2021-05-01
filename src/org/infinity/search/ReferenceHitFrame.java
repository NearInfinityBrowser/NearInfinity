// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.TextResource;
import org.infinity.resource.Viewable;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

public final class ReferenceHitFrame extends ChildFrame implements ActionListener, ListSelectionListener
{
  private static final String QUERY_STRING = "string reference";

  private final Component parent;
  private final JButton bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
  private final JButton bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
  private final JLabel count;
  private final Object query;
  /** List of the {@link ReferenceHit} objects. */
  private final SortableTable table;

  public ReferenceHitFrame(Object query, Component parent)
  {
    super("Result", true);
    if (query == null) {
      query = QUERY_STRING;
    }
    this.query = query;
    this.parent = parent;
    setIconImage(Icons.getIcon(Icons.ICON_HISTORY_16).getImage());

    table = new SortableTable(new String[]{"File", "Name/Text", "Attribute/Line"},
                              new Class<?>[]{ResourceEntry.class, String.class, String.class},
                              new Integer[]{100, 100, 300});

    bopen.setMnemonic('o');
    bopennew.setMnemonic('n');
    bsave.setMnemonic('s');
    getRootPane().setDefaultButton(bopennew);
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    panel.add(bopen);
    panel.add(bopennew);
    panel.add(bsave);
    count = new JLabel(table.getRowCount() + " hits found", JLabel.CENTER);
    count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout(0, 3));
    JScrollPane scrollTable = new JScrollPane(table);
    scrollTable.getViewport().setBackground(table.getBackground());
    pane.add(count, BorderLayout.NORTH);
    pane.add(scrollTable, BorderLayout.CENTER);
    pane.add(panel, BorderLayout.SOUTH);
    pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    bopen.setEnabled(false);
    bopennew.setEnabled(false);
    table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
    table.getSelectionModel().addListSelectionListener(this);
    final ChildFrame frame = this;
    table.addMouseListener(new MouseAdapter()
    {
      @Override
      public void mouseReleased(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          int row = table.getSelectedRow();
          if (row != -1) {
            Resource res = ResourceFactory.getResource((ResourceEntry)table.getValueAt(row, 0));
            new ViewFrame(frame, res);
            showEntryInViewer(row, res);
          }
        }
      }
    });
    bopen.addActionListener(this);
    bopennew.addActionListener(this);
    bsave.addActionListener(this);
    setPreferredSize(Misc.getScaledDimension(getPreferredSize()));
    pack();
    Center.center(this, parent.getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry entry = (ResourceEntry)table.getValueAt(row, 0);
        if (parent instanceof ViewFrame && parent.isVisible()) {
          Resource res = ResourceFactory.getResource(entry);
          ((ViewFrame)parent).setViewable(res);
          showEntryInViewer(row, res);
          if (res instanceof DlgResource)
            ((ViewFrame) parent).toFront();
        }
        else {
          NearInfinity.getInstance().showResourceEntry(entry);
          Viewable viewable = NearInfinity.getInstance().getViewable();
          showEntryInViewer(row, viewable);
          if (viewable instanceof DlgResource)
            NearInfinity.getInstance().toFront();
        }
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        Resource res = ResourceFactory.getResource((ResourceEntry)table.getValueAt(row, 0));
        new ViewFrame(this, res);
        showEntryInViewer(row, res);
      }
    }
    else if (event.getSource() == bsave) {
      table.saveSearchResult(this, query.toString());
    }
  }

  private void showEntryInViewer(int row, Viewable viewable) {
    ReferenceHit hit = (ReferenceHit)table.getTableItemAt(row);
    if (viewable instanceof DlgResource) {
      DlgResource dlgRes = (DlgResource) viewable;
      JComponent detailViewer = dlgRes.getViewerTab(0);
      JTabbedPane parent = (JTabbedPane) detailViewer.getParent();
      dlgRes.selectInEdit(hit.getStructEntry());
      // make sure we see the detail viewer
      parent.getModel().setSelectedIndex(parent.indexOfComponent(detailViewer));
    }
    else if (viewable instanceof AbstractStruct) {
      ((AbstractStruct)viewable).getViewer().selectEntry(hit.getStructEntry().getOffset());
    }
    else if (viewable instanceof TextResource) {
      ((TextResource)viewable).highlightText(hit.getLineNr(), hit.getLine());
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

// --------------------- End Interface ListSelectionListener ---------------------

  @Override
  public void setVisible(boolean b)
  {
    table.tableComplete();
    count.setText(table.getRowCount() + " hit(s) found");
    if (b && table.getRowCount() == 0)
      JOptionPane.showMessageDialog(parent, "No hits found", "Info", JOptionPane.INFORMATION_MESSAGE);
    else
      super.setVisible(b);
  }

  public void addHit(ResourceEntry entry, String name, StructEntry ref)
  {
    table.addTableItem(new ReferenceHit(entry, name, ref));
  }

  public void addHit(ResourceEntry entry, String line, int lineNr)
  {
    table.addTableItem(new ReferenceHit(entry, line, lineNr));
  }

// -------------------------- INNER CLASSES --------------------------

  /** Stores a reference to a specific resource field. */
  public static final class ReferenceHit implements TableItem, Comparable<ReferenceHit>
  {
    public enum Mode {
      Struct,
      Text,
    }

    private final Mode mode;
    private final ResourceEntry entry;
    private final String name;
    private final StructEntry ref;
    private final String line;
    private final int lineNr;

    public ReferenceHit(ResourceEntry entry, String name, StructEntry ref)
    {
      this.mode = Mode.Struct;
      this.entry = entry;
      this.name = name;
      this.ref = ref;
      this.line = "";
      this.lineNr = 0;
    }

    public ReferenceHit(ResourceEntry entry, String line, int lineNr)
    {
      this.mode = Mode.Text;
      this.entry = entry;
      this.name = "";
      this.ref = null;
      this.line = line;
      this.lineNr = lineNr;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      switch (columnIndex) {
        case 0:
          return entry;
        case 1:
          if (mode == Mode.Text) {
            return line;
          } else {
            if (name != null) {
              return name;
            } else if (entry instanceof FileResourceEntry) {
              return entry.getActualPath().getParent().toString();
            } else {
              return "";
            }
          }
        default:
          if (mode == Mode.Text) {
            return lineNr;
          } else {
            if (ref != null)
              return ref.getName() + '=' + ref;
            return null;
          }
      }
    }

    public Mode getMode()
    {
      return mode;
    }

    public ResourceEntry getResource()
    {
      return entry;
    }

    public String getName()
    {
      return name;
    }

    public StructEntry getStructEntry()
    {
      return ref;
    }

    public String getLine()
    {
      return line;
    }

    public int getLineNr()
    {
      return lineNr;
    }

    @Override
    public String toString()
    {
      if (mode == Mode.Text) {
        return String.format("File: %s, Line: %d, Text: %s", entry.getResourceName(), lineNr, line);
      } else {
        final StringBuilder buf = new StringBuilder("File: ");
        buf.append(entry.getResourceName());
        if (name != null)
          buf.append(", Name: ").append(name);
        if (ref != null)
          buf.append(", Attribute: ").append(ref.getName()).append('=').append(ref);
        return buf.toString();
      }
    }

    @Override
    public int hashCode()
    {
      int hash = 7;
      hash = 31 * hash + ((mode == null) ? 0 : mode.hashCode());
      hash = 31 * hash + ((entry == null) ? 0 : entry.hashCode());
      hash = 31 * hash + ((name == null) ? 0 : name.hashCode());
      hash = 31 * hash + ((ref == null) ? 0 : ref.hashCode());
      hash = 31 * hash + ((line == null) ? 0 : line.hashCode());
      hash = 31 * hash + lineNr;
      return hash;
    }

    @Override
    public boolean equals(Object obj)
    {
      if (obj instanceof ReferenceHit) {
        ReferenceHit rh = (ReferenceHit)obj;
        if (entry == null && rh.entry != null || !entry.equals(rh.entry))
          return false;
        if (name == null && rh.name != null || !name.equalsIgnoreCase(rh.name))
          return false;
        if (ref == null && rh.ref != null || !ref.equals(rh.ref))
          return false;
        return true;
      }
      return super.equals(obj);
    }

    @Override
    public int compareTo(ReferenceHit rh)
    {
      if (rh == null)
        throw new NullPointerException();

      if (entry == null) {
        if (rh.entry == null)
          return 0;
        else
          return 1;
      }

      int retVal = entry.compareTo(rh.entry);
      if (retVal == 0) {
        if (name != null) {
          retVal = name.compareToIgnoreCase(rh.name);
          if (retVal == 0) {
            if (ref != null) {
              retVal = ref.compareTo(rh.ref);
            }
          }
        }
      }

      return retVal;
    }
  }
}
