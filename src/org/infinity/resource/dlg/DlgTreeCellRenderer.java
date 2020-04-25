// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.dlg;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.ViewerUtil;

/**
 * Renderer for dialogue tree, drawing elements of each dialog with its own color
 * (maximum of {@link #OTHER_DIALOG_COLORS}{@code .length} different colors).
 * <p>
 * Also, draws non-main items in gray, broken references (items, that refers from
 * {@link State} or {@link Transition} but that do not exists in the target {@link
 * DlgResource}) in red and transition items in blue.
 *
 * @author Mingun
 */
final class DlgTreeCellRenderer extends DefaultTreeCellRenderer
{
  /** Background colors for text in dialogs to that can refer main dialog. */
  private final HashMap<DlgResource, Color> dialogColors = new HashMap<>();
  /** Main dialogue that shown in the tree. */
  private final DlgResource dlg;

  public DlgTreeCellRenderer(DlgResource dlg) { this.dlg = dlg; }

  @Override
  public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
                                                boolean expanded, boolean leaf, int row,
                                                boolean focused)
  {
    final Component c = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, focused);
    // Tree reuse component, so we need to clear background
    setBackgroundNonSelectionColor(null);
    if (!(value instanceof ItemBase)) return c;

    final ItemBase item = (ItemBase)value;

    final BrowserMenuBar options = BrowserMenuBar.getInstance();
    setIcon(options.showDlgTreeIcons() ? item.getIcon() : null);
    setBackgroundNonSelectionColor(options.colorizeOtherDialogs() ? getColor(item.getDialog()) : null);

    if (options.useDifferentColorForResponses() && item instanceof TransitionItem) {
      setForeground(Color.BLUE);
    }

    if (item instanceof BrokenReference) {
      setForeground(Color.RED);// Broken reference
    } else
    if (item instanceof StateItem) {
      final StateItem state = (StateItem)item;
      final State s = state.getEntry();
      if (s.getNumber() == 0 && s.getTriggerIndex() < 0 && options.alwaysShowState0()) {
        setForeground(Color.GRAY);
      }
    }
    if (item.getMain() != null) {
      setForeground(Color.GRAY);
    }
    return c;
  }

  private Color getColor(DlgResource dialog)
  {
    if (dlg == dialog) {
      return null;
    }
    return dialogColors.computeIfAbsent(dialog,
        d -> ViewerUtil.BACKGROUND_COLORS[dialogColors.size() % ViewerUtil.BACKGROUND_COLORS.length]
    );
  }
}
