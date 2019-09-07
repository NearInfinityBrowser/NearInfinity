// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2019 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.ui;

import java.util.ArrayList;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;

import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;

/**
 *
 * @author Mingun
 */
public class ResourceListModel extends AbstractListModel<ResourceEntry>
                               implements ComboBoxModel<ResourceEntry>
{
  private final ArrayList<ResourceEntry> resources = new ArrayList<>();
  private ResourceEntry selected;

  /**
   * Initializes this model with all resources of specified types.
   * Internal list sorted according to resource names.
   *
   * @param types Array of resource types, that this model must contain
   */
  public ResourceListModel(String... types)
  {
    for (final String type : types) {
      resources.addAll(ResourceFactory.getResources(type));
    }
    resources.sort(null);
  }

  @Override
  public void setSelectedItem(Object anItem) { selected = (ResourceEntry)anItem; }

  @Override
  public ResourceEntry getSelectedItem() { return selected; }

  @Override
  public int getSize() { return 1 + resources.size(); }

  @Override
  public ResourceEntry getElementAt(int index)
  {
    return index == 0 ? null : resources.get(index - 1);
  }
}
