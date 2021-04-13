// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.infinity.resource.Profile;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.browser.ColorSelectionModel.ColorEntry;
import org.infinity.resource.cre.browser.CreatureAnimationModel.AnimateEntry;
import org.infinity.resource.cre.browser.CreatureSelectionModel.CreatureItem;
import org.infinity.resource.cre.browser.CreatureStatusModel.StatusEntry;
import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.util.CreatureInfo;
import org.infinity.resource.cre.decoder.util.ItemInfo;
import org.infinity.resource.cre.decoder.util.ItemInfo.ItemPredicate;
import org.infinity.resource.key.ResourceEntry;

/**
 * This model controls the relationships between creature controls and provides access to the various
 * creature-specific options.
 */
public class CreatureControlModel
{
  private final List<ColorSelectionModel> colorModels = new ArrayList<>();
  private final CreatureControlPanel panel;

  private CreatureSelectionModel modelCreSelection;
  private CreatureAnimationModel modelCreAnimation;
  private CreatureStatusModel modelCreAllegiance;
  private ItemSelectionModel modelItemHelmet;
  private ItemSelectionModel modelItemArmor;
  private ItemSelectionModel modelItemShield;
  private ItemSelectionModel modelItemWeapon;
  private int hashCreature;
  private boolean canApply, canReset;

  private SpriteDecoder decoder;

  /**
   * Creates a new {@code CreatureControlModel} object.
   * @param panel the {@code CreatureControlPanel} instance associated with this model.
   */
  public CreatureControlModel(CreatureControlPanel panel)
  {
    super();
    this.panel = Objects.requireNonNull(panel);
    init();
  }

  /**
   * Returns the {@link SpriteDecoder} instance associated with the currently selected creature animation.
   * Returns {@code null} if no creature resource has been selected.
   */
  public SpriteDecoder getDecoder() { return decoder; }

  /** Recreates the {@code SpriteDecoder} instance with the specified {@code CreResource}. */
  public void resetDecoder(CreResource cre) throws Exception
  {
    decoder = SpriteDecoder.importSprite(cre);
  }

  /**
   * Selects the specified CRE resource and initializes related fields.
   * @param entry {@code ResourceEntry} instance of the creature
   * @throws Exception if creature resource could not be initialized
   */
  public void setSelectedCreature(ResourceEntry entry) throws Exception
  {
    if (getModelCreature().getSize() == 0) {
      getModelCreature().reload();
    }

    int idx = Math.max(0, getModelCreature().getIndexOf(entry));
    getModelCreature().setSelectedItem(getModelCreature().getElementAt(idx));
    creSelectionChanged();
  }

  /**
   * Selects the specified CRE resource and initializes related fields.
   * @param entry {@code CreResource} instance of the creature
   * @throws Exception if creature resource could not be initialized
   */
  public void setSelectedCreature(CreResource cre) throws Exception
  {
    Object selection = cre;
    if (selection == null) {
      selection = CreatureSelectionModel.CreatureItem.getDefault();
    }

    if (getModelCreature().getSize() == 0) {
      getModelCreature().reload();
    }

    int idx = getModelCreature().getIndexOf(selection);
    if (idx >= 0) {
      getModelCreature().setSelectedItem(getModelCreature().getElementAt(idx));
    } else {
      getModelCreature().setSelectedItem(selection);
    }
    creSelectionChanged();
  }

  /** Selects the specified creature animation and updates related fields. */
  public void setSelectedAnimation(int value)
  {
    if (getModelAnimation().getSize() == 0) {
      getModelAnimation().reload();
    }

    int idx = getModelAnimation().getIndexOf(Integer.valueOf(value));
    if (idx >= 0) {
      getModelAnimation().setSelectedItem(getModelAnimation().getElementAt(idx));
    } else {
      getModelAnimation().setSelectedItem(new AnimateEntry(value, "(Unknown)"));
    }
    creAnimationChanged();
  }

  /** Selects the specified creature allegiance and updates related fields. */
  public void setSelectedAllegiance(int value)
  {
    if (getModelAllegiance().getSize() == 0) {
      getModelAllegiance().reload();
    }

    int idx = Math.max(0, getModelAllegiance().getIndexOf(Integer.valueOf(value)));
    getModelAllegiance().setSelectedItem(getModelAllegiance().getElementAt(idx));
    creAllegianceChanged();
  }

  /** Selects the specified helmet item and updates related fields. */
  public void setSelectedHelmet(ItemInfo item)
  {
    if (getModelHelmet().getSize() == 0) {
      getModelHelmet().reload();
    }

    int idx = Math.max(0, getModelHelmet().getIndexOf(item));
    getModelHelmet().setSelectedItem(getModelHelmet().getElementAt(idx));
    itemHelmetChanged();
  }

  /** Selects the specified armor item and updates related fields. */
  public void setSelectedArmor(ItemInfo item)
  {
    if (getModelArmor().getSize() == 0) {
      getModelArmor().reload();
    }

    int idx = Math.max(0, getModelArmor().getIndexOf(item));
    getModelArmor().setSelectedItem(getModelArmor().getElementAt(idx));
    itemArmorChanged();
  }

  /**
   * Selects the specified shield or left-handed weapon item and updates related fields.
   */
  public void setSelectedShield(ItemInfo item)
  {
    if (getModelShield().getSize() == 0) {
      getModelShield().reload();
    }

    int idx = Math.max(0, getModelShield().getIndexOf(item));
    getModelShield().setSelectedItem(getModelShield().getElementAt(idx));
    itemShieldChanged();
  }

  /**
   * Selects the specified weapon item and updates related fields.
   * This method should be called <b>BEFORE</b> {@link #setSelectedShield(ItemInfo)} to ensure
   * shield item list is compatible with the selected weapon.
   */
  public void setSelectedWeapon(ItemInfo item)
  {
    if (getModelWeapon().getSize() == 0) {
      getModelWeapon().reload();
    }

    int idx = Math.max(0, getModelWeapon().getIndexOf(item));
    getModelWeapon().setSelectedItem(getModelWeapon().getElementAt(idx));
    itemWeaponChanged();
  }

  /**
   * Selects the specified color entry for the given location.
   * @param index the color location index
   * @param color the color value
   */
  public void setSelectedColor(int index, int color)
  {
    final ColorSelectionModel model = getModelColor(index);
    if (model == null) {
      return;
    }

    if (model.getSize() == 0) {
      model.reload();
    }

    int idx = model.getIndexOf(Integer.valueOf(color));
    model.setSelectedItem(model.getElementAt(idx));
    colorChanged(index);
  }

  /** Returns the control panel associated with the model. */
  public CreatureControlPanel getPanel()
  {
    return panel;
  }

  /** Returns the model of the creature selection combobox. */
  public CreatureSelectionModel getModelCreature()
  {
    return modelCreSelection;
  }

  /** Returns the model of the creature animation combobox. */
  public CreatureAnimationModel getModelAnimation()
  {
    return modelCreAnimation;
  }

  /** Returns the model of the creature allegiance combobox. */
  public CreatureStatusModel getModelAllegiance()
  {
    return modelCreAllegiance;
  }

  /** Returns the model of the helmet combobox. */
  public ItemSelectionModel getModelHelmet()
  {
    return modelItemHelmet;
  }

  /** Returns the model of the armor combobox. */
  public ItemSelectionModel getModelArmor()
  {
    return modelItemArmor;
  }

  /** Returns the model of the shield combobox. */
  public ItemSelectionModel getModelShield()
  {
    return modelItemShield;
  }

  /** Returns the model of the weapon combobox. */
  public ItemSelectionModel getModelWeapon()
  {
    return modelItemWeapon;
  }

  /** Returns the model of the specified color combobox. */
  public ColorSelectionModel getModelColor(int index)
  {
    if (index >= 0 && index < colorModels.size()) {
      return colorModels.get(index);
    }
    return null;
  }

  /** Returns an iterator over the list of color models. */
  public Iterator<ColorSelectionModel> getColorModelIterator()
  {
    return colorModels.iterator();
  }

  /**
   * Returns the {@code CreatureItem} instance of the currently selected CRE resource.
   * Returns {@code null} if no creature is selected.
   */
  public CreatureItem getSelectedCreature()
  {
    if (modelCreSelection != null && modelCreSelection.getSelectedItem() instanceof CreatureItem) {
      return (CreatureItem)modelCreSelection.getSelectedItem();
    } else {
      return null;
    }
  }

  /**
   * Returns the {@code AnimateEntry} instance of the currently selected creature animation.
   * Returns {@code null} if valid entry is not available.
   */
  public AnimateEntry getSelectedAnimation()
  {
    AnimateEntry retVal = null;
    if (modelCreAnimation != null && modelCreAnimation.getSelectedItem() != null) {
      Object o = modelCreAnimation.getSelectedItem();
      if (o instanceof AnimateEntry) {
        retVal = (AnimateEntry)o;
      } else {
        int value = modelCreAnimation.parseValue(o);
        if (value >= 0) {
          retVal = new AnimateEntry(value, "(Unknown)");
        }
      }
    }
    return retVal;
  }

  /**
   * Returns the {@code AnimateEntry} instance of the currently selected creature allegiance.
   * Returns {@code null} if entry is not available.
   */
  public StatusEntry getSelectedAllegiance()
  {
    if (modelCreAllegiance != null && modelCreAllegiance.getSelectedItem() instanceof StatusEntry) {
      return (StatusEntry)modelCreAllegiance.getSelectedItem();
    } else {
      return null;
    }
  }

  /**
   * Returns the {@code ItemInfo} instance of the currently equipped helmet.
   * Returns {@code null} if entry is not available.
   */
  public ItemInfo getSelectedHelmet(ItemSelectionModel model)
  {
    return getItem(modelItemHelmet);
  }

  /**
   * Returns the {@code ItemInfo} instance of the currently equipped armor.
   * Returns {@code null} if entry is not available.
   */
  public ItemInfo getSelectedArmor(ItemSelectionModel model)
  {
    return getItem(modelItemArmor);
  }

  /**
   * Returns the {@code ItemInfo} instance of the currently equipped shield.
   * Returns {@code null} if entry is not available.
   */
  public ItemInfo getSelectedShield(ItemSelectionModel model)
  {
    return getItem(modelItemShield);
  }

  /**
   * Returns the {@code ItemInfo} instance of the currently equipped weapon.
   * Returns {@code null} if entry is not available.
   */
  public ItemInfo getSelectedWeapon(ItemSelectionModel model)
  {
    return getItem(modelItemWeapon);
  }

  /**
   * Returns the {@code ColorEntry} instance of the specified color location.
   * Returns {@code null} if entry is not available.
   * @param index color location index (range: [0, 7])
   */
  public ColorEntry getSelectedColor(int index)
  {
    try {
      ColorSelectionModel model = colorModels.get(index);
      if (model != null && model.getSelectedItem() instanceof ColorEntry) {
        return (ColorEntry)model.getSelectedItem();
      }
    } catch (IndexOutOfBoundsException e) {
    }
    return null;
  }

  /** This method updates relevant settings when creature resource selection has changed. */
  public void creSelectionChanged() throws Exception
  {
    Object item = getModelCreature().getSelectedItem();
    if (item != null) {
      CreResource cre;
      if (item instanceof CreatureItem) {
        cre = new CreResource(((CreatureItem)item).getResourceEntry());
      } else if (item instanceof CreResource) {
        cre = (CreResource)item;
      } else if (item instanceof ResourceEntry) {
        cre = new CreResource((ResourceEntry)item);
      } else {
        throw new IllegalArgumentException("No valid creature resource selected");
      }

      resetDecoder(cre);
      hashCreature = decoder.getCreResource().hashCode();
      final CreatureInfo creInfo = getDecoder().getCreatureInfo();

      // setting animation
      int animId = creInfo.getAnimationId();
      setSelectedAnimation(animId);

      // setting allegiance
      int ea = creInfo.getAllegiance();
      setSelectedAllegiance(ea);

      // setting equipped helmet
      ItemInfo helmet = creInfo.getEquippedHelmet();
      setSelectedHelmet(helmet);

      // setting equipped armor
      ItemInfo armor = creInfo.getEquippedArmor();
      setSelectedArmor(armor);

      // setting equipped main-hand weapon
      ItemInfo weapon = creInfo.getEquippedWeapon();
      setSelectedWeapon(weapon);

      // setting equipped shield or off-hand weapon
      ItemInfo shield = creInfo.getEquippedShield();
      setSelectedShield(shield);

      // setting colors
      String[] labels = CreatureControlPanel.createColorLabels(getDecoder());
      getPanel().setColorLabels(labels);

      for (int i = 0, numColors = creInfo.getColorCount(); i < colorModels.size(); i++) {
        int value = (i < numColors) ? creInfo.getColorValue(i) : -1;
        setSelectedColor(i, value);
      }
    }

    setModified();
  }

  /** This method updates relevant settings when creature animation selection has changed. */
  public void creAnimationChanged()
  {
    // nothing to do
    setModified();
  }

  /** This method updates relevant settings when creature allegiance selection has changed. */
  public void creAllegianceChanged()
  {
    // nothing to do
    setModified();
  }

  public void crePanicChanged()
  {
    // nothing to do
    setModified();
  }

  /** This method updates relevant settings when helmet equipment has changed. */
  public void itemHelmetChanged()
  {
    // nothing to do
    setModified();
  }

  /** This method updates relevant settings when armor equipment has changed. */
  public void itemArmorChanged()
  {
    // nothing to do
    setModified();
  }

  /** This method updates relevant settings when shield/left-handed weapon equipment has changed. */
  public void itemShieldChanged()
  {
    // nothing to do
    setModified();
  }

  /** This method updates relevant settings when weapon equipment has changed. */
  public void itemWeaponChanged()
  {
    // update shield equipment list depending on selected weapon type
    if (getModelWeapon().getSelectedItem() instanceof ItemInfo) {
      // determining valid filters for shield slot
      ItemInfo info = (ItemInfo)getModelWeapon().getSelectedItem();
      boolean isMelee = (info.getAbilityCount() > 0) && (info.getAbility(0).getAbilityType() == 1);
      boolean isTwoHanded = (info.getFlags() & (1 << 1)) != 0;
      isTwoHanded |= Profile.isEnhancedEdition() && ((info.getFlags() & (1 << 12)) != 0);
      ItemPredicate shieldPred = null;
      if (!isTwoHanded) {
        shieldPred = (shieldPred == null) ? ItemInfo.FILTER_SHIELD : shieldPred.or(ItemInfo.FILTER_SHIELD);
        if (isMelee) {
          shieldPred = (shieldPred == null) ? ItemInfo.FILTER_WEAPON_MELEE_LEFT_HANDED : shieldPred.or(ItemInfo.FILTER_WEAPON_MELEE_LEFT_HANDED);
        }
      }
      if (shieldPred == null) {
        shieldPred = ItemInfo.FILTER_NONE;
      }

      // updating item list of shield slot
      Object oldItem = getModelShield().getSelectedItem();
      getModelShield().setFilter(shieldPred);
      getModelShield().reload();
      int idx = Math.max(0, getModelShield().getIndexOf(oldItem));
      getModelShield().setSelectedItem(getModelShield().getElementAt(idx));
    }
    setModified();
  }

  /** This method updates relevant settings when specified color entry has changed. */
  public void colorChanged(int index)
  {
    // nothing to do
    setModified();
  }

  /** Resets settings to the defaults as defined by the currently selected creature resource. */
  public void reset()
  {
    try {
      creSelectionChanged();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Returns whether the specified color entry is a random color. */
  public boolean isColorRandom(int index)
  {
    boolean retVal = false;
    if (index >= 0 && index < colorModels.size()) {
      Object item = colorModels.get(index).getSelectedItem();
      if (item instanceof ColorSelectionModel.ColorEntry) {
        retVal = (((ColorSelectionModel.ColorEntry)item).getState() == ColorSelectionModel.ColorEntry.State.RANDOM);
      }
    }
    return retVal;
  }

  /** Returns whether settings have been modified since the last "Reset" or "Apply" operation. */
  public boolean canApply()
  {
    return canApply;
  }

  /** Returns whether creature selection changed since the last "Reset" operation. */
  public boolean canReset()
  {
    return canReset;
  }

  protected void setModified()
  {
    if (!canApply || !canReset) {
      canApply = true;
      canReset = isCreatureModified();
      getPanel().fireSettingsChanged();
    }
  }

  protected void resetModified()
  {
    if (canApply || canReset) {
      boolean randomColor = false;
      for (int i = 0; i < colorModels.size() && !randomColor; i++) {
        randomColor |= isColorRandom(i);
      }
      canApply = randomColor;
      canReset = isCreatureModified();
      getPanel().fireSettingsChanged();
    }
  }

  /** Returns whether modifications have been applied to the currently selected creature entry. */
  public boolean isCreatureModified()
  {
    int hash = getDecoder().getCreResource().hashCode();
    return (hashCreature != hash);
  }

  /** Returns the currently selected item in the specified selection model. */
  private ItemInfo getItem(ItemSelectionModel model)
  {
    if (model != null && model.getSelectedItem() instanceof ItemInfo) {
      return (ItemInfo)model.getSelectedItem();
    } else {
      return null;
    }
  }

  private void init()
  {
    // perform lazy initialization: time-consuming initializations are performed on demand
    modelCreSelection = new CreatureSelectionModel(false);
    modelCreAnimation = new CreatureAnimationModel();
    modelCreAllegiance = new CreatureStatusModel();
    modelItemHelmet = new ItemSelectionModel(ItemInfo.FILTER_HELMET, false);
    modelItemArmor = new ItemSelectionModel(ItemInfo.FILTER_ARMOR, false);
    modelItemShield = new ItemSelectionModel(ItemInfo.FILTER_SHIELD.or(ItemInfo.FILTER_WEAPON_MELEE_LEFT_HANDED), false);
    modelItemWeapon = new ItemSelectionModel(ItemInfo.FILTER_WEAPON, false);
    for (int i = 0; i < 7; i++) {
      colorModels.add(new ColorSelectionModel());
    }
  }
}
