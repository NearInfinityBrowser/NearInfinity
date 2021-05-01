// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.gui.ChildFrame;
import org.infinity.gui.ViewerUtil;
import org.infinity.gui.WindowBlocker;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.graphics.ColorConvert;
import org.infinity.util.SimpleListModel;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;
import org.infinity.util.io.StreamUtils;

public class ConvertToBmp extends ChildFrame
    implements ActionListener, FocusListener, PropertyChangeListener
{
  private static Path currentPath = Profile.getGameRoot();

  private SimpleListModel<String> modelInputFiles;
  private JList<String> listInputFiles;
  private JButton bAdd, bAddFolder, bRemove, bRemoveAll;
  private JTextField tfOutput;
  private JButton bOutput;
  private JButton bConvert, bCancel;
  private JCheckBox cbCloseOnExit, cbEnableAlpha, cbFixPremultipliedAlpha;
  private JComboBox<String> cbOverwrite;
  private SwingWorker<List<String>, Void> workerConvert;
  private WindowBlocker blocker;


  // Returns a list of supported graphics file formats
  private static FileNameExtensionFilter[] getGraphicsFilters()
  {
    FileNameExtensionFilter[] filters = new FileNameExtensionFilter[] {
        new FileNameExtensionFilter("Graphics files (*.bmp, *.png, *,jpg, *.jpeg)",
                                    "bmp", "png", "jpg", "jpeg"),
            new FileNameExtensionFilter("BMP files (*.bmp)", "bmp"),
            new FileNameExtensionFilter("PNG files (*.png)", "png"),
            new FileNameExtensionFilter("JPEG files (*.jpg, *.jpeg)", "jpg", "jpeg")
    };
    return filters;
  }

  // returns a selection of files
  private static Path[] getOpenFileName(Component parent, String title, Path rootPath,
                                        boolean selectMultiple,
                                        FileNameExtensionFilter[] filters, int filterIndex)
  {
    if (rootPath == null) {
      rootPath = currentPath;
    }
    Path file = FileManager.resolve(rootPath);
    JFileChooser fc = new JFileChooser(file.toFile());
    if (!FileEx.create(file).isDirectory()) {
        fc.setSelectedFile(file.toFile());
    }
    if (title == null) {
      title = selectMultiple ? "Select file(s)" : "Select file";
    }
    fc.setDialogTitle(title);
    fc.setDialogType(JFileChooser.OPEN_DIALOG);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fc.setMultiSelectionEnabled(selectMultiple);
    if (filters != null) {
      for (final FileNameExtensionFilter filter: filters) {
        fc.addChoosableFileFilter(filter);
      }
      if (filterIndex >= 0 && filterIndex < filters.length) {
        fc.setFileFilter(filters[filterIndex]);
      }
    }
    if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
      if (selectMultiple) {
        if (fc.getSelectedFiles().length > 0) {
          currentPath = fc.getSelectedFiles()[0].toPath().getParent();
        }
        File[] files = fc.getSelectedFiles();
        Path[] paths = new Path[files.length];
        for (int i = 0; i < files.length; i++) {
          paths[i] = files[i].toPath();
        }
        return paths;
      } else {
        file = fc.getSelectedFile().toPath();
        currentPath = file.getParent();
        return new Path[]{file.getFileName()};
      }
    } else {
      return null;
    }
  }

  // returns a path name
  private static Path getOpenPathName(Component parent, String title, Path rootPath)
  {
    if (rootPath == null) {
      rootPath = currentPath;
    }
    JFileChooser fc = new JFileChooser(rootPath.toFile());
    if (title == null) {
      title = "Select folder";
    }
    fc.setDialogTitle(title);
    fc.setDialogType(JFileChooser.OPEN_DIALOG);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
      currentPath = fc.getSelectedFile().toPath();
      return fc.getSelectedFile().toPath();
    } else {
      return null;
    }
  }


  public ConvertToBmp()
  {
    super("Convert to BMP", true);
    init();
  }

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    clear();
    return super.windowClosing(forced);
  }

//--------------------- End Class ChildFrame ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bConvert) {
      workerConvert = new SwingWorker<List<String>, Void>() {
        @Override
        public List<String> doInBackground()
        {
          return convert();
        }
      };
      blocker = new WindowBlocker(this);
      blocker.setBlocked(true);
      workerConvert.addPropertyChangeListener(this);
      workerConvert.execute();
    } else if (event.getSource() == bCancel) {
      setVisible(false);
    } else if (event.getSource() == bAdd) {
      inputAdd();
    } else if (event.getSource() == bAddFolder) {
      inputAddFolder();
    } else if (event.getSource() == bRemove) {
      inputRemove();
    } else if (event.getSource() == bRemoveAll) {
      inputRemoveAll();
    } else if (event.getSource() == bOutput) {
      setOutput();
    } else if (event.getSource() == cbEnableAlpha) {
      cbFixPremultipliedAlpha.setEnabled(cbEnableAlpha.isSelected());
    }
  }

//--------------------- End Interface ActionListener ---------------------

//--------------------- Begin Interface PropertyChangeListener ---------------------

  @Override
  public void propertyChange(PropertyChangeEvent event)
  {
    if (event.getSource() == workerConvert) {
      if ("state".equals(event.getPropertyName()) &&
          SwingWorker.StateValue.DONE == event.getNewValue()) {

        if (blocker != null) {
          blocker.setBlocked(false);
          blocker = null;
        }

        List<String> sl = null;
        try {
          sl = workerConvert.get();
        } catch (Exception e) {
          e.printStackTrace();
        }
        workerConvert = null;

        boolean isError = false;
        String s = null;
        if (sl != null && !sl.isEmpty()) {
          if (sl.get(0) != null) {
            s = sl.get(0);
          } else if (sl.size() > 1 && sl.get(1) != null) {
            s = sl.get(1);
            isError = true;
          }
        }
        if (s != null) {
          if (isError) {
            JOptionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE);
          } else {
            JOptionPane.showMessageDialog(this, s, "Information", JOptionPane.INFORMATION_MESSAGE);
            if (cbCloseOnExit.isSelected()) {
              hideWindow();
            } else {
              clear();
            }
          }
        } else {
          JOptionPane.showMessageDialog(this, "Unknown error!", "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

//--------------------- End Interface PropertyChangeListener ---------------------

//--------------------- Begin Interface FocusListener ---------------------

  @Override
  public void focusGained(FocusEvent event)
  {
    // nothing to do
  }

  @Override
  public void focusLost(FocusEvent event)
  {
    if (event.getSource() == tfOutput) {
//      bConvert.setEnabled(isReady());
    }
  }

//--------------------- End Interface FocusListener ---------------------


  private void init()
  {
    setIconImage(Icons.getImage(Icons.ICON_APPLICATION_16));
    GridBagConstraints c = new GridBagConstraints();

    bAdd = new JButton("Add...");
    bAdd.addActionListener(this);
    bAddFolder = new JButton("Add folder...");
    bAddFolder.addActionListener(this);
    JPanel pAdd = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pAdd.add(bAdd, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pAdd.add(bAddFolder, c);

    bRemove = new JButton("Remove");
    bRemove.addActionListener(this);
    bRemoveAll = new JButton("Remove all");
    bRemoveAll.addActionListener(this);
    JPanel pRemove = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pRemove.add(bRemove, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pRemove.add(bRemoveAll, c);

    JPanel pInputButtons = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pInputButtons.add(pAdd, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pInputButtons.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pInputButtons.add(pRemove, c);

    modelInputFiles = new SimpleListModel<>();
    listInputFiles = new JList<>(modelInputFiles);
    JScrollPane scroll = new JScrollPane(listInputFiles);
    JPanel pInputFrame = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pInputFrame.add(scroll, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pInputFrame.add(pInputButtons, c);

    JPanel pInput = new JPanel(new GridBagLayout());
    pInput.setBorder(BorderFactory.createTitledBorder("Input "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    pInput.add(pInputFrame, c);

    JLabel lOutput = new JLabel("Directory:");
    tfOutput = new JTextField();
    tfOutput.addFocusListener(this);
    bOutput = new JButton("...");
    bOutput.addActionListener(this);
    JLabel lOverwrite = new JLabel("Overwrite:");
    cbOverwrite = new JComboBox<>(new String[]{"Ask", "Replace", "Skip"});
    cbOverwrite.setSelectedIndex(1);
    cbEnableAlpha = new JCheckBox("Enable transparency support", true);
    cbEnableAlpha.setToolTipText("Activate to create bitmap files with alpha channel");
    cbEnableAlpha.addActionListener(this);
    cbFixPremultipliedAlpha = new JCheckBox("Fix premultiplied alpha", false);
    cbFixPremultipliedAlpha.setEnabled(cbEnableAlpha.isSelected());
    cbFixPremultipliedAlpha.setToolTipText("Activate if the resulting BMP image " +
                                           "differs from the source image");

    JPanel pOutputDir = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pOutputDir.add(lOutput, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pOutputDir.add(tfOutput, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pOutputDir.add(bOutput, c);

    JPanel pOutputOptions = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pOutputOptions.add(lOverwrite, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 4, 0);
    pOutputOptions.add(cbOverwrite, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pOutputOptions.add(cbEnableAlpha, c);
    c = ViewerUtil.setGBC(c, 0, 1, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pOutputOptions.add(new JPanel(), c);
    c = ViewerUtil.setGBC(c, 2, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(4, 16, 0, 0), 0, 0);
    pOutputOptions.add(cbFixPremultipliedAlpha, c);

    JPanel pOutputFrame = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pOutputFrame.add(pOutputDir, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pOutputFrame.add(pOutputOptions, c);

    JPanel pOutput = new JPanel(new GridBagLayout());
    pOutput.setBorder(BorderFactory.createTitledBorder("Output "));
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 4, 4, 4), 0, 0);
    pOutput.add(pOutputFrame, c);

    cbCloseOnExit = new JCheckBox("Close dialog after conversion", true);
    bConvert = new JButton("Start Conversion");
    bConvert.addActionListener(this);
    Insets i = bConvert.getInsets();
    bConvert.setMargin(new Insets(i.top + 2, i.left, i.bottom + 2, i.right));
    bCancel = new JButton("Cancel");
    bCancel.addActionListener(this);
    i = bCancel.getInsets();
    bCancel.setMargin(new Insets(i.top + 2, i.left, i.bottom + 2, i.right));
    JPanel pButtons = new JPanel(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(cbCloseOnExit, c);
    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bConvert, c);
    c = ViewerUtil.setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
                          GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bCancel, c);

    setLayout(new GridBagLayout());
    c = ViewerUtil.setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    add(pInput, c);
    c = ViewerUtil.setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 8), 0, 0);
    add(pOutput, c);
    c = ViewerUtil.setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                          GridBagConstraints.HORIZONTAL, new Insets(4, 8, 8, 8), 0, 0);
    add(pButtons, c);

    setPreferredSize(new Dimension(getPreferredSize().width+50, getPreferredSize().height + 50));
    setMinimumSize(getPreferredSize());
    pack();
    setLocationRelativeTo(getParent());
    updateStatus();
    setVisible(true);
  }

  private void clear()
  {
    inputRemoveAll();
    updateStatus();
  }

  private void hideWindow()
  {
    clear();
    setVisible(false);
  }

  private void updateStatus()
  {
    boolean enabled = (!modelInputFiles.isEmpty() && !tfOutput.getText().isEmpty());

    bConvert.setEnabled(enabled);
  }

  // checks for valid graphics file
  private boolean isValidInput(Path file)
  {
    boolean result = false;
    if (file != null) {
      try (ImageInputStream iis = ImageIO.createImageInputStream(file.toFile())) {
        final Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (readers.hasNext()) {
          result = true;
        }
        iis.close();
      } catch (Exception e) {
      }
    }
    return result;
  }

  private void inputAdd()
  {
    Path rootPath = null;
    if (!modelInputFiles.isEmpty()) {
      rootPath = FileManager.resolve(modelInputFiles.get(modelInputFiles.size() - 1));
    }
    Path[] files = getOpenFileName(this, "Choose file(s)", rootPath, true, getGraphicsFilters(), 0);
    if (files != null) {
      List<String> skippedFiles = new ArrayList<>();
      int idx = listInputFiles.getSelectedIndex() + 1;
      for (final Path file: files) {
        if (isValidInput(file)) {
          modelInputFiles.addElement(file.toString());
          idx++;
        } else {
          skippedFiles.add(file.toString());
        }
      }
      listInputFiles.setSelectedIndex(idx - 1);
      listInputFiles.requestFocus();
      updateStatus();

      if (!skippedFiles.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        if (skippedFiles.size() == 1) {
          sb.append(String.format("%d file has been skipped:\n", skippedFiles.size()));
        } else {
          sb.append(String.format("%d files have been skipped:\n", skippedFiles.size()));
        }
        for (int i = 0; i < Math.min(5, skippedFiles.size()); i++) {
          sb.append(String.format("  - %s\n", skippedFiles.get(i)));
        }
        if (skippedFiles.size() > 5) {
          sb.append("  - ...\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void inputAddFolder()
  {
    Path rootPath = null;
    if (!modelInputFiles.isEmpty()) {
      rootPath = FileManager.resolve(modelInputFiles.get(modelInputFiles.size() - 1));
    }
    Path path = getOpenPathName(this, "Choose folder", rootPath);
    if (path != null && FileEx.create(path).isDirectory()) {
      // adding all files in the directory
      FileNameExtensionFilter[] filters = getGraphicsFilters();
      List<String> skippedFiles = new ArrayList<>();
      int idx = listInputFiles.getSelectedIndex() + 1;
      try (DirectoryStream<Path> dstream = Files.newDirectoryStream(path)) {
        for (final Path file: dstream) {
          for (final FileNameExtensionFilter filter: filters) {
            if (FileEx.create(file).isFile() && filter.accept(file.toFile())) {
              if (isValidInput(file)) {
                modelInputFiles.addElement(file.toString());
                idx++;
              } else {
                skippedFiles.add(file.toString());
              }
              break;
            }
          }
        }
      } catch (IOException e) {
        JOptionPane.showMessageDialog(this, "Unable to read files from the specified folder.",
                                      "Error", JOptionPane.ERROR_MESSAGE);
        e.printStackTrace();
        return;
      }
      listInputFiles.setSelectedIndex(idx - 1);
      listInputFiles.requestFocus();
      updateStatus();

      if (!skippedFiles.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        if (skippedFiles.size() == 1) {
          sb.append(String.format("%d file has been skipped:\n", skippedFiles.size()));
        } else {
          sb.append(String.format("%d files have been skipped:\n", skippedFiles.size()));
        }
        for (int i = 0; i < Math.min(5, skippedFiles.size()); i++) {
          sb.append(String.format("  - %s\n", skippedFiles.get(i)));
        }
        if (skippedFiles.size() > 5) {
          sb.append("  - ...\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void inputRemove()
  {
    int curIdx = Integer.MAX_VALUE;
    int indices[] = listInputFiles.getSelectedIndices();
    if (indices != null && indices.length > 0) {
      for (int i = indices.length - 1; i >= 0; i--) {
        modelInputFiles.remove(indices[i]);
        if (indices[i] < curIdx) {
          curIdx = indices[i];
        }
      }
      curIdx = Math.min(modelInputFiles.size() - 1, curIdx);
      listInputFiles.setSelectedIndex(curIdx);
      listInputFiles.requestFocus();
      updateStatus();
    }
  }

  private void inputRemoveAll()
  {
    if (!modelInputFiles.isEmpty()) {
      modelInputFiles.clear();
      listInputFiles.requestFocus();
      updateStatus();
    }
  }

  private void setOutput()
  {
    Path rootPath = null;
    if (!tfOutput.getText().isEmpty()) {
      rootPath = FileManager.resolve(tfOutput.getText());
    }
    Path path = getOpenPathName(this, "Select output directory", rootPath);
    if (path != null) {
      tfOutput.setText(path.toString());
      updateStatus();
    }
  }

  private List<String> convert()
  {
    List<String> result = new ArrayList<>(2);
    final String progressMsg = "Converting file %d / %d";
    int progressIdx = 0, progressMax = modelInputFiles.size() + 1;
    ProgressMonitor progress = new ProgressMonitor(this, "Converting files...", "Preparing", 0, progressMax);
    progress.setMillisToDecideToPopup(250);
    progress.setMillisToPopup(500);
    progress.setProgress(progressIdx++);

    int failed = 0, skipped = 0;
    final String outPath = tfOutput.getText();
    try {
      for (int i = 0; i < modelInputFiles.size(); i++) {
        if (progress.isCanceled()) {
          progress.close();
          result.add(null);
          result.add("Conversion cancelled.");
          return result;
        }
        progress.setNote(String.format(progressMsg, progressIdx, modelInputFiles.size()));
        progress.setProgress(progressIdx++);

        // 1. prepare data
        Path inFile = FileManager.resolve(modelInputFiles.get(i));
        Path outFile = FileManager.resolve(outPath, StreamUtils.replaceFileExtension(inFile.getFileName().toString(), "BMP"));
        if (FileEx.create(outFile).exists()) {
          if (cbOverwrite.getSelectedIndex() == 0) {          // ask
            String msg = String.format("File %s already exists. Overwrite?", outFile.getFileName());
            int ret = JOptionPane.showConfirmDialog(this, msg, "Overwrite?", JOptionPane.YES_NO_CANCEL_OPTION);
            if (ret == JOptionPane.NO_OPTION) {
              skipped++;
              continue;
            } else if (ret == JOptionPane.CANCEL_OPTION) {
              progress.close();
              result.add(null);
              result.add("Conversion cancelled.");
              return result;
            }
          } else if (cbOverwrite.getSelectedIndex() == 2) {   // skip
            skipped++;
            continue;
          }
        }

        Image img = null;
        try {
          img = ImageIO.read(inFile.toFile());
        } catch (Exception e) {
          failed++;
          img = null;
        }

        // 2. write BMP output
        if (img != null && outFile != null) {
          if (!writeBMP(img, outFile, cbEnableAlpha.isSelected())) {
            failed++;
          }
        }
      }
    } finally {
      progress.close();
      progress = null;
    }

    // creating summary
    if (failed+skipped > 0) {
      if (failed > 0) {
        result.add(null);
      }
      String msg = null;
      if (failed+skipped == 1) {
        msg = "1 input file has been skipped.";
      } else {
        msg = String.format("%d input files have been skipped.", failed+skipped);
      }
      result.add(msg);
    } else {
      result.add("Conversion finished successfully.");
    }

    return result;
  }

  // creates a 32-bit BMP files that is compatible with BG(2)EE
  private boolean writeBMP(Image srcImage, Path file, boolean hasAlpha)
  {
    if (srcImage != null && file != null) {
      BufferedImage image = ColorConvert.toBufferedImage(srcImage, true);

      // "fixing" premultiplied alpha format
      if (hasAlpha && cbFixPremultipliedAlpha.isSelected()) {
        int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        if (pixels != null) {
          for (int i = 0; i < pixels.length; i++) {
            float anorm = (float)((pixels[i] >>> 24) & 0xff) / 255.0f;
            anorm = (anorm > 0.0f) ? 1.0f / anorm : 0.0f;
            int r = (int)(((float)((pixels[i] >>> 16) & 0xff) * anorm) + 0.5f);
            if (r > 255) r = 255;
            int g = (int)(((float)((pixels[i] >>> 8) & 0xff) * anorm) + 0.5f);
            if (g > 255) g = 255;
            int b = (int)(((float)(pixels[i] & 0xff) * anorm) + 0.5f);
            if (b > 255) b = 255;
            pixels[i] = (pixels[i] & 0xff000000) | (r << 16) | (g << 8) | b;
          }
        }
      }

      int bpp = hasAlpha ? 32 : 24;
      int bytesPerPixel = bpp / 8;
      int bytesPerLine = image.getWidth()*bytesPerPixel;
      int fillBytes = (4 - (bytesPerLine & 3)) & 3;

      // writing BMP header
      int comression = hasAlpha ? 3 : 0;
      int sizeFileHeader = 14;
      int sizeBitmapHeader = hasAlpha ? 124 : 40;
      int headerSize = sizeFileHeader + sizeBitmapHeader;
      int fileSize = headerSize + (bytesPerLine + fillBytes)*image.getHeight();
      ByteBuffer buffer = StreamUtils.getByteBuffer(headerSize);

      // file header
      buffer.put("BM".getBytes());          // File type ("BM")
      buffer.putInt(fileSize);              // total file size
      buffer.putInt(0);                     // reserved
      buffer.putInt(sizeFileHeader+sizeBitmapHeader);   // start of pixel data
      // bitmap header
      buffer.putInt(sizeBitmapHeader);      // bitmap header size
      buffer.putInt(image.getWidth());      // image width
      buffer.putInt(image.getHeight());     // image height
      buffer.putShort((short)1);            // color planes
      buffer.putShort((short)bpp);          // bits per pixel
      buffer.putInt(comression);            // compression (0=uncompressed, 3=bitfield)
      buffer.putInt(image.getWidth()*image.getHeight()*4);    // size of bitmap in bytes
      buffer.putInt(0xb12);                 // pixels per meter
      buffer.putInt(0xb12);                 // pixels per meter
      buffer.putInt(0);                     // colors used (palette only)
      buffer.putInt(0);                     // important colors (palette only)
      if (hasAlpha) {
        buffer.putInt(0x00ff0000);          // red bitmask
        buffer.putInt(0x0000ff00);          // green bitmask
        buffer.putInt(0x000000ff);          // blue bitmask
        buffer.putInt(0xff000000);          // alpha bitmask
        buffer.put("BGRs".getBytes());      // color space type
        byte[] zero = new byte[16*4];
        Arrays.fill(zero, (byte)0);
        buffer.put(zero);                   // remaining fields are empty
        zero = null;
      }

      // writing BMP pixel data in ARGB format (upside down)
      try (OutputStream os = StreamUtils.getOutputStream(file, true)) {
        // writing header
        os.write(buffer.array());

        // writing pixel data
        final int transThreshold = 0x20;
        byte[] row = new byte[bytesPerLine+fillBytes];
        int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for(int y = image.getHeight() - 1; y >= 0; y--) {
          for (int i = 0, idx = y*image.getWidth(); i < bytesPerLine; i += bytesPerPixel, idx++) {
            if (!hasAlpha && (pixels[idx] >>> 24) < transThreshold) {
              pixels[idx] = 0x00ff00;   // transparent pixels are translated into RGB(0, 255, 0)
            }
            row[i+0] = (byte)(pixels[idx] & 0xff);
            row[i+1] = (byte)((pixels[idx] >>> 8) & 0xff);
            row[i+2] = (byte)((pixels[idx] >>> 16) & 0xff);
            if (hasAlpha) {
              row[i+3] = (byte)((pixels[idx] >>> 24) & 0xff);
            }
          }
          // adding alignment bytes
          for (int i = 0; i < fillBytes; i++) {
            row[bytesPerLine+i] = (byte)0;
          }
          os.write(row);
        }
        return true;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return false;
  }

}
