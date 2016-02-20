// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.spl;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.Unknown;
import infinity.datatype.UnsignDecNumber;
import infinity.gui.StructViewer;
import infinity.resource.AbstractAbility;
import infinity.resource.AbstractStruct;
import infinity.resource.AddRemovable;
import infinity.resource.Effect;
import infinity.resource.HasAddRemovable;
import infinity.resource.HasViewerTabs;
import infinity.resource.Profile;
import infinity.resource.ResourceFactory;

import javax.swing.JComponent;

public final class Ability extends AbstractAbility implements AddRemovable, HasAddRemovable, HasViewerTabs
{
  // SPL/Ability-specific field labels (more fields defined in AbstractAbility)
  public static final String SPL_ABIL                     = "Spell ability";
  public static final String SPL_ABIL_HOSTILITY           = "Hostility";
  public static final String SPL_ABIL_MIN_LEVEL           = "Minimum level";
  public static final String SPL_ABIL_CASTING_SPEED       = "Casting speed";

  public static final String[] s_hostility = {"Hostile", "", "", "", "Non-hostile"};
  public static final String[] s_abilityuse = {"", "", "Spell slots", "", "Innate slots"};

  Ability() throws Exception
  {
    super(null, SPL_ABIL, new byte[40], 0);
  }

  Ability(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, SPL_ABIL + " " + number, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Effect()};
  }

  @Override
  public AddRemovable confirmAddEntry(AddRemovable entry) throws Exception
  {
    return entry;
  }

  @Override
  public boolean confirmRemoveEntry(AddRemovable entry) throws Exception
  {
    return true;
  }

// --------------------- End Interface HasAddRemovable ---------------------


//--------------------- Begin Interface AddRemovable ---------------------

  @Override
  public boolean canRemove()
  {
    return true;
  }

//--------------------- End Interface AddRemovable ---------------------


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
    return new ViewerAbility(this);
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return true;
  }

// --------------------- End Interface HasViewerTabs ---------------------

  @Override
  public int read(byte buffer[], int offset) throws Exception
  {
    if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset, 1, ABILITY_TYPE, s_type));
      addField(new Bitmap(buffer, offset + 1, 1, SPL_ABIL_HOSTILITY, s_hostility));
    } else {
      addField(new Bitmap(buffer, offset, 2, ABILITY_TYPE, s_type));
    }
    addField(new Bitmap(buffer, offset + 2, 2, ABILITY_LOCATION, s_abilityuse));
    addField(new ResourceRef(buffer, offset + 4, ABILITY_ICON, "BAM"));
    addField(new Bitmap(buffer, offset + 12, 1, ABILITY_TARGET, s_targettype));
    addField(new UnsignDecNumber(buffer, offset + 13, 1, ABILITY_NUM_TARGETS));
    addField(new DecNumber(buffer, offset + 14, 2, ABILITY_RANGE));
    addField(new DecNumber(buffer, offset + 16, 2, SPL_ABIL_MIN_LEVEL));
    addField(new DecNumber(buffer, offset + 18, 2, SPL_ABIL_CASTING_SPEED));
    addField(new DecNumber(buffer, offset + 20, 2, ABILITY_HIT_BONUS));
    addField(new DecNumber(buffer, offset + 22, 2, ABILITY_DICE_SIZE));
    addField(new DecNumber(buffer, offset + 24, 2, ABILITY_DICE_COUNT));
    addField(new DecNumber(buffer, offset + 26, 2, ABILITY_DAMAGE_BONUS));
    addField(new Bitmap(buffer, offset + 28, 2, ABILITY_DAMAGE_TYPE, s_dmgtype));
    addField(new SectionCount(buffer, offset + 30, 2, ABILITY_NUM_EFFECTS, Effect.class));
    addField(new DecNumber(buffer, offset + 32, 2, ABILITY_FIRST_EFFECT_INDEX));
    addField(new DecNumber(buffer, offset + 34, 2, ABILITY_NUM_CHARGES));
    addField(new Unknown(buffer, offset + 36, 2));
    if (ResourceFactory.resourceExists("PROJECTL.IDS")) {
      addField(new ProRef(buffer, offset + 38, ABILITY_PROJECTILE));
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset + 38, 2, ABILITY_PROJECTILE, s_proj_pst));
    } else if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new Bitmap(buffer, offset + 38, 2, ABILITY_PROJECTILE, s_proj_iwd));
    } else {
      addField(new Bitmap(buffer, offset + 38, 2, ABILITY_PROJECTILE, s_projectile));
    }
    return offset + 40;
  }
}

