// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.mus;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.sound.AudioPlayer;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;

public class Viewer extends JPanel implements Runnable, ActionListener
{
  private final SimpleListModel<Entry> listModel = new SimpleListModel<>();
  private final JList<Entry> list = new JList<>(listModel);
  private final AudioPlayer player = new AudioPlayer();
  private final List<Entry> entryList = new Vector<>();

  private JLabel playList;
  private JButton bPlay, bEnd, bStop;
  private boolean play, end, closed = false;


  public Viewer(MusResource mus)
  {
    initGUI();
    loadMusResource(mus);
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bPlay) {
      new Thread(this).start();
    } else if (event.getSource() == bStop) {
      bStop.setEnabled(false);
      bEnd.setEnabled(false);
      play = false;
      player.stopPlay();
    } else if (event.getSource() == bEnd) {
      bEnd.setEnabled(false);
      end = true;
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface Runnable ---------------------

  @Override
  public void run()
  {
    bPlay.setEnabled(false);
    bStop.setEnabled(true);
    bEnd.setEnabled(true);
    list.setEnabled(false);
    int nextnr = list.getSelectedIndex();
    if (nextnr == -1)
      nextnr = 0;
    play = true;
    end = false;
    try {
      while (play) {
        if (!end) {
          list.setSelectedIndex(nextnr);
          list.ensureIndexIsVisible(nextnr);
          list.repaint();
          player.play(entryList.get(nextnr).getAudioBuffer());
        } else if (entryList.get(nextnr).getEndBuffer() != null) {
          player.play(entryList.get(nextnr).getEndBuffer());
          play = false;
        }
        if (!end) {
          nextnr = entryList.get(nextnr).getNextNr();
          if (nextnr == -1 || nextnr == entryList.size()) {
            play = false;
          }
        }
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    bPlay.setEnabled(true);
    bStop.setEnabled(false);
    bEnd.setEnabled(false);
    list.setEnabled(true);
    list.setSelectedIndex(0);
    list.ensureIndexIsVisible(0);
  }

//--------------------- End Interface Runnable ---------------------

  public void close()
  {
    setClosed(true);
    stopPlay();
    for (final Entry entry: entryList) {
      entry.close();
    }
    entryList.clear();
  }

  // Creates a new music list and loads all associated soundtracks
  public void loadMusResource(final MusResource mus)
  {
    if (mus != null) {
      // Parse and load soundtracks in a separate thread
      (new SwingWorker<Boolean, Void>() {
        @Override
        public Boolean doInBackground()
        {
          return parseMusFile(mus);
        }
      }).execute();
    }
  }

  private boolean parseMusFile(MusResource mus)
  {
    if (!isClosed()) {
      stopPlay();
      bPlay.setEnabled(false);
      list.setEnabled(false);
      StringTokenizer tokenizer = new StringTokenizer(mus.getText(), "\r\n");
      String dir = tokenizer.nextToken().trim();
      listModel.clear();
      entryList.clear();
      int count = Integer.valueOf(tokenizer.nextToken().trim()).intValue();
      for (int i = 0; i < count; i++) {
        if (isClosed()) return false;
        Entry entry = new Entry(mus.getResourceEntry(), dir, entryList, tokenizer.nextToken().trim(), i);
        entryList.add(entry);
        listModel.addElement(entry);
      }
      list.setSelectedIndex(0);
      validate();

      for (final Entry entry: entryList) {
        if (isClosed()) return false;
        try {
          entry.init();
        } catch (Exception e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(getTopLevelAncestor(), "Error loading " + entry.toString() + '\n' +
                                        e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }

      boolean enable = (!entryList.isEmpty() && entryList.get(0).getAudioBuffer() != null);
      bPlay.setEnabled(enable);
      list.setEnabled(enable);
      return true;
    }
    return false;
  }


  private void initGUI()
  {
    bPlay = new JButton("Play", Icons.getIcon(Icons.ICON_PLAY_16));
    bPlay.addActionListener(this);
    bEnd = new JButton("Finish", Icons.getIcon(Icons.ICON_END_16));
    bEnd.setEnabled(false);
    bEnd.addActionListener(this);
    bStop = new JButton("Stop", Icons.getIcon(Icons.ICON_STOP_16));
    bStop.setEnabled(false);
    bStop.addActionListener(this);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 6, 0));
    buttonPanel.add(bPlay);
    buttonPanel.add(bEnd);
    buttonPanel.add(bStop);

    list.setEnabled(false);
    list.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
    list.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getScriptFont()));
    playList = new JLabel("Playlist:");

    JScrollPane scroll = new JScrollPane(list);
    JPanel centerPanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    centerPanel.setLayout(gbl);
    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbl.setConstraints(playList, gbc);
    centerPanel.add(playList);
    gbl.setConstraints(scroll, gbc);
    centerPanel.add(scroll);
    gbl.setConstraints(buttonPanel, gbc);
    centerPanel.add(buttonPanel);

    setLayout(new BorderLayout());
    add(centerPanel, BorderLayout.CENTER);
  }

  public void stopPlay()
  {
    if (player != null) {
      play = false;
      player.stopPlay();
    }
  }

  private synchronized void setClosed(boolean b)
  {
    if (b != closed) {
      closed = b;
    }
  }

  private synchronized boolean isClosed()
  {
    return closed;
  }
}
