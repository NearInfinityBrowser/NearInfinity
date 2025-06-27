// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.Objects;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
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
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.TextResource;
import org.infinity.resource.Viewable;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

public final class ReferenceHitFrame extends ChildFrame implements ActionListener, ListSelectionListener {
  private static final String QUERY_STRING = "string reference";

  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private final Component parent;
  private final Object query;

  private final ResultPane<SortableTable> resultPane;

  public ReferenceHitFrame(Object query, Component parent) {
    super("Result", true);
    this.query = (query != null) ? query : QUERY_STRING;
    this.parent = parent;
    setIconImage(Icons.ICON_HISTORY_16.getIcon().getImage());

    final SortableTable table = new SortableTable(new String[] { "File", "Name/Text", "Attribute/Line" },
        new Class<?>[] { ResourceEntry.class, String.class, String.class }, new Integer[] { 100, 100, 300 });

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
          showEntryInViewer(row, res);
          if (res instanceof DlgResource) {
            ((ViewFrame) parent).toFront();
          }
        } else {
          NearInfinity.getInstance().showResourceEntry(entry, () -> {
            final Viewable viewable = NearInfinity.getInstance().getViewable();
            showEntryInViewer(row, viewable);
            if (viewable instanceof DlgResource) {
              NearInfinity.getInstance().toFront();
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

  public void addHit(ResourceEntry entry, String name, StructEntry ref) {
    resultPane.getTable().addTableItem(new ReferenceHit(entry, name, ref));
  }

  public void addHit(ResourceEntry entry, String line, int lineNr) {
    resultPane.getTable().addTableItem(new ReferenceHit(entry, line, lineNr));
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the content of the resource specified in the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    final int row = resultPane.getTable().getSelectedRow();
    if (row != -1) {
      Resource res = ResourceFactory.getResource(getResourceEntryAt(row));
      new ViewFrame(this, res);
      showEntryInViewer(row, res);
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

  private void showEntryInViewer(int row, Viewable viewable) {
    final ReferenceHit hit = (ReferenceHit) resultPane.getTable().getTableItemAt(row);
    if (viewable instanceof DlgResource) {
      final DlgResource dlgRes = (DlgResource) viewable;
      final JComponent detailViewer = dlgRes.getViewerTab(0);
      final JTabbedPane parent = (JTabbedPane) detailViewer.getParent();
      dlgRes.selectInEdit(hit.getStructEntry());
      // make sure we see the detail viewer
      parent.getModel().setSelectedIndex(parent.indexOfComponent(detailViewer));
    } else if (viewable instanceof AbstractStruct) {
      ((AbstractStruct) viewable).getViewer().selectEntry(hit.getStructEntry().getOffset());
    } else if (viewable instanceof TextResource) {
      ((TextResource) viewable).highlightText(hit.getLineNr(), hit.getLine());
    }
  }

  /** Removes regular expression quotation markers if they enclose the whole string. */
  private static String getFilteredQuery(Object query) {
    if (query == null) {
      return "(null)";
    }
    String retVal = query.toString();
    retVal = retVal.replaceFirst("^\\\\Q", "");
    retVal = retVal.replaceFirst("\\\\E$", "");
    return retVal;
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Stores a reference to a specific resource field. */
  public static final class ReferenceHit implements TableItem, Comparable<ReferenceHit> {
    public enum Mode {
      Struct, Text,
    }

    private final Mode mode;
    private final ResourceEntry entry;
    private final String name;
    private final StructEntry ref;
    private final String line;
    private final int lineNr;

    public ReferenceHit(ResourceEntry entry, String name, StructEntry ref) {
      this.mode = Mode.Struct;
      this.entry = entry;
      this.name = name;
      this.ref = ref;
      this.line = "";
      this.lineNr = 0;
    }

    public ReferenceHit(ResourceEntry entry, String line, int lineNr) {
      this.mode = Mode.Text;
      this.entry = entry;
      this.name = "";
      this.ref = null;
      this.line = line;
      this.lineNr = lineNr;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return entry;
        case 1:
          if (mode == Mode.Text) {
            return line;
          } else {
            if (name != null) {
              return name;
            } else if (entry instanceof FileResourceEntry) {
              return entry.getActualPath().getParent().toString();
            } else {
              return "";
            }
          }
        default:
          if (mode == Mode.Text) {
            return lineNr;
          } else {
            if (ref != null) {
              return ref.getName() + '=' + ref;
            }
            return null;
          }
      }
    }

    public Mode getMode() {
      return mode;
    }

    public ResourceEntry getResource() {
      return entry;
    }

    public String getName() {
      return name;
    }

    public StructEntry getStructEntry() {
      return ref;
    }

    public String getLine() {
      return line;
    }

    public int getLineNr() {
      return lineNr;
    }

    @Override
    public String toString() {
      if (mode == Mode.Text) {
        return String.format("File: %s, Line: %d, Text: %s", entry.getResourceName(), lineNr, line);
      } else {
        final StringBuilder buf = new StringBuilder("File: ");
        buf.append(entry.getResourceName());
        if (name != null) {
          buf.append(", Name: ").append(name);
        }
        if (ref != null) {
          buf.append(", Attribute: ").append(ref.getName()).append('=').append(ref);
        }
        return buf.toString();
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(entry, line, lineNr, mode, name, ref);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ReferenceHit other = (ReferenceHit) obj;
      return Objects.equals(entry, other.entry) && Objects.equals(line, other.line) && lineNr == other.lineNr
          && mode == other.mode && Objects.equals(name, other.name) && Objects.equals(ref, other.ref);
    }

    @Override
    public int compareTo(ReferenceHit rh) {
      if (rh == null) {
        throw new NullPointerException();
      }

      if (entry == null) {
        if (rh.entry == null) {
          return 0;
        } else {
          return 1;
        }
      }

      int retVal = entry.compareTo(rh.entry);
      if (retVal == 0) {
        if (name != null) {
          retVal = name.compareToIgnoreCase(rh.name);
          if (retVal == 0) {
            if (ref != null) {
              retVal = ref.compareTo(rh.ref);
            }
          }
        }
      }

      return retVal;
    }
  }
}
