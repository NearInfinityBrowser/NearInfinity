// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.CycleDef;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.resource.cre.decoder.util.DirDef;
import org.infinity.resource.cre.decoder.util.Direction;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.graphics.BamV1Decoder.BamV1Control;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.tuples.Couple;

/**
 * Creature animation decoder for processing type 0000 (effect) animations.
 * Available ranges: [0000,0fff]
 */
public class EffectDecoder extends SpriteDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.EFFECT;

  public static final DecoderAttribute KEY_SHADOW                 = DecoderAttribute.with("shadow", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PALLETIZED             = DecoderAttribute.with("palletized", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_RANDOM_RENDER          = DecoderAttribute.with("random_render", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_DELTA_Z                = DecoderAttribute.with("delta_z", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_ALT_PALETTE            = DecoderAttribute.with("alt_palette", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_HEIGHT_CODE_SHIELD     = DecoderAttribute.with("height_code_shield", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_HEIGHT_CODE_HELMET     = DecoderAttribute.with("height_code_helmet", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_HEIGHT_CODE            = DecoderAttribute.with("height_code", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_RESREF_PAPERDOLL       = DecoderAttribute.with("resref_paperdoll", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_RESREF_ARMOR_BASE      = DecoderAttribute.with("resref_armor_base", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_RESREF_ARMOR_SPECIFIC  = DecoderAttribute.with("resref_armor_specific", DecoderAttribute.DataType.STRING);
  // Note: these attribute are artificial to store hardcoded information
  // The cycle to play back (if >= 0)
  public static final DecoderAttribute KEY_CYCLE                  = DecoderAttribute.with("cycle", DecoderAttribute.DataType.INT);
  // A secondary resref to consider if random_render == 1
  public static final DecoderAttribute KEY_RESREF2                = DecoderAttribute.with("resref2", DecoderAttribute.DataType.STRING);

  protected final BeforeSourceBam FN_BEFORE_SRC_BAM = new BeforeSourceBam() {
    @Override
    public void accept(BamV1Control control, SegmentDef sd)
    {
      if (isPaletteReplacementEnabled() && sd.getSpriteType() == SegmentDef.SpriteType.AVATAR) {
        int[] palette = getNewPaletteData(sd.getEntry());
        if (palette != null) {
          SpriteUtils.applyNewPalette(control, palette);
        }
      }

      if (isFalseColor()) {
        SpriteUtils.fixShadowColor(control, isTransparentShadow());
        if (isPaletteReplacementEnabled()) {
          applyFalseColors(control, sd);
        }
      }

      if (isTintEnabled()) {
        applyColorTint(control, sd);
      }

      if ((isTranslucencyEnabled() && isTranslucent()) ||
          (isBlurEnabled() && isBlurred())) {
        int minVal = (isBlurEnabled() && isBlurred()) ? 64 : 255;
        applyTranslucency(control, minVal);
      }
    }
  };

  /**
   * A helper method that parses the specified data array and generates a {@link IniMap} instance out of it.
   * @param data a String array containing table values for a specific table entry.
   * @return a {@code IniMap} instance with the value derived from the specified data array.
   *         Returns {@code null} if no data could be derived.
   */
  public static IniMap processTableData(String[] data)
  {
    IniMap retVal = null;
    if (data == null || data.length < 16) {
      return retVal;
    }

    String resref = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF, "");
    if (resref.isEmpty()) {
      return retVal;
    }
    String shadow = SpriteTables.valueToString(data, SpriteTables.COLUMN_RESREF2, "");
    int translucent = SpriteTables.valueToInt(data, SpriteTables.COLUMN_TRANSLUCENT, 0);
    int falseColor = SpriteTables.valueToInt(data, SpriteTables.COLUMN_CLOWN, 0);
    int random = SpriteTables.valueToInt(data, SpriteTables.COLUMN_SPLIT, 0);
    int cycle = SpriteTables.valueToInt(data, SpriteTables.COLUMN_HELMET, -1);
    String altPalette = SpriteTables.valueToString(data, SpriteTables.COLUMN_PALETTE2, "");
    String resref2 = SpriteTables.valueToString(data, SpriteTables.COLUMN_HEIGHT, "");

    List<String> lines = SpriteUtils.processTableDataGeneral(data, ANIMATION_TYPE);
    lines.add("[effect]");
    lines.add("resref=" + resref);
    if (!shadow.isEmpty()) {
      lines.add("shadow=" + shadow);
    }
    lines.add("translucent=" + translucent);
    lines.add("false_color=" + falseColor);
    lines.add("random_render=" + random);
    if (cycle >= 0) {
      lines.add("cycle=" + cycle);
    }
    if (!altPalette.isEmpty()) {
      lines.add("alt_palette=" + altPalette);
    }
    if (!resref2.isEmpty()) {
      lines.add("resref2=" + resref2);
    }

    retVal = IniMap.from(lines);

    return retVal;
  }

  public EffectDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public EffectDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
  }

  /** Returns a separate shadow sprite resref. */
  public String getShadowResref() { return getAttribute(KEY_SHADOW); }
  protected void setShadowResref(String s) { setAttribute(KEY_SHADOW, s); }

  /** Returns a secondary sprite resref to consider if RenderRandom() is set. */
  public String getSecondaryResref() { return getAttribute(KEY_RESREF2); }
  protected void setSecondaryResref(String s) { setAttribute(KEY_RESREF2, s); }

  /** Returns a replacement palette resref (BMP). */
  public String getAltPalette() { return getAttribute(KEY_ALT_PALETTE); }
  protected void setAltPalette(String s) { setAttribute(KEY_ALT_PALETTE, s); }

  /** Returns the height code prefix for shield overlay sprites. */
  public String getShieldHeightCode() { return getAttribute(KEY_HEIGHT_CODE_SHIELD); }
  protected void setShieldHeightCode(String s) { setAttribute(KEY_HEIGHT_CODE_SHIELD, s); }

  /** Returns the height code prefix for helmet overlay sprites. */
  public String getHelmetHeightCode() { return getAttribute(KEY_HEIGHT_CODE_HELMET); }
  protected void setHelmetHeightCode(String s) { setAttribute(KEY_HEIGHT_CODE_HELMET, s); }

  /** Returns the creature animation height code prefix. */
  public String getHeightCode() { return getAttribute(KEY_HEIGHT_CODE); }
  protected void setHeightCode(String s) { setAttribute(KEY_HEIGHT_CODE, s); }

  /** Returns the paperdoll resref. */
  public String getPaperdollResref() { return getAttribute(KEY_RESREF_PAPERDOLL); }
  protected void setPaperdollResref(String s) { setAttribute(KEY_RESREF_PAPERDOLL, s); }

  /** Returns animation resref suffix (last letter) for lesser armor types. */
  public String getArmorBaseResref() { return getAttribute(KEY_RESREF_ARMOR_BASE); }
  protected void setArmorBaseResref(String s) { setAttribute(KEY_RESREF_ARMOR_BASE, s); }

  /** Returns animation resref suffix (last letter) for greater armor types. */
  public String getArmorSpecificResref() { return getAttribute(KEY_RESREF_ARMOR_SPECIFIC); }
  protected void setArmorSpecificResref(String s) { setAttribute(KEY_RESREF_ARMOR_SPECIFIC, s); }

  /** unused */
  public boolean isPalettized() { return getAttribute(KEY_PALLETIZED); }
  protected void setPalettized(boolean b) { setAttribute(KEY_PALLETIZED, b); }

  /** Returns whether a randomly chosen animation cycle is drawn. */
  public boolean isRenderRandom() { return getAttribute(KEY_RANDOM_RENDER); }
  protected void setRenderRandom(boolean b) { setAttribute(KEY_RANDOM_RENDER, b); }

  /** Returns the BAM cycle index to use. -1 indicates no specific BAM cycle. */
  public int getCycle() { return getAttribute(KEY_CYCLE); }
  protected void setCycle(int v) { setAttribute(KEY_CYCLE, v); }

  /** ??? */
  public int getDeltaZ() { return getAttribute(KEY_DELTA_Z); }
  protected void setDeltaZ(int v) { setAttribute(KEY_DELTA_Z, v); }

  @Override
  public String getNewPalette()
  {
    String retVal = getAltPalette();
    if (retVal == null || retVal.isEmpty()) {
      retVal = super.getNewPalette();
    }
    return retVal;
  }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    ArrayList<String> retVal = new ArrayList<>();
    retVal.add(getAnimationResref() + ".BAM");
    return retVal;
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq)
  {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected void init() throws Exception
  {
    // setting properties
    initDefaults(getAnimationInfo());
    IniMapSection section = getSpecificIniSection();
    setShadowResref(section.getAsString(KEY_SHADOW.getName(), ""));
    setPalettized(section.getAsInteger(KEY_PALLETIZED.getName(), 0) != 0);
    setTranslucent(section.getAsInteger(KEY_TRANSLUCENT.getName(), 0) != 0);
    setRenderRandom(section.getAsInteger(KEY_RANDOM_RENDER.getName(), 0) != 0);
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
    setCycle(section.getAsInteger(KEY_CYCLE.getName(), -1));
    setDeltaZ(section.getAsInteger(KEY_DELTA_Z.getName(), 0));
    setAltPalette(section.getAsString(KEY_ALT_PALETTE.getName(), ""));
    setShieldHeightCode(section.getAsString(KEY_HEIGHT_CODE_SHIELD.getName(), ""));
    setHelmetHeightCode(section.getAsString(KEY_HEIGHT_CODE_HELMET.getName(), ""));
    setHeightCode(section.getAsString(KEY_HEIGHT_CODE.getName(), ""));
    setPaperdollResref(section.getAsString(KEY_RESREF_PAPERDOLL.getName(), ""));
    setArmorBaseResref(section.getAsString(KEY_RESREF_ARMOR_BASE.getName(), ""));
    setArmorSpecificResref(section.getAsString(KEY_RESREF_ARMOR_SPECIFIC.getName(), ""));
  }

  @Override
  protected void createSequence(Sequence seq, Direction[] directions) throws Exception
  {
    SeqDef sd = Objects.requireNonNull(getSequenceDefinition(seq), "Sequence not available: " + (seq != null ? seq : "(null)"));
    if (directions == null) {
      directions = Direction.values();
    }
    createAnimation(sd, Arrays.asList(directions), FN_BEFORE_SRC_BAM, FN_BEFORE_SRC_FRAME, FN_AFTER_SRC_FRAME, FN_AFTER_DST_FRAME);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    if (seq != Sequence.STAND) {
      return retVal;
    }

    ArrayList<Couple<ResourceEntry, Integer>> creResList = new ArrayList<>();
    if (!getShadowResref().isEmpty()) {
      ResourceEntry shdEntry = ResourceFactory.getResourceEntry(getShadowResref() + ".BAM");
      if (shdEntry != null) {
        creResList.add(Couple.with(shdEntry, 0));
      }
    }

    Random rnd = new Random();
    String resref = getAnimationResref();
    if (isRenderRandom()) {
      if (!getSecondaryResref().isEmpty() && (Math.abs(rnd.nextInt()) % 3) == 0) {
        resref = getSecondaryResref();
      }
    }

    ResourceEntry resEntry = ResourceFactory.getResourceEntry(resref + ".BAM");
    BamControl ctrl = SpriteUtils.loadBamController(resEntry);
    if (ctrl != null) {
      int cycle = 0;
      if (isRenderRandom()) {
        cycle = Math.abs(rnd.nextInt()) % ctrl.cycleCount();
      } else if (getCycle() >= 0) {
        cycle = getCycle();
      }
      creResList.add(Couple.with(resEntry, 0));

      retVal = new SeqDef(seq);
      for (final Couple<ResourceEntry, Integer> data : creResList) {
        resEntry = data.getValue0();
        cycle = data.getValue1().intValue();
        if (SpriteUtils.bamCyclesExist(resEntry, cycle, 1)) {
          retVal.addDirections(new DirDef(Direction.S, false, new CycleDef(resEntry, cycle)));
        }
      }
    }

    if (retVal.isEmpty()) {
      retVal = null;
    }

    return retVal;
  }
}
