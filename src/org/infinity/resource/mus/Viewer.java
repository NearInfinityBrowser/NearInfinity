// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.mus;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import org.infinity.gui.ViewerUtil;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.sound.AudioStateEvent;
import org.infinity.resource.sound.AudioStateListener;
import org.infinity.resource.sound.StreamingAudioPlayer;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;
import org.infinity.util.StopWatch;

public class Viewer extends JPanel implements ActionListener, AudioStateListener {
  private static final ImageIcon ICON_PLAY  = Icons.ICON_PLAY_16.getIcon();
  private static final ImageIcon ICON_PAUSE = Icons.ICON_PAUSE_16.getIcon();
  private static final ImageIcon ICON_END   = Icons.ICON_END_16.getIcon();
  private static final ImageIcon ICON_STOP  = Icons.ICON_STOP_16.getIcon();

  /** Display format of elapsed time (minutes, seconds) */
  private static final String DISPLAY_TIME_FORMAT = "Elapsed time: %02d:%02d";

  private final SimpleListModel<Entry> listModel = new SimpleListModel<>();
  private final JList<Entry> list = new JList<>(listModel);
  private final StopWatch elapsedTimer = new StopWatch(1000L, false);

  private MusResourceHandler musHandler;
  private StreamingAudioPlayer player;
  private JLabel playList;
  private JButton bPlay;
  private JButton bEnd;
  private JButton bStop;
  private JLabel displayLabel;

  private boolean closed;

  public Viewer(MusResource mus) {
    initGUI();
    loadMusResource(mus);
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == elapsedTimer) {
      updateTimeLabel();
    } else if (event.getSource() == bPlay) {
      if (player == null) {
        try {
          player = new StreamingAudioPlayer(this);
        } catch (Exception e) {
          updateControls();
          Logger.error(e);
          JOptionPane.showMessageDialog(this, "Error during playback:\n" + e.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
      if (player.isPlaying()) {
        player.setPaused(!player.isPaused());
      } else {
        musHandler.setStartIndex(list.getSelectedIndex());
        player.setPlaying(true);
      }
    } else if (event.getSource() == bStop) {
      player.setPlaying(false);
    } else if (event.getSource() == bEnd) {
      musHandler.setSignalEnding(true);
      updateControls();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface Runnable ---------------------

  @Override
  public void audioStateChanged(AudioStateEvent event) {
//    Logger.trace("{}.audioStateChanged({})", Viewer.class.getName(), event);
    switch (event.getAudioState()) {
      case OPEN:
        handleAudioOpenEvent(event.getValue());
        break;
      case CLOSE:
        handleAudioCloseEvent(event.getValue());
        break;
      case START:
        handleAudioStartEvent();
        break;
      case STOP:
        handleAudioStopEvent();
        break;
      case PAUSE:
        handleAudioPauseEvent(event.getValue());
        break;
      case RESUME:
        handleAudioResumeEvent(event.getValue());
        break;
      case BUFFER_EMPTY:
        handleAudioBufferEmptyEvent(event.getValue());
        break;
      case ERROR:
        handleAudioErrorEvent(event.getValue());
        break;
    }
  }

  // --------------------- End Interface Runnable ---------------------

  /** Closes the MUS resource viewer and all releases resource. */
  public void close() {
    closed = true;
    resetPlayer();
    elapsedTimer.close();
    try {
      musHandler.close();
    } catch (Exception e) {
      Logger.error(e);
    }
    updateControls();
  }

  /**
   * Creates a new music list and loads all associated soundtracks. Load operation is performed in a background task to
   * prevent the UI from blocking.
   */
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

  /** Parses the specified {@link MusResource} instances for playback. */
  private boolean parseMusFile(MusResource mus) {
    if (!isClosed()) {
      resetPlayer();
      bPlay.setEnabled(false);
      list.setEnabled(false);
      listModel.clear();
      try {
        musHandler = new MusResourceHandler(mus.getResourceEntry(), 0, false, true);
        for (int i = 0, size = musHandler.size(); i < size; i++) {
          listModel.add(musHandler.getEntry(i));
        }
        list.setSelectedIndex(0);
        validate();
      } catch (Exception e) {
        Logger.error(e);
        JOptionPane.showMessageDialog(getTopLevelAncestor(),
            "Error loading " + mus.getResourceEntry() + ":\n" + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
      updateControls();
    }
    return !isClosed();
  }

  /** Sets up the UI of the viewer. */
  private void initGUI() {
    bPlay = new JButton();
    bPlay.addActionListener(this);

    // prevent Play button state change from affecting the overall layout
    setPlayButtonState(true);
    int minWidth = bPlay.getPreferredSize().width;
    setPlayButtonState(false);
    minWidth = Math.max(minWidth, bPlay.getPreferredSize().width);
    bPlay.setPreferredSize(new Dimension(minWidth, bPlay.getPreferredSize().height));

    bEnd = new JButton("Finish", ICON_END);
    bEnd.addActionListener(this);

    bStop = new JButton("Stop", ICON_STOP);
    bStop.addActionListener(this);

    displayLabel = new JLabel("", SwingConstants.LEADING);
    updateTimeLabel(0L);

    list.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
    list.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
    JScrollPane listScroll = new JScrollPane(list);

    playList = new JLabel("Playlist:");

    elapsedTimer.addActionListener(this);

    final GridBagConstraints gbc = new GridBagConstraints();

    final JPanel buttonPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    buttonPanel.add(bPlay, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 8, 0, 0), 0, 0);
    buttonPanel.add(bEnd, gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 8, 0, 0), 0, 0);
    buttonPanel.add(bStop, gbc);

    final JPanel centerPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    centerPanel.add(playList, gbc);
    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(8, 0, 0, 0), 0, 0);
    centerPanel.add(listScroll, gbc);
    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    centerPanel.add(displayLabel, gbc);
    ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    centerPanel.add(buttonPanel, gbc);

    // default list height is rather small
    final Dimension dim = listScroll.getPreferredSize();
    dim.height *= 2;
    listScroll.setPreferredSize(dim);

    setLayout(new BorderLayout());
    add(centerPanel, BorderLayout.CENTER);

    updateControls();
  }

  /** Called when the audio player triggers a {@code OPEN} event. */
  private void handleAudioOpenEvent(Object value) {
    musHandler.reset();
  }

  /** Called when the audio player triggers a {@code CLOSE} event. */
  private void handleAudioCloseEvent(Object value) {
    // nothing to do
  }

  /** Called when the audio player triggers a {@code START} event. */
  private void handleAudioStartEvent() {
    elapsedTimer.reset();
    elapsedTimer.resume();
    updateTimeLabel();
    updateControls();
  }

  /** Called when the audio player triggers a {@code STOP} event. */
  private void handleAudioStopEvent() {
    if (player == null) {
      return;
    }

    player.clearAudioQueue();
    elapsedTimer.pause();
    elapsedTimer.reset();
    updateTimeLabel();
    updateControls();
    list.setSelectedIndex(0);
    list.ensureIndexIsVisible(0);
    musHandler.reset();
  }

  /** Called when the audio player triggers a {@code PAUSE} event. */
  private void handleAudioPauseEvent(Object value) {
    elapsedTimer.pause();
    setPlayButtonState(false);
  }

  /** Called when the audio player triggers a {@code RESUME} event. */
  private void handleAudioResumeEvent(Object value) {
    elapsedTimer.resume();
    setPlayButtonState(true);
  }

  /** Called when the audio player triggers a {@code BUFFER_EMPTY} event. */
  private void handleAudioBufferEmptyEvent(Object value) {
    if (player == null) {
      return;
    }

    if (musHandler.advance()) {
      player.addAudioBuffer(musHandler.getAudioBuffer());
      list.setSelectedIndex(musHandler.getCurrentIndex());
      list.ensureIndexIsVisible(musHandler.getCurrentIndex());
    } else {
      player.setPlaying(false);
    }
  }

  /** Called when the audio player triggers an {@code ERROR} event. */
  private void handleAudioErrorEvent(Object value) {
    if (player != null) {
      player.setPlaying(false);
    }
    final Exception e = (value instanceof Exception) ? (Exception)value : null;
    if (e != null) {
      Logger.error(e);
    }
    final String msg = (e != null) ? "Error during playback:\n" + e.getMessage() : "Error during playback.";
    JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }

  /** Closes the audio player and releases associated resources. */
  private void resetPlayer() {
    if (player != null) {
      try {
        player.close();
      } catch (Exception e) {
        Logger.debug(e);
      }
      player = null;
    }
  }

  /** Returns whether the MUS resource viewer has been closed. */
  private boolean isClosed() {
    return closed;
  }

  /** Updates the elapsed time label with the elapsed playback time. */
  private void updateTimeLabel() {
    updateTimeLabel(elapsedTimer.elapsed());
  }

  /** Updates the elapsed time label with the specified time value. */
  private void updateTimeLabel(long millis) {
    final long minutes = millis / 60_000L;
    final long seconds = (millis / 1000L) % 60L;
    displayLabel.setText(String.format(DISPLAY_TIME_FORMAT, minutes, seconds));
  }

  /** Updates audio controls depending on current playback state. */
  private void updateControls() {
    if (musHandler != null && player != null && player.isPlaying()) {
      setPlayButtonState(!player.isPaused());
      bPlay.setEnabled(true);
      bEnd.setEnabled(!musHandler.isEndingSignaled() && !musHandler.isEnding());
      bStop.setEnabled(true);
      list.setEnabled(false);
    } else {
      setPlayButtonState(false);
      bPlay.setEnabled(!listModel.isEmpty());
      bEnd.setEnabled(false);
      bStop.setEnabled(false);
      list.setEnabled(!listModel.isEmpty());
    }
  }

  /** Sets icon and text for the Play button according to the specified parameter. */
  private void setPlayButtonState(boolean paused) {
    bPlay.setIcon(paused ? ICON_PAUSE : ICON_PLAY);
    bPlay.setText(paused ? "Pause" : "Play");
  }
}
