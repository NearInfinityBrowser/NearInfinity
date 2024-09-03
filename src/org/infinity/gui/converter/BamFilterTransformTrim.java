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
import java.util.Arrays;
import java.util.EnumMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * Transform filter: automatically trims excess space from each frame.
 */
public class BamFilterTransformTrim extends BamFilterBaseTransform implements ActionListener, ChangeListener {
  private static final String FILTER_NAME = "Trim BAM frames";
  private static final String FILTER_DESC = "This filter attempts to remove unused space around each "
                                            + "BAM frame. Center positions will be adjusted accordingly.";

  private enum Edge {
    Top("Top"),
    Bottom("Bottom"),
    Left("Left"),
    Right("Right"),
    ;

    private final String label;

    private Edge(String label) {
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

  private EnumMap<Edge, JCheckBox> cbEdges;
  private JSpinner spinnerMargin;
  private JCheckBox cbAdjustCenter;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterTransformTrim(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public PseudoBamFrameEntry process(PseudoBamFrameEntry entry) throws Exception {
    return applyEffect(entry);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry entry) {
    return applyEffect(entry);
  }

  @Override
  public String getConfiguration() {
    return String.valueOf(cbEdges.get(Edge.Top).isSelected()) + ';' +
        cbEdges.get(Edge.Left).isSelected() + ';' +
        cbEdges.get(Edge.Bottom).isSelected() + ';' +
        cbEdges.get(Edge.Right).isSelected() + ';' +
        ((SpinnerNumberModel) spinnerMargin.getModel()).getNumber().intValue() + ';' +
        cbAdjustCenter.isSelected();
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        boolean t = true, l = true, b = true, r = true;
        int margin = Integer.MIN_VALUE;
        boolean a = true;

        if (params.length > 0) {
          if (params[0].equalsIgnoreCase("true")) {
            t = true;
          } else if (params[0].equalsIgnoreCase("false")) {
            t = false;
          } else {
            return false;
          }
        }
        if (params.length > 1) {
          if (params[1].equalsIgnoreCase("true")) {
            l = true;
          } else if (params[1].equalsIgnoreCase("false")) {
            l = false;
          } else {
            return false;
          }
        }
        if (params.length > 2) {
          if (params[2].equalsIgnoreCase("true")) {
            b = true;
          } else if (params[2].equalsIgnoreCase("false")) {
            b = false;
          } else {
            return false;
          }
        }
        if (params.length > 3) {
          if (params[3].equalsIgnoreCase("true")) {
            r = true;
          } else if (params[3].equalsIgnoreCase("false")) {
            r = false;
          } else {
            return false;
          }
        }
        if (params.length > 4) {
          int min = ((Number) ((SpinnerNumberModel) spinnerMargin.getModel()).getMinimum()).intValue();
          int max = ((Number) ((SpinnerNumberModel) spinnerMargin.getModel()).getMaximum()).intValue();
          margin = decodeNumber(params[4], min, max, Integer.MIN_VALUE);
          if (margin == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 5) {
          if (params[5].equalsIgnoreCase("true")) {
            a = true;
          } else if (params[5].equalsIgnoreCase("false")) {
            a = false;
          } else {
            return false;
          }
        }

        cbEdges.get(Edge.Top).setSelected(t);
        cbEdges.get(Edge.Left).setSelected(l);
        cbEdges.get(Edge.Bottom).setSelected(b);
        cbEdges.get(Edge.Right).setSelected(r);
        if (margin != Integer.MIN_VALUE) {
          spinnerMargin.setValue(margin);
        }
        cbAdjustCenter.setSelected(a);
      }
      return true;
    }
    return false;
  }

  @Override
  protected JPanel loadControls() {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l1 = new JLabel("Edges:");
    JLabel l2 = new JLabel("Margin:");
    JLabel l3 = new JLabel("pixels");

    cbEdges = new EnumMap<>(Edge.class);
    for (final Edge edge : Edge.values()) {
      final JCheckBox cb = new JCheckBox(edge.getLabel(), true);
      cb.addActionListener(this);
      cbEdges.put(edge, cb);
    }

    spinnerMargin = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
    spinnerMargin.addChangeListener(this);

    cbAdjustCenter = new JCheckBox("Adjust center position", true);
    cbAdjustCenter.addActionListener(this);

    JPanel p1 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    p1.add(cbEdges.get(Edge.Top), c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    p1.add(cbEdges.get(Edge.Left), c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    p1.add(cbEdges.get(Edge.Bottom), c);
    ViewerUtil.setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    p1.add(cbEdges.get(Edge.Right), c);

    JPanel p2 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 8, 0, 0), 0, 0);
    p2.add(spinnerMargin, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    p2.add(l3, c);

    JPanel pMain = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    pMain.add(p1, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    pMain.add(l2, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    pMain.add(p2, c);
    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(12, 0, 0, 0), 0, 0);
    pMain.add(new JPanel(), c);
    ViewerUtil.setGBC(c, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(12, 4, 0, 0), 0, 0);
    pMain.add(cbAdjustCenter, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(pMain, c);

    return panel;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == cbAdjustCenter) {
      fireChangeListener();
    } else {
      for (final JCheckBox cb : cbEdges.values()) {
        if (cb == event.getSource()) {
          fireChangeListener();
          return;
        }
      }
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == spinnerMargin) {
      fireChangeListener();
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  private PseudoBamFrameEntry applyEffect(PseudoBamFrameEntry entry) {
    if (entry != null && entry.getFrame() != null) {
      int width = entry.getFrame().getWidth();
      int height = entry.getFrame().getHeight();
      BufferedImage dstImage = null;
      int newWidth, newHeight;
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      int transIndex = 0;
      IndexColorModel cm = null;
      if (entry.getFrame().getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte) entry.getFrame().getRaster().getDataBuffer()).getData();
        cm = (IndexColorModel) entry.getFrame().getColorModel();
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
        srcI = ((DataBufferInt) entry.getFrame().getRaster().getDataBuffer()).getData();
      } else {
        return entry;
      }

      // calculating the properties of the resulting image
      int left = 0, right = width - 1, top = 0, bottom = height - 1;
      boolean edgeLeft = !cbEdges.get(Edge.Left).isSelected();
      boolean edgeRight = !cbEdges.get(Edge.Right).isSelected();
      boolean edgeTop = !cbEdges.get(Edge.Top).isSelected();
      boolean edgeBottom = !cbEdges.get(Edge.Bottom).isSelected();
      while ((left < right || top < bottom) && (!edgeLeft || !edgeRight || !edgeTop || !edgeBottom)) {
        int ofs, step;
        // checking top edge
        if (cbEdges.get(Edge.Top).isSelected() && !edgeTop) {
          ofs = top * width;
          step = 1;
          for (int x = 0; x < width; x++, ofs += step) {
            if (srcB != null) {
              if ((srcB[ofs] & 0xff) != transIndex) {
                edgeTop = true;
                break;
              }
            } else {
              if ((srcI[ofs] & 0xff000000) != 0) {
                edgeTop = true;
                break;
              }
            }
          }
        }

        // checking bottom edge
        if (cbEdges.get(Edge.Bottom).isSelected() && !edgeBottom) {
          ofs = bottom * width;
          step = 1;
          for (int x = 0; x < width; x++, ofs += step) {
            if (srcB != null) {
              if ((srcB[ofs] & 0xff) != transIndex) {
                edgeBottom = true;
                break;
              }
            } else {
              if ((srcI[ofs] & 0xff000000) != 0) {
                edgeBottom = true;
                break;
              }
            }
          }
        }

        // checking left edge
        if (cbEdges.get(Edge.Left).isSelected() && !edgeLeft) {
          ofs = left;
          step = width;
          for (int y = 0; y < height; y++, ofs += step) {
            if (srcB != null) {
              if ((srcB[ofs] & 0xff) != transIndex) {
                edgeLeft = true;
                break;
              }
            } else {
              if ((srcI[ofs] & 0xff000000) != 0) {
                edgeLeft = true;
                break;
              }
            }
          }
        }

        // checking right edge
        if (cbEdges.get(Edge.Right).isSelected() && !edgeRight) {
          ofs = right;
          step = width;
          for (int y = 0; y < height; y++, ofs += step) {
            if (srcB != null) {
              if ((srcB[ofs] & 0xff) != transIndex) {
                edgeRight = true;
                break;
              }
            } else {
              if ((srcI[ofs] & 0xff000000) != 0) {
                edgeRight = true;
                break;
              }
            }
          }
        }

        if (!edgeLeft) {
          left++;
        }
        if (!edgeRight) {
          right--;
        }
        if (!edgeTop) {
          top++;
        }
        if (!edgeBottom) {
          bottom--;
        }
      }

      // preventing empty frames
      if (right < left) {
        left = right = 0;
      }
      if (bottom < top) {
        top = bottom = 0;
      }

      // creating new image
      int margin = ((Integer) spinnerMargin.getValue());
      int dstX = 0;
      int dstY = 0;
      newWidth = right - left + 1;
      newHeight = bottom - top + 1;
      if (cbEdges.get(Edge.Left).isSelected()) {
        newWidth += margin;
        dstX += margin;
      }
      if (cbEdges.get(Edge.Right).isSelected()) {
        newWidth += margin;
      }
      if (cbEdges.get(Edge.Top).isSelected()) {
        newHeight += margin;
        dstY += margin;
      }
      if (cbEdges.get(Edge.Bottom).isSelected()) {
        newHeight += margin;
      }
      if (srcB != null) {
        // paletted image
        dstImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_INDEXED, cm);
        dstB = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();
        Arrays.fill(dstB, (byte) transIndex);
      } else {
        // truecolor image
        dstImage = new BufferedImage(newWidth, newHeight, entry.getFrame().getType());
        dstI = ((DataBufferInt) dstImage.getRaster().getDataBuffer()).getData();
        Arrays.fill(dstI, 0);
      }
      int srcOfs = top * width + left;
      int dstOfs = dstY * newWidth + dstX;
      for (int y = 0; y < bottom - top + 1; y++, srcOfs += width, dstOfs += newWidth) {
        if (srcB != null) {
          System.arraycopy(srcB, srcOfs, dstB, dstOfs, right - left + 1);
        }
        if (srcI != null) {
          System.arraycopy(srcI, srcOfs, dstI, dstOfs, right - left + 1);
        }
      }
      entry.setFrame(dstImage);

      // updating center information
      if (cbAdjustCenter.isSelected()) {
        int centerX = entry.getCenterX() - left + dstX;
        int centerY = entry.getCenterY() - top + dstY;
        entry.setCenterX(centerX);
        entry.setCenterY(centerY);
      }
    }

    return entry;
  }
}
