// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.Dimension;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.DxtEncoder;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.Misc;
import org.infinity.util.io.FileManager;

/**
 * Output filter: split BAM and output each part into a separate file.
 */
public class BamFilterOutputSplitted extends BamFilterBaseOutput
    implements ActionListener, ChangeListener
{
  private static final String FilterName = "Splitted BAM output";
  private static final String FilterDesc = "This filter allows you to split the BAM into multiple " +
                                           "parts and output each one into a separate BAM file.\n" +
                                           "Note: Output filters will always be processed last.";

  private static final int MaxSplits = 7;   // max. supported number of splits

  private JSpinner spinnerSplitX, spinnerSplitY, spinnerSuffixStart, spinnerSuffixStep;
  private JCheckBox cbSplitAuto;
  private JComboBox<String> cbSuffixDigits;

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  public BamFilterOutputSplitted(ConvertToBam parent)
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
  public String getConfiguration()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(spinnerSplitX.getValue()).append(';');
    sb.append(spinnerSplitY.getValue()).append(';');
    sb.append(cbSplitAuto.isSelected()).append(';');
    sb.append(cbSuffixDigits.getSelectedIndex()).append(';');
    sb.append(spinnerSuffixStart.getValue()).append(';');
    sb.append(spinnerSuffixStep.getValue());
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config)
  {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        Integer splitX = Integer.MIN_VALUE;
        Integer splitY = Integer.MIN_VALUE;
        boolean auto = true;
        int digits = -1;
        Integer start = Integer.MIN_VALUE;
        Integer step = Integer.MIN_VALUE;

        if (params.length > 0) {
          int min = ((Number)((SpinnerNumberModel)spinnerSplitX.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerSplitX.getModel()).getMaximum()).intValue();
          splitX = decodeNumber(params[0], min, max, Integer.MIN_VALUE);
          if (splitX == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 1) {
          int min = ((Number)((SpinnerNumberModel)spinnerSplitY.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerSplitY.getModel()).getMaximum()).intValue();
          splitY = decodeNumber(params[1], min, max, Integer.MIN_VALUE);
          if (splitY == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 2) {
          if (params[2].equalsIgnoreCase("true")) {
            auto = true;
          } else if (params[2].equalsIgnoreCase("false")) {
            auto = false;
          } else {
            return false;
          }
        }
        if (params.length > 3) {
          digits = Misc.toNumber(params[3], -1);
          if (digits < 0 || digits >= cbSuffixDigits.getModel().getSize()) {
            return false;
          }
        }
        if (params.length > 4) {
          int min = ((Number)((SpinnerNumberModel)spinnerSuffixStart.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerSuffixStart.getModel()).getMaximum()).intValue();
          start = decodeNumber(params[4], min, max, Integer.MIN_VALUE);
          if (start == Integer.MIN_VALUE) {
            return false;
          }
        }
        if (params.length > 5) {
          int min = ((Number)((SpinnerNumberModel)spinnerSuffixStep.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerSuffixStep.getModel()).getMaximum()).intValue();
          step = decodeNumber(params[5], min, max, Integer.MIN_VALUE);
          if (step == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (splitX != Integer.MIN_VALUE) {
          spinnerSplitX.setValue(splitX);
        }
        if (splitY != Integer.MIN_VALUE) {
          spinnerSplitY.setValue(splitY);
        }
        cbSplitAuto.setSelected(auto);
        if (digits >= 0) {
          cbSuffixDigits.setSelectedIndex(digits);
        }
        if (start != Integer.MIN_VALUE) {
          spinnerSuffixStart.setValue(start);
        }
        if (step != Integer.MIN_VALUE) {
          spinnerSuffixStep.setValue(step);
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

    JLabel l1 = new JLabel("Split");
    JLabel l2 = new JLabel("x horizontally and");
    JLabel l3 = new JLabel("x vertically.");
    JLabel l4 = new JLabel("Output filename suffix:");
    JLabel l5 = new JLabel("Digits:");
    JLabel l6 = new JLabel("Start at:");
    JLabel l7 = new JLabel("Step by:");
    spinnerSplitX = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    ((SpinnerNumberModel)spinnerSplitX.getModel()).setMaximum(Integer.valueOf(MaxSplits));
    spinnerSplitX.addChangeListener(this);
    spinnerSplitY = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
    ((SpinnerNumberModel)spinnerSplitY.getModel()).setMaximum(Integer.valueOf(MaxSplits));
    spinnerSplitY.addChangeListener(this);
    cbSplitAuto = new JCheckBox("Split automatically", true);
    cbSplitAuto.addActionListener(this);

    String[] items = new String[7];
    for (int i = 0; i < items.length; i++) {
      items[i] = String.format("%d", i+1);
    }
    cbSuffixDigits = new JComboBox<>(items);
    cbSuffixDigits.setSelectedIndex(1);
    cbSuffixDigits.addActionListener(this);
    spinnerSuffixStart = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));
    ((SpinnerNumberModel)spinnerSuffixStart.getModel()).setMaximum(Integer.valueOf(100000));
    spinnerSuffixStart.addChangeListener(this);
    spinnerSuffixStep = new JSpinner(new SpinnerNumberModel(1, 1, 1000, 1));
    ((SpinnerNumberModel)spinnerSuffixStep.getModel()).setMaximum(Integer.valueOf(10000));
    spinnerSuffixStep.addChangeListener(this);

    JPanel p1 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p1.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    p1.add(spinnerSplitX, c);
    ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    p1.add(l2, c);
    ViewerUtil.setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    p1.add(spinnerSplitY, c);
    ViewerUtil.setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    p1.add(l3, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p1.add(new JPanel(), c);
    ViewerUtil.setGBC(c, 1, 1, 4, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p1.add(cbSplitAuto, c);

    JPanel p2 = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 6, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    p2.add(l4, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 0, 0, 0), 0, 0);
    p2.add(l5, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 8, 0);
    p2.add(cbSuffixDigits, c);
    ViewerUtil.setGBC(c, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 12, 0, 0), 0, 0);
    p2.add(l6, c);
    ViewerUtil.setGBC(c, 3, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p2.add(spinnerSuffixStart, c);
    ViewerUtil.setGBC(c, 4, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(4, 12, 0, 0), 0, 0);
    p2.add(l7, c);
    ViewerUtil.setGBC(c, 5, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    p2.add(spinnerSuffixStep, c);

    JPanel pMain = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(p1, c);
    ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pMain.add(p2, c);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(pMain, c);

    updateAutoSplit();

    return panel;
  }

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == cbSplitAuto) {
      updateAutoSplit();
      fireChangeListener();
    } else if (event.getSource() == cbSuffixDigits) {
      fireChangeListener();
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == spinnerSplitX || event.getSource() == spinnerSplitY ||
        event.getSource() == spinnerSuffixStart || event.getSource() == spinnerSuffixStep) {
      fireChangeListener();
    }
  }

//--------------------- End Interface ChangeListener ---------------------


  private void updateAutoSplit()
  {
    boolean b = cbSplitAuto.isSelected();
    spinnerSplitX.setEnabled(!b);
    spinnerSplitY.setEnabled(!b);
  }

  private boolean applyEffect(PseudoBamDecoder decoder) throws Exception
  {
    if (getConverter() != null && decoder != null) {
      // finding largest dimension
      Dimension maxDim = new Dimension(0, 0);
      for (int i = 0; i < decoder.frameCount(); i++) {
        int w = decoder.getFrameInfo(i).getWidth();
        int h = decoder.getFrameInfo(i).getHeight();
        if (w > maxDim.width) maxDim.width = w;
        if (h > maxDim.height) maxDim.height = h;
      }

      // getting number of segments per dimension
      int segmentsX, segmentsY;
      if (cbSplitAuto.isSelected()) {
        segmentsX = (maxDim.width < 256) ? 1 : 0;
        segmentsY = (maxDim.height < 256) ? 1 : 0;
        for (int i = 2; i <= MaxSplits; i++) {
          if (segmentsX == 0 && maxDim.width <= 255*i) {
            segmentsX = i;
          }
          if (segmentsY == 0 && maxDim.height <= 255*i) {
            segmentsY = i;
          }
          if(segmentsX > 0 && segmentsY > 0) {
            break;
          }
        }
      } else {
        segmentsX = ((Integer)spinnerSplitX.getValue()).intValue() + 1;
        segmentsY = ((Integer)spinnerSplitY.getValue()).intValue() + 1;
      }

      // calculating individual splits for each frame
      List<List<Rectangle>> listSegments = new ArrayList<>(decoder.frameCount());
      int segmentCount = segmentsX*segmentsY;
      for (int frameIdx = 0; frameIdx < decoder.frameCount(); frameIdx++) {
        listSegments.add(new ArrayList<Rectangle>(segmentCount));
        final double fract = 0.499999;    // fractions of .5 or less will be rounded down!
        int curHeight = decoder.getFrameInfo(frameIdx).getHeight(), y = 0;
        for (int curSegY = segmentsY; curSegY > 0; curSegY--) {
          double dh = (double)curHeight / (double)curSegY;
          int h = (int)(dh + fract);
          curHeight -= h;
          int curWidth = decoder.getFrameInfo(frameIdx).getWidth(), x = 0;
          for (int curSegX = segmentsX; curSegX > 0; curSegX--) {
            double dw = (double)curWidth / (double)curSegX;
            int w = (int)(dw + fract);
            curWidth -= w;
            // store current segment as Rectangle structure into list
            listSegments.get(frameIdx).add(new Rectangle(x, y, w, h));
            x += w;
          }
          y += h;
        }
      }

      // creating a format string for BAM output filenames
      String bamFileName = getConverter().getBamOutput().toString();
      String ext = "BAM";
      int suffixStart = ((Integer)spinnerSuffixStart.getValue()).intValue();
      int suffixStep = ((Integer)spinnerSuffixStep.getValue()).intValue();
      int idx = bamFileName.lastIndexOf('.');
      if (idx >= 0) {
        ext = bamFileName.substring(idx+1);
        bamFileName = bamFileName.substring(0, idx);
      }
      String fmtBamFileName = String.format("%1$s%%1$0%2$dd.%3$s", bamFileName,
                                            cbSuffixDigits.getSelectedIndex() + 1, ext);

      // creating BamDecoder instances for each individual segment
      PseudoBamDecoder segmentDecoder = new PseudoBamDecoder();
      // adding global custom options
      String[] options = decoder.getOptionNames();
      for (int i = 0; i < options.length; i++) {
        segmentDecoder.setOption(options[i], decoder.getOption(options[i]));
      }
      // for each segment...
      for (int segIdx = 0; segIdx < segmentCount; segIdx++) {
        // creating segmented frames list
        List<PseudoBamFrameEntry> framesList = new ArrayList<>(decoder.getFramesList().size());
        for (int i = 0; i < listSegments.size(); i++) {
          framesList.add(createFrameSegment(decoder.getFramesList().get(i), listSegments.get(i).get(segIdx)));
        }
        segmentDecoder.setFramesList(framesList);

        // attaching cycles list
        segmentDecoder.setCyclesList(decoder.getCyclesList());

        // converting segmented BAM structure
        int suffix = suffixStart + segIdx * suffixStep;
        if (!convertBam(FileManager.resolve(String.format(fmtBamFileName, suffix)), segmentDecoder)) {
          throw new Exception(String.format("Error converting segment %d/%d", segIdx + 1, segmentCount));
        }

        // resetting decoder
        segmentDecoder.setCyclesList(null);
        segmentDecoder.setFramesList(null);
      }

      return true;
    }
    return false;
  }


  // Creates a new FrameEntry based on the specified original entry and the segment rectangle
  private PseudoBamFrameEntry createFrameSegment(PseudoBamFrameEntry entry, Rectangle rect)
  {
    PseudoBamFrameEntry retVal = null;
    if (entry != null && rect != null) {
      // preparations
      BufferedImage srcImage = entry.getFrame();
      BufferedImage dstImage = null;
      byte[] srcB = null, dstB = null;
      int[] srcI = null, dstI = null;
      Dimension dstDim = new Dimension(rect.width, rect.height);
      if (dstDim.width == 0 || dstDim.height == 0) {
        dstDim.width = dstDim.height = 1;
      }
      if (srcImage.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
        srcB = ((DataBufferByte)srcImage.getRaster().getDataBuffer()).getData();
        IndexColorModel cm1 = (IndexColorModel)srcImage.getColorModel();
        int[] colors = new int[1 << cm1.getPixelSize()];
        cm1.getRGBs(colors);
        IndexColorModel cm2 = new IndexColorModel(cm1.getPixelSize(), colors.length, colors, 0,
                                                  cm1.hasAlpha(), cm1.getTransparentPixel(),
                                                  DataBuffer.TYPE_BYTE);
        dstImage = new BufferedImage(dstDim.width, dstDim.height, BufferedImage.TYPE_BYTE_INDEXED, cm2);
        dstB = ((DataBufferByte)dstImage.getRaster().getDataBuffer()).getData();
      } else {
        srcI = ((DataBufferInt)srcImage.getRaster().getDataBuffer()).getData();
        dstImage = new BufferedImage(dstDim.width, dstDim.height, srcImage.getType());
        dstI = ((DataBufferInt)dstImage.getRaster().getDataBuffer()).getData();
      }

      // copying segment
      if (rect.width > 0 && rect.height > 0) {
        int srcOfs = rect.y*srcImage.getWidth() + rect.x;
        int dstOfs = 0;
        for (int y = 0; y < rect.height; y++, srcOfs += srcImage.getWidth(), dstOfs += dstImage.getWidth()) {
          if (srcB != null) {
            System.arraycopy(srcB, srcOfs, dstB, dstOfs, rect.width);
          }
          if (srcI != null) {
            System.arraycopy(srcI, srcOfs, dstI, dstOfs, rect.width);
          }
        }
      } else {
        dstB[0] = 0;
      }

      // creating new FrameEntry structure
      retVal = new PseudoBamFrameEntry(dstImage, entry.getCenterX() - rect.x, entry.getCenterY() - rect.y);
    }
    return retVal;
  }


  // Exports the BAM specified by "decoder" into the filename "outFileName" using global settings
  private boolean convertBam(Path outFileName, PseudoBamDecoder decoder) throws Exception
  {
    if (getConverter() != null && outFileName != null && decoder != null) {
      if (getConverter().isBamV1Selected()) {
        // convert to BAM v1
        decoder.setOption(PseudoBamDecoder.OPTION_INT_RLEINDEX,
                          Integer.valueOf(getConverter().getPaletteDialog().getRleIndex()));
        decoder.setOption(PseudoBamDecoder.OPTION_BOOL_COMPRESSED,
                          Boolean.valueOf(getConverter().isBamV1Compressed()));
        try {
          return decoder.exportBamV1(outFileName, getConverter().getProgressMonitor(),
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
