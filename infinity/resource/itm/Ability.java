// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.itm;

import infinity.datatype.*;
import infinity.resource.*;
import infinity.resource.spl.SplResource;

import javax.swing.*;

final class Ability extends AbstractAbility implements AddRemovable, HasAddRemovable, HasDetailViewer
{
  private static final String[] s_yesno = {"No", "Yes"};
  private static final String[] s_drain = {"Item remains", "Item vanishes", "Replace with used up", "Item recharges"};
  private static final String[] s_launcher = {"None", "Bow", "Crossbow", "Sling"};
  private static final String[] s_abilityuse = {"", "Weapon slots", "", "Item slots", "Gem?"};
  private static final String[] s_recharge = {"No flags set", "Add strength bonus", "Breakable", "", "",
                                              "", "", "", "", "", "", "Hostile", "Recharge after resting",
                                              "", "", "", "", "Bypass armor", "Keen edge", "", "", "", "", "", "",
                                              "", "Ex: toggle backstab", "Ex: cannot target invisible"};

  Ability() throws Exception
  {
    super(null, "Item ability", new byte[56], 0);
  }

  Ability(AbstractStruct superStruct, byte buffer[], int offset) throws Exception
  {
    super(superStruct, "Item ability", buffer, offset);
  }

// --------------------- Begin Interface HasAddRemovable ---------------------

  public AddRemovable[] getAddRemovables() throws Exception
  {
    return new AddRemovable[]{new Effect()};
  }

// --------------------- End Interface HasAddRemovable ---------------------


// --------------------- Begin Interface HasDetailViewer ---------------------

  public JComponent getDetailViewer()
  {
    return new ViewerAbility(this);
  }

// --------------------- End Interface HasDetailViewer ---------------------

  protected int read(byte buffer[], int offset) throws Exception
  {
    if (ResourceFactory.getGameID() == ResourceFactory.ID_BG2 ||
        ResourceFactory.getGameID() == ResourceFactory.ID_BG2TOB ||
        ResourceFactory.getGameID() == ResourceFactory.ID_TUTU) {
      list.add(new Bitmap(buffer, offset, 1, "Type", s_type));
      list.add(new Bitmap(buffer, offset + 1, 1, "Identify to use?", s_yesno));
      list.add(new Bitmap(buffer, offset + 2, 1, "Ability location", s_abilityuse));
      list.add(new DecNumber(buffer, offset + 3, 1, "Alternate dice size"));
      list.add(new ResourceRef(buffer, offset + 4, "Icon", "BAM"));
      list.add(new Bitmap(buffer, offset + 12, 1, "Target", s_targettype));
      list.add(new UnsignDecNumber(buffer, offset + 13, 1, "# targets"));
      list.add(new DecNumber(buffer, offset + 14, 2, "Range (feet)"));
      list.add(new Bitmap(buffer, offset + 16, 1, "Launcher required", s_launcher));
      list.add(new DecNumber(buffer, offset + 17, 1, "Alternate # dice thrown"));
      list.add(new DecNumber(buffer, offset + 18, 1, "Speed"));
      list.add(new DecNumber(buffer, offset + 19, 1, "Alternate damage bonus"));
      list.add(new DecNumber(buffer, offset + 20, 2, "Bonus to hit"));
      list.add(new DecNumber(buffer, offset + 22, 1, "Dice size"));
      list.add(new Bitmap(buffer, offset + 23, 1, "Primary type (school)", SplResource.s_school));
      list.add(new DecNumber(buffer, offset + 24, 1, "# dice thrown"));
      list.add(new Bitmap(buffer, offset + 25, 1, "Secondary type", SplResource.s_category));
    }
    else {
      list.add(new Bitmap(buffer, offset, 1, "Type", s_type));
      list.add(new Bitmap(buffer, offset + 1, 1, "Identify to use?", s_yesno));
      list.add(new Bitmap(buffer, offset + 2, 2, "Ability location", s_abilityuse));
      list.add(new ResourceRef(buffer, offset + 4, "Icon", "BAM"));
      list.add(new Bitmap(buffer, offset + 12, 2, "Target", s_targettype));
      list.add(new DecNumber(buffer, offset + 14, 2, "Range (feet)"));
      list.add(new Bitmap(buffer, offset + 16, 2, "Launcher required", s_launcher));
      list.add(new DecNumber(buffer, offset + 18, 2, "Speed"));
      list.add(new DecNumber(buffer, offset + 20, 2, "Bonus to hit"));
      list.add(new DecNumber(buffer, offset + 22, 2, "Dice size"));
      list.add(new DecNumber(buffer, offset + 24, 2, "# dice thrown"));
    }
    list.add(new DecNumber(buffer, offset + 26, 2, "Damage bonus"));
    list.add(new Bitmap(buffer, offset + 28, 2, "Damage type", s_dmgtype));
    list.add(new SectionCount(buffer, offset + 30, 2, "# effects", Effect.class));
    list.add(new DecNumber(buffer, offset + 32, 2, "First effect index"));
    list.add(new DecNumber(buffer, offset + 34, 2, "# charges"));
    list.add(new Bitmap(buffer, offset + 36, 2, "When drained", s_drain));
//    list.add(new Unknown(buffer, offset + 37, 1));
//    list.add(new Bitmap(buffer, offset + 38, 1, "Allow strength bonus?", s_yesno));
    list.add(new Flag(buffer, offset + 38, 4, "Flags", s_recharge));
//    if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
//      list.add(
//              new Bitmap(buffer, offset + 40, 2, "Attack type",
//                         new String[]{"Normal", "Bypass armour", "Keen"}));
//    else
//      list.add(new Unknown(buffer, offset + 40, 2));
    if (ResourceFactory.getInstance().resourceExists("PROJECTL.IDS"))
      list.add(new ProRef(buffer, offset + 42, "Projectile"));
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_TORMENT)
      list.add(new Bitmap(buffer, offset + 42, 2, "Projectile", s_proj_pst));
    else if (ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOW ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWINDHOWTOT ||
             ResourceFactory.getGameID() == ResourceFactory.ID_ICEWIND2)
      list.add(new Bitmap(buffer, offset + 42, 2, "Projectile", s_proj_iwd));
    else
      list.add(new Bitmap(buffer, offset + 42, 2, "Projectile", s_projectile));
    list.add(new DecNumber(buffer, offset + 44, 2, "Animation: Overhand swing %"));
    list.add(new DecNumber(buffer, offset + 46, 2, "Animation: Backhand swing %"));
    list.add(new DecNumber(buffer, offset + 48, 2, "Animation: Thrust %"));
    list.add(new Bitmap(buffer, offset + 50, 2, "Is arrow?", s_yesno));
    list.add(new Bitmap(buffer, offset + 52, 2, "Is bolt?", s_yesno));
    list.add(new Bitmap(buffer, offset + 54, 2, "Is bullet?", s_yesno));

    return offset + 56;
  }
}

