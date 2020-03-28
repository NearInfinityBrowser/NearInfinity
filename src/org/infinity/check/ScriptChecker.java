// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptMessage;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Misc;

/** Performs checking {@link BcsResource BCS} & {@code BS} resources. */
public final class ScriptChecker extends AbstractSearcher implements Runnable, ActionListener, ListSelectionListener, ChangeListener
{
  private ChildFrame resultFrame;
  private JButton bopen, bopennew, bsave;
  private JTabbedPane tabbedPane;
  /** List of the {@link ScriptErrorsTableLine} objects with compiler errors. */
  private SortableTable errorTable;
  /** List of the {@link ScriptErrorsTableLine} objects with compiler warnings. */
  private SortableTable warningTable;

  public ScriptChecker(Component parent)
  {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
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
      final String type = table == errorTable ? "Errors" : "Warnings";
      table.saveCheckResult(resultFrame, type + " in scripts");
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
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> scriptFiles = ResourceFactory.getResources("BCS");
      scriptFiles.addAll(ResourceFactory.getResources("BS"));

      final Class<?>[] colClasses = {ResourceEntry.class, String.class, Integer.class};
      errorTable = new SortableTable(new String[]{"Script", "Error message", "Line"},
                                     colClasses,
                                     new Integer[]{120, 440, 50});
      warningTable = new SortableTable(new String[]{"Script", "Warning", "Line"},
                                       colClasses,
                                       new Integer[]{120, 440, 50});

      if (runSearch("Checking scripts", scriptFiles)) {
        return;
      }

      if (errorTable.getRowCount() + warningTable.getRowCount() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors or warnings found",
                                      "Info", JOptionPane.INFORMATION_MESSAGE);
      } else {
        errorTable.tableComplete();
        warningTable.tableComplete();
        resultFrame = new ChildFrame("Result of script check", true);
        resultFrame.setIconImage(Icons.getIcon(Icons.ICON_REFRESH_16).getImage());
        JScrollPane scrollErrorTable = new JScrollPane(errorTable);
        scrollErrorTable.getViewport().setBackground(errorTable.getBackground());
        JScrollPane scrollWarningTable = new JScrollPane(warningTable);
        scrollWarningTable.getViewport().setBackground(warningTable.getBackground());
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Errors (" + errorTable.getRowCount() + ')', scrollErrorTable);
        tabbedPane.addTab("Warnings (" + warningTable.getRowCount() + ')', scrollWarningTable);
        tabbedPane.addChangeListener(this);
        bopen = new JButton("Open", Icons.getIcon(Icons.ICON_OPEN_16));
        bopennew = new JButton("Open in new window", Icons.getIcon(Icons.ICON_OPEN_16));
        bsave = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
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
    } finally {
      blocker.setBlocked(false);
    }
  }

// --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry)
  {
    return () -> {
      try {
        final BcsResource script = new BcsResource(entry);
        final Decompiler decompiler = new Decompiler(script.getCode(), true);
        decompiler.setGenerateComments(false);
        decompiler.setGenerateResourcesUsed(false);

        final Compiler compiler = new Compiler(decompiler.decompile());
        compiler.compile();
        for (final ScriptMessage sm : compiler.getErrors()) {
          synchronized (errorTable) {
            errorTable.addTableItem(new ScriptErrorsTableLine(entry, sm.getLine(), sm.getMessage(),
                                                              ScriptErrorsTableLine.Type.ERROR));
          }
        }
        for (final ScriptMessage sm : compiler.getWarnings()) {
          synchronized (warningTable) {
            warningTable.addTableItem(new ScriptErrorsTableLine(entry, sm.getLine(), sm.getMessage(),
                                                                ScriptErrorsTableLine.Type.WARNING));
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

// -------------------------- INNER CLASSES --------------------------

  private static final class ScriptErrorsTableLine implements TableItem
  {
    public enum Type {
      ERROR,
      WARNING,
    }

    private final ResourceEntry resourceEntry;
    private final Integer lineNr;
    private final String error;
    private final Type type;

    private ScriptErrorsTableLine(ResourceEntry resourceEntry, Integer lineNr, String error, Type type)
    {
      this.resourceEntry = resourceEntry;
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
        return error;
      return lineNr;
    }

    @Override
    public String toString()
    {
      final String type = (this.type == Type.ERROR) ? "Error" : "Warning";
      return String.format("File: %s, Line: %d, %s: %s",
                           resourceEntry.getResourceName(), lineNr, type, error);
    }
  }
}
