// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.spl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.ListIterator;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

import org.infinity.datatype.AbstractBitmap;
import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.PriTypeBitmap;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SecTypeBitmap;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.StringRef;
import org.infinity.datatype.TextString;
import org.infinity.datatype.Unknown;
import org.infinity.datatype.UpdateEvent;
import org.infinity.datatype.UpdateListener;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Effect;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.itm.ItmResource;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.SearchOptions;
import org.infinity.util.StringTable;
import org.infinity.util.io.StreamUtils;

/**
 * This resource describes a "spell". Spells include mage spells, priest spells,
 * innate abilities, special abilities and effects used for game advancement
 * (e.g. animation effects, custom spells). SPL files have a similar structure
 * to {@link ItmResource ITM} files.
 * <p>
 * SPL files consist of a main header, zero or more extended headers (each
 * containing zero or more feature blocks) and zero or more casting feature blocks.
 * All the feature blocks are stored as a continuous data segment, with each extended
 * header containing an offset into this data, and the main header containing an
 * offset into this data for the casting feature blocks.
 *
 * @see <a href="https://gibberlings3.github.io/iesdp/file_formats/ie_formats/spl_v1.htm">
 * https://gibberlings3.github.io/iesdp/file_formats/ie_formats/spl_v1.htm</a>
 */
public final class SplResource extends AbstractStruct implements Resource, HasChildStructs, HasViewerTabs,
                                                                 UpdateListener
{
  // SPL-specific field labels
  public static final String SPL_NAME                             = "Spell name";
  public static final String SPL_NAME_IDENTIFIED                  = org.infinity.resource.itm.ItmResource.ITM_NAME_IDENTIFIED + SUFFIX_UNUSED;
  public static final String SPL_CASTING_SOUND                    = "Casting sound";
  public static final String SPL_FLAGS                            = "Flags";
  public static final String SPL_TYPE                             = "Spell type";
  public static final String SPL_EXCLUSION_FLAGS                  = "Exclusion flags";
  public static final String SPL_CASTING_ANIMATION                = "Casting animation";
  public static final String SPL_MINIMUM_LEVEL                    = "Minimum level (unused)";
  public static final String SPL_PRIMARY_TYPE                     = "Primary type (school)";
  public static final String SPL_SECONDARY_TYPE                   = "Secondary type";
  public static final String SPL_LEVEL                            = "Spell level";
  public static final String SPL_ICON                             = "Spell icon";
  public static final String SPL_ICON_GROUND                      = "Ground icon";
  public static final String SPL_DESCRIPTION                      = "Spell description";
  public static final String SPL_DESCRIPTION_IDENTIFIED           = org.infinity.resource.itm.ItmResource.ITM_DESCRIPTION_IDENTIFIED + SUFFIX_UNUSED;
  public static final String SPL_DESCRIPTION_IMAGE                = org.infinity.resource.itm.ItmResource.ITM_DESCRIPTION_IMAGE;
  public static final String SPL_OFFSET_ABILITIES                 = org.infinity.resource.itm.ItmResource.ITM_OFFSET_ABILITIES;
  public static final String SPL_NUM_ABILITIES                    = org.infinity.resource.itm.ItmResource.ITM_NUM_ABILITIES;
  public static final String SPL_OFFSET_EFFECTS                   = org.infinity.resource.itm.ItmResource.ITM_OFFSET_EFFECTS;
  public static final String SPL_FIRST_EFFECT_INDEX               = org.infinity.resource.itm.ItmResource.ITM_FIRST_EFFECT_INDEX;
  public static final String SPL_NUM_GLOBAL_EFFECTS               = org.infinity.resource.itm.ItmResource.ITM_NUM_GLOBAL_EFFECTS;
  public static final String SPL_SPELL_DURATION_ROUNDS_PER_LEVEL  = "Spell duration rounds/level";
  public static final String SPL_SPELL_DURATION_BASE              = "Spell duration rounds base";

  public static final String[] s_spelltype = {"Special", "Wizard", "Priest", "Psionic", "Innate", "Bard song"};
  public static final String[] s_anim = {"None", "Fire aqua", "Fire blue", "Fire gold", "Fire green",
                                         "Fire magenta", "Fire purple", "Fire red", "Fire white",
                                         "Necromancy", "Alteration", "Enchantment", "Abjuration",
                                         "Illusion", "Conjuration", "Invocation", "Divination",
                                         "Fountain aqua", "Fountain black", "Fountain blue", "Fountain gold",
                                         "Fountain green", "Fountain magenta", "Fountain purple",
                                         "Fountain red", "Fountain white", "Swirl aqua", "Swirl black",
                                         "Swirl blue", "Swirl gold", "Swirl green",
                                         "Swirl magenta", "Swirl purple", "Swirl red",
                                         "Swirl white"};
  public static final String[] s_anim_pst = {"None", "", "", "", "", "", "", "", "", "", "", "",
                                             "", "", "", "", "", "", "", "", "", "", "", "", "",
                                             "", "", "", "", "", "", "", "", "", "",
                                             "Abjuration", "Alteration", "Conjuration", "Enchantment",
                                             "Divination", "Illusion", "Invocation", "Necromancy", "Innate"};

  public static final String[] s_spellflag = {"No flags set", "", "", "", "", "", "", "", "",
                                              "", "EE: Break Sanctuary/Invisibility", "Hostile", "No LOS required",
                                              "Allow spotting", "Outdoors only", "Ignore dead/wild magic",
                                              "Ignore wild surge", "Non-combat ability", "", "", "", "", "",
                                              "", "", "EE/Ex: Can target invisible", "EE/Ex: Castable when silenced"};
  public static final String[] s_spellflag2 = {"No flags set", "", "", "", "", "", "", "", "",
                                               "", "", "Hostile", "No LOS required",
                                               "Allow spotting", "Outdoors only", "Simplified duration",
                                               "Trigger/Contingency", "", "", "Non-combat ability (?)", "", "", "",
                                               "", "", "", ""};
  public static final String[] s_exclude =
    { "None",
      "Berserker", "Wizard slayer", "Kensai", "Cavalier", "Inquisitor", "Undead hunter",
      "Abjurer", "Conjurer", "Diviner", "Enchanter", "Illusionist", "Invoker", "Necromancer",
      "Transmuter", "Generalist;Includes trueclass mages, sorcerers and bards",
      "Archer", "Stalker", "Beastmaster", "Assasin", "Bounty hunter", "Swashbuckler", "Blade",
      "Jester", "Skald", "Cleric of Talos", "Cleric of Helm", "Cleric of Lathander",
      "Totemic druid", "Shapeshifter", "Avenger", "Barbarian", "Wild mage"};
  public static final String[] s_exclude_priest =
    { "None",
      "Chaotic;Includes Chaotic Good, Chaotic Neutral and Chaotic Evil",
      "Evil;Includes Lawful Evil, Neutral Evil and Chaotic Evil",
      "Good;Includes Lawful Good, Neutral Good and Chaotic Good",
      "... Neutral;Includes Lawful Neutral, True Neutral and Chaotic Neutral",
      "Lawful;Includes Lawful Good, Lawful Neutral and Lawful Evil",
      "Neutral ...;Includes Neutral Good, True Neutral and Neutral Evil",
      "Unused", "Unused", "Unused", "Unused", "Unused", "Unused", "Unused", "Unused",
      "Unused", "Unused", "Unused", "Unused", "Unused", "Unused", "Unused", "Unused",
      "Unused", "Unused", "Unused", "Unused", "Unused", "Unused", "Unused", "Unused",
      "Cleric/Paladin", "Druid/Ranger/Shaman"};
  public static final String[] s_exclude_combined =
    { "None",
      "Chaotic/Berserker", "Evil/Wizard slayer", "Good/Kensai", "... Neutral/Cavalier",
      "Lawful/Inquisitor", "Neutral .../Undead hunter", "Abjurer", "Conjurer", "Diviner",
      "Enchanter", "Illusionist", "Invoker", "Necromancer", "Transmuter",  "Generalist",
      "Archer", "Stalker", "Beastmaster", "Assasin", "Bounty hunter", "Swashbuckler", "Blade",
      "Jester", "Skald", "Cleric of Talos", "Cleric of Helm", "Cleric of Lathander", "Totemic druid",
      "Shapeshifter", "Avenger", "Cleric/Paladin/Barbarian", "Druid/Ranger/Wild mage"};

  private StructHexViewer hexViewer;

  public static String getSearchString(InputStream is) throws IOException
  {
    is.skip(8);
    return StringTable.getStringRef(StreamUtils.readInt(is)).trim();
  }

  public SplResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    return new AddRemovable[]{new Ability(), new Effect()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public int getViewerTabCount()
  {
    return 2;
  }

  @Override
  public String getViewerTabName(int index)
  {
    switch (index) {
      case 0:
        return StructViewer.TAB_VIEW;
      case 1:
        return StructViewer.TAB_RAW;
    }
    return null;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    switch (index) {
      case 0:
      {
        JScrollPane scroll = new JScrollPane(new Viewer(this));
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
      }
      case 1:
      {
        if (hexViewer == null) {
          hexViewer = new StructHexViewer(this, new BasicColorMap(this, true));
        }
        return hexViewer;
      }
    }
    return null;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return (index == 0);
  }

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.write(os);
    for (final StructEntry o : getFields()) {
      if (o instanceof Ability) {
        Ability a = (Ability)o;
        a.writeEffects(os);
      }
    }
  }

  @Override
  public boolean valueUpdated(UpdateEvent event)
  {
    if (event.getSource() instanceof AbstractBitmap<?> &&
        SPL_TYPE.equals(((AbstractBitmap<?>)event.getSource()).getName())) {
      Flag curFlags = (Flag)getAttribute(SPL_EXCLUSION_FLAGS);
      if (curFlags != null) {
        int type = ((IsNumeric)event.getSource()).getValue();
        int size = curFlags.getSize();
        int offset = curFlags.getOffset();
        ByteBuffer b = ByteBuffer.allocate(size).order(ByteOrder.LITTLE_ENDIAN).putInt(curFlags.getValue());
        Flag newFlags = new Flag(b, 0, size, SPL_EXCLUSION_FLAGS, (type == 2) ? s_exclude_priest : s_exclude);
        newFlags.setOffset(offset);
        replaceField(newFlags);
        return true;
      }
    }
    return false;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);
  }

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    if (datatype instanceof Effect) {
      for (final StructEntry o : getFields()) {
        if (o instanceof Ability)
          ((Ability)o).incEffectsIndex(1);
      }
    }
    else if (datatype instanceof Ability) {
      int effect_count = ((IsNumeric)getAttribute(SPL_NUM_GLOBAL_EFFECTS)).getValue();
      for (final StructEntry o : getFields()) {
        if (o instanceof Ability) {
          Ability ability = (Ability)o;
          ability.setEffectsIndex(effect_count);
          effect_count += ability.getEffectsCount();
        }
      }
    }
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    incAbilityEffects(child, datatype, 1);
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (datatype instanceof Effect) {
      for (final StructEntry o : getFields()) {
        if (o instanceof Ability)
          ((Ability)o).incEffectsIndex(-1);
      }
    }
    else if (datatype instanceof Ability) {
      int effect_count = ((IsNumeric)getAttribute(SPL_NUM_GLOBAL_EFFECTS)).getValue();
      for (final StructEntry o : getFields()) {
        if (o instanceof Ability) {
          Ability ability = (Ability)o;
          ability.setEffectsIndex(effect_count);
          effect_count += ability.getEffectsCount();
        }
      }
    }
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    incAbilityEffects(child, datatype, -1);
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    addField(new TextString(buffer, offset, 4, COMMON_SIGNATURE));
    TextString version = new TextString(buffer, offset + 4, 4, COMMON_VERSION);
    addField(version);
    addField(new StringRef(buffer, offset + 8, SPL_NAME));
    addField(new StringRef(buffer, offset + 12, SPL_NAME_IDENTIFIED));
    addField(new ResourceRef(buffer, offset + 16, SPL_CASTING_SOUND, "WAV"));
    if (version.getText().equalsIgnoreCase("V2.0")) {
      addField(new Flag(buffer, offset + 24, 4, SPL_FLAGS, s_spellflag2));
    } else {
      addField(new Flag(buffer, offset + 24, 4, SPL_FLAGS, s_spellflag));
    }
    Bitmap spellType = new Bitmap(buffer, offset + 28, 2, SPL_TYPE, s_spelltype);   // 0x1c
    spellType.addUpdateListener(this);
    addField(spellType);
    addField(new Flag(buffer, offset + 30, 4, SPL_EXCLUSION_FLAGS,
                      (spellType.getValue() == 2) ? s_exclude_priest : s_exclude));   // 0x1e
    if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
      addField(new Bitmap(buffer, offset + 34, 2, SPL_CASTING_ANIMATION, s_anim_pst));  // 0x22
    } else {
      addField(new Bitmap(buffer, offset + 34, 2, SPL_CASTING_ANIMATION, s_anim));  // 0x22
    }
    addField(new Unknown(buffer, offset + 36, 1, COMMON_UNUSED));               // 0x24
    addField(new PriTypeBitmap(buffer, offset + 37, 1, SPL_PRIMARY_TYPE)); // 0x25
    addField(new Unknown(buffer, offset + 38, 1, COMMON_UNUSED));
    addField(new SecTypeBitmap(buffer, offset + 39, 1, SPL_SECONDARY_TYPE));       // 0x27
    addField(new Unknown(buffer, offset + 40, 12, COMMON_UNUSED));
    addField(new DecNumber(buffer, offset + 52, 4, SPL_LEVEL));
    addField(new Unknown(buffer, offset + 56, 2, COMMON_UNUSED));
    addField(new ResourceRef(buffer, offset + 58, SPL_ICON, "BAM"));
    addField(new Unknown(buffer, offset + 66, 2, COMMON_UNUSED));
    addField(new Unknown(buffer, offset + 68, 8, COMMON_UNUSED));
    addField(new Unknown(buffer, offset + 76, 4, COMMON_UNUSED));
    addField(new StringRef(buffer, offset + 80, SPL_DESCRIPTION));
    addField(new StringRef(buffer, offset + 84, SPL_DESCRIPTION_IDENTIFIED));
    addField(new ResourceRef(buffer, offset + 88, SPL_DESCRIPTION_IMAGE, "BAM"));
    addField(new Unknown(buffer, offset + 96, 4, COMMON_UNUSED));
    SectionOffset abil_offset = new SectionOffset(buffer, offset + 100, SPL_OFFSET_ABILITIES,
                                                  Ability.class);
    addField(abil_offset);
    SectionCount abil_count = new SectionCount(buffer, offset + 104, 2, SPL_NUM_ABILITIES,
                                               Ability.class);
    addField(abil_count);
    SectionOffset global_offset = new SectionOffset(buffer, offset + 106, SPL_OFFSET_EFFECTS,
                                                    Effect.class);
    addField(global_offset);
    addField(new DecNumber(buffer, offset + 110, 2, SPL_FIRST_EFFECT_INDEX));
    SectionCount global_count = new SectionCount(buffer, offset + 112, 2, SPL_NUM_GLOBAL_EFFECTS,
                                                 Effect.class);
    addField(global_count);

    if (version.toString().equalsIgnoreCase("V2.0")) {
      addField(new DecNumber(buffer, offset + 114, 1, SPL_SPELL_DURATION_ROUNDS_PER_LEVEL));
      addField(new DecNumber(buffer, offset + 115, 1, SPL_SPELL_DURATION_BASE));
      addField(new Unknown(buffer, offset + 116, 14));
    }

    offset = abil_offset.getValue();
    Ability abilities[] = new Ability[abil_count.getValue()];
    for (int i = 0; i < abilities.length; i++) {
      abilities[i] = new Ability(this, buffer, offset, i);
      addField(abilities[i]);
      offset = abilities[i].getEndOffset();
    }

    int offset2 = global_offset.getValue();
    for (int i = 0; i < global_count.getValue(); i++) {
      Effect eff = new Effect(this, buffer, offset2, i);
      offset2 = eff.getEndOffset();
      addField(eff);
    }

    for (final Ability ability : abilities)
      offset2 = ability.readEffects(buffer, offset2);

    return Math.max(offset, offset2);
  }

  private void incAbilityEffects(StructEntry child, AddRemovable datatype, int value)
  {
    if (child instanceof Ability && datatype instanceof Effect) {
      final List<StructEntry> fields = getFields();
      final ListIterator<StructEntry> it = fields.listIterator(fields.indexOf(child) + 1);
      while (it.hasNext()) {
        final StructEntry se = it.next();
        if (se instanceof Ability) {
          ((Ability)se).incEffectsIndex(value);
        }
      }
    }
    if (hexViewer != null) {
      hexViewer.dataModified();
    }
  }

  /**
   * Checks whether the specified resource entry matches all available search options.
   * Called by "Extended Search"
   */
  public static boolean matchSearchOptions(ResourceEntry entry, SearchOptions searchOptions)
  {
    if (entry != null && searchOptions != null) {
      try {
        SplResource spl = new SplResource(entry);
        Ability[] abilities;
        Effect[][] abilityEffects;
        Effect[] effects;
        boolean retVal = true;
        String key;
        Object o;

        // preparing substructures
        IsNumeric ofs = (IsNumeric)spl.getAttribute(SPL_OFFSET_EFFECTS, false);
        IsNumeric cnt = (IsNumeric)spl.getAttribute(SPL_NUM_GLOBAL_EFFECTS, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          effects = new Effect[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.SPL_Effect), idx);
            effects[idx] = (Effect)spl.getAttribute(label, false);
          }
        } else {
          effects = new Effect[0];
        }

        ofs = (IsNumeric)spl.getAttribute(SPL_OFFSET_ABILITIES, false);
        cnt = (IsNumeric)spl.getAttribute(SPL_NUM_ABILITIES, false);
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          abilities = new Ability[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.SPL_Ability), idx);
            abilities[idx] = (Ability)spl.getAttribute(label, false);
          }
        } else {
          abilities = new Ability[0];
        }

        abilityEffects = new Effect[abilities.length][];
        for (int idx = 0; idx < abilities.length; idx++) {
          if (abilities[idx] != null) {
            cnt = (IsNumeric)abilities[idx].getAttribute(AbstractAbility.ABILITY_NUM_EFFECTS, false);
            if (cnt != null && cnt.getValue() > 0) {
              abilityEffects[idx] = new Effect[cnt.getValue()];
              for (int idx2 = 0; idx2 < cnt.getValue(); idx2++) {
                String label = String.format(SearchOptions.getResourceName(SearchOptions.SPL_Ability_Effect), idx2);
                abilityEffects[idx][idx2] = (Effect)abilities[idx].getAttribute(label, false);
              }
            } else {
              abilityEffects[idx] = new Effect[0];
            }
          } else {
            abilityEffects[idx] = new Effect[0];
          }
        }

        // checking options
        if (retVal) {
          key = SearchOptions.SPL_Name;
          o = searchOptions.getOption(key);
          StructEntry struct = spl.getAttribute(SearchOptions.getResourceName(key), false);
          retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
        }

        String[] keyList = new String[]{SearchOptions.SPL_SpellType, SearchOptions.SPL_CastingAnimation,
                                        SearchOptions.SPL_PrimaryType, SearchOptions.SPL_SecondaryType,
                                        SearchOptions.SPL_Level};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = spl.getAttribute(SearchOptions.getResourceName(key), false);
            retVal &= SearchOptions.Utils.matchNumber(struct, o);
          } else {
            break;
          }
        }

        keyList = new String[]{SearchOptions.SPL_Flags, SearchOptions.SPL_Exclusion};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = spl.getAttribute(SearchOptions.getResourceName(key), false);
            retVal &= SearchOptions.Utils.matchFlags(struct, o);
          } else {
            break;
          }
        }


        keyList = new String[]{SearchOptions.SPL_Effect_Type1, SearchOptions.SPL_Effect_Type2,
                               SearchOptions.SPL_Effect_Type3};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            boolean found = false;
            key = keyList[idx];
            o = searchOptions.getOption(key);
            for (int idx2 = 0; idx2 < effects.length; idx2++) {
              if (!found) {
                if (effects[idx2] != null) {
                  StructEntry struct = effects[idx2].getAttribute(SearchOptions.getResourceName(key), false);
                  found |= SearchOptions.Utils.matchNumber(struct, o);
                }
              } else {
                break;
              }
            }
            retVal &= found || (o == null);
          } else {
            break;
          }
        }

        SearchOptions abilityOption = (SearchOptions)searchOptions.getOption(SearchOptions.SPL_Ability);
        if (retVal && abilityOption != null) {
          // indicates whether any ability options have been selected
          boolean hasAbilityOptions = false;
          keyList = new String[]{SearchOptions.SPL_Ability_Type, SearchOptions.SPL_Ability_Location,
                                 SearchOptions.SPL_Ability_Target, SearchOptions.SPL_Ability_Range,
                                 SearchOptions.SPL_Ability_Level, SearchOptions.SPL_Ability_Speed,
                                 SearchOptions.SPL_Ability_Projectile,
                                 SearchOptions.SPL_Ability_Effect_Type1, SearchOptions.SPL_Ability_Effect_Type2,
                                 SearchOptions.SPL_Ability_Effect_Type3};
          for (int i = 0; i < keyList.length; i++) {
            hasAbilityOptions |= (abilityOption.getOption(keyList[i]) != null);
          }

          // tracks matches for each option in every available ability
          final int abilityOptions = keyList.length;    // number of supported spell ability options
          boolean[][] abilityMatches = new boolean[abilities.length][abilityOptions];
          for (int i = 0; i < abilities.length; i++) {
            for (int j = 0; j < abilityMatches[i].length; j++) {
              abilityMatches[i][j] = false;
            }
          }

          for (int i = 0; i < abilities.length; i++) {
            if (abilities[i] != null) {
              for (int j = 0; j < 7; j++) {
                key = keyList[j];
                o = abilityOption.getOption(key);
                StructEntry struct = abilities[i].getAttribute(SearchOptions.getResourceName(key), false);
                abilityMatches[i][j] = SearchOptions.Utils.matchNumber(struct, o);
              }

              for (int j = 7; j < keyList.length; j++) {
                key = keyList[j];
                o = abilityOption.getOption(key);
                for (int k = 0; k < abilityEffects[i].length; k++) {
                  if (abilityEffects[i][k] != null) {
                    StructEntry struct = abilityEffects[i][k].getAttribute(SearchOptions.getResourceName(key), false);
                    abilityMatches[i][j] |= SearchOptions.Utils.matchNumber(struct, o);
                  }
                }
              }
            }
          }

          // evaluating collected results
          boolean[] foundSingle = new boolean[abilityMatches.length];    // for single ability option
          for (int i = 0; i < foundSingle.length; i++)
          {
            foundSingle[i] = false;
          }
          boolean[] foundMulti = new boolean[abilityOptions];           // for multiple abilities option
          for (int i = 0; i < foundMulti.length; i++)
          {
            foundMulti[i] = (abilityOption.getOption(keyList[i]) == null);
          }

          for (int i = 0; i < abilityMatches.length; i++) {
            if (abilities[i] != null) {
              foundSingle[i] = true;
              for (int j = 0; j < abilityMatches[i].length; j++) {
                foundSingle[i] &= abilityMatches[i][j];
                foundMulti[j] |= abilityMatches[i][j];
              }
            }
          }

          boolean resultSingle = false;
          for (int i = 0; i < foundSingle.length; i++) { resultSingle |= foundSingle[i]; }
          resultSingle |= !hasAbilityOptions;

          boolean resultMulti = true;
          for (int i = 0; i < foundMulti.length; i++) { resultMulti &= foundMulti[i]; }
          resultMulti |= !hasAbilityOptions;

          Boolean isAbilitySingle;
          o = abilityOption.getOption(SearchOptions.SPL_Ability_MatchSingle);
          if (o != null && o instanceof Boolean) {
            isAbilitySingle = (Boolean)o;
          } else {
            isAbilitySingle = false;
          }

          if (isAbilitySingle) {
            retVal &= resultSingle;
          } else {
            retVal &= resultMulti;
          }
        }

        keyList = new String[]{SearchOptions.SPL_Custom1, SearchOptions.SPL_Custom2,
                               SearchOptions.SPL_Custom3, SearchOptions.SPL_Custom4};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            retVal &= SearchOptions.Utils.matchCustomFilter(spl, o);
          } else {
            break;
          }
        }

        return retVal;
      } catch (Exception e) {
      }
    }
    return false;
  }
}
