// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * Output filter: combine all source frames to a single frame in the output BAM.
 */
public class BamFilterOutputCombine extends BamFilterBaseOutput {
  private static final String FILTER_NAME = "Single frame BAM output";
  private static final String FILTER_DESC = "This filter combines all available frames of the current "
                                            + "BAM into a single frame and cycle before it is written to disk.\n"
                                            + "It can be used to assemble paperdoll or description images "
                                            + "that have been split because of engine limitations.\n"
                                            + "Note: Output filters will always be processed last.";

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterOutputCombine(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public boolean process(PseudoBamDecoder decoder) throws Exception {
    return applyEffect(decoder);
  }

  @Override
  public String getConfiguration() {
    return "";
  }

  @Override
  public boolean setConfiguration(String config) {
    return true;
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry frame) {
    // does not modify the source image
    return frame;
  }

  @Override
  protected JPanel loadControls() {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l = new JLabel("No settings available.", SwingConstants.CENTER);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    panel.add(l, c);

    return panel;
  }

  private boolean applyEffect(PseudoBamDecoder decoder) throws Exception {
    // calculating canvas dimension relative to center (0,0)
    int x1 = 0, y1 = 0, x2 = 0, y2 = 0; // left, top, right, bottom coordinates
    int pixelType = -1;
    for (int i = 0; i < decoder.frameCount(); i++) {
      PseudoBamFrameEntry entry = decoder.getFrameInfo(i);
      int px = entry.getCenterX();
      int py = entry.getCenterY();
      int w = entry.getWidth();
      int h = entry.getHeight();
      x1 = Math.min(x1, (px < 0) ? px : -px);
      y1 = Math.min(y1, (py < 0) ? py : -py);
      x2 = Math.max(x2, w - px);
      y2 = Math.max(y2, h - py);
      if (pixelType < 0) {
        pixelType = entry.getFrame().getType();
      }
    }
    int w = x2 - x1;
    int h = y2 - y1;

    // rendering source frames to canvas
    BufferedImage image = null;
    if (pixelType == BufferedImage.TYPE_BYTE_INDEXED) {
      IndexColorModel cm = (IndexColorModel) decoder.getFrameInfo(0).getFrame().getColorModel();
      int[] colors = new int[1 << cm.getPixelSize()];
      cm.getRGBs(colors);
      IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), colors.length, colors, 0, cm.hasAlpha(),
          cm.getTransparentPixel(), DataBuffer.TYPE_BYTE);
      image = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_INDEXED, cm2);
    } else if (pixelType >= 0) {
      image = new BufferedImage(w, h, pixelType);
    }

    if (image == null) {
      return false;
    }

    Graphics2D g = image.createGraphics();
    try {
      g.setComposite(AlphaComposite.Src);
      for (int i = 0; i < decoder.frameCount(); i++) {
        PseudoBamFrameEntry entry = decoder.getFrameInfo(i);
        int px = entry.getCenterX();
        int py = entry.getCenterY();
        int shiftX = -x1 - px;
        int shiftY = -y1 - py;
        g.drawImage(entry.getFrame(), shiftX, shiftY, null);
      }
    } finally {
      g.dispose();
      g = null;
    }

    // generating output BAM
    PseudoBamDecoder newDecoder = new PseudoBamDecoder();
    newDecoder.frameAdd(image, new Point(-x1, -y1));
    newDecoder.createControl().cycleAdd(new int[] { 0 });

    // relay output operation to default output filter
    BamFilterOutputDefault defOutput = new BamFilterOutputDefault(getConverter());
    return defOutput.process(newDecoder);
  }
}
