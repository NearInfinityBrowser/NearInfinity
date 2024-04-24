// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.mus;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.sound.AudioPlayer;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;

public class Viewer extends JPanel implements Runnable, ActionListener {
  /** Provides quick access to the "play" and "pause" image icon. */
  private static final HashMap<Boolean, Icon> PLAY_ICONS = new HashMap<>();

  static {
    PLAY_ICONS.put(true, Icons.ICON_PLAY_16.getIcon());
    PLAY_ICONS.put(false, Icons.ICON_PAUSE_16.getIcon());
  }

  private final SimpleListModel<Entry> listModel = new SimpleListModel<>();
  private final JList<Entry> list = new JList<>(listModel);
  private final AudioPlayer player = new AudioPlayer();
  private final List<Entry> entryList = new Vector<>();

  private JLabel playList;
  private JButton bPlay;
  private JButton bEnd;
  private JButton bStop;
  private boolean play= false;
  private boolean end = false;
  private boolean closed = false;

  public Viewer(MusResource mus) {
    initGUI();
    loadMusResource(mus);
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == bPlay) {
      if (player == null || !player.isRunning()) {
        new Thread(this).start();
      } else if (player.isRunning()) {
        setPlayButtonState(player.isPaused());
        player.setPaused(!player.isPaused());
      }
    } else if (event.getSource() == bStop) {
      bStop.setEnabled(false);
      bEnd.setEnabled(false);
      setPlayButtonState(false);
      play = false;
      player.stopPlay();
    } else if (event.getSource() == bEnd) {
      bEnd.setEnabled(false);
      end = true;
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void run() {
    setPlayButtonState(true);
    bStop.setEnabled(true);
    bEnd.setEnabled(true);
    list.setEnabled(false);
    int nextnr = list.getSelectedIndex();
    if (nextnr == -1) {
      nextnr = 0;
    }
    play = true;
    end = false;
    try {
      while (play) {
        if (!end) {
          list.setSelectedIndex(nextnr);
          list.ensureIndexIsVisible(nextnr);
          list.repaint();
          player.playContinuous(entryList.get(nextnr).getAudioBuffer());
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
    player.stopPlay();
    setPlayButtonState(false);
    bStop.setEnabled(false);
    bEnd.setEnabled(false);
    list.setEnabled(true);
    list.setSelectedIndex(0);
    list.ensureIndexIsVisible(0);
  }

  // --------------------- End Interface Runnable ---------------------

  public void close() {
    setClosed(true);
    stopPlay();
    for (final Entry entry : entryList) {
      entry.close();
    }
    entryList.clear();
  }

  // Creates a new music list and loads all associated soundtracks
  public void loadMusResource(final MusResource mus) {
    if (mus != null) {
      // Parse and load soundtracks in a separate thread
      (new SwingWorker<Boolean, Void>() {
        @Override
        public Boolean doInBackground() {
          return parseMusFile(mus);
        }
      }).execute();
    }
  }

  private boolean parseMusFile(MusResource mus) {
    if (!isClosed()) {
      stopPlay();
      bPlay.setEnabled(false);
      list.setEnabled(false);
      StringTokenizer tokenizer = new StringTokenizer(mus.getText(), "\r\n");
      String dir = getNextToken(tokenizer, true);
      listModel.clear();
      entryList.clear();
      int count = Integer.valueOf(getNextToken(tokenizer, true));
      for (int i = 0; i < count; i++) {
        if (isClosed()) {
          return false;
        }
        Entry entry = new Entry(mus.getResourceEntry(), dir, entryList, getNextToken(tokenizer, true), i);
        entryList.add(entry);
        listModel.addElement(entry);
      }
      list.setSelectedIndex(0);
      validate();

      for (final Entry entry : entryList) {
        if (isClosed()) {
          return false;
        }
        try {
          entry.init();
        } catch (Exception e) {
          e.printStackTrace();
          JOptionPane.showMessageDialog(getTopLevelAncestor(),
              "Error loading " + entry.toString() + '\n' + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }

      boolean enable = (!entryList.isEmpty() && entryList.get(0).getAudioBuffer() != null);
      bPlay.setEnabled(enable);
      list.setEnabled(enable);
      return true;
    }
    return false;
  }

  /**
   * Returns the next valid token from the given {@code StringTokenizer}.
   *
   * @param tokenizer {@link StringTokenizer} containing string tokens.
   * @param ignoreComments Whether comments should be skipped.
   * @return The next string token if available, an empty string otherwise.
   */
  private String getNextToken(StringTokenizer tokenizer, boolean ignoreComments) {
    String retVal = "";
    while (tokenizer != null && tokenizer.hasMoreTokens()) {
      retVal = tokenizer.nextToken().trim();
      if (!ignoreComments || !retVal.startsWith("#")) {
        break;
      }
    }
    return retVal;
  }

  private void initGUI() {
    bPlay = new JButton();
    setPlayButtonState(false);
    bPlay.addActionListener(this);
    bEnd = new JButton("Finish", Icons.ICON_END_16.getIcon());
    bEnd.setEnabled(false);
    bEnd.addActionListener(this);
    bStop = new JButton("Stop", Icons.ICON_STOP_16.getIcon());
    bStop.setEnabled(false);
    bStop.addActionListener(this);

    JPanel buttonPanel = new JPanel(new GridLayout(1, 0, 6, 0));
    buttonPanel.add(bPlay);
    buttonPanel.add(bEnd);
    buttonPanel.add(bStop);

    list.setEnabled(false);
    list.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
    list.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
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

  public void stopPlay() {
    if (player != null) {
      play = false;
      player.stopPlay();
    }
  }

  private synchronized void setClosed(boolean b) {
    if (b != closed) {
      closed = b;
    }
  }

  private synchronized boolean isClosed() {
    return closed;
  }

  // Sets icon and text for the Play button according to the specified parameter.
  private void setPlayButtonState(boolean paused) {
    bPlay.setIcon(PLAY_ICONS.get(!paused));
    bPlay.setText(paused ? "Pause" : "Play");
  }
}
