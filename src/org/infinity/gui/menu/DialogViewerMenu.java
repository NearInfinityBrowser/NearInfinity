// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.menu;

import java.awt.Color;
import java.util.prefs.Preferences;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

/**
 * Handles the "Dialog Tree Viewer" menu items of the Options menu for the {@link BrowserMenuBar}.
 */
public class DialogViewerMenu extends JMenu {
  private static final String OPTION_SHOWICONS              = "DlgShowIcons";
  private static final String OPTION_SORT_STATES_BY_WEIGHT  = "DlgSortStatesByWeight";
  private static final String OPTION_ALWAYS_SHOW_STATE_0    = "DlgAlwaysShowState0";
  private static final String OPTION_COLORIZE_OTHER_DIALOGS = "DlgColorizeOtherDialogs";
  private static final String OPTION_BREAK_CYCLES           = "DlgBreakCycles";
  private static final String OPTION_COLORIZE_RESPONSES     = "DlgColorizeResponses";
  private static final String OPTION_SHOW_TECH_INFO         = "DlgShowTechInfo";

  /**
   * If checked, the tree will show icons on which it is possible to distinguish nodes with NPC replies from nodes of
   * the player responses. It is unknown for what reasons be necessary it to switch off. By default this option is on.
   */
  private final JCheckBoxMenuItem showIcons;

  /**
   * If checked, root states are sorted by the processing order in the game engine. This order is based on the state
   * trigger index.
   */
  private final JCheckBoxMenuItem sortStatesByWeight;

  /**
   * If checked, state 0 in dialogs will be always visible under root. This is useful for exploring dialogs, that in
   * game started only from other dialogs, and never as independent entity. By default this option is off.
   */
  private final JCheckBoxMenuItem alwaysShowState0;

  /**
   * If checked, background of states and transitions from other dialogs will be drawn in other colors. By default
   * this option is on.
   */
  private final JCheckBoxMenuItem colorizeOtherDialogs;

  /**
   * If checked, the tree will not allow to open nodes which already met earlier in other place of a tree. By default
   * this option is on.
   */
  private final JCheckBoxMenuItem breakCyclesInDialogs;

  /**
   * If checked, transition items in the tree will be drawn in {@link Color#BLUE blue}. By default this option is off.
   */
  private final JCheckBoxMenuItem differentColorForResponses;

  /**
   * If checked, in the tree will be shown technical information: state or transition number. By default this option
   * is on.
   */
  private final JCheckBoxMenuItem showTechInfo;

  public DialogViewerMenu(Preferences prefs) {
    super("Dialog Tree Viewer");
    showIcons = new JCheckBoxMenuItem("Show icons", prefs.getBoolean(OPTION_SHOWICONS, true));
    add(showIcons);
    sortStatesByWeight = new JCheckBoxMenuItem("Sort states by weight",
        prefs.getBoolean(OPTION_SORT_STATES_BY_WEIGHT, false));
    add(sortStatesByWeight);
    alwaysShowState0 = new JCheckBoxMenuItem("Always show State 0",
        prefs.getBoolean(OPTION_ALWAYS_SHOW_STATE_0, false));
    add(alwaysShowState0);
    colorizeOtherDialogs = new JCheckBoxMenuItem("Show colored entries from other dialogs",
        prefs.getBoolean(OPTION_COLORIZE_OTHER_DIALOGS, true));
    add(colorizeOtherDialogs);
    breakCyclesInDialogs = new JCheckBoxMenuItem("Break cycles (NWN like tree)",
        prefs.getBoolean(OPTION_BREAK_CYCLES, true));
    add(breakCyclesInDialogs);
    differentColorForResponses = new JCheckBoxMenuItem("Use different color for responses (PC replies)",
        prefs.getBoolean(OPTION_COLORIZE_RESPONSES, false));
    add(differentColorForResponses);
    showTechInfo = new JCheckBoxMenuItem("Show state/response numbers",
        prefs.getBoolean(OPTION_SHOW_TECH_INFO, true));
    add(showTechInfo);
  }

  /** Returns whether the dialog tree viewer shows icons in front of state and response entries. */
  public boolean showDlgTreeIcons() {
    return showIcons.isSelected();
  }

  /** Returns whether root states are sorted by processing order (based on state trigger index). */
  public boolean sortStatesByWeight() {
    return sortStatesByWeight.isSelected();
  }

  /** Returns whether state 0 is always shown as a root node in the dialog tree viewer. */
  public boolean alwaysShowState0() {
    return alwaysShowState0.isSelected();
  }

  /** Returns whether external dialog references are shown with a colored background in the dialog tree viewer. */
  public boolean colorizeOtherDialogs() {
    return colorizeOtherDialogs.isSelected();
  }

  /**
   * Returns whether duplicate states are combined and only shown once to break infinite loops in the dialog tree
   * viewer.
   */
  public boolean breakCyclesInDialogs() {
    return breakCyclesInDialogs.isSelected();
  }

  /** Returns whether response entries in the dialog tree viewer are shown with a colored background. */
  public boolean useDifferentColorForResponses() {
    return differentColorForResponses.isSelected();
  }

  /** Returns whether additional information about the dialog is shown in the dialog tree viewer. */
  public boolean showDlgTechInfo() {
    return showTechInfo.isSelected();
  }

  public void storePreferences(Preferences prefs) {
    prefs.putBoolean(OPTION_SHOWICONS, showIcons.isSelected());
    prefs.putBoolean(OPTION_SORT_STATES_BY_WEIGHT, sortStatesByWeight.isSelected());
    prefs.putBoolean(OPTION_ALWAYS_SHOW_STATE_0, alwaysShowState0.isSelected());
    prefs.putBoolean(OPTION_COLORIZE_OTHER_DIALOGS, colorizeOtherDialogs.isSelected());
    prefs.putBoolean(OPTION_BREAK_CYCLES, breakCyclesInDialogs.isSelected());
    prefs.putBoolean(OPTION_COLORIZE_RESPONSES, differentColorForResponses.isSelected());
    prefs.putBoolean(OPTION_SHOW_TECH_INFO, showTechInfo.isSelected());
  }
}
