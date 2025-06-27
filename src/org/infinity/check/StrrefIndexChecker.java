// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
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
import org.infinity.resource.TextResource;
import org.infinity.resource.are.AutomapNote;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Compiler;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.bcs.ScriptType;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.Action;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.gam.JournalEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.search.StringReferenceSearcher;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;

public class StrrefIndexChecker extends AbstractChecker implements ListSelectionListener {
  /** Index of "Open" button */
  private static final int BUTTON_OPEN      = 0;
  /** Index of "Open in new window" button */
  private static final int BUTTON_OPEN_NEW  = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;

  private ChildFrame resultFrame;
  private ResultPane<SortableTable> resultPane;

  /** List of the {@link StrrefEntry} objects. */
  private SortableTable table;

  public StrrefIndexChecker() {
    super("Find illegal strrefs", StringReferenceSearcher.FILE_TYPES);
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (resultPane != null && event.getSource() == resultPane.getButton(BUTTON_OPEN)) {
      tableEntryOpened(false);
    } else if (resultPane != null && event.getSource() == resultPane.getButton(BUTTON_OPEN_NEW)) {
      tableEntryOpened(true);
    } else if (resultPane != null && event.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      resultPane.getTable().saveCheckResult(resultFrame,
          "Unknown string references (maximum " + StringTable.getNumEntries() + ")");
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
    table = new SortableTable(new String[] { "File", "Offset / Line:Pos", "Strref" },
        new Class<?>[] { ResourceEntry.class, String.class, Integer.class }, new Integer[] { 100, 50, 50 });

    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      if (runCheck(getFiles())) {
        if (resultFrame != null) {
          resultFrame.close();
        }
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    if (table.getRowCount() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No errors found", "Info",
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

    final String title = table.getRowCount() + " error(s) found";

    resultPane = new ResultPane<>(table, new JButton[] { openButton, openNewButton, saveButton }, title, true, true);
    resultPane.setOnActionPerformed(this::actionPerformed);
    resultPane.setOnTableSelectionChanged(this::valueChanged);
    resultPane.setOnTableAction(this::performTableAction);

    resultFrame = new ChildFrame("Illegal strrefs found", true);
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
      final Resource resource = ResourceFactory.getResource(entry);
      if (resource instanceof DlgResource) {
        checkDialog((DlgResource) resource);
      } else if (resource instanceof BcsResource) {
        checkScript((BcsResource) resource);
      } else if (resource instanceof PlainTextResource) {
        checkText((PlainTextResource) resource);
      } else if (resource instanceof AbstractStruct) {
        checkStruct((AbstractStruct) resource);
      }
      advanceProgress();
    };
  }

  private void checkDialog(DlgResource dialog) {
    for (final StructEntry entry : dialog.getFlatFields()) {
      if (entry instanceof StringRef) {
        final int strref = ((StringRef) entry).getValue();
        if (!isValidStringRef(strref)) {
          synchronized (table) {
            table.addTableItem(new StrrefEntry(dialog.getResourceEntry(), entry.getOffset(), strref));
          }
        }
      } else if (entry instanceof AbstractCode) {
        final AbstractCode code = (AbstractCode) entry;
        try {
          final ScriptType type = code instanceof Action ? ScriptType.ACTION : ScriptType.TRIGGER;
          final Compiler compiler = new Compiler(code.getText(), type);

          final Decompiler decompiler = new Decompiler(compiler.getCode(), type, true);
          decompiler.setGenerateComments(false);
          decompiler.setGenerateResourcesUsed(true);
          decompiler.decompile();
          for (final Integer stringRef : decompiler.getStringRefsUsed()) {
            final int strref = stringRef;
            if (!isValidStringRef(strref)) {
              synchronized (table) {
                table.addTableItem(new StrrefEntry(dialog.getResourceEntry(), entry.getOffset(), strref));
              }
            }
          }
        } catch (Exception e) {
          Logger.error(e);
        }
      }
    }
  }

  private void checkScript(BcsResource script) {
    final Decompiler decompiler = new Decompiler(script.getCode(), true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(true);
    try {
      decompiler.decompile();
      for (final Integer stringRef : decompiler.getStringRefsUsed()) {
        final int strref = stringRef;
        if (!isValidStringRef(strref)) {
          // XXX: search routine may produce false positives
          final String strrefString = stringRef.toString();
          final String source = decompiler.getSource();
          final String[] lines = source.split("\r?\n");
          int line = -1, pos = -1;
          final Pattern pattern = Pattern.compile("\\b" + strrefString + "\\b", Pattern.DOTALL);
          for (int i = 0; i < lines.length; i++) {
            final Matcher matcher = pattern.matcher(lines[i]);
            if (matcher.find()) {
              line = i;
              pos = matcher.start();
              break;
            }
          }
          synchronized (table) {
            table.addTableItem(new StrrefEntry(script.getResourceEntry(), line + 1, pos + 1, strref));
          }
        }
      }
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  private void checkStruct(AbstractStruct struct) {
    for (final StructEntry entry : struct.getFlatFields()) {
      if (entry instanceof StringRef) {
        final int strref = ((StringRef) entry).getValue();
        if (!isValidStringRef(strref)) {
          if (strref >= 3000000 && strref < 4000000
              && (entry.getParent() instanceof AutomapNote || entry.getParent() instanceof JournalEntry)) {
            // skip talk override entries
            continue;
          }
          synchronized (table) {
            table.addTableItem(new StrrefEntry(struct.getResourceEntry(), entry.getOffset(), strref));
          }
        }
      }
    }
  }

  private void checkText(PlainTextResource text) {
    if (text.getResourceEntry().getExtension().equalsIgnoreCase("2DA")) {
      check2da(Table2daCache.get(text.getResourceEntry(), false));
      return;
    }

    final String[] lines = text.getText().split("\r?\n");
    for (int line = 0; line < lines.length; line++) {
      final Matcher matcher = StringReferenceSearcher.NUMBER_PATTERN.matcher(lines[line]);
      while (matcher.find()) {
        final int pos = matcher.start();
        final int len = matcher.end() - pos;
        try {
          final long strref = Long.parseLong(lines[line].substring(pos, pos + len));
          // skip values out of integer range
          if (strref >= Integer.MIN_VALUE && strref <= Integer.MAX_VALUE) {
            if (!isValidStringRef((int) strref)) {
              synchronized (table) {
                table.addTableItem(new StrrefEntry(text.getResourceEntry(), line + 1, pos + 1, (int) strref));
              }
            }
          }
        } catch (NumberFormatException e) {
          Logger.error(e);
        }
      }
    }
  }

  private void check2da(Table2da array) {
    if (array != null) {
      // checking default value
      try {
        long strref = Long.parseLong(array.getDefaultValue());
        if (strref >= Integer.MIN_VALUE
            && strref <= Integer.MAX_VALUE
            && !isValidStringRef((int) strref)) {
          synchronized (table) {
            table.addTableItem(new StrrefEntry(array.getResourceEntry(), array.getDefaultEntry().getLine() + 1,
                array.getDefaultEntry().getPosition() + 1, (int) strref));
          }
        }
      } catch (NumberFormatException e) {
        Logger.trace(e);
      }

      // checking table content
      for (int row = 0, numRows = array.getRowCount(); row < numRows; row++) {
        for (int col = 0, numCols = array.getColCount(row); col < numCols; col++) {
          final Table2da.Entry entry = array.getEntry(row, col);
          try {
            long strref = Long.parseLong(entry.getValue());
            if (strref >= Integer.MIN_VALUE
                && strref <= Integer.MAX_VALUE
                && !isValidStringRef((int) strref)) {
              synchronized (table) {
                table.addTableItem(new StrrefEntry(array.getResourceEntry(), entry.getLine() + 1,
                    entry.getPosition() + 1, (int) strref));
              }
            }
          } catch (NumberFormatException e) {
            Logger.trace(e);
          }
        }
      }
    }
  }

  private boolean isValidStringRef(int strref) {
    strref = StringTable.getTranslatedIndex(strref);
    return (strref >= -1 && strref < StringTable.getNumEntries());
  }

  /**
   * Opens the currently selected table row entry and selects the relevant data. {@code inNewWindow} indicates whether
   * the entry should be opened in a new child window or the main view of the Near Infinity instance.
   */
  private void tableEntryOpened(boolean inNewWindow) {
    final int row = resultPane.getTable().getSelectedRow();
    if (row != -1) {
      final StrrefEntry item = (StrrefEntry)resultPane.getTable().getTableItemAt(row);
      final Resource resource = ResourceFactory.getResource(item.entry);
      if (inNewWindow) {
        new ViewFrame(resultFrame, resource);
      } else {
        NearInfinity.getInstance().showResourceEntry(item.entry);
      }
      if (item.isText && resource instanceof TextResource) {
        ((TextResource) resource).highlightText(item.line, Integer.toString(item.strref));
      } else if (resource instanceof AbstractStruct) {
        ((AbstractStruct) resource).getViewer().selectEntry(item.offset);
      }
    }
  }

  /**
   * Performs the default action on the results table as if the user double-clicked on a table row which opens a new
   * child window with the content of the resource specified in the selected table row.
   */
  private void performTableAction(MouseEvent event) {
    tableEntryOpened(event == null || !event.isAltDown());
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

  private static final class StrrefEntry implements TableItem {
    private final boolean isText;
    private final ResourceEntry entry;
    private final int offset;
    private final int line, pos;
    private final int strref;

    /** Constructor for text resources (2DA, BCS, ...). */
    public StrrefEntry(ResourceEntry entry, int line, int pos, int strref) {
      this.isText = true;
      this.entry = entry;
      this.line = line;
      this.pos = pos;
      this.strref = strref;
      this.offset = -1;
    }

    /** Constructor for structured resources. */
    public StrrefEntry(ResourceEntry entry, int offset, int strref) {
      this.isText = false;
      this.entry = entry;
      this.offset = offset;
      this.strref = strref;
      this.line = this.pos = -1;
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return entry;
        case 1:
          return isText ? (line + ":" + pos) : Integer.toHexString(offset) + 'h';
        default:
          return strref;
      }
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder("File: ");
      sb.append(entry.getResourceName());
      if (isText) {
        sb.append(", Line: ").append(line);
        sb.append(", Position: ").append(pos);
      } else {
        sb.append(", Offset: ").append(Integer.toHexString(offset)).append('h');
      }
      sb.append(", Strref: ").append(strref).append(" (").append(Integer.toHexString(strref)).append("h)");
      return sb.toString();
    }
  }
}
