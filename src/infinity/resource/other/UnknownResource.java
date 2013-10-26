// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.other;

import infinity.icon.Icons;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.BIFFResourceEntry;
import infinity.resource.key.ResourceEntry;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public final class UnknownResource implements Resource, ActionListener
{
  private final ResourceEntry entry;
  private JButton bexport;
  private JPanel panel;

  public UnknownResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    byte data[] = entry.getResourceData();
    if (data.length >= 8)
      System.out.println("Unknown resource: " + new String(data, 0, 4) + " - " + new String(data, 4, 4));
    if (entry instanceof BIFFResourceEntry)
      System.out.println("Type: " + ((BIFFResourceEntry)entry).getType());
    System.out.println("Size: " + data.length);
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bexport.setMnemonic('e');
    bexport.addActionListener(this);
    JLabel label = new JLabel("Unsupported file format", JLabel.CENTER);

    JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bpanel.add(bexport);

    panel = new JPanel(new BorderLayout());
    panel.add(label, BorderLayout.CENTER);
    panel.add(bpanel, BorderLayout.SOUTH);
    label.setBorder(BorderFactory.createLoweredBevelBorder());

    return panel;
  }

// --------------------- End Interface Viewable ---------------------
}

