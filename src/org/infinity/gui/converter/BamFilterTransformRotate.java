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
import java.util.Objects;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.Misc;

/**
 * Transform filter: rotates each frame by a specified amount.
 */
public class BamFilterTransformRotate extends BamFilterBaseTransform implements ActionListener {
  private static final String FILTER_NAME = "Rotate BAM frames";
  private static final String FILTER_DESC = "This filter allows you to rotate each BAM frame by a specified amount.";

  private enum Angle {
    Angle90("90°"),
    Angle180("180°"),
    Angle270("270°"),
    ;

    private final String label;

    Angle(String label) {
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

  private JRadioButton rbCW;
  private JRadioButton rbCCW;
  private JCheckBox cbAdjustCenter;
  private JComboBox<Angle> cbAngle;

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
        cbAngle.getSelectedIndex() + ';' +
        cbAdjustCenter.isSelected();
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        int orientation = -1;
        int angle = -1;
        boolean adjust = true;

        if (params.length > 0) {
          orientation = Misc.toNumber(params[0], -1);
          if (orientation < 0 || orientation > 1) {
            return false;
          }
        }
        if (params.length > 1) {
          angle = Misc.toNumber(params[1], -1);
          if (angle < 0 || angle >= cbAngle.getModel().getSize()) {
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

        if (orientation >= 0) {
          if (orientation == 0) {
            rbCW.setSelected(true);
          } else {
            rbCCW.setSelected(true);
          }
        }
        if (angle >= 0) {
          cbAngle.setSelectedIndex(angle);
        }
        cbAdjustCenter.setSelected(adjust);
      }
      return true;
    }
    return false;
  }

  @Override
  protected JPanel loadControls() {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l1 = new JLabel("Orientation:");
    JLabel l2 = new JLabel("Angle:");
    ButtonGroup bg = new ButtonGroup();
    rbCW = new JRadioButton("Clockwise", true);
    rbCW.addActionListener(this);
    rbCCW = new JRadioButton("Counter clockwise");
    rbCCW.addActionListener(this);
    bg.add(rbCW);
    bg.add(rbCCW);
    cbAngle = new JComboBox<>(Angle.values());
    cbAngle.addActionListener(this);
    cbAdjustCenter = new JCheckBox("Adjust center position", true);
    cbAdjustCenter.addActionListener(this);

    JPanel p = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    p.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    p.add(rbCW, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    p.add(rbCCW, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END, GridBagConstraints.NONE,
        new Insets(4, 0, 0, 0), 0, 0);
    p.add(l2, c);
    ViewerUtil.setGBC(c, 1, 1, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(4, 8, 0, 0), 0, 0);
    p.add(cbAngle, c);
    ViewerUtil.setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(12, 0, 0, 0), 0, 0);
    p.add(new JPanel(), c);
    ViewerUtil.setGBC(c, 1, 2, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(12, 4, 0, 0), 0, 0);
    p.add(cbAdjustCenter, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p, c);

    return panel;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == rbCW || event.getSource() == rbCCW || event.getSource() == cbAngle
        || event.getSource() == cbAdjustCenter) {
      fireChangeListener();
    }
  }

  // --------------------- Begin Interface ActionListener ---------------------

  private PseudoBamFrameEntry applyEffect(PseudoBamFrameEntry entry) {
    if (entry != null && entry.getFrame() != null) {
      int width = entry.getFrame().getWidth();
      int height = entry.getFrame().getHeight();
      BufferedImage dstImage;
      int newWidth, newHeight;
      switch ((Angle) Objects.requireNonNull(cbAngle.getSelectedItem())) {
        case Angle90:
        case Angle270:
          newWidth = height;
          newHeight = width;
          break;
        default:
          newWidth = width;
          newHeight = height;
      }
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      if (entry.getFrame().getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte) entry.getFrame().getRaster().getDataBuffer()).getData();
        IndexColorModel cm = (IndexColorModel) entry.getFrame().getColorModel();
        dstImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_BYTE_INDEXED, cm);
        dstB = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();
      } else if (entry.getFrame().getRaster().getDataBuffer().getDataType() == DataBuffer.TYPE_INT) {
        srcI = ((DataBufferInt) entry.getFrame().getRaster().getDataBuffer()).getData();
        dstImage = new BufferedImage(newWidth, newHeight, entry.getFrame().getType());
        dstI = ((DataBufferInt) dstImage.getRaster().getDataBuffer()).getData();
      } else {
        return entry;
      }

      // normalizing rotation for easier processing
      Angle angle = (Angle) cbAngle.getSelectedItem();
      if (rbCCW.isSelected()) {
        angle = Angle.values()[Angle.values().length - angle.ordinal() - 1];
      }
      // rotating each pixel
      int srcOfs = 0;
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++, srcOfs++) {
          int nx, ny;
          switch (angle) {
            case Angle90:
              nx = newWidth - y - 1;
              ny = x;
              break;
            case Angle180:
              nx = newWidth - x - 1;
              ny = newHeight - y - 1;
              break;
            case Angle270:
              nx = y;
              ny = newHeight - x - 1;
              break;
            default:
              nx = x;
              ny = y;
          }
          int dstOfs = ny * newWidth + nx;
          if (srcB != null) {
            dstB[dstOfs] = srcB[srcOfs];
          }
          if (srcI != null) {
            dstI[dstOfs] = srcI[srcOfs];
          }
        }
      }
      entry.setFrame(dstImage);

      // adjusting center
      if (cbAdjustCenter.isSelected()) {
        int cx = entry.getCenterX(), cy = entry.getCenterY();
        switch (angle) {
          case Angle90:
            cx = newWidth - entry.getCenterY() - 1;
            cy = entry.getCenterX();
            break;
          case Angle180:
            cx = newWidth - entry.getCenterX() - 1;
            cy = newHeight - entry.getCenterY() - 1;
            break;
          case Angle270:
            cx = entry.getCenterY();
            cy = newHeight - entry.getCenterX() - 1;
            break;
        }
        entry.setCenterX(cx);
        entry.setCenterY(cy);
      }
    }

    return entry;
  }
}
