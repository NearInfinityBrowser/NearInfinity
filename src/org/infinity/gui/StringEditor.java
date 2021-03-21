// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2018 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.NearInfinity;
import org.infinity.datatype.Editable;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.ResourceRef;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.search.SearchClient;
import org.infinity.search.SearchMaster;
import org.infinity.search.StringReferenceSearcher;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

public class StringEditor extends ChildFrame implements SearchClient
{
  public static final String TLK_FLAGS  = "Flags";
  public static final String TLK_SOUND  = "Associated sound";
  public static final String TLK_VOLUME = "Volume variance";
  public static final String TLK_PITCH  = "Pitch variance";

  public static final String[] s_flags = { "None", "Has text", "Has sound", "Has token" };

  private final ArrayDeque<UndoAction> undoStack = new ArrayDeque<>();

  private final Listeners listeners = new Listeners();
  private final JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
  private final ButtonPopupMenu bpmFind = new ButtonPopupMenu("Find...", ButtonPopupMenu.Align.TOP);
  private final ButtonPopupMenu bpmExport = new ButtonPopupMenu("Export...", ButtonPopupMenu.Align.TOP);
  private final ButtonPopupMenu bpmRevert = new ButtonPopupMenu("Revert...", ButtonPopupMenu.Align.TOP);
  private final JButton bAdd = new JButton("Add", Icons.getIcon(Icons.ICON_ADD_16));
  private final JButton bDelete = new JButton("Delete", Icons.getIcon(Icons.ICON_REMOVE_16));
  private final JButton bSave = new JButton("Save", Icons.getIcon(Icons.ICON_SAVE_16));
  private final JButton bSync = new JButton("Sync entry", Icons.getIcon(Icons.ICON_REFRESH_16));
  private final JMenuItem miFindAttribute = new JMenuItem("selected attribute");
  private final JMenuItem miFindString = new JMenuItem("string");
  private final JMenuItem miFindRef = new JMenuItem("references to this entry");
  private final JMenuItem miExportTra = new JMenuItem("as TRA file");
  private final JMenuItem miExportTxt = new JMenuItem("as TXT file");
  private final JMenuItem miRevertLast = new JMenuItem("last operation");
  private final JMenuItem miRevertAll = new JMenuItem("all");
  private final JPanel pTabMain = new JPanel();  // contains whole tab content
  private final JPanel pAttribEdit = new JPanel();
  private final JSlider slider = new JSlider(0, 100, 0);
  private final JTable table = new JTable();
  private final InfinityTextArea taText = new InfinityTextArea(true);
  private final JTextField tfStrref = new JTextField(6);

  private Editable editable;
  private int selectedIndex = -1;
  private StringTable.StringEntry selectedEntry = null;


  public StringEditor()
  {
    super(getWindowTitle(StringTable.Type.MALE));

    String msg = "Make sure you have a backup of ";
    msg += StringTable.getPath(StringTable.Type.MALE).getFileName().toString();
    if (StringTable.hasFemaleTable()) {
      msg += " and " + StringTable.getPath(StringTable.Type.FEMALE).getFileName().toString();
    }
    msg += ".";
    JOptionPane.showMessageDialog(NearInfinity.getInstance(), msg, "Warning", JOptionPane.WARNING_MESSAGE);

    initUI();
  }

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    boolean retVal = true;
    updateEntry(getSelectedEntry());
    if (StringTable.isModified()) {
      setVisible(true);
      int optionType = forced ? JOptionPane.YES_NO_OPTION : JOptionPane.YES_NO_CANCEL_OPTION;
      int result = JOptionPane.showConfirmDialog(this, "String table has been modified. Save changes to disk?",
                                                 "Save changes", optionType, JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() throws Exception
          {
            WindowBlocker blocker = new WindowBlocker(StringEditor.this);
            try {
              blocker.setBlocked(true);
              save(false);
            } finally {
              blocker.setBlocked(false);
            }
            return null;
          }
        };

        try {
          worker.execute();
          worker.get();
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }

      retVal = (result != JOptionPane.CANCEL_OPTION);
    }
    return retVal;
  }

//--------------------- Begin Interface SearchClient ---------------------

  @Override
  public String getText(int index)
  {
    if (index >= 0 && index < StringTable.getNumEntries(getSelectedDialogType())) {
      return StringTable.getStringRef(index, StringTable.Format.NONE);
    }
    return null;
  }

  @Override
  public void hitFound(int index)
  {
    showEntry(index);
  }

//--------------------- End Interface SearchClient ---------------------

  private static String getWindowTitle(StringTable.Type dlgType)
  {
    if (dlgType != null) {
      return "Edit: " + StringTable.getPath(dlgType).toString() +
                    " (" + StringTable.getNumEntries(dlgType) + " entries)";
    } else {
      return "String Editor";
    }
  }

  private void initUI()
  {
    setIconImage(Icons.getIcon(Icons.ICON_EDIT_16).getImage());

    table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(listeners);
    table.getTableHeader().setReorderingAllowed(false);
    table.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    JScrollPane spTable = new JScrollPane(table);
    spTable.getViewport().setBackground(table.getBackground());
    spTable.setBorder(BorderFactory.createEmptyBorder());

    tfStrref.addActionListener(listeners);
    if (BrowserMenuBar.getInstance().getTlkSyntaxHighlightingEnabled()) {
      taText.applyExtendedSettings(InfinityTextArea.Language.TLK, null);
      taText.setFont(Misc.getScaledFont(taText.getFont()));
    }
    taText.setMargin(new Insets(3, 3, 3, 3));
    taText.setLineWrap(true);
    taText.setWrapStyleWord(true);
    taText.getDocument().addDocumentListener(listeners);

    miFindAttribute.setEnabled(false);
    bpmFind.setMenuItems(new JMenuItem[]{miFindAttribute, miFindString, miFindRef}, false);
    bpmFind.setIcon(Icons.getIcon(Icons.ICON_FIND_16));
    bpmFind.addItemListener(listeners);
    miExportTra.setToolTipText("Exports male and female string table into WeiDU TRA file");
    miExportTxt.setToolTipText("Exports selected string table into text file");
    bpmExport.setMenuItems(new JMenuItem[]{miExportTxt, miExportTra}, false);
    bpmExport.setIcon(Icons.getIcon(Icons.ICON_EXPORT_16));
    bpmExport.addItemListener(listeners);
    miRevertAll.setEnabled(false);
    miRevertAll.setToolTipText("Reverts all changes");
    miRevertLast.setEnabled(false);
    miRevertLast.setToolTipText("Reverts most recent add/delete operation");
    bpmRevert.setMenuItems(new JMenuItem[]{miRevertLast, miRevertAll}, false);
    bpmRevert.setIcon(Icons.getIcon(Icons.ICON_UNDO_16));
    bpmRevert.addItemListener(listeners);
    bAdd.addActionListener(listeners);
    bAdd.setMnemonic('a');
    bDelete.addActionListener(listeners);
    bDelete.setMnemonic('d');
    bSave.addActionListener(listeners);
    bSave.setMnemonic('s');
    bSync.addActionListener(listeners);
    bSync.setMnemonic('y');
    bSync.setToolTipText("Copies male string entry to female entry of same index");
    bSync.setEnabled(StringTable.hasFemaleTable());

    slider.setMaximum(StringTable.getNumEntries());
    int v = (StringTable.getNumEntries() / 25000) + 1;
    slider.setMajorTickSpacing(v * 2500);
    slider.setMinorTickSpacing(v * 250);
    slider.setPaintTicks(true);
    slider.setPaintLabels(true);
    slider.setFont(slider.getFont().deriveFont(slider.getFont().getSize2D() * 0.85f));
    slider.addChangeListener(listeners);

    tabPane.addTab("dialog.tlk", new JPanel());
    tabPane.addTab("dialogF.tlk", new JPanel());
    tabPane.setSelectedIndex(0);
    tabPane.setEnabledAt(1, StringTable.hasFemaleTable());
    if (!StringTable.hasFemaleTable()) {
      tabPane.setToolTipTextAt(1, "Not available for the current language.");
    }
    tabPane.addChangeListener(listeners);

    // constructing tab content
    JLabel l = new JLabel("Strref: ");
    l.setLabelFor(tfStrref);
    l.setFont(l.getFont().deriveFont((float)l.getFont().getSize() + 2.0f));
    JPanel pTopLeft = new JPanel(new FlowLayout());
    pTopLeft.add(l);
    pTopLeft.add(tfStrref);

    JPanel pTop = new JPanel(new BorderLayout());
    pTop.add(pTopLeft, BorderLayout.WEST);
    pTop.add(slider, BorderLayout.CENTER);

    pAttribEdit.setLayout(new BorderLayout());
    JSplitPane splitAttrib = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spTable, pAttribEdit);
    splitAttrib.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));

    JPanel pCenterLeft = new JPanel(new BorderLayout());
    pCenterLeft.add(new JLabel("Attributes:"), BorderLayout.NORTH);
    pCenterLeft.add(splitAttrib, BorderLayout.CENTER);

    JPanel pCenterRight = new JPanel(new BorderLayout());
    pCenterRight.add(new JLabel("Text:"), BorderLayout.NORTH);
    pCenterRight.add(new InfinityScrollPane(taText, true), BorderLayout.CENTER);

    JSplitPane splitCenter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitCenter.setLeftComponent(pCenterLeft);
    splitCenter.setRightComponent(pCenterRight);
    splitCenter.setResizeWeight(0.0);

    pTabMain.setLayout(new BorderLayout(3, 3));
    pTabMain.add(pTop, BorderLayout.NORTH);
    pTabMain.add(splitCenter, BorderLayout.CENTER);
    tabPane.setComponentAt(tabPane.getSelectedIndex(), pTabMain);

    // constructing bottom bar
    JPanel pBottomMain = new JPanel(new FlowLayout(FlowLayout.CENTER));
    pBottomMain.add(bSync);
    pBottomMain.add(bAdd);
    pBottomMain.add(bDelete);
    pBottomMain.add(bpmFind);
    pBottomMain.add(bpmRevert);
    pBottomMain.add(bpmExport);
    pBottomMain.add(bSave);

    // putting all together
    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout(3, 3));
    pane.add(tabPane, BorderLayout.CENTER);
    pane.add(pBottomMain, BorderLayout.SOUTH);
    pane.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    if (screenSize.width > 0 && screenSize.height > 0) {
      setSize(screenSize.width * 2 / 3, screenSize.height * 2 / 3);
    } else {
      setSize(750, 500);
    }
    Center.center(this, NearInfinity.getInstance().getBounds());
    setVisible(true);

    splitCenter.setDividerLocation(splitCenter.getWidth() / 2);
    splitAttrib.setDividerLocation(splitAttrib.getHeight() / 3);
  }

  private void updateStringTableUI(int tabIndex)
  {
    if (tabIndex < 0 || tabIndex >= tabPane.getTabCount()) {
      return;
    }

    for (int i = 0; i < tabPane.getTabCount(); i++) {
      if (i != tabIndex) {
        tabPane.setComponentAt(i, new JPanel());
      }
    }
    tabPane.setComponentAt(tabIndex, pTabMain);

    updateUI(getSelectedDialogType());
    showEntry(getSelectedIndex());

    pTabMain.revalidate();
    pTabMain.repaint();
  }

  private void updateUI(StringTable.Type dlgType)
  {
    if (dlgType == null) { dlgType = StringTable.Type.MALE; }

    setTitle(getWindowTitle(dlgType));
    slider.setMaximum(StringTable.getNumEntries(dlgType));
  }

  private void updateModifiedUI(StringTable.Type dlgType)
  {
    if (dlgType == null) {
      updateModifiedUI(StringTable.Type.MALE);
      if (StringTable.hasFemaleTable()) {
        updateModifiedUI(StringTable.Type.FEMALE);
      }
    } else {
      int idx = (dlgType == StringTable.Type.FEMALE) ? 1 : 0;
      String name = tabPane.getTitleAt(idx);
      if (StringTable.isModified(dlgType) && !name.endsWith("*")) {
        name += '*';
        tabPane.setTitleAt(idx, name);
      } else if (!StringTable.isModified(dlgType) && name.endsWith("*")) {
        name = name.substring(0, name.length() - 1);
        tabPane.setTitleAt(idx, name);
      }
    }

    boolean modified = StringTable.isModified(StringTable.Type.MALE);
    if (StringTable.hasFemaleTable()) {
      modified |= StringTable.isModified(StringTable.Type.FEMALE);
    }
    miRevertAll.setEnabled(modified);
  }

  private StringTable.Type getSelectedDialogType()
  {
    return (tabPane.getSelectedIndex() == 1) ? StringTable.Type.FEMALE : StringTable.Type.MALE;
  }

  public void selectDialogType(StringTable.Type type)
  {
    if (tabPane.getTabCount() > 0) {
      if (type == null) { type = StringTable.Type.MALE; }
      if (type == StringTable.Type.FEMALE && !StringTable.hasFemaleTable()) {
        type = StringTable.Type.MALE;
      }

      switch (type) {
        case MALE:   tabPane.setSelectedIndex(0); break;
        case FEMALE: tabPane.setSelectedIndex(1); break;
      }
      updateStringTableUI(tabPane.getSelectedIndex());
    }
  }

  public int getSelectedIndex()
  {
    return selectedIndex;
  }

  public StringTable.StringEntry getSelectedEntry()
  {
    return selectedEntry;
  }

  public void showEntry(StringTable.Type dlgType, int index)
  {
    selectDialogType(dlgType);
    showEntry(index);
  }

  public void showEntry(int index)
  {
    index = Math.max(Math.min(index, StringTable.getNumEntries(getSelectedDialogType()) - 1), 0);

    if (selectedEntry != null) {
      updateEntry(selectedEntry);
      selectedEntry.removeTableModelListener(listeners);
    }

    StringTable.StringEntry entry = StringTable.getStringEntry(getSelectedDialogType(), index);
    entry.fillList(index);
    tfStrref.setText(Integer.toString(index));
    slider.setValue(index);
    table.clearSelection();
    table.setModel(entry);
    if (table.getParent() != null) {
      table.getParent().setPreferredSize(table.getPreferredSize());
    }
    taText.getDocument().removeDocumentListener(listeners);
    taText.setText(entry.getText());
    taText.setCaretPosition(0);
    taText.discardAllEdits();
    taText.getDocument().addDocumentListener(listeners);
    updateAttributePanel(table.getSelectionModel());
    table.repaint();
    editable = null;
    selectedIndex = index;
    selectedEntry = entry;
    selectedEntry.addTableModelListener(listeners);
  }

  /**
   * If string editor window already opened, focus it and show specified value in
   * it, otherwise creates new window and show specified value.
   *
   * @param value String index to show in the editor
   */
  public static void edit(int value)
  {
    final StringEditor editor = ChildFrame.show(StringEditor.class, () -> new StringEditor());
    editor.showEntry(StringTable.Type.MALE, StringTable.getTranslatedIndex(value));
  }

  private void updateEntry(StringTable.StringEntry entry)
  {
    if (entry != null) {
      entry.setText(taText.getText());
      updateModifiedUI(entry.getTableType());
    }
  }

  private void syncEntry(int index)
  {
    if (!StringTable.hasFemaleTable() ||
        index < 0 ||
        index >= StringTable.getNumEntries(StringTable.Type.MALE) ||
        index >= StringTable.getNumEntries(StringTable.Type.FEMALE)) {
      return;
    }

    updateEntry(getSelectedEntry());
    StringTable.StringEntry entryFemale = StringTable.getStringEntry(StringTable.Type.FEMALE, index);
    boolean allow = true;
    if (!entryFemale.getText().isEmpty()) {
      allow = (JOptionPane.showConfirmDialog(this, "Female string entry is not empty. Continue?",
                                             "Synchronize entries", JOptionPane.YES_NO_OPTION,
                                             JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
    }

    if (allow) {
      StringTable.StringEntry entryMale = StringTable.getStringEntry(StringTable.Type.MALE, index);
      entryFemale.setText(entryMale.getText());
      entryFemale.setFlags(entryMale.getFlags());
      entryFemale.setSoundRef(entryMale.getSoundRef());
      entryFemale.setVolume(entryMale.getVolume());
      entryFemale.setPitch(entryMale.getPitch());
      entryFemale.clearList();
      if (getSelectedDialogType() == StringTable.Type.FEMALE && getSelectedIndex() == index) {
        selectedIndex = -1;
        selectedEntry = null;
        showEntry(index);
      }
    }
  }

  private void syncTables()
  {
    if (!StringTable.hasFemaleTable()) {
      return;
    }

    updateEntry(getSelectedEntry());

    StringTable.Type male = StringTable.Type.MALE;
    StringTable.Type female = StringTable.Type.FEMALE;

    if (StringTable.getNumEntries(male) < StringTable.getNumEntries(female)) {
      while (StringTable.getNumEntries(male) < StringTable.getNumEntries(female)) {
        StringTable.StringEntry entry = StringTable.getStringEntry(female, StringTable.getNumEntries(male));
        StringTable.addEntry(male, entry.clone());
      }
      updateModifiedUI(male);
    }

    if (StringTable.getNumEntries(female) < StringTable.getNumEntries(male)) {
      while (StringTable.getNumEntries(female) < StringTable.getNumEntries(male)) {
        StringTable.StringEntry entry = StringTable.getStringEntry(male, StringTable.getNumEntries(female));
        StringTable.addEntry(female, entry.clone());
      }
      updateModifiedUI(female);
    }
  }

  private int addEntry(StringTable.StringEntry entryMale, StringTable.StringEntry entryFemale, boolean undoable)
  {
    if (entryMale == null) {
      entryMale = new StringTable.StringEntry(null);
    }
    if (entryFemale == null) {
      entryFemale = new StringTable.StringEntry(null);
    }

    syncTables();

    int index = StringTable.addEntry(StringTable.Type.MALE, entryMale);
    int index2 = index;
    if (getSelectedDialogType() == StringTable.Type.MALE) {
      showEntry(index);
    }

    if (StringTable.hasFemaleTable()) {
      index2 = StringTable.addEntry(StringTable.Type.FEMALE, entryFemale);
      if (getSelectedDialogType() == StringTable.Type.FEMALE) {
        showEntry(index2);
      }
    }

    updateUI(getSelectedDialogType());
    updateModifiedUI(null);
    if (undoable) {
      addUndo(new UndoAction());
    }

    return (tabPane.getSelectedIndex() == 1) ? index2 : index;
  }

  private void deleteEntry(boolean undoable)
  {
    StringTable.StringEntry entry1 = null;
    StringTable.StringEntry entry2 = null;

    syncTables();

    int index = StringTable.getNumEntries(StringTable.Type.MALE) - 1;
    if (getSelectedDialogType() == StringTable.Type.MALE && index == getSelectedIndex()) {
      showEntry(index - 1);
    }
    if (index >= 0) {
      entry1 = StringTable.getStringEntry(StringTable.Type.MALE, index);
      StringTable.removeEntry(StringTable.Type.MALE, index);
    }

    if (StringTable.hasFemaleTable()) {
      index = StringTable.getNumEntries(StringTable.Type.FEMALE) - 1;
      if (getSelectedDialogType() == StringTable.Type.FEMALE && index == getSelectedIndex()) {
        showEntry(index - 1);
      }
      if (index >= 0) {
        entry2 = StringTable.getStringEntry(StringTable.Type.FEMALE, index);
        StringTable.removeEntry(StringTable.Type.FEMALE, index);
      }
    }

    updateUI(getSelectedDialogType());
    updateModifiedUI(null);
    if (undoable) {
      addUndo(new UndoAction(entry1, entry2));
    }
  }

  private void updateAttributePanel(ListSelectionModel model)
  {
    if (model != null) {
      miFindAttribute.setEnabled(!model.isSelectionEmpty());
      pAttribEdit.removeAll();
      if (!model.isSelectionEmpty()) {
        Object selected = table.getModel().getValueAt(model.getMinSelectionIndex(), 1);
        if (selected instanceof Editable) {
          editable = (Editable)selected;
          pAttribEdit.add(editable.edit(listeners), BorderLayout.CENTER);
          editable.select();
        }
      }
      pAttribEdit.revalidate();
      pAttribEdit.repaint();
    }
  }

  private void updateTableItem(int row)
  {
    // tracking changes made to table entries
    Object cellName = table.getModel().getValueAt(row, 0).toString();
    Object cellValue = table.getModel().getValueAt(row, 1);
    StringTable.StringEntry entry = getSelectedEntry();
    if (cellName != null && cellValue != null && entry != null) {
      String name = cellName.toString();
      if (StringEditor.TLK_FLAGS.equals(name)) {
        entry.setFlags((short)((IsNumeric)cellValue).getValue());
      } else if (StringEditor.TLK_SOUND.equals(name)) {
        ResourceRef ref = (ResourceRef)cellValue;
        entry.setSoundRef(ref.isEmpty() ? "" : ref.getText());
      } else if (StringEditor.TLK_VOLUME.equals(name)) {
        entry.setVolume(((IsNumeric)cellValue).getValue());
      } else if (StringEditor.TLK_PITCH.equals(name)) {
        entry.setPitch(((IsNumeric)cellValue).getValue());
      }
      updateModifiedUI(getSelectedDialogType());
    }
  }

  private boolean addUndo(UndoAction action)
  {
    if (action == null) {
      return false;
    }

    undoStack.push(action);
    updateUndoMenu();

    return true;
  }

  /** Undoes the last add/remove action if available. */
  private boolean undo()
  {
    if (!undoStack.isEmpty()) {
      undoStack.pop().undo();
      updateUndoMenu();
      return true;
    }
    return false;
  }

  private void clearUndo()
  {
    undoStack.clear();
    updateUndoMenu();
  }

  private void updateUndoMenu()
  {
    if (undoStack.isEmpty()) {
      miRevertLast.setText("last operation");
      miRevertLast.setEnabled(false);
    } else {
      String op = undoStack.peek().isUndoAdd() ? "Add" : "Delete";
      miRevertLast.setText("last operation: " + op);
      miRevertLast.setEnabled(true);
    }
  }

  private void revertAll()
  {
    StringTable.resetModified(StringTable.Type.MALE);
    StringTable.ensureFullyLoaded(StringTable.Type.MALE);

    if (StringTable.hasFemaleTable()) {
      StringTable.resetModified(StringTable.Type.FEMALE);
      StringTable.ensureFullyLoaded(StringTable.Type.FEMALE);
    }

    selectedIndex = -1;
    selectedEntry = null;

    setTitle(getWindowTitle(getSelectedDialogType()));
    clearUndo();
    updateUI(getSelectedDialogType());
    updateModifiedUI(null);
    showEntry(0);

    String msg = StringTable.getPath(StringTable.Type.MALE).getFileName().toString();
    if (StringTable.hasFemaleTable()) {
      msg += " and " + StringTable.getPath(StringTable.Type.FEMALE).getFileName().toString() + "  have ";
    } else {
      msg += " has ";
    }
    msg += "been reloaded from disk.";
    JOptionPane.showMessageDialog(this, msg, "Revert string tables", JOptionPane.INFORMATION_MESSAGE);
  }

  /** Save changes to all available string tables. */
  private void save(boolean interactive)
  {
    boolean isSync = bSync.isEnabled();
    boolean isAdd = bAdd.isEnabled();
    boolean isDelete = bDelete.isEnabled();
    boolean isSave = bSave.isEnabled();
    boolean isRevert = bpmRevert.isEnabled();
    try {
      bSync.setEnabled(false);
      bAdd.setEnabled(false);
      bDelete.setEnabled(false);
      bSave.setEnabled(false);
      bpmRevert.setEnabled(false);

      Path outFile = StringTable.getPath(StringTable.Type.MALE);
      Path outPath = outFile.getParent();

      // consider string tables in DLC archives
      if (!FileManager.isDefaultFileSystem(outFile)) {
        if (!interactive) {
          return;
        }
        String msg = "\"" + outFile.toString() + "\" is located within a write-protected archive." +
                     "\nDo you want to export it to another location instead?";
        int result = JOptionPane.showConfirmDialog(this, msg, "Save resource",
                                                   JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
          JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
          fc.setDialogTitle("Select output folder");
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          int ret = fc.showSaveDialog(this);
          if (ret == JFileChooser.APPROVE_OPTION) {
            outPath = fc.getSelectedFile().toPath();
            if (!FileEx.create(outPath).isDirectory()) {
              outPath = outPath.getParent();
            }
            outFile = outPath.resolve(StringTable.getPath(StringTable.Type.MALE).getFileName().toString());
          } else {
            JOptionPane.showMessageDialog(this, "Operation cancelled.", "Information",
                                          JOptionPane.INFORMATION_MESSAGE);
            return;
          }
        }
      }

      // writing male string table
      ProgressTracker pt = null;
      if (interactive) {
        pt = new ProgressTracker("Saving " + outFile.getFileName().toString(), null,
                                 "Error writing " + outFile.getFileName().toString());
      }
      if (!StringTable.write(StringTable.Type.MALE, outFile, pt)) {
        return;
      }

      if (StringTable.hasFemaleTable()) {
        outFile = outPath.resolve(StringTable.getPath(StringTable.Type.FEMALE).getFileName());
        if (interactive) {
          pt = new ProgressTracker("Saving " + outFile.getFileName().toString(), null,
                                   "Error writing " + outFile.getFileName().toString());
        }
        if (!StringTable.write(StringTable.Type.FEMALE, outFile, pt)) {
          return;
        }
      }

      updateModifiedUI(null);
      if (interactive) {
        JOptionPane.showMessageDialog(this, "File(s) written successfully.", "Save complete",
                                      JOptionPane.INFORMATION_MESSAGE);
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      bSync.setEnabled(isSync);
      bAdd.setEnabled(isAdd);
      bDelete.setEnabled(isDelete);
      bSave.setEnabled(isSave);
      bpmRevert.setEnabled(isRevert);
    }
  }

  private void exportText(StringTable.Type dlgType)
  {
    if (dlgType == null) { dlgType = StringTable.Type.MALE; }

    String dlgFile = StringTable.getPath(dlgType).getFileName().toString();

    JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
    fc.setDialogTitle("Export as text file");
    fc.setFileFilter(new FileNameExtensionFilter("Text files", "txt"));
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setSelectedFile(new File(fc.getCurrentDirectory(), dlgFile.toLowerCase(Locale.ENGLISH).replace(".tlk", ".txt")));
    if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    Path outFile = fc.getSelectedFile().toPath();
    ProgressTracker pt = new ProgressTracker("Exporting " + dlgFile, "File exported successfully",
                                             "Error while exporting " + dlgFile);
    StringTable.exportText(dlgType, outFile, pt);
  }

  private void exportTra()
  {
    String dlgFile = StringTable.getPath().getFileName().toString();

    JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
    fc.setDialogTitle("Export as translation file");
    fc.setFileFilter(new FileNameExtensionFilter("TRA files", "tra"));
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setSelectedFile(new File(fc.getCurrentDirectory(), dlgFile.toLowerCase(Locale.ENGLISH).replace(".tlk", ".tra")));
    if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
      return;
    }

    Path outFile = fc.getSelectedFile().toPath();
    ProgressTracker pt = new ProgressTracker("Exporting " + dlgFile, "File exported successfully",
                                             "Error while exporting " + dlgFile);
    StringTable.exportTra(outFile, pt);
  }

  //-------------------------- INNER CLASSES --------------------------

  private class Listeners implements ActionListener, ListSelectionListener, ItemListener,
                                     ChangeListener, TableModelListener, DocumentListener
  {
    protected Listeners() {}

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getActionCommand().equals(StructViewer.UPDATE_VALUE)) {
        if (editable.updateValue(StringTable.getStringEntry(getSelectedDialogType(), getSelectedIndex()))) {
          updateTableItem(table.getSelectedRow());
        } else {
          JOptionPane.showMessageDialog(StringEditor.this, "Error updating value.", "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
        table.repaint();
      } else if (e.getSource() == bAdd) {
        new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() throws Exception
          {
            WindowBlocker blocker = new WindowBlocker(StringEditor.this);
            try {
              blocker.setBlocked(true);
              showEntry(addEntry(new StringTable.StringEntry(null), new StringTable.StringEntry(null), true));
            } finally {
              blocker.setBlocked(false);
            }
            return null;
          }
        }.execute();
      } else if (e.getSource() == bDelete) {
        if (getSelectedIndex() == StringTable.getNumEntries(getSelectedDialogType()) - 1) {
          new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception
            {
              WindowBlocker blocker = new WindowBlocker(StringEditor.this);
              try {
                blocker.setBlocked(true);
                deleteEntry(true);
                showEntry(StringTable.getNumEntries(getSelectedDialogType()) - 1);
              } finally {
                blocker.setBlocked(false);
              }
              return null;
            }
          }.execute();
        } else {
          JOptionPane.showMessageDialog(StringEditor.this, "You can only delete the last entry.",
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
      } else if (e.getSource() == bSave) {
        updateEntry(getSelectedEntry());
        new SwingWorker<Void, Void>() {
          @Override
          protected Void doInBackground() throws Exception
          {
            WindowBlocker blocker = new WindowBlocker(StringEditor.this);
            try {
              blocker.setBlocked(true);
              save(true);
            } finally {
              blocker.setBlocked(false);
            }
            return null;
          }
        }.execute();
      } else if (e.getSource() == bSync) {
        syncEntry(getSelectedIndex());
      } else if (e.getSource() == tfStrref) {
        try {
          int i = Integer.parseInt(tfStrref.getText().trim());
          if (i >= 0 && i < StringTable.getNumEntries(getSelectedDialogType())) {
            showEntry(i);
          } else {
            JOptionPane.showMessageDialog(StringEditor.this, "Entry not found.", "Error",
                                          JOptionPane.ERROR_MESSAGE);
          }
        }  catch (NumberFormatException nfe) {
          JOptionPane.showMessageDialog(StringEditor.this, "Not a number.", "Error",
                                        JOptionPane.ERROR_MESSAGE);
        }
      }
    }

    @Override
    public void valueChanged(ListSelectionEvent e)
    {
      if (e.getValueIsAdjusting()) { return; }
      updateAttributePanel((ListSelectionModel)e.getSource());
    }

    @Override
    public void itemStateChanged(ItemEvent e)
    {
      if (e.getSource() instanceof ButtonPopupMenu) {
        JMenuItem item = ((ButtonPopupMenu)e.getSource()).getSelectedItem();
        if (item == miFindString) {
          SearchMaster.createAsFrame(StringEditor.this, "StringRef", StringEditor.this);
        } else if (item == miFindAttribute) {
          SearchMaster.createAsFrame(new AttributeSearcher(table.getSelectedRow()),
                                     StringTable.getStringEntry(getSelectedDialogType(), getSelectedIndex()).getValueAt(table.getSelectedRow(), 0).toString(),
                                     StringEditor.this);
        } else if (item == miFindRef) {
          new StringReferenceSearcher(getSelectedIndex(), StringEditor.this);
        } else if (item == miExportTxt) {
          updateEntry(getSelectedEntry());
          new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception
            {
              WindowBlocker blocker = new WindowBlocker(StringEditor.this);
              try {
                blocker.setBlocked(true);
                exportText(getSelectedDialogType());
              } finally {
                blocker.setBlocked(false);
              }
              return null;
            }
          }.execute();
        } else if (item == miExportTra) {
          updateEntry(getSelectedEntry());
          new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception
            {
              WindowBlocker blocker = new WindowBlocker(StringEditor.this);
              try {
                blocker.setBlocked(true);
                exportTra();
              } finally {
                blocker.setBlocked(false);
              }
              return null;
            }
          }.execute();
        } else if (item == miRevertLast) {
          undo();
        } else if (item == miRevertAll) {
          new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception
            {
              WindowBlocker blocker = new WindowBlocker(StringEditor.this);
              try {
                blocker.setBlocked(true);
                revertAll();
              } finally {
                blocker.setBlocked(false);
              }
              return null;
            }
          }.execute();
        }
      }
    }

    @Override
    public void stateChanged(ChangeEvent e)
    {
      if (e.getSource() == tabPane) {
        updateStringTableUI(tabPane.getSelectedIndex());
      } else if (e.getSource() == slider) {
        if (!slider.getValueIsAdjusting() && slider.getValue() != getSelectedIndex()) {
          showEntry(slider.getValue());
        }
      }
    }

    @Override
    public void tableChanged(TableModelEvent e)
    {
      if (e.getType() == TableModelEvent.UPDATE) {
        // tracking changes made to table entries
        updateTableItem(e.getFirstRow());
      }
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
      if (e.getDocument() == taText.getDocument()) {
        updateEntry(getSelectedEntry());
      }
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
      if (e.getDocument() == taText.getDocument()) {
        updateEntry(getSelectedEntry());
      }
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
      // unused
    }
  }

  private class AttributeSearcher implements SearchClient
  {
    private final int selectedRow;

    public AttributeSearcher(int row)
    {
      this.selectedRow = row;
    }

    @Override
    public String getText(int index)
    {
      if (index < 0 || index > StringTable.getNumEntries(getSelectedDialogType())) {
        return null;
      }

      StringTable.StringEntry entry = StringTable.getStringEntry(getSelectedDialogType(), index);
      entry.fillList(index);
      return entry.getFields().get(selectedRow).toString();
    }

    @Override
    public void hitFound(int index)
    {
      showEntry(index);
    }
  }

  /** A simple structure for holding data needed to undo an add/remove operation. */
  private final class UndoAction
  {
    private final StringTable.StringEntry entryMale;
    private final StringTable.StringEntry entryFemale;

    public UndoAction()
    {
      this(null, null);
    }

    public UndoAction(StringTable.StringEntry entryMale, StringTable.StringEntry entryFemale)
    {
      this.entryMale = entryMale;
      this.entryFemale = entryFemale;
    }

    /** Returns whether this action undoes an 'Add' operation. */
    public boolean isUndoAdd()
    {
      return (entryMale == null);
    }

    /** Undo this action. */
    public void undo()
    {
      if (entryMale == null) {
        deleteEntry(false);
      } else {
        addEntry(entryMale, entryFemale, false);
      }
    }
  }

  /** A general-purpose progress monitor. */
  private final class ProgressTracker extends StringTable.ProgressCallback
  {
    private final String title;
    private final String msgSuccess;
    private final String msgFailed;

    private ProgressMonitor pm;
    private int count, step;

    public ProgressTracker(String title, String successMessage, String failedMessage)
    {
      this.title = (title != null) ? title : "";
      this.msgSuccess = successMessage;
      this.msgFailed = failedMessage;
    }

    @Override
    public void init(int numEntries)
    {
      count = numEntries;
      if (count < 50000) {
        step = 500;
      } else if (count < 100000) {
        step = 1000;
      } else if (count < 200000) {
        step = 2000;
      } else {
        step = 5000;
      }
      pm = new ProgressMonitor(StringEditor.this, title, "Initializing...", 0, count);
      pm.setMillisToDecideToPopup(0);
      pm.setMillisToPopup(0);
    }

    @Override
    public void done(boolean success)
    {
      pm.close();
      if (success && msgSuccess != null) {
        JOptionPane.showMessageDialog(StringEditor.this, msgSuccess, "Information", JOptionPane.INFORMATION_MESSAGE);
      } else if (!success && msgFailed != null) {
        JOptionPane.showMessageDialog(StringEditor.this, msgFailed, "Error", JOptionPane.ERROR_MESSAGE);
      }
    }

    @Override
    public boolean progress(int index)
    {
      if ((index % step) == 0) {
        pm.setNote(index + " of " + count);
        pm.setProgress(index);
      }
      return true;
    }

  }
}
