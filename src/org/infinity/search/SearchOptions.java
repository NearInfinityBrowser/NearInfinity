// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsTextual;
import org.infinity.datatype.IwdRef;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.Unknown;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructEntry;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

/**
 * Stores a list of search options specified in SearchResource (Extended search) for use in the
 * resource-specific search methods.
 */
public class SearchOptions
{
  // List of option keys for each supported resource type
  // Format 1: [Type1.[Type2.[...]]]]Name.Index
  // Exception 1: Names starting with an underscore (_) are fetched by offset, index specifies the (relative) offset value
  // Exception 2: Names starting with a plus sign (+) are symbolic names with special meanings
  // REMEMBER: Keep list of Strings in sync with options provided by SearchResource
  // REMEMBER: String values must correspond to the respective resource attribute names
  public static final String ARE                      = "ARE.0";              // marks structure
  public static final String ARE_AreaScript           = "ARE.Area script.0";
  public static final String ARE_AreaType             = "ARE.Area type.0";
  public static final String ARE_Location             = "ARE.Location.0";
  public static final String ARE_Actor                = "ARE.Actor %d.0";        // marks substructure
  public static final String ARE_Actor_Character      = "ARE.Actor.Character.0";
  public static final String ARE_Animation            = "ARE.Animation %d.0";    // marks substructure
  public static final String ARE_Animation_Animation  = "ARE.Animation.Animation.0";
  public static final String ARE_Container            = "ARE.Container %d.0";    // marks substructure
  public static final String ARE_Container_Item       = "ARE.Container.Item %d.0";  // marks substructure
  public static final String ARE_Container_Item_Item  = "ARE.Container.Item.Item.0";
  public static final String ARE_Custom1              = "ARE.+Custom.0";
  public static final String ARE_Custom2              = "ARE.+Custom.1";
  public static final String ARE_Custom3              = "ARE.+Custom.2";
  public static final String ARE_Custom4              = "ARE.+Custom.3";

  public static final String CRE            = "CRE.0";            // marks structure
  public static final String CRE_Name       = "CRE.Name.0";
  public static final String CRE_ScriptName = "CRE.Script name.0";
  public static final String CRE_Script1    = "CRE.+Script.0";    // special: matches all available scripts
  public static final String CRE_Script2    = "CRE.+Script.1";    // special: matches all available scripts
  public static final String CRE_Script3    = "CRE.+Script.2";    // special: matches all available scripts
  public static final String CRE_Flags      = "CRE.Flags.0";
  public static final String CRE_Level1     = "CRE.Level first class.0";
  public static final String CRE_Level2     = "CRE.Level second class.0";
  public static final String CRE_Level3     = "CRE.Level third class.0";
  public static final String CRE_IWD2LevelTotal     = "CRE.Total level.0";
  public static final String CRE_IWD2LevelBarbarian = "CRE.Barbarian level.0";
  public static final String CRE_IWD2LevelBard      = "CRE.Bard level.0";
  public static final String CRE_IWD2LevelCleric    = "CRE.Cleric level.0";
  public static final String CRE_IWD2LevelDruid     = "CRE.Druid level.0";
  public static final String CRE_IWD2LevelFighter   = "CRE.Fighter level.0";
  public static final String CRE_IWD2LevelMonk      = "CRE.Monk level.0";
  public static final String CRE_IWD2LevelPaladin   = "CRE.Paladin level.0";
  public static final String CRE_IWD2LevelRanger    = "CRE.Ranger level.0";
  public static final String CRE_IWD2LevelRogue     = "CRE.Rogue level.0";
  public static final String CRE_IWD2LevelSorcerer  = "CRE.Sorcerer level.0";
  public static final String CRE_IWD2LevelWizard    = "CRE.Wizard level.0";
  public static final String CRE_General    = "CRE.General.0";
  public static final String CRE_Class      = "CRE.Class.0";
  public static final String CRE_Specifics  = "CRE.Specifics.0";
  public static final String CRE_Alignment  = "CRE.Alignment.0";
  public static final String CRE_Gender     = "CRE.Gender.0";
  public static final String CRE_Sex        = "CRE.Sex.0";        // IWD2 only
  public static final String CRE_Race       = "CRE.Race.0";
  public static final String CRE_Allegiance = "CRE.Allegiance.0";
  public static final String CRE_Kit        = "CRE.Kit.0";
  public static final String CRE_Animation  = "CRE.Animation.0";
  public static final String CRE_Feats1     = "CRE.Feats (1/3).0";
  public static final String CRE_Feats2     = "CRE.Feats (2/3).0";
  public static final String CRE_Feats3     = "CRE.Feats (3/3).0";
  public static final String CRE_Attributes = "CRE.Attributes.0";
  public static final String CRE_Effect       = "CRE.Effect %d.0";    // marks substructure
  public static final String CRE_Effect_Type1 = "CRE.Effect.Type.0";
  public static final String CRE_Effect_Type2 = "CRE.Effect.Type.1";
  public static final String CRE_Effect_Type3 = "CRE.Effect.Type.2";
  public static final String CRE_Effect_Type4 = "CRE.Effect.Type.3";
  public static final String CRE_Item         = "CRE.Item %d.0";    // marks substructure
  public static final String CRE_Item_Item1   = "CRE.Item.Item.0";
  public static final String CRE_Item_Item2   = "CRE.Item.Item.1";
  public static final String CRE_Item_Item3   = "CRE.Item.Item.2";
  public static final String CRE_Item_Item4   = "CRE.Item.Item.3";
  public static final String CRE_Spell         = "CRE.Known spell %d.0";  // marks substructure
  public static final String CRE_Spell_Spell1  = "CRE.Known spell.Spell.0";
  public static final String CRE_Spell_Spell2  = "CRE.Known spell.Spell.1";
  public static final String CRE_Spell_Spell3  = "CRE.Known spell.Spell.2";
  public static final String CRE_Spell_Spell4  = "CRE.Known spell.Spell.3";
  public static final String CRE_IWD2SpellBard                  = "CRE.Bard spells %d.0";     // marks substructure
  public static final String CRE_IWD2SpellBard_Spell            = "CRE.Bard spells.Spell.0";    // marks substructure
  public static final String CRE_IWD2SpellBard_Spell_ResRef     = "CRE.Bard spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellCleric                = "CRE.Cleric spells %d.0";   // marks substructure
  public static final String CRE_IWD2SpellCleric_Spell          = "CRE.Cleric spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellCleric_Spell_ResRef   = "CRE.Cleric spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellDruid                 = "CRE.Druid spells %d.0";   // marks substructure
  public static final String CRE_IWD2SpellDruid_Spell           = "CRE.Druid spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellDruid_Spell_ResRef    = "CRE.Druid spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellPaladin               = "CRE.Paladin spells %d.0";   // marks substructure
  public static final String CRE_IWD2SpellPaladin_Spell         = "CRE.Paladin spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellPaladin_Spell_ResRef  = "CRE.Paladin spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellRanger                = "CRE.Ranger spells %d.0";   // marks substructure
  public static final String CRE_IWD2SpellRanger_Spell          = "CRE.Ranger spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellRanger_Spell_ResRef   = "CRE.Ranger spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellSorcerer              = "CRE.Sorcerer spells %d.0";   // marks substructure
  public static final String CRE_IWD2SpellSorcerer_Spell        = "CRE.Sorcerer spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellSorcerer_Spell_ResRef = "CRE.Sorcerer spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellWizard                = "CRE.Wizard spells %d.0";   // marks substructure
  public static final String CRE_IWD2SpellWizard_Spell          = "CRE.Wizard spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellWizard_Spell_ResRef   = "CRE.Wizard spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellDomain                = "CRE.Domain spells %d.0";   // marks substructure
  public static final String CRE_IWD2SpellDomain_Spell          = "CRE.Domain spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellDomain_Spell_ResRef   = "CRE.Domain spells.Spell.ResRef.0";
  public static final String CRE_Custom1                        = "CRE.+Custom.0";
  public static final String CRE_Custom2                        = "CRE.+Custom.1";
  public static final String CRE_Custom3                        = "CRE.+Custom.2";
  public static final String CRE_Custom4                        = "CRE.+Custom.3";

  public static final String EFF            = "EFF.0";                // marks structure
  public static final String EFF_Effect     = "EFF.Type.0";
  public static final String EFF_Param1     = "EFF._Param 1.28";      // special: fetched by offset
  public static final String EFF_Param2     = "EFF._Param 2.32";      // special: fetched by offset
  public static final String EFF_TimingMode = "EFF.Timing Mode.0";
  public static final String EFF_Duration   = "EFF.Duration.0";
  public static final String EFF_Resource1  = "EFF._Resource 1.48";   // special: fetched by offset
  public static final String EFF_Resource2  = "EFF.Resource 2.0";
  public static final String EFF_Resource3  = "EFF.Resource 3.0";
  public static final String EFF_SaveType   = "EFF.Save type.0";
  public static final String EFF_Custom1    = "EFF.+Custom.0";
  public static final String EFF_Custom2    = "EFF.+Custom.1";
  public static final String EFF_Custom3    = "EFF.+Custom.2";
  public static final String EFF_Custom4    = "EFF.+Custom.3";

  public static final String ITM                      = "ITM.0";            // marks structure
  public static final String ITM_Name                 = "ITM.Identified name.0";
  public static final String ITM_Flags                = "ITM.Flags.0";
  public static final String ITM_Category             = "ITM.Category.0";
  public static final String ITM_Unusable             = "ITM.Unusable by.0";
  public static final String ITM_KitsUnusable1        = "ITM.Unusable by (1/4).0";
  public static final String ITM_KitsUnusable2        = "ITM.Unusable by (2/4).0";
  public static final String ITM_KitsUnusable3        = "ITM.Unusable by (3/4).0";
  public static final String ITM_KitsUnusable4        = "ITM.Unusable by (4/4).0";
  public static final String ITM_Appearance           = "ITM.Equipped appearance.0";
  public static final String ITM_MinLevel             = "ITM.Minimum level.0";
  public static final String ITM_MinSTR               = "ITM.Minimum strength.0";
  public static final String ITM_MinSTRExtra          = "ITM.Minimum strength bonus.0";
  public static final String ITM_MinCON               = "ITM.Minimum constitution.0";
  public static final String ITM_MinDEX               = "ITM.Minimum dexterity.0";
  public static final String ITM_MinINT               = "ITM.Minimum intelligence.0";
  public static final String ITM_MinWIS               = "ITM.Minimum wisdom.0";
  public static final String ITM_MinCHA               = "ITM.Minimum charisma.0";
  public static final String ITM_Price                = "ITM.Price.0";
  public static final String ITM_Enchantment          = "ITM.Enchantment.0";
  public static final String ITM_Ability              = "ITM.Item ability %d.0";   // marks substructure
  public static final String ITM_Ability_MatchSingle  = "ITM.Item ability.MatchSingle.0";  // special: indicates whether to limit matches to a single ability
  public static final String ITM_Ability_Type         = "ITM.Item ability.Type.0";
  public static final String ITM_Ability_Target       = "ITM.Item ability.Target.0";
  public static final String ITM_Ability_Launcher     = "ITM.Item ability.Launcher required.0";
  public static final String ITM_Ability_DamageType   = "ITM.Item ability.Damage type.0";
  public static final String ITM_Ability_Projectile   = "ITM.Item ability.Projectile.0";
  public static final String ITM_Ability_Range        = "ITM.Item ability.Range (feet).0";
  public static final String ITM_Ability_Speed        = "ITM.Item ability.Speed.0";
  public static final String ITM_Ability_DiceCount    = "ITM.Item ability.# dice thrown.0";
  public static final String ITM_Ability_DiceSize     = "ITM.Item ability.Dice size.0";
  public static final String ITM_Ability_Charges      = "ITM.Item ability.# charges.0";
  public static final String ITM_Ability_Effect       = "ITM.Item ability.Effect %d.0";   // marks substructure
  public static final String ITM_Ability_Effect_Type1 = "ITM.Item ability.Effect.Type.0";
  public static final String ITM_Ability_Effect_Type2 = "ITM.Item ability.Effect.Type.1";
  public static final String ITM_Ability_Effect_Type3 = "ITM.Item ability.Effect.Type.2";
  public static final String ITM_Ability_Flags        = "ITM.Item ability.Flags.0";
  public static final String ITM_Effect               = "ITM.Effect %d.0";   // marks substructure
  public static final String ITM_Effect_Type1         = "ITM.Effect.Type.0";
  public static final String ITM_Effect_Type2         = "ITM.Effect.Type.1";
  public static final String ITM_Effect_Type3         = "ITM.Effect.Type.2";
  public static final String ITM_Custom1              = "ITM.+Custom.0";
  public static final String ITM_Custom2              = "ITM.+Custom.1";
  public static final String ITM_Custom3              = "ITM.+Custom.2";
  public static final String ITM_Custom4              = "ITM.+Custom.3";

  public static final String PRO                  = "PRO.0";                    // marks structure
  public static final String PRO_Animation        = "PRO.Projectile animation.0";
  public static final String PRO_Type             = "PRO.Projectile type.0";
  public static final String PRO_Speed            = "PRO.Speed.0";
  public static final String PRO_Behavior         = "PRO.Behavior.0";
  public static final String PRO_Flags            = "PRO.Flags.0";
  public static final String PRO_AreaFlags        = "PRO.Area flags.0";
  public static final String PRO_TrapSize         = "PRO.Trap size.0";
  public static final String PRO_ExplosionSize    = "PRO.Explosion size.0";
  public static final String PRO_ExplosionEffect  = "PRO.Explosion effect.0";
  public static final String PRO_SingleTarget     = "PRO.Projectile info.0";    // marks substructure
  public static final String PRO_AreaOfEffect     = "PRO.Area effect info.0";   // marks substructure
  public static final String PRO_Custom1          = "PRO.+Custom.0";
  public static final String PRO_Custom2          = "PRO.+Custom.1";
  public static final String PRO_Custom3          = "PRO.+Custom.2";
  public static final String PRO_Custom4          = "PRO.+Custom.3";

  public static final String SPL                      = "SPL.0";                  // marks structure
  public static final String SPL_Name                 = "SPL.Spell name.0";
  public static final String SPL_Flags                = "SPL.Flags.0";
  public static final String SPL_SpellType            = "SPL.Spell type.0";
  public static final String SPL_Exclusion            = "SPL.Exclusion flags.0";
  public static final String SPL_CastingAnimation     = "SPL.Casting animation.0";
  public static final String SPL_PrimaryType          = "SPL.Primary type (school).0";
  public static final String SPL_SecondaryType        = "SPL.Secondary type.0";
  public static final String SPL_Level                = "SPL.Spell level.0";
  public static final String SPL_Ability              = "SPL.Spell ability %d.0";    // marks substructure
  public static final String SPL_Ability_MatchSingle  = "SPL.Spell ability.MatchSingle.0";  // special: indicates whether to limit matches to a single ability
  public static final String SPL_Ability_Type         = "SPL.Spell ability.Type.0";
  public static final String SPL_Ability_Location     = "SPL.Spell ability.Ability location.0";
  public static final String SPL_Ability_Target       = "SPL.Spell ability.Target.0";
  public static final String SPL_Ability_Range        = "SPL.Spell ability.Range (feet).0";
  public static final String SPL_Ability_Level        = "SPL.Spell ability.Minimum level.0";
  public static final String SPL_Ability_Speed        = "SPL.Spell ability.Casting speed.0";
  public static final String SPL_Ability_Projectile   = "SPL.Spell ability.Projectile.0";
  public static final String SPL_Ability_Effect       = "SPL.Spell ability.Effect %d.0";   // marks substructure
  public static final String SPL_Ability_Effect_Type1 = "SPL.Spell ability.Effect.Type.0";
  public static final String SPL_Ability_Effect_Type2 = "SPL.Spell ability.Effect.Type.1";
  public static final String SPL_Ability_Effect_Type3 = "SPL.Spell ability.Effect.Type.2";
  public static final String SPL_Effect               = "SPL.Effect %d.0";     // marks substructure
  public static final String SPL_Effect_Type1         = "SPL.Effect.Type.0";
  public static final String SPL_Effect_Type2         = "SPL.Effect.Type.1";
  public static final String SPL_Effect_Type3         = "SPL.Effect.Type.2";
  public static final String SPL_Custom1              = "SPL.+Custom.0";
  public static final String SPL_Custom2              = "SPL.+Custom.1";
  public static final String SPL_Custom3              = "SPL.+Custom.2";
  public static final String SPL_Custom4              = "SPL.+Custom.3";

  public static final String STO                = "STO.0";    // marks structure
  public static final String STO_Name           = "STO.Name.0";
  public static final String STO_Type           = "STO.Type.0";
  public static final String STO_Flags          = "STO.Flags.0";
  public static final String STO_SellMarkup     = "STO.Sell markup.0";
  public static final String STO_BuyMarkup      = "STO.Buy markup.0";
  public static final String STO_Stealing       = "STO.Stealing difficulty.0";
  public static final String STO_Capacity       = "STO.Storage capacity.0";
  public static final String STO_Depreciation   = "STO.Depreciation rate.0";
  public static final String STO_RoomsAvailable = "STO.Available rooms.0";
  public static final String STO_Purchased      = "STO.Store purchases %d.0";   // marks addremovable field
  public static final String STO_Purchased1     = "STO.Store purchases.0";
  public static final String STO_Purchased2     = "STO.Store purchases.1";
  public static final String STO_Purchased3     = "STO.Store purchases.2";
  public static final String STO_Purchased4     = "STO.Store purchases.3";
  public static final String STO_Purchased5     = "STO.Store purchases.4";
  public static final String STO_Item           = "STO.Item for sale %d.0";  // marks substructure
  public static final String STO_Item_Item1     = "STO.Item for sale.Item.0";
  public static final String STO_Item_Item2     = "STO.Item for sale.Item.1";
  public static final String STO_Item_Item3     = "STO.Item for sale.Item.2";
  public static final String STO_Item_Item4     = "STO.Item for sale.Item.3";
  public static final String STO_Item_Item5     = "STO.Item for sale.Item.4";
  public static final String STO_Custom1        = "STO.+Custom.0";
  public static final String STO_Custom2        = "STO.+Custom.1";
  public static final String STO_Custom3        = "STO.+Custom.2";
  public static final String STO_Custom4        = "STO.+Custom.3";

  public static final String VVC                  = "VVC.0";    // marks structure
  public static final String VVC_Animation        = "VVC.Animation.0";
  public static final String VVC_Flags            = "VVC.Drawing.0";
  public static final String VVC_ColorAdjustment  = "VVC.Color adjustment.0";
  public static final String VVC_Sequencing       = "VVC.Sequencing.0";
  public static final String VVC_Orientation      = "VVC.Travel orientation.0";
  public static final String VVC_Custom1          = "VVC.+Custom.0";
  public static final String VVC_Custom2          = "VVC.+Custom.1";
  public static final String VVC_Custom3          = "VVC.+Custom.2";
  public static final String VVC_Custom4          = "VVC.+Custom.3";


  private final HashMap<String, Object> mapOptions = new HashMap<>();
  private final String resourceType;

  /**
   * Extracts the resource type from the specified option key.
   * @param key The option key to extract the resource type from.
   * @return The resource type associated with the option key.
   */
  public static String getResourceType(String key)
  {
    if (key != null) {
      String[] segments = key.split("\\.");
      if (segments != null) {
        if (segments.length > 2) {
          return segments[segments.length - 3];
        }
      }
    }
    return "";
  }

  /**
   * Extracts the resource name from the specified option key.
   * @param key The option key to extract the resource name from.
   * @return The resource name associated with the option key.
   */
  public static String getResourceName(String key)
  {
    if (key != null) {
      String[] segments = key.split("\\.");
      if (segments != null) {
        if (segments.length > 1) {
          return segments[segments.length - 2];
        }
      }
    }
    return "";
  }

  /**
   * Extracts the resource index (or offset) from the specified option key.
   * @param key The option key to extract the resource index/offset from.
   * @return The resource index/offset associated with the option key.
   */
  public static int getResourceIndex(String key)
  {
    if (key != null) {
      String[] segments = key.split("\\.");
      if (segments != null) {
        if (segments.length > 0) {
          try {
            return Integer.parseInt(segments[segments.length - 1]);
          } catch (NumberFormatException e) {
          }
        }
      }
    }
    return 0;
  }

  /**
   * Returns the nested level of the option key.
   * @param key The option key to extract the resource index from.
   * @return The nested level (0=no resource type defined, 1=first level resource type, ...)
   */
  public static int getResourceNameLevel(String key)
  {
    if (key != null) {
      String[] segments = key.split("\\.");
      if (segments != null) {
        if (segments.length > 1) {
          return segments.length - 2;
        }
      }
    }
    return 0;
  }

  /**
   * Returns whether the specified option key defines a resource field by offset rather than by name.
   * @param key The option key.
   * @return {@code true} if the specified option key defines a resource field by offset.
   */
  public static boolean isResourceByOffset(String key)
  {
    if (key != null) {
      String[] segments = key.split("\\.");
      if (segments != null) {
        if (segments.length > 1) {
          return segments[segments.length - 2].startsWith("_");
        }
      }
    }
    return false;
  }


  /**
   * Initializes a new empty SearchOptions container.
   * @param resourceType The resource type to identify the list of option keys.
   */
  public SearchOptions(String resourceType)
  {
    this.resourceType = resourceType;
  }

  /**
   * Returns true if this instance doesn't contain any option entries.
   * @return true if this instance doesn't contain any option entries.
   */
  public boolean isEmpty()
  {
    return mapOptions.isEmpty();
  }

  /**
   * Returns the number of option entries.
   * @return Number of option entries.
   */
  public int size()
  {
    return mapOptions.size();
  }

  /**
   * Returns the resource type the option entries are referring to.
   * @return The resource type as file extension string.
   */
  public String getResourceType()
  {
    return resourceType;
  }

  /**
   * Returns the option of the specified name as an option-specific Object type.
   * @param name The name of the option.
   * @return The option object associated with the specified name, or {@code null} if not found.
   */
  public Object getOption(String name)
  {
    if (name != null) {
      return mapOptions.get(name);
    }
    return null;
  }

  /**
   * Adds the specified name/value pair into the list. If {@code value} is {@code null}, an
   * existing entry of the specified name will be removed.
   * @param name The name of the option to add.
   * @param value The value of the option to add. Special: If {@code value} is {@code null},
   *        an existing option of the same name will be removed.
   * @return The previous value of the option, or {@code null} if no previous option of the
   *         specified name exists.
   */
  public Object setOption(String name, Object value)
  {
    if (name != null) {
      if (value != null) {
        return mapOptions.put(name, value);
      } else {
        return mapOptions.remove(name);
      }
    }
    return null;
  }

  /**
   * Returns a list of available option keys in the options list.
   * @return A list of available option keys.
   */
  public String[] getOptionKeys()
  {
    String[] retVal = new String[mapOptions.keySet().size()];
    Iterator<String> iter = mapOptions.keySet().iterator();
    int idx = 0;
    while (iter.hasNext()) {
      retVal[idx++] = iter.next();
    }
    return retVal;
  }


//-------------------------- INNER CLASSES --------------------------

  public static final class Utils
  {
    // Returns whether ref and value are equal, optionally taking case-sensitivity into account.
    public static boolean matchResourceRef(StructEntry ref, Object value, boolean caseSensitive)
    {
      if (ref != null && value != null ) {
        String s1, s2;
        if (ref instanceof ResourceRef && value instanceof String) {
          s1 = (String)value;
          s2 = ((ResourceRef)ref).getResourceName();
        } else if (ref instanceof IwdRef) {
          if (value instanceof Integer) {
            s1 = ((IwdRef)ref).getValueRef((Integer)value);
          } else if (value instanceof Long) {
            s1 = ((IwdRef)ref).getValueRef((Long)value);
          } else if (value instanceof String) {
            s1 = (String)value;
          } else {
            return false;
          }
          s2 = ((IwdRef)ref).getValueRef();
        } else {
          return false;
        }

        // special case: "NONE"
        if ((s1.isEmpty() || "NONE".equalsIgnoreCase(s1)) &&
            (s2.isEmpty() || "NONE".equalsIgnoreCase(s2) || ResourceFactory.getResourceEntry(s2) == null)) {
          return true;
        }

        if (caseSensitive) {
          return s1.equals(s2);
        } else {
          return s1.equalsIgnoreCase(s2);
        }
      }
      return (value == null);
    }

    // Returns whether entry contains (or equals) value, optionally taking case-sensitivity into account.
    public static boolean matchString(StructEntry entry, Object value, boolean exact, boolean caseSensitive)
    {
      if (entry != null && value != null && value instanceof String) {
        // preparing source
        String s1 = (String)value;
        if (s1.isEmpty()) {
          return false;
        }

        // preparing target
        String s2;
        if (entry instanceof IsTextual) {
          s2 = ((IsTextual)entry).getText();
        } else if (entry instanceof Unknown) {
          ByteBuffer buf = ((Unknown)entry).getData();
          s2 = StreamUtils.readString(buf, 0, buf.limit());
        } else {
          return false;
        }

        // comparing strings
        if (s2 != null) {
          if (exact) {
            if (caseSensitive) {
              return s1.equals(s2);
            } else {
              return s1.equalsIgnoreCase(s2);
            }
          } else {
            if (caseSensitive) {
              return s2.contains(s1);
            } else {
              return s2.toLowerCase(Locale.ENGLISH).contains(s1.toLowerCase(Locale.ENGLISH));
            }
          }
        }
      }
      return (value == null);
    }

    // Returns whether all bits match (exact=true) or only the set bits match (exact=false)
    public static boolean matchFlags(StructEntry flag, Object value)
    {
      if (flag != null && flag instanceof Flag && value != null) {
        boolean retVal = true;
        int v;
        boolean isExact;

        if (value instanceof Couple<?, ?> && ((Couple<?, ?>)value).getValue0() instanceof Integer &&
            ((Couple<?, ?>)value).getValue1() instanceof Boolean) {
          v = (Integer)((Couple<?, ?>)value).getValue0();
          isExact = (Boolean)((Couple<?, ?>)value).getValue1();
        } else if (value instanceof Integer) {
          v = (Integer)value;
          isExact = false;
        } else {
          return false;
        }

        for (int mask = 1, bit = 0; bit < (flag.getSize() << 3); bit++, mask <<= 1) {
          if (isExact) {
            if (((v & mask) != 0) != ((Flag)flag).isFlagSet(bit)) {
              retVal = false;
              break;
            }
          } else {
            if (((v & mask) != 0) && !((Flag)flag).isFlagSet(bit)) {
              retVal = false;
              break;
            }
          }
        }
        return retVal;
      }
      return (value == null);
    }

    // Returns whether number and value are equal
    public static boolean matchNumber(StructEntry number, Object value)
    {
      if (number != null && value != null) {
        // preparations
        int n1, n2, n3;
        if (value instanceof Integer) {
          n1 = n2 = (Integer)value;
        } else if (value instanceof Couple<?, ?> &&
                   ((Couple<?, ?>)value).getValue0() instanceof Integer &&
                   ((Couple<?, ?>)value).getValue1() instanceof Integer) {
          n1 = (Integer)((Couple<?, ?>)value).getValue0();
          n2 = (Integer)((Couple<?, ?>)value).getValue1();
          if (n1 > n2) { int tmp = n1; n1 = n2; n2 = tmp; }
        } else {
          return false;
        }

        // supported number-related datatypes
        if (number instanceof IsNumeric) {
          n3 = ((IsNumeric)number).getValue();
        } else {
          return false;
        }

        return (n3 >= n1 && n3 <= n2);
      }
      return (value == null);
    }


    public static boolean matchCustomFilter(AbstractStruct struct, Object match)
    {
      if (struct != null && match != null && match instanceof Couple<?, ?> &&
          ((Couple<?, ?>)match).getValue0() instanceof String) {
        String fieldName = (String)((Couple<?, ?>)match).getValue0();
        Object value = ((Couple<?, ?>)match).getValue1();
        if (!fieldName.isEmpty() && value != null) {
          boolean bRet = false;
          final List<StructEntry> structList = struct.getFlatFields();
          if (structList != null && !structList.isEmpty()) {
            for (int i = 0; i < structList.size(); i++) {
              StructEntry entry = structList.get(i);
              if (entry != null) {
                String name = entry.getName();
                if (name.toUpperCase(Locale.ENGLISH).contains(fieldName.toUpperCase(Locale.ENGLISH))) {
                  // field name matches
                  if (value instanceof String) {
                    bRet |= matchResourceRef(entry, value, false) || matchString(entry, value, false, false);
                  } else if (value instanceof Couple<?, ?>) {
                    if (((Couple<?, ?>)value).getValue0() instanceof Integer) {
                      if (((Couple<?, ?>)value).getValue1() instanceof Integer) {
                        bRet |= matchNumber(entry, value);
                      } else if (((Couple<?, ?>)value).getValue1() instanceof Boolean) {
                        bRet |= matchFlags(entry, value);
                      }
                    }
                  }
                }
              }
            }
            return bRet;
          } else {
            return false;
          }
        }
      }
      return (match == null);
    }
  }
}
