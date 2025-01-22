// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp.viewer;

import java.awt.Component;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import org.infinity.datatype.IsNumeric;
import org.infinity.datatype.IsReference;
import org.infinity.datatype.StringRef;
import org.infinity.gui.ViewerUtil.ListValueRenderer;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.graphics.BamDecoder.FrameEntry;
import org.infinity.resource.wmp.AreaEntry;
import org.infinity.util.StringTable;

/**
 * Specialized list item renderer for worldmap area entries.
 */
final class AreaListCellRenderer extends DefaultListCellRenderer implements ListValueRenderer {
  private static final int TEXT_GAP = 4;

  private final BamDecoder bam;
  private final BamControl ctrl;
  private final int maxFrameWidth;

  public AreaListCellRenderer(BamDecoder decoder) {
    bam = decoder;
    ctrl = (bam != null) ? bam.createControl() : null;

    int maxWidth = 0;
    for (int i = 0, count = ctrl.cycleCount(); i < count; i++) {
      if (ctrl.cycleFrameCount(i) > 0) {
        final int frameIndex = ctrl.cycleGetFrameIndexAbsolute(i, 0);
        if (frameIndex >= 0) {
          final FrameEntry fe = bam.getFrameInfo(frameIndex);
          if (fe != null) {
            maxWidth = Math.max(maxWidth, fe.getWidth());
          }
        }
      }
    }
    maxFrameWidth = maxWidth;
  }

  @Override
  public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
      boolean cellHasFocus) {
    JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
    label.setText(getListValue(value, true));

    int iconIndex = -1;
    if (value instanceof AbstractStruct) {
      final AbstractStruct struct = (AbstractStruct)value;
      iconIndex = ((IsNumeric) struct.getAttribute(AreaEntry.WMP_AREA_ICON_INDEX)).getValue();
    } else if (value instanceof VirtualAreaEntry) {
      final VirtualAreaEntry vae = (VirtualAreaEntry)value;
      iconIndex = vae.getAreaIconIndex();
    }

    if (ctrl != null && iconIndex >= 0) {
      final int frameIndex = ctrl.cycleGetFrameIndexAbsolute(iconIndex, 0);
      setIcon(new ImageIcon(bam.frameGet(ctrl, frameIndex)));

      // properly aligning label text
      final FrameEntry fe = bam.getFrameInfo(frameIndex);
      final int gap = Math.max(0, maxFrameWidth - fe.getWidth());
      setIconTextGap(gap + TEXT_GAP);
    } else {
      setIcon(null);
      setIconTextGap(maxFrameWidth + TEXT_GAP);
    }

    return label;
  }

  @Override
  public String getListValue(Object value) {
    return getListValue(value, false);
  }

  private String getListValue(Object value, boolean showFull) {
    String text1 = "";
    String text2 = "";
    StringTable.Format fmt = BrowserMenuBar.getInstance().getOptions().showStrrefs() ? StringTable.Format.STRREF_SUFFIX
        : StringTable.Format.NONE;

    if (value instanceof AbstractStruct) {
      final AbstractStruct struct = (AbstractStruct)value;

      StringRef areaName = (StringRef)struct.getAttribute(AreaEntry.WMP_AREA_NAME);
      IsReference areaRef = (IsReference)struct.getAttribute(AreaEntry.WMP_AREA_CURRENT);
      if (areaName.getValue() >= 0) {
        text1 = StringTable.getStringRef(areaName.getValue(), fmt);
      }
      text2 = areaRef.getResourceName();
    } else if (value instanceof VirtualAreaEntry) {
      final VirtualAreaEntry vae = (VirtualAreaEntry)value;
      text1 = StringTable.getStringRef(vae.getAreaLabelStrref(), fmt);
      text2 = vae.getAreaResource().getResourceName();
    }

    if (!text2.equalsIgnoreCase("NONE")) {
      text2 = text2.toUpperCase(Locale.ENGLISH).replace(".ARE", "");
    }

    if (showFull) {
      return '[' + text2 + "] " + text1;
    }
    return text2;
  }
}