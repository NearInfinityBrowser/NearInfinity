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

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ButtonPopupWindow;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * Color filter: swaps individual color channels.
 */
public class BamFilterColorSwap extends BamFilterBaseColor implements ChangeListener, ActionListener {
  private static final String FILTER_NAME = "Swap color channels";
  private static final String FILTER_DESC = "This filter provides controls for swapping color "
      + "channels in any desired order.";

  /** Definition of supported swap combinations. */
  private enum SwapType {
    RBG("RBG", new int[] {  0, -8,  8}),
    GRB("GRB", new int[] { -8,  8,  0}),
    GBR("GBR", new int[] {-16,  8,  8}),
    BGR("BGR", new int[] {-16,  0, 16}),
    BRG("BRG", new int[] { -8, -8, 16}),
    ;

    private final String label;
    private final int[] shift;

    private SwapType(String label, int[] shift) {
      this.label = label;
      this.shift = shift;
    }

    public String getLabel() {
      return label;
    }

    /** Returns the number of bits to shift for each color channel to get the resulting order. */
    public int[] getShift() {
      return shift;
    }

    @Override
    public String toString() {
      return getLabel();
    }
  }

  private JComboBox<SwapType> cbSwapType;
  private ButtonPopupWindow bpwExclude;
  private BamFilterBaseColor.ExcludeColorsPanel pExcludeColors;

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterColorSwap(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public BufferedImage process(BufferedImage frame) throws Exception {
    return applyEffect(frame);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(int frameIndex, PseudoBamFrameEntry entry) {
    if (entry != null) {
      entry.setFrame(applyEffect(entry.getFrame()));
    }
    return entry;
  }

  @Override
  public void updateControls() {
    bpwExclude.setEnabled(getConverter().isBamV1Selected());
  }

  @Override
  public String getConfiguration() {
    return String.valueOf(cbSwapType.getSelectedIndex()) + ';' +
        encodeColorList(pExcludeColors.getSelectedIndices());
  }

  @Override
  public boolean setConfiguration(String config) {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.trim().split(";");
        int type = -1;
        int[] indices = null;

        // parsing configuration data
        if (params.length > 0) { // set swap type
          type = decodeNumber(params[0], 0, cbSwapType.getModel().getSize() - 1, -1);
          if (type == -1) {
            return false;
          }
        }
        if (params.length > 1) {
          indices = decodeColorList(params[1]);
          if (indices == null) {
            return false;
          }
        }

        // applying configuration data
        if (type >= 0) {
          cbSwapType.setSelectedIndex(type);
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
  protected JPanel loadControls() {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l1 = new JLabel("Exclude colors:");
    pExcludeColors = new BamFilterBaseColor.ExcludeColorsPanel(
        getConverter().getPaletteDialog().getPalette(getConverter().getPaletteDialog().getPaletteType()));
    pExcludeColors.addChangeListener(this);
    bpwExclude = new ButtonPopupWindow("Palette", Icons.ICON_ARROW_DOWN_15.getIcon(), pExcludeColors);
    bpwExclude.setIconTextGap(8);
    bpwExclude.addActionListener(this);
    bpwExclude.setEnabled(getConverter().isBamV1Selected());
    JPanel pExclude = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    pExclude.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    pExclude.add(bpwExclude, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    pExclude.add(new JPanel(), c);

    JLabel l = new JLabel("RGB =>");
    cbSwapType = new JComboBox<>(SwapType.values());
    cbSwapType.addActionListener(this);

    JPanel p = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    p.add(l, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 4, 0);
    p.add(cbSwapType, c);
    ViewerUtil.setGBC(c, 0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 0, 0, 0), 4, 0);
    p.add(pExclude, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(p, c);

    return panel;
  }

  // --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event) {
    if (event.getSource() == pExcludeColors) {
      fireChangeListener();
    }
  }

  // --------------------- End Interface ChangeListener ---------------------

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (event.getSource() == cbSwapType) {
      fireChangeListener();
    } else if (event.getSource() == bpwExclude) {
      pExcludeColors.updatePalette(
          getConverter().getPaletteDialog().getPalette(getConverter().getPaletteDialog().getPaletteType()));
    }
  }

  // --------------------- Begin Interface ActionListener ---------------------

  private BufferedImage applyEffect(BufferedImage srcImage) {
    if (srcImage != null) {
      int[] buffer;
      IndexColorModel cm = null;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        // paletted image
        cm = (IndexColorModel) srcImage.getColorModel();
        buffer = new int[1 << cm.getPixelSize()];
        cm.getRGBs(buffer);
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
        buffer = ((DataBufferInt) srcImage.getRaster().getDataBuffer()).getData();
      } else {
        buffer = new int[0];
      }

      // shift contains shift values for r, g, b
      final SwapType type = (SwapType) cbSwapType.getSelectedItem();
      final int[] shift = Objects.requireNonNull(type).getShift();

      for (int i = 0; i < buffer.length; i++) {
        if ((cm == null || !pExcludeColors.isSelectedIndex(i)) && (buffer[i] & 0xff000000) != 0) {
          // extracting color channels
          int ir = buffer[i] & 0x00ff0000;
          int ig = buffer[i] & 0x0000ff00;
          int ib = buffer[i] & 0x000000ff;

          // applying effect
          ir = (shift[0] < 0) ? (ir >>> -shift[0]) : (ir << shift[0]);
          ig = (shift[1] < 0) ? (ig >>> -shift[1]) : (ig << shift[1]);
          ib = (shift[2] < 0) ? (ib >>> -shift[2]) : (ib << shift[2]);

          buffer[i] = (buffer[i] & 0xff000000) | ir | ig | ib;
        }
      }

      if (cm != null) {
        // recreating paletted image
        IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), buffer.length, buffer, 0, cm.hasAlpha(),
            cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        BufferedImage dstImage = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        byte[] srcPixels = ((DataBufferByte) srcImage.getRaster().getDataBuffer()).getData();
        byte[] dstPixels = ((DataBufferByte) dstImage.getRaster().getDataBuffer()).getData();
        System.arraycopy(srcPixels, 0, dstPixels, 0, srcPixels.length);
        srcImage = dstImage;
        srcPixels = null;
        dstPixels = null;
      }
    }

    return srcImage;
  }
}
