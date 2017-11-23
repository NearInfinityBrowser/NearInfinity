// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
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
import org.infinity.util.Misc;

/**
 * Transform filter: adjust the size of the BAM frames.
 */
public class BamFilterTransformResize extends BamFilterBaseTransform
    implements ActionListener, ChangeListener
{
  private static final String FilterName = "Resize BAM frames";
  private static final String FilterDesc = "This filter allows you to adjust the size of each BAM frame.";

  private static final int TYPE_NEAREST_NEIGHBOR  = 0;
  private static final int TYPE_BILINEAR          = 1;
  private static final int TYPE_BICUBIC           = 2;
  private static final int TYPE_SCALEX            = 3;
  private static final String[] ScalingTypeItems = {"Nearest neighbor", "Bilinear",
                                                    "Bicubic", "Scale2x/3x/4x"};

  private JComboBox<String> cbType;
  private JCheckBox cbAdjustCenter;
  private JRadioButton rbScaleBoth, rbScaleIndividually;
  private JSpinner spinnerFactor, spinnerFactorX, spinnerFactorY;
  private JLabel lFactor, lFactorX, lFactorY;
  private JTextArea taInfo;

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  public BamFilterTransformResize(ConvertToBam parent)
  {
    super(parent, FilterName, FilterDesc);
  }

  @Override
  public PseudoBamFrameEntry process(PseudoBamFrameEntry entry) throws Exception
  {
    return applyEffect(entry);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry entry)
  {
    return applyEffect(entry);
  }

  @Override
  public void updateControls()
  {
    updateStatus();
  }

  @Override
  public String getConfiguration()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(cbType.getSelectedIndex()).append(';');
    sb.append(rbScaleBoth.isSelected() ? 0 : 1).append(';');
    sb.append(((SpinnerNumberModel)spinnerFactor.getModel()).getNumber().doubleValue()).append(';');
    sb.append(((SpinnerNumberModel)spinnerFactorX.getModel()).getNumber().doubleValue()).append(';');
    sb.append(((SpinnerNumberModel)spinnerFactorY.getModel()).getNumber().doubleValue()).append(';');
    sb.append(cbAdjustCenter.isSelected());
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config)
  {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        int type = -1;
        Double factor = Double.MIN_VALUE;
        Double factorX = Double.MIN_VALUE;
        Double factorY = Double.MIN_VALUE;
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
          int index = (params.length >= 6) ? 2 : 1;
          double min = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMinimum()).doubleValue();
          double max = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMaximum()).doubleValue();
          factor = decodeDouble(params[index], min, max, Double.MIN_VALUE);
          if (factor == Double.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 2) {
          int index = (params.length >= 6) ? 5 : 2;
          if (params[index].equalsIgnoreCase("true")) {
            adjust = true;
          } else if (params[index].equalsIgnoreCase("false")) {
            adjust = false;
          } else {
            return false;
          }
        }

        // loading revised options
        if (params.length >= 6) {
          uniformSelected = (Misc.toNumber(params[1], 0) == 0);

          double min = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMinimum()).doubleValue();
          double max = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMaximum()).doubleValue();
          factorX = decodeDouble(params[3], min, max, Double.MIN_VALUE);
          if (factorX == Double.MIN_VALUE) {
            return false;
          }

          min = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMinimum()).doubleValue();
          max = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMaximum()).doubleValue();
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
  protected JPanel loadControls()
  {
    /*
     * Possible scaling algorithms:
     * - nearest neighbor (BamV1, BamV2) -> use Java's internal filters
     * - bilinear (BamV2) -> use Java's internal filters
     * - bicubic (BamV2) -> use Java's internal filters
     * - scale2x/scale3x (BamV1, BamV2) -> http://en.wikipedia.org/wiki/Image_scaling
     * - [?] lanczos (BamV2) -> http://en.wikipedia.org/wiki/Lanczos_resampling
     * - [?] xBR (BamV1, BamV2) -> http://board.byuu.org/viewtopic.php?f=10&t=2248
     */
    GridBagConstraints c = new GridBagConstraints();

    JLabel l1 = new JLabel("Type:");
    lFactor = new JLabel("Factor:");
    lFactorX = new JLabel("Factor X:");
    lFactorX.setEnabled(false);
    lFactorY = new JLabel("Factor Y:");
    lFactorY.setEnabled(false);
    cbType = new JComboBox<>(ScalingTypeItems);
    cbType.addActionListener(this);
    rbScaleBoth = new JRadioButton("Scale uniformly");
    rbScaleIndividually = new JRadioButton("Scale individually");
    ButtonGroup bg = new ButtonGroup();
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
    taInfo.setPreferredSize(new Dimension(Math.max(w, taInfo.getPreferredSize().width),
                                          taInfo.getPreferredSize().height));
    cbAdjustCenter = new JCheckBox("Adjust center position", true);
    cbAdjustCenter.addActionListener(this);

    JPanel p1 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p1.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    p1.add(cbType, c);
    ViewerUtil.setGBC(c, 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    p1.add(taInfo, c);

    JPanel p2 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    p2.add(rbScaleBoth, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 24, 0, 0), 0, 0);
    p2.add(lFactor, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    p2.add(spinnerFactor, c);

    JPanel p3 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 4, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    p3.add(rbScaleIndividually, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 24, 0, 0), 0, 0);
    p3.add(lFactorX, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    p3.add(spinnerFactorX, c);
    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 8, 0, 0), 0, 0);
    p3.add(lFactorY, c);
    ViewerUtil.setGBC(c, 3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    p3.add(spinnerFactorY, c);

    JPanel p4 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 1, 4, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    p4.add(cbAdjustCenter, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p1, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    panel.add(p2, c);
    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p3, c);
    ViewerUtil.setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(12, 0, 0, 0), 0, 0);
    panel.add(p4, c);

    updateStatus();

    return panel;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == cbType ||
        event.getSource() == rbScaleBoth ||
        event.getSource() == rbScaleIndividually) {
      updateStatus();
      fireChangeListener();
    } else if (event.getSource() == cbAdjustCenter) {
      fireChangeListener();
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == spinnerFactor ||
        event.getSource() == spinnerFactorX ||
        event.getSource() == spinnerFactorY) {
      fireChangeListener();
    }
  }

//--------------------- Begin Interface ChangeListener ---------------------


  // Updates controls depending on current scaling type
  private void updateStatus()
  {
    final String fmtSupport1 = "Supported target: %s";
    final String fmtSupport2 = "Supported targets: %s, %s";

    int type = cbType.getSelectedIndex();
    double factor = getFactor(spinnerFactor);
    double factorX = getFactor(spinnerFactorX);
    double factorY = getFactor(spinnerFactorY);

    boolean uniformEnabled = rbScaleBoth.isSelected() && isTypeSupported(type);
    boolean individualEnabled = rbScaleIndividually.isSelected() && isTypeSupported(type);
    switch (type) {
      case TYPE_NEAREST_NEIGHBOR:
        taInfo.setText(String.format(fmtSupport2, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV1],
                                                  ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
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
      case TYPE_BILINEAR:
        taInfo.setText(String.format(fmtSupport1, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
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
      case TYPE_BICUBIC:
        taInfo.setText(String.format(fmtSupport1, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
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
      case TYPE_SCALEX:
        taInfo.setText(String.format(fmtSupport2, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV1],
                                                  ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
        setFactor(spinnerFactor, (int)factor, 2, 4, 1);
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

  private boolean isTypeSupported(int type)
  {
    switch (type) {
      case TYPE_BILINEAR:
      case TYPE_BICUBIC:
        return !getConverter().isBamV1Selected();
      default:
        return true;
    }
  }

  private void setFactor(JSpinner spinner, Number current, Number min, Number max, Number step)
  {
    if (spinner != null && spinner.getModel() instanceof SpinnerNumberModel) {
      SpinnerNumberModel snm = (SpinnerNumberModel)spinner.getModel();
      boolean isDouble = ((current instanceof Double) || (min instanceof Double) ||
                          (max instanceof Double) || (step instanceof Double));
      int curI = 0, minI = 0, maxI = 0, stepI = 0;
      double curD = 0, minD = 0, maxD = 0, stepD = 0;
      if (isDouble) {
        curD = ((Double)current).doubleValue();
        minD = ((Double)min).doubleValue();
        maxD = ((Double)max).doubleValue();
        stepD = ((Double)step).doubleValue();
      } else {
        curI = ((Integer)current).intValue();
        minI = ((Integer)min).intValue();
        maxI = ((Integer)max).intValue();
        stepI = ((Integer)step).intValue();
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
          spinner.setModel(new SpinnerNumberModel(curI, minI, (maxI < 10) ? 10 : maxI, stepI));
          if (maxI < 10) {
            ((SpinnerNumberModel)spinner.getModel()).setMaximum(Integer.valueOf(maxI));
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

  private double getFactor(JSpinner spinner)
  {
    if (spinner != null) {
      SpinnerNumberModel snm = (SpinnerNumberModel)spinner.getModel();
      return ((Number)snm.getValue()).doubleValue();
    } else {
      return 1.0;
    }
  }

  private PseudoBamFrameEntry applyEffect(PseudoBamFrameEntry entry)
  {
    if (entry != null && entry.getFrame() != null) {
      BufferedImage dstImage;
      double factorX = getFactor(rbScaleBoth.isSelected() ? spinnerFactor : spinnerFactorX);
      double factorY = getFactor(rbScaleBoth.isSelected() ? spinnerFactor : spinnerFactorY);
      int type = cbType.getSelectedIndex();
      switch (type) {
        case TYPE_NEAREST_NEIGHBOR:
          dstImage = scaleNative(entry.getFrame(), factorX, factorY, AffineTransformOp.TYPE_NEAREST_NEIGHBOR, true);
          break;
        case TYPE_BILINEAR:
          dstImage = scaleNative(entry.getFrame(), factorX, factorY, AffineTransformOp.TYPE_BILINEAR, false);
          break;
        case TYPE_BICUBIC:
          dstImage = scaleNative(entry.getFrame(), factorX, factorY, AffineTransformOp.TYPE_BICUBIC, false);
          break;
        case TYPE_SCALEX:
          dstImage = scaleScaleX(entry.getFrame(), (int)factorX);
          break;
        default:
          dstImage = entry.getFrame();
      }

      if (dstImage != null) {
        // adjusting center
        if (cbAdjustCenter.isSelected()) {
          double fx = (double)dstImage.getWidth() / (double)entry.getFrame().getWidth();
          double fy = (double)dstImage.getHeight() / (double)entry.getFrame().getHeight();
          entry.setCenterX((int)((double)entry.getCenterX()*fx));
          entry.setCenterY((int)((double)entry.getCenterY()*fy));
        }
        entry.setFrame(dstImage);
      }
    }

    return entry;
  }


  // Scales the specified image using Java's native scalers
  private BufferedImage scaleNative(BufferedImage srcImage, double factorX, double factorY, int scaleType, boolean paletteSupported)
  {
    BufferedImage dstImage = srcImage;
    boolean isValid = paletteSupported || srcImage.getType() != BufferedImage.TYPE_BYTE_INDEXED;
    if (isValid && srcImage != null && factorX > 0.0 && factorY > 0.0 && (factorX != 1.0 || factorY != 1.0)) {
      int width = srcImage.getWidth();
      int height = srcImage.getHeight();
      int newWidth = (int)((double)width * factorX);
      if (newWidth < 1) newWidth = 1;
      int newHeight = (int)((double)height * factorY);
      if (newHeight < 1) newHeight = 1;

      // preparing target image
      if (paletteSupported && srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        IndexColorModel cm = (IndexColorModel)srcImage.getColorModel();
        int[] colors = new int[1 << cm.getPixelSize()];
        cm.getRGBs(colors);
        IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), colors.length,
                                                  colors, 0, cm.hasAlpha(), cm.getTransparentPixel(),
                                                  DataBuffer.TYPE_BYTE);
        dstImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_INDEXED, cm2);
      } else if (srcImage.getType() != BufferedImage.TYPE_BYTE_INDEXED) {
        dstImage = new BufferedImage(newWidth, newHeight, srcImage.getType());
      } else {
        // not supported
        return dstImage;
      }

      // scaling image
      Graphics2D g = dstImage.createGraphics();
      try {
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC));
        BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(factorX, factorY),
                                                   scaleType);
        g.drawImage(srcImage, op, 0, 0);
      } finally {
        g.dispose();
        g = null;
      }
    }
    return dstImage;
  }


  // Uses the Scale2x/Scale3x algorithm
  private BufferedImage scaleScaleX(BufferedImage srcImage, int factor)
  {
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
  private BufferedImage scaleScale2X(BufferedImage srcImage)
  {
    BufferedImage dstImage = srcImage;
    if (srcImage != null) {
      int srcWidth = srcImage.getWidth();
      int srcHeight = srcImage.getHeight();
      int dstWidth = 2*srcWidth;
      int dstHeight = 2*srcHeight;
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      byte transIndex = -1;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte)srcImage.getRaster().getDataBuffer()).getData();
        IndexColorModel cm = (IndexColorModel)srcImage.getColorModel();
        int[] colors = new int[1 << cm.getPixelSize()];
        cm.getRGBs(colors);
        IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), colors.length, colors, 0,
                                                  cm.hasAlpha(), cm.getTransparentPixel(),
                                                  DataBuffer.TYPE_BYTE);
        dstImage = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        dstB = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < colors.length; i++) {
          if (transIndex < 0 && (colors[i] & 0x00ffffff) == 0x0000ff00) {
            transIndex = (byte)i;
            break;
          }
        }
        if (transIndex < 0) {
          transIndex = 0;
        }
      } else {
        srcI = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
        dstImage = new BufferedImage(dstWidth, dstHeight, srcImage.getType());
        dstI = ((DataBufferInt)dstImage.getRaster().getDataBuffer()).getData();
      }

      // applying scaling
      int srcOfs = 0, dstOfs = 0;
      for (int y = 0; y < srcHeight; y++) {
        for (int x = 0; x < srcWidth; x++) {
          if (srcB != null) {
            byte p = srcB[srcOfs];
            byte a = (y > 0) ? srcB[srcOfs-srcWidth] : transIndex;
            byte b = (x+1 < srcWidth) ? srcB[srcOfs+1] : transIndex;
            byte c = (x > 0) ? srcB[srcOfs-1] : transIndex;
            byte d = (y+1 < srcHeight) ? srcB[srcOfs+srcWidth] : transIndex;
            byte t1 = p, t2 = p, t3 = p, t4 = p;
            if (c == a && c != d && a != b) t1 = a;
            if (a == b && a != c && b != d) t2 = b;
            if (b == d && b != a && d != c) t4 = d;
            if (d == c && d != b && c != a) t3 = c;
            dstB[dstOfs] = t1;
            dstB[dstOfs+1] = t2;
            dstB[dstOfs+dstWidth] = t3;
            dstB[dstOfs+dstWidth+1] = t4;
          }
          if (srcI != null) {
            int p = srcI[srcOfs];
            int a = (y > 0) ? srcI[srcOfs-srcWidth] : 0;
            int b = (x+1 < srcWidth) ? srcI[srcOfs+1] : 0;
            int c = (x > 0) ? srcI[srcOfs-1] : 0;
            int d = (y+1 < srcHeight) ? srcI[srcOfs+srcWidth] : 0;
            int t1 = p, t2 = p, t3 = p, t4 = p;
            if (c == a && c != d && a != b) t1 = a;
            if (a == b && a != c && b != d) t2 = b;
            if (b == d && b != a && d != c) t4 = d;
            if (d == c && d != b && c != a) t3 = c;
            dstI[dstOfs] = t1;
            dstI[dstOfs+1] = t2;
            dstI[dstOfs+dstWidth] = t3;
            dstI[dstOfs+dstWidth+1] = t4;
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
  private BufferedImage scaleScale3X(BufferedImage srcImage)
  {
    BufferedImage dstImage = srcImage;
    if (srcImage != null) {
      int srcWidth = srcImage.getWidth();
      int srcHeight = srcImage.getHeight();
      int dstWidth = 3*srcWidth;
      int dstWidth2 = dstWidth+dstWidth;    // for optimization purposes
      int dstHeight = 3*srcHeight;
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      byte transIndex = -1;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte)srcImage.getRaster().getDataBuffer()).getData();
        IndexColorModel cm = (IndexColorModel)srcImage.getColorModel();
        int[] colors = new int[1 << cm.getPixelSize()];
        cm.getRGBs(colors);
        IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), colors.length, colors, 0,
                                                  cm.hasAlpha(), cm.getTransparentPixel(),
                                                  DataBuffer.TYPE_BYTE);
        dstImage = new BufferedImage(dstWidth, dstHeight, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        dstB = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < colors.length; i++) {
          if (transIndex < 0 && (colors[i] & 0x00ffffff) == 0x0000ff00) {
            transIndex = (byte)i;
            break;
          }
        }
        if (transIndex < 0) {
          transIndex = 0;
        }
      } else {
        srcI = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
        dstImage = new BufferedImage(dstWidth, dstHeight, srcImage.getType());
        dstI = ((DataBufferInt)dstImage.getRaster().getDataBuffer()).getData();
      }

      // applying scaling
      int srcOfs = 0, dstOfs = 0;
      for (int y = 0; y < srcHeight; y++) {
        for (int x = 0; x < srcWidth; x++) {
          if (srcB != null) {
            byte e = srcB[srcOfs];
            byte a = (x > 0 && y > 0) ? srcB[srcOfs-srcWidth-1] : transIndex;
            byte b = (y > 0) ? srcB[srcOfs-srcWidth] : transIndex;
            byte c = (x+1 < srcWidth && y > 0) ? srcB[srcOfs-srcWidth+1] : transIndex;
            byte d = (x > 0) ? srcB[srcOfs-1] : transIndex;
            byte f = (x+1 < srcWidth) ? srcB[srcOfs+1] : transIndex;
            byte g = (x > 0 && y+1 < srcHeight) ? srcB[srcOfs+srcWidth-1] : transIndex;
            byte h = (y+1 < srcHeight) ? srcB[srcOfs+srcWidth] : transIndex;
            byte i = (x+1 < srcWidth && y+1 < srcHeight) ? srcB[srcOfs+srcWidth+1] : transIndex;
            byte t1 = e, t2 = e, t3 = e, t4 = e, t5 = e, t6 = e, t7 = e, t8 = e, t9 = e;
            if (d == b && d != h && b != f) t1 = d;
            if ((d == b && d != h && b != f && e != c) || (b == f && b != d && f != h && e != a)) t2 = b;
            if (b == f && b != d && f != h) t3 = f;
            if ((h == d && h != f && d != b && e != a) || (d == b && d != h && b != f && e != g)) t4 = d;
            if ((b == f && b != d && f != h && e != i) || (f == h && f != b && h != d && e != c)) t6 = f;
            if (h == d && h != f && d != b) t7 = d;
            if ((f == h && f != b && h != d && e != g) || (h == d && h != f && d != b && e != i)) t8 = h;
            if (f == h && f != b && h != d) t9 = f;
            dstB[dstOfs] = t1;
            dstB[dstOfs+1] = t2;
            dstB[dstOfs+2] = t3;
            dstB[dstOfs+dstWidth] = t4;
            dstB[dstOfs+dstWidth+1] = t5;
            dstB[dstOfs+dstWidth+2] = t6;
            dstB[dstOfs+dstWidth2] = t7;
            dstB[dstOfs+dstWidth2+1] = t8;
            dstB[dstOfs+dstWidth2+2] = t9;
          }
          if (srcI != null) {
            int e = srcI[srcOfs];
            int a = (x > 0 && y > 0) ? srcI[srcOfs-srcWidth-1] : 0;
            int b = (y > 0) ? srcI[srcOfs-srcWidth] : 0;
            int c = (x+1 < srcWidth && y > 0) ? srcI[srcOfs-srcWidth+1] : 0;
            int d = (x > 0) ? srcI[srcOfs-1] : 0;
            int f = (x+1 < srcWidth) ? srcI[srcOfs+1] : 0;
            int g = (x > 0 && y+1 < srcHeight) ? srcI[srcOfs+srcWidth-1] : 0;
            int h = (y+1 < srcHeight) ? srcI[srcOfs+srcWidth] : 0;
            int i = (x+1 < srcWidth && y+1 < srcHeight) ? srcI[srcOfs+srcWidth+1] : 0;
            int t1 = e, t2 = e, t3 = e, t4 = e, t5 = e, t6 = e, t7 = e, t8 = e, t9 = e;
            if (d == b && d != h && b != f) t1 = d;
            if ((d == b && d != h && b != f && e != c) || (b == f && b != d && f != h && e != a)) t2 = b;
            if (b == f && b != d && f != h) t3 = f;
            if ((h == d && h != f && d != b && e != a) || (d == b && d != h && b != f && e != g)) t4 = d;
            if ((b == f && b != d && f != h && e != i) || (f == h && f != b && h != d && e != c)) t6 = f;
            if (h == d && h != f && d != b) t7 = d;
            if ((f == h && f != b && h != d && e != g) || (h == d && h != f && d != b && e != i)) t8 = h;
            if (f == h && f != b && h != d) t9 = f;
            dstI[dstOfs] = t1;
            dstI[dstOfs+1] = t2;
            dstI[dstOfs+2] = t3;
            dstI[dstOfs+dstWidth] = t4;
            dstI[dstOfs+dstWidth+1] = t5;
            dstI[dstOfs+dstWidth+2] = t6;
            dstI[dstOfs+dstWidth2] = t7;
            dstI[dstOfs+dstWidth2+1] = t8;
            dstI[dstOfs+dstWidth2+2] = t9;
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
  private BufferedImage scaleScale4X(BufferedImage srcImage)
  {
    BufferedImage dstImage = srcImage;

    if (srcImage != null) {
      dstImage = scaleScale2X(dstImage);
      dstImage = scaleScale2X(dstImage);
    }

    return dstImage;
  }
}
