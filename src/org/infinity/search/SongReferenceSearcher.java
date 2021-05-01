// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.awt.Component;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.infinity.datatype.Song2daBitmap;
import org.infinity.datatype.ResourceBitmap.RefEntry;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.are.Song;
import org.infinity.resource.bcs.BcsResource;
import org.infinity.resource.bcs.Decompiler;
import org.infinity.resource.dlg.AbstractCode;
import org.infinity.resource.dlg.DlgResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.mus.MusResource;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.io.StreamUtils;

/**
 * Performs search of the specified song in the {@link AreResource areas},
 * {@link BcsResource scripts} and {@link DlgResource dialogues}.
 */
public class SongReferenceSearcher extends AbstractReferenceSearcher
{
  /** Regular expression that contains script commands that controls music. */
  private final Pattern pattern;
  /** Index of searched playlist resource in the file with playlists. */
  private final long songId;

  /**
   * Creates finder that searches usages of the specified {@link MusResource playlist}.
   *
   * @param musPlaylist Pointer to the searched playlist
   * @param parent GUI component that will be parent for results window
   */
  public SongReferenceSearcher(ResourceEntry musPlaylist, Component parent)
  {
    super(musPlaylist, new String[]{"ARE", "BCS", "BS", "DLG"},
          new boolean[]{true, true, false, true}, parent);
    long songId = -1L;
    final Song2daBitmap songBitmap = new Song2daBitmap(StreamUtils.getByteBuffer(4), 0, 4);
    for (final RefEntry refEntry : songBitmap.getBitmap().values()) {
      final ResourceEntry entry = refEntry.getResourceEntry();
      if (entry != null && entry.equals(musPlaylist)) {
        songId = refEntry.getValue();
        break;
      }
    }

    if (songId >= 0 && Profile.getGame() != Profile.Game.PST) {
      final StringBuilder sb = new StringBuilder();
      sb.append("StartMusic\\(").append(songId).append(",.+\\)");
      IdsMap map = null;
      if (ResourceFactory.resourceExists("SONGLIST.IDS")) {
        map = IdsMapCache.get("SONGLIST.IDS");
      } else if (Profile.getGame() == Profile.Game.IWD2) {
        map = IdsMapCache.get("MUSIC.IDS");
      }
      if (map != null && map.get(songId) != null) {
        final String musicId = map.get(songId).getSymbol();
        sb.append("|SetMusic\\(.+?,").append(songId).append("\\)");
        if (musicId != null && !musicId.isEmpty()) {
          sb.append("|SetMusic\\(.+?,").append(Pattern.quote(musicId)).append("\\)");
        }
      }
      pattern = Pattern.compile(sb.toString());
    } else {
      pattern = null;
    }
    this.songId = songId;
  }

  @Override
  protected void search(ResourceEntry entry, Resource resource)
  {
    if (resource instanceof AreResource) {
      searchAre(entry, (AreResource)resource);
    } else if (resource instanceof BcsResource) {
      searchBcs(entry, (BcsResource)resource);
    } else if (resource instanceof DlgResource) {
      searchDlg(entry, (DlgResource)resource);
    } else if (resource instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)resource);
    }
  }

  private void searchBcs(ResourceEntry entry, BcsResource bcs)
  {
    if (pattern == null) { return; }

    final Decompiler decompiler = new Decompiler(bcs.getCode(), true);
    decompiler.setGenerateComments(false);
    decompiler.setGenerateResourcesUsed(false);
    try {
      String text = decompiler.decompile();
      searchText(entry, null, text);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void searchDlg(ResourceEntry entry, DlgResource dlg)
  {
    if (pattern == null) { return; }

    for (final StructEntry e : dlg.getFields()) {
      if (e instanceof AbstractCode) {
        String text = ((AbstractCode)e).getText();
        searchText(entry, e, text);
      }
    }
  }

  private void searchText(ResourceEntry entry, StructEntry res, String text)
  {
    final Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      addHit(entry, matcher.group(), res);
    }
  }

  private void searchAre(ResourceEntry entry, AreResource are)
  {
    StructEntry se = are.getAttribute(Song.ARE_SONGS);
    if (se instanceof AbstractStruct) {
      searchStruct(entry, (AbstractStruct)se);
    }
  }

  private void searchStruct(ResourceEntry entry, AbstractStruct struct)
  {
    for (final StructEntry e : struct.getFlatFields()) {
      if (e instanceof Song2daBitmap) {
        if (songId == ((Song2daBitmap)e).getLongValue()) {
          addHit(entry, String.format("%s (%d)", targetEntry.getResourceName(), songId), e);
        }
      }
    }
  }
}
