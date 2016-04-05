// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Editable;
import org.infinity.datatype.Flag;
import org.infinity.datatype.InlineEditable;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.Unknown;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.search.SearchClient;
import org.infinity.search.SearchMaster;
import org.infinity.search.StringReferenceSearcher;
import org.infinity.util.StringResource;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

public final class StringEditor extends ChildFrame implements ActionListener, ListSelectionListener, SearchClient,
                                                              ChangeListener, ItemListener
{
  private static final String s_flags[] = { "None", "Has text", "Has sound", "Has token" };
  private static String signature, version;
  private static int entry_size = 26; // V1
  private final ButtonPopupMenu bfind;
  private final CardLayout cards = new CardLayout();
  private final Path stringPath;
  private final JButton badd = new JButton("Add", Icons.getIcon(Icons.ICON_ADD_16));
  private final JButton bdelete = new JButton("Delete", Icons.getIcon(Icons.ICON_REMOVE_16));
  private final JButton breread = new JButton("Revert", Icons.getIcon(Icons.ICON_UNDO_16));
  private final JButton bsave = new JButton("Save", Icons.getIcon(Icons.ICON_SAVE_16));
  private final JButton bexport = new JButton("Export as TXT...", Icons.getIcon(Icons.ICON_EXPORT_16));
  private final JMenuItem ifindattribute = new JMenuItem("selected attribute");
  private final JMenuItem ifindstring = new JMenuItem("string");
  private final JMenuItem ifindref = new JMenuItem("references to this entry");
  private final JPanel editpanel = new JPanel();
  private final JPanel editcontent = new JPanel();
  private final JSlider slider = new JSlider(0, 100, 0);
  private final JTable table = new JTable();
  private final RSyntaxTextArea tatext = new InfinityTextArea(true);
  private final JTextField tstrref = new JTextField(5);
  private final StringEditor editor;
  private final java.util.List<StringEntry> added_entries = new ArrayList<StringEntry>();
  private DecNumber entries_count, entries_offset;
  private Editable editable;
  private StringEntry entries[];
  private Unknown unknown;
  private int index_shown = -1, init_show;

  public StringEditor(Path stringPath, int init_show)
  {
    super("Edit: " + stringPath);
    setIconImage(Icons.getIcon(Icons.ICON_EDIT_16).getImage());
    this.stringPath = stringPath;
    if (init_show >= 0) {
      this.init_show = init_show;
    }
    StringResource.close();

    JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                  "Make sure you have a backup of " + stringPath.getFileName(),
                                  "Warning", JOptionPane.WARNING_MESSAGE);

    tstrref.addActionListener(this);
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    table.setFont(BrowserMenuBar.getInstance().getScriptFont());
    tatext.setMargin(new Insets(3, 3, 3, 3));
    tatext.setLineWrap(true);
    tatext.setWrapStyleWord(true);
    ifindattribute.setEnabled(false);
    breread.setToolTipText("Undo all changes");
    bfind = new ButtonPopupMenu("Find...", new JMenuItem[]{ifindattribute, ifindstring, ifindref});
    bfind.setIcon(Icons.getIcon(Icons.ICON_FIND_16));
    badd.setMnemonic('a');
    bdelete.setMnemonic('d');
    bsave.setMnemonic('s');
    breread.setMnemonic('r');
    bexport.setMnemonic('e');
    badd.addActionListener(this);
    bdelete.addActionListener(this);
    bsave.addActionListener(this);
    breread.addActionListener(this);
    bfind.addItemListener(this);
    bexport.addActionListener(this);
    slider.setMajorTickSpacing(10000);
    slider.setMinorTickSpacing(1000);
    slider.setPaintTicks(true);
    editpanel.setLayout(cards);
    editpanel.add(new JPanel(), "Empty");
    editpanel.add(editcontent, "Edit");
    editpanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
    cards.show(editpanel, "Empty");

    // Construct GUI
    JLabel label = new JLabel("StrRef: ");
    label.setLabelFor(tstrref);
    label.setFont(label.getFont().deriveFont((float)label.getFont().getSize() + 2.0f));
    JPanel topleftPanel = new JPanel(new FlowLayout());
    topleftPanel.add(label);
    topleftPanel.add(tstrref);

    JPanel topPanel = new JPanel(new BorderLayout());
    topPanel.add(topleftPanel, BorderLayout.WEST);
    topPanel.add(slider, BorderLayout.CENTER);

    JPanel centerleft = new JPanel(new BorderLayout(0, 6));
    centerleft.add(table, BorderLayout.NORTH);
    centerleft.add(editpanel, BorderLayout.CENTER);
    centerleft.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));

    JPanel attributePanel = new JPanel(new BorderLayout());
    attributePanel.add(new JLabel("Attributes:"), BorderLayout.NORTH);
    attributePanel.add(centerleft, BorderLayout.CENTER);

    JPanel textPanel = new JPanel(new BorderLayout());
    textPanel.add(new JLabel("String:"), BorderLayout.NORTH);
    textPanel.add(new InfinityScrollPane(tatext, true), BorderLayout.CENTER);

    JPanel centerPanel = new JPanel(new GridLayout(1, 3, 6, 0));
    centerPanel.add(attributePanel);
    centerPanel.add(textPanel);

    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(badd);
    buttonPanel.add(bdelete);
    buttonPanel.add(bfind);
    buttonPanel.add(bexport);
    buttonPanel.add(breread);
    buttonPanel.add(bsave);

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout(3, 3));

    pane.add(topPanel, BorderLayout.NORTH);
    pane.add(centerPanel, BorderLayout.CENTER);
    pane.add(buttonPanel, BorderLayout.SOUTH);
    pane.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));

    setSize(750, 500);
    Center.center(this, NearInfinity.getInstance().getBounds());

    editor = this;
    new Thread(new StrRefReader()).start();
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == tstrref) {
      try {
        int i = Integer.parseInt(tstrref.getText().trim());
        if (i >= 0 && i < entries_count.getValue()) {
          showEntry(i);
        } else {
          JOptionPane.showMessageDialog(this, "Entry not found", "Error", JOptionPane.ERROR_MESSAGE);
        }
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Not a number", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    else if (event.getSource() == bsave) {
      if (index_shown != -1) {
        updateEntry(index_shown);
      }
      new Thread(new StrRefWriter()).start();
    }
    else if (event.getSource() == breread) {
      setVisible(false);
      new Thread(new StrRefReader()).start();
    }
    else if (event.getActionCommand().equals(StructViewer.UPDATE_VALUE)) {
      if (!editable.updateValue(null)) {
        JOptionPane.showMessageDialog(this, "Error updating value", "Error", JOptionPane.ERROR_MESSAGE);
      }
      table.repaint();
    }
    else if (event.getSource() == badd) {
      try {
        showEntry(addEntry(new StringEntry()));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    else if (event.getSource() == bdelete) {
      if (index_shown == entries_count.getValue() - 1) {
        deleteLastEntry();
      } else {
        JOptionPane.showMessageDialog(this, "You can only delete the last entry",
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    else if (event.getSource() == bexport) {
      new Thread(new StrRefExporter()).start();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == slider) {
      if (!slider.getValueIsAdjusting() && slider.getValue() != index_shown) {
        showEntry(slider.getValue());
      }
    }
  }

// --------------------- End Interface ChangeListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == bfind) {
      //      JMenuItem item = (JMenuItem)event.getItem();  // Should have worked!
      JMenuItem item = bfind.getSelectedItem();
      if (item == ifindstring) {
        SearchMaster.createAsFrame(this, "StringRef", this);
      }
      else if (item == ifindattribute) {
        SearchMaster.createAsFrame(new AttributeSearcher(table.getSelectedRow()),
                                   entries[0].getValueAt(table.getSelectedRow(), 0).toString(), this);
      }
      else if (item == ifindref) {
        new StringReferenceSearcher(index_shown, this);
      }
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getValueIsAdjusting()) return;
    ListSelectionModel lsm = (ListSelectionModel)event.getSource();
    ifindattribute.setEnabled(!lsm.isSelectionEmpty());
    if (lsm.isSelectionEmpty()) {
      tatext.setText("");
      cards.show(editpanel, "Empty");
    }
    else {
      Object selected = table.getModel().getValueAt(lsm.getMinSelectionIndex(), 1);
      if (selected instanceof Editable) {
        editable = (Editable)selected;
        editcontent.removeAll();
        editcontent.setLayout(new BorderLayout());
        editcontent.add(editable.edit(this), BorderLayout.CENTER);
        editcontent.revalidate();
        editcontent.repaint();
        cards.show(editpanel, "Edit");
        editable.select();
      }
      else if (selected instanceof InlineEditable) {
        cards.show(editpanel, "Empty");
      }
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface SearchClient ---------------------

  @Override
  public String getText(int index)
  {
    if (index < 0 || index >= entries_count.getValue()) {
      return null;
    }
    if (index < entries.length) {
      return entries[index].string;
    }
    StringEntry entry = added_entries.get(index - entries.length);
    return entry.string;
  }

  @Override
  public void hitFound(int index)
  {
    showEntry(index);
  }

// --------------------- End Interface SearchClient ---------------------

  public Path getPath()
  {
    return stringPath;
  }

  public void showEntry(int index)
  {
    if (index < 0) {
      return;
    }
    if (index_shown != -1) {
      updateEntry(index_shown);
    }
    StringEntry entry;
    if (index < entries.length) {
      entry = entries[index];
    } else {
      entry = added_entries.get(index - entries.length);
    }
    entry.fillList();
    tstrref.setText(String.valueOf(index));
    index_shown = index;
    slider.setValue(index);
    table.setModel(entry);
    if (table.getColumnCount() == 3) {
      table.getColumnModel().getColumn(2).setPreferredWidth(6);
    }
    tatext.setText(entry.string);
    tatext.setCaretPosition(0);
    cards.show(editpanel, "Empty");
    table.repaint();
    editable = null;
    init_show = 0;
  }

  private int addEntry(StringEntry entry)
  {
    if (entries_count.getValue() < entries.length) {
      entries[entries_count.getValue()] = entry;
    } else {
      added_entries.add(entry);
    }
    entries_count.incValue(1);
    slider.setMaximum(entries_count.getValue() - 1);
    entries_offset.incValue(entry_size);
    return entries_count.getValue() - 1;
  }

  private void deleteLastEntry()
  {
    if (added_entries.size() > 0) {
      added_entries.remove(added_entries.size() - 1);
    } else {
      entries[entries_count.getValue() - 1] = null;
    }
    entries_count.incValue(-1);
    index_shown = -1;
    slider.setMaximum(entries_count.getValue() - 1);
    entries_offset.incValue(-entry_size);
    showEntry(entries_count.getValue() - 1);
  }

  private void updateEntry(int index)
  {
    if (index < entries.length) {
      entries[index].setString(tatext.getText());
    } else {
      StringEntry entry = added_entries.get(index - entries.length);
      entry.setString(tatext.getText());
    }
  }

// -------------------------- INNER CLASSES --------------------------

  // StrRefReader ///////////////////////////////////////
  private final class StrRefReader implements Runnable
  {
    private StrRefReader()
    {
    }

    @Override
    public void run()
    {
      Charset charset = StringResource.getCharset();
      ProgressMonitor progress = null;
      try (InputStream is = StreamUtils.getInputStream(stringPath)) {
        signature = StreamUtils.readString(is, 4);
        version = StreamUtils.readString(is, 4);
        if (version.equals("V1  ")) {
          unknown = new Unknown(StreamUtils.readBytes(is, 2), 0, 2);
        }
        else {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Unsupported version: " + version,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          throw new IOException();
        }
        entries_count = new DecNumber(StreamUtils.readBytes(is, 4), 0, 4, "# entries");
        entries_offset = new DecNumber(StreamUtils.readBytes(is, 4), 0, 4, "Entries offset");

        entries = new StringEntry[entries_count.getValue()];
        progress = new ProgressMonitor(NearInfinity.getInstance(), "Reading strings...", null,
                                       0, 2 * entries_count.getValue());
        progress.setMillisToDecideToPopup(100);
        for (int i = 0; i < entries_count.getValue(); i++) {
          entries[i] = new StringEntry(StreamUtils.readBytes(is, entry_size), charset);
          progress.setProgress(i + 1);
          if (progress.isCanceled()) {
            entries = null;
            return;
          }
        }
      } catch (Throwable e) {
        entries = null;
        e.printStackTrace();
        JOptionPane.showMessageDialog(editor,
                                      "Error reading " + stringPath.getFileName() + '\n' + e.toString(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
        return;
      }

      try (SeekableByteChannel ch = Files.newByteChannel(stringPath)) {
        ByteBuffer buffer = StreamUtils.getByteBuffer((int)ch.size());
        if (ch.read(buffer) < ch.size()) {
          throw new IOException();
        }
        buffer.position(0);
        for (int i = 0; i < entries.length; i++) {
          entries[i].readString(buffer, entries_offset.getValue());
          progress.setProgress(i + 1 + entries_count.getValue());
          if (progress.isCanceled()) {
            entries = null;
            return;
          }
        }
        slider.setMaximum(entries_count.getValue() - 1);
        slider.addChangeListener(editor);
        showEntry(init_show);
        setVisible(true);
      } catch (Throwable t) {
        progress.close();
        entries = null;
        t.printStackTrace();
        JOptionPane.showMessageDialog(editor,
                                      "Error reading " + stringPath.getFileName() + '\n' + t.getMessage(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  // StrRefExporter ///////////////////////////////////////

  private final class StrRefExporter implements Runnable
  {
    private StrRefExporter()
    {
    }

    @Override
    public void run()
    {
      bexport.setEnabled(false);
      bsave.setEnabled(false);
      breread.setEnabled(false);
      badd.setEnabled(false);
      JFileChooser chooser = new JFileChooser(Profile.getGameRoot().toFile());
      chooser.setDialogTitle("Export " + stringPath.getFileName());
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "dialog.txt"));
      int returnval = chooser.showSaveDialog(editor);
      if (returnval == JFileChooser.APPROVE_OPTION) {
        Path output = chooser.getSelectedFile().toPath();
        if (Files.exists(output)) {
          String options[] = {"Overwrite", "Cancel"};
          int result = JOptionPane.showOptionDialog(editor, output + " exists. Overwrite?",
                                                    "Save resource", JOptionPane.YES_NO_OPTION,
                                                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
          if (result == 1 || result == JOptionPane.CLOSED_OPTION) {
            bexport.setEnabled(true);
            bsave.setEnabled(true);
            breread.setEnabled(true);
            badd.setEnabled(true);
            return;
          }
        }
        try {
          ProgressMonitor progress = new ProgressMonitor(editor, "Writing file...", null, 0,
                                                         entries_count.getValue());
          progress.setMillisToDecideToPopup(100);
          try (BufferedWriter writer = Files.newBufferedWriter(output, StringResource.getCharset())) {
            for (int i = 0; i < entries.length; i++) {
              if (entries[i] != null) {
                writer.write(i + ":"); writer.newLine();
                writer.write(entries[i].string); writer.newLine();
                writer.newLine();
              }
              progress.setProgress(i + 1);
            }
            for (int i = 0; i < added_entries.size(); i++) {
              StringEntry entry = added_entries.get(i);
              writer.write(i + entries.length + ":"); writer.newLine();
              writer.write(entry.string); writer.newLine();
              writer.newLine();
              progress.setProgress(entries.length + i + 1);
            }
          }
          JOptionPane.showMessageDialog(editor, "File exported to " + output,
                                        "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(editor, "Error writing " + output.getFileName(),
                                        "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
      bexport.setEnabled(true);
      bsave.setEnabled(true);
      breread.setEnabled(true);
      badd.setEnabled(true);
    }
  }

  // StrRefWriter ///////////////////////////////////////

  private final class StrRefWriter implements Runnable
  {
    private StrRefWriter()
    {
    }

    @Override
    public void run()
    {
      Path outFile = stringPath;

      // Saving into DLC is not supported
      if (!FileManager.isDefaultFileSystem(outFile)) {
        boolean cancel = true;
        String msg = "\"" + outFile.toString() + "\" is located within a write-protected archive." +
                     "\nDo you want to export it to another location instead?";
        int result = JOptionPane.showConfirmDialog(editor, msg, "Save resource",
                                                   JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
          outFile = Profile.getGameRoot().resolve(outFile.getFileName().toString());
          JFileChooser fc = new JFileChooser(outFile.getParent().toFile());
          fc.setSelectedFile(outFile.toFile());
          int ret = fc.showSaveDialog(editor);
          if (ret == JFileChooser.APPROVE_OPTION) {
            outFile = fc.getSelectedFile().toPath();
            cancel = false;
          }
        }
        if (cancel) {
          JOptionPane.showMessageDialog(editor, "Operation cancelled.", "Information",
                                        JOptionPane.INFORMATION_MESSAGE);
          return;
        }
      }

      bexport.setEnabled(false);
      bsave.setEnabled(false);
      breread.setEnabled(false);
      badd.setEnabled(false);
      if (Files.exists(outFile)) {
        String options[] = {"Overwrite", "Cancel"};
        int result = JOptionPane.showOptionDialog(editor, outFile + " exists. Overwrite?",
                                                  "Save resource", JOptionPane.YES_NO_OPTION,
                                                  JOptionPane.WARNING_MESSAGE, null,
                                                  options, options[0]);
        if (result == 1 || result == JOptionPane.CLOSED_OPTION) {
          bexport.setEnabled(true);
          bsave.setEnabled(true);
          breread.setEnabled(true);
          badd.setEnabled(true);
          return;
        }
      }

      StringResource.close();
      ProgressMonitor progress = null;
      try (OutputStream os = StreamUtils.getOutputStream(outFile, true)) {
        StreamUtils.writeString(os, signature, 4);
        StreamUtils.writeString(os, version, 4);
        unknown.write(os);
        entries_count.write(os);
        entries_offset.write(os);

        int offset = 0;
        for (final StringEntry entry : entries) {
          if (entry != null) {
            offset += entry.update(offset);
          }
        }
        for (int i = 0; i < added_entries.size(); i++) {
          offset += added_entries.get(i).update(offset);
        }

        progress = new ProgressMonitor(editor, "Writing file...", null, 0, 2 * entries_count.getValue());
        progress.setMillisToDecideToPopup(100);
        for (int i = 0; i < entries.length; i++) {
          if (entries[i] != null) {
            entries[i].write(os);
          }
          progress.setProgress(i + 1);
        }
        for (int i = 0; i < added_entries.size(); i++) {
          added_entries.get(i).write(os);
          progress.setProgress(entries.length + i + 1);
        }

        for (int i = 0; i < entries.length; i++) {
          if (entries[i] != null) {
            entries[i].writeString(os);
          }
          progress.setProgress(i + 1 + entries_count.getValue());
        }
        for (int i = 0; i < added_entries.size(); i++) {
          added_entries.get(i).writeString(os);
          progress.setProgress(entries_count.getValue() + entries.length + i + 1);
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(editor, "Error writing " + outFile.getFileName(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
        return;
      } finally {
        if (progress != null) {
          progress.close();
          progress = null;
        }
      }
      JOptionPane.showMessageDialog(editor, "File written successfully",
                                    "Save complete", JOptionPane.INFORMATION_MESSAGE);
      bsave.setEnabled(true);
      breread.setEnabled(true);
      bexport.setEnabled(true);
      badd.setEnabled(true);
    }
  }

  // StringEntry ///////////////////////////////////////

  private static final class StringEntry extends AbstractStruct
  {
    private int doffset, dlength;
    private String string = "";
    private ByteBuffer buffer;
    private Charset charset;

    private StringEntry() throws Exception
    {
      super(null, null, StreamUtils.getByteBuffer(entry_size), 0);
      this.charset = StringResource.getCharset();
    }

    StringEntry(ByteBuffer buffer, Charset charset) throws Exception
    {
      super(null, null, buffer, 0);
      this.charset = (charset != null) ? charset : StringResource.getCharset();
    }

    @Override
    public int read(ByteBuffer buffer, int offset) throws Exception
    {
      this.buffer = StreamUtils.getByteBuffer(18);
      StreamUtils.copyBytes(buffer, offset, this.buffer, 0, this.buffer.limit());
      doffset = buffer.getInt(offset + 0x12);
      dlength = buffer.getInt(offset + 0x16);
      return offset + entry_size;
    }

    public void fillList()
    {
      try {
        if (getFieldCount() == 0) {
          buffer.position(0);
          addField(new Flag(buffer, 0, 2, "Flags", s_flags));
          addField(new ResourceRef(buffer, 2, "Associated sound", "WAV"));
          addField(new DecNumber(buffer, 10, 4, "Volume variance"));
          addField(new DecNumber(buffer, 14, 4, "Pitch variance"));
          buffer = null;
        }
      } catch (Exception e) {
        buffer = null;
        e.printStackTrace();
      }
    }

    public void readString(ByteBuffer buffer, int baseoffset) throws IOException
    {
      string = StreamUtils.readString(buffer, baseoffset + doffset, dlength, charset);
    }

    public int update(int newoffset)
    {
      doffset = newoffset;
      dlength = string.getBytes(charset).length;
      return dlength;
    }

    @Override
    public void write(OutputStream os) throws IOException
    {
      // Update must be called first
      if (getFieldCount() == 0) {
        buffer.position(0);
        StreamUtils.writeBytes(os, buffer);
      } else {
        super.write(os);
      }
      StreamUtils.writeInt(os, doffset);
      StreamUtils.writeInt(os, dlength);
    }

    public void writeString(OutputStream os) throws IOException
    {
      StreamUtils.writeString(os, string, dlength, charset);
    }

    private void setString(String newstring)
    {
      string = newstring;
    }
  }

  // AttributeSearcher ///////////////////////////////////////

  private final class AttributeSearcher implements SearchClient
  {
    private final int selectedrow;

    private AttributeSearcher(int selectedrow)
    {
      this.selectedrow = selectedrow;
    }

    @Override
    public String getText(int index)
    {
      if (index < 0 || index >= entries_count.getValue())
        return null;
      StringEntry entry;
      if (index < entries.length)
        entry = entries[index];
      else
        entry = added_entries.get(index - entries.length);
      entry.fillList();
      return entry.getField(selectedrow).toString();
    }

    @Override
    public void hitFound(int index)
    {
      showEntry(index);
    }
  }
}

