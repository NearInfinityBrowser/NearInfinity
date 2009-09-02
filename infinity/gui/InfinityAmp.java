// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.icon.Icons;
import infinity.resource.Closeable;
import infinity.resource.ResourceFactory;
import infinity.resource.key.ResourceEntry;
import infinity.resource.mus.Entry;
import infinity.resource.sound.SoundUtilities;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.StringTokenizer;

public final class InfinityAmp extends ChildFrame implements ActionListener, ListSelectionListener, Runnable,
                                                             Closeable
{
  private final DefaultListModel allMusModel = new DefaultListModel();
  private final DefaultListModel selectedMusModel = new DefaultListModel();
  private final DefaultListModel excludedAcmModel = new DefaultListModel();
  private final JButton bPlay = new JButton(Icons.getIcon("Play16.gif"));
  private final JButton bStop = new JButton(Icons.getIcon("Stop16.gif"));
  private final JButton bAdd = new JButton(Icons.getIcon("Forward16.gif"));
  private final JButton bRemove = new JButton(Icons.getIcon("Back16.gif"));
  private final JButton bUp = new JButton(Icons.getIcon("Up16.gif"));
  private final JButton bDown = new JButton(Icons.getIcon("Down16.gif"));
  private final JButton bAddEx = new JButton("Add");
  private final JButton bRemoveEx = new JButton("Remove");
  private final JCheckBox cbLoop = new JCheckBox("Loop", true);
  private final JList allMusList;
  private final JList selectedMusList;
  private final JList excludedAcmList;
  private final JTextField tfNowPlaying = new JTextField(10);
  private final JTextField tfExclude = new JTextField(15);
  private final SoundUtilities a2w = new SoundUtilities();
  private Entry entries[];
  private boolean keepPlaying = true;

  public InfinityAmp()
  {
    super("InfinityAmp");
    setIconImage(Icons.getIcon("Volume16.gif").getImage());
    List<ResourceEntry> files = ResourceFactory.getInstance().getResources("MUS");
    for (int i = 0; i < files.size(); i++)
      allMusModel.addElement(files.get(i));
    excludedAcmModel.addElement("SPC1.ACM");
    excludedAcmModel.addElement("MX0000A.ACM");
    excludedAcmModel.addElement("MX9000A.ACM");
    allMusList = new JList(allMusModel);
    selectedMusList = new JList(selectedMusModel);
    excludedAcmList = new JList(excludedAcmModel);
    bPlay.addActionListener(this);
    bStop.addActionListener(this);
    bAdd.addActionListener(this);
    bRemove.addActionListener(this);
    bUp.addActionListener(this);
    bDown.addActionListener(this);
    bAddEx.addActionListener(this);
    bRemoveEx.addActionListener(this);
    tfExclude.addActionListener(this);
    allMusList.addListSelectionListener(this);
    selectedMusList.addListSelectionListener(this);
    excludedAcmList.addListSelectionListener(this);
    selectedMusList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    excludedAcmList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    bAdd.setEnabled(false);
    bRemove.setEnabled(false);
    bUp.setEnabled(false);
    bDown.setEnabled(false);
    bPlay.setEnabled(false);
    bStop.setEnabled(false);
    bRemoveEx.setEnabled(false);
    tfNowPlaying.setEnabled(false);
    bAddEx.setMnemonic('a');
    bRemoveEx.setMnemonic('r');
    cbLoop.setMnemonic('l');
    tfExclude.setMinimumSize(new Dimension(tfExclude.getMinimumSize().width, bAddEx.getMinimumSize().height));
    tfExclude.setPreferredSize(
            new Dimension(tfExclude.getPreferredSize().width, bAddEx.getPreferredSize().height));

    JPanel leftPanel = new JPanel(new BorderLayout());
    leftPanel.add(new JLabel("Available music"), BorderLayout.NORTH);
    leftPanel.add(new JScrollPane(allMusList), BorderLayout.CENTER);

    JPanel rightPanel = new JPanel(new BorderLayout());
    rightPanel.add(new JLabel("Playlist"), BorderLayout.NORTH);
    rightPanel.add(new JScrollPane(selectedMusList), BorderLayout.CENTER);

    JPanel buttonPanel = new JPanel(new GridLayout(4, 1, 0, 3));
    buttonPanel.add(bAdd);
    buttonPanel.add(bRemove);
    buttonPanel.add(bUp);
    buttonPanel.add(bDown);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel playlistPanel = new JPanel(gbl);

    rightPanel.setPreferredSize(leftPanel.getPreferredSize());
    rightPanel.setMaximumSize(leftPanel.getMaximumSize());
    rightPanel.setMinimumSize(leftPanel.getMinimumSize());

    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    gbc.anchor = GridBagConstraints.CENTER;
    gbc.insets = new Insets(3, 3, 3, 3);
    gbl.setConstraints(leftPanel, gbc);
    playlistPanel.add(leftPanel);

    gbc.fill = GridBagConstraints.NONE;
    gbc.weightx = 0.0;
    gbl.setConstraints(buttonPanel, gbc);
    playlistPanel.add(buttonPanel);

    gbc.fill = GridBagConstraints.BOTH;
    gbc.weightx = 1.0;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(rightPanel, gbc);
    playlistPanel.add(rightPanel);

    JPanel lowerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    lowerPanel.add(bPlay);
    lowerPanel.add(bStop);
    lowerPanel.add(cbLoop);
    lowerPanel.add(new JLabel("Now playing:"));
    lowerPanel.add(tfNowPlaying);

    JPanel exclusionPanel = new JPanel(new BorderLayout());
    JPanel exBottonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    exBottonPanel.add(tfExclude);
    exBottonPanel.add(bAddEx);
    exBottonPanel.add(bRemoveEx);
    exclusionPanel.add(new JLabel("Sound clips excluded from playback"), BorderLayout.NORTH);
    exclusionPanel.add(new JScrollPane(excludedAcmList), BorderLayout.CENTER);
    exclusionPanel.add(exBottonPanel, BorderLayout.SOUTH);
    exclusionPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 0, 3));

    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Playlist editor", playlistPanel);
    tabbedPane.addTab("Excluded clips", exclusionPanel);

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(tabbedPane, BorderLayout.CENTER);
    pane.add(lowerPanel, BorderLayout.SOUTH);
    pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    setSize(450, 350);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bPlay) {
      new Thread(this).start();
    }
    else if (event.getSource() == bStop) {
      keepPlaying = false;
      a2w.stopPlay();
    }
    else if (event.getSource() == bAdd) {
      int indices[] = allMusList.getSelectedIndices();
      for (final int index : indices)
        selectedMusModel.addElement(allMusModel.get(index));
      bPlay.setEnabled(true);
    }
    else if (event.getSource() == bRemove) {
      int index = selectedMusList.getSelectedIndex();
      selectedMusModel.remove(index);
      index = Math.min(index, selectedMusModel.size() - 1);
      if (index >= 0)
        selectedMusList.addSelectionInterval(index, index);
      bPlay.setEnabled(selectedMusModel.size() > 0);
    }
    else if (event.getSource() == bUp) {
      int index = selectedMusList.getSelectedIndex();
      Object o = selectedMusModel.remove(index);
      selectedMusModel.add(index - 1, o);
      selectedMusList.addSelectionInterval(index - 1, index - 1);
    }
    else if (event.getSource() == bDown) {
      int index = selectedMusList.getSelectedIndex();
      Object o = selectedMusModel.remove(index);
      selectedMusModel.add(index + 1, o);
      selectedMusList.addSelectionInterval(index + 1, index + 1);
    }
    else if (event.getSource() == bAddEx) {
      if (tfExclude.getText() != null && !tfExclude.getText().equals("")) {
        String file = tfExclude.getText();
        if (!file.toUpperCase().endsWith(".ACM"))
          file += ".ACM";
        excludedAcmModel.addElement(file);
        tfExclude.setText("");
      }
    }
    else if (event.getSource() == bRemoveEx) {
      int index = excludedAcmList.getSelectedIndex();
      excludedAcmModel.remove(index);
      index = Math.min(index, excludedAcmModel.size() - 1);
      if (index >= 0)
        excludedAcmList.addSelectionInterval(index, index);
    }
    else if (event.getSource() == tfExclude)
      bAddEx.doClick();
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close()
  {
    keepPlaying = false;
    a2w.stopPlay();
    if (entries != null) {
      for (final Entry entry : entries)
        if (entry != null)
          entry.close();
    }
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getSource() == allMusList)
      bAdd.setEnabled(allMusList.getSelectedIndices().length != 0);
    else if (event.getSource() == selectedMusList) {
      bRemove.setEnabled(selectedMusList.getSelectedIndices().length != 0);
      bUp.setEnabled(selectedMusList.getSelectedIndex() > 0);
      bDown.setEnabled(selectedMusList.getSelectedIndex() >= 0 &&
                       selectedMusList.getSelectedIndex() < selectedMusModel.size() - 1);
    }
    else if (event.getSource() == excludedAcmList)
      bRemoveEx.setEnabled(excludedAcmList.getSelectedIndices().length != 0);
  }

// --------------------- End Interface ListSelectionListener ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    keepPlaying = true;
    bPlay.setEnabled(false);
    bStop.setEnabled(true);
    bAdd.setEnabled(false);
    bRemove.setEnabled(false);
    bUp.setEnabled(false);
    bDown.setEnabled(false);
    allMusList.setEnabled(false);
    selectedMusList.setEnabled(false);
    int index = 0;
    if (selectedMusList.getSelectedIndex() != -1)
      index = selectedMusList.getSelectedIndex();
    while (keepPlaying) {
      ResourceEntry musEntry = (ResourceEntry)selectedMusModel.get(index++);
      playMus(musEntry);
      if (index == selectedMusModel.size()) {
        if (cbLoop.isSelected())
          index = 0;
        else
          keepPlaying = false;
      }
    }
    tfNowPlaying.setText("");
    bPlay.setEnabled(true);
    bStop.setEnabled(false);
    bAdd.setEnabled(allMusList.getSelectedIndices().length != 0);
    bRemove.setEnabled(selectedMusList.getSelectedIndices().length != 0);
    bUp.setEnabled(selectedMusList.getSelectedIndex() > 0);
    bDown.setEnabled(selectedMusList.getSelectedIndex() >= 0 &&
                     selectedMusList.getSelectedIndex() < selectedMusModel.size() - 1);
    allMusList.setEnabled(true);
    selectedMusList.setEnabled(true);
  }

// --------------------- End Interface Runnable ---------------------

  private boolean isExcluded(File file)
  {
    String name = file.getName().substring(0, file.getName().indexOf((int)'.'));
    for (int i = 0; i < excludedAcmModel.size(); i++) {
      String ex = excludedAcmModel.get(i).toString();
      if (name.equalsIgnoreCase(ex.substring(0, ex.indexOf((int)'.'))))
        return true;
    }
    return false;
  }

  private void playMus(ResourceEntry musEntry)
  {
    try {
      StringTokenizer tokenizer = new StringTokenizer(new String(musEntry.getResourceData()), "\n");
      String dir = tokenizer.nextToken().trim();
      entries = new Entry[Integer.valueOf(tokenizer.nextToken().trim()).intValue()];
      for (int i = 0; i < entries.length; i++)
        entries[i] = new Entry(musEntry, dir, entries, tokenizer.nextToken().trim(), i);
      for (final Entry entry : entries)
        entry.init();

      tfNowPlaying.setText(musEntry.toString());
      setTitle("InfinityAmp: " + musEntry.toString());
      int nextnr = 0;
      while (keepPlaying) {
        File file = entries[nextnr].getWavFile();
        if (!isExcluded(file))
          a2w.play(file);
        if (entries[nextnr].getNextNr() <= nextnr ||
            entries[nextnr].getNextNr() >= entries.length)
          break;
        nextnr = entries[nextnr].getNextNr();
      }
      if (keepPlaying && entries[nextnr].getEndfile() != null && !isExcluded(entries[nextnr].getEndfile()))
        a2w.play(entries[nextnr].getEndfile());
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Error accessing " + musEntry + '\n' + e.getMessage(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    for (final Entry entry : entries)
      if (entry != null)
        entry.close();
  }
}

