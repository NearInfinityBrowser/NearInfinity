// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ThreadPoolExecutor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Debugging;
import org.infinity.util.Misc;
import org.infinity.util.io.FileNI;
import org.infinity.util.io.FileWriterNI;
import org.infinity.util.io.PrintWriterNI;

public final class DialogChecker implements Runnable, ActionListener, ListSelectionListener, ChangeListener
{
  private static final String FMT_PROGRESS = "Checking resource %d/%d";

  private final boolean checkOnlyOverride;
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  private JTabbedPane tabbedPane;
  private SortableTable errorTable, warningTable;
  private ProgressMonitor progress;
  private int progressIndex;
  private List<ResourceEntry> dlgFiles;

  public DialogChecker(boolean checkOnlyOverride)
  {
    this.checkOnlyOverride = checkOnlyOverride;
    new Thread(this).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
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
    else if (event.getSource() == bsave) {
      JFileChooser fc = new JFileChooser(Profile.getGameRoot());
      fc.setDialogTitle("Save result");
      fc.setSelectedFile(new FileNI("result.txt"));
      if (fc.showSaveDialog(resultFrame) == JFileChooser.APPROVE_OPTION) {
        File output = fc.getSelectedFile();
        if (output.exists()) {
          String[] options = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(resultFrame, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0) {
            return;
          }
        }
        try {
          PrintWriter pw = new PrintWriterNI(new BufferedWriter(new FileWriterNI(output)));
          pw.println("Result of triggers & actions check");
          if (table == errorTable) {
            pw.println("Number of errors: " + table.getRowCount());
          } else {
            pw.println("Number of warnings: " + table.getRowCount());
          }
          for (int i = 0; i < table.getRowCount(); i++) {
            pw.println(table.getTableItemAt(i).toString());
          }
          pw.close();
          JOptionPane.showMessageDialog(resultFrame, "Result saved to " + output,
                                        "Save complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ioe) {
          JOptionPane.showMessageDialog(resultFrame, "Error while savig " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          ioe.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  @Override
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

  @Override
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

  @Override
  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      ThreadPoolExecutor executor = Misc.createThreadPool();
      dlgFiles = ResourceFactory.getResources("DLG");
      if (checkOnlyOverride) {
        for (Iterator<ResourceEntry> i = dlgFiles.iterator(); i.hasNext();) {
          ResourceEntry resourceEntry = i.next();
          if (!resourceEntry.hasOverride())
            i.remove();
        }
      }
      progressIndex = 0;
      progress = new ProgressMonitor(NearInfinity.getInstance(), "Checking dialogue triggers & actions...",
                                     String.format(FMT_PROGRESS, dlgFiles.size(), dlgFiles.size()),
                                     0, dlgFiles.size());
      progress.setNote(String.format(FMT_PROGRESS, 0, dlgFiles.size()));

      List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(4);
      colClasses.add(Object.class); colClasses.add(Object.class); colClasses.add(Object.class);
      colClasses.add(Integer.class);
      errorTable = new SortableTable(
          Arrays.asList(new String[]{"Dialogue", "Trigger/Action", "Error message", "Line"}),
          colClasses, Arrays.asList(new Integer[]{50, 100, 350, 10}));
      warningTable = new SortableTable(
          Arrays.asList(new String[]{"Dialogue", "Trigger/Action", "Warning", "Line"}),
          colClasses, Arrays.asList(new Integer[]{50, 100, 350, 10}));

      boolean isCancelled = false;
      Debugging.timerReset();
      for (int i = 0; i < dlgFiles.size(); i++) {
        Misc.isQueueReady(executor, true, -1);
        executor.execute(new Worker(dlgFiles.get(i)));
        if (progress.isCanceled()) {
          isCancelled = true;
          break;
        }
      }

      // enforcing thread termination if process has been cancelled
      if (isCancelled) {
        executor.shutdownNow();
      } else {
        executor.shutdown();
      }

      // waiting for pending threads to terminate
      while (!executor.isTerminated()) {
        if (!isCancelled && progress.isCanceled()) {
          executor.shutdownNow();
          isCancelled = true;
        }
        try { Thread.sleep(1); } catch (InterruptedException e) {}
      }

      if (isCancelled) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Operation cancelled",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
        return;
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
        bsave = new JButton("Save...", Icons.getIcon("Save16.gif"));
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
        bsave.setMnemonic('s');
        resultFrame.getRootPane().setDefaultButton(bopennew);
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.add(bopen);
        panel.add(bopennew);
        panel.add(bsave);
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
          @Override
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
        bsave.addActionListener(this);
        pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        resultFrame.setSize(700, 600);
        Center.center(resultFrame, NearInfinity.getInstance().getBounds());
        resultFrame.setVisible(true);
      }
    } finally {
      advanceProgress(true);
      blocker.setBlocked(false);
      if (dlgFiles != null) {
        dlgFiles.clear();
        dlgFiles = null;
      }
    }
    Debugging.timerShow("Check completed", Debugging.TimeFormat.MILLISECONDS);
  }

// --------------------- End Interface Runnable ---------------------

  private synchronized void advanceProgress(boolean finished)
  {
    if (progress != null) {
      if (finished) {
        progressIndex = 0;
        progress.close();
        progress = null;
      } else {
        progressIndex++;
        if (progressIndex % 100 == 0) {
          progress.setNote(String.format(FMT_PROGRESS, progressIndex, dlgFiles.size()));
        }
        progress.setProgress(progressIndex);
      }
    }
  }

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

    @Override
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

    @Override
    public String toString()
    {
      return String.format("File: %1$s  Type: %2$s  Error: %3$s  Line: %4$d",
                           resourceEntry.toString(), structEntry.getName(), error, lineNr);
    }
  }

  private class Worker implements Runnable
  {
    private final ResourceEntry entry;

    public Worker(ResourceEntry entry)
    {
      this.entry = entry;
    }

    @Override
    public void run()
    {
      if (entry != null) {
        try {
          DlgResource dialog = new DlgResource(entry);
          for (int j = 0; j < dialog.getFieldCount(); j++) {
            StructEntry o = dialog.getField(j);
            if (o instanceof AbstractCode) {
              AbstractCode dialogCode = (AbstractCode)o;
              Compiler compiler = new Compiler(dialogCode.toString(),
                                               (dialogCode instanceof Action) ? Compiler.ScriptType.Action :
                                                                                Compiler.ScriptType.Trigger);
              compiler.getCode();
              SortedMap<Integer, String> errorMap = compiler.getErrors();
              for (final Integer lineNr : errorMap.keySet()) {
                String error = errorMap.get(lineNr);
                synchronized (errorTable) {
                  errorTable.addTableItem(new ActionErrorsTableLine(entry, dialogCode, lineNr, error));
                }
              }
              SortedMap<Integer, String> warningMap = compiler.getWarnings();
              for (final Integer lineNr : warningMap.keySet()) {
                String warning = warningMap.get(lineNr);
                synchronized (warningTable) {
                  warningTable.addTableItem(new ActionErrorsTableLine(entry, dialogCode, lineNr, warning));
                }
              }
            }
          }
        } catch (Exception e) {
          synchronized (System.err) {
            e.printStackTrace();
          }
        }
      }
      advanceProgress(false);
    }
  }
}

