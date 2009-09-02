// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.NearInfinity;
import infinity.datatype.*;
import infinity.gui.*;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.graphics.TisResource;
import infinity.resource.wed.Overlay;
import infinity.resource.wed.WedResource;

import javax.swing.*;
import java.awt.*;
import java.awt.Container;
import java.awt.image.*;

public final class ViewerGraphics extends ChildFrame implements Runnable
{
  private final AreResource areaFile;
  private JLabel label;

  public ViewerGraphics(AreResource areaFile)
  {
    super("Area Viewer: " + areaFile.getName(), true);
    this.areaFile = areaFile;
    new Thread(this).start();
  }

// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    try {
      ResourceRef wedRef = (ResourceRef)areaFile.getAttribute("WED resource");
      WedResource wedFile = new WedResource(
              ResourceFactory.getInstance().getResourceEntry(wedRef.getResourceName()));
      Overlay overlay = (Overlay)wedFile.getAttribute("Overlay");
      ResourceRef tisRef = (ResourceRef)overlay.getAttribute("Tileset");
      int width = ((DecNumber)overlay.getAttribute("Width")).getValue();
      int height = ((DecNumber)overlay.getAttribute("Height")).getValue();
      int mapOffset = ((HexNumber)overlay.getAttribute("Tilemap offset")).getValue();
      int lookupOffset = ((HexNumber)overlay.getAttribute("Tilemap lookup offset")).getValue();
      int mapIndex = 0, lookupIndex = 0;
      for (int i = 0; i < overlay.getRowCount(); i++) {
        StructEntry entry = overlay.getStructEntryAt(i);
        if (entry.getOffset() == mapOffset)
          mapIndex = i;
        else if (entry.getOffset() == lookupOffset)
          lookupIndex = i;
      }

      blocker.setBlocked(true);
//      TisResource tisFile = new TisResource(ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName()));
//      BufferedImage image = tisFile.drawImage(width, height, mapIndex, lookupIndex, overlay);
//      tisFile.close();
      BufferedImage image = TisResource.drawImage(
              ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName()),
              width, height, mapIndex, lookupIndex, overlay);

      Container pane = getContentPane();
      pane.setLayout(new BorderLayout());
      label = new JLabel(new ImageIcon(image));
      pane.add(new JScrollPane(label), BorderLayout.CENTER);
      setSize(NearInfinity.getInstance().getSize());
      Center.center(this, NearInfinity.getInstance().getBounds());
      blocker.setBlocked(false);
      setVisible(true);
    } catch (Exception e) {
      e.printStackTrace();
      blocker.setBlocked(false);
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), e.toString(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
    } catch (OutOfMemoryError e) {
      e.printStackTrace();
      blocker.setBlocked(false);
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), e.toString(), "Error",
                                    JOptionPane.ERROR_MESSAGE);
    }
  }

// --------------------- End Interface Runnable ---------------------

  protected void windowClosing() throws Exception
  {
    if (label != null && label.getIcon() != null)
      ((ImageIcon)label.getIcon()).getImage().flush();
    dispose();
  }
}

