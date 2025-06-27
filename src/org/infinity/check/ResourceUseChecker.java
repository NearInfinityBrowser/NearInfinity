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
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.StringRef;
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
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.dlg.State;
import org.infinity.resource.dlg.Transition;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;

public final class ResourceUseChecker extends AbstractChecker
    implements Runnable, ListSelectionListener, ActionListener {
  private static final Pattern RESREF_PATTERN = Pattern.compile("\\w{3,8}");

  private static final String[] FILE_TYPES = { "2DA", "ARE", "BCS", "BS", "CHR", "CHU", "CRE", "DLG", "EFF", "INI",
                                               "ITM", "PRO", "SPL", "STO", "VEF", "VVC", "WED", "WMP" };

  private static final String[] CHECK_TYPES = { "ARE", "BAM", "BCS", "CRE", "DLG", "EFF", "ITM", "PRO", "SPL", "STO",
                                                "TIS", "VEF", "VVC", "WAV", "WED" };

  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private final TreeMap<String, ResourceEntry> unusedResources = new TreeMap<>(String::compareToIgnoreCase);
  private final HashSet<String> checkTypes = new HashSet<>(CHECK_TYPES.length * 2);

  private ChildFrame resultFrame;
  private ResultPane<SortableTable> resultPane;

  /** List of the {@link BCSIDSErrorTableLine} objects. */
  private SortableTable table;

  public ResourceUseChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, CHECK_TYPES);
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (resultPane != null && event.getSource() == resultPane.getButton(BUTTON_OPEN)) {
      final int row = table.getSelectedRow();
      if (row != -1) {
        NearInfinity.getInstance().showResourceEntry(getResourceEntryAt(row));
      }
    } else if (resultPane != null && event.getSource() == resultPane.getButton(BUTTON_OPEN_NEW)) {
      performTableAction(null);
    } else if (resultPane != null && event.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      table.saveCheckResult(resultFrame, "Unused resources");
    } else {
      super.actionPerformed(event);
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
    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final ArrayList<ResourceEntry> files = new ArrayList<>();
      for (final String fileType : FILE_TYPES) {
        files.addAll(ResourceFactory.getResources(fileType));
      }

      checkTypes.clear();
      final List<JCheckBox> typeList = getSelector().getFileTypes();
      for (final JCheckBox cb : typeList) {
        if (cb.isSelected()) {
          checkTypes.add(cb.getText());
        }
      }

      for (final ResourceEntry e : getFiles()) {
        unusedResources.put(e.getResourceName(), e);
      }

      if (runSearch("Searching", files)) {
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    if (unusedResources.isEmpty()) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No unused resources found", "Info",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    table = new SortableTable(new String[] { "File", "Name" },
        new Class<?>[] { ResourceEntry.class, String.class }, new Integer[] { 50, 150 });
    for (final Entry<String, ResourceEntry> entry : unusedResources.entrySet()) {
      table.addTableItem(new UnusedFileTableItem(entry.getValue()));
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

    final String title = table.getRowCount() + " unused resources found";

    resultPane = new ResultPane<>(table, new JButton[] { openButton, openNewButton, saveButton }, title, true, true);
    resultPane.setOnActionPerformed(this::actionPerformed);
    resultPane.setOnTableSelectionChanged(this::valueChanged);
    resultPane.setOnTableAction(this::performTableAction);

    resultFrame = new ChildFrame("Result", true);
    resultFrame.setIconImage(Icons.ICON_FIND_16.getIcon().getImage());
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
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof DlgResource) {
        checkDialog((DlgResource) resource);
      } else if (resource instanceof BcsResource) {
        checkScript((BcsResource) resource);
      } else if (resource instanceof PlainTextResource) {
        checkTextfile((PlainTextResource) resource);
      } else if (resource instanceof AbstractStruct) {
        checkStruct((AbstractStruct) resource);
      }
      advanceProgress();
    };
  }

  private void checkDialog(DlgResource dialog) {
    for (final StructEntry entry : dialog.getFields()) {
      if (entry instanceof ResourceRef) {
        checkResourceRef((ResourceRef) entry);
      } else if (entry instanceof AbstractCode) {
        try {
          final AbstractCode code = (AbstractCode) entry;
          final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
          final Compiler compiler = new Compiler(code.getText(), type);

          checkCode(compiler.getCode(), type);
        } catch (Exception e) {
          Logger.error(e);
        }
      } else if (checkTypes.contains("WAV") && (entry instanceof State || entry instanceof Transition)) {
        for (final StructEntry e : ((AbstractStruct) entry).getFlatFields()) {
          if (e instanceof StringRef) {
            checkSound((StringRef) e);
          }
        }
      }
    }
  }

  private void checkScript(BcsResource script) {
    try {
      checkCode(script.getCode(), ScriptType.BCS);
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  private void checkStruct(AbstractStruct struct) {
    for (final StructEntry entry : struct.getFlatFields()) {
      if (entry instanceof ResourceRef) {
        checkResourceRef((ResourceRef) entry);
      } else if (checkTypes.contains("WAV") && entry instanceof StringRef) {
        checkSound((StringRef) entry);
      }
    }
  }

  private void checkTextfile(PlainTextResource text) {
    final Matcher m = RESREF_PATTERN.matcher(text.getText());
    while (m.find()) {
      for (final String checkType : checkTypes) {
        removeEntries(m.group() + '.' + checkType);
      }
    }
  }

  /**
   * Removes from {@link #unusedResources} all resources to which the script code refers.
   * <p>
   * This method can be called from several threads
   *
   * @param compiledCode Compiled code from BCS, dialog action or trigger. Must not be {@code null}
   *
   * @throws Exception If {@code compiledCode} contains invalid code
   */
  private void checkCode(String compiledCode, ScriptType type) throws Exception {
    final Decompiler decompiler = new Decompiler(compiledCode, type, true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    decompiler.decompile();

    synchronized (unusedResources) {
      for (final ResourceEntry entry : decompiler.getResourcesUsed()) {
        unusedResources.remove(entry.getResourceName());
      }
      for (final int strref : decompiler.getStringRefsUsed()) {
        checkSound(strref);
      }
    }
  }

  /**
   * If type (a.k.a. extension) of the resource equals to the {@link #checkTypes} type of checking resources, removes
   * specified resource name from {@link #unusedResources}, otherwise do nothing.
   * <p>
   * This method can be called from several threads
   *
   * @param ref Reference to the resource for checking
   */
  private void checkResourceRef(ResourceRef ref) {
    final String type = ref.getType().toUpperCase();
    if (checkTypes.contains(type)) {
      removeEntries(ref.getResourceName());
    }
  }

  /**
   * If string reference has the associated sound, removes this sound from {@link #unusedResources}, otherwise do
   * nothing.
   * <p>
   * This method can be called from several threads
   *
   * @param ref Reference to entry in string table that contains sound file name
   */
  private void checkSound(StringRef ref) {
    if (ref != null) {
      checkSound(ref.getValue());
    }
  }

  /**
   * If string reference has the associated sound, removes this sound from {@link #unusedResources}, otherwise do
   * nothing.
   * <p>
   * This method can be called from several threads
   *
   * @param strref Reference to entry in string table that contains sound file name
   */
  private void checkSound(int strref) {
    if (strref >= 0) {
      final String wav = StringTable.getSoundResource(strref);
      if (!wav.isEmpty()) {
        removeEntries(wav + ".WAV");
      }
    }
  }

  /**
   * Removes from {@link #unusedResources} all entries that name equals {@code name}.
   * <p>
   * This method can be called from several threads
   *
   * @param name Resource name that must be erased from unused list
   */
  private void removeEntries(String name) {
    synchronized (unusedResources) {
      unusedResources.remove(name);
    }
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the content of the resource specified in the selected table row..
   */
  private void performTableAction(MouseEvent event) {
    final int row = table.getSelectedRow();
    if (row != -1) {
      final Resource resource = ResourceFactory.getResource(getResourceEntryAt(row));
      new ViewFrame(resultFrame, resource);
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

  private static final class UnusedFileTableItem implements TableItem {
    private final ResourceEntry file;

    private UnusedFileTableItem(ResourceEntry file) {
      this.file = file;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      if (columnIndex == 0) {
        return file;
      }
      return file.getSearchString();
    }

    @Override
    public String toString() {
      return String.format("File: %s, Name: %s", file.getResourceName(), file.getSearchString());
    }
  }
}
