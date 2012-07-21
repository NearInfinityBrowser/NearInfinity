// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.sound;

import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.key.ResourceEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public final class AcmResource implements Resource, ActionListener, Closeable, Runnable
{
  private final ResourceEntry entry;
  private final SoundUtilities a2w = new SoundUtilities();
  private File wavfile;
  private JButton bplay, bstop, bconvertmono, bconvertstereo;
  private JPanel panel;

  public AcmResource(ResourceEntry entry)
  {
    this.entry = entry;
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bplay)
      new Thread(this).start();
    else if (event.getSource() == bstop) {
      bstop.setEnabled(false);
      a2w.stopPlay();
      bplay.setEnabled(true);
    }
    else if (event.getSource() == bconvertmono) {
      bplay.setEnabled(false);
      bconvertmono.setEnabled(false);
      bconvertstereo.setEnabled(false);
      new Thread(new Convert(true)).start();
    }
    else if (event.getSource() == bconvertstereo) {
      bplay.setEnabled(false);
      bconvertmono.setEnabled(false);
      bconvertstereo.setEnabled(false);
      new Thread(new Convert(false)).start();
    }
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close()
  {
    a2w.stopPlay();
    if (wavfile == null) return;
    if (!wavfile.delete())
      wavfile.deleteOnExit();
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Runnable ---------------------

  public void run()
  {
    bplay.setEnabled(false);
    bstop.setEnabled(true);
    bconvertmono.setEnabled(false);
    bconvertstereo.setEnabled(false);
    try {
      a2w.play(wavfile);
    } catch (Exception e) {
      JOptionPane.showMessageDialog(panel, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
      e.printStackTrace();
    }
    bstop.setEnabled(false);
    bplay.setEnabled(true);
    bconvertmono.setEnabled(true);
    bconvertstereo.setEnabled(true);
  }

// --------------------- End Interface Runnable ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    bplay = new JButton(Icons.getIcon("Play16.gif"));
    bstop = new JButton(Icons.getIcon("Stop16.gif"));
    bconvertmono = new JButton("Convert - mono", Icons.getIcon("Refresh16.gif"));
    bconvertstereo = new JButton("Convert - stereo", Icons.getIcon("Refresh16.gif"));
    bplay.addActionListener(this);
    bstop.addActionListener(this);
    bconvertmono.addActionListener(this);
    bconvertstereo.addActionListener(this);
    bplay.setEnabled(false);
    bstop.setEnabled(false);
    bplay.setToolTipText("You must convert file before playback is possible");

    JPanel bpanel = new JPanel(new GridLayout(1, 2, 6, 0));
    bpanel.add(bplay);
    bpanel.add(bstop);

    JPanel buttonpanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    buttonpanel.setLayout(gbl);

    gbc.insets = new Insets(3, 3, 3, 3);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridwidth = GridBagConstraints.REMAINDER;
    gbl.setConstraints(bpanel, gbc);
    buttonpanel.add(bpanel);

    gbl.setConstraints(bconvertmono, gbc);
    buttonpanel.add(bconvertmono);

    gbl.setConstraints(bconvertstereo, gbc);
    buttonpanel.add(bconvertstereo);

    panel = new JPanel(new BorderLayout());
    panel.add(buttonpanel, BorderLayout.CENTER);

    return panel;
  }

// --------------------- End Interface Viewable ---------------------


// -------------------------- INNER CLASSES --------------------------

  private final class Convert implements Runnable
  {
    private final boolean isMono;

    private Convert(boolean isMono)
    {
      this.isMono = isMono;
    }

    public void run()
    {
      try {
        if (wavfile != null)
          wavfile.delete();
        wavfile = SoundUtilities.convert(entry.getResourceData(), 0, entry.toString(), isMono);
        bplay.setEnabled(true);
        bplay.setToolTipText(null);
      } catch (Exception e) {
        JOptionPane.showMessageDialog(panel, "Error during conversion", "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
      bconvertmono.setEnabled(true);
      bconvertstereo.setEnabled(true);
    }
  }
}

