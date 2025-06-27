// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.check;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
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
import org.infinity.util.CharsetDetector;
import org.infinity.util.DynamicByteArray;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.StringTable;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

public class StringValidationChecker extends AbstractSearcher
    implements Runnable, ActionListener, ListSelectionListener, PropertyChangeListener {
  /** Index of "Open in StringRef Lookup" button */
  private static final int BUTTON_LOOKUP    = 0;
  /** Index of "Open in String Table" button */
  private static final int BUTTON_TABLE     = 1;
  /** Index of "Save" button */
  private static final int BUTTON_SAVE      = 2;
  /** Index of "Repair" button */
  private static final int BUTTON_REPAIR    = 3;

  private ChildFrame resultFrame;
  private ResultPane<SortableTable> resultPane;
  private SortableTable table;
  private SwingWorker<Couple<Integer, Integer>, Void> repairWorker;
  private ProgressMonitor repairProgress;

  public StringValidationChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
    table = new SortableTable(new String[] { "String table", "StringRef", "String", "Error message" },
        new Class<?>[] { ResourceEntry.class, Integer.class, String.class, String.class },
        new Integer[] { 100, 75, 500, 300 });

    final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    blocker.setBlocked(true);
    try {
      final List<ResourceEntry> files = new ArrayList<>();
      for (final Profile.Key key : new Profile.Key[] { Profile.Key.GET_GAME_DIALOG_FILE,
          Profile.Key.GET_GAME_DIALOGF_FILE }) {
        final Path path = Profile.getProperty(key);
        if (path != null) {
          final ResourceEntry entry = new FileResourceEntry(path);
          files.add(entry);
        }
      }

      if (files.isEmpty()) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Could not determine path of the string table.",
            "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      if (runSearch("Checking strings", files)) {
        return;
      }
    } finally {
      blocker.setBlocked(false);
    }

    if (table.getRowCount() == 0) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No string encoding errors found.", "Info",
          JOptionPane.INFORMATION_MESSAGE);
      return;
    }

    table.tableComplete();

    final JButton openLookupButton = new JButton("Open in StringRef Lookup", Icons.ICON_OPEN_16.getIcon());
    openLookupButton.setMnemonic('l');
    openLookupButton.setToolTipText("Only available for male strings.");

    final JButton openStringTableButton = new JButton("Open in String table", Icons.ICON_OPEN_16.getIcon());
    openStringTableButton.setMnemonic('t');

    final JButton saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
    saveButton.setMnemonic('s');

    final JButton repairButton = new JButton("Repair", Icons.ICON_CHECK_16.getIcon());
    repairButton.setToolTipText("Replace or remove malformed characters.");
    repairButton.setMnemonic('r');

    // counting individual strings
    final HashSet<String> errorMap = new HashSet<>();
    for (int row = 0, rowCount = table.getRowCount(); row < rowCount; row++) {
      final StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
      errorMap.add(item.resource.toString() + item.strref);
    }

    final String title = table.getRowCount() + " error(s) found in " + errorMap.size() + " string(s)";

    resultPane = new ResultPane<>(table,
        new JButton[] { openLookupButton, openStringTableButton, saveButton, repairButton }, title);
    resultPane.setOnActionPerformed(this::actionPerformed);
    resultPane.setOnTableSelectionChanged(this::valueChanged);
    resultPane.setOnTableAction(this::performTableAction);

    resultFrame = new ChildFrame("Result of string validation", true);
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
    // checking strings for malformed or unmappable UTF-8 characters
    return () -> {
      final Path dlgPath = entry.getActualPath();
      final boolean isFemale = Profile.getProperty(Profile.Key.GET_GLOBAL_DIALOG_NAME_FEMALE).toString()
          .equalsIgnoreCase(dlgPath.getFileName().toString());

      try (FileChannel ch = FileChannel.open(dlgPath, StandardOpenOption.READ)) {
        final int ENTRY_SIZE = 26;

        // parsing header
        String sig = StreamUtils.readString(ch, 8);
        if (!"TLK V1  ".equals(sig)) {
          throw new Exception("Invalid TLK signature");
        }

        ch.position(ch.position() + 2); // skip language id
        int numEntries = StreamUtils.readInt(ch);
        int ofsStrings = StreamUtils.readInt(ch);

        int bufferSize = ENTRY_SIZE * numEntries;
        ByteBuffer headerData = StreamUtils.getByteBuffer(bufferSize);
        if (ch.read(headerData) < bufferSize) {
          throw new Exception("Not enough data");
        }
        headerData.position(0);

        // calculating max. string length
        int maxLength = 0;
        for (int idx = 0; idx < numEntries; idx++) {
          int ofs = idx * ENTRY_SIZE; // rel. offset entry
          headerData.position(ofs + 0x16);
          int length = headerData.getInt();
          if (length > maxLength) {
            maxLength = length;
          }
        }
        final ByteBuffer inBuf = ByteBuffer.allocate(maxLength);

        CharsetDecoder decoder = StringTable.getCharset().newDecoder();
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        decoder.onMalformedInput(CodingErrorAction.REPORT);
        int maxCharLength = (int) Math.ceil(decoder.maxCharsPerByte() * maxLength);
        final CharBuffer outBuf = CharBuffer.allocate(maxCharLength);

        // processing strings
        for (int idx = 0; idx < numEntries; idx++) {
          int ofs = idx * ENTRY_SIZE; // rel. offset entry
          headerData.position(ofs + 0x12);
          int ofsString = headerData.getInt();
          ofsString += ofsStrings;
          int lenString = headerData.getInt();
          if (lenString > 0) {
            ch.position(ofsString);

            // reading string as byte array
            inBuf.limit(lenString);
            inBuf.position(0);
            int len = ch.read(inBuf);
            if (len < lenString) {
              throw new IOException(
                  String.format("%s: Could not read string entry %d at file offset %d", entry, idx, ofsString));
            }
            inBuf.flip();
            outBuf.position(0);

            try {
              validateInput(decoder, inBuf, outBuf, entry, idx, isFemale);
            } catch (IllegalStateException | CoderMalfunctionError e) {
              Logger.error(e);
            }
          }
          decoder.reset();
        }
      } catch (Exception e) {
        Logger.error(e);
      }

      advanceProgress();
    };
  }

  /** Encodes the given byte array in the specified charset and reports any errors or potential errors. */
  private void validateInput(CharsetDecoder decoder, ByteBuffer inBuf, CharBuffer outBuf, ResourceEntry entry,
      int strref, boolean isFemale) throws IllegalStateException, CoderMalfunctionError {
    if (decoder == null || inBuf == null) {
      return;
    }
    if (outBuf == null) {
      final int maxCharLength = (int) Math.ceil(decoder.maxCharsPerByte() * inBuf.limit());
      outBuf = CharBuffer.allocate(maxCharLength);
    }

    if (decoder.charset().equals(StandardCharsets.UTF_8)) {
      validateInputUnicode(decoder, inBuf, outBuf, entry, strref, isFemale);
    } else {
      validateInputAnsi(decoder, inBuf, outBuf, entry, strref, isFemale);
    }
  }

  private void validateInputUnicode(CharsetDecoder decoder, ByteBuffer inBuf, CharBuffer outBuf, ResourceEntry entry,
      int strref, boolean isFemale) throws IllegalStateException, CoderMalfunctionError {
    outBuf.limit(outBuf.capacity());
    outBuf.position(0);
    int lenString = inBuf.limit();

    // report all errors
    while (lenString > 0) {
      final CoderResult cr = decoder.decode(inBuf, outBuf, true);
      if (cr.isError()) {
        synchronized (this) {
          final String text = StringTable.getStringRef(isFemale ? StringTable.Type.FEMALE : StringTable.Type.MALE,
              strref);
          final int offset = inBuf.position();
          final int length = cr.length();
          final byte[] buffer = Arrays.copyOf(inBuf.array(), inBuf.limit());
          if (cr.isMalformed()) {
            table.addTableItem(new StringErrorTableItem(entry, strref, text,
                new StringError(isFemale, strref, buffer, offset, length, "malformed input data")));
          }
          if (cr.isUnmappable()) {
            table.addTableItem(new StringErrorTableItem(entry, strref, text,
                new StringError(isFemale, strref, buffer, offset, length, "unmappable character")));
          }
        }
      }
      if (inBuf.position() < inBuf.limit() && cr.length() > 0) {
        inBuf.position(inBuf.position() + cr.length());
        for (int i = cr.length(); i > 0; i--) {
          outBuf.put('\ufffd'); // adding replacement character
        }
        lenString -= cr.length();
      } else {
        break;
      }
    }
  }

  private void validateInputAnsi(CharsetDecoder decoder, ByteBuffer inBuf, CharBuffer outBuf, ResourceEntry entry,
      int strref, boolean isFemale) throws IllegalStateException, CoderMalfunctionError {
    outBuf.limit(outBuf.capacity());
    outBuf.position(0);
    decoder.decode(inBuf, outBuf, true);
    outBuf.flip();
    final String textAnsi = outBuf.toString();

    // performing detection of utf-8 characters in ansi byte code
    inBuf.flip();
    final byte[] buffer = Arrays.copyOf(inBuf.array(), inBuf.limit());
    int offset = 0;
    while (offset < buffer.length) {
      final StringError se = validateUtf8Input(buffer, offset, buffer.length - offset, isFemale, strref);
      if (se == null) {
        break;
      }
      table.addTableItem(new StringErrorTableItem(entry, strref, textAnsi, se));
      offset = se.getOffset() + se.getLength();
    }
  }

  /**
   * Analyzes the specified buffer for multi-byte UTF-8 characters and returns them as {@link StringError} objects.
   *
   * @param buffer Byte buffer with raw text data.
   * @param offset Start offset for the analyzation process.
   * @param len Max. number of bytes to analyze.
   * @return An initialized {@link StringError} if a multi-byte UTF-8 character was found, {@code null} otherwise.
   */
  private StringError validateUtf8Input(byte[] buffer, int offset, int len, boolean isFemale, int strref) {
    if (buffer == null || offset < 0 || len <= 0 || offset + len > buffer.length) {
      return null;
    }

    // Limiting max. number of bytes per utf-8 code to the specified ANSI code page
    // to reduce detection of false positives.
    final int maxBytes = CharsetDetector.getMaxAnsiUtf8Length(StringTable.getCharset());

    final int length = offset + len;
    for (int i = offset; i < length; i++) {
      final int b1 = buffer[i] & 0xff;
      if (b1 >= 0x80) {
        switch (b1) {
          case 0xc0:
          case 0xc1:
          case 0xf5:
          case 0xf6:
          case 0xf7:
          case 0xf8:
          case 0xf9:
          case 0xfa:
          case 0xfb:
          case 0xfc:
          case 0xfd:
          case 0xfe:
          case 0xff:
            // not a legal UTF-8 code
            continue;
        }

        if (maxBytes > 1 && (b1 & 0xe0) == 0xc0 && i + 1 < length) {
          // two bytes
          final int b2 = buffer[i + 1] & 0xff;
          if ((b2 & 0xc0) == 0x80) {
            return new StringError(isFemale, strref, buffer, i, 2, "encoding error");
          }
        } else if (maxBytes > 2 && (b1 & 0xf0) == 0xe0 && i + 2 < length) {
          // three bytes
          final int b2 = buffer[i + 1] & 0xff;
          final int b3 = buffer[i + 2] & 0xff;
          if ((b2 & 0xc0) == 0x80 && (b3 & 0xc0) == 0x80) {
            return new StringError(isFemale, strref, buffer, i, 3, "encoding error");
          }
        } else if (maxBytes > 3 && (b1 & 0xf8) == 0xf0 && i + 3 < length) {
          // four bytes
          final int b2 = buffer[i + 1] & 0xff;
          final int b3 = buffer[i + 2] & 0xff;
          final int b4 = buffer[i + 3] & 0xff;
          if ((b2 & 0xc0) == 0x80 && (b3 & 0xc0) == 0x80 && (b4 & 0xc0) == 0x80) {
            final int codePoint = ((b1 & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3f) << 6) | (b4 & 0x3f);
            if (codePoint < 0x110000) {
              return new StringError(isFemale, strref, buffer, i, 4, "encoding error");
            }
          }
        }
        // else assume legal ANSI character
      }
    }

    return null;
  }

  /** Executes the repair operation as a background task with UI feedback. */
  private void repairEntriesBackground() {
    final String note = (table.getModel().getRowCount() >= 1000) ? "This may take a while..." : null;
    repairProgress = new ProgressMonitor(resultFrame, "Repairing game strings", note, 0, table.getModel().getRowCount());
    repairProgress.setMillisToDecideToPopup(100);
    repairProgress.setMillisToPopup(250);

    repairWorker = new SwingWorker<Couple<Integer, Integer>, Void>() {
      @Override
      protected Couple<Integer, Integer> doInBackground() {
        return repairEntries(repairWorker);
      }
    };
    repairWorker.addPropertyChangeListener(this);
    repairWorker.execute();
  }

  /**
   * Performs the repair operation on all available table entries.
   *
   * @param worker Optional {@code SwingWorker} instance to provide update information. Specify {@code null} to ignore.
   * @return A {@link Couple} with the number of replacements in the first slot and the number of removals in the second
   *         slot. Returns {@code null} if the operation was cancelled.
   */
  private Couple<Integer, Integer> repairEntries(SwingWorker<?,?> worker) {
    Couple<Integer, Integer> retVal = null;

    // Collecting and sorting encoding issues:
    // Correct sorting is required to properly repair multiple issues in a single string.
    final List<StringError> itemList = new ArrayList<>();
    for (int i = 0; i < table.getModel().getRowCount(); i++) {
      final Object o = table.getTableItemAt(i).getObjectAt(3);
      if (o instanceof StringError) {
        itemList.add((StringError)o);
      } else {
        Logger.warn("Unexpected item type at row {}: {}", i, o.getClass().getSimpleName());
      }
    }
    itemList.sort(Comparator.reverseOrder());

    // repairing issues
    boolean isCancelled = false;
    // for statistics: number of replaced and removed characters
    int numReplaced = 0;
    int numRemoved = 0;
    // Stores errors found in a single string entry
    final TreeSet<StringError> issues = new TreeSet<>((se1, se2) -> se2.getOffset() - se1.getOffset());
    boolean isFemale = itemList.get(0).isFemale();
    int strref = -1;
    for (int i = 0, count = itemList.size(); i < count; i++) {
      final StringError error = itemList.get(i);
      if (error.isFemale() != isFemale || error.getStrref() != strref) {
        // applying fixes
        final Couple<Integer, Integer> result = repairStringEntry(issues, isFemale, strref);
        numReplaced += result.getValue0();
        numRemoved += result.getValue1();
        issues.clear();
        isFemale = error.isFemale();
        strref = error.getStrref();
      }
      issues.add(error);
      if (worker != null) {
        if (worker.isCancelled()) {
          isCancelled = true;
          break;
        }
        worker.firePropertyChange("progress", i - 1, i);
      }
    }

    if (!isCancelled) {
      if (!issues.isEmpty()) {
        // applying fixes to remaining group of errors
        final Couple<Integer, Integer> result = repairStringEntry(issues, isFemale, strref);
        numReplaced += result.getValue0();
        numRemoved += result.getValue1();
      }

      // writing changes back to disk
      if (numReplaced + numRemoved > 0) {
        StringTable.write(null);
      }

      retVal = new Couple<>(numReplaced, numRemoved);
    }

    return retVal;
  }

  /**
   * Performs repair operations on the string referenced by the parameters and returns the result.
   *
   * @param issues   Set of {@link StringError} objects defining individual issues in the specified string.
   * @param isFemale Specify {@code true} to process a string in the female talk table. Specify {@code false} to process
   *                   a default/male string entry.
   * @param strref   The string reference index to repair.
   * @return A {@link Couple} with the number of replacements in the first slot and the number of removals in the second
   *         slot.
   */
  private Couple<Integer, Integer> repairStringEntry(TreeSet<StringError> issues, boolean isFemale, int strref) {
    final Couple<Integer, Integer> retVal = new Couple<>(0, 0);
    if (!issues.isEmpty()) {
      final StringTable.Type tlkType = isFemale ? StringTable.Type.FEMALE : StringTable.Type.MALE;
      final DynamicByteArray array = new DynamicByteArray(StringTable.getStringEntry(tlkType, strref).getBuffer());
      for (final StringError issue : issues) {
        final int ofs = issue.getOffset();
        final int len = issue.getLength();
        final String replacement = issue.getRepaired();
        array.delete(ofs, len);
        if (replacement.isEmpty()) {
          retVal.setValue1(retVal.getValue1() + 1);
        } else {
          try {
            array.insert(ofs, replacement.getBytes(StringTable.getCharset()));
          } catch (Exception e) {
            Logger.error(e);
            throw e;
          }
          retVal.setValue0(retVal.getValue0() + 1);
        }
      }
      final String newText = new String(array.getArray(), StringTable.getCharset());
      StringTable.setStringRef(tlkType, strref, newText);
    }
    return retVal;
  }

  private void tableEntryOpened(boolean openInEditor) {
    final SortableTable table = resultPane.getTable();
    final int row = table.getSelectedRow();
    if (row != -1) {
      final StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
      if (openInEditor) {
        final StringTable.Type type = item.isFemaleDialog() ? StringTable.Type.FEMALE : StringTable.Type.MALE;
        ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(type, item.strref);
      } else if (!item.isFemaleDialog()) {
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

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == resultPane.getButton(BUTTON_LOOKUP)) {
      tableEntryOpened(false);
    } else if (e.getSource() == resultPane.getButton(BUTTON_TABLE)) {
      tableEntryOpened(true);
    } else if (e.getSource() == resultPane.getButton(BUTTON_SAVE)) {
      table.saveCheckResult(resultFrame, "Encoding errors in game strings");
    } else if (e.getSource() == resultPane.getButton(BUTTON_REPAIR)) {
      // Check for modified string table
      if (StringTable.isModified()) {
        final int result = JOptionPane.showConfirmDialog(resultFrame,
            "The string table contains unsaved data. Save?", "Question",
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
          StringTable.write(null);
        } else if (result == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }

      // perform repair operation
      final int count = table.getModel().getRowCount();
      String msg = (count > 1) ? "Repair all " + count + " issues?" : "Repair " + count + " issue?";
      if (!Profile.isEnhancedEdition()) {
        msg += "\nCaution: Operation may be inaccurate for some languages.";
      }
      final int result = JOptionPane.showConfirmDialog(resultFrame, msg, "Question", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (result == JOptionPane.YES_OPTION) {
        repairEntriesBackground();
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event) {
    if (event.getSource() instanceof ListSelectionModel) {
      final ListSelectionModel model = (ListSelectionModel)event.getSource();
      final int row = model.getMinSelectionIndex();
      if (row != -1) {
        final StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
        resultPane.getButton(BUTTON_LOOKUP).setEnabled(!item.isFemaleDialog());
      } else {
        resultPane.getButton(BUTTON_LOOKUP).setEnabled(false);
      }
      resultPane.getButton(BUTTON_TABLE).setEnabled(row != -1);
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------

  // --------------------- Start Interface PropertyChangeListener ---------------------

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if (evt.getSource() == repairWorker) {
      if ("progress".equals(evt.getPropertyName())) {
        final int index = (Integer)evt.getNewValue();
        repairProgress.setProgress(index);
        if (repairProgress.isCanceled()) {
          repairWorker.cancel(false);
        }
      } else if ("state".equals(evt.getPropertyName())) {
        if (SwingWorker.StateValue.DONE == evt.getNewValue()) {
          // repair operation completed
          repairProgress.close();
          Couple<Integer, Integer> result = null;
          boolean isCancelled = repairWorker.isCancelled();
          try {
            result = repairWorker.get();
          } catch (InterruptedException | ExecutionException e) {
            Logger.error(e);
          } catch (CancellationException e) {
            isCancelled = true;
          }

          if (result != null) {
            final int numReplaced = result.getValue0();
            final int numRemoved = result.getValue1();
            JOptionPane.showMessageDialog(resultFrame,
                "Repair operation completed.\nReplaced characters: " + numReplaced + "\nRemoved characters: " + numRemoved,
                "Information", JOptionPane.INFORMATION_MESSAGE);
          } else {
            if (isCancelled) {
              JOptionPane.showMessageDialog(resultFrame,
                  "Repair operation was cancelled.\nString table may contain unsaved changes.", "Information",
                  JOptionPane.INFORMATION_MESSAGE);
            } else {
              StringTable.resetAll();
              JOptionPane.showMessageDialog(resultFrame,
                  "Repair operation failed.\nString table may contain unsaved changes.", "Error",
                  JOptionPane.ERROR_MESSAGE);
            }
          }
          repairProgress = null;
          repairWorker = null;

          resultFrame.close();
        }
      }
    }
  }

  // --------------------- End Interface PropertyChangeListener ---------------------

  // -------------------------- INNER CLASSES --------------------------

  /**
   * This class describes a single character encoding error in a game string.
   */
  public static final class StringError implements Comparable<StringError> {
    private final StringTable.Type tlkType;
    private final int strref;
    private final byte[] buffer;
    private final int offset;
    private final int length;
    private final String errorDesc;

    /**
     * Initializes a new StringError object.
     *
     * @param isFemale Indicates whether the string belongs to the female or male/default string table.
     * @param strref The string reference index.
     * @param buffer Byte content of the whole string referenced by {@code strref}.
     * @param offset Byte offset of malformed characters.
     * @param length Number of malformed bytes.
     * @param errorDesc A short error description.
     * @throws IllegalArgumentException if one or more of the given parameters are invalid.
     */
    public StringError(boolean isFemale, int strref, byte[] buffer, int offset, int length, String errorDesc)
        throws IllegalArgumentException {
      this.tlkType = isFemale ? StringTable.Type.FEMALE : StringTable.Type.MALE;
      this.strref = validateStrref(strref);
      this.buffer = validateBuffer(buffer);
      this.offset = validateOffset(this.buffer, offset);
      this.length = validateLength(this.buffer, this.offset, length);
      this.errorDesc = validateErrorDesc(errorDesc);
    }

    /**
     * Returns {@code true} if the string belongs to the female string table {@code DIALOGF.TLK}. Otherwise it belongs
     * to the male/default string table {@code DIALOG.TLK}. A female string table exists only for selected languages.
     */
    public boolean isFemale() {
      return tlkType == StringTable.Type.FEMALE;
    }

    /** Returns the string reference index of the string. */
    public int getStrref() {
      return strref;
    }

    /** Returns the whole string as byte buffer. */
    public byte[] getBuffer() {
      return buffer;
    }

    /** Returns the byte offset of malformed characters in the original string. */
    public int getOffset() {
      return offset;
    }

    /** Returns the number of malformed bytes. */
    public int getLength() {
      return length;
    }

    /** Returns a short error description as string. */
    public String getErrorDesc() {
      return errorDesc;
    }

    /**
     * Performs an analysis on the malformed data and attempts to repair it. Returns a string representation of the
     * fixed data, or empty string if fixing is not possible.
     *
     * @return Repaired string representation of the malformed bytes if successful, an empty string otherwise.
     */
    public String getRepaired() {
      StringBuilder retVal = new StringBuilder();
      final byte[] data = Arrays.copyOfRange(buffer, offset, offset + length);
      if (StringTable.getCharset().equals(StandardCharsets.UTF_8)) {
        // Fixing UTF-8 charset
        final String curLangCode = Profile.getProperty(Profile.Key.GET_GAME_LANG_FOLDER_NAME);
        Charset cs = null;
        try {
          cs = Charset.forName(CharsetDetector.getDefaultCharset(curLangCode));
        } catch (UnsupportedCharsetException e) {
          cs = StandardCharsets.US_ASCII;
        }

        for (final Charset curCharset : new Charset[] { cs, Charset.forName("windows-1252") }) {
          final CharsetDecoder csd = curCharset.newDecoder();
          csd.onMalformedInput(CodingErrorAction.REPORT);
          csd.onUnmappableCharacter(CodingErrorAction.REPORT);
          final int maxCharLength = (int) Math.ceil(csd.maxCharsPerByte() * data.length);
          final CharBuffer outBuf = CharBuffer.allocate(maxCharLength);
          final CoderResult cr = csd.decode(ByteBuffer.wrap(data), outBuf, true);
          if (!cr.isError()) {
            retVal = new StringBuilder(outBuf.flip().toString());
            break;
          }
        }
      } else {
        // Fixing ANSI/multi-byte charset
        if (data.length > 0) {
          // attempting to find a replacement string
          int ofs = 0;
          while (ofs < data.length) {
            int codePoint = 0;
            final int b1 = data[ofs] & 0xff;
            if ((b1 & 0xe0) == 0xc0 && ofs + 1 < data.length) {
              // two bytes
              final int b2 = data[ofs + 1] & 0xff;
              if ((b2 & 0xc0) == 0x80) {
                codePoint = ((b1 & 0x1f) << 6) | (b2 & 0x3f);
              }
              ofs += 2;
            } else if ((b1 & 0xf0) == 0xe0 && ofs + 2 < data.length) {
              // three bytes
              final int b2 = data[ofs + 1] & 0xff;
              final int b3 = data[ofs + 2] & 0xff;
              if ((b2 & 0xc0) == 0x80 && (b3 & 0xc0) == 0x80) {
                codePoint = ((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f);
              }
              ofs += 3;
            } else if ((b1 & 0xf8) == 0xf0 && ofs + 3 < data.length) {
              // four bytes
              final int b2 = data[ofs + 1] & 0xff;
              final int b3 = data[ofs + 2] & 0xff;
              final int b4 = data[ofs + 3] & 0xff;
              if ((b2 & 0xc0) == 0x80 && (b3 & 0xc0) == 0x80 && (b4 & 0xc0) == 0x80) {
                codePoint = ((b1 & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3f) << 6) | (b4 & 0x3f);
              }
              ofs += 4;
            }

            if (codePoint > 0) {
              // Performing thorough analysis on byte data
              boolean isUtf = true;
              try {
                final char[] chars = Character.toChars(codePoint);
                final CharsetEncoder cse = StringTable.getCharset().newEncoder();
                cse.onMalformedInput(CodingErrorAction.REPORT);
                cse.onUnmappableCharacter(CodingErrorAction.REPORT);
                final CoderResult ecr = cse.encode(CharBuffer.wrap(chars), ByteBuffer.allocate(chars.length * 4), true);
                if (!ecr.isError()) {
                  // Add only if utf-8 code point defines a valid character in the local charset of the string table
                  retVal.append(new String(chars));
                } else {
                  isUtf = false;
                }
              } catch (IllegalArgumentException e) {
                // not a valid Unicode code point
                isUtf = false;
              }

              if (!isUtf) {
                // Test if raw bytes already defined valid characters in the local charset of the string table
                final CharsetDecoder csd = StringTable.getCharset().newDecoder();
                csd.onMalformedInput(CodingErrorAction.REPORT);
                csd.onUnmappableCharacter(CodingErrorAction.REPORT);
                final ByteBuffer bb = ByteBuffer.wrap(data);
                final CharBuffer cb = CharBuffer.allocate(data.length * 2);
                final CoderResult dcr = csd.decode(bb, cb, true);
                if (!dcr.isError()) {
                  // Restoring original content
                  cb.flip();
                  retVal.append(cb);
                }
              }
            }
          }
        }
      }

      return retVal.toString();
    }

    /** Returns the character set used for encoding bytes into string data. */
    private static Charset getCharset() {
      return StringTable.getCharset();
    }

    @Override
    public int compareTo(StringError o) {
      int retVal = tlkType.ordinal() - o.tlkType.ordinal();
      if (retVal != 0) {
        return retVal;
      }
      retVal = strref - o.strref;
      if (retVal != 0) {
        return retVal;
      }
      retVal = offset - o.offset;
      return retVal;
    }

    private static int validateStrref(int strref) throws IllegalArgumentException {
      if (strref < 0 || strref >= StringTable.getNumEntries()) {
        throw new IllegalArgumentException("Strref is out of bounds");
      }
      return strref;
    }

    private static byte[] validateBuffer(byte[] buffer) throws IllegalArgumentException {
      if (buffer == null) {
        throw new IllegalArgumentException("Buffer is null");
      }
      return buffer;
    }

    private static int validateOffset(byte[] buffer, int offset) throws IllegalArgumentException {
      if (offset < 0) {
        throw new IllegalArgumentException("Offset is negative");
      } else if (offset >= buffer.length) {
        throw new IllegalArgumentException("Offset is out of bounds");
      }
      return offset;
    }

    private static int validateLength(byte[] buffer, int offset, int length) throws IllegalArgumentException {
      if (length <= 0) {
        throw new IllegalArgumentException("Length is <= 0");
      } else if (offset + length > buffer.length) {
        throw new IllegalArgumentException("Invalid length");
      }
      return length;
    }

    private static String validateErrorDesc(String errorType) throws IllegalArgumentException {
      if (errorType == null || errorType.isEmpty()) {
        errorType = "encoding error";
      }
      if (getCharset().equals(StandardCharsets.UTF_8)) {
        errorType = Character.toUpperCase(errorType.charAt(0)) + errorType.substring(1);
      } else {
        errorType = Character.toLowerCase(errorType.charAt(0)) + errorType.substring(1);
      }
      return errorType;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      if (!getCharset().equals(StandardCharsets.UTF_8)) {
        sb.append("Possible ");
      }
      sb.append(errorDesc).append(" found at offset ").append(offset).append(", length: ").append(length);
      sb.append(length == 1 ? " byte" : " bytes");
      return sb.toString();
    }
  }

  private static final class StringErrorTableItem implements TableItem {
    private static final Pattern REGEX_LINEBREAK = Pattern.compile("\r?\n");

    private final ResourceEntry resource;
    private final Integer strref;
    private final String text;
    private final StringError error;

    public StringErrorTableItem(ResourceEntry dlg, int strref, String text, StringError error) {
      this.resource = dlg;
      this.strref = strref;
      this.text = text;
      this.error = error;
    }

    /** Returns whether the dialog resource contains female strings. */
    public boolean isFemaleDialog() {
      final String dlgName = Profile.getProperty(Profile.Key.GET_GLOBAL_DIALOG_NAME_FEMALE);
      return (dlgName.equalsIgnoreCase(resource.getResourceName()));
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      switch (columnIndex) {
        case 0:
          return resource;
        case 2:
          return text;
        case 3:
          return error;
        default:
          return strref;
      }
    }

    @Override
    public String toString() {
      return String.format("String table: %s, StringRef: %d /* %s */, Error message: %s", resource.getResourceName(),
          strref, REGEX_LINEBREAK.matcher(text).replaceAll(" "), error.toString());
    }
  }
}
