// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * Color filter: perform Gaussian blur on each pixel (BAM V2 only).
 */
public class BamFilterColorBlur extends BamFilterBaseColor implements ChangeListener {
  private static final String FILTER_NAME = "Gaussian Blur (BAM v2 only)";
  private static final String FILTER_DESC = "This filter performs a Gaussian blur on the pixels of all frames.\n"
      + "Note: Supports pvrz-based BAM (v2) only.";

  // Calculates weights for the Gaussian blur kernel
  // Expected parameters: blur radius, x and y coordinates within the kernel
  private static final KernelOperation KERNEL_FUNC = (radius, x, y) -> {
    final double sigma = Math.max(radius / 2.0, 0.1);
    final double weightY = KernelOperation.getWeight(radius, y);
    final double weightX = KernelOperation.getWeight(radius, x);

    // Gaussian function: G(x,y) = 1 / (2 * PI * sigma^2) * e^(-(x^2 + y^2) / (2 * sigma^2))
    final double expNum = -(x * x + y * y);
    final double expDenum = 2.0 * sigma * sigma;
    final double exp = Math.exp(expNum / expDenum);
    return (exp / (2.0 * Math.PI * sigma * sigma) * weightY * weightX);
  };

  private JLabel labelRadius;
  private JLabel labelSuffix;
  private JSpinner spinnerRadius;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterColorBlur(ConvertToBam parent) {
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
    final boolean enable = !getConverter().isBamV1Selected();
    labelRadius.setEnabled(enable);
    labelSuffix.setEnabled(enable);
    spinnerRadius.setEnabled(enable);
  }

  @Override
  public String getConfiguration() {
    return Double.toString(((SpinnerNumberModel)spinnerRadius.getModel()).getNumber().doubleValue());
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      final double min = ((Number)((SpinnerNumberModel)spinnerRadius.getModel()).getMinimum()).doubleValue();
      final double max = ((Number)((SpinnerNumberModel)spinnerRadius.getModel()).getMaximum()).doubleValue();
      final double factor = decodeDouble(config, min, max, Double.MIN_VALUE);
      if (factor != Double.MIN_VALUE) {
        spinnerRadius.setValue(factor);
        return true;
      }
    }
    return false;
  }

  @Override
  protected JPanel loadControls() {
    GridBagConstraints c = new GridBagConstraints();

    labelRadius = new JLabel("Radius:");
    labelRadius.setEnabled(false);

    labelSuffix = new JLabel("pixels");
    labelSuffix.setEnabled(false);

    spinnerRadius = new JSpinner(new SpinnerNumberModel(2.0, 0.0, 16.0, 0.1));
    spinnerRadius.addChangeListener(this);
    spinnerRadius.setEnabled(false);

    final JPanel subPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    subPanel.add(labelRadius, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    subPanel.add(spinnerRadius, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    subPanel.add(labelSuffix, c);

    final JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(subPanel, c);

    updateControls();

    return panel;
  }

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == spinnerRadius) {
      fireChangeListener();
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  /** Returns the user-defined blur radius. */
  private double getRadius() {
    if (spinnerRadius != null) {
      final SpinnerNumberModel model = (SpinnerNumberModel)spinnerRadius.getModel();
      return ((Number)model.getValue()).doubleValue();
    }
    return 0.0;
  }

  /** Performs the filter operation on the specified {@link BufferedImage} object. */
  private BufferedImage applyEffect(BufferedImage srcImage) {
    // Only available for truecolor images
    if (srcImage != null && srcImage.getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
      return applyGaussianBlur(srcImage, getRadius());
    }

    return srcImage;
  }

  /**
   * Performs the Gaussian blur operation on the specified image.
   *
   * @param image {@link BufferedImage} object of the source image.
   * @param radius The blur radius.
   * @return Blurred image as {@link BufferedImage} object.
   */
  private static BufferedImage applyGaussianBlur(BufferedImage image, double radius) {
    if (image == null || radius <= 0.0) {
      return image;
    }

    final int width = image.getWidth();
    final int height = image.getHeight();
    final BufferedImage outImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    // processing image pixels
    final GenericKernel kernel = new GenericKernel(KERNEL_FUNC, radius, true);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        final Color blurredColor = applyKernel(image, x, y, kernel);
        outImage.setRGB(x, y, blurredColor.getRGB());
      }
    }

    return outImage;
  }

  /** Performs the Gaussian blur operation for the pixel at the specified position. */
  private static Color applyKernel(BufferedImage image, int x, int y, GenericKernel kernel) {
    final int width = image.getWidth();
    final int height = image.getHeight();

    double redValue = 0.0;
    double greenValue = 0.0;
    double blueValue = 0.0;
    double alphaValue = 0.0;

    // iterating through the kernel
    for (int ky = -kernel.radiusSize, maxky = kernel.radiusSize; ky <= maxky; ky++) {
      for (int kx = -kernel.radiusSize, maxkx = kernel.radiusSize; kx <= maxkx; kx++) {
        final int px = Math.min(Math.max(x + kx, 0), width - 1);
        final int py = Math.min(Math.max(y + ky, 0), height - 1);
        final Color color = new Color(image.getRGB(px, py), true);
        final double weight = kernel.getKernelValue(kx, ky);

        redValue += color.getRed() * weight;
        greenValue += color.getGreen() * weight;
        blueValue += color.getBlue() * weight;
        alphaValue += color.getAlpha() * weight;
      }
    }

    int r = (int)Math.min(Math.max(redValue, 0), 255);
    int g = (int)Math.min(Math.max(greenValue, 0), 255);
    int b = (int)Math.min(Math.max(blueValue, 0), 255);
    int a = (int)Math.min(Math.max(alphaValue, 0), 255);

    return new Color(r, g, b, a);
  }

  // -------------------------- INNER CLASSES --------------------------

  /**
   * Represents a function that calculates the weight values for individual kernel matrix positions.
   */
  @FunctionalInterface
  private static interface KernelOperation {
    /**
     * Calculates a weight value for a specific kernel position.
     *
     * @param radius Radius of the kernel.
     * @param x      X coordinate of the position in the kernel matrix ({@code -radius}..{@code radius}).
     * @param y      Y coordinate of the position in the kernel matrix ({@code -radius}..{@code radius}).
     * @return A weight value that is applied to the specified kernel matrix position.
     */
    double calculate(double radius, int x, int y);

    /**
     * Returns the weight of the kernel value at the specified radius.
     *
     * @param radius   the kernel radius.
     * @param distance distance from the kernel center to check.
     * @return Weight of the kernel value at the specified radius ({@code 0.0} < result <= {@code 1.0}).
     */
    static double getWeight(double radius, int distance) {
      final int radiusSize = (int)Math.ceil(radius);
      if (distance > -radiusSize && distance < radiusSize) {
        return 1.0;
      } else if (distance == -radiusSize || distance == radiusSize) {
        final double radiusFract = radius % 1.0;
        return (radiusFract != 0.0) ? radiusFract : 1.0;
      } else {
        return 0.0;
      }
    }
  }

  /**
   * Implementation of a general-purpose kernel matrix that can be used for filter operations of graphics data.
   */
  private static class GenericKernel {
    private final KernelOperation kernelFunc;

    /** Radius of the kernel. */
    public final double radius;
    /** Integral radius size of the kernel that covers all of {@code radius}. */
    public final int radiusSize;
    /** Kernel width and height. */
    public final int kernelSize;
    /** Indicates whether to perform normalization to ensure that the sum of kernel values is 1. */
    public final boolean normalize;

    /** The kernel matrix. */
    public final double[][] kernel;

    /**
     * Generates a new kernel matrix according to the specified parameters.
     *
     * @param kernelFunc Functional interface of type {@link KernelOperation} that performs the actual kernel matrix
     *                     calculations.
     * @param radius     Radius of the kernel.
     * @param normalize  Specificies whether the kernel values should be normalized to ensure that the sum of kernel
     *                     values is 1.
     */
    public GenericKernel(KernelOperation kernelFunc, double radius, boolean normalize) {
      this.kernelFunc = Objects.requireNonNull(kernelFunc);
      this.radius = Math.max(0.0, radius);
      this.normalize = normalize;
      this.radiusSize = (int)Math.ceil(radius);
      this.kernelSize = (2 * radiusSize) + 1;
      this.kernel = new double[this.kernelSize][this.kernelSize];
      generateKernel();
    }

    /**
     * Returns the blur weight value from the specified kernel position. {@code x} and {@code y} range from
     * {@code -radiusSize} to {@code radiusSize}, inclusive. {@link IndexOutOfBoundsException} is thrown if the
     * coordinates are out of bounds.
     */
    public double getKernelValue(int x, int y) throws IndexOutOfBoundsException {
      if (x < -radiusSize || x > radiusSize) {
        throw new IndexOutOfBoundsException("x is out of bounds: " + x);
      }
      if (y < -radiusSize || y > radiusSize) {
        throw new IndexOutOfBoundsException("y is out of bounds: " + y);
      }

      return kernel[y + radiusSize][x + radiusSize];
    }

    /** Generates the kernel values. */
    private void generateKernel() {
      double sum = 0.0;
      for (int y = -radiusSize; y <= radiusSize; y++) {
        for (int x = -radiusSize; x <= radiusSize; x++) {
          final double weight = kernelFunc.calculate(radius, x, y);
          kernel[y + radiusSize][x + radiusSize] = weight;
          sum += weight;
        }
      }

      if (normalize && sum != 0.0) {
        for (int y = 0; y < kernelSize; y++) {
          for (int x = 0; x < kernelSize; x++) {
            kernel[y][x] /= sum;
          }
        }
      }
    }
  }
}
