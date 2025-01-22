// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JComponent;

import org.infinity.resource.wmp.AreaEntry;
import org.infinity.resource.wmp.AreaLink;
import org.infinity.resource.wmp.MapEntry;

/**
 * A specialized bitmap to represent linked areas in a WMP resource.
 * <p>
 * The bitmap list is lazily populated to avoid performing read-ahead operations.
 * </p>
 */
public class WmpLinkBitmap extends AbstractBitmap<String> {
  public WmpLinkBitmap(ByteBuffer buffer, int offset, int length, String name) {
    super(buffer, offset, length, name, new TreeMap<>());
    setFormatter(formatterHashBitmapReverse);
  }

  // --------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container) {
    updateLinkList();
    return super.edit(container);
  }

  // --------------------- End Interface Editable ---------------------

  @Override
  public String toString() {
    updateLinkList();
    return super.toString();
  }

  private void updateLinkList() {
    if (getBitmap().isEmpty() &&
        getParent() instanceof AreaLink &&
        getParent().getParent() instanceof AreaEntry &&
        getParent().getParent().getParent() instanceof MapEntry) {
      final TreeMap<Long, String> map = getBitmap();
      final MapEntry mapEntry = (MapEntry)getParent().getParent().getParent();
      final List<AreaEntry> areas = mapEntry.getCachedAreas();
      for (int i = 0, count = areas.size(); i < count; i++) {
        final ResourceRef currentArea = (ResourceRef)areas.get(i).getAttribute(AreaEntry.WMP_AREA_CURRENT);
        String label = (currentArea != null) ? currentArea.toString() : "[Unknown]";
        map.put(Long.valueOf(i), label);
      }
    }
  }
}
