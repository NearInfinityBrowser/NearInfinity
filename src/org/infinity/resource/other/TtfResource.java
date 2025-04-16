// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.other;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Logger;

public class TtfResource implements Resource, DocumentListener, ActionListener {
  private static final ButtonPanel.Control PROPERTIES = ButtonPanel.Control.CUSTOM_1;

  private static final String DEFAULT_STRING = "The quick brown fox jumps over the lazy dog.  1234567890";

  private static final int[] FONT_SIZE = { 12, 18, 24, 36, 48, 60, 72, 96, 128, 192 };

  private static String CURRENT_STRING = DEFAULT_STRING;

  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JTextField tfInput;
  private JTextPane tpDisplay;
  private JPanel panel;
  private Font font;

  public TtfResource(ResourceEntry entry) throws Exception {
    this.entry = entry;

    try (InputStream is = this.entry.getResourceDataAsStream()) {
      font = Font.createFont(Font.TRUETYPE_FONT, is);
    } catch (Exception e) {
      font = null;
      Logger.error(e);
      throw new Exception("Invalid TTF resource");
    }

    if (font != null) {
      GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
    }
  }

  // --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry() {
    return entry;
  }

  // --------------------- End Interface Resource ---------------------

  // --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container) {
    JLabel lInput = new JLabel("Sample text:");
    tfInput = new JTextField();
    if (!CURRENT_STRING.equals(DEFAULT_STRING)) {
      tfInput.setText(CURRENT_STRING);
    }
    tfInput.getDocument().addDocumentListener(this);

    JPanel pInput = new JPanel(new GridBagLayout());
    GridBagConstraints gbc = new GridBagConstraints();
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(8, 8, 8, 0), 0, 0);
    pInput.add(lInput, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 8, 8, 8), 0, 0);
    pInput.add(tfInput, gbc);

    tpDisplay = new JTextPane(new DefaultStyledDocument());
    tpDisplay.setMargin(new Insets(8, 8, 8, 8));
    tpDisplay.setEditable(false);
    tpDisplay.setBackground(UIManager.getColor("Label.background"));
    tpDisplay.setSelectionColor(tpDisplay.getBackground());
    updateText(tfInput.getText());
    JScrollPane scroll = new JScrollPane(tpDisplay);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);
    scroll.setBorder(BorderFactory.createLoweredBevelBorder());

    ((JButton) buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);
    JButton bProperties = new JButton("Properties...", Icons.ICON_EDIT_16.getIcon());
    bProperties.addActionListener(this);
    buttonPanel.addControl(bProperties, PROPERTIES);

    panel = new JPanel(new BorderLayout());
    panel.add(pInput, BorderLayout.NORTH);
    panel.add(scroll, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    buttonPanel.addControl(0, ViewerUtil.createViewerSyncButton(panel, getResourceEntry()));

    return panel;
  }

  // --------------------- End Interface Viewable ---------------------

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event) {
    if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(PROPERTIES) == event.getSource()) {
      showProperties();
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event) {
    try {
      updateText(event.getDocument().getText(0, event.getDocument().getLength()));
    } catch (BadLocationException e) {
      Logger.trace(e);
    }
  }

  @Override
  public void removeUpdate(DocumentEvent event) {
    try {
      updateText(event.getDocument().getText(0, event.getDocument().getLength()));
    } catch (BadLocationException e) {
      Logger.trace(e);
    }
  }

  @Override
  public void changedUpdate(DocumentEvent event) {
    try {
      updateText(event.getDocument().getText(0, event.getDocument().getLength()));
    } catch (BadLocationException e) {
      Logger.trace(e);
    }
  }

  // --------------------- Begin Interface DocumentListener ---------------------

  // Updates text display
  private void updateText(String text) {
    if (text == null || text.isEmpty()) {
      text = DEFAULT_STRING;
    }
    CURRENT_STRING = text;

    DefaultStyledDocument doc = (DefaultStyledDocument) tpDisplay.getDocument();
    if (doc != null) {
      // removing old content
      if (doc.getLength() > 0) {
        try {
          doc.remove(0, doc.getLength());
        } catch (BadLocationException e) {
          Logger.error(e);
        }
      }

      // adding current text in different sizes
      int pos = 0;
      for (int element : FONT_SIZE) {
        String label = element + "    ";
        SimpleAttributeSet as = new SimpleAttributeSet();
        StyleConstants.setFontFamily(as, "SansSerif");
        StyleConstants.setFontSize(as, FONT_SIZE[0]);
        try {
          doc.insertString(pos, label, as);
          pos += label.length();
        } catch (BadLocationException e) {
          Logger.error(e);
        }

        as = new SimpleAttributeSet();
        StyleConstants.setFontFamily(as, font.getFamily());
        StyleConstants.setFontSize(as, element);
        try {
          doc.insertString(pos, CURRENT_STRING, as);
          pos += CURRENT_STRING.length();
          doc.insertString(pos, "\n\n", as);
          pos += 2;
        } catch (BadLocationException e) {
          Logger.error(e);
        }
      }
      tpDisplay.setCaretPosition(0);
    }
  }

  // Shows message box about basic resource properties
  private void showProperties() {
    String resName = entry.getResourceName().toUpperCase(Locale.ENGLISH);
    String fontName = font.getFontName();
    String fontFamily = font.getFamily();

    String sb = "<html><table align=\"left\" border=\"0\">" +
        "<tr><td>Font name:</td><td>" + fontName + "</td></tr>" +
        "<tr><td>Font family:</td><td>" + fontFamily + "</td></tr>" +
        "</table></html>";
    JOptionPane.showMessageDialog(panel, sb, "Properties of " + resName, JOptionPane.INFORMATION_MESSAGE);
  }
}
