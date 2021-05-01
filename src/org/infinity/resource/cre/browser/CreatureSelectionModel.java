// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.StructureFactory;
import org.infinity.resource.StructureFactory.StructureException;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.key.BufferedResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;
import org.infinity.util.ResourceStructure;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;

/**
 * {@code ComboBoxModel} for the creature selection combo box used in the Creature Animation Browser.
 */
public class CreatureSelectionModel extends AbstractListModel<CreatureSelectionModel.CreatureItem>
    implements ComboBoxModel<CreatureSelectionModel.CreatureItem>
{
  private final List<CreatureItem> creList = new ArrayList<>();

  private Object selectedItem;

  /**
   * Creates a new combobox model.
   * @param autoInit whether the model is automatically populated with creature data.
   */
  public CreatureSelectionModel(boolean autoInit)
  {
    super();
    if (autoInit) {
      init();
    }
  }

  /** Discards the current content and reloads creature data. */
  public void reload()
  {
    init();
  }

  /**
   * Returns the index-position of the specified object in the list.
   * @param anItem a {@code CreatureItem} object, {@code ResourceEntry} object or {@code String} specifying a resref.
   * @return an int representing the index position, where 0 is the first position.
   *         Returns -1 if the item could not be found in the list.
   */
  public int getIndexOf(Object anItem)
  {
    if (anItem instanceof CreatureItem) {
      return creList.indexOf(anItem);
    } else if (anItem instanceof CreResource) {
      CreResource cre = (CreResource)anItem;
      if (cre.getResourceEntry() != null) {
        return getIndexOf(cre.getResourceEntry());
      }
    } else if (anItem instanceof ResourceEntry) {
      return IntStream
          .range(0, creList.size())
          .filter(i -> creList.get(i).getResourceEntry().equals(anItem))
          .findAny()
          .orElse(-1);
    } else if (anItem != null) {
      final String name = anItem.toString();
      return IntStream
          .range(0, creList.size())
          .filter(i -> name.equalsIgnoreCase(creList.get(i).getResourceEntry().getResourceRef()))
          .findAny()
          .orElse(-1);
    }
    return -1;
  }

  /** Empties the list. */
  public void removeAllElements()
  {
    if (!creList.isEmpty()) {
      int oldSize = creList.size();
      creList.clear();
      selectedItem = null;
      if (oldSize > 0) {
        fireIntervalRemoved(this, 0, oldSize - 1);
      }
    } else {
      selectedItem = null;
    }
  }

//--------------------- Begin Interface ListModel ---------------------

  @Override
  public int getSize()
  {
    return creList.size();
  }

  @Override
  public CreatureItem getElementAt(int index)
  {
    if (index >= 0 && index < creList.size()) {
      return creList.get(index);
    } else {
      return null;
    }
  }

//--------------------- End Interface ListModel ---------------------

//--------------------- Begin Interface ComboBoxModel ---------------------

  @Override
  public void setSelectedItem(Object anItem)
  {
    if ((selectedItem != null && !selectedItem.equals(anItem)) ||
        selectedItem == null && anItem != null) {
      selectedItem = anItem;
      fireContentsChanged(this, -1, -1);
    }
  }

  @Override
  public Object getSelectedItem()
  {
    return selectedItem;
  }

//--------------------- End Interface ComboBoxModel ---------------------

  private void init()
  {
    removeAllElements();
    selectedItem = null;

    ResourceFactory.getResources("CRE").stream().forEach(re -> creList.add(new CreatureItem(re)));
    Collections.sort(creList);
    creList.add(0, CreatureItem.getDefault());
    if (!creList.isEmpty()) {
      fireIntervalAdded(this, 0, creList.size() - 1);
    }
  }

//-------------------------- INNER CLASSES --------------------------

  public static class CreatureItem implements Comparable<CreatureItem>
  {
    /** The default creature item referencing a pseudo creature. */
    private static final Couple<Profile.Game, CreatureItem> DEFAULT_CREATURE = Couple.with(null, null);

    private final ResourceEntry entry;

    public CreatureItem(ResourceEntry entry)
    {
      this.entry = Objects.requireNonNull(entry);
    }

    /** Returns the {@code ResourceEntry} associated with the item. */
    public ResourceEntry getResourceEntry() { return entry; }

    @Override
    public String toString()
    {
      if (entry == DEFAULT_CREATURE.getValue1().getResourceEntry()) {
        return "None (customize from scratch)";
      } else {
        String resref = entry.getResourceName();
        String name = entry.getSearchString();
        if (name == null || name.isEmpty()) {
          return resref;
        } else {
          return resref + " (" + name + ")";
        }
      }
    }

    //--------------------- Begin Interface Comparable ---------------------

    @Override
    public int compareTo(CreatureItem o)
    {
      return entry.compareTo(o.entry);
    }

    //--------------------- End Interface Comparable ---------------------

    /** Returns a default creature suitable for the current game. */
    public static CreatureItem getDefault()
    {
      if (DEFAULT_CREATURE.getValue0() != Profile.getGame()) {
        DEFAULT_CREATURE.setValue0(Profile.getGame());
        DEFAULT_CREATURE.setValue1(new CreatureItem(createCreature()));
      }
      return DEFAULT_CREATURE.getValue1();
    }

    /** Helper method: Creates a default CRE resource from scratch based on the current game. */
    private static ResourceEntry createCreature()
    {
      ResourceEntry retVal = null;
      try {
        ResourceStructure res = StructureFactory.getInstance().createStructure(StructureFactory.ResType.RES_CRE, null, null);
        ByteBuffer buf = res.getBuffer();

        // setting default animation
        switch (Profile.getGame()) {
          case PST:
            buf.putInt(0x28, 0xe03e); // Townie, LC Male
            break;
          case PSTEE:
            buf.putInt(0x28, 0xf03e); // cl_3_Townie_LC_Male_2
            break;
          default:
            buf.putInt(0x28, 0x6100); // fighter_male_human
        }

        // setting default colors
        final byte[] colorsPst = {(byte)7, (byte)86, (byte)40, (byte)75};
        final byte[] locationsPst = {(byte)224, (byte)144, (byte)176, (byte)160};
        final byte[] colorsOther = {(byte)30, (byte)37, (byte)57, (byte)12, (byte)23, (byte)28, (byte)0};
        switch (Profile.getGame()) {
          case PST:
          case PSTEE:
          {
            int ofsColors = (Profile.getGame() == Profile.Game.PST) ? 0x2e4 : 0x2c;
            int ofsLocations = (Profile.getGame() == Profile.Game.PST) ? 0x2f5 : -1;
            int sizeColors = (Profile.getGame() == Profile.Game.PST) ? 2 : 1;
            int cnt = Math.min(colorsPst.length, locationsPst.length);
            if (ofsLocations > 0) {
              buf.put(0x2df, (byte)cnt);  // # colors
            }
            for (int i = 0; i < cnt; i++) {
              buf.put(ofsColors + i * sizeColors, colorsPst[i]);
              if (ofsLocations > 0) {
                buf.put(ofsLocations + i, locationsPst[i]);
              }
            }
            break;
          }
          default:
            for (int i = 0; i < colorsOther.length; i++) {
              buf.put(0x2c + i, colorsOther[i]);
            }
        }

        // setting default properties
        // SEX=MALE, EA=NEUTRAL, GENERAL=HUMANOID, RACE=HUMAN, CLASS=FIGHTER, GENDER=MALE, ALIGNMENT=NEUTRAL
        byte[] properties = {(byte)1, (byte)128, (byte)1, (byte)1, (byte)2, (byte)1, (byte)0x22};
        int[] offsets;
        String ver = StreamUtils.readString(buf, 4, 4, Misc.CHARSET_DEFAULT).toUpperCase();
        switch (ver) {
          case "V1.2":
            offsets = new int[] {0x237, 0x314, 0x315, 0x316, 0x317, 0x319, 0x31f};
            break;
          case "V2.2":
            offsets = new int[] {0x389, 0x384, 0x385, 0x386, 0x387, 0x389, 0x38f};
            break;
          case "V9.0":
            offsets = new int[] {0x237, 0x2d8, 0x2d9, 0x2da, 0x2db, 0x2dd, 0x2e3};
            break;
          default:
            offsets = new int[] {0x237, 0x270, 0x271, 0x272, 0x273, 0x275, 0x27b};
        }
        for (int i = 0; i < properties.length; i++) {
          buf.put(offsets[i], properties[i]);
        }

        retVal = new BufferedResourceEntry(buf, "*.CRE");
      } catch (StructureException e) {
        e.printStackTrace();
      }

      return retVal;
    }
  }
}
