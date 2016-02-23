// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.wmp;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Locale;

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

import org.infinity.NearInfinity;
import org.infinity.datatype.DecNumber;
import org.infinity.datatype.Flag;
import org.infinity.datatype.ResourceRef;
import org.infinity.datatype.SectionCount;
import org.infinity.datatype.SectionOffset;
import org.infinity.datatype.StringRef;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.RenderCanvas;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.gui.ViewerUtil.StructListPanel;
import org.infinity.resource.AbstractStruct;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.MosDecoder;
import org.infinity.resource.graphics.BamDecoder.BamControl;
import org.infinity.resource.key.ResourceEntry;

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
  private JLabel lInfoSize, lInfoPos;

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
        ResourceRef iconRef = (ResourceRef)wmpMap.getAttribute(MapEntry.WMP_MAP_ICONS);
        if (iconRef != null) {
          ResourceEntry iconEntry = ResourceFactory.getResourceEntry(iconRef.getResourceName());
          if (iconEntry != null) {
            mapIcons = BamDecoder.loadBam(iconEntry);
            mapIconsCtrl = mapIcons.createControl();
          }
        }

        if (ResourceFactory.resourceExists(((ResourceRef)wmpMap.getAttribute(MapEntry.WMP_MAP_RESREF)).getResourceName())) {
          mapOrig = loadMap();
          rcMap = new RenderCanvas(ColorConvert.cloneImage(mapOrig));
          rcMap.addMouseListener(listeners);
          rcMap.addMouseMotionListener(listeners);

          listPanel = (StructListPanel)ViewerUtil.makeListPanel("Areas", wmpMap, AreaEntry.class, AreaEntry.WMP_AREA_CURRENT,
                                                                new WmpAreaListRenderer(mapIcons), listeners);
          JScrollPane mapScroll = new JScrollPane(rcMap);
          mapScroll.getVerticalScrollBar().setUnitIncrement(16);
          mapScroll.getHorizontalScrollBar().setUnitIncrement(16);
          mapScroll.setBorder(BorderFactory.createEmptyBorder());

          JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mapScroll, listPanel);
          split.setDividerLocation(NearInfinity.getInstance().getWidth() - 475);
          setLayout(new BorderLayout());
          add(split, BorderLayout.CENTER);

          JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
          lInfoSize = new JLabel(String.format("Worldmap size: %1$d x %2$d pixels", mapOrig.getWidth(), mapOrig.getHeight()));
          lInfoSize.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
          lInfoPos = new JLabel();
          lInfoPos.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
          pInfo.add(lInfoSize);
          pInfo.add(lInfoPos);
          add(pInfo, BorderLayout.SOUTH);

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
    String mapName = ((ResourceRef)getEntry().getAttribute(MapEntry.WMP_MAP_RESREF)).getResourceName();
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
            int iconIndex = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_ICON_INDEX)).getValue();
            int frameIndex = mapIconsCtrl.cycleGetFrameIndexAbsolute(iconIndex, 0);
            if (frameIndex >= 0) {
              BufferedImage mapIcon = (BufferedImage)mapIcons.frameGet(mapIconsCtrl, frameIndex);
              String mapCode = ((ResourceRef)area.getAttribute(AreaEntry.WMP_AREA_CURRENT)).getResourceName();
              if (ResourceFactory.resourceExists(mapCode)) {
                mapCode = mapCode.replace(".ARE", "");
              } else {
                mapCode = "";
              }
              int x = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_COORDINATE_X)).getValue();
              int y = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_COORDINATE_Y)).getValue();
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
      int ofsLinkBase = ((SectionOffset)getEntry().getAttribute(MapEntry.WMP_MAP_OFFSET_AREA_LINKS)).getValue();

      links[0] = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_FIRST_LINK_NORTH)).getValue();
      links[1] = ((SectionCount)area.getAttribute(AreaEntry.WMP_AREA_NUM_LINKS_NORTH)).getValue();
      links[2] = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_FIRST_LINK_WEST)).getValue();
      links[3] = ((SectionCount)area.getAttribute(AreaEntry.WMP_AREA_NUM_LINKS_WEST)).getValue();
      links[4] = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_FIRST_LINK_SOUTH)).getValue();
      links[5] = ((SectionCount)area.getAttribute(AreaEntry.WMP_AREA_NUM_LINKS_SOUTH)).getValue();
      links[6] = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_FIRST_LINK_EAST)).getValue();
      links[7] = ((SectionCount)area.getAttribute(AreaEntry.WMP_AREA_NUM_LINKS_EAST)).getValue();
      for (int dir = 0; dir < srcDir.length; dir++) {
        Direction curDir = srcDir[dir];
        Point ptOrigin = getMapIconCoordinate(areaIndex, curDir);
        for (int dirIndex = 0, dirCount = links[dir * 2 + 1]; dirIndex < dirCount; dirIndex++) {
          int ofsLink = ofsLinkBase + (links[dir * 2] + dirIndex)*linkSize;
          AreaLink destLink = (AreaLink)area.getAttribute(ofsLink, false);

          if (destLink != null) {
            int dstAreaIndex = ((DecNumber)destLink.getAttribute(AreaLink.WMP_LINK_TARGET_AREA)).getValue();
            Flag flag = (Flag)destLink.getAttribute(AreaLink.WMP_LINK_DEFAULT_ENTRANCE);
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
            if (((DecNumber)destLink.getAttribute(AreaLink.WMP_LINK_RANDOM_ENCOUNTER_PROBABILITY)).getValue() > 0) {
              for (int rnd = 1; rnd < 6; rnd++) {
                String rndArea = ((ResourceRef)destLink
                    .getAttribute(String.format(AreaLink.WMP_LINK_RANDOM_ENCOUNTER_AREA_FMT, rnd)))
                    .getResourceName();
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
              String duration = String.format("%1$d h", ((DecNumber)destLink.getAttribute(AreaLink.WMP_LINK_DISTANCE_SCALE)).getValue() * 4);
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
      int x = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_COORDINATE_X)).getValue();
      int y = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_COORDINATE_Y)).getValue();
      int iconIndex = ((DecNumber)area.getAttribute(AreaEntry.WMP_AREA_ICON_INDEX)).getValue();
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
      int x = ((DecNumber)entry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_X)).getValue();
      int y = ((DecNumber)entry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_Y)).getValue();
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
      int x = ((DecNumber)entry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_X)).getValue();
      int y = ((DecNumber)entry.getAttribute(AreaEntry.WMP_AREA_COORDINATE_Y)).getValue();
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

  // Shows specified coordinates as text info. Hides display for negative coordinates.
  private void updateCursorInfo(int x, int y)
  {
    if (lInfoPos != null) {
      if (x >= 0 && y >= 0) {
        lInfoPos.setText(String.format("Cursor at (%1$d, %2$d)", x, y));
      } else {
        lInfoPos.setText("");
      }
    }
  }

//-------------------------- INNER CLASSES --------------------------

  private class Listeners implements ActionListener, MouseListener, MouseMotionListener, ListSelectionListener
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

    //--------------------- Begin Interface MouseMotionListener ---------------------

    @Override
    public void mouseDragged(MouseEvent e)
    {
    }

    @Override
    public void mouseMoved(MouseEvent e)
    {
      if (e.getSource() == rcMap) {
        int ctrlWidth = rcMap.getWidth();
        int ctrlHeight = rcMap.getHeight();
        Image image = rcMap.getImage();
        int imgWidth = image.getWidth(null);
        int imgHeight = image.getHeight(null);
        int startX = (ctrlWidth - imgWidth) / 2;
        int startY = (ctrlHeight - imgHeight) / 2;
        int x = e.getX();
        int y = e.getY();
        if (x >= startX && x < startX + imgWidth && y >= startY && y < startY + imgHeight) {
          updateCursorInfo(x - startX, y - startY);
        } else {
          updateCursorInfo(-1, -1);
        }
      }
    }

    //--------------------- End Interface MouseMotionListener ---------------------

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

      StringRef areaName = (StringRef)struct.getAttribute(AreaEntry.WMP_AREA_NAME);
      ResourceRef areaRef = (ResourceRef)struct.getAttribute(AreaEntry.WMP_AREA_CURRENT);
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

      DecNumber animNr = (DecNumber)struct.getAttribute(AreaEntry.WMP_AREA_ICON_INDEX);
      setIcon(null);
      if (ctrl != null) {
        setIcon(new ImageIcon(bam.frameGet(ctrl, ctrl.cycleGetFrameIndexAbsolute(animNr.getValue(), 0))));
      }
      return label;
    }
  }
}
