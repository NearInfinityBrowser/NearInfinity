// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
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
 * Color filter: adjust color balance for red, green, blue individually.
 */
public class BamFilterColorBalance extends BamFilterBaseColor implements ChangeListener, ActionListener {
  private static final String FILTER_NAME = "Color Balance";
  private static final String FILTER_DESC = "This filter provides controls for adjusting the "
      + "balance of each individual color channel.";

  private JSlider sliderRed;
  private JSlider sliderGreen;
  private JSlider sliderBlue;
  private JSpinner spinnerRed;
  private JSpinner spinnerGreen;
  private JSpinner spinnerBlue;
  private ButtonPopupWindow bpwExclude;
  private BamFilterBaseColor.ExcludeColorsPanel pExcludeColors;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterColorBalance(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public BufferedImage process(BufferedImage frame) throws Exception {
    return applyEffect(frame);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(int frameIndex, PseudoBamFrameEntry entry) {
    if (entry != null) {
      entry.setFrame(applyEffect(entry.getFrame()));
    }
    return entry;
  }

  @Override
  public void updateControls() {
    bpwExclude.setEnabled(getConverter().isBamV1Selected());
  }

  @Override
  public String getConfiguration() {
    return String.valueOf(sliderRed.getValue()) + ';' +
        sliderGreen.getValue() + ';' +
        sliderBlue.getValue() + ';' +
        encodeColorList(pExcludeColors.getSelectedIndices());
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.trim().split(";");
        int redValue = Integer.MIN_VALUE;
        int greenValue = Integer.MIN_VALUE;
        int blueValue = Integer.MIN_VALUE;
        int[] indices = null;

        // parsing configuration data
        if (params.length > 0) { // set red value
          redValue = decodeNumber(params[0], sliderRed.getMinimum(), sliderRed.getMaximum(), Integer.MIN_VALUE);
          if (redValue == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 1) { // set green value
          greenValue = decodeNumber(params[1], sliderGreen.getMinimum(), sliderGreen.getMaximum(), Integer.MIN_VALUE);
          if (greenValue == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 2) { // set blue value
          blueValue = decodeNumber(params[2], sliderBlue.getMinimum(), sliderBlue.getMaximum(), Integer.MIN_VALUE);
          if (blueValue == Integer.MIN_VALUE) {
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
        if (redValue != Integer.MIN_VALUE) {
          sliderRed.setValue(redValue);
        }
        if (greenValue != Integer.MIN_VALUE) {
          sliderGreen.setValue(greenValue);
        }
        if (blueValue != Integer.MIN_VALUE) {
          sliderBlue.setValue(blueValue);
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
  protected JPanel loadControls() {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l1 = new JLabel("Exclude colors:");
    pExcludeColors = new BamFilterBaseColor.ExcludeColorsPanel(
        getConverter().getPaletteDialog().getPalette(getConverter().getPaletteDialog().getPaletteType()));
    pExcludeColors.addChangeListener(this);
    bpwExclude = new ButtonPopupWindow("Palette", Icons.ICON_ARROW_DOWN_15.getIcon(), pExcludeColors);
    bpwExclude.setIconTextGap(8);
    bpwExclude.addActionListener(this);
    bpwExclude.setEnabled(getConverter().isBamV1Selected());
    JPanel pExclude = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    pExclude.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    pExclude.add(bpwExclude, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    pExclude.add(new JPanel(), c);

    JLabel lr = new JLabel("Red:");
    JLabel lg = new JLabel("Green:");
    JLabel lb = new JLabel("Blue:");
    sliderRed = new JSlider(SwingConstants.HORIZONTAL, -255, 255, 0);
    sliderRed.addChangeListener(this);
    sliderGreen = new JSlider(SwingConstants.HORIZONTAL, -255, 255, 0);
    sliderGreen.addChangeListener(this);
    sliderBlue = new JSlider(SwingConstants.HORIZONTAL, -255, 255, 0);
    sliderBlue.addChangeListener(this);
    spinnerRed = new JSpinner(
        new SpinnerNumberModel(sliderRed.getValue(), sliderRed.getMinimum(), sliderRed.getMaximum(), 1));
    spinnerRed.addChangeListener(this);
    spinnerGreen = new JSpinner(
        new SpinnerNumberModel(sliderGreen.getValue(), sliderGreen.getMinimum(), sliderGreen.getMaximum(), 1));
    spinnerGreen.addChangeListener(this);
    spinnerBlue = new JSpinner(
        new SpinnerNumberModel(sliderBlue.getValue(), sliderBlue.getMinimum(), sliderBlue.getMaximum(), 1));
    spinnerBlue.addChangeListener(this);

    JPanel p = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    p.add(lr, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 4, 0, 0), 0, 0);
    p.add(sliderRed, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 4, 0, 0), 0, 0);
    p.add(spinnerRed, c);

    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    p.add(lg, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 0, 0), 0, 0);
    p.add(sliderGreen, c);
    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 0, 0), 0, 0);
    p.add(spinnerGreen, c);

    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    p.add(lb, c);
    ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 0, 0), 0, 0);
    p.add(sliderBlue, c);
    ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 4, 0, 0), 0, 0);
    p.add(spinnerBlue, c);
    ViewerUtil.setGBC(c, 0, 3, 3, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 0, 0, 0), 0, 0);
    p.add(pExclude, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p, c);

    return panel;
  }

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == pExcludeColors) {
      fireChangeListener();
    } else if (event.getSource() == sliderRed) {
      spinnerRed.setValue(sliderRed.getValue());
      if (!sliderRed.getModel().getValueIsAdjusting()) {
        fireChangeListener();
      }
    } else if (event.getSource() == sliderGreen) {
      spinnerGreen.setValue(sliderGreen.getValue());
      if (!sliderGreen.getModel().getValueIsAdjusting()) {
        fireChangeListener();
      }
    } else if (event.getSource() == sliderBlue) {
      spinnerBlue.setValue(sliderBlue.getValue());
      if (!sliderBlue.getModel().getValueIsAdjusting()) {
        fireChangeListener();
      }
    } else if (event.getSource() == spinnerRed) {
      sliderRed.setValue(((Integer) spinnerRed.getValue()));
    } else if (event.getSource() == spinnerGreen) {
      sliderGreen.setValue(((Integer) spinnerGreen.getValue()));
    } else if (event.getSource() == spinnerBlue) {
      sliderBlue.setValue(((Integer) spinnerBlue.getValue()));
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == bpwExclude) {
      pExcludeColors.updatePalette(
          getConverter().getPaletteDialog().getPalette(getConverter().getPaletteDialog().getPaletteType()));
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  private BufferedImage applyEffect(BufferedImage srcImage) {
    if (srcImage != null) {
      int[] buffer;
      IndexColorModel cm = null;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        // paletted image
        cm = (IndexColorModel) srcImage.getColorModel();
        buffer = new int[1 << cm.getPixelSize()];
        cm.getRGBs(buffer);
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
        buffer = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();
      } else {
        buffer = new int[0];
      }

      // red/gree/blue in range [-255, 255]
      float red = ((Integer) spinnerRed.getValue()).floatValue() / 255.0f;
      float green = ((Integer) spinnerGreen.getValue()).floatValue() / 255.0f;
      float blue = ((Integer) spinnerBlue.getValue()).floatValue() / 255.0f;

      for (int i = 0; i < buffer.length; i++) {
        if ((cm == null || !pExcludeColors.isSelectedIndex(i)) && (buffer[i] & 0xff000000) != 0) {
          // extracting color channels
          float fa = ((buffer[i] >>> 24) & 0xff) / 255.0f;
          float fr = (((buffer[i] >>> 16) & 0xff) / 255.0f) / fa;
          float fg = (((buffer[i] >>> 8) & 0xff) / 255.0f) / fa;
          float fb = ((buffer[i] & 0xff) / 255.0f) / fa;

          // applying color balance
          fr += (red / fa);
          fg += (green / fa);
          fb += (blue / fa);

          int ir = (int) ((fr * fa) * 255.0f);
          if (ir < 0) {
            ir = 0;
          } else if (ir > 255) {
            ir = 255;
          }
          int ig = (int) ((fg * fa) * 255.0f);
          if (ig < 0) {
            ig = 0;
          } else if (ig > 255) {
            ig = 255;
          }
          int ib = (int) ((fb * fa) * 255.0f);
          if (ib < 0) {
            ib = 0;
          } else if (ib > 255) {
            ib = 255;
          }
          buffer[i] = (buffer[i] & 0xff000000) | (ir << 16) | (ig << 8) | ib;
        }
      }

      if (cm != null) {
        // recreating paletted image
        IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), buffer.length, buffer, 0, cm.hasAlpha(),
            cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        byte[] srcPixels = ((DataBufferByte) srcImage.getRaster().getDataBuffer()).getData();
        byte[] dstPixels = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
        srcImage = dstImage;
        srcPixels = null;
        dstPixels = null;
      }
    }

    return srcImage;
  }
}
