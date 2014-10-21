// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.spl;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.IdsBitmap;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.datatype.TextString;
import infinity.datatype.Unknown;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Effect;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.SearchOptions;

import java.io.IOException;
import java.io.OutputStream;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public final class SplResource extends AbstractStruct implements Resource, HasAddRemovable, HasViewerTabs
{
  public static final String[] s_category = {"None", "Spell protections", "Specific protections", "Illusionary protections",
                                             "Magic attack", "Divination attack", "Conjuration", "Combat protections",
                                             "Contingency", "Battleground", "Offensive damage", "Disabling", "Combination",
                                             "Non-combat"};
  public static final String[] s_school = {"None", "Abjurer", "Conjurer", "Diviner", "Enchanter", "Illusionist", "Invoker",
                                           "Necromancer", "Transmuter", "Generalist"};
//  private static final LongIntegerHashMap<String> m_wizardtype = new LongIntegerHashMap<String>();
//  private static final LongIntegerHashMap<String> m_priesttype = new LongIntegerHashMap<String>();
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
  public static final String[] s_spellflag = {"No flags set", "", "", "", "", "", "", "", "",
                                              "", "", "Hostile",
                                              "No LOS required", "Allow spotting", "Outdoors only",
                                              "Non-magical ability", "Trigger/Contingency",
                                              "Non-combat ability", "", "", "", "", "", "", "",
                                              "Ex: can target invisible", "Ex: castable when silenced"};
  public static final String[] s_exclude = { "None", "Chaotic", "Evil", "Good",
                                             "... Neutral", "Lawful", "Neutral ...",
                                             "Abjurer", "Conjurer", "Diviner", "Enchanter",
                                             "Illusionist", "Invoker", "Necromancer", "Transmuter",
                                             "Generalist", "", "", "", "", "", "", "", "", "Elf",
                                             "Dwarf", "Half-elf", "Halfling", "Human", "Gnome", "",
                                             "Cleric", "Druid"};

  static
  {
//    m_wizardtype.put((long)0x0000, "Schoolless");
//    m_wizardtype.put((long)0x0040, "Alteration");
//    m_wizardtype.put((long)0x0080, "Divination");
//    m_wizardtype.put((long)0x0200, "Invocation");
//    m_wizardtype.put((long)0x0800, "Enchantment");
//    m_wizardtype.put((long)0x0900, "Conjuration");
//    m_wizardtype.put((long)0x1000, "Illusion");
//    m_wizardtype.put((long)0x2000, "Abjuration");
//    m_wizardtype.put((long)0x2400, "Necromancy");

//    m_priesttype.put((long)0x0000, "All priests");
//    m_priesttype.put((long)0x4000, "Druid/Ranger");
//    m_priesttype.put((long)0x8000, "Cleric/Paladin");
  }

  public static String getSearchString(byte buffer[])
  {
    return new StringRef(buffer, 8, "").toString().trim();
  }

  public SplResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Ability(), new Effect()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_VIEW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    JScrollPane scroll = new JScrollPane(new Viewer(this));
    scroll.setBorder(BorderFactory.createEmptyBorder());
    return scroll;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------


// --------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    super.write(os);
    for (int i = 0; i < list.size(); i++) {
      Object o = list.get(i);
      if (o instanceof Ability) {
        Ability a = (Ability)o;
        a.writeEffects(os);
      }
    }
  }

// --------------------- End Interface Writeable ---------------------

  @Override
  protected void datatypeAdded(AddRemovable datatype)
  {
    if (datatype instanceof Effect) {
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability)
          ((Ability)o).incEffectsIndex(1);
      }
    }
    else if (datatype instanceof Ability) {
      int effect_count = ((SectionCount)getAttribute("# global effects")).getValue();
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability) {
          Ability ability = (Ability)o;
          ability.setEffectsIndex(effect_count);
          effect_count += ability.getEffectsCount();
        }
      }
    }
  }

  @Override
  protected void datatypeAddedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeAddedInChild(child, datatype);
    if (child instanceof Ability && datatype instanceof Effect) {
      int index = getIndexOf(child) + 1;
      while (index < getRowCount()) {
        StructEntry se = getStructEntryAt(index++);
        if (se instanceof Ability)
          ((Ability)se).incEffectsIndex(1);
      }
    }
  }

  @Override
  protected void datatypeRemoved(AddRemovable datatype)
  {
    if (datatype instanceof Effect) {
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability)
          ((Ability)o).incEffectsIndex(-1);
      }
    }
    else if (datatype instanceof Ability) {
      int effect_count = ((SectionCount)getAttribute("# global effects")).getValue();
      for (int i = 0; i < list.size(); i++) {
        Object o = list.get(i);
        if (o instanceof Ability) {
          Ability ability = (Ability)o;
          ability.setEffectsIndex(effect_count);
          effect_count += ability.getEffectsCount();
        }
      }
    }
  }

  @Override
  protected void datatypeRemovedInChild(AbstractStruct child, AddRemovable datatype)
  {
    super.datatypeRemovedInChild(child, datatype);
    if (child instanceof Ability && datatype instanceof Effect) {
      int index = getIndexOf(child) + 1;
      while (index < getRowCount()) {
        StructEntry se = getStructEntryAt(index++);
        if (se instanceof Ability)
          ((Ability)se).incEffectsIndex(-1);
      }
    }
  }

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    list.add(new TextString(buffer, offset, 4, "Signature"));
    TextString version = new TextString(buffer, offset + 4, 4, "Version");
    list.add(version);
    list.add(new StringRef(buffer, offset + 8, "Spell name"));
    list.add(new StringRef(buffer, offset + 12, "Identified name"));
    list.add(new ResourceRef(buffer, offset + 16, "Casting sound", "WAV"));
    list.add(new Flag(buffer, offset + 24, 4, "Flags", s_spellflag));
    list.add(new Bitmap(buffer, offset + 28, 2, "Spell type", s_spelltype));
    list.add(new Flag(buffer, offset + 30, 4, "Exclusion flags", s_exclude));   // 0x1e
//    list.add(new HashBitmap(buffer, offset + 32, 2, "Priest type", m_priesttype));     // 0x20
    list.add(new Bitmap(buffer, offset + 34, 2, "Casting animation", s_anim));  // 0x22
    list.add(new Unknown(buffer, offset + 36, 1));                                    // 0x23
    if (ResourceFactory.getInstance().resourceExists("SCHOOL.IDS"))
      list.add(new IdsBitmap(buffer, offset + 37, 1, "Primary type (school)", "SCHOOL.IDS")); // 0x25
    else
      list.add(new Bitmap(buffer, offset + 37, 1, "Primary type (school)", s_school)); // 0x25
    list.add(new Unknown(buffer, offset + 38, 1));
    list.add(new Bitmap(buffer, offset + 39, 1, "Secondary type", s_category));       // 0x27
    list.add(new Unknown(buffer, offset + 40, 12));
    list.add(new DecNumber(buffer, offset + 52, 4, "Spell level"));
    list.add(new Unknown(buffer, offset + 56, 2));
    list.add(new ResourceRef(buffer, offset + 58, "Spell icon", "BAM"));
    list.add(new Unknown(buffer, offset + 66, 2));
    list.add(new ResourceRef(buffer, offset + 68, "Ground icon", "BAM"));
    list.add(new Unknown(buffer, offset + 76, 4));
    list.add(new StringRef(buffer, offset + 80, "Spell description"));
    list.add(new StringRef(buffer, offset + 84, "Identified description"));
    list.add(new ResourceRef(buffer, offset + 88, "Description image", "BAM"));
    list.add(new Unknown(buffer, offset + 96, 4));
    SectionOffset abil_offset = new SectionOffset(buffer, offset + 100, "Abilities offset",
                                                  Ability.class);
    list.add(abil_offset);
    SectionCount abil_count = new SectionCount(buffer, offset + 104, 2, "# abilities",
                                               Ability.class);
    list.add(abil_count);
    SectionOffset global_offset = new SectionOffset(buffer, offset + 106, "Effects offset",
                                                    Effect.class);
    list.add(global_offset);
    list.add(new DecNumber(buffer, offset + 110, 2, "First effect index"));
    SectionCount global_count = new SectionCount(buffer, offset + 112, 2, "# global effects",
                                                 Effect.class);
    list.add(global_count);

    if (version.toString().equalsIgnoreCase("V2.0")) {
      list.add(new DecNumber(buffer, offset + 114, 1, "Spell duration rounds/level"));
      list.add(new DecNumber(buffer, offset + 115, 1, "Spell duration rounds base"));
      list.add(new Unknown(buffer, offset + 116, 14));
    }

    offset = abil_offset.getValue();
    Ability abilities[] = new Ability[abil_count.getValue()];
    for (int i = 0; i < abilities.length; i++) {
      abilities[i] = new Ability(this, buffer, offset, i);
      list.add(abilities[i]);
      offset = abilities[i].getEndOffset();
    }

    int offset2 = global_offset.getValue();
    for (int i = 0; i < global_count.getValue(); i++) {
      Effect eff = new Effect(this, buffer, offset2, i);
      offset2 = eff.getEndOffset();
      list.add(eff);
    }

    for (final Ability ability : abilities)
      offset2 = ability.readEffects(buffer, offset2);

    return Math.max(offset, offset2);
  }


  // Called by "Extended Search"
  // Checks whether the specified resource entry matches all available search options.
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
        DecNumber ofs = (DecNumber)spl.getAttribute("Effects offset");
        DecNumber cnt = (DecNumber)spl.getAttribute("# global effects");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          effects = new Effect[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.SPL_Effect), idx);
            effects[idx] = (Effect)spl.getAttribute(label);
          }
        } else {
          effects = new Effect[0];
        }

        ofs = (DecNumber)spl.getAttribute("Abilities offset");
        cnt = (DecNumber)spl.getAttribute("# abilities");
        if (ofs != null && ofs.getValue() > 0 && cnt != null && cnt.getValue() > 0) {
          abilities = new Ability[cnt.getValue()];
          for (int idx = 0; idx < cnt.getValue(); idx++) {
            String label = String.format(SearchOptions.getResourceName(SearchOptions.SPL_Ability), idx);
            abilities[idx] = (Ability)spl.getAttribute(label);
          }
        } else {
          abilities = new Ability[0];
        }

        abilityEffects = new Effect[abilities.length][];
        for (int idx = 0; idx < abilities.length; idx++) {
          if (abilities[idx] != null) {
            cnt = (DecNumber)abilities[idx].getAttribute("# effects");
            if (cnt != null && cnt.getValue() > 0) {
              abilityEffects[idx] = new Effect[cnt.getValue()];
              for (int idx2 = 0; idx2 < cnt.getValue(); idx2++) {
                String label = String.format(SearchOptions.getResourceName(SearchOptions.SPL_Ability_Effect), idx2);
                abilityEffects[idx][idx2] = (Effect)abilities[idx].getAttribute(label);
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
          StructEntry struct = spl.getAttribute(SearchOptions.getResourceName(key));
          retVal &= SearchOptions.Utils.matchString(struct, o, false, false);
        }

        String[] keyList = new String[]{SearchOptions.SPL_SpellType, SearchOptions.SPL_CastingAnimation,
                                        SearchOptions.SPL_PrimaryType, SearchOptions.SPL_SecondaryType,
                                        SearchOptions.SPL_Level};
        for (int idx = 0; idx < keyList.length; idx++) {
          if (retVal) {
            key = keyList[idx];
            o = searchOptions.getOption(key);
            StructEntry struct = spl.getAttribute(SearchOptions.getResourceName(key));
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
            StructEntry struct = spl.getAttribute(SearchOptions.getResourceName(key));
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
                  StructEntry struct = effects[idx2].getAttribute(SearchOptions.getResourceName(key));
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
                StructEntry struct = abilities[i].getAttribute(SearchOptions.getResourceName(key));
                abilityMatches[i][j] = SearchOptions.Utils.matchNumber(struct, o);
              }

              for (int j = 7; j < keyList.length; j++) {
                key = keyList[j];
                o = abilityOption.getOption(key);
                for (int k = 0; k < abilityEffects[i].length; k++) {
                  if (abilityEffects[i][k] != null) {
                    StructEntry struct = abilityEffects[i][k].getAttribute(SearchOptions.getResourceName(key));
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

