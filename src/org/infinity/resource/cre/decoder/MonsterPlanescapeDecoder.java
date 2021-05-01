// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TreeMap;

import org.infinity.datatype.IsNumeric;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.cre.decoder.util.AnimationInfo;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.resource.cre.decoder.util.DirDef;
import org.infinity.resource.cre.decoder.util.Direction;
import org.infinity.resource.cre.decoder.util.SegmentDef;
import org.infinity.resource.cre.decoder.util.SeqDef;
import org.infinity.resource.cre.decoder.util.Sequence;
import org.infinity.resource.cre.decoder.util.SpriteUtils;
import org.infinity.resource.graphics.BamV1Decoder;
import org.infinity.resource.graphics.BamV1Decoder.BamV1Control;
import org.infinity.resource.graphics.BamV1Decoder.BamV1FrameEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapSection;
import org.infinity.util.Misc;

/**
 * Creature animation decoder for processing type F000 (monster_planescape) animations.
 * Available ranges: [f000,ffff]
 */
public class MonsterPlanescapeDecoder extends SpriteDecoder
{
  /** The animation type associated with this class definition. */
  public static final AnimationInfo.Type ANIMATION_TYPE = AnimationInfo.Type.MONSTER_PLANESCAPE;

  public static final DecoderAttribute KEY_BESTIARY         = DecoderAttribute.with("bestiary", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_CLOWN            = DecoderAttribute.with("clown", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_ARMOR            = DecoderAttribute.with("armor", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_WALKSCALE        = DecoderAttribute.with("walkscale", DecoderAttribute.DataType.DECIMAL);
  public static final DecoderAttribute KEY_RUNSCALE         = DecoderAttribute.with("runscale", DecoderAttribute.DataType.DECIMAL);
  public static final DecoderAttribute KEY_COLOR_LOCATION   = DecoderAttribute.with("color", DecoderAttribute.DataType.USERDEFINED, new int[0]);
  public static final DecoderAttribute KEY_SHADOW           = DecoderAttribute.with("shadow", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_RESREF2          = DecoderAttribute.with("resref2", DecoderAttribute.DataType.STRING);

  protected final BeforeSourceBam FN_BEFORE_SRC_BAM = new BeforeSourceBam() {
    @Override
    public void accept(BamV1Control control, SegmentDef sd)
    {
      String resref = sd.getEntry().getResourceRef();

      // fix hardcoded "Pillar of Skulls" shadow center: shift by: [193.59]
      if (resref.equalsIgnoreCase("POSSHAD")) {
        BamV1Decoder decoder = control.getDecoder();
        for (int idx = 0, count = decoder.frameCount(); idx < count; idx++) {
          BamV1FrameEntry entry = decoder.getFrameInfo(idx);
          entry.resetCenter();
          entry.setCenterX(entry.getCenterX() + 193);
          entry.setCenterY(entry.getCenterY() + 59);
        }
      }

      // "Pillar of Skulls" uses separate shadow animation
      if (!resref.equalsIgnoreCase("POSMAIN")) {
        SpriteUtils.fixShadowColor(control, isTransparentShadow());
      }

      if (isPaletteReplacementEnabled() && isFalseColor()) {
        applyFalseColors(control, sd);
      }

      if ((isTranslucencyEnabled() && isTranslucent()) ||
          (isBlurEnabled() && isBlurred())) {
        int minVal = (isBlurEnabled() && isBlurred()) ? 64 : 255;
        applyTranslucency(control, minVal);
      }
    }
  };

  // available animation slot names
  private static final HashMap<Sequence, String> Slots = new HashMap<Sequence, String>() {{
    put(Sequence.PST_ATTACK1, "attack1");
    put(Sequence.PST_ATTACK2, "attack2");
    put(Sequence.PST_ATTACK3, "attack3");
    put(Sequence.PST_GET_HIT, "gethit");
    put(Sequence.PST_RUN, "run");
    put(Sequence.PST_WALK, "walk");
    put(Sequence.PST_SPELL1, "spell1");
    put(Sequence.PST_SPELL2, "spell2");
    put(Sequence.PST_SPELL3, "spell3");
    put(Sequence.PST_GET_UP, "getup");
    put(Sequence.PST_DIE_FORWARD, "dieforward");
    put(Sequence.PST_DIE_BACKWARD, "diebackward");
    put(Sequence.PST_DIE_COLLAPSE, "diecollapse");
    put(Sequence.PST_TALK1, "talk1");
    put(Sequence.PST_TALK2, "talk2");
    put(Sequence.PST_TALK3, "talk3");
    put(Sequence.PST_STAND_FIDGET1, "standfidget1");
    put(Sequence.PST_STAND_FIDGET2, "standfidget2");
    put(Sequence.PST_STANCE_FIDGET1, "stancefidget1");
    put(Sequence.PST_STANCE_FIDGET2, "stancefidget2");
    put(Sequence.PST_STAND, "stand");
    put(Sequence.PST_STANCE, "stance");
    put(Sequence.PST_STANCE_TO_STAND, "stance2stand");
    put(Sequence.PST_STAND_TO_STANCE, "stand2stance");
    put(Sequence.PST_MISC1, "misc1");
    put(Sequence.PST_MISC2, "misc2");
    put(Sequence.PST_MISC3, "misc3");
    put(Sequence.PST_MISC4, "misc4");
    put(Sequence.PST_MISC5, "misc5");
    put(Sequence.PST_MISC6, "misc6");
    put(Sequence.PST_MISC7, "misc7");
    put(Sequence.PST_MISC8, "misc8");
    put(Sequence.PST_MISC9, "misc9");
    put(Sequence.PST_MISC10, "misc10");
    put(Sequence.PST_MISC11, "misc11");
    put(Sequence.PST_MISC12, "misc12");
    put(Sequence.PST_MISC13, "misc13");
    put(Sequence.PST_MISC14, "misc14");
    put(Sequence.PST_MISC15, "misc15");
    put(Sequence.PST_MISC16, "misc16");
    put(Sequence.PST_MISC17, "misc17");
    put(Sequence.PST_MISC18, "misc18");
    put(Sequence.PST_MISC19, "misc19");
    put(Sequence.PST_MISC20, "misc20");
  }};

  // action prefixes used to determine BAM resref for animation sequences
  private static final HashMap<Sequence, String> ActionPrefixes = new HashMap<Sequence, String>() {{
    put(Sequence.PST_ATTACK1, "at1");
    put(Sequence.PST_ATTACK2, "at2");
    put(Sequence.PST_ATTACK3, "at2");
    put(Sequence.PST_GET_HIT, "hit");
    put(Sequence.PST_RUN, "run");
    put(Sequence.PST_WALK, "wlk");
    put(Sequence.PST_SPELL1, "sp1");
    put(Sequence.PST_SPELL2, "sp2");
    put(Sequence.PST_SPELL3, "sp3");
    put(Sequence.PST_GET_UP, "gup");
    put(Sequence.PST_DIE_FORWARD, "dff");
    put(Sequence.PST_DIE_BACKWARD, "dfb");
    put(Sequence.PST_DIE_COLLAPSE, "dcl");
    put(Sequence.PST_TALK1, "tk1");
    put(Sequence.PST_TALK2, "tk2");
    put(Sequence.PST_TALK3, "tk3");
    put(Sequence.PST_STAND_FIDGET1, "sf1");
    put(Sequence.PST_STAND_FIDGET2, "sf2");
    put(Sequence.PST_STANCE_FIDGET1, "cf1");
    put(Sequence.PST_STANCE_FIDGET2, "cf2");
    put(Sequence.PST_STAND, "std");
    put(Sequence.PST_STANCE, "stc");
    put(Sequence.PST_STANCE_TO_STAND, "c2s");
    put(Sequence.PST_STAND_TO_STANCE, "s2c");
    put(Sequence.PST_MISC1, "ms1");
    put(Sequence.PST_MISC2, "ms2");
    put(Sequence.PST_MISC3, "ms3");
    put(Sequence.PST_MISC4, "ms4");
    put(Sequence.PST_MISC5, "ms5");
    put(Sequence.PST_MISC6, "ms6");
    put(Sequence.PST_MISC7, "ms7");
    put(Sequence.PST_MISC8, "ms8");
    put(Sequence.PST_MISC9, "ms9");
    // guessed action prefixes
    put(Sequence.PST_MISC10, "msa");
    put(Sequence.PST_MISC11, "msb");
    put(Sequence.PST_MISC12, "msc");
    put(Sequence.PST_MISC13, "msd");
    put(Sequence.PST_MISC14, "mse");
    put(Sequence.PST_MISC15, "msf");
    put(Sequence.PST_MISC16, "msg");
    put(Sequence.PST_MISC17, "msh");
    put(Sequence.PST_MISC18, "msi");
    put(Sequence.PST_MISC19, "msj");
    put(Sequence.PST_MISC20, "msk");
  }};

  /**
   * Returns the action command associated with the specified action sequence.
   * @param seq the action sequence.
   * @return the action command, {@code null} otherwise.
   */
  public static String getActionCommand(Sequence seq)
  {
    return Slots.get(seq);
  }

  /**
   * Returns the action prefix associated with the command of the associated action sequence
   * according to the default naming scheme.
   * @param seq the action sequence.
   * @return the action prefix, {@code null} otherwise.
   */
  public static String getActionPrefix(Sequence seq)
  {
    return ActionPrefixes.get(seq);
  }

  /**
   * A helper method that parses the specified data array and generates a {@link IniMap} instance out of it.
   * @param data a String array containing table values for a specific table entry.
   * @return a {@code IniMap} instance with the value derived from the specified data array.
   *         Returns {@code null} if no data could be derived.
   */
  public static IniMap processTableData(String[] data)
  {
    IniMap retVal = null;
    if (data == null || data.length < 9) {
      return retVal;
    }

    int id = SpriteTables.valueToInt(data, SpriteTables.COLUMN_ID, -1);
    if (id < 0) {
      return retVal;
    }
    boolean isSpecial = (id >= 0x1000);

    String s = SpriteTables.valueToString(data, SpriteTables.COLUMN_PST_RESREF, "");
    if (s.isEmpty()) {
      return retVal;
    }
    String prefix = isSpecial ? "" : s.substring(0, 1);
    String resref = isSpecial ? s : s.substring(1, s.length() - 1);
    String suffix = isSpecial ? "" : s.substring(s.length() - 1, s.length());
    TreeMap<String, String> actions = new TreeMap<>();
    if (isSpecial) {
      actions.put(Slots.get(Sequence.PST_STAND), resref);
    } else {
      for (final Sequence seq : Slots.keySet()) {
        String action = Slots.get(seq);
        String actionPrefix = ActionPrefixes.get(seq);
        if (action != null && actionPrefix != null) {
          String bamRes = prefix + actionPrefix + resref;
          if (ResourceFactory.resourceExists(bamRes + suffix + ".BAM")) {
            actions.put(action, bamRes);
          }
        }
      }
      if (actions.isEmpty()) {
        if (ResourceFactory.resourceExists(prefix + resref + suffix + ".BAM")) {
          actions.put(Slots.get(Sequence.PST_STAND), prefix + resref);
        }
      }
    }
    int clown = SpriteTables.valueToInt(data, SpriteTables.COLUMN_PST_CLOWN, 0);
    int bestiary = SpriteTables.valueToInt(data, SpriteTables.COLUMN_PST_BESTIARY, 0);
    int armor = SpriteTables.valueToInt(data, SpriteTables.COLUMN_PST_ARMOR, 0);

    List<String> lines = SpriteUtils.processTableDataGeneralPst(data);
    lines.add("[monster_planescape]");
    for (final String action : actions.keySet()) {
      lines.add(action + "=" + actions.get(action));
    }
    if (clown != 0) {
      lines.add("clown=" + clown);
      for (int i = 0; i < clown; i++) {
        lines.add("color" + (i + 1) + "=" + (128 + i * 16));
      }
    }
    if (bestiary != 0) {
      lines.add("bestiary=" + bestiary);
    }
    if (armor != 0) {
      lines.add("armor=" + armor);
    }

    retVal = IniMap.from(lines);

    return retVal;
  }

  public MonsterPlanescapeDecoder(int animationId, IniMap ini) throws Exception
  {
    super(ANIMATION_TYPE, animationId, ini);
  }

  public MonsterPlanescapeDecoder(CreResource cre) throws Exception
  {
    super(ANIMATION_TYPE, cre);
    setTransparentShadow(false);
  }

  /** Returns the bestiary entry index. */
  public int getBestiaryIndex() { return getAttribute(KEY_BESTIARY); }
  protected void setBestiaryIndex(int v) { setAttribute(KEY_BESTIARY, v); }

  /** ??? */
  public int getArmor() { return getAttribute(KEY_ARMOR); }
  protected void setArmor(int v) { setAttribute(KEY_ARMOR, v); }

  /** Returns the walking speed of the creature animation. */
  public double getWalkScale() { return getAttribute(KEY_WALKSCALE); }
  protected void setWalkScale(double d) { setAttribute(KEY_WALKSCALE, d); }

  /** Returns the running speed of the creature animation. */
  public double getRunScale() { return getAttribute(KEY_RUNSCALE); }
  protected void setRunScale(double d) { setAttribute(KEY_RUNSCALE, d); }

  @Override
  protected int getColorOffset(int locationIndex)
  {
    int retVal = -1;
    int numLocations = getColorLocationCount();
    if (locationIndex >= 0 && locationIndex < numLocations) {
      retVal = 256 - (numLocations - locationIndex) * 32;
    }
    return retVal;
  }

  /** Returns the number of available false color ranges. */
  public int getColorLocationCount()
  {
    List<Integer> color = getAttribute(KEY_COLOR_LOCATION);
    return color.size();
  }

  /** Returns the specified color location. */
  public int getColorLocation(int index) throws IndexOutOfBoundsException
  {
    List<Integer> color = getAttribute(KEY_COLOR_LOCATION);
    if (index < 0 || index >= color.size()) {
      throw new IndexOutOfBoundsException();
    }
    return color.get(index);
  }

  /** Returns a list of all valid color locations. */
  private List<Integer> setColorLocations(IniMapSection section)
  {
    List<Integer> retVal = null;
    if (Profile.getGame() == Profile.Game.PST) {
      retVal = setColorLocationsCre();
      if (retVal.isEmpty()) {
        retVal = setColorLocationsIni(section);
      }
    } else {
      retVal = setColorLocationsIni(section);
    }
    return retVal;
  }

  /** Returns a list of all valid color locations defined in the associated INI file. (PSTEE) */
  private List<Integer> setColorLocationsIni(IniMapSection section)
  {
    final HashSet<Integer> usedColors = new HashSet<>();
    List<Integer> retVal = new ArrayList<>(7);
    for (int i = 0; i < 7; i++) {
      int v = section.getAsInteger(KEY_COLOR_LOCATION.getName() + (i + 1), 0);
      if (v >= 128 && v < 240 && !usedColors.contains(v & 0xf0)) {
        usedColors.add(v & 0xf0);
        retVal.add(Integer.valueOf(v));
      }
    }
    return retVal;
  }

  /** Returns a list of all valid color locations from the CRE resource. (original PST) */
  private List<Integer> setColorLocationsCre()
  {
    final HashSet<Integer> usedColors = new HashSet<>();
    CreResource cre = getCreResource();
    int numColors = Math.max(0, Math.min(7, ((IsNumeric)cre.getAttribute(CreResource.CRE_NUM_COLORS)).getValue()));
    List<Integer> retVal = new ArrayList<>(numColors);
    for (int i = 0; i < numColors; i++) {
      int l = ((IsNumeric)cre.getAttribute(String.format(CreResource.CRE_COLOR_PLACEMENT_FMT, i + 1))).getValue();
      if (l > 0 && !usedColors.contains(l & 0xf0)) {
        usedColors.add(l & 0xf0);
        retVal.add(Integer.valueOf(l));
      }
    }
    return retVal;
  }

  @Override
  public List<String> getAnimationFiles(boolean essential)
  {
    ArrayList<String> retVal = new ArrayList<>();
    IniMapSection section = getAnimationInfo().getSection(getAnimationSectionName());
    for (final HashMap.Entry<Sequence, String> e : Slots.entrySet()) {
      String resref = section.getAsString(e.getValue(), "");
      if (!resref.isEmpty()) {
        resref = resref.toUpperCase(Locale.ENGLISH);
        if (ResourceFactory.resourceExists(resref + "B.BAM")) {
          retVal.add(resref + "B.BAM");
        } else if (ResourceFactory.resourceExists(resref + ".BAM")) {
          retVal.add(resref + ".BAM");
        }
      }
    }
    return retVal;
  }

  @Override
  public boolean isSequenceAvailable(Sequence seq)
  {
    return (getSequenceDefinition(seq) != null);
  }

  @Override
  protected void initDefaults(IniMap ini) throws Exception
  {
    IniMapSection section = getGeneralIniSection(Objects.requireNonNull(ini, "INI object cannot be null"));

    Objects.requireNonNull(section.getAsString(KEY_ANIMATION_TYPE.getName()), "animation_type required");
    Misc.requireCondition(getAnimationType().contains(getAnimationId()),
                          String.format("Animation slot (%04X) is not compatible with animation type (%s)",
                                        getAnimationId(), getAnimationType().toString()));

    setMoveScale(section.getAsDouble(KEY_MOVE_SCALE.getName(), 0.0));
    setEllipse(section.getAsInteger(KEY_ELLIPSE.getName(), 16));
    setPersonalSpace(section.getAsInteger(KEY_PERSONAL_SPACE.getName(), 3));
  }

  @Override
  protected void init() throws Exception
  {
    // setting properties
    initDefaults(getAnimationInfo());
    IniMapSection section = getSpecificIniSection();
    setFalseColor(section.getAsInteger(KEY_CLOWN.getName(), 0) != 0);
    setBestiaryIndex(section.getAsInteger(KEY_BESTIARY.getName(), 0));
    setArmor(section.getAsInteger(KEY_ARMOR.getName(), 0));
    setWalkScale(section.getAsDouble(KEY_WALKSCALE.getName(), getMoveScale()));
    setRunScale(section.getAsDouble(KEY_RUNSCALE.getName(), getWalkScale()));
    setAttribute(KEY_COLOR_LOCATION, setColorLocations(section));

    // hardcoded stuff
    String resref = section.getAsString(Slots.get(Sequence.PST_STAND), "");
    if ("POSMAIN".equalsIgnoreCase(resref)) {
      // Pillar of Skulls: relocate shadow center
      setAttribute(KEY_SHADOW, "POSSHAD");
    }
//    if ("IGHEAD".equalsIgnoreCase(resref)) {
//      // "Coaxmetal" (iron golem): relocate center of arm segments
//      setAttribute(KEY_RESREF2, "IGARM");
//    }
    if ((getAnimationId() & 0x0fff) == 0x000e) {
      // Deionarra
      setBrightest(true);
      setLightSource(true); // TODO: confirm
    }
  }

  @Override
  protected void createSequence(Sequence seq, Direction[] directions) throws Exception
  {
    SeqDef sd = Objects.requireNonNull(getSequenceDefinition(seq), "Sequence not available: " + (seq != null ? seq : "(null)"));
    if (directions == null) {
      directions = Direction.values();
    }
    createAnimation(sd, Arrays.asList(directions), FN_BEFORE_SRC_BAM, FN_BEFORE_SRC_FRAME, FN_AFTER_SRC_FRAME, FN_AFTER_DST_FRAME);
  }

  @Override
  protected SeqDef getSequenceDefinition(Sequence seq)
  {
    SeqDef retVal = null;
    IniMapSection section = getAnimationInfo().getSection(getAnimationSectionName());
    if (Slots.containsKey(seq)) {
      ArrayList<ResourceEntry> resrefList = new ArrayList<>();

      // Shadow resref?
      String resref = getAttribute(KEY_SHADOW);
      if (!resref.isEmpty()) {
        ResourceEntry entry = ResourceFactory.getResourceEntry(resref + ".BAM");
        if (entry != null) {
          resrefList.add(entry);
        }
      }

      // Sprite resref
      String name = Slots.get(seq);
      resref = section.getAsString(name, "");
      if (resref.isEmpty()) {
        return retVal;
      } else {
        ResourceEntry entry = ResourceFactory.getResourceEntry(resref.toUpperCase(Locale.ENGLISH) + "B.BAM");
        if (entry == null) {
          entry = ResourceFactory.getResourceEntry(resref.toUpperCase(Locale.ENGLISH) + ".BAM");
        }
        if (entry != null) {
          resrefList.add(entry);
        }
      }

      // Secondary sprite?
      resref = getAttribute(KEY_RESREF2);
      if (!resref.isEmpty()) {
        ResourceEntry entry = ResourceFactory.getResourceEntry(resref + ".BAM");
        if (entry != null) {
          resrefList.add(entry);
        }
      }

      retVal = new SeqDef(seq);
      for (final ResourceEntry entry : resrefList) {
        // determining number of directions
        BamV1Control ctrl = SpriteUtils.loadBamController(entry);
        int dirCount = ctrl.cycleCount();
        Direction[] directions = null;
        Direction[] directionsE = null;
        boolean mirror = true;
        if (dirCount < SeqDef.DIR_REDUCED_W.length) {
          // special: no individual directions
          mirror = false;
          directions = Arrays.copyOfRange(SeqDef.DIR_REDUCED_W, 0, dirCount);
        } else if (dirCount == SeqDef.DIR_REDUCED_W.length) {
          // reduced directions, mirrored east
          directions = SeqDef.DIR_REDUCED_W;
          directionsE = SeqDef.DIR_REDUCED_E;
        } else if (dirCount == SeqDef.DIR_REDUCED.length) {
          // special case: reduced directions, not mirrored
          mirror = false;
          directions = new Direction[SeqDef.DIR_REDUCED.length];
          System.arraycopy(SeqDef.DIR_REDUCED_W, 0, directions, 0, SeqDef.DIR_REDUCED_W.length);
          System.arraycopy(SeqDef.DIR_REDUCED_E, 0, directions, SeqDef.DIR_REDUCED_W.length, SeqDef.DIR_REDUCED_E.length);
        } else if (dirCount == SeqDef.DIR_FULL_W.length) {
          // full directions, mirrored east
          directions = SeqDef.DIR_FULL_W;
          directionsE = SeqDef.DIR_FULL_E;
        } else if (dirCount == SeqDef.DIR_FULL.length) {
          // full directions, not mirrored
          mirror = false;
          directions = SeqDef.DIR_FULL;
        }

        if (directions != null && SpriteUtils.bamCyclesExist(entry, 0, directions.length)) {
          SeqDef tmp = SeqDef.createSequence(seq, directions, false, entry, 0, null);
          retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
          if (mirror) {
            tmp = SeqDef.createSequence(seq, directionsE, true, entry, 1, null);
            retVal.addDirections(tmp.getDirections().toArray(new DirDef[tmp.getDirections().size()]));
          }
        }
      }

      if (retVal.isEmpty()) {
        retVal = null;
      }
    }

    return retVal;
  }
}
