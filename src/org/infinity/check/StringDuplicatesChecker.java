// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
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
  private ChildFrame resultFrame;
  private SortableTable table;
  private StringSet stringSet;

  private JButton saveButton;
  private JButton openLookupButton;
  private JButton openStringTableButton;

  public StringDuplicatesChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == openLookupButton) {
      final int row = table.getSelectedRow();
      if (row != -1) {
        final DuplicateStringTableItem item = (DuplicateStringTableItem) table.getTableItemAt(row);
        ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
      }
    } else if (e.getSource() == openStringTableButton) {
      final int row = table.getSelectedRow();
      if (row != -1) {
        final DuplicateStringTableItem item = (DuplicateStringTableItem) table.getTableItemAt(row);
        ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(item.strref);
      }
    } else if (e.getSource() == saveButton) {
      table.saveCheckResult(resultFrame, "Duplicate game strings");
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e) {
    final int row = table.getSelectedRow();
    openLookupButton.setEnabled(row != -1);
    openStringTableButton.setEnabled(row != -1);
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

      if (stringSet.size() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No duplicate strings found.", "Info",
            JOptionPane.INFORMATION_MESSAGE);
      } else {
        table = new SortableTable(new String[] { "#", "StringRef", "String", "Sound", "Flags" },
            new Class<?>[] { Integer.class, Integer.class, String.class, String.class, Integer.class },
            new Integer[] { 25, 25, 600, 50, 25 });
        List<Integer> list = stringSet.toList();
        for (final Integer strref : list) {
          table.addTableItem(new DuplicateStringTableItem(strref));
        }
        getResultFrame().setVisible(true);
      }
    } finally {
      blocker.setBlocked(false);
    }
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

  private ChildFrame getResultFrame() {
    if (resultFrame == null) {
      table.tableComplete();
      resultFrame = new ChildFrame("Result", true);
      resultFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());
      openLookupButton = new JButton("Open in StringRef Lookup", Icons.ICON_OPEN_16.getIcon());
      openLookupButton.setMnemonic('l');
      openLookupButton.setEnabled(false);
      openLookupButton.addActionListener(this);
      openStringTableButton = new JButton("Open in String Table", Icons.ICON_OPEN_16.getIcon());
      openStringTableButton.setMnemonic('t');
      openStringTableButton.setEnabled(false);
      openStringTableButton.addActionListener(this);
      saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
      saveButton.setMnemonic('s');
      saveButton.addActionListener(this);

      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      resultFrame.getRootPane().setDefaultButton(openLookupButton);

      JLabel countLabel = new JLabel(table.getRowCount() + " duplicate(s) in " + stringSet.size() + " string(s) found",
          SwingConstants.CENTER);
      countLabel.setFont(countLabel.getFont().deriveFont(countLabel.getFont().getSize2D() + 2.0f));

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(openLookupButton);
      buttonPanel.add(openStringTableButton);
      buttonPanel.add(saveButton);

      JPanel pane = (JPanel) resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(countLabel, BorderLayout.PAGE_START);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(buttonPanel, BorderLayout.PAGE_END);

      table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
      table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
      table.getSelectionModel().addListSelectionListener(this);

      final MouseListener listener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent event) {
          tableEntryOpened(event);
        }
      };
      table.addMouseListener(listener);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.setSize(1024, 576);
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
    }
    return resultFrame;
  }

  private void tableEntryOpened(MouseEvent event) {
    if (event.getClickCount() == 2) {
      final SortableTable table = (SortableTable) event.getSource();
      int row = table.getSelectedRow();
      if (row != -1) {
        final DuplicateStringTableItem item = (DuplicateStringTableItem) table.getTableItemAt(row);
        if (event.isAltDown()) {
          ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(item.strref);
        } else {
          ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
        }
      }
    }
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
      List<Integer> list = strings.get(s);
      if (list == null) {
        list = new ArrayList<>();
        strings.put(s, list);
      }
      list.add(strref);
    }

    /** Removes all entries without duplicates. */
    public void cleanup() {
      final Iterator<Map.Entry<String, List<Integer>>> iter = strings.entrySet().iterator();
      while (iter.hasNext()) {
        final Map.Entry<String, List<Integer>> entry = iter.next();
        if (entry.getValue().size() < 2) {
          iter.remove();
        }
      }
    }

    /** Returns the number of duplicate string instances. */
    public int size() {
      return strings.size();
    }

    /** Converts current set of stringrefs to a flat list and returns it. */
    public List<Integer> toList() {
      final List<Integer> retVal = new ArrayList<>();

      // make sure resulting list is sorted by first strref instance per group
      final List<List<Integer>> sortedList = new ArrayList<>();
      for (final Map.Entry<String, List<Integer>> entry : strings.entrySet()) {
        sortedList.add(entry.getValue());
      }
      sortedList.sort((a, b) -> a.get(0) - b.get(0));

      for (final List<Integer> list : sortedList) {
        retVal.addAll(list);
      }

      return retVal;
    }

    private String getInternalString(int strref) {
      final String text = normalize(StringTable.getStringRef(strref, StringTable.Format.NONE));
      final String sound = StringTable.getSoundResource(strref);
      return '"' + text + "\"[" + sound + "]";
    }

    /**
     * Returns a normalized version of the given string.
     *
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
    /** Provides the next available serial number. */
    private static int currentIndex = 0;

    private final Integer index;  // serial number
    private final Integer strref;
    private final String string;
    private final String sound;
    private final Integer flags;

    public DuplicateStringTableItem(int strref) {
      this.index = currentIndex++;
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
      return String.format("StringRef: %d, Sound: %s, Flags: %d, Text: %s",
          strref, sound, flags, string.replace("\r\n", Misc.LINE_SEPARATOR));
    }
  }
}
