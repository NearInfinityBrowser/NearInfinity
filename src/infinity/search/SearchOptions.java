package infinity.search;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.HashBitmap;
import infinity.datatype.IDSTargetEffect;
import infinity.datatype.IdsBitmap;
import infinity.datatype.IwdRef;
import infinity.datatype.Kit2daBitmap;
import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.datatype.Song2daBitmap;
import infinity.datatype.StringRef;
import infinity.datatype.TextBitmap;
import infinity.datatype.TextEdit;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.util.DynamicArray;
import infinity.util.Pair;
import infinity.util.StringResource;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Stores a list of search options specified in SearchResource (Extended search) for use in the
 * resource-specific search methods.
 * @author argent77
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
  public static final String ARE_Actor                = "ARE.Actor %1$d.0";        // marks substructure
  public static final String ARE_Actor_Character      = "ARE.Actor.Character.0";
  public static final String ARE_Animation            = "ARE.Animation %1$d.0";    // marks substructure
  public static final String ARE_Animation_Animation  = "ARE.Animation.Animation.0";
  public static final String ARE_Container            = "ARE.Container %1$d.0";    // marks substructure
  public static final String ARE_Container_Item       = "ARE.Container.Item %1$d.0";  // marks substructure
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
  public static final String CRE_Effect       = "CRE.Effect %1$d.0";    // marks substructure
  public static final String CRE_Effect_Type1 = "CRE.Effect.Type.0";
  public static final String CRE_Effect_Type2 = "CRE.Effect.Type.1";
  public static final String CRE_Effect_Type3 = "CRE.Effect.Type.2";
  public static final String CRE_Effect_Type4 = "CRE.Effect.Type.3";
  public static final String CRE_Item         = "CRE.Item %1$d.0";    // marks substructure
  public static final String CRE_Item_Item1   = "CRE.Item.Item.0";
  public static final String CRE_Item_Item2   = "CRE.Item.Item.1";
  public static final String CRE_Item_Item3   = "CRE.Item.Item.2";
  public static final String CRE_Item_Item4   = "CRE.Item.Item.3";
  public static final String CRE_Spell         = "CRE.Known spell %1$d.0";  // marks substructure
  public static final String CRE_Spell_Spell1  = "CRE.Known spell.Spell.0";
  public static final String CRE_Spell_Spell2  = "CRE.Known spell.Spell.1";
  public static final String CRE_Spell_Spell3  = "CRE.Known spell.Spell.2";
  public static final String CRE_Spell_Spell4  = "CRE.Known spell.Spell.3";
  public static final String CRE_IWD2SpellBard                  = "CRE.Bard spells %1$d.0";     // marks substructure
  public static final String CRE_IWD2SpellBard_Spell            = "CRE.Bard spells.Spell.0";    // marks substructure
  public static final String CRE_IWD2SpellBard_Spell_ResRef     = "CRE.Bard spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellCleric                = "CRE.Cleric spells %1$d.0";   // marks substructure
  public static final String CRE_IWD2SpellCleric_Spell          = "CRE.Cleric spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellCleric_Spell_ResRef   = "CRE.Cleric spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellDruid                 = "CRE.Druid spells %1$d.0";   // marks substructure
  public static final String CRE_IWD2SpellDruid_Spell           = "CRE.Druid spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellDruid_Spell_ResRef    = "CRE.Druid spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellPaladin               = "CRE.Paladin spells %1$d.0";   // marks substructure
  public static final String CRE_IWD2SpellPaladin_Spell         = "CRE.Paladin spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellPaladin_Spell_ResRef  = "CRE.Paladin spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellRanger                = "CRE.Ranger spells %1$d.0";   // marks substructure
  public static final String CRE_IWD2SpellRanger_Spell          = "CRE.Ranger spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellRanger_Spell_ResRef   = "CRE.Ranger spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellSorcerer              = "CRE.Sorcerer spells %1$d.0";   // marks substructure
  public static final String CRE_IWD2SpellSorcerer_Spell        = "CRE.Sorcerer spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellSorcerer_Spell_ResRef = "CRE.Sorcerer spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellWizard                = "CRE.Wizard spells %1$d.0";   // marks substructure
  public static final String CRE_IWD2SpellWizard_Spell          = "CRE.Wizard spells.Spell.0";  // marks substructure
  public static final String CRE_IWD2SpellWizard_Spell_ResRef   = "CRE.Wizard spells.Spell.ResRef.0";
  public static final String CRE_IWD2SpellDomain                = "CRE.Domain spells %1$d.0";   // marks substructure
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
  public static final String ITM_Ability              = "ITM.Item Ability %1$d.0";   // marks substructure
  public static final String ITM_Ability_MatchSingle  = "ITM.Item Ability.MatchSingle.0";  // special: indicates whether to limit matches to a single ability
  public static final String ITM_Ability_Type         = "ITM.Item Ability.Type.0";
  public static final String ITM_Ability_Target       = "ITM.Item Ability.Target.0";
  public static final String ITM_Ability_Launcher     = "ITM.Item Ability.Launcher required.0";
  public static final String ITM_Ability_DamageType   = "ITM.Item Ability.Damage type.0";
  public static final String ITM_Ability_Projectile   = "ITM.Item Ability.Projectile.0";
  public static final String ITM_Ability_Range        = "ITM.Item Ability.Range (feet).0";
  public static final String ITM_Ability_Speed        = "ITM.Item Ability.Speed.0";
  public static final String ITM_Ability_DiceCount    = "ITM.Item Ability.# dice thrown.0";
  public static final String ITM_Ability_DiceSize     = "ITM.Item Ability.Dice size.0";
  public static final String ITM_Ability_Charges      = "ITM.Item Ability.# charges.0";
  public static final String ITM_Ability_Effect       = "ITM.Item Ability.Effect %1$d.0";   // marks substructure
  public static final String ITM_Ability_Effect_Type1 = "ITM.Item Ability.Effect.Type.0";
  public static final String ITM_Ability_Effect_Type2 = "ITM.Item Ability.Effect.Type.1";
  public static final String ITM_Ability_Effect_Type3 = "ITM.Item Ability.Effect.Type.2";
  public static final String ITM_Ability_Flags        = "ITM.Item Ability.Flags.0";
  public static final String ITM_Effect               = "ITM.Effect %1$d.0";   // marks substructure
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
  public static final String SPL_Ability              = "SPL.Spell ability %1$d.0";    // marks substructure
  public static final String SPL_Ability_MatchSingle  = "SPL.Spell ability.MatchSingle.0";  // special: indicates whether to limit matches to a single ability
  public static final String SPL_Ability_Type         = "SPL.Spell ability.Type.0";
  public static final String SPL_Ability_Location     = "SPL.Spell ability.Ability location.0";
  public static final String SPL_Ability_Target       = "SPL.Spell ability.Target.0";
  public static final String SPL_Ability_Range        = "SPL.Spell ability.Range (feet).0";
  public static final String SPL_Ability_Level        = "SPL.Spell ability.Minimum level.0";
  public static final String SPL_Ability_Speed        = "SPL.Spell ability.Casting speed.0";
  public static final String SPL_Ability_Projectile   = "SPL.Spell ability.Projectile.0";
  public static final String SPL_Ability_Effect       = "SPL.Spell ability.Effect %1$d.0";   // marks substructure
  public static final String SPL_Ability_Effect_Type1 = "SPL.Spell ability.Effect.Type.0";
  public static final String SPL_Ability_Effect_Type2 = "SPL.Spell ability.Effect.Type.1";
  public static final String SPL_Ability_Effect_Type3 = "SPL.Spell ability.Effect.Type.2";
  public static final String SPL_Effect               = "SPL.Effect %1$d.0";     // marks substructure
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
  public static final String STO_Purchased      = "STO.Store purchases %1$d.0";   // marks addremovable field
  public static final String STO_Purchased1     = "STO.Store purchases.0";
  public static final String STO_Purchased2     = "STO.Store purchases.1";
  public static final String STO_Purchased3     = "STO.Store purchases.2";
  public static final String STO_Purchased4     = "STO.Store purchases.3";
  public static final String STO_Purchased5     = "STO.Store purchases.4";
  public static final String STO_Item           = "STO.Item for sale %1$d.0";  // marks substructure
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


  private final HashMap<String, Object> mapOptions = new HashMap<String, Object>();
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
   * @param name The option key to extract the resource index from.
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
   * @return <code>true</code> if the specified option key defines a resource field by offset.
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
   * @return The option object associated with the specified name, or <code>null</code> if not found.
   */
  public Object getOption(String name)
  {
    if (name != null) {
      return mapOptions.get(name);
    }
    return null;
  }

  /**
   * Adds the specified name/value pair into the list. If <code>value</code> is <code>null</code>, an
   * existing entry of the specified name will be removed.
   * @param name The name of the option to add.
   * @param value The value of the option to add. Special: If <code>value</code> is <code>null</code>,
   *        an existing option of the same name will be removed.
   * @return The previous value of the option, or <code>null</code> if no previous option of the
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
            (s2.isEmpty() || "NONE".equalsIgnoreCase(s2) || ResourceFactory.getInstance().getResourceEntry(s2) == null)) {
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
        if (entry instanceof StringRef) {
          s2 = StringResource.getStringRef(((StringRef)entry).getValue());
        } else if (entry instanceof Unknown) {
          byte[] buf = ((Unknown)entry).getData();
          s2 = DynamicArray.getString(buf, 0, buf.length);
        } else if (entry instanceof ResourceRef) {
          s2 = ((ResourceRef)entry).getResourceName();
          s2 = s2.substring(0, s2.indexOf('.'));
        } else if (entry instanceof TextBitmap) {
          s2 = ((TextBitmap)entry).getIdsValue();
        } else if (entry instanceof TextEdit) {
          s2 = ((TextEdit)entry).toString();
        } else if (entry instanceof TextString) {
          s2 = ((TextString)entry).toString();
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
              return s2.toLowerCase().contains(s1.toLowerCase());
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

        if (value instanceof Pair && ((Pair<Object>)value).getFirst() instanceof Integer &&
            ((Pair<Object>)value).getSecond() instanceof Boolean) {
          v = (Integer)((Pair<Object>)value).getFirst();
          isExact = (Boolean)((Pair<Object>)value).getSecond();
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
        } else if (value instanceof Pair &&
                   ((Pair<Integer>)value).getFirst() instanceof Integer &&
                   ((Pair<Integer>)value).getSecond() instanceof Integer) {
          n1 = ((Pair<Integer>)value).getFirst();
          n2 = ((Pair<Integer>)value).getSecond();
          if (n1 > n2) { int tmp = n1; n1 = n2; n2 = tmp; }
        } else {
          return false;
        }

        // supported number-related datatypes
        if (number instanceof DecNumber) {
          n3 = ((DecNumber)number).getValue();
        } else if (number instanceof UnsignDecNumber) {
          n3 = (int)((UnsignDecNumber)number).getValue();
        } else if (number instanceof Bitmap) {
          n3 = ((Bitmap)number).getValue();
        } else if (number instanceof HashBitmap) {
          n3 = (int)((HashBitmap)number).getValue();
        } else if (number instanceof IdsBitmap) {
          n3 = (int)((IdsBitmap)number).getValue();
        } else if (number instanceof IDSTargetEffect) {
          n3 = (int)((IDSTargetEffect)number).getValue();
        } else if (number instanceof IwdRef) {
          n3 = (int)((IwdRef)number).getValue();
        } else if (number instanceof Kit2daBitmap) {
          n3 = (int)((Kit2daBitmap)number).getValue();
        } else if (number instanceof ProRef) {
          n3 = (int)((ProRef)number).getValue();
        } else if (number instanceof Song2daBitmap) {
          n3 = (int)((Song2daBitmap)number).getValue();
        } else {
          return false;
        }

        return (n3 >= n1 && n3 <= n2);
      }
      return (value == null);
    }


    public static boolean matchCustomFilter(AbstractStruct struct, Object match)
    {
      if (struct != null && match != null && match instanceof Pair &&
          ((Pair<Object>)match).getFirst() instanceof String) {
        String fieldName = (String)((Pair<Object>)match).getFirst();
        Object value = ((Pair<Object>)match).getSecond();
        if (!fieldName.isEmpty() && value != null) {
          boolean bRet = false;
          List<StructEntry> structList = struct.getFlatList();
          if (structList != null && !structList.isEmpty()) {
            for (int i = 0; i < structList.size(); i++) {
              StructEntry entry = structList.get(i);
              if (entry != null) {
                String name = entry.getName();
                if (name.toUpperCase().contains(fieldName.toUpperCase())) {
                  // field name matches
                  if (value instanceof String) {
                    bRet |= matchResourceRef(entry, value, false) || matchString(entry, value, false, false);
                  } else if (value instanceof Pair) {
                    if (((Pair<Object>)value).getFirst() instanceof Integer) {
                      if (((Pair<Object>)value).getSecond() instanceof Integer) {
                        bRet |= matchNumber(entry, value);
                      } else if (((Pair<Object>)value).getSecond() instanceof Boolean) {
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
