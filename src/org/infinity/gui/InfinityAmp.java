// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.Closeable;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.mus.Entry;
import org.infinity.resource.sound.AudioBuffer;
import org.infinity.resource.sound.AudioPlayer;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;
import org.infinity.util.io.StreamUtils;

public final class InfinityAmp extends ChildFrame
                                implements ActionListener, ListSelectionListener, Runnable, Closeable
{
  private final SimpleListModel<ResourceEntry> allMusModel = new SimpleListModel<>();
  private final SimpleListModel<ResourceEntry> selectedMusModel = new SimpleListModel<>();
  private final JButton bPlay = new JButton(Icons.getIcon(Icons.ICON_PLAY_16));
  private final JButton bStop = new JButton(Icons.getIcon(Icons.ICON_STOP_16));
  private final JButton bAdd = new JButton(Icons.getIcon(Icons.ICON_FORWARD_16));
  private final JButton bRemove = new JButton(Icons.getIcon(Icons.ICON_BACK_16));
  private final JButton bUp = new JButton(Icons.getIcon(Icons.ICON_UP_16));
  private final JButton bDown = new JButton(Icons.getIcon(Icons.ICON_DOWN_16));
  private final JCheckBox cbLoop = new JCheckBox("Loop", true);
  private final JList<ResourceEntry> allMusList;
  private final JList<ResourceEntry> selectedMusList;
  private final JTextField tfNowPlaying = new JTextField(10);
  private final AudioPlayer player = new AudioPlayer();
  private List<Entry> entryList = new ArrayList<>();
  private boolean keepPlaying = true;

  public InfinityAmp()
  {
    super("InfinityAmp");
    setIconImage(Icons.getIcon(Icons.ICON_VOLUME_16).getImage());
    List<ResourceEntry> files = ResourceFactory.getResources("MUS");
    for (int i = 0; i < files.size(); i++) {
      allMusModel.addElement(files.get(i));
    }
    allMusList = new JList<>(allMusModel);
    selectedMusList = new JList<>(selectedMusModel);
    bPlay.addActionListener(this);
    bStop.addActionListener(this);
    bAdd.addActionListener(this);
    bRemove.addActionListener(this);
    bUp.addActionListener(this);
    bDown.addActionListener(this);
    allMusList.addListSelectionListener(this);
    selectedMusList.addListSelectionListener(this);
    selectedMusList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    bAdd.setEnabled(false);
    bRemove.setEnabled(false);
    bUp.setEnabled(false);
    bDown.setEnabled(false);
    bPlay.setEnabled(false);
    bStop.setEnabled(false);
    tfNowPlaying.setEnabled(false);
    cbLoop.setMnemonic('l');

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

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(playlistPanel, BorderLayout.CENTER);
    pane.add(lowerPanel, BorderLayout.SOUTH);
    pane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));

    setSize(Misc.getScaledValue(450), Misc.getScaledValue(350));
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

//--------------------- Begin Interface ActionListener ---------------------

 @Override
public void actionPerformed(ActionEvent event)
 {
   if (event.getSource() == bPlay) {
     new Thread(this).start();
   }
   else if (event.getSource() == bStop) {
     keepPlaying = false;
     player.stopPlay();
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
     ResourceEntry o = selectedMusModel.remove(index);
     selectedMusModel.add(index - 1, o);
     selectedMusList.addSelectionInterval(index - 1, index - 1);
   }
   else if (event.getSource() == bDown) {
     int index = selectedMusList.getSelectedIndex();
     ResourceEntry o = selectedMusModel.remove(index);
     selectedMusModel.add(index + 1, o);
     selectedMusList.addSelectionInterval(index + 1, index + 1);
   }
 }

//--------------------- End Interface ActionListener ---------------------


//--------------------- Begin Interface Closeable ---------------------

 @Override
public void close()
 {
   keepPlaying = false;
   player.stopPlay();
   Entry.clearCache();
 }

//--------------------- End Interface Closeable ---------------------


//--------------------- Begin Interface ListSelectionListener ---------------------

 @Override
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
 }

//--------------------- End Interface ListSelectionListener ---------------------


//--------------------- Begin Interface Runnable ---------------------

 @Override
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
     ResourceEntry musEntry = selectedMusModel.get(index++);
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

//--------------------- End Interface Runnable ---------------------

 private void playMus(ResourceEntry musEntry)
 {
   try {
     ByteBuffer bb = musEntry.getResourceBuffer();
     String[] lines = StreamUtils.readString(bb, bb.limit()).split("\r?\n");
     int idx = 0;
     String dir = lines[idx++].trim();
     int count = Integer.parseInt(lines[idx++].trim());
     entryList.clear();
     for (int i = 0; i < count; i++) {
       entryList.add(new Entry(musEntry, dir, entryList, lines[idx++].trim(), i));
     }
     for (final Entry entry : entryList) {
       entry.init();
     }

     tfNowPlaying.setText(musEntry.toString());
     setTitle("InfinityAmp: " + musEntry.toString());
     int nextnr = 0;
     while (keepPlaying) {
       AudioBuffer audio = entryList.get(nextnr).getAudioBuffer();
       player.play(audio);
       if (entryList.get(nextnr).getNextNr() <= nextnr ||
           entryList.get(nextnr).getNextNr() >= entryList.size())
         break;
       nextnr = entryList.get(nextnr).getNextNr();
     }
     if (keepPlaying && entryList.get(nextnr).getEndBuffer() != null)
       player.play(entryList.get(nextnr).getEndBuffer());
   } catch (Exception e) {
     JOptionPane.showMessageDialog(this, "Error accessing " + musEntry + '\n' + e.getMessage(), "Error",
                                   JOptionPane.ERROR_MESSAGE);
     e.printStackTrace();
   }
 }
}
