// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.NearInfinity;
import org.infinity.gui.ButtonPopupMenu.Align;
import org.infinity.icon.Icons;
import org.infinity.resource.Closeable;
import org.infinity.resource.Profile;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.mus.Entry;
import org.infinity.resource.mus.MusResourceHandler;
import org.infinity.resource.sound.AudioStateEvent;
import org.infinity.resource.sound.AudioStateListener;
import org.infinity.resource.sound.StreamingAudioPlayer;
import org.infinity.util.InputKeyHelper;
import org.infinity.util.Logger;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;
import org.infinity.util.StopWatch;
import org.infinity.util.Threading;
import org.infinity.util.io.FileManager;
import org.infinity.util.tuples.Couple;

/**
 * A global music player for MUS files found in all supported IE games. It supersedes the original {@link InfinityAmp}
 * music player.
 */
public class InfinityAmpPlus extends ChildFrame implements Closeable {
  // static window title
  private static final String TITLE_DEFAULT = "InfinityAmp";
  // format string for window title: resource name, directory
  private static final String TITLE_FMT = "InfinityAmp - Playing %s (%s)";

  // Time information for current soundtrack (elapsed time / total time)
  private static final String PLAYTIME_FMT = "%02d:%02d / %02d:%02d";
  // Time information for current soundtrack (elapsed time only)
  private static final String PLAYTIME_SHORT_FMT = "%02d:%02d";

  // Title string for the available music list
  private static final String TITLE_AVAILABLE_LIST = "Available Music";
  private static final String TITLE_AVAILABLE_LIST_FMT = "Available Music - %d item(s)";

  // Title string for the playlist
  private static final String TITLE_PLAY_LIST = "Playlist";
  private static final String TITLE_PLAY_LIST_FMT = "Playlist - %d item(s)";

  // {@link Preferences} node for storing and restoring settings.
  private static final String PREFS_NODE                    = "InfinityAmp";
  // Available preferences keys
  private static final String PREFS_AVAILABLE_ENTRIES_COUNT = "AvailableEntryCount";
  private static final String PREFS_AVAILABLE_ENTRY_BASE    = "AvailableEntry";
  private static final String PREFS_PLAYLIST_INDEX_COUNT    = "PlaylistIndexCount";
  private static final String PREFS_PLAYLIST_INDEX_BASE     = "PlaylistIndex";
  private static final String PREFS_PLAYLIST_SELECTED_INDEX = "PlaylistSelectedIndex";
  private static final String PREFS_FILTER_SOUND_COUNT      = "FilterSoundCount";
  private static final String PREFS_FILTER_SOUND_BASE       = "FilterSound";
  private static final String PREFS_FILTER_SOUND_ENABLED    = "FilterSoundEnabled";
  private static final String PREFS_OPTION_CALC_DURATIONS   = "CalculateDurations";
  private static final String PREFS_OPTION_USE_MEDIA_KEYS   = "UseMediaKeys";
  private static final String PREFS_OPTION_LOOP             = "Loop";
  private static final String PREFS_OPTION_SHUFFLE          = "Shuffle";
  private static final String PREFS_VIEW_PREFIX             = "ViewPathAsPrefix";
  private static final String PREFS_WINDOW_X                = "WindowX";
  private static final String PREFS_WINDOW_Y                = "WindowY";
  private static final String PREFS_WINDOW_WIDTH            = "WindowWidth";
  private static final String PREFS_WINDOW_HEIGHT           = "WindowHeight";

  private final SimpleListModel<MusicResourceEntry> availableListModel = new SimpleListModel<>();
  private final SimpleListModel<MusicResourceEntry> playListModel = new SimpleListModel<>();
  private final DefaultListCellRenderer availableListRenderer = new IndexedCellRenderer(true);
  private final DefaultListCellRenderer playListRenderer = new IndexedCellRenderer(true);
  private final JList<MusicResourceEntry> availableList = new JList<>(availableListModel);
  private final JList<MusicResourceEntry> playList = new JList<>(playListModel);

  private final TreeSet<String> exclusionFilter = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

  private final JLabel availableListTitleLabel = new JLabel(TITLE_AVAILABLE_LIST);
  private final JLabel playListTitleLabel = new JLabel(TITLE_PLAY_LIST);

  private final JButton toPlayListButton = new JButton(Icons.ICON_FORWARD_16.getIcon());
  private final JButton fromPlayListButton = new JButton(Icons.ICON_BACK_16.getIcon());
  private final JButton moveUpButton = new JButton(Icons.ICON_UP_16.getIcon());
  private final JButton moveDownButton = new JButton(Icons.ICON_DOWN_16.getIcon());

  private final ButtonPopupMenu bpmAddMusic = new ButtonPopupMenu("Add...");
  private final JMenuItem miAddMusicCurrent = new JMenuItem("Music from current game");
  private final JMenuItem miAddMusicExternal = new JMenuItem("Music from directory...");
  private final JButton removeMusicButton = new JButton("Remove");
  private final JButton clearMusicButton = new JButton("Clear");

  private final JButton importPlayListButton = new JButton("Import...", Icons.ICON_IMPORT_16.getIcon());
  private final JButton exportPlayListButton = new JButton("Export...", Icons.ICON_EXPORT_16.getIcon());

  private final JButton prevMusicButton = new JButton(Icons.ICON_STEP_BACK_16.getIcon());
  private final JButton nextMusicButton = new JButton(Icons.ICON_STEP_FORWARD_16.getIcon());
  private final JButton playMusicButton = new JButton(Icons.ICON_PLAY_16.getIcon());
  private final JButton stopMusicButton = new JButton(Icons.ICON_STOP_16.getIcon());

  private final JLabel elapsedTimeLabel = new JLabel();

  private final JTextField tfMusicPath = new JTextField(5);
  private final JCheckBox cbLoop = new JCheckBox("Loop");
  private final JCheckBox cbShuffle = new JCheckBox("Shuffle");

  private final JCheckBoxMenuItem cbmiEnableExclusionFilter = new JCheckBoxMenuItem("Enable sound filter");
  private final JMenuItem miDefineExclusionFilter = new JMenuItem("Define sound filter...");
  private final JCheckBoxMenuItem cbmiCalcDurations = new JCheckBoxMenuItem("Calculate sound durations");
  private final JCheckBoxMenuItem cbmiUseMediaKeys = new JCheckBoxMenuItem("Allow media keys for playback");
  private final JRadioButtonMenuItem rbmiPathAsSuffix = new JRadioButtonMenuItem("Format: <filename.MUS> (<path>)");
  private final JRadioButtonMenuItem rbmiPathAsPrefix = new JRadioButtonMenuItem("Format: (<path>) <filename.MUS>");

  // Stores the previously played back MUS entries if Shuffle mode is enabled
  private final List<Integer> undoPlayListItems = new ArrayList<>();
  private final Listeners listeners = new Listeners();
  private final Random random = new Random();
  private final StopWatch timer = new StopWatch(1000L, false);

  private final StreamingAudioPlayer player;

  private MusResourceHandler musHandler;
  private boolean requestNextItem = false;

  public InfinityAmpPlus() {
    super(TITLE_DEFAULT, false, false);
    player = initPlayer(true);
    init();
  }

  @Override
  public void close() {
    closePlayer();
    timer.close();
    Entry.clearCache();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(listeners::dispatchKeyEvent);
    savePreferences();
  }

  @Override
  protected void gameReset(boolean refreshOnly) {
    // Update the window content to reflect a potentially changed game configuration
    refreshAllResourceEntries();
  }

  /** Scans available music list and playlist for non-existing files. */
  public void refreshAllResourceEntries() {
    for (int i = availableListModel.size() - 1; i >= 0; i--) {
      final MusicResourceEntry mre = availableListModel.get(i);
      if (Files.isRegularFile(mre.getActualPath())) {
        availableListModel.set(i, mre);
      } else {
        availableListModel.remove(i);
      }
    }

    for (int i = playListModel.size() - 1; i >= 0; i--) {
      final MusicResourceEntry mre = playListModel.get(i);
      if (Files.isRegularFile(mre.getActualPath())) {
        playListModel.set(i, mre);
      } else {
        playListModel.remove(i);
      }
    }
  }

  /** Returns the currently selected "path-as-prefix" state for list items. */
  public boolean isPathAsPrefix() {
    return rbmiPathAsPrefix.isSelected();
  }

  /** Updates all {@link MusicResourceEntry} instances in both lists with the specified display option. */
  public void updatePathAsPrefix(boolean b) {
    for (int i = 0, size = availableListModel.size(); i < size; i++) {
      final MusicResourceEntry mre = availableListModel.get(i);
      mre.setPathAsPrefix(b);
      availableListModel.set(i, mre);
    }

    for (int i = 0, size = playListModel.size(); i < size; i++) {
      final MusicResourceEntry mre = playListModel.get(i);
      mre.setPathAsPrefix(b);
      playListModel.set(i, mre);
    }
  }

  /** Returns whether playback is currently active. Paused state does not affect the result. */
  public boolean isPlaying() {
    return player.isPlaying();
  }

  public synchronized void setPlaying(boolean play) {
    if (play != isPlaying()) {
      if (play) {
        player.setPlaying(true);
      } else {
        player.setPlaying(false);
        player.clearAudioQueue();
      }
    }
  }

  /**
   * Returns whether playback is currently paused.
   *
   * @return {@code true} only if playback is enabled but paused, {@code false} otherwise.
   */
  public boolean isPaused() {
    return player.isPaused();
  }

  public synchronized void setPaused(boolean pause) {
    player.setPaused(pause);
  }

  /** Returns whether Loop mode is enabled. */
  public boolean isLoopEnabled() {
    return cbLoop.isSelected();
  }

  /** Specifies whether Loop mode is enabled. */
  public void setLoopEnabled(boolean b) {
    if (b != cbLoop.isSelected()) {
      cbLoop.setSelected(b);
      updatePlayListButtons();
    }
  }

  /** Returns whether Shuffle mode is enabled. */
  public boolean isShuffleEnabled() {
    return cbShuffle.isSelected();
  }

  /** Specifies whether Shuffle mode is enabled. */
  public void setShuffleEnabled(boolean b) {
    if (b != cbShuffle.isSelected()) {
      cbShuffle.setSelected(b);
      updatePlayListButtons();
      clearUndoList();
    }
  }

  /** Returns whether duration of soundtrack are calculated and displayed in the song lists. */
  public boolean isAutoCalcDurationsEnabled() {
    return cbmiCalcDurations.isSelected();
  }

  /** Specifies whether duration of soundtrack are calculated and displayed in the song lists. */
  public void setAutoCalcDurationsEnabled(boolean b) {
    if (b != cbmiCalcDurations.isSelected()) {
      cbmiCalcDurations.setSelected(b);
      if (cbmiCalcDurations.isSelected()) {
        calculateDurations();
      }
    }
  }

  /** Returns whether media keys on the keyboard can be used for music playback controls. */
  public boolean isMediaKeysEnabled() {
    return cbmiUseMediaKeys.isSelected();
  }

  /** Specifies whether media keys on the keyboard can be used for music playback controls. */
  public void setMediaKeysEnabled(boolean b) {
    if (b != cbmiUseMediaKeys.isSelected()) {
      cbmiUseMediaKeys.setSelected(b);
    }
  }

  /**
   * Sets the playlist to the first selected entry. Otherwise, it selects the first playlist item.
   *
   * @return Index of the selected playlist item if available, -1 otherwise.
   */
  public int setCurrentPlayListItem() {
    int newIndex = playList.getSelectedIndex();
    if (newIndex < 0 && !playListModel.isEmpty()) {
      newIndex = 0;
    }
    playList.setSelectedIndex(newIndex);
    playList.ensureIndexIsVisible(newIndex);
    return playList.getSelectedIndex();
  }

  /**
   * Sets the playlist to the next music entry if available.
   *
   * @return Next playlist item index if available, -1 otherwise.
   */
  public int setNextPlayListItem() {
    int newIndex;
    if (isShuffleEnabled()) {
      int curIndex = playList.getSelectedIndex();
      newIndex = curIndex;
      while (playListModel.size() > 1 && newIndex == curIndex) {
        newIndex = random.nextInt(playListModel.size());
      }
      if (curIndex >= 0) {
        undoPlayListItems.add(curIndex);
      }
    } else {
      newIndex = playList.getSelectedIndex() + 1;
      if (isLoopEnabled()) {
        newIndex %= playListModel.size();
      }
    }

    if (newIndex >= 0 && newIndex < playListModel.size()) {
      playList.setSelectedIndex(newIndex);
      playList.ensureIndexIsVisible(newIndex);
      updateWindowTitle();
    } else {
      newIndex = -1;
    }

    return newIndex;
  }

  /**
   * Sets the playlist to the previous music entry if available. For shuffled playback this entry is fetched from
   * an undo buffer. Otherwise the entry is calculated.
   *
   * @return Previous playlist item index if available, -1 otherwise.
   */
  public int setPrevPlayListItem() {
    int newIndex;
    if (isShuffleEnabled()) {
      // first try to restore previous selections
      if (!undoPlayListItems.isEmpty()) {
        newIndex = undoPlayListItems.remove(undoPlayListItems.size() - 1);
      } else {
        int curIndex = playList.getSelectedIndex();
        newIndex = curIndex;
        while (playListModel.size() > 1 && newIndex == curIndex) {
          newIndex = random.nextInt(playListModel.size());
        }
      }
    } else {
      newIndex = playList.getSelectedIndex() - 1;
      if (isLoopEnabled() && newIndex < 0) {
        newIndex = playListModel.size() - 1;
      }
    }

    if (newIndex >= 0 && newIndex < playListModel.size()) {
      playList.setSelectedIndex(newIndex);
      playList.ensureIndexIsVisible(newIndex);
      updateWindowTitle();
    } else {
      newIndex = -1;
    }

    return newIndex;
  }

  /** Returns an unmodifiable set of sound filter items. */
  public Set<String> getExclusionFilters() {
    return Collections.unmodifiableSet(exclusionFilter);
  }

  /**
   * Adds the specified sound filter to the exclusion list.
   *
   * @param filter Sound filter string without file extension.
   * @return {@code true} if the filter was added, {@code false} otherwise.
   */
  public boolean addExclusionFilter(String filter) {
    boolean retVal = false;

    if (filter != null) {
      final int pos = filter.lastIndexOf('.');
      if (pos >= 0 && filter.substring(pos).equalsIgnoreCase(".acm")) {
        filter = filter.substring(0, pos);
      }

      filter = filter.trim();
      if (!filter.isEmpty()) {
        retVal = exclusionFilter.add(filter);
      }
    }

    return retVal;
  }

  /** Replaces the current sound filters by the specified collection of filter items. */
  public int setExclusionFilters(Collection<String> filterSet) {
    int retVal = 0;
    exclusionFilter.clear();
    if (filterSet != null) {
      for (final String filter : filterSet) {
        if (addExclusionFilter(filter)) {
          retVal++;
        }
      }
    }
    return retVal;
  }

  /** Calculates duration of all available song entries. */
  private void calculateDurations() {
    for (int i = 0, size = availableListModel.size(); i < size; i++) {
      availableListModel.get(i).calculateDuration();
    }
  }

  /** Used internally to check if a request for playing back a new playlist item has been made. */
  private boolean isNextItemRequested() {
    return requestNextItem;
  }

  /** Used internally to signal that playback should restart at the currently selected playlist item. */
  private synchronized void requestNextItem() {
    if (isPlaying() && !isNextItemRequested()) {
      requestNextItem = true;
      setPlaying(false);
    }
  }

  /** Clears the request for playback of a new playlist item. */
  private synchronized void acceptNextItem() {
    if (isNextItemRequested()) {
      requestNextItem = false;
      setPlaying(true);
    }
  }

  /**
   * Finds the specified {@link MusicResourceEntry} in the available music list and returns the item index.
   *
   * @param entry {@link MusicResourceEntry} to find.
   * @return Index of the list item if found, -1 otherwise.
   */
  private int getAvailableListIndex(MusicResourceEntry entry) {
    int retVal = -1;

    if (entry != null) {
      for (int i = 0, size = availableListModel.size(); i < size; i++) {
        if (entry.equals(availableListModel.get(i))) {
          retVal = i;
          break;
        }
      }
    }

    return retVal;
  }

  /**
   * Finds the specified {@link MusicResourceEntry} in the playlist and returns the item index.
   *
   * @param entry {@link MusicResourceEntry} to find.
   * @return Index of the list item if found, -1 otherwise.
   */
  @SuppressWarnings("unused")
  private int getPlayListIndex(MusicResourceEntry entry) {
    int retVal = -1;

    if (entry != null) {
      for (int i = 0, size = playListModel.size(); i < size; i++) {
        if (entry.equals(playListModel.get(i))) {
          retVal = i;
          break;
        }
      }
    }

    return retVal;
  }

  /**
   * Adds the specified item from the available music list to the playlist.
   *
   * @param index Item index from the available music list to add.
   * @return {@code true} if the item was successfully added, {@code false} otherwise.
   */
  private boolean addToPlayList(int index) {
    if (!playList.isEnabled() || index < 0 || index >= availableListModel.size()) {
      return false;
    }

    final MusicResourceEntry mre = availableListModel.get(index);
    playListModel.add(mre);
    updatePlayListButtons();
    updatePlayListTitle();
    return true;
  }

  /**
   * Removes the specified item from the playlist.
   *
   * @param index Item index from the playlist to remove.
   * @return {@code true} if the item was successfully removed, {@code false} otherwise.
   */
  private boolean removeFromPlayList(int index) {
    if (!playList.isEnabled() || index < 0 || index >= playListModel.size()) {
      return false;
    }

    int curSelected = playList.getSelectedIndex();
    if (index == curSelected) {
      if (curSelected < playListModel.size() - 1) {
        playList.setSelectedIndex(curSelected + 1);
      } else if (curSelected > 0) {
        playList.setSelectedIndex(curSelected - 1);
      }
    }

    playListModel.remove(index);
    updatePlayListButtons();
    updatePlayListTitle();
    return true;
  }

  /** Moves all selected items in the playlist up by one position. */
  private void moveItemsUp() {
    if (playList.isEnabled()) {
      final int[] indices = playList.getSelectedIndices();
      if (indices.length == 0 || indices[0] <= 0) {
        return;
      }

      for (int i = 0; i < indices.length; i++) {
        final MusicResourceEntry mre = playListModel.remove(indices[i]);
        indices[i]--;
        playListModel.add(indices[i], mre);
      }
      playList.setSelectedIndices(indices);
    }
  }

  /** Moves all selected items in the playlist down by one position. */
  private void moveItemsDown() {
    if (playList.isEnabled()) {
      final int[] indices = playList.getSelectedIndices();
      if (indices.length == 0 || indices[indices.length - 1] >= playListModel.size() - 1) {
        return;
      }

      for (int i = indices.length - 1; i >= 0; i--) {
        final MusicResourceEntry mre = playListModel.remove(indices[i]);
        indices[i]++;
        playListModel.add(indices[i], mre);
      }
      playList.setSelectedIndices(indices);
    }
  }

  private void clearUndoList() {
    undoPlayListItems.clear();
  }

  /**
   * Functionally the same as {@link #addMusFiles(Path)} but also provides user feedback.
   *
   * @param musicDir Directory path of the game or "music" subfolder.
   */
  private void addMusFilesInteractive(Path musicDir) {
    try {
      // change to "music" dir if needed
      if (musicDir != null && !musicDir.getFileName().toString().equalsIgnoreCase("music")) {
        final Path path = FileManager.queryExisting(musicDir, "music");
        if (path != null) {
          musicDir = path;
        }
      }

      int count = addMusFiles(musicDir);
      if (count == 0) {
        JOptionPane.showMessageDialog(this, "No MUS files added.", "Information", JOptionPane.INFORMATION_MESSAGE);
      }
    } catch (Exception ex) {
      Logger.error(ex);
      JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  /** Adds MUS files from the specified directory to the available music list. */
  private int addMusFiles(Path musicDir) throws Exception {
    if (musicDir == null) {
      return 0;
    }

    if (!Files.isDirectory(musicDir)) {
      throw new IOException("Directory does not exist: " + musicDir);
    }

    try (final Stream<Path> fstream = Files.list(musicDir)) {
      final List<MusicResourceEntry> fileList = fstream
          .map(file -> {
            if (Files.isRegularFile(file) && file.getFileName().toString().toLowerCase().endsWith(".mus") &&
                !inAvailableList(file)) {
              try {
                return new MusicResourceEntry(file, isPathAsPrefix());
              } catch (Exception e) {
                Logger.trace(e);
              }
            }
            return null;
          })
          .filter(Objects::nonNull)
          .sorted((item1, item2) -> item1.getResourceName().compareToIgnoreCase(item2.getResourceName()))
          .collect(Collectors.toList());

      for (final MusicResourceEntry mre : fileList) {
        availableListModel.add(mre);
        if (isAutoCalcDurationsEnabled()) {
          mre.calculateDuration();
        }
      }
      return fileList.size();
    }
  }

  private boolean removeFromAvailableList(int index) {
    if (!availableList.isEnabled() || index < 0 || index >= availableListModel.size()) {
      return false;
    }

    int curSelected = availableList.getSelectedIndex();
    if (index == curSelected) {
      if (curSelected < availableListModel.size() - 1) {
        availableList.setSelectedIndex(curSelected + 1);
      } else {
        availableList.setSelectedIndex(curSelected - 1);
      }
    }

    final MusicResourceEntry mre = availableListModel.get(index);
    removeMusEntry(mre);

    updateAvailableListButtons();
    updateAvailableListTitle();
    updatePlayListTitle();
    return true;
  }

  /** Returns {@code true} if the specified MUS file path is already present in the list of available MUS entries. */
  private boolean inAvailableList(Path musFile) {
    boolean retVal = false;
    if (musFile == null) {
      return retVal;
    }

    for (int i = 0, size = availableListModel.size(); i < size; i++) {
      final MusicResourceEntry mre = availableListModel.get(i);
      if (musFile.equals(mre.getActualPath())) {
        retVal = true;
        break;
      }
    }

    return retVal;
  }

  /** Removes the specified {@link MusicResourceEntry} instance from both lists. */
  private boolean removeMusEntry(MusicResourceEntry entry) {
    boolean retVal = false;

    if (entry != null) {
      retVal = availableListModel.remove(entry);
      if (retVal) {
        playListModel.remove(entry);
      }
    }

    return retVal;
  }

  /** Allows the user to choose a M3U playlist path to export the current music list. */
  private boolean exportPlayListInteractive() throws Exception {
    final JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
    for (final FileFilter ff : fc.getChoosableFileFilters()) {
      fc.removeChoosableFileFilter(ff);
    }
    final FileFilter m3uFilter = new FileNameExtensionFilter("M3U files (*.m3u)", "m3u");
    final FileFilter m3u8Filter = new FileNameExtensionFilter("M3U8 files (*.m3u8)", "m3u8");
    fc.addChoosableFileFilter(m3uFilter);
    fc.addChoosableFileFilter(m3u8Filter);
    fc.setFileFilter(m3u8Filter);

    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setMultiSelectionEnabled(false);
    fc.setDialogTitle("Export playlist");
    final int retVal = fc.showSaveDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      Path file = fc.getSelectedFile().toPath();
      final String fileName = file.getFileName().toString();
      final String ext = (fc.getFileFilter() == m3uFilter) ? "m3u" : "m3u8";
      final Charset ch = (fc.getFileFilter() == m3uFilter) ? StandardCharsets.ISO_8859_1 : StandardCharsets.UTF_8;
      if (fileName.lastIndexOf('.') < 0) {
        file = file.getParent().resolve(fileName + "." + ext);
      }
      exportPlayList(file, ch);
      return true;
    }
    return false;
  }

  /**
   * Exports MUS entries to the specified playlist file.
   *
   * @param playlist {@link Path} of the M3U playlist file to export.
   * @param cs The character set to use when encoding playlist content.
   * @throws Exception if the playlist could not be exported.
   */
  private void exportPlayList(Path playlist, Charset cs) throws Exception {
    final Playlist pl = new Playlist();
    for (int i = 0, size = availableListModel.size(); i < size; i++) {
      final MusicResourceEntry mre = availableListModel.get(i);
      final int length = mre.isDurationAvailable() ? (int) mre.getDuration() : -1;
      pl.addEntry(mre.getActualPath(), length);
    }
    pl.exportFile(playlist, cs);
  }

  /** Allows the user to choose a M3U playlist that will be imported to the player. */
  private boolean importPlayListInteractive() throws Exception {
    final JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
    final FileFilter m3uFilter = new FileNameExtensionFilter("Playlist files (*.m3u, *.m3u8)", "m3u", "m3u8");
    fc.addChoosableFileFilter(m3uFilter);
    fc.setFileFilter(m3uFilter);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setMultiSelectionEnabled(false);
    fc.setDialogTitle("Import playlist");
    final int retVal = fc.showOpenDialog(this);
    if (retVal == JFileChooser.APPROVE_OPTION) {
      importPlayList(fc.getSelectedFile().toPath());
      return true;
    }

    return false;
  }

  /** Imports MUS files from the specified M3U playlist. Returns number of imported song entries. */
  private int importPlayList(Path playlist) throws Exception {
    int retVal;

    final Playlist pl = new Playlist(playlist);
    playListModel.clear();
    availableListModel.clear();
    for (int i = 0; i < pl.getEntriesCount(); i++) {
      final int length = pl.getEntryLength(i);
      String pathString = pl.getEntryPath(i).toString();
      if (length >= 0) {
        pathString += ";" + length;
      }
      final MusicResourceEntry mre = getMusicResource(pathString, isPathAsPrefix());
      if (mre != null) {
        availableListModel.add(mre);
        if (isAutoCalcDurationsEnabled()) {
          mre.calculateDuration();
        }
      }
    }
    if (!availableListModel.isEmpty()) {
      availableList.setSelectedIndex(0);
      availableList.ensureIndexIsVisible(0);
    }

    retVal = pl.getEntriesCount();

    return retVal;
  }

  /**
   * Returns whether the specified MUS sound segment should be skipped (i.e. excluded from playback).
   *
   * @param musEntry {@link Entry} to test.
   * @return {@code true} if {@code musEntry} is listed in the exclusion list or the argument is {@code null}.
   *         {@code false} otherwise.
   */
  private boolean isSoundExcluded(Entry musEntry) {
    return (musEntry == null) || exclusionFilter.contains(musEntry.getName());
  }

  /** Forces the specified resource entry to be updated in available music list and playlist. */
  private void refreshResourceEntry(MusicResourceEntry entry) {
    if (entry != null) {
      for (int i = 0, size = availableListModel.size(); i < size; i++) {
        if (entry.equals(availableListModel.get(i))) {
          final MusicResourceEntry mre = availableListModel.get(i);
          availableListModel.set(i, mre);
          break;
        }
      }
      for (int i = 0, size = playListModel.size(); i < size; i++) {
        if (entry.equals(playListModel.get(i))) {
          final MusicResourceEntry mre = playListModel.get(i);
          playListModel.set(i, mre);
          break;
        }
      }
    }
  }

  /** Updates the item transfer buttons based on the current state of available music list and playlist. */
  private void updateTransferButtons() {
    if (isPlaying()) {
      toPlayListButton.setEnabled(false);
      fromPlayListButton.setEnabled(false);
      moveUpButton.setEnabled(false);
      moveDownButton.setEnabled(false);
    } else if (!isNextItemRequested()) {
      toPlayListButton.setEnabled(!availableList.getSelectionModel().isSelectionEmpty());
      fromPlayListButton.setEnabled(!playList.getSelectionModel().isSelectionEmpty());

      final int[] indices = playList.getSelectedIndices();
      if (indices.length == 0) {
        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);
      } else {
        moveUpButton.setEnabled(indices[0] > 0);
        moveDownButton.setEnabled(indices[indices.length - 1] < playList.getModel().getSize() - 1);
      }
    }
  }

  /** Updates the item management buttons for the available music list. */
  private void updateAvailableListButtons() {
    bpmAddMusic.setEnabled(!isPlaying());
    removeMusicButton.setEnabled(!isPlaying() && !availableList.getSelectionModel().isSelectionEmpty());
    clearMusicButton.setEnabled(!isPlaying() && !availableListModel.isEmpty());
    importPlayListButton.setEnabled(!isPlaying());
    exportPlayListButton.setEnabled(!availableListModel.isEmpty());
  }

  /** Updates the available music list title. */
  private void updateAvailableListTitle() {
    if (availableListModel.isEmpty()) {
      availableListTitleLabel.setText(TITLE_AVAILABLE_LIST);
    } else {
      availableListTitleLabel.setText(String.format(TITLE_AVAILABLE_LIST_FMT, availableListModel.size()));
    }
  }

  /** Updates the media buttons for the playlist. */
  private void updatePlayListButtons() {
    if (!isPlayerAvailable()) {
      prevMusicButton.setEnabled(false);
      nextMusicButton.setEnabled(false);
      playMusicButton.setEnabled(false);
      stopMusicButton.setEnabled(false);
      return;
    }

    final int[] indices = playList.getSelectedIndices();
    if (playListModel.isEmpty() || indices.length == 0) {
      prevMusicButton.setEnabled(false);
      nextMusicButton.setEnabled(false);
      playMusicButton.setEnabled(!playListModel.isEmpty());
      stopMusicButton.setEnabled(false);
    } else {
      prevMusicButton.setEnabled(isLoopEnabled() || isShuffleEnabled() || indices[0] > 0);
      nextMusicButton.setEnabled(isLoopEnabled() || isShuffleEnabled() || indices[0] < playListModel.size() - 1);
      playMusicButton.setEnabled(true);
      if (isPlaying() && !isPaused()) {
        playMusicButton.setIcon(Icons.ICON_PAUSE_16.getIcon());
      } else {
        playMusicButton.setIcon(Icons.ICON_PLAY_16.getIcon());
      }
      stopMusicButton.setEnabled(isPlaying());
    }
  }

  /** Updates the playlist title. */
  private void updatePlayListTitle() {
    if (playListModel.isEmpty()) {
      playListTitleLabel.setText(TITLE_PLAY_LIST);
    } else {
      playListTitleLabel.setText(String.format(TITLE_PLAY_LIST_FMT, playListModel.size()));
    }
  }

  /** Updates the MUS file info field depending on current list selection. */
  private void updateFileInfo() {
    final int[] indices = availableList.getSelectedIndices();
    if (indices.length == 1 && indices[0] >= 0) {
      final MusicResourceEntry mre = availableListModel.get(indices[0]);
      tfMusicPath.setText(mre.getActualPath().toString());
    } else {
      tfMusicPath.setText("");
    }
  }

  /**
   * Updates the display for elapsed and total playing time.
   *
   * @param resourceEntry {@link MusicResourceEntry} to be played back. Specify {@code null} to reset the time display.
   */
  private void updateTimeDisplay(MusicResourceEntry resourceEntry) {
    if (resourceEntry != null) {
      final int elapsed = (int) StopWatch.toSeconds(timer.elapsed());
      final int elapsedMin = elapsed / 60;
      final int elapsedSec = elapsed % 60;
      if (resourceEntry.isDurationAvailable()) {
        final int duration = resourceEntry.getDurationSeconds();
        final int totalMin = duration / 60;
        final int totalSec = duration % 60;
        elapsedTimeLabel.setText(String.format(PLAYTIME_FMT, elapsedMin, elapsedSec, totalMin, totalSec));
      } else {
        elapsedTimeLabel.setText(String.format(PLAYTIME_SHORT_FMT, elapsedMin, elapsedSec));
      }
    } else {
      // default display
      elapsedTimeLabel.setText(String.format(PLAYTIME_FMT, 0, 0, 0, 0));
    }
  }

  /** Updates the window title. Displays currently playing title if available. */
  private void updateWindowTitle() {
    String title = TITLE_DEFAULT;

    if (isPlaying()) {
      int index = playList.getSelectedIndex();
      if (index >= 0 && index < playListModel.size()) {
        final MusicResourceEntry mre = playListModel.get(index);
        title = String.format(TITLE_FMT, mre.getResourceName(), mre.getLocation());
      }
    }

    setTitle(title);
  }

  /**
   * Used internally to handle UI-related updates depending on the specified playback state.
   *
   * @param playing Specify {@code true} if audio playback started, and {@code false} if playback stopped.
   */
  private void onAudioPlaying(boolean playing) {
    if (playing) {
      timer.reset();
      timer.resume();
      availableList.setEnabled(false);
      playList.setEnabled(false);
      elapsedTimeLabel.setEnabled(true);
      updateAvailableListButtons();
      updatePlayListButtons();
      updateTransferButtons();
      updateTimeDisplay(playList.getSelectedValue());
      updateWindowTitle();
    } else {
      timer.pause();
      timer.reset();
      availableList.setEnabled(true);
      playList.setEnabled(true);
      elapsedTimeLabel.setEnabled(false);
      updateWindowTitle();
      updateTimeDisplay(null);
      updateAvailableListButtons();
      updatePlayListButtons();
      updateTransferButtons();
    }
  }

  /** Called when the audio player triggers an {@code OPEN} event. */
  private void handleAudioOpenEvent(Object value) {
    // nothing to do
  }

  /** Called when the audio player triggers a {@code CLOSE} event. */
  private void handleAudioCloseEvent(Object value) {
    // nothing to do
  }

  /** Called when the audio player triggers a {@code START} event. */
  private void handleAudioStartEvent() {
    onAudioPlaying(true);
  }

  /** Called when the audio player triggers a {@code STOP} event. */
  private void handleAudioStopEvent() {
    if (musHandler != null) {
      try {
        musHandler.close();
      } catch (Exception e) {
        Logger.error(e);
      }
      musHandler = null;
    }

    if (isNextItemRequested()) {
      acceptNextItem();
    } else {
      onAudioPlaying(false);
    }
  }

  /** Called when the audio player triggers a {@code PAUSE} event. */
  private void handleAudioPauseEvent(Object value) {
    timer.pause();
    updatePlayListButtons();
  }

  /** Called when the audio player triggers a {@code RESUME} event. */
  private void handleAudioResumeEvent(Object value) {
    timer.resume();
    updatePlayListButtons();
  }

  /** Called when the audio player triggers a {@code BUFFER_EMPTY} event. */
  private void handleAudioBufferEmptyEvent(Object value) {
    if (musHandler == null) {
      // loading currently selected MUS resource
      int index = setCurrentPlayListItem();
      if (index < 0) {
        setPlaying(false);
        return;
      }

      try {
        musHandler = new MusResourceHandler(playListModel.get(index), 0, true, false);
      } catch (Exception e) {
        handleAudioErrorEvent(e);
        return;
      }
    }

    boolean advanced;
    for (advanced = musHandler.advance(); advanced; advanced = musHandler.advance()) {
      if (!isSoundExcluded(musHandler.getCurrentEntry())) {
        player.addAudioBuffer(musHandler.getAudioBuffer());
        break;
      }
    }

    if (!advanced) {
      if (setNextPlayListItem() >= 0) {
        requestNextItem();
      } else {
        setPlaying(false);
      }
    }
  }

  /** Called when the audio player triggers an {@code ERROR} event. */
  private void handleAudioErrorEvent(Object value) {
    requestNextItem = false;
    setPlaying(false);
    onAudioPlaying(false);

    final Exception e = (value instanceof Exception) ? (Exception)value : null;
    if (e != null) {
      Logger.error(e);
    }

    final String msg = (e != null) ? "Error during playback:\n" + e.getMessage() : "Error during playback.";
    JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
  }

  private void init() {
    availableList.setCellRenderer(availableListRenderer);
    playList.setCellRenderer(playListRenderer);

    loadPreferences();

    addComponentListener(listeners);

    availableList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    availableList.addListSelectionListener(listeners);
    availableList.addMouseListener(listeners);
    availableList.addKeyListener(listeners);
    if (!availableListModel.isEmpty()) {
      availableList.setSelectedIndex(0);
      availableList.ensureIndexIsVisible(0);
    }
    final JScrollPane availableScroll = new JScrollPane(availableList);
    availableScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    availableScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    playList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    playList.addListSelectionListener(listeners);
    playList.addMouseListener(listeners);
    playList.addKeyListener(listeners);
    if (!playListModel.isEmpty()) {
      playList.setSelectedIndex(0);
      playList.ensureIndexIsVisible(0);
    }
    final JScrollPane playlistScroll = new JScrollPane(playList);
    playlistScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
    playlistScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

    toPlayListButton.addActionListener(listeners);
    fromPlayListButton.addActionListener(listeners);
    moveUpButton.addActionListener(listeners);
    moveDownButton.addActionListener(listeners);

    bpmAddMusic.setIcon(Icons.ICON_ARROW_UP_15.getIcon());
    miAddMusicCurrent.addActionListener(listeners);
    miAddMusicExternal.addActionListener(listeners);
    bpmAddMusic.addItem(miAddMusicCurrent);
    bpmAddMusic.addItem(miAddMusicExternal);
    removeMusicButton.addActionListener(listeners);
    clearMusicButton.addActionListener(listeners);

    if (isPlayerAvailable()) {
      prevMusicButton.addActionListener(listeners);
      playMusicButton.addActionListener(listeners);
      stopMusicButton.addActionListener(listeners);
      nextMusicButton.addActionListener(listeners);
    }

    importPlayListButton.setToolTipText("Import music entries from M3U playlist file.");
    importPlayListButton.addActionListener(listeners);
    exportPlayListButton.setToolTipText("Export music entries to M3U playlist file.");
    exportPlayListButton.addActionListener(listeners);

    cbLoop.addActionListener(listeners);
    cbShuffle.addActionListener(listeners);

    tfMusicPath.setEditable(false);

    final Font monoFont = new Font(Font.MONOSPACED, Font.BOLD, elapsedTimeLabel.getFont().getSize() + 1);
    elapsedTimeLabel.setFont(monoFont);
    elapsedTimeLabel.setEnabled(false);
    updateTimeDisplay(null);
    timer.addActionListener(listeners);

    final JMenu viewMenu = new JMenu("Item View");
    viewMenu.setToolTipText("Display format of list items.");
    viewMenu.add(rbmiPathAsPrefix);
    viewMenu.add(rbmiPathAsSuffix);

    ButtonGroup bg = new ButtonGroup();
    bg.add(rbmiPathAsPrefix);
    bg.add(rbmiPathAsSuffix);
    rbmiPathAsPrefix.addItemListener(listeners);
    rbmiPathAsSuffix.addItemListener(listeners);

    final JMenu filterMenu = new JMenu("Sound filter");
    filterMenu.setToolTipText("Sound segments to auto-skip at playback.");
    filterMenu.add(cbmiEnableExclusionFilter);
    filterMenu.add(miDefineExclusionFilter);
    miDefineExclusionFilter.addActionListener(listeners);

    cbmiCalcDurations.addActionListener(listeners);
    cbmiCalcDurations.setToolTipText("Enable to calculate total duration of listed music entries. (Operation may take a while.)");

    // currently only supported on Windows platforms
    cbmiUseMediaKeys.setEnabled(InputKeyHelper.isEnabled());
    if (cbmiUseMediaKeys.isEnabled()) {
      cbmiUseMediaKeys.setToolTipText("Enable to use multimedia keys for music playback controls while the player window is open.");
    } else {
      cbmiUseMediaKeys.setToolTipText("Support for multimedia keys is not available on this platform.");
    }

    final ButtonPopupMenu bpmOptions = new ButtonPopupMenu("Options");
    bpmOptions.setIcon(Icons.ICON_ARROW_DOWN_15.getIcon());
    bpmOptions.setMenuAlignment(Align.BOTTOM);
    bpmOptions.addItem(viewMenu);
    bpmOptions.addItem(filterMenu);
    bpmOptions.addItem(cbmiCalcDurations);
    bpmOptions.addItem(cbmiUseMediaKeys);

    final GridBagConstraints gbc = new GridBagConstraints();

    for (final JButton button : new JButton[] { toPlayListButton, fromPlayListButton, moveUpButton, moveDownButton }) {
      final Insets insets = button.getMargin();
      insets.top += 4;
      insets.bottom += 4;
      button.setMargin(insets);
    }

    // buttons in vertical middle bar
    final JPanel middleButtonsPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    middleButtonsPanel.add(bpmOptions, gbc);
    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(24, 0, 0, 0), 0, 0);
    middleButtonsPanel.add(toPlayListButton, gbc);
    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    middleButtonsPanel.add(fromPlayListButton, gbc);
    ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(24, 0, 0, 0), 0, 0);
    middleButtonsPanel.add(moveUpButton, gbc);
    ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    middleButtonsPanel.add(moveDownButton, gbc);

    // available music buttons
    final JPanel availableButtonPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    availableButtonPanel.add(bpmAddMusic, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    availableButtonPanel.add(removeMusicButton, gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    availableButtonPanel.add(clearMusicButton, gbc);

    // playlist buttons
    final JPanel playlistButtonPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    playlistButtonPanel.add(prevMusicButton, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    playlistButtonPanel.add(playMusicButton, gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    playlistButtonPanel.add(stopMusicButton, gbc);
    ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    playlistButtonPanel.add(nextMusicButton, gbc);

    // playlist elapsed time
    final JPanel playlistElapsedPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    final JLabel lElapsedLabel = new JLabel("Elapsed time:", SwingConstants.RIGHT);
    playlistElapsedPanel.add(lElapsedLabel, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 8, 0, 0), 0, 0);
    playlistElapsedPanel.add(elapsedTimeLabel, gbc);

    // available music file info
    final JPanel availableInfoPanel = new JPanel(new GridBagLayout());
    final JLabel lInfo = new JLabel("File:");
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    availableInfoPanel.add(lInfo, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 8, 0, 0), 0, 0);
    availableInfoPanel.add(tfMusicPath, gbc);

    // music file import/export buttons
    final JPanel availableImportExportPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    availableImportExportPanel.add(importPlayListButton, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    availableImportExportPanel.add(exportPlayListButton, gbc);

    // playlist options
    final JPanel playlistOptionsPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    playlistOptionsPanel.add(cbLoop, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 16, 0, 0), 0, 0);
    playlistOptionsPanel.add(cbShuffle, gbc);

    // main panel
    final JPanel mainPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(availableListTitleLabel, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(playListTitleLabel, gbc);

    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(availableScroll, gbc);
    ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.VERTICAL,
        new Insets(8, 8, 0, 8), 0, 0);
    mainPanel.add(middleButtonsPanel, gbc);
    ViewerUtil.setGBC(gbc, 2, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_END, GridBagConstraints.BOTH,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(playlistScroll, gbc);

    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    mainPanel.add(availableInfoPanel, gbc);
    ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);
    ViewerUtil.setGBC(gbc, 2, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    mainPanel.add(playlistElapsedPanel, gbc);

    ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(availableButtonPanel, gbc);
    ViewerUtil.setGBC(gbc, 1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);
    ViewerUtil.setGBC(gbc, 2, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(playlistButtonPanel, gbc);

    ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(availableImportExportPanel, gbc);
    ViewerUtil.setGBC(gbc, 1, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);
    ViewerUtil.setGBC(gbc, 2, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(playlistOptionsPanel, gbc);

    // content pane of the frame
    final Container pane = getContentPane();
    pane.setLayout(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(8, 8, 8, 8), 0, 0);
    pane.add(mainPanel, gbc);

    pack();
    setMinimumSize(getSize());

    // restoring window size and position
    final Rectangle bounds = loadWindowBounds();
    bounds.x = Math.max(0, bounds.x);
    bounds.y = Math.max(0, bounds.y);
    bounds.width = (bounds.width == 0) ? Misc.getScaledValue(650) : Math.max(getMinimumSize().width, bounds.width);
    bounds.height = (bounds.height == 0) ? Misc.getScaledValue(450) : Math.max(getMinimumSize().height, bounds.height);
    setBounds(bounds);

    updateAvailableListButtons();
    updatePlayListButtons();
    updateTransferButtons();
    updateAvailableListTitle();
    updatePlayListTitle();

    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(listeners::dispatchKeyEvent);

    setIconImage(Icons.ICON_MUSIC_16.getIcon().getImage());
  }

  /**
   * Returns a new and fully initialized {@link StreamingAudioPlayer} instance.
   *
   * @param showError Whether to show an error message box if the player instance could not be created.
   * @return {@link StreamingAudioPlayer} instance if successful, {@code null} otherwise.
   */
  private StreamingAudioPlayer initPlayer(boolean showError) {
    try {
      return new StreamingAudioPlayer(listeners);
    } catch (Exception e) {
      Logger.error(e);
      if (showError) {
        JOptionPane.showMessageDialog(NearInfinity.getInstance(),
            "Failed to initialize audio backend:\n" + e.getMessage() + "\n\nPlayback will be disabled.",
            "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
    return null;
  }

  /**
   * Returns whether the global {@link StreamingAudioPlayer} instance is available.
   *
   * @return {@code true} if audio player instance is available, {@code false} otherwise.
   */
  private boolean isPlayerAvailable() {
    return Objects.nonNull(player);
  }

  /** Closes the {@link StreamingAudioPlayer} instance. */
  private void closePlayer() {
    if (!isPlayerAvailable()) {
      return;
    }

    try {
      player.close();
    } catch (Exception e) {
      Logger.error(e);
    }
  }

  /**
   * Loads the window position and size from the preferences.
   *
   * @return {@link Rectangle} instances with position relative to the parent and size of the window.
   */
  private Rectangle loadWindowBounds() {
    final Rectangle retVal = new Rectangle();

    final Preferences prefs = getPreferences();
    retVal.x = prefs.getInt(PREFS_WINDOW_X, 0);
    retVal.y = prefs.getInt(PREFS_WINDOW_Y, 0);
    retVal.width = prefs.getInt(PREFS_WINDOW_WIDTH, 0);
    retVal.height = prefs.getInt(PREFS_WINDOW_HEIGHT, 0);

    return retVal;
  }

  /** Loads the InfinityAmp configuration from the preferences. */
  private void loadPreferences() {
    final Preferences prefs = getPreferences();

    // restoring playback options
    cbLoop.setSelected(prefs.getBoolean(PREFS_OPTION_LOOP, false));
    cbShuffle.setSelected(prefs.getBoolean(PREFS_OPTION_SHUFFLE, false));

    cbmiCalcDurations.setSelected(prefs.getBoolean(PREFS_OPTION_CALC_DURATIONS, true));
    cbmiUseMediaKeys.setSelected(prefs.getBoolean(PREFS_OPTION_USE_MEDIA_KEYS, true) && InputKeyHelper.isEnabled());

    // restoring list view options
    if (prefs.getBoolean(PREFS_VIEW_PREFIX, true)) {
      rbmiPathAsPrefix.setSelected(true);
    } else {
      rbmiPathAsSuffix.setSelected(true);
    }

    // restoring sound exclusion filter
    exclusionFilter.clear();
    final int numSounds = prefs.getInt(PREFS_FILTER_SOUND_COUNT, -1);
    if (numSounds < 0) {
      exclusionFilter.addAll(SoundFilterDialog.getDefaultSoundFilters());
    } else {
      for (int i = 0; i < numSounds; i++) {
        String sound = prefs.get(PREFS_FILTER_SOUND_BASE + i, "").trim();

        // don't include file extension
        final int sep = sound.indexOf('.');
        if (sep >= 0) {
          sound = sound.substring(0, sep);
        }

        if (!sound.isEmpty() && sound.length() <= 8) {
          exclusionFilter.add(sound);
        }
      }
    }
    cbmiEnableExclusionFilter.setSelected(prefs.getBoolean(PREFS_FILTER_SOUND_ENABLED, true));

    // restoring available music entries
    availableListModel.clear();
    final int numAvailable = prefs.getInt(PREFS_AVAILABLE_ENTRIES_COUNT, 0);
    for (int i = 0; i < numAvailable; i++) {
      final MusicResourceEntry mre = getMusicResource(prefs.get(PREFS_AVAILABLE_ENTRY_BASE + i, null),
          rbmiPathAsPrefix.isSelected());
      if (mre != null) {
        availableListModel.add(mre);
        if (isAutoCalcDurationsEnabled()) {
          mre.calculateDuration();
        }
      }
    }

    // restoring playlist entries
    playListModel.clear();
    final int numPlayList = prefs.getInt(PREFS_PLAYLIST_INDEX_COUNT, 0);
    for (int i = 0; i < numPlayList; i++) {
      final int index = prefs.getInt(PREFS_PLAYLIST_INDEX_BASE + i, -1);
      if (index >= 0 && index < availableListModel.size()) {
        playListModel.add(availableListModel.get(index));
      }
    }

    // restoring selected playlist entry
    if (!playListModel.isEmpty()) {
      final int playListIndex =
          Math.max(0, Math.min(playListModel.size() - 1, prefs.getInt(PREFS_PLAYLIST_SELECTED_INDEX, 0)));
      SwingUtilities.invokeLater(() -> {
        playList.setSelectedIndex(playListIndex);
        playList.ensureIndexIsVisible(playListIndex);
      });
    }
  }

  /** Saves the current configuration to the preferences. */
  private void savePreferences() {
    final Preferences prefs = getPreferences();

    final Rectangle bounds = getBounds();
    prefs.putInt(PREFS_WINDOW_X, bounds.x);
    prefs.putInt(PREFS_WINDOW_Y, bounds.y);
    prefs.putInt(PREFS_WINDOW_WIDTH, bounds.width);
    prefs.putInt(PREFS_WINDOW_HEIGHT, bounds.height);

    // clearing old available music entries
    final int oldNumAvailable = prefs.getInt(PREFS_AVAILABLE_ENTRIES_COUNT, 0);
    for (int i = 0; i < oldNumAvailable; i++) {
      prefs.remove(PREFS_AVAILABLE_ENTRY_BASE + i);
    }
    prefs.putInt(PREFS_AVAILABLE_ENTRIES_COUNT, 0);

    // storing current available music entries
    final int numAvailable = availableListModel.size();
    for (int i = 0; i < numAvailable; i++) {
      final MusicResourceEntry mre = availableListModel.get(i);
      String entry = mre.getActualPath().toString();
      // optional duration (ms) can be found after the file path
      if (mre.isDurationAvailable()) {
        entry += ";" + mre.getDuration();
      }
      prefs.put(PREFS_AVAILABLE_ENTRY_BASE + i, entry);
    }
    prefs.putInt(PREFS_AVAILABLE_ENTRIES_COUNT, numAvailable);

    // clearing old playlist entries
    final int oldNumPlayList = prefs.getInt(PREFS_PLAYLIST_INDEX_COUNT, 0);
    for (int i = 0; i < oldNumPlayList; i++) {
      prefs.remove(PREFS_PLAYLIST_INDEX_BASE + i);
    }
    prefs.putInt(PREFS_PLAYLIST_INDEX_COUNT, 0);

    // storing current playlist entries
    int numPlayList = 0;
    for (int i = 0, size = playListModel.size(); i < size; i++) {
      final int index = getAvailableListIndex(playListModel.get(i));
      if (index >= 0) {
        prefs.putInt(PREFS_PLAYLIST_INDEX_BASE + numPlayList, index);
        numPlayList++;
      }
    }
    prefs.putInt(PREFS_PLAYLIST_INDEX_COUNT, numPlayList);

    // storing selected playlist entry
    final int playListIndex = playList.getSelectedIndex();
    prefs.putInt(PREFS_PLAYLIST_SELECTED_INDEX, playListIndex);

    // clearing old sound exclusion filter
    final int oldNumSounds = prefs.getInt(PREFS_FILTER_SOUND_COUNT, 0);
    for (int i = 0; i < oldNumSounds ; i++) {
      prefs.remove(PREFS_FILTER_SOUND_BASE + i);
    }
    prefs.putInt(PREFS_FILTER_SOUND_COUNT, 0);

    // storing current sound exclusion entries
    int numSounds = 0;
    for (final String sound : exclusionFilter) {
      if (!sound.isEmpty()) {
        prefs.put(PREFS_FILTER_SOUND_BASE + numSounds, sound);
        numSounds++;
      }
    }
    prefs.putInt(PREFS_FILTER_SOUND_COUNT, numSounds);
    prefs.putBoolean(PREFS_FILTER_SOUND_ENABLED, cbmiEnableExclusionFilter.isSelected());

    prefs.putBoolean(PREFS_OPTION_CALC_DURATIONS, cbmiCalcDurations.isSelected());
    prefs.putBoolean(PREFS_OPTION_USE_MEDIA_KEYS, cbmiUseMediaKeys.isSelected());

    // storing playback options
    prefs.putBoolean(PREFS_OPTION_LOOP, cbLoop.isSelected());
    prefs.putBoolean(PREFS_OPTION_SHUFFLE, cbShuffle.isSelected());

    // list view options
    prefs.putBoolean(PREFS_VIEW_PREFIX, rbmiPathAsPrefix.isSelected());
  }

  /** Returns the {@link Preferences} instance for InfinityAmp. */
  private static Preferences getPreferences() {
    final Preferences prefsRoot = Preferences.userNodeForPackage(InfinityAmpPlus.class);
    return prefsRoot.node(PREFS_NODE);
  }

  /**
   * Returns a {@link MusicResourceEntry} object for the specified MUS file.
   *
   * @param musicFilePath {@link Path} to the MUS file. The path string can optionally be appended by the music duration
   *                        in milliseconds, separated by semicolon.
   * @return A {@link MusicResourceEntry} object for the MUS file. Returns {@code null} if not available.
   */
  private static MusicResourceEntry getMusicResource(String musicFilePath, boolean pathAsPrefix) {
    MusicResourceEntry retVal = null;

    if (musicFilePath != null) {
      final String[] items = musicFilePath.split(";");
      if (items.length > 0) {
        final Path musicPath = Paths.get(items[0]);
        long duration = -1L;
        if (items.length > 1) {
          try {
            duration = Long.parseLong(items[1]);
          } catch (NumberFormatException e) {
            Logger.debug(e);
          }
        }
        if (Files.isRegularFile(musicPath)) {
          try {
            retVal = new MusicResourceEntry(musicPath, pathAsPrefix);
            if (duration >= 0) {
              retVal.setDuration(duration);
            }
          } catch (Exception e) {
            Logger.error(e);
          }
        }
      }
    }

    return retVal;
  }

  // -------------------------- INNER CLASSES --------------------------

  private class Listeners implements ActionListener, ListSelectionListener, ItemListener, AudioStateListener,
      MouseListener, KeyListener, ComponentListener {
    public Listeners() {
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == timer) {
        // Playback time
        updateTimeDisplay(playList.getSelectedValue());
      } else if (e.getSource() == toPlayListButton) {
        // Move item from available music list to playlist
        final int[] indices = availableList.getSelectedIndices();
        for (final int index : indices) {
          addToPlayList(index);
        }
      } else if (e.getSource() == fromPlayListButton) {
        // remove item from playlist
        final int[] indices = playList.getSelectedIndices();
        for (int i = indices.length - 1; i >= 0; i--) {
          removeFromPlayList(indices[i]);
        }
      } else if (e.getSource() == moveUpButton) {
        // move playlist item up by one position
        moveItemsUp();
      } else if (e.getSource() == moveDownButton) {
        // move playlist item down by one position
        moveItemsDown();
      } else if (e.getSource() == miAddMusicCurrent) {
        // add music of currently open game to the available music list
        addMusFilesInteractive(Profile.getGameRoot());
        updateAvailableListTitle();
      } else if (e.getSource() == miAddMusicExternal) {
        // add music from a selected directory to the available music list
        final JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select game folder");
        fc.setCurrentDirectory(Profile.getGameRoot().toFile());
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (fc.showOpenDialog(InfinityAmpPlus.this) == JFileChooser.APPROVE_OPTION) {
          final Path folder = fc.getSelectedFile().toPath();
          addMusFilesInteractive(folder);
        }
        updateAvailableListTitle();
      } else if (e.getSource() == removeMusicButton) {
        // remove selected items from the available music list
        final int[] indices = availableList.getSelectedIndices();
        if (indices.length > 0) {
          for (int i = indices.length - 1; i >= 0; i--) {
            removeFromAvailableList(indices[i]);
          }
        }
      } else if (e.getSource() == clearMusicButton) {
        // remove all items from the available music list
        int retVal = JOptionPane.showConfirmDialog(InfinityAmpPlus.this, "Remove all MUS entries?", "Question",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (retVal == JOptionPane.YES_OPTION) {
          playListModel.clear();
          availableListModel.clear();
        }
        updateTransferButtons();
        updateAvailableListButtons();
        updatePlayListButtons();
        updateAvailableListTitle();
        updatePlayListTitle();
      } else if (e.getSource() == prevMusicButton) {
        // jump to previously played music file
        if (setPrevPlayListItem() >= 0) {
          requestNextItem();
        }
        updateTransferButtons();
      } else if (e.getSource() == nextMusicButton) {
        // jump to next music file to play
        if (setNextPlayListItem() >= 0) {
          requestNextItem();
        }
        updateTransferButtons();
      } else if (e.getSource() == playMusicButton) {
        // play music from the playlist
        if (isPlaying()) {
          setPaused(!isPaused());
        } else {
          setPlaying(true);
        }
      } else if (e.getSource() == stopMusicButton) {
        // stop music playback
        setPlaying(false);
      } else if (e.getSource() == cbLoop) {
        // Loop playback option has changed
        updatePlayListButtons();
      } else if (e.getSource() == cbmiCalcDurations) {
        // Shuffle playback option has changed
        if (cbmiCalcDurations.isSelected()) {
          calculateDurations();
        }
      } else if (e.getSource() == miDefineExclusionFilter) {
        // Manage list of sound exclusion filters
        final Collection<String> filters =
            SoundFilterDialog.getSoundFiltersInteractive(InfinityAmpPlus.this, getExclusionFilters());
        if (filters != null) {
          setExclusionFilters(filters);
        }
      } else if (e.getSource() == importPlayListButton) {
        // Import M3U playlist file
        try {
          if (importPlayListInteractive()) {
            updateAvailableListButtons();
            updatePlayListButtons();
            updateAvailableListTitle();
            updatePlayListTitle();
          }
        } catch (Exception ex) {
          Logger.error(ex);
          JOptionPane.showMessageDialog(InfinityAmpPlus.this, "Failed to import playlist.\n" + ex.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      } else if (e.getSource() == exportPlayListButton) {
        // Export current available music to M3U playlist file
        try {
          if (exportPlayListInteractive()) {
            updateAvailableListButtons();
            updatePlayListButtons();
            updateAvailableListTitle();
            updatePlayListTitle();
            JOptionPane.showMessageDialog(InfinityAmpPlus.this, "Playlist exported successfully.", "Information",
                JOptionPane.INFORMATION_MESSAGE);
          }
        } catch (Exception ex) {
          Logger.error(ex);
          JOptionPane.showMessageDialog(InfinityAmpPlus.this, "Failed to export playlist.\n" + ex.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
      if (e.getSource() == availableList) {
        // update file info
        updateFileInfo();
        // update middle bar buttons
        updateTransferButtons();
        // update bottom bar buttons
        updateAvailableListButtons();
      } else if (e.getSource() == playList) {
        // update middle bar buttons
        updateTransferButtons();
        // update media buttons
        updatePlayListButtons();
      }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getSource() == rbmiPathAsPrefix || e.getSource() == rbmiPathAsSuffix) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          updatePathAsPrefix(rbmiPathAsPrefix.isSelected());
        }
      }
    }

    @Override
    public void audioStateChanged(AudioStateEvent event) {
//      Logger.trace("{}.audioStateChanged: state={}({})", InfinityAmpPlus.class.getSimpleName(), event.getAudioState(),
//          event.getValue());
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

    @Override
    public void mouseClicked(MouseEvent e) {
      if (e.getSource() == availableList) {
        if (e.getClickCount() == 2) {
          addToPlayList(availableList.getSelectedIndex());
        }
      } else if (e.getSource() == playList) {
        if (!isPlaying() && e.getClickCount() == 2) {
          setPlaying(true);
        }
      }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
      if (e.getSource() == availableList) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE && !availableList.isSelectionEmpty()) {
          if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(InfinityAmpPlus.this,
              "Remove selected entries?", "Question", JOptionPane.YES_NO_OPTION)) {
            final int[] indices = availableList.getSelectedIndices();
            if (indices.length > 0) {
              for (int i = indices.length - 1; i >= 0; i--) {
                removeFromAvailableList(indices[i]);
              }
            }
          }
        }
      } else if (e.getSource() == playList) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE && !playList.isSelectionEmpty()) {
          if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(InfinityAmpPlus.this,
              "Remove selected entries?", "Question", JOptionPane.YES_NO_OPTION)) {
            final int[] indices = playList.getSelectedIndices();
            if (indices.length > 0) {
              for (int i = indices.length - 1; i >= 0; i--) {
                removeFromPlayList(indices[i]);
              }
            }
          }
        }
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void componentResized(ComponentEvent e) {
    }

    @Override
    public void componentMoved(ComponentEvent e) {
    }

    @Override
    public void componentShown(ComponentEvent e) {
    }

    @Override
    public void componentHidden(ComponentEvent e) {
      setPlaying(false);
    }

    /** Called by a {@link KeyEventDispatcher} instance. */
    public boolean dispatchKeyEvent(KeyEvent e) {
      boolean retVal = false;
      if (InfinityAmpPlus.this.isVisible() &&
          e.getID() == KeyEvent.KEY_PRESSED &&
          InfinityAmpPlus.this.isMediaKeysEnabled()) {
        // process only if InfinityAmp window is open and a "keypressed" event is triggered
        final int keyCode = InputKeyHelper.getExtendedKeyCode(e);
        try {
          switch (keyCode) {
            case InputKeyHelper.VK_MEDIA_PLAY_PAUSE:
              // play music from the playlist
              if (isPlaying()) {
                setPaused(!isPaused());
              } else {
                setPlaying(true);
              }
              retVal = true;
              break;
            case InputKeyHelper.VK_MEDIA_STOP:
              // stop music playback
              setPlaying(false);
              retVal = true;
              break;
            case InputKeyHelper.VK_MEDIA_PREV_TRACK:
              // jump to previously played music file
              if (setPrevPlayListItem() >= 0) {
                requestNextItem();
              }
              updateTransferButtons();
              retVal = true;
              break;
            case InputKeyHelper.VK_MEDIA_NEXT_TRACK:
              // jump to next music file to play
              if (setNextPlayListItem() >= 0) {
                requestNextItem();
              }
              updateTransferButtons();
              retVal = true;
              break;
          }
        } catch (Exception ex) {
          Logger.error(ex);
        }
      }
      return retVal;
    }
  }

  /** A wrapper class for {@link ResourceEntry} objects to provide customized string output. */
  private static class MusicResourceEntry extends FileResourceEntry {
    private static final Threading THREAD_POOL = new Threading(Threading.Priority.LOWEST);

    private boolean pathAsPrefix;
    private boolean pendingCalculation;
    private Long duration;

    public MusicResourceEntry(ResourceEntry musEntry, boolean pathAsPrefix) {
      super(Objects.requireNonNull(musEntry).getActualPath());
      if (!musEntry.getExtension().equalsIgnoreCase("mus")) {
        throw new IllegalArgumentException("Not a MUS resource: " + musEntry);
      }
      this.pathAsPrefix = pathAsPrefix;
    }

    public MusicResourceEntry(Path musFile, boolean pathAsPrefix) throws Exception {
      super(Objects.requireNonNull(musFile));
      final String fileName = musFile.getFileName().toString().toLowerCase();
      if (!fileName.endsWith(".mus")) {
        throw new IllegalArgumentException("Not a MUS resource: " + fileName);
      } else if (!Files.isRegularFile(musFile)) {
        throw new IOException("File does not exist: " + musFile);
      }
      this.pathAsPrefix = pathAsPrefix;
    }

    /**
     * Returns the location of the resource entry in abbreviated form.
     *
     * @return Effective parent folder of the resource. Returns a symbolic name for "root folder" and "current game
     *         folder".
     */
    public String getLocation() {
      final Path path = getActualPath();
      final int nameCount = path.getNameCount();
      final String gameFolder;
      if (path.startsWith(Profile.getGameRoot())) {
        gameFolder = "<current game>";
      } else if (nameCount > 2 && path.getName(nameCount - 2).toString().equalsIgnoreCase("music")) {
        gameFolder = path.getName(nameCount - 3).toString();
      } else if (nameCount > 1) {
        gameFolder = path.getName(nameCount - 2).toString();
      } else {
        gameFolder = "<root>";
      }

      return gameFolder;
    }

    /**
     * Returns whether game folder should be printed before the resource name {@code true} or after the resource name
     * {@code false}.
     */
    public boolean isPathAsPrefix() {
      return pathAsPrefix;
    }

    /**
     * Specifies whether game folder should be printed before the resource name {@code true} or after the resource name
     * {@code false}.
     */
    public void setPathAsPrefix(boolean b) {
      pathAsPrefix = b;
    }

    /** Returns whether duration of music file is available. */
    public boolean isDurationAvailable() {
      return (duration != null);
    }

    /** Returns the duration of the music file in seconds if available, -1 otherwise. */
    public int getDurationSeconds() {
      final long duration = getDuration();
      if (duration >= 0) {
        // duration is rounded to the nearest full second
        return (int)((duration + 500L) / 1000L);
      }
      return -1;
    }

    /** Returns the duration of the music file in milliseconds if available, -1 otherwise. */
    public long getDuration() {
      return isDurationAvailable() ? duration : -1L;
    }

    /** Sets music file duration manually. */
    public void setDuration(long duration) {
      if (duration >= 0L) {
        pendingCalculation = false;
        this.duration = duration;
      }
    }

    /** Calculates the duration of the MUS soundtrack in a background task. */
    public void calculateDuration() {
      if (duration != null) {
        return;
      }

      pendingCalculation = true;
      final Supplier<Long> supplier = () -> {
        final InfinityAmpPlus wnd = ChildFrame.getFirstFrame(InfinityAmpPlus.class);
        if (wnd != null) {
          if (wnd.getAvailableListIndex(this) < 0) {
            throw new CancellationException();
          }
        }

        try {
          final List<Entry> entries = MusResourceHandler.parseMusFile(this);
          long timeMs = 0L;
          int index = 0;
          while (true) {
            final Entry entry = entries.get(index);
            timeMs += entry.getAudioBuffer().getDuration();
            final int nextIndex = entry.getNextNr();
            if (nextIndex <= index || nextIndex >= entries.size()) {
              break;
            }
            index = nextIndex;
          }
          if (index < entries.size()) {
            final Entry entry = entries.get(index);
            if (entry.getEndBuffer() != null) {
              timeMs += entry.getEndBuffer().getDuration();
            }
          }
          entries.forEach(Entry::close);
          entries.clear();
          return timeMs;
        } catch (Throwable t) {
          Logger.debug(t, "Resource: " + this);
        }
        return -1L;
      };
      CompletableFuture.supplyAsync(supplier, THREAD_POOL.getExecutor()).thenAccept(this::durationCalculated);
    }

    /** Called by the background task when the task has ended. */
    private void durationCalculated(Long duration) {
      pendingCalculation = false;
      this.duration = duration;
      final InfinityAmpPlus wnd = ChildFrame.getFirstFrame(InfinityAmpPlus.class);
      if (wnd != null) {
        SwingUtilities.invokeLater(() -> wnd.refreshResourceEntry(this));
      }
    }

    @Override
    public String toString() {
      // getting relevant game folder name
      final String gameFolder = getLocation();

      final String time;
      if (isDurationAvailable()) {
        final int duration = getDurationSeconds();
        if (duration >= 0) {
          final int hours = duration / 3600;
          final int minutes = (duration / 60) % 60;
          final int seconds = duration % 60;
          time = String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
          time = "<unavailable>";
        }
      } else if (pendingCalculation) {
        time = "<pending>";
      } else {
        time = "";
      }

      String fmt = isPathAsPrefix() ? "(%2$s) %1$s" : "%1$s (%2$s)";
      if (!time.isEmpty()) {
        fmt += " [%3$s]";
      }
      return String.format(fmt, super.toString(), gameFolder, time);
    }
  }

  /**
   * A dialog for managing MUS sound segments to auto-skip at playback.
   */
  private static class SoundFilterDialog extends JDialog implements ActionListener, ListSelectionListener {
    // Default list of potential entries
    private static final List<String> DEFAULT_FILTERS = Arrays.asList("SPC", "SPC1", "MX0000A", "MX9000A");

    private final SimpleListModel<String> filterListModel = new SimpleListModel<>();
    private final JList<String> filterList = new JList<>(filterListModel);

    private final JButton addButton = new JButton("Add...");
    private final JButton removeButton = new JButton("Remove");
    private final JButton clearButton = new JButton("Clear");
    private final JButton resetButton = new JButton("Reset");
    private final JButton acceptButton = new JButton("Accept");
    private final JButton cancelButton = new JButton("Cancel");

    private boolean accepted;

    /**
     * A convenience function that presents an initialized dialog for customization and returns the
     * resulting sound filter items.
     *
     * @param owner Dialog owner as {@link Frame} instance.
     * @param filterEntries Initial set of filter items to populate the filter list. Argument can be {@code null}.
     * @return {@link Set} of user-defined filter items if accepted, {@code null} otherwise.
     */
    public static Collection<String> getSoundFiltersInteractive(Frame owner, Set<String> filterEntries) {
      final SoundFilterDialog dlg = new SoundFilterDialog(owner, filterEntries);
      final Collection<String> retVal = dlg.getResult();
      dlg.dispose();
      return retVal;
    }

    /** Returns the default set of sound filters. */
    public static Collection<String> getDefaultSoundFilters() {
      return Collections.unmodifiableCollection(DEFAULT_FILTERS);
    }

    public SoundFilterDialog(Frame owner, Set<String> filterEntries) {
      super(owner, "Define sound filters", true);
      init(filterEntries);
    }

    /** Returns whether the user accepted the filter list customization. */
    @SuppressWarnings("unused")
    public boolean accepted() {
      return accepted;
    }

    /**
     * Returns the sound filter items as a {@link Collection}.
     *
     * @return {@link Collection} of defined sound filters if accepted, {@code null} if the user cancelled the operation.
     */
    public Collection<String> getResult() {
      final HashSet<String> retVal;
      if (accepted) {
        retVal = new HashSet<>();
        for (int i = 0, size = filterListModel.size(); i < size; i++) {
          retVal.add(filterListModel.get(i));
        }
      } else {
        retVal = null;
      }
      return retVal;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == addButton) {
        addItems();
      } else if (e.getSource() == removeButton) {
        removeItems(filterList.getSelectedIndices());
      } else if (e.getSource() == clearButton) {
        // remove all filter items fromthe list
        int retVal = JOptionPane.showConfirmDialog(this, "Remove all filter entries?", "Question",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (retVal == JOptionPane.YES_OPTION) {
          filterListModel.clear();
          updateButtons();
        }
      } else if (e.getSource() == resetButton) {
        resetItems(true);
      } else if (e.getSource() == acceptButton) {
        accept();
      } else if (e.getSource() == cancelButton) {
        cancel();
      }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
      updateButtons();
    }

    /** Updates button states depending on current configuration */
    private void updateButtons() {
      removeButton.setEnabled(!filterList.isSelectionEmpty());
      clearButton.setEnabled(!filterListModel.isEmpty());
    }

    /** Adds new items interactively. */
    private void addItems() {
      final JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
      for (final FileFilter ff : fc.getChoosableFileFilters()) {
        fc.removeChoosableFileFilter(ff);
      }
      final FileFilter acmFilter = new FileNameExtensionFilter("ACM files", "acm");
      fc.addChoosableFileFilter(acmFilter);
      fc.setFileFilter(acmFilter);
      fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fc.setMultiSelectionEnabled(true);
      fc.setDialogTitle("Select ACM sound files");
      final int retVal = fc.showOpenDialog(this);
      if (retVal == JFileChooser.APPROVE_OPTION) {
        for (final File file : fc.getSelectedFiles()) {
          addItem(file.getName());
        }
      }
    }

    /** Adds the specified filter item to the list. Item is normalized and only added if not yet present. */
    private boolean addItem(String item) {
      boolean retVal = false;

      item = normalizeItem(item, true);
      if (item != null) {
        // check if item is already present
        if (!item.isEmpty()) {
          retVal = true;
          for (int i = 0, size = filterListModel.size(); i < size && retVal; i++) {
            retVal = !item.equalsIgnoreCase(filterListModel.get(i));
            if (!retVal) {
              filterList.setSelectedIndex(i);
            }
          }
        }

        if (retVal) {
          filterListModel.add(item);
        }
      }

      return retVal;
    }

    /** Removes all list items specified by the item index array. */
    private void removeItems(int[] indices) {
      if (indices != null && indices.length > 0) {
        for (int i = indices.length - 1; i >= 0; i--) {
          final int index = indices[i];
          if (index >= 0 && index < filterListModel.size()) {
            filterListModel.remove(index);
          }
        }
      }
    }

    /**
     * Resets the filter list content by a predefined list of items.
     *
     * @param prompt Whether to prompt for confirmation.
     */
    private void resetItems(boolean prompt) {
      int retVal = JOptionPane.YES_OPTION;
      if (prompt) {
        retVal = JOptionPane.showConfirmDialog(this, "Restore predefined filter entries?", "Question",
            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      }

      if (retVal == JOptionPane.YES_OPTION) {
        filterListModel.clear();
        DEFAULT_FILTERS.forEach(this::addItem);
        filterList.setSelectedIndex(0);
        filterList.ensureIndexIsVisible(0);
        updateButtons();
      }
    }

    /** Accepts the current filter list and closes the dialog. */
    private void accept() {
      accepted = true;
      setVisible(false);
    }

    /** Discards the current filter list and closes the dialog. */
    private void cancel() {
      accepted = false;
      setVisible(false);
    }

    /**
     * Normalizes the item string presentation by dealing with path elements and file extensions.
     *
     * @param item The sound filter item string.
     * @param withExtension Specify {@code true} to ensure it contains the ".acm" file extension.
     * @return The normalized item.
     */
    private String normalizeItem(String item, boolean withExtension) {
      String retVal = item;

      if (retVal != null) {
        retVal = retVal.trim();

        // normalize item (no path; file extension: .acm)
        for (final char ch : new char[] {'/', '\\'}) {
          int pos = retVal.indexOf(ch);
          if (pos >= 0) {
            retVal = retVal.substring(pos);
          }
        }

        // ensure item has the right file extension
        if (!retVal.isEmpty()) {
          int pos = retVal.lastIndexOf('.');
          if (withExtension) {
            if (pos < 0) {
              retVal += ".acm";
            } else if (!retVal.substring(pos).equalsIgnoreCase(".acm")) {
              retVal = retVal.substring(0, pos).trim();
              if (!retVal.isEmpty()) {
                retVal += ".acm";
              }
            }
          } else {
            if (pos >= 0 && retVal.substring(pos).equalsIgnoreCase(".acm")) {
              retVal = retVal.substring(0, pos);
            }
          }
        }
      }

      return retVal;
    }

    /** Adds the specified set of filters to the filter list. */
    private void loadFilters(Set<String> filterEntries) {
      filterListModel.clear();
      final Iterable<String> iterable = (filterEntries != null) ? filterEntries : DEFAULT_FILTERS;
      for (final String item : iterable) {
        addItem(item);
      }
    }

    private void init(Set<String> filterEntries) {
      setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      getRootPane().setDefaultButton(acceptButton);

      filterList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
      filterList.addListSelectionListener(this);

      final JScrollPane filterScroll = new JScrollPane(filterList);
      filterScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
      filterScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

      addButton.addActionListener(this);
      removeButton.addActionListener(this);
      clearButton.addActionListener(this);
      resetButton.addActionListener(this);
      acceptButton.addActionListener(this);
      cancelButton.addActionListener(this);
      addButton.requestFocusInWindow();

      // setting ESC keypress functionality
      getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
          this);
      getRootPane().getActionMap().put(this, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          cancel();
        }
      });

      GridBagConstraints gbc = new GridBagConstraints();

      final JPanel buttonPanel = new JPanel(new GridBagLayout());
      ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, 0, 0), 0, 0);
      buttonPanel.add(addButton, gbc);
      ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(8, 0, 0, 0), 0, 0);
      buttonPanel.add(removeButton, gbc);
      ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(8, 0, 0, 0), 0, 0);
      buttonPanel.add(clearButton, gbc);
      ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(24, 0, 0, 0), 0, 0);
      buttonPanel.add(resetButton, gbc);
      ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(0, 0, 0, 0), 0, 0);
      buttonPanel.add(new JPanel(), gbc);
      ViewerUtil.setGBC(gbc, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.LAST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(16, 0, 0, 0), 0, 0);
      buttonPanel.add(acceptButton, gbc);
      ViewerUtil.setGBC(gbc, 0, 6, 1, 1, 0.0, 0.0, GridBagConstraints.LAST_LINE_START, GridBagConstraints.HORIZONTAL,
          new Insets(8, 0, 0, 0), 0, 0);
      buttonPanel.add(cancelButton, gbc);

      final JPanel mainPanel = new JPanel(new GridBagLayout());
      ViewerUtil.setGBC(gbc, 0, 0, GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
      final JLabel listTitle = new JLabel("Filter list");
      mainPanel.add(listTitle, gbc);
      ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
          new Insets(8, 0, 0, 0), 0, 0);
      mainPanel.add(filterScroll, gbc);
      ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.VERTICAL,
          new Insets(8, 8, 0, 0), 0, 0);
      mainPanel.add(buttonPanel, gbc);

      final Container pane = getContentPane();
      pane.setLayout(new GridBagLayout());
      ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
          new Insets(8, 8, 8, 8), 0, 0);
      pane.add(mainPanel, gbc);

      pack();
      setMinimumSize(getSize());
      Center.center(this, getOwner().getBounds());

      loadFilters(filterEntries);
      if (!filterListModel.isEmpty()) {
        filterList.setSelectedIndex(0);
        filterList.ensureIndexIsVisible(0);
      }

      setVisible(true);
    }
  }

  /**
   * Extends the {@link DefaultListCellRenderer} by an option to print the list index in front of the cell content.
   */
  private static class IndexedCellRenderer extends DefaultListCellRenderer {
    private boolean showIndex;

    public IndexedCellRenderer(boolean indexEnabled) {
      super();
      showIndex = indexEnabled;
    }

    /** Returns whether an index number is printed in front of the cell content. */
    @SuppressWarnings("unused")
    public boolean isIndexEnabled() {
      return showIndex;
    }

    /** Specifies whether an index number should be printed in front of the cell content. */
    @SuppressWarnings("unused")
    public void setIndexEnabled(boolean enable) {
      if (enable != showIndex) {
        showIndex = enable;
      }
    }

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
        boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (showIndex) {
        final String text = (index + 1) + ". " + getText();
        setText(text);
      }
      return this;
    }
  }

  /**
   * A simple M3U file importer and exporter class.
   */
  private static class Playlist {
    private static final String UTF8_BOM = "\uFEFF";

    private static final int MILLISECONDS_MULTIPLIER = 1000;

    // Stores individual MUS entries as {MUS file path, Track length in milliseconds} pairs.
    // Unavailable track length is specified as a negative value.
    private final List<Couple<Path, Integer>> entries = new ArrayList<>();

    /** Initializes an empty {@link Playlist} object. */
    public Playlist() {
    }

    /**
     * Initializes a new {@link Playlist} object with the specified M3U playlist file.
     *
     * @param file {@link Path} of the UTF-8 encoded M3U playlist file.
     * @throws IOException    if an I/O error occurred.
     * @throws ParseException if the playlist data contains invalid content.
     */
    public Playlist(Path file) throws IOException, ParseException {
      this(readFile(Objects.requireNonNull(file)));
    }

    /**
     * Initializes a new {@link Playlist} object with the text content of a M3U playlist file.
     *
     * @param content A {@link String} containing M3U playlist data.
     * @throws ParseException if the playlist data contains invalid content.
     */
    @SuppressWarnings("unused")
    public Playlist(String content) throws ParseException {
      this(Arrays.asList(Objects.requireNonNull(content).split("\r?\n")));
    }

    /**
     * Initializes a new {@link Playlist} objects.
     *
     * @param lines M3U playlist data as a list of individual text lines.
     * @throws ParseException if the playlist data contains invalid content.
     */
    private Playlist(List<String> lines) throws ParseException {
      parse(Objects.requireNonNull(lines));
    }

    /** Returns the number of available soundtracks. */
    public int getEntriesCount() {
      return entries.size();
    }

    /**
     * Returns the file path of the specified entry.
     *
     * @param index The soundtrack index.
     * @return {@link Path} of the soundtrack file.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     */
    public Path getEntryPath(int index) throws IndexOutOfBoundsException {
      return entries.get(index).getValue0();
    }

    /**
     * Returns the soundtrack length of the specified entry, in milliseconds.
     *
     * @param index The soundtrack index.
     * @return Length in milliseconds. A negative value indicates unavailable length information.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     */
    public int getEntryLength(int index) throws IndexOutOfBoundsException {
      return entries.get(index).getValue1();
    }

    /**
     * Adds the specified music file to the playlist.
     *
     * @param musicFile Path of the music file as {@code String}.
     * @param length Soundtrack length, in milliseconds.
     */
    @SuppressWarnings("unused")
    public void addEntry(String musicFile, int length) {
      addEntry(Paths.get(Objects.requireNonNull(musicFile)), length);
    }

    /**
     * Adds the specified music file to the playlist.
     *
     * @param musicFile {@link Path} of the music file.
     * @param length Soundtrack length, in milliseconds.
     */
    public void addEntry(Path musicFile, int length) {
      final Couple<Path, Integer> entry = new Couple<>(Objects.requireNonNull(musicFile), length);
      entries.add(entry);
    }

    /**
     * Removes the specified playlist entry.
     *
     * @param index The soundtrack index.
     * @throws IndexOutOfBoundsException if {@code index} is out of bounds.
     */
    @SuppressWarnings("unused")
    public void removeEntry(int index) throws IndexOutOfBoundsException {
      entries.remove(index);
    }

    /**
     * Exports the playlist content to a M3U playlist file with UTF-8 text encoding.
     *
     * @param fileName Name of the playlist file.
     * @param cs       The character set to use for text encoding. Defaults to {@code UTF-8} if not specified.
     * @throws IOException if an I/O error occurred.
     */
    public void exportFile(Path fileName, Charset cs) throws IOException {
      if (cs == null) {
        cs = StandardCharsets.UTF_8;
      }
      boolean isUtf = (cs.name().toUpperCase().contains("UTF"));

      // preparing playlist content
      final String crlf = "\r\n";
      final StringBuilder sb = new StringBuilder();
      if (isUtf) {
        sb.append(UTF8_BOM);
      }
      sb.append("#EXTM3U").append(crlf);
      for (final Couple<Path, Integer> entry : entries) {
        final int length;
        if (entry.getValue1() >= 0) {
          // rounding up
          length = (entry.getValue1() + (MILLISECONDS_MULTIPLIER / 2)) / MILLISECONDS_MULTIPLIER;
        } else {
          length = -1;
        }
        final Path path = entry.getValue0();
        final String name = path.getFileName().toString();
        sb.append("#EXTINF:").append(length).append(',').append(name).append(crlf);
        sb.append(path).append(crlf);
      }

      Files.write(fileName, sb.toString().getBytes(cs));
    }

    private void parse(List<String> lines) throws ParseException {
      Objects.requireNonNull(lines);

      // normalizing content and eliminating empty lines
      final List<String> data = new ArrayList<>();
      lines.forEach(line -> {
        if (line != null) {
          String s = line;
          if (s.startsWith(UTF8_BOM)) {
            s = s.substring(1);
          }
          s = s.trim();
          if (!s.isEmpty()) {
            data.add(s);
          }
        }
      });

      int index = 0;
      if (data.isEmpty()) {
        throw new ParseException("Playlist is empty", index);
      }

      // parsing header
      final String header = data.get(index);
      if (!header.equals("#EXTM3U")) {
        throw new ParseException("Invalid header", index);
      }
      index++;

      // parsing content
      Couple<Path, Integer> entry = null;
      while (index < data.size()) {
        String line = data.get(index);
        if (line.startsWith("#")) {
          // processing directive
          final String token = extractToken(line, ":", 0);
          switch (token) {
            case "#EXTINF":
              break;
            case "#PLAYLIST":
            case "#EXTGRP":
            case "#EXTALB":
            case "#EXTART":
            case "#EXTGENRE":
            case "#EXTM3A":
            case "#EXTBYT":
            case "#EXTBIN":
            case "#EXTENC":
            case "#EXTIMG":
              // skipping unsupported directives
              index++;
              continue;
            default:
              throw new ParseException("Invalid directive", index);
          }

          // track length, in seconds
          line = line.substring(token.length() + 1);
          final String lengthString = extractToken(line, ",", 0);
          try {
            final int length = Integer.parseInt(lengthString);
            if (entry == null) {
              // rounding up
              entry = new Couple<>(null, length * MILLISECONDS_MULTIPLIER + (MILLISECONDS_MULTIPLIER / 2));
            }
          } catch (NumberFormatException e) {
            throw new ParseException("Missing or invalid track length", index);
          }
        } else {
          // processing file path
          Path musPath;
          try {
            musPath = Paths.get(line);
            if (Files.isRegularFile(musPath)) {
              final String name = musPath.getFileName().toString();
              if (!name.toLowerCase().endsWith(".mus")) {
                Logger.info("Not a MUS file: {}", name);
              } else {
                if (entry == null) {
                  entry = new Couple<>(musPath, -1);
                } else {
                  entry.setValue0(musPath);
                }
                entries.add(entry);
              }
            } else {
              Logger.info("File does not exist: {}", musPath);
            }
          } catch (InvalidPathException e) {
            throw new ParseException("Invalid ", index);
          }
          entry = null;
        }
        index++;
      }

      if (entry != null) {
        Logger.info("Dangling playlist directive at line {}", index - 1);
      }
    }

    /** Returns the token starting at {@code startOfs} and ending at the first occurrence of {@code separator}, exclusive. */
    private String extractToken(String content, String separator, int startOfs) {
      if (content != null && separator != null) {
        if (!separator.isEmpty()) {
          int pos = content.indexOf(separator, startOfs);
          if (pos >= 0) {
            return content.substring(startOfs, pos);
          }
        }

      }
      return content;
    }

    /**
     * Used internally to read a text file and return it as a list of individual lines.
     * Attempts to autodetect the correct character encoding.
     */
    private static List<String> readFile(Path file) throws IOException {
      final byte[] data = Files.readAllBytes(file);
      Charset cs = StandardCharsets.ISO_8859_1;
      if (data.length > 2) {
        if (data[0] == (byte)0xef && data[1] == (byte)0xbb && data[2] == (byte)0xbf) {
          cs = StandardCharsets.UTF_8;
        } else if (data[0] == (byte)0xff && data[1] == (byte)0xfe) {
          cs = StandardCharsets.UTF_16LE;
        } else if (data[0] == (byte)0xfe && data[1] == (byte)0xff) {
          cs = StandardCharsets.UTF_16BE;
        }
      }
      final String content = new String(data, cs);
      return Arrays.asList(content.split("\r?\n"));
    }
  }
}
