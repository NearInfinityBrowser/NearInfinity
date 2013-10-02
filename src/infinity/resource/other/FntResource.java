// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import java.awt.Component;

import javax.swing.AbstractButton;
import javax.swing.JPanel;

import infinity.datatype.DecNumber;
import infinity.datatype.ResourceRef;
import infinity.gui.StructViewer;
import infinity.resource.AbstractStruct;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.key.ResourceEntry;

public final class FntResource extends AbstractStruct implements Resource, Closeable
{

  public FntResource(ResourceEntry entry) throws Exception
  {
    super(entry);
  }

  public void close() throws Exception
  {
    // don't save changes
  }

  protected int read(byte[] buffer, int startoffset) throws Exception
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

  protected void viewerInitialized(StructViewer viewer)
  {
    // removing 'Save' button
    JPanel panel = viewer.getButtonPanel();
    Component[] components = panel.getComponents();
    if (components != null) {
      viewer.getButtonPanel().removeAll();
      for (final Component c: components) {
        if (c instanceof AbstractButton &&
            ((AbstractButton)c).getActionCommand().equals(StructViewer.CMD_SAVE)) {
          continue;
        }
        panel.add(c);
      }
    }
  }

}
