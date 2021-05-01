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
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.tuples.Triple;

/**
 * ColorFilter: adjust CIELAB values L, a and b.
 */
public class BamFilterColorLab extends BamFilterBaseColor
    implements ChangeListener, ActionListener
{
  private static final String FilterName = "CIELAB (L/a/b)";
  private static final String FilterDesc = "This filter provides controls for adjusting values in the CIELAB color space";

  private static final int LAB_L_MIN = -127;
  private static final int LAB_L_MAX = 127;
  private static final int LAB_A_MIN = -255;
  private static final int LAB_A_MAX = 255;
  private static final int LAB_B_MIN = -255;
  private static final int LAB_B_MAX = 255;

  private JSlider sliderL, sliderA, sliderB;
  private JSpinner spinnerL, spinnerA, spinnerB;
  private ButtonPopupWindow bpwExclude;
  private BamFilterBaseColor.ExcludeColorsPanel pExcludeColors;

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  public BamFilterColorLab(ConvertToBam parent)
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
    sb.append(sliderL.getValue()).append(';');
    sb.append(sliderA.getValue()).append(';');
    sb.append(sliderB.getValue()).append(';');
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
        Integer lValue = Integer.MIN_VALUE;
        Integer aValue = Integer.MIN_VALUE;
        Integer bValue = Integer.MIN_VALUE;
        int[] indices = null;

        // parsing configuration data
        if (params.length > 0) {  // set L value
          lValue = decodeNumber(params[0], sliderL.getMinimum(), sliderL.getMaximum(), Integer.MIN_VALUE);
          if (lValue == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 1) {  // set A value
          aValue = decodeNumber(params[1], sliderA.getMinimum(), sliderA.getMaximum(), Integer.MIN_VALUE);
          if (aValue == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 2) {  // set B value
          bValue = decodeNumber(params[2], sliderB.getMinimum(), sliderB.getMaximum(), Integer.MIN_VALUE);
          if (bValue == Integer.MIN_VALUE) {
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
        if (lValue != Integer.MIN_VALUE) {
          sliderL.setValue(lValue);
        }
        if (aValue != Integer.MIN_VALUE) {
          sliderA.setValue(aValue);
        }
        if (bValue != Integer.MIN_VALUE) {
          sliderB.setValue(bValue);
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

    JLabel lh = new JLabel("L:");
    JLabel ls = new JLabel("a:");
    JLabel ll = new JLabel("b:");
    sliderL = new JSlider(SwingConstants.HORIZONTAL, LAB_L_MIN, LAB_L_MAX, 0);
    sliderL.addChangeListener(this);
    sliderA = new JSlider(SwingConstants.HORIZONTAL, LAB_A_MIN, LAB_A_MAX, 0);
    sliderA.addChangeListener(this);
    sliderB = new JSlider(SwingConstants.HORIZONTAL, LAB_B_MIN, LAB_B_MAX, 0);
    sliderB.addChangeListener(this);
    spinnerL = new JSpinner(new SpinnerNumberModel(sliderL.getValue(),
                                                   sliderL.getMinimum(),
                                                   sliderL.getMaximum(), 1));
    spinnerL.addChangeListener(this);
    spinnerA = new JSpinner(new SpinnerNumberModel(sliderA.getValue(),
                                                   sliderA.getMinimum(),
                                                   sliderA.getMaximum(), 1));
    spinnerA.addChangeListener(this);
    spinnerB = new JSpinner(new SpinnerNumberModel(sliderB.getValue(),
                                                   sliderB.getMinimum() ,
                                                   sliderB.getMaximum(), 1));
    spinnerB.addChangeListener(this);

    JPanel p = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p.add(lh, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    p.add(sliderL, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    p.add(spinnerL, c);

    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(ls, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(sliderA, c);
    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(spinnerA, c);

    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p.add(ll, c);
    ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(sliderB, c);
    ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p.add(spinnerB, c);
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
    } else if (event.getSource() == sliderL) {
      spinnerL.setValue(Integer.valueOf(sliderL.getValue()));
      if (sliderL.getModel().getValueIsAdjusting() == false) {
        fireChangeListener();
      }
    } else if (event.getSource() == sliderA) {
      spinnerA.setValue(Integer.valueOf(sliderA.getValue()));
      if (sliderA.getModel().getValueIsAdjusting() == false) {
        fireChangeListener();
      }
    } else if (event.getSource() == sliderB) {
      spinnerB.setValue(Integer.valueOf(sliderB.getValue()));
      if (sliderB.getModel().getValueIsAdjusting() == false) {
        fireChangeListener();
      }
    } else if (event.getSource() == spinnerL) {
      sliderL.setValue(((Integer)spinnerL.getValue()).intValue());
    } else if (event.getSource() == spinnerA) {
      sliderA.setValue(((Integer)spinnerA.getValue()).intValue());
    } else if (event.getSource() == spinnerB) {
      sliderB.setValue(((Integer)spinnerB.getValue()).intValue());
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

      // L hue in range [0, 100]
      double labL = ((Integer)spinnerL.getValue()).doubleValue();
      // a in range [-160, 160]
      double labA = ((Integer)spinnerA.getValue()).doubleValue();
      // b in range [-160, 160]
      double labB = ((Integer)spinnerB.getValue()).doubleValue();

      for (int i = 0; i < buffer.length; i++) {
        if ((cm == null || (cm != null && !pExcludeColors.isSelectedIndex(i))) &&
            (buffer[i] & 0xff000000) != 0) {
          // convert RGB -> Lab
          int fa = isPremultiplied ? (buffer[i] >>> 24) & 0xff : 255;
          int fr = ((buffer[i] >>> 16) & 0xff) * fa / 255;
          int fg = ((buffer[i] >>> 8) & 0xff) * fa / 255;
          int fb = (buffer[i] & 0xff) * fa / 255;
          int rgb = (fr << 16) | (fg << 8) | fb;
          Triple<Double, Double, Double> lab = ColorConvert.convertRGBtoLab(rgb);
          double l = lab.getValue0().doubleValue();
          double a = lab.getValue1().doubleValue();
          double b = lab.getValue2().doubleValue();

//          float fa = isPremultiplied ? (float)((buffer[i] >>> 24) & 0xff) : 255.0f;
//          float fr = (float)((buffer[i] >>> 16) & 0xff) / fa;
//          float fg = (float)((buffer[i] >>> 8) & 0xff) / fa;
//          float fb = (float)(buffer[i] & 0xff) / fa;
//          float cmin = fr; if (fg < cmin) cmin = fg; if (fb < cmin) cmin = fb;
//          float cmax = fr; if (fg > cmax) cmax = fg; if (fb > cmax) cmax = fb;
//          float cdelta = cmax - cmin;
//          float cdelta2 = cdelta / 2.0f;
//          float h, s, l;
//
//          l = (cmax + cmin) / 2.0f;
//
//          if (cdelta == 0.0f) {
//            h = 0.0f;
//            s = 0.0f;
//          } else {
//            if (l < 0.5f) {
//              s = cdelta / (cmax + cmin);
//            } else {
//              s = cdelta / (2.0f - cmax - cmin);
//            }
//
//            float dr = (((cmax - fr) / 6.0f) + cdelta2) / cdelta;
//            float dg = (((cmax - fg) / 6.0f) + cdelta2) / cdelta;
//            float db = (((cmax - fb) / 6.0f) + cdelta2) / cdelta;
//
//            if (fr == cmax) {
//              h = db - dg;
//            } else if (fg == cmax) {
//              h = (1.0f / 3.0f) + dr - db;
//            } else {
//              h = (2.0f / 3.0f) + dg - dr;
//            }
//
//            if (h < 0.0f) h += 1.0f; else if (h > 1.0f) h -= 1.0f;
//          }

          // applying adjustments

          l = Math.max((double)LAB_L_MIN, Math.min((double)LAB_L_MAX, l + labL));
          a = Math.max((double)LAB_A_MIN, Math.min((double)LAB_A_MAX, a + labA));
          b = Math.max((double)LAB_B_MIN, Math.min((double)LAB_B_MAX, b + labB));

          // converting Lab -> RGB
          rgb = ColorConvert.convertLabToRGB(l, a, b);
          buffer[i] = (buffer[i] & 0xff000000) | rgb;

//          if (s == 0.0f) {
//            // achromatic
//            int v = (int)(l * 255.0f);
//            buffer[i] = (buffer[i] & 0xff000000) | (v << 16) | (v << 8) | v;
//          } else {
//            float f2 = (l < 0.5f) ? l * (1.0f + s) : (l + s) - (s * l);
//            float f1 = 2.0f * l - f2;
//            float res;
//
//            // red
//            float t = h + (1.0f / 3.0f);
//            if (t < 0.0f) t += 1.0f; else if (t > 1.0f) t -= 1.0f;
//            if ((6.0f * t) < 1.0f) {
//              res = f1 + (f2 - f1) * 6.0f * t;
//            } else if ((2.0f * t) < 1.0f) {
//              res = f2;
//            } else if ((3.0f * t) < 2.0f) {
//              res = f1 + (f2 - f1) * ((2.0f / 3.0f) - t) * 6.0f;
//            } else {
//              res = f1;
//            }
//            int r = (int)(res * fa);
//
//            // green
//            t = h;
//            if ((6.0f * t) < 1.0f) {
//              res = f1 + (f2 - f1) * 6.0f * t;
//            } else if ((2.0f * t) < 1.0f) {
//              res = f2;
//            } else if ((3.0f * t) < 2.0f) {
//              res = f1 + (f2 - f1) * ((2.0f / 3.0f) - t) * 6.0f;
//            } else {
//              res = f1;
//            }
//            int g = (int)(res * fa);
//
//            // blue
//            t = h - (1.0f / 3.0f);
//            if (t < 0.0f) t += 1.0f; else if (t > 1.0f) t -= 1.0f;
//            if ((6.0f * t) < 1.0f) {
//              res = f1 + (f2 - f1) * 6.0f * t;
//            } else if ((2.0f * t) < 1.0f) {
//              res = f2;
//            } else if ((3.0f * t) < 2.0f) {
//              res = f1 + (f2 - f1) * ((2.0f / 3.0f) - t) * 6.0f;
//            } else {
//              res = f1;
//            }
//            int b = (int)(res * fa);
//
//            if (r < 0) r = 0; else if (r > 255) r = 255;
//            if (g < 0) g = 0; else if (g > 255) g = 255;
//            if (b < 0) b = 0; else if (b > 255) b = 255;
//            buffer[i] = (buffer[i] & 0xff000000) | (r << 16) | (g << 8) | b;
//          }
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
