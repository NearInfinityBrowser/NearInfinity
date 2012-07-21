// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.datatype.*;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.search.*;
import infinity.util.*;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public final class StringEditor extends ChildFrame implements ActionListener, ListSelectionListener, SearchClient,
                                                              ChangeListener, ItemListener
{
  private static final String s_msgtype[] = {"No message data", "", "Ambient message", "Standard message", "", "",
                                             "", "Message with tags"};
  private static final String s_msgtypeNWN[] = {"No message data", "Text present", "Sound present",
                                                "Sound length present" };
  private static String signature, version;
  private static int entry_size = 26; // V1
  private final ButtonPopupMenu bfind;
  private final CardLayout cards = new CardLayout();
  private final File stringfile;
  private final JButton badd = new JButton("Add", Icons.getIcon("Add16.gif"));
  private final JButton bdelete = new JButton("Delete", Icons.getIcon("Remove16.gif"));
  private final JButton breread = new JButton("Revert", Icons.getIcon("Undo16.gif"));
  private final JButton bsave = new JButton("Save", Icons.getIcon("Save16.gif"));
  private final JButton bexport = new JButton("Export as TXT...", Icons.getIcon("Export16.gif"));
  private final JMenuItem ifindattribute = new JMenuItem("selected attribute");
  private final JMenuItem ifindstring = new JMenuItem("string");
  private final JMenuItem ifindref = new JMenuItem("references to this entry");
  private final JPanel editpanel = new JPanel();
  private final JPanel editcontent = new JPanel();
  private final JSlider slider = new JSlider(0, 100, 0);
  private final JTable table = new JTable();
  private final JTextArea tatext = new JTextArea();
  private final JTextField tstrref = new JTextField(5);
  private final StringEditor editor;
  private final java.util.List<StringEntry> added_entries = new ArrayList<StringEntry>();
  private DecNumber entries_count, entries_offset;
  private Editable editable;
  private StringEntry entries[];
  private Unknown unknown;
  private int index_shown = -1, init_show;

  public StringEditor(File stringfile, int init_show)
  {
    super("Edit: " + stringfile);
    setIconImage(Icons.getIcon("Edit16.gif").getImage());
    this.stringfile = stringfile;
    if (init_show >= 0)
      this.init_show = init_show;
    StringResource.close();

    JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Make sure you have a backup of " +
                                                              stringfile.getName(),
                                  "Warning", JOptionPane.WARNING_MESSAGE);

    tstrref.addActionListener(this);
    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(this);
    table.setFont(BrowserMenuBar.getInstance().getScriptFont());
    tatext.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    tatext.setLineWrap(true);
    tatext.setWrapStyleWord(true);
    ifindattribute.setEnabled(false);
    breread.setToolTipText("Undo all changes");
    bfind = new ButtonPopupMenu("Find...", new JMenuItem[]{ifindattribute, ifindstring, ifindref});
    bfind.setIcon(Icons.getIcon("Find16.gif"));
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
    textPanel.add(new JScrollPane(tatext));

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

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == tstrref) {
      try {
        int i = Integer.parseInt(tstrref.getText().trim());
        if (i >= 0 && i < entries_count.getValue())
          showEntry(i);
        else
          JOptionPane.showMessageDialog(this, "Entry not found", "Error", JOptionPane.ERROR_MESSAGE);
      } catch (NumberFormatException e) {
        JOptionPane.showMessageDialog(this, "Not a number", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    else if (event.getSource() == bsave) {
      if (index_shown != -1)
        updateEntry(index_shown);
      new Thread(new StrRefWriter()).start();
    }
    else if (event.getSource() == breread) {
      setVisible(false);
      new Thread(new StrRefReader()).start();
    }
    else if (event.getActionCommand().equals(StructViewer.UPDATE_VALUE)) {
      if (!editable.updateValue(null))
        JOptionPane.showMessageDialog(this, "Error updating value", "Error", JOptionPane.ERROR_MESSAGE);
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
      if (index_shown == entries_count.getValue() - 1)
        deleteLastEntry();
      else
        JOptionPane.showMessageDialog(this, "You can only delete the last entry",
                                      "Error", JOptionPane.ERROR_MESSAGE);
    }
    else if (event.getSource() == bexport)
      new Thread(new StrRefExporter()).start();
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == slider) {
      if (!slider.getValueIsAdjusting() && slider.getValue() != index_shown)
        showEntry(slider.getValue());
    }
  }

// --------------------- End Interface ChangeListener ---------------------


// --------------------- Begin Interface ItemListener ---------------------

  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == bfind) {
      //      JMenuItem item = (JMenuItem)event.getItem();  // Should have worked!
      JMenuItem item = bfind.getSelectedItem();
      if (item == ifindstring)
        SearchMaster.createAsFrame(this, "StringRef", this);
      else if (item == ifindattribute)
        SearchMaster.createAsFrame(new AttributeSearcher(table.getSelectedRow()),
                                   entries[0].getValueAt(table.getSelectedRow(), 0).toString(), this);
      else if (item == ifindref)
        new StringReferenceSearcher(index_shown, this);
    }
  }

// --------------------- End Interface ItemListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

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
      else if (selected instanceof InlineEditable)
        cards.show(editpanel, "Empty");
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface SearchClient ---------------------

  public String getText(int index)
  {
    if (index < 0 || index >= entries_count.getValue())
      return null;
    if (index < entries.length)
      return entries[index].string;
    StringEntry entry = added_entries.get(index - entries.length);
    return entry.string;
  }

  public void hitFound(int index)
  {
    showEntry(index);
  }

// --------------------- End Interface SearchClient ---------------------

  public File getFile()
  {
    return stringfile;
  }

  public void showEntry(int index)
  {
    if (index < 0)
      return;
    if (index_shown != -1)
      updateEntry(index_shown);
    StringEntry entry;
    if (index < entries.length)
      entry = entries[index];
    else
      entry = added_entries.get(index - entries.length);
    entry.fillList();
    tstrref.setText(String.valueOf(index));
    index_shown = index;
    slider.setValue(index);
    table.setModel(entry);
    if (table.getColumnCount() == 3)
      table.getColumnModel().getColumn(2).setPreferredWidth(6);
    tatext.setText(entry.string);
    tatext.setCaretPosition(0);
    cards.show(editpanel, "Empty");
    table.repaint();
    editable = null;
    init_show = 0;
  }

  private int addEntry(StringEntry entry)
  {
    if (entries_count.getValue() < entries.length)
      entries[entries_count.getValue()] = entry;
    else
      added_entries.add(entry);
    entries_count.incValue(1);
    slider.setMaximum(entries_count.getValue() - 1);
    entries_offset.incValue(entry_size);
    return entries_count.getValue() - 1;
  }

  private void deleteLastEntry()
  {
    if (added_entries.size() > 0)
      added_entries.remove(added_entries.size() - 1);
    else
      entries[entries_count.getValue() - 1] = null;
    entries_count.incValue(-1);
    index_shown = -1;
    slider.setMaximum(entries_count.getValue() - 1);
    entries_offset.incValue(-entry_size);
    showEntry(entries_count.getValue() - 1);
  }

  private void updateEntry(int index)
  {
    if (index < entries.length)
      entries[index].setString(tatext.getText());
    else {
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

    public void run()
    {
      Charset charset = StringResource.getCharset();
      ProgressMonitor progress = null;
      try {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(stringfile));
        signature = Filereader.readString(bis, 4);
        version = Filereader.readString(bis, 4);
        if (version.equals("V1  "))
          unknown = new Unknown(Filereader.readBytes(bis, 2), 0, 2);
        else if (version.equals("V3.0")) {
          unknown = new Unknown(Filereader.readBytes(bis, 4), 0, 4); // LanguageID
          entry_size = 40;
        }
        else {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Unsupported version: " + version,
                                        "Error", JOptionPane.ERROR_MESSAGE);
          throw new IOException();
        }
        entries_count = new DecNumber(Filereader.readBytes(bis, 4), 0, 4, "# entries");
        entries_offset = new DecNumber(Filereader.readBytes(bis, 4), 0, 4, "Entries offset");

        entries = new StringEntry[entries_count.getValue()];
        progress = new ProgressMonitor(NearInfinity.getInstance(), "Reading strings...", null,
                                       0, 2 * entries_count.getValue());
        progress.setMillisToDecideToPopup(100);
        for (int i = 0; i < entries_count.getValue(); i++) {
          entries[i] = new StringEntry(Filereader.readBytes(bis, entry_size));
          progress.setProgress(i + 1);
          if (progress.isCanceled()) {
            entries = null;
            bis.close();
            return;
          }
        }
        bis.close();

        RandomAccessFile ranfile = new RandomAccessFile(stringfile, "r");
        for (int i = 0; i < entries.length; i++) {
          entries[i].readString(ranfile, entries_offset.getValue(), charset);
          progress.setProgress(i + 1 + entries_count.getValue());
          if (progress.isCanceled()) {
            entries = null;
            bis.close();
            return;
          }
        }
        ranfile.close();
        slider.setMaximum(entries_count.getValue() - 1);
        slider.addChangeListener(editor);
        showEntry(init_show);
        setVisible(true);
      } catch (Exception e) {
        progress.close();
        e.printStackTrace();
        JOptionPane.showMessageDialog(editor, "Error reading " + stringfile.getName() + '\n' +
                                              e.getMessage(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
        entries = null;
      } catch (Error err) {
        progress.close();
        entries = null;
        err.printStackTrace();
        JOptionPane.showMessageDialog(editor, "Error reading " + stringfile.getName() + '\n' +
                                              err.toString(),
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

    public void run()
    {
      bexport.setEnabled(false);
      bsave.setEnabled(false);
      breread.setEnabled(false);
      badd.setEnabled(false);
      JFileChooser chooser = new JFileChooser(ResourceFactory.getRootDir());
      chooser.setDialogTitle("Export " + stringfile.getName());
      chooser.setSelectedFile(new File("dialog.txt"));
      int returnval = chooser.showSaveDialog(editor);
      if (returnval == JFileChooser.APPROVE_OPTION) {
        File output = chooser.getSelectedFile();
        if (output.exists()) {
          String options[] = {"Overwrite", "Cancel"};
          int result = JOptionPane.showOptionDialog(editor, output + " exists. Overwrite?",
                                                    "Save resource", JOptionPane.YES_NO_OPTION,
                                                    JOptionPane.WARNING_MESSAGE, null, options, options[0]);
          if (result == 1) {
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
          PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(output)));
          for (int i = 0; i < entries.length; i++) {
            if (entries[i] != null) {
              pw.println(i + ":");
              pw.println(entries[i].string);
              pw.println();
            }
            progress.setProgress(i + 1);
          }
          for (int i = 0; i < added_entries.size(); i++) {
            StringEntry entry = added_entries.get(i);
            pw.println(i + entries.length + ":");
            pw.println(entry.string);
            pw.println();
            progress.setProgress(entries.length + i + 1);
          }
          pw.close();
          JOptionPane.showMessageDialog(editor, "File exported to " + output,
                                        "Export complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(editor, "Error writing " + output.getName(),
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

    public void run()
    {
      Charset charset = StringResource.getCharset();
      bexport.setEnabled(false);
      bsave.setEnabled(false);
      breread.setEnabled(false);
      badd.setEnabled(false);
      try {
        if (stringfile.exists()) {
          String options[] = {"Overwrite", "Cancel"};
          int result = JOptionPane.showOptionDialog(editor, stringfile + " exists. Overwrite?",
                                                    "Save resource", JOptionPane.YES_NO_OPTION,
                                                    JOptionPane.WARNING_MESSAGE, null,
                                                    options, options[0]);
          if (result == 1) {
            bexport.setEnabled(true);
            bsave.setEnabled(true);
            breread.setEnabled(true);
            badd.setEnabled(true);
            return;
          }
        }

        StringResource.close();
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(stringfile));
        Filewriter.writeString(bos, signature, 4);
        Filewriter.writeString(bos, version, 4);
        unknown.write(bos);
        entries_count.write(bos);
        entries_offset.write(bos);

        int offset = 0;
        for (final StringEntry entry : entries)
          if (entry != null)
            offset += entry.update(offset);
        for (int i = 0; i < added_entries.size(); i++)
          offset += added_entries.get(i).update(offset);

        ProgressMonitor progress = new ProgressMonitor(editor, "Writing file...", null,
                                                       0, 2 * entries_count.getValue());
        progress.setMillisToDecideToPopup(100);
        for (int i = 0; i < entries.length; i++) {
          if (entries[i] != null)
            entries[i].write(bos);
          progress.setProgress(i + 1);
        }
        for (int i = 0; i < added_entries.size(); i++) {
          added_entries.get(i).write(bos);
          progress.setProgress(entries.length + i + 1);
        }

        for (int i = 0; i < entries.length; i++) {
          if (entries[i] != null)
            entries[i].writeString(bos, charset);
          progress.setProgress(i + 1 + entries_count.getValue());
        }
        for (int i = 0; i < added_entries.size(); i++) {
          added_entries.get(i).writeString(bos, charset);
          progress.setProgress(entries_count.getValue() + entries.length + i + 1);
        }
        bos.close();

        JOptionPane.showMessageDialog(editor, "File written successfully",
                                      "Save complete", JOptionPane.INFORMATION_MESSAGE);
        bsave.setEnabled(true);
        breread.setEnabled(true);
        bexport.setEnabled(true);
        badd.setEnabled(true);
      } catch (IOException e) {
        JOptionPane.showMessageDialog(editor, "Error writing " + stringfile.getName(),
                                      "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  // StringEntry ///////////////////////////////////////

  private static final class StringEntry extends AbstractStruct
  {
    private int doffset, dlength;
    private String string = "";
    private byte data[];

    private StringEntry() throws Exception
    {
      super(null, null, new byte[entry_size], 0);
    }

    StringEntry(byte buffer[]) throws Exception
    {
      super(null, null, buffer, 0);
    }

    protected int read(byte buffer[], int offset) throws Exception
    {
      if (version.equals("V1  ")) {
        data = ArrayUtil.getSubArray(buffer, offset, 18);
        doffset = Byteconvert.convertInt(buffer, offset + 18);
        dlength = Byteconvert.convertInt(buffer, offset + 22);
      }
      else if (version.equals("V3.0")) {
        data = ArrayUtil.getSubArray(buffer, offset, 40);
        doffset = Byteconvert.convertInt(buffer, offset + 28);
        dlength = Byteconvert.convertInt(buffer, offset + 32);
      }
      return offset + entry_size;
    }

    public void fillList()
    {
      try {
        if (getRowCount() == 0) {
          if (version.equals("V1  ")) {
            list.add(new Bitmap(data, 0, 2, "Entry type", s_msgtype));
            list.add(new ResourceRef(data, 2, "Associated sound", "WAV"));
            list.add(new DecNumber(data, 10, 4, "Volume variance"));
            list.add(new DecNumber(data, 14, 4, "Pitch variance"));
          }
          else if (version.equals("V3.0")) { // Remember to updateValue writeField() as this changes
            list.add(new Flag(data, 0, 4, "Entry type", s_msgtypeNWN));
            list.add(new TextString(data, 4, 16, "Associated sound"));
            list.add(new DecNumber(data, 20, 4, "Volume variance"));
            list.add(new DecNumber(data, 24, 4, "Pitch variance"));
            list.add(new Unknown(data, 36, 4, "Sound length"));
          }
          data = null;
        }
      } catch (Exception e) {
        data = null;
      }
    }

    public void readString(RandomAccessFile ranfile, int baseoffset, Charset charset) throws IOException
    {
      ranfile.seek((long)(baseoffset + doffset));
      string = Filereader.readString(ranfile, dlength, charset);
    }

    public int update(int newoffset)
    {
      doffset = newoffset;
      dlength = string.length();
      return dlength;
    }

    public void write(OutputStream os) throws IOException
    {
      // Update must be called first
      if (version.equals("V1  ")) {
        if (getRowCount() == 0)
          os.write(data);
        else
          super.write(os);
        Filewriter.writeInt(os, doffset);
        Filewriter.writeInt(os, dlength);
      }
      else if (version.equals("V3.0")) {
        if (getRowCount() == 0) {
          os.write(data, 0, 28);
          Filewriter.writeInt(os, doffset);
          Filewriter.writeInt(os, dlength);
          os.write(data, 36, 4);
        }
        else {
          getStructEntryAt(0).write(os);
          getStructEntryAt(1).write(os);
          getStructEntryAt(2).write(os);
          Filewriter.writeInt(os, doffset);
          Filewriter.writeInt(os, dlength);
          getStructEntryAt(3).write(os);
        }
      }
    }

    public void writeString(OutputStream os, Charset charset) throws IOException
    {
      Filewriter.writeString(os, string, dlength, charset);
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
      return entry.getValueAt(selectedrow, 1).toString();
    }

    public void hitFound(int index)
    {
      showEntry(index);
    }
  }
}

