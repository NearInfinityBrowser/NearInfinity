// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.video;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.infinity.NearInfinity;
import org.infinity.gui.ButtonPanel;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.resource.Closeable;
import org.infinity.resource.Referenceable;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.ViewableContainer;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.search.ReferenceSearcher;
import org.infinity.util.FileDeletionHook;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

public final class WbmResource implements Resource, Closeable, Referenceable, ActionListener
{
  private final ResourceEntry entry;
  private final ButtonPanel buttonPanel = new ButtonPanel();

  private JPanel panel;
  private JButton bPlayExternal;
  private Path videoFile;
  private boolean isTempFile;

  public WbmResource(ResourceEntry entry)
  {
    this.entry = entry;
  }

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (buttonPanel.getControlByType(ButtonPanel.Control.FIND_REFERENCES) == event.getSource()) {
      searchReferences(panel.getTopLevelAncestor());
    } else if (buttonPanel.getControlByType(ButtonPanel.Control.EXPORT_BUTTON) == event.getSource()) {
      ResourceFactory.exportResource(entry,panel.getTopLevelAncestor());
    } else if (event.getSource() == bPlayExternal) {
      try {
        WindowBlocker.blockWindow(true);
        if (videoFile == null) {
            videoFile = getVideoFile();
        }
        if (videoFile != null) {
          try {
            Desktop.getDesktop().open(videoFile.toFile());
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
    try {
      // first attempt to delete temporary video file
      if (videoFile != null && FileEx.create(videoFile).isFile() && isTempFile) {
        Files.delete(videoFile);
      }
    } catch (Exception e) {
    }
  }

// --------------------- End Interface Closeable ---------------------

//--------------------- Begin Interface Referenceable ---------------------

  @Override
  public boolean isReferenceable()
  {
    return true;
  }

  @Override
  public void searchReferences(Component parent)
  {
    new ReferenceSearcher(entry, parent);
  }

//--------------------- End Interface Referenceable ---------------------

// --------------------- Begin Interface Viewable ---------------------

  @Override
  public JComponent makeViewer(ViewableContainer container)
  {
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.FIND_REFERENCES)).addActionListener(this);
    ((JButton)buttonPanel.addControl(ButtonPanel.Control.EXPORT_BUTTON)).addActionListener(this);

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
  private Path getVideoFile()
  {
    Path retVal = null;
    if (entry instanceof FileResourceEntry &&
        FileManager.isDefaultFileSystem(((FileResourceEntry)entry).getActualPath())) {
      retVal = ((FileResourceEntry)entry).getActualPath();
      isTempFile = false;
    } else {
      String fileBase = entry.getResourceRef();
      String fileExt = entry.getExtension();
      if (fileExt.isEmpty())
        fileExt = "wbm";
      try {
        Path outFile = Files.createTempFile(fileBase + "-", "." + fileExt);
        if (FileEx.create(outFile).isFile()) {
          try (InputStream is = entry.getResourceDataAsStream()) {
            try (OutputStream os = StreamUtils.getOutputStream(outFile, true)) {
              byte[] buffer = new byte[8192];
              int size;
              while ((size = is.read(buffer)) > 0) {
                os.write(buffer, 0, size);
              }
            }
            retVal = outFile;
          }
        } else {
          throw new IOException("Could not create temp file");
        }
        isTempFile = true;
        FileDeletionHook.getInstance().registerFile(retVal);
      } catch (Exception e) {
        e.printStackTrace();
        retVal = null;
      }
    }
    return retVal;
  }
}
