// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Closeable;
import org.infinity.resource.Resource;
import org.infinity.resource.StructEntry;
import org.infinity.resource.TextResource;
import org.infinity.resource.Viewable;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.graphics.BamResource;
import org.infinity.resource.key.Keyfile;
import org.infinity.resource.key.ResourceEntry;

public final class ViewFrame extends ChildFrame implements ViewableContainer {
  private final StatusBar statusBar = new StatusBar();

  private Viewable viewable;

  /**
   * Returns a formatted string consisting of the name and optional description of the {@code Viewable} if available.
   *
   * @param viewable The {@code Viewable} object.
   * @return A string describing the {@code Viewable}.
   */
  private static String getViewableTitle(Viewable viewable) {
    if (viewable == null) {
      return "";
    }
    if (viewable instanceof Resource) {
      final Resource res = (Resource) viewable;
      final ResourceEntry entry = res.getResourceEntry();
      if (entry != null) {
        final String sn = entry.getSearchString();
        final String rn = entry.getResourceName();
        if (sn != null && !sn.isEmpty()) {
          return rn + " (" + sn + ")";
        } else {
          return rn;
        }
      }
    }
    return viewable.getClass().getName();
  }

  public ViewFrame(Component parent, Viewable viewable) {
    super(getViewableTitle(viewable), true);
    setViewable(viewable);
    if (viewable instanceof AbstractStruct || viewable instanceof TextResource || viewable instanceof BamResource) {
      setSize(getLastFrameSize());
    } else {
      pack();
    }
    setLocation(getLastFrameLocation(parent));
    setVisible(true);
  }

  // --------------------- Begin Interface ViewableContainer ---------------------

  @Override
  public StatusBar getStatusBar() {
    return statusBar;
  }

  @Override
  public Viewable getViewable() {
    return viewable;
  }

  @Override
  public void setViewable(Viewable viewable) {
    this.viewable = viewable;
    if (viewable instanceof Resource && ((Resource) viewable).getResourceEntry() != null) {
      ResourceEntry entry = ((Resource) viewable).getResourceEntry();
      // setTitle(entry.toString());
      setIconImage(entry.getIcon().getImage());
      statusBar.setMessage(entry.getActualPath().toString());
    } else {
      setIconImage(Keyfile.ICON_STRUCT.getImage());
      setTitle(((StructEntry) viewable).getName());
      if (((StructEntry) viewable).getParent() != null) {
        statusBar.setMessage("Parent structure: " + ((StructEntry) viewable).getParent().getName());
      }
    }
    JPanel pane = (JPanel) getContentPane();
    pane.setLayout(new BorderLayout());
    pane.removeAll();
    pane.add(viewable.makeViewer(this), BorderLayout.CENTER);
    pane.add(statusBar, BorderLayout.SOUTH);
    pane.invalidate();
    pane.revalidate();
  }

  // --------------------- End Interface ViewableContainer ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception {
    if (viewable instanceof Closeable) {
      ((Closeable) viewable).close();
    }

    this.viewable = null;
    this.getContentPane().removeAll();
    return super.windowClosing(forced);
  }
}
