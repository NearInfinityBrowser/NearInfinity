// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.itm;

import java.nio.ByteBuffer;

import javax.swing.JComponent;

import org.infinity.datatype.Bitmap;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.PriTypeBitmap;
import org.infinity.datatype.ProRef;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SecTypeBitmap;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.UnsignDecNumber;
import org.infinity.gui.StructViewer;
import org.infinity.resource.AbstractAbility;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.AddRemovable;
import org.infinity.resource.Effect;
import org.infinity.resource.HasChildStructs;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.util.io.StreamUtils;

public final class Ability extends AbstractAbility implements AddRemovable, HasChildStructs, HasViewerTabs
{
  // ITM/Ability-specific field labels (more fields defined in AbstractAbility)
  public static final String ITM_ABIL                     = "Item ability";
  public static final String ITM_ABIL_DICE_SIZE_ALT       = "Alternate dice size";
  public static final String ITM_ABIL_LAUNCHER_REQUIRED   = "Launcher required";
  public static final String ITM_ABIL_DICE_COUNT_ALT      = "Alternate # dice thrown";
  public static final String ITM_ABIL_SPEED               = "Speed";
  public static final String ITM_ABIL_DAMAGE_BONUS_ALT    = "Alternate damage bonus";
  public static final String ITM_ABIL_PRIMARY_TYPE        = "Primary type (school)";
  public static final String ITM_ABIL_SECONDARY_TYPE      = "Secondary type";
  public static final String ITM_ABIL_FLAGS               = "Flags";
  public static final String ITM_ABIL_ANIM_OVERHAND       = "Animation: Overhand swing %";
  public static final String ITM_ABIL_ANIM_BACKHAND       = "Animation: Backhand swing %";
  public static final String ITM_ABIL_ANIM_THRUST         = "Animation: Thrust %";
  public static final String ITM_ABIL_IS_ARROW            = "Is arrow?";
  public static final String ITM_ABIL_IS_BOLT             = "Is bolt?";
  public static final String ITM_ABIL_IS_BULLET           = "Is bullet?";

  public static final String[] s_launcher = {"None", "Bow", "Crossbow", "Sling"};
  public static final String[] s_abilityuse = {"", "Weapon", "Spell", "Item", "Ability", "reserved"};
  public static final String[] s_recharge = {
    "No flags set", "Add strength bonus", "Breakable", "EE: Damage strength bonus",
    "EE: THAC0 strength bonus", null, null, null, null, null, "EE: Break Sanctuary/Invisibility;Ignored for Target: Caster",
    "Hostile", "Recharge after resting", null, null, null, null, "Bypass armor", "Keen edge", null,
    null, null, null, null, null, null, "Ex: Toggle backstab", "EE/Ex: Cannot target invisible"};

  Ability() throws Exception
  {
    super(null, ITM_ABIL, StreamUtils.getByteBuffer(56), 0);
  }

  Ability(AbstractStruct superStruct, ByteBuffer buffer, int offset, int number) throws Exception
  {
    super(superStruct, ITM_ABIL + " " + number, buffer, offset);
  }

  @Override
  public AddRemovable[] getPrototypes() throws Exception
  {
    return new AddRemovable[]{new Effect()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public boolean canRemove()
  {
    return true;
  }

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
    return new ViewerAbility(this);
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
  }

  @Override
  public int read(ByteBuffer buffer, int offset) throws Exception
  {
    if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      addField(new Bitmap(buffer, offset, 1, ABILITY_TYPE, s_type));
      addField(new Flag(buffer, offset + 1, 1, ABILITY_TYPE_FLAGS, s_type_flags));
      addField(new Bitmap(buffer, offset + 2, 1, ABILITY_LOCATION, s_abilityuse));
      addField(new DecNumber(buffer, offset + 3, 1, ITM_ABIL_DICE_SIZE_ALT));
      addField(new ResourceRef(buffer, offset + 4, ABILITY_ICON, "BAM"));
      addField(new Bitmap(buffer, offset + 12, 1, ABILITY_TARGET, s_targettype));
      addField(new UnsignDecNumber(buffer, offset + 13, 1, ABILITY_NUM_TARGETS));
      addField(new DecNumber(buffer, offset + 14, 2, ABILITY_RANGE));
      addField(new Bitmap(buffer, offset + 16, 1, ITM_ABIL_LAUNCHER_REQUIRED, s_launcher));
      addField(new DecNumber(buffer, offset + 17, 1, ITM_ABIL_DICE_COUNT_ALT));
      addField(new DecNumber(buffer, offset + 18, 1, ITM_ABIL_SPEED));
      addField(new DecNumber(buffer, offset + 19, 1, ITM_ABIL_DAMAGE_BONUS_ALT));
      addField(new DecNumber(buffer, offset + 20, 2, ABILITY_HIT_BONUS));
      addField(new DecNumber(buffer, offset + 22, 1, ABILITY_DICE_SIZE));
      addField(new PriTypeBitmap(buffer, offset + 23, 1, ITM_ABIL_PRIMARY_TYPE));
      addField(new DecNumber(buffer, offset + 24, 1, ABILITY_DICE_COUNT));
      addField(new SecTypeBitmap(buffer, offset + 25, 1, ITM_ABIL_SECONDARY_TYPE));
    }
    else {
      addField(new Bitmap(buffer, offset, 1, ABILITY_TYPE, s_type));
      addField(new Flag(buffer, offset + 1, 1, ABILITY_TYPE_FLAGS, s_type_flags));
      addField(new Bitmap(buffer, offset + 2, 2, ABILITY_LOCATION, s_abilityuse));
      addField(new ResourceRef(buffer, offset + 4, ABILITY_ICON, "BAM"));
      addField(new Bitmap(buffer, offset + 12, 2, ABILITY_TARGET, s_targettype));
      addField(new DecNumber(buffer, offset + 14, 2, ABILITY_RANGE));
      addField(new Bitmap(buffer, offset + 16, 2, ITM_ABIL_LAUNCHER_REQUIRED, s_launcher));
      addField(new DecNumber(buffer, offset + 18, 2, ITM_ABIL_SPEED));
      addField(new DecNumber(buffer, offset + 20, 2, ABILITY_HIT_BONUS));
      addField(new DecNumber(buffer, offset + 22, 2, ABILITY_DICE_SIZE));
      addField(new DecNumber(buffer, offset + 24, 2, ABILITY_DICE_COUNT));
    }
    addField(new DecNumber(buffer, offset + 26, 2, ABILITY_DAMAGE_BONUS));
    addField(new Bitmap(buffer, offset + 28, 2, ABILITY_DAMAGE_TYPE, s_dmgtype));
    addField(new SectionCount(buffer, offset + 30, 2, ABILITY_NUM_EFFECTS, Effect.class));
    addField(new DecNumber(buffer, offset + 32, 2, ABILITY_FIRST_EFFECT_INDEX));
    addField(new DecNumber(buffer, offset + 34, 2, ABILITY_NUM_CHARGES));
    addField(new Bitmap(buffer, offset + 36, 2, ABILITY_WHEN_DRAINED, s_drain));
    addField(new Flag(buffer, offset + 38, 4, ITM_ABIL_FLAGS, s_recharge));
    if (ResourceFactory.resourceExists("PROJECTL.IDS") && ResourceFactory.resourceExists("MISSILE.IDS")) {
      addField(new ProRef(buffer, offset + 42, ABILITY_PROJECTILE));
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset + 42, 2, ABILITY_PROJECTILE, s_proj_pst));
    } else if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new Bitmap(buffer, offset + 42, 2, ABILITY_PROJECTILE, s_proj_iwd));
    } else {
      addField(new Bitmap(buffer, offset + 42, 2, ABILITY_PROJECTILE, s_projectile));
    }
    addField(new DecNumber(buffer, offset + 44, 2, ITM_ABIL_ANIM_OVERHAND));
    addField(new DecNumber(buffer, offset + 46, 2, ITM_ABIL_ANIM_BACKHAND));
    addField(new DecNumber(buffer, offset + 48, 2, ITM_ABIL_ANIM_THRUST));
    addField(new Bitmap(buffer, offset + 50, 2, ITM_ABIL_IS_ARROW, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 52, 2, ITM_ABIL_IS_BOLT, OPTION_NOYES));
    addField(new Bitmap(buffer, offset + 54, 2, ITM_ABIL_IS_BULLET, OPTION_NOYES));

    return offset + 56;
  }
}

