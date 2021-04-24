// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ButtonPopupMenu;
import org.infinity.gui.ColorGrid;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.StreamUtils;

/**
 * A dialog for managing BAM v1 palette entries.
 */
class BamPaletteDialog extends JDialog
    implements FocusListener, ActionListener, ChangeListener, ColorGrid.MouseOverListener
{
  /** Specifies generated palette type */
  public static final int TYPE_GENERATED  = 0;
  /** Specifies external palette type */
  public static final int TYPE_EXTERNAL   = 1;

  private static final String[] PaletteTypeInfo = {"Generated palette", "External palette"};
  private static final String FmtInfoRGB    = "%d  %d  %d  %d";
  private static final String FmtInfoHexRGB = "#%02X%02X%02X%02X";

  // Stores all available color values of the current BAM and their number of occurence for faster palette creation
  private final LinkedHashMap<Integer, Integer> colorMap = new LinkedHashMap<>();
  private final int[][] palettes = new int[2][];

  private ConvertToBam converter;
  private ColorGrid cgPalette;
  private JLabel lInfoType, lInfoIndex, lInfoRGB, lInfoHexRGB, lColorIndex;
  private JTextField tfColorRed, tfColorGreen, tfColorBlue, tfColorAlpha, tfCompressedColor;
  private JMenuItem miPaletteSet, miPaletteClear;
  private ButtonPopupMenu bpmPalette;
  private JCheckBox cbLockPalette;
  private JButton bClose;
  private int currentPaletteType, currentRed, currentGreen, currentBlue, currentAlpha, rleIndex;
  private boolean lockedPalette, hasExternalPalette, paletteModified;

  public BamPaletteDialog(ConvertToBam parent)
  {
    super(parent, "Palette", Dialog.ModalityType.DOCUMENT_MODAL);
    this.converter = parent;
    init();
  }

  /** Shows the dialog. */
  public void open()
  {
    if (!isVisible()) {
      updateGeneratedPalette();
      setLocationRelativeTo(getOwner());
      setVisible(true);
      toFront();
      requestFocusInWindow();
    }
  }

  /** Hides the dialog */
  public void close()
  {
    if (isVisible()) {
      setVisible(false);
    }
  }

  /** Resets the current palette dialog state. */
  public void clear()
  {
    Arrays.fill(palettes[TYPE_GENERATED], 0);
    Arrays.fill(palettes[TYPE_EXTERNAL], 0);

    cgPalette.setSelectedIndex(-1);
    currentRed = currentGreen = currentBlue = currentAlpha = 0;
    rleIndex = 0;
    currentPaletteType = TYPE_GENERATED;
    hasExternalPalette = false;
    lockedPalette = false;
    colorMap.clear();
    cbLockPalette.setEnabled(true);
    cbLockPalette.setSelected(lockedPalette);
    miPaletteClear.setEnabled(false);

    applyPalette(currentPaletteType);
    updateInfoBox(-1);
    updateColorBox(-1, null);
  }

  /** Returns the associated ConverToBam instance. */
  public ConvertToBam getConverter()
  {
    return converter;
  }

  /** Returns the index of the RLE-compressed color. */
  public int getRleIndex()
  {
    return rleIndex;
  }

  /** Returns the global color map for the current BAM structure. */
  public HashMap<Integer, Integer> getColorMap()
  {
    return colorMap;
  }

  /** Indicates whether to prevent updating the current palette automatically. */
  public boolean isPaletteLocked()
  {
    return isExternalPalette() || lockedPalette;
  }

  /** A convenience method for determining the current type of the palette. */
  public boolean isExternalPalette()
  {
    return (currentPaletteType == TYPE_EXTERNAL);
  }

  /** Returns the currently active palette. (Either one of TYPE_GENERATED or TYPE_EXTERNAL.) */
  public int getPaletteType()
  {
    return currentPaletteType;
  }

  /** Specify the new active palette. (Either one of TYPE_GENERATED or TYPE_EXTERNAL.) */
  public void setPaletteType(int type)
  {
    switch (type) {
      case TYPE_EXTERNAL:
        if (!hasExternalPalette()) {
          break;
        }
      case TYPE_GENERATED:
        if (type != currentPaletteType) {
          currentPaletteType = type;
          applyPalette(currentPaletteType);
          lInfoType.setText(PaletteTypeInfo[currentPaletteType]);
        }
        break;
    }
  }

  /** Returns the specified palette if available. Returns {@code null} if palette is not available. */
  public int[] getPalette(int type)
  {
    switch (type) {
      case TYPE_EXTERNAL:
        if (!hasExternalPalette()) {
          break;
        }
      case TYPE_GENERATED:
      {
        return palettes[type];
      }
    }

    return null;
  }

  /**
   * Defines the colors of a specific palette.
   * @param type either one of TYPE_GENERATED or TYPE_EXTERNAL.
   * @param palette The palette data to assign.
   */
  public void setPalette(int type, int[] palette)
  {
    switch (type) {
      case TYPE_EXTERNAL:
        hasExternalPalette = (palette != null);
      case TYPE_GENERATED:
      {
        if (!isPaletteLocked() || type == TYPE_EXTERNAL) {
          if (palette != null) {
            // loading palette data
            for (int i = 0; i < palette.length; i++) {
              palettes[type][i] = palette[i];
            }
            for (int i = palette.length; i < palettes[type].length; i++) {
              palettes[type][i] = 0;
            }
            // updating current palette if needed
            if (type == currentPaletteType) {
              applyPalette(type);
            }
          }
        }
        break;
      }
    }
  }

  /** Returns whether an external palette has been defined. */
  public boolean hasExternalPalette()
  {
    return hasExternalPalette;
  }

  /** Loads the palette from the specified file resource into the specified palette slot. */
  public void loadExternalPalette(int type, Path paletteFile) throws Exception
  {
    if (type != TYPE_EXTERNAL && type != TYPE_GENERATED) {
      throw new Exception("Internal error: Invalid palette slot specified!");
    }

    // fetching file signature
    if (paletteFile != null && FileEx.create(paletteFile).isFile()) {
      byte[] signature = new byte[8];
      try (InputStream is = StreamUtils.getInputStream(paletteFile)) {
        is.read(signature);
      } catch (IOException e) {
        throw new Exception("Error reading from file " + paletteFile.getFileName());
      }

      // fetching palette data
      int[] palette = null;
      if ("BM".equals(new String(signature, 0, 2))) {
        palette = ColorConvert.loadPaletteBMP(paletteFile);
      } else if (Arrays.equals(Arrays.copyOfRange(signature, 0, 4),
                               new byte[]{(byte)0x89, 0x50, 0x4e, 0x47})) {
        // PNG supports palette with alpha channel
        palette = ColorConvert.loadPalettePNG(paletteFile, ConvertToBam.getUseAlpha());
      } else if ("RIFF".equals(new String(signature, 0, 4))) {
        palette = ColorConvert.loadPalettePAL(paletteFile);
      } else {
        String s = new String(signature);
        if ("BAM V1  ".equals(s) || "BAMCV1  ".equals(s)) {
          palette = ColorConvert.loadPaletteBAM(paletteFile, ConvertToBam.getUseAlpha());
        } else {
          // Photoshop ACT files don't have a header
          palette = ColorConvert.loadPaletteACT(paletteFile);
        }
      }

      // applying palette
      if (palette != null && palette.length > 0) {
        System.arraycopy(palette, 0, palettes[type], 0, palette.length);
        for (int i = palette.length; i < palettes[type].length; i++) {
          palettes[type][i] = 0xff000000;
        }
      } else {
        throw new Exception("No palette found in file " + paletteFile.getFileName());
      }
    } else {
      throw new Exception("File does not exist.");
    }

    hasExternalPalette = true;
  }

  /** Removes the currently assigned external palette. */
  public void clearExternalPalette()
  {
    setPaletteType(TYPE_GENERATED);
    if (hasExternalPalette()) {
      Arrays.fill(palettes[TYPE_EXTERNAL], 0xff000000);
      hasExternalPalette = false;
    }
  }

  /** Returns whether the palette has been modified after the last call to {@link #updatePalette()}. */
  public boolean isPaletteModified()
  {
    return paletteModified;
  }

  /** Marks generated palette as modified. */
  public void setPaletteModified()
  {
    paletteModified = true;
  }

  /**
   * Calculates a new palette based on the currently registered colors and stores it in the
   * PaletteDialog instance.
   */
  public void updateGeneratedPalette()
  {
    if (isPaletteModified() && !lockedPalette) {
      if (colorMap.size() <= 256) {
        // checking whether all frames share the same palette
        boolean sharedPalette = true;
        List<PseudoBamFrameEntry> listFrames =
            getConverter().getBamDecoder(ConvertToBam.BAM_ORIGINAL).getFramesList();
        int[] palette = null;
        int[] tmpPalette = new int[256];
        for (int i = 0; i < listFrames.size(); i++) {
          BufferedImage image = listFrames.get(i).getFrame();
          if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            IndexColorModel cm = (IndexColorModel)image.getColorModel();
            if (palette == null) {
              palette = new int[256];
              cm.getRGBs(palette);
            } else {
              cm.getRGBs(tmpPalette);
              if (!Arrays.equals(palette, tmpPalette)) {
                sharedPalette = false;
                break;
              }
            }
          } else {
            sharedPalette = false;
            break;
          }
        }
        tmpPalette = null;

        if (sharedPalette) {
          // using shared palette as is
          System.arraycopy(palette, 0, palettes[TYPE_GENERATED], 0, palette.length);
        } else {
          // creating palette directly from color map without reduction
          Iterator<Integer> iter = colorMap.keySet().iterator();
          int idx = 0;
          while (idx < 256 && iter.hasNext()) {
            palettes[TYPE_GENERATED][idx] = iter.next();
            idx++;
          }
          for (; idx < 256; idx++) {
            palettes[TYPE_GENERATED][idx] = 0xff000000;
          }
        }
      } else {
        // reducing color count to max. 256
        int[] pixels = new int[colorMap.size()];
        Iterator<Integer> iter = colorMap.keySet().iterator();
        int idx = 0;
        while (idx < pixels.length && iter.hasNext()) {
          pixels[idx] = iter.next();
          idx++;
        }
        ColorConvert.medianCut(pixels, 256, palettes[TYPE_GENERATED], false);
      }

      // moving special "green" to the first index
      if ((palettes[TYPE_GENERATED][0] & 0x00ffffff) != 0x0000ff00) {
        for (int i = 1; i < palettes[TYPE_GENERATED].length; i++) {
          if ((palettes[TYPE_GENERATED][i] & 0x00ffffff) == 0x0000ff00) {
            int v = palettes[TYPE_GENERATED][0];
            palettes[TYPE_GENERATED][0] = palettes[TYPE_GENERATED][i];
            palettes[TYPE_GENERATED][i] = v;
            break;
          }
        }
      }

      if (colorMap.size() > 256) {
        boolean ignoreAlpha = !(Boolean)Profile.getProperty(Profile.Key.IS_SUPPORTED_BAM_V1_ALPHA);
        int startIdx = (palettes[TYPE_GENERATED][0] & 0xffffff) == 0x00ff00 ? 1 : 0;
        ColorConvert.sortPalette(palettes[TYPE_GENERATED], startIdx, BamOptionsDialog.getSortPalette(), ignoreAlpha);
      }

      paletteModified = false;
    }
    // updating palette dialog
    if (currentPaletteType == TYPE_GENERATED) {
      applyPalette(currentPaletteType);
    }
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bClose) {
      close();
    } else if (event.getSource() == cgPalette) {
      updateColorBox(cgPalette.getSelectedIndex(), cgPalette.getSelectedColor());
    } else if (event.getSource() == miPaletteClear) {
      clearExternalPalette();
      cbLockPalette.setEnabled(true);
      cbLockPalette.setSelected(lockedPalette);
      miPaletteClear.setEnabled(false);
      lInfoType.setText(PaletteTypeInfo[currentPaletteType]);
    } else if (event.getSource() == miPaletteSet) {
      Path[] files = ConvertToBam.getOpenFileName(this, "Load palette from", null, false, ConvertToBam.getPaletteFilters(), 0);
      if (files != null && files.length > 0) {
        try {
          loadExternalPalette(TYPE_EXTERNAL, files[0]);
          setPaletteType(TYPE_EXTERNAL);
          applyPalette(TYPE_EXTERNAL);
          miPaletteClear.setEnabled(true);
          cbLockPalette.setSelected(true);
          cbLockPalette.setEnabled(false);
        } catch (Exception e) {
          JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } else if (event.getSource() == cbLockPalette) {
      lockedPalette = cbLockPalette.isSelected();
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == cgPalette) {
      updatePalette();
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface FocusListener ---------------------

  @Override
  public void focusGained(FocusEvent event)
  {
    if (event.getSource() instanceof JTextField) {
      ((JTextField)event.getSource()).selectAll();
    }
  }

  @Override
  public void focusLost(FocusEvent event)
  {
    if (event.getSource() == tfColorRed) {
      currentRed = ConvertToBam.numberValidator(tfColorRed.getText(), 0, 255, currentRed);
      tfColorRed.setText(Integer.toString(currentRed));
      updateCurrentColor();
    } else if (event.getSource() == tfColorGreen) {
      currentGreen = ConvertToBam.numberValidator(tfColorGreen.getText(), 0, 255, currentGreen);
      tfColorGreen.setText(Integer.toString(currentGreen));
      updateCurrentColor();
    } else if (event.getSource() == tfColorBlue) {
      currentBlue = ConvertToBam.numberValidator(tfColorBlue.getText(), 0, 255, currentBlue);
      tfColorBlue.setText(Integer.toString(currentBlue));
      updateCurrentColor();
    } else if (event.getSource() == tfColorAlpha) {
      currentAlpha = ConvertToBam.numberValidator(tfColorAlpha.getText(), 0, 255, currentAlpha);
      tfColorAlpha.setText(Integer.toString(currentAlpha));
      updateCurrentColor();
    } else if (event.getSource() == tfCompressedColor) {
      rleIndex = ConvertToBam.numberValidator(tfCompressedColor.getText(), 0, 255, rleIndex);
      tfCompressedColor.setText(Integer.toString(rleIndex));
    }
  }

//--------------------- End Interface FocusListener ---------------------

//--------------------- Begin Interface MouseOverListener ---------------------

  @Override
  public void mouseOver(ColorGrid.MouseOverEvent event)
  {
    if (event.getSource() == cgPalette) {
      updateInfoBox(event.getColorIndex());
    }
  }

//--------------------- End Interface MouseOverListener ---------------------

  private void init()
  {
    // first-time initializations
    palettes[TYPE_GENERATED] = new int[256];
    palettes[TYPE_EXTERNAL] = new int[256];
    currentPaletteType = TYPE_GENERATED;
    currentRed = currentGreen = currentBlue = currentAlpha = 0;
    rleIndex = 0;
    lockedPalette = false;
    hasExternalPalette = false;
    paletteModified = true;

    GridBagConstraints c = new GridBagConstraints();

    // creating palette section
    JPanel pPalette = new JPanel(new GridBagLayout());
    pPalette.setBorder(BorderFactory.createTitledBorder("Palette "));
    cgPalette = new ColorGrid(256);
    cgPalette.setColorEntrySize(new Dimension(18, 18));
    cgPalette.setSelectionFrame(ColorGrid.Frame.DOUBLE_LINE);
    cgPalette.setDragDropEnabled(true);
    cgPalette.addActionListener(this);
    cgPalette.addMouseOverListener(this);
    cgPalette.addChangeListener(this);
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 2, 4), 0, 0);
    pPalette.add(cgPalette, c);

    // creating information panel
    JPanel pInfo = new JPanel(new GridBagLayout());
    pInfo.setBorder(BorderFactory.createTitledBorder("Information "));
    lInfoType = new JLabel(PaletteTypeInfo[currentPaletteType]);
    JLabel lInfoIndexTitle = new JLabel("Index:");
    JLabel lInfoRGBTitle = new JLabel("RGBA:");
    JLabel lInfoHexRGBTitle = new JLabel("Hex:");
    lInfoIndex = new JLabel("255");
    lInfoIndex.setMinimumSize(lInfoIndex.getPreferredSize());
    lInfoRGB = new JLabel(String.format(FmtInfoRGB, 255, 255, 255, 255));
    lInfoHexRGB = new JLabel(String.format(FmtInfoHexRGB, 255, 255, 255, 255));
    lInfoHexRGB.setMinimumSize(lInfoHexRGB.getPreferredSize());
    c = ViewerUtil.setGBC(c, 0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pInfo.add(lInfoType, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pInfo.add(lInfoIndexTitle, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 8, 0, 4), 0, 0);
    pInfo.add(lInfoIndex, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pInfo.add(lInfoRGBTitle, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 8, 0, 4), 0, 0);
    pInfo.add(lInfoRGB, c);
    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pInfo.add(lInfoHexRGBTitle, c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 8, 4, 4), 0, 0);
    pInfo.add(lInfoHexRGB, c);

    // creating color edit panel
    JPanel pColor = new JPanel(new GridBagLayout());
    pColor.setBorder(BorderFactory.createTitledBorder("Color "));
    JLabel lColorIndexTitle = new JLabel("Index:");
    JLabel lColorRedTitle = new JLabel("Red:");
    JLabel lColorGreenTitle = new JLabel("Green:");
    JLabel lColorBlueTitle = new JLabel("Blue:");
    JLabel lColorAlphaTitle = new JLabel("Alpha:");
    lColorIndex = new JLabel("255");
    tfColorRed = new JTextField(4);
    tfColorRed.addFocusListener(this);
    tfColorGreen = new JTextField(4);
    tfColorGreen.addFocusListener(this);
    tfColorBlue = new JTextField(4);
    tfColorBlue.addFocusListener(this);
    tfColorAlpha = new JTextField(4);
    tfColorAlpha.addFocusListener(this);
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pColor.add(lColorIndexTitle, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 10, 0, 4), 0, 0);
    pColor.add(lColorIndex, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pColor.add(lColorRedTitle, c);
    c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 8, 0, 4), 0, 0);
    pColor.add(tfColorRed, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pColor.add(lColorGreenTitle, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 8, 0, 4), 0, 0);
    pColor.add(tfColorGreen, c);
    c = ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pColor.add(lColorBlueTitle, c);
    c = ViewerUtil.setGBC(c, 1, 3, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 8, 0, 4), 0, 0);
    pColor.add(tfColorBlue, c);
    c = ViewerUtil.setGBC(c, 0, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pColor.add(lColorAlphaTitle, c);
    c = ViewerUtil.setGBC(c, 1, 4, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 8, 4, 4), 0, 0);
    pColor.add(tfColorAlpha, c);
    pColor.setMinimumSize(pColor.getPreferredSize());

    // creating Options panel
    JPanel pOptions = new JPanel(new GridBagLayout());
    pOptions.setBorder(BorderFactory.createTitledBorder("Options "));
    miPaletteSet = new JMenuItem("Load palette...");
    miPaletteSet.addActionListener(this);
    miPaletteClear = new JMenuItem("Clear palette");
    miPaletteClear.addActionListener(this);
    miPaletteClear.setEnabled(currentPaletteType == TYPE_EXTERNAL);
    bpmPalette = new ButtonPopupMenu("External palette", new JMenuItem[]{miPaletteClear, miPaletteSet});
    bpmPalette.setIcon(Icons.getIcon(Icons.ICON_ARROW_UP_15));
    bpmPalette.setIconTextGap(8);
    cbLockPalette = new JCheckBox("Lock palette");
    cbLockPalette.setToolTipText("Selecting this option prevents automatic palette generation when modifying the global frames list");
    cbLockPalette.addActionListener(this);
    JLabel lCompressedColor = new JLabel("Compressed color:");
    tfCompressedColor = new JTextField(4);
    tfCompressedColor.setText("0");
    tfCompressedColor.setToolTipText("The compressed color index for RLE encoded frames");
    tfCompressedColor.addFocusListener(this);
    c = ViewerUtil.setGBC(c, 0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4), 0, 0);
    pOptions.add(bpmPalette, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(8, 0, 0, 4), 0, 0);
    pOptions.add(cbLockPalette, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pOptions.add(lCompressedColor, c);
    c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
    pOptions.add(tfCompressedColor, c);

    // putting right sidebar together
    JPanel pSideBar = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pSideBar.add(pInfo, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pSideBar.add(pColor, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pSideBar.add(pOptions, c);

    JPanel pBottom = new JPanel(new GridBagLayout());
    bClose = new JButton("Close");
    bClose.addActionListener(this);
    bClose.setMargin(new Insets(4, bClose.getInsets().left, 4, bClose.getInsets().right));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pBottom.add(bClose, c);

    // putting all together
    JPanel pMain = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 4, 4, 0), 0, 0);
    pMain.add(pPalette, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 8, 4, 4), 0, 0);
    pMain.add(pSideBar, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 8, 0), 0, 0);
    pMain.add(pBottom, c);

    setLayout(new BorderLayout());
    add(pMain, BorderLayout.CENTER);
    pack();
    setResizable(false);

    // "Closing" the dialog only makes it invisible
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    getRootPane().setDefaultButton(bClose);
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), getRootPane());
    getRootPane().getActionMap().put(getRootPane(), new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent event) { close(); }
    });

    updateInfoBox(cgPalette.getSelectedIndex());
    updateColorBox(cgPalette.getSelectedIndex(), cgPalette.getSelectedColor());
  }

  // Applies the specified palette to the color grid control and updates the edit box
  private void applyPalette(int paletteType)
  {
    switch (paletteType) {
      case TYPE_GENERATED:
      case TYPE_EXTERNAL:
      {
        for (int i = 0; i < palettes[paletteType].length; i++) {
          cgPalette.setColor(i, new Color(palettes[paletteType][i], true));
        }
        updateColorBox(cgPalette.getSelectedIndex(), cgPalette.getSelectedColor());
        break;
      }
    }
  }

  // Updates the information panel
  private void updateInfoBox(int index)
  {
    if (index >= 0 && index < cgPalette.getColorCount()) {
      Color c = cgPalette.getColor(index);
      lInfoType.setText(PaletteTypeInfo[currentPaletteType]);
      lInfoIndex.setText(Integer.toString(index));
      lInfoRGB.setText(String.format(FmtInfoRGB, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()));
      lInfoHexRGB.setText(String.format(FmtInfoHexRGB, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()));
    } else {
      lInfoType.setText(PaletteTypeInfo[currentPaletteType]);
      lInfoIndex.setText("");
      lInfoRGB.setText("");
      lInfoHexRGB.setText("");
    }
  }

  // Applies the color values specified in the color edit controls to the currently selected color
  private void updateCurrentColor()
  {
    if (cgPalette.getSelectedIndex() >= 0) {
      Color c = new Color(currentRed, currentGreen, currentBlue, currentAlpha);
      cgPalette.setColor(cgPalette.getSelectedIndex(), c);
      palettes[currentPaletteType][cgPalette.getSelectedIndex()] = c.getRGB();
    }
  }

  // Applies all colors of the color grid to the currently active palette
  private void updatePalette()
  {
    for (int i = 0; i < palettes[currentPaletteType].length; i++) {
      palettes[currentPaletteType][i] = cgPalette.getColor(i).getRGB();
    }
  }

  // Updates the color edit panel with the selected color data
  private void updateColorBox(int index, Color color)
  {
    boolean isValid = (index >= 0 && index < cgPalette.getColorCount());
    if (isValid) {
      lColorIndex.setText(Integer.toString(index));
    } else {
      lColorIndex.setText("");
    }
    if (color != null) {
      currentRed = color.getRed();
      currentGreen = color.getGreen();
      currentBlue = color.getBlue();
      currentAlpha = color.getAlpha();
    } else {
      currentRed = currentGreen = currentBlue = currentAlpha = 0;
    }
    tfColorRed.setText(Integer.toString(currentRed));
    tfColorGreen.setText(Integer.toString(currentGreen));
    tfColorBlue.setText(Integer.toString(currentBlue));
    tfColorAlpha.setText(Integer.toString(currentAlpha));
    tfColorRed.setEnabled(isValid);
    tfColorGreen.setEnabled(isValid);
    tfColorBlue.setEnabled(isValid);
    tfColorAlpha.setEnabled(isValid);
  }
}
