package org.infinity.resource.mus;

import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.infinity.resource.Closeable;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.sound.AudioBuffer;
import org.infinity.util.Logger;
import org.infinity.util.io.StreamUtils;

/**
 * Customizable helper class for handling the details of MUS playback progress.
 */
public class MusResourceHandler implements Closeable {
  /** List of sound segments of the loaded MUS resource. */
  private List<Entry> entryList;
  /** The initial sound entry index. */
  private int startEntryIndex;
  /** Index of the current sound entry. */
  private int currentEntryIndex;
  /** Whether to auto-switch to "end" segment when advancing from last regular sound segment. */
  private boolean allowEnding;
  /** Whether "ending" flag should be set by the next advancement. */
  private boolean signalEnding;
  /** Whether "end" sound segment is selected. */
  private boolean ending;
  /** Whether advancing sound segments should consider looping back to previous segments. */
  private boolean loop;

  /**
   * Loads the specified MUS resource for playback with the specified options.
   *
   * @param musEntry    {@link ResourceEntry} of the MUS resource.
   * @param startIndex  The initially selected sound segment index.
   * @param allowEnding Whether to switch to the "end" sound segment automatically when the last regular sound segment
   *                      was processed. Setting this option has only an effect if {@code looping} is disabled or the
   *                      MUS resource does not loop.
   * @param looping     Whether soundtrack is alloed to loop back to previously played entries.
   * @throws NullPointerException  if {@code musEntry} is {@code null}.
   * @throws FileNotFoundException if the MUS resource does not exist.
   * @throws Exception             if the MUS resource could not be parsed.
   */
  public MusResourceHandler(ResourceEntry musEntry, int startIndex, boolean allowEnding, boolean looping)
      throws Exception {
    if (Objects.isNull(musEntry)) {
      throw new NullPointerException("musEntry is null");
    }
    if (!Files.isRegularFile(musEntry.getActualPath())) {
      throw new FileNotFoundException("MUS file not found: " + musEntry.getActualPath());
    }
    init(MusResourceHandler.parseMusFile(musEntry), startIndex, allowEnding, looping);
  }

  /**
   * Loads a parsed MUS entry list for playback with the specified options.
   *
   * @param musEntries  Collection of {@link Entry} instances of a MUS resource.
   * @param startIndex  The initially selected sound segment index.
   * @param allowEnding Whether to switch to the "end" sound segment automatically when the last regular sound segment
   *                      was processed. Setting this option has only an effect if {@code looping} is disabled or the
   *                      MUS resource does not loop.
   * @param looping     Whether soundtrack is alloed to loop back to previously played entries.
   * @throws NullPointerException if {@code musEntries} is {@code null}.
   */
  public MusResourceHandler(Collection<Entry> musEntries, int startIndex, boolean allowEnding, boolean looping) {
    if (Objects.isNull(musEntries)) {
      throw new NullPointerException("musEntries is null");
    }
    init(musEntries, startIndex, allowEnding, looping);
  }

  /**
   * Returns the number of sound segments defined by this MUS resource, not counting the special "end" segment.
   *
   * @return Number of available sound segments in the MUS resource.
   */
  public int size() {
    return entryList.size();
  }

  /**
   * Returns whether a subsequent call of {@link #advance()} method will return successfully with the current configuration.
   *
   * @return {@code true} if the current sound segment is the last segment in the sound list, {@code false} otherwise.
   */
  public boolean hasNextEntry() {
    if (currentEntryIndex == Integer.MIN_VALUE) {
      return validIndex(startEntryIndex);
    } else if (!isEnding() && validIndex(currentEntryIndex)) {
      final int index = entryList.get(currentEntryIndex).getNextNr();
      boolean retVal = (isLooping() || (index > currentEntryIndex || (isAllowEnding() && getCurrentEntry().getEndBuffer() != null)));
      retVal = retVal && ((isAllowEnding() && getCurrentEntry().getEndBuffer() != null) || validIndex(index));
      return retVal;
    }
    return false;
  }

  /**
   * Returns whether advancing the soundtrack should allow to return to previously selected sound segments
   *
   * @return {@code true} if looping back to previous sound segments is allowed, {@code false} otherwise.
   */
  public boolean isLooping() {
    return loop;
  }

  /**
   * Specify whether looping back to previously selected sound segments should be allowed by the {@link #advance()}
   * method.
   *
   * @param loop Specify {@code true} to allow looping, {@code false} to end advancement when reaching the last sound
   *               segment.
   */
  public void setLooping(boolean loop) {
    this.loop = loop;
  }

  /**
   * Returns whether the "end" sound segment is automatically returned when the last regular segment has been
   * processed.
   *
   * @return {@code true} to allow advancing to "end" sound segment
   */
  public boolean isAllowEnding() {
    return allowEnding;
  }

  /**
   * Specify whether the "end" sound segment should be automatically selected after the last regular sound segment has
   * been processed.
   *
   * @param allow Whether to enable or disable automatic availability of the "end" sound segment.
   */
  public void setAllowEnding(boolean allow) {
    allowEnding = allow;
  }

  /**
   * Returns whether the "end" sound segment is enabled as current sound segment. Advancing sound segments if "end"
   * segment is enabled finishes advancement instantly.
   *
   * @return {@code true} if "end" sound segment is enabled, {@code false} otherwise.
   */
  public boolean isEnding() {
    return ending;
  }

  /**
   * Specify to activate the "end" sound segment for the currently selected entry. This flag indicates which audio
   * segment is returned by a call of {@link #getAudioBuffer()}.
   *
   * @param ending whether to activate or deactivate "end" sound segment.
   */
  public void setEnding(boolean ending) {
    this.ending = ending;
    if (this.ending) {
      signalEnding = false;
    }
  }

  /**
   * Returns whether the handler has been signaled to enable the "end" sound segment flag with the next call of
   * {@link #advance()}.
   * <p>
   * <strong>Note:</strong> The signal is cleared automatically when the ending flag has been set.
   * </p>
   *
   * @return {@code true} if signal has been set, {@code false} otherwise.
   */
  public boolean isEndingSignaled() {
    return signalEnding;
  }

  /**
   * Specifies whether to signal the handler to set the ending flag at the next call of {@link #advance()}. Does nothing
   * if the ending flag has already been enabled.
   * <p>
   * <strong>Note:</strong> The signal is cleared automatically when the ending flag has been set.
   * </p>
   */
  public void setSignalEnding(boolean signal) {
    if (signal != signalEnding && (!signal || !ending)) {
      signalEnding = signal;
    }
  }

  /**
   * Returns the sound segment index that was explicitly or implicitly passed to the constructor of this
   * {@link MusResourceHandler} instance.
   *
   * @return initial sound segment index.
   */
  public int getStartIndex() {
    return startEntryIndex;
  }

  /**
   * Specify the initially selected sound segment index when resetting the handler instance.
   *
   * @param newIndex index value.
   */
  public void setStartIndex(int newIndex) {
    startEntryIndex = Math.max(0, Math.min(entryList.size() - 1, newIndex));
  }

  /**
   * Returns the index of the current sound segment.
   *
   * @return Index of current sound segment. Returns {@code -1} if {@link #advance()} was not yet called after constructing or
   *         resetting this {@link MusResourceHandler} instance. Returns {@link #size()} if no more sound segments are
   *         available.
   */
  public int getCurrentIndex() {
    return (currentEntryIndex == Integer.MIN_VALUE) ? -1 : currentEntryIndex;
  }

  /**
   * Specify the index for the currently selected sound segment.
   *
   * @param newIndex new sound segment index. Specify {@code -1} to reset advancement. Specify {@link #size()} to
   *                   indicate that the soundtrack has ended.
   * @throws IndexOutOfBoundsException if {@code newIndex} is out of bounds.
   * @throws IllegalArgumentException  if {@link #isEnding()} is set and the end buffer is {@code null}.
   */
  public void setCurrentIndex(int newIndex) {
    if (!validIndex(newIndex) && newIndex != -1 && newIndex != size()) {
      throw new IndexOutOfBoundsException("index = " + newIndex);
    }
    if (isEnding() && newIndex != -1 && newIndex != size() && entryList.get(newIndex).getEndBuffer() == null) {
      throw new IllegalArgumentException("end buffer is null for index = " + newIndex);
    }
    currentEntryIndex = (newIndex == -1) ? Integer.MIN_VALUE : newIndex;
  }

  /**
   * Returns the {@link Entry} instance of the current sound segment.
   *
   * @return {@link Entry} of the current segment if available, {@code null} otherwise.
   */
  public Entry getCurrentEntry() {
    if (validIndex(currentEntryIndex)) {
      return entryList.get(currentEntryIndex);
    }
    return null;
  }

  /**
   * Returns the {@link Entry} instance at the specified position in the sound entry list.
   *
   * @param index Index of the entry instance.
   * @return the {@link Entry} instance.
   * @throws IndexOutOfBoundsException  if the index is out of range.
   */
  public Entry getEntry(int index) {
    return entryList.get(index);
  }

  /**
   * Returns the audio buffer of the current sound segment. Returns the buffer of the "end" sound segment if
   * {@link #isEnding()} is {@code true}.
   *
   * @return {@link AudioBuffer} of the current sound segment. Returns {@code null} if audio buffer is not available.
   */
  public AudioBuffer getAudioBuffer() {
    final Entry entry = getCurrentEntry();
    if (Objects.nonNull(entry)) {
      return isEnding() ? entry.getEndBuffer() : entry.getAudioBuffer();
    }
    return null;
  }

  /**
   * Advances MUS segment list to the next sound entry. Enables playback of "end" sound segment if
   * {@link #isAllowEnding()} is set and the last sound segment was selected by a previous call of {@code #advance()}.
   *
   * @return {@code true} if the soundtrack could be advanced to the next segment, {@code false} otherwise.
   */
  public boolean advance() {
    if (!hasNextEntry()) {
      currentEntryIndex = size();
    } else if (currentEntryIndex == Integer.MIN_VALUE) {
      currentEntryIndex = startEntryIndex;
    } else if (validIndex(currentEntryIndex)) {
      if (signalEnding) {
        if (entryList.get(currentEntryIndex).getEndBuffer() != null) {
          ending = true;
        } else {
          currentEntryIndex = size();
        }
      } else {
        // advance to next sound segment or switch to end segment if needed
        final int index = entryList.get(currentEntryIndex).getNextNr();
        if ((!isLooping() && index <= currentEntryIndex) || !validIndex(index)) {
          if (isAllowEnding() && !isEnding()) {
            // switch to "end" segment after processing the last regular sound segment
            ending = true;
          } else {
            currentEntryIndex = size();
          }
        } else {
          currentEntryIndex = index;
        }
      }
    } else {
      // no further sound segments available
      currentEntryIndex = size();
    }

    signalEnding = false;

    return validIndex(currentEntryIndex);
  }

  /**
   * Resets the playback position back to the index that was explicitly or implicitly passed to the constructor.
   */
  public void reset() {
    currentEntryIndex = Integer.MIN_VALUE;
    ending = false;
  }

  /** Releases all sound segment resources. */
  @Override
  public void close() throws Exception {
    ending = false;
    currentEntryIndex = 0;
    for (int i = entryList.size() - 1; i >= 0; i--) {
      final Entry entry = entryList.get(i);
      entry.close();
      entryList.remove(i);
    }
  }

  /** Returns {@code true} only if {@code index} is a valid list index. */
  private boolean validIndex(int index) {
    return (index >= 0 && index < entryList.size());
  }

  /** Initializes the MusResourceHandler instance. */
  private void init(Collection<Entry> entries, int startIndex, boolean allowEnding, boolean looping) {
    if (Objects.isNull(entries)) {
      throw new NullPointerException("MUS entry list is null");
    }

    entryList = new ArrayList<>(entries);
    currentEntryIndex = Integer.MIN_VALUE;
    setStartIndex(startIndex);
    setAllowEnding(allowEnding);
    setEnding(false);
    setLooping(looping);
  }

  /**
   * Creates a parsed list of sound entries from the specified MUS resource.
   *
   * @param resource MUS resource as {@link ResourceEntry} instance.
   * @return List of MUS {@link Entry} objects for each of the parsed MUS sound segments.
   * @throws NullPointerException if {@code resource} is {@code null}.
   * @throws Exception if the MUS resource could not be parsed.
   */
  public static List<Entry> parseMusFile(ResourceEntry resource) throws Exception {
    Objects.requireNonNull(resource);
    List<Entry> retVal = new ArrayList<>();

    final ByteBuffer bb = resource.getResourceBuffer();
    final String[] lines = StreamUtils.readString(bb, bb.limit()).split("\r?\n");
    int idx = 0;

    String acmFolder = null;
    while (acmFolder == null && idx < lines.length) {
      final String s = getNormalizedString(lines[idx++]);
      if (!s.isEmpty()) {
        acmFolder = s;
      }
    }

    int numEntries = 0;
    while (idx < lines.length) {
      final String s = getNormalizedString(lines[idx++]);
      if (!s.isEmpty()) {
        numEntries = Integer.parseInt(s);
        break;
      }
    }

    int counter = 0;
    while (idx < lines.length && counter < numEntries) {
      String line = getNormalizedString(lines[idx++]);
      if (!line.isEmpty()) {
        retVal.add(new Entry(resource, acmFolder, retVal, line, counter));
        counter++;
      }
    }

    if (counter != numEntries) {
      Logger.warn("{}: Unexpected number of parsed sound segments (found: {}, expected: {})", resource, counter,
          numEntries);
    }

    for (final Entry entry : retVal) {
      entry.init();
    }

    return retVal;
  }

  private static String getNormalizedString(String s) {
    if (s == null) {
      return "";
    }
    String retVal = s;
    final int pos = retVal.indexOf('#');
    if (pos >= 0) {
      retVal = retVal.substring(0, pos);
    }
    retVal = retVal.trim();
    return retVal;
  }
}
