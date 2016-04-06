// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;

final class DebugConsole extends ChildFrame implements ActionListener
{
  private final JButton bclearconsole = new JButton("Clear", Icons.getIcon(Icons.ICON_NEW_16));
  private final JButton bsaveconsole = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));

  DebugConsole()
  {
    super("Debug Console");
    setIconImage(Icons.getIcon(Icons.ICON_PROPERTIES_16).getImage());

    bclearconsole.setMnemonic('c');
    bclearconsole.addActionListener(this);
    bsaveconsole.setMnemonic('s');
    bsaveconsole.addActionListener(this);

    InfinityTextArea taconsole = NearInfinity.getConsoleText();
    taconsole.setHighlightCurrentLine(false);
    taconsole.setEditable(false);
    taconsole.setFont(BrowserMenuBar.getInstance().getScriptFont());

    JPanel lowerpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    lowerpanel.add(bclearconsole);
    lowerpanel.add(bsaveconsole);

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(new InfinityScrollPane(taconsole, false), BorderLayout.CENTER);
    pane.add(lowerpanel, BorderLayout.SOUTH);

    setSize(450, 450);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bclearconsole)
      NearInfinity.getConsoleText().setText("");
    else if (event.getSource() == bsaveconsole) {
      JFileChooser chooser = new JFileChooser(Profile.getGameRoot().toFile());
      chooser.setDialogTitle("Save console");
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "nidebuglog.txt"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        Path output = chooser.getSelectedFile().toPath();
        if (Files.exists(output)) {
          String options[] = {"Overwrite", "Cancel"};
          if (JOptionPane.showOptionDialog(this, output + " exists. Overwrite?", "Save debug log", JOptionPane.YES_NO_OPTION,
                                           JOptionPane.WARNING_MESSAGE, null, options, options[0]) != 0)
            return;
        }
        try (BufferedWriter bw = Files.newBufferedWriter(output)) {
          bw.write("Near Infinity Debug Log"); bw.newLine();
          bw.write(BrowserMenuBar.VERSION); bw.newLine();
          bw.write((String)Profile.getProperty(Profile.Key.GET_GAME_TITLE)); bw.newLine();
          bw.newLine();
          bw.write(NearInfinity.getConsoleText().getText()); bw.newLine();
          bw.newLine();
          Properties props = System.getProperties();
          for (Object key : props.keySet()) {
            bw.write(key + "=" + props.get(key)); bw.newLine();
          }
          JOptionPane.showMessageDialog(this, "Console saved to " + output, "Save complete",
                                        JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
          JOptionPane.showMessageDialog(this, "Error while saving " + output, "Error",
                                        JOptionPane.ERROR_MESSAGE);
          e.printStackTrace();
        }
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

}

