// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.converter;

import infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.IndexColorModel;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Color filter: create negatives of each color.
 * @author argent77
 */
public class BamFilterColorInvert extends BamFilterBaseColor
{
  private static final String FilterName = "Invert colors";
  private static final String FilterDesc = "This filter inverts the colors of all frames.";

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  public BamFilterColorInvert(ConvertToBam parent)
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
  protected JPanel loadControls()
  {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l = new JLabel("No settings available.", SwingConstants.CENTER);

    JPanel panel = new JPanel(new GridBagLayout());
    ConvertToBam.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(l, c);

    return panel;
  }

  private BufferedImage applyEffect(BufferedImage srcImage)
  {
    if (srcImage != null) {
      int[] buffer;
      IndexColorModel cm = null;
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        // paletted image
        cm = (IndexColorModel)srcImage.getColorModel();
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
        buffer = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
      } else {
        buffer = new int[0];
      }

      for (int i = 0; i < buffer.length; i++) {
        if ((buffer[i] & 0xff000000) != 0) {
          buffer[i] = (buffer[i] & 0xff000000) | (~buffer[i] & 0x00ffffff);
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
