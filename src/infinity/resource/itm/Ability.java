// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.itm;

import infinity.datatype.Bitmap;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ProRef;
import infinity.datatype.ResourceRef;
import infinity.datatype.SecType2daBitmap;
import infinity.datatype.SectionCount;
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
import infinity.resource.spl.SplResource;

import javax.swing.JComponent;

public final class Ability extends AbstractAbility implements AddRemovable, HasAddRemovable, HasViewerTabs
{
  public static final String[] s_yesno = {"No", "Yes"};
  public static final String[] s_drain = {"Item remains", "Item vanishes", "Replace with used up", "Item recharges"};
  public static final String[] s_launcher = {"None", "Bow", "Crossbow", "Sling"};
  public static final String[] s_abilityuse = {"", "Weapon slots", "", "Item slots", "Gem?"};
  public static final String[] s_recharge = {
    "No flags set", "Add strength bonus", "Breakable", "EE: Damage strength bonus",
    "EE: THAC0 strength bonus", "", "", "", "", "", "",
    "Hostile", "Recharge after resting", "", "", "", "", "Bypass armor", "Keen edge", "",
    "", "", "", "", "", "", "Ex: Toggle backstab", "Ex: Cannot target invisible"};

  Ability() throws Exception
  {
    super(null, "Item ability", new byte[56], 0);
  }

  Ability(AbstractStruct superStruct, byte buffer[], int offset, int number) throws Exception
  {
    super(superStruct, "Item ability " + number, buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  @Override
  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Effect()};
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
    if (Profile.getEngine() == Profile.Engine.BG2 || Profile.isEnhancedEdition()) {
      addField(new Bitmap(buffer, offset, 1, "Type", s_type));
      addField(new Bitmap(buffer, offset + 1, 1, "Identify to use?", s_yesno));
      addField(new Bitmap(buffer, offset + 2, 1, "Ability location", s_abilityuse));
      addField(new DecNumber(buffer, offset + 3, 1, "Alternate dice size"));
      addField(new ResourceRef(buffer, offset + 4, "Icon", "BAM"));
      addField(new Bitmap(buffer, offset + 12, 1, "Target", s_targettype));
      addField(new UnsignDecNumber(buffer, offset + 13, 1, "# targets"));
      addField(new DecNumber(buffer, offset + 14, 2, "Range (feet)"));
      addField(new Bitmap(buffer, offset + 16, 1, "Launcher required", s_launcher));
      addField(new DecNumber(buffer, offset + 17, 1, "Alternate # dice thrown"));
      addField(new DecNumber(buffer, offset + 18, 1, "Speed"));
      addField(new DecNumber(buffer, offset + 19, 1, "Alternate damage bonus"));
      addField(new DecNumber(buffer, offset + 20, 2, "Bonus to hit"));
      addField(new DecNumber(buffer, offset + 22, 1, "Dice size"));
      addField(new Bitmap(buffer, offset + 23, 1, "Primary type (school)", SplResource.s_school));
      addField(new DecNumber(buffer, offset + 24, 1, "# dice thrown"));
      addField(new SecType2daBitmap(buffer, offset + 25, 1, "Secondary type"));
    }
    else {
      addField(new Bitmap(buffer, offset, 1, "Type", s_type));
      addField(new Bitmap(buffer, offset + 1, 1, "Identify to use?", s_yesno));
      addField(new Bitmap(buffer, offset + 2, 2, "Ability location", s_abilityuse));
      addField(new ResourceRef(buffer, offset + 4, "Icon", "BAM"));
      addField(new Bitmap(buffer, offset + 12, 2, "Target", s_targettype));
      addField(new DecNumber(buffer, offset + 14, 2, "Range (feet)"));
      addField(new Bitmap(buffer, offset + 16, 2, "Launcher required", s_launcher));
      addField(new DecNumber(buffer, offset + 18, 2, "Speed"));
      addField(new DecNumber(buffer, offset + 20, 2, "Bonus to hit"));
      addField(new DecNumber(buffer, offset + 22, 2, "Dice size"));
      addField(new DecNumber(buffer, offset + 24, 2, "# dice thrown"));
    }
    addField(new DecNumber(buffer, offset + 26, 2, "Damage bonus"));
    addField(new Bitmap(buffer, offset + 28, 2, "Damage type", s_dmgtype));
    addField(new SectionCount(buffer, offset + 30, 2, "# effects", Effect.class));
    addField(new DecNumber(buffer, offset + 32, 2, "First effect index"));
    addField(new DecNumber(buffer, offset + 34, 2, "# charges"));
    addField(new Bitmap(buffer, offset + 36, 2, "When drained", s_drain));
    addField(new Flag(buffer, offset + 38, 4, "Flags", s_recharge));
    if (ResourceFactory.resourceExists("PROJECTL.IDS")) {
      addField(new ProRef(buffer, offset + 42, "Projectile"));
    } else if (Profile.getEngine() == Profile.Engine.PST) {
      addField(new Bitmap(buffer, offset + 42, 2, "Projectile", s_proj_pst));
    } else if (Profile.getEngine() == Profile.Engine.IWD || Profile.getEngine() == Profile.Engine.IWD2) {
      addField(new Bitmap(buffer, offset + 42, 2, "Projectile", s_proj_iwd));
    } else {
      addField(new Bitmap(buffer, offset + 42, 2, "Projectile", s_projectile));
    }
    addField(new DecNumber(buffer, offset + 44, 2, "Animation: Overhand swing %"));
    addField(new DecNumber(buffer, offset + 46, 2, "Animation: Backhand swing %"));
    addField(new DecNumber(buffer, offset + 48, 2, "Animation: Thrust %"));
    addField(new Bitmap(buffer, offset + 50, 2, "Is arrow?", s_yesno));
    addField(new Bitmap(buffer, offset + 52, 2, "Is bolt?", s_yesno));
    addField(new Bitmap(buffer, offset + 54, 2, "Is bullet?", s_yesno));

    return offset + 56;
  }
}

