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
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.FastMath;
import org.infinity.util.Misc;

/**
 * Transform filter: adjust the size of the BAM frames.
 */
public class BamFilterTransformResize extends BamFilterBaseTransform implements ActionListener, ChangeListener {
  private static final String FILTER_NAME = "Resize BAM frames";
  private static final String FILTER_DESC = "This filter allows you to adjust the size of each BAM frame.";

  private static final int LANCZOS_KERNEL_SIZE = 3;

  private enum ScalingType {
    NEAREST("Nearest Neighbor"),
    BILINEAR("Bilinear"),
    BICUBIC("Bicubic"),
    SCALE_X("Scale2x/3x/4x"),
    LANCZOS("Lanczos"),
    ;

    private final String label;

    ScalingType(String label) {
      this.label = label;
    }

    public String getLabel() {
      return label;
    }

    @Override
    public String toString() {
      return getLabel();
    }
  }

  private JComboBox<ScalingType> cbType;
  private JCheckBox cbAdjustCenter;
  private JRadioButton rbScaleBoth;
  private JRadioButton rbScaleIndividually;
  private JSpinner spinnerFactor;
  private JSpinner spinnerFactorX;
  private JSpinner spinnerFactorY;
  private JLabel lFactor;
  private JLabel lFactorX;
  private JLabel lFactorY;
  private JTextArea taInfo;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterTransformResize(ConvertToBam parent) {
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
  public void updateControls() {
    updateStatus();
  }

  @Override
  public String getConfiguration() {
    return String.valueOf(cbType.getSelectedIndex()) + ';' +
        (rbScaleBoth.isSelected() ? 0 : 1) + ';' +
        ((SpinnerNumberModel) spinnerFactor.getModel()).getNumber().doubleValue() + ';' +
        ((SpinnerNumberModel) spinnerFactorX.getModel()).getNumber().doubleValue() + ';' +
        ((SpinnerNumberModel) spinnerFactorY.getModel()).getNumber().doubleValue() + ';' +
        cbAdjustCenter.isSelected();
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        final String[] params = config.split(";");
        int type = -1;
        double factor = Double.MIN_VALUE;
        double factorX = Double.MIN_VALUE;
        double factorY = Double.MIN_VALUE;
        boolean uniformSelected = true;
        boolean adjust = true;

        // loading legacy options
        if (params.length > 0) {
          type = Misc.toNumber(params[0], -1);
          if (type < 0 || type >= cbType.getModel().getSize()) {
            return false;
          }
        }
        if (params.length > 1) {
          final int index = (params.length >= 6) ? 2 : 1;
          final double min = ((Number) ((SpinnerNumberModel) spinnerFactor.getModel()).getMinimum()).doubleValue();
          final double max = ((Number) ((SpinnerNumberModel) spinnerFactor.getModel()).getMaximum()).doubleValue();
          factor = decodeDouble(params[index], min, max, Double.MIN_VALUE);
          if (factor == Double.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 2) {
          final int index = (params.length >= 6) ? 5 : 2;
          adjust = Misc.toBoolean(params[index], true);
        }

        // loading revised options
        if (params.length >= 6) {
          uniformSelected = (Misc.toNumber(params[1], 0) == 0);

          double min = ((Number) ((SpinnerNumberModel) spinnerFactor.getModel()).getMinimum()).doubleValue();
          double max = ((Number) ((SpinnerNumberModel) spinnerFactor.getModel()).getMaximum()).doubleValue();
          factorX = decodeDouble(params[3], min, max, Double.MIN_VALUE);
          if (factorX == Double.MIN_VALUE) {
            return false;
          }

          min = ((Number) ((SpinnerNumberModel) spinnerFactor.getModel()).getMinimum()).doubleValue();
          max = ((Number) ((SpinnerNumberModel) spinnerFactor.getModel()).getMaximum()).doubleValue();
          factorY = decodeDouble(params[4], min, max, Double.MIN_VALUE);
          if (factorY == Double.MIN_VALUE) {
            return false;
          }
        }

        if (type >= 0) {
          cbType.setSelectedIndex(type);
        }
        if (uniformSelected) {
          rbScaleBoth.setSelected(true);
          actionPerformed(new ActionEvent(rbScaleBoth, ActionEvent.ACTION_PERFORMED, null));
        } else {
          rbScaleIndividually.setSelected(true);
          actionPerformed(new ActionEvent(rbScaleIndividually, ActionEvent.ACTION_PERFORMED, null));
        }
        if (factor != Double.MIN_VALUE) {
          spinnerFactor.setValue(factor);
        }
        if (factorX != Double.MIN_VALUE) {
          spinnerFactorX.setValue(factorX);
        }
        if (factorY != Double.MIN_VALUE) {
          spinnerFactorY.setValue(factorY);
        }
        cbAdjustCenter.setSelected(adjust);
      }
      return true;
    }
    return false;
  }

  @Override
  protected JPanel loadControls() {
    /*
     * Possible scaling algorithms:
     * - nearest neighbor (BamV1, BamV2) -> use Java's internal filters
     * - bilinear (BamV2) -> use Java's internal filters
     * - bicubic (BamV2) -> use Java's internal filters
     * - scale2x/scale3x (BamV1, BamV2) -> http://en.wikipedia.org/wiki/Image_scaling
     * - lanczos (BamV2) -> http://en.wikipedia.org/wiki/Lanczos_resampling
     * - [?] xBR (BamV1, BamV2) -> http://board.byuu.org/viewtopic.php?f=10&t=2248
     */
    final GridBagConstraints c = new GridBagConstraints();

    final JLabel labelType = new JLabel("Type:");
    lFactor = new JLabel("Factor:");
    lFactorX = new JLabel("Factor X:");
    lFactorX.setEnabled(false);
    lFactorY = new JLabel("Factor Y:");
    lFactorY.setEnabled(false);
    cbType = new JComboBox<>(ScalingType.values());
    cbType.addActionListener(this);
    rbScaleBoth = new JRadioButton("Scale uniformly");
    rbScaleIndividually = new JRadioButton("Scale individually");
    final ButtonGroup bg = new ButtonGroup();
    bg.add(rbScaleBoth);
    bg.add(rbScaleIndividually);
    rbScaleBoth.setSelected(true);
    rbScaleBoth.addActionListener(this);
    rbScaleIndividually.addActionListener(this);
    spinnerFactor = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 10.0, 0.05));
    spinnerFactor.addChangeListener(this);
    spinnerFactorX = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 10.0, 0.05));
    spinnerFactorX.addChangeListener(this);
    spinnerFactorX.setEnabled(false);
    spinnerFactorY = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 10.0, 0.05));
    spinnerFactorY.addChangeListener(this);
    spinnerFactorY.setEnabled(false);
    taInfo = new JTextArea(2, 0);
    taInfo.setEditable(false);
    taInfo.setFont(UIManager.getFont("Label.font"));
    taInfo.setBackground(UIManager.getColor("Label.background"));
    taInfo.setSelectionColor(UIManager.getColor("Label.background"));
    taInfo.setSelectedTextColor(UIManager.getColor("Label.textColor"));
    taInfo.setWrapStyleWord(true);
    taInfo.setLineWrap(true);
    int w = (lFactorX.getPreferredSize().width + spinnerFactorX.getPreferredSize().width + 32) * 2;
    taInfo.setPreferredSize(
        new Dimension(Math.max(w, taInfo.getPreferredSize().width), taInfo.getPreferredSize().height));
    cbAdjustCenter = new JCheckBox("Adjust center position", true);
    cbAdjustCenter.addActionListener(this);

    final JPanel panelType = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panelType.add(labelType, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    panelType.add(cbType, c);
    ViewerUtil.setGBC(c, 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    panelType.add(taInfo, c);

    final JPanel panelFactorAll = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    panelFactorAll.add(rbScaleBoth, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 24, 0, 0), 0, 0);
    panelFactorAll.add(lFactor, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 0), 0, 0);
    panelFactorAll.add(spinnerFactor, c);

    final JPanel panelFactors = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    panelFactors.add(rbScaleIndividually, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 24, 0, 0), 0, 0);
    panelFactors.add(lFactorX, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 0), 0, 0);
    panelFactors.add(spinnerFactorX, c);
    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 8, 0, 0), 0, 0);
    panelFactors.add(lFactorY, c);
    ViewerUtil.setGBC(c, 3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 4, 0, 0), 0, 0);
    panelFactors.add(spinnerFactorY, c);

    final JPanel panelAdjustCenter = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 1, 4, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    panelAdjustCenter.add(cbAdjustCenter, c);

    final JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(panelType, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    panel.add(panelFactorAll, c);
    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(panelFactors, c);
    ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(12, 0, 0, 0), 0, 0);
    panel.add(panelAdjustCenter, c);

    updateStatus();

    return panel;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == cbType || event.getSource() == rbScaleBoth || event.getSource() == rbScaleIndividually) {
      updateStatus();
      fireChangeListener();
    } else if (event.getSource() == cbAdjustCenter) {
      fireChangeListener();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == spinnerFactor || event.getSource() == spinnerFactorX
        || event.getSource() == spinnerFactorY) {
      fireChangeListener();
    }
  }

  // --------------------- Begin Interface ChangeListener ---------------------

  // Updates controls depending on current scaling type
  private void updateStatus() {
    final String fmtSupport1 = "Supported target: %s";
    final String fmtSupport2 = "Supported targets: %s, %s";

    final ScalingType type = (ScalingType) cbType.getSelectedItem();
    final double factor = getFactor(spinnerFactor);
    final double factorX = getFactor(spinnerFactorX);
    final double factorY = getFactor(spinnerFactorY);

    final boolean uniformEnabled = rbScaleBoth.isSelected() && isTypeSupported(type);
    final boolean individualEnabled = rbScaleIndividually.isSelected() && isTypeSupported(type);
    switch (type) {
      case NEAREST:
        taInfo.setText(String.format(fmtSupport2, ConvertToBam.BAM_VERSION_ITEMS[ConvertToBam.VERSION_BAMV1],
            ConvertToBam.BAM_VERSION_ITEMS[ConvertToBam.VERSION_BAMV2]));
        setFactor(spinnerFactor, factor, 0.01, 10.0, 0.05);
        setFactor(spinnerFactorX, factorX, 0.01, 10.0, 0.05);
        setFactor(spinnerFactorY, factorY, 0.01, 10.0, 0.05);
        rbScaleIndividually.setEnabled(true);
        lFactor.setEnabled(uniformEnabled);
        lFactorX.setEnabled(individualEnabled);
        lFactorY.setEnabled(individualEnabled);
        spinnerFactor.setEnabled(uniformEnabled);
        spinnerFactorX.setEnabled(individualEnabled);
        spinnerFactorY.setEnabled(individualEnabled);
        break;
      case BILINEAR:
      case BICUBIC:
      case LANCZOS:
        taInfo.setText(String.format(fmtSupport1, ConvertToBam.BAM_VERSION_ITEMS[ConvertToBam.VERSION_BAMV2]));
        setFactor(spinnerFactor, factor, 0.01, 10.0, 0.05);
        setFactor(spinnerFactorX, factorX, 0.01, 10.0, 0.05);
        setFactor(spinnerFactorY, factorY, 0.01, 10.0, 0.05);
        rbScaleIndividually.setEnabled(true);
        lFactor.setEnabled(uniformEnabled);
        lFactorX.setEnabled(individualEnabled);
        lFactorY.setEnabled(individualEnabled);
        spinnerFactor.setEnabled(uniformEnabled);
        spinnerFactorX.setEnabled(individualEnabled);
        spinnerFactorY.setEnabled(individualEnabled);
        break;
      case SCALE_X:
        taInfo.setText(String.format(fmtSupport2, ConvertToBam.BAM_VERSION_ITEMS[ConvertToBam.VERSION_BAMV1],
            ConvertToBam.BAM_VERSION_ITEMS[ConvertToBam.VERSION_BAMV2]));
        setFactor(spinnerFactor, (int) factor, 2, 4, 1);
        if (!rbScaleBoth.isSelected()) {
          rbScaleBoth.setSelected(true);
          actionPerformed(new ActionEvent(rbScaleBoth, ActionEvent.ACTION_PERFORMED, null));
        }
        spinnerFactor.setEnabled(isTypeSupported(type));
        rbScaleIndividually.setEnabled(false);
        lFactorX.setEnabled(false);
        lFactorY.setEnabled(false);
        spinnerFactorX.setEnabled(false);
        spinnerFactorY.setEnabled(false);
        break;
      default:
        taInfo.setText("No information available");
        setFactor(spinnerFactor, factor, 0.01, 10.0, 0.05);
        setFactor(spinnerFactorX, factorX, 0.01, 10.0, 0.05);
        setFactor(spinnerFactorY, factorY, 0.01, 10.0, 0.05);
    }
  }

  private boolean isTypeSupported(ScalingType type) {
    switch (type) {
      case BILINEAR:
      case BICUBIC:
      case LANCZOS:
        return !getConverter().isBamV1Selected();
      default:
        return true;
    }
  }

  private void setFactor(JSpinner spinner, Number current, Number min, Number max, Number step) {
    if (spinner != null && spinner.getModel() instanceof SpinnerNumberModel) {
      final SpinnerNumberModel snm = (SpinnerNumberModel) spinner.getModel();
      final boolean isDouble = ((current instanceof Double) || (min instanceof Double) || (max instanceof Double)
          || (step instanceof Double));
      int curI = 0, minI = 0, maxI = 0, stepI = 0;
      double curD = 0, minD = 0, maxD = 0, stepD = 0;
      if (isDouble) {
        curD = current.doubleValue();
        minD = min.doubleValue();
        maxD = max.doubleValue();
        stepD = step.doubleValue();
      } else {
        curI = current.intValue();
        minI = min.intValue();
        maxI = max.intValue();
        stepI = step.intValue();
      }
      if (isDouble) {
        if (snm.getValue() instanceof Integer) {
          curD = Math.max(Math.min(curD, maxD), minD);
          spinner.setModel(new SpinnerNumberModel(curD, minD, maxD, stepD));
        } else {
          snm.setMinimum(minD);
          snm.setMaximum(maxD);
          snm.setValue(curD);
          snm.setStepSize(stepD);
        }
      } else {
        if (snm.getValue() instanceof Double) {
          curI = Math.max(Math.min(curI, maxI), minI);
          spinner.setModel(new SpinnerNumberModel(curI, minI, Math.max(maxI, 10), stepI));
          if (maxI < 10) {
            ((SpinnerNumberModel) spinner.getModel()).setMaximum(maxI);
          }
        } else {
          snm.setMinimum(minI);
          snm.setMaximum(maxI);
          snm.setValue(curI);
          snm.setStepSize(stepI);
        }
      }
    }
  }

  private double getFactor(JSpinner spinner) {
    if (spinner != null) {
      final SpinnerNumberModel snm = (SpinnerNumberModel) spinner.getModel();
      return ((Number) snm.getValue()).doubleValue();
    } else {
      return 1.0;
    }
  }

  private PseudoBamFrameEntry applyEffect(PseudoBamFrameEntry entry) {
    if (entry != null && entry.getFrame() != null) {
      final BufferedImage dstImage;
      final double factorX = getFactor(rbScaleBoth.isSelected() ? spinnerFactor : spinnerFactorX);
      final double factorY = getFactor(rbScaleBoth.isSelected() ? spinnerFactor : spinnerFactorY);
      final ScalingType type = (ScalingType) cbType.getSelectedItem();
      switch (type) {
        case NEAREST:
          dstImage = scaleNative(entry.getFrame(), factorX, factorY, AffineTransformOp.TYPE_NEAREST_NEIGHBOR, true);
          break;
        case BILINEAR:
          dstImage = scaleNative(entry.getFrame(), factorX, factorY, AffineTransformOp.TYPE_BILINEAR, false);
          break;
        case BICUBIC:
          dstImage = scaleNative(entry.getFrame(), factorX, factorY, AffineTransformOp.TYPE_BICUBIC, false);
          break;
        case SCALE_X:
          dstImage = scaleScaleX(entry.getFrame(), (int) factorX);
          break;
        case LANCZOS:
          dstImage = scaleLanczos(entry.getFrame(), factorX, factorY, LANCZOS_KERNEL_SIZE);
          break;
        default:
          dstImage = entry.getFrame();
      }

      if (dstImage != null) {
        // adjusting center
        if (cbAdjustCenter.isSelected()) {
          final double fx = (double) dstImage.getWidth() / (double) entry.getFrame().getWidth();
          final double fy = (double) dstImage.getHeight() / (double) entry.getFrame().getHeight();
          entry.setCenterX((int) (entry.getCenterX() * fx));
          entry.setCenterY((int) (entry.getCenterY() * fy));
        }
        entry.setFrame(dstImage);
      }
    }

    return entry;
  }

  // Scales the specified image using Java's native scalers
  private static BufferedImage scaleNative(BufferedImage srcImage, double factorX, double factorY, int scaleType,
      boolean paletteSupported) {
    BufferedImage dstImage = srcImage;
    final boolean isValid = paletteSupported || srcImage.getType() != BufferedImage.TYPE_BYTE_INDEXED;
    if (isValid && srcImage != null && factorX > 0.0 && factorY > 0.0 && (factorX != 1.0 || factorY != 1.0)) {
      final int width = srcImage.getWidth();
      final int height = srcImage.getHeight();
      int newWidth = (int) (width * factorX);
      if (newWidth < 1) {
        newWidth = 1;
      }
      int newHeight = (int) (height * factorY);
      if (newHeight < 1) {
        newHeight = 1;
      }

      // preparing target image
      if (paletteSupported && srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        final IndexColorModel cm = (IndexColorModel) srcImage.getColorModel();
        final int[] colors = new int[1 << cm.getPixelSize()];
        cm.getRGBs(colors);
        final IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), colors.length, colors, 0, cm.hasAlpha(),
            cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
        dstImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_INDEXED, cm2);
      } else if (srcImage.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
        dstImage = new BufferedImage(newWidth, newHeight, srcImage.getType());
      } else {
        // not supported
        return dstImage;
      }

      // scaling image
      final Graphics2D g = dstImage.createGraphics();
      try {
        g.setComposite(AlphaComposite.Src);
        BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(factorX, factorY), scaleType);
        g.drawImage(srcImage, op, 0, 0);
      } finally {
        g.dispose();
      }
    }
    return dstImage;
  }

  // Uses the Scale2x/Scale3x algorithm
  private static BufferedImage scaleScaleX(BufferedImage srcImage, int factor) {
    BufferedImage dstImage = srcImage;
    if (srcImage != null) {
      switch (factor) {
        case 2:
          dstImage = scaleScale2X(srcImage);
          break;
        case 3:
          dstImage = scaleScale3X(srcImage);
          break;
        case 4:
          dstImage = scaleScale4X(srcImage);
          break;
      }
    }
    return dstImage;
  }

  // Applies the Scale2x algorithm
  private static BufferedImage scaleScale2X(BufferedImage srcImage) {
    BufferedImage dstImage = srcImage;
    if (srcImage != null) {
      final int srcWidth = srcImage.getWidth();
      final int srcHeight = srcImage.getHeight();
      final int dstWidth = 2 * srcWidth;
      final int dstHeight = 2 * srcHeight;
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      byte transIndex = -1;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte) srcImage.getRaster().getDataBuffer()).getData();
        final IndexColorModel cm = (IndexColorModel) srcImage.getColorModel();
        int[] colors = new int[1 << cm.getPixelSize()];
        cm.getRGBs(colors);
        final IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), colors.length, colors, 0, cm.hasAlpha(),
            cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
        dstImage = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        dstB = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < colors.length; i++) {
          if (transIndex < 0 && (colors[i] & 0x00ffffff) == 0x0000ff00) {
            transIndex = (byte) i;
            break;
          }
        }
        if (transIndex < 0) {
          transIndex = 0;
        }
      } else {
        srcI = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();
        dstImage = new BufferedImage(dstWidth, dstHeight, srcImage.getType());
        dstI = ((DataBufferInt) dstImage.getRaster().getDataBuffer()).getData();
      }

      // applying scaling
      int srcOfs = 0, dstOfs = 0;
      for (int y = 0; y < srcHeight; y++) {
        for (int x = 0; x < srcWidth; x++) {
          if (srcB != null) {
            byte p = srcB[srcOfs];
            byte a = (y > 0) ? srcB[srcOfs - srcWidth] : transIndex;
            byte b = (x + 1 < srcWidth) ? srcB[srcOfs + 1] : transIndex;
            byte c = (x > 0) ? srcB[srcOfs - 1] : transIndex;
            byte d = (y + 1 < srcHeight) ? srcB[srcOfs + srcWidth] : transIndex;
            byte t1 = p, t2 = p, t3 = p, t4 = p;
            if (c == a && c != d && a != b) {
              t1 = a;
            }
            if (a == b && a != c && b != d) {
              t2 = b;
            }
            if (b == d && b != a && d != c) {
              t4 = d;
            }
            if (d == c && d != b && c != a) {
              t3 = c;
            }
            dstB[dstOfs] = t1;
            dstB[dstOfs + 1] = t2;
            dstB[dstOfs + dstWidth] = t3;
            dstB[dstOfs + dstWidth + 1] = t4;
          }
          if (srcI != null) {
            int p = srcI[srcOfs];
            int a = (y > 0) ? srcI[srcOfs - srcWidth] : 0;
            int b = (x + 1 < srcWidth) ? srcI[srcOfs + 1] : 0;
            int c = (x > 0) ? srcI[srcOfs - 1] : 0;
            int d = (y + 1 < srcHeight) ? srcI[srcOfs + srcWidth] : 0;
            int t1 = p, t2 = p, t3 = p, t4 = p;
            if (c == a && c != d && a != b) {
              t1 = a;
            }
            if (a == b && a != c && b != d) {
              t2 = b;
            }
            if (b == d && b != a && d != c) {
              t4 = d;
            }
            if (d == c && d != b && c != a) {
              t3 = c;
            }
            dstI[dstOfs] = t1;
            dstI[dstOfs + 1] = t2;
            dstI[dstOfs + dstWidth] = t3;
            dstI[dstOfs + dstWidth + 1] = t4;
          }
          srcOfs++;
          dstOfs += 2;
        }
        dstOfs += dstWidth;
      }

    }
    return dstImage;
  }

  // Applies the Scale3x algorithm
  private static BufferedImage scaleScale3X(BufferedImage srcImage) {
    BufferedImage dstImage = srcImage;
    if (srcImage != null) {
      final int srcWidth = srcImage.getWidth();
      final int srcHeight = srcImage.getHeight();
      final int dstWidth = 3 * srcWidth;
      final int dstWidth2 = dstWidth + dstWidth; // for optimization purposes
      final int dstHeight = 3 * srcHeight;
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      byte transIndex = -1;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte) srcImage.getRaster().getDataBuffer()).getData();
        final IndexColorModel cm = (IndexColorModel) srcImage.getColorModel();
        int[] colors = new int[1 << cm.getPixelSize()];
        cm.getRGBs(colors);
        final IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), colors.length, colors, 0, cm.hasAlpha(),
            cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
        dstImage = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        dstB = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < colors.length; i++) {
          if (transIndex < 0 && (colors[i] & 0x00ffffff) == 0x0000ff00) {
            transIndex = (byte) i;
            break;
          }
        }
        if (transIndex < 0) {
          transIndex = 0;
        }
      } else {
        srcI = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();
        dstImage = new BufferedImage(dstWidth, dstHeight, srcImage.getType());
        dstI = ((DataBufferInt) dstImage.getRaster().getDataBuffer()).getData();
      }

      // applying scaling
      int srcOfs = 0, dstOfs = 0;
      for (int y = 0; y < srcHeight; y++) {
        for (int x = 0; x < srcWidth; x++) {
          if (srcB != null) {
            byte e = srcB[srcOfs];
            byte a = (x > 0 && y > 0) ? srcB[srcOfs - srcWidth - 1] : transIndex;
            byte b = (y > 0) ? srcB[srcOfs - srcWidth] : transIndex;
            byte c = (x + 1 < srcWidth && y > 0) ? srcB[srcOfs - srcWidth + 1] : transIndex;
            byte d = (x > 0) ? srcB[srcOfs - 1] : transIndex;
            byte f = (x + 1 < srcWidth) ? srcB[srcOfs + 1] : transIndex;
            byte g = (x > 0 && y + 1 < srcHeight) ? srcB[srcOfs + srcWidth - 1] : transIndex;
            byte h = (y + 1 < srcHeight) ? srcB[srcOfs + srcWidth] : transIndex;
            byte i = (x + 1 < srcWidth && y + 1 < srcHeight) ? srcB[srcOfs + srcWidth + 1] : transIndex;
            byte t1 = e, t2 = e, t3 = e, t4 = e, t5 = e, t6 = e, t7 = e, t8 = e, t9 = e;
            if (d == b && d != h && b != f) {
              t1 = d;
            }
            if ((d == b && d != h && b != f && e != c) || (b == f && b != d && f != h && e != a)) {
              t2 = b;
            }
            if (b == f && b != d && f != h) {
              t3 = f;
            }
            if ((h == d && h != f && d != b && e != a) || (d == b && d != h && b != f && e != g)) {
              t4 = d;
            }
            if ((b == f && b != d && f != h && e != i) || (f == h && f != b && h != d && e != c)) {
              t6 = f;
            }
            if (h == d && h != f && d != b) {
              t7 = d;
            }
            if ((f == h && f != b && h != d && e != g) || (h == d && h != f && d != b && e != i)) {
              t8 = h;
            }
            if (f == h && f != b && h != d) {
              t9 = f;
            }
            dstB[dstOfs] = t1;
            dstB[dstOfs + 1] = t2;
            dstB[dstOfs + 2] = t3;
            dstB[dstOfs + dstWidth] = t4;
            dstB[dstOfs + dstWidth + 1] = t5;
            dstB[dstOfs + dstWidth + 2] = t6;
            dstB[dstOfs + dstWidth2] = t7;
            dstB[dstOfs + dstWidth2 + 1] = t8;
            dstB[dstOfs + dstWidth2 + 2] = t9;
          }
          if (srcI != null) {
            int e = srcI[srcOfs];
            int a = (x > 0 && y > 0) ? srcI[srcOfs - srcWidth - 1] : 0;
            int b = (y > 0) ? srcI[srcOfs - srcWidth] : 0;
            int c = (x + 1 < srcWidth && y > 0) ? srcI[srcOfs - srcWidth + 1] : 0;
            int d = (x > 0) ? srcI[srcOfs - 1] : 0;
            int f = (x + 1 < srcWidth) ? srcI[srcOfs + 1] : 0;
            int g = (x > 0 && y + 1 < srcHeight) ? srcI[srcOfs + srcWidth - 1] : 0;
            int h = (y + 1 < srcHeight) ? srcI[srcOfs + srcWidth] : 0;
            int i = (x + 1 < srcWidth && y + 1 < srcHeight) ? srcI[srcOfs + srcWidth + 1] : 0;
            int t1 = e, t2 = e, t3 = e, t4 = e, t5 = e, t6 = e, t7 = e, t8 = e, t9 = e;
            if (d == b && d != h && b != f) {
              t1 = d;
            }
            if ((d == b && d != h && b != f && e != c) || (b == f && b != d && f != h && e != a)) {
              t2 = b;
            }
            if (b == f && b != d && f != h) {
              t3 = f;
            }
            if ((h == d && h != f && d != b && e != a) || (d == b && d != h && b != f && e != g)) {
              t4 = d;
            }
            if ((b == f && b != d && f != h && e != i) || (f == h && f != b && h != d && e != c)) {
              t6 = f;
            }
            if (h == d && h != f && d != b) {
              t7 = d;
            }
            if ((f == h && f != b && h != d && e != g) || (h == d && h != f && d != b && e != i)) {
              t8 = h;
            }
            if (f == h && f != b && h != d) {
              t9 = f;
            }
            dstI[dstOfs] = t1;
            dstI[dstOfs + 1] = t2;
            dstI[dstOfs + 2] = t3;
            dstI[dstOfs + dstWidth] = t4;
            dstI[dstOfs + dstWidth + 1] = t5;
            dstI[dstOfs + dstWidth + 2] = t6;
            dstI[dstOfs + dstWidth2] = t7;
            dstI[dstOfs + dstWidth2 + 1] = t8;
            dstI[dstOfs + dstWidth2 + 2] = t9;
          }
          srcOfs++;
          dstOfs += 3;
        }
        dstOfs += dstWidth2;
      }
    }
    return dstImage;
  }

  // Applies Scale2x algorithm twice
  private static BufferedImage scaleScale4X(BufferedImage srcImage) {
    BufferedImage dstImage = srcImage;

    if (srcImage != null) {
      dstImage = scaleScale2X(dstImage);
      dstImage = scaleScale2X(dstImage);
    }

    return dstImage;
  }

  // Performs Lanczos resampling
  private static BufferedImage scaleLanczos(BufferedImage srcImage, double factorX, double factorY, int kernelSize) {
    if (srcImage == null || srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED ||
        factorX <= 0.0 || factorY <= 0.0 || kernelSize < 1) {
      return srcImage;
    }

    final int newWidth = Math.max(1, (int)(srcImage.getWidth() * factorX));
    final int newHeight = Math.max(1, (int)(srcImage.getHeight() * factorY));
    final double scaleX = (double)srcImage.getWidth() / newWidth;
    final double scaleY = (double)srcImage.getHeight() / newHeight;
    final BufferedImage outImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);

    for (int y = 0; y < newHeight; y++) {
      final double srcY = y * scaleY;
      for (int x = 0; x < newWidth; x++) {
        final double srcX = x * scaleX;
        final int color = scaleLanczosSample(srcImage, srcX, srcY, kernelSize);
        outImage.setRGB(x, y, color);
      }
    }

    return outImage;
  }

  // Calculates the sample value at the specified image position
  private static int scaleLanczosSample(BufferedImage image, double x, double y, int kernelSize) {
    double a = 0.0;
    double r = 0.0;
    double g = 0.0;
    double b = 0.0;
    double sum = 0.0;

    final int width = image.getWidth();
    final int height = image.getHeight();
    final int centerX = (int)Math.floor(x);
    final int centerY = (int)Math.floor(y);

    for (int j = -kernelSize + 1; j <= kernelSize; j++) {
      final int srcY = Math.min(Math.max(centerY + j, 0), height - 1);
      final double lanczosWeightY = lanczos(y - srcY, kernelSize);
      for (int i = -kernelSize + 1; i <= kernelSize; i++) {
        final int srcX = Math.min(Math.max(centerX + i, 0), width - 1);
        final int color = image.getRGB(srcX, srcY);
        final double lanczosWeightX = lanczos(x - srcX, kernelSize);
        final double lanczosWeight = lanczosWeightX * lanczosWeightY;

        a += ((color >> 24) & 0xff) * lanczosWeight;
        r += ((color >> 16) & 0xff) * lanczosWeight;
        g += ((color >> 8) & 0xff) * lanczosWeight;
        b += (color & 0xff) * lanczosWeight;
        sum += lanczosWeight;
      }
    }

    final int alpha = Math.min(Math.max((int)(a / sum), 0), 255);
    final int red = Math.min(Math.max((int)(r / sum), 0), 255);
    final int green = Math.min(Math.max((int)(g / sum), 0), 255);
    final int blue = Math.min(Math.max((int)(b / sum), 0), 255);

    return (alpha << 24) | (red << 16) | (green << 8) | blue;
  }

  // Calculates the Lanczos weight
  private static double lanczos(double x, int kernelSize) {
    if (x == 0.0) {
      return 1.0;
    }

    if (x <= -kernelSize || x > kernelSize) {
      return 0.0;
    }

    x *= Math.PI;
    return (kernelSize * FastMath.sin(x) * FastMath.sin(x / kernelSize)) / (x * x);
  }
}
