// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.StringEditor;
import org.infinity.gui.StringLookup;
import org.infinity.gui.TableItem;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.menu.BrowserMenuBar;
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
public class StringSoundsChecker extends AbstractSearcher implements Runnable, ActionListener, ListSelectionListener, ChangeListener {
  private ChildFrame resultsFrame;
  private JTabbedPane tabbedPane;
  private SortableTable table, tableFemale;
  private EnumMap<StringTable.Type, List<Integer>> stringMap;

  private JButton saveButton;
  private JButton openLookupButton;
  private JButton openStringTableButton;

  public StringSoundsChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    final SortableTable table = (tabbedPane.getSelectedIndex() == 1) ? this.tableFemale : this.table;
    if (e.getSource() == saveButton) {
      if (table != this.tableFemale) {
        table.saveCheckResult(resultsFrame, "Illegal sound resrefs in string table");
      } else {
        tableFemale.saveCheckResult(resultsFrame, "Illegal sound resrefs in female string table");
      }
    } else if (e.getSource() == openLookupButton) {
      final int row = table.getSelectedRow();
      if (row != -1) {
        final StringSoundsItem item = (StringSoundsItem) table.getTableItemAt(row);
        ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
      }
    } else if (e.getSource() == openStringTableButton) {
      final int row = table.getSelectedRow();
      if (row != -1) {
        final StringSoundsItem item = (StringSoundsItem) table.getTableItemAt(row);
        ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(item.strref);
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e) {
    final SortableTable table = (tabbedPane.getSelectedIndex() == 1) ? this.tableFemale : this.table;
    final int row = table.getSelectedRow();
    openLookupButton.setEnabled(row != -1);
    openStringTableButton.setEnabled(row != -1);
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e) {
    if (e.getSource() == tabbedPane) {
      final SortableTable table = (tabbedPane.getSelectedIndex() == 1) ? this.tableFemale : this.table;
      final ListSelectionEvent event = new ListSelectionEvent(table, table.getSelectedRow(), table.getSelectedRow(), false);
      valueChanged(event);
    }
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

      boolean found = !stringMap.get(StringTable.Type.MALE).isEmpty();
      if (stringMap.containsKey(StringTable.Type.FEMALE)) {
        found |= !stringMap.get(StringTable.Type.FEMALE).isEmpty();
      }
      if (!found) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No illegal sound resrefs in strings found.", "Info",
            JOptionPane.INFORMATION_MESSAGE);
      } else {
        table = new SortableTable(new String[] { "StringRef", "String", "Sound" },
            new Class<?>[] { Integer.class, String.class, String.class },
            new Integer[] { 25, 600, 50 });
        List<Integer> list = stringMap.get(StringTable.Type.MALE);
        for (int i = 0, size = list.size(); i < size; i++) {
          table.addTableItem(new StringSoundsItem(list.get(i), StringTable.Type.MALE));
        }

        // Female string table is presented in a separate tab, if available
        if (stringMap.containsKey(StringTable.Type.FEMALE)) {
          tableFemale = new SortableTable(new String[] { "StringRef", "String", "Sound" },
              new Class<?>[] { Integer.class, String.class, String.class },
              new Integer[] { 25, 600, 50 });
          list = stringMap.get(StringTable.Type.FEMALE);
          for (int i = 0, size = list.size(); i < size; i++) {
            tableFemale.addTableItem(new StringSoundsItem(list.get(i), StringTable.Type.FEMALE));
          }
        }

        getResultFrame().setVisible(true);
      }
    } finally {
      blocker.setBlocked(false);
    }
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

  private ChildFrame getResultFrame() {
    if (resultsFrame == null) {
      resultsFrame = new ChildFrame("Result", true);
      resultsFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());

      saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
      saveButton.setMnemonic('s');
      saveButton.addActionListener(this);
      openLookupButton = new JButton("Open in StringRef Lookup", Icons.ICON_OPEN_16.getIcon());
      openLookupButton.setMnemonic('l');
      openLookupButton.setEnabled(false);
      openLookupButton.addActionListener(this);
      openStringTableButton = new JButton("Open in String Table", Icons.ICON_OPEN_16.getIcon());
      openStringTableButton.setMnemonic('t');
      openStringTableButton.setEnabled(false);
      openStringTableButton.addActionListener(this);
      resultsFrame.getRootPane().setDefaultButton(openLookupButton);

      int count = table.getRowCount();
      if (tableFemale != null) {
        count += tableFemale.getRowCount();
      }
      final JLabel countLabel = new JLabel(count + " illegal sound resrefs found", SwingConstants.CENTER);
      countLabel.setFont(countLabel.getFont().deriveFont(countLabel.getFont().getSize2D() + 2.0f));

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      buttonPanel.add(openLookupButton);
      buttonPanel.add(openStringTableButton);
      buttonPanel.add(saveButton);

      tabbedPane = new JTabbedPane(JTabbedPane.TOP);

      // Male string table
      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      tabbedPane.addTab("Male (" + table.getRowCount() + ")", scrollTable);
      table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
      table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
      table.getSelectionModel().addListSelectionListener(this);

      // Female string table
      if (tableFemale != null) {
        scrollTable = new JScrollPane(tableFemale);
        scrollTable.getViewport().setBackground(tableFemale.getBackground());
        tabbedPane.addTab("Female (" + tableFemale.getRowCount() + ")", scrollTable);
        tableFemale.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
        tableFemale.setRowHeight(tableFemale.getFontMetrics(tableFemale.getFont()).getHeight() + 1);
        tableFemale.getSelectionModel().addListSelectionListener(this);
      } else {
        tabbedPane.addTab("Female", new JPanel());
        tabbedPane.setEnabledAt(1, false);
        tabbedPane.setToolTipTextAt(1, "Female string table not available.");
      }
      tabbedPane.setSelectedIndex(0);
      tabbedPane.addChangeListener(this);

      JPanel pane = (JPanel) resultsFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(countLabel, BorderLayout.PAGE_START);
      pane.add(tabbedPane, BorderLayout.CENTER);
      pane.add(buttonPanel, BorderLayout.PAGE_END);

      final MouseListener listener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent event) {
          tableEntryOpened(event);
        }
      };
      table.addMouseListener(listener);
      if (tableFemale != null) {
        tableFemale.addMouseListener(listener);
      }
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultsFrame.setSize(1024, 576);
      Center.center(resultsFrame, NearInfinity.getInstance().getBounds());
    }
    return resultsFrame;
  }

  private void tableEntryOpened(MouseEvent event) {
    if (event.getClickCount() == 2) {
      final SortableTable table = (SortableTable) event.getSource();
      final StringTable.Type tableType = (table == tableFemale) ? StringTable.Type.FEMALE : StringTable.Type.MALE;
      int row = table.getSelectedRow();
      if (row != -1) {
        final StringSoundsItem item = (StringSoundsItem) table.getTableItemAt(row);
        if (event.isAltDown()) {
          ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(tableType, item.strref);
        } else {
          ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
        }
      }
    }
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
