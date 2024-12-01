// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.graphics;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Locale;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.infinity.gui.ButtonPanel;
import org.infinity.gui.RenderCanvas;
import org.infinity.icon.Icons;
import org.infinity.resource.Referenceable;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceSearcher;

public final class GraphicsResource implements Resource, Referenceable, ActionListener {
  private static final ButtonPanel.Control PROPERTIES = ButtonPanel.Control.CUSTOM_1;

  private final ResourceEntry entry;
  private final BmpDecoder decoder;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JPanel panel;

  public GraphicsResource(ResourceEntry entry) throws Exception {
    this.entry = entry;
    this.decoder = BmpDecoder.loadBmp(entry);
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES) == event.getSource()) {
      searchReferences(panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(PROPERTIES) == event.getSource()) {
      showProperties();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry() {
    return entry;
  }

  // --------------------- End Interface Resource ---------------------

  // --------------------- Begin Interface Referenceable ---------------------

  @Override
  public boolean isReferenceable() {
    return true;
  }

  @Override
  public void searchReferences(Component parent) {
    new ReferenceSearcher(entry, parent);
  }

  // --------------------- End Interface Referenceable ---------------------

  // --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container) {
    RenderCanvas rcCanvas = new RenderCanvas(decoder.getImage());
    JScrollPane scroll = new JScrollPane(rcCanvas);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    ((JButton) buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    ((JButton) buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);

    JButton bProperties = new JButton("Properties...", Icons.ICON_EDIT_16.getIcon());
    bProperties.setMnemonic('p');
    bProperties.addActionListener(this);
    buttonPanel.addControl(bProperties, PROPERTIES);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());
    return panel;
  }

  // --------------------- End Interface Viewable ---------------------

  public BufferedImage getImage() {
    return decoder.getImage();
  }

  public Palette getPalette() {
    return decoder.getPalette();
  }

  public BmpDecoder.Info getInfo() {
    return decoder.getInfo();
  }

  private void showProperties() {
    // Width, Height, BitsPerPixel, Compression
    final Function<Integer, String> space = (i) -> new String(new char[i]).replace("\0", "&nbsp;");
    final String br = "<br/>";
    final String resName = entry.getResourceName().toUpperCase(Locale.ENGLISH);
    String sb = "<html><div style='font-family:monospace'>" +
        "Width:" + space.apply(7) + getInfo().getWidth() + br +
        "Height:" + space.apply(6) + getInfo().getHeight() + br +
        "Bits/Pixel:" + space.apply(2) + getInfo().getBitsPerPixel() + br +
        "</div></html>";

    JOptionPane.showMessageDialog(panel, sb, "Properties of " + resName, JOptionPane.INFORMATION_MESSAGE);
  }
}
