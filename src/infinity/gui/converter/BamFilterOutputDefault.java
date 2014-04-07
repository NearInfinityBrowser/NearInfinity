// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui.converter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import infinity.resource.graphics.DxtEncoder;
import infinity.resource.graphics.PseudoBamDecoder;
import infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * The default BAM output filter.
 * @author argent77
 */
public class BamFilterOutputDefault extends BamFilterBaseOutput
{
  private static final String FilterName = "Default BAM output";
  private static final String FilterDesc = "This filter outputs the current BAM structure into a " +
                                           "single file. It is selected by default if no other " +
                                           "output filter has been specified.\n" +
                                           "Note: Output filters will always be processed last.";

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }


  public BamFilterOutputDefault(ConvertToBam parent)
  {
    super(parent, FilterName, FilterDesc);
  }

  @Override
  public boolean process(PseudoBamDecoder decoder) throws Exception
  {
    return applyEffect(decoder);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry entry)
  {
    // does not modify the source image
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


  private boolean applyEffect(PseudoBamDecoder decoder) throws Exception
  {
    if (getConverter() != null && decoder != null) {
      String outFileName = getConverter().getBamOutput();

      if (getConverter().isBamV1Selected()) {
        // convert to BAM v1
        int threshold = getConverter().getTransparencyThreshold();
        decoder.setOption(PseudoBamDecoder.OPTION_INT_RLEINDEX,
                          Integer.valueOf(getConverter().getPaletteDialog().getRleIndex()));
        decoder.setOption(PseudoBamDecoder.OPTION_BOOL_COMPRESSED,
                          Boolean.valueOf(getConverter().isBamV1Compressed()));
        int[] palette = retrievePalette(decoder);
        try {
          return decoder.exportBamV1(outFileName, palette, threshold,
                                     getConverter().getProgressMonitor(),
                                     getConverter().getProgressMonitorStage());
        } catch (Exception e) {
          e.printStackTrace();
          throw e;
        }
      } else {
        // convert to BAM v2
        DxtEncoder.DxtType dxtType = getConverter().getDxtType();
        int pvrzIndex = getConverter().getPvrzIndex();
        try {
          return decoder.exportBamV2(outFileName, dxtType, pvrzIndex, getConverter().getProgressMonitor(),
                                     getConverter().getProgressMonitorStage());
        } catch (Exception e) {
          e.printStackTrace();
          throw e;
        }
      }
    }
    return false;
  }
}
