// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.video;

import infinity.NearInfinity;
import infinity.gui.ButtonPanel;
import infinity.gui.ViewerUtil;
import infinity.gui.WindowBlocker;
import infinity.resource.Closeable;
import infinity.resource.Resource;
import infinity.resource.ResourceFactory;
import infinity.resource.ViewableContainer;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.search.ReferenceSearcher;
import infinity.util.io.FileNI;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

public final class WbmResource implements Resource, Closeable, ActionListener
{
  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JPanel panel;
  private JButton bPlayExternal;
  private File videoFile;
  private boolean isTempFile;

  public WbmResource(ResourceEntry entry)
  {
    this.entry = entry;
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FindReferences) == event.getSource()) {
      new ReferenceSearcher(entry, panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.ExportButton) == event.getSource()) {
      ResourceFactory.exportResource(entry,panel.getTopLevelAncestor());
    } else if (event.getSource() == bPlayExternal) {
      try {
        WindowBlocker.blockWindow(true);
        if (videoFile == null) {
            videoFile = getVideoFile();
        }
        if (videoFile != null) {
          try {
            Desktop.getDesktop().open(videoFile);
          } catch (Exception e) {
            bPlayExternal.setEnabled(false);
            WindowBlocker.blockWindow(false);
            JOptionPane.showMessageDialog(NearInfinity.getInstance(),
                                          "Error opening video or no application registered " +
                                              "to play back WBM (WebM) files.",
                                          "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
          }
        }
      } finally {
        WindowBlocker.blockWindow(false);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

// --------------------- Begin Interface Resource ---------------------

  @Override
  public ResourceEntry getResourceEntry()
  {
    return entry;
  }

// --------------------- End Interface Resource ---------------------

// --------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    if (videoFile != null && isTempFile) {
      videoFile.delete();
    }
  }

// --------------------- End Interface Closeable ---------------------

// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.FindReferences)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.ExportButton)).addActionListener(this);

    bPlayExternal = new JButton("Open in video player");
    bPlayExternal.addActionListener(this);
    JPanel subPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    subPanel.add(bPlayExternal, c);

    panel = new JPanel();
    panel.setLayout(new BorderLayout());
    panel.add(subPanel, BorderLayout.CENTER);
    panel.add(buttonPanel, BorderLayout.SOUTH);

    return panel;
  }

// --------------------- End Interface Viewable ---------------------

  // Returns a (temporary) file based on the current WBM resource
  private File getVideoFile()
  {
    File retVal = null;
    if (entry instanceof FileResourceEntry) {
      retVal = ((FileResourceEntry)entry).getActualFile();
      isTempFile = false;
    } else {
      String tempDir = System.getProperty("java.io.tmpdir");
      File outFile = new FileNI(tempDir, entry.getResourceName());
      if (outFile.isFile()) {
        retVal = outFile;
        retVal.deleteOnExit();
      } else if (!outFile.exists()) {
        InputStream is = null;
        FileOutputStream os = null;
        try {
          try {
            is = entry.getResourceDataAsStream();
            os = new FileOutputStream(outFile);
            byte[] buffer = new byte[65536];
            int size;
            while ((size = is.read(buffer)) > 0) {
              os.write(buffer, 0, size);
            }
          } finally {
            if (os != null) { os.close(); os = null; }
            if (is != null) { is.close(); is = null; }
          }
          retVal = outFile;
          retVal.deleteOnExit();
        } catch (Exception e) {
          retVal = null;
          e.printStackTrace();
        }
      }
      isTempFile = true;
    }
    return retVal;
  }
}
