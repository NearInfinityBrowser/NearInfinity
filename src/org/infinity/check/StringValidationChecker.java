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
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderMalfunctionError;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

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
import org.infinity.util.io.StreamUtils;

public class StringValidationChecker extends AbstractSearcher
    implements Runnable, ActionListener, ListSelectionListener {
  private ChildFrame resultFrame;
  private SortableTable table;
  private JButton bSave;
  private JButton bOpenLookup;
  private JButton bOpenStringTable;

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
          if (entry != null) {
            files.add(entry);
          }
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

      JPanel pane = (JPanel) resultFrame.getContentPane();
      pane.setLayout(new BorderLayout(0, 3));
      pane.add(count, BorderLayout.PAGE_START);
      pane.add(scrollTable, BorderLayout.CENTER);
      pane.add(panel, BorderLayout.PAGE_END);
      bOpenLookup.setEnabled(false);
      bOpenStringTable.setEnabled(false);
      table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
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
        decoder.onUnmappableCharacter(CodingErrorAction.REPORT);
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
              synchronized (System.err) {
                e.printStackTrace();
              }
            }
          }
          decoder.reset();
        }
      } catch (Exception e) {
        synchronized (System.err) {
          e.printStackTrace();
        }
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
        synchronized (table) {
          final String text = StringTable.getStringRef(isFemale ? StringTable.Type.FEMALE : StringTable.Type.MALE,
              strref);
          final String infoBytes = (cr.length() == 1) ? " byte" : " bytes";
          final String info = "offset " + outBuf.position() + ", length: " + cr.length() + infoBytes;
          if (cr.isMalformed()) {
            table.addTableItem(new StringErrorTableItem(entry, strref, text, "Malformed input data found at " + info));
          }
          if (cr.isUnmappable()) {
            table.addTableItem(new StringErrorTableItem(entry, strref, text, "Unmappable character found at " + info));
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
          final String msg = "Possible encoding error found at offset " + ofs;
          table.addTableItem(new StringErrorTableItem(entry, strref, textAnsi, msg));
          isError = true;
        }
      }

      if (!isError && textAnsi.length() > textUtf8.length()) {
        final String msg = "Possible encoding error found at offset " + textUtf8.length();
        table.addTableItem(new StringErrorTableItem(entry, strref, textAnsi, msg));
      }
    }
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

  // -------------------------- INNER CLASSES --------------------------

  private static final class StringErrorTableItem implements TableItem {
    private static final Pattern REGEX_LINEBREAK = Pattern.compile("\r?\n");

    private final ResourceEntry resource;
    private final Integer strref;
    private final String text;
    private final String message;

    public StringErrorTableItem(ResourceEntry dlg, int strref, String text, String msg) {
      this.resource = dlg;
      this.strref = strref;
      this.text = text;
      this.message = msg;
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
        case 1:
          return strref;
        case 2:
          return text;
        case 3:
          return message;
        default:
          return strref;
      }
    }

    @Override
    public String toString() {
      return String.format("String table: %s, StringRef: %d /* %s */, Error message: %s", resource.getResourceName(),
          strref, REGEX_LINEBREAK.matcher(text).replaceAll(" "), message);
    }
  }
}
