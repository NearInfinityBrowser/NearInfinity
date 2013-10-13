// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.search;

import infinity.NearInfinity;
import infinity.gui.BrowserMenuBar;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.SortableTable;
import infinity.gui.TableItem;
import infinity.gui.ViewFrame;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.Viewable;
import infinity.resource.dlg.DlgResource;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public final class ReferenceHitFrame extends ChildFrame implements ActionListener, ListSelectionListener
{
  private final Component parent;
  private final JButton bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
  private final JButton bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
  private final JButton bsave = new JButton("Save...", Icons.getIcon("Save16.gif"));
  private final JLabel count;
  private final Object query;
  private final SortableTable table;

  public ReferenceHitFrame(Object query, Component parent)
  {
    super("Result", true);
    this.query = query;
    this.parent = parent;
    setIconImage(Icons.getIcon("History16.gif").getImage());

    List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(3);
    colClasses.add(Object.class); colClasses.add(Object.class); colClasses.add(Object.class);
    table = new SortableTable(Arrays.asList(new String[]{"File", "Name", "Attribute"}),
                              colClasses, Arrays.asList(new Integer[]{100, 100, 300}));

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
    table.setFont(BrowserMenuBar.getInstance().getScriptFont());
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
      JFileChooser chooser = new JFileChooser(ResourceFactory.getRootDir());
      chooser.setDialogTitle("Save result");
      chooser.setSelectedFile(new File("result.txt"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File output = chooser.getSelectedFile();
        if (output.exists()) {
          String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(this, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) == 1)
            return;
        }
        try {
          PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
          pw.println("Searched for: " + query);
          pw.println("Number of hits: " + table.getRowCount());
          for (int i = 0; i < table.getRowCount(); i++)
            pw.println(table.getTableItemAt(i).toString());
          pw.close();
          JOptionPane.showMessageDialog(this, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(this, "Error while saving " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

  private void showEntryInViewer(int row, Viewable viewable) {
    if (viewable instanceof DlgResource) {
      DlgResource dlgRes = (DlgResource) viewable;
      JComponent detailViewer = dlgRes.getDetailViewer();
      JTabbedPane parent = (JTabbedPane) detailViewer.getParent();
      dlgRes.showStateWithStructEntry(
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
      if (columnIndex == 0)
        return entry;
      if (columnIndex == 1) {
        if (name == null && entry instanceof FileResourceEntry)
          return entry.getActualFile().getParent();
        if (name == null)
          return "";
        return name;
      }
      if (ref != null)
        return ref.getName() + '=' + ref;
      return null;
    }

    public StructEntry getStructEntry()
    {
      return ref;
    }

    @Override
    public String toString()
    {
      StringBuffer buf = new StringBuffer("File: ");
      buf.append(entry.toString());
      if (name != null)
        buf.append("  Name: ").append(name);
      if (ref != null)
        buf.append("  Attribute: ").append(ref.getName() + '=' + ref);
      return buf.toString();
    }
  }
}

