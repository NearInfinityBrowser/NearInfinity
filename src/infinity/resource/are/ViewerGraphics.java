// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.are;

import infinity.NearInfinity;
import infinity.datatype.*;
import infinity.gui.*;
import infinity.resource.ResourceFactory;
import infinity.resource.StructEntry;
import infinity.resource.graphics.TisDecoder;
import infinity.resource.graphics.TisResource2;
import infinity.resource.key.ResourceEntry;
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

  public static boolean IsValid(AreResource are)
  {
    if (are != null) {
      ResourceRef wedRef = (ResourceRef)are.getAttribute("WED resource");
      ResourceEntry wedEntry = ResourceFactory.getInstance().getResourceEntry(wedRef.getResourceName());
      if (wedEntry != null) {
        try {
          WedResource wedFile = new WedResource(wedEntry);
          Overlay overlay = (Overlay)wedFile.getAttribute("Overlay");
          ResourceRef tisRef = (ResourceRef)overlay.getAttribute("Tileset");
          ResourceEntry tisEntry = ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
          if (tisEntry != null)
            return true;
        } catch (Exception e) {
          return false;
        }
      }
    }
    return false;
  }

  public ViewerGraphics(AreResource areaFile)
  {
    super("Area Viewer: " + areaFile.getName(), true);

    if ((NearInfinity.getInstance().getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
      setExtendedState(Frame.MAXIMIZED_BOTH);
    else
      setExtendedState(Frame.NORMAL);

    this.areaFile = areaFile;
    new Thread(this).start();
  }

// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
    try {
      ResourceRef wedRef = (ResourceRef)areaFile.getAttribute("WED resource");
      ResourceEntry wedEntry = ResourceFactory.getInstance().getResourceEntry(wedRef.getResourceName());
      if (wedEntry == null)
        throw new NullPointerException("Resource " + wedRef.getResourceName() + " does not exist.");
      WedResource wedFile = new WedResource(wedEntry);
      Overlay overlay = (Overlay)wedFile.getAttribute("Overlay");
      ResourceRef tisRef = (ResourceRef)overlay.getAttribute("Tileset");
      ResourceEntry tisEntry = ResourceFactory.getInstance().getResourceEntry(tisRef.getResourceName());
      if (tisEntry == null)
        throw new NullPointerException("Resource " + tisRef.getResourceName() + " does not exist.");
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
      TisDecoder decoder = new TisDecoder(tisEntry);
      BufferedImage image = new BufferedImage(width*decoder.info().tileWidth(),
                                              height*decoder.info().tileHeight(),
                                              BufferedImage.TYPE_INT_RGB);
      if (!TisResource2.drawImage(image, decoder, width, height, mapIndex, lookupIndex, overlay, false)) {
        image = null;
        decoder = null;
        throw new Exception("Error creating area map");
      }
      decoder = null;

      Container pane = getContentPane();
      pane.setLayout(new BorderLayout());
      label = new JLabel(new ImageIcon(image));
      JScrollPane scroll = new JScrollPane(label);
      scroll.getVerticalScrollBar().setUnitIncrement(16);
      scroll.getHorizontalScrollBar().setUnitIncrement(16);
      pane.add(scroll, BorderLayout.CENTER);
      setSize(NearInfinity.getInstance().getSize());
      Center.center(this, NearInfinity.getInstance().getBounds());
      blocker.setBlocked(false);
      setVisible(true);
    } catch (Throwable e) {
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

