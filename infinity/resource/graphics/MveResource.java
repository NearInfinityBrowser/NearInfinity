// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.graphics;

import infinity.NearInfinity;
import infinity.gui.WindowBlocker;
import infinity.icon.Icons;
import infinity.resource.*;
import infinity.resource.Closeable;
import infinity.resource.key.ResourceEntry;
import infinity.util.Filereader;
import infinity.util.Filewriter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;

public final class MveResource implements Resource, ActionListener, Closeable
{
  private final ResourceEntry entry;
  private final WindowBlocker blocker = new WindowBlocker(NearInfinity.getInstance());
  private File moviefile;
  private JButton bplay, bexport;
  private JPanel panel;

  public MveResource(ResourceEntry entry) throws Exception
  {
    this.entry = entry;
    blocker.setBlocked(true);
    try {
      moviefile = new File('_' + entry.toString() + ".exe");
      BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(moviefile));
      // Write stub
      BufferedInputStream stub = new BufferedInputStream(MveResource.class.getResourceAsStream("mve.stub"));
      Filewriter.writeBytes(bos, Filereader.readBytes(stub, 77312));
      stub.close();
      // Append movie
      int size = entry.getResourceInfo()[0];
      InputStream is = entry.getResourceDataAsStream();
      byte buffer[] = new byte[65536];
      int bytesread = is.read(buffer);
      while (size > 0) {
        bos.write(buffer, 0, bytesread);
        size -= bytesread;
        bytesread = is.read(buffer, 0, Math.min(size, buffer.length));
      }
      is.close();
      bos.close();
    } catch (Exception e) {
      blocker.setBlocked(false);
      throw e;
    }
    blocker.setBlocked(false);
  }

// --------------------- Begin Interface ActionListener ---------------------

  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bplay) {
      blocker.setBlocked(true);
      try {
        Process play = Runtime.getRuntime().exec(moviefile.getAbsolutePath());
        try {
          play.waitFor();
        } catch (InterruptedException e) {
        }
        blocker.setBlocked(false);
      } catch (IOException e) {
        blocker.setBlocked(false);
        JOptionPane.showMessageDialog(panel, "Error during playback", "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
      }
    }
    else if (event.getSource() == bexport)
      ResourceFactory.getInstance().exportResource(entry, panel.getTopLevelAncestor());
  }

// --------------------- End Interface ActionListener ---------------------


// --------------------- Begin Interface Closeable ---------------------

  public void close()
  {
    if (moviefile != null)
      if (!moviefile.delete())
        moviefile.deleteOnExit();
  }

// --------------------- End Interface Closeable ---------------------


// --------------------- Begin Interface Resource ---------------------

  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------


// --------------------- Begin Interface Viewable ---------------------

  public JComponent makeViewer(ViewableContainer container)
  {
    bplay = new JButton("Play movie", Icons.getIcon("Play16.gif"));
    bexport = new JButton("Export...", Icons.getIcon("Export16.gif"));
    bexport.setMnemonic('e');
    bexport.addActionListener(this);
    bplay.setMnemonic('p');
    bplay.addActionListener(this);

    JPanel cpanel = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints gbc = new GridBagConstraints();
    cpanel.setLayout(gbl);
    gbc.weightx = 1.0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.NONE;
    gbl.setConstraints(bplay, gbc);
    cpanel.add(bplay);

    JPanel bpanel = new JPanel();
    bpanel.setLayout(new FlowLayout());
    bpanel.add(bexport);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(cpanel, BorderLayout.CENTER);
    panel.add(bpanel, BorderLayout.SOUTH);
    cpanel.setBorder(BorderFactory.createLoweredBevelBorder());
    return panel;
  }

// --------------------- End Interface Viewable ---------------------
}

