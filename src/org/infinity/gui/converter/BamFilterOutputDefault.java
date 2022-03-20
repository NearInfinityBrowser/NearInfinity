// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2022 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.DxtEncoder;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;

/**
 * The default BAM output filter.
 */
public class BamFilterOutputDefault extends BamFilterBaseOutput {
  private static final String FILTER_NAME = "Default BAM output";
  private static final String FILTER_DESC = "This filter outputs the current BAM structure into a "
                                            + "single file. It is selected by default if no other "
                                            + "output filter has been specified.\n"
                                            + "Note: Output filters will always be processed last.";

  public static String getFilterName() {
    return FILTER_NAME;
  }

  public static String getFilterDesc() {
    return FILTER_DESC;
  }

  public BamFilterOutputDefault(ConvertToBam parent) {
    super(parent, FILTER_NAME, FILTER_DESC);
  }

  @Override
  public boolean process(PseudoBamDecoder decoder) throws Exception {
    return applyEffect(decoder);
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry entry) {
    // does not modify the source image
    return entry;
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
    if (getConverter() != null && decoder != null) {
      Path outFile = getConverter().getBamOutput();

      if (getConverter().isBamV1Selected()) {
        // convert to BAM v1
        decoder.setOption(PseudoBamDecoder.OPTION_INT_RLEINDEX,
            Integer.valueOf(getConverter().getPaletteDialog().getRleIndex()));
        decoder.setOption(PseudoBamDecoder.OPTION_BOOL_COMPRESSED, Boolean.valueOf(getConverter().isBamV1Compressed()));
        try {
          return decoder.exportBamV1(outFile, getConverter().getProgressMonitor(),
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
          return decoder.exportBamV2(outFile, dxtType, pvrzIndex, getConverter().getProgressMonitor(),
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
