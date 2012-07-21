// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.bcs.BcsResource;
import infinity.resource.bcs.Decompiler;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public final class BCSIDSChecker implements Runnable, ActionListener, ListSelectionListener
{
  private ChildFrame resultFrame;
  private JButton bopen, bopennew;
  private SortableTable table;

  public BCSIDSChecker()
  {
    new Thread(this).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
        BcsResource bcsfile = (BcsResource)NearInfinity.getInstance().getViewable();
        bcsfile.highlightText(((Integer)table.getValueAt(row, 2)).intValue(), null);
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        ViewFrame viewFrame = new ViewFrame(resultFrame, resource);
        BcsResource bcsfile = (BcsResource)viewFrame.getViewable();
        bcsfile.highlightText(((Integer)table.getValueAt(row, 2)).intValue(), null);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    bopen.setEnabled(true);
    bopennew.setEnabled(true);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    List<ResourceEntry> bcsFiles = ResourceFactory.getInstance().getResources("BCS");
    bcsFiles.addAll(ResourceFactory.getInstance().getResources("BS"));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Checking...", null, 0, bcsFiles.size());
    table = new SortableTable(new String[]{"File", "Error message", "Line"},
                              new Class[]{Object.class, Object.class, Integer.class},
                              new int[]{100, 300, 50});
    for (int i = 0; i < bcsFiles.size(); i++) {
      ResourceEntry entry = bcsFiles.get(i);
      BcsResource script;
      try {
        script = new BcsResource(entry);
        checkScript(script);
      } catch (Exception e) {
        e.printStackTrace();
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation canceled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
        blocker.setBlocked(false);
        return;
      }
    }
    if (table.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unknown references found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      table.tableComplete();
      resultFrame = new ChildFrame("Result", true);
      resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
      bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
      bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
      JLabel count = new JLabel(table.getRowCount() + " hits(s) found", JLabel.CENTER);
      count.setFont(count.getFont().deriveFont((float)count.getFont().getSize() + 2.0f));
      bopen.setMnemonic('o');
      bopennew.setMnemonic('n');
      resultFrame.getRootPane().setDefaultButton(bopennew);
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bopen);
      panel.add(bopennew);
      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      JPanel pane = (JPanel)resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.NORTH);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.SOUTH);
      bopen.setEnabled(false);
      bopennew.setEnabled(false);
      table.setFont(BrowserMenuBar.getInstance().getScriptFont());
      table.getSelectionModel().addListSelectionListener(this);
      table.addMouseListener(new MouseAdapter()
      {
        public void mouseReleased(MouseEvent event)
        {
          if (event.getClickCount() == 2) {
            int row = table.getSelectedRow();
            if (row != -1) {
              ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
              Resource resource = ResourceFactory.getResource(resourceEntry);
              new ViewFrame(resultFrame, resource);
            }
          }
        }
      });
      bopen.addActionListener(this);
      bopennew.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.pack();
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
    blocker.setBlocked(false);
  }

// --------------------- End Interface Runnable ---------------------

  private void checkScript(BcsResource script)
  {
    Decompiler.decompile(script.getCode(), true);
    SortedMap<Integer, String> idsErrors = Decompiler.getIdsErrors();
    for (final Integer lineNr: idsErrors.keySet()) {
      String error = idsErrors.get(lineNr);
      if (error.indexOf("GTIMES.IDS") == -1 &&
          error.indexOf("SCROLL.IDS") == -1 &&
          error.indexOf("SHOUTIDS.IDS") == -1 &&
          error.indexOf("SPECIFIC.IDS") == -1 &&
          error.indexOf("TIME.IDS") == -1)
        table.addTableItem(new BCSIDSErrorTableLine(script.getResourceEntry(), error, lineNr));
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class BCSIDSErrorTableLine implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final String error;
    private final Integer lineNr;

    private BCSIDSErrorTableLine(ResourceEntry resourceEntry, String error, Integer lineNr)
    {
      this.resourceEntry = resourceEntry;
      this.error = error;
      this.lineNr = lineNr;
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return error;
      return lineNr;
    }
  }
}

