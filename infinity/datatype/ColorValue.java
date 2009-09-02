// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.datatype;

import infinity.gui.StructViewer;
import infinity.icon.Icons;
import infinity.resource.AbstractStruct;
import infinity.resource.ResourceFactory;
import infinity.resource.graphics.BmpResource;
import infinity.util.Byteconvert;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.IOException;
import java.io.OutputStream;

public final class ColorValue extends Datatype implements Editable, ChangeListener, ActionListener
{
  private static BufferedImage image;
  private JLabel colors[], infolabel;
  private JSlider slider;
  private JTextField tfield;
  private int shownnumber;
  private int number;

  private static Color getColor(int index, int brightness)
  {
    if (image == null)
      initImage();
    if (index >= image.getHeight() || brightness >= image.getWidth())
      return null;
    return new Color(image.getRGB(brightness, index));
  }

  private static int getNumColors()
  {
    if (image == null)
      initImage();
    return image.getHeight();
  }

  private static int getRangeSize()
  {
    if (image == null)
      initImage();
    return image.getWidth();
  }

  private static void initImage()
  {
    try {
      if (ResourceFactory.getInstance().resourceExists("RANGES12.BMP"))
        image = new BmpResource(ResourceFactory.getInstance().getResourceEntry("RANGES12.BMP")).getImage();
      else
        image = new BmpResource(ResourceFactory.getInstance().getResourceEntry("MPALETTE.BMP")).getImage();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public ColorValue(byte buffer[], int offset, int length, String name)
  {
    super(offset, length, name);
    if (length == 4)
      number = Byteconvert.convertInt(buffer, offset);
    else if (length == 2)
      number = (int)Byteconvert.convertShort(buffer, offset);
    else if (length == 1)
      number = (int)Byteconvert.convertByte(buffer, offset);
    else
      throw new IllegalArgumentException();
    if (number < 0)
      number += 256;
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == tfield) {
      try {
        int newnumber = Integer.parseInt(tfield.getText());
        if (newnumber < Math.pow((double)2, (double)(8 * getSize())))
          shownnumber = newnumber;
        else
          tfield.setText(String.valueOf(shownnumber));
      } catch (NumberFormatException e) {
        tfield.setText(String.valueOf(shownnumber));
      }
      slider.setValue(shownnumber);
      setColors();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface ChangeListener ---------------------

  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == slider) {
      tfield.setText(String.valueOf(slider.getValue()));
      shownnumber = slider.getValue();
      setColors();
    }
  }

// --------------------- End Interface ChangeListener ---------------------


// --------------------- Begin Interface Editable ---------------------

  public JComponent edit(ActionListener container)
  {
    if (tfield == null) {
      tfield = new JTextField(4);
      tfield.setHorizontalAlignment(JTextField.CENTER);
      colors = new JLabel[getRangeSize()];
      for (int i = 0; i < colors.length; i++) {
        colors[i] = new JLabel("     ");
        colors[i].setOpaque(true);
      }
      tfield.addActionListener(this);
      slider = new JSlider(0, Math.max(number, getNumColors() - 1), number);
      slider.addChangeListener(this);
      slider.setMajorTickSpacing(25);
      slider.setMinorTickSpacing(5);
      slider.setPaintTicks(true);
      infolabel = new JLabel(" ", JLabel.CENTER);
    }
    tfield.setText(String.valueOf(number));
    shownnumber = number;
    setColors();

    JButton bUpdate = new JButton("Update value", Icons.getIcon("Refresh16.gif"));
    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    JLabel label = new JLabel(getName() + ": ");

    JPanel cpanel = new JPanel();
    cpanel.setLayout(new GridLayout(1, colors.length));
    for (final JLabel color : colors) 
      cpanel.add(color);

    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    JPanel panel = new JPanel(gbl);

    gbc.insets = new Insets(3, 0, 3, 3);
    gbl.setConstraints(label, gbc);
    panel.add(label);

    gbl.setConstraints(tfield, gbc);
    panel.add(tfield);

    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(slider, gbc);
    panel.add(slider);

    gbl.setConstraints(cpanel, gbc);
    panel.add(cpanel);

    gbl.setConstraints(infolabel, gbc);
    panel.add(infolabel);

    gbl.setConstraints(bUpdate, gbc);
    panel.add(bUpdate);

    return panel;
  }

  public void select()
  {
  }

  public boolean updateValue(AbstractStruct struct)
  {
    try {
      int newnumber = Integer.parseInt(tfield.getText());
      if (newnumber >= Math.pow((double)2, (double)(8 * getSize())))
        return false;
      number = newnumber;
      shownnumber = number;
      setColors();
      return true;
    } catch (NumberFormatException e) {
      e.printStackTrace();
    }
    return false;
  }

// --------------------- End Interface Editable ---------------------


// --------------------- Begin Interface Writeable ---------------------

  public void write(OutputStream os) throws IOException
  {
    super.writeInt(os, number);
  }

// --------------------- End Interface Writeable ---------------------

  public String toString()
  {
    return "Color index " + number;
  }

  private void setColors()
  {
    for (int i = 0; i < colors.length; i++) {
      Color c = getColor(shownnumber, i);
      if (c != null) {
        colors[i].setText("     ");
        colors[i].setBackground(c);
      }
      else {
        colors[i].setText(" ? ");
        colors[i].setBackground(Color.white);
      }
      colors[i].repaint();
    }
    if (shownnumber > 199 && ResourceFactory.getInstance().resourceExists("RANDCOLR.2DA"))
      infolabel.setText("Color drawn from RANDCOLR.2DA");
    else
      infolabel.setText("");
  }
}

