// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.io.File;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.GifSequenceWriter;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamControl;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.Misc;

/**
 * Output filter: Exports frame or cycles into a GIF file.
 */
public class BamFilterOutputGif extends BamFilterBaseOutput implements ChangeListener, ActionListener
{
  private static final String FilterName = "GIF output (BAM v1 only)";
  private static final String FilterDesc = "This filter exports all cycles of the BAM into separate GIF files. " +
                                           "Each cycle adds the suffix \"_cycleX\" to the filename, " +
                                           "where X is the cycle index.\n" +
                                           "Notes: Output filters will always be processed last. Supports " +
                                           "legacy BAM (v1) only. All frames must be of same dimension " +
                                           "(use \"Center BAM frames\" filter if needed).";

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  private JSpinner spinnerFPS;
  private JCheckBox cbLoopAnim;

  public BamFilterOutputGif(ConvertToBam parent)
  {
    super(parent, FilterName, FilterDesc);
  }

  @Override
  public boolean process(PseudoBamDecoder decoder) throws Exception
  {
    return applyEffect(decoder);
  }

  @Override
  public String getConfiguration()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(spinnerFPS.getValue()).append(';');
    sb.append(cbLoopAnim.isSelected());
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config)
  {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        Integer fps = Integer.MIN_VALUE;
        boolean loop = true;

        if (params.length > 0) {
          int min = ((Number)((SpinnerNumberModel)spinnerFPS.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerFPS.getModel()).getMaximum()).intValue();
          fps = decodeNumber(params[0], min, max, Integer.MIN_VALUE);
          if (fps == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 1) {
          if (params[1].equalsIgnoreCase("true")) {
            loop = true;
          } else if (params[1].equalsIgnoreCase("false")) {
            loop = false;
          } else {
            return false;
          }
        }

        if (fps != Integer.MIN_VALUE) {
          spinnerFPS.setValue(fps);
        }
        cbLoopAnim.setSelected(loop);
      }
      return true;
    }
    return false;
  }

  @Override
  public PseudoBamFrameEntry updatePreview(PseudoBamFrameEntry frame)
  {
    return frame;
  }

  @Override
  protected JPanel loadControls()
  {
    GridBagConstraints c = new GridBagConstraints();

    JLabel l1 = new JLabel("Frames per second:");
    spinnerFPS = new JSpinner(new SpinnerNumberModel(15, 1, 60, 1));
    spinnerFPS.getEditor().setPreferredSize(Misc.getPrototypeSize(spinnerFPS.getEditor(), "0000"));
    spinnerFPS.addChangeListener(this);
    cbLoopAnim = new JCheckBox("Loop animation", true);
    cbLoopAnim.addActionListener(this);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    panel.add(spinnerFPS, c);

    ViewerUtil.setGBC(c, 0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel.add(cbLoopAnim, c);

    return panel;
  }

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == spinnerFPS) {
      fireChangeListener();
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == cbLoopAnim) {
      fireChangeListener();
    }
  }

//--------------------- End Interface ActionListener ---------------------

  private boolean applyEffect(PseudoBamDecoder decoder) throws Exception
  {
    if (getConverter() != null && decoder != null) {
      if (!getConverter().isBamV1Selected()) {
        throw new Exception("Only BAM V1 output supported.");
      }

      boolean canvasCheck = true;
      Dimension dim = new Dimension(decoder.getFrameInfo(0).getWidth(), decoder.getFrameInfo(0).getHeight());
      for (int idx = 0; idx < decoder.frameCount(); idx++) {
        canvasCheck &= (decoder.getFrameInfo(idx).getWidth() == dim.width);
        if (!canvasCheck) break;
        canvasCheck &= (decoder.getFrameInfo(idx).getHeight() == dim.height);
        if (!canvasCheck) break;
      }
      if (!canvasCheck) {
        throw new Exception("Width and height of all BAM frames must be identical.");
      }

      int fps = ((Integer)spinnerFPS.getValue()).intValue();
      boolean loop = cbLoopAnim.isSelected();
      String fileName = getConverter().getBamOutput().toString();
      String fileExt = ".GIF";
      int idx = fileName.lastIndexOf('.');
      if (idx >= 0) {
        if (Character.isLowerCase(fileName.charAt(idx+1))) {
          fileExt = ".gif";
        }
        fileName = fileName.substring(0, idx);
      }

      PseudoBamControl control = decoder.createControl();
      for (int idxCycle = 0; idxCycle < control.cycleCount(); idxCycle++) {
        control.cycleSet(idxCycle);
        if (control.cycleFrameCount() > 0) {
          try (ImageOutputStream output = new FileImageOutputStream(new File(fileName + "_cycle" + idxCycle + fileExt))) {
            control.cycleSetFrameIndex(0);
            BufferedImage image = (BufferedImage)control.cycleGetFrame();
            int transIndex = -1;
            ColorModel cm = image.getColorModel();
            if (cm instanceof IndexColorModel && ((IndexColorModel)cm).getTransparentPixel() >= 0) {
              transIndex = ((IndexColorModel)cm).getTransparentPixel();
            }
            try (GifSequenceWriter writer = new GifSequenceWriter(output, image.getType(), 1000 / fps, loop, transIndex)) {
              writer.writeToSequence(image);
              for (int idxFrames = 1; idxFrames < control.cycleFrameCount(); idxFrames++) {
                control.cycleSetFrameIndex(idxFrames);
                writer.writeToSequence((BufferedImage)control.cycleGetFrame());
              }
            }
          }
        }
      }
      control.cycleReset();
      return true;
    }
    return false;
  }
}
