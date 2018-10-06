// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
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
import org.infinity.resource.bcs.ScriptMessage;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

/** Performs checking {@link DLG} resources. */
public final class DialogChecker extends AbstractChecker implements Runnable, ActionListener, ListSelectionListener, ChangeListener
{
  private final boolean checkOnlyOverride;
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  private JTabbedPane tabbedPane;
  /** List of the {@link ActionErrorsTableLine} objects with compiler errors in dialog actions. */
  private SortableTable errorTable;
  /** List of the {@link ActionErrorsTableLine} objects with compiler warnings in dialog actions. */
  private SortableTable warningTable;

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
      JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
      fc.setDialogTitle("Save result");
      fc.setSelectedFile(new File(fc.getCurrentDirectory(), "result.txt"));
      if (fc.showSaveDialog(resultFrame) == JFileChooser.APPROVE_OPTION) {
        Path output = fc.getSelectedFile().toPath();
        if (Files.exists(output)) {
          String[] options = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(resultFrame, output + " exists. Overwrite?",
                                           "Save result", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0) {
            return;
          }
        }
        try (BufferedWriter bw = Files.newBufferedWriter(output)) {
          bw.write("Result of triggers & actions check"); bw.newLine();
          if (table == errorTable) {
            bw.write("Number of errors: " + table.getRowCount()); bw.newLine();
          } else {
            bw.write("Number of warnings: " + table.getRowCount()); bw.newLine();
          }
          for (int i = 0; i < table.getRowCount(); i++) {
            bw.write(table.getTableItemAt(i).toString()); bw.newLine();
          }
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
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> dlgFiles = ResourceFactory.getResources("DLG");
      if (checkOnlyOverride) {
        for (Iterator<ResourceEntry> i = dlgFiles.iterator(); i.hasNext();) {
          ResourceEntry resourceEntry = i.next();
          if (!resourceEntry.hasOverride())
            i.remove();
        }
      }

      final Class<?>[] colClasses = {ResourceEntry.class, String.class, String.class, Integer.class};
      errorTable = new SortableTable(
          new String[]{"Dialogue", "Trigger/Action", "Error message", "Line"},
          colClasses,
          new Integer[]{50, 100, 350, 10});
      warningTable = new SortableTable(
          new String[]{"Dialogue", "Trigger/Action", "Warning", "Line"},
          colClasses,
          new Integer[]{50, 100, 350, 10});

      if (runCheck("Checking dialogue triggers & actions...", dlgFiles)) {
        return;
      }

      if (errorTable.getRowCount() + warningTable.getRowCount() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors or warnings found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        errorTable.tableComplete();
        warningTable.tableComplete();
        resultFrame = new ChildFrame("Result of triggers & actions check", true);
        resultFrame.setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
        bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
        bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
        bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
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
        errorTable.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
        errorTable.setRowHeight(errorTable.getFontMetrics(errorTable.getFont()).getHeight() + 1);
        errorTable.getSelectionModel().addListSelectionListener(this);
        warningTable.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
        warningTable.setRowHeight(warningTable.getFontMetrics(warningTable.getFont()).getHeight() + 1);
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
      blocker.setBlocked(false);
    }
  }

// --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    return () -> {
      try {
        final DlgResource dialog = new DlgResource(entry);
        for (int i = 0; i < dialog.getFieldCount(); ++i) {
          final StructEntry o = dialog.getField(i);
          if (o instanceof AbstractCode) {
            checkCode(entry, (AbstractCode)o);
          }
        }
      } catch (Exception e) {
        synchronized (System.err) {
          e.printStackTrace();
        }
      }
      advanceProgress();
    };
  }

  /**
   * Performs code checking. This method can be called from several threads
   *
   * @param entry Pointer to dialog resource for check. Never {@code null}
   * @param code Code of action or trigger in dialog. Never {@code null}
   *
   * @throws Exception If {@code script} contains invalid code
   */
  private void checkCode(ResourceEntry entry, AbstractCode code) {
    final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
    final Compiler compiler = new Compiler(code.toString(), type);
    compiler.getCode();
    for (final ScriptMessage sm : compiler.getErrors()) {
      synchronized (errorTable) {
        errorTable.addTableItem(new ActionErrorsTableLine(entry, code, sm.getLine(), sm.getMessage(),
                                                          ActionErrorsTableLine.Type.ERROR));
      }
    }
    for (final ScriptMessage sm : compiler.getWarnings()) {
      synchronized (warningTable) {
        warningTable.addTableItem(new ActionErrorsTableLine(entry, code, sm.getLine(), sm.getMessage(),
                                                            ActionErrorsTableLine.Type.WARNING));
      }
    }
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class ActionErrorsTableLine implements TableItem
  {
    public enum Type {
      ERROR,
      WARNING,
    }

    private final ResourceEntry resourceEntry;
    private final StructEntry structEntry;
    private final Integer lineNr;
    private final String error;
    private final Type type;

    private ActionErrorsTableLine(ResourceEntry resourceEntry, StructEntry structEntry, Integer lineNr,
                                  String error, Type type)
    {
      this.resourceEntry = resourceEntry;
      this.structEntry = structEntry;
      this.lineNr = lineNr;
      this.error = error;
      this.type = type;
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
      String type = (this.type == Type.ERROR) ? "Error" : "Warning";
      return String.format("File: %s  Type: %s  %s: %s  Line: %d",
                           resourceEntry.toString(), structEntry.getName(), type, error, lineNr);
    }
  }
}
