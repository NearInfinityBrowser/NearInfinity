// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.NearInfinity;
import infinity.resource.AbstractStruct;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.StructEntry;
import infinity.resource.TextResource;
import infinity.resource.Viewable;
import infinity.resource.ViewableContainer;
import infinity.resource.graphics.BamResource;
import infinity.resource.graphics.BamResource2;
import infinity.resource.key.Keyfile;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;

public final class ViewFrame extends ChildFrame implements ViewableContainer
{
  private final StatusBar statusBar = new StatusBar();
  private Viewable viewable;

  public ViewFrame(Component parent, Viewable viewable)
  {
    super(viewable.getClass().getName(), true);
    setViewable(viewable);
    if (viewable instanceof AbstractStruct || viewable instanceof TextResource ||
        viewable instanceof BamResource || viewable instanceof BamResource2)
      setSize(NearInfinity.getInstance().getWidth() - 200, NearInfinity.getInstance().getHeight() - 45);
    else
      pack();
    Center.center(this, parent.getBounds());
    setVisible(true);
  }

// --------------------- Begin Interface ViewableContainer ---------------------

  @Override
  public StatusBar getStatusBar()
  {
    return statusBar;
  }

  @Override
  public Viewable getViewable()
  {
    return viewable;
  }

  @Override
  public void setViewable(Viewable viewable)
  {
    this.viewable = viewable;
    if (viewable instanceof Resource && ((Resource)viewable).getResourceEntry() != null) {
      ResourceEntry entry = ((Resource)viewable).getResourceEntry();
      setTitle(entry.toString());
      setIconImage(entry.getIcon().getImage());
      statusBar.setMessage(entry.getActualFile().toString());
    }
    else {
      setIconImage(Keyfile.ICON_STRUCT.getImage());
      setTitle(((StructEntry)viewable).getName());
      if (((AbstractStruct)viewable).getSuperStruct() != null)
        statusBar.setMessage("Parent structure: " + ((AbstractStruct)viewable).getSuperStruct().getName());
    }
    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.removeAll();
    pane.add(viewable.makeViewer(this), BorderLayout.CENTER);
    pane.add(statusBar, BorderLayout.SOUTH);
    pane.invalidate();
    pane.revalidate();
  }

// --------------------- End Interface ViewableContainer ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    if (viewable instanceof Closeable)
      ((Closeable)viewable).close();

    this.viewable = null;
    this.getContentPane().removeAll();
    return super.windowClosing(forced);
  }
}

