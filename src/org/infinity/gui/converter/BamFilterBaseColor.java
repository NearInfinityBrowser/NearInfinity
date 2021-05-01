// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ColorGrid;
import org.infinity.gui.ColorGrid.MouseOverEvent;
import org.infinity.gui.ColorGrid.MouseOverListener;
import org.infinity.gui.ViewerUtil;
import org.infinity.util.Misc;

/**
 * The base class for filters that manipulate on color/pixel level.
 */
public abstract class BamFilterBaseColor extends BamFilterBase
{
  protected BamFilterBaseColor(ConvertToBam parent, String name, String desc)
  {
    super(parent, name, desc, Type.COLOR);
  }

  /**
   * Applies the filter to the specified BufferedImage object.
   * The returned BufferedImage object can either ne the modified source image or a new copy.
   * @param frame The BufferedImage object to modify.
   * @return The resulting BufferedImage object.
   */
  public abstract BufferedImage process(BufferedImage frame) throws Exception;


  /** Parses a list of palette indices from a parameter string of the format "[idx1,idx2,...]". */
  protected int[] decodeColorList(String param)
  {
    int[] indices = null;
    if (param != null && param.matches("\\[.*\\]")) {
      String colorString = param.substring(1, param.length() - 1).trim();
      if (!colorString.isEmpty()) {
        String[] colors = colorString.split(",");
        indices = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
          indices[i] = Misc.toNumber(colors[i], -1);
          if (indices[i] < 0 || indices[i] > 255) {
            indices = null;
            break;
          }
        }
      } else {
        indices = new int[0];
      }
    }
    return indices;
  }

  /** Converts a list of palette indices into a parameter string. */
  protected String encodeColorList(int[] indices)
  {
    StringBuilder sb = new StringBuilder();
    sb.append('[');
    if (indices != null) {
      for (int i = 0; i < indices.length; i++) {
        if (i > 0) {
          sb.append(',');
        }
        sb.append(indices[i]);
      }
    }
    sb.append(']');
    return sb.toString();
  }

//-------------------------- INNER CLASSES --------------------------

  /**
   * Lets you select multiple color entries from the palette. It can be used to exclude the
   * selected colors from filtering.
   */
  public static class ExcludeColorsPanel extends JPanel
      implements MouseOverListener, ActionListener
  {
    private static final String FmtInfoRGB    = "%d  %d  %d  %d";
    private static final String FmtInfoHexRGB = "#%02X%02X%02X%02X";
    private static final int[] AllColorIndices = new int[256];
    static {
      for (int i = 0; i < AllColorIndices.length; i++) {
        AllColorIndices[i] = i;
      }
    }

    private final List<ChangeListener> listChangeListeners = new ArrayList<>();

    private ColorGrid cgPalette;
    private JLabel lInfoIndex, lInfoRGB, lInfoHexRGB;
    private JButton bSelectAll, bSelectNone, bSelectInvert;

    public ExcludeColorsPanel(int[] palette)
    {
      super(new GridBagLayout());
      init();
      updatePalette(palette);
    }

    /**
     * Adds a ChangeListener to the listener list.
     * ChangeListeners will be notified whenever the color selection changes.
     */
    public void addChangeListener(ChangeListener l)
    {
      if (l != null) {
        if (listChangeListeners.indexOf(l) < 0) {
          listChangeListeners.add(l);
        }
      }
    }

    /** Returns an array of all ChangeListeners added to this object. */
    public ChangeListener[] getChangeListeners()
    {
      ChangeListener[] retVal = new ChangeListener[listChangeListeners.size()];
      for (int i = 0; i < listChangeListeners.size(); i++) {
        retVal[i] = listChangeListeners.get(i);
      }
      return retVal;
    }

    /** Removes a ChangeListener from the listener list. */
    public void removeChangeListener(ChangeListener l)
    {
      if (l != null) {
        int idx = listChangeListeners.indexOf(l);
        if (idx >= 0) {
          listChangeListeners.remove(idx);
        }
      }
    }


    /** Applies the specified palette to the color grid component. */
    public void updatePalette(int[] palette)
    {
      for (int i = 0; i < cgPalette.getColorCount(); i++) {
        if (palette != null && i < palette.length) {
          cgPalette.setColor(i, new Color(palette[i], true));
        } else {
          cgPalette.setColor(i, Color.BLACK);
        }
      }
    }

    /** Returns the selected color indices as an array of integers. */
    public int[] getSelectedIndices()
    {
      return cgPalette.getSelectedIndices();
    }

    /** Returns whether the specified color index has been selected. */
    public boolean isSelectedIndex(int index)
    {
      return cgPalette.isSelectedIndex(index);
    }

    /** Selects the specified color indices. Previous selections will be cleared. */
    public void setSelectedIndices(int[] indices)
    {
      cgPalette.clearSelection();
      if (indices != null) {
        cgPalette.setSelectedIndices(indices);
      }
    }

    //--------------------- Begin Interface MouseOverListener ---------------------

    @Override
    public void mouseOver(MouseOverEvent event)
    {
      if (event.getSource() == cgPalette) {
        updateInfoBox(event.getColorIndex());
      }
    }

    //--------------------- End Interface MouseOverListener ---------------------

    //--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == cgPalette) {
        fireChangeListener();
      } else if (event.getSource() == bSelectAll) {
        cgPalette.setSelectedIndices(AllColorIndices);
        fireChangeListener();
      } else if (event.getSource() == bSelectNone) {
        cgPalette.clearSelection();
        fireChangeListener();
      } else if (event.getSource() == bSelectInvert) {
        int[] selectedIndices = cgPalette.getSelectedIndices();
        int[] indices = new int[cgPalette.getColorCount() - selectedIndices.length];
        for (int i = 0, ofs = 0; i < cgPalette.getColorCount(); i++) {
          int idx = -1;
          for (int j = 0; j < selectedIndices.length; j++) {
            if (i == selectedIndices[j]) {
              idx = j;
              break;
            }
          }
          if (idx < 0) {
            indices[ofs++] = i;
          }
        }
        cgPalette.setSelectedIndices(indices);
        fireChangeListener();
      }
    }

    //--------------------- End Interface ActionListener ---------------------

    private void init()
    {
      GridBagConstraints c = new GridBagConstraints();

      // creating palette section
      cgPalette = new ColorGrid(256);
      cgPalette.setColorEntryHorizontalGap(4);
      cgPalette.setColorEntryVerticalGap(4);
      cgPalette.setSelectionMode(ColorGrid.SELECTION_MULTIPLE);
      cgPalette.setSelectionFrame(ColorGrid.Frame.SINGLE_LINE);
      cgPalette.addMouseOverListener(this);
      cgPalette.addActionListener(this);
      JPanel pPalette = new JPanel(new GridBagLayout());
      pPalette.setBorder(BorderFactory.createTitledBorder("Palette "));
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 4, 2, 4), 0, 0);
      pPalette.add(cgPalette, c);

      // creating information panel
      JPanel pInfo = new JPanel(new GridBagLayout());
      pInfo.setBorder(BorderFactory.createTitledBorder("Information "));
      JLabel lInfoIndexTitle = new JLabel("Index:");
      JLabel lInfoRGBTitle = new JLabel("RGBA:");
      JLabel lInfoHexRGBTitle = new JLabel("Hex:");
      // XXX: making sure that the initial size of the components is big enough to hold all valid data
      lInfoIndex = new JLabel("1999");
      lInfoRGB = new JLabel(String.format(FmtInfoRGB, 1999, 1999, 1999, 1999));
      lInfoHexRGB = new JLabel(String.format(FmtInfoHexRGB, 0xAAA, 0xAAA, 0xAAA, 0xAAA));
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
      pInfo.add(lInfoIndexTitle, c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 8, 0, 4), 0, 0);
      pInfo.add(lInfoIndex, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
      pInfo.add(lInfoRGBTitle, c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 8, 0, 4), 0, 0);
      pInfo.add(lInfoRGB, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
      pInfo.add(lInfoHexRGBTitle, c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 8, 4, 4), 0, 0);
      pInfo.add(lInfoHexRGB, c);

      // creating button section
      JPanel pButtons = new JPanel(new GridBagLayout());
      bSelectAll = new JButton("Select all");
      bSelectAll.setMnemonic('a');
      bSelectAll.addActionListener(this);
      bSelectNone = new JButton("Select none");
      bSelectNone.setMnemonic('n');
      bSelectNone.addActionListener(this);
      bSelectInvert = new JButton("Invert selection");
      bSelectInvert.setMnemonic('i');
      bSelectInvert.addActionListener(this);
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0);
      pButtons.add(bSelectAll, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 4), 0, 0);
      pButtons.add(bSelectNone, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
      pButtons.add(bSelectInvert, c);

      // putting sidebar together
      JPanel pSideBar = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pSideBar.add(pInfo, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
      pSideBar.add(pButtons, c);

      // putting all together
      JPanel pMain = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
      pMain.add(pPalette, c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
      pMain.add(pSideBar, c);

      // and adding to main panel
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
      add(pMain, c);

      updateInfoBox(-1);
    }

    // Updates the information panel
    private void updateInfoBox(int index)
    {
      if (index >= 0 && index < cgPalette.getColorCount()) {
        Color c = cgPalette.getColor(index);
        setLabelText(lInfoIndex, Integer.toString(index), null);
        setLabelText(lInfoRGB, String.format(FmtInfoRGB, c.getRed(), c.getGreen(), c.getBlue(),
                                                         c.getAlpha()), null);
        setLabelText(lInfoHexRGB, String.format(FmtInfoHexRGB, c.getRed(), c.getGreen(), c.getBlue(),
                                                               c.getAlpha()), null);
      } else {
        setLabelText(lInfoIndex, "", null);
        setLabelText(lInfoRGB, "", null);
        setLabelText(lInfoHexRGB, "", null);
      }
    }

    // Sets a new text to the specified JLabel component while retaining its preferred size
    private void setLabelText(JLabel c, String text, Dimension d)
    {
      if (c != null) {
        if (text == null) text = "";
        if (d == null) {
          d = c.getPreferredSize();
        }
        c.setText(text);
        c.setPreferredSize(d);
      }
    }

    private void fireChangeListener()
    {
      ChangeEvent event = new ChangeEvent(this);
      for (int i = 0; i < listChangeListeners.size(); i++) {
        listChangeListeners.get(i).stateChanged(event);
      }
    }
  }
}
