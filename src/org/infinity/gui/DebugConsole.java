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
import java.util.Map;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.util.Misc;
import org.infinity.util.io.FileEx;

final class DebugConsole extends ChildFrame implements ActionListener
{
  private final JButton bClearConsole = new JButton("Clear", Icons.getIcon(Icons.ICON_NEW_16));
  private final JButton bSaveConsole = new JButton("Save...", Icons.getIcon(Icons.ICON_SAVE_16));
  private final JCheckBox cbExtraInfo = new JCheckBox("Print internal debug info");

  DebugConsole()
  {
    super("Debug Console");
    setIconImage(Icons.getIcon(Icons.ICON_PROPERTIES_16).getImage());

    bClearConsole.setMnemonic('c');
    bClearConsole.addActionListener(this);
    bSaveConsole.setMnemonic('s');
    bSaveConsole.addActionListener(this);
    cbExtraInfo.setToolTipText("Enable output of internal class information of current top-level window, resource and selected field in structure viewer.");
    cbExtraInfo.setSelected(BrowserMenuBar.getInstance().getShowDebugExtraInfo());
    cbExtraInfo.addActionListener(this);

    InfinityTextArea taconsole = NearInfinity.getConsoleText();
    taconsole.setHighlightCurrentLine(false);
    taconsole.setEditable(false);

    JPanel lowerpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    lowerpanel.add(bClearConsole);
    lowerpanel.add(bSaveConsole);
    lowerpanel.add(cbExtraInfo);

    JPanel pane = (JPanel)getContentPane();
    pane.setLayout(new BorderLayout());
    pane.add(new InfinityScrollPane(taconsole, false), BorderLayout.CENTER);
    pane.add(lowerpanel, BorderLayout.SOUTH);

    setSize(Misc.getScaledValue(450), Misc.getScaledValue(450));
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bClearConsole)
      NearInfinity.getConsoleText().setText("");
    else if (event.getSource() == bSaveConsole) {
      JFileChooser chooser = new JFileChooser(Profile.getGameRoot().toFile());
      chooser.setDialogTitle("Save console");
      chooser.setSelectedFile(new File(chooser.getCurrentDirectory(), "nidebuglog.txt"));
      if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        Path output = chooser.getSelectedFile().toPath();
        if (FileEx.create(output).exists()) {
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
          for (Map.Entry<Object, Object> entry : props.entrySet()) {
            bw.write(entry.getKey() + "=" + entry.getValue());
            bw.newLine();
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
    else if (event.getSource() == cbExtraInfo) {
      BrowserMenuBar.getInstance().setShowDebugExtraInfo(cbExtraInfo.isSelected());
    }
  }

// --------------------- End Interface ActionListener ---------------------

}
