// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Objects;

import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.io.StreamUtils;

/**
 * Provides useful information about equippable items.
 */
public class ItemInfo
{
  private final ColorInfo colorInfo = new ColorInfo();
  private final ResourceEntry itmEntry;

  private int flags;
  private int category;
  private int unusable;
  private int unusableKits;
  private String appearance;
  // list of item abilities (sorted by index): contains ability type (1=melee, 2=range, 3=magical, 4=launcher)
  private int[] abilities;

  /**
   * This is a convenience method to speed up the process.
   * Returns the category of the specified item resource. Returns -1 if category could not be determined.
   */
  public static int getItemCategory(ResourceEntry itmEntry)
  {
    if (itmEntry != null) {
      try (final InputStream is = itmEntry.getResourceDataAsStream()) {
        byte[] sig = new byte[8];
        Misc.requireCondition(is.read(sig) == 8, "Could not read signature field");
        final String signature = new String(sig).toUpperCase(Locale.ENGLISH);
        switch (signature) {
          case "ITM V1  ":
          case "ITM V1.1":
          case "ITM V2.0":
            break;
          default:
            throw new Exception("Not an item resource: " + itmEntry.getResourceName());
        }
        Misc.requireCondition(is.skip(0x14) == 0x14, "Could not advance in data stream");
        return StreamUtils.readShort(is);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return -1;
  }

  public ItemInfo(ResourceEntry itmEntry) throws Exception
  {
    this.itmEntry = Objects.requireNonNull(itmEntry, "ITM resource cannot be null");
    init();
  }

  /** Returns the {@code ResourceEntry} instance of the item resource. */
  public ResourceEntry getItemEntry() { return itmEntry; }

  /** Returns the item flags. */
  public int getFlags() { return flags; }

  /** Returns the item category. */
  public int getCategory() { return category; }

  /** Returns the general usability flags. */
  public int getUnusable() { return unusable; }

  /** Returns the kit-specific usability flags (packed value, kits #1 at bit 24, ..., kits #4 at bit 0). */
  public int getUnusableKits() { return unusableKits; }

  /** Returns the two-letter appearance code. */
  public String getAppearance() { return appearance; }

  /** Provides access to the {@link ColorInfo} instance associated with the item. */
  public ColorInfo getColorInfo() { return colorInfo; }

  /** Returns number of defined item abilities. */
  public int getAbilityCount() { return abilities.length; }

  /** Returns the type of the specified ability. */
  public int getAbility(int index) { return (index >= 0 && index < abilities.length) ? abilities[index] : -1; }

  private void init() throws IOException, Exception
  {
    try (final InputStream is = itmEntry.getResourceDataAsStream()) {
      byte[] sig = new byte[8];
      // offset = 0x00
      Misc.requireCondition(is.read(sig) == 8, "Could not read signature field");
      final String signature = new String(sig).toUpperCase(Locale.ENGLISH);
      switch (signature) {
        case "ITM V1  ":
        case "ITM V1.1":
        case "ITM V2.0":
          break;
        default:
          throw new Exception("Not an item resource: " + itmEntry.getResourceName());
      }

      // flags (common)
      Misc.requireCondition(is.skip(0x10) == 0x10, "Could not advance in data stream");
      // offset = 0x18
      this.flags = StreamUtils.readInt(is);

      // category (common)
      // offset = 0x1c
      this.category = StreamUtils.readShort(is);

      // unusable (common)
      // offset = 0x1e
      this.unusable = StreamUtils.readInt(is);

      // appearance (common)
      // offset = 0x22
      this.appearance = StreamUtils.readString(is, 2);

      // unusableKits (common for V1/V2.0 only)
      this.unusableKits = 0;
      if (!"ITM V1.1".equals(signature)) {
        int v;
        Misc.requireCondition(is.skip(5) == 5, "Could not advance in data stream");
        // offset = 0x29
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field");
        this.unusableKits |= (v << 24);
        Misc.requireCondition(is.skip(1) == 1, "Could not advance in data stream");
        // offset = 0x2b
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field");
        this.unusableKits |= (v << 16);
        Misc.requireCondition(is.skip(1) == 1, "Could not advance in data stream");
        // offset = 0x2d
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field");
        this.unusableKits |= (v << 8);
        Misc.requireCondition(is.skip(1) == 1, "Could not advance in data stream");
        // offset = 0x2f
        Misc.requireCondition((v = is.read()) != -1, "Could not read kits usability field");
        this.unusableKits |= v;
      } else {
        // to keep stream position synchronized
        Misc.requireCondition(is.skip(0xc) == 0xc, "Could not advance in data stream");
      }

      // abilities (common: ofs, num, header)
      Misc.requireCondition(is.skip(0x34) == 0x34, "Could not advance in data stream");
      // offset = 0x64
      int ofsAbil = StreamUtils.readInt(is);    // abilities offset
      int numAbil = StreamUtils.readShort(is);  // abilities count
      int ofsFx = StreamUtils.readInt(is);      // effects offset
      int idxFx = StreamUtils.readShort(is);    // start index of global effects
      int numFx = StreamUtils.readShort(is);    // total effects count
      ofsFx += idxFx * 0x30;                    // adjusting global effects offset
      numFx -= idxFx;                           // adjusting global effects count
      int curOfs = 0x72;                        // end of main structure

      // reading global color effects (attempt 1)
      int skip;
      if (ofsFx < ofsAbil) {
        skip = ofsFx - curOfs;
        Misc.requireCondition(is.skip(skip) == skip, "Could not advance in data stream");
        curOfs += skip;
        // offset = [ofsFx]
        curOfs += readEffects(is, numFx);
      }

      // reading ability types
      skip = ofsAbil - curOfs;
      Misc.requireCondition(skip >= 0 && is.skip(skip) == skip, "Could not advance in data stream");
      curOfs += skip;
      // offset = [ofsAbil]
      curOfs += readAbilities(is, numAbil);

      // reading global color effects (attempt 2)
      if (ofsFx >= ofsAbil) {
        skip = ofsFx - curOfs;
        Misc.requireCondition(skip >= 0 && is.skip(skip) == skip, "Could not advance in data stream");
        curOfs += skip;
        // offset = [ofsFx]
        curOfs += readEffects(is, numFx);
      }
    }
  }

  // Processes global effects: only "set color" effect is considered
  private int readEffects(InputStream is, int num) throws Exception
  {
    int retVal = 0;
    while (num > 0) {
      int opcode = StreamUtils.readShort(is);
      if (opcode == 7) {
        // set color
        int target = is.read();
        is.read();  // skip power
        int param1 = StreamUtils.readInt(is);
        int param2 = StreamUtils.readInt(is);
        int timing = is.read();
        Misc.requireCondition(is.skip(0x23) == 0x23, "Could not advance in data stream");
        if (target == 1 && timing ==2) {
          // self target; on equip
          SegmentDef.SpriteType type = null;
          int location = param2 & 0xf;
          switch ((param2 >> 4) & 0xf) {
            case 0:
              type = SegmentDef.SpriteType.AVATAR;
              break;
            case 1:
              type = SegmentDef.SpriteType.WEAPON;
              break;
            case 2:
              type = SegmentDef.SpriteType.SHIELD;
              break;
            case 3:
              type = SegmentDef.SpriteType.HELMET;
              break;
            default:
              if ((param2 & 0xff) == 0xff) {
                type = SegmentDef.SpriteType.AVATAR;
                location = -1;
              }
          }
          getColorInfo().add(type, location, param1);
        }
      } else {
        // sync stream offset
        Misc.requireCondition(is.skip(0x2e) == 0x2e, "Could not advance in data stream");
      }
      retVal += 0x30;
      num--;
    }
    return retVal; // returns number of bytes read or skipped
  }

  private int readAbilities(InputStream is, int num) throws Exception
  {
    int retVal = 0;
    this.abilities = new int[num];
    while (num > 0) {
      this.abilities[this.abilities.length - num] = StreamUtils.readShort(is);
      retVal += 2;
      Misc.requireCondition(is.skip(0x36) == 0x36, "Could not advance in data stream");
      retVal += 0x36;
      num--;
    }
    return retVal; // returns number of bytes read or skipped
  }

  @Override
  public int hashCode()
  {
    int hash = 7;
    hash = 31 * hash + ((colorInfo == null) ? 0 : colorInfo.hashCode());
    hash = 31 * hash + ((itmEntry == null) ? 0 : itmEntry.hashCode());
    hash = 31 * hash + flags;
    hash = 31 * hash + category;
    hash = 31 * hash + unusable;
    hash = 31 * hash + unusableKits;
    hash = 31 * hash + ((appearance == null) ? 0 : appearance.hashCode());
    hash = 31 * hash + ((abilities == null) ? 0 : abilities.hashCode());
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof ItemInfo)) {
      return false;
    }
    ItemInfo other = (ItemInfo)o;
    boolean retVal = (this.colorInfo == null && other.colorInfo == null) ||
                     (this.colorInfo != null && this.colorInfo.equals(other.colorInfo));
    retVal &= (this.itmEntry == null && other.itmEntry == null) ||
              (this.itmEntry != null && this.itmEntry.equals(other.itmEntry));
    retVal &= this.flags == other.flags;
    retVal &= this.category == other.category;
    retVal &= this.unusable == other.unusable;
    retVal &= this.unusableKits == other.unusableKits;
    retVal &= (this.appearance == null && other.appearance == null) ||
              (this.appearance != null && this.appearance.equals(other.appearance));
    retVal &= (this.abilities == null && other.abilities == null) ||
              (this.abilities != null && this.abilities.equals(other.abilities));
    return retVal;
  }
}
