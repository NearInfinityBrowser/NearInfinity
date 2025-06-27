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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.ResultPane;
import org.infinity.gui.SortableTable;
import org.infinity.gui.StringEditor;
import org.infinity.gui.StringLookup;
import org.infinity.gui.TableItem;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

/**
 * Checks for illegal sound resrefs associated to TLK strings.
 */
public class StringSoundsChecker extends AbstractSearcher
    implements Runnable, ActionListener, ListSelectionListener, ChangeListener {
  /** Index of "Open in StringRef Lookup" button */
  private static final int BUTTON_LOOKUP    = 0;
  /** Index of "Open in String Table" button */
  private static final int BUTTON_TABLE     = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private ChildFrame resultsFrame;
  private ResultPane<SortableTable> resultPane;
  private ResultPane<SortableTable> resultPaneFemale;
  private JTabbedPane tabbedPane;
  private EnumMap<StringTable.Type, List<Integer>> stringMap;

  public StringSoundsChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    final ResultPane<SortableTable> resultPane = getSelectedResultsPane();
    final SortableTable table = resultPane.getTable();

    if (e.getSource() == resultPane.getButton(BUTTON_LOOKUP)) {
      tableEntryOpened(false);
    } else if (e.getSource() == resultPane.getButton(BUTTON_TABLE)) {
      tableEntryOpened(true);
    } else if (e.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      if (table == resultPaneFemale.getTable()) {
        table.saveCheckResult(resultsFrame, "Illegal sound resrefs in female string table");
      } else {
        table.saveCheckResult(resultsFrame, "Illegal sound resrefs in string table");
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    performTableChanged(getSelectedResultsPane());
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e) {
    performTableChanged(getSelectedResultsPane());
    resultsFrame.getRootPane().setDefaultButton(getSelectedResultsPane().getButton(BUTTON_LOOKUP));
  }

  // --------------------- End Interface ChangeListener ---------------------

  @Override
  public void run() {
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> files = new ArrayList<>();
      files.add(new FileResourceEntry(Profile.getProperty(Profile.Key.GET_GAME_DIALOG_FILE)));    // dummy entry
      stringMap = new EnumMap<>(StringTable.Type.class);
      stringMap.put(StringTable.Type.MALE, new ArrayList<>());
      if (StringTable.hasFemaleTable()) {
        stringMap.put(StringTable.Type.FEMALE, new ArrayList<>());
      }

      if (runSearch("Checking strings", files)) {
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    boolean found = !stringMap.get(StringTable.Type.MALE).isEmpty();
    if (stringMap.containsKey(StringTable.Type.FEMALE)) {
      found |= !stringMap.get(StringTable.Type.FEMALE).isEmpty();
    }

    if (!found) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No illegal sound resrefs in strings found.", "Info",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    final SortableTable table = new SortableTable(new String[] { "StringRef", "String", "Sound" },
        new Class<?>[] { Integer.class, String.class, String.class },
        new Integer[] { 25, 600, 50 });
    List<Integer> list = stringMap.get(StringTable.Type.MALE);
    for (Integer integer : list) {
      table.addTableItem(new StringSoundsItem(integer, StringTable.Type.MALE));
    }
    table.tableComplete();

    // Female string table is presented in a separate tab, if available
    final SortableTable tableFemale;
    if (stringMap.containsKey(StringTable.Type.FEMALE)) {
      tableFemale = new SortableTable(new String[] { "StringRef", "String", "Sound" },
          new Class<?>[] { Integer.class, String.class, String.class },
          new Integer[] { 25, 600, 50 });
      list = stringMap.get(StringTable.Type.FEMALE);
      for (Integer integer : list) {
        tableFemale.addTableItem(new StringSoundsItem(integer, StringTable.Type.FEMALE));
      }
      tableFemale.tableComplete();
    } else {
      tableFemale = null;
    }

    // setting up result panes
    for (int i = 0, imax = (tableFemale != null) ? 2 : 1; i < imax; i++) {
      final JButton openLookupButton = new JButton("Open in StringRef Lookup", Icons.ICON_OPEN_16.getIcon());
      openLookupButton.setMnemonic('l');
      openLookupButton.setEnabled(false);

      final JButton openStringTableButton = new JButton("Open in String Table", Icons.ICON_OPEN_16.getIcon());
      openStringTableButton.setMnemonic('t');
      openStringTableButton.setEnabled(false);

      final JButton saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
      saveButton.setMnemonic('s');

      final ResultPane<SortableTable> resultPane;
      if (i == 0) {
        resultPane = new ResultPane<>(table, new JButton[] { openLookupButton, openStringTableButton, saveButton }, null);
        this.resultPane = resultPane;
      } else {
        resultPane = new ResultPane<>(tableFemale, new JButton[] { openLookupButton, openStringTableButton, saveButton }, null);
        resultPaneFemale = resultPane;
      }
      resultPane.setOnActionPerformed(this::actionPerformed);
      resultPane.setOnTableSelectionChanged(this::valueChanged);
      resultPane.setOnTableAction(this::performTableAction);
    }

    // setting up result window
    resultsFrame = new ChildFrame("Result", true);
    resultsFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());

    int count = table.getRowCount();
    if (tableFemale != null) {
      count += tableFemale.getRowCount();
    }
    final JLabel countLabel = new JLabel(count + " illegal sound resrefs found", SwingConstants.CENTER);
    countLabel.setFont(countLabel.getFont().deriveFont(countLabel.getFont().getSize2D() + 2.0f));

    tabbedPane = new JTabbedPane();

    // Male string table
    tabbedPane.addTab("Male (" + table.getRowCount() + ")", resultPane);

    // Female string table
    if (tableFemale != null) {
      tabbedPane.addTab("Female (" + tableFemale.getRowCount() + ")", resultPaneFemale);
    } else {
      tabbedPane.addTab("Female", new JPanel());
      tabbedPane.setEnabledAt(1, false);
      tabbedPane.setToolTipTextAt(1, "Female string table not available.");
    }
    tabbedPane.setSelectedIndex(0);
    tabbedPane.addChangeListener(this);
    resultsFrame.getRootPane().setDefaultButton(getSelectedResultsPane().getButton(BUTTON_LOOKUP));

    final JPanel pane = (JPanel) resultsFrame.getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(countLabel, BorderLayout.PAGE_START);
    pane.add(tabbedPane, BorderLayout.CENTER);

    resultsFrame.setPreferredSize(Misc.getScaledDimension(resultsFrame.getPreferredSize()));
    resultsFrame.pack();
    Center.center(resultsFrame, NearInfinity.getInstance().getBounds());
    resultsFrame.setVisible(true);
  }

  @Override
  protected Runnable newWorker(ResourceEntry entry) {
    if (entry != null) {
      return () -> {
        for (int strref = 0, count = StringTable.getNumEntries(); strref < count; strref++) {
          String soundRef = StringTable.getSoundResource(strref);
          if (!soundRef.isEmpty() && !ResourceFactory.resourceExists(soundRef + ".WAV", true)) {
            stringMap.get(StringTable.Type.MALE).add(strref);
          }
          if (StringTable.hasFemaleTable()) {
            soundRef = StringTable.getSoundResource(StringTable.Type.FEMALE, strref);
            if (!soundRef.isEmpty() && !ResourceFactory.resourceExists(soundRef + ".WAV", true)) {
              stringMap.get(StringTable.Type.FEMALE).add(strref);
            }
          }
        }
      };
    } else {
      return () -> {};
    }
  }

  private void tableEntryOpened(boolean openInEditor) {
    final SortableTable table = getSelectedResultsPane().getTable();
    final StringTable.Type tableType =
        (getSelectedResultsPane() == resultPaneFemale) ? StringTable.Type.FEMALE : StringTable.Type.MALE;
    final int row = table.getSelectedRow();
    if (row != -1) {
      final StringSoundsItem item = (StringSoundsItem) table.getTableItemAt(row);
      if (openInEditor) {
        ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(tableType, item.strref);
      } else {
        ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
      }
    }
  }

  /** Updates controls based on the table state in the specified {@link ResultPane}. */
  private void performTableChanged(ResultPane<SortableTable> resultPane) {
    if (resultPane == null) {
      return;
    }

    final ListSelectionModel model = resultPane.getTable().getSelectionModel();
    final int row = model.getMinSelectionIndex();
    resultPane.getButton(BUTTON_LOOKUP).setEnabled(row != -1);
    resultPane.getButton(BUTTON_TABLE).setEnabled(row != -1);
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the information provided by the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    tableEntryOpened(event != null && event.isAltDown());
  }

  /** Returns the {@link ResultPane} of the currently selected tab. */
  private ResultPane<SortableTable> getSelectedResultsPane() {
    if (tabbedPane == null) {
      return resultPane;
    }
    return tabbedPane.getSelectedIndex() == 1 ? resultPaneFemale : resultPane;
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class StringSoundsItem implements TableItem {
    private final Integer strref;
    private final String string;
    private final String sound;

    public StringSoundsItem(int strref, StringTable.Type type) {
      this.strref = strref;
      this.string = StringTable.getStringRef(type, strref, StringTable.Format.NONE);
      this.sound = StringTable.getSoundResource(type, strref).toUpperCase(Locale.ENGLISH);
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return strref;
        case 2:
          return sound;
        default:
          return string;
      }
    }

    @Override
    public String toString() {
      return String.format("StringRef: %d, Sound: %s, Text: %s",
          strref, sound, string.replace("\r\n", Misc.LINE_SEPARATOR));
    }
  }
}
