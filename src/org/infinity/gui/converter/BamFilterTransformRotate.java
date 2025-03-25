// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.util.HashMap;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.Misc;

/**
 * Transform filter: rotates each frame by a specified amount.
 */
public class BamFilterTransformRotate extends BamFilterBaseTransform implements ActionListener, ChangeListener {
  private static final String FILTER_NAME = "Rotate BAM frames";
  private static final String FILTER_DESC = "This filter allows you to rotate BAM frames by arbitrary angles.";

  private JRadioButton rbCW;
  private JRadioButton rbCCW;
  private JSpinner spinnerAngle;
  private JCheckBox cbAdjustCenter;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterTransformRotate(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public PseudoBamFrameEntry process(PseudoBamFrameEntry entry) throws Exception {
    return applyEffect(entry);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(int frameIndex, PseudoBamFrameEntry entry) {
    return applyEffect(entry);
  }

  @Override
  public String getConfiguration() {
    return String.valueOf(rbCW.isSelected() ? 0 : 1) + ';' +
        spinnerAngle.getValue() + ';' +
        cbAdjustCenter.isSelected();
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        final String[] params = config.split(";");
        int orientation = -1;
        double angle = 0.0;
        boolean adjust = true;

        if (params.length > 0) {
          orientation = Misc.toNumber(params[0], -1);
          if (orientation < 0 || orientation > 1) {
            return false;
          }
        }
        if (params.length > 1) {
          if (params[1].indexOf('.') >= 0) {
            // arbitrary angle
            angle = Misc.toDouble(params[1], Double.MIN_VALUE);
            if (angle == Double.MIN_VALUE) {
              return false;
            }
          } else {
            // deprecated angle index
            final int index = Misc.toNumber(params[1], -1);
            if (index < 0 || index >= 3) {
              return false;
            }
            angle = (index + 1) * 90.0;
          }
          angle = Math.min(Math.max(angle, -360.0), 360.0);
        }
        if (params.length > 2) {
          adjust = Misc.toBoolean(params[2], true);
        }

        if (orientation >= 0) {
          if (orientation == 0) {
            rbCW.setSelected(true);
          } else {
            rbCCW.setSelected(true);
          }
        }
        spinnerAngle.setValue(angle);
        cbAdjustCenter.setSelected(adjust);
      }
      return true;
    }
    return false;
  }

  @Override
  protected JPanel loadControls() {
    final GridBagConstraints c = new GridBagConstraints();

    final JLabel labelOrientation = new JLabel("Orientation:");
    final JLabel lableAngle = new JLabel("Angle:");
    final JLabel lableDegrees = new JLabel("degrees");

    final ButtonGroup bg = new ButtonGroup();
    rbCW = new JRadioButton("Clockwise", true);
    rbCW.addActionListener(this);
    rbCCW = new JRadioButton("Counter clockwise");
    rbCCW.addActionListener(this);
    bg.add(rbCW);
    bg.add(rbCCW);

    spinnerAngle = new JSpinner(new SpinnerNumberModel(0.0, -360.0, 360.0, 5.0));
    spinnerAngle.addChangeListener(this);

    cbAdjustCenter = new JCheckBox("Adjust center position", true);
    cbAdjustCenter.addActionListener(this);

    final JPanel panelOrientation = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panelOrientation.add(rbCW, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    panelOrientation.add(rbCCW, c);

    final JPanel panelAngle = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panelAngle.add(spinnerAngle, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    panelAngle.add(lableDegrees, c);

    final JPanel subPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    subPanel.add(labelOrientation, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    subPanel.add(panelOrientation, c);

    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    subPanel.add(lableAngle, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 8, 0, 0), 0, 0);
    subPanel.add(panelAngle, c);

    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    subPanel.add(new JPanel(), c);
    ViewerUtil.setGBC(c, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(12, 4, 0, 0), 0, 0);
    subPanel.add(cbAdjustCenter, c);

    final JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(subPanel, c);

    return panel;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == rbCW || event.getSource() == rbCCW || event.getSource() == cbAdjustCenter) {
      fireChangeListener();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == spinnerAngle) {
      fireChangeListener();
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  /** Returns whether rotation is set to clockwise direction. */
  private boolean isClockwise() {
    if (rbCW != null) {
      return rbCW.isSelected();
    }
    return true;
  }

  /** Returns the current angle value, in degrees. */
  private double getAngle() {
    if (spinnerAngle != null) {
      return ((SpinnerNumberModel)spinnerAngle.getModel()).getNumber().doubleValue();
    }
    return 0.0;
  }

  /** Returns whether the center position should be rotated as well. */
  private boolean isAdjustCenter() {
    if (cbAdjustCenter != null) {
      return cbAdjustCenter.isSelected();
    }
    return true;
  }

  /** Returns an angle value that is always clockwise and within range [0, 360). */
  private double getNormalizedAngle(double angleInDeg, boolean clockwise) {
    if (!clockwise) {
      angleInDeg = -angleInDeg;
    }

    angleInDeg = angleInDeg % 360.0;

    if (angleInDeg < 0) {
      angleInDeg = 360.0 + angleInDeg;
    }

    return angleInDeg;
  }

  private PseudoBamFrameEntry applyEffect(PseudoBamFrameEntry entry) {
    if (entry == null || entry.getFrame() == null) {
      return entry;
    }

    final double angle = Math.toRadians(getNormalizedAngle(getAngle(), isClockwise()));
    if (angle == 0.0) {
      // no rotation needed
      return entry;
    }

    final int interpolationType = getConverter().isBamV1Selected() ? AffineTransformOp.TYPE_NEAREST_NEIGHBOR
        : AffineTransformOp.TYPE_BICUBIC;

    final BufferedImage inImage = entry.getFrame();
    final BufferedImage rotatedImage = rotateImage(inImage, angle, interpolationType);
    final BufferedImage outImage = rotatedImage; // trimImage(rotatedImage);
    entry.setFrame(outImage);

    final int cx = entry.getCenterX() + (outImage.getWidth() - inImage.getWidth()) / 2;
    final int cy = entry.getCenterY() + (outImage.getHeight() - inImage.getHeight()) / 2;
    if (isAdjustCenter()) {
      final Point2D point = rotatePoint(
          new Point2D.Double(outImage.getWidth() / 2.0, outImage.getHeight() / 2.0), angle,
          new Point2D.Double(cx, cy));
      entry.setCenterX((int)Math.round(point.getX()));
      entry.setCenterY((int)Math.round(point.getY()));
    } else {
      entry.setCenterX(cx);
      entry.setCenterY(cy);
    }

    return entry;
  }

  /**
   * Rotates the specified image clockwise by the specified angle around the image center.
   *
   * @param image             {@link BufferedImage} object of the source image.
   * @param angleInRads       Angle in radians.
   * @param interpolationType Interpolation type by one of the {@code TYPE_xxx} constants defined in
   *                            {@link AffineTransformOp}. <p><b>Note:</b> Type is limited to {@code TYPE_NEAREST_NEIGHBOR}
   *                            for indexed image types.</p>
   * @return Rotated image as new {@link BufferedImage} of the same type as the source image.
   */
  private static BufferedImage rotateImage(BufferedImage image, double angleInRads, int interpolationType) {
    if (image == null) {
      throw new NullPointerException("image is null");
    }

    if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
      interpolationType = AffineTransformOp.TYPE_NEAREST_NEIGHBOR;
    } else {
      interpolationType = Math.min(Math.max(interpolationType, AffineTransformOp.TYPE_NEAREST_NEIGHBOR),
          AffineTransformOp.TYPE_BICUBIC);
    }

    final Dimension dim = new Dimension(image.getWidth(), image.getHeight());

    final int extraSpace = (interpolationType == AffineTransformOp.TYPE_NEAREST_NEIGHBOR) ? 0 : 2;
    final Dimension newDim = calculateImageSize(dim, angleInRads, extraSpace);

    final BufferedImage workingImage = new BufferedImage(newDim.width, newDim.height, BufferedImage.TYPE_INT_ARGB);

    final AffineTransform transform = new AffineTransform();
    transform.rotate(angleInRads, newDim.width / 2.0, newDim.height / 2.0);
    transform.translate((newDim.width - dim.width) / 2.0, (newDim.height - dim.height) / 2.0);

    final Graphics2D g = workingImage.createGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      final BufferedImageOp op = new AffineTransformOp(transform, interpolationType);
      g.drawImage(image, op, 0, 0);
    } finally {
      g.dispose();
    }

    final BufferedImage outImage;
    if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
      outImage = remapImage(workingImage, (IndexColorModel)image.getColorModel());
    } else {
      outImage = workingImage;
    }

    return outImage;
  }

  /** Remaps {@code image} to a paletted image using {@code colors}. */
  private static BufferedImage remapImage(BufferedImage image, IndexColorModel colors) {
    if (image == null) {
      throw new NullPointerException("image is null");
    }
    if (colors == null) {
      throw new NullPointerException("color map is null");
    }

    if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
      // no conversion needed
      return image;
    }

    final BufferedImage outImage = new BufferedImage(image.getWidth(), image.getHeight(),
        BufferedImage.TYPE_BYTE_INDEXED, colors);

    final int[] palette = new int[colors.getMapSize()];
    colors.getRGBs(palette);

    // speed up conversion by caching color mappings
    final HashMap<Integer, Integer> colorMap = new HashMap<>(350);

    final int[] srcPixels = ((DataBufferInt)image.getData().getDataBuffer()).getData();
    final Raster dstRaster = outImage.getData();
    final byte[] dstPixels = ((DataBufferByte)dstRaster.getDataBuffer()).getData();
    for (int i = 0; i < srcPixels.length; i++) {
      final Integer index = colorMap.computeIfAbsent(srcPixels[i], color -> {
        final int result = ColorConvert.getNearestColor(color, palette, 1.0, ColorConvert.COLOR_DISTANCE_ARGB);
        return (result >= 0) ? result : null;
      });
      if (index != null) {
        dstPixels[i] = index.byteValue();
      }
    }
    outImage.setData(dstRaster);

    return outImage;
  }

  /**
   * Calculates the dimension of the rotated image. {@code angleInRads} specifies the clockwise angle in
   * radians. {@code space} specifies an extra number of pixels to add to each border of the image.
   */
  private static Dimension calculateImageSize(Dimension dimension, double angleInRads, int space) {
    if (dimension == null) {
      throw new NullPointerException("Argument is null");
    }

    final int newWidth = (int)(Math.abs(dimension.getWidth() * Math.cos(angleInRads))
        + Math.abs(dimension.getHeight() * Math.sin(angleInRads))) + (space * 2);
    final int newHeight = (int)(Math.abs(dimension.getHeight() * Math.cos(angleInRads))
        + Math.abs(dimension.getWidth() * Math.sin(angleInRads))) + (space * 2);

    // always return a dimension of even widths and heights to reduce rounding errors
    return new Dimension((newWidth + 1) & ~1, (newHeight + 1) & ~1);
  }

  /** Rotates Point {@code p} around {@code center} clockwise by {@code angleInRads}. */
  private static Point2D rotatePoint(Point2D center, double angleInRads, Point2D p) {
    final Point2D.Double retVal = new Point2D.Double();
    final double s = Math.sin(angleInRads);
    final double c = Math.cos(angleInRads);

    retVal.x = p.getX() - center.getX();
    retVal.y = p.getY() - center.getY();

    final double newX = retVal.x * c - retVal.y * s;
    final double newY = retVal.x * s + retVal.y * c;

    retVal.x = newX + center.getX();
    retVal.y = newY + center.getY();

    return retVal;
  }
}
