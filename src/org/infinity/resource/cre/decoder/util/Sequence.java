// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.decoder.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Available animation sequences. Note: PST-specific animation sequences are prefixed by "PST_".
 */
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

  private static final List<Sequence> DEFAULT_SEQUENCES = new ArrayList<Sequence>() {{
    add(Sequence.STAND);
    add(Sequence.STAND2);
    add(Sequence.STAND3);
    add(Sequence.STAND_EMERGED);
    add(Sequence.PST_STAND);
    add(Sequence.STANCE);
    add(Sequence.STANCE2);
    add(Sequence.PST_STANCE);
    add(Sequence.WALK);
    add(Sequence.PST_WALK);
  }};

  private final String desc;

  /** Creates a new {@code AnimationSequence} with an empty label. */
  private Sequence() { this(null); }

  /** Creates a new {@code AnimationSequence} with the specified label. */
  private Sequence(String desc) { this.desc = desc; }

  @Override
  public String toString()
  {
    return (desc != null) ? desc : super.toString();
  }

  /**
   * Returns a list of default animation sequences that are safe for initializing a new
   * creature animation in order of preference.
   */
  public static List<Sequence> getDefaultSequences() { return DEFAULT_SEQUENCES; }
}
