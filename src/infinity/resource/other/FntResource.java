// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.gui.ButtonPanel;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

import javax.swing.JButton;

public final class FntResource extends AbstractStruct implements Resource, Closeable
{

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
    list.add(new DecNumber(buffer, startoffset, 4, "# extra letters"));
    list.add(new ResourceRef(b, 0, "Letters", "BAM"));
    list.add(new ResourceRef(b, 0, "Extra letters", "BMP"));
    return buffer.length;
  }

  @Override
  protected void viewerInitialized(StructViewer viewer)
  {
    // disabling 'Save' button
    JButton bSave = (JButton)viewer.getButtonPanel().getControlByType(ButtonPanel.Control.Save);
    if (bSave != null) {
      bSave.setEnabled(false);
    }
  }

}
