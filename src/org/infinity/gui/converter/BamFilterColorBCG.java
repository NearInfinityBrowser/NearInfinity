// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ButtonPopupWindow;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * Color filter: adjust brightness, contrast and gamma.
 */
public class BamFilterColorBCG extends BamFilterBaseColor
    implements ChangeListener, ActionListener
{
  private static final String FilterName = "Brightness/Contrast/Gamma";
  private static final String FilterDesc = "This filter provides controls for adjusting brightness, " +
                                           "contrast and gamma";

  private static final double GammaScaleFactor = 100.0;   // the scale factor for gamma slider

  private JSlider sliderBrightness, sliderContrast, sliderGamma;
  private JSpinner spinnerBrightness, spinnerContrast, spinnerGamma;
  private ButtonPopupWindow bpwExclude;
  private BamFilterBaseColor.ExcludeColorsPanel pExcludeColors;

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  public BamFilterColorBCG(ConvertToBam parent)
  {
    super(parent, FilterName, FilterDesc);
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
  public void updateControls()
  {
    bpwExclude.setEnabled(getConverter().isBamV1Selected());
  }

  @Override
  public String getConfiguration()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(sliderBrightness.getValue()).append(';');
    sb.append(sliderContrast.getValue()).append(';');
    sb.append(((SpinnerNumberModel)spinnerGamma.getModel()).getNumber().doubleValue()).append(';');
    sb.append(encodeColorList(pExcludeColors.getSelectedIndices()));
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config)
  {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.trim().split(";");
        Integer bValue = Integer.MIN_VALUE;
        Integer cValue = Integer.MIN_VALUE;
        Double gValue = Double.MIN_VALUE;
        int[] indices = null;

        // parsing configuration data
        if (params.length > 0) {  // set brightness value
          bValue = decodeNumber(params[0], sliderBrightness.getMinimum(), sliderBrightness.getMaximum(), Integer.MIN_VALUE);
          if (bValue == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 1) {  // set contrast value
          cValue = decodeNumber(params[1], sliderContrast.getMinimum(), sliderContrast.getMaximum(), Integer.MIN_VALUE);
          if (cValue == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 2) {  // set gamma value
          double min = ((Number)((SpinnerNumberModel)spinnerGamma.getModel()).getMinimum()).doubleValue();
          double max = ((Number)((SpinnerNumberModel)spinnerGamma.getModel()).getMaximum()).doubleValue();
          gValue = decodeDouble(params[2], min, max, Double.MIN_VALUE);
          if (gValue == Double.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 3) {
          indices = decodeColorList(params[3]);
          if (indices == null) {
            return false;
          }
        }

        // applying configuration data
        if (bValue != Integer.MIN_VALUE) {
          sliderBrightness.setValue(bValue);
        }
        if (cValue != Integer.MIN_VALUE) {
          sliderContrast.setValue(cValue);
        }
        if (gValue != Double.MIN_VALUE) {
          sliderGamma.setValue((int)(gValue * GammaScaleFactor));
        }
        if (indices != null) {
          pExcludeColors.setSelectedIndices(indices);
        }
      }
      return true;
    }
    return false;
  }

  @Override
  protected JPanel loadControls()
  {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l1 = new JLabel("Exclude colors:");
    pExcludeColors = new BamFilterBaseColor.ExcludeColorsPanel(
        getConverter().getPaletteDialog().getPalette(getConverter().getPaletteDialog().getPaletteType()));
    pExcludeColors.addChangeListener(this);
    bpwExclude = new ButtonPopupWindow("Palette", Icons.getIcon(Icons.ICON_ARROW_DOWN_15), pExcludeColors);
    bpwExclude.setIconTextGap(8);
    bpwExclude.addActionListener(this);
    bpwExclude.setEnabled(getConverter().isBamV1Selected());
    JPanel pExclude = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pExclude.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pExclude.add(bpwExclude, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pExclude.add(new JPanel(), c);

    JLabel lb = new JLabel("Brightness:");
    JLabel lc = new JLabel("Contrast:");
    JLabel lg = new JLabel("Gamma:");
    sliderBrightness = new JSlider(SwingConstants.HORIZONTAL, -100, 100, 0);
    sliderBrightness.addChangeListener(this);
    sliderContrast = new JSlider(SwingConstants.HORIZONTAL, -100, 100, 0);
    sliderContrast.addChangeListener(this);
    sliderGamma = new JSlider(SwingConstants.HORIZONTAL, 1, 500, 100);
    sliderGamma.addChangeListener(this);
    spinnerBrightness = new JSpinner(new SpinnerNumberModel(sliderBrightness.getValue(),
                                                            sliderBrightness.getMinimum(),
                                                            sliderBrightness.getMaximum(), 1));
    spinnerBrightness.addChangeListener(this);
    spinnerContrast = new JSpinner(new SpinnerNumberModel(sliderContrast.getValue(),
                                                          sliderContrast.getMinimum(),
                                                          sliderContrast.getMaximum(), 1));
    spinnerContrast.addChangeListener(this);
    spinnerGamma = new JSpinner(new SpinnerNumberModel((double)sliderGamma.getValue() / GammaScaleFactor,
                                                       (double)sliderGamma.getMinimum() / GammaScaleFactor,
                                                       (double)sliderGamma.getMaximum() / GammaScaleFactor, 0.1));
    spinnerGamma.addChangeListener(this);

    JPanel p = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p.add(lb, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    p.add(sliderBrightness, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    p.add(spinnerBrightness, c);

    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(lc, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(sliderContrast, c);
    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(spinnerContrast, c);

    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(lg, c);
    ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(sliderGamma, c);
    ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(spinnerGamma, c);
    ViewerUtil.setGBC(c, 0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    p.add(pExclude, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p, c);

    return panel;
  }

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == pExcludeColors) {
      fireChangeListener();
    } else if (event.getSource() == sliderBrightness) {
      spinnerBrightness.setValue(Integer.valueOf(sliderBrightness.getValue()));
      if (sliderBrightness.getModel().getValueIsAdjusting() == false) {
        fireChangeListener();
      }
    } else if (event.getSource() == sliderContrast) {
      spinnerContrast.setValue(Integer.valueOf(sliderContrast.getValue()));
      if (sliderContrast.getModel().getValueIsAdjusting() == false) {
        fireChangeListener();
      }
    } else if (event.getSource() == sliderGamma) {
      spinnerGamma.setValue(Double.valueOf((double)sliderGamma.getValue() / GammaScaleFactor));
      if (sliderGamma.getModel().getValueIsAdjusting() == false) {
        fireChangeListener();
      }
    } else if (event.getSource() == spinnerBrightness) {
      sliderBrightness.setValue(((Integer)spinnerBrightness.getValue()).intValue());
    } else if (event.getSource() == spinnerContrast) {
      sliderContrast.setValue(((Integer)spinnerContrast.getValue()).intValue());
    } else if (event.getSource() == spinnerGamma) {
      double v = ((Double)spinnerGamma.getValue()).doubleValue() * GammaScaleFactor;
      sliderGamma.setValue((int)v);
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bpwExclude) {
      pExcludeColors.updatePalette(getConverter().getPaletteDialog().getPalette(
          getConverter().getPaletteDialog().getPaletteType()));
    }
  }

//--------------------- End Interface ActionListener ---------------------

  private BufferedImage applyEffect(BufferedImage srcImage)
  {
    if (srcImage != null) {
      int[] buffer;
      IndexColorModel cm = null;
      boolean isPremultiplied = false;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        // paletted image
        cm = (IndexColorModel)srcImage.getColorModel();
        buffer = new int[1 << cm.getPixelSize()];
        cm.getRGBs(buffer);
        isPremultiplied = cm.isAlphaPremultiplied();
        // applying proper alpha
        if (!cm.hasAlpha()) {
          final int Green = 0x0000ff00;
          boolean greenFound = false;
          for (int i = 0; i < buffer.length; i++) {
            if (!greenFound && buffer[i] == Green) {
              greenFound = true;
              buffer[i] &= 0x00ffffff;
            } else {
              buffer[i] |= 0xff000000;
            }
          }
        }
      } else if (srcImage.getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
        // truecolor image
        buffer = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
        isPremultiplied = srcImage.isAlphaPremultiplied();
      } else {
        buffer = new int[0];
      }

      // brightness in range [-100, 100]
      float brightness = ((Integer)spinnerBrightness.getValue()).floatValue() / 100.0f;
      // contrast in range [-100, 100]
      float contrast = (((Integer)spinnerContrast.getValue()).floatValue() + 100.0f) / 100.0f;
      // gamma in range [0.01, 5.0]
      float gamma2 = 1.0f / ((Double)spinnerGamma.getValue()).floatValue();

      for (int i = 0; i < buffer.length; i++) {
        if ((cm == null || (cm != null && !pExcludeColors.isSelectedIndex(i))) &&
            (buffer[i] & 0xff000000) != 0) {
          // extracting color channels
          float fa = isPremultiplied ? (float)((buffer[i] >>> 24) & 0xff) : 255.0f;
          float fr = (float)((buffer[i] >>> 16) & 0xff) / fa;
          float fg = (float)((buffer[i] >>> 8) & 0xff) / fa;
          float fb = (float)(buffer[i] & 0xff) / fa;

          // applying brightness
          if (brightness != 0.0f) {
            fr += brightness;
            fg += brightness;
            fb += brightness;
            if (fr < 0.0f) fr = 0.0f; else if (fr > 1.0f) fr = 1.0f;
            if (fg < 0.0f) fg = 0.0f; else if (fg > 1.0f) fg = 1.0f;
            if (fb < 0.0f) fb = 0.0f; else if (fb > 1.0f) fb = 1.0f;
          }

          // applying contrast
          if (contrast != 0.0f) {
            fr = ((fr - 0.5f) * contrast) + 0.5f;
            fg = ((fg - 0.5f) * contrast) + 0.5f;
            fb = ((fb - 0.5f) * contrast) + 0.5f;
            if (fr < 0.0f) fr = 0.0f; else if (fr > 1.0f) fr = 1.0f;
            if (fg < 0.0f) fg = 0.0f; else if (fg > 1.0f) fg = 1.0f;
            if (fb < 0.0f) fb = 0.0f; else if (fb > 1.0f) fb = 1.0f;
          }

          // applying gamma
          if (gamma2 != 1.0f) {
            fr = (float)Math.pow((double)fr, gamma2);
            fg = (float)Math.pow((double)fg, gamma2);
            fb = (float)Math.pow((double)fb, gamma2);
            if (fr < 0.0f) fr = 0.0f; else if (fr > 1.0f) fr = 1.0f;
            if (fg < 0.0f) fg = 0.0f; else if (fg > 1.0f) fg = 1.0f;
            if (fb < 0.0f) fb = 0.0f; else if (fb > 1.0f) fb = 1.0f;
          }

          // reverting to int RGB
          int ir = (int)(fr * fa); if (ir < 0) ir = 0; else if (ir > 255) ir = 255;
          int ig = (int)(fg * fa); if (ig < 0) ig = 0; else if (ig > 255) ig = 255;
          int ib = (int)(fb * fa); if (ib < 0) ib = 0; else if (ib > 255) ib = 255;
          buffer[i] = (buffer[i] & 0xff000000) | (ir << 16) | (ig << 8) | ib;
        }
      }

      if (cm != null) {
        // recreating paletted image
        IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), buffer.length, buffer, 0,
                                                  cm.hasAlpha(), cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        byte[] srcPixels = ((DataBufferByte)srcImage.getRaster().getDataBuffer()).getData();
        byte[] dstPixels = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
        srcImage = dstImage;
        srcPixels = null; dstPixels = null;
      }
    }

    return srcImage;
  }
}
