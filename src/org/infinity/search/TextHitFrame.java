// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

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
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.TextResource;
import org.infinity.resource.Viewable;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

public final class TextHitFrame extends ChildFrame implements ActionListener, ListSelectionListener {
  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private final Component parent;
  private final ResultPane<SortableTable> resultPane;
  private final String query;

  public TextHitFrame(final String query, Component parent) {
    super("Search Result", true);
    this.query = query;
    this.parent = parent;
    setIconImage(Icons.ICON_HISTORY_16.getIcon().getImage());

    final SortableTable table = new SortableTable(new String[] { "File", "Text", "Line" },
        new Class<?>[] { ResourceEntry.class, String.class, Integer.class }, new Integer[] { 100, 300, 50 });

    final JButton openButton = new JButton("Open", Icons.ICON_OPEN_16.getIcon());
    openButton.setMnemonic('o');
    openButton.setEnabled(false);

    final JButton openNewButton = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
    openNewButton.setMnemonic('n');
    openNewButton.setEnabled(false);
    getRootPane().setDefaultButton(openNewButton);

    final JButton saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
    saveButton.setMnemonic('s');

    final String title = table.getRowCount() + " hits found";

    resultPane = new ResultPane<>(table, new JButton[] { openButton, openNewButton, saveButton }, title, true, true);
    resultPane.setOnActionPerformed(this::actionPerformed);
    resultPane.setOnTableSelectionChanged(this::valueChanged);
    resultPane.setOnTableAction(this::performTableAction);

    final JPanel pane = (JPanel) getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(resultPane, BorderLayout.CENTER);

    setPreferredSize(Misc.getScaledDimension(getPreferredSize()));
    pack();
    Center.center(this, parent.getBounds());
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == resultPane.getButton(BUTTON_OPEN)) {
      final int row = resultPane.getTable().getSelectedRow();
      if (row != -1) {
        final ResourceEntry entry = getResourceEntryAt(row);
        if (parent instanceof ViewFrame && parent.isVisible()) {
          final Resource res = ResourceFactory.getResource(entry);
          ((ViewFrame) parent).setViewable(res);
          if (res instanceof TextResource) {
            ((TextResource) res).highlightText(((Integer) resultPane.getTable().getValueAt(row, 2)), query);
          }
        } else {
          NearInfinity.getInstance().showResourceEntry(entry, () -> {
            final Viewable viewable = NearInfinity.getInstance().getViewable();
            if (viewable instanceof TextResource) {
              ((TextResource) viewable).highlightText(((Integer) resultPane.getTable().getValueAt(row, 2)), query);
            }
          });
        }
      }
    } else if (event.getSource() == resultPane.getButton(BUTTON_OPEN_NEW)) {
      performTableAction(null);
    } else if (event.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      resultPane.getTable().saveSearchResult(this, getFilteredQuery(query));
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
        final ResourceEntry entry = getResourceEntryAt(row);
        resultPane.setStatusMessage(entry.getActualPath().toString());
      } else {
        resultPane.setStatusMessage("");
      }
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  @Override
  public void setVisible(boolean b) {
    resultPane.getTable().tableComplete();
    final int rowCount = resultPane.getTable().getRowCount();
    resultPane.setTitle(rowCount + " hit(s) found");
    if (b && rowCount == 0) {
      JOptionPane.showMessageDialog(parent, "No hits found", "Info", JOptionPane.INFORMATION_MESSAGE);
    } else {
      super.setVisible(b);
    }
  }

  public void addHit(ResourceEntry entry, String line, int lineNr) {
    resultPane.getTable().addTableItem(new TextHit(entry, line.trim(), lineNr));
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the content of the resource specified in the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    final int row = resultPane.getTable().getSelectedRow();
    if (row != -1) {
      final Resource res = ResourceFactory.getResource(getResourceEntryAt(row));
      new ViewFrame(this, res);
      if (res instanceof TextResource) {
        ((TextResource) res).highlightText(((Integer) resultPane.getTable().getValueAt(row, 2)), query);
      }
    }
  }

  /**
   * Returns the {@link ResourceEntry} instance specified in the specified table row. Returns {@code null} if entry is
   * unavailable.
   */
  private ResourceEntry getResourceEntryAt(int row) {
    ResourceEntry retVal = null;

    if (row >= 0 && row < resultPane.getTable().getRowCount()) {
      final Object value = resultPane.getTable().getValueAt(row, 0);
      if (value instanceof ResourceEntry) {
        retVal = (ResourceEntry)value;
      }
    }

    return retVal;
  }

  /** Removes regular expression quotation markers if they enclose the whole string. */
  private static String getFilteredQuery(String query) {
    if (query == null) {
      return "(null)";
    }
    String retVal = query;
    retVal = retVal.replaceFirst("^\\\\Q", "");
    retVal = retVal.replaceFirst("\\\\E$", "");
    return retVal;
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class TextHit implements TableItem {
    private final ResourceEntry entry;
    private final String line;
    private final Integer linenr;

    private TextHit(ResourceEntry entry, String name, int linenr) {
      this.entry = entry;
      this.line = name;
      this.linenr = linenr;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      if (columnIndex == 0) {
        return entry;
      } else if (columnIndex == 1) {
        return line;
      }
      return linenr;
    }

    @Override
    public String toString() {
      return String.format("File: %s, Line: %d, Text: %s", entry.getResourceName(), linenr, line);
    }
  }
}
