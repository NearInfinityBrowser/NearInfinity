// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.bcs.*;
import infinity.resource.bcs.Compiler;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public final class ScriptChecker implements Runnable, ActionListener, ListSelectionListener, ChangeListener
{
  private ChildFrame resultFrame;
  private JButton bopen, bopennew;
  private JTabbedPane tabbedPane;
  private SortableTable errorTable, warningTable;

  public ScriptChecker()
  {
    new Thread(this).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    SortableTable table = errorTable;
    if (tabbedPane.getSelectedIndex() == 1)
      table = warningTable;
    if (event.getSource() == bopen) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry);
        ((BcsResource)NearInfinity.getInstance().getViewable()).highlightText(
                ((Integer)table.getValueAt(row, 2)).intValue(), null);
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
        ((BcsResource)resource).highlightText(((Integer)table.getValueAt(row, 2)).intValue(), null);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  public void stateChanged(ChangeEvent e)
  {
    if (tabbedPane.getSelectedIndex() == 0)
      bopen.setEnabled(errorTable.getSelectedRowCount() > 0);
    else
      bopen.setEnabled(warningTable.getSelectedRowCount() > 0);
    bopennew.setEnabled(bopen.isEnabled());
  }

// --------------------- End Interface ChangeListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (tabbedPane.getSelectedIndex() == 0)
      bopen.setEnabled(errorTable.getSelectedRowCount() > 0);
    else
      bopen.setEnabled(warningTable.getSelectedRowCount() > 0);
    bopennew.setEnabled(bopen.isEnabled());
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    List<ResourceEntry> scriptFiles = ResourceFactory.getInstance().getResources("BCS");
    scriptFiles.addAll(ResourceFactory.getInstance().getResources("BS"));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Checking scripts...", null, 0, scriptFiles.size());
    errorTable = new SortableTable(new String[]{"Script", "Error message", "Line"},
                                   new Class[]{Object.class, Object.class, Integer.class},
                                   new int[]{120, 440, 50});
    warningTable = new SortableTable(new String[]{"Script", "Warning", "Line"},
                                     new Class[]{Object.class, Object.class, Integer.class},
                                     new int[]{120, 440, 50});
    for (int i = 0; i < scriptFiles.size(); i++) {
      ResourceEntry entry = scriptFiles.get(i);
      try {
        BcsResource script = new BcsResource(entry);
        String decompiled = Decompiler.decompile(script.getCode(), true);
        Compiler.getInstance().compile(decompiled);
        SortedMap<Integer, String> errorMap = Compiler.getInstance().getErrors();
        for (final Integer lineNr : errorMap.keySet()) {
          String error = errorMap.get(lineNr);
          errorTable.addTableItem(new ScriptErrorsTableLine(entry, lineNr, error));
        }
        SortedMap<Integer, String> warningMap = Compiler.getInstance().getWarnings();
        for (final Integer lineNr : warningMap.keySet()) {
          String warning = warningMap.get(lineNr);
          warningTable.addTableItem(new ScriptErrorsTableLine(entry, lineNr, warning));
        }
      } catch (Exception e) {
        e.printStackTrace();
        errorTable.addTableItem(new ScriptErrorsTableLine(entry, new Integer(0), "Fatal compile error"));
      }
      progress.setProgress(i + 1);
      if (progress.isCanceled()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation canceled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
        blocker.setBlocked(false);
        return;
      }
    }
    if (errorTable.getRowCount() + warningTable.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors or warnings found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      errorTable.tableComplete();
      warningTable.tableComplete();
      resultFrame = new ChildFrame("Result of script check", true);
      resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
      JScrollPane scrollErrorTable = new JScrollPane(errorTable);
      scrollErrorTable.getViewport().setBackground(errorTable.getBackground());
      JScrollPane scrollWarningTable = new JScrollPane(warningTable);
      scrollWarningTable.getViewport().setBackground(warningTable.getBackground());
      tabbedPane = new JTabbedPane();
      tabbedPane.addTab("Errors (" + errorTable.getRowCount() + ')', scrollErrorTable);
      tabbedPane.addTab("Warnings (" + warningTable.getRowCount() + ')', scrollWarningTable);
      tabbedPane.addChangeListener(this);
      bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
      bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
      bopen.setMnemonic('o');
      bopennew.setMnemonic('n');
      resultFrame.getRootPane().setDefaultButton(bopennew);
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bopen);
      panel.add(bopennew);
      JPanel pane = (JPanel)resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(tabbedPane, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.SOUTH);
      bopen.setEnabled(false);
      bopennew.setEnabled(false);
      errorTable.setFont(BrowserMenuBar.getInstance().getScriptFont());
      errorTable.getSelectionModel().addListSelectionListener(this);
      warningTable.setFont(BrowserMenuBar.getInstance().getScriptFont());
      warningTable.getSelectionModel().addListSelectionListener(this);
      MouseListener listener = new MouseAdapter()
      {
        public void mouseReleased(MouseEvent event)
        {
          if (event.getClickCount() == 2) {
            SortableTable table = (SortableTable)event.getSource();
            int row = table.getSelectedRow();
            if (row != -1) {
              ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
              Resource resource = ResourceFactory.getResource(resourceEntry);
              new ViewFrame(resultFrame, resource);
              ((BcsResource)resource).highlightText(((Integer)table.getValueAt(row, 2)).intValue(), null);
            }
          }
        }
      };
      errorTable.addMouseListener(listener);
      warningTable.addMouseListener(listener);
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


// -------------------------- INNER CLASSES --------------------------

  private static final class ScriptErrorsTableLine implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final Integer lineNr;
    private final String error;

    private ScriptErrorsTableLine(ResourceEntry resourceEntry, Integer lineNr, String error)
    {
      this.resourceEntry = resourceEntry;
      this.lineNr = lineNr;
      this.error = error;
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

