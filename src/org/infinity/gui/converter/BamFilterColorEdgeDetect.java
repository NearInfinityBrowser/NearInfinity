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
import java.awt.image.IndexColorModel;
import java.util.Arrays;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.Misc;

/**
 * Color filter: perform Edge detection with the Sobel algorithm.
 */
public class BamFilterColorEdgeDetect extends BamFilterBaseColor implements ActionListener, ChangeListener {
  private static final String FILTER_NAME = "Edge Detection";
  private static final String FILTER_DESC = "This filter performs edge detection on the frames.";

  private static final double[][] KERNEL_X = {
      { -1.0, 0.0, 1.0 },
      { -2.0, 0.0, 2.0 },
      { -1.0, 0.0, 1.0 }
  };

  private static final double[][] KERNEL_Y = {
      { -1.0, -2.0, -1.0 },
      {  0.0,  0.0,  0.0 },
      {  1.0,  2.0,  1.0 }
  };

  private JCheckBox checkTransparency;
  private JLabel labelThreshold;
  private JSpinner spinnerThreshold;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterColorEdgeDetect(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public BufferedImage process(BufferedImage frame) throws Exception {
    return applyEffect(frame);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(int frameIndex, PseudoBamFrameEntry frame) {
    if (frame != null) {
      frame.setFrame(applyEffect(frame.getFrame()));
    }
    return frame;
  }

  @Override
  public void updateControls() {
    final boolean enable = getConverter().isBamV1Selected() && checkTransparency.isSelected();
    labelThreshold.setEnabled(enable);
    spinnerThreshold.setEnabled(enable);
  }

  @Override
  public String getConfiguration() {
    return String.valueOf(checkTransparency.isSelected()) + ';' + spinnerThreshold.getValue();
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        boolean transparent = false;
        int threshold = 0;

        if (params.length > 0) {
          transparent = Misc.toBoolean(params[0], false);
        }

        if (params.length > 1) {
          final int min = ((Number)((SpinnerNumberModel)spinnerThreshold.getModel()).getMinimum()).intValue();
          final int max = ((Number)((SpinnerNumberModel)spinnerThreshold.getModel()).getMaximum()).intValue();
          threshold = decodeNumber(params[1], min, max, Integer.MIN_VALUE);
          if (threshold == Integer.MIN_VALUE) {
            return false;
          }
        }

        checkTransparency.setSelected(transparent);
        spinnerThreshold.setValue(threshold);
        return true;
      }

    }
    return false;
  }

  @Override
  protected JPanel loadControls() {
    GridBagConstraints c = new GridBagConstraints();

    checkTransparency = new JCheckBox("Transparent background");
    checkTransparency.addActionListener(this);

    final String tooltip = "Threshold for transparent pixels (BAM V1 only).";
    labelThreshold = new JLabel("Threshold:");
    labelThreshold.setToolTipText(tooltip);
    labelThreshold.setEnabled(false);

    spinnerThreshold = new JSpinner(new SpinnerNumberModel(0, 0, 254, 1));
    spinnerThreshold.setToolTipText(tooltip);
    spinnerThreshold.addChangeListener(this);
    spinnerThreshold.setEnabled(false);

    final JPanel subPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    subPanel.add(checkTransparency, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 0, 0, 0), 0, 0);
    subPanel.add(labelThreshold, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 4, 0, 0), 0, 0);
    subPanel.add(spinnerThreshold, c);

    final JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(subPanel, c);

    updateControls();

    return panel;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == checkTransparency) {
      updateControls();
      fireChangeListener();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == spinnerThreshold) {
      fireChangeListener();
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  /** Returns whether background should be transparent. */
  private boolean isTransparentBackground() {
    if (checkTransparency != null) {
      return checkTransparency.isSelected();
    }
    return false;
  }

  /** Returns the transparency threshold for BAM V1 frames. */
  private int getThreshold() {
    if (spinnerThreshold != null) {
      final SpinnerNumberModel model = (SpinnerNumberModel)spinnerThreshold.getModel();
      return ((Number)model.getValue()).intValue();
    }
    return 0;

  }

  /** Performs the filter operation on the specified {@link BufferedImage} object. */
  private BufferedImage applyEffect(BufferedImage srcImage) {
    if (srcImage != null) {
      return applyEdgeDetection(srcImage, isTransparentBackground(), getThreshold());
    }

    return srcImage;
  }

  /**
   * Performs edge detection on the specified image.
   *
   * @param image                 {@link BufferedImage} object of the source image.
   * @param transparentBackground Specifies whether the image background should be transparent.
   * @param threshold             Transparency threshold for BAM V1 frames if {@code transparentBackground} is
   *                                {@code true}.
   * @return Resulting image as {@link BufferedImage} object.
   */
  private static BufferedImage applyEdgeDetection(BufferedImage image, boolean transparentBackground, int threshold) {
    if (image == null) {
      return image;
    }

    final boolean isPalette = (image.getType() == BufferedImage.TYPE_BYTE_INDEXED);
    final int width = image.getWidth();
    final int height = image.getHeight();
    final BufferedImage outImage;
    if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
      final byte[] reds = new byte[256];
      for (int i = 2; i < reds.length; i++) {
        reds[i] = (byte)i;
      }
      final byte[] greens = Arrays.copyOf(reds, reds.length);
      greens[0] = (byte)255;
      final byte[] blues = Arrays.copyOf(reds, reds.length);
      final IndexColorModel colorModel = new IndexColorModel(8, 256, reds, greens, blues, 0);
      outImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, colorModel);
    } else {
      outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    // processing image pixels
    final int[] buffer = new int[width * height];
    int maxValue = 0; // collects the max. color intensity for the normalization step
    for (int y = 1; y < height - 1; y++) {
      for (int x = 1; x < width - 1; x++) {
        final int value = applyKernel(image, x, y);
        buffer[y * width + x] = value;
        maxValue = Math.max(maxValue, value);
      }
    }

    // normalizing pixel data
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int value = buffer[y * width + x];
        if (maxValue > 0) {
          value = value * 255 / maxValue;
        }
        value = Math.max(0, Math.min(255, value));
        int color = 0xff000000 | (value << 16) | (value << 8) | value;
        if (transparentBackground) {
          if (isPalette) {
            if (value <= threshold) {
              color = 0x0000ff00; // magic "green"
            }
          } else {
            color = (value << 24) | (value << 16) | (value << 8) | value;
          }
        }
        outImage.setRGB(x, y, color);
      }
    }

    return outImage;
  }

  /** Performs edge detection for the pixel at the specified image position. */
  private static int applyKernel(BufferedImage image, int x, int y) {
    int gx = 0;
    int gy = 0;

    for (int i = -1; i <= 1; i++) {
      for (int j = -1; j <= 1; j++) {
        final int gray = toGrayscale(image.getRGB(x + j, y + i));
        gx += KERNEL_X[i + 1][j + 1] * gray;
        gy += KERNEL_Y[i + 1][j + 1] * gray;
      }
    }

    int g = (int) Math.sqrt(gx * gx + gy * gy);
    return g;
  }

  /** Returns the gray intensity in range [0, 255] of the specified color. */
  private static int toGrayscale(int color) {
    final int r = (color >> 16) & 0xff;
    final int g = (color >> 8) & 0xff;
    final int b = color & 0xff;
    return (int)Math.round(0.2989 * r + 0.5870 * g + 0.1140 * b);
  }
}
