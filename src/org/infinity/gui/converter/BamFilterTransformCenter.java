// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
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
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * Transform filter: Expands canvas to keep center position at fixed location for all frames.
 */
public class BamFilterTransformCenter extends BamFilterBaseTransform
    implements ChangeListener, ActionListener
{
  private static final String FilterName = "Center BAM frames";
  private static final String FilterDesc = "This filter expands the canvas to a constant size, so that " +
                                           "center position will be kept at a fixed location for all frames.\n" +
                                           "This is helpful for operations that do not take " +
                                           "center position into account (such as GIF output filter).";

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  private JSpinner spinnerLeft, spinnerTop, spinnerRight, spinnerBottom;
  private JSpinner spinnerCenterX, spinnerCenterY;
  private JCheckBox cbAdjustCenter;

  public BamFilterTransformCenter(ConvertToBam parent)
  {
    super(parent, FilterName, FilterDesc);
  }

  @Override
  public PseudoBamFrameEntry process(PseudoBamFrameEntry frame) throws Exception
  {
    return applyEffect(frame);
  }

  @Override
  public String getConfiguration()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(spinnerLeft.getValue()).append(';');
    sb.append(spinnerTop.getValue()).append(';');
    sb.append(spinnerRight.getValue()).append(';');
    sb.append(spinnerBottom.getValue()).append(';');
    sb.append(cbAdjustCenter.isSelected()).append(';');
    sb.append(spinnerCenterX.getValue()).append(';');
    sb.append(spinnerCenterY.getValue());
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config)
  {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        Integer left = Integer.MIN_VALUE;
        Integer top = Integer.MIN_VALUE;
        Integer right = Integer.MIN_VALUE;
        Integer bottom = Integer.MIN_VALUE;
        Integer cx = Integer.MIN_VALUE;
        Integer cy = Integer.MIN_VALUE;
        boolean a = true;

        if (params.length > 0) {
          int min = ((Number)((SpinnerNumberModel)spinnerLeft.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerLeft.getModel()).getMaximum()).intValue();
          left = decodeNumber(params[0], min, max, Integer.MIN_VALUE);
          if (left == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 1) {
          int min = ((Number)((SpinnerNumberModel)spinnerTop.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerTop.getModel()).getMaximum()).intValue();
          top = decodeNumber(params[1], min, max, Integer.MIN_VALUE);
          if (top == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 2) {
          int min = ((Number)((SpinnerNumberModel)spinnerRight.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerRight.getModel()).getMaximum()).intValue();
          right = decodeNumber(params[2], min, max, Integer.MIN_VALUE);
          if (right == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 3) {
          int min = ((Number)((SpinnerNumberModel)spinnerBottom.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerBottom.getModel()).getMaximum()).intValue();
          bottom = decodeNumber(params[3], min, max, Integer.MIN_VALUE);
          if (bottom == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 4) {
          if (params[4].equalsIgnoreCase("true")) {
            a = true;
          } else if (params[4].equalsIgnoreCase("false")) {
            a = false;
          } else {
            return false;
          }
        }

        if (params.length > 5) {
          int min = ((Number)((SpinnerNumberModel)spinnerCenterX.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerCenterX.getModel()).getMaximum()).intValue();
          cx = decodeNumber(params[5], min, max, Integer.MIN_VALUE);
          if (cx == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 6) {
          int min = ((Number)((SpinnerNumberModel)spinnerCenterY.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerCenterY.getModel()).getMaximum()).intValue();
          cy = decodeNumber(params[6], min, max, Integer.MIN_VALUE);
          if (cy == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (left != Integer.MIN_VALUE) {
          spinnerLeft.setValue(left);
        }
        if (top != Integer.MIN_VALUE) {
          spinnerTop.setValue(top);
        }
        if (right != Integer.MIN_VALUE) {
          spinnerRight.setValue(right);
        }
        if (bottom != Integer.MIN_VALUE) {
          spinnerBottom.setValue(bottom);
        }
        if (cx != Integer.MIN_VALUE) {
          spinnerCenterX.setValue(cx);
        }
        if (cy != Integer.MIN_VALUE) {
          spinnerCenterY.setValue(cy);
        }
        cbAdjustCenter.setSelected(a);
      }
      return true;
    }
    return false;
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry frame)
  {
    return applyEffect(frame);
  }

  @Override
  protected JPanel loadControls()
  {
    GridBagConstraints c = new GridBagConstraints();

    JLabel lTitle = new JLabel("Add extra space (in pixels)");
    JLabel lLeft = new JLabel("Left:");
    JLabel lTop = new JLabel("Top:");
    JLabel lRight = new JLabel("Right:");
    JLabel lBottom = new JLabel("Bottom:");
    spinnerLeft = new JSpinner(new SpinnerNumberModel(0, 0, 256, 1));
    spinnerLeft.addChangeListener(this);
    spinnerTop = new JSpinner(new SpinnerNumberModel(0, 0, 256, 1));
    spinnerTop.addChangeListener(this);
    spinnerRight = new JSpinner(new SpinnerNumberModel(0, 0, 256, 1));
    spinnerRight.addChangeListener(this);
    spinnerBottom = new JSpinner(new SpinnerNumberModel(0, 0, 256, 1));
    spinnerBottom.addChangeListener(this);

    JLabel lCenterX= new JLabel("Center X:");
    JLabel lCenterY= new JLabel("Center Y:");
    cbAdjustCenter = new JCheckBox("Auto-adjust center position", true);
    cbAdjustCenter.addActionListener(this);
    spinnerCenterX = new JSpinner(new SpinnerNumberModel(0, 0, 512, 1));
    spinnerCenterX.addChangeListener(this);
    spinnerCenterX.setEnabled(!cbAdjustCenter.isSelected());
    spinnerCenterY = new JSpinner(new SpinnerNumberModel(0, 0, 512, 1));
    spinnerCenterY.addChangeListener(this);
    spinnerCenterY.setEnabled(!cbAdjustCenter.isSelected());

    JPanel p1 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 4, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 4, 0), 0, 0);
    p1.add(lTitle, c);

    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p1.add(lLeft, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p1.add(spinnerLeft, c);

    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 16, 0, 0), 0, 0);
    p1.add(lTop, c);
    ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p1.add(spinnerTop, c);

    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p1.add(lRight, c);
    ViewerUtil.setGBC(c, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p1.add(spinnerRight, c);

    ViewerUtil.setGBC(c, 2, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 16, 0, 0), 0, 0);
    p1.add(lBottom, c);
    ViewerUtil.setGBC(c, 3, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p1.add(spinnerBottom, c);

    JPanel p2 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 4, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 0, 4, 0), 0, 0);
    p2.add(cbAdjustCenter, c);

    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p2.add(lCenterX, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p2.add(spinnerCenterX, c);

    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 16, 0, 0), 0, 0);
    p2.add(lCenterY, c);
    ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p2.add(spinnerCenterY, c);


    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p1, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                      GridBagConstraints.NONE, new Insets(16, 0, 0, 0), 0, 0);
    panel.add(p2, c);

    return panel;
  }

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == spinnerLeft || event.getSource() == spinnerTop ||
        event.getSource() == spinnerRight || event.getSource() == spinnerBottom ||
        event.getSource() == spinnerCenterX || event.getSource() == spinnerCenterY) {
      fireChangeListener();
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == cbAdjustCenter) {
      spinnerCenterX.setEnabled(!cbAdjustCenter.isSelected());
      spinnerCenterY.setEnabled(!cbAdjustCenter.isSelected());
      fireChangeListener();
    }
  }

//--------------------- End Interface ActionListener ---------------------

  /**
   * Returns the canvas dimension which fits every frame of the BAM.
   * X defines space left of center, Y defines space on top of center.
   */
  public static Rectangle getMaxRectangle(PseudoBamDecoder decoder)
  {
    Rectangle rect = new Rectangle();
    if (decoder != null) {
      int left = 0, right = 0, top = 0, bottom = 0;
      for (int idx = 0; idx < decoder.frameCount(); idx++) {
        PseudoBamFrameEntry frame = decoder.getFrameInfo(idx);
        left = Math.max(left, frame.getCenterX());
        right = Math.max(right, frame.getWidth() - frame.getCenterX());
        top = Math.max(top, frame.getCenterY());
        bottom = Math.max(bottom, frame.getHeight() - frame.getCenterY());
      }
      rect.x = left; rect.y = top;
      rect.width = left + right;
      rect.height = top + bottom;
    }
    return rect;
  }

  /** Returns a frame padded according to the specified rectangle parameter. */
  public static BufferedImage padFrame(PseudoBamFrameEntry entry, Rectangle canvasRect)
  {
    BufferedImage dstImage = entry.getFrame();
    if (entry != null && canvasRect != null && !canvasRect.isEmpty()) {
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      int transIndex = 0;
      IndexColorModel cm = null;
      if (entry.getFrame().getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte)entry.getFrame().getRaster().getDataBuffer()).getData();
        cm = (IndexColorModel)entry.getFrame().getColorModel();
        // fetching transparent palette entry (default: 0)
        if (cm.getTransparentPixel() >= 0) {
          transIndex = cm.getTransparentPixel();
        } else {
          int[] colors = new int[1 << cm.getPixelSize()];
          cm.getRGBs(colors);
          final int Green = 0x0000ff00;
          for (int i = 0; i < colors.length; i++) {
            if ((colors[i] & 0x00ffffff) == Green) {
              transIndex = i;
              break;
            }
          }
        }
      } else if (entry.getFrame().getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
        srcI = ((DataBufferInt)entry.getFrame().getRaster().getDataBuffer()).getData();
      } else {
        return null;
      }

      if (srcB != null) {
        // paletted image
        dstImage = new BufferedImage(canvasRect.width, canvasRect.height, BufferedImage.TYPE_BYTE_INDEXED, cm);
        dstB = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
        Arrays.fill(dstB, (byte)transIndex);
      } else {
        // truecolor image
        dstImage = new BufferedImage(canvasRect.width, canvasRect.height, entry.getFrame().getType());
        dstI = ((DataBufferInt)dstImage.getRaster().getDataBuffer()).getData();
        Arrays.fill(dstI, 0);
      }
      int posX = canvasRect.x - entry.getCenterX();
      int posY = canvasRect.y - entry.getCenterY();
      int srcOfs = 0;
      int dstOfs = posY * canvasRect.width + posX;
      for (int y = 0; y < entry.getHeight(); y++, srcOfs += entry.getWidth(), dstOfs += canvasRect.width) {
        if (srcB != null) {
          System.arraycopy(srcB, srcOfs, dstB, dstOfs, entry.getWidth());
        }
        if (srcI != null) {
          System.arraycopy(srcI, srcOfs, dstI, dstOfs, entry.getWidth());
        }
      }
    }
    return dstImage;
  }

  private PseudoBamFrameEntry applyEffect(PseudoBamFrameEntry entry)
  {
    if (getConverter() != null && entry != null) {
      // calculating canvas size and center
      Rectangle rect = getMaxRectangle(getConverter().getBamDecoder(ConvertToBam.BAM_ORIGINAL));
      if (rect.isEmpty()) {
        return entry;
      }

      // rendering frame to canvas
      if (!rect.isEmpty()) {
        int padLeft = ((Integer)spinnerLeft.getValue()).intValue();
        int padTop = ((Integer)spinnerTop.getValue()).intValue();
        int padRight = ((Integer)spinnerRight.getValue()).intValue();
        int padBottom = ((Integer)spinnerBottom.getValue()).intValue();
        rect.x += padLeft;
        rect.width += padLeft + padRight;
        rect.y += padTop;
        rect.height += padTop + padBottom;

        BufferedImage dstImage = padFrame(entry, rect);
        if (dstImage != null) {
          entry.setFrame(dstImage);
        } else {
          return entry;
        }

        // setting center position
        int cx = 0, cy = 0;
        if (cbAdjustCenter.isSelected()) {
          cx = rect.x;
          cy = rect.y;
        } else {
          cx = ((Integer)spinnerCenterX.getValue()).intValue();
          cy = ((Integer)spinnerCenterY.getValue()).intValue();
        }
        entry.setCenterX(cx);
        entry.setCenterY(cy);
      }
    }
    return entry;
  }
}
