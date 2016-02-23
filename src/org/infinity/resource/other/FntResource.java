// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.other;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.infinity.datatype.DecNumber;
import org.infinity.datatype.ResourceRef;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.StructViewer;
import org.infinity.gui.hexview.BasicColorMap;
import org.infinity.gui.hexview.ResourceDataProvider;
import org.infinity.gui.hexview.StructHexViewer;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.Closeable;
import org.infinity.resource.HasViewerTabs;
import org.infinity.resource.Resource;
import org.infinity.resource.key.ResourceEntry;

public final class FntResource extends AbstractStruct implements Resource, Closeable, HasViewerTabs
{
  // FNT-specific field labels
  public static final String FNT_NUM_EXTRA_LETTERS = "# extra letters";
  public static final String FNT_LETTERS = "Letters";
  public static final String FNT_EXTRA_LETTERS = "Extra letters";

  private StructHexViewer hexViewer;

  public FntResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  @Override
  public void close() throws Exception
  {
    // don't save changes
  }

  @Override
  public int read(byte[] buffer, int startoffset) throws Exception
  {
    String resName = getResourceEntry().getResourceName();
    if (resName.lastIndexOf('.') > 0)
      resName = resName.substring(0, resName.lastIndexOf('.'));

    byte[] b = new byte[8];
    System.arraycopy(resName.getBytes(), 0, b, 0, resName.length());
    addField(new DecNumber(buffer, startoffset, 4, FNT_NUM_EXTRA_LETTERS));
    addField(new ResourceRef(b, 0, FNT_LETTERS, "BAM"));
    addField(new ResourceRef(b, 0, FNT_EXTRA_LETTERS, "BMP"));
    return buffer.length;
  }

  //--------------------- Begin Interface HasViewerTabs ---------------------

  @Override
  public int getViewerTabCount()
  {
    return 1;
  }

  @Override
  public String getViewerTabName(int index)
  {
    return StructViewer.TAB_RAW;
  }

  @Override
  public JComponent getViewerTab(int index)
  {
    if (hexViewer == null) {
      hexViewer = new StructHexViewer(this, new BasicColorMap(this, true),
                                new ResourceDataProvider(getResourceEntry()));
    }
    return hexViewer;
  }

  @Override
  public boolean viewerTabAddedBefore(int index)
  {
    return false;
  }

//--------------------- End Interface HasViewerTabs ---------------------

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    viewer.addTabChangeListener(hexViewer);

    // disabling 'Save' button
    JButton bSave = (JButton)viewer.getButtonPanel().getControlByType(ButtonPanel.Control.Save);
    if (bSave != null) {
      bSave.setEnabled(false);
    }
  }
}
