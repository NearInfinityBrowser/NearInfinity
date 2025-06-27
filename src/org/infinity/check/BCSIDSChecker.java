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
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
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
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Logger;
import org.infinity.util.Misc;

/** Performs checking {@link BcsResource BCS} & {@code BS} resources. */
public final class BCSIDSChecker extends AbstractSearcher implements Runnable, ActionListener, ListSelectionListener {
  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private ChildFrame resultFrame;
  private ResultPane<SortableTable> resultPane;

  /** List of the {@link BCSIDSErrorTableLine} objects. */
  private SortableTable table;

  public BCSIDSChecker(Component parent) {
    super(CHECK_ONE_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == resultPane.getButton(BUTTON_OPEN)) {
      int row = table.getSelectedRow();
      if (row != -1) {
        ResourceEntry resourceEntry = (ResourceEntry) table.getValueAt(row, 0);
        NearInfinity.getInstance().showResourceEntry(resourceEntry, () -> {
          final BcsResource bcsfile = (BcsResource) NearInfinity.getInstance().getViewable();
          bcsfile.highlightText(((Integer) table.getValueAt(row, 2)), null);
        });
      }
    } else if (event.getSource() == resultPane.getButton(BUTTON_OPEN_NEW)) {
      performTableAction(null);
    } else if (event.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      table.saveCheckResult(resultFrame, "Unknown IDS references in BCS & BS files");
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    if (event.getSource() instanceof ListSelectionModel) {
      final ListSelectionModel model = (ListSelectionModel)event.getSource();
      final int row = model.getMinSelectionIndex();
      resultPane.getButton(BUTTON_OPEN).setEnabled(row != -1);
      resultPane.getButton(BUTTON_OPEN_NEW).setEnabled(row != -1);
      if (row != -1) {
        ResourceEntry entry = getResourceEntryAt(row);
        resultPane.setStatusMessage(entry.getActualPath().toString());
      } else {
        resultPane.setStatusMessage("");
      }
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
    table = new SortableTable(new String[] { "File", "Error message", "Line" },
        new Class<?>[] { ResourceEntry.class, String.class, Integer.class }, new Integer[] { 100, 300, 50 });

    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> bcsFiles = ResourceFactory.getResources("BCS");
      bcsFiles.addAll(ResourceFactory.getResources("BS"));

      if (runSearch("Checking", bcsFiles)) {
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    if (table.getRowCount() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unknown references found", "Info",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    table.tableComplete();

    final JButton openButton = new JButton("Open", Icons.ICON_OPEN_16.getIcon());
    openButton.setMnemonic('o');
    openButton.setEnabled(false);

    final JButton openNewButton = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
    openNewButton.setMnemonic('n');
    openNewButton.setEnabled(false);

    final JButton saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
    saveButton.setMnemonic('s');

    final String title = table.getRowCount() + " hits(s) found";

    resultPane = new ResultPane<>(table, new JButton[] { openButton, openNewButton, saveButton }, title, true, true);
    resultPane.setOnActionPerformed(this::actionPerformed);
    resultPane.setOnTableSelectionChanged(this::valueChanged);
    resultPane.setOnTableAction(this::performTableAction);

    resultFrame = new ChildFrame("Result of Check for Unknown IDS References", true);
    resultFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());
    resultFrame.getRootPane().setDefaultButton(openNewButton);

    final JPanel pane = (JPanel) resultFrame.getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(resultPane, BorderLayout.CENTER);

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
        checkScript(new BcsResource(entry));
      } catch (Exception e) {
        Logger.error(e);
      }
      advanceProgress();
    };
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the content of the resource specified in the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    final int row = table.getSelectedRow();
    if (row != -1) {
      final Resource resource = ResourceFactory.getResource(getResourceEntryAt(row));
      final ViewFrame viewFrame = new ViewFrame(resultFrame, resource);
      final BcsResource bcsfile = (BcsResource) viewFrame.getViewable();
      bcsfile.highlightText(((Integer) table.getValueAt(row, 2)), null);
    }
  }

  /**
   * Performs script checking. This method can be called from several threads
   *
   * @param script Script resource for check. Never {@code null}
   *
   * @throws Exception If {@code script} contains invalid code
   */
  private void checkScript(BcsResource script) throws Exception {
    final Decompiler decompiler = new Decompiler(script.getCode(), ScriptType.BCS, true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    decompiler.decompile();
    for (final Map.Entry<Integer, String> e : decompiler.getIdsErrors().entrySet()) {
      final Integer lineNr = e.getKey();
      final String error = e.getValue();
      if (!error.contains("GTIMES.IDS") && !error.contains("SCROLL.IDS") && !error.contains("SHOUTIDS.IDS")
          && !error.contains("SPECIFIC.IDS") && !error.contains("TIME.IDS")) {
        synchronized (this) {
          table.addTableItem(new BCSIDSErrorTableLine(script.getResourceEntry(), error, lineNr));
        }
      }
    }
  }

  /**
   * Returns the {@link ResourceEntry} instance specified in the specified table row. Returns {@code null} if entry is
   * unavailable.
   */
  private ResourceEntry getResourceEntryAt(int row) {
    ResourceEntry retVal = null;

    if (row >= 0 && row < table.getRowCount()) {
      final Object value = table.getValueAt(row, 0);
      if (value instanceof ResourceEntry) {
        retVal = (ResourceEntry)value;
      }
    }

    return retVal;
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class BCSIDSErrorTableLine implements TableItem {
    private final ResourceEntry resourceEntry;
    private final String error;
    private final Integer lineNr;

    private BCSIDSErrorTableLine(ResourceEntry resourceEntry, String error, Integer lineNr) {
      this.resourceEntry = resourceEntry;
      this.error = error;
      this.lineNr = lineNr;
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
      return String.format("File: %s, Line: %d, Error: %s", resourceEntry.getResourceName(), lineNr, error);
    }
  }
}
