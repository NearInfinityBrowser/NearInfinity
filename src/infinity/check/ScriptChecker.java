// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.check;

import infinity.NearInfinity;
import infinity.gui.BrowserMenuBar;
import infinity.gui.Center;
import infinity.gui.ChildFrame;
import infinity.gui.SortableTable;
import infinity.gui.TableItem;
import infinity.gui.ViewFrame;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.Profile;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.bcs.BcsResource;
import infinity.resource.bcs.Compiler;
import infinity.resource.bcs.Decompiler;
import infinity.resource.key.ResourceEntry;
import infinity.util.io.FileNI;
import infinity.util.io.FileWriterNI;
import infinity.util.io.PrintWriterNI;

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
import java.util.List;
import java.util.SortedMap;

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

public final class ScriptChecker implements Runnable, ActionListener, ListSelectionListener, ChangeListener
{
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  private JTabbedPane tabbedPane;
  private SortableTable errorTable, warningTable;

  public ScriptChecker()
  {
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
    else if (event.getSource() == bsave) {
      JFileChooser fc = new JFileChooser(Profile.getGameRoot());
      fc.setDialogTitle("Save search result");
      fc.setSelectedFile(new FileNI("result.txt"));
      if (fc.showSaveDialog(resultFrame) == JFileChooser.APPROVE_OPTION) {
        File output = fc.getSelectedFile();
        if (output.exists()) {
          String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(resultFrame, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
            return;
        }
        try {
          PrintWriter pw = new PrintWriterNI(new BufferedWriter(new FileWriterNI(output)));
          pw.println("Result of script check");
          if (table == errorTable) {
            pw.println("Number of errors: " + table.getRowCount());
          } else {
            pw.println("Number of warnings: " + table.getRowCount());
          }
          for (int i = 0; i < table.getRowCount(); i++)
            pw.println(table.getTableItemAt(i).toString());
          pw.close();
          JOptionPane.showMessageDialog(resultFrame, "Result saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(resultFrame, "Error while saving " + output,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  @Override
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
    List<ResourceEntry> scriptFiles = ResourceFactory.getResources("BCS");
    scriptFiles.addAll(ResourceFactory.getResources("BS"));
    ProgressMonitor progress = new ProgressMonitor(NearInfinity.getInstance(),
                                                   "Checking scripts...", null, 0, scriptFiles.size());

    List<Class<? extends Object>> colClasses = new ArrayList<Class<? extends Object>>(3);
    colClasses.add(Object.class); colClasses.add(Object.class); colClasses.add(Integer.class);
    errorTable = new SortableTable(Arrays.asList(new String[]{"Script", "Error message", "Line"}),
                                   colClasses, Arrays.asList(new Integer[]{120, 440, 50}));
    warningTable = new SortableTable(Arrays.asList(new String[]{"Script", "Warning", "Line"}),
                                     colClasses, Arrays.asList(new Integer[]{120, 440, 50}));

    for (int i = 0; i < scriptFiles.size(); i++) {
      ResourceEntry entry = scriptFiles.get(i);
      try {
        BcsResource script = new BcsResource(entry);
        Decompiler decompiler = new Decompiler(script.getCode(), true);
        String decompiled = decompiler.getSource();
        Compiler compiler = new Compiler(decompiled);
        compiler.compile();
        SortedMap<Integer, String> errorMap = compiler.getErrors();
        for (final Integer lineNr : errorMap.keySet()) {
          String error = errorMap.get(lineNr);
          errorTable.addTableItem(new ScriptErrorsTableLine(entry, lineNr, error));
        }
        SortedMap<Integer, String> warningMap = compiler.getWarnings();
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
      bsave = new JButton("Save...", Icons.getIcon("Save16.gif"));
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
              ((BcsResource)resource).highlightText(((Integer)table.getValueAt(row, 2)).intValue(), null);
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

    @Override
    public Object getObjectAt(int columnIndex)
    {
      if (columnIndex == 0)
        return resourceEntry;
      else if (columnIndex == 1)
        return error;
      return lineNr;
    }

    @Override
    public String toString()
    {
      return String.format("File: %1$s  Error: %2$s  Line: %3$d",
                           resourceEntry.toString(), error, lineNr);
    }
  }
}

