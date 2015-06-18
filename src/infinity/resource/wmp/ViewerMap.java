// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.wmp;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

import infinity.NearInfinity;
import infinity.datatype.DecNumber;
import infinity.datatype.Flag;
import infinity.datatype.ResourceRef;
import infinity.datatype.SectionCount;
import infinity.datatype.SectionOffset;
import infinity.datatype.StringRef;
import infinity.gui.BrowserMenuBar;
import infinity.gui.RenderCanvas;
import infinity.gui.ViewerUtil;
import infinity.gui.ViewerUtil.StructListPanel;
import infinity.gui.WindowBlocker;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.graphics.BamDecoder;
import infinity.resource.graphics.BamDecoder.BamControl;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.MosDecoder;
import infinity.resource.key.ResourceEntry;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class ViewerMap extends JPanel
{
  // Needed to determine map edges to travel from/to
  private enum Direction { North, West, South, East }

  private final JPopupMenu pmOptions = new JPopupMenu("Options");
  private final JCheckBoxMenuItem miShowIcons = new JCheckBoxMenuItem("Show all map icons", true);
  private final JCheckBoxMenuItem miShowDistances = new JCheckBoxMenuItem("Show travel distances", false);
  private final BufferedImage iconDot;
  private final Listeners listeners = new Listeners();
  private final MapEntry mapEntry;

  private RenderCanvas rcMap;
  private BufferedImage mapOrig;
  private BamDecoder mapIcons;
  private BamControl mapIconsCtrl;
  private StructListPanel listPanel;
  private BufferedImage dotBackup;
  private int dotX, dotY;

  ViewerMap(MapEntry wmpMap)
  {
    super();
    WindowBlocker.blockWindow(true);
    try {
      mapEntry = wmpMap;

      // creating marker for selected map icon
      iconDot = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = iconDot.createGraphics();
      g.setColor(Color.RED);
      g.fillRect(0, 0, iconDot.getWidth(), iconDot.getHeight());
      g.setColor(Color.YELLOW);
      g.drawRect(0, 0, iconDot.getWidth() - 1, iconDot.getHeight() - 1);
      g.dispose();
      g = null;

      dotBackup = new BufferedImage(iconDot.getWidth(), iconDot.getHeight(), iconDot.getType());
      dotX = dotY = -1;

      miShowIcons.setMnemonic('i');
      miShowIcons.addActionListener(listeners);
      miShowDistances.setMnemonic('d');
      miShowDistances.addActionListener(listeners);
      pmOptions.add(miShowIcons);
      pmOptions.add(miShowDistances);

      try {
        mapIcons = null;
        ResourceRef iconRef = (ResourceRef)wmpMap.getAttribute("Map icons");
        if (iconRef != null) {
          ResourceEntry iconEntry = ResourceFactory.getResourceEntry(iconRef.getResourceName());
          if (iconEntry != null) {
            mapIcons = BamDecoder.loadBam(iconEntry);
            mapIconsCtrl = mapIcons.createControl();
          }
        }

        if (ResourceFactory.resourceExists(((ResourceRef)wmpMap.getAttribute("Map")).getResourceName())) {
          mapOrig = loadMap();
          rcMap = new RenderCanvas(ColorConvert.cloneImage(mapOrig));
          rcMap.addMouseListener(listeners);

          listPanel = (StructListPanel)ViewerUtil.makeListPanel("Areas", wmpMap, AreaEntry.class, "Current area",
                                                                new WmpAreaListRenderer(mapIcons), listeners);
          JScrollPane mapScroll = new JScrollPane(rcMap);
          mapScroll.getVerticalScrollBar().setUnitIncrement(16);
          mapScroll.getHorizontalScrollBar().setUnitIncrement(16);
          mapScroll.setBorder(BorderFactory.createEmptyBorder());

          JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapScroll, listPanel);
          split.setDividerLocation(NearInfinity.getInstance().getWidth() - 475);
          setLayout(new BorderLayout());
          add(split, BorderLayout.CENTER);

        } else {
          rcMap = null;
          mapOrig = null;
        }
      } catch (Throwable t) {
        t.printStackTrace();
      }

      // applying preselected overlays
      showOverlays(miShowIcons.isSelected(), miShowDistances.isSelected());

    } finally {
      WindowBlocker.blockWindow(false);
    }
  }

  // Returns current map entry structure
  private MapEntry getEntry()
  {
    return mapEntry;
  }

  // Load and return map graphics
  private BufferedImage loadMap()
  {
    String mapName = ((ResourceRef)getEntry().getAttribute("Map")).getResourceName();
    if (ResourceFactory.resourceExists(mapName)) {
      MosDecoder mos = MosDecoder.loadMos(ResourceFactory.getResourceEntry(mapName));
      if (mos != null) {
        return (BufferedImage)mos.getImage();
      }
    }
    return null;
  }

  // show popup menu
  private void showPopup(Component invoker, int x, int y)
  {
    pmOptions.show(invoker, x, y);
  }

  // display either or both map icons and travel distances
  private void showOverlays(boolean showIcons, boolean showDistances)
  {
    resetMap();
    if (showIcons) {
      showMapIcons();
      if (showDistances) {
        showMapDistances(listPanel.getList().getSelectedIndex());
      }
    }
    showDot((AreaEntry)listPanel.getList().getSelectedValue(), false);
    rcMap.repaint();
  }

  // Draws map icons onto the map
  private void showMapIcons()
  {
    if (mapIcons != null) {
      Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
      try {
        g.setFont(g.getFont().deriveFont(g.getFont().getSize2D()*0.9f));
        for (int i = 0, count = listPanel.getList().getModel().getSize(); i < count; i++) {
          AreaEntry area = getAreaEntry(i);
          if (area != null) {
            int iconIndex = ((DecNumber)area.getAttribute("Icon number")).getValue();
            int frameIndex = mapIconsCtrl.cycleGetFrameIndexAbsolute(iconIndex, 0);
            if (frameIndex >= 0) {
              BufferedImage mapIcon = (BufferedImage)mapIcons.frameGet(mapIconsCtrl, frameIndex);
              String mapCode = ((ResourceRef)area.getAttribute("Current area")).getResourceName();
              if (ResourceFactory.resourceExists(mapCode)) {
                mapCode = mapCode.replace(".ARE", "");
              } else {
                mapCode = "";
              }
              int x = ((DecNumber)area.getAttribute("Coordinate: X")).getValue();
              int y = ((DecNumber)area.getAttribute("Coordinate: Y")).getValue();
              int width = mapIcons.getFrameInfo(frameIndex).getWidth();
              int height = mapIcons.getFrameInfo(frameIndex).getHeight();
              int cx = mapIcons.getFrameInfo(frameIndex).getCenterX();
              int cy = mapIcons.getFrameInfo(frameIndex).getCenterY();
              x -= cx;
              y -= cy;
              g.drawImage(mapIcon, x, y, x+width, y+height, 0, 0, width, height, null);

              // printing label
              if (!mapCode.isEmpty()) {
                LineMetrics lm = g.getFont().getLineMetrics(mapCode, g.getFontRenderContext());
                Rectangle2D rectText = g.getFont().getStringBounds(mapCode, g.getFontRenderContext());
                int textX = x + (width - rectText.getBounds().width) / 2;
                int textY = y + height;
                int textWidth = rectText.getBounds().width;
                int textHeight = rectText.getBounds().height;
                g.setColor(Color.WHITE);
                g.fillRect(textX - 2, textY, textWidth + 4, textHeight);
                g.setColor(Color.BLACK);
                g.drawString(mapCode, (float)textX, (float)textY + lm.getAscent() + lm.getLeading());
              }
            }
          }
        }
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  // Displays all map distances from the specified area (by index)
  private void showMapDistances(int areaIndex)
  {
    AreaEntry area = getAreaEntry(areaIndex);
    if (area != null) {
      final Direction[] srcDir = { Direction.North, Direction.West, Direction.South, Direction.East };
      final Color[] dirColor = { Color.GREEN, Color.RED, Color.CYAN, Color.YELLOW };
      final int[] links = new int[8];
      final int linkSize = 216;   // size of a single area link structure
      int ofsLinkBase = ((SectionOffset)getEntry().getAttribute("Area links offset")).getValue();

      links[0] = ((DecNumber)area.getAttribute("First link (north)")).getValue();
      links[1] = ((SectionCount)area.getAttribute("# links (north)")).getValue();
      links[2] = ((DecNumber)area.getAttribute("First link (west)")).getValue();
      links[3] = ((SectionCount)area.getAttribute("# links (west)")).getValue();
      links[4] = ((DecNumber)area.getAttribute("First link (south)")).getValue();
      links[5] = ((SectionCount)area.getAttribute("# links (south)")).getValue();
      links[6] = ((DecNumber)area.getAttribute("First link (east)")).getValue();
      links[7] = ((SectionCount)area.getAttribute("# links (east)")).getValue();
      for (int dir = 0; dir < srcDir.length; dir++) {
        Direction curDir = srcDir[dir];
        Point ptOrigin = getMapIconCoordinate(areaIndex, curDir);
        for (int dirIndex = 0, dirCount = links[dir * 2 + 1]; dirIndex < dirCount; dirIndex++) {
          int ofsLink = ofsLinkBase + (links[dir * 2] + dirIndex)*linkSize;
          AreaLink destLink = (AreaLink)area.getAttribute(ofsLink, false);

          if (destLink != null) {
            int dstAreaIndex = ((DecNumber)destLink.getAttribute("Target area")).getValue();
            Flag flag = (Flag)destLink.getAttribute("Default entrance");
            Direction dstDir = Direction.North;
            if (flag.isFlagSet(1)) {
              dstDir = Direction.East;
            } else if (flag.isFlagSet(2)) {
              dstDir = Direction.South;
            } else if (flag.isFlagSet(3)) {
              dstDir = Direction.West;
            }
            Point ptTarget = getMapIconCoordinate(dstAreaIndex, dstDir);

            // checking for random encounters during travels
            boolean hasRandomEncounters = false;
            if (((DecNumber)destLink.getAttribute("Random encounter probability")).getValue() > 0) {
              for (int rnd = 1; rnd < 6; rnd++) {
                String rndArea = ((ResourceRef)destLink.getAttribute("Random encounter area " + rnd)).getResourceName();
                if (ResourceFactory.resourceExists(rndArea)) {
                  hasRandomEncounters = true;
                  break;
                }
              }
            }

            Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
            g.setFont(g.getFont().deriveFont(g.getFont().getSize2D()*0.8f));
            try {
              // drawing line
              g.setColor(dirColor[dir]);
              if (hasRandomEncounters) {
                g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f,
                            new float[]{6.0f, 4.0f}, 0.0f));
              } else {
                g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
              }
              g.drawLine(ptOrigin.x, ptOrigin.y, ptTarget.x, ptTarget.y);

              // printing travel time (in hours)
              String duration = String.format("%1$d h", ((DecNumber)destLink.getAttribute("Distance scale")).getValue()*4);
              LineMetrics lm = g.getFont().getLineMetrics(duration, g.getFontRenderContext());
              Rectangle2D rectText = g.getFont().getStringBounds(duration, g.getFontRenderContext());
              int textX = ptOrigin.x + ((ptTarget.x - ptOrigin.x) - rectText.getBounds().width) / 2;
              int textY = ptOrigin.y + ((ptTarget.y - ptOrigin.y) - rectText.getBounds().height) / 2;
              int textWidth = rectText.getBounds().width;
              int textHeight = rectText.getBounds().height;
              g.setColor(Color.LIGHT_GRAY);
              g.fillRect(textX - 2, textY, textWidth + 4, textHeight);
              g.setColor(Color.BLUE);
              g.drawString(duration, (float)textX, (float)textY + lm.getAscent() + lm.getLeading());
            } finally {
              g.dispose();
              g = null;
            }
          }
        }
      }
    }
  }

  // Returns a pixel coordinate for one of the edges of the specified area icon
  private Point getMapIconCoordinate(int areaIndex, Direction dir)
  {
    AreaEntry area = getAreaEntry(areaIndex);
    if (area != null) {
      int x = ((DecNumber)area.getAttribute("Coordinate: X")).getValue();
      int y = ((DecNumber)area.getAttribute("Coordinate: Y")).getValue();
      int iconIndex = ((DecNumber)area.getAttribute("Icon number")).getValue();
      int frameIndex = mapIconsCtrl.cycleGetFrameIndexAbsolute(iconIndex, 0);
      int width, height;
      if (frameIndex >= 0) {
        width = mapIcons.getFrameInfo(frameIndex).getWidth();
        height = mapIcons.getFrameInfo(frameIndex).getHeight();
        x -= mapIcons.getFrameInfo(frameIndex).getCenterX();
        y -= mapIcons.getFrameInfo(frameIndex).getCenterY();
      } else {
        width = height = 0;
      }
      Point retVal = new Point();
      switch (dir) {
        case North:
          retVal.x = x + (width / 2);
          retVal.y = y;
          break;
        case West:
          retVal.x = x;
          retVal.y = y + (height / 2);
          break;
        case South:
          retVal.x = x + (width / 2);
          retVal.y = y + height - 1;
          break;
        case East:
          retVal.x = x + width - 1;
          retVal.y = y + (height / 2);
          break;
      }
      return retVal;
    }
    return null;
  }

  // Returns area structure of specified item index
  private AreaEntry getAreaEntry(int index)
  {
    if (index >= 0 && index < listPanel.getList().getModel().getSize()) {
      return (AreaEntry)listPanel.getList().getModel().getElementAt(index);
    } else {
      return null;
    }
  }

  // Show "dot" on specified map icon, optionally restore background graphics
  private void showDot(AreaEntry entry, boolean restore)
  {
    if (restore) {
      restoreDot();
    }
    if (entry != null) {
      storeDot(entry);
      int x = ((DecNumber)entry.getAttribute("Coordinate: X")).getValue();
      int y = ((DecNumber)entry.getAttribute("Coordinate: Y")).getValue();
      int width = iconDot.getWidth();
      int height = iconDot.getHeight();
      int xofs = width / 2;
      int yofs = height / 2;
      Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
      try {
        g.drawImage(iconDot, x-xofs, y-yofs, x-xofs+width, y-yofs+height, 0, 0, width, height, null);
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  // Stores background graphics of "dot"
  private void storeDot(AreaEntry entry)
  {
    if (entry != null) {
      int x = ((DecNumber)entry.getAttribute("Coordinate: X")).getValue();
      int y = ((DecNumber)entry.getAttribute("Coordinate: Y")).getValue();
      int width = dotBackup.getWidth();
      int height = dotBackup.getHeight();
      int xofs = width / 2;
      int yofs = height / 2;
      Graphics2D g = dotBackup.createGraphics();
      try {
        g.drawImage(rcMap.getImage(), 0, 0, width, height, x-xofs, y-yofs, x-xofs+width, y-yofs+height, null);
        dotX = x-xofs;
        dotY = y-yofs;
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  // Restores background graphics of "dot"
  private void restoreDot()
  {
    if (dotX != -1 && dotY != -1) {
      int x = dotX;
      int y = dotY;
      int width = dotBackup.getWidth();
      int height = dotBackup.getHeight();
      Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
      try {
        g.drawImage(dotBackup, x, y, x+width, y+height, 0, 0, width, height, null);
        dotX = -1;
        dotY = -1;
      } finally {
        g.dispose();
        g = null;
      }
    }
  }

  // Attempts to restore the whole map graphics
  private void resetMap()
  {
    Graphics2D g = ((BufferedImage)rcMap.getImage()).createGraphics();
    try {
      g.drawImage(mapOrig, 0, 0, null);
    } finally {
      g.dispose();
      g = null;
    }
  }


//-------------------------- INNER CLASSES --------------------------

  private class Listeners implements ActionListener, MouseListener, ListSelectionListener
  {
    //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent e)
    {
      if (e.getSource() == miShowIcons) {
        try {
          WindowBlocker.blockWindow(true);
          if (!miShowIcons.isSelected() && miShowDistances.isSelected()) {
            miShowDistances.setSelected(false);
          }
          showOverlays(miShowIcons.isSelected(), miShowDistances.isSelected());
        } finally {
          WindowBlocker.blockWindow(false);
        }
      } else if (e.getSource() == miShowDistances) {
        try {
          WindowBlocker.blockWindow(true);
          if (miShowDistances.isSelected() && !miShowIcons.isSelected()) {
            miShowIcons.setSelected(true);
          }
          showOverlays(miShowIcons.isSelected(), miShowDistances.isSelected());
        } finally {
          WindowBlocker.blockWindow(false);
        }
      }
    }

    //--------------------- End Interface ActionListener ---------------------

    //--------------------- Begin Interface MouseListener ---------------------

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e)
    {
      if (e.isPopupTrigger() && e.getComponent() == rcMap) {
        showPopup(e.getComponent(), e.getX(), e.getY());
      }
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
      if (e.isPopupTrigger() && e.getComponent() == rcMap) {
        showPopup(e.getComponent(), e.getX(), e.getY());
      }
    }

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    //--------------------- End Interface MouseListener ---------------------

    // --------------------- Begin Interface ListSelectionListener ---------------------

    @Override
    public void valueChanged(ListSelectionEvent event)
    {
      if (!event.getValueIsAdjusting()) {
        JList list = (JList)event.getSource();
        if (miShowDistances.isSelected()) {
          showOverlays(miShowIcons.isSelected(), miShowDistances.isSelected());
        } else {
          showDot((AreaEntry)list.getSelectedValue(), true);
        }
        repaint();
      }
    }

    // --------------------- End Interface ListSelectionListener ---------------------
  }


  private static final class WmpAreaListRenderer extends DefaultListCellRenderer
  {
    private final BamDecoder bam;
    private final BamControl ctrl;

    private WmpAreaListRenderer(BamDecoder decoder)
    {
      bam = decoder;
      ctrl = (bam != null) ? bam.createControl() : null;
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                  boolean cellHasFocus)
    {
      JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      AbstractStruct struct = (AbstractStruct)value;

      StringRef areaName = (StringRef)struct.getAttribute("Name");
      ResourceRef areaRef = (ResourceRef)struct.getAttribute("Current area");
      String text1 = null, text2 = null;
      if (areaName.getValue() >= 0) {
        text1 = areaName.toString(BrowserMenuBar.getInstance().showStrrefs());
      } else {
        text1 = "";
      }
      text2 = areaRef.getResourceName();
      if (!text2.equalsIgnoreCase("NONE")) {
        text2 = text2.toUpperCase(Locale.ENGLISH).replace(".ARE", "");
      }
      label.setText(String.format("[%1$s] %2$s", text2, text1));

      DecNumber animNr = (DecNumber)struct.getAttribute("Icon number");
      setIcon(null);
      if (ctrl != null) {
        setIcon(new ImageIcon(bam.frameGet(ctrl, ctrl.cycleGetFrameIndexAbsolute(animNr.getValue(), 0))));
      }
      return label;
    }
  }
}
