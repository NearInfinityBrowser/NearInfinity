// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.spl.SplResource;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.infinity.util.io.StreamUtils;

/**
 * Manages effects opcodes for a specific target.
 */
public class EffectInfo
{
  /** Effect opcode 7: Set color */
  public static final int OPCODE_SET_COLOR      = 7;
  /** Effect opcode 8: Set color glow solid */
  public static final int OPCODE_SET_COLOR_GLOW = 8;
  /** Effect opcode 51: Character tint solid */
  public static final int OPCODE_TINT_SOLID     = 51;
  /** Effect opcode 52: Character tint bright */
  public static final int OPCODE_TINT_BRIGHT    = 52;
  /** Effect opcode 65: Blur */
  public static final int OPCODE_BLUR           = 65;
  /** Effect opcode 66: Translucency */
  public static final int OPCODE_TRANSLUCENCY   = 66;
  /** Effect opcode 134: Petrification */
  public static final int OPCODE_PETRIFICATION  = 134;
  /** Effect opcode 218: Stoneskin */
  public static final int OPCODE_STONESKIN      = 218;

  /** A predicate for filtering opcode 7 effects (set color). */
  public static final Predicate<Effect> FILTER_COLOR_EFFECT = (fx) -> (fx.getOpcode() == OPCODE_SET_COLOR);
  /** A predicate for filtering opcode 8 effects (color glow). */
  public static final Predicate<Effect> FILTER_COLOR_GLOW_EFFECT = (fx) -> (fx.getOpcode() == OPCODE_SET_COLOR_GLOW);
  /** A predicate for filtering opcode 51 effects (tint solid). */
  public static final Predicate<Effect> FILTER_TINT_SOLID_EFFECT = (fx) -> (fx.getOpcode() == OPCODE_TINT_SOLID);
  /** A predicate for filtering opcode 52 effects (tint bright). */
  public static final Predicate<Effect> FILTER_TINT_BRIGHT_EFFECT = (fx) -> (fx.getOpcode() == OPCODE_TINT_BRIGHT);
  /** A predicate for filtering blur effects. */
  public static final Predicate<Effect> FILTER_BLUR_EFFECT = (fx) -> (fx.getOpcode() == OPCODE_BLUR);
  /** A predicate for filtering translucency effects. */
  public static final Predicate<Effect> FILTER_TRANSLUCENCY_EFFECT = (fx) -> (fx.getOpcode() == OPCODE_TRANSLUCENCY);
  /** A predicate for filtering all stone-like effects. */
  public static final Predicate<Effect> FILTER_STONE_EFFECT = (fx) -> (fx.getOpcode() == OPCODE_PETRIFICATION ||
                                                                       fx.getOpcode() == OPCODE_STONESKIN);


  private final EnumMap<SegmentDef.SpriteType, Set<Effect>> effectMap = new EnumMap<>(SegmentDef.SpriteType.class);

  /** A helper method that reverses byte order of the specified parameter. */
  public static int swapBytes(int value)
  {
    int retVal = 0;
    retVal |= (value >>> 24) & 0xff;
    retVal |= (value >>> 8) & 0xff00;
    retVal |= (value << 8) & 0xff0000;
    retVal |= (value << 24) & 0xff000000;
    return retVal;
  }

  /** A helper method that reverses word order of the specified parameter. */
  public static int swapWords(int value)
  {
    int retVal = 0;
    retVal |= (value >> 16) & 0xffff;
    retVal |= (value << 16) & 0xffff0000;
    return retVal;
  }

  public EffectInfo()
  {
  }

  /**
   * Returns a list of {@code SpriteType} definitions containing effect definitions.
   * @param creInfo the creature target.
   * @return List of sprite type definitions.
   */
  public List<SegmentDef.SpriteType> getSpriteTypes(CreatureInfo creInfo)
  {
    return getSpriteTypes(creInfo, null);
  }

  /**
   * Returns a list of {@code SpriteType} definitions with effects matching the specified opcode.
   * @param creInfo the creature target.
   * @param opcode the opcode to match.
   * @return List of sprite type definitions.
   */
  public List<SegmentDef.SpriteType> getSpriteTypes(CreatureInfo creInfo, int opcode)
  {
    return getSpriteTypes(creInfo, (fx) -> fx.getOpcode() == opcode);
  }

  /**
   * Returns a list of {@code SpriteType} definitions with effects matching the specified predicate.
   * @param creInfo the creature target.
   * @param pred the predicate function object. Specify {@code null} to match any effects.
   * @return List of sprite type definitions.
   */
  public List<SegmentDef.SpriteType> getSpriteTypes(CreatureInfo creInfo, Predicate<Effect> pred)
  {
    final List<SegmentDef.SpriteType> retVal = new ArrayList<>();

    if (pred == null) {
      pred = (fx) -> true;
    }

    for (final Map.Entry<SegmentDef.SpriteType, Set<Effect>> entry : effectMap.entrySet()) {
      if (entry.getValue().parallelStream().anyMatch(pred.and(e -> isEffectValid(e, creInfo)))) {
        retVal.add(entry.getKey());
      }
    }

    return retVal;
  }

  /**
   * A convenience method that returns the first available effect associated with the
   * specified {@code SpriteType}.
   * @param creInfo the creature target.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @return first matching {@code Effect} instance. Returns {@code null} if not available.
   */
  public Effect getFirstEffect(CreatureInfo creInfo, SegmentDef.SpriteType type)
  {
    List<Effect> list = getEffects(creInfo, type, null);
    return !list.isEmpty() ? list.get(0) : null;
  }

  /**
   * A convenience method that returns the first available effect matching the specified {@code SpriteType}
   * and opcode.
   * @param creInfo the creature target.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @param opcode the opcode to filter.
   * @return first matching {@code Effect} instance. Returns {@code null} if not available.
   */
  public Effect getFirstEffect(CreatureInfo creInfo, SegmentDef.SpriteType type, int opcode)
  {
    List<Effect> list = getEffects(creInfo, type, (fx) -> fx.getOpcode() == opcode);
    return !list.isEmpty() ? list.get(0) : null;
  }

  /**
   * A convenience method that returns the first available effect matching the specified {@code SpriteType}
   * and opcode.
   * @param creInfo the creature target.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @param pred the predicate function object responsible for filtering. Specify {@code null} to return the
   *             first available effects without filtering.
   * @return first matching {@code Effect} instance. Returns {@code null} if not available.
   */
  public Effect getFirstEffect(CreatureInfo creInfo, SegmentDef.SpriteType type, Predicate<Effect> pred)
  {
    if (creInfo == null) {
      throw new NullPointerException("Creature info cannot be null");
    }

    Effect retVal = null;

    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    if (pred == null) {
      pred = (fx) -> true;
    }

    Set<Effect> set = effectMap.get(type);
    if (set != null) {
      retVal = set
          .parallelStream()
          .filter(pred.and(e -> isEffectValid(e, creInfo)))
          .findAny()
          .orElse(null);
    }

    return retVal;
  }

  /**
   * Returns all available effects associated with the specified {@code SpriteType}.
   * @param creInfo the creature target.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @return list of effects associated with the sprite type. Empty list if no effects available.
   */
  public List<Effect> getEffects(CreatureInfo creInfo, SegmentDef.SpriteType type)
  {
    return getEffects(creInfo, type, null);
  }

  /**
   * Returns all available effects matching the specified {@code SpriteType} and opcode.
   * @param creInfo the creature target.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @param opcode the opcode to filter.
   * @return list of effects associated with the sprite type and matching opcode. Empty list if no effects available.
   */
  public List<Effect> getEffects(CreatureInfo creInfo, SegmentDef.SpriteType type, int opcode)
  {
    return getEffects(creInfo, type, (fx) -> fx.getOpcode() == opcode);
  }

  /**
   * Returns all available effects matching the specified {@code SpriteType} and predicate.
   * @param creInfo the creature target.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @param pred the predicate function object. Effects passing the test are added to the results list.
   *             Specify {@code null} to return all effects associated with the specified sprite type.
   * @return list of effects associated with the sprite type and matching predicate. Empty list if no effects available.
   */
  public List<Effect> getEffects(CreatureInfo creInfo, SegmentDef.SpriteType type, Predicate<Effect> pred)
  {
    if (creInfo == null) {
      throw new NullPointerException("Creature info cannot be null");
    }

    final List<Effect> retVal = new ArrayList<>();

    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    if (pred == null) {
      pred = (fx) -> true;
    }

    Set<Effect> set = effectMap.get(type);
    if (set != null) {
      set
        .stream()
        .filter(pred.and(e -> isEffectValid(e, creInfo)))
        .forEach(fx -> retVal.add(fx));
    }

    return retVal;
  }

  /**
   * Returns {@code true} if the specified {@code SpriteType} contains one or more effects matching
   * the specified opcode.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @param opcode the opcode to match.
   * @return {@code true} if match is found, {@code false} otherwise.
   */
  public boolean hasEffect(SegmentDef.SpriteType type, int opcode)
  {
    return hasEffect(type, (fx) -> fx.getOpcode() == opcode);
  }

  /**
   * Returns {@code true} if the specified {@code SpriteType} contains one or more effects matching
   * the specified predicate.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @param pred the predicate function object. Specify {@code null} to match any effects.
   * @return {@code true} if match is found, {@code false} otherwise.
   */
  public boolean hasEffect(SegmentDef.SpriteType type, Predicate<Effect> pred)
  {
    if (type == null) {
      type = SegmentDef.SpriteType.AVATAR;
    }

    if (pred == null) {
      pred = (fx) -> true;
    }

    Set<Effect> set = effectMap.get(type);
    if (set != null) {
      set.parallelStream().anyMatch(pred);
    }

    return false;
  }

  /**
   * A specialized method that returns the first available color-related effect matching the specified parameters.
   * @param creInfo the creature target.
   * @param type the sprite type to filter. Specify {@code null} for non-specific effects.
   * @param opcode the opcode to filter.
   * @param location color location index. See opcode description for more details.
   * @return the first matching effect. Returns {@code null} if there is no match available.
   */
  public Effect getColorByLocation(CreatureInfo creInfo, SegmentDef.SpriteType type, int opcode, int location)
  {
    final int locationIndex = (location == 255) ? location : (location & 0x0f);

    final Predicate<Effect> pred = (fx) -> {
      return (fx.getOpcode() == opcode) &&
             ((fx.getParameter2() & 0xf) == locationIndex);
    };

    return getFirstEffect(creInfo, type, pred);
  }

  /**
   * Adds the specified effect and associates it with the {@code SpriteType} defined by the effect.
   * <p>Indirectly created effects (e.g. via opcode 146) are correctly resolved and added.
   * @param effect the effect to add.
   */
  public void add(Effect effect)
  {
    if (effect == null) {
      throw new NullPointerException("Effect parameter cannot be null");
    }

    List<Effect> effects = resolveEffect(null, effect);
    for (final Effect fx : effects) {
      SegmentDef.SpriteType type = getEffectType(fx);
      Set<Effect> set = effectMap.get(type);
      if (set == null) {
        set = new HashSet<>();
        effectMap.put(type, set);
      }
      set.add(fx);
    }
  }

  /**
   * Checks if the specified effect is available for the given creature.
   * @param effect the effect to check
   * @param creInfo the creature target
   * @return {@code true} if the effect is valid for the target. Returns {@code false} otherwise.
   */
  protected boolean isEffectValid(Effect effect, CreatureInfo creInfo)
  {
    boolean retVal = true;
    if (effect == null) {
      return retVal;
    }

    switch (effect.getOpcode()) {
      case 177: // Use EFF
      case 283: // Use EFF as curse
        retVal = evaluateIds(creInfo, effect);
        break;
      case 183: // Use EFF for item type
        retVal = evaluateItemCategory(creInfo, effect.getParameter2());
        break;
      case 326: // Apply effects list
        retVal = evaluateSplProt(creInfo, effect.getParameter2(), effect.getParameter1());
        break;
    }

    if (retVal) {
      retVal = isEffectValid(effect.getParent(), creInfo);
    }

    return retVal;
  }

  /** Determines the sprite type target defined by this effect. Only relevant for color-related effects. */
  private SegmentDef.SpriteType getEffectType(Effect effect)
  {
    switch (effect.getOpcode()) {
      case 7:   // Set color
      case 8:   // Set color glow solid
      case 9:   // Set color glow pulse
      case 50:  // Character color pulse
      case 51:  // Character tint solid
      case 52:  // Character tint bright
        switch ((effect.getParameter2() >> 4) & 0xf) {
          case 1:
            return SegmentDef.SpriteType.WEAPON;
          case 2:
            return SegmentDef.SpriteType.SHIELD;
          case 3:
            return SegmentDef.SpriteType.HELMET;
          default:
            return SegmentDef.SpriteType.AVATAR;
        }
      default:
        return SegmentDef.SpriteType.AVATAR;
    }
  }

  /**
   * Resolves effects which reference additional effects via secondary resources (e.g. SPL, EFF).
   * The primary effect is skipped in these cases.
   */
  private List<Effect> resolveEffect(Effect parent, Effect effect)
  {
    List<Effect> retVal = new ArrayList<>();

    effect.setParent(parent);
    retVal.add(effect);

    switch (effect.getOpcode()) {
      case 146: // Cast spell
        resolveSPL(retVal, effect, ResourceFactory.getResourceEntry(effect.getResource() + ".SPL"));
        break;
      case 177: // Use EFF
        resolveEFF(retVal, effect, ResourceFactory.getResourceEntry(effect.getResource() + ".EFF"));
        break;
      case 183: // Use EFF for item type
        if (Profile.getEngine() != Profile.Engine.PST) {
          resolveEFF(retVal, effect, ResourceFactory.getResourceEntry(effect.getResource() + ".EFF"));
        }
        break;
      case 283: // Use EFF as curse
        switch (Profile.getEngine()) {
          case BG2:
          case EE:
            resolveEFF(retVal, effect, ResourceFactory.getResourceEntry(effect.getResource() + ".EFF"));
            break;
          default:
        }
        break;
      case 326: // Apply effects list
        if (Profile.getEngine() == Profile.Engine.EE) {
          resolveSPL(retVal, effect, ResourceFactory.getResourceEntry(effect.getResource() + ".SPL"));
        }
        break;
      default:
    }

    return retVal;
  }

  /** Resolves all effects (global effects and effects from first available ability) from the specified SPL resource. */
  private void resolveSPL(List<Effect> list, Effect parent, ResourceEntry entry)
  {
    if (entry == null) {
      return;
    }

    try {
      SplResource spl = new SplResource(entry);
      int ofsAbil = ((IsNumeric)spl.getAttribute(SplResource.SPL_OFFSET_ABILITIES)).getValue();
      int numAbil = ((IsNumeric)spl.getAttribute(SplResource.SPL_NUM_ABILITIES)).getValue();
      int numGlobalFx = ((IsNumeric)spl.getAttribute(SplResource.SPL_NUM_GLOBAL_EFFECTS)).getValue();

      // evaluating global effects
      if (numGlobalFx > 0) {
        List<StructEntry> fxList = spl.getFields(org.infinity.resource.Effect.class);
        if (fxList != null) {
          for (final StructEntry se : fxList) {
            if (se instanceof org.infinity.resource.Effect) {
              final List<Effect> retList = resolveEffect(parent, new Effect((org.infinity.resource.Effect)se));
              list.addAll(retList);
            }
          }
        }
      }

      // evaluating ability effects (first ability only)
      if (numAbil > 0) {
        StructEntry abil = spl.getField(org.infinity.resource.spl.Ability.class, ofsAbil + spl.getExtraOffset());
        if (abil instanceof org.infinity.resource.spl.Ability) {
          List<StructEntry> fxList = ((org.infinity.resource.spl.Ability)abil).getFields(org.infinity.resource.Effect.class);
          for (final StructEntry se : fxList) {
            if (se instanceof org.infinity.resource.Effect) {
              final List<Effect> retList = resolveEffect(parent, new Effect((org.infinity.resource.Effect)se));
              list.addAll(retList);
            }
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Resolves the effect from the specified EFF resource. */
  private void resolveEFF(List<Effect> list, Effect parent, ResourceEntry entry)
  {
    if (entry == null) {
      return;
    }

    try {
      final List<Effect> retList = resolveEffect(parent, Effect.fromEffectV2(entry.getResourceBuffer(), 8));
      list.addAll(retList);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Evaluates the item category filter based on equipped items by the target.
   * @param creInfo creature target
   * @param cat the item category
   * @return whether creature has equipped items with the specified item category.
   */
  private boolean evaluateItemCategory(CreatureInfo creInfo, int cat)
  {
    boolean retVal = false;

    for (final CreatureInfo.ItemSlots slot : CreatureInfo.ItemSlots.values()) {
      ItemInfo ii = creInfo.getItemInfo(slot);
      if (ii != null) {
        if (cat == ii.getCategory()) {
          retVal = true;
          break;
        }
      }
    }

    return retVal;
  }

  /**
   * Evaluates the IDS filter specified by type and entry value for the given target.
   * @param creInfo creature target
   * @param type the IDS resource type
   * @param entry the IDS entry index
   * @return whether creature stat matches reference stat.
   */
  private boolean evaluateIds(CreatureInfo creInfo, Effect effect)
  {
    boolean retVal = false;

    int type = effect.getParameter2();
    int entry = effect.getParameter1();
    switch (type) {
      case 2: // EA.IDS
        retVal = (entry == creInfo.getAllegiance());
        break;
      case 3: // GENERAL.IDS
        retVal = (entry == ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_GENERAL)).getValue());
        break;
      case 4: // RACE.IDS
        retVal = (entry == ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RACE)).getValue());
        break;
      case 5: // CLASS.IDS
        retVal = (entry == ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_CLASS)).getValue());
        break;
      case 6: // SPECIFIC.IDS
        retVal = (entry == ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SPECIFICS)).getValue());
        break;
      case 7: // GENDER.IDS
        retVal = (entry == ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_GENDER)).getValue());
        break;
      case 8: // ALIGN.IDS
        retVal = (entry == ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_ALIGNMENT)).getValue());
        break;
      case 9: // KIT.IDS
        if (Profile.isEnhancedEdition()) {
          int kit = ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RACE)).getValue();
          kit = ((kit & 0xffff) << 16) | ((kit >>> 16) & 0xffff);
          retVal = (entry == kit);
        }
        break;
      case 11:  // Actor's script name
        if (Profile.isEnhancedEdition()) {
          String name = ((IsTextual)creInfo.getCreResource().getAttribute(CreResource.CRE_SCRIPT_NAME)).getText();
          retVal = (effect.getResource().equalsIgnoreCase(name));
        }
        break;
    }

    return retVal;
  }

  /**
   * Evaluates the SPLPROT filter specified by type and value for the given target.
   * @param creInfo creature target
   * @param type SPLPROT.2DA entry index
   * @param refValue an optional reference value used by selected types
   * @return whether creature stat matches reference stat.
   */
  private boolean evaluateSplProt(CreatureInfo creInfo, int type, int refValue)
  {
    Table2da table = Table2daCache.get("SPLPROT.2DA");
    if (table == null) {
      return true;
    }

    String s = table.get(type, 1);
    int radix = s.startsWith("0x") ? 16 : 10;
    int stat = Misc.toNumber(s, radix, -1);

    s = table.get(type, 2);
    radix = s.startsWith("0x") ? 16 : 10;
    int value = Misc.toNumber(s, radix, -1);
    if (value == -1) {
      value = refValue;
    }

    s = table.get(type, 3);
    radix = s.startsWith("0x") ? 16 : 10;
    int rel = Misc.toNumber(s, radix, -1);

    switch (stat) {
      case 0x100: // source equals target
        // irrelevant
        break;
      case 0x101: // source is not target
        // irrelevant
        break;
      case 0x102: // circle size
        return splProtRelation(rel, creInfo.getDecoder().getPersonalSpace(), value);
      case 0x103: // use two rows
        return evaluateSplProt(creInfo, value, -1) || evaluateSplProt(creInfo, rel, -1);
      case 0x104: // negate 0x103
        return !(evaluateSplProt(creInfo, value, -1) || evaluateSplProt(creInfo, rel, -1));
      case 0x105: // source and target morale match
        // not supported
        break;
      case 0x106: // AREATYPE.IDS
        // irrelevant
        break;
      case 0x107: // time of day (hours)
        // irrelevant
        break;
      case 0x108: // source and target ethical match
        // not supported
        break;
      case 0x109: // evasion
        // not supported
        break;
      case 0x10a: // EA.IDS
        return splProtRelation(rel, creInfo.getAllegiance(), value);
      case 0x10b: // GENERAL.IDS
        return splProtRelation(rel, ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_GENERAL)).getValue(), value);
      case 0x10c: // RACE.iDS
        return splProtRelation(rel, ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RACE)).getValue(), value);
      case 0x10d: // CLASS.IDS
        return splProtRelation(rel, ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_CLASS)).getValue(), value);
      case 0x10e: // SPECIFIC.IDS
        return splProtRelation(rel, ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SPECIFICS)).getValue(), value);
      case 0x10f: // GENDER.IDS
        return splProtRelation(rel, ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_GENDER)).getValue(), value);
      case 0x110: // ALIGN.IDS
        return splProtRelation(rel, ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_ALIGNMENT)).getValue(), value);
      case 0x111: // STATE.IDS
        return splProtRelation(rel, creInfo.getStatus(), value);
      case 0x112: // SPLSTATE.IDS
        // not supported
        break;
      case 0x113: // source and target allies
        // irrelevant
        break;
      case 0x114: // source and target enemies
        // irrelevant
        break;
      case 0x115: // summon creature limit
        // irrelevant
        break;
      case 0x116: // chapter check
        // irrelevant
        break;
      default:    // STATS.IDS
        return splProtRelation(rel, getStatValue(creInfo, stat), value);
    }
    return true;
  }

  /**
   * Performs a relational operation on the value parameters and returns the result.
   * @param relation the relation type
   * @param creValue value retrieved from the target
   * @param refValue value retrieved either from splprot table or custom effect value
   */
  private boolean splProtRelation(int relation, int creValue, int refValue)
  {
    switch (relation) {
      case 0:   // less or equal
        return (creValue <= refValue);
      case 1:   // equal
        return (creValue == refValue);
      case 2:   // less
        return (creValue < refValue);
      case 3:   // greater
        return (creValue > refValue);
      case 4:   // greater or equal
        return (creValue >= refValue);
      case 5:   // not equal
        return (creValue != refValue);
      case 6:   // binary less or equal (stat doesn't contain extra bits not in value)
        return (creValue & ~refValue) == 0;
      case 7:   // binary more or equal (stat contains all bits of value)
        return (creValue & refValue) == refValue;
      case 8:   // binary match (at least one bit is common)
        return (creValue & refValue) != 0;
      case 9:   // binary not match (none of the bits are common)
        return (creValue & refValue) == 0;
      case 10:  // binary more (stat contains at least one bit not in value)
        return (creValue & ~refValue) != 0;
      case 11:  // binary less (stat doesn't contain all the bits of value)
        return (creValue & refValue) != refValue;
    }
    return false;
  }

  /** Retrieves the specified stat from the given creature if available. Returns 0 otherwise. */
  private int getStatValue(CreatureInfo creInfo, int stat)
  {
    // only selected stat values are supported
    switch (stat) {
      case 1:   // MAXHITPOINTS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_HP_MAX)).getValue();
      case 2:   // ARMORCLASS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_AC_EFFECTIVE)).getValue();
      case 7:   // THAC0
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_THAC0)).getValue();
      case 8:   // NUMBEROFATTACKS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_ATTACKS_PER_ROUND)).getValue();
      case 9:   // SAVEVSDEATH
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SAVE_DEATH)).getValue();
      case 10:  // SAVEVSWANDS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SAVE_WAND)).getValue();
      case 11:  // SAVEVSPOLY
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SAVE_POLYMORPH)).getValue();
      case 12:  // SAVEVSBREATH
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SAVE_BREATH)).getValue();
      case 13:  // SAVEVSSPELL
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SAVE_SPELL)).getValue();
      case 14:  // RESISTFIRE
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_FIRE)).getValue();
      case 15:  // RESISTCOLD
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_COLD)).getValue();
      case 16:  // RESISTELECTRICITY
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_ELECTRICITY)).getValue();
      case 17:  // RESISTACID
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_ACID)).getValue();
      case 18:  // RESISTMAGIC
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_MAGIC)).getValue();
      case 19:  // RESISTMAGICFIRE
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_MAGIC_FIRE)).getValue();
      case 20:  // RESISTMAGICCOLD
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_MAGIC_COLD)).getValue();
      case 21:  // RESISTSLASHING
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_SLASHING)).getValue();
      case 22:  // RESISTCRUSHING
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_CRUSHING)).getValue();
      case 23:  // RESISTPIERCING
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_PIERCING)).getValue();
      case 24:  // RESISTMISSILE
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_RESISTANCE_MISSILE)).getValue();
      case 25:  // LORE
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_LORE)).getValue();
      case 26:  // LOCKPICKING
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_OPEN_LOCKS)).getValue();
      case 27:  // STEALTH
      {
        int v1 = ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_MOVE_SILENTLY)).getValue();
        int v2;
        if (Profile.getEngine() != Profile.Engine.BG1 && Profile.getEngine() != Profile.Engine.IWD) {
          v2 = ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_HIDE_IN_SHADOWS)).getValue();
        } else {
          v2 = v1;
        }
        return (v1 + v2) / 2;
      }
      case 28:  // TRAPS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_FIND_TRAPS)).getValue();
      case 29:  // PICKPOCKET
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_PICK_POCKETS)).getValue();
      case 30:  // FATIGUE
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_FATIGUE)).getValue();
      case 31:  // INTOXICATION
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_INTOXICATION)).getValue();
      case 32:  // LUCK
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_LUCK)).getValue();
      case 34:  // LEVEL
      {
        int v1 = ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_LEVEL_FIRST_CLASS)).getValue();
        int v2 = ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_LEVEL_SECOND_CLASS)).getValue();
        int v3 = ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_LEVEL_THIRD_CLASS)).getValue();
        if ((creInfo.getFlags() & 0x1f8) != 0) {
          // dual-classed?
          return v2;
        } else {
          return Math.max(v1, Math.max(v2, v3));
        }
      }
      case 35:  // SEX
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_GENDER)).getValue();
      case 36:  // STR
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_STRENGTH)).getValue();
      case 37:  // STREXTRA
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_STRENGTH_BONUS)).getValue();
      case 38:  // INT
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_INTELLIGENCE)).getValue();
      case 39:  // WIS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_WISDOM)).getValue();
      case 40:  // DEX
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_DEXTERITY)).getValue();
      case 41:  // CON
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_CONSTITUTION)).getValue();
      case 42:  // CHR
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_CHARISMA)).getValue();
      case 43:  // XPVALUE
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_XP_VALUE)).getValue();
      case 44:  // XP
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_XP)).getValue();
      case 45:  // GOLD
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_GOLD)).getValue();
      case 46:  // MORALEBREAK
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_MORALE_BREAK)).getValue();
      case 47:  // MORALERECOVERYTIME
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_MORALE_RECOVERY)).getValue();
      case 48:  // REPUTATION
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_REPUTATION)).getValue();
      case 60:  // TRANSLUCENT
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_TRANSLUCENCY)).getValue();
      case 68:  // LEVEL2
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_LEVEL_SECOND_CLASS)).getValue();
      case 69:  // LEVEL3
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_LEVEL_THIRD_CLASS)).getValue();
      case 135: // HIDEINSHADOWS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_HIDE_IN_SHADOWS)).getValue();
      case 136: // DETECTILLUSIONS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_DETECT_ILLUSION)).getValue();
      case 137: // SETTRAPS
        return ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_SET_TRAPS)).getValue();
      case 152: // KIT
      {
        int v = ((IsNumeric)creInfo.getCreResource().getAttribute(CreResource.CRE_KIT)).getValue();
        v = ((v >>> 16) & 0xffff) | ((v & 0xffff) << 16);
        return v;
      }
    }
    return 0;
  }

  @Override
  public int hashCode()
  {
    int hashCode = 7;
    hashCode = 31 * hashCode + ((effectMap == null) ? 0 : effectMap.hashCode());
    return hashCode;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    }
    if (!(o instanceof EffectInfo)) {
      return false;
    }

    EffectInfo other = (EffectInfo)o;

    boolean retVal = (effectMap == null && other.effectMap == null) ||
                     (effectMap != null && effectMap.equals(other.effectMap));

    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  public static class Effect
  {
    private final int opcode;
    private final int[] probability = new int[2];

    private int target;
    private int power;
    private int parameter1;
    private int parameter2;
    private int timing;
    private int dispelResist;
    private int duration;
    private String resource;
    private int diceCount;
    private int diceSize;
    private int saveFlags;
    private int saveBonus;
    private int special;

    private Effect parent;

    /** Convenience method for creating an {@code Effect} instance from a byte array containing EFF V1 data. */
    public static Effect fromEffectV1(ByteBuffer buf, int startOfs)
    {
      Effect retVal = new Effect(buf.order(ByteOrder.LITTLE_ENDIAN).getShort(0x0 + startOfs));
      retVal.initEffectV1(buf, startOfs);
      return retVal;
    }

    /** Convenience method for creating an {@code Effect} instance from a byte array containing EFF V2 data. */
    public static Effect fromEffectV2(ByteBuffer buf, int startOfs)
    {
      Effect retVal = new Effect(buf.order(ByteOrder.LITTLE_ENDIAN).getInt(0x8 + startOfs));
      retVal.initEffectV2(buf, startOfs);
      return retVal;
    }

    /** Initializes a new Effect instance. */
    public Effect(int opcode)
    {
      this.opcode = opcode;
      setProbability(0, 100);
      setTarget(1); // Self
      setTiming(2); // Instant/While equipped
    }

    /** Initializes a new Effect instance from an effect V1 resource structure. */
    public Effect(org.infinity.resource.Effect struct)
    {
      ByteBuffer bb = Objects.requireNonNull(struct).getDataBuffer().order(ByteOrder.LITTLE_ENDIAN);

      this.opcode = bb.getShort(0x0);
      initEffectV1(bb, 0);
    }

    /** Initializes a new Effect instance from an effect V2 resource structure. */
    public Effect(org.infinity.resource.Effect2 struct)
    {
      ByteBuffer bb = Objects.requireNonNull(struct).getDataBuffer().order(ByteOrder.LITTLE_ENDIAN);

      this.opcode = bb.getInt(0x8);
      initEffectV2(bb, 0);
    }

    /** Returns the parent effect which applies the current effect. */
    public Effect getParent() { return parent; }
    /** Sets the parent effect which applies the current effect. */
    public Effect setParent(Effect parent) { this.parent = parent; return this; }

    public int getOpcode() { return opcode; }

    public int getTarget() { return target; }
    public Effect setTarget(int v) { target = v; return this; }

    public int getPower() { return power; }
    public Effect setPower(int v) { power = v; return this; }

    public int getParameter1() { return parameter1; }
    public Effect setParameter1(int v) { parameter1 = v; return this; }

    public int getParameter2() { return parameter2; }
    public Effect setParameter2(int v) { parameter2 = v; return this; }

    public int getTiming() { return timing; }
    public Effect setTiming(int v) { timing = v; return this; }

    public int getDispelResist() { return dispelResist; }
    public Effect setDispelResist(int v) { dispelResist = v; return this; }

    public int getDuration() { return duration; }
    public Effect setDuration(int v) { duration = v; return this; }

    public Effect setProbability(int low, int high)
    {
      probability[0] = low;
      probability[1] = high;
      return this;
    }

    public int getProbabilityLow() { return probability[0]; }
    public Effect setProbabilityLow(int v) { probability[0] = v; return this; }

    public int getProbabilityHigh() { return probability[1]; }
    public Effect setProbabilityHigh(int v) { probability[1] = v; return this; }

    public String getResource() { return resource; }
    public Effect setResource(String s) { resource = (s != null) ? s : ""; return this; }

    public int getDiceCount() { return diceCount; }
    public Effect setDiceCount(int v) { diceCount = v; return this; }

    public int getDiceSize() { return diceSize; }
    public Effect setDiceSize(int v) { diceSize = v; return this; }

    public int getSaveFlags() { return saveFlags; }
    public Effect setSaveFlags(int v) { saveFlags = v; return this; }
    public Effect setSaveFlag(int bitIdx, boolean set)
    {
      if (bitIdx >= 0 && bitIdx < 32) {
        if (set) {
          saveFlags |= (1 << bitIdx);
        } else {
          saveFlags &= ~(1 << bitIdx);
        }
      }
      return this;
    }

    public int getSaveBonus() { return saveBonus; }
    public Effect setSaveBonus(int v) { saveBonus = v; return this; }

    public int getSpecial() { return special; }
    public Effect setSpecial(int v) { special = v; return this; }

    @Override
    public int hashCode()
    {
      int hashCode = 7;
      hashCode = 31 * hashCode + opcode;
      hashCode = 31 * hashCode + probability[0];
      hashCode = 31 * hashCode + probability[1];
      hashCode = 31 * hashCode + target;
      hashCode = 31 * hashCode + power;
      hashCode = 31 * hashCode + parameter1;
      hashCode = 31 * hashCode + parameter2;
      hashCode = 31 * hashCode + timing;
      hashCode = 31 * hashCode + dispelResist;
      hashCode = 31 * hashCode + duration;
      hashCode = 31 * hashCode + ((resource == null) ? 0 : resource.hashCode());
      hashCode = 31 * hashCode + diceCount;
      hashCode = 31 * hashCode + diceSize;
      hashCode = 31 * hashCode + saveFlags;
      hashCode = 31 * hashCode + saveBonus;
      hashCode = 31 * hashCode + special;

      return hashCode;
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o) {
        return true;
      }
      if (!(o instanceof Effect)) {
        return false;
      }

      Effect other = (Effect)o;
      boolean retVal = opcode == other.opcode;
      retVal &= (probability[0] == other.probability[0]);
      retVal &= (probability[1] == other.probability[1]);
      retVal &= (target == other.target);
      retVal &= (power == other.power);
      retVal &= (parameter1 == other.parameter1);
      retVal &= (parameter2 == other.parameter2);
      retVal &= (timing == other.timing);
      retVal &= (dispelResist == other.dispelResist);
      retVal &= (duration == other.duration);
      retVal &= (resource == null && other.resource == null) ||
                (resource != null && resource.equals(other.resource));
      retVal &= (diceCount == other.diceCount);
      retVal &= (diceSize == other.diceSize);
      retVal &= (saveFlags == other.saveFlags);
      retVal &= (saveBonus == other.saveBonus);
      retVal &= (special == other.special);

      return retVal;
    }

    // Initializes effect parameters from EFF V1 structure
    private void initEffectV1(ByteBuffer buf, int startOfs)
    {
      setTarget(buf.get(0x2 + startOfs));
      setPower(buf.get(0x3 + startOfs));
      setParameter1(buf.getInt(0x4 + startOfs));
      setParameter2(buf.getInt(0x8 + startOfs));
      setTiming(buf.get(0xc + startOfs));
      setDispelResist(buf.get(0xd + startOfs));
      setDuration(buf.getInt(0xe + startOfs));
      setProbability(buf.get(0x13 + startOfs), buf.get(0x12 + startOfs));
      setResource(StreamUtils.readString(buf, 0x14 + startOfs, 8));
      setDiceCount(buf.getInt(0x1c + startOfs));
      setDiceSize(buf.getInt(0x20 + startOfs));
      setSaveFlags(buf.getInt(0x24 + startOfs));
      setSaveBonus(buf.getInt(0x28 + startOfs));
      setSpecial(buf.getInt(0x2c + startOfs));
    }

    // Initializes effect parameters from EFF V2 structure. startOfs can be used to adjust offset for EFF resources.
    private void initEffectV2(ByteBuffer buf, int startOfs)
    {
      setTarget(buf.getInt(0xc + startOfs));
      setPower(buf.getInt(0x10 + startOfs));
      setParameter1(buf.getInt(0x14 + startOfs));
      setParameter2(buf.getInt(0x18 + startOfs));
      setTiming(buf.getInt(0x1c + startOfs));
      setDispelResist(buf.get(0x54 + startOfs));
      setDuration(buf.getInt(0x20 + startOfs));
      setProbability(buf.getShort(0x26 + startOfs), buf.getShort(0x24 + startOfs));
      setResource(StreamUtils.readString(buf, 0x28 + startOfs, 8));
      setDiceCount(buf.getInt(0x30 + startOfs));
      setDiceSize(buf.getInt(0x34 + startOfs));
      setSaveFlags(buf.getInt(0x38 + startOfs));
      setSaveBonus(buf.getInt(0x3c + startOfs));
      setSpecial(buf.getInt(0x40 + startOfs));
    }
  }
}
