// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.AlphaComposite;
import java.awt.Color;
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

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
  private JSpinner spinnerFactor;
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
    sb.append(((SpinnerNumberModel)spinnerFactor.getModel()).getNumber().doubleValue()).append(';');
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
        boolean adjust = true;

        if (params.length > 0) {
          type = Misc.toNumber(params[0], -1);
          if (type < 0 || type >= cbType.getModel().getSize()) {
            return false;
          }
        }
        if (params.length > 1) {
          double min = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMinimum()).doubleValue();
          double max = ((Number)((SpinnerNumberModel)spinnerFactor.getModel()).getMaximum()).doubleValue();
          factor = decodeDouble(params[1], min, max, Double.MIN_VALUE);
          if (factor == Double.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 2) {
          if (params[2].equalsIgnoreCase("true")) {
            adjust = true;
          } else if (params[2].equalsIgnoreCase("false")) {
            adjust = false;
          } else {
            return false;
          }
        }

        if (type >= 0) {
          cbType.setSelectedIndex(type);
        }
        if (factor != Double.MIN_VALUE) {
          spinnerFactor.setValue(factor);
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
    JLabel l2 = new JLabel("Factor:");
    cbType = new JComboBox<>(ScalingTypeItems);
    cbType.addActionListener(this);
    spinnerFactor = new JSpinner(new SpinnerNumberModel(1.0, 0.01, 10.0, 0.05));
    spinnerFactor.addChangeListener(this);
    taInfo = new JTextArea(2, 0);
    taInfo.setEditable(false);
    taInfo.setFont(UIManager.getFont("Label.font"));
    Color bg = UIManager.getColor("Label.background");
    taInfo.setBackground(bg);
    taInfo.setSelectionColor(bg);
    taInfo.setSelectedTextColor(bg);
    taInfo.setWrapStyleWord(true);
    taInfo.setLineWrap(true);
    cbAdjustCenter = new JCheckBox("Adjust center position", true);
    cbAdjustCenter.addActionListener(this);

    JPanel p = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    p.add(cbType, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 12, 0, 0), 0, 0);
    p.add(l2, c);
    ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    p.add(spinnerFactor, c);
    ViewerUtil.setGBC(c, 0, 1, 4, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    p.add(taInfo, c);
    ViewerUtil.setGBC(c, 0, 2, 4, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    p.add(cbAdjustCenter, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p, c);

    updateStatus();

    return panel;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == cbType) {
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
    if (event.getSource() == spinnerFactor) {
      fireChangeListener();
    }
  }

//--------------------- Begin Interface ChangeListener ---------------------


  // Updates controls depending on current scaling type
  private void updateStatus()
  {
    final String fmtSupport1 = "Supported target: %1$s";
    final String fmtSupport2 = "Supported targets: %1$s, %2$s";

    int type = cbType.getSelectedIndex();
    SpinnerNumberModel snm = (SpinnerNumberModel)spinnerFactor.getModel();
    double factor;
    if (snm.getValue() instanceof Double) {
      factor = ((Double)snm.getValue()).doubleValue();
    } else {
      factor = ((Integer)snm.getValue()).doubleValue();
    }

    switch (type) {
      case TYPE_NEAREST_NEIGHBOR:
        taInfo.setText(String.format(fmtSupport2, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV1],
                                                  ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
        setFactor(factor, 0.01, 10.0, 0.05);
        spinnerFactor.setEnabled(true);
        break;
      case TYPE_BILINEAR:
        taInfo.setText(String.format(fmtSupport1, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
        setFactor(factor, 0.01, 10.0, 0.05);
        spinnerFactor.setEnabled(!getConverter().isBamV1Selected());
        break;
      case TYPE_BICUBIC:
        taInfo.setText(String.format(fmtSupport1, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
        setFactor(factor, 0.01, 10.0, 0.05);
        spinnerFactor.setEnabled(!getConverter().isBamV1Selected());
        break;
      case TYPE_SCALEX:
        taInfo.setText(String.format(fmtSupport2, ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV1],
                                                  ConvertToBam.BamVersionItems[ConvertToBam.VERSION_BAMV2]));
        setFactor((int)factor, 2, 4, 1);
        spinnerFactor.setEnabled(true);
        break;
      default:
        taInfo.setText("");
        setFactor(factor, 0.01, 10.0, 0.05);
    }
  }

  private void setFactor(Number current, Number min, Number max, Number step)
  {
    if (spinnerFactor.getModel() instanceof SpinnerNumberModel) {
      SpinnerNumberModel snm = (SpinnerNumberModel)spinnerFactor.getModel();
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
          spinnerFactor.setModel(new SpinnerNumberModel(curD, minD, maxD, stepD));
        } else {
          snm.setMinimum(minD);
          snm.setMaximum(maxD);
          snm.setValue(curD);
          snm.setStepSize(stepD);
        }
      } else {
        if (snm.getValue() instanceof Double) {
          curI = Math.max(Math.min(curI, maxI), minI);
          spinnerFactor.setModel(new SpinnerNumberModel(curI, minI, (maxI < 10) ? 10 : maxI, stepI));
          if (maxI < 10) {
            ((SpinnerNumberModel)spinnerFactor.getModel()).setMaximum(Integer.valueOf(maxI));
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

  private double getFactor()
  {
    SpinnerNumberModel snm = (SpinnerNumberModel)spinnerFactor.getModel();
    return ((Number)snm.getValue()).doubleValue();
  }

  private PseudoBamFrameEntry applyEffect(PseudoBamFrameEntry entry)
  {
    if (entry != null && entry.getFrame() != null) {
      BufferedImage dstImage;
      double factor = getFactor();
      int type = cbType.getSelectedIndex();
      switch (type) {
        case TYPE_NEAREST_NEIGHBOR:
          dstImage = scaleNative(entry.getFrame(), factor, AffineTransformOp.TYPE_NEAREST_NEIGHBOR, true);
          break;
        case TYPE_BILINEAR:
          dstImage = scaleNative(entry.getFrame(), factor, AffineTransformOp.TYPE_BILINEAR, false);
          break;
        case TYPE_BICUBIC:
          dstImage = scaleNative(entry.getFrame(), factor, AffineTransformOp.TYPE_BICUBIC, false);
          break;
        case TYPE_SCALEX:
          dstImage = scaleScaleX(entry.getFrame(), (int)factor);
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
  private BufferedImage scaleNative(BufferedImage srcImage, double factor, int scaleType, boolean paletteSupported)
  {
    BufferedImage dstImage = srcImage;
    boolean isValid = paletteSupported || srcImage.getType() != BufferedImage.TYPE_BYTE_INDEXED;
    if (isValid && srcImage != null && factor > 0.0 && factor != 1.0) {
      int width = srcImage.getWidth();
      int height = srcImage.getHeight();
      int newWidth = (int)((double)width * factor);
      if (newWidth < 1) newWidth = 1;
      int newHeight = (int)((double)height * factor);
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
        BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(factor, factor),
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
