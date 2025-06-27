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
import org.infinity.resource.AbstractStruct;
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
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Logger;
import org.infinity.util.Misc;

/** Performs checking {@link DlgResource DLG} resources. */
public final class DialogChecker extends AbstractSearcher
    implements Runnable, ActionListener, ListSelectionListener, ChangeListener {
  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private final boolean checkOnlyOverride;

  private ChildFrame resultFrame;
  private JTabbedPane tabbedPane;
  private ResultPane<SortableTable> errorResultPane;
  private ResultPane<SortableTable> warningResultPane;

  /** List of the {@link Problem} objects with compiler errors in dialog actions. */
  private SortableTable errorTable;

  /** List of the {@link Problem} objects with compiler warnings in dialog actions. */
  private SortableTable warningTable;

  public DialogChecker(boolean checkOnlyOverride, Component parent) {
    super(CHECK_ONE_TYPE_FORMAT, parent);
    this.checkOnlyOverride = checkOnlyOverride;
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    final ResultPane<SortableTable> resultPane = getSelectedResultsPane();
    final SortableTable table = resultPane.getTable();

    if (event.getSource() == resultPane.getButton(BUTTON_OPEN)) {
      int row = table.getSelectedRow();
      if (row != -1) {
        final SortableTable tableCapture = table;
        NearInfinity.getInstance().showResourceEntry(getResourceEntryAt(table, row),
            () -> ((AbstractStruct)NearInfinity.getInstance().getViewable()).getViewer()
                .selectEntry((String)tableCapture.getValueAt(row, 1)));
      }
    } else if (event.getSource() == resultPane.getButton(BUTTON_OPEN_NEW)) {
      performTableAction(null);
    } else if (event.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      final String type = (table == errorTable) ? "Errors" : "Warnings";
      table.saveCheckResult(resultFrame, type + " in dialogues");
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
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
    final Class<?>[] colClasses = { ResourceEntry.class, String.class, String.class, Integer.class };
    errorTable = new SortableTable(new String[] { "Dialogue", "Field", "Error message", "Line" }, colClasses,
        new Integer[] { 50, 100, 350, 10 });
    warningTable = new SortableTable(new String[] { "Dialogue", "Field", "Warning", "Line" }, colClasses,
        new Integer[] { 50, 100, 350, 10 });

    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> dlgFiles = ResourceFactory.getResources("DLG");
      if (checkOnlyOverride) {
        dlgFiles.removeIf(resourceEntry -> !resourceEntry.hasOverride());
      }

      if (runSearch("Checking dialogues", dlgFiles)) {
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

      final ResultPane<SortableTable> resultPane;
      if (i == 0) {
        resultPane =
            new ResultPane<>(errorTable, new JButton[] { openButton, openNewButton, saveButton }, null, true, true);
        errorResultPane = resultPane;
      } else {
        resultPane =
            new ResultPane<>(warningTable, new JButton[] { openButton, openNewButton, saveButton }, null, true, true);
        warningResultPane = resultPane;
      }
      resultPane.setOnActionPerformed(this::actionPerformed);
      resultPane.setOnTableSelectionChanged(this::valueChanged);
      resultPane.setOnTableAction(this::performTableAction);
    }

    // setting up result window
    resultFrame = new ChildFrame("Result of dialogues check", true);
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
        final DlgResource dialog = new DlgResource(entry);
        for (final StructEntry o : dialog.getFields()) {
          if (o instanceof AbstractCode) {
            checkCode(entry, (AbstractCode) o);
          }
        }
      } catch (Exception e) {
        Logger.error(e);
      }
      advanceProgress();
    };
  }

  /**
   * Performs code checking. This method can be called from several threads
   *
   * @param entry Pointer to dialog resource for check. Never {@code null}
   * @param code  Code of action or trigger in dialog. Never {@code null}
   */
  private void checkCode(ResourceEntry entry, AbstractCode code) {
    final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
    final Compiler compiler = new Compiler(code.getText(), type);
    compiler.compile();
    synchronized (this) {
      for (final ScriptMessage sm : compiler.getErrors()) {
        errorTable.addTableItem(new Problem(entry, code, sm.getLine(), sm.getMessage(), Problem.Type.ERROR));
      }
    }
    synchronized (this) {
      for (final ScriptMessage sm : compiler.getWarnings()) {
        warningTable.addTableItem(new Problem(entry, code, sm.getLine(), sm.getMessage(), Problem.Type.WARNING));
      }
    }
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
      ResourceEntry entry = getResourceEntryAt(resultsPane.getTable(), row);
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
    final int row = table.getSelectedRow();
    if (row != -1) {
      final Resource resource = ResourceFactory.getResource(getResourceEntryAt(table, row));
      new ViewFrame(resultFrame, resource);
      ((AbstractStruct) resource).getViewer().selectEntry((String) table.getValueAt(row, 1));
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

  private static final class Problem implements TableItem {
    public enum Type {
      ERROR, WARNING,
    }

    /** Resource in which problem is found. */
    private final ResourceEntry resourceEntry;
    /** Entry in resource, in which problem is found. */
    private final StructEntry problemEntry;
    /** If problem in code block, then this is line with problem, otherwize {@code null}. */
    private final Integer lineNr;
    /** Description of a problem. */
    private final String message;
    /** Problem severity. */
    private final Type type;

    private Problem(ResourceEntry resourceEntry, StructEntry problemEntry, Integer lineNr, String message, Type type) {
      this.resourceEntry = resourceEntry;
      this.problemEntry = problemEntry;
      this.lineNr = lineNr;
      this.message = message;
      this.type = type;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return resourceEntry;
        case 1:
          return problemEntry.getName();
        case 2:
          return message;
        default:
          return lineNr;
      }
    }

    @Override
    public String toString() {
      final String type = (this.type == Type.ERROR) ? "Error" : "Warning";
      if (lineNr == null) {
        return String.format("File: %s, Owner: %s, %s: %s", resourceEntry.getResourceName(), problemEntry.getName(),
            type, message);
      }
      return String.format("File: %s, Line: %d, Owner: %s, %s: %s", resourceEntry.getResourceName(), lineNr,
          problemEntry.getName(), type, message);
    }
  }
}
