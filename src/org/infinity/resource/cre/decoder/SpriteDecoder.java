// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.infinity.datatype.IsNumeric;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.cre.decoder.internal.CreatureInfo;
import org.infinity.resource.cre.decoder.internal.CycleDef;
import org.infinity.resource.cre.decoder.internal.DecoderAttribute;
import org.infinity.resource.cre.decoder.internal.DirDef;
import org.infinity.resource.cre.decoder.internal.FrameInfo;
import org.infinity.resource.cre.decoder.internal.SegmentDef;
import org.infinity.resource.cre.decoder.internal.SeqDef;
import org.infinity.resource.cre.decoder.tables.SpriteTables;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.BamV1Decoder.BamV1Control;
import org.infinity.resource.graphics.BlendingComposite;
import org.infinity.resource.key.BufferedResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.IniMap;
import org.infinity.util.IniMapCache;
import org.infinity.util.IniMapEntry;
import org.infinity.util.IniMapSection;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.tuples.Couple;

/**
 * Specialized BAM decoder for creature animation sprites.
 */
public abstract class SpriteDecoder extends PseudoBamDecoder
{
  // List of general creature animation attributes
  public static final DecoderAttribute KEY_ANIMATION_TYPE     = DecoderAttribute.with("animation_type", DecoderAttribute.DataType.USERDEFINED);
  public static final DecoderAttribute KEY_ANIMATION_SECTION  = DecoderAttribute.with("animation_section", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_MOVE_SCALE         = DecoderAttribute.with("move_scale", DecoderAttribute.DataType.DECIMAL);
  public static final DecoderAttribute KEY_ELLIPSE            = DecoderAttribute.with("ellipse", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_COLOR_BLOOD        = DecoderAttribute.with("color_blood", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_COLOR_CHUNKS       = DecoderAttribute.with("color_chunks", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_SOUND_FREQ         = DecoderAttribute.with("sound_freq", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_SOUND_DEATH        = DecoderAttribute.with("sound_death", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_PERSONAL_SPACE     = DecoderAttribute.with("personal_space", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_CAST_FRAME         = DecoderAttribute.with("cast_frame", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_HEIGHT_OFFSET      = DecoderAttribute.with("height_offset", DecoderAttribute.DataType.INT);
  public static final DecoderAttribute KEY_BRIGHTEST          = DecoderAttribute.with("brightest", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_MULTIPLY_BLEND     = DecoderAttribute.with("multiply_blend", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_LIGHT_SOURCE       = DecoderAttribute.with("light_source", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_NEW_PALETTE        = DecoderAttribute.with("new_palette", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_SOUND_REF          = DecoderAttribute.with("sound_ref", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_COMBAT_ROUND_0     = DecoderAttribute.with("combat_round_0", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_COMBAT_ROUND_1     = DecoderAttribute.with("combat_round_1", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_COMBAT_ROUND_2     = DecoderAttribute.with("combat_round_2", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_COMBAT_ROUND_3     = DecoderAttribute.with("combat_round_3", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_COMBAT_ROUND_4     = DecoderAttribute.with("combat_round_4", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_WALK_SOUND         = DecoderAttribute.with("walk_sound", DecoderAttribute.DataType.STRING);
  // List of commonly used attributes specific to creature animation types
  public static final DecoderAttribute KEY_RESREF             = DecoderAttribute.with("resref", DecoderAttribute.DataType.STRING);
  public static final DecoderAttribute KEY_DETECTED_BY_INFRAVISION  = DecoderAttribute.with("detected_by_infravision", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_FALSE_COLOR        = DecoderAttribute.with("false_color", DecoderAttribute.DataType.BOOLEAN);
  public static final DecoderAttribute KEY_TRANSLUCENT        = DecoderAttribute.with("translucent", DecoderAttribute.DataType.BOOLEAN);


  // Predefined sets of games with common animation slot mappings
  private static final EnumSet<Profile.Game> TYPE_GAME_BG1      = EnumSet.of(Profile.Game.BG1, Profile.Game.BG1TotSC);
  private static final EnumSet<Profile.Game> TYPE_GAME_BG2_TOB  = EnumSet.of(Profile.Game.BG2ToB, Profile.Game.BGT, Profile.Game.Tutu);
  // all BG2 game variants
  private static final EnumSet<Profile.Game> TYPE_GAME_BG2      = EnumSet.of(Profile.Game.BG2SoA, Profile.Game.BG2ToB, Profile.Game.BGT,
                                                                             Profile.Game.Tutu);
  private static final EnumSet<Profile.Game> TYPE_GAME_IWD      = EnumSet.of(Profile.Game.IWD);
  private static final EnumSet<Profile.Game> TYPE_GAME_IWD_HOW  = EnumSet.of(Profile.Game.IWDHoW, Profile.Game.IWDHowTotLM);
  private static final EnumSet<Profile.Game> TYPE_GAME_IWD2     = EnumSet.of(Profile.Game.IWD2);
  private static final EnumSet<Profile.Game> TYPE_GAME_PST      = EnumSet.of(Profile.Game.PST);
  // all EE games
  private static final EnumSet<Profile.Game> TYPE_GAME_EE       = EnumSet.of(Profile.Game.BG1EE, Profile.Game.BG1SoD, Profile.Game.BG2EE,
                                                                             Profile.Game.EET, Profile.Game.IWDEE, Profile.Game.PSTEE);
  private static final EnumSet<Profile.Game> TYPE_GAME_PSTEE    = EnumSet.of(Profile.Game.PSTEE);
  // all games except PST
  private static final EnumSet<Profile.Game> TYPE_GAME_ALL      = EnumSet.complementOf(EnumSet.of(Profile.Game.Unknown, Profile.Game.PST));

  // Predefined slot ranges for specific animation types
  private static final List<Range> RANGE_EFFECT                 = Arrays.asList(new Range(0, 0xfff));
  private static final List<Range> RANGE_MONSTER_QUADRANT       = Arrays.asList(new Range(0x1000, 0x1ff));
  private static final List<Range> RANGE_MONSTER_MULTI          = Arrays.asList(new Range(0x1200, 0xff),
                                                                                new Range(0x1400, 0xbff));
  private static final List<Range> RANGE_MONSTER_MULTI_NEW      = Arrays.asList(new Range(0x1300, 0xff));
  private static final List<Range> RANGE_MONSTER_LAYERED        = Arrays.asList(new Range(0x8000, 0xfff));
  private static final List<Range> RANGE_MONSTER_LAYERED_SPELL  = Arrays.asList(new Range(0x2000, 0xfff));
  private static final List<Range> RANGE_MONSTER_ANKHEG         = Arrays.asList(new Range(0x3000, 0xfff));
  private static final List<Range> RANGE_TOWN_STATIC            = Arrays.asList(new Range(0x4000, 0xfff));
  private static final List<Range> RANGE_CHARACTER              = Arrays.asList(new Range(0x5000, 0x3ff),
                                                                                new Range(0x5500, 0xff),
                                                                                new Range(0x6000, 0x3ff),
                                                                                new Range(0x6500, 0xff));
  private static final List<Range> RANGE_CHARACTER_IA           = Arrays.asList(new Range(0x6600, 0x4ff));
  private static final List<Range> RANGE_CHARACTER_OLD          = Arrays.asList(new Range(0x5400, 0xff),
                                                                                new Range(0x5600, 0x9ff),
                                                                                new Range(0x6400, 0xff),
                                                                                new Range(0x6600, 0x9ff));
  private static final List<Range> RANGE_MONSTER                = Arrays.asList(new Range(0x7002, 0xd, 0x00, 0x1f, 4),
                                                                                new Range(0x7004, 0xb, 0x20, 0x0f, 4),
                                                                                new Range(0x7000, 0xf, 0x30, 0x0f, 4),
                                                                                new Range(0x7003, 0xc, 0x40, 0x0f, 4),
                                                                                new Range(0x7002, 0xd, 0x50, 0x0f, 4),
                                                                                new Range(0x7003, 0xc, 0x70, 0x0f, 4),
                                                                                new Range(0x7005, 0xa, 0x90, 0x1f, 4),
                                                                                new Range(0x7007, 0x8, 0xb0, 0x0f, 4),
                                                                                new Range(0x7002, 0xd, 0xc0, 0x0f, 4),
                                                                                new Range(0x7002, 0xd, 0xe0, 0x0f, 4),
                                                                                new Range(0x7000, 0xf, 0xf0, 0x0f, 4));
  private static final List<Range> RANGE_MONSTER_IA             = Arrays.asList(new Range(0x5b00, 0x4ff));
  private static final List<Range> RANGE_MONSTER_OLD            = Arrays.asList(new Range(0x7000, 0x1, 0x00, 0x1f, 4),
                                                                                new Range(0x7000, 0x3, 0x20, 0x0f, 4),
                                                                                new Range(0x7000, 0x2, 0x40, 0x0f, 4),
                                                                                new Range(0x7000, 0x1, 0x50, 0x0f, 4),
                                                                                new Range(0x7000, 0xf, 0x60, 0x0f, 4),
                                                                                new Range(0x7000, 0x2, 0x70, 0x0f, 4),
                                                                                new Range(0x7000, 0xf, 0x80, 0x0f, 4),
                                                                                new Range(0x7000, 0x4, 0x90, 0x1f, 4),
                                                                                new Range(0x7000, 0x6, 0xb0, 0x0f, 4),
                                                                                new Range(0x7000, 0x1, 0xc0, 0x0f, 4),
                                                                                new Range(0x7000, 0xf, 0xd0, 0x0f, 4),
                                                                                new Range(0x7000, 0x1, 0xe0, 0x0f, 4));
  private static final List<Range> RANGE_MONSTER_OLD_IA         = Arrays.asList(new Range(0x547a, 0x479));
  private static final List<Range> RANGE_MONSTER_LARGE          = Arrays.asList(new Range(0x9000, 0xfff));
  private static final List<Range> RANGE_MONSTER_LARGE_16       = Arrays.asList(new Range(0xa000, 0xfff));
  private static final List<Range> RANGE_AMBIENT_STATIC         = Arrays.asList(new Range(0xb000, 0xfff));
  private static final List<Range> RANGE_AMBIENT                = Arrays.asList(new Range(0xc000, 0xfff));
  private static final List<Range> RANGE_FLYING                 = Arrays.asList(new Range(0xd000, 0xfff));
  private static final List<Range> RANGE_MONSTER_ICEWIND        = Arrays.asList(new Range(0xe000, 0xfff));
  private static final List<Range> RANGE_MONSTER_ICEWIND_EX     = Arrays.asList(new Range(0xe000, 0x1fff));
  private static final List<Range> RANGE_MONSTER_ICEWIND_IA     = Arrays.asList(new Range(0x5000, 0x478));
  private static final List<Range> RANGE_MONSTER_PLANESCAPE     = Arrays.asList(new Range(0xf000, 0xfff));
  private static final List<Range> RANGE_MONSTER_PLANESCAPE_EX  = Arrays.asList(new Range(0x0000, 0xffff));

  /** Available creature animation types. */
  public enum AnimationType {
    /** Animation type: 0000 */
    EFFECT(0x0000, "effect", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_EFFECT))),                   // type=0
    /** Animation type: 1000 (slots [1000..11ff]) */
    MONSTER_QUADRANT(0x1000, "monster_quadrant", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_MONSTER_QUADRANT))),         // type=1
    /** Animation type: 1000 (slots [1200..12ff], [1400..1fff] */
    MONSTER_MULTI(0x1000, "monster_multi", Arrays.asList(
        Couple.with(TYPE_GAME_EE, RANGE_MONSTER_MULTI),
        Couple.with(TYPE_GAME_BG2, RANGE_MONSTER_MULTI),
        Couple.with(TYPE_GAME_IWD2, RANGE_MONSTER_MULTI))),           // type=2
    /** Animation type: 1000 (slots [1300..13ff]) */
    MONSTER_MULTI_NEW(0x1000, "multi_new", Arrays.asList(
        Couple.with(TYPE_GAME_EE, RANGE_MONSTER_MULTI_NEW),
        Couple.with(TYPE_GAME_BG2_TOB, RANGE_MONSTER_MULTI_NEW))),    // type=3
    /** Animation type: 8000 */
    MONSTER_LAYERED(0x8000, "monster_layered", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_MONSTER_LAYERED))),          // type=4
    /** Animation type: 2000 */
    MONSTER_LAYERED_SPELL(0x2000, "monster_layered_spell", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_MONSTER_LAYERED_SPELL))),    // type=5
    /** Animation type: 3000 */
    MONSTER_ANKHEG(0x3000, "monster_ankheg", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_MONSTER_ANKHEG))),           // type=6
    /** Animation type: 4000 */
    TOWN_STATIC(0x4000, "town_static", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_TOWN_STATIC))),              // type=7
    /** Animation types: 5000, 6000 (slots [5000..53ff], [5500..55ff], [6000..63ff], [6500..65ff]) */
    CHARACTER(new int[] {0x5000, 0x6000}, "character", Arrays.asList(
        Couple.with(TYPE_GAME_EE, RANGE_CHARACTER),
        Couple.with(TYPE_GAME_BG2, RANGE_CHARACTER),
        Couple.with(TYPE_GAME_IWD_HOW, RANGE_CHARACTER),
        Couple.with(TYPE_GAME_IWD2, RANGE_CHARACTER)),
        RANGE_CHARACTER_IA),                                          // type=8
    /** Animation types: 5000, 6000 (slots [5400..54ff], [5600..5fff], [6400..64ff], [6600..6fff]) */
    CHARACTER_OLD(new int[] {0x5000, 0x6000}, "character_old", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_CHARACTER_OLD),
        Couple.with(TYPE_GAME_BG1, RANGE_CHARACTER),
        Couple.with(TYPE_GAME_IWD, RANGE_CHARACTER))),                // type=9
    /** Animation type: 7000 (many subranges) */
    MONSTER(0x7000, "monster", Arrays.asList(
        Couple.with(TYPE_GAME_EE, RANGE_MONSTER),
        Couple.with(TYPE_GAME_BG2, RANGE_MONSTER),
        Couple.with(TYPE_GAME_IWD, RANGE_MONSTER),
        Couple.with(TYPE_GAME_IWD_HOW, RANGE_MONSTER),
        Couple.with(TYPE_GAME_IWD2, RANGE_MONSTER)),
        RANGE_MONSTER_IA),                                            // type=10
    /** Animation type: 7000 (many subranges) */
    MONSTER_OLD(0x7000, "monster_old", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_MONSTER_OLD)),
        RANGE_MONSTER_OLD_IA),                                        // type=11
    /** Animation type: 9000 */
    MONSTER_LARGE(0x9000, "monster_large", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_MONSTER_LARGE))),            // type=12
    /** Animation type: A000 */
    MONSTER_LARGE_16(0xa000, "monster_large16", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_MONSTER_LARGE_16))),         // type=13
    /** Animation type: B000 */
    AMBIENT_STATIC(0xb000, "ambient_static", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_AMBIENT_STATIC))),           // type=14
    /** Animation type: C000 */
    AMBIENT(0xc000, "ambient", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_AMBIENT))),                  // type=15
    /** Animation type: D000 */
    FLYING(0xd000, "flying", Arrays.asList(
        Couple.with(TYPE_GAME_ALL, RANGE_FLYING))),                   // type=16
    /** Animation type: E000 (for non-EE: also slots [f000..ffff]) */
    MONSTER_ICEWIND(0xe000, "monster_icewind", Arrays.asList(
        Couple.with(TYPE_GAME_EE, RANGE_MONSTER_ICEWIND),
        Couple.with(TYPE_GAME_BG2, RANGE_MONSTER_ICEWIND),
        Couple.with(TYPE_GAME_IWD, RANGE_MONSTER_ICEWIND_EX),
        Couple.with(TYPE_GAME_IWD_HOW, RANGE_MONSTER_ICEWIND_EX),
        Couple.with(TYPE_GAME_IWD2, RANGE_MONSTER_ICEWIND_EX)),
        RANGE_MONSTER_ICEWIND_IA),                                    // type=17
    /** Animation type: F000 */
    MONSTER_PLANESCAPE(0xf000, "monster_planescape", Arrays.asList(
        Couple.with(TYPE_GAME_PSTEE, RANGE_MONSTER_PLANESCAPE),
        Couple.with(TYPE_GAME_PST, RANGE_MONSTER_PLANESCAPE_EX)));    // type=18

    private final EnumMap<Profile.Game, List<Range>> rangeMap = new EnumMap<>(Profile.Game.class);
    private final List<Range> iaRanges;
    private final int[] animationTypes;
    private final String sectionName;

    /**
     * @param type slot base range
     * @param sectionName INI section name
     * @param entries list of games and their associated slot ranges.
     */
    private AnimationType(int type, String sectionName, List<Couple<EnumSet<Profile.Game>, List<Range>>> entries)
        throws IllegalArgumentException
    {
      this(new int[] {type}, sectionName, entries, null);
    }

    /**
     * @param type
     * @param sectionName
     * @param entries
     * @param infinityAnimationRanges
     * @throws IllegalArgumentException
     */
    private AnimationType(int type, String sectionName, List<Couple<EnumSet<Profile.Game>, List<Range>>> entries,
                          List<Range> infinityAnimationRanges)
        throws IllegalArgumentException
    {
      this(new int[] {type}, sectionName, entries, infinityAnimationRanges);
    }

    /**
     * @param types
     * @param sectionName
     * @param entries
     * @throws IllegalArgumentException
     */
    private AnimationType(int[] types, String sectionName, List<Couple<EnumSet<Profile.Game>, List<Range>>> entries)
        throws IllegalArgumentException
    {
      this(types, sectionName, entries, null);
    }

    /**
     * @param type list of slot base ranges
     * @param sectionName INI section name
     * @param entries list of games and their associated slot ranges.
     * @throws IllegalArgumentException
     */
    private AnimationType(int[] types, String sectionName, List<Couple<EnumSet<Profile.Game>, List<Range>>> entries,
                          List<Range> infinityAnimationRanges) throws IllegalArgumentException
    {
      try {
        Misc.requireCondition(types != null && types.length > 0, "Type cannot be empty", IllegalArgumentException.class);
        Misc.requireCondition(sectionName != null && !sectionName.isEmpty(), "Section name cannot be empty", IllegalArgumentException.class);
      } catch (IllegalArgumentException iae) {
        throw iae;
      } catch (Exception e) {
      }
      this.animationTypes = types;
      this.sectionName = sectionName;
      for (final Couple<EnumSet<Profile.Game>, List<Range>> entry : entries) {
        EnumSet<Profile.Game> games = entry.getValue0();
        List<Range> ranges = entry.getValue1();
        for (final Profile.Game game : games) {
          if (ranges.size() > 0) {
            List<Range> list = this.rangeMap.get(game);
            if (list != null) {
              list.addAll(ranges);
            } else {
              this.rangeMap.put(game, new ArrayList<>(ranges));
            }
          }
        }
      }
      this.iaRanges = infinityAnimationRanges;
    }

    /** Returns the name for the type-specific INI section. */
    public String getSectionName() { return sectionName; }

    /** Returns the first available base animation type associated with the enum instance. */
    public int getType() { return animationTypes[0]; }

    /** Returns the number of defined base animation types associated with the enum instance. */
    public int getTypeCount() { return animationTypes.length; }

    /** Returns the specified base animation type associated with the enum instance. */
    public int getType(int idx) { return animationTypes[idx]; }

    /**
     * Returns whether the specified value is covered by the ranges associated with the enum instance
     * for the current game.
     */
    public boolean contains(int value) { return contains(Profile.getGame(), value); }

    /**
     * Returns whether the specified value is covered by the ranges associated with the enum instance
     * for the specified game.
     */
    public boolean contains(Profile.Game game, int value)
    {
      if (game == null) {
        game = Profile.getGame();
      }
      boolean retVal = false;

      // Infinity Animations sprite takes precedence over regular sprite
      AnimationType type = containsInfinityAnimations(value);
      if (type != null && type != this) {
        return retVal;
      }
      retVal = (type == this);

      if (!retVal) {
        retVal = contains(value, rangeMap.get(Profile.getGame()));
      }
      return retVal;
    }

    // Checks whether specified value is covered by the given ranges
    private static boolean contains(int value, List<Range> ranges)
    {
      if (ranges != null) {
        return ranges
            .parallelStream()
            .anyMatch(r -> r.contains(value));
      }
      return false;
    }

    /**
     * Determines the {@code AnimationType} enum where a defined Infinity Animations (IA) range covers the specied value.
     * @param value the value to check.
     * @return {@code AnimationType} enum supporting the specified IA value.
     *         Returns {@code null} if value is not covered by any IA range.
     */
    public static AnimationType containsInfinityAnimations(int value)
    {
      AnimationType retVal = null;
      if (Profile.<Integer>getProperty(Profile.Key.GET_INFINITY_ANIMATIONS) > 0) {
        for (AnimationType type : AnimationType.values()) {
          if (contains(value, type.iaRanges)) {
            retVal = type;
            break;
          }
        }
      }
      return retVal;
    }

    /**
     * Returns the {@code AnimationType} enum covering the specified animation id.
     * @param animationId the animation id
     * @return {@code AnimationType} enum that covers the specified animation id. Returns {@code null} otherwise.
     */
    public static AnimationType typeOfId(int animationId)
    {
      for (final AnimationType type : values()) {
        if (type.contains(animationId)) {
          return type;
        }
      }
      return null;
    }
  }

  /** Available animation sequences. Note: PST-specific animation sequences are prefixed by "PST_". */
  public enum Sequence {
    /** Special value: Used when no animation sequence is loaded. */
    NONE,
    STAND("Stand"),
    STAND2("Stand 2"),
    STAND3("Stand 3"),
    /** For buried creatures only: stand sequence when emerged. */
    STAND_EMERGED("Stand (emerged)"),
    /** For buried creatures only: stand sequence when hidden. */
    STAND_HIDDEN("Stand (hidden)"),
    STANCE("Combat ready"),
    STANCE2("Combat ready"),
    GET_HIT("Get hit"),
    /** Dying sequence; may also be used for sleep sequence. */
    DIE("Die"),
    TWITCH("Twitch"),
    /** The animation sequence used while chanting a spell. */
    SPELL("Conjure spell"),
    /** The animation sequence used when releasing a spell. */
    CAST("Cast spell"),
    SPELL1("Conjure spell 1"),
    CAST1("Cast spell 1"),
    SPELL2("Conjure spell 2"),
    CAST2("Cast spell 2"),
    SPELL3("Conjure spell 3"),
    CAST3("Cast spell 3"),
    SPELL4("Conjure spell 4"),
    CAST4("Cast spell 4"),
    SLEEP("Sleep"),
    GET_UP("Get up"),
    SLEEP2("Sleep 2"),
    GET_UP2("Get up 2"),
    WALK("Walk"),
    ATTACK("Attack"),
    ATTACK_2("Attack 2"),
    ATTACK_3("Attack 3"),
    ATTACK_4("Attack 4"),
    ATTACK_5("Attack 5"),
    ATTACK_2H("Attack (2-h)"),
    /** 1-h slash attack; also used for throwing/sling in BG1/IWD */
    ATTACK_SLASH_1H("Attack (slash)"),
    /** 1-h backslash attack */
    ATTACK_BACKSLASH_1H("Attack (backslash)"),
    /** 1-h thrust/jab attack */
    ATTACK_JAB_1H("Attack (jab)"),
    /** 2-h slash attack */
    ATTACK_SLASH_2H("Attack (slash)"),
    /** 2-h backslash attack */
    ATTACK_BACKSLASH_2H("Attack (backslash)"),
    /** 2-h thrust/jab attack */
    ATTACK_JAB_2H("Attack (jab)"),
    /** two-weapon attack */
    ATTACK_2WEAPONS1("Attack (two-weapons) 1"),
    /** two-weapon attack */
    ATTACK_2WEAPONS2("Attack (two-weapons) 2"),
    /** Generic overhead attack; may be used for throwing weapons and weapons without equipped appearance. */
    ATTACK_OVERHEAD("Attack (overhead)"),
    ATTACK_BOW("Attack (bow)"),
    ATTACK_SLING("Attack (sling)"),
    ATTACK_CROSSBOW("Attack (crossbow)"),
    /** Generic ranged attack animation (e.g. for innate powers/breath weapons) */
    SHOOT("Attack (ranged)"),
    /** For buried creatures only: monster emerges from underground. */
    EMERGE("Emerge"),
    /** For buried creatures only: monster retreats to underground. */
    HIDE("Hide"),

    PST_ATTACK1("Attack"),
    PST_ATTACK2("Attack 2"),
    PST_ATTACK3("Attack 3"),
    PST_GET_HIT("Get hit"),
    PST_RUN("Run"),
    PST_WALK("Walk"),
    PST_SPELL1("Cast spell"),
    PST_SPELL2("Cast spell 2"),
    PST_SPELL3("Cast spell 3"),
    PST_GET_UP("Get up"),
    PST_DIE_FORWARD("Die (fall forward)"),
    PST_DIE_BACKWARD("Die (fall backwards)"),
    PST_DIE_COLLAPSE("Die (collapse)"),
    PST_TALK1("Talk"),
    PST_TALK2("Talk 2"),
    PST_TALK3("Talk 3"),
    PST_STAND_FIDGET1("Stand (fidget)"),
    PST_STAND_FIDGET2("Stand (fidget 2)"),
    PST_STANCE_FIDGET1("Combat ready (fidget)"),
    PST_STANCE_FIDGET2("Combat ready (fidget 2)"),
    PST_STAND("Stand"),
    PST_STANCE("Combat ready"),
    PST_STANCE_TO_STAND("Combat ready to stand"),
    PST_STAND_TO_STANCE("Stand to combat ready"),
    PST_MISC1("Custom sequence 1"),
    PST_MISC2("Custom sequence 2"),
    PST_MISC3("Custom sequence 3"),
    PST_MISC4("Custom sequence 4"),
    PST_MISC5("Custom sequence 5"),
    PST_MISC6("Custom sequence 6"),
    PST_MISC7("Custom sequence 7"),
    PST_MISC8("Custom sequence 8"),
    PST_MISC9("Custom sequence 9"),
    PST_MISC10("Custom sequence 10"),
    PST_MISC11("Custom sequence 11"),
    PST_MISC12("Custom sequence 12"),
    PST_MISC13("Custom sequence 13"),
    PST_MISC14("Custom sequence 14"),
    PST_MISC15("Custom sequence 15"),
    PST_MISC16("Custom sequence 16"),
    PST_MISC17("Custom sequence 17"),
    PST_MISC18("Custom sequence 18"),
    PST_MISC19("Custom sequence 19"),
    PST_MISC20("Custom sequence 20");

    private final String desc;

    private Sequence() { this(null); }
    private Sequence(String desc) { this.desc = desc; }

    @Override
    public String toString()
    {
      return (desc != null) ? desc : super.toString();
    }
  }

  /** Available cardinal directions for action sequences. */
  public enum Direction {
    /** South */
    S(0),
    /** South-southwest */
    SSW(1),
    /** Southwest */
    SW(2),
    /** West-southwest */
    WSW(3),
    /** West */
    W(4),
    /** West-northwest */
    WNW(5),
    /** Northwest */
    NW(6),
    /** North-northwest */
    NNW(7),
    /** North */
    N(8),
    /** North-northeast */
    NNE(9),
    /** Northeast */
    NE(10),
    /** East-northeast */
    ENE(11),
    /** East */
    E(12),
    /** East-southeast */
    ESE(13),
    /** Southeast */
    SE(14),
    /** South-southeast */
    SSE(15);

    private final int dir;
    private Direction(int dir) { this.dir = dir; }

    /** Returns the numeric direction value. */
    public int getValue() { return dir; }

    /**
     * Determines the {@link Direction} instance associated with the specified numeric value and returns it.
     * Return {@code null} if association could not be determined.
     */
    public static Direction from(int value) {
      for (final Direction d : Direction.values()) {
        if (d.getValue() == value) {
          return d;
        }
      }
      return null;
    }
  }

  /** Mappings between animation types and compatible sprite classes. */
  private static final EnumMap<AnimationType, Class<? extends SpriteDecoder>> typeAssociations =
      new EnumMap<AnimationType, Class<? extends SpriteDecoder>>(AnimationType.class) {{
        put(AnimationType.EFFECT, EffectDecoder.class);
        put(AnimationType.MONSTER_QUADRANT, MonsterQuadrantDecoder.class);
        put(AnimationType.MONSTER_MULTI, MonsterMultiDecoder.class);
        put(AnimationType.MONSTER_MULTI_NEW, MonsterMultiNewDecoder.class);
        put(AnimationType.MONSTER_LAYERED_SPELL, MonsterLayeredSpellDecoder.class);
        put(AnimationType.MONSTER_ANKHEG, MonsterAnkhegDecoder.class);
        put(AnimationType.TOWN_STATIC, TownStaticDecoder.class);
        put(AnimationType.CHARACTER, CharacterDecoder.class);
        put(AnimationType.CHARACTER_OLD, CharacterOldDecoder.class);
        put(AnimationType.MONSTER, MonsterDecoder.class);
        put(AnimationType.MONSTER_OLD, MonsterOldDecoder.class);
        put(AnimationType.MONSTER_LAYERED, MonsterLayeredDecoder.class);
        put(AnimationType.MONSTER_LARGE, MonsterLargeDecoder.class);
        put(AnimationType.MONSTER_LARGE_16, MonsterLarge16Decoder.class);
        put(AnimationType.AMBIENT_STATIC, AmbientStaticDecoder.class);
        put(AnimationType.AMBIENT, AmbientDecoder.class);
        put(AnimationType.FLYING, FlyingDecoder.class);
        put(AnimationType.MONSTER_ICEWIND, MonsterIcewindDecoder.class);
        put(AnimationType.MONSTER_PLANESCAPE, MonsterPlanescapeDecoder.class);
      }};

  /**
   * A default operation that can be passed to the
   * {@link #createAnimation(SeqDef, List, BeforeSourceBam, BeforeSourceFrame, AfterSourceFrame, AfterDestFrame)}
   * method. It is called once per source BAM resource.
   * Performed actions: palette replacement, shadow color fix, false color replacement, translucency
   */
  protected final BeforeSourceBam FN_BEFORE_SRC_BAM = new BeforeSourceBam() {
    @Override
    public void accept(BamV1Control control, SegmentDef sd)
    {
      if (isPaletteReplacementEnabled() && sd.getSpriteType() == SegmentDef.SpriteType.AVATAR) {
        int[] palette = getNewPaletteData(sd.getEntry());
        if (palette != null) {
          SpriteUtils.applyNewPalette(control, palette);
        }
      }

      SpriteUtils.fixShadowColor(control);

      if (isPaletteReplacementEnabled() && isFalseColor()) {
        applyFalseColors(control, sd);
      }

      if (isTranslucencyEnabled() && isTranslucent()) {
        applyTranslucency(control);
      }
    }
  };

  /**
   * A default operation that can be passed to the
   * {@link #createAnimation(SeqDef, List, BeforeSourceBam, BeforeSourceFrame, AfterSourceFrame, AfterDestFrame)}
   * method. It is called for each source frame (segment) before being applied to the target frame.
   */
  protected final BeforeSourceFrame FN_BEFORE_SRC_FRAME = new BeforeSourceFrame() {
    @Override
    public BufferedImage apply(SegmentDef sd, BufferedImage image, Graphics2D g)
    {
      // nothing to do...
      return image;
    }
  };

  /**
   * A default operation that can be passed to the
   * {@link #createAnimation(SeqDef, List, BeforeSourceBam, BeforeSourceFrame, AfterSourceFrame, AfterDestFrame)}
   * method. It is called for each source frame (segment) after being applied to the target frame.
   */
  protected final AfterSourceFrame FN_AFTER_SRC_FRAME = new AfterSourceFrame() {
    @Override
    public void accept(SegmentDef sd, Graphics2D g)
    {
      // nothing to do...
    }
  };

  /**
   * A default action that can be passed to the
   * {@link #createAnimation(SeqDef, List, BeforeSourceBam, BeforeSourceFrame, AfterSourceFrame, AfterDestFrame)}
   * method. It calculates an eastern direction frame by mirroring it horizontally if needed.
   */
  protected final AfterDestFrame FN_AFTER_DST_FRAME = new AfterDestFrame() {
    @Override
    public void accept(DirDef dd, int frameIdx)
    {
      if (dd.isMirrored()) {
        flipImageHorizontal(frameIdx);
      }
    }
  };

  private final CreatureInfo creInfo;
  private final IniMap ini;
  /** Storage for associations between directions and cycle indices. */
  private final EnumMap<Direction, Integer> directionMap;
  /** Cache for creature animation attributes. */
  private final TreeMap<DecoderAttribute, Object> attributesMap;

  private Sequence currentSequence;
  private boolean showCircle;
  private boolean showPersonalSpace;
  private boolean showBoundingBox;
  private boolean translucencyEnabled;
  private boolean paletteReplacementEnabled;
  private boolean renderSpriteAvatar;
  private boolean renderSpriteWeapon;
  private boolean renderSpriteHelmet;
  private boolean renderSpriteShield;
  private boolean animationChanged;
  private boolean autoApplyChanges;

  /**
   * Convenience method for loading the animation of the specified CRE resource.
   * @param cre The CRE resource instance.
   * @return A {@code SpriteDecoder} instance with processed animation data.
   * @throws Exception if the specified resource could not be processed.
   */
  public static SpriteDecoder importSprite(CreResource cre) throws Exception
  {
    Objects.requireNonNull(cre, "CRE resource cannot be null");
    int animationId = ((IsNumeric)cre.getAttribute(CreResource.CRE_ANIMATION)).getValue();
    Class<? extends SpriteDecoder> spriteClass =
        Objects.requireNonNull(detectAnimationType(animationId), "Could not determine animation type");
    try {
      Constructor<? extends SpriteDecoder> ctor =
          Objects.requireNonNull(spriteClass.getConstructor(CreResource.class), "No matching constructor found");
      return ctor.newInstance(cre);
    } catch (InvocationTargetException ite) {
      throw (ite.getCause() instanceof Exception) ? (Exception)ite.getCause() : ite;
    }
  }

  /**
   * Returns the {@code SpriteClass} class associated with the specified animation id.
   * @param animationId the animation id
   * @return a class type compatible with the specified animation id.
   *         Returns {@code null} if no class could be determined.
   */
  public static Class<? extends SpriteDecoder> getSpriteDecoderClass(int animationId)
  {
    Class<? extends SpriteDecoder> retVal = null;

    // Testing Infinity Animation range first
    AnimationType animType = AnimationType.containsInfinityAnimations(animationId);
    if (animType != null) {
      retVal = typeAssociations.get(animType);
    }

    // Testing regular ranges
    if (retVal == null) {
      for (final AnimationType type : AnimationType.values()) {
        if (type.contains(animationId)) {
          retVal = typeAssociations.get(type);
          if (retVal != null) {
            break;
          }
        }
      }
    }

    return retVal;
  }

  /**
   * Returns the {@code SpriteClass} class associated with the specified {@code AnimationType} enum.
   * @param type the {@code AnimationType}
   * @return the associated {@code SpriteClass} class object. Returns {@code null} if class could not be determined.
   */
  public static Class<? extends SpriteDecoder> getSpriteDecoderClass(AnimationType type)
  {
    return typeAssociations.get(type);
  }

  /**
   * Instances creates with this constructor are only suited for identification purposes.
   * @param type the animation type
   * @param animationId specific animation id
   * @param sectionName INI section name for animation-specific data
   * @param ini the INI file with creature animation attributes
   * @throws Exception
   */
  protected SpriteDecoder(AnimationType type, int animationId, IniMap ini) throws Exception
  {
    Objects.requireNonNull(type, "Animation type cannot be null");
    Objects.requireNonNull(ini, "No INI data available for animation id: " + animationId);
    this.attributesMap = new TreeMap<>();
    this.directionMap = new EnumMap<>(Direction.class);
    setAttribute(KEY_ANIMATION_TYPE, type);
    setAttribute(KEY_ANIMATION_SECTION, type.getSectionName());
    this.creInfo = new CreatureInfo(this, SpriteUtils.getPseudoCre(animationId, null, null));
    this.ini = ini;
    this.currentSequence = Sequence.NONE;
    init();
    if (!isMatchingAnimationType()) {
      throw new IllegalArgumentException("Animation id is incompatible with animation type: " + type.toString());
    }
  }

  /**
   * This constructor creates an instance that can be used to render animation sequences.
   * @param type the animation type
   * @param cre the CRE resource instance.
   * @throws Exception
   */
  protected SpriteDecoder(AnimationType type, CreResource cre) throws Exception
  {
    Objects.requireNonNull(type, "Animation type cannot be null");
    this.attributesMap = new TreeMap<>();
    this.directionMap = new EnumMap<>(Direction.class);
    setAttribute(KEY_ANIMATION_TYPE, type);
    setAttribute(KEY_ANIMATION_SECTION, type.getSectionName());
    this.creInfo = new CreatureInfo(this, cre);
    this.ini = Objects.requireNonNull(getAnimationInfo(getAnimationId()), "No INI data available for animation id: " + getAnimationId());
    this.currentSequence = Sequence.NONE;
    this.showCircle = false;
    this.showPersonalSpace = false;
    this.showBoundingBox = false;
    this.translucencyEnabled = true;
    this.paletteReplacementEnabled = true;
    this.renderSpriteAvatar = true;
    this.renderSpriteWeapon = true;
    this.renderSpriteShield = true;
    this.renderSpriteHelmet = true;
    this.autoApplyChanges = true;
    SpriteUtils.updateRandomPool();
    init();
  }

  /**
   * Returns the data associated with the specified attribute name.
   * @param key the attribute name.
   * @return attribute data in the type inferred from the method call.
   *                   Returns {@code null} if data is not available for the inferred type.
   */
  @SuppressWarnings("unchecked")
  public <T> T getAttribute(DecoderAttribute att)
  {
    T retVal = null;
    if (att == null) {
      return retVal;
    }

    Object data = attributesMap.getOrDefault(att, att.getDefaultValue());
    if (data != null) {
      try {
        retVal = (T)data;
      } catch (ClassCastException e) {
        // e.printStackTrace();
      }
    }
    return retVal;
  }

  /**
   * Stores the attribute key and value along with the autodetected data type.
   * @param key the attribute name.
   * @param value the value in one of the data types covered by {@link KeyType}.
   */
  protected void setAttribute(DecoderAttribute att, Object value)
  {
    if (att == null) {
      return;
    }
    attributesMap.put(att, value);
  }

  /** Returns an iterator over the attribute keys. */
  public Iterator<DecoderAttribute> getAttributeIterator()
  {
    return attributesMap.keySet().iterator();
  }

  /** Returns the type of the current creature animation. */
  public AnimationType getAnimationType()
  {
    return getAttribute(KEY_ANIMATION_TYPE);
  }

  /**
   * Returns the INI section name for the current animation type.
   * Returns {@code null} if the name could not be determined.
   */
  public String getAnimationSectionName()
  {
    return getAttribute(KEY_ANIMATION_SECTION);
  }

  /**
   * Returns a list of BAM filenames associated with the current animation type.
   * @param essential if set returns only essential files required for the animation.
   * @return list of BAM filenames associated with the current animation type.
   *         Returns {@code null} if files could not be determined.
   */
  public abstract List<String> getAnimationFiles(boolean essential);

  /** Recreates the creature animation based on the current creature resource. */
  public void reset() throws Exception
  {
    Direction[] directions = getDirectionMap().keySet().toArray(new Direction[getDirectionMap().keySet().size()]);
    discard();
    // recreating current sequence
    if (getCurrentSequence() != Sequence.NONE) {
      createSequence(getCurrentSequence(), directions);
    }
  }

  /** Removes the currently loaded animation sequence. */
  protected void discard()
  {
    frameClear();
    directionMap.clear();
  }

  /**
   * Loads the specified sequence if available. Discards the currently active sequence.
   * Call {@code reset()} instead to enforce reloading the same sequence with different
   * creature attributes.
   * @param seq the animation sequence to load. Specifying {@code Sequence.None} only discards the current sequence.
   * @return whether the sequence was successfully loaded.
   */
  public boolean loadSequence(Sequence seq) throws Exception
  {
    return loadSequence(seq, null);
  }

  /**
   * Loads selected directions of the specified sequence if available. Discards the currently active sequence.
   * Call {@code reset()} instead to enforce reloading the same sequence with different
   * creature attributes.
   * @param seq the animation sequence to load. Specifying {@code Sequence.None} only discards the current sequence.
   * @param directions array with directions allowed to be created. Specify {@code null} to create animations
   *                   for all directions.
   * @return whether the sequence was successfully loaded.
   */
  public boolean loadSequence(Sequence seq, Direction[] directions) throws Exception
  {
    boolean retVal = true;

    if (getCurrentSequence() != Objects.requireNonNull(seq, "Animation sequence cannot be null")) {
      // discarding current sequence
      discard();

      try {
        createSequence(seq, directions);
        currentSequence = seq;
      } catch (NullPointerException e) {
        retVal = (seq != Sequence.NONE);
      } catch (Exception e) {
        e.printStackTrace();
        retVal = (seq != Sequence.NONE);
      }
    }

    return retVal;
  }

  /** Returns the currently active sequence. */
  public Sequence getCurrentSequence()
  {
    return currentSequence;
  }

  /** Returns whether the specified animation sequence is available for the current creature animation. */
  public abstract boolean isSequenceAvailable(Sequence seq);

  /**
   * Returns the closest available direction to the specified direction.
   * @param dir the requested direction
   * @return an available {@code Direction} that is closest to the specified direction.
   *         Returns {@code null} if no direction is available.
   */
  public Direction getExistingDirection(Direction dir)
  {
    Direction retVal = null;

    if (dir == null) {
      return retVal;
    }
    if (getDirectionMap().containsKey(dir)) {
      return dir;
    }
    SeqDef sd = getSequenceDefinition(getCurrentSequence());
    if (sd == null || sd.isEmpty()) {
      return retVal;
    }

    int dirIdx = dir.getValue();
    int dirLen = Direction.values().length;
    int maxRange = dirLen / 2;
    for (int range = 1; range <= maxRange; range++) {
      int dist = (dirIdx + range + dirLen) % dirLen;
      Direction distDir = Direction.from(dist);
      if (getDirectionMap().containsKey(distDir)) {
        retVal = distDir;
        break;
      }
      dist = (dirIdx - range + dirLen) % dirLen;
      distDir = Direction.from(dist);
      if (getDirectionMap().containsKey(distDir)) {
        retVal = distDir;
        break;
      }
    }

    return retVal;
  }

  /** Provides access to the {@link CreatureInfo} instance associated with the sprite decoder. */
  public CreatureInfo getCreatureInfo()
  {
    return creInfo;
  }

  /** Returns the {@code CreResource} instance of the current CRE resource. */
  public CreResource getCreResource()
  {
    return creInfo.getCreResource();
  }

  /** Returns the numeric animation id of the current CRE resource. */
  public int getAnimationId()
  {
    return creInfo.getAnimationId();
  }

  /** Returns a INI structure with creature animation info. */
  public IniMap getAnimationInfo()
  {
    return ini;
  }

  /** Returns whether the selection circle for the creature is drawn. */
  public boolean isSelectionCircleEnabled()
  {
    return showCircle;
  }

  /** Sets whether the selection circle for the creature is drawn. */
  public void setSelectionCircleEnabled(boolean b)
  {
    if (showCircle != b) {
      showCircle = b;
      selectionCircleChanged();
    }
  }

  /** Returns whether the space occupied by the creature is visualized. */
  public boolean isPersonalSpaceVisible()
  {
    return showPersonalSpace;
  }

  /** Sets whether the space occupied by the creature is visualized. */
  public void setPersonalSpaceVisible(boolean b)
  {
    if (showPersonalSpace != b) {
      showPersonalSpace = b;
      personalSpaceChanged();
    }
  }

  /** Returns whether a bounding box is drawn around sprites (or quadrants) and secondary overlays. */
  public boolean isBoundingBoxVisible()
  {
    return showBoundingBox;
  }

  /** Sets whether a bounding box is drawn around sprites (or quadrants) and secondary overlays. */
  public void setBoundingBoxVisible(boolean b)
  {
    if (showBoundingBox != b) {
      showBoundingBox = b;
      spriteChanged();
    }
  }

  /** Returns whether the avatar sprite should be rendered. */
  public boolean getRenderAvatar()
  {
    return renderSpriteAvatar;
  }

  /** Sets whether the avatar sprite should be rendered. */
  public void setRenderAvatar(boolean b)
  {
    if (renderSpriteAvatar != b) {
      renderSpriteAvatar = b;
      spriteChanged();
    }
  }

  /** Returns whether the weapon overlay should be rendered. This option affects only specific animation types. */
  public boolean getRenderWeapon()
  {
    return renderSpriteWeapon;
  }

  /** Sets whether the weapon overlay should be rendered. This option affects only specific animation types. */
  public void setRenderWeapon(boolean b)
  {
    if (renderSpriteWeapon != b) {
      renderSpriteWeapon = b;
      spriteChanged();
    }
  }

  /**
   * Returns whether the shield (or left-handed weapon) overlay should be rendered.
   * This option affects only specific animation types.
   */
  public boolean getRenderShield()
  {
    return renderSpriteShield;
  }

  /**
   * Sets whether the shield (or left-handed weapon) overlay should be rendered.
   * This option affects only specific animation types.
   */
  public void setRenderShield(boolean b)
  {
    if (renderSpriteShield != b) {
      renderSpriteShield = b;
      spriteChanged();
    }
  }

  /**  Returns whether the helmet overlay should be rendered. This option affects only specific animation types. */
  public boolean getRenderHelmet()
  {
    return renderSpriteHelmet;
  }

  /** Sets whether the helmet overlay should be rendered. This option affects only specific animation types. */
  public void setRenderHelmet(boolean b)
  {
    if (renderSpriteHelmet != b) {
      renderSpriteHelmet = b;
      spriteChanged();
    }
  }

  /** Returns whether translucency effect is applied to the creature animation. */
  public boolean isTranslucencyEnabled()
  {
    return translucencyEnabled;
  }

  /** Sets whether translucency effect is applied to the creature animation. */
  public void setTranslucencyEnabled(boolean b)
  {
    if (translucencyEnabled != b) {
      translucencyEnabled = b;
      if (isTranslucent()) {
        SpriteUtils.clearBamCache();
        spriteChanged();
      }
    }
  }

  /** Returns whether any kind of palette replacement (full palette or false colors) is enabled. */
  public boolean isPaletteReplacementEnabled()
  {
    return paletteReplacementEnabled;
  }

  /** Sets whether palette replacement (full palette or false colors) is enabled. */
  public void setPaletteReplacementEnabled(boolean b)
  {
    if (paletteReplacementEnabled != b) {
      paletteReplacementEnabled = b;
      SpriteUtils.clearBamCache();
      spriteChanged();
    }
  }

  /** Returns the moving speed of the creature animation. */
  public double getMoveScale()
  {
    return getAttribute(KEY_MOVE_SCALE);
  }

  /** Sets the moving speed of the creature animation. */
  protected void setMoveScale(double value)
  {
    setAttribute(KEY_MOVE_SCALE, value);
  }

  /** Returns the selection circle size of the creature animation. */
  public int getEllipse()
  {
    return getAttribute(KEY_ELLIPSE);
  }

  /** Sets the selection circle size of the creature animation. */
  public void setEllipse(int value)
  {
    if (getEllipse() != value) {
      setAttribute(KEY_ELLIPSE, value);
      selectionCircleChanged();
    }
  }

  /** Returns the map space (in search map units) reserved exclusively for the creature animation*/
  public int getPersonalSpace()
  {
    return getAttribute(KEY_PERSONAL_SPACE);
  }

  /** Sets the map space (in search map units) reserved exclusively for the creature animation*/
  public void setPersonalSpace(int value)
  {
    if (getPersonalSpace() != value) {
      setAttribute(KEY_PERSONAL_SPACE, value);
      personalSpaceChanged();
    }
  }

  /** Returns the resref (prefix) for the associated animation files. */
  public String getAnimationResref()
  {
    return getAttribute(KEY_RESREF);
  }

  /** Sets the resref (prefix) for the associated animation files. */
  protected void setAnimationResref(String resref)
  {
    setAttribute(KEY_RESREF, resref);
  }

  /** Returns the replacement palette for the creature animation. Returns empty string if no replacement palette exists. */
  public String getNewPalette()
  {
    return getAttribute(KEY_NEW_PALETTE);
  }

  /** Sets the replacement palette for the creature animation. */
  public void setNewPalette(String resref)
  {
    resref = (resref != null) ? resref.trim() : "";
    if (!getNewPalette().equalsIgnoreCase(resref)) {
      setAttribute(KEY_NEW_PALETTE, resref);
      paletteChanged();
    }
  }

  /** Loads the replacement palette associated with the specified BAM resource. */
  protected int[] getNewPaletteData(ResourceEntry bamRes)
  {
    // Note: method argument is irrelevant for base implementation
    return SpriteUtils.loadReplacementPalette(getNewPalette());
  }

  /** ??? */
  public boolean isBrightest()
  {
    return getAttribute(KEY_BRIGHTEST);
  }

  /** ??? */
  protected void setBrightest(boolean b)
  {
    setAttribute(KEY_BRIGHTEST, b);
  }

  /** Returns whether blending mode is enabled. */
  public boolean isMultiplyBlend()
  {
    return getAttribute(KEY_MULTIPLY_BLEND);
  }

  /** Sets blending mode. */
  protected void setMultiplyBlend(boolean b)
  {
    setAttribute(KEY_MULTIPLY_BLEND, b);
  }

  /** ??? */
  public boolean isLightSource()
  {
    return getAttribute(KEY_LIGHT_SOURCE);
  }

  /** ??? */
  protected void setLightSource(boolean b)
  {
    setAttribute(KEY_LIGHT_SOURCE, b);
  }

  /** Returns whether a red tint is applied to the creature if detected by infravision. */
  public boolean isDetectedByInfravision()
  {
    return getAttribute(KEY_DETECTED_BY_INFRAVISION);
  }

  /** Sets whether a red tint is applied to the creature if detected by infravision. */
  protected void setDetectedByInfravision(boolean b)
  {
    setAttribute(KEY_DETECTED_BY_INFRAVISION, b);
  }

  /** Returns whether palette range replacement is enabled. */
  public boolean isFalseColor()
  {
    return getAttribute(KEY_FALSE_COLOR);
  }

  /** Sets whether palette range replacement is enabled. */
  protected void setFalseColor(boolean b)
  {
    setAttribute(KEY_FALSE_COLOR, b);
  }

  /** Returns whether creature animation is translucent.  */
  public boolean isTranslucent()
  {
    return getCreatureInfo().getEffectiveTranslucency() > 0;
  }

  /** Sets whether creature animation is translucent.  */
  protected void setTranslucent(boolean b)
  {
    setAttribute(KEY_TRANSLUCENT, b);
  }

  /** Call this method whenever the visibility of the selection circle has been changed. */
  public void selectionCircleChanged()
  {
    setAnimationChanged();
  }

  /** Call this method whenever the visibility of personal space has been changed. */
  public void personalSpaceChanged()
  {
    setAnimationChanged();
  }

  /** Call this method whenever the visibility of any sprite types has been changed. */
  public void spriteChanged()
  {
    setAnimationChanged();
  }

  /** Call this method whenever the allegiance value has been changed. */
  public void allegianceChanged()
  {
    setAnimationChanged();
  }

  /**
   * Call this method whenever the creature palette has changed.
   * False color is processed by {@link #falseColorChanged()}.
   */
  public void paletteChanged()
  {
    // Note: PST false color palette may also contain true color regions.
    if (!isFalseColor() ||
        Profile.getGame() == Profile.Game.PSTEE ||
        Profile.getEngine() == Profile.Engine.PST) {
      setAnimationChanged();
    }
  }

  /**
   * Call this method whenever the false color palette of the creature has changed.
   * Conventional palette changes are processed by {@link #paletteChanged()}. */
  public void falseColorChanged()
  {
    if (isFalseColor()) {
      setAnimationChanged();
    }
  }

  /** This method reloads the creature animation if any relevant changes have been made. */
  public void applyAnimationChanges()
  {
    if (hasAnimationChanged()) {
      resetAnimationChanged();
      try {
        reset();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /** Returns whether changes to the creature animation should be applied immediately. */
  public boolean isAutoApplyChanges()
  {
    return autoApplyChanges;
  }

  /** Sets whether changes to the creature animation should be applied immediately. */
  public void setAutoApplyChanges(boolean b)
  {
    if (autoApplyChanges != b) {
      autoApplyChanges = b;
      if (autoApplyChanges) {
        applyAnimationChanges();
      }
    }
  }

  /**
   * Returns whether a creature animation reload has been requested.
   * Call {@link #applyPendingChanges()} to apply the changes.
   */
  public boolean hasAnimationChanged()
  {
    return animationChanged;
  }

  /** Call to request a creature animation reload by the method {@link #applyPendingChanges()}. */
  public void setAnimationChanged()
  {
    animationChanged = true;
    if (isAutoApplyChanges()) {
      applyAnimationChanges();
    }
  }

  /** Call to cancel the request of a creature animation reload by the method {@link #applyPendingChanges()}. */
  public void resetAnimationChanged()
  {
    animationChanged = false;
  }

  /**
   * Returns the preferred compositor for rendering the sprite on the target surface.
   */
  public Composite getComposite()
  {
    int blending = ((isBrightest() ? 1 : 0) << 0) | ((isMultiplyBlend() ? 1 : 0) << 1);
    switch (blending) {
      case 1:   // brightest
        return BlendingComposite.Brightest;
      case 2:   // multiply
        return BlendingComposite.Multiply;
      case 3:   // brightest + multiply
        return BlendingComposite.BrightestMultiply;
      default:
        return AlphaComposite.SrcOver;
    }
  }

  /** Creates the BAM structure for the creature animation. */
  protected abstract void init() throws Exception;

  /**
   * Initializes general data for the creature animation.
   * @param ini The INI map containing creature animation data.
   */
  protected void initDefaults(IniMap ini) throws Exception
  {
    IniMapSection section = getGeneralIniSection(Objects.requireNonNull(ini, "INI object cannot be null"));
    Objects.requireNonNull(section.getAsString("animation_type"), "animation_type required");
    Misc.requireCondition(getAnimationType().contains(getAnimationId()),
                          String.format("Animation slot (%04X) is not compatible with animation type (%s)",
                                        getAnimationId(), getAnimationType().toString()));

    setMoveScale(section.getAsDouble("move_scale", 0.0));
    setEllipse(section.getAsInteger("ellipse", 16));
    setPersonalSpace(section.getAsInteger("personal_space", 3));
    setBrightest(section.getAsInteger("brightest", 0) != 0);
    setMultiplyBlend(section.getAsInteger("multiply_blend", 0) != 0);
    setLightSource(section.getAsInteger("light_source", 0) != 0);

    String s = section.getAsString("new_palette", "");
    setNewPalette(s);

    // getting first available "resref" definition
    for (Iterator<IniMapSection> iter = getAnimationInfo().iterator(); iter.hasNext(); ) {
      section = iter.next();
      s = section.getAsString("resref", "");
      if (!s.isEmpty()) {
        setAnimationResref(s);
        break;
      }
    }
    Misc.requireCondition(!getAnimationResref().isEmpty(), "Animation resource prefix required");
  }

  /**
   * Returns the general INI map section defined for all supported creature animation types from the specified
   * {@code IniMap} instance. Returns an empty {@code IniMapSection} instance if section could not be determined.
   */
  protected IniMapSection getGeneralIniSection(IniMap ini)
  {
    final String sectionName = "general";
    IniMapSection retVal = null;
    if (ini != null) {
      retVal = ini.getSection(sectionName);
    }

    if (retVal == null) {
      retVal = new IniMapSection(sectionName, 0, null);
    }

    return retVal;
  }

  /**
   * Returns the INI map section responsible for animation-type-specific attributes.
   * Returns an empty {@code IniMapSection} instance if section could not be determined.
   */
  protected IniMapSection getSpecificIniSection()
  {
    IniMapSection retVal = null;
    IniMap ini = getAnimationInfo();
    if (ini != null) {
      retVal = ini.getSection(getAnimationSectionName());
    }

    if (retVal == null) {
      retVal = new IniMapSection(getAnimationSectionName(), 0, null);
    }

    return retVal;
  }

  /**
   * Returns the BAM cycle associated with the specified direction.
   * Returns -1 if entry not found.
   */
  public int getCycleIndex(Direction dir)
  {
    int retVal = -1;
    Integer value = directionMap.get(dir);
    if (value != null) {
      retVal = value.intValue();
    }

    return retVal;
  }

  /**
   * Returns a copy of the map containing associations between animation directions and bam sequence numbers.
   */
  public EnumMap<Direction, Integer> getDirectionMap()
  {
    return directionMap.clone();
  }

  /**
   * Assigns a cycle index to the specified BAM sequence and direction.
   * @param seq the sequence type for identification purposes.
   * @param dir the direction type
   * @param cycleIndex the cycle index associated with the specified sequence and direction.
   * @return The previous BAM cycle index if available. -1 otherwise.
   */
  protected int addDirection(Direction dir, int cycleIndex)
  {
    int retVal = -1;
    dir = Objects.requireNonNull(dir, "Creature direction required");
    Integer value = directionMap.get(dir);
    if (value != null) {
      retVal = value.intValue();
    }
    directionMap.put(dir, cycleIndex);

    return retVal;
  }

  /**
   * Generates definitions for the specified animation sequence.
   * @param seq the requested animation sequence.
   * @return a fully initialized {@code SeqDef} object if sequence is supported, {@code null} otherwise.
   */
  protected abstract SeqDef getSequenceDefinition(Sequence seq);

  /**
   * Loads the specified animation sequence into the SpriteDecoder.
   * @param seq the sequence to load.
   * @throws NullPointerException if specified sequence is not available.
   */
  protected void createSequence(Sequence seq) throws Exception
  {
    SeqDef sd = Objects.requireNonNull(getSequenceDefinition(seq), "Sequence not available: " + (seq != null ? seq : "(null)"));
    createAnimation(sd, null, FN_BEFORE_SRC_BAM, FN_BEFORE_SRC_FRAME, FN_AFTER_SRC_FRAME, FN_AFTER_DST_FRAME);
  }

  /**
   * Loads the specified animation sequence into the SpriteDecoder.
   * Only directions listed in the given {@code Direction} array will be considered.
   * @param seq the sequence to load.
   * @param directions an array of {@code Direction} values. Only directions listed in the array
   *                   are considered by the creation process. Specify {@code null} to allow all directions.
   * @throws NullPointerException if specified sequence is not available.
   */
  protected void createSequence(Sequence seq, Direction[] directions) throws Exception
  {
    SeqDef sd = Objects.requireNonNull(getSequenceDefinition(seq), "Sequence not available: " + (seq != null ? seq : "(null)"));
    if (directions == null) {
      directions = Direction.values();
    }
    createAnimation(sd, Arrays.asList(directions), FN_BEFORE_SRC_BAM, FN_BEFORE_SRC_FRAME, FN_AFTER_SRC_FRAME, FN_AFTER_DST_FRAME);
  }

  protected void createAnimation(SeqDef definition, List<Direction> directions,
                                 BeforeSourceBam beforeSrcBam,
                                 BeforeSourceFrame beforeSrcFrame,
                                 AfterSourceFrame afterSrcFrame,
                                 AfterDestFrame afterDstFrame)
  {
    PseudoBamControl dstCtrl = createControl();
    BamV1Control srcCtrl = null;
    ResourceEntry entry = null;
    definition = Objects.requireNonNull(definition, "Sequence definition cannot be null");

    if (directions == null) {
      directions = Arrays.asList(Direction.values());
    }
    if (directions.isEmpty()) {
      return;
    }

    for (final DirDef dd : definition.getDirections()) {
      if (!directions.contains(dd.getDirection())) {
        continue;
      }
      CycleDef cd = dd.getCycle();
      int cycleIndex = dstCtrl.cycleAdd();
      addDirection(dd.getDirection(), cycleIndex);

      cd.reset();
      int frameCount = cd.getMaximumFrames();
      final ArrayList<FrameInfo> frameInfo = new ArrayList<>();
      for (int frame = 0; frame < frameCount; frame++) {
        frameInfo.clear();
        for (final SegmentDef sd : cd.getCycles()) {
          // checking visibility of sprite types
          boolean skip = (sd.getSpriteType() == SegmentDef.SpriteType.AVATAR) && !getRenderAvatar();
          skip |= (sd.getSpriteType() == SegmentDef.SpriteType.WEAPON) && !getRenderWeapon();
          skip |= (sd.getSpriteType() == SegmentDef.SpriteType.SHIELD) && !getRenderShield();
          skip |= (sd.getSpriteType() == SegmentDef.SpriteType.HELMET) && !getRenderHelmet();
          if (skip) {
            continue;
          }

          entry = sd.getEntry();
          srcCtrl = Objects.requireNonNull(SpriteUtils.loadBamController(entry));
          srcCtrl.cycleSet(sd.getCycleIndex());

          if (sd.getCurrentFrame() >= 0) {
            if (beforeSrcBam != null) {
              beforeSrcBam.accept(srcCtrl, sd);
            }
            frameInfo.add(new FrameInfo(srcCtrl, sd));
          }
          sd.advance();
        }

        int frameIndex = createFrame(frameInfo.toArray(new FrameInfo[frameInfo.size()]), beforeSrcFrame, afterSrcFrame);
        if (afterDstFrame != null) {
          afterDstFrame.accept(dd, frameIndex);
        }
        dstCtrl.cycleAddFrames(cycleIndex, new int[] {frameIndex});
      }
    }
  }

  /**
   * Creates a single creature animation frame from the given array of source frame segments
   * and adds it to the BAM frame list. Each source frame segment can be processed by the specified lambda function
   * before it is drawn onto to the target frame.
   * @param sourceFrames array of source frame segments to compose.
   * @param beforeSrcFrame optional function that is executed before a source frame segment is drawn onto the
   *                       target frame.
   * @param afterSrcFrame optional method that is executed right after a source frame segment has been drawn onto the
   *                      target frame.
   * @return the absolute target BAM frame index.
   */
  protected int createFrame(FrameInfo[] sourceFrames, BeforeSourceFrame beforeSrcFrame, AfterSourceFrame afterSrcFrame)
  {
    Rectangle rect;
    if (Objects.requireNonNull(sourceFrames, "Source frame info objects required").length > 0) {
      rect = getTotalFrameDimension(sourceFrames);
    } else {
      rect = new Rectangle(0, 0, 1, 1);
    }

    // include personal space region in image size
    if (isPersonalSpaceVisible()) {
      rect = updateFrameDimension(rect, getPersonalSpaceSize(true));
    }

    // include selection circle in image size
    float circleStrokeSize = getSelectionCircleStrokeSize();
    if (isSelectionCircleEnabled()) {
      Dimension dim = getSelectionCircleSize();
      rect = updateFrameDimension(rect, new Dimension(2 * (dim.width + (int)circleStrokeSize),
                                                      2 * (dim.height + (int)circleStrokeSize)));
    }

    // creating target image
    BufferedImage image;
    if (rect.width > 0 && rect.height > 0) {
      image = ColorConvert.createCompatibleImage(rect.width, rect.height, Transparency.TRANSLUCENT);
      Graphics2D g = image.createGraphics();
      try {
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(new Color(0, true));
        g.fillRect(0, 0, image.getWidth(), image.getHeight());

        Point center = new Point(-rect.x, -rect.y);

        if (isPersonalSpaceVisible()) {
          // Drawing personal space region
          drawPersonalSpace(g, center, null, 0.5f);
        }

        if (isSelectionCircleEnabled()) {
          // Drawing selection circle
          drawSelectionCircle(g, center, null, circleStrokeSize);
        }

        // drawing source frames to target image
        for (final FrameInfo fi : sourceFrames) {
          BamV1Control ctrl = fi.getController();
          ctrl.cycleSet(fi.getCycle());
          int frameIdx = fi.getFrame();
          ctrl.cycleSetFrameIndex(frameIdx);
          BufferedImage srcImage = (BufferedImage)ctrl.cycleGetFrame();
          if (beforeSrcFrame != null) {
            srcImage = beforeSrcFrame.apply(fi.getSegmentDefinition(), srcImage, g);
          }
          FrameEntry entry = ctrl.getDecoder().getFrameInfo(ctrl.cycleGetFrameIndexAbsolute());
          int x = -rect.x - entry.getCenterX();
          int y = -rect.y - entry.getCenterY();

          if (isBoundingBoxVisible() && entry.getWidth() > 2 && entry.getHeight() > 2) {
            // drawing bounding box around sprite elements
            Stroke oldStroke = g.getStroke();
            Color oldColor = g.getColor();
            Object oldHints = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            try {
              g.setStroke(FrameInfo.STROKE_BOUNDING_BOX);
              g.setColor(FrameInfo.SPRITE_COLOR.getOrDefault(fi.getSegmentDefinition().getSpriteType(), FrameInfo.SPRITE_COLOR_DEFAULT));
              g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
              g.drawRect(x, y, entry.getWidth() - 1, entry.getHeight() - 1);
            } finally {
              g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (oldHints != null) ? oldHints : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
              if (oldColor != null) {
                g.setColor(oldColor);
              }
              if (oldStroke != null) {
                g.setStroke(oldStroke);
              }
            }
          }

          g.drawImage(srcImage, x, y, entry.getWidth(), entry.getHeight(), null);

          if (afterSrcFrame != null) {
            afterSrcFrame.accept(fi.getSegmentDefinition(), g);
          }
          ctrl = null;
        }
      } finally {
        g.dispose();
        g = null;
      }
    } else {
      // dummy graphics
      image = ColorConvert.createCompatibleImage(1, 1, Transparency.TRANSLUCENT);
    }

    // setting center point
    int cx = -rect.x;
    int cy = -rect.y;

    return frameAdd(image, new Point(cx, cy));
  }

  /**
   * Calculates the total size of the personal space region.
   * @param scaled whether dimension should be scaled according to search map unit size.
   */
  protected Dimension getPersonalSpaceSize(boolean scaled)
  {
    int size = (Math.max(1, getPersonalSpace()) - 1) | 1;
    if (scaled) {
      return new Dimension(size * 16, size * 12);
    } else {
      return new Dimension(size, size);
    }
  }

  /**
   * Draws the personal space region onto the specified graphics object.
   * @param g the {@code Graphics2D} instance of the image.
   * @param center center position of the personal space.
   * @param color the fill color of the drawn region. Specify {@code null} to use a default color.
   * @param alpha alpha transparency in range [0.0, 1.0] where 0.0 is fully transparent (invisible) and 1.0 is fully opaque.
   */
  protected void drawPersonalSpace(Graphics2D g, Point center, Color color, float alpha)
  {
    if (g != null) {
      BufferedImage image = createPersonalSpace(color, alpha);
      g.drawImage(image, center.x - (image.getWidth() / 2), center.y - (image.getHeight() / 2), null);
    }
  }

  /** Creates a bitmap with the personal space tiles. */
  private BufferedImage createPersonalSpace(Color color, float alpha)
  {
    // preparations
    if (color == null) {
      color = new Color(224, 0, 224);
    }
    alpha = Math.max(0.0f, Math.min(1.0f, alpha));  // clamping alpha to [0.0, 1.0]
    color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * alpha));

    // creating personal space pattern (unscaled)
    Dimension dim = getPersonalSpaceSize(false);
    BufferedImage image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
    int[] bitmap = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    int cx = dim.width / 2;
    int cy = dim.height / 2;
    int c = color.getRGB();
    int maxDist = dim.width * dim.width / 4;
    for (int y = 0; y < dim.height; y++) {
      for (int x = 0; x < dim.width; x++) {
        int ofs = y * dim.width + x;
        int dx = (cx - x) * (cx - x);
        int dy = (cy - y) * (cy - y);
        if (dx + dy < maxDist) {
          bitmap[ofs] = c;
        }
      }
    }

    // scaling up to search map unit size
    dim = getPersonalSpaceSize(true);
    BufferedImage retVal = new BufferedImage(dim.width, dim.height, image.getType());
    Graphics2D g = retVal.createGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      Object oldHints = g.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
      g.drawImage(image, 0, 0, dim.width, dim.height, null);
      g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                         (oldHints != null) ? oldHints : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    } finally {
      g.dispose();
      g = null;
    }

    return retVal;
  }

  /** Calculates the horizontal and vertical radius of the selection circle (ellipse). */
  protected Dimension getSelectionCircleSize()
  {
    Dimension dim = new Dimension();
    dim.width = Math.max(0, getEllipse());
    dim.height = dim.width * 4 / 7;   // ratio 1.75
    if (dim.height % 7 > 3) {
      // rounding up
      dim.height++;
    }
    return dim;
  }

  /** Determines a circle stroke size relative to the circle size. Empty circles have no stroke size. */
  protected float getSelectionCircleStrokeSize()
  {
    float circleStrokeSize;
    if (getEllipse() > 0) {
      // thickness relative to circle size
      circleStrokeSize = Math.max(1.0f, (float)(Math.floor(Math.sqrt(getEllipse()) / 2.0)));
    } else {
      circleStrokeSize = 0.0f;
    }

    return circleStrokeSize;
  }

  /**
   * Draws a selection circle onto the specified graphics object.
   * @param g the {@code Graphics2D} instance of the image.
   * @param center center position of the circle.
   * @param color the circle color. Specify {@code null} to use global defaults.
   * @param strokeSize the thickness of the selection circle.
   */
  protected void drawSelectionCircle(Graphics2D g, Point center, Color color, float strokeSize)
  {
    if (g != null) {
      Dimension dim = getSelectionCircleSize();
      if (color == null) {
        if (getCreatureInfo().isStatusPanic()) {
          // panic
          color = getAllegianceColor(-1);
        } else {
          color = getAllegianceColor(getCreatureInfo().getAllegiance());
        }
      }
      Object oldHints = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(color);
      g.setStroke(new BasicStroke(strokeSize));
      g.drawOval(center.x - dim.width, center.y - dim.height, 2 * dim.width, 2 * dim.height);
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, (oldHints != null) ? oldHints : RenderingHints.VALUE_ANTIALIAS_DEFAULT);
    }
  }

  /**
   * Translates the specified color location index into a palette color offset.
   * @param locationIndex the location to translate.
   * @return the resulting palette color offset. Returns -1 if location is not supported.
   */
  protected int getColorOffset(int locationIndex)
  {
    int retVal = -1;
    if (locationIndex >= 0 && locationIndex < 7) {
      retVal = 4 + locationIndex * 12;
    }
    return retVal;
  }

  /**
   * Returns the palette data for the specified color entry.
   * @param colorIndex the color entry.
   * @return palette data as int array. Returns {@code null} if palette data could not be determined.
   */
  protected int[] getColorData(int colorIndex, boolean allowRandom)
  {
    int[] retVal = null;
    try {
      retVal = SpriteUtils.getColorGradient(colorIndex, allowRandom);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return retVal;
  }

  /**
   * Replaces false colors with color ranges defined in the associated CRE resource.
   * @param control the BAM controller.
   */
  protected void applyFalseColors(BamV1Control control, SegmentDef sd)
  {
    if (control == null || sd == null) {
      return;
    }

    // preparations
    final Map<Integer, int[]> colorRanges = new HashMap<Integer, int[]>();
    for (int loc = 0; loc < 7; loc++) {
      int ofs = getColorOffset(loc);
      Couple<Integer, Boolean> colorInfo = getCreatureInfo().getEffectiveColorValue(sd.getSpriteType(), loc);
      int colIdx = colorInfo.getValue0().intValue();
      boolean allowRandom = colorInfo.getValue1().booleanValue();
      if (ofs > 0 && colIdx >= 0) {
        int[] range = getColorData(colIdx, allowRandom);
        if (range != null) {
          colorRanges.put(ofs, range);
        }
      }
    }

    // applying colors
    int[] palette = control.getCurrentPalette();
    for (final Integer ofs : colorRanges.keySet()) {
      // replacing base ranges
      final int[] range = colorRanges.get(ofs);
      palette = SpriteUtils.replaceColors(palette, range, ofs.intValue(), range.length, false);
    }

    if (getAnimationType() != AnimationType.MONSTER_PLANESCAPE) {
      // preparing offset array
      final int srcOfs = 4;
      final int dstOfs = 88;
      final int srcLen = 12;
      final int dstLen = 8;
      final int[] offsets = new int[colorRanges.size()];
      for (int i = 0; i < offsets.length; i++) {
        offsets[i] = srcOfs + i * srcLen;
      }

      // calculating mixed ranges
      int k = 0;
      for (int i = 0; i < offsets.length - 1; i++) {
        int ofs1 = offsets[i];
        for (int j = i + 1; j < offsets.length; j++, k++) {
          int ofs2 = offsets[j];
          int ofs3 = dstOfs + k * dstLen;
          palette = SpriteUtils.interpolateColors(palette, ofs1, ofs2, srcLen, ofs3, dstLen, false);
        }
      }

      // fixing special palette entries
      palette[2] = 0xFF000000;
      palette[3] = 0xFF000000;
    }

    control.setExternalPalette(palette);
  }

  /**
   * The specified frame is mirrored horizontally. Both pixel data and center point are adjusted.
   * @param frameIndex absolute frame index in the BAM frame list.
   */
  protected void flipImageHorizontal(int frameIndex)
  {
    PseudoBamFrameEntry frame = getFrameInfo(frameIndex);
    // flipping image horizontally
    BufferedImage image = frame.getFrame();
    AffineTransform at = AffineTransform.getScaleInstance(-1, 1);
    at.translate(-image.getWidth(), 0);
    AffineTransformOp op = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
    image = op.filter(image, null);
    // updating frame data
    frame.setFrame(image);
    frame.setCenterX(frame.getWidth() - frame.getCenterX() - 1);
  }

  /**
   * Applies translucency to the specified paletted image.
   * @param control the BAM controller.
   */
  protected void applyTranslucency(BamV1Control control)
  {
    if (control != null) {
      int alpha = getCreatureInfo().getEffectiveTranslucency();
      int[] palette = control.getCurrentPalette();

      // shadow color (alpha relative to semi-transparency of shadow)
      int alphaShadow = 255 - (palette[1] >>> 24);
      alphaShadow = alpha * alphaShadow / 255;
      alphaShadow <<= 24; // setting alpha mask
      palette[1] = alphaShadow | (palette[1] & 0x00ffffff);

      // creature colors
      alpha <<= 24; // setting alpha mask
      for (int i = 2; i < palette.length; i++) {
        palette[i] = alpha | (palette[i] & 0x00ffffff);
      }

      control.setExternalPalette(palette);
    }
  }

  /**
   * Returns whether the current CRE resource provides an animation that is compatible with the
   * {@code SpriteDecoder} class.
   * @return {@code true} if the animation type of the CRE is compatible with this {@code SpriteDecoder} instance.
   *         Returns {@code false} otherwise.
   */
  protected boolean isMatchingAnimationType()
  {
    boolean retVal = false;

    List<String> names = getAnimationFiles(true);
    if (!names.isEmpty()) {
      retVal = names.parallelStream().allMatch(ResourceFactory::resourceExists);
    }

    return retVal;
  }

  /**
   * A helper method for calculating a dimension that can contain all specified source frames.
   * @param frames one or more source frames.
   * @return A rectangle object where x and y indicate the top-left corner relative to the center point.
   *         Width and height specify frame dimension.
   */
  protected static Rectangle getTotalFrameDimension(FrameInfo... frames)
  {
    Rectangle retVal = new Rectangle();

    if (frames.length > 0) {
      int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
      for (final FrameInfo fi : frames) {
        BamV1Control ctrl = fi.getController();
        int frameIdx = fi.getFrame();
        frameIdx = ctrl.cycleGetFrameIndexAbsolute(fi.getCycle(), frameIdx);
        FrameEntry entry = fi.getController().getDecoder().getFrameInfo(frameIdx);
        left = Math.min(left, -entry.getCenterX());
        top = Math.min(top, -entry.getCenterY());
        right = Math.max(right, entry.getWidth() - entry.getCenterX());
        bottom = Math.max(bottom, entry.getHeight() - entry.getCenterY());
      }

      retVal.x = left;
      retVal.y = top;
      retVal.width = right - left;
      retVal.height = bottom - top;
    }

    return retVal;
  }

  /** A helper method that expands the rectangle to fit the specified dimension. */
  protected static Rectangle updateFrameDimension(Rectangle rect, Dimension dim)
  {
    Rectangle retVal = new Rectangle(Objects.requireNonNull(rect, "Bounding box cannot be null"));
    if (dim != null) {
      int w2 = dim.width / 2;
      int h2 = dim.height / 2;
      int left = retVal.x;
      int top = retVal.y;
      int right = left + retVal.width;
      int bottom = top + retVal.height;
      left = Math.min(left, -w2);
      top = Math.min(top, -h2);
      right = Math.max(right, w2);
      bottom = Math.max(bottom, h2);
      retVal.x = left;
      retVal.y = top;
      retVal.width = right - left;
      retVal.height = bottom - top;
    }
    return retVal;
  }


  /**
   * Determines the right allegiance color for selection circles and returns it as {@code Color} object.
   * A negative value will enable the "panic" color.
   * @param value numeric allegiance value. Specify a negative value to override allegiance by the "panic" status.
   * @return a {@code Color} object with the associated allegiance or status color.
   */
  protected static Color getAllegianceColor(int value)
  {
    Color c = null;
    if (value < 0) {
      // treat as panic
      c = new Color(0xffff40, false);
    } else if (value >= 2 && value <= 4 || value == 201) {
      // ally
      c = new Color(0x20ff20, false);
    } else if (value == 255 || value == 254 || value == 28 || value == 6 || value == 5) {
      // enemy
      c = new Color(0xff2020, false);
    } else {
      // neutral
      c = new Color(0x40ffff, false);
    }

    return c;
  }

  /**
   * A helper method that parses the specified data array and generates a list of INI lines
   * related to the "general" section.
   * @param data the String array containing data for a specific table entry.
   * @param type the animation type.
   * @return the initialized "general" INI section as list of strings. An empty list otherwise.
   */
  protected static List<String> processTableDataGeneral(String[] data, AnimationType type)
  {
    List<String> retVal = new ArrayList<>();
    if (data == null || type == null) {
      return retVal;
    }

    int id = SpriteTables.valueToAnimationId(data, SpriteTables.COLUMN_ID, -1);
    if (id < 0) {
      return retVal;
    }
    int ellipse = SpriteTables.valueToInt(data, SpriteTables.COLUMN_ELLIPSE, 16);
    int space = SpriteTables.valueToInt(data, SpriteTables.COLUMN_SPACE, 3);
    int blending = SpriteTables.valueToInt(data, SpriteTables.COLUMN_BLENDING, 0);
    String palette = SpriteTables.valueToString(data, SpriteTables.COLUMN_PALETTE, "");

    int animIndex = SpriteTables.valueToInt(data, SpriteTables.COLUMN_TYPE, -1);
    if (animIndex < 0 || animIndex >= AnimationType.values().length || AnimationType.values()[animIndex] != type) {
      return retVal;
    }

    int animType = -1;
    for (int i = 0; i < type.getTypeCount(); i++) {
      if (animType < 0 || (id & 0xf000) == type.getType(i)) {
        animType = type.getType(i);
      }
    }

    retVal.add("[general]");
    retVal.add(String.format("animation_type=%04X", animType));
    retVal.add("ellipse=" + ellipse);
    retVal.add("personal_space=" + space);
    if ((blending & 1) == 1) {
      retVal.add("brightest=1");
    }
    if ((blending & 2) == 2) {
      retVal.add("multiply_blend=1");
    }
    if ((blending & 4) == 4) {
      retVal.add("light_source=1");
    }
    if (!palette.isEmpty()) {
      retVal.add("new_palette=" + palette);
    }

    return retVal;
  }

  /**
   * A helper method for PST animations that parses the specified data array and generates a list of INI lines
   * related to the "general" section.
   * @param data the String array containing data for a specific table entry.
   * @return the initialized "general" INI section as list of strings. An empty list otherwise.
   */
  protected static List<String> processTableDataGeneralPst(String[] data)
  {
    List<String> retVal = new ArrayList<>();
    if (data == null) {
      return retVal;
    }

    int id = SpriteTables.valueToInt(data, SpriteTables.COLUMN_ID, -1);
    if (id < 0) {
      return retVal;
    }
    int ellipse = SpriteTables.valueToInt(data, SpriteTables.COLUMN_PST_ELLIPSE, 16);
    int space = SpriteTables.valueToInt(data, SpriteTables.COLUMN_PST_SPACE, 3);

    retVal.add("[general]");
    retVal.add("animation_type=F000");
    retVal.add("ellipse=" + ellipse);
    retVal.add("personal_space=" + space);

    return retVal;
  }

  /**
   * Returns whether the specified {@code SpriteDecoder} class is compatible with the given animation id
   * and any of the IniMap definitions.
   */
  private static boolean isSpriteDecoderAvailable(Class<? extends SpriteDecoder> spriteClass, int animationId, List<IniMap> iniList)
  {
    boolean retVal = false;
    if (spriteClass == null || iniList == null) {
      return retVal;
    }

    try {
      Constructor<? extends SpriteDecoder> ctor = spriteClass.getConstructor(int.class, IniMap.class);
      if (ctor != null) {
        for (final IniMap ini : iniList) {
          try {
            retVal = (ctor.newInstance(animationId, ini).getClass() != null);
          } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
          }
        }
      }
    } catch (NoSuchMethodException e) {
    }

    return retVal;
  }

  /**
   * Attempts to determine the animation type assigned to the specified creature.
   * @return Class instance responsible for handling the detected animation type. {@code null} if type could not be determined.
   */
  protected static Class<? extends SpriteDecoder> detectAnimationType(int animationId)
  {
    Class<? extends SpriteDecoder> retVal = null;

    List<IniMap> iniList = new ArrayList<>();
    iniList.addAll(getAnimationInfoByIni(animationId));

    if (iniList.isEmpty()) {
      iniList.addAll(getAnimationInfoByTable(animationId));
    }

    if (iniList.isEmpty()) {
      iniList.addAll(getAnimationInfoByGuess(animationId));
    }

    if (!iniList.isEmpty()) {
      // trying recommended sprite decoder class first
      Class<? extends SpriteDecoder> cls = getSpriteDecoderClass(animationId);
      if (isSpriteDecoderAvailable(cls, animationId, iniList)) {
        retVal = cls;
      }

      if (retVal == null) {
        // trying out all available sprite decoder classes otherwise
        if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
          if (isSpriteDecoderAvailable(MonsterPlanescapeDecoder.class, animationId, iniList)) {
            retVal = cls;
          }
        } else {
          for (final AnimationType type : AnimationType.values()) {
            if (type != AnimationType.MONSTER_PLANESCAPE) {
              cls = typeAssociations.get(type);
              if (isSpriteDecoderAvailable(cls, animationId, iniList)) {
                retVal = cls;
                break;
              }
            }
          }
        }
      }
    }

    return retVal;
  }

  /**
   * Returns creature animation info from an existing INI file.
   * @param animationId the creature animation id
   * @return an list of {@link IniMap} instances with potential creature animation data.
   *         Returns {@code null} if no matching INI was found.
   */
  protected static List<IniMap> getAnimationInfoByIni(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();

    animationId &= 0xffff;
    String iniFile = String.format("%04X.INI", animationId);
    if (ResourceFactory.resourceExists(iniFile)) {
      retVal.add(new IniMap(ResourceFactory.getResourceEntry(iniFile), true));
    }

    return retVal;
  }

  /**
   * Returns creature animation info from hardcoded creature data.
   * @param animationId the creature animation id
   * @return an list of {@link IniMap} instance with potential creature animation data.
   *         Returns empty list if no creature data was found.
   */
  protected static List<IniMap> getAnimationInfoByTable(int animationId)
  {
    return SpriteTables.createIniMaps(animationId & 0xffff);
  }

  /**
   * Returns creature animation info based on ANISND.2DA data and analyzing potential slot ranges.
   * May return false positives.
   * @param animationId the creature animation id
   * @return a list of {@link IniMap} instances with potential creature animation data.
   *         Returns {@code null} if no potential match was found.
   */
  protected static List<IniMap> getAnimationInfoByGuess(int animationId)
  {
    if (Profile.getGame() == Profile.Game.PST || Profile.getGame() == Profile.Game.PSTEE) {
      return guessIniMapsPst(animationId);
    } else {
      return guessIniMaps(animationId);
    }
  }

  /**
   * Returns creature animation info in INI format. Section and field format is based on the EE v2.0 INI format.
   * The method will first look for existing INI data in the game resources. Failing that it will look up data in
   * hardcoded tables and fill in missing data from associated 2DA file if available. Failing that it will guess
   * the correct format based on animation type and available resources.
   * @param animationId the 16-bit animation id.
   * @return An IniMap structure containing necessary data for rendering creature animation. Returns {@code null} if no
   *         animation info could be assembled.
   */
  protected static IniMap getAnimationInfo(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();

    // 1. look up existing INI resource
    retVal.addAll(getAnimationInfoByIni(animationId));

    if (retVal.isEmpty()) {
      // 2. look up hardcoded tables
      retVal.addAll(getAnimationInfoByTable(animationId));
    }

    if (retVal.isEmpty()) {
      // 3. guess animation info based on anisnd.2da entry and available sprite classes
      retVal.addAll(getAnimationInfoByGuess(animationId));
    }

    if (!retVal.isEmpty()) {
      return retVal.get(0);
    } else {
      return null;
    }
  }

  // Attempts to find potential non-PST-specific IniMap instances
  private static List<IniMap> guessIniMaps(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    String resref = null;
    String palette = null;

    // evaluate ANIMATE.SRC if available
    ResourceEntry resEntry = ResourceFactory.getResourceEntry("ANIMATE.SRC");
    if (resEntry != null) {
      IniMap anisrc = IniMapCache.get(resEntry);
      if (anisrc != null) {
        IniMapSection iniSection = anisrc.getUnnamedSection();
        if (iniSection != null) {
          for (final Iterator<IniMapEntry> iter = iniSection.iterator(); iter.hasNext(); ) {
            IniMapEntry entry = iter.next();
            try {
              String key = entry.getKey();
              int id = (key.startsWith("0x") || key.startsWith("0X")) ? Misc.toNumber(key.substring(2, key.length()), 16, -1)
                  : Misc.toNumber(key, -1);
              if (id == animationId) {
                String value = entry.getValue();
                if (id > 0x1000 && value.length() > 4) {
                  value = value.substring(0, 4);
                }
                resref = value;
                break;
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }

    if (resref == null) {
      // evaluate ANISND.IDS if available
      IdsMap anisnd = IdsMapCache.get("ANISND.IDS");
      if (anisnd != null) {
        IdsMapEntry anisndEntry = anisnd.get(animationId);
        if (anisndEntry != null) {
          String[] elements = anisndEntry.getSymbol().split("\\s+");
          if (elements.length > 0 && elements[0].length() <= 8) {
            resref = elements[0];
            int pos = resref.indexOf('_');
            if (pos > 0) {
              // assuming underscore indicates a palette resref
              palette = resref;
              resref = resref.substring(0, pos);
            } else if (animationId >= 0x1000 && resref.length() > 4) {
              resref = resref.substring(0, 4);
            }
          }
        }
      }
    }

    if (resref == null) {
      return retVal;
    }

    if (palette == null) {
      palette = "*";
    }

    List<String> tableEntries = new ArrayList<>();
    AnimationType type = AnimationType.typeOfId(animationId);
    if (type == null) {
      return retVal;
    }

    ResourceEntry bamEntry;
    switch (type) {
      case EFFECT:
        tableEntries.add(String.format("0x%04x %s 0 0 0 * %s * * * * * * * * *", animationId, resref, palette));
        break;
      case MONSTER_QUADRANT:
        if (ResourceFactory.resourceExists(resref + "G14.BAM")) {
          tableEntries.add(String.format("0x%04x %s 1 32 5 * %s * * * * * * * * *", animationId, resref, palette));
        }
        break;
      case MONSTER_MULTI:
        if (ResourceFactory.resourceExists(resref + "1908.BAM")) {
          tableEntries.add(String.format("0x%04x %s 2 72 13 * %s * * * * 1 * * * *", animationId, resref, palette));
        }
        break;
      case MONSTER_MULTI_NEW:
        if (ResourceFactory.resourceExists(resref + "G145.BAM")) {
          tableEntries.add(String.format("0x%04x %s 2 32 5 * %s * * * * 1 * * * *", animationId, resref, palette));
        } else if (ResourceFactory.resourceExists(resref + "G1.BAM")) {
          tableEntries.add(String.format("0x%04x %s 2 32 5 * %s * * * * 0 * * * *", animationId, resref, palette));
        }
        break;
      case MONSTER_LAYERED_SPELL:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 4 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_ANKHEG:
        resref = guessResourceRef(resref, "DG1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "DG1.BAM");
        if (bamEntry != null) {
          tableEntries.add(String.format("0x%04x %s 6 24 5 * %s * * * * * * * * *", animationId, resref, palette));
        }
        break;
      case TOWN_STATIC:
        resref = guessResourceRef(resref, "");
        bamEntry = ResourceFactory.getResourceEntry(resref + ".BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 7 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case CHARACTER:
        bamEntry = ResourceFactory.getResourceEntry(resref + "1G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          int split = ResourceFactory.resourceExists(resref + "1G15.BAM") ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 8 16 3 * * * * * %d %d 1 * * *", animationId, resref, falseColor, split));
        }
        break;
      case CHARACTER_OLD:
        bamEntry = ResourceFactory.getResourceEntry(resref + "1G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 9 16 3 * * * * * %d * * * * *", animationId, resref, falseColor));
        }
        break;
      case MONSTER:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int split = ResourceFactory.resourceExists(resref + "G15.BAM") ? 1 : 0;
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 10 16 3 * %s * * * %d %d * * * *", animationId, resref, palette, falseColor, split));
          tableEntries.add(String.format("0x%04x %s 11 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_OLD:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 11 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_LAYERED:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          tableEntries.add(String.format("0x%04x %s 4 16 3 * * * * * * * * * * *", animationId, resref));
        }
        break;
      case MONSTER_LARGE:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 12 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_LARGE_16:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 13 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case AMBIENT_STATIC:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 14 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case AMBIENT:
        resref = guessResourceRef(resref, "G1");
        bamEntry = ResourceFactory.getResourceEntry(resref + "G1.BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 15 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case FLYING:
        resref = guessResourceRef(resref, "");
        bamEntry = ResourceFactory.getResourceEntry(resref + ".BAM");
        if (bamEntry != null) {
          int falseColor = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
          tableEntries.add(String.format("0x%04x %s 16 16 3 * %s * * * %d * * * * *", animationId, resref, palette, falseColor));
        }
        break;
      case MONSTER_ICEWIND:
      {
        boolean found = false;
        while (resref.length() >= 4 && !found) {
          for (final String suffix : new String[] { "A1", "A2", "A3", "A4", "CA", "DE", "GH", "GU", "SC", "SD", "SL", "SP", "TW", "WK" }) {
            if (ResourceFactory.resourceExists(resref + suffix + ".BAM")) {
              found = true;
              break;
            }
          }
        }
        if (found) {
          tableEntries.add(String.format("0x%04x %s 17 24 3 * %s * * * * * * * * *", animationId, resref, palette));
        }
        break;
      }
      default:
    }

    if (!tableEntries.isEmpty()) {
      for (final String line : tableEntries) {
        StringBuilder sb = new StringBuilder();
        sb.append("2DA V1.0").append('\n');
        sb.append("*").append('\n');
        sb.append("  RESREF TYPE ELLIPSE SPACE BLENDING PALETTE PALETTE2 RESREF2 TRANSLUCENT CLOWN SPLIT HELMET WEAPON HEIGHT HEIGHT_SHIELD").append('\n');
        sb.append(line).append('\n');
        ResourceEntry entry = new BufferedResourceEntry(ByteBuffer.wrap(sb.toString().getBytes()), Integer.toString(animationId, 16) + ".2DA");
        Table2da table = new Table2da(entry);
        retVal.addAll(SpriteTables.processTable(Profile.getGame(), table, animationId));
      }
    }

    return retVal;
  }

  // Helper method: attempts to find an existing resource with the specified name parts.
  // Returns the resref of the matching resource. Returns the original resref otherwise.
  private static String guessResourceRef(String resref, String suffix)
  {
    String retVal = resref;
    if (retVal == null) {
      return retVal;
    }

    if (suffix == null) {
      suffix = "";
    }

    while (retVal.length() >= 4) {
      if (ResourceFactory.resourceExists(retVal + suffix + ".BAM")) {
        return retVal;
      }
      retVal = retVal.substring(0, resref.length() - 1);
    }

    return resref;
  }

  // Attempts to find potential PST-specific IniMap instances
  private static List<IniMap> guessIniMapsPst(int animationId)
  {
    List<IniMap> retVal = new ArrayList<>();
    String resref = null;

    IniMap resIni = IniMapCache.get("RESDATA.INI", true);
    if (resIni == null) {
      return retVal;
    }

    // only regular animations are considered
    int id = animationId & 0x0fff;
    IniMapSection iniSection = resIni.getSection(Integer.toString(id));
    if (iniSection == null) {
      iniSection = resIni.getSection("0x" + Integer.toString(id, 16));
    }
    if (iniSection == null) {
      return retVal;
    }

    int clown = 0;
    for (final Sequence seq : Sequence.values()) {
      String cmd = MonsterPlanescapeDecoder.getActionCommand(seq);
      if (cmd != null) {
        String key = iniSection.getAsString(cmd);
        if (key != null && key.length() >= 7) {
          ResourceEntry bamEntry = ResourceFactory.getResourceEntry(key + "b.bam");
          if (bamEntry != null) {
            clown = SpriteUtils.bamHasFalseColors(bamEntry) ? 1 : 0;
            resref = key.substring(0, 1) + key.substring(4, key.length()) + "b";
            break;
          }
        }
      }
    }

    if (resref != null) {
      int armor = iniSection.getAsInteger("armor", 0);
      int bestiary = iniSection.getAsInteger("bestiary", 0);

      StringBuilder sb = new StringBuilder();
      sb.append("2DA V1.0").append('\n');
      sb.append("*").append('\n');
      sb.append("         RESREF   RESREF2  TYPE     ELLIPSE  SPACE    CLOWN    ARMOR    BESTIARY").append('\n');
      sb.append(String.format("0x%04x  %s  *  18  16  3  %d  %d  %d", id, resref, clown, armor, bestiary)).append('\n');
      ResourceEntry entry = new BufferedResourceEntry(ByteBuffer.wrap(sb.toString().getBytes()), Integer.toString(animationId, 16) + ".2DA");
      Table2da table = new Table2da(entry);
      retVal = SpriteTables.processTable(Profile.getGame(), table, animationId);
    }

    return retVal;
  }

  @Override
  public int hashCode()
  {
    int hash = super.hashCode();
    hash = 31 * hash + ((creInfo == null) ? 0 : creInfo.hashCode());
    hash = 31 * hash + ((ini == null) ? 0 : ini.hashCode());
    hash = 31 * hash + ((directionMap == null) ? 0 : directionMap.hashCode());
    hash = 31 * hash + ((attributesMap == null) ? 0 : attributesMap.hashCode());
    hash = 31 * hash + ((currentSequence == null) ? 0 : currentSequence.hashCode());
    hash = 31 * hash + Boolean.valueOf(showCircle).hashCode();
    hash = 31 * hash + Boolean.valueOf(showPersonalSpace).hashCode();
    hash = 31 * hash + Boolean.valueOf(renderSpriteWeapon).hashCode();
    hash = 31 * hash + Boolean.valueOf(renderSpriteHelmet).hashCode();
    hash = 31 * hash + Boolean.valueOf(renderSpriteShield).hashCode();
    hash = 31 * hash + Boolean.valueOf(animationChanged).hashCode();
    hash = 31 * hash + Boolean.valueOf(autoApplyChanges).hashCode();
    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!(o instanceof SpriteDecoder)) {
      return false;
    }
    boolean retVal = super.equals(o);
    if (retVal) {
      SpriteDecoder other = (SpriteDecoder)o;
      retVal &= (this.creInfo == null && other.creInfo == null) ||
                (this.creInfo != null && this.creInfo.equals(other.creInfo));
      retVal &= (this.ini == null && other.ini == null) ||
                (this.ini != null && this.ini.equals(other.ini));
      retVal &= (this.directionMap == null && other.directionMap == null) ||
                (this.directionMap != null && this.directionMap.equals(other.directionMap));
      retVal &= (this.attributesMap == null && other.attributesMap == null) ||
                (this.attributesMap != null && this.attributesMap.equals(other.attributesMap));
      retVal &= (this.currentSequence == null && other.currentSequence == null) ||
                (this.currentSequence != null && this.currentSequence.equals(other.currentSequence));
      retVal &= (this.showCircle == other.showCircle);
      retVal &= (this.showPersonalSpace == other.showPersonalSpace);
      retVal &= (this.renderSpriteWeapon == other.renderSpriteWeapon);
      retVal &= (this.renderSpriteHelmet == other.renderSpriteHelmet);
      retVal &= (this.renderSpriteShield == other.renderSpriteShield);
      retVal &= (this.animationChanged == other.animationChanged);
      retVal &= (this.autoApplyChanges == other.autoApplyChanges);
    }
    return retVal;
  }

//-------------------------- INNER CLASSES --------------------------

  /**
   * A helper class that allows you to define numeric ranges.
   */
  public static class Range
  {
    private final List<Couple<Integer, Integer>> ranges = new ArrayList<>();

    /**
     * Defines a range that starts at <code>base</code> and ends at <code>base + range</code> (inclusive).
     * @param base start value of the range.
     * @param range length of the range.
     */
    public Range(int base, int range)
    {
      this(base, range, 0, 0, 0);
    }

    /**
     * Defines a set of common ranges. The resolved list of ranges can be defined as:<br><br>
     * <code>base + ([subBase, subBase+subRange] << subPos) + [0, range]</code><br><br>
     * where [x, y] defines a range from x to y (inclusive).
     * @param base start value of the range.
     * @param range length of the range.
     */
    public Range(int base, int range, int subBase, int subRange, int subPos)
    {
      init(base, range, subBase, subRange, subPos);
    }

    /** Returns whether the range covers the specified value. */
    public boolean contains(int value)
    {
      return ranges
          .parallelStream()
          .anyMatch(c -> (value >= c.getValue0().intValue() &&
                          value <= (c.getValue0().intValue() + c.getValue1().intValue())));
    }

    private void init(int base, int range, int subBase, int subRange, int subPos)
    {
      range = Math.abs(range);
      subRange = Math.abs(subRange);
      subPos = Math.max(0, Math.min(32, subPos));
      for (int i = 0; i <= subRange; i++) {
        int curBase = base + ((subBase + i) << subPos);
        ranges.add(Couple.with(curBase, range));
      }
    }
  }

  /**
   * Represents an operation that is called once per source BAM resource when creating a creature animation.
   */
  public interface BeforeSourceBam
  {
    /**
     * Performs this operation on the given arguments.
     * @param control the {@code BamV1Control} instance of the source BAM
     * @param sd the {@code SegmentDef} instance describing the source BAM.
     */
    void accept(BamV1Control control, SegmentDef sd);
  }

  /**
   * Represents a function that is called for each source frame before it is drawn onto the destination image.
   */
  public interface BeforeSourceFrame
  {
    /**
     * Performs this function on the given arguments.
     * @param sd the {@code SegmentDef} structure describing the given source frame.
     * @param srcImage {@code BufferedImage} object of the the source frame
     * @param g the {@code Graphics2D} object of the destination image.
     * @return the updated source frame image
     */
    BufferedImage apply(SegmentDef sd, BufferedImage srcImage, Graphics2D g);
  }

  /**
   * Represents an operation that is called for each source frame after it has been drawn onto the destination image.
   * It can be used to clean up modifications made to the {@code Graphics2D} instance
   * in the {@code BeforeSourceFrame} function.
   */
  public interface AfterSourceFrame
  {
    /**
     * Performs this operation on the given arguments.
     * @param sd the {@code SegmentDef} structure describing the current source frame.
     * @param g the {@code Graphics2D} object of the destination image.
     */
    void accept(SegmentDef sd, Graphics2D g);
  }

  /**
   * Represents an operation that is called for each destination frame after it has been created.
   */
  public interface AfterDestFrame
  {
    /**
     * Performs this operation on the given arguments.
     * @param dd the {@code DirDef} object defining the current destination cycle
     * @param frameIdx the absolute destination BAM frame index.
     */
    void accept(DirDef dd, int frameIdx);
  }
}
