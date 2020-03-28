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

    table = new SortableTable(new String[]{"File", "Name", "Attribute"},
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
    if (viewable instanceof DlgResource) {
      DlgResource dlgRes = (DlgResource) viewable;
      JComponent detailViewer = dlgRes.getViewerTab(0);
      JTabbedPane parent = (JTabbedPane) detailViewer.getParent();
      dlgRes.selectInEdit(
          ((ReferenceHit)table.getTableItemAt(row)).getStructEntry());
      // make sure we see the detail viewer
      parent.getModel().setSelectedIndex(parent.indexOfComponent(detailViewer));

    }
    else if (viewable instanceof AbstractStruct) {
      ((AbstractStruct)viewable).getViewer().selectEntry(
          ((ReferenceHit)table.getTableItemAt(row)).getStructEntry().getOffset());
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

// -------------------------- INNER CLASSES --------------------------

  private static final class ReferenceHit implements TableItem
  {
    private final ResourceEntry entry;
    private final String name;
    private final StructEntry ref;

    private ReferenceHit(ResourceEntry entry, String name, StructEntry ref)
    {
      this.entry = entry;
      this.name = name;
      this.ref = ref;
    }

    @Override
    public Object getObjectAt(int columnIndex)
    {
      switch (columnIndex) {
        case 0:
          return entry;
        case 1:
          if (name != null) {
            return name;
          } else {
            if (entry instanceof FileResourceEntry) {
              return entry.getActualPath().getParent().toString();
            } else {
              return "";
            }
          }
        default:
          if (ref != null) {
            return ref.getName() + '=' + ref;
          } else {
            return null;
          }
      }
    }

    public StructEntry getStructEntry()
    {
      return ref;
    }

    @Override
    public String toString()
    {
      final StringBuilder buf = new StringBuilder("File: ");
      buf.append(entry.getResourceName());
      if (name != null)
        buf.append(", Name: ").append(name);
      if (ref != null)
        buf.append(", Attribute: ").append(ref.getName()).append('=').append(ref);
      return buf.toString();
    }
  }
}
