// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;

public final class ProRef extends ResourceBitmap {
  private static final ArrayList<RefEntry> PRO_MISSILE_LIST = new ArrayList<>();
  private static final ArrayList<RefEntry> PRO_LIST = new ArrayList<>();

  public ProRef(ByteBuffer buffer, int offset, String name) {
    this(buffer, offset, 2, name, true);
  }

  public ProRef(ByteBuffer buffer, int offset, String name, boolean useMissile) {
    this(buffer, offset, 2, name, useMissile);
  }

  public ProRef(ByteBuffer buffer, int offset, int size, String name) {
    this(buffer, offset, size, name, true);
  }

  public ProRef(ByteBuffer buffer, int offset, int size, String name, boolean useMissile) {
    super(buffer, offset, size, name, createRefList(useMissile), null, useMissile ? FMT_NAME_REF_VALUE : FMT_REF_VALUE);
  }

  public ResourceEntry getSelectedEntry() {
    return ResourceFactory.getResourceEntry(getResourceName(), true);
  }

  /** Called whenever a new game is opened. */
  public static synchronized void clearCache() {
    PRO_MISSILE_LIST.clear();
    PRO_LIST.clear();
  }

  /** Returns a list of projectiles with associated indices and search names. */
  public static List<RefEntry> createRefList(boolean useMissile) {
    if (useMissile) {
      return createProMissileRefList();
    } else {
      return createProRefList();
    }
  }

  private static synchronized List<RefEntry> createProMissileRefList() {
    if (PRO_MISSILE_LIST.isEmpty()) {
      // preparing cached list
      IdsMap mslMap = IdsMapCache.get("MISSILE.IDS");
      IdsMap proMap = IdsMapCache.get("PROJECTL.IDS");

      int maxSize = Math.max(mslMap.size(), proMap.size());
      PRO_MISSILE_LIST.ensureCapacity(2 + maxSize);

      if (mslMap.get(0L) == null) {
        PRO_MISSILE_LIST.add(new RefEntry(0L, "None", "Default"));
      }
      if (mslMap.get(1L) == null) {
        PRO_MISSILE_LIST.add(new RefEntry(1L, "None", "None"));
      }

      for (final Long key : mslMap.getKeys()) {
        long k = key;
        IdsMapEntry mslEntry = mslMap.get(k);
        IdsMapEntry proEntry = proMap.get(k - 1L);
        final RefEntry entry;
        if (proEntry != null) {
          entry = new RefEntry(k, proEntry.getSymbol().toUpperCase(Locale.ENGLISH) + ".PRO", mslEntry.getSymbol());
        } else {
          entry = new RefEntry(key, "None", mslEntry.getSymbol());
        }
        PRO_MISSILE_LIST.add(entry);
      }
    }
    return PRO_MISSILE_LIST;
  }

  private static synchronized List<RefEntry> createProRefList() {
    if (PRO_LIST.isEmpty()) {
      IdsMap proMap = IdsMapCache.get("PROJECTL.IDS");
      PRO_LIST.ensureCapacity(2 + proMap.size());

      if (proMap.get(0L) == null) {
        PRO_LIST.add(new RefEntry(0L, "None", "None"));
      }

      for (final IdsMapEntry e : proMap.getAllValues()) {
        PRO_LIST.add(new RefEntry(e.getID(), e.getSymbol().toUpperCase(Locale.ENGLISH) + ".PRO", e.getSymbol()));
      }
    }
    return PRO_LIST;
  }
}
