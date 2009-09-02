// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.key.*;
import infinity.util.Filewriter;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

public final class BIFFEditor implements ActionListener, ListSelectionListener, Runnable
{
  public static final int BIFF = 0;
  public static final int BIF = 1;
  public static final int BIFC = 2;
  private static final String[] s_bifformat = {"BIFF", "BIF", "BIFC"};
  private static boolean firstrun = true;
  private final BIFFEditorTable biftable = new BIFFEditorTable();
  private final BIFFEditorTable overridetable = new BIFFEditorTable();
  private final List<BIFFResourceEntry> origbiflist = new ArrayList<BIFFResourceEntry>();
  private BIFFEntry bifentry;
  private ChildFrame editframe;

  private JButton bcancel, bsave, btobif, bfrombif;
  private JComboBox cbformat;
  private int format;

  public BIFFEditor()
  {
    if (firstrun)
      JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                    "Make sure you have a backup of " +
                                    ResourceFactory.getKeyfile().toString(),
                                    "Warning", JOptionPane.WARNING_MESSAGE);
    firstrun = false;
    new ChooseBIFFrame(this);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bcancel)
      editframe.close();
    else if (event.getSource() == bsave) {
      editframe.close();
      String s_format = (String)cbformat.getSelectedItem();
      for (int i = 0; i < s_bifformat.length; i++)
        if (s_format.equals(s_bifformat[i])) {
          format = i;
          break;
        }
      new Thread(this).start();
    }
    else if (event.getSource() == btobif) {
      bsave.setEnabled(true);
      Object selected[] = overridetable.getSelectedValues();
      for (final Object value : selected) {
        if (biftable.addTableLine(value))
          overridetable.removeTableLine(value);
      }
    }
    else if (event.getSource() == bfrombif) {
      bsave.setEnabled(true);
      Object selected[] = biftable.getSelectedValues();
      for (final Object value : selected) {
        if (overridetable.addTableLine(value))
          biftable.removeTableLine(value);
      }
    }
    else if (event.getSource() == cbformat)
      bsave.setEnabled(true);
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (!event.getValueIsAdjusting()) {
      bfrombif.setEnabled(biftable.getSelectedValues().length != 0);
      btobif.setEnabled(overridetable.getSelectedValues().length != 0);
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    BifSaveProgress progress = new BifSaveProgress();
    blocker.setBlocked(true);
    // 1: Delete old entries from keyfile
    for (int i = 0; i < origbiflist.size(); i++)
      ResourceFactory.getInstance().getResources().removeResourceEntry(origbiflist.get(i));
    progress.setProgress(1, true);

    try {
      // 2: Extract files from BIF (if applicable)
      List<ResourceEntry> overrideBif = overridetable.getValueList(BIFFEditorTable.TYPE_BIF);
      for (int i = 0; i < overrideBif.size(); i++) {
        ResourceEntry entry = overrideBif.get(i);
        File file = new File(ResourceFactory.getRootDir(),
                             ResourceFactory.OVERRIDEFOLDER + File.separatorChar + entry.toString());
        OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
        Filewriter.writeBytes(os, entry.getResourceData(true));
        os.close();
        FileResourceEntry fileEntry = new FileResourceEntry(file, true);
        ResourceFactory.getInstance().getResources().addResourceEntry(fileEntry, fileEntry.getTreeFolder());
      }
      progress.setProgress(2, true);
    } catch (Exception e) {
      progress.setProgress(2, false);
      JOptionPane.showMessageDialog(editframe, "Error while extracting files from " + bifentry,
                                    "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      blocker.setBlocked(false);
      return;
    }

    // 3: Write new BIF
    BIFFWriter biffwriter = new BIFFWriter(bifentry, format);
    List<ResourceEntry> bifBif = biftable.getValueList(BIFFEditorTable.TYPE_BIF);
    for (int i = 0; i < bifBif.size(); i++)
      biffwriter.addResource(bifBif.get(i), true); // Ignore overrides
    List<ResourceEntry> tobif = biftable.getValueList(BIFFEditorTable.TYPE_NEW);
    tobif.addAll(biftable.getValueList(BIFFEditorTable.TYPE_UPD));
    for (int i = 0; i < tobif.size(); i++)
      biffwriter.addResource(tobif.get(i), false);
    try {
      biffwriter.write();
      progress.setProgress(3, true);
    } catch (Exception e) {
      progress.setProgress(3, false);
      JOptionPane.showMessageDialog(editframe, "Error while saving " + bifentry,
                                    "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
      blocker.setBlocked(false);
      return;
    }

    // 4: Delete old files from override
    for (int i = 0; i < tobif.size(); i++)
      new File(ResourceFactory.getRootDir(),
               ResourceFactory.OVERRIDEFOLDER + File.separatorChar + tobif.get(i).toString()).delete();
    progress.setProgress(4, true);

    // 5: Add new OverrideResourceEntries (ResourceEntries deleted from BIF)
    origbiflist.removeAll(biftable.getValueList(BIFFEditorTable.TYPE_BIF));
    origbiflist.removeAll(overridetable.getValueList(BIFFEditorTable.TYPE_BIF));
    for (int i = 0; i < origbiflist.size(); i++) {
      File file = new File(ResourceFactory.getRootDir(),
                           ResourceFactory.OVERRIDEFOLDER + File.separatorChar +
                           origbiflist.get(i).toString());
      FileResourceEntry fileEntry = new FileResourceEntry(file, true);
      ResourceFactory.getInstance().getResources().addResourceEntry(fileEntry, fileEntry.getTreeFolder());
    }
    progress.setProgress(5, true);

    // 6: Write keyfile
    try {
      ResourceFactory.getKeyfile().write();
      progress.setProgress(6, true);
    } catch (IOException e) {
      progress.setProgress(6, false);
      JOptionPane.showMessageDialog(editframe, "Error while saving keyfile", "Error",
                                    JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    ResourceFactory.getInstance().getResources().sort();
    blocker.setBlocked(false);
  }

// --------------------- End Interface Runnable ---------------------

  public void makeEditor(BIFFEntry bifentry, int format)
  {
    this.bifentry = bifentry;
    this.format = format;
    editframe = new ChildFrame("Edit BIFF", true);
    editframe.setIconImage(Icons.getIcon("Edit16.gif").getImage());
    Container pane = editframe.getContentPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    pane.setLayout(gbl);

    for (final ResourceEntry entry : ResourceFactory.getInstance().getResources().getResourceEntries()) {
      if (entry instanceof FileResourceEntry && entry.hasOverride() && entry.toString().length() < 13 &&
          ResourceFactory.getKeyfile().getExtensionType(entry.getExtension()) != -1)
        overridetable.addEntry(entry, BIFFEditorTable.TYPE_NEW);
      else if (bifentry.getIndex() != -1 && entry instanceof BIFFResourceEntry) {
        BIFFResourceEntry bentry = (BIFFResourceEntry)entry;
        if (bentry.getBIFFEntry() == bifentry) {
          biftable.addEntry(bentry, BIFFEditorTable.TYPE_BIF);
          origbiflist.add(bentry);
          if (bentry.hasOverride())
            overridetable.addEntry(entry, BIFFEditorTable.TYPE_UPD);
        }
      }
    }
    overridetable.sortTable();
    biftable.sortTable();

    biftable.addListSelectionListener(this);
    overridetable.addListSelectionListener(this);
    bcancel = new JButton("Cancel", Icons.getIcon("Delete16.gif"));
    bsave = new JButton("Save", Icons.getIcon("Save16.gif"));
    bcancel.setMnemonic('c');
    bsave.setMnemonic('s');
    btobif = new JButton(Icons.getIcon("Back16.gif"));
    bfrombif = new JButton(Icons.getIcon("Forward16.gif"));

    biftable.setBorder(BorderFactory.createTitledBorder("Files in " + bifentry.toString()));
    overridetable.setBorder(BorderFactory.createTitledBorder("Files in override"));

    JPanel bpanel1 = new JPanel(new GridLayout(2, 1, 6, 6));
    bpanel1.add(btobif);
    bpanel1.add(bfrombif);

    JPanel bpanel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    bpanel2.add(bsave);
    bpanel2.add(bcancel);

    List<String> formats = new ArrayList<String>();
    formats.add(s_bifformat[BIFF]);
    int gameid = ResourceFactory.getGameID();
    if (gameid == ResourceFactory.ID_ICEWIND || gameid == ResourceFactory.ID_ICEWINDHOW ||
        gameid == ResourceFactory.ID_ICEWINDHOWTOT)
      formats.add(s_bifformat[BIF]);
    else if (gameid == ResourceFactory.ID_BG2 || gameid == ResourceFactory.ID_BG2TOB)
      formats.add(s_bifformat[BIFC]);
    cbformat = new JComboBox(formats.toArray());
    cbformat.addActionListener(this);
    if (format != BIFF)
      cbformat.setSelectedIndex(1);
    else
      cbformat.setSelectedIndex(0);
    JPanel bpanel3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
    bpanel3.add(new JLabel("Format: "));
    bpanel3.add(cbformat);
    cbformat.setEnabled(false); // Temporary while I figure things out

    btobif.addActionListener(this);
    bfrombif.addActionListener(this);
    bsave.addActionListener(this);
    bcancel.addActionListener(this);
    btobif.setEnabled(false);
    bfrombif.setEnabled(false);
    bsave.setEnabled(false);

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = 1;
    gbc.insets = new Insets(6, 6, 6, 6);
    gbl.setConstraints(biftable, gbc);
    pane.add(biftable);

    gbc.weightx = 0.0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.CENTER;
    gbl.setConstraints(bpanel1, gbc);
    pane.add(bpanel1);

    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(overridetable, gbc);
    pane.add(overridetable);

    gbc.gridwidth = 2;
    gbc.weighty = 0.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(bpanel3, gbc);
    pane.add(bpanel3);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.anchor = GridBagConstraints.EAST;
    gbl.setConstraints(bpanel2, gbc);
    pane.add(bpanel2);

    editframe.setSize(550, 550);
    Center.center(editframe, NearInfinity.getInstance().getBounds());
    editframe.setVisible(true);
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class BifSaveProgress extends JFrame implements ActionListener
  {
    private final JCheckBox[] boxes = new JCheckBox[6];
    private final JLabel[] labels = new JLabel[6];
    private final JButton bok = new JButton("Ok");

    private BifSaveProgress()
    {
      super("Progress");
      labels[0] = new JLabel("Remove old entries");
      labels[1] = new JLabel("Extract files");
      labels[2] = new JLabel("Write new BIFF");
      labels[3] = new JLabel("Remove old files");
      labels[4] = new JLabel("Add new files");
      labels[5] = new JLabel("Write new keyfile");
      bok.addActionListener(this);
      bok.setEnabled(false);

      Container pane = getContentPane();
      GridBagLayout gbl = new GridBagLayout();
      GridBagConstraints gbc = new GridBagConstraints();
      pane.setLayout(gbl);
      gbc.insets = new Insets(6, 6, 6, 6);
      gbc.weightx = 0.0;
      gbc.weighty = 0.0;
      gbc.fill = GridBagConstraints.NONE;
      for (int i = 0; i < boxes.length; i++) {
        boxes[i] = new JCheckBox();
        boxes[i].setEnabled(false);
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbl.setConstraints(boxes[i], gbc);
        pane.add(boxes[i]);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(labels[i], gbc);
        pane.add(labels[i]);
      }
      gbc.anchor = GridBagConstraints.CENTER;
      gbl.setConstraints(bok, gbc);
      pane.add(bok);

      setSize(200, 280);
      Center.center(this, NearInfinity.getInstance().getBounds());
      setVisible(true);
    }

    private void setProgress(int level, boolean ok)
    {
      if (ok)
        boxes[level - 1].setSelected(true);
      else
        boxes[level - 1].setForeground(Color.red);
      bok.setEnabled(level == boxes.length || !ok);
    }

    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == bok)
        setVisible(false);
    }
  }
}

