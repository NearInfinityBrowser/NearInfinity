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
import java.util.Comparator;
import java.util.HashMap;
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
import org.infinity.gui.StringEditor;
import org.infinity.gui.StringLookup;
import org.infinity.gui.TableItem;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.AbstractSearcher;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

public class StringDuplicatesChecker extends AbstractSearcher
    implements Runnable, ListSelectionListener, ActionListener {
  /** Index of "Open in StringRef Lookup" button */
  private static final int BUTTON_LOOKUP    = 0;
  /** Index of "Open in String Table" button */
  private static final int BUTTON_TABLE     = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private ChildFrame resultFrame;
  private ResultPane<SortableTable> resultPane;
  private StringSet stringSet;

  public StringDuplicatesChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == resultPane.getButton(BUTTON_LOOKUP)) {
      tableEntryOpened(false);
    } else if (e.getSource() == resultPane.getButton(BUTTON_TABLE)) {
      tableEntryOpened(true);
    } else if (e.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      resultPane.getTable().saveCheckResult(resultFrame, "Duplicate game strings");
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    if (event.getSource() instanceof ListSelectionModel) {
      final ListSelectionModel model = (ListSelectionModel)event.getSource();
      final int row = model.getMinSelectionIndex();
      resultPane.getButton(BUTTON_LOOKUP).setEnabled(row != -1);
      resultPane.getButton(BUTTON_TABLE).setEnabled(row != -1);
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> files = new ArrayList<>();
      files.add(new FileResourceEntry(Profile.getProperty(Profile.Key.GET_GAME_DIALOG_FILE)));  // dummy entry

      stringSet = new StringSet();
      if (runSearch("Checking strings", files)) {
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    if (stringSet.size() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No duplicate strings found.", "Info",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    final SortableTable table = new SortableTable(new String[] { "Group", "StringRef", "String", "Sound", "Flags" },
        new Class<?>[] { Integer.class, Integer.class, String.class, String.class, Integer.class },
        new Integer[] { 25, 25, 600, 50, 25 });

    final List<List<Integer>> list = stringSet.toList();
    for (int i = 0, size = list.size(); i < size; i++) {
      for (final Integer strref : list.get(i)) {
        table.addTableItem(new DuplicateStringTableItem(strref, i + 1));
      }
    }

    table.tableComplete();

    final JButton openLookupButton = new JButton("Open in StringRef Lookup", Icons.ICON_OPEN_16.getIcon());
    openLookupButton.setMnemonic('l');
    openLookupButton.setEnabled(false);

    final JButton openStringTableButton = new JButton("Open in String Table", Icons.ICON_OPEN_16.getIcon());
    openStringTableButton.setMnemonic('t');
    openStringTableButton.setEnabled(false);

    final JButton saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
    saveButton.setMnemonic('s');

    final String title = table.getRowCount() + " duplicate(s) in " + stringSet.size() + " string(s) found";

    resultPane = new ResultPane<>(table, new JButton[] { openLookupButton, openStringTableButton, saveButton }, title);
    resultPane.setOnActionPerformed(this::actionPerformed);
    resultPane.setOnTableSelectionChanged(this::valueChanged);
    resultPane.setOnTableAction(this::performTableAction);

    resultFrame = new ChildFrame("Result", true);
    resultFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());
    resultFrame.getRootPane().setDefaultButton(openLookupButton);

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
    if (entry != null) {
      return () -> {
        for (int strref = 0, count = StringTable.getNumEntries(); strref < count; strref++) {
          stringSet.register(strref);
        }
        stringSet.cleanup();
      };
    } else {
      return () -> {};
    }
  }

  private void tableEntryOpened(boolean openInEditor) {
    final int row = resultPane.getTable().getSelectedRow();
    if (row != -1) {
      final DuplicateStringTableItem item = (DuplicateStringTableItem) resultPane.getTable().getTableItemAt(row);
      if (openInEditor) {
        ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(item.strref);
      } else {
        ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
      }
    }
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the information provided by the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    tableEntryOpened(event != null && event.isAltDown());
  }

  // -------------------------- INNER CLASSES --------------------------

  /** Tracks and manages duplicate strings. */
  private static final class StringSet {
    // Map: String -> list of duplicate strrefs
    private final Map<String, List<Integer>> strings = new HashMap<>();

    public StringSet() {
    }

    /** Registers the specified strref in the string set. */
    public void register(int strref) {
      final String s = getInternalString(strref);
      List<Integer> list = strings.computeIfAbsent(s, k -> new ArrayList<>());
      list.add(strref);
    }

    /** Removes all entries without duplicates. */
    public void cleanup() {
      strings.entrySet().removeIf(entry -> entry.getValue().size() < 2);
    }

    /** Returns the number of duplicate string instances. */
    public int size() {
      return strings.size();
    }

    /** Converts current set of stringrefs to a grouped list and returns it. */
    public List<List<Integer>> toList() {
      final List<List<Integer>> retVal = new ArrayList<>();

      for (final Map.Entry<String, List<Integer>> entry : strings.entrySet()) {
        retVal.add(entry.getValue());
      }
      retVal.sort(Comparator.comparingInt(a -> a.get(0)));

      return retVal;
    }

    private String getInternalString(int strref) {
      final String text = normalize(StringTable.getStringRef(strref, StringTable.Format.NONE));
      final String sound = StringTable.getSoundResource(strref);
      return '"' + text + "\"[" + sound + "]";
    }

    /**
     * Returns a normalized version of the given string.
     * A {@code null} string reference returns an empty string. Otherwise, whitespace is trimmed from the given string.
     *
     * @param s String to normalize.
     * @return the normalized string.
     */
    public static String normalize(String s) {
      if (s == null) {
        return "";
      } else {
        return s.trim().replace("\r", "");
      }
    }
  }

  private static final class DuplicateStringTableItem implements TableItem {
    // group index: indicates a group of duplicates of the same string
    private final Integer index;
    private final Integer strref;
    private final String string;
    private final String sound;
    private final Integer flags;

    public DuplicateStringTableItem(int strref, int groupIndex) {
      this.index = groupIndex;
      this.strref = strref;
      this.string = StringTable.getStringRef(strref, StringTable.Format.NONE);
      this.sound = StringTable.getSoundResource(strref);
      this.flags = (int) StringTable.getFlags(strref);
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return index;
        case 1:
          return strref;
        case 3:
          return sound;
        case 4:
          return flags;
        default:
          return string;
      }
    }

    @Override
    public String toString() {
      return String.format("Group: %d, StringRef: %d, Sound: %s, Flags: %d, Text: %s",
          index, strref, sound, flags, string.replace("\r\n", Misc.LINE_SEPARATOR));
    }
  }
}
