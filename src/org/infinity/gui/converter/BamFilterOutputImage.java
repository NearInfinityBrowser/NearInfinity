// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Locale;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.infinity.gui.ViewerUtil;
import org.infinity.resource.graphics.BamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder;
import org.infinity.resource.graphics.PseudoBamDecoder.PseudoBamFrameEntry;
import org.infinity.util.Misc;

/**
 * Output filter: Exports each frame as separate image.
 */
public class BamFilterOutputImage extends BamFilterBaseOutput implements ItemListener, ChangeListener
{
  private static final String FilterName = "Image output";
  private static final String FilterDesc = "This filter exports all frames of the BAM as individual images.\n" +
                                           "Notes: Output filters will always be processed last.";

  public static String getFilterName() { return FilterName; }
  public static String getFilterDesc() { return FilterDesc; }

  private JComboBox<String> cbImageType;
  private JSpinner spinnerDigits;
  private JCheckBox cbTransparent;

  public BamFilterOutputImage(ConvertToBam parent)
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
    sb.append(cbImageType.getSelectedIndex()).append(';');
    sb.append(spinnerDigits.getValue()).append(';');
    sb.append(cbTransparent.isSelected());
    return sb.toString();
  }

  @Override
  public boolean setConfiguration(String config)
  {
    if (config != null) {
      config = config.trim();
      if (!config.isEmpty()) {
        String[] params = config.split(";");
        Integer type = Integer.MIN_VALUE;
        Integer digits = Integer.MIN_VALUE;
        boolean t = true;

        if (params.length > 0) {
          type = decodeNumber(params[0], 0, 1, Integer.MIN_VALUE);
          if (type == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 1) {
          int min = ((Number)((SpinnerNumberModel)spinnerDigits.getModel()).getMinimum()).intValue();
          int max = ((Number)((SpinnerNumberModel)spinnerDigits.getModel()).getMaximum()).intValue();
          digits = decodeNumber(params[1], min, max, Integer.MIN_VALUE);
          if (digits == Integer.MIN_VALUE) {
            return false;
          }
        }

        if (params.length > 2) {
          if (params[2].equalsIgnoreCase("true")) {
            t = true;
          } else if (params[1].equalsIgnoreCase("false")) {
            t = false;
          } else {
            return false;
          }
        }

        if (type != Integer.MIN_VALUE) {
          cbImageType.setSelectedIndex(type);
        }
        if (digits != Integer.MIN_VALUE) {
          spinnerDigits.setValue(digits);
        }
        cbTransparent.setSelected(t);
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

    JLabel l1 = new JLabel("Image output format:");
    JLabel l2 = new JLabel("Digits for frame index:");
    cbImageType = new JComboBox<>(new String[]{"PNG", "BMP"});
    cbImageType.setEditable(false);
    cbImageType.setPreferredSize(new Dimension(cbImageType.getPreferredSize().width + 16, cbImageType.getPreferredSize().height));
    cbImageType.setSelectedIndex(0);
    cbImageType.addItemListener(this);
    spinnerDigits = new JSpinner(new SpinnerNumberModel(5, 1, 9, 1));
    spinnerDigits.getEditor().setPreferredSize(Misc.getPrototypeSize(spinnerDigits.getEditor(), "0000"));
    spinnerDigits.addChangeListener(this);
    cbTransparent = new JCheckBox("Transparent background (BAM v1 only)", true);

    JPanel panel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(l1, c);
    ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    panel.add(cbImageType, c);

    ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    panel.add(l2, c);
    ViewerUtil.setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    panel.add(spinnerDigits, c);

    ViewerUtil.setGBC(c, 0, 2, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                      GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    panel.add(cbTransparent, c);

    return panel;
  }

//--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == cbImageType) {
      cbTransparent.setEnabled(cbImageType.getSelectedIndex() == 0);
      fireChangeListener();
    }
  }

//--------------------- End Interface ItemListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == spinnerDigits) {
      fireChangeListener();
    }
  }

//--------------------- End Interface ChangeListener ---------------------

  private boolean applyEffect(PseudoBamDecoder decoder) throws Exception
  {
    if (getConverter() != null && decoder != null) {
      if (!getConverter().isBamV1Selected() && cbImageType.getSelectedIndex() == 1) {
        throw new Exception("BMP output format supports BAM v1 only.");
      }

      String fileExt = (cbImageType.getSelectedIndex() == 1) ? "BMP" : "PNG";
      String fileName = getConverter().getBamOutput().toString();
      int idx = fileName.lastIndexOf('.');
      if (idx >= 0) {
        if (Character.isLowerCase(fileName.charAt(idx+1))) {
          fileExt = fileExt.toLowerCase(Locale.ENGLISH);
        }
        fileName = fileName.substring(0, idx);
      }
      int digits = ((Integer)spinnerDigits.getValue()).intValue();
      String fmt = fileName + "%0" + digits + "d." + fileExt;
      boolean transparent = cbTransparent.isSelected() || fileExt.equalsIgnoreCase("bmp");

      BamDecoder.BamControl control = decoder.createControl();
      control.setMode(BamDecoder.BamControl.Mode.INDIVIDUAL);

      for (int frameIdx = 0; frameIdx < decoder.frameCount(); frameIdx++) {
        PseudoBamFrameEntry entry = decoder.getFrameInfo(frameIdx);
        File file = new File(String.format(fmt, frameIdx));

        BufferedImage image = entry.getFrame();
        try {
          // remove transparent palette entry
          if (!transparent && image.getType() == BufferedImage.TYPE_BYTE_INDEXED &&
              image.getColorModel() instanceof IndexColorModel) {
            IndexColorModel cm = (IndexColorModel)image.getColorModel();
            int index = cm.getTransparentPixel();
            if (index >= 0) {
              byte[] r = new byte[cm.getMapSize()];
              byte[] g = new byte[cm.getMapSize()];
              byte[] b = new byte[cm.getMapSize()];
              byte[] a = new byte[cm.getMapSize()];
              cm.getReds(r);
              cm.getGreens(g);
              cm.getBlues(b);
              cm.getAlphas(a);
              IndexColorModel cm2 = new IndexColorModel(cm.getPixelSize(), cm.getMapSize(), r, g, b, -1);
              String[] names = image.getPropertyNames();
              Hashtable<String, Object> table = null;
              if (names != null) {
                table = new Hashtable<>();
                for (String name: names) {
                  table.put(name, image.getProperty(name));
                }
              }
              image = new BufferedImage(cm2, image.getRaster(), image.isAlphaPremultiplied(), table);
            }
          }
          if (!ImageIO.write(image, fileExt, file)) {
            throw new IOException();
          }
        } catch (IOException e) {
          e.printStackTrace();
          throw new Exception("Could not export frame " + frameIdx);
        }
        image.flush();
        image = null;
      }
      return true;
    }
    return false;
  }

}
