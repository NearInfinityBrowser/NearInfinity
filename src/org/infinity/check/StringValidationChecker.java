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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
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
  private ChildFrame resultFrame;
  private SortableTable table;
  private JButton bSave;
  private JButton bOpenLookup;
  private JButton bOpenStringTable;
  private JButton bRepair;
  private SwingWorker<Couple<Integer, Integer>, Void> repairWorker;
  private ProgressMonitor repairProgress;

  public StringValidationChecker(Component parent) {
    super(CHECK_MULTI_TYPE_FORMAT, parent);
    new Thread(this).start();
  }

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
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

      table = new SortableTable(new String[] { "String table", "StringRef", "String", "Error message" },
          new Class<?>[] { ResourceEntry.class, Integer.class, String.class, String.class },
          new Integer[] { 100, 75, 500, 300 });

      if (runSearch("Checking strings", files)) {
        return;
      }

      if (table.getRowCount() == 0) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No string encoding errors found.", "Info",
            JOptionPane.INFORMATION_MESSAGE);
      } else {
        getResultFrame().setVisible(true);
      }

    } finally {
      blocker.setBlocked(false);
    }
  }

  // --------------------- End Interface Runnable ---------------------

  private ChildFrame getResultFrame() {
    if (resultFrame == null) {
      table.tableComplete();
      resultFrame = new ChildFrame("Result of string validation", true);
      resultFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());
      bOpenLookup = new JButton("Open in StringRef Lookup", Icons.ICON_OPEN_16.getIcon());
      bOpenLookup.setMnemonic('l');
      bOpenLookup.setToolTipText("Only available for male strings.");
      bOpenStringTable = new JButton("Open in String table", Icons.ICON_OPEN_16.getIcon());
      bOpenStringTable.setMnemonic('t');
      bSave = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
      bSave.setMnemonic('s');

      bRepair = new JButton("Repair", Icons.ICON_CHECK_16.getIcon());
      bRepair.setToolTipText("Replace or remove malformed characters.");
      bRepair.setMnemonic('r');

      JScrollPane scrollTable = new JScrollPane(table);
      scrollTable.getViewport().setBackground(table.getBackground());
      resultFrame.getRootPane().setDefaultButton(bOpenLookup);

      // counting individual strings
      HashSet<String> errorMap = new HashSet<>();
      for (int row = 0, rowCount = table.getRowCount(); row < rowCount; row++) {
        final StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
        errorMap.add(item.resource.toString() + item.strref);
      }
      JLabel count = new JLabel(table.getRowCount() + " error(s) found in " + errorMap.size() + " string(s)",
          SwingConstants.CENTER);
      count.setFont(count.getFont().deriveFont(count.getFont().getSize() + 2.0f));

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      panel.add(bOpenLookup);
      panel.add(bOpenStringTable);
      panel.add(bSave);
      panel.add(bRepair);

      JPanel pane = (JPanel) resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.PAGE_START);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.PAGE_END);
      bOpenLookup.setEnabled(false);
      bOpenStringTable.setEnabled(false);
      table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
      table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);
      table.getSelectionModel().addListSelectionListener(this);
      final MouseListener listener = new MouseAdapter() {
        @Override
        public void mouseReleased(MouseEvent event) {
          if (event.getClickCount() == 2) {
            SortableTable table = (SortableTable) event.getSource();
            int row = table.getSelectedRow();
            if (row != -1) {
              StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
              if (event.isAltDown()) {
                final StringTable.Type type = item.isFemaleDialog() ? StringTable.Type.FEMALE : StringTable.Type.MALE;
                ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(type, item.strref);
              } else if (!item.isFemaleDialog()) {
                ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
              }
            }
          }
        }
      };
      table.addMouseListener(listener);
      bOpenLookup.addActionListener(this);
      bOpenStringTable.addActionListener(this);
      bSave.addActionListener(this);
      bRepair.addActionListener(this);
      pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
      resultFrame.setSize(1024, 576);
      Center.center(resultFrame, NearInfinity.getInstance().getBounds());
    }

    return resultFrame;
  }

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

    inBuf.flip();
    outBuf.limit(outBuf.capacity());
    outBuf.position(0);
    final CharsetDecoder decoderUtf8 = StandardCharsets.UTF_8.newDecoder();
    final CoderResult cr = decoderUtf8.decode(inBuf, outBuf, true);
    if (!cr.isError()) {
      outBuf.flip();
      final String textUtf8 = outBuf.toString();

      boolean isError = false;
      for (int ofs = 0, len1 = textAnsi.length(), len2 = textUtf8.length(); ofs < len1 && ofs < len2
          && !isError; ofs++) {
        final char ch1 = textAnsi.charAt(ofs);
        final char ch2 = textUtf8.charAt(ofs);
        if (ch1 != ch2) {
          synchronized(this) {
            final byte[] buffer = Arrays.copyOf(inBuf.array(), inBuf.limit());
            table.addTableItem(new StringErrorTableItem(entry, strref, textAnsi,
                new StringError(isFemale, strref, buffer, ofs, 1, "encoding error")));
            isError = true;
          }
        }
      }

      if (!isError && textAnsi.length() > textUtf8.length()) {
        synchronized (this) {
          final byte[] buffer = Arrays.copyOf(inBuf.array(), inBuf.limit());
          table.addTableItem(new StringErrorTableItem(entry, strref, textAnsi,
              new StringError(isFemale, strref, buffer, textUtf8.length(), 1, "encoding error")));
        }
      }
    }
  }

  /** Executes the repair operation as a background task with UI feedback. */
  private void repairEntriesBackground() {
    final String note = (table.getModel().getRowCount() >= 1000) ? "This may take a while..." : null;
    repairProgress = new ProgressMonitor(getResultFrame(), "Repairing game strings", note, 0, table.getModel().getRowCount());
    repairProgress.setMillisToDecideToPopup(100);
    repairProgress.setMillisToPopup(250);

    repairWorker = new SwingWorker<Couple<Integer, Integer>, Void>() {
      @Override
      protected Couple<Integer, Integer> doInBackground() throws Exception {
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

      retVal = new Couple<>(numRemoved, numRemoved);
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

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == bOpenLookup) {
      final int row = table.getSelectedRow();
      if (row >= 0) {
        final StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
        ChildFrame.show(StringLookup.class, StringLookup::new).hitFound(item.strref);
      }
    } else if (e.getSource() == bOpenStringTable) {
      final int row = table.getSelectedRow();
      if (row >= 0) {
        final StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
        final StringTable.Type type = item.isFemaleDialog() ? StringTable.Type.FEMALE : StringTable.Type.MALE;
        ChildFrame.show(StringEditor.class, StringEditor::new).showEntry(type, item.strref);
      }
    } else if (e.getSource() == bSave) {
      table.saveCheckResult(resultFrame, "Encoding errors in game strings");
    } else if (e.getSource() == bRepair) {
      // Check for modified string table
      if (StringTable.isModified()) {
        final int result = JOptionPane.showConfirmDialog(getResultFrame(),
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
      final String msg = (count > 1) ? "Repair all " + count + " entries?" : "Repair " + count + " entry?";
      final int result = JOptionPane.showConfirmDialog(getResultFrame(), msg, "Question", JOptionPane.YES_NO_OPTION,
          JOptionPane.QUESTION_MESSAGE);
      if (result == JOptionPane.YES_OPTION) {
        repairEntriesBackground();
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e) {
    final int row = table.getSelectedRow();
    if (row >= 0) {
      StringErrorTableItem item = (StringErrorTableItem) table.getTableItemAt(row);
      bOpenLookup.setEnabled(!item.isFemaleDialog());
    } else {
      bOpenLookup.setEnabled(false);
    }
    bOpenStringTable.setEnabled(row >= 0);
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
            JOptionPane.showMessageDialog(getResultFrame(),
                "Repair operation completed.\nReplaced characters: " + numReplaced + "\nRemoved characters: " + numRemoved,
                "Information", JOptionPane.INFORMATION_MESSAGE);
          } else {
            if (isCancelled) {
              JOptionPane.showMessageDialog(getResultFrame(),
                  "Repair operation was cancelled.\nString table may contain unsaved changes.", "Information",
                  JOptionPane.INFORMATION_MESSAGE);
            } else {
              StringTable.resetAll();
              JOptionPane.showMessageDialog(getResultFrame(),
                  "Repair operation failed.\nString table may contain unsaved changes.", "Error",
                  JOptionPane.ERROR_MESSAGE);
            }
          }
          repairProgress = null;
          repairWorker = null;

          getResultFrame().close();
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
     * @throws IllegalArgumentException
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
      String retVal = "";
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
            retVal = outBuf.flip().toString();
            break;
          }
        }
      } else {
        // Fixing ANSI/multi-byte charset
        final CharsetDecoder csd = StandardCharsets.UTF_8.newDecoder();
        csd.onMalformedInput(CodingErrorAction.REPORT);
        csd.onUnmappableCharacter(CodingErrorAction.REPORT);
        final int maxCharLength = (int) Math.ceil(csd.maxCharsPerByte() * data.length);
        final CharBuffer outBuf = CharBuffer.allocate(maxCharLength);
        final CoderResult cr = csd.decode(ByteBuffer.wrap(data), outBuf, true);
        if (!cr.isError() && !cr.isUnderflow()) {
          retVal = outBuf.flip().toString();
        }
      }

      return retVal;
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
