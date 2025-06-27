// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.ResultPane;
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
import org.infinity.util.Logger;
import org.infinity.util.Misc;

/** Performs checking {@link BcsResource BCS} & {@code BS} resources. */
public final class ScriptChecker extends AbstractSearcher
    implements Runnable, ActionListener, ListSelectionListener, ChangeListener {
  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private ChildFrame resultFrame;
  private JTabbedPane tabbedPane;
  private ResultPane<SortableTable> errorResultPane;
  private ResultPane<SortableTable> warningResultPane;

  /** List of the {@link ScriptErrorsTableLine} objects with compiler errors. */
  private SortableTable errorTable;

  /** List of the {@link ScriptErrorsTableLine} objects with compiler warnings. */
  private SortableTable warningTable;

  public ScriptChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    final ResultPane<SortableTable> resultsPane = getSelectedResultsPane();
    final SortableTable table = resultsPane.getTable();

    if (event.getSource() == resultsPane.getButton(BUTTON_OPEN)) {
      final int row = table.getSelectedRow();
      if (row != -1) {
        final SortableTable tableCapture = table;
        NearInfinity.getInstance().showResourceEntry(getResourceEntryAt(table, row),
            () -> ((BcsResource)NearInfinity.getInstance().getViewable())
                .highlightText(((Integer)tableCapture.getValueAt(row, 2)), null));
      }
    } else if (event.getSource() == resultsPane.getButton(BUTTON_OPEN_NEW)) {
      performTableAction(null);
    } else if (event.getSource() == resultsPane.getButton(BUTTON_SAVE)) {
      final String type = table == errorTable ? "Errors" : "Warnings";
      table.saveCheckResult(resultFrame, type + " in scripts");
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e) {
    performTableChanged(getSelectedResultsPane());
    resultFrame.getRootPane().setDefaultButton(getSelectedResultsPane().getButton(BUTTON_OPEN_NEW));
  }

  // --------------------- End Interface ChangeListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    performTableChanged(getSelectedResultsPane());
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
    final Class<?>[] colClasses = { ResourceEntry.class, String.class, Integer.class };
    errorTable = new SortableTable(new String[] { "Script", "Error message", "Line" }, colClasses,
        new Integer[] { 120, 440, 50 });
    warningTable = new SortableTable(new String[] { "Script", "Warning", "Line" }, colClasses,
        new Integer[] { 120, 440, 50 });

    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> scriptFiles = ResourceFactory.getResources("BCS");
      scriptFiles.addAll(ResourceFactory.getResources("BS"));

      if (runSearch("Checking scripts", scriptFiles)) {
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    if (errorTable.getRowCount() + warningTable.getRowCount() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors or warnings found", "Info",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    errorTable.tableComplete();
    warningTable.tableComplete();

    // setting up result panes
    for (int i = 0; i < 2; i++) {
      final JButton openButton = new JButton("Open", Icons.ICON_OPEN_16.getIcon());
      openButton.setMnemonic('o');
      openButton.setEnabled(false);

      final JButton openNewButton = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
      openNewButton.setMnemonic('n');
      openNewButton.setEnabled(false);

      final JButton saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
      saveButton.setMnemonic('s');

      final ResultPane<SortableTable> resultsPane;
      if (i == 0) {
        resultsPane =
            new ResultPane<>(errorTable, new JButton[] { openButton, openNewButton, saveButton }, null, true, true);
        errorResultPane = resultsPane;
      } else {
        resultsPane =
            new ResultPane<>(warningTable, new JButton[] { openButton, openNewButton, saveButton }, null, true, true);
        warningResultPane = resultsPane;
      }
      resultsPane.setOnActionPerformed(this::actionPerformed);
      resultsPane.setOnTableSelectionChanged(this::valueChanged);
      resultsPane.setOnTableAction(this::performTableAction);
    }

    // setting up result window
    resultFrame = new ChildFrame("Result of script check", true);
    resultFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());

    tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Errors (" + errorTable.getRowCount() + ')', errorResultPane);
    tabbedPane.addTab("Warnings (" + warningTable.getRowCount() + ')', warningResultPane);
    tabbedPane.addChangeListener(this);
    resultFrame.getRootPane().setDefaultButton(getSelectedResultsPane().getButton(BUTTON_OPEN_NEW));

    final JPanel pane = (JPanel) resultFrame.getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(tabbedPane, BorderLayout.CENTER);

    resultFrame.setPreferredSize(Misc.getScaledDimension(resultFrame.getPreferredSize()));
    resultFrame.pack();
    Center.center(resultFrame, NearInfinity.getInstance().getBounds());
    resultFrame.setVisible(true);
  }

  // --------------------- End Interface Runnable ---------------------

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    return () -> {
      try {
        final BcsResource script = new BcsResource(entry);
        final Decompiler decompiler = new Decompiler(script.getCode(), true);
        decompiler.setGenerateComments(false);
        decompiler.setGenerateResourcesUsed(false);

        final Compiler compiler = new Compiler(decompiler.decompile());
        compiler.compile();
        synchronized (this) {
          for (final ScriptMessage sm : compiler.getErrors()) {
            errorTable.addTableItem(
                new ScriptErrorsTableLine(entry, sm.getLine(), sm.getMessage(), ScriptErrorsTableLine.Type.ERROR));
          }
        }
        synchronized (this) {
          for (final ScriptMessage sm : compiler.getWarnings()) {
            warningTable.addTableItem(
                new ScriptErrorsTableLine(entry, sm.getLine(), sm.getMessage(), ScriptErrorsTableLine.Type.WARNING));
          }
        }
      } catch (Exception e) {
        Logger.error(e);
      }
      advanceProgress();
    };
  }

  /** Updates controls based on the table state in the specified {@link ResultPane}. */
  private void performTableChanged(ResultPane<SortableTable> resultsPane) {
    if (resultsPane == null) {
      return;
    }

    final ListSelectionModel model = resultsPane.getTable().getSelectionModel();
    final int row = model.getMinSelectionIndex();
    resultsPane.getButton(BUTTON_OPEN).setEnabled(row != -1);
    resultsPane.getButton(BUTTON_OPEN_NEW).setEnabled(row != -1);
    if (row != -1) {
      final ResourceEntry entry = getResourceEntryAt(resultsPane.getTable(), row);
      resultsPane.setStatusMessage(entry.getActualPath().toString());
    } else {
      resultsPane.setStatusMessage("");
    }
  }

  /**
   * Performs the default action on the selected results table as if the user double-clicked on a table row which opens
   * a new child window with the content of the resource specified in the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    final SortableTable table = getSelectedResultsPane().getTable();
    int row = table.getSelectedRow();
    if (row != -1) {
      final Resource resource = ResourceFactory.getResource(getResourceEntryAt(table, row));
      new ViewFrame(resultFrame, resource);
      ((BcsResource) resource).highlightText(((Integer) table.getValueAt(row, 2)), null);
    }
  }

  /**
   * Returns the {@link ResourceEntry} instance specified in the specified table row. Returns {@code null} if entry is
   * unavailable.
   */
  private ResourceEntry getResourceEntryAt(SortableTable table, int row) {
    ResourceEntry retVal = null;

    if (table != null && row >= 0 && row < table.getRowCount()) {
      final Object value = table.getValueAt(row, 0);
      if (value instanceof ResourceEntry) {
        retVal = (ResourceEntry)value;
      }
    }

    return retVal;
  }

  /** Returns the {@link ResultPane} of the currently selected tab. */
  private ResultPane<SortableTable> getSelectedResultsPane() {
    if (tabbedPane == null) {
      return errorResultPane;
    }
    return tabbedPane.getSelectedIndex() == 1 ? warningResultPane : errorResultPane;
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class ScriptErrorsTableLine implements TableItem {
    public enum Type {
      ERROR, WARNING,
    }

    private final ResourceEntry resourceEntry;
    private final Integer lineNr;
    private final String error;
    private final Type type;

    private ScriptErrorsTableLine(ResourceEntry resourceEntry, Integer lineNr, String error, Type type) {
      this.resourceEntry = resourceEntry;
      this.lineNr = lineNr;
      this.error = error;
      this.type = type;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      if (columnIndex == 0) {
        return resourceEntry;
      } else if (columnIndex == 1) {
        return error;
      }
      return lineNr;
    }

    @Override
    public String toString() {
      final String type = (this.type == Type.ERROR) ? "Error" : "Warning";
      return String.format("File: %s, Line: %d, %s: %s", resourceEntry.getResourceName(), lineNr, type, error);
    }
  }
}
