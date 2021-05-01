// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.hexview;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.infinity.gui.DataMenuItem;
import org.infinity.gui.StructViewer;
import org.infinity.gui.ViewFrame;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.StructEntry;

import tv.porst.jhexview.JHexView;

/**
 * Provides a dynamic popupmenu for the StructHexViewer component.
 */
public class ResourceMenuCreator extends MenuCreator
{
  private final AbstractStruct struct;


  public ResourceMenuCreator(JHexView hexView, AbstractStruct struct)
  {
    super(hexView);

    if (struct == null) {
      throw new NullPointerException("struct is null");
    }
    this.struct = struct;
  }

//--------------------- Begin Interface IMenuCreator ---------------------

  @Override
  public JPopupMenu createMenu(long offset)
  {
    // generating popup menu
    JPopupMenu popup = super.createMenu(offset);

    // adding dynamic entries on top of popup menu
    List<JMenuItem> list = createStructEntries((int)offset);
    if (!list.isEmpty()) {
      popup.add(new JPopupMenu.Separator(), 0);
      popup.addSeparator();
    }

    for (int i = list.size() - 1; i >= 0; i--) {
      final JMenuItem mi = list.get(i);
      mi.addActionListener(this);
      popup.add(mi, 0);
    }

    return popup;
  }

//--------------------- End Interface IMenuCreator ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() instanceof DataMenuItem) {
      DataMenuItem mi = (DataMenuItem)e.getSource();
      StructEntry entry = (StructEntry)mi.getData();
      if (entry != null) {
        List<StructEntry> listEntries = entry.getStructChain();
        StructViewer curViewer = null;
        for (int i = 0; i < listEntries.size(); i++) {
          entry = listEntries.get(i);
          if (entry instanceof AbstractStruct) {
            if (entry == getStruct()) {
              curViewer = ((AbstractStruct)entry).getViewer();
              if (curViewer != null) {
                curViewer.selectEditTab();
                curViewer.selectEntry(entry.getOffset(), false);
              }
            } else {
              if (curViewer != null) {
                curViewer.selectEntry(entry.getOffset(), false);
              }
              ViewFrame curFrame = curViewer.getViewFrame((AbstractStruct)entry);
              if (curFrame.getViewable() instanceof AbstractStruct) {
                curViewer = ((AbstractStruct)curFrame.getViewable()).getViewer();
              } else {
                curViewer = null;
              }
            }
          } else if (curViewer != null) {
            curViewer.selectEntry(entry.getOffset(), false);
            curViewer = null;
          }
        }
      }
    } else {
      super.actionPerformed(e);
    }
  }

//--------------------- End Interface ActionListener ---------------------

  /** Returns the associated resource structure instance. */
  public AbstractStruct getStruct()
  {
    return struct;
  }

  // Creates a list of all structures containing the specified offset, starting from topmost level.
  private List<JMenuItem> createStructEntries(int offset)
  {
    List<JMenuItem> list = new ArrayList<>();
    if (getHexView().getData() instanceof StructuredDataProvider) {
      StructEntry curEntry = ((StructuredDataProvider)getHexView().getData()).getFieldAt(offset);
      if (curEntry != null) {
        List<StructEntry> listEntries = curEntry.getStructChain();
        // we don't need actual resource structure
        if (!listEntries.isEmpty() && listEntries.get(0) == getStruct()) {
          listEntries.remove(0);
        }
        for (int i = 0; i < listEntries.size(); i++) {
          StructEntry e = listEntries.get(i);
          JMenuItem mi = new DataMenuItem(String.format("Go to \"%s\"", e.getName()), null, e);
          list.add(mi);
        }
      }
    }
    return list;
  }
}
