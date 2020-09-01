// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ColorGrid;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.ColorGrid.MouseOverEvent;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.Misc;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.StreamUtils;

// TODO: change filter to display the palette in the state after applying previous filters (if any)
/**
 * Color filter: adjust palette entries (for BAM v1 only).
 */
public class BamFilterColorReplace extends BamFilterBaseColor implements ActionListener
{
  private static final String FilterName = "Replace colors (BAM v1 only)";
  private static final String FilterDesc = "This filter allows you to manipulate individual palette " +
                                           "entries or replace the palette as a whole.\n" +
                                           "Note: Supports legacy BAM (v1) only.";

  private final PaletteDialog paletteDialog;
  private JButton bPalette;

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  public BamFilterColorReplace(ConvertToBam parent)
  {
    super(parent, FilterName, FilterDesc);
    paletteDialog = new PaletteDialog(parent);
  }

  @Override
  public BufferedImage process(BufferedImage frame) throws Exception
  {
    return applyEffect(frame);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry entry)
  {
    if (entry != null) {
      entry.setFrame(applyEffect(entry.getFrame()));
    }
    return entry;
  }

  @Override
  protected JPanel loadControls()
  {
    GridBagConstraints c = new GridBagConstraints();

    bPalette = new JButton("Palette...");
    bPalette.addActionListener(this);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(bPalette, c);

    updateControls();

    return panel;
  }

  @Override
  public void updateControls()
  {
    // supportys BAM v1 only
    bPalette.setEnabled(getConverter().isBamV1Selected());
  }

  @Override
  public String getConfiguration()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(encodeColorList(paletteDialog.getPalette()));
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config)
  {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.trim().split(";");
        int[] colors = null;

        if (params.length > 0) {
          colors = decodePalette(params[0]);
          if (colors == null) {
            return false;
          }
        }

        if (colors != null) {
          paletteDialog.loadPalette(colors);
        }
      }
      return true;
    }
    return false;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bPalette) {
      paletteDialog.open();
      fireChangeListener();
    }
  }

//--------------------- End Interface ActionListener ---------------------


  private BufferedImage applyEffect(BufferedImage srcImage)
  {
    BufferedImage dstImage = srcImage;
    if (srcImage != null &&
        paletteDialog.isModified() &&
        srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
      int width = srcImage.getWidth();
      int height = srcImage.getHeight();
      IndexColorModel cm = (IndexColorModel)srcImage.getColorModel();
      int[] newPalette = paletteDialog.getPalette();

      IndexColorModel cm2 = new IndexColorModel(8, 256, newPalette, 0, cm.hasAlpha(),
                                                cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
      dstImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm2);
      byte[] srcBuffer = ((DataBufferByte)srcImage.getRaster().getDataBuffer()).getData();
      byte[] dstBuffer = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
      System.arraycopy(srcBuffer, 0, dstBuffer, 0, dstBuffer.length);
      srcBuffer = null; dstBuffer = null;
    }

    return dstImage;
  }

  /** Parses a list of color values from a parameter string of the format "[rgb1,rgb2,...]". */
  private int[] decodePalette(String param)
  {
    int[] palette = null;
    if (param != null && param.matches("\\[.*\\]")) {
      String colorString = param.substring(1, param.length() - 1).trim();
      if (!colorString.isEmpty()) {
        String[] colors = colorString.split(",");
        palette = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
          palette[i] = Misc.toNumber(colors[i], 0);
        }
      } else {
        palette = new int[0];
      }
    }
    return palette;
  }

//-------------------------- INNER CLASSES --------------------------

  // Provides controls for modifying individual colors or the whole palette
  private static class PaletteDialog extends JDialog
      implements ActionListener, ChangeListener, FocusListener, ColorGrid.MouseOverListener
  {
    private static final String TitleClean = "Replacement palette";
    private static final String TitleModified = "Replacement palette [enabled]";
    private static final String FmtInfoRGB    = "%d  %d  %d  %d";
    private static final String FmtInfoHexRGB = "#%02X%02X%02X%02X";

    private final ConvertToBam parent;

    private ColorGrid cgPalette;
    private JLabel lInfoIndex, lInfoRGB, lInfoHexRGB, lColorIndex;
    private JTextField tfColorRed, tfColorGreen, tfColorBlue, tfColorAlpha;
    private JButton bClose, bLoadPalette, bResetPalette;
    private int currentRed, currentGreen, currentBlue, currentAlpha;
    private boolean isModified;

    public PaletteDialog(ConvertToBam parent)
    {
      super(parent, TitleClean, Dialog.ModalityType.DOCUMENT_MODAL);
      this.parent = parent;
      init();
    }

    /** Shows the dialog. */
    public void open()
    {
      if (!isVisible()) {
        if (!isModified()) {
          resetPalette();
        }
        setLocationRelativeTo(getOwner());
        setVisible(true);
        toFront();
        requestFocusInWindow();
      }
    }

    /** Hides the dialog. */
    public void close()
    {
      if (isVisible()) {
        if (tfColorRed.isFocusOwner()) {
          registerColorValue(tfColorRed);
        } else if (tfColorGreen.isFocusOwner()) {
          registerColorValue(tfColorGreen);
        } else if (tfColorBlue.isFocusOwner()) {
          registerColorValue(tfColorBlue);
        } else if (tfColorAlpha.isFocusOwner()) {
          registerColorValue(tfColorAlpha);
        }
        setVisible(false);
      }
    }

    /**
     * Returns whether the palette contains any modified data.
     * (Either because an external palette has been loaded or color entries have been modified.)
     */
    public boolean isModified()
    {
      return isModified;
    }

    /** Returns the currently defined palette as int array. */
    public int[] getPalette()
    {
      int[] retVal = new int[256];
      int max = Math.min(retVal.length, cgPalette.getColorCount());
      for (int i = 0; i < max; i++) {
        retVal[i] = cgPalette.getColor(i).getRGB();
      }
      return retVal;
    }

    /** Loads the palette from the specified file resource into the color grid component. */
    public void loadPalette(Path paletteFile) throws Exception
    {
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
          palette = ColorConvert.loadPalettePNG(paletteFile, ConvertToBam.getUseAlpha());
        } else if ("RIFF".equals(new String(signature, 0, 4))) {
          palette = ColorConvert.loadPalettePAL(paletteFile);
        } else {
          String sig = new String(signature, 0, 4);
          String ver = new String(signature, 4, 4);
          if ("BAM ".equals(sig) || "BAMC".equals(sig)) {
            if ("V1  ".equals(ver)) {
              palette = ColorConvert.loadPaletteBAM(paletteFile, ConvertToBam.getUseAlpha());
            } else {
              throw new Exception(String.format("BAM file \"%s\" does not contain palette data.",
                                                paletteFile.getFileName()));
            }
          } else {
            // Photoshop ACT files don't have a header
            palette = ColorConvert.loadPaletteACT(paletteFile);
          }
        }

        // applying palette
        if (palette != null && palette.length > 0) {
          loadPalette(palette);
        } else {
          throw new Exception("No palette found in file " + paletteFile.getFileName());
        }
      } else {
        throw new Exception("File does not exist.");
      }
    }

    /** Loads the palette from the specified color array into the color grid component. */
    public void loadPalette(int[] colors)
    {
      if (colors != null && colors.length > 0) {
        int max = Math.min(cgPalette.getColorCount(), colors.length);
        for (int i = 0; i < max; i++) {
          cgPalette.setColor(i, new Color(colors[i], true));
        }
        for (int i = max; i < cgPalette.getColorCount(); i++) {
          cgPalette.setColor(i, Color.BLACK);
        }
        setModified();
      }
    }

    /** Discards the current palette and loads the default palette as defined in the ConvertToBam instance. */
    public void resetPalette()
    {
      parent.getPaletteDialog().updateGeneratedPalette();
      int[] colors = parent.getPaletteDialog().getPalette(parent.getPaletteDialog().getPaletteType());
      if (colors != null) {
        int max = Math.min(cgPalette.getColorCount(), colors.length);
        for (int i = 0; i < max; i++) {
          cgPalette.setColor(i, new Color(colors[i], true));
        }
        for (int i = max; i < cgPalette.getColorCount(); i++) {
          cgPalette.setColor(i, Color.BLACK);
        }
      }
      resetModified();
    }

//--------------------- Begin Interface ActionListener ---------------------

    @Override
    public void actionPerformed(ActionEvent event)
    {
      if (event.getSource() == cgPalette) {
        updateColorBox(cgPalette.getSelectedIndex(), cgPalette.getSelectedColor());
      } else if (event.getSource() == bClose) {
        close();
      } else if (event.getSource() == bLoadPalette) {
        Path[] files = ConvertToBam.getOpenFileName(this, "Load palette from", null, false, ConvertToBam.getPaletteFilters(), 0);
        if (files != null && files.length > 0) {
          try {
            loadPalette(files[0]);
          } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
          }
        }
      } else if (event.getSource() == bResetPalette) {
        resetPalette();
      }
    }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

    @Override
    public void stateChanged(ChangeEvent event)
    {
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
      if (event.getSource() instanceof JTextField) {
        registerColorValue((JTextField)event.getSource());
      }
    }

//--------------------- End Interface FocusListener ---------------------

//--------------------- Begin Interface MouseOverListener ---------------------

    @Override
    public void mouseOver(MouseOverEvent event)
    {
      if (event.getSource() == cgPalette) {
        updateInfoBox(event.getColorIndex());
      }
    }

//--------------------- End Interface MouseOverListener ---------------------

    private void init()
    {
      // first-time initializations
      currentRed = currentGreen = currentBlue = currentAlpha = 0;
      isModified = false;

      GridBagConstraints c = new GridBagConstraints();

      // creating palette section
      JPanel pPalette = new JPanel(new GridBagLayout());
      pPalette.setBorder(BorderFactory.createTitledBorder("Palette "));
      cgPalette = new ColorGrid(256);
      cgPalette.setSelectionFrame(ColorGrid.Frame.DOUBLE_LINE);
      cgPalette.addActionListener(this);
      cgPalette.addMouseOverListener(this);
      cgPalette.addChangeListener(this);
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 4, 2, 4), 0, 0);
      pPalette.add(cgPalette, c);

      // creating information panel
      JPanel pInfo = new JPanel(new GridBagLayout());
      pInfo.setBorder(BorderFactory.createTitledBorder("Information "));
      JLabel lInfoIndexTitle = new JLabel("Index:");
      JLabel lInfoRGBTitle = new JLabel("RGB:");
      JLabel lInfoHexRGBTitle = new JLabel("Hex:");
      lInfoIndex = new JLabel("255", SwingConstants.LEFT);
      lInfoRGB = new JLabel(String.format(FmtInfoRGB, 255, 255, 255, 255));
      lInfoHexRGB = new JLabel(String.format(FmtInfoHexRGB, 0xAA, 0xAA, 0xAA, 0xAA));
      lInfoHexRGB.setMinimumSize(lInfoHexRGB.getPreferredSize());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
      pInfo.add(lInfoIndexTitle, c);
      c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 8, 0, 4), 0, 0);
      pInfo.add(lInfoIndex, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 4, 0, 4), 0, 0);
      pInfo.add(lInfoRGBTitle, c);
      c = ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 8, 0, 4), 0, 0);
      pInfo.add(lInfoRGB, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(4, 4, 4, 4), 0, 0);
      pInfo.add(lInfoHexRGBTitle, c);
      c = ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
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

      // creating "Options" panel
      bLoadPalette = new JButton("Load palette...");
      bLoadPalette.addActionListener(this);
      bResetPalette = new JButton("Reset palette");
      bResetPalette.addActionListener(this);
      JPanel pOptions = new JPanel(new GridBagLayout());
      pOptions.setBorder(BorderFactory.createTitledBorder("Options "));
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 4), 0, 0);
      pOptions.add(bLoadPalette, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 4, 4, 4), 0, 0);
      pOptions.add(bResetPalette, c);

      // putting right sidebar together
      JPanel pSideBar = new JPanel(new GridBagLayout());
      c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
      pSideBar.add(pInfo, c);
      c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
      pSideBar.add(pColor, c);
      c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
      pSideBar.add(pOptions, c);

      // creating bottom bar
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

      resetPalette();
      updateInfoBox(cgPalette.getSelectedIndex());
      updateColorBox(cgPalette.getSelectedIndex(), cgPalette.getSelectedColor());
    }

    // marks palette as modified
    private void setModified() { isModified = true; updateState(); }

    // resets modified flag
    private void resetModified() { isModified = false; updateState(); }

    // Updates dialog components
    private void updateState()
    {
      setTitle(isModified() ? TitleModified : TitleClean);
      bResetPalette.setEnabled(isModified());
    }

    // Updates the information panel
    private void updateInfoBox(int index)
    {
      if (index >= 0 && index < cgPalette.getColorCount()) {
        Color c = cgPalette.getColor(index);
        lInfoIndex.setText(Integer.toString(index));
        lInfoRGB.setText(String.format(FmtInfoRGB, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()));
        lInfoHexRGB.setText(String.format(FmtInfoHexRGB, c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha()));
      } else {
        lInfoIndex.setText("");
        lInfoRGB.setText("");
        lInfoHexRGB.setText("");
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

    // Applies the color values specified in the color edit controls to the currently selected color
    private void updateCurrentColor()
    {
      if (cgPalette.getSelectedIndex() >= 0) {
        Color c = new Color(currentRed, currentGreen, currentBlue, currentAlpha);
        if (!c.equals(cgPalette.getSelectedColor())) {
          cgPalette.setColor(cgPalette.getSelectedIndex(), c);
          setModified();
        }
      }
    }

    // Parses color component value from specified text field
    private void registerColorValue(JTextField tf)
    {
      if (tf == tfColorRed) {
        currentRed = ConvertToBam.numberValidator(tfColorRed.getText(), 0, 255, currentRed);
        tfColorRed.setText(Integer.toString(currentRed));
        updateCurrentColor();
      } else if (tf == tfColorGreen) {
        currentGreen = ConvertToBam.numberValidator(tfColorGreen.getText(), 0, 255, currentGreen);
        tfColorGreen.setText(Integer.toString(currentGreen));
        updateCurrentColor();
      } else if (tf == tfColorBlue) {
        currentBlue = ConvertToBam.numberValidator(tfColorBlue.getText(), 0, 255, currentBlue);
        tfColorBlue.setText(Integer.toString(currentBlue));
        updateCurrentColor();
      } else if (tf == tfColorAlpha) {
        currentAlpha = ConvertToBam.numberValidator(tfColorAlpha.getText(), 0, 255, currentAlpha);
        tfColorAlpha.setText(Integer.toString(currentAlpha));
        updateCurrentColor();
      }
    }

  }
}
