// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.Locale;
import java.util.regex.Pattern;

import org.infinity.datatype.IsNumeric;
import org.infinity.resource.Profile;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.resource.cre.decoder.util.ItemInfo;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.Misc;

/**
 * Common base for processing creature animations with different armor levels.
 */
public abstract class CharacterBaseDecoder extends SpriteDecoder
{
  public static final DecoderAttribute KEY_CAN_LIE_DOWN       = DecoderAttribute.with("can_lie_down", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_DOUBLE_BLIT        = DecoderAttribute.with("double_blit", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_EQUIP_HELMET       = DecoderAttribute.with("equip_helmet", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_ARMOR_MAX_CODE     = DecoderAttribute.with("armor_max_code", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_HEIGHT_CODE        = DecoderAttribute.with("height_code", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_HEIGHT_CODE_HELMET = DecoderAttribute.with("height_code_helmet", DecoderAttribute.DataType.STRING);

  /** Available attack types associated with attack sequences. */
  public enum AttackType {
    ONE_HANDED, TWO_HANDED, TWO_WEAPON, THROWING,
    BOW, CROSSBOW, SLING
  }

  public CharacterBaseDecoder(AnimationInfo.Type type, int animationId, IniMap ini) throws Exception
  {
    super(type, animationId, ini);
  }

  public CharacterBaseDecoder(AnimationInfo.Type type, CreResource cre) throws Exception
  {
    super(type, cre);
  }

  /** Returns whether the creature falls down when dead/unconscious. */
  public boolean canLieDown() { return getAttribute(KEY_CAN_LIE_DOWN); }
  protected void setCanLieDown(boolean b) { setAttribute(KEY_CAN_LIE_DOWN, b); }

  /** unused */
  public boolean isDoubleBlit() { return getAttribute(KEY_DOUBLE_BLIT); }
  protected void setDoubleBlit(boolean b) { setAttribute(KEY_DOUBLE_BLIT, b); }

  /**
   * Returns the maximum armor code value used as suffix in animation filenames.
   * Highest code value is usually used by ArmorSpecificResref().
   */
  public int getMaxArmorCode() { return getAttribute(KEY_ARMOR_MAX_CODE); }
  protected void setMaxArmorCode(int v) { setAttribute(KEY_ARMOR_MAX_CODE, Math.max(0, v)); }

  /** Returns whether helmet overlay is shown. */
  public boolean isHelmetEquipped() { return getAttribute(KEY_EQUIP_HELMET); }
  protected void setHelmetEquipped(boolean b) { setAttribute(KEY_EQUIP_HELMET, b); }

  /** Returns the height code prefix for helmet overlay sprites. Falls back to generic height code if needed. */
  public String getHelmetHeightCode()
  {
    String retVal = getAttribute(KEY_HEIGHT_CODE_HELMET);
    if (retVal.isEmpty()) {
      retVal = getHeightCode();
    }
    return retVal;
  }

  protected void setHelmetHeightCode(String s) { setAttribute(KEY_HEIGHT_CODE_HELMET, s); }

  /** Returns the creature animation height code prefix. */
  public String getHeightCode() { return getAttribute(KEY_HEIGHT_CODE); }
  protected void setHeightCode(String s)
  {
    if (s == null || s.isEmpty()) {
      // heuristically determine height code
      s = guessHeightCode();
    }
    setAttribute(KEY_HEIGHT_CODE, s);
  }

  /** Returns the armor code based on equipped armor of the current creature. */
  public int getArmorCode()
  {
    int retVal = 1;
    ItemInfo itm = getCreatureInfo().getEquippedArmor();
    if (itm != null) {
      String code = itm.getAppearance();
      if (!code.isEmpty()) {
        retVal = Math.max(1, Math.min(getMaxArmorCode(), Misc.toNumber(code.substring(0, 1), 1)));
      }
    }
    return retVal;
  }

  /**
   * Determines the attack type based on the specified item resource.
   * @param itm the item resource.
   * @param abilityIndex the item-specific ability to check (e.g. throwing or melee for throwing axes)
   * @param preferTwoWeapon whether {@code AttackType.TwoWeapon} should be returned if a melee one-handed weapon is detected.
   * @return attack type associated with the item resource.
   */
  public AttackType getAttackType(ItemInfo itm, int abilityIndex, boolean preferTwoWeapon)
  {
    AttackType retVal = AttackType.ONE_HANDED;
    if (itm == null) {
      return retVal;
    }

    // collecting data
    boolean isTwoHanded = (itm.getFlags() & (1 << 1)) != 0;
    if (Profile.isEnhancedEdition()) {
      // include fake two-handed weapons (e.g. monk fists)
      isTwoHanded |= (itm.getFlags() & (1 << 12)) != 0;
    }
    int abilType = -1;
    abilityIndex = Math.max(0, Math.min(itm.getAbilityCount() - 1, abilityIndex));
    if (abilityIndex >= 0) {
      abilType = itm.getAbility(abilityIndex).getAbilityType();
    }

    switch (itm.getCategory()) {
      case 15:  // Bows
        retVal = AttackType.BOW;
        break;
      case 27:  // Crossbows
        retVal = AttackType.CROSSBOW;
        break;
      case 18:  // Slings
        retVal = AttackType.SLING;
        break;
      default:
        if (abilType == 1) {  // melee
          if (isTwoHanded) {
            retVal = AttackType.TWO_HANDED;
          } else {
            retVal = (preferTwoWeapon) ? AttackType.TWO_WEAPON : AttackType.ONE_HANDED;
          }
        } else { // assume ranged
          retVal = AttackType.THROWING;
        }
    }

    return retVal;
  }

  /**
   * Attempts to determine the correct height code.
   * @return the "guessed" height code. Returns empty string if code could not be determined.
   */
  protected String guessHeightCode()
  {
    String retVal = "";
    boolean isCharacter = (getAnimationType() == AnimationInfo.Type.CHARACTER);
    String c2 = isCharacter ? "Q" : "P";

    // try resref naming scheme
    String resref = getAnimationResref().toUpperCase(Locale.ENGLISH);
    if (resref.length() >= 3 && Pattern.matches(".[DEGHIO][FM].?", resref)) {
      char race = resref.charAt(1);
      char gender = resref.charAt(2);
      if (gender == 'M' || gender == 'F') {
        switch (race) {
          case 'H':   // human
          case 'O':   // half-orc
            if (isCharacter) {
              retVal = "W" + c2 + ((gender == 'F') ? "N" : "L");
            } else {
              retVal = "W" + c2 + "L";
            }
            break;
          case 'E':   // elf/half-elf
            retVal = "W" + c2 + "M";
            break;
          case 'D':   // dwarf/gnome
          case 'G':   // gnome (?)
          case 'I':   // halfling
            retVal = "W" + c2 + "S";
            break;
        }
      }
    }

    // try associated CRE data
    if (retVal.isEmpty()) {
      CreResource cre = getCreResource();
      if (cre != null) {
        boolean isFemale = ((IsNumeric)cre.getAttribute(CreResource.CRE_GENDER)).getValue() == 2;
        int race = ((IsNumeric)cre.getAttribute(CreResource.CRE_RACE)).getValue();
        switch (race) {
          case 1:   // human
          case 7:   // half-orc
            if (isCharacter) {
              retVal = "W" + c2 + (isFemale ? "N" : "L");
            } else {
              retVal = "W" + c2 + "L";
            }
            break;
          case 2:   // elf
          case 3:   // half-elf
            retVal = "W" + c2 + "M";
            break;
          case 4:   // dwarf
          case 5:   // halfling
          case 6:   // gnome
            retVal = "W" + c2 + "S";
            break;
        }
      }
    }

    return retVal;
  }

  @Override
  protected void init() throws Exception
  {
    // setting properties
    initDefaults(getAnimationInfo());
    IniMapSection section = getSpecificIniSection();
    setCanLieDown(section.getAsInteger(KEY_CAN_LIE_DOWN.getName(), 0) != 0);
    setFalseColor(section.getAsInteger(KEY_FALSE_COLOR.getName(), 0) != 0);
    setDetectedByInfravision(section.getAsInteger(KEY_DETECTED_BY_INFRAVISION.getName(), 0) != 0);
    setDoubleBlit(section.getAsInteger(KEY_DOUBLE_BLIT.getName(), 0) != 0);
    setMaxArmorCode(section.getAsInteger(KEY_ARMOR_MAX_CODE.getName(), 0));
    setHelmetEquipped(section.getAsInteger(KEY_EQUIP_HELMET.getName(), 0) != 0);
    setHeightCode(section.getAsString(KEY_HEIGHT_CODE.getName(), ""));
    setHelmetHeightCode(section.getAsString(KEY_HEIGHT_CODE_HELMET.getName(), ""));
  }
}
