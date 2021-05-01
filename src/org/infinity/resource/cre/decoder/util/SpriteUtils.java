// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.browser.icon.Icons;
import org.infinity.resource.cre.decoder.AmbientDecoder;
import org.infinity.resource.cre.decoder.AmbientStaticDecoder;
import org.infinity.resource.cre.decoder.CharacterDecoder;
import org.infinity.resource.cre.decoder.CharacterOldDecoder;
import org.infinity.resource.cre.decoder.PlaceholderDecoder;
import org.infinity.resource.cre.decoder.EffectDecoder;
import org.infinity.resource.cre.decoder.FlyingDecoder;
import org.infinity.resource.cre.decoder.MonsterAnkhegDecoder;
import org.infinity.resource.cre.decoder.MonsterDecoder;
import org.infinity.resource.cre.decoder.MonsterIcewindDecoder;
import org.infinity.resource.cre.decoder.MonsterLarge16Decoder;
import org.infinity.resource.cre.decoder.MonsterLargeDecoder;
import org.infinity.resource.cre.decoder.MonsterLayeredDecoder;
import org.infinity.resource.cre.decoder.MonsterLayeredSpellDecoder;
import org.infinity.resource.cre.decoder.MonsterMultiDecoder;
import org.infinity.resource.cre.decoder.MonsterMultiNewDecoder;
import org.infinity.resource.cre.decoder.MonsterOldDecoder;
import org.infinity.resource.cre.decoder.MonsterPlanescapeDecoder;
import org.infinity.resource.cre.decoder.MonsterQuadrantDecoder;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.TownStaticDecoder;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.graphics.BamV1Decoder;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.GraphicsResource;
import org.infinity.resource.graphics.BamDecoder.FrameEntry;
import org.infinity.resource.graphics.BamV1Decoder.BamV1Control;
import org.infinity.resource.key.BufferedResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapCache;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

/**
 * Collection of helpful methods for Sprite rendering.
 */
public class SpriteUtils
{
  /** Cache for source BAM resources (decoder and attached controller). */
  private static final HashMap<ResourceEntry, Couple<BamV1Decoder, BamV1Decoder.BamV1Control>> bamCache = new HashMap<>();
  /** Cache for replacement palettes. */
  private static final HashMap<ResourceEntry, int[]> paletteCache = new HashMap<>();
  /** Cache for color gradients. */
  private static final HashMap<Integer, int[]> colorGradients = new HashMap<>(350);
  /** Cache for randomized color gradients. */
  private static final HashMap<Integer, int[]> randomGradientIndices = new HashMap<>();
  /** A random number generator for general use */
  private static final Random random = new Random();

  /** Mappings between animation types and compatible sprite classes. */
  private static final EnumMap<AnimationInfo.Type, Class<? extends SpriteDecoder>> typeAssociations =
      new EnumMap<AnimationInfo.Type, Class<? extends SpriteDecoder>>(AnimationInfo.Type.class) {{
        put(AnimationInfo.Type.EFFECT, EffectDecoder.class);
        put(AnimationInfo.Type.MONSTER_QUADRANT, MonsterQuadrantDecoder.class);
        put(AnimationInfo.Type.MONSTER_MULTI, MonsterMultiDecoder.class);
        put(AnimationInfo.Type.MONSTER_MULTI_NEW, MonsterMultiNewDecoder.class);
        put(AnimationInfo.Type.MONSTER_LAYERED_SPELL, MonsterLayeredSpellDecoder.class);
        put(AnimationInfo.Type.MONSTER_ANKHEG, MonsterAnkhegDecoder.class);
        put(AnimationInfo.Type.TOWN_STATIC, TownStaticDecoder.class);
        put(AnimationInfo.Type.CHARACTER, CharacterDecoder.class);
        put(AnimationInfo.Type.CHARACTER_OLD, CharacterOldDecoder.class);
        put(AnimationInfo.Type.MONSTER, MonsterDecoder.class);
        put(AnimationInfo.Type.MONSTER_OLD, MonsterOldDecoder.class);
        put(AnimationInfo.Type.MONSTER_LAYERED, MonsterLayeredDecoder.class);
        put(AnimationInfo.Type.MONSTER_LARGE, MonsterLargeDecoder.class);
        put(AnimationInfo.Type.MONSTER_LARGE_16, MonsterLarge16Decoder.class);
        put(AnimationInfo.Type.AMBIENT_STATIC, AmbientStaticDecoder.class);
        put(AnimationInfo.Type.AMBIENT, AmbientDecoder.class);
        put(AnimationInfo.Type.FLYING, FlyingDecoder.class);
        put(AnimationInfo.Type.MONSTER_ICEWIND, MonsterIcewindDecoder.class);
        put(AnimationInfo.Type.MONSTER_PLANESCAPE, MonsterPlanescapeDecoder.class);
      }};

  /** A stable pool of random numbers. */
  private static int[] randomPool;

  /** Clears cached resources. */
  public static void clearCache()
  {
    clearBamCache();
    clearColorCache();
  }

  /** Clears BAM cache only. */
  public static void clearBamCache()
  {
    bamCache.clear();
  }

  /** Clears all palette-related caches. */
  public static void clearColorCache()
  {
    paletteCache.clear();
    colorGradients.clear();
    randomGradientIndices.clear();
  }

  /**
   * Creates a pseudo CRE resource with the specified animation id and an optional set of creature colors.
   * @param animationId The animation id for the CRE.
   * @param colors Optional creature colors. Each entry uses the CRE_COLOR_xxx field names defined in {@link CreResource}
   *               associated with the numeric color value.
   * @param equipment Optional equipment. Each entry uses inventory slot index (as defined in CRE) as key and
   *                  item resref as value.
   *                  These items are only used as source for overlay bams (i.e. weapons, shields, helmets).
   * @return A {@code CreResource} instance with the virtual creature data.
   */
  public static CreResource getPseudoCre(int animationId, HashMap<String, Integer> colors, HashMap<Integer, String> equipment) throws Exception
  {
    CreResource entry = null;
    ByteBuffer buffer = null;
    if ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V90)) {
      // IWD
      int sizeBase = 0x33c;
      int slotsSize = 0x50; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V9.0".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          switch (e.getKey()) {
            case CreResource.CRE_COLOR_METAL:
              buffer.put(0x2c, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MINOR:
              buffer.put(0x2d, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MAJOR:
              buffer.put(0x2e, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_SKIN:
              buffer.put(0x2f, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_LEATHER:
              buffer.put(0x30, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_ARMOR:
              buffer.put(0x31, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_HAIR:
              buffer.put(0x32, e.getValue().byteValue());
              break;
          }
        }
      }
      // Enemy-Ally
      buffer.put(0x2d8, (byte)128);
      // setting valid offsets
      for (int ofs : new int[] {0x308, 0x310, 0x318, 0x320, 0x324, 0x32c}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    } else if ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V22)) {
      // IWD2
      int sizeBase = 0x62e;
      int slotsSize = 0x68; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V2.2".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          switch (e.getKey()) {
            case CreResource.CRE_COLOR_METAL:
              buffer.put(0x2c, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MINOR:
              buffer.put(0x2d, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MAJOR:
              buffer.put(0x2e, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_SKIN:
              buffer.put(0x2f, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_LEATHER:
              buffer.put(0x30, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_ARMOR:
              buffer.put(0x31, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_HAIR:
              buffer.put(0x32, e.getValue().byteValue());
              break;
          }
        }
      }
      // Enemy-Ally
      buffer.put(0x384, (byte)128);
      // setting valid offsets
      for (int i = 0; i < 63; i++) {
        buffer.putInt(0x3ba + (i * 4), sizeBase);
      }
      for (int i = 0; i < 9; i++) {
        buffer.putInt(0x5b2+ (i * 4), sizeBase);
      }
      for (int ofs : new int[] {0x5fa, 0x602, 0x60a, 0x612, 0x616, 0x61e}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    } else if ((Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_CRE_V12)) {
      // PST
      int sizeBase = 0x378;
      int slotsSize = 0x60; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V1.2".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      int numColors = 0;
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          int value = e.getValue().intValue();
          for (int i = 0; i < 7; i++) {
            String labelColor = String.format(CreResource.CRE_COLOR_FMT, i + 1);
            String labelColorPlacement = String.format(CreResource.CRE_COLOR_PLACEMENT_FMT, i + 1);
            if (labelColor.equals(e.getKey())) {
              buffer.putShort(0x2e4 + (i * 2), (short)value);
            } else if (labelColorPlacement.equals(e.getKey())) {
              buffer.put(0x2f5 + i, (byte)value);
              numColors++;
            }
          }
        }
      }
      buffer.put(0x2df, (byte)numColors);
      // Enemy-Ally
      buffer.put(0x314, (byte)128);
      // setting valid offsets
      for (int ofs : new int[] {0x344, 0x34c, 0x354, 0x35c, 0x360, 0x368}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    } else {
      // BG, BG2, EE
      int sizeBase = 0x2d4;
      int slotsSize = 0x50; // item slots
      buffer = StreamUtils.getByteBuffer(sizeBase + slotsSize);
      buffer.position(0);
      buffer.put("CRE V1.0".getBytes(Misc.CHARSET_ASCII));
      // creature colors
      if (colors != null) {
        for (final HashMap.Entry<String, Integer> e : colors.entrySet()) {
          switch (e.getKey()) {
            case CreResource.CRE_COLOR_METAL:
              buffer.put(0x2c, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MINOR:
              buffer.put(0x2d, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_MAJOR:
              buffer.put(0x2e, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_SKIN:
              buffer.put(0x2f, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_LEATHER:
              buffer.put(0x30, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_ARMOR:
              buffer.put(0x31, e.getValue().byteValue());
              break;
            case CreResource.CRE_COLOR_HAIR:
              buffer.put(0x32, e.getValue().byteValue());
              break;
          }
        }
      }
      // Enemy-Ally
      buffer.put(0x270, (byte)128);
      // setting valid offsets
      for (int ofs : new int[] {0x2a0, 0x2a8, 0x2b0, 0x2b8, 0x2bc, 0x2c4}) {
        buffer.putInt(ofs, sizeBase);
      }
      for (int i = 0; i < slotsSize - 2; i++) {
        // marking item slots as empty
        buffer.put(sizeBase + i, (byte)0xff);
      }
    }

    if (buffer != null) {
      buffer.putInt(0x08, -1);  // creature name
      buffer.putInt(0x0c, -1);  // creature tooltip
      buffer.putInt(0x28, animationId);
      if (equipment != null) {
        for (final HashMap.Entry<Integer, String> itm : equipment.entrySet()) {
          addPseudoCreItem(buffer, itm.getKey().intValue(), itm.getValue());
        }
      }

      entry = new CreResource(null, String.format("%04X", animationId & 0xffff), buffer, 0);
    }

    return entry;
  }

  /**
   * Adds a new item to the pseudo CRE resource and assigns it to an item slot.
   * @param buffer the source CRE buffer.
   * @param slot slot to assign the new item. Specify negative value to skip.
   * @param resref The item resref.
   * @return the updated CRE buffer.
   */
  private static ByteBuffer addPseudoCreItem(ByteBuffer buffer, int slot, String resref)
  {
    ByteBuffer outBuffer = buffer;
    if (buffer == null || resref == null || resref.isEmpty()) {
      return outBuffer;
    }

    // preparing item entry
    ByteBuffer item = StreamUtils.getByteBuffer(20);
    item.position(0);
    item.put(resref.getBytes(Misc.CHARSET_ASCII));

    int numSlots = 0, ofsSlotsOffset = 0, ofsItemsCount = 0;
    String ver = StreamUtils.readString(buffer, 4, 4);
    switch (ver) {
      case "V9.0":
        numSlots = 38;
        ofsSlotsOffset = 0x320;
        ofsItemsCount = 0x328;
        break;
      case "V2.2":
        numSlots = 50;
        ofsSlotsOffset = 0x612;
        ofsItemsCount = 0x61a;
        break;
      case "V1.2":
        numSlots = 46;
        ofsSlotsOffset = 0x35c;
        ofsItemsCount = 0x364;
        break;
      case "V1.0":
        numSlots = 38;
        ofsSlotsOffset = 0x2b8;
        ofsItemsCount = 0x2c0;
        break;
    }

    if (ofsSlotsOffset > 0) {
      outBuffer = StreamUtils.getByteBuffer(buffer.limit() + item.limit());
      int ofsSlots = buffer.getInt(ofsSlotsOffset);
      int numItems = buffer.getInt(ofsItemsCount);
      outBuffer.position(0);
      // adding CRE base
      outBuffer.put(buffer.array(), 0, ofsSlots);
      // adding new item
      outBuffer.put(item.array(), 0, item.limit());
      // adding CRE inventory slots
      outBuffer.put(buffer.array(), ofsSlots, buffer.limit() - ofsSlots);
      // updating items count
      numItems++;
      outBuffer.putInt(ofsItemsCount, numItems);
      // updating slots offset
      ofsSlots += item.limit();
      outBuffer.putInt(ofsSlotsOffset, ofsSlots);
      // assigning item to slot
      if (slot >= 0 && slot < numSlots) {
        outBuffer.putShort(ofsSlots + (slot * 2), (short)(numItems - 1));
      }
    }

    return outBuffer;
  }

  /**
   * Loads the decoder instance for the specified BAM V1 resource. Retrieves the decoder from cache if available.
   * @param entry the BAM resource entry.
   * @return the {@code BamV1Decoder} instance created from the BAM resource.
   *         Returns {@code null} if decoder could not be retrieved.
   */
  public static BamV1Decoder loadBamDecoder(ResourceEntry entry)
  {
    Couple<BamV1Decoder, BamV1Decoder.BamV1Control> retVal = loadBamDecoderController(entry);
    return (retVal != null) ? retVal.getValue0() : null;
  }

  /**
   * Loads the BAM controller instance for the specified BAM V1 resource. Retrieves the object from cache if available.
   * @param entry the BAM resource entry.
   * @return the {@code BamV1Control} instance created from the BAM resource.
   *         Returns {@code null} if the controller could not be retrieved.
   */
  public static BamV1Decoder.BamV1Control loadBamController(ResourceEntry entry)
  {
    Couple<BamV1Decoder, BamV1Decoder.BamV1Control> retVal = loadBamDecoderController(entry);
    return (retVal != null) ? retVal.getValue1() : null;
  }

  /**
   * Loads the decoder instance and an associated controller for the specified BAM V1 resource. Retrieves the objects
   * from cache if available.
   * @param entry the BAM resource entry.
   * @return {@code BamV1Decoder} and {@code BamV1Control} instances created from the BAM resource.
   *         Returns {@code null} if the objects could not be retrieved.
   */
  public static Couple<BamV1Decoder, BamV1Decoder.BamV1Control> loadBamDecoderController(ResourceEntry entry)
  {
    Couple<BamV1Decoder, BamV1Decoder.BamV1Control> retVal = bamCache.getOrDefault(entry, null);
    if (retVal == null) {
      try {
        BamV1Decoder decoder = new BamV1Decoder(entry);
        BamV1Decoder.BamV1Control control = decoder.createControl();
        retVal = Couple.with(decoder, control);
        bamCache.put(entry, retVal);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return retVal;
  }

  /**
   * Returns whether the specified BAM V1 resource exists and contains the specified  range of cycles.
   * @param entry the BAM resource to check.
   * @param cycle start index of cycle range.
   * @param length number of cycles.
   * @return {@code true} if the BAM resource exists, is paletted and contains the specified cycles.
   */
  public static boolean bamCyclesExist(ResourceEntry entry, int cycle, int length)
  {
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      int numCycles = control.cycleCount();
      return (numCycles >= cycle + length && control.cycleFrameCount(cycle) > 0);
    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Returns the number of cycles available in the specified BAM resource.
   * @param entry BAM resource to check.
   * @return number of cycles. Returns 0 if number of cycles could not be determined.
   */
  public static int getBamCycles(ResourceEntry entry)
  {
    int retVal = 0;
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      retVal = control.cycleCount();
    } catch (Exception e) {
    }
    return retVal;
  }

  /**
   * Returns the number of frames available in the specified BAM cycle.
   * @param entry the BAM resource
   * @param cycleIdx the BAM cycle to check.
   * @return number of frames in the specified BAM cycle. Returns 0 if number of frames could not be determined.
   */
  public static int getBamCycleFrames(ResourceEntry entry, int cycleIdx)
  {
    int retVal = 0;
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      if (cycleIdx >= 0 && cycleIdx < control.cycleCount()) {
        retVal = control.cycleFrameCount(cycleIdx);
      }
    } catch (Exception e) {
    }
    return retVal;
  }

  /**
   * Returns whether the specified BAM contains a palette that is most likely replaced by external color ranges.
   * @param entry BAM resource to check
   * @return {@code true} if BAM contains a false color palette, {@code false} otherwise.
   */
  public static boolean bamHasFalseColors(ResourceEntry entry)
  {
    boolean retVal = false;
    try {
      BamV1Decoder.BamV1Control control = loadBamController(entry);
      int[] palette = control.getPalette();
      if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
        retVal = (palette[224] & 0xffffff) == 0x0000ff;
        retVal &= (palette[240] & 0xffffff) == 0x00009f;
      } else {
        retVal = (palette[15] & 0xffffff) == 0x1e1e1e;
        retVal &= (palette[27] & 0xffffff) == 0x004040;
        retVal &= (palette[39] & 0xffffff) == 0x400040;
        retVal &= (palette[51] & 0xffffff) == 0x404000;
        retVal &= (palette[63] & 0xffffff) == 0x400000;
        retVal &= (palette[75] & 0xffffff) == 0x000040;
        retVal &= (palette[87] & 0xffffff) == 0x004000;
      }
    } catch (Exception e) {
    }
    return retVal;
  }

  /**
   * Ensures that "transparent" and "shadow" palette entries of the animation are properly set.
   * @param control the BAM controller associated with the animation.
   * @param isTransparentShadow indicates whether shadow color is semi-transparent
   */
  public static void fixShadowColor(BamV1Control control, boolean isTransparentShadow)
  {
    if (control != null) {
      int[] palette = control.getCurrentPalette();
      palette[0] = 0x0000FF00;
      palette[1] = isTransparentShadow ? 0x80000000 : 0xFF000000;
      control.setExternalPalette(palette);
    }
  }

  /**
   * Applies the specified palette to the BAM animation associated with the specified controller.
   * @param control the BAM controller associated with the animation.
   * @param palette the new palette data.
   */
  public static void applyNewPalette(BamV1Control control, int[] palette)
  {
    if (palette == null) {
      return;
    }
    if (palette.length < 256) {
      palette = Arrays.copyOf(palette, 256);
    }
    control.setExternalPalette(palette);
  }

  /**
   * Loads the palette specified by the resource reference.
   * It looks for a palette in the following order: [resref].BMP, [resref].BAM.
   * @param resref the resource reference to use for loading a palette.
   * @return palette data with 256 entries. Returns {@code null} if palette could not be loaded.
   */
  public static int[] loadReplacementPalette(String resref)
  {
    return loadReplacementPalette(resref, -1);
  }

  /**
   * Loads the palette specified by the resource reference and index.
   * It looks for a palette in the following order: [resref+suffix].BMP, [resref+suffix].BAM, [resref].BMP, [resref].BAM.
   * @param resref the resource reference to use for loading a palette.
   * @param index a numeric suffix added to the resref.
   * @return palette data with 256 entries. Returns {@code null} if palette could not be loaded.
   */
  public static int[] loadReplacementPalette(String resref, int index)
  {
    int[] retVal = null;
    if (resref == null || resref.isEmpty()) {
      return retVal;
    }

    String resName = resref;
    String suffix = (index >= 0) ? Integer.toString(index) : "";
    String[] suffixList = (suffix.isEmpty()) ? new String[] {""} : new String[] {suffix, ""};
    ResourceEntry entry = null;
    for (final String s : suffixList) {
      if (ResourceFactory.resourceExists(resName + s + ".BMP")) {
        entry = ResourceFactory.getResourceEntry(resName + s + ".BMP");
        break;
      }
    }
    if (entry == null) {
      return retVal;
    }

    retVal = paletteCache.getOrDefault(entry, null);
    if (retVal == null) {
      try {
        retVal = ColorConvert.loadPaletteBMP(entry);
        if (retVal != null) {
          if (retVal.length < 256) {
            retVal = Arrays.copyOf(retVal, 256);
          }
          paletteCache.put(entry, retVal);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return retVal;
  }

  /**
   * Replaces a range of color entries in the palette with new entries.
   * @param palette the palette to modify.
   * @param newColors array of colors to add.
   * @param startOffset first color entry in {@code palette} to update.
   * @param len number of color entries to update. Parameter is adjusted if {@code newColors} array contains
   *            less entries than {@code len} or range would exceed palette size.
   * @param allowAlpha {@code true} to use alpha from {@code newColors}; {@code false} to discard alpha values.
   * @return the updated palette.
   */
  public static int[] replaceColors(int[] palette, int[] newColors, int startOffset, int len, boolean allowAlpha)
  {
    int[] retVal = palette;
    if (palette != null && newColors != null) {
      retVal = Arrays.copyOf(palette, palette.length);
      if (newColors.length > 0 && startOffset >= 0 && startOffset < retVal.length) {
        len = Math.min(len, newColors.length);
        len = Math.min(len, retVal.length - startOffset);
        int mask = allowAlpha ? 0xffffffff : 0x00ffffff;
        for (int i = 0; i < len; i++) {
          retVal[startOffset + i] = newColors[i] & mask;
        }
      }
    }
    return retVal;
  }

  /**
   * Interpolates two palette ranges and stores the result in a third palette range.
   * @param palette the palette to modify.
   * @param srcOfs1 start offset of first source color range.
   * @param srcOfs2 start offset of second source color range.
   * @param srcLen number of source color entries to read.
   * @param dstOfs start offset of target color range.
   * @param dstLen number of interpolated color entries to write.
   * @param allowAlpha {@code true} to interpolate alpha, {@code false} to discard alpha values.
   * @return the updated palette.
   */
  public static int[] interpolateColors(int[] palette, int srcOfs1, int srcOfs2, int srcLen,
                                        int dstOfs, int dstLen, boolean allowAlpha)
  {
    int[] retVal = palette;
    if (palette != null && srcLen > 0 && dstLen > 0 &&
        srcOfs1 >= 0 && srcOfs1 + srcLen <= palette.length &&
        srcOfs2 >= 0 && srcOfs2 + srcLen <= palette.length &&
        dstOfs >= 0 && dstOfs + dstLen <= palette.length) {
      retVal = Arrays.copyOf(palette, palette.length);
      for (int dstIdx = 0; dstIdx < dstLen; dstIdx++) {
        int srcIdx = dstIdx * srcLen / dstLen;
        int r = ((retVal[srcOfs1 + srcIdx] & 0xff) + (retVal[srcOfs2 + srcIdx] & 0xff)) >>> 1;
        int g = (((retVal[srcOfs1 + srcIdx] >> 8) & 0xff) + ((retVal[srcOfs2 + srcIdx] >> 8) & 0xff)) >>> 1;
        int b = (((retVal[srcOfs1 + srcIdx] >> 16) & 0xff) + ((retVal[srcOfs2 + srcIdx] >> 16) & 0xff)) >>> 1;
        int a = 0xff000000;
        if (allowAlpha) {
          a = (((retVal[srcOfs1 + srcIdx] >> 24) & 0xff) + ((retVal[srcOfs2 + srcIdx] >> 24) & 0xff)) >>> 1;
        }
        retVal[dstOfs + dstIdx] = (a << 24) | (b << 16) | (g << 8) | r;
      }
    }
    return retVal;
  }

  /**
   * Returns the specified color gradient. Colors are retrieved from the game-specific gradient resource.
   * Optionally takes random color entry definitions into account.
   * @param index Index of the color gradient.
   * @param allowRandom whether random color entries are taken into account.
   * @return the gradient as array of colors. Returns {@code null} if color index does not exist.
   */
  public static int[] getColorGradient(int index, boolean allowRandom)
  {
    if (colorGradients.isEmpty()) {
      // initializing color gradient map on demand
      ResourceEntry palFile = null;
      if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
        palFile = ResourceFactory.getResourceEntry("PAL32.BMP");
      } else if (ResourceFactory.resourceExists("RANGES12.BMP")) {
        palFile = ResourceFactory.getResourceEntry("RANGES12.BMP");
      } else if (ResourceFactory.resourceExists("MPALETTE.BMP")) {
        palFile = ResourceFactory.getResourceEntry("MPALETTE.BMP");
      }

      if (palFile != null) {
        try {
          BufferedImage image = new GraphicsResource(palFile).getImage();
          for (int y = 0; y < image.getHeight(); y++) {
            int[] pixels = image.getRGB(0, y, image.getWidth(), 1, null, 0, image.getWidth());
            colorGradients.put(y, pixels);
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      } else {
        // dummy entry to skip continuous gradient initialization attempts if gradient bitmap isn't available
        colorGradients.put(Integer.MIN_VALUE, null);
      }
    }

    int[] retVal = allowRandom ? getRandomColorGradient(index) : null;
    if (retVal == null) {
      retVal = colorGradients.getOrDefault(index, null);
    }

    return retVal;
  }

  /**
   * Attempts to resolve a random color entry to a real color entry.
   * @param index random color entry index.
   * @return a randomly chosen color gradient from the random color entry list.
   *         Returns {@code null} if no real color gradient could be determined.
   */
  public static int[] getRandomColorGradient(int index)
  {
    if (randomGradientIndices.isEmpty()) {
      if (ResourceFactory.resourceExists("RANDCOLR.2DA")) {
        Table2da table = Table2daCache.get("RANDCOLR.2DA");
        if (table != null && table.getRowCount() > 1) {
          for (int col = 1, numCols = table.getColCount(); col < numCols; col++) {
            // random entry index
            int randIdx = Misc.toNumber(table.get(0, col), -1);
            if (randIdx >= 0) {
              int[] indices = new int[table.getRowCount() - 1];
              for (int row = 1, numRows = table.getRowCount(); row < numRows; row++) {
                indices[row - 1] = Misc.toNumber(table.get(row, col), 0);
              }
              randomGradientIndices.put(randIdx, indices);
            }
          }
        }
      } else {
        // dummy entry to skip continuous random gradient initialization attempts if RANDCOLR.2DA isn't available
        randomGradientIndices.put(Integer.MIN_VALUE, null);
      }
    }

    int failCounter = 100;
    int[] retVal = null;
    int[] indices = randomGradientIndices.getOrDefault(index, null);
    while (retVal == null && indices != null && indices.length > 0 && failCounter-- > 0) {
      int idx = indices[getRandomInt(index, false) % indices.length];
      if (randomGradientIndices.containsKey(idx)) {
        // random color entries may refer to other random color entries
        indices = randomGradientIndices.getOrDefault(index, null);
      } else {
        retVal = getColorGradient(idx, false);
      }
    }

    return retVal;
  }

  /**
   * Returns a random number from a predefined pool of random numbers.
   * @param index Index into the random number pool.
   * @param allowNegative whether negative return values are allowed.
   * @return the pseudo-random number specified by the index.
   */
  public static int getRandomInt(int index, boolean allowNegative)
  {
    if (randomPool == null) {
      updateRandomPool();
    }
    int retVal = randomPool[index % randomPool.length];
    if (!allowNegative && retVal < 0) {
      retVal = -retVal;
    }
    return retVal;
  }

  /** Recreates the pool of random numbers. */
  public static void updateRandomPool()
  {
    if (randomPool == null) {
      randomPool = new int[256];
    }
    for (int i = 0; i < randomPool.length; i++) {
      randomPool[i] = random.nextInt();
    }
  }

  /**
   * Modifies the specified range of palette entries according to the opcode and color specifications.
   * @param palette ARGB palette to modify.
   * @param startOfs index of first color entry to modify.
   * @param length number of color entries to modify.
   * @param opcode effect opcode to apply. Supported opcodes: 8, 51, 52
   * @param color color value associated with the opcode.
   * @return the modified palette.
   */
  public static int[] tintColors(int[] palette, int startOfs, int length, int opcode, int color)
  {
    int[] retVal = palette;

    switch (opcode) {
      case EffectInfo.OPCODE_SET_COLOR_GLOW:
      case EffectInfo.OPCODE_TINT_SOLID:
      case EffectInfo.OPCODE_TINT_BRIGHT:
        break;
      default:
        return retVal;
    }

    if (palette != null) {
      retVal = Arrays.copyOf(palette, palette.length);
      startOfs = Math.max(0, Math.min(retVal.length - 1, startOfs));
      length = Math.max(0, Math.min(retVal.length - startOfs, length));

      int dr = (color >> 16) & 0xff;
      int dg = (color >> 8) & 0xff;
      int db = color & 0xff;
      for (int i = 0; i < length; i++) {
        int sr = (retVal[startOfs + i] >> 16) & 0xff;
        int sg = (retVal[startOfs + i] >> 8) & 0xff;
        int sb = retVal[startOfs + i] & 0xff;
        switch (opcode) {
          case EffectInfo.OPCODE_SET_COLOR_GLOW:
            sr = Math.min(255, sr + dr - (sr >>> 2));
            sg = Math.min(255, sg + dg - (sg >>> 2));
            sb = Math.min(255, sb + db - (sb >>> 2));
            break;
          case EffectInfo.OPCODE_TINT_SOLID:
            sr = Math.min(255, dr * sr / 255);
            sg = Math.min(255, dg * sg / 255);
            sb = Math.min(255, db * sb / 255);
            break;
          case EffectInfo.OPCODE_TINT_BRIGHT:
            sr = Math.min(255, sr + (dr * (sr >>> 3)));
            sg = Math.min(255, sg + (dg * (sg >>> 3)));
            sb = Math.min(255, sb + (db * (sb >>> 3)));
            break;
        }
        retVal[startOfs + i] = (retVal[startOfs + i] & 0xff000000) | (sr << 16) | (sg << 8) | sb;
      }
    }

    return retVal;
  }


  /**
   * Calculates a dimension that can contain all the specified source frames.
   * @param frames one or more source frames.
   * @return A rectangle object where x and y indicate the top-left corner relative to the center point.
   *         Width and height specify frame dimension.
   */
  public static Rectangle getTotalFrameDimension(FrameInfo... frames)
  {
    Rectangle retVal = new Rectangle();

    if (frames.length > 0) {
      int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
      for (final FrameInfo fi : frames) {
        BamV1Control ctrl = fi.getController();
        int frameIdx = fi.getFrame();
        frameIdx = ctrl.cycleGetFrameIndexAbsolute(fi.getCycle(), frameIdx);
        FrameEntry entry = fi.getController().getDecoder().getFrameInfo(frameIdx);
        left = Math.min(left, -entry.getCenterX() + fi.getCenterShift().x);
        top = Math.min(top, -entry.getCenterY() + fi.getCenterShift().y);
        right = Math.max(right, entry.getWidth() - entry.getCenterX() + fi.getCenterShift().x);
        bottom = Math.max(bottom, entry.getHeight() - entry.getCenterY() + fi.getCenterShift().y);
      }

      retVal.x = left;
      retVal.y = top;
      retVal.width = right - left;
      retVal.height = bottom - top;
    }

    return retVal;
  }

  /** Expands the rectangle to fit the specified dimension. */
  public static Rectangle updateFrameDimension(Rectangle rect, Dimension dim)
  {
    Rectangle retVal = new Rectangle(Objects.requireNonNull(rect, "Bounding box cannot be null"));
    if (dim != null) {
      int w2 = dim.width / 2;
      int h2 = dim.height / 2;
      int left = retVal.x;
      int top = retVal.y;
      int right = left + retVal.width;
      int bottom = top + retVal.height;
      left = Math.min(left, -w2);
      top = Math.min(top, -h2);
      right = Math.max(right, w2);
      bottom = Math.max(bottom, h2);
      retVal.x = left;
      retVal.y = top;
      retVal.width = right - left;
      retVal.height = bottom - top;
    }
    return retVal;
  }


  /**
   * Determines the right allegiance color for selection circles and returns it as {@code Color} object.
   * A negative value will enable the "panic" color.
   * @param value numeric allegiance value. Specify a negative value to override allegiance by the "panic" status.
   * @return a {@code Color} object with the associated allegiance or status color.
   */
  public static Color getAllegianceColor(int value)
  {
    Color c = null;
    if (value < 0) {
      // treat as panic
      c = new Color(0xffff20, false);
    } else if (value >= 2 && value <= 4 || value == 201) {
      // ally
      c = new Color(0x20ff20, false);
    } else if (value == 255 || value == 254 || value == 28 || value == 6 || value == 5) {
      // enemy
      c = new Color(0xff2020, false);
    } else {
      // neutral
      c = new Color(0x20ffff, false);
    }

    return c;
  }

  /**
   * Determines the right selection circle bitmap based on the specified allegiance value and returns it
   * as {@code Image} object. A negative value will enable the "panic" bitmap.
   * @param value numeric allegiance value. Specify a negative value to override allegiance by the "panic" status.
   * @return
   */
  public static Image getAllegianceImage(int value)
  {
    Image retVal = null;
    if (value < 0) {
      // treat as panic
      retVal = Icons.getImage(Icons.ICON_CIRCLE_YELLOW);
    } else if (value >= 2 && value <= 4 || value == 201) {
      // ally
      retVal = Icons.getImage(Icons.ICON_CIRCLE_GREEN);
    } else if (value == 255 || value == 254 || value == 28 || value == 6 || value == 5) {
      // enemy
      retVal = Icons.getImage(Icons.ICON_CIRCLE_RED);
    } else {
      // neutral
      retVal = Icons.getImage(Icons.ICON_CIRCLE_BLUE);
    }

    return retVal;
  }


  /**
   * Returns the {@code SpriteClass} class associated with the specified {@code AnimationType} enum.
   * @param type the {@code AnimationType}
   * @return the associated {@code SpriteClass} class object. Returns {@code null} if class could not be determined.
   */
  public static Class<? extends SpriteDecoder> getSpriteDecoderClass(AnimationInfo.Type type)
  {
    return typeAssociations.get(type);
  }

  /**
   * Returns the {@code SpriteClass} class associated with the specified animation id.
   * @param animationId the animation id
   * @return a class type compatible with the specified animation id.
   *         Returns {@code null} if no class could be determined.
   */
  public static Class<? extends SpriteDecoder> getSpriteDecoderClass(int animationId)
  {
    Class<? extends SpriteDecoder> retVal = null;

    // Testing Infinity Animation range first
    AnimationInfo.Type animType = AnimationInfo.Type.containsInfinityAnimations(animationId);
    if (animType != null) {
      retVal = typeAssociations.get(animType);
    }

    // Testing regular ranges
    if (retVal == null) {
      for (final AnimationInfo.Type type : AnimationInfo.Type.values()) {
        if (type.contains(animationId)) {
          retVal = typeAssociations.get(type);
          if (retVal != null) {
            break;
          }
        }
      }
    }

    return retVal;
  }

  /**
   * Returns creature animation info in INI format. Section and field format is based on the EE v2.0 INI format.
   * The method will first look for existing INI data in the game resources. Failing that it will look up data in
   * hardcoded tables and fill in missing data from associated 2DA file if available. Failing that it will guess
   * the correct format based on animation type and available resources.
   * @param animationId the 16-bit animation id.
   * @return An IniMap structure containing necessary data for rendering creature animation. Returns {@code null} if no
   *         animation info could be assembled.
   */
  public static IniMap getAnimationInfo(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();

    // 1. look up existing INI resource
    retVal.addAll(getAnimationInfoByIni(animationId));

    if (retVal.isEmpty()) {
      // 2. look up hardcoded tables
      retVal.addAll(getAnimationInfoByTable(animationId));
    }

    if (retVal.isEmpty()) {
      // 3. guess animation info based on anisnd.2da entry and available sprite classes
      retVal.addAll(getAnimationInfoByGuess(animationId));
    }

    if (!retVal.isEmpty()) {
      return retVal.get(0);
    } else {
      return null;
    }
  }

  /**
   * Attempts to determine the animation type assigned to the specified creature.
   * @return Class instance responsible for handling the detected animation type. {@code null} if type could not be determined.
   */
  public static Class<? extends SpriteDecoder> detectAnimationType(int animationId)
  {
    Class<? extends SpriteDecoder> retVal = null;

    List<IniMap> iniList = new ArrayList<>();
    iniList.addAll(getAnimationInfoByIni(animationId));

    if (iniList.isEmpty()) {
      iniList.addAll(getAnimationInfoByTable(animationId));
    }

    if (iniList.isEmpty()) {
      iniList.addAll(getAnimationInfoByGuess(animationId));
    }

    if (!iniList.isEmpty()) {
      // trying recommended sprite decoder class first
      Class<? extends SpriteDecoder> cls = getSpriteDecoderClass(animationId);
      if (isSpriteDecoderAvailable(cls, animationId, iniList)) {
        retVal = cls;
      }

      if (retVal == null) {
        // trying out all available sprite decoder classes otherwise
        if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
          if (isSpriteDecoderAvailable(MonsterPlanescapeDecoder.class, animationId, iniList)) {
            retVal = cls;
          }
        } else {
          for (final AnimationInfo.Type type : AnimationInfo.Type.values()) {
            if (type != AnimationInfo.Type.MONSTER_PLANESCAPE) {
              cls = typeAssociations.get(type);
              if (isSpriteDecoderAvailable(cls, animationId, iniList)) {
                retVal = cls;
                break;
              }
            }
          }
        }
      }

      if (retVal == null) {
        // No luck yet? Fall back to placeholder animation!
        retVal = PlaceholderDecoder.class;
      }
    }

    return retVal;
  }

  /**
   * A helper method that parses the specified data array and generates a list of INI lines
   * related to the "general" section.
   * @param data the String array containing data for a specific table entry.
   * @param type the animation type.
   * @return the initialized "general" INI section as list of strings. An empty list otherwise.
   */
  public static List<String> processTableDataGeneral(String[] data, AnimationInfo.Type type)
  {
    List<String> retVal = new ArrayList<>();
    if (data == null || type == null) {
      return retVal;
    }

    int id = SpriteTables.valueToAnimationId(data, SpriteTables.COLUMN_ID, -1);
    if (id < 0) {
      return retVal;
    }
    int ellipse = SpriteTables.valueToInt(data, SpriteTables.COLUMN_ELLIPSE, 16);
    int space = SpriteTables.valueToInt(data, SpriteTables.COLUMN_SPACE, 3);
    int blending = SpriteTables.valueToInt(data, SpriteTables.COLUMN_BLENDING, 0);
    String palette = SpriteTables.valueToString(data, SpriteTables.COLUMN_PALETTE, "");

    int animIndex = SpriteTables.valueToInt(data, SpriteTables.COLUMN_TYPE, -1);
    if (animIndex < 0 || animIndex >= AnimationInfo.Type.values().length || AnimationInfo.Type.values()[animIndex] != type) {
      return retVal;
    }

    int animType = -1;
    for (int i = 0; i < type.getTypeCount(); i++) {
      if (animType < 0 || (id & 0xf000) == type.getType(i)) {
        animType = type.getType(i);
      }
    }

    retVal.add("[general]");
    retVal.add(String.format("animation_type=%04X", animType));
    retVal.add("ellipse=" + ellipse);
    retVal.add("personal_space=" + space);
    if ((blending & 1) == 1) {
      retVal.add("brightest=1");
    }
    if ((blending & 2) == 2) {
      retVal.add("multiply_blend=1");
    }
    if ((blending & 4) == 4) {
      retVal.add("light_source=1");
    }
    if (!palette.isEmpty()) {
      retVal.add("new_palette=" + palette);
    }

    return retVal;
  }

  /**
   * A helper method for PST animations that parses the specified data array and generates a list of INI lines
   * related to the "general" section.
   * @param data the String array containing data for a specific table entry.
   * @return the initialized "general" INI section as list of strings. An empty list otherwise.
   */
  public static List<String> processTableDataGeneralPst(String[] data)
  {
    List<String> retVal = new ArrayList<>();
    if (data == null) {
      return retVal;
    }

    int id = SpriteTables.valueToInt(data, SpriteTables.COLUMN_ID, -1);
    if (id < 0) {
      return retVal;
    }
    int ellipse = SpriteTables.valueToInt(data, SpriteTables.COLUMN_PST_ELLIPSE, 16);
    int space = SpriteTables.valueToInt(data, SpriteTables.COLUMN_PST_SPACE, 3);

    retVal.add("[general]");
    retVal.add("animation_type=F000");
    retVal.add("ellipse=" + ellipse);
    retVal.add("personal_space=" + space);

    return retVal;
  }

  /**
   * Returns whether the specified {@code SpriteDecoder} class is compatible with the given animation id
   * and any of the IniMap definitions.
   */
  private static boolean isSpriteDecoderAvailable(Class<? extends SpriteDecoder> spriteClass, int animationId, List<IniMap> iniList)
  {
    boolean retVal = false;
    if (spriteClass == null || iniList == null) {
      return retVal;
    }

    try {
      Constructor<? extends SpriteDecoder> ctor = spriteClass.getConstructor(int.class, IniMap.class);
      if (ctor != null) {
        for (final IniMap ini : iniList) {
          try {
            retVal = (ctor.newInstance(animationId, ini).getClass() != null);
          } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
          }
        }
      }
    } catch (NoSuchMethodException e) {
    }

    return retVal;
  }

  /**
   * Returns creature animation info from an existing INI file.
   * @param animationId the creature animation id
   * @return an list of {@link IniMap} instances with potential creature animation data.
   *         Returns {@code null} if no matching INI was found.
   */
  private static List<IniMap> getAnimationInfoByIni(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();

    animationId &= 0xffff;
    String iniFile = String.format("%04X.INI", animationId);
    if (ResourceFactory.resourceExists(iniFile)) {
      retVal.add(new IniMap(ResourceFactory.getResourceEntry(iniFile), true));
    }

    return retVal;
  }

  /**
   * Returns creature animation info from hardcoded creature data.
   * @param animationId the creature animation id
   * @return an list of {@link IniMap} instance with potential creature animation data.
   *         Returns empty list if no creature data was found.
   */
  private static List<IniMap> getAnimationInfoByTable(int animationId)
  {
    return SpriteTables.createIniMaps(animationId & 0xffff);
  }

  /**
   * Returns creature animation info based on ANISND.2DA data and analyzing potential slot ranges.
   * May return false positives.
   * @param animationId the creature animation id
   * @return a list of {@link IniMap} instances with potential creature animation data.
   *         Returns {@code null} if no potential match was found.
   */
  private static List<IniMap> getAnimationInfoByGuess(int animationId)
  {
    if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
      return guessIniMapsPst(animationId);
    } else {
      return guessIniMaps(animationId);
    }
  }

  // Attempts to find potential non-PST-specific IniMap instances
  private static List<IniMap> guessIniMaps(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    String resref = null;
    String palette = null;

    // evaluate ANIMATE.SRC if available
    ResourceEntry resEntry = ResourceFactory.getResourceEntry("ANIMATE.SRC");
    if (resEntry != null) {
      IniMap anisrc = IniMapCache.get(resEntry);
      if (anisrc != null) {
        IniMapSection iniSection = anisrc.getUnnamedSection();
        if (iniSection != null) {
          for (final Iterator<IniMapEntry> iter = iniSection.iterator(); iter.hasNext(); ) {
            IniMapEntry entry = iter.next();
            try {
              String key = entry.getKey();
              int id = (key.startsWith("0x") || key.startsWith("0X")) ? Misc.toNumber(key.substring(2, key.length()), 16, -1)
                  : Misc.toNumber(key, -1);
              if (id == animationId) {
                String value = entry.getValue();
                if (id > 0x1000 && value.length() > 4) {
                  value = value.substring(0, 4);
                }
                resref = value;
                break;
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    if (resref == null) {
      // evaluate ANISND.IDS if available
      IdsMap anisnd = IdsMapCache.get("ANISND.IDS");
      if (anisnd != null) {
        IdsMapEntry anisndEntry = anisnd.get(animationId);
        if (anisndEntry != null) {
          String[] elements = anisndEntry.getSymbol().split("\\s+");
          if (elements.length > 0 && elements[0].length() <= 8) {
            resref = elements[0];
            int pos = resref.indexOf('_');
            if (pos > 0) {
              // assuming underscore indicates a palette resref
              palette = resref;
              resref = resref.substring(0, pos);
            } else if (animationId >= 0x1000 && resref.length() > 4) {
              resref = resref.substring(0, 4);
            }
          }
        }
      }
    }

    if (resref == null) {
      return retVal;
    }

    if (palette == null) {
      palette = "*";
    }

    List<String> tableEntries = new ArrayList<>();
    AnimationInfo.Type type = AnimationInfo.Type.typeOfId(animationId);
    if (type == null) {
      return retVal;
    }

    ResourceEntry bamEntry;
    switch (type) {
      case EFFECT:
        tableEntries.add(String.format("0x%04x %s 0 0 0 * %s * * * * * * * * *", animationId, resref, palette));
        break;
      case MONSTER_QUADRANT:
        if (ResourceFactory.resourceExists(resref + "G14.BAM")) {
          tableEntries.add(String.format("0x%04x %s 1 32 5 * %s * * * * * * * * *", animationId, resref, palette));
        }
        break;
      case MONSTER_MULTI:
        if (ResourceFactory.resourceExists(resref + "1908.BAM")) {
          tableEntries.add(String.format("0x%04x %s 2 72 13 * %s * * * * 1 * * * *", animationId, resref, palette));
        }
        break;
      case MONSTER_MULTI_NEW:
        if (ResourceFactory.resourceExists(resref + "G145.BAM")) {
          tableEntries.add(String.format("0x%04x %s 2 32 5 * %s * * * * 1 * * * *", animationId, resref, palette));
        } else if (ResourceFactory.resourceExists(resref + "G1.BAM")) {
          tableEntries.add(String.format("0x%04x %s 2 32 5 * %s * * * * 0 * * * *", animationId, resref, palette));
        }
        break;
      case MONSTER_LAYERED_SPELL:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 4 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_ANKHEG:
        resref = guessResourceRef(resref, "DG1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "DG1.BAM");
        if (bamEntry != null) {
          tableEntries.add(String.format("0x%04x %s 6 24 5 * %s * * * * * * * * *", animationId, resref, palette));
        }
        break;
      case TOWN_STATIC:
        resref = guessResourceRef(resref, "");
        bamEntry = ResourceFactory.getResourceEntry(resref + ".BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 7 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case CHARACTER:
        bamEntry = ResourceFactory.getResourceEntry(resref + "1G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          int split = ResourceFactory.resourceExists(resref + "1G15.BAM") ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 8 16 3 * * * * * %d %d 1 * * *", animationId, resref, falseColor, split));
        }
        break;
      case CHARACTER_OLD:
        bamEntry = ResourceFactory.getResourceEntry(resref + "1G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 9 16 3 * * * * * %d * * * * *", animationId, resref, falseColor));
        }
        break;
      case MONSTER:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int split = ResourceFactory.resourceExists(resref + "G15.BAM") ? 1 : 0;
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 10 16 3 * %s * * * %d %d * * * *", animationId, resref, palette, falseColor, split));
          tableEntries.add(String.format("0x%04x %s 11 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_OLD:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 11 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_LAYERED:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          tableEntries.add(String.format("0x%04x %s 4 16 3 * * * * * * * * * * *", animationId, resref));
        }
        break;
      case MONSTER_LARGE:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 12 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_LARGE_16:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 13 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case AMBIENT_STATIC:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 14 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case AMBIENT:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 15 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case FLYING:
        resref = guessResourceRef(resref, "");
        bamEntry = ResourceFactory.getResourceEntry(resref + ".BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 16 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_ICEWIND:
      {
        boolean found = false;
        if (resref.length() >= 4 && !found) {
          for (final String suffix : new String[] { "A1", "A2", "A3", "A4", "CA", "DE", "GH", "GU", "SC", "SD", "SL", "SP", "TW", "WK" }) {
            if (ResourceFactory.resourceExists(resref + suffix + ".BAM")) {
              found = true;
              break;
            }
          }
        }
        if (found) {
          tableEntries.add(String.format("0x%04x %s 17 24 3 * %s * * * * * * * * *", animationId, resref, palette));
        }
        break;
      }
      default:
    }

    if (!tableEntries.isEmpty()) {
      for (final String line : tableEntries) {
        StringBuilder sb = new StringBuilder();
        sb.append("2DA V1.0").append('\n');
        sb.append("*").append('\n');
        sb.append("  RESREF TYPE ELLIPSE SPACE BLENDING PALETTE PALETTE2 RESREF2 TRANSLUCENT CLOWN SPLIT HELMET WEAPON HEIGHT HEIGHT_SHIELD").append('\n');
        sb.append(line).append('\n');
        ResourceEntry entry = new BufferedResourceEntry(ByteBuffer.wrap(sb.toString().getBytes()), Integer.toString(animationId, 16) + ".2DA");
        Table2da table = new Table2da(entry);
        retVal.addAll(SpriteTables.processTable(Profile.getGame(), table, animationId));
      }
    }

    return retVal;
  }

  // Helper method: attempts to find an existing resource with the specified name parts.
  // Returns the resref of the matching resource. Returns the original resref otherwise.
  private static String guessResourceRef(String resref, String suffix)
  {
    String retVal = resref;
    if (retVal == null) {
      return retVal;
    }

    if (suffix == null) {
      suffix = "";
    }

    while (retVal.length() >= 4) {
      if (ResourceFactory.resourceExists(retVal + suffix + ".BAM")) {
        return retVal;
      }
      retVal = retVal.substring(0, resref.length() - 1);
    }

    return resref;
  }

  // Attempts to find potential PST-specific IniMap instances
  private static List<IniMap> guessIniMapsPst(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    String resref = null;

    IniMap resIni = IniMapCache.get("RESDATA.INI", true);
    if (resIni == null) {
      return retVal;
    }

    // only regular animations are considered
    int id = animationId & 0x0fff;
    IniMapSection iniSection = resIni.getSection(Integer.toString(id));
    if (iniSection == null) {
      iniSection = resIni.getSection("0x" + Integer.toString(id, 16));
    }
    if (iniSection == null) {
      return retVal;
    }

    int clown = 0;
    for (final Sequence seq : Sequence.values()) {
      String cmd = MonsterPlanescapeDecoder.getActionCommand(seq);
      if (cmd != null) {
        String key = iniSection.getAsString(cmd);
        if (key != null && key.length() >= 7) {
          ResourceEntry bamEntry = ResourceFactory.getResourceEntry(key + "b.bam");
          if (bamEntry != null) {
            clown = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
            resref = key.substring(0, 1) + key.substring(4, key.length()) + "b";
            break;
          }
        }
      }
    }

    if (resref != null) {
      int armor = iniSection.getAsInteger("armor", 0);
      int bestiary = iniSection.getAsInteger("bestiary", 0);

      StringBuilder sb = new StringBuilder();
      sb.append("2DA V1.0").append('\n');
      sb.append("*").append('\n');
      sb.append("         RESREF   RESREF2  TYPE     ELLIPSE  SPACE    CLOWN    ARMOR    BESTIARY").append('\n');
      sb.append(String.format("0x%04x  %s  *  18  16  3  %d  %d  %d", id, resref, clown, armor, bestiary)).append('\n');
      ResourceEntry entry = new BufferedResourceEntry(ByteBuffer.wrap(sb.toString().getBytes()), Integer.toString(animationId, 16) + ".2DA");
      Table2da table = new Table2da(entry);
      retVal = SpriteTables.processTable(Profile.getGame(), table, animationId);
    }

    return retVal;
  }


  private SpriteUtils()
  {
  }
}
