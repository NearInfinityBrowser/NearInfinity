// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.gui.*;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.bcs.Compiler;
import infinity.resource.dlg.*;
import infinity.resource.dlg.Action;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public final class DialogCheker implements Runnable, ActionListener, ListSelectionListener, ChangeListener
{
  private final boolean checkOnlyOverride;
  private ChildFrame resultFrame;
  private JButton bopen, bopennew;
  private JTabbedPane tabbedPane;
  private SortableTable errorTable, warningTable;

  public DialogCheker(boolean checkOnlyOverride)
  {
    this.checkOnlyOverride = checkOnlyOverride;
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
        ((AbstractStruct)NearInfinity.getInstance().getViewable()).getViewer().selectEntry(
                (String)table.getValueAt(row, 1));
      }
    }
    else if (event.getSource() == bopennew) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry)table.getValueAt(row, 0);
        Resource resource = ResourceFactory.getResource(resourceEntry);
        new ViewFrame(resultFrame, resource);
        ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  public void stateChanged(ChangeEvent event)
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
    List<ResourceEntry> dlgFiles = ResourceFactory.getInstance().getResources("DLG");
    if (checkOnlyOverride) {
      for (Iterator<ResourceEntry> i = dlgFiles.iterator(); i.hasNext();) {
        ResourceEntry resourceEntry = i.next();
        if (!resourceEntry.hasOverride())
          i.remove();
      }
    }
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Checking dialogue triggers & actions...", null, 0,
                                                   dlgFiles.size());
    errorTable = new SortableTable(new String[]{"Dialogue", "Trigger/Action", "Error message", "Line"},
                                   new Class[]{Object.class, Object.class, Object.class, Integer.class},
                                   new int[]{50, 100, 350, 10});
    warningTable = new SortableTable(new String[]{"Dialogue", "Trigger/Action", "Warning", "Line"},
                                     new Class[]{Object.class, Object.class, Object.class, Integer.class},
                                     new int[]{50, 100, 350, 10});
    for (int i = 0; i < dlgFiles.size(); i++) {
      ResourceEntry entry = dlgFiles.get(i);
      try {
        DlgResource dialog = new DlgResource(entry);
        for (int j = 0; j < dialog.getRowCount(); j++) {
          StructEntry o = dialog.getStructEntryAt(j);
          if (o instanceof AbstractCode) {
            AbstractCode dialogCode = (AbstractCode)o;
            Compiler.getInstance().compileDialogCode(dialogCode.toString(), dialogCode instanceof Action);
            SortedMap<Integer, String> errorMap = Compiler.getInstance().getErrors();
            for (final Integer lineNr : errorMap.keySet()) {
              String error = errorMap.get(lineNr);
              errorTable.addTableItem(new ActionErrorsTableLine(entry, dialogCode, lineNr, error));
            }
            SortedMap<Integer, String> warningMap = Compiler.getInstance().getWarnings();
            for (final Integer lineNr : warningMap.keySet()) {
              String warning = warningMap.get(lineNr);
              warningTable.addTableItem(new ActionErrorsTableLine(entry, dialogCode, lineNr, warning));
            }
          }
        }
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
    if (errorTable.getRowCount() + warningTable.getRowCount() == 0)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors or warnings found",
                                    "Info", JOptionPane.INFORMATION_MESSAGE);
    else {
      errorTable.tableComplete();
      warningTable.tableComplete();
      resultFrame = new ChildFrame("Result of triggers & actions check", true);
      resultFrame.setIconImage(Icons.getIcon("Refresh16.gif").getImage());
      bopen = new JButton("Open", Icons.getIcon("Open16.gif"));
      bopennew = new JButton("Open in new window", Icons.getIcon("Open16.gif"));
      JScrollPane scrollErrorTable = new JScrollPane(errorTable);
      scrollErrorTable.getViewport().setBackground(errorTable.getBackground());
      JScrollPane scrollWarningTable = new JScrollPane(warningTable);
      scrollWarningTable.getViewport().setBackground(warningTable.getBackground());
      tabbedPane = new JTabbedPane();
      tabbedPane.addTab("Errors (" + errorTable.getRowCount() + ')', scrollErrorTable);
      tabbedPane.addTab("Warnings (" + warningTable.getRowCount() + ')', scrollWarningTable);
      tabbedPane.addChangeListener(this);
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
              ((AbstractStruct)resource).getViewer().selectEntry((String)table.getValueAt(row, 1));
            }
          }
        }
      };
      errorTable.addMouseListener(listener);
      warningTable.addMouseListener(listener);
      bopen.addActionListener(this);
      bopennew.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.setSize(700, 600);
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
      resultFrame.setVisible(true);
    }
    blocker.setBlocked(false);
  }

// --------------------- End Interface Runnable ---------------------


// -------------------------- INNER CLASSES --------------------------

  private static final class ActionErrorsTableLine implements TableItem
  {
    private final ResourceEntry resourceEntry;
    private final StructEntry structEntry;
    private final Integer lineNr;
    private final String error;

    private ActionErrorsTableLine(ResourceEntry resourceEntry, StructEntry structEntry, Integer lineNr,
                                  String error)
    {
      this.resourceEntry = resourceEntry;
      this.structEntry = structEntry;
      this.lineNr = lineNr;
      this.error = error;
    }

    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return structEntry.getName();
      else if (columnIndex == 2)
        return error;
      return lineNr;
    }
  }
}

