// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.IsNumeric;
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
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.Item;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Logger;
import org.infinity.util.Misc;

/** Performs checking {@link CreResource CRE} & {@code CHR} resources. */
public final class CreInvChecker extends AbstractSearcher implements Runnable, ActionListener, ListSelectionListener {
  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private ChildFrame resultFrame;
  private ResultPane<SortableTable> resultPane;

  /** List of the {@link CreInvError} objects. */
  private SortableTable table;

  public CreInvChecker(Component parent) {
    super(CHECK_ONE_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == resultPane.getButton(BUTTON_OPEN)) {
      int row = table.getSelectedRow();
      if (row != -1) {
        NearInfinity.getInstance().showResourceEntry(getResourceEntryAt(row),
            () -> ((AbstractStruct)NearInfinity.getInstance().getViewable()).getViewer()
                .selectEntry(((Item)table.getValueAt(row, 2)).getName()));
      }
    } else if (event.getSource() == resultPane.getButton(BUTTON_OPEN_NEW)) {
      performTableAction(null);
    } else if (event.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      table.saveCheckResult(resultFrame, "CRE items not in inventory");
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
    table = new SortableTable(new String[] { "File", "Name", "Item" },
        new Class<?>[] { ResourceEntry.class, String.class, Item.class }, new Integer[] { 100, 100, 200 });

    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> creFiles = ResourceFactory.getResources("CRE");
      creFiles.addAll(ResourceFactory.getResources("CHR"));

      if (runSearch("Checking inventories", creFiles)) {
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    if (table.getRowCount() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No hits found", "Info",
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

    final String title = table.getRowCount() + " hit(s) found";

    resultPane = new ResultPane<>(table, new JButton[] { openButton, openNewButton, saveButton }, title, true, true);
    resultPane.setOnActionPerformed(this::actionPerformed);
    resultPane.setOnTableSelectionChanged(this::valueChanged);
    resultPane.setOnTableAction(this::performTableAction);

    resultFrame = new ChildFrame("Result of CRE inventory check", true);
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
        checkCreature(new CreResource(entry));
      } catch (Exception e) {
        Logger.error(e);
      }
      advanceProgress();
    };
  }

  private void checkCreature(CreResource cre) {
    final List<Item> items = new ArrayList<>();
    final List<DecNumber> slots = new ArrayList<>();
    final int slotsOffset = ((IsNumeric) cre.getAttribute(CreResource.CRE_OFFSET_ITEM_SLOTS)).getValue() + cre.getOffset();

    // Gathering item and slot entries
    for (final StructEntry entry : cre.getFields()) {
      if (entry instanceof Item) {
        items.add((Item) entry);
      } else if (entry.getOffset() >= slotsOffset
          && entry instanceof DecNumber
          && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_SLOT)
          && !entry.getName().equals(CreResource.CRE_SELECTED_WEAPON_ABILITY)) {
        slots.add((DecNumber) entry);
      }
    }

    // Checking item references
    for (final DecNumber slot : slots) {
      final int value = slot.getValue();
      if (value >= 0 && value < items.size()) {
        items.set(value, null);
      }
    }

    // Evaluating results
    synchronized (this) {
      for (final Item item : items) {
        if (item != null) {
          table.addTableItem(new CreInvError(cre.getResourceEntry(), item));
        }
      }
    }
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the content of the resource specified in the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    final int row = table.getSelectedRow();
    if (row != -1) {
      final Resource resource = ResourceFactory.getResource(getResourceEntryAt(row));
      new ViewFrame(resultFrame, resource);
      ((AbstractStruct) resource).getViewer().selectEntry(((Item) table.getValueAt(row, 2)).getName());
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

  private static final class CreInvError implements TableItem {
    private final ResourceEntry resourceEntry;
    private final Item itemRef;

    private CreInvError(ResourceEntry resourceEntry, Item itemRef) {
      this.resourceEntry = resourceEntry;
      this.itemRef = itemRef;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      if (columnIndex == 0) {
        return resourceEntry;
      } else if (columnIndex == 1) {
        return resourceEntry.getSearchString();
      } else {
        return itemRef;
      }
    }

    @Override
    public String toString() {
      return String.format("File: %s, Name: %s, %s", resourceEntry.getResourceName(), resourceEntry.getSearchString(),
          itemRef.toString());
    }
  }
}
