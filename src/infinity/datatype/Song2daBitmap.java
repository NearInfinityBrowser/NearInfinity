// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import infinity.resource.Profile;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.IdsMapCache;
import infinity.util.IdsMapEntry;
import infinity.util.LongIntegerHashMap;
import infinity.util.Pair;
import infinity.util.Table2da;
import infinity.util.Table2daCache;
import infinity.util.io.FileNI;

/** Specialized ResourceBitmap type for parsing ARE song reference entries. */
public class Song2daBitmap extends ResourceBitmap
{
  // Cached content of the song entries
  private static final ArrayList<RefEntry> SongList = new ArrayList<ResourceBitmap.RefEntry>();

  // Source of music resource references
  private static String TableName;
  // Textual representation of the song reference field data
  private static String FormatString = FMT_REF_NAME_VALUE;

  public Song2daBitmap(byte buffer[], int offset, int length)
  {
    this(null, buffer, offset, length);
  }

  public Song2daBitmap(StructEntry parent, byte buffer[], int offset, int length)
  {
    this(parent, buffer, offset, length, "Song");
  }

  public Song2daBitmap(byte buffer[], int offset, int length, String name)
  {
    this(null, buffer, offset, length, name);
  }

  public Song2daBitmap(StructEntry parent, byte buffer[], int offset, int length, String name)
  {
    super(parent, buffer, offset, length, name, createSongList(), "Unknown", FormatString);
  }

  private static synchronized List<RefEntry> createSongList()
  {
    if (SongList.isEmpty()) {
      // search "music" subfolder as well
      List<File> searchDirs = new ArrayList<File>();
      searchDirs.add(new FileNI((File)Profile.getProperty(Profile.GET_GAME_ROOT_FOLDER), "Music"));

      if (ResourceFactory.resourceExists("SONGLIST.2DA")) {
        TableName = "SONGLIST.2DA";
        FormatString = FMT_REF_NAME_VALUE;
        createSongList_SONGLIST(searchDirs);
      } else if (ResourceFactory.resourceExists("MUSICLIS.IDS")) {
        TableName = "MUSICLIS.IDS";
        FormatString = FMT_REF_HYPHEN_VALUE;
        createSongList_MUSIC(searchDirs);
      } else if (ResourceFactory.resourceExists("MUSIC.IDS")) {
        TableName = "MUSIC.IDS";
        FormatString = FMT_REF_HYPHEN_VALUE;
        createSongList_MUSIC(searchDirs);
      } else if (ResourceFactory.resourceExists("SONGS.IDS")) {
        TableName = "SONGS.IDS";
        FormatString = FMT_REF_NAME_VALUE;
        createSongList_PST(searchDirs);
      } else {
        TableName = "";
        FormatString = FMT_REF_HYPHEN_VALUE;
        createSongList_BG1(searchDirs);
      }
    }
    return SongList;
  }

  // Create song references for BG2 and Enhanced Edition games
  private static List<RefEntry> createSongList_SONGLIST(List<File> searchDirs)
  {
    if (SongList.isEmpty()) {
      Table2da table = Table2daCache.get(TableName);
      SongList.ensureCapacity(2 + table.getRowCount());
      SongList.add(new RefEntry(0xfffffffeL, "Special", "Continue area music"));
      SongList.add(new RefEntry(0xffffffffL, "Special", "Continue outside music"));

      for (int i = 0, size = table.getRowCount(); i < size; i++) {
        String name = table.get(i, 1);
        String ref = table.get(i, 2);
        SongList.add(new RefEntry((long)i, ref, name, searchDirs));
      }
    }
    return SongList;
  }

  // Create song references for IWD and IWD2
  private static List<RefEntry> createSongList_MUSIC(List<File> searchDirs)
  {
    if (SongList.isEmpty()) {
      LongIntegerHashMap<IdsMapEntry> map = IdsMapCache.get(TableName).getMap();
      SongList.ensureCapacity(2 + map.size());
      SongList.add(new RefEntry(0xfffffffeL, "Continue area music"));
      SongList.add(new RefEntry(0xffffffffL, "Continue outside music"));

      long[] keys = map.keys();
      for (final long key: keys) {
        String name = map.get(key).getString();
        String ref = name.replaceAll("_", "") + ".MUS";
        if (!ResourceFactory.resourceExists(ref, false, searchDirs)) {
          ref = name;
        }
        SongList.add(new RefEntry(key, ref, null, searchDirs));
      }
    }
    return SongList;
  }

  // Create song references for PST
  private static List<RefEntry> createSongList_PST(List<File> searchDirs)
  {
    // PST used static associations of SONGS.IDS with MUS resources (source: songlist.txt in game's music folder)
    final LongIntegerHashMap<Pair<String>> map = new LongIntegerHashMap<Pair<String>>();
    map.put(0L, new Pair<String>(null, "No music"));
    map.put(1L, new Pair<String>("MAIN.MUS", "Planescape Main Theme"));
    map.put(2L, new Pair<String>("CHAR_01.MUS", "Char: Nameless One"));
    map.put(3L, new Pair<String>("CHAR_02.MUS", "Char: Transcendent One"));
    map.put(4L, new Pair<String>("CHAR_03.MUS", "Char: The Shadows"));
    map.put(5L, new Pair<String>("CHAR_04.MUS", "Char: Deionarra"));
    map.put(6L, new Pair<String>("CHAR_05.MUS", "Char: Ravel"));
    map.put(7L, new Pair<String>("CHAR_06.MUS", "Char: Fhjull Forked-Tongue"));
    map.put(8L, new Pair<String>("CHAR_07.MUS", "Char: Trias"));
    map.put(9L, new Pair<String>("CHAR_09.MUS", "Char: Annah"));
    map.put(10L, new Pair<String>("CHAR_10.MUS", "Char: Dak'kon"));
    map.put(11L, new Pair<String>("CHAR_11.MUS", "Char: Morte"));
    map.put(12L, new Pair<String>("CHAR_12.MUS", "Char: Nordom"));
    map.put(13L, new Pair<String>("CHAR_13.MUS", "Char: Fall-from-Grace"));
    map.put(14L, new Pair<String>("CHAR_14.MUS", "Char: Vhailor"));
    map.put(15L, new Pair<String>("CHAR_15.MUS", "Char: Ignus"));
    map.put(16L, new Pair<String>("MORT.MUS", "Area: Mortuary"));
    map.put(17L, new Pair<String>("SIG.MUS", "Area: Sigil"));
    map.put(18L, new Pair<String>("SMOL.MUS", "Area: Smoldering Corpse Bar"));
    map.put(19L, new Pair<String>("BONE.MUS", "Area: Bones of the Night"));
    map.put(20L, new Pair<String>("CIVIC.MUS", "Area: Civic Festhall"));
    map.put(21L, new Pair<String>("RAVE.MUS", "Area: Ravel's Maze"));
    map.put(22L, new Pair<String>("MODR.MUS", "Area: Modron Maze"));
    map.put(23L, new Pair<String>("CUR.MUS", "Area: Curst"));
    map.put(24L, new Pair<String>("BAAT.MUS", "Area: Baator"));
    map.put(25L, new Pair<String>("FORT.MUS", "Area: Fortress of Regrets"));
    map.put(26L, new Pair<String>("CHAR13b.MUS", "Area: Fall-from-Grace"));
    map.put(27L, new Pair<String>("BT1.MUS", "Battle: Sigil"));
    map.put(28L, new Pair<String>("BT2.MUS", "Battle: Catacombs"));
    map.put(29L, new Pair<String>("BT3.MUS", "Battle: Curst"));
    map.put(30L, new Pair<String>("BT4.MUS", "Battle: Modron Cube"));
    map.put(31L, new Pair<String>("BT5.MUS", "Battle: Fortress of Regrets"));
    map.put(32L, new Pair<String>("BT5.MUS", "Battle: Fortress of Regrets - Final)"));
    map.put(33L, new Pair<String>("END_01G.MUS", "Ending: Good"));
    map.put(34L, new Pair<String>("END_01B.MUS", "Ending: Bad"));
    map.put(35L, new Pair<String>("END_02.MUS", "Ending: Indifferent"));
    map.put(36L, new Pair<String>("SMO2.MUS", "Smoldering Corpse Alternate"));

    if (SongList.isEmpty()) {
      SongList.ensureCapacity(2 + map.size());
      SongList.add(new RefEntry(0xfffffffeL, "Special", "Continue area music"));
      SongList.add(new RefEntry(0xffffffffL, "Special", "Continue outside music"));

      long[] keys = map.keys();
      for (final long key: keys) {
        String name = map.get(key).getSecond();
        String ref = map.get(key).getFirst();
        if (ref != null) {
          SongList.add(new RefEntry(key, ref, name, searchDirs));
        } else {
          SongList.add(new RefEntry(key, "None", name));
        }
      }
    }
    return SongList;
  }

  // Create song references for BG1
  private static List<RefEntry> createSongList_BG1(List<File> searchDirs)
  {
    // BG1 used static list of songs (source: GemRB's unhardcoded music.2da)
    final LongIntegerHashMap<String> map = new LongIntegerHashMap<String>();
    map.put(0L, "None");
    map.put(1L, "CDAY1.MUS");
    map.put(2L, "CDAY2.MUS");
    map.put(3L, "CNITE.MUS");
    map.put(4L, "CHANTS.MUS");
    map.put(5L, "DREAM.MUS");
    map.put(6L, "FESTI.MUS");
    map.put(7L, "FDAY.MUS");
    map.put(8L, "FNITE.MUS");
    map.put(9L, "PDAY.MUS");
    map.put(10L, "PNITE.MUS");
    map.put(11L, "TEMPLE.MUS");
    map.put(12L, "THEME.MUS");
    map.put(13L, "TDAY1.MUS");
    map.put(14L, "TDAY2.MUS");
    map.put(15L, "TNITE.MUS");
    map.put(16L, "DUNG1.MUS");
    map.put(17L, "DUNG2.MUS");
    map.put(18L, "DUNG3.MUS");
    map.put(19L, "BC1.MUS");
    map.put(20L, "BC2.MUS");
    map.put(21L, "BD1.MUS");
    map.put(22L, "BD2.MUS");
    map.put(23L, "BL1.MUS");
    map.put(24L, "BL2.MUS");
    map.put(25L, "BF1.MUS");
    map.put(26L, "BF2.MUS");
    map.put(27L, "BP1.MUS");
    map.put(28L, "BP2.MUS");
    map.put(29L, "BW1.MUS");
    map.put(30L, "BRIDGE.MUS");
    map.put(31L, "FORT.MUS");
    map.put(32L, "CHAPTER.MUS");
    map.put(33L, "MPO900.MUS");
    map.put(34L, "TAV1.MUS");
    map.put(35L, "TAV2.MUS");
    map.put(36L, "TAV3.MUS");
    map.put(37L, "TAV4.MUS");
    map.put(38L, "DEATH.MUS");

    if (SongList.isEmpty()) {
      SongList.add(new RefEntry(0xfffffffeL, "Continue area music"));
      SongList.add(new RefEntry(0xffffffffL, "Continue outside music"));
      long[] keys = map.keys();
      for (final long key: keys) {
        String ref = map.get(key);
        SongList.add(new RefEntry(key, ref, null, searchDirs));
      }
    }
    return SongList;
  }

  public static String getTableName()
  {
    return TableName;
  }

  public static synchronized void resetSonglist()
  {
    Table2daCache.cacheInvalid(ResourceFactory.getResourceEntry(TableName));
    SongList.clear();
    TableName = "";
    FormatString = FMT_REF_NAME_VALUE;
  }
}
