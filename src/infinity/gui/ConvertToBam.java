// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import infinity.icon.Icons;
import infinity.resource.ResourceFactory;
import infinity.resource.graphics.BamDecoder;
import infinity.resource.graphics.ColorConvert;
import infinity.resource.graphics.Compressor;
import infinity.resource.graphics.DxtEncoder;
import infinity.resource.graphics.PvrDecoder;
import infinity.resource.key.FileResourceEntry;
import infinity.resource.key.ResourceEntry;
import infinity.util.DynamicArray;
import infinity.util.GridManager;
import infinity.util.IntegerHashMap;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.DataBufferInt;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ProgressMonitor;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileNameExtensionFilter;

public class ConvertToBam extends ChildFrame
    implements ActionListener, PropertyChangeListener, FocusListener, ChangeListener,
               ListSelectionListener
{
  private static String currentPath = ResourceFactory.getRootDir().toString();

  private JTabbedPane tabPane;
  private DefaultListModel modelFrames, modelCycles, modelCurCycle;   // Frames == FramesAvail
  private JList listFrames, listCycles, listFramesAvail, listCurCycle;
  private JButton bConvert, bCancel;
  private JMenuItem miFramesAdd, miFramesImport, miBamPaletteClear, miBamPaletteSet;
  private ButtonPopupMenu bFramesAdd, bBamPalette;
  private JButton bFramesAddFolder, bFramesRemove, bFramesRemoveAll, bBamOutput;
  private JButton bFramesUp, bFramesDown, bCyclesUp, bCyclesDown, bCurCycleUp, bCurCycleDown;
  private JButton bCyclesAdd, bCyclesRemove, bCyclesClear, bCurCycleAdd, bCurCycleRemove;
  private JButton bVersionHelp, bCompressionHelp;
  private JButton bMacroAssignFrames, bMacroRemoveFrames, bMacroSortFramesAsc, bMacroDuplicate,
                  bMacroReverseFrames, bMacroRemoveAll, bMacroReverseCycles;
  private JButton bPreviewCyclePrev, bPreviewCycleNext, bPreviewFramePrev, bPreviewFrameNext,
                  bPreviewPlay, bPreviewStop;
  private JTextField tfFrameWidth, tfFrameHeight, tfFrameCenterX, tfFrameCenterY,
                     tfBamOutput, tfBamPalette;
  private JCheckBox cbCloseOnExit, cbCompressFrame, cbCompressBam, cbPreviewShowMarker, cbPreviewZoom;
  private JPanel pFramesCurFrame, pCurrentCycle;
  private JPanel pFramesCurFrameVersion, pFramesOptionsVersion;
  private JComboBox cbVersion, cbCompression, cbPreviewMode;
  private RenderCanvas rcQuickPreview, rcPreview;
  private JLabel lBamPalette, lPreviewCycle, lPreviewFrame;
  private JSpinner sPvrzIndex, sPreviewFps;

  // preview related data
  private boolean isPreviewModified, isPreviewPlaying;
  private Timer timer;
  private int currentCycle, currentFrame, currentFps;
  // size and relative position to fit all available frames (incl. center positions) for preview
  private Rectangle previewBounds;

  private SwingWorker<List<String>, Void> workerConvert;
  private WindowBlocker blocker;


  // Returns a list of supported graphics file formats
  private static FileNameExtensionFilter[] getGraphicsFilters()
  {
    FileNameExtensionFilter[] filters = new FileNameExtensionFilter[] {
        new FileNameExtensionFilter("Graphics files (*.bam, *.bmp, *.png, *,jpg, *.jpeg)",
                                    "bam", "bmp", "png", "jpg", "jpeg"),
        new FileNameExtensionFilter("BAM files (*.bam)", "bam"),
        new FileNameExtensionFilter("BMP files (*.bmp)", "bmp"),
        new FileNameExtensionFilter("PNG files (*.png)", "png"),
        new FileNameExtensionFilter("JPEG files (*.jpg, *.jpeg)", "jpg", "jpeg")
    };
    return filters;
  }

  // Returns a list of supported file formats containing palettes
  private static FileNameExtensionFilter[] getPaletteFilters()
  {
    FileNameExtensionFilter[] filters = new FileNameExtensionFilter[] {
        new FileNameExtensionFilter("Palette from files (*.bam, *.bmp)", "bam", "bmp"),
        new FileNameExtensionFilter("Palette from BAM files (*.bam)", "bam"),
        new FileNameExtensionFilter("Palette from BMP files (*.bmp)", "bmp"),
    };
    return filters;
  }

  private static FileNameExtensionFilter getBamFilter()
  {
    return new FileNameExtensionFilter("BAM files (*.bam)", "bam");
  }

  // returns a selection of files
  private static File[] getOpenFileName(Component parent, String title, String rootPath,
                                        boolean selectMultiple,
                                        FileNameExtensionFilter[] filters, int filterIndex)
  {
    if (rootPath == null || rootPath.isEmpty()) {
      rootPath = currentPath;
    }
    JFileChooser fc = new JFileChooser(rootPath);
    File file = new File(rootPath);
    if (!file.isDirectory()) {
        fc.setSelectedFile(file);
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
          currentPath = fc.getSelectedFiles()[0].getParent();
        }
        return fc.getSelectedFiles();
      } else {
        currentPath = fc.getSelectedFile().getParent();
        return new File[]{fc.getSelectedFile()};
      }
    } else {
      return null;
    }
  }

  // returns a path name
  private static File getOpenPathName(Component parent, String title, String rootPath)
  {
    if (rootPath == null || rootPath.isEmpty()) {
      rootPath = currentPath;
    }
    JFileChooser fc = new JFileChooser(rootPath);
    if (title == null) {
      title = "Select folder";
    }
    fc.setDialogTitle(title);
    fc.setDialogType(JFileChooser.OPEN_DIALOG);
    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
      currentPath = fc.getSelectedFile().toString();
      return fc.getSelectedFile();
    } else {
      return null;
    }
  }

  // returns a filename
  private static File getSaveFileName(Component parent, String title, String rootPath,
                                      FileNameExtensionFilter[] filters, int filterIndex)
  {
    if (rootPath == null || rootPath.isEmpty()) {
      rootPath = currentPath;
    }
    JFileChooser fc = new JFileChooser(rootPath);
    File file = new File(rootPath);
    if (!file.isDirectory()) {
        fc.setSelectedFile(file);
    }
    if (title == null) {
      title = "Specify filename";
    }
    fc.setDialogTitle(title);
    fc.setDialogType(JFileChooser.SAVE_DIALOG);
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    if (filters != null) {
      for (final FileNameExtensionFilter filter: filters) {
        fc.addChoosableFileFilter(filter);
      }
      if (filterIndex >= 0 && filterIndex < filters.length) {
        fc.setFileFilter(filters[filterIndex]);
      }
    }
    if (fc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
      currentPath = fc.getSelectedFile().getParent();
      return fc.getSelectedFile();
    } else {
      return null;
    }
  }

  // checks "input" to be within the specified specs and returns it or a default value
  private static int numberValidator(String input, int min, int max, int defaultValue)
  {
    int retVal = defaultValue;
    try {
      int value = Integer.parseInt(input);
      if (value >= min && value <= max) {
        retVal = value;
      }
    } catch (NumberFormatException e) {
    }

    return retVal;
  }

  // sets a new file extension to the specified filename string
  private static String setFileExtension(String fileName, String extension)
  {
    if (fileName != null && !fileName.isEmpty()) {
      int pos = fileName.lastIndexOf('.');
      if (pos > 0) {
        // make sure our 'dot' belongs to the file's extension
        if (pos > fileName.lastIndexOf(File.separatorChar)) {
          if (fileName.substring(pos+1).equalsIgnoreCase(extension)) {
            return fileName;
          }
          fileName = fileName.substring(0, pos);
        }
      }
      if (extension != null && !extension.isEmpty()) {
        fileName = fileName + "." + extension;
      }
    }
    return fileName;
  }


  public ConvertToBam()
  {
    super("Convert image sequence to BAM", true);
    init();
  }

//--------------------- Begin Class ChildFrame ---------------------

  @Override
  protected boolean windowClosing(boolean forced) throws Exception
  {
    if (forced || confirmCloseDialog()) {
      clear();
      return super.windowClosing(forced);
    } else {
      return false;
    }
  }

//--------------------- End Class ChildFrame ---------------------

//--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == timer) {
      previewAdvanceAnimation();
    }
    else if (event.getSource() == bConvert) {
      workerConvert = new SwingWorker<List<String>, Void>() {
        @Override
        public List<String> doInBackground()
        {
          return convert();
        }
      };
      workerConvert.addPropertyChangeListener(this);
      blocker = new WindowBlocker(this);
      blocker.setBlocked(true);
      workerConvert.execute();
    } else if (event.getSource() == bCancel) {
      hideWindow(false);
    } else if (event.getSource() == cbVersion) {
      setBamVersionEnabled((cbVersion.getSelectedIndex() == 0) ? 1 : 2);
    } else if (event.getSource() == bBamOutput) {
      setBamOutput();
    } else if (event.getSource() == miBamPaletteClear) {
      tfBamPalette.setText("");
      lBamPalette.setEnabled(false);
    } else if (event.getSource() == miBamPaletteSet) {
      setBamPalette();
    } else if (event.getSource() == miFramesAdd) {
      try {
        WindowBlocker.blockWindow(this, true);
        framesAdd();
        listFrames.ensureIndexIsVisible(listFrames.getMaxSelectionIndex());
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getSource() == miFramesImport) {
      try {
        WindowBlocker.blockWindow(this, true);
        framesImportBam();
        listFrames.ensureIndexIsVisible(listFrames.getMaxSelectionIndex());
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getSource() == bFramesAddFolder) {
      try {
        WindowBlocker.blockWindow(this, true);
        framesAddFolder();
        listFrames.ensureIndexIsVisible(listFrames.getMaxSelectionIndex());
      } finally {
        WindowBlocker.blockWindow(this, false);
      }
    } else if (event.getSource() == bFramesRemove) {
      framesRemove();
    } else if (event.getSource() == bFramesRemoveAll) {
      framesRemoveAll();
    } else if (event.getSource() == bFramesUp) {
      framesMoveUp();
    } else if (event.getSource() == bFramesDown) {
      framesMoveDown();
    } else if (event.getSource() == cbCompressFrame) {
      int[] indices = listFrames.getSelectedIndices();
      for (int i = 0; i < indices.length; i++) {
        final BamFrame bc = (BamFrame)modelFrames.get(indices[i]);
        bc.isCompressed = cbCompressFrame.isSelected();
      }
    } else if (event.getSource() == bCyclesAdd) {
      cyclesAdd();
      listCycles.ensureIndexIsVisible(listCycles.getSelectedIndex());
    } else if (event.getSource() == bCyclesRemove) {
      cyclesRemove();
    } else if (event.getSource() == bCyclesClear) {
      cyclesRemoveAll();
    } else if (event.getSource() == bCyclesUp) {
      cyclesMoveUp();
    } else if (event.getSource() == bCyclesDown) {
      cyclesMoveDown();
    } else if (event.getSource() == bMacroAssignFrames) {
      macroAssignFrames();
      listCycles.ensureIndexIsVisible(listCycles.getSelectedIndex());
    } else if (event.getSource() == bMacroRemoveFrames) {
      macroRemoveFrames();
      listCycles.ensureIndexIsVisible(listCycles.getSelectedIndex());
    } else if (event.getSource() == bMacroDuplicate) {
      macroDuplicateCycle();
      listCycles.ensureIndexIsVisible(listCycles.getSelectedIndex());
    } else if (event.getSource() == bMacroSortFramesAsc) {
      macroSortFrames();
      listCycles.ensureIndexIsVisible(listCycles.getSelectedIndex());
    } else if (event.getSource() == bMacroReverseFrames) {
      macroReverseFramesOrder();
      listCycles.ensureIndexIsVisible(listCycles.getSelectedIndex());
    } else if (event.getSource() == bMacroRemoveAll) {
      macroRemoveAll();
    } else if (event.getSource() == bMacroReverseCycles) {
      macroReverseCyclesOrder();
    } else if (event.getSource() == bCurCycleAdd) {
      curCycleAdd();
      listCurCycle.ensureIndexIsVisible(listCurCycle.getMaxSelectionIndex());
    } else if (event.getSource() == bCurCycleRemove) {
      curCycleRemove();
    } else if (event.getSource() == bCurCycleUp) {
      curCycleMoveUp();
    } else if (event.getSource() == bCurCycleDown) {
      curCycleMoveDown();
    } else if (event.getSource() == bPreviewPlay) {
      previewPlay();
    } else if (event.getSource() == bPreviewStop) {
      previewStop();
    } else if (event.getSource() == bPreviewCyclePrev) {
      previewCycleDec();
    } else if (event.getSource() == bPreviewCycleNext) {
      previewCycleInc();
    } else if (event.getSource() == bPreviewFramePrev) {
      previewFrameDec();
    } else if (event.getSource() == bPreviewFrameNext) {
      previewFrameInc();
    } else if (event.getSource() == cbPreviewShowMarker) {
      previewDisplay();
    } else if (event.getSource() == cbPreviewZoom) {
      previewPrepare(true);
    } else if (event.getSource() == bVersionHelp) {
      final String helpMsg =
          "\"Legacy (V1)\" is the old and proven BAM format supported by all available\n" +
          "Infinity Engine games. It uses a global 256 color table for all frames\n" +
          "and supports simple bitmasked transparency.\n\n" +
          "\"PVRZ-based (V2)\" uses a new BAM format introduced by BG:EE. Graphics data\n" +
          "is stored separately in PVRZ files. Each frame supports interpolated alpha\n" +
          "transitions and is not limited to a global 256 color table.\n" +
          "It is only supported by the Enhanced Editions of the Baldur's Gate games.";
      JOptionPane.showMessageDialog(this, helpMsg, "About BAM versions", JOptionPane.INFORMATION_MESSAGE);
    } else if (event.getSource() == bCompressionHelp) {
      final String helpMsg =
          "\"DXT1\" provides the highest compression ratio. It supports only 1 bit alpha\n" +
          "(i.e. either no or full transparency) and is the preferred type for TIS or MOS resources.\n\n" +
          "\"DXT5\" provides an average compression ratio. It features interpolated\n" +
          "alpha transitions and is the preferred type for BAM resources.\n\n" +
          "\"Auto\" selects the most appropriate compression type based on the input data.";
      JOptionPane.showMessageDialog(this, helpMsg, "About Compression Types", JOptionPane.INFORMATION_MESSAGE);
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
              hideWindow(true);
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
    if (event.getSource() == tfFrameCenterX) {
      int idx = listFrames.getSelectedIndex();
      if (idx >= 0) {
        final BamFrame bc = (BamFrame)modelFrames.get(idx);
        int value = numberValidator(tfFrameCenterX.getText(), Short.MIN_VALUE, Short.MAX_VALUE, bc.centerX);
        bc.centerX = value;
        tfFrameCenterX.setText(Integer.toString(value));
      } else {
        tfFrameCenterX.setText(Integer.toString(numberValidator(tfFrameCenterX.getText(), 0, 0, 0)));
      }
      updateFramePreview(listFrames.getSelectedIndices());
    } else if (event.getSource() == tfFrameCenterY) {
      int idx = listFrames.getSelectedIndex();
      if (idx >= 0) {
        final BamFrame bc = (BamFrame)modelFrames.get(idx);
        int value = numberValidator(tfFrameCenterY.getText(), Short.MIN_VALUE, Short.MAX_VALUE, bc.centerY);
        bc.centerY = value;
        tfFrameCenterY.setText(Integer.toString(value));
      } else {
        tfFrameCenterY.setText(Integer.toString(numberValidator(tfFrameCenterY.getText(), 0, 0, 0)));
      }
      updateFramePreview(listFrames.getSelectedIndices());
    } else if (event.getSource() == tfBamOutput) {
      String fileName = tfBamOutput.getText();
      if (!fileName.isEmpty()) {
        fileName = setFileExtension(fileName.toString(), "BAM");
        tfBamOutput.setText(fileName);
      }
      updateStatus();
    } else if (event.getSource() == tfBamPalette) {
      lBamPalette.setEnabled(!tfBamPalette.getText().isEmpty());
    }
  }

//--------------------- End Interface FocusListener ---------------------

//--------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent event)
  {
    if (event.getSource() == tabPane) {
      if (tabPane.getSelectedIndex() == 2) {
        previewPrepare(false);
        updatePreview();
      } else {
        previewStop();
      }
    } else if (event.getSource() == sPreviewFps) {
      previewSetFrameRate();
    }
  }

//--------------------- End Interface ChangeListener ---------------------

//--------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getSource() == listFrames) {
      int idxMin = getLowestIndex(listFrames.getSelectedIndices());
      int idxMax = getHighestIndex(listFrames.getSelectedIndices());
      bFramesUp.setEnabled(!modelFrames.isEmpty() && idxMin > 0);
      bFramesDown.setEnabled(!modelFrames.isEmpty() && idxMin >= 0 && idxMax < modelFrames.size() - 1);
      updateFrameInfo(listFrames.getSelectedIndices());
      updateFramePreview(listFrames.getSelectedIndices());
    } else if (event.getSource() == listCycles) {
      int idx = listCycles.getSelectedIndex();
      bCyclesUp.setEnabled(!modelCycles.isEmpty() && idx > 0);
      bCyclesDown.setEnabled(!modelCycles.isEmpty() && idx >= 0 && idx < modelCycles.size() - 1);
      initCurrentCycle(listCycles.getSelectedIndex());
    } else if (event.getSource() == listCurCycle) {
      updateCurrentCycle();
    } else if (event.getSource() == listFramesAvail) {
      updateCurrentCycle();
    }
  }

//--------------------- End Interface ListSelectionListener ---------------------

  private void init()
  {
    JPanel pFrames = createFramesTab();
    JPanel pCycles = createCyclesTab();
    JPanel pPreview = createPreviewTab();

    // setting up tabbed pane
    tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.addTab("Frames", pFrames);
    tabPane.setMnemonicAt(0, KeyEvent.VK_F);
    tabPane.addTab("Cycles", pCycles);
    tabPane.setMnemonicAt(1, KeyEvent.VK_C);
    tabPane.addTab("Preview", pPreview);
    tabPane.setMnemonicAt(2, KeyEvent.VK_P);
    tabPane.addChangeListener(this);

    // setting up bottom button bar
    GridBagConstraints c = new GridBagConstraints();
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
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(cbCloseOnExit, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bConvert, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_END,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pButtons.add(bCancel, c);

    // putting all sections together
    setLayout(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    add(tabPane, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 8, 8, 8), 0, 0);
    add(pButtons, c);

    // finalizing dialog initialization
    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(getParent());
    updateFramesList();
    updateFrameInfo(listFrames.getSelectedIndices());
    updateFramePreview(listFrames.getSelectedIndices());
    initCurrentCycle(-1);
    updateStatus();
    setVisible(true);
  }

  private JPanel createFramesTab()
  {
    GridBagConstraints c = new GridBagConstraints();

    // create "Frames List"
    JPanel pFramesListArrows = new JPanel(new GridBagLayout());
    bFramesUp = new JButton(Icons.getIcon("Up16.gif"));
    bFramesUp.setMargin(new Insets(bFramesUp.getInsets().top, 2, bFramesUp.getInsets().bottom, 2));
    bFramesUp.addActionListener(this);
    bFramesDown = new JButton(Icons.getIcon("Down16.gif"));
    bFramesDown.setMargin(new Insets(bFramesDown.getInsets().top, 2, bFramesDown.getInsets().bottom, 2));
    bFramesDown.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_END,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 16);
    pFramesListArrows.add(bFramesUp, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 16);
    pFramesListArrows.add(bFramesDown, c);

    JPanel pFramesAdd = new JPanel(new GridBagLayout());
    miFramesAdd = new JMenuItem("Add image(s)...");
    miFramesAdd.setToolTipText("Add one or more images to the frames list");
    miFramesAdd.addActionListener(this);
    miFramesImport = new JMenuItem("Import BAM...");
    miFramesImport.setToolTipText("Import both frame and cycle definitions from the selected BAM. " +
                                  "Current content (if any) will be discarded.");
    miFramesImport.addActionListener(this);
    bFramesAdd = new ButtonPopupMenu("Add/Import...", new JMenuItem[]{miFramesAdd, miFramesImport});
    bFramesAddFolder = new JButton("Add folder...");
    bFramesAddFolder.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFramesAdd.add(bFramesAdd, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pFramesAdd.add(bFramesAddFolder, c);

    JPanel pFramesRemove = new JPanel(new GridBagLayout());
    bFramesRemove = new JButton("Remove");
    bFramesRemove.addActionListener(this);
    bFramesRemoveAll = new JButton("Remove all");
    bFramesRemoveAll.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFramesRemove.add(bFramesRemove, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pFramesRemove.add(bFramesRemoveAll, c);

    JPanel pFramesListButtons = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFramesListButtons.add(pFramesAdd, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pFramesListButtons.add(new JPanel(), c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFramesListButtons.add(pFramesRemove, c);

    JPanel pFramesList = new JPanel(new GridBagLayout());
    JLabel lFramesTitle = new JLabel("Frames:");
    modelFrames = new DefaultListModel();
    listFrames= new JList(modelFrames);
    listFrames.setCellRenderer(new IndexedCellRenderer());
    listFrames.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    listFrames.addListSelectionListener(this);
    JScrollPane scroll = new JScrollPane(listFrames);
    c = setGBC(c, 0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 4, 0), 0, 0);
    pFramesList.add(lFramesTitle, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pFramesList.add(scroll, c);
    c = setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.VERTICAL, new Insets(0, 8, 0, 0), 0, 0);
    pFramesList.add(pFramesListArrows, c);
    c = setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pFramesList.add(pFramesListButtons, c);

    // create "Current Frame" section
    pFramesCurFrame = new JPanel(new GridBagLayout());
    pFramesCurFrame.setBorder(BorderFactory.createTitledBorder(" "));

    JPanel pFramesCurFrameVersionV1 = new JPanel(new GridBagLayout());
    cbCompressFrame = new JCheckBox("Compress frame");
    cbCompressFrame.setToolTipText("Compress transparent pixel data in the frame");
    cbCompressFrame.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pFramesCurFrameVersionV1.add(cbCompressFrame, c);
    JPanel pFramesCurFrameVersionV2 = new JPanel();

    pFramesCurFrameVersion = new JPanel(new CardLayout());
    pFramesCurFrameVersion.add(pFramesCurFrameVersionV1, "V1");
    pFramesCurFrameVersion.add(pFramesCurFrameVersionV2, "V2");

    JLabel lFramesWidth = new JLabel("Width:");
    JLabel lFramesHeight = new JLabel("Height:");
    JLabel lFramesCenterX = new JLabel("Center X:");
    JLabel lFramesCenterY = new JLabel("Center Y:");
    tfFrameWidth = new JTextField("0", 6);
    tfFrameWidth.setEditable(false);
    tfFrameHeight = new JTextField("0", 6);
    tfFrameHeight.setEditable(false);
    tfFrameCenterX = new JTextField("0", 6);
    tfFrameCenterX.addFocusListener(this);
    tfFrameCenterY = new JTextField("0", 6);
    tfFrameCenterY.addFocusListener(this);

    JPanel pFramesCurFrameLeft = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pFramesCurFrameLeft.add(lFramesWidth, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFramesCurFrameLeft.add(tfFrameWidth, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pFramesCurFrameLeft.add(lFramesHeight, c);
    c = setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pFramesCurFrameLeft.add(tfFrameHeight, c);

    JPanel pFramesCurFrameRight = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pFramesCurFrameRight.add(lFramesCenterX, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFramesCurFrameRight.add(tfFrameCenterX, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pFramesCurFrameRight.add(lFramesCenterY, c);
    c = setGBC(c, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pFramesCurFrameRight.add(tfFrameCenterY, c);

    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 4, 0), 0, 0);
    pFramesCurFrame.add(pFramesCurFrameLeft, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 16, 4, 8), 0, 0);
    pFramesCurFrame.add(pFramesCurFrameRight, c);
    c = setGBC(c, 0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 4, 8), 0, 0);
    pFramesCurFrame.add(pFramesCurFrameVersion, c);
    pFramesCurFrame.setMinimumSize(pFramesCurFrame.getPreferredSize());

    // create "Quick View" section
    JPanel pFramesQuickView = new JPanel(new GridBagLayout());
    pFramesQuickView.setBorder(BorderFactory.createTitledBorder("Quick Preview "));
    rcQuickPreview = new RenderCanvas(ColorConvert.createCompatibleImage(256, 256, true));
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    pFramesQuickView.add(rcQuickPreview, c);
    pFramesQuickView.setMinimumSize(pFramesQuickView.getPreferredSize());

    // create "Import" section
    JPanel pFramesImport = new JPanel(new GridBagLayout());
    pFramesImport.setBorder(BorderFactory.createTitledBorder("Import "));
    c = setGBC(c, 0, 0, 1, 2, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 4, 16), 0, 0);
    pFramesImport.add(pFramesList, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 4), 0, 0);
    pFramesImport.add(pFramesCurFrame, c);
    c = setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 4), 0, 0);
    pFramesImport.add(pFramesQuickView, c);

    // create "Export options" sections
    JPanel pFramesOptionsVersionV1 = new JPanel(new GridBagLayout());
    cbCompressBam = new JCheckBox("Compress BAM");
    cbCompressBam.setToolTipText("Create a zlib compressed BAM file (BAMC)");
    lBamPalette = new JLabel("Palette: ");
    lBamPalette.setEnabled(false);
    tfBamPalette = new JTextField();
    tfBamPalette.addFocusListener(this);
    miBamPaletteClear = new JMenuItem("Clear palette");
    miBamPaletteClear.addActionListener(this);
    miBamPaletteSet = new JMenuItem("Set palette");
    miBamPaletteSet.addActionListener(this);
    bBamPalette = new ButtonPopupMenu("...", new JMenuItem[]{miBamPaletteClear, miBamPaletteSet});
    bBamPalette.setToolTipText("Use a predefined palette for the resulting BAM");
    bBamPalette.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pFramesOptionsVersionV1.add(cbCompressBam, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pFramesOptionsVersionV1.add(lBamPalette, c);
    c = setGBC(c, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 4, 0, 0), 0, 0);
    pFramesOptionsVersionV1.add(tfBamPalette, c);
    c = setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pFramesOptionsVersionV1.add(bBamPalette, c);

    JPanel pFramesOptionsVersionV2 = new JPanel(new GridBagLayout());
    JLabel lPvrzIndex = new JLabel("PVRZ index starts at:");
    sPvrzIndex = new JSpinner(new SpinnerNumberModel(0, 0, 99999, 1));
    sPvrzIndex.setToolTipText("Enter a number from 0 to 99999");
    sPvrzIndex.addChangeListener(this);
    JLabel lFramesCompression = new JLabel("Compression type:");
    cbCompression = new JComboBox(new Object[]{"Auto", "DXT1", "DXT5"});
    bCompressionHelp = new JButton("?");
    bCompressionHelp.setMargin(new Insets(2, 4, 2, 4));
    bCompressionHelp.setToolTipText("About compression types");
    bCompressionHelp.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 0, 0);
    pFramesOptionsVersionV2.add(lPvrzIndex, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFramesOptionsVersionV2.add(sPvrzIndex, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pFramesOptionsVersionV2.add(lFramesCompression, c);
    c = setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 8, 0);
    pFramesOptionsVersionV2.add(cbCompression, c);
    c = setGBC(c, 4, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pFramesOptionsVersionV2.add(bCompressionHelp, c);

    pFramesOptionsVersion = new JPanel(new CardLayout());
    pFramesOptionsVersion.add(pFramesOptionsVersionV1, "V1");
    pFramesOptionsVersion.add(pFramesOptionsVersionV2, "V2");

    // create "Export" section
    JPanel pFramesExportOptions = new JPanel(new GridBagLayout());
    JLabel lOutputFile = new JLabel("Output file:");
    tfBamOutput = new JTextField();
    tfBamOutput.addFocusListener(this);
    bBamOutput = new JButton("...");
    bBamOutput.addActionListener(this);
    JLabel lFramesVersion = new JLabel("BAM version:");
    cbVersion = new JComboBox(new Object[]{"Legacy (V1)", "PVRZ-based (V2)"});
    cbVersion.addActionListener(this);
    bVersionHelp = new JButton("?");
    bVersionHelp.setMargin(new Insets(2, 4, 2, 4));
    bVersionHelp.setToolTipText("About BAM versions");
    bVersionHelp.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pFramesExportOptions.add(cbVersion, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
        GridBagConstraints.NONE, new Insets(0, 8, 0, 0), 8, 0);
    pFramesExportOptions.add(cbVersion, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pFramesExportOptions.add(bVersionHelp, c);
    c = setGBC(c, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pFramesExportOptions.add(pFramesOptionsVersion, c);

    JPanel pFramesExport = new JPanel(new GridBagLayout());
    pFramesExport.setBorder(BorderFactory.createTitledBorder("Export "));
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 0, 0), 0, 0);
    pFramesExport.add(lOutputFile, c);
    c = setGBC(c, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
    pFramesExport.add(tfBamOutput, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(4, 4, 0, 4), 0, 0);
    pFramesExport.add(bBamOutput, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(8, 4, 8, 0), 0, 0);
    pFramesExport.add(lFramesVersion, c);
    c = setGBC(c, 1, 1, 2, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 0, 8, 0), 0, 0);
    pFramesExport.add(pFramesExportOptions, c);


    // putting all together
    JPanel pFrames = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(8, 8, 0, 8), 0, 0);
    pFrames.add(pFramesImport, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 8, 8, 8), 0, 0);
    pFrames.add(pFramesExport, c);

    setBamVersionEnabled(1);

    return pFrames;
  }

  private JPanel createCyclesTab()
  {
    GridBagConstraints c = new GridBagConstraints();

    // create "Cycles" section
    JPanel pCyclesButtons = new JPanel(new GridBagLayout());
    bCyclesAdd = new JButton("Add cycle");
    bCyclesAdd.addActionListener(this);
    bCyclesRemove = new JButton("Remove cycle");
    bCyclesRemove.addActionListener(this);
    bCyclesClear = new JButton("Remove all");
    bCyclesClear.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pCyclesButtons.add(bCyclesAdd, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pCyclesButtons.add(bCyclesRemove, c);
    c = setGBC(c, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pCyclesButtons.add(bCyclesClear, c);

    JPanel pCyclesArrows = new JPanel(new GridBagLayout());
    bCyclesUp = new JButton(Icons.getIcon("Up16.gif"));
    bCyclesUp.setMargin(new Insets(bCyclesUp.getInsets().top, 2, bCyclesUp.getInsets().bottom, 2));
    bCyclesUp.addActionListener(this);
    bCyclesDown = new JButton(Icons.getIcon("Down16.gif"));
    bCyclesDown.setMargin(new Insets(bCyclesDown.getInsets().top, 2, bCyclesDown.getInsets().bottom, 2));
    bCyclesDown.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 16);
    pCyclesArrows.add(bCyclesUp, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 16);
    pCyclesArrows.add(bCyclesDown, c);

    JPanel pCyclesList = new JPanel(new GridBagLayout());
    JLabel lCycles = new JLabel("Cycles:");
    modelCycles = new DefaultListModel();
    listCycles = new JList(modelCycles);
    listCycles.setCellRenderer(new IndexedCellRenderer());
    listCycles.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listCycles.addListSelectionListener(this);
    JScrollPane scroll = new JScrollPane(listCycles);
    c = setGBC(c, 0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 4, 0), 0, 0);
    pCyclesList.add(lCycles, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pCyclesList.add(scroll, c);
    c = setGBC(c, 1, 1, 1, 2, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.VERTICAL, new Insets(0, 4, 0, 0), 0, 0);
    pCyclesList.add(pCyclesArrows, c);
    c = setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pCyclesList.add(pCyclesButtons, c);

    // create "Macros" section
    JPanel pMacro = new JPanel(new GridBagLayout());
    pMacro.setBorder(BorderFactory.createTitledBorder("Macros "));
    JLabel lMacroCurCycle = new JLabel("Currently selected cycle:");
    bMacroAssignFrames = new JButton("Assign all frames");
    bMacroAssignFrames.addActionListener(this);
    bMacroRemoveFrames = new JButton("Remove all frames");
    bMacroRemoveFrames.addActionListener(this);
    bMacroSortFramesAsc = new JButton("Sort frames");
    bMacroSortFramesAsc.addActionListener(this);
    bMacroDuplicate = new JButton("Duplicate cycle");
    bMacroDuplicate.addActionListener(this);
    bMacroReverseFrames = new JButton("Reverse frames order");
    bMacroReverseFrames.addActionListener(this);
    JLabel lMacroAllCycles = new JLabel("All cycles:");
    bMacroRemoveAll = new JButton("Remove all frames");
    bMacroRemoveAll.addActionListener(this);
    bMacroReverseCycles = new JButton("Reverse cycles order");
    bMacroReverseCycles.addActionListener(this);
    JPanel pMacroPanel = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 2, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pMacroPanel.add(lMacroCurCycle, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pMacroPanel.add(bMacroAssignFrames, c);
    c = setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    pMacroPanel.add(bMacroRemoveFrames, c);
    c = setGBC(c, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pMacroPanel.add(bMacroDuplicate, c);
    c = setGBC(c, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    pMacroPanel.add(bMacroSortFramesAsc, c);
    c = setGBC(c, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pMacroPanel.add(bMacroReverseFrames, c);
    c = setGBC(c, 0, 4, 2, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
    pMacroPanel.add(lMacroAllCycles, c);
    c = setGBC(c, 0, 5, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 0, 0);
    pMacroPanel.add(bMacroRemoveAll, c);
    c = setGBC(c, 1, 5, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 4, 0, 0), 0, 0);
    pMacroPanel.add(bMacroReverseCycles, c);

    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
        GridBagConstraints.BOTH, new Insets(4, 8, 8, 8), 0, 0);
    pMacro.add(pMacroPanel, c);
    pMacro.setMinimumSize(pMacro.getPreferredSize());

    JPanel pTopHalf = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 8, 0, 0), 0, 0);
    pTopHalf.add(pCyclesList, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(8, 16, 0, 0), 0, 0);
    pTopHalf.add(pMacro, c);

    // create "Current Cycles" section
    JPanel pCycleTransfer = new JPanel(new GridBagLayout());
    bCurCycleAdd = new JButton(Icons.getIcon("Forward16.gif"));
    bCurCycleAdd.setMargin(new Insets(2, bCurCycleAdd.getInsets().left, 2, bCurCycleAdd.getInsets().right));
    bCurCycleAdd.addActionListener(this);
    bCurCycleRemove = new JButton(Icons.getIcon("Back16.gif"));
    bCurCycleRemove.setMargin(new Insets(2, bCurCycleRemove.getInsets().left,
                                         2, bCurCycleRemove.getInsets().right));
    bCurCycleRemove.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 8, 0);
    pCycleTransfer.add(bCurCycleAdd, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 0, 0, 0), 8, 0);
    pCycleTransfer.add(bCurCycleRemove, c);

    JPanel pCurCycleArrows = new JPanel(new GridBagLayout());
    bCurCycleUp = new JButton(Icons.getIcon("Up16.gif"));
    bCurCycleUp.setMargin(new Insets(bCurCycleUp.getInsets().top, 2, bCurCycleUp.getInsets().bottom, 2));
    bCurCycleUp.addActionListener(this);
    bCurCycleDown = new JButton(Icons.getIcon("Down16.gif"));
    bCurCycleDown.setMargin(new Insets(bCurCycleDown.getInsets().top, 2, bCurCycleDown.getInsets().bottom, 2));
    bCurCycleDown.addActionListener(this);
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 16);
    pCurCycleArrows.add(bCurCycleUp, c);
    c = setGBC(c, 0, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 16);
    pCurCycleArrows.add(bCurCycleDown, c);

    JLabel lCurCycleFrames = new JLabel("Available frames:");
    listFramesAvail = new JList(modelFrames);
    listFramesAvail.setCellRenderer(new IndexedCellRenderer());
    listFramesAvail.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    listFramesAvail.addListSelectionListener(this);
    scroll = new JScrollPane(listFramesAvail);
    JLabel lCurCycle = new JLabel("Current cycle:");
    modelCurCycle = new DefaultListModel();
    listCurCycle = new JList(modelCurCycle);
    listCurCycle.setCellRenderer(new IndexedCellRenderer());
    listCurCycle.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    listCurCycle.addListSelectionListener(this);
    JScrollPane scroll2 = new JScrollPane(listCurCycle);

    pCurrentCycle = new JPanel(new GridBagLayout());
    pCurrentCycle.setBorder(BorderFactory.createTitledBorder(" "));
    c = setGBC(c, 0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pCurrentCycle.add(lCurCycleFrames, c);
    c = setGBC(c, 2, 0, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pCurrentCycle.add(lCurCycle, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 4, 0), 0, 0);
    pCurrentCycle.add(scroll, c);
    c = setGBC(c, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 4), 0, 0);
    pCurrentCycle.add(pCycleTransfer, c);
    c = setGBC(c, 2, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 0, 4, 0), 0, 0);
    pCurrentCycle.add(scroll2, c);
    c = setGBC(c, 3, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.VERTICAL, new Insets(4, 4, 0, 4), 0, 0);
    pCurrentCycle.add(pCurCycleArrows, c);

    // putting all together
    JPanel pCycles = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    pCycles.add(pTopHalf, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 2.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    pCycles.add(pCurrentCycle, c);

    return pCycles;
  }

  private JPanel createPreviewTab()
  {
    GridBagConstraints c = new GridBagConstraints();

    // create bottom control bar
    lPreviewCycle = new JLabel("Cycle: X/Y");
    bPreviewCyclePrev = new JButton(Icons.getIcon("Back16.gif"));
    bPreviewCyclePrev.setMargin(new Insets(bPreviewCyclePrev.getMargin().top, 2,
                                           bPreviewCyclePrev.getMargin().bottom, 2));
    bPreviewCyclePrev.addActionListener(this);
    bPreviewCycleNext = new JButton(Icons.getIcon("Forward16.gif"));
    bPreviewCycleNext.setMargin(new Insets(bPreviewCycleNext.getMargin().top, 2,
                                           bPreviewCycleNext.getMargin().bottom, 2));
    bPreviewCycleNext.addActionListener(this);

    lPreviewFrame = new JLabel("Frame: X/Y");
    bPreviewFramePrev = new JButton(Icons.getIcon("Back16.gif"));
    bPreviewFramePrev.setMargin(new Insets(bPreviewFramePrev.getMargin().top, 2,
                                           bPreviewFramePrev.getMargin().bottom, 2));
    bPreviewFramePrev.addActionListener(this);
    bPreviewFrameNext = new JButton(Icons.getIcon("Forward16.gif"));
    bPreviewFrameNext.setMargin(new Insets(bPreviewFrameNext.getMargin().top, 2,
                                           bPreviewFrameNext.getMargin().bottom, 2));
    bPreviewFrameNext.addActionListener(this);

    bPreviewPlay = new JButton("Play", Icons.getIcon("Play16.gif"));
    bPreviewPlay.addActionListener(this);
    bPreviewStop = new JButton("Stop", Icons.getIcon("Stop16.gif"));
    bPreviewStop.addActionListener(this);

    JLabel lFps = new JLabel("Frames/second:");
    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(15, 1, 30, 1);
    sPreviewFps = new JSpinner(spinnerModel);
    sPreviewFps.addChangeListener(this);

    JLabel lPreviewMode = new JLabel("Playback mode:");
    cbPreviewMode = new JComboBox(new Object[]{"Current cycle once", "Current cycle looped",
                                               "All cycles once", "All cycles looped"});
    cbPreviewMode.setSelectedIndex(1);

    cbPreviewShowMarker = new JCheckBox("Show markers");
    cbPreviewShowMarker.addActionListener(this);

    cbPreviewZoom = new JCheckBox("Zoom");
    cbPreviewZoom.addActionListener(this);

    JPanel pControls = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pControls.add(lPreviewCycle, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pControls.add(bPreviewCyclePrev, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 2, 0, 0), 0, 0);
    pControls.add(bPreviewCycleNext, c);
    c = setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pControls.add(lPreviewFrame, c);
    c = setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pControls.add(bPreviewFramePrev, c);
    c = setGBC(c, 5, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 2, 0, 0), 0, 0);
    pControls.add(bPreviewFrameNext, c);
    c = setGBC(c, 6, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pControls.add(bPreviewPlay, c);
    c = setGBC(c, 7, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pControls.add(bPreviewStop, c);

    JPanel pOptions = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pOptions.add(lPreviewMode, c);
    c = setGBC(c, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pOptions.add(cbPreviewMode, c);
    c = setGBC(c, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pOptions.add(lFps, c);
    c = setGBC(c, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 4, 0, 0), 0, 0);
    pOptions.add(sPreviewFps, c);
    c = setGBC(c, 4, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pOptions.add(cbPreviewZoom, c);
    c = setGBC(c, 5, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
               GridBagConstraints.NONE, new Insets(0, 16, 0, 0), 0, 0);
    pOptions.add(cbPreviewShowMarker, c);

    JPanel pCanvas = new JPanel(new GridBagLayout());
    rcPreview = new RenderCanvas();
    JScrollPane scroll = new JScrollPane(rcPreview);
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.getHorizontalScrollBar().setUnitIncrement(16);

    c = setGBC(c, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
               GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pCanvas.add(scroll, c);

    // putting all together
    JPanel pPreview = new JPanel(new GridBagLayout());
    c = setGBC(c, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 8), 0, 0);
    pPreview.add(pOptions, c);
    c = setGBC(c, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.BOTH, new Insets(4, 4, 4, 4), 0, 0);
    pPreview.add(pCanvas, c);
    c = setGBC(c, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
               GridBagConstraints.HORIZONTAL, new Insets(4, 8, 8, 8), 0, 0);
    pPreview.add(pControls, c);

    timer = new Timer(1000, this);
    isPreviewModified = true;
    isPreviewPlaying = false;
    previewSetFrameRate();

    return pPreview;
  }

  private GridBagConstraints setGBC(GridBagConstraints gbc, int gridX, int gridY,
                                    int gridWidth, int gridHeight, double weightX, double weightY,
                                    int anchor, int fill, Insets insets, int iPadX, int iPadY)
  {
    if (gbc == null)
      gbc = new GridBagConstraints();

    gbc.gridx = gridX;
    gbc.gridy = gridY;
    gbc.gridwidth = gridWidth;
    gbc.gridheight = gridHeight;
    gbc.weightx = weightX;
    gbc.weighty = weightY;
    gbc.anchor = anchor;
    gbc.fill = fill;
    gbc.insets = (insets == null) ? new Insets(0, 0, 0, 0) : insets;
    gbc.ipadx = iPadX;
    gbc.ipady = iPadY;

    return gbc;
  }

  private void hideWindow(boolean force)
  {
    if (force || confirmCloseDialog()) {
      clear();
      setVisible(false);
    }
  }

  private boolean confirmCloseDialog()
  {
    boolean isEmpty = (modelFrames.isEmpty() && modelCycles.isEmpty());
    if (!isEmpty) {

      String msg = String.format("%1$d frame(s) and %2$d cycle(s) will be discarded.\n" +
                                 "Do you really want to close the dialog?",
                                 modelFrames.size(), modelCycles.size());
      if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(this, msg, "Close dialog",
                                                                  JOptionPane.YES_NO_OPTION)) {
        isEmpty = true;
      }
    }

    return isEmpty;
  }

  // resetting dialog state
  private void clear()
  {
    previewStop();
    previewSetModified(true);
    modelFrames.clear();
    modelCycles.clear();
    modelCurCycle.clear();
    updateFramesList();
    updateStatus();
  }

  // got enough data to start conversion?
  private void updateStatus()
  {
    boolean isReady = (!modelFrames.isEmpty() && !modelCycles.isEmpty());
    boolean showTabs = !modelFrames.isEmpty();

    previewSetModified(true);

    bConvert.setEnabled(isReady && !tfBamOutput.getText().isEmpty());
    if (!showTabs) {
      tabPane.setSelectedIndex(0);
    }
    tabPane.setEnabledAt(1, showTabs);
    tabPane.setEnabledAt(2, isReady);
  }

  // toggle BAM version specific options
  private void setBamVersionEnabled(int version)
  {
    String name = (version == 1) ? "V1" : "V2";
    CardLayout cl = (CardLayout)pFramesCurFrameVersion.getLayout();
    cl.show(pFramesCurFrameVersion, name);
    cl = (CardLayout)pFramesOptionsVersion.getLayout();
    cl.show(pFramesOptionsVersion, name);
  }

  // update relevant components to reflect current state of global frames list
  private void updateFramesList()
  {
    // updating button states
    int idxMin = getLowestIndex(listFrames.getSelectedIndices());
    int idxMax = getHighestIndex(listFrames.getSelectedIndices());
    bFramesUp.setEnabled(!modelFrames.isEmpty() && idxMin > 0);
    bFramesDown.setEnabled(!modelFrames.isEmpty() && idxMin >= 0 && idxMax < modelFrames.size() - 1);

    // removing invalid frame indices from cycles
    for (int idx = modelCycles.size() - 1; idx >= 0; idx--) {
      BamCycle bc = (BamCycle)modelCycles.get(idx);
      int i = 0;
      while (i < bc.frames.size()) {
        if (bc.frames.get(i) >= modelFrames.size()) {
          bc.frames.remove(i);
        } else {
          i++;
        }
      }
      if (bc.frames.isEmpty()) {
        modelCycles.remove(idx);
      }
    }
    updateCyclesList();
    updateStatus();
  }

  // update relevant components to reflect current state of global cycles list
  private void updateCyclesList()
  {
    int idx = listCycles.getSelectedIndex();

    // updating button states
    bCyclesUp.setEnabled(!modelCycles.isEmpty() && idx > 0);
    bCyclesDown.setEnabled(!modelCycles.isEmpty() && idx >= 0 && idx < modelCycles.size() - 1);

    // updating macro buttons
    bMacroAssignFrames.setEnabled(idx != -1);
    bMacroRemoveFrames.setEnabled(idx != -1);
    bMacroDuplicate.setEnabled(idx != -1);
    bMacroSortFramesAsc.setEnabled(idx != -1);
    bMacroReverseFrames.setEnabled(idx != -1);
    bMacroRemoveAll.setEnabled(!modelCycles.isEmpty());
    bMacroReverseCycles.setEnabled(!modelCycles.isEmpty());

    initCurrentCycle(idx);
    updateStatus();
  }

  private void updateCurrentCycle()
  {
    // updating button states
    int idxMin = getLowestIndex(listCurCycle.getSelectedIndices());
    int idxMax = getHighestIndex(listCurCycle.getSelectedIndices());
    bCurCycleUp.setEnabled(!modelCurCycle.isEmpty() && idxMin > 0);
    bCurCycleDown.setEnabled(!modelCurCycle.isEmpty() && idxMin >= 0 &&
                             idxMax < modelCurCycle.size() - 1);

    bCurCycleAdd.setEnabled(!listFramesAvail.isSelectionEmpty());
    bCurCycleRemove.setEnabled(!listCurCycle.isSelectionEmpty());
  }

  // initializes the "Current cycle" section in the bottom half of the "Cycles" tab
  private void initCurrentCycle(int index)
  {
    if (index >= 0 && index < modelCycles.size()) {
      // enable components
      listFramesAvail.setEnabled(true);
      bCurCycleAdd.setEnabled(true);
      bCurCycleRemove.setEnabled(true);
      listCurCycle.setEnabled(true);

      // update group box title
      pCurrentCycle.setBorder(BorderFactory.createTitledBorder(String.format("Cycle %1$d ", index)));

      listFramesAvail.setSelectedIndices(new int[]{});

      // update current cycle list view
      int curIdx = listCurCycle.getSelectedIndex();
      modelCurCycle.clear();

      List<Integer> list = ((BamCycle)modelCycles.get(index)).frames;
      for (int i = 0; i < list.size(); i++) {
        modelCurCycle.addElement(modelFrames.get(list.get(i)));
      }

      curIdx = Math.min(modelCurCycle.size() - 1, curIdx);
      listCurCycle.setSelectedIndices(new int[]{curIdx});

      updateCurrentCycle();
    } else {
      // no cycle selected
      listFramesAvail.setSelectedIndices(new int[]{});
      listFramesAvail.setEnabled(false);
      bCurCycleAdd.setEnabled(false);
      bCurCycleRemove.setEnabled(false);
      listCurCycle.setSelectedIndices(new int[]{});
      listCurCycle.setEnabled(false);

      pCurrentCycle.setBorder(BorderFactory.createTitledBorder("No cycle selected "));
      modelCurCycle.clear();
    }
  }

  private void updateFrameInfo(int[] indices)
  {
    if (indices != null && indices.length > 0) {
      // enabling components
      tfFrameWidth.setEnabled(true);
      tfFrameHeight.setEnabled(true);
      tfFrameCenterX.setEnabled(true);
      tfFrameCenterY.setEnabled(true);
      cbCompressFrame.setEnabled(true);

      // evaluating data
      BamFrame bf = (BamFrame)modelFrames.get(indices[0]);
      int initialWidth = bf.image.getWidth(), initialHeight = bf.image.getHeight();
      int initialX = bf.centerX, initialY = bf.centerY;
      boolean initialState = bf.isCompressed;
      boolean hasChangedWidth = false, hasChangedHeight = false;
      boolean hasChangedCenterX = false, hasChangedCenterY = false;
      boolean hasChangedCompressed = false;

      for (int i = 0; i < indices.length; i++) {
        bf = (BamFrame)modelFrames.get(indices[i]);
        if (!hasChangedWidth && initialWidth != bf.image.getWidth()) {
          hasChangedWidth = true;
        }
        if (!hasChangedHeight && initialHeight != bf.image.getHeight()) {
          hasChangedHeight = true;
        }
        if (!hasChangedCenterX && initialX != bf.centerX) {
          hasChangedCenterX = true;
        }
        if (!hasChangedCenterY && initialY != bf.centerY) {
          hasChangedCenterY = true;
        }
        if (!hasChangedCompressed && initialState != bf.isCompressed) {
          hasChangedCompressed = true;
        }
        if (hasChangedWidth && hasChangedHeight && hasChangedCenterX && hasChangedCenterY &&
            hasChangedCompressed) {
          break;
        }
      }

      // setting frame info
      String title = null;
      if (indices.length > 1) {
        title = String.format("%1$d frames selected ", indices.length);
      } else {
        title = String.format("Frame %1$d ", indices[0]);
      }
      pFramesCurFrame.setBorder(BorderFactory.createTitledBorder(title));
      tfFrameWidth.setText(hasChangedWidth ? "" : Integer.toString(initialWidth));
      tfFrameHeight.setText(hasChangedHeight ? "" : Integer.toString(initialHeight));
      tfFrameCenterX.setText(hasChangedCenterX ? "" : Integer.toString(initialX));
      tfFrameCenterY.setText(hasChangedCenterY ? "" : Integer.toString(initialY));
      cbCompressFrame.setSelected(hasChangedCompressed ? false : initialState);
    } else {
      // no frame selected
      tfFrameWidth.setEnabled(false);
      tfFrameHeight.setEnabled(false);
      tfFrameCenterX.setEnabled(false);
      tfFrameCenterY.setEnabled(false);
      cbCompressFrame.setEnabled(false);

      pFramesCurFrame.setBorder(BorderFactory.createTitledBorder("No frame selected "));
      final String zero = "0";
      tfFrameWidth.setText(zero);
      tfFrameHeight.setText(zero);
      tfFrameCenterX.setText(zero);
      tfFrameCenterY.setText(zero);
      cbCompressFrame.setSelected(false);
    }
  }

  private void updateFramePreview(int[] indices)
  {
    // clear quick preview
    Graphics2D g = (Graphics2D)rcQuickPreview.getImage().getGraphics();
    try {
      g.setBackground(new Color(0, true));
      g.clearRect(0, 0, rcQuickPreview.getImage().getWidth(null), rcQuickPreview.getImage().getHeight(null));

      if (indices != null && indices.length == 1 &&
          indices[0] >= 0 && indices[0] < modelFrames.size()) {
        BamFrame bf = (BamFrame)modelFrames.get(indices[0]);
        boolean zoom = (bf.image.getWidth() > rcQuickPreview.getImage().getWidth(null) ||
                        bf.image.getHeight() > rcQuickPreview.getImage().getHeight(null));
        g = (Graphics2D)rcQuickPreview.getImage().getGraphics();
        if (zoom) {
          float fx = (float)rcQuickPreview.getImage().getWidth(null) / (float)bf.image.getWidth();
          float fy = (float)rcQuickPreview.getImage().getHeight(null) / (float)bf.image.getHeight();
          float factor = Math.min(fx, fy);
          int scaledWidth = (int)((float)bf.image.getWidth()*factor);
          int scaledHeight = (int)((float)bf.image.getHeight()*factor);
          int x = (rcQuickPreview.getImage().getWidth(null) - scaledWidth) / 2;
          int y = (rcQuickPreview.getImage().getHeight(null) - scaledHeight) / 2;
          BufferedImageOp op = new AffineTransformOp(AffineTransform.getScaleInstance(factor, factor),
                                                     AffineTransformOp.TYPE_BILINEAR);
          g.drawImage(bf.image, op, x, y);

          // draw center location
          int cx = (int)((float)bf.centerX * factor);
          int cy = (int)((float)bf.centerY * factor);
          if (cx + x >= 0 && cx + x < rcQuickPreview.getImage().getWidth(null) &&
              cy + y >= 0 && cy + y < rcQuickPreview.getImage().getHeight(null)) {
            cx += x;
            cy += y;
            g.setColor(new Color(0x008000));
            g.drawLine(cx-4, cy, cx+4, cy);
            g.drawLine(cx, cy-4, cx, cy+4);
          }
        } else {
          int x = (rcQuickPreview.getImage().getWidth(null) - bf.image.getWidth()) / 2;
          int y = (rcQuickPreview.getImage().getHeight(null) - bf.image.getHeight()) / 2;
          g.drawImage(bf.image, x, y, null);

          // draw center location
          int cx = x + bf.centerX;
          int cy = y + bf.centerY;
          if (cx >= 0 && cx < rcQuickPreview.getImage().getWidth(null) &&
              cy >= 0 && cy < rcQuickPreview.getImage().getHeight(null)) {
            g.setColor(Color.RED);
            g.drawLine(cx-4, cy, cx+4, cy);
            g.drawLine(cx, cy-4, cx, cy+4);
          }
        }
      }
    } finally {
      g.dispose();
      g = null;
      rcQuickPreview.repaint();
    }
  }

  private void updatePreview()
  {
    rcPreview.repaint();

    // updating buttons
    int cycleFrames = 0;
    if (currentCycle >= 0 && currentCycle < modelCycles.size()) {
      cycleFrames = ((BamCycle)modelCycles.get(currentCycle)).frames.size();
    }
    bPreviewCyclePrev.setEnabled(currentCycle > 0);
    bPreviewCycleNext.setEnabled(currentCycle < modelCycles.size() - 1);
    bPreviewFramePrev.setEnabled(currentFrame > 0);
    bPreviewFrameNext.setEnabled(currentFrame < cycleFrames - 1);
    lPreviewCycle.setText(String.format("Cycle: %1$d/%2$d", currentCycle+1, modelCycles.size()));
    lPreviewFrame.setText(String.format("Frame: %1$d/%2$d", currentFrame+1, cycleFrames));

    bPreviewPlay.setEnabled(!previewIsPlaying());
    bPreviewStop.setEnabled(previewIsPlaying());
  }

  private int getPvrzStartIndex()
  {
    int idx = 0;
    try {
      idx = Integer.parseInt(((SpinnerNumberModel)sPvrzIndex.getModel()).getValue().toString());
    } catch (Exception e) {
      idx = 0;
    }

    return idx;
  }

  private DxtEncoder.DxtType getDxtType()
  {
    switch (cbCompression.getSelectedIndex()) {
      case 1:  return DxtEncoder.DxtType.DXT1;
      case 2:  return DxtEncoder.DxtType.DXT5;
      default: return null;
    }
  }

  private int getLowestIndex(int[] indices)
  {
    int ret = -1;
    if (indices != null && indices.length > 0) {
      ret = Integer.MAX_VALUE;
      for (int i = 0; i < indices.length; i++) {
        if (indices[i] < ret) {
          ret = indices[i];
        }
      }
    }
    return ret;
  }

  private int getHighestIndex(int[] indices)
  {
    int ret = -1;
    if (indices != null && indices.length > 0) {
      for (int i = 0; i < indices.length; i++) {
        if (indices[i] > ret) {
          ret = indices[i];
        }
      }
    }
    return ret;
  }

  // specify BAM output file
  private void setBamOutput()
  {
    String rootPath = null;
    if (!tfBamOutput.getText().isEmpty()) {
      rootPath = tfBamOutput.getText();
    }
    File outFile = getSaveFileName(this, "Specify output file", rootPath,
                                   new FileNameExtensionFilter[]{getBamFilter()}, 0);
    if (outFile != null) {
      String fileName = setFileExtension(outFile.toString(), "BAM");
      tfBamOutput.setText(fileName);
      updateStatus();
    }
  }

  private boolean isBam(File bamFile)
  {
    if (bamFile != null && bamFile.exists() && bamFile.isFile()) {
      ResourceEntry entry = new FileResourceEntry(bamFile);
      return BamDecoder.isValid(entry);
    }
    return false;
  }

  private BamFrame[] loadBamFrames(File bamFile)
  {
    if (bamFile != null && bamFile.exists() && bamFile.isFile()) {
      ResourceEntry entry = new FileResourceEntry(bamFile);
      BamDecoder.BamType type = BamDecoder.getType(entry);
      if (type == BamDecoder.BamType.BAMV1 || type == BamDecoder.BamType.BAMC) {
        // use BamDecoder
        List<BamFrame> frameList = new ArrayList<BamFrame>();
        if (loadBamV1Frames(entry, frameList, null)) {
          BamFrame[] frames = new BamFrame[frameList.size()];
          for (int i = 0; i < frames.length; i++) {
            frames[i] = frameList.get(i);
          }
          return frames;
        }
      } else if (type == BamDecoder.BamType.BAMV2) {
        // decode manually and use PvrzDecoder to fetch graphics data
        List<BamFrame> frameList = new ArrayList<BamFrame>();
        if (loadBamV2Frames(entry, frameList, null)) {
          BamFrame[] frames = new BamFrame[frameList.size()];
          for (int i = 0; i < frames.length; i++) {
            frames[i] = frameList.get(i);
          }
          return frames;
        }
      }
    }
    return null;
  }

  // specify predefined BAM palette for BAM V1 export
  private void setBamPalette()
  {
    String rootPath = null;
    if (!tfBamPalette.getText().isEmpty()) {
      rootPath = tfBamPalette.getText();
    }
    File outFile = getSaveFileName(this, "Specify palette file", rootPath, getPaletteFilters(), 0);
    if (outFile != null) {
      if (hasPalette(outFile)) {
        tfBamPalette.setText(outFile.toString());
        lBamPalette.setEnabled(true);
      } else {
        String msg = String.format("No palette found in \"%1$s\".", outFile.getName());
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private boolean hasPalette(File palFile)
  {
    boolean retVal = false;
    if (palFile != null && palFile.exists() && palFile.isFile()) {
      FileInputStream fis = null;
      try {
        try {
          fis = new FileInputStream(palFile);
          byte[] signature = new byte[8];
          int len = fis.read(signature);
          if (len == signature.length) {
            if (signature[0] == 0x42 && signature[1] == 0x4d) {
              // BMP file?
              byte[] header = new byte[54];
              System.arraycopy(signature, 0, header, 0, signature.length);
              len = fis.read(header, signature.length, header.length - signature.length);
              if (len >= header.length - signature.length) {
                if (DynamicArray.getShort(header, 0x00) == 0x4d42&&   // signature = 'BM'
                    DynamicArray.getInt(header, 0x0e) == 0x28 &&      // correct BMP header size
                    DynamicArray.getInt(header, 0x12) > 0 &&          // valid width
                    DynamicArray.getInt(header, 0x16) > 0 &&          // valid height
                    (DynamicArray.getShort(header, 0x1c) == 4 ||      // either 4bpp
                     DynamicArray.getInt(header, 0x1c) == 8) &&       // or 8bpp
                    DynamicArray.getInt(header, 0x1e) == 0) {         // no special encoding
                  retVal = true;
                }
              }
            } else {
              // BAM V1 or BAMC V1?
              String s = new String(signature, Charset.forName("US-ASCII"));
              if ("BAM V1  ".equals(s) || "BAMCV1  ".equals(s)) {
                retVal = true;
              }
            }
          }
          fis.close();
        } catch (Exception e) {
          e.printStackTrace();
          if (fis != null) {
            fis.close();
          }
        }
      } catch (Exception e) {
        fis = null;
      }
    }
    return retVal;
  }

  // extracts the palette from the specified file
  private int[] loadPaletteFromFile(String fileName)
  {
    if (fileName != null && !fileName.isEmpty()) {
      File file = new File(fileName);
      FileInputStream fis = null;
      try {
        try {
          fis = new FileInputStream(file);
          byte[] signature = new byte[8];
          int len = fis.read(signature);
          if (len == signature.length) {
            if (signature[0] == 0x42 && signature[1] == 0x4d) {
              // BMP file
              byte[] data = new byte[(int)file.length()];
              System.arraycopy(signature, 0, data, 0, signature.length);
              fis.read(data, signature.length, data.length - signature.length);
              fis.close();
              return loadPaletteFromBMP(data);
            } else {
              String s = new String(signature, Charset.forName("US-ASCII"));
              if ("BAM V1  ".equals(s) || "BAMCV1  ".equals(s)) {
                // BAM V1 or BAMC V1
                byte[] data = new byte[(int)file.length()];
                System.arraycopy(signature, 0, data, 0, signature.length);
                fis.read(data, signature.length, data.length - signature.length);
                fis.close();
                return loadPaletteFromBAM(data);
              }
            }
          }
          fis.close();
        } catch (Exception e) {
          if (fis != null) {
            fis.close();
          }
        }
      } catch (Exception e) {
        fis = null;
      }
    }
    return null;
  }

  // return palette from 4-bit or 8-bit uncompressed BMP
  private int[] loadPaletteFromBMP(byte[] buf)
  {
    if (buf != null && buf.length > 54) {
      int sig = DynamicArray.getUnsignedShort(buf, 0);
      if (sig == 0x4d42) {
        if (DynamicArray.getInt(buf, 0x0e) == 0x28 &&
            DynamicArray.getInt(buf, 0x12) > 0 &&
            DynamicArray.getInt(buf, 0x16) > 0 &&
            (DynamicArray.getInt(buf, 0x1c) == 4 || DynamicArray.getInt(buf, 0x1c) == 8)) {
          int colors = 1 << DynamicArray.getUnsignedShort(buf, 0x1c);
          int[] palette = new int[colors];
          int ofs = 0x36;
          for (int i = 0; i < colors; i++) {
            palette[i] = DynamicArray.getInt(buf, ofs + i*4);
          }
          return palette;
        }
      }
    }
    return null;
  }

  // return palette from BAM V1 or BAMC V1
  private int[] loadPaletteFromBAM(byte[] buf)
  {
    if (buf != null) {
      String sig = DynamicArray.getString(buf, 0, 8);
      if ("BAMCV1  ".equals(sig)) {
        try {
          buf = Compressor.decompress(buf);
          sig = DynamicArray.getString(buf, 0, 8);
        } catch (Exception e) {
          return null;
        }
      }
      if ("BAM V1  ".equals(sig)) {
        int ofs = DynamicArray.getInt(buf, 0x10);
        int[] palette = new int[256];
        for (int i = 0; i < 256; i++) {
          palette[i] = DynamicArray.getInt(buf, ofs + i*4);
        }
        return palette;
      }
    }
    return null;
  }


  // imports both frames and cycles
  private boolean importBam(File bamFile)
  {
    String errorMsg = null;
    if (bamFile != null && bamFile.exists() && bamFile.isFile()) {
      ResourceEntry entry = new FileResourceEntry(bamFile);
      BamDecoder.BamType type = BamDecoder.getType(entry);
      List<BamFrame> frameList = new ArrayList<BamFrame>();
      List<BamCycle> cycleList = new ArrayList<BamCycle>();
      boolean result = false;
      if (type == BamDecoder.BamType.BAMV1 || type == BamDecoder.BamType.BAMC) {
        result = loadBamV1Frames(entry, frameList, cycleList);
      } else if (type == BamDecoder.BamType.BAMV2) {
        result = loadBamV2Frames(entry, frameList, cycleList);
      } else {
        errorMsg = String.format("Unrecognized BAM file: \"%1$s\".", bamFile.toString());
      }

      if (result) {
        for (int i = 0; i < frameList.size(); i++) {
          modelFrames.addElement(frameList.get(i));
        }
        for (int i = 0; i < cycleList.size(); i++) {
          modelCycles.addElement(cycleList.get(i));
        }
        return true;
      }
    } else {
      errorMsg = String.format("File not found: \"%1$s\".", bamFile.toString());
    }

    if (errorMsg != null) {
      JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

  private boolean loadBamV1Frames(ResourceEntry entry, List<BamFrame> frameList, List<BamCycle> cycleList)
  {
    String errorMsg = null;
    if (entry != null) {
      BamDecoder decoder = new BamDecoder(entry);
      if (decoder.data() != null) {
        // filling frames list
        if (frameList != null) {
          for (int i = 0; i < decoder.data().frameCount(); i++) {
            BufferedImage img = ColorConvert.toBufferedImage(decoder.data().frameGet(i), true);
            BamFrame bf = new BamFrame(entry.getActualFile().toString(), i, img,
                                       decoder.data().frameCenterX(i), decoder.data().frameCenterY(i),
                                       decoder.data().frameCompressed(i));
            frameList.add(bf);
          }
        }

        // filling cycles list
        if (cycleList != null) {
          for (int i = 0; i < decoder.data().cycleCount(); i++) {
            decoder.data().cycleSet(i);
            BamCycle bc = new BamCycle();
            for (int j = 0; j < decoder.data().cycleFrameCount(); j++) {
              bc.frames.add(new Integer(decoder.data().cycleGetFrameIndexAbs(j)));
            }
            cycleList.add(bc);
          }
        }

        return true;
      } else {
        errorMsg = String.format("Error while loading \"%1$s\".", entry.getActualFile().toString());
      }
    }
    if (errorMsg != null) {
      JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }

  private boolean loadBamV2Frames(ResourceEntry entry, List<BamFrame> frameList, List<BamCycle> cycleList)
  {
    if (entry != null) {
      try {
        byte[] bamData = entry.getResourceData();
        // getting BAM header data
        int frameCount = DynamicArray.getInt(bamData, 0x08);
        int cycleCount = DynamicArray.getInt(bamData, 0x0c);
        int ofsFrame = DynamicArray.getInt(bamData, 0x14);
        int ofsCycle = DynamicArray.getInt(bamData, 0x18);
        int ofsData = DynamicArray.getInt(bamData, 0x1c);
        // lists required to eliminate duplicate frame data
        List<List<FrameDataV2>> frameDataList = new ArrayList<List<FrameDataV2>>(frameCount);
        List<Integer> frameRefList = new ArrayList<Integer>(frameCount);
        // using PVR cache for faster loading
        ConcurrentHashMap<Integer, PvrDecoder> pvrTable = new ConcurrentHashMap<Integer, PvrDecoder>(20);

        // filling frames list
        if (frameList != null) {
          for (int i = 0; i < frameCount; i++) {
            // fetching frame definitions
            int ofs = ofsFrame + i*12;
            int w = DynamicArray.getShort(bamData, ofs);
            int h = DynamicArray.getShort(bamData, ofs+2);
            int cx = DynamicArray.getShort(bamData, ofs+4);
            int cy = DynamicArray.getShort(bamData, ofs+6);
            int dataIdx = DynamicArray.getShort(bamData, ofs+8);
            int dataCount = DynamicArray.getShort(bamData, ofs+10);

            List<FrameDataV2> details = new ArrayList<FrameDataV2>(dataCount);
            for (int j = 0; j < dataCount; j++) {
              // fetching frame data segments
              ofs = ofsData + (dataIdx+j)*28;
              int page = DynamicArray.getInt(bamData, ofs);
              int sx = DynamicArray.getInt(bamData, ofs+4);
              int sy = DynamicArray.getInt(bamData, ofs+8);
              int bw = DynamicArray.getInt(bamData, ofs+12);
              int bh = DynamicArray.getInt(bamData, ofs+16);
              int dx = DynamicArray.getInt(bamData, ofs+20);
              int dy = DynamicArray.getInt(bamData, ofs+24);
              details.add(new FrameDataV2(page, sx, sy, bw, bh, dx, dy));
            }
            frameRefList.add(new Integer(i));
            frameDataList.add(details);

            // check for duplicate entries
            boolean match = false;
            int matchIdx = -1;
            for (int j = 0; j < frameDataList.size() - 1; j++) {
              List<FrameDataV2> fd = frameDataList.get(j);
              match = (fd.size() == details.size());
              matchIdx = j;
              if (match) {
                for (int k = 0; k < fd.size(); k++) {
                  if (!fd.get(k).equals(details.get(k))) {
                    match = false;
                    break;
                  }
                }
              }
              if (match)
                break;
            }
            if (match) {
              // redirect to previous frame
              frameRefList.set(i, new Integer(matchIdx));
            } else {
              // add new frame to the list
              BufferedImage image = ColorConvert.createCompatibleImage(w, h, true);
              Graphics2D g = (Graphics2D)image.getGraphics();
              PvrDecoder decoder = null;
              try {
                for (int j = 0; j < details.size(); j++) {
                  FrameDataV2 fd = details.get(j);
                  // make use of PVR cache
                  decoder = pvrTable.get(new Integer(fd.page));
                  if (decoder == null) {
                    File pvrzFile = new File(entry.getActualFile().getParent(),
                                             String.format("MOS%1$04d.PVRZ", fd.page));
                    if (pvrzFile.exists() && pvrzFile.isFile()) {
                      ResourceEntry pvrEntry = new FileResourceEntry(pvrzFile);
                      try {
                        byte[] data = Compressor.decompress(pvrEntry.getResourceData(), 0);
                        decoder = new PvrDecoder(data);
                        pvrTable.put(new Integer(fd.page), decoder);
                      } catch (Exception  e) {
                        decoder = null;
                      }
                    } else {
                      String errorMsg = String.format("File \"%1$s\" not found.", pvrzFile.toString());
                      JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                      return false;
                    }
                    if (decoder == null) {
                      String errorMsg = String.format("Error while processing \"%1$s\".", pvrzFile.toString());
                      JOptionPane.showMessageDialog(this, errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                      return false;
                    }
                  }

                  // load block of pixel data
                  Image srcImg = getPvrzBlock(decoder, fd.sx, fd.sy, fd.width, fd.height);
                  if (srcImg != null) {
                    g.drawImage(srcImg, fd.dx, fd.dy, null);
                  } else {
                    return false;
                  }
                  srcImg = null;
                }
              } finally {
                g.dispose();
              }

              // store resulting frame
              BamFrame bf = new BamFrame(entry.getActualFile().toString(), i, image, cx, cy, false);
              frameList.add(bf);
            }
          }
        }

        // clean up PVR cache
        for (final PvrDecoder decoder: pvrTable.values()) {
          decoder.close();
        }
        pvrTable.clear();

        // filling cycles list
        if (cycleList != null) {
          for (int i = 0; i < cycleCount; i++) {
            BamCycle bc = new BamCycle();
            int ofs = ofsCycle + i*4;
            int cnt = DynamicArray.getShort(bamData, ofs);
            int idx = DynamicArray.getShort(bamData, ofs+2);
            for (int j = 0; j < cnt; j++) {
              bc.frames.add(frameRefList.get(idx+j));
            }
            // store resulting cycle
            cycleList.add(bc);
          }
        }

        return true;
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return false;
  }

  // returns whether the specified image contains transparent areas that can be RLE compressed (BAM V1 only)
  private boolean imageIsCompressable(BufferedImage image)
  {
    boolean isCompressable = false;
    if (image != null) {
      try {
        int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        if (pixels != null) {
          final int transThreshold = 0x20;
          for (int i = 0; i < pixels.length; i++) {
            if ((pixels[i] >>> 24) < transThreshold) {
              isCompressable = true;
              break;
            }
          }
        }
        pixels = null;
        image.flush();
      } catch (Exception e) {
      }
    }
    return isCompressable;
  }

  // returns a pixel block from a PVRZ
  private Image getPvrzBlock(PvrDecoder decoder, int x, int y, int width, int height)
  {
    if (decoder != null && decoder.isOpen()) {
      BufferedImage image = ColorConvert.createCompatibleImage(width, height, true);
      try {
        if (decoder.decode(image, x, y, width, height)) {
          return image;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      image = null;
    }
    return null;
  }

  private BamFrame loadImage(File file)
  {
    BamFrame bf = null;
    if (file != null && file.exists() && file.isFile()) {
      try {
        BufferedImage image = ColorConvert.toBufferedImage(ImageIO.read(file), true);
        if (image != null) {
          boolean IsCompressable = imageIsCompressable(image);
          bf = new BamFrame(file.toString(), image, 0, 0, IsCompressable);
        }
      } catch (Exception e) {
      }
    }
    return bf;
  }

  private void framesAdd()
  {
    String rootPath = null;
    if (!modelFrames.isEmpty()) {
      rootPath = new File(((BamFrame)modelFrames.get(modelFrames.size() - 1)).fileName).getParent();
    }
    File[] files = getOpenFileName(this, "Choose file(s)", rootPath, true, getGraphicsFilters(), 0);
    if (files != null) {
      List<String> skippedFiles = new ArrayList<String>();
      int idx = listFrames.getSelectedIndex() + 1;
      for (final File file: files) {
        if (isBam(file)) {
          // import all BAM frames
          BamFrame[] frames = loadBamFrames(file);
          if (frames != null) {
            for (int i = 0; i < frames.length; i++) {
              modelFrames.add(idx, frames[i]);
              idx++;
            }
            frames = null;
          } else {
            // error: no valid BAM
            skippedFiles.add(file.toString());
          }
        } else {
          // import current graphics
          BamFrame bf = loadImage(file);
          if (bf != null) {
            modelFrames.add(idx, bf);
            idx++;
          } else {
            // error: no valid graphics file
            skippedFiles.add(file.toString());
          }
        }
      }
      updateFramesList();
      listFrames.setSelectedIndex(idx - 1);
      listFrames.requestFocus();

      // preparing error message (if any)
      if (!skippedFiles.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        if (skippedFiles.size() == 1) {
          sb.append(String.format("%1$d file has been skipped:\n", skippedFiles.size()));
        } else {
          sb.append(String.format("%1$d files have been skipped:\n", skippedFiles.size()));
        }
        for (int i = 0; i < Math.min(5, skippedFiles.size()); i++) {
          sb.append(String.format("  - %1$s\n", skippedFiles.get(i)));
        }
        if (skippedFiles.size() > 5) {
          sb.append("  - ...\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void framesImportBam()
  {
    String rootPath = null;
    if (!modelFrames.isEmpty()) {
      rootPath = new File(((BamFrame)modelFrames.get(modelFrames.size() - 1)).fileName).getParent();
    }
    File[] files = getOpenFileName(this, "Import BAM file", rootPath, false,
                                   new FileNameExtensionFilter[]{getBamFilter()}, 0);
    if (files != null && files.length > 0) {
      boolean cancelled = false;
      if (!modelFrames.isEmpty()) {
        String msg = "Existing frame and cycle entries will be overwritten.\nDo you want to continue?";
        int ret = JOptionPane.showConfirmDialog(this, msg, "Question", JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE);
        cancelled = (ret != JOptionPane.YES_OPTION);
      }
      if (!cancelled) {
        clear();
        importBam(files[0]);
        if (!modelCycles.isEmpty()) {
          listCycles.setSelectedIndex(0);
        }
        updateFramesList();
        listFrames.setSelectedIndex(modelFrames.size() - 1);
        listFrames.requestFocus();
      }
    }
  }

  private void framesAddFolder()
  {
    String rootPath = null;
    if (!modelFrames.isEmpty()) {
      rootPath = new File(((BamFrame)modelFrames.get(modelFrames.size() - 1)).fileName).getParent();
    }
    File path = getOpenPathName(this, "Choose folder", rootPath);
    if (path != null && path.isDirectory()) {
      // adding all files in the directory
      FileNameExtensionFilter[] filters = getGraphicsFilters();
      File[] fileList = path.listFiles();
      List<String> skippedFiles = new ArrayList<String>();
      int idx = listFrames.getSelectedIndex() + 1;
      for (final File file: fileList) {
        for (final FileNameExtensionFilter filter: filters) {
          if (file != null && file.isFile() && filter.accept(file)) {
            if (isBam(file)) {
              // import all BAM frames
              BamFrame[] frames = loadBamFrames(file);
              if (frames != null) {
                for (int i = 0; i < frames.length; i++) {
                  modelFrames.add(idx, frames[i]);
                  idx++;
                }
              } else {
                // error: no valid BAM
                skippedFiles.add(file.toString());
              }
            } else {
              // import current graphics
              BamFrame bf = loadImage(file);
              if (bf != null) {
                modelFrames.add(idx, bf);
                idx++;
              } else {
                // error: no valid graphics file
                skippedFiles.add(file.toString());
              }
            }
            break;
          }
        }
      }
      updateFramesList();
      listFrames.setSelectedIndex(idx - 1);
      listFrames.requestFocus();

      // preparing error message (if any)
      if (!skippedFiles.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        if (skippedFiles.size() == 1) {
          sb.append(String.format("%1$d file has been skipped:\n", skippedFiles.size()));
        } else {
          sb.append(String.format("%1$d files have been skipped:\n", skippedFiles.size()));
        }
        for (int i = 0; i < Math.min(5, skippedFiles.size()); i++) {
          sb.append(String.format("  - %1$s\n", skippedFiles.get(i)));
        }
        if (skippedFiles.size() > 5) {
          sb.append("  - ...\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void framesRemove()
  {
    int curIdx = Integer.MAX_VALUE;
    int indices[] = listFrames.getSelectedIndices();
    if (indices != null && indices.length > 0) {
      for (int i = indices.length - 1; i >= 0; i--) {
        modelFrames.remove(indices[i]);
        if (indices[i] < curIdx) {
          curIdx = indices[i];
        }
      }
      updateFramesList();
      curIdx = Math.min(modelFrames.size() - 1, curIdx);
      listFrames.setSelectedIndex(curIdx);
      listFrames.requestFocus();
    }
  }

  private void framesRemoveAll()
  {
    if (!modelFrames.isEmpty()) {
      modelFrames.clear();
      updateFramesList();
      listFrames.requestFocus();
    }
  }

  private void framesMoveUp()
  {
    int[] indices = listFrames.getSelectedIndices();
    for (int i = 0; i < indices.length; i++) {
      if (indices[i] > 0 && indices[i] < modelFrames.size()) {
        BamFrame bf = (BamFrame)modelFrames.get(indices[i]-1);
        modelFrames.set(indices[i]-1, modelFrames.get(indices[i]));
        modelFrames.set(indices[i], bf);
        indices[i]--;
      }
    }
    listFrames.setSelectedIndices(indices);
    updateFramesList();
    listFrames.requestFocus();
  }

  private void framesMoveDown()
  {
    int[] indices = listFrames.getSelectedIndices();
    for (int i = indices.length - 1; i >= 0; i--) {
      if (indices[i] >= 0 && indices[i] < modelFrames.size() - 1) {
        BamFrame bf = (BamFrame)modelFrames.get(indices[i]+1);
        modelFrames.set(indices[i]+1, modelFrames.get(indices[i]));
        modelFrames.set(indices[i], bf);
        indices[i]++;
      }
    }
    listFrames.setSelectedIndices(indices);
    updateFramesList();
    listFrames.requestFocus();
  }

  private void cyclesAdd()
  {
    BamCycle bc = new BamCycle();
    modelCycles.add(listCycles.getSelectedIndex() + 1, bc);
    listCycles.setSelectedIndex(listCycles.getSelectedIndex() + 1);
    updateCyclesList();
  }

  private void cyclesRemove()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx >= 0) {
      modelCycles.remove(listCycles.getSelectedIndex());
      if (idx > 0) {
        listCycles.setSelectedIndex(idx-1);
      } else if (idx < modelCycles.size()) {
        listCycles.setSelectedIndex(idx);
      }
      updateCyclesList();
    }
  }

  private void cyclesRemoveAll()
  {
    modelCycles.clear();
    updateCyclesList();
  }

  private void cyclesMoveUp()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx > 0) {
      BamCycle bc = (BamCycle)modelCycles.get(idx - 1);
      modelCycles.set(idx - 1, modelCycles.get(idx));
      modelCycles.set(idx, bc);
      listCycles.setSelectedIndex(idx - 1);
      updateCyclesList();
    }
  }

  private void cyclesMoveDown()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx >= 0 && idx < modelCycles.size() - 1) {
      BamCycle bc = (BamCycle)modelCycles.get(idx + 1);
      modelCycles.set(idx + 1, modelCycles.get(idx));
      modelCycles.set(idx, bc);
      listCycles.setSelectedIndex(idx + 1);
      updateCyclesList();
    }
  }

  private void macroAssignFrames()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx >= 0) {
      BamCycle bc = (BamCycle)modelCycles.get(idx);
      bc.frames.clear();
      for (int i = 0; i < modelFrames.size(); i++) {
        bc.frames.add(new Integer(i));
      }
      modelCycles.set(idx, bc);
      updateCyclesList();
    }
  }

  private void macroRemoveFrames()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx >= 0) {
      BamCycle bc = (BamCycle)modelCycles.get(idx);
      bc.frames.clear();
      modelCycles.set(idx, bc);
      updateCyclesList();
    }
  }

  private void macroDuplicateCycle()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx >= 0) {
      BamCycle bc = (BamCycle)((BamCycle)modelCycles.get(idx)).clone();
      modelCycles.add(idx+1, bc);
      listCycles.setSelectedIndex(idx+1);
    }
  }

  private void macroSortFrames()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx >= 0) {
      BamCycle bc = (BamCycle)modelCycles.get(idx);
      Collections.sort(bc.frames);
      modelCycles.set(idx, bc);
      updateCyclesList();
    }
  }

  private void macroReverseFramesOrder()
  {
    int idx = listCycles.getSelectedIndex();
    if (idx >= 0) {
      BamCycle bc = (BamCycle)modelCycles.get(idx);
      int up = 0, down = bc.frames.size() - 1;
      while (up < down) {
        Integer i = bc.frames.get(up);
        bc.frames.set(up, bc.frames.get(down));
        bc.frames.set(down, i);
        up++;
        down--;
      }
      modelCycles.set(idx, bc);
      updateCyclesList();
    }
  }

  private void macroRemoveAll()
  {
    for (int i = 0; i < modelCycles.size(); i++) {
      BamCycle bc = (BamCycle)modelCycles.get(i);
      bc.frames.clear();
      modelCycles.set(i, bc);
    }
    updateCyclesList();
  }

  private void macroReverseCyclesOrder()
  {
    int idx = listCycles.getSelectedIndex();
    int up = 0, down = modelCycles.size() - 1;
    while (up < down) {
      BamCycle bc = (BamCycle)modelCycles.get(up);
      modelCycles.set(up, modelCycles.get(down));
      modelCycles.set(down, bc);
      up++;
      down--;
    }
    listCycles.setSelectedIndex(modelCycles.size() - idx - 1);
    updateCyclesList();
  }

  private void curCycleAdd()
  {
    BamCycle bc = (BamCycle)modelCycles.get(listCycles.getSelectedIndex());
    int[] indices = listFramesAvail.getSelectedIndices();
    int dstIdx = getHighestIndex(listCurCycle.getSelectedIndices()) + 1;
    for (int i = 0; i < indices.length; i++) {
      BamFrame bf = (BamFrame)modelFrames.get(indices[i]);
      modelCurCycle.add(dstIdx, bf);
      bc.frames.add(dstIdx, new Integer(indices[i]));
      dstIdx++;
    }
    modelCycles.set(listCycles.getSelectedIndex(), bc);
    listFramesAvail.setSelectedIndices(new int[]{});
    updateCurrentCycle();
  }

  private void curCycleRemove()
  {
    BamCycle bc = (BamCycle)modelCycles.get(listCycles.getSelectedIndex());
    int[] indices = listCurCycle.getSelectedIndices();
    for (int i = indices.length - 1; i >= 0 ; i--) {
      modelCurCycle.remove(indices[i]);
      bc.frames.remove(indices[i]);
    }
    modelCycles.set(listCycles.getSelectedIndex(), bc);
    listCurCycle.setSelectedIndices(new int[]{});
    updateCurrentCycle();
  }

  private void curCycleMoveUp()
  {
    BamCycle bc = (BamCycle)modelCycles.get(listCycles.getSelectedIndex());
    int[] indices = listCurCycle.getSelectedIndices();
    for (int i = 0; i < indices.length; i++) {
      if (indices[i] > 0 && indices[i] < modelCurCycle.size()) {
        BamFrame bf = (BamFrame)modelCurCycle.get(indices[i]-1);
        modelCurCycle.set(indices[i]-1, modelCurCycle.get(indices[i]));
        modelCurCycle.set(indices[i], bf);
        Integer v = bc.frames.get(indices[i]-1);
        bc.frames.set(indices[i]-1, bc.frames.get(indices[i]));
        bc.frames.set(indices[i], v);
        indices[i]--;
      }
    }
    modelCycles.set(listCycles.getSelectedIndex(), bc);
    listCurCycle.setSelectedIndices(indices);
    listCurCycle.requestFocus();
    updateCurrentCycle();
  }

  private void curCycleMoveDown()
  {
    BamCycle bc = (BamCycle)modelCycles.get(listCycles.getSelectedIndex());
    int[] indices = listCurCycle.getSelectedIndices();
    for (int i = 0; i < indices.length; i++) {
      if (indices[i] >= 0 && indices[i] < modelCurCycle.size() - 1) {
        BamFrame bf = (BamFrame)modelCurCycle.get(indices[i]+1);
        modelCurCycle.set(indices[i]+1, modelCurCycle.get(indices[i]));
        modelCurCycle.set(indices[i], bf);
        Integer v = bc.frames.get(indices[i]+1);
        bc.frames.set(indices[i]+1, bc.frames.get(indices[i]));
        bc.frames.set(indices[i], v);
        indices[i]++;
      }
    }
    modelCycles.set(listCycles.getSelectedIndex(), bc);
    listCurCycle.setSelectedIndices(indices);
    listCurCycle.requestFocus();
    updateCurrentCycle();
  }

  private void previewPlay()
  {
    if (!previewIsPlaying()) {
      timer.start();
      isPreviewPlaying = true;
      updatePreview();
    }
  }

  private boolean previewIsPlaying()
  {
    return isPreviewPlaying;
  }

  private void previewStop()
  {
    if (previewIsPlaying()) {
      timer.stop();
      isPreviewPlaying = false;
      updatePreview();
    }
  }

  private void previewCycleDec()
  {
    if (currentCycle > 0) {
      currentCycle--;
      currentFrame = 0;
      previewDisplay();
      updatePreview();
    }
  }

  private void previewCycleInc()
  {
    if (currentCycle < modelCycles.size() - 1) {
      currentCycle++;
      currentFrame = 0;
      previewDisplay();
      updatePreview();
    }
  }

  private void previewFrameDec()
  {
    if (currentFrame > 0) {
      currentFrame--;
      previewDisplay();
      updatePreview();
    }
  }

  private void previewFrameInc()
  {
    int size = ((BamCycle)modelCycles.get(currentCycle)).frames.size();
    if (currentFrame < size - 1) {
      currentFrame++;
      previewDisplay();
      updatePreview();
    }
  }

  // specify whether the preview has to be (re-)created
  private void previewSetModified(boolean state)
  {
    if (state != isPreviewModified) {
      isPreviewModified = state;
      if (isPreviewModified == true) {
        previewStop();
        currentCycle = currentFrame = 0;
      }
    }
  }

  private boolean previewIsModified()
  {
    return isPreviewModified;
  }

  private int previewGetMode()
  {
    return cbPreviewMode.getSelectedIndex();
  }

  private void previewSetFrameRate()
  {
    try {
      SpinnerNumberModel model = (SpinnerNumberModel)sPreviewFps.getModel();

      int min = Integer.parseInt(model.getMinimum().toString());
      int max = Integer.parseInt(model.getMaximum().toString());
      int rate = Integer.parseInt(model.getValue().toString());
      if (rate != currentFps && rate >= min && rate <= max) {
        currentFps = rate;
        timer.setInitialDelay(1000 / currentFps);
        timer.setDelay(1000 / currentFps);
      }
    } catch (Exception e) {
    }
  }

  private int previewGetZoom()
  {
    return cbPreviewZoom.isSelected() ? 2 : 1;
  }

  private void previewPrepare(boolean forceUpdate)
  {
    if (forceUpdate || previewIsModified()) {
      previewSetModified(false);

      // calculate bounding box that is able to contain all available frames without cropping
      int x1 = Integer.MAX_VALUE, x2 = Integer.MIN_VALUE;
      int y1 = Integer.MAX_VALUE, y2 = Integer.MIN_VALUE;
      for (int i = 0; i < modelFrames.size(); i++) {
        BamFrame bf = (BamFrame)modelFrames.get(i);
        x1 = Math.min(x1, -bf.centerX);
        y1 = Math.min(y1, -bf.centerY);
        x2 = Math.max(x2, bf.image.getWidth() - bf.centerX);
        y2 = Math.max(y2, bf.image.getHeight() - bf.centerY);
      }
      // create bounding box and add enough space for the origin marker
      previewBounds = new Rectangle(x1 - 5, y1 - 5, (x2 - x1 + 1) + 10, (y2 - y1 + 1) + 10);
      int zoom = previewGetZoom();
      rcPreview.setImage(ColorConvert.createCompatibleImage(previewBounds.width*zoom,
                                                            previewBounds.height*zoom, true));
      // show content
      previewDisplay();
    }
  }

  // displays the current state of the preview BAM
  private void previewDisplay()
  {
    // fetching correct frame
    BamFrame bf = null;
    if (!previewIsModified()) {
      if (currentCycle >= 0 && currentCycle < modelCycles.size()) {
        List<Integer> list = ((BamCycle)modelCycles.get(currentCycle)).frames;
        if (currentFrame >= 0 && currentFrame < list.size()) {
          int frameIdx = list.get(currentFrame).intValue();
          if (frameIdx >= 0 && frameIdx < modelFrames.size()) {
            bf = (BamFrame)modelFrames.get(frameIdx);
          }
        }
      }
    }

    int zoom = previewGetZoom();
    Image image = rcPreview.getImage();
    Graphics2D g = (Graphics2D)image.getGraphics();
    try {
      g.setBackground(new Color(0, true));
      g.clearRect(0, 0, image.getWidth(null), image.getHeight(null));
      if (bf != null) {
        // draw frame image if available
        int dx = (-previewBounds.x - bf.centerX)*zoom;
        int dy = (-previewBounds.y - bf.centerY)*zoom;
        BufferedImageOp op =
            new AffineTransformOp(AffineTransform.getScaleInstance((double)zoom, (double)zoom),
                                  AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        g.drawImage(bf.image, op, dx, dy);
      }
      if (cbPreviewShowMarker.isSelected()) {
        // draw bounding box
        g.setColor(new Color(0x00A000));
        g.drawRect(4*zoom+(zoom-1), 4*zoom+(zoom-1),
                   image.getWidth(null) - 10*zoom-(zoom-1), image.getHeight(null) - 10*zoom-(zoom-1));
        // draw origin marker
        g.setStroke(new BasicStroke(3.0f));
        g.setColor(Color.BLACK);
        g.drawLine(-previewBounds.x*zoom - 4, -previewBounds.y*zoom,
                   -previewBounds.x*zoom + 4, -previewBounds.y*zoom);
        g.drawLine(-previewBounds.x*zoom, -previewBounds.y*zoom - 4,
                   -previewBounds.x*zoom, -previewBounds.y*zoom + 4);
        g.setStroke(new BasicStroke(1.0f));
        g.setColor(Color.RED);
        g.drawLine(-previewBounds.x*zoom - 4, -previewBounds.y*zoom,
                   -previewBounds.x*zoom + 4, -previewBounds.y*zoom);
        g.drawLine(-previewBounds.x*zoom, -previewBounds.y*zoom - 4,
                   -previewBounds.x*zoom, -previewBounds.y*zoom + 4);
      }
    } finally {
      g.dispose();
      g = null;
      rcPreview.repaint();
    }
  }

  private void previewAdvanceAnimation()
  {
    int mode = previewGetMode();
    int cyclesCount = modelCycles.size();
    int framesCount = ((BamCycle)modelCycles.get(currentCycle)).frames.size();

    // advances the animation by one frame, depending on current playback mode
    int retries = cyclesCount;
    do {
      if (currentFrame >= framesCount - 1) {
        switch (mode) {
          case 0:     // current cycle only, play once
            currentFrame = 0;
            previewDisplay();
            previewStop();
            return;
          case 1:     // current cycle only, play looped
            currentFrame = 0;
            break;
          case 2:     // all cycles, play once
            if (currentCycle == cyclesCount - 1) {
              currentCycle = 0;
              currentFrame = 0;
              previewDisplay();
              previewStop();
              return;
            } else {
              currentCycle++;
              currentFrame = 0;
              framesCount = ((BamCycle)modelCycles.get(currentCycle)).frames.size();
              break;
            }
          case 3:     // all cycles, play looped
            if (currentCycle == cyclesCount - 1) {
              currentCycle = 0;
            } else {
              currentCycle++;
            }
            currentFrame = 0;
            framesCount = ((BamCycle)modelCycles.get(currentCycle)).frames.size();
            break;
        }
      } else {
        currentFrame++;
      }
    } while (retries-- > 0 && currentFrame >= framesCount);

    // update displayed frame
    previewDisplay();
    updatePreview();
  }


  private List<String> convert()
  {
    List<String> result = new Vector<String>(2);

    boolean isV2 = (cbVersion.getSelectedIndex() == 1);

    // handling "auto" compression
    DxtEncoder.DxtType dxtType = getDxtType();
    if (isV2 && dxtType == null) {
      dxtType = DxtEncoder.DxtType.DXT1;
      for (int i = 0; i < modelFrames.size(); i++) {
        BufferedImage image = ((BamFrame)modelFrames.get(i)).image;
        int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        for (int j = 0; j < pixels.length; j++) {
          int alpha = pixels[j] >>> 24;
          if (alpha > 0x20 && alpha < 0xe0) {
            dxtType = DxtEncoder.DxtType.DXT5;
            break;
          }
        }
        pixels = null;
        image.flush();
        if (dxtType == DxtEncoder.DxtType.DXT5)
          break;
      }
    }

    // converting BAM
    if (!isV2) {
      convertV1(result);
    } else {
      convertV2(dxtType, result);
    }

    return result;
  }

  // export as BAM V1 (compressed or uncompressed)
  private boolean convertV1(List<String> result)
  {
    // checking input data
    if (result == null)
      return false;
    if (modelFrames.size() > 65535) {
      result.add(null);
      result.add("No more than 65535 frames supported.");
      return false;
    }
    if (modelCycles.size() > 255) {
      result.add(null);
      result.add("No more than 255 cycles supported.");
      return false;
    }
    for (int i = 0; i < modelCycles.size(); i++) {
      int len = ((BamCycle)modelCycles.get(i)).frames.size();
      if (len > 65535) {
        result.add(null);
        result.add(String.format("No more than 65535 entries per cycle supported. " +
                                 "(Cycle #%1$d contains %2$d entries.", i, len));
        return false;
      }
    }

    ProgressMonitor progress = new ProgressMonitor(this, "Converting BAM...", null, 0, 4);
    progress.setMillisToDecideToPopup(0);
    progress.setMillisToPopup(0);
    progress.setProgress(0);

    // preparations
    String outFile = tfBamOutput.getText();
    boolean isCompressed = cbCompressBam.isSelected();
    List<FrameEntry> framesList = new ArrayList<FrameEntry>(modelFrames.size());
    List<CycleEntry> cyclesList = new ArrayList<CycleEntry>(modelCycles.size());
    List<Short> frameLookup = new ArrayList<Short>();
    List<byte[]> frameDataList = new ArrayList<byte[]>(modelFrames.size());
    int maxImageSize = 0;   // useful to reduce memory allocations

    // calculating global palette for all frames
    final int transThreshold = 0x20;    // transparency threshold
    int pixelCount = 0;   // number of pixels to consider during color reduction
    int[] pixels = null;
    for (int i = 0; i < modelFrames.size(); i++) {
      BufferedImage image = ((BamFrame)modelFrames.get(i)).image;
      if (image.getWidth()*image.getHeight() > maxImageSize) {
        maxImageSize = image.getWidth()*image.getHeight();
      }
      pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      for (int j = 0; j < pixels.length; j++) {
        if ((pixels[j] >>> 24) >= transThreshold)
          pixelCount++;
      }
    }
    // add pixels of each frame to a temp. image
    BufferedImage palImage = ColorConvert.createCompatibleImage(pixelCount, 1, false);
    pixels = ((DataBufferInt)palImage.getRaster().getDataBuffer()).getData();
    pixelCount = 0;
    for (int i = 0; i < modelFrames.size(); i++) {
      BufferedImage image = ((BamFrame)modelFrames.get(i)).image;
      int[] imgData = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
      for (int j = 0; j < imgData.length; j++) {
        if ((imgData[j] >>> 24) >= transThreshold) {
          pixels[pixelCount++] = imgData[j] | 0xff000000;
        }
      }
    }
    progress.setProgress(1);

    // try to load predefined palette first
    int[] palette = null;
    int[] presetPalette = loadPaletteFromFile(tfBamPalette.getText());
    if (presetPalette != null) {
      palette = new int[255];
      Arrays.fill(palette, 0);
      System.arraycopy(presetPalette, 1, palette, 0, presetPalette.length - 1);
    } else {
      // creating global color table
      palette = ColorConvert.medianCut(pixels, 255, false);
    }
    int[] hslPalette = new int[palette.length];
    ColorConvert.toHslPalette(palette, hslPalette);
    // initializing color cache
    IntegerHashMap<Byte> colorCache = new IntegerHashMap<Byte>(2048);
    for (int i = 0; i < palette.length; i++) {
      palette[i] &= 0x00ffffff;
      colorCache.put(palette[i], (byte)i);
    }
    // adding transparent color index to the palette if available
    int[] tmp = palette;
    palette = new int[tmp.length + 1];
    if (presetPalette != null) {
      palette[0] = presetPalette[0];
    } else {
      palette[0] = 0x0000ff00;    // it's usually defined as RGB(0, 255, 0)
    }
    System.arraycopy(tmp, 0, palette, 1, tmp.length);
    tmp = null;
    progress.setProgress(2);

    // encoding frames
    byte[] dstData = new byte[maxImageSize*2];    // working buffer for encoded frame data
    for (int idx = 0; idx < modelFrames.size(); idx++) {
      BamFrame bf = (BamFrame)modelFrames.get(idx);
      FrameEntry fe = new FrameEntry((short)bf.image.getWidth(), (short)bf.image.getHeight(),
                                     (short)bf.centerX, (short)bf.centerY, bf.isCompressed);
      // frame data offset will be added later
      framesList.add(fe);

      pixels = ((DataBufferInt)bf.image.getRaster().getDataBuffer()).getData();
      if (bf.isCompressed) {
        // do RLE compression on color index 0
        int srcIdx = 0, dstIdx = 0, srcMax = bf.image.getWidth()*bf.image.getHeight();
        while (srcIdx < srcMax) {
          if ((pixels[srcIdx] >>> 24) < transThreshold) {
            // color index to compress
            int cnt = 0;
            srcIdx++;
            while (srcIdx < srcMax && cnt < 255 && (pixels[srcIdx] >>> 24) < transThreshold) {
              cnt++;
              srcIdx++;
            }
            dstData[dstIdx++] = 0;
            dstData[dstIdx++] = (byte)cnt;
          } else {
            // uncompressed color indices
            Byte colIdx = colorCache.get(pixels[srcIdx] & 0x00ffffff);
            if (colIdx != null) {
              dstData[dstIdx++] = (byte)(colIdx + 1);
            } else {
              int color = ColorConvert.nearestColor(pixels[srcIdx], hslPalette);
              dstData[dstIdx++] = (byte)(color + 1);
              colorCache.put(pixels[srcIdx] & 0x00ffffff, (byte)color);
            }
            srcIdx++;
          }
        }
        // storing resulting frame data
        byte[] outData = new byte[dstIdx];
        System.arraycopy(dstData, 0, outData, 0, outData.length);
        frameDataList.add(outData);
      } else {
        // storing uncompressed data
        int curIdx = 0, max = bf.image.getWidth()*bf.image.getHeight();
        while (curIdx < max) {
          if ((pixels[curIdx] >>> 24) < transThreshold) {
            dstData[curIdx] = 0;
          } else {
            Byte colIdx = colorCache.get(pixels[curIdx] & 0x00ffffff);
            if (colIdx != null) {
              dstData[curIdx] = (byte)(colIdx + 1);
            } else {
              int color = ColorConvert.nearestColor(pixels[curIdx], hslPalette);
              dstData[curIdx] = (byte)(color + 1);
              colorCache.put(pixels[curIdx] & 0x00ffffff, (byte)color);
            }
          }
          curIdx++;
        }
        // storing resulting frame data
        byte[] outData = new byte[curIdx];
        System.arraycopy(dstData, 0, outData, 0, outData.length);
        frameDataList.add(outData);
      }
      pixels = null;
      bf.image.flush();
    }
    progress.setProgress(3);

    // creating cycles table and frame lookup table
    int fltIdx = 0;   // frame lookup table index
    for (int i = 0; i < modelCycles.size(); i++) {
      BamCycle bc = (BamCycle)modelCycles.get(i);
      CycleEntry ce = new CycleEntry((short)bc.frames.size(), (short)fltIdx);
      cyclesList.add(ce);

      for (int j = 0; j < bc.frames.size(); j++) {
        frameLookup.add(new Short(bc.frames.get(j).shortValue()));
        fltIdx++;
      }
    }

    // putting all together
    int ofsFrameEntries = 0x18;
    int ofsPalette = ofsFrameEntries +
                     framesList.size()*FrameEntry.entrySize +
                     cyclesList.size()*CycleEntry.entrySize;
    int ofsLookup = ofsPalette + 1024;
    int ofsFrameData = ofsLookup + frameLookup.size()*2;
    int bamSize = ofsFrameData;
    int curOfs = ofsFrameData;
    // updating frame data offsets
    for (int i = 0; i < framesList.size(); i++) {
      FrameEntry fe = framesList.get(i);
      fe.offset = curOfs;
      curOfs += frameDataList.get(i).length;
      bamSize += frameDataList.get(i).length;
    }
    byte[] bamArray = new byte[bamSize];
    // adding global header
    System.arraycopy("BAM V1  ".getBytes(Charset.forName("US-ASCII")), 0, bamArray, 0, 8);
    DynamicArray.putShort(bamArray, 0x08, (short)framesList.size());
    DynamicArray.putByte(bamArray, 0x0a, (byte)cyclesList.size());
    DynamicArray.putByte(bamArray, 0x0b, (byte)0);
    DynamicArray.putInt(bamArray, 0x0c, ofsFrameEntries);
    DynamicArray.putInt(bamArray, 0x10, ofsPalette);
    DynamicArray.putInt(bamArray, 0x14, ofsLookup);
    curOfs = ofsFrameEntries;
    // adding frame entries
    for (int i = 0; i < framesList.size(); i++) {
      FrameEntry fe = framesList.get(i);
      DynamicArray.putShort(bamArray, curOfs, (short)fe.width);
      DynamicArray.putShort(bamArray, curOfs+2, (short)fe.height);
      DynamicArray.putShort(bamArray, curOfs+4, (short)fe.cx);
      DynamicArray.putShort(bamArray, curOfs+6, (short)fe.cy);
      DynamicArray.putInt(bamArray, curOfs+8, fe.getOffset());
      curOfs += FrameEntry.entrySize;
    }
    // adding cycle entries
    for (int i = 0; i < cyclesList.size(); i++) {
      CycleEntry ce = cyclesList.get(i);
      DynamicArray.putShort(bamArray, curOfs, (short)ce.count);
      DynamicArray.putShort(bamArray, curOfs+2, (short)ce.index);
      curOfs += CycleEntry.entrySize;
    }
    // adding palette
    curOfs = ofsPalette;
    for (int i = 0; i < palette.length; i++) {
      DynamicArray.putByte(bamArray, curOfs, (byte)(palette[i] & 0xff));
      DynamicArray.putByte(bamArray, curOfs+1, (byte)((palette[i] >>> 8) & 0xff));
      DynamicArray.putByte(bamArray, curOfs+2, (byte)((palette[i] >>> 16) & 0xff));
      DynamicArray.putByte(bamArray, curOfs+3, (byte)0);
      curOfs += 4;
    }
    // adding frame lookup table
    curOfs = ofsLookup;
    for (int i = 0; i < frameLookup.size(); i++) {
      DynamicArray.putShort(bamArray, curOfs, frameLookup.get(i).shortValue());
      curOfs += 2;
    }
    // adding frame data
    curOfs = ofsFrameData;
    for(int i = 0; i < frameDataList.size(); i++) {
      byte[] data = frameDataList.get(i);
      System.arraycopy(data, 0, bamArray, curOfs, data.length);
      curOfs += data.length;
    }

    // compressing (optional)
    if (isCompressed) {
      if (bamArray != null) {
        bamArray = Compressor.compress(bamArray, "BAMC", "V1  ");
      }
    }

    // writing to disk
    BufferedOutputStream bos = null;
    try {
      try {
        bos = new BufferedOutputStream(new FileOutputStream(new File(outFile)));
        bos.write(bamArray);
        bos.close();
      } catch (Exception e) {
        result.add(null);
        result.add(String.format("Error writing BAM file \"%1$s\" to disk.", outFile.toString()));
        if (bos != null) {
          bos.close();
          bos = null;
        }
        progress.close();
        return false;
      }
    } catch (Exception e) {
      bos = null;
      progress.close();
      return false;
    }
    progress.setProgress(progress.getMaximum());

    result.add("Conversion finished successfully.");
    return true;
  }

  // export as BAM V2
  private boolean convertV2(DxtEncoder.DxtType dxtType, List<String> result)
  {
    // checking input data
    if (result == null)
      return false;
    if (dxtType != DxtEncoder.DxtType.DXT1 && dxtType != DxtEncoder.DxtType.DXT5) {
      dxtType = DxtEncoder.DxtType.DXT5;
    }

    // preparations
    String outFile = tfBamOutput.getText();
    List<List<FrameDataV2>> framesList = new ArrayList<List<FrameDataV2>>(modelFrames.size());
    List<GridManager> gridList = new ArrayList<GridManager>();

    ProgressMonitor progress = new ProgressMonitor(this, "Converting BAM...",
                                                   "Calculating PVRZ layout", 0, 5);
    progress.setMillisToDecideToPopup(0);
    progress.setMillisToPopup(0);
    progress.setProgress(0);
    try {
      // generating block data list
      if (!buildFrameDataList(framesList, gridList, result)) {
        return false;
      }

      // generating remaining info blocks
      List<FrameDataV2> frameDataBlockList = new ArrayList<FrameDataV2>();
      List<FrameEntry> frameEntryList = new ArrayList<FrameEntry>();
      List<CycleEntry> cyclesList = new ArrayList<CycleEntry>(modelCycles.size());
      short frameStartIndex = 0;    // keeps track of current start index of frame entries
      short blockStartIndex = 0;    // keeps track of current start index of frame data blocks
      for (int i = 0; i < modelCycles.size(); i++) {
        List<Integer> cycleFrames = ((BamCycle)modelCycles.get(i)).frames;

        // generating cycle entries
        CycleEntry ce = new CycleEntry((short)cycleFrames.size(), frameStartIndex);
        cyclesList.add(ce);

        for (int j = 0; j < cycleFrames.size(); j++) {
          // preparing data
          int idx = cycleFrames.get(j).intValue();
          List<FrameDataV2> frame = framesList.get(idx);
          BamFrame bf = (BamFrame)modelFrames.get(idx);

          // generating frame entries
          FrameEntry fe = new FrameEntry((short)bf.image.getWidth(), (short)bf.image.getHeight(),
                                         (short)bf.centerX, (short)bf.centerY,
                                         blockStartIndex, (short)frame.size());
          frameEntryList.add(fe);

          // generating frame data block entries
          for (int k = 0; k < frame.size(); k++) {
            frameDataBlockList.add(frame.get(k));
          }

          blockStartIndex += frame.size();
        }

        frameStartIndex += cycleFrames.size();
      }

      // putting all together
      int ofsFrameEntries = 32;
      int ofsCycleEntries = ofsFrameEntries + frameEntryList.size()*FrameEntry.entrySize;
      int ofsFrameData = ofsCycleEntries + cyclesList.size()*CycleEntry.entrySize;
      int bamSize = ofsFrameData + frameDataBlockList.size()*FrameDataV2.entrySize;
      byte[] bamArray = new byte[bamSize];
      // writing main header
      System.arraycopy("BAM V2  ".getBytes(Charset.forName("US-ASCII")), 0, bamArray, 0, 8);
      DynamicArray.putInt(bamArray, 8, framesList.size());
      DynamicArray.putInt(bamArray, 12, cyclesList.size());
      DynamicArray.putInt(bamArray, 16, frameDataBlockList.size());
      DynamicArray.putInt(bamArray, 20, ofsFrameEntries);
      DynamicArray.putInt(bamArray, 24, ofsCycleEntries);
      DynamicArray.putInt(bamArray, 28, ofsFrameData);
      // writing frame entries
      int ofs = ofsFrameEntries;
      for (int i = 0; i < frameEntryList.size(); i++) {
        FrameEntry fe = frameEntryList.get(i);
        DynamicArray.putShort(bamArray, ofs, fe.width);
        DynamicArray.putShort(bamArray, ofs+2, fe.height);
        DynamicArray.putShort(bamArray, ofs+4, fe.cx);
        DynamicArray.putShort(bamArray, ofs+6, fe.cy);
        DynamicArray.putShort(bamArray, ofs+8, fe.index);
        DynamicArray.putShort(bamArray, ofs+10, fe.count);
        ofs += FrameEntry.entrySize;
      }
      // writing cycle entries
      ofs = ofsCycleEntries;
      for (int i = 0; i < cyclesList.size(); i++) {
        CycleEntry ce = cyclesList.get(i);
        DynamicArray.putShort(bamArray, ofs, ce.count);
        DynamicArray.putShort(bamArray, ofs+2, ce.index);
        ofs += CycleEntry.entrySize;
      }
      // writing frame data blocks
      ofs = ofsFrameData;
      for (int i = 0; i < frameDataBlockList.size(); i++) {
        FrameDataV2 fd = frameDataBlockList.get(i);
        DynamicArray.putInt(bamArray, ofs, fd.page);
        DynamicArray.putInt(bamArray, ofs+4, fd.sx);
        DynamicArray.putInt(bamArray, ofs+8, fd.sy);
        DynamicArray.putInt(bamArray, ofs+12, fd.width);
        DynamicArray.putInt(bamArray, ofs+16, fd.height);
        DynamicArray.putInt(bamArray, ofs+20, fd.dx);
        DynamicArray.putInt(bamArray, ofs+24, fd.dy);
        ofs += FrameDataV2.entrySize;
      }

      // writing BAM file to disk
      BufferedOutputStream bos = null;
      try {
        try {
          bos = new BufferedOutputStream(new FileOutputStream(new File(outFile)));
          bos.write(bamArray);
          bos.close();
          bos = null;
        } catch (IOException e) {
          // error handling
          if (bos != null) {
            bos.close();
            bos = null;
          }
          e.printStackTrace();
          result.add(null);
          result.add(String.format("Error writing BAM file \"%1$s\" to disk.", outFile.toString()));
          return false;
        }
      } catch (Exception e) {
        // non-critical error
        e.printStackTrace();
      }
      bamArray = null;

      // generating PVRZ files
      if (!createPvrzPages(new File(outFile).getParent(), dxtType, gridList,
                           framesList, result, progress)) {
        return false;
      }
    } finally {
      if (progress != null) {
        progress.close();
        progress = null;
      }
    }

    result.add("Conversion finished successfully.");
    return true;
  }

  // calculates the locations of all available frames on the PVRZ textures
  private boolean buildFrameDataList(List<List<FrameDataV2>> framesList, List<GridManager> gridList,
                                     List<String> result)
  {
    if (framesList != null && gridList != null) {
      final int pageDim = 1024;
      int pvrzIndex = getPvrzStartIndex();

      for (int frameIdx = 0; frameIdx < modelFrames.size(); frameIdx++) {
        List<FrameDataV2> frameDataList = new ArrayList<FrameDataV2>();
        framesList.add(frameDataList);

        BufferedImage image = ((BamFrame)modelFrames.get(frameIdx)).image;
        int width = image.getWidth();
        int height = image.getHeight();
        int x = 0, y = 0, pOfs = 0;

        while (pOfs < width*height) {
          int w = Math.min(pageDim, width - x);
          int h = Math.min(pageDim, height - y);
          if (w == pageDim && h == pageDim) {
            // Unlikely case: frame is bigger than max. texture size
            GridManager gm = new GridManager(pageDim >>> 2, pageDim >>> 2);
            gm.add(new Rectangle(0, 0, pageDim >>> 2, pageDim >>> 2));
            gridList.add(gm);
            // register page entry
            int pageIdx = gridList.size() - 1;
            FrameDataV2 entry = new FrameDataV2(pvrzIndex + pageIdx, 0, 0, w, h, x, y);
            frameDataList.add(entry);
          } else {
            // The common case: frame is smaller than max. texture size
            // finding first available page containing sufficient space for the current region
            // (forcing 4 pixels alignment for better DXT compression)
            Dimension space = new Dimension((w + 3) >>> 2, (h + 3) >>> 2);
            int pageIdx = -1;
            Rectangle rectMatch = new Rectangle(0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
            for (int i = 0; i < gridList.size(); i++) {
              GridManager gm = gridList.get(i);
              Rectangle rect = gm.findNext(space, GridManager.Alignment.TopLeftHorizontal);
              if (rect != null) {
                pageIdx = i;
                rectMatch = (Rectangle)rect.clone();
                break;
              }
              if (pageIdx >= 0) {
                break;
              }
            }

            // create new page if no match found
            if (pageIdx == -1) {
              GridManager gm = new GridManager(pageDim >>> 2, pageDim >>> 2);
              gridList.add(gm);
              pageIdx = gridList.size() - 1;
              rectMatch.x = rectMatch.y = 0;
              rectMatch.width = gm.getWidth(); rectMatch.height = gm.getHeight();
            }

            // add region to the page
            GridManager gm = gridList.get(pageIdx);
            gm.add(new Rectangle(rectMatch.x, rectMatch.y, space.width, space.height));
            // register page entry
            FrameDataV2 entry = new FrameDataV2(pvrzIndex + pageIdx, rectMatch.x << 2, rectMatch.y << 2,
                                                w, h, x, y);
            frameDataList.add(entry);
          }

          // advance scanning
          if (x + pageDim >= width) {
            x = 0;
            y += pageDim;
          } else {
            x += pageDim;
          }
          pOfs = y*width + x;
        }
      }

      // check PVRZ index again
      if (pvrzIndex + gridList.size() > 100000) {
        result.add(null);
        result.add(String.format("One or more PVRZ indices exceed the max. possible value of 99999.\n" +
                                 "Please choose a start index smaller than or equal to %1$d.",
                                 100000 - gridList.size()));
        return false;
      }

      return true;
    }
    return false;
  }

  // generates PVRZ textures
  private boolean createPvrzPages(String path, DxtEncoder.DxtType dxtType, List<GridManager> gridList,
                                  List<List<FrameDataV2>> framesList, List<String> result,
                                  ProgressMonitor progress)
  {
    // preparing variables
    if (path == null)
      path = "";
    if (!path.isEmpty()) {
      if (path.charAt(path.length() - 1) != File.separatorChar)
        path = path + File.separator;
    }
    int dxtCode = (dxtType == DxtEncoder.DxtType.DXT5) ? 11 : 7;
    byte[] output = new byte[DxtEncoder.calcImageSize(1024, 1024, dxtType)];
    int pageMin = Integer.MAX_VALUE;
    int pageMax = -1;
    for (final List<FrameDataV2> l: framesList) {
      for (final FrameDataV2 fd: l) {
        pageMin = Math.min(pageMin, fd.page);
        pageMax = Math.max(pageMax, fd.page);
      }
    }

    String note = "Generating PVRZ file %1$s / %2$s";
    int curProgress = 1;
    if (progress != null) {
      progress.setMinimum(0);
      progress.setMaximum(pageMax - pageMin + 2);
      progress.setProgress(curProgress);
    }

    // processing each PVRZ page
    for (int i = pageMin; i <= pageMax; i++) {
      if (progress != null) {
        if (progress.isCanceled()) {
          result.add(null);
          result.add("Conversion has been cancelled.");
          return false;
        }
        progress.setProgress(curProgress);
        progress.setNote(String.format(note, curProgress, pageMax - pageMin + 1));
        curProgress++;
      }
      String pvrzName = path + String.format("MOS%1$04d.PVRZ", i);
      GridManager gm = gridList.get(i - pageMin);
      gm.shrink();

      // generating texture image
      int tw = ConvertToPvrz.nextPowerOfTwo(gm.getWidth() << 2);
      int th = ConvertToPvrz.nextPowerOfTwo(gm.getHeight() << 2);
      BufferedImage texture = ColorConvert.createCompatibleImage(tw, th, true);
      Graphics2D g = (Graphics2D)texture.getGraphics();
      try {
        g.setBackground(new Color(0, true));
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, texture.getWidth(), texture.getHeight());
        for (int frameIdx = 0; frameIdx < modelFrames.size(); frameIdx++) {
          BufferedImage img = ((BamFrame)modelFrames.get(frameIdx)).image;
          List<FrameDataV2> frame = framesList.get(frameIdx);
          for (int entryIdx = 0; entryIdx < frame.size(); entryIdx++) {
            FrameDataV2 entry = frame.get(entryIdx);
            if (entry.page == i) {
              int sx = entry.dx, sy = entry.dy;
              int dx = entry.sx, dy = entry.sy;
              int w = entry.width, h = entry.height;
              g.clearRect(dx, dy, w, h);
              g.drawImage(img, dx, dy, dx+w, dy+h, sx, sy, sx+w, sy+h, null);
            }
          }
        }
      } finally {
        g.dispose();
      }

      // compressing PVRZ
      int[] textureData = ((DataBufferInt)texture.getRaster().getDataBuffer()).getData();
      try {
        int outSize = DxtEncoder.calcImageSize(texture.getWidth(), texture.getHeight(), dxtType);
        DxtEncoder.encodeImage(textureData, texture.getWidth(), texture.getHeight(), output, dxtType);
        byte[] header = ConvertToPvrz.createPVRHeader(texture.getWidth(), texture.getHeight(), dxtCode);
        byte[] pvrz = new byte[header.length + outSize];
        System.arraycopy(header, 0, pvrz, 0, header.length);
        System.arraycopy(output, 0, pvrz, header.length, outSize);
        header = null;
        pvrz = Compressor.compress(pvrz, 0, pvrz.length, true);

        // writing PVRZ to disk
        BufferedOutputStream bos = null;
        try {
          try {
            bos = new BufferedOutputStream(new FileOutputStream(new File(pvrzName)));
            bos.write(pvrz);
            bos.close();
            bos = null;
          } catch (IOException e) {
            // critical error
            if (bos != null) {
              bos.close();
              bos = null;
            }
            e.printStackTrace();
            result.add(null);
            result.add(String.format("Error writing PVRZ file \"%1$s\" to disk.", pvrzName));
            return false;
          }
        } catch (Exception e) {
          // non-critical error
          e.printStackTrace();
        }
        textureData = null;
        texture.flush();
        pvrz = null;
      } catch (Exception e) {
        e.printStackTrace();
        result.add(null);
        result.add(String.format("Error while generating PVRZ files:\n%1$s", e.getMessage()));
        return false;
      }
    }
    output = null;
    return true;
  }

//-------------------------- INNER CLASSES --------------------------

  // Adds a prefix to the cell's visual output
  private static class IndexedCellRenderer extends DefaultListCellRenderer
  {
    public IndexedCellRenderer()
    {
      super();
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
      String template = "%1$0" +
                        String.format("%1$d", Integer.toString(list.getModel().getSize()).length()) +
                        "d - %2$s";
      return super.getListCellRendererComponent(list, String.format(template, index, value),
                                                index, isSelected, cellHasFocus);
    }
  }


  // Stores relevant data of a single BAM frame
  private class BamFrame
  {
    public String fileName;       // name of the graphics file or BAM file
    public BufferedImage image;   // image object of the graphics file or a specific BAM frame
    public int frameIndex;        // frame index if source is a BAM file
    public int centerX, centerY;  // frame center coordinates
    public boolean isBamSource;   // indicates whether the frame belongs to a source BAM
    public boolean isCompressed;  // indicates whether frame should be RLE compressed

    // Use this for graphics files
    public BamFrame(String imageFile, BufferedImage image, int centerX, int centerY, boolean isCompressed)
    {
      this.fileName = imageFile;
      this.image = (image != null) ? image : ColorConvert.createCompatibleImage(1, 1, true);
      this.centerX = centerX;
      this.centerY = centerY;
      this.isCompressed = isCompressed;
      this.frameIndex = -1;
      this.isBamSource = false;
    }

    // Use this for specific frames of an imported BAM file
    public BamFrame(String bamFile, int frameIndex, BufferedImage image,
                    int centerX, int centerY, boolean isCompressed)
    {
      this.fileName = bamFile;
      this.isBamSource = true;
      this.frameIndex = frameIndex;
      this.image = (image != null) ? image : ColorConvert.createCompatibleImage(1, 1, true);
      this.centerX = centerX;
      this.centerY = centerY;
      this.isCompressed = isCompressed;
    }

    @Override
    public String toString()
    {
      if (isBamSource) {
        return String.format("%1$s:%2$d", fileName, frameIndex);
      } else {
        return fileName;
      }
    }
  }

  // Stores relevant data of a single BAM cycle
  private class BamCycle
  {
    public final List<Integer> frames;    // references frame indices

    public BamCycle()
    {
      frames = new ArrayList<Integer>();
    }

    public BamCycle(Collection<Integer> frames)
    {
      this();
      if (frames != null && !frames.isEmpty()) {
        for (final Integer i: frames) {
          this.frames.add((i != null) ? i : new Integer(0));
        }
      }
    }

    @Override
    public String toString()
    {
      final int MAX_LIST = 30;    // max. number of indices to display

      StringBuilder sb = new StringBuilder();
      sb.append("[");
      if (frames.isEmpty()) {
        sb.append(" ");
      } else {
        for (int i = 0; i < frames.size(); i++) {
          if (frames.size() > MAX_LIST) {
            if ((i < MAX_LIST*2/3) || (i > frames.size() - MAX_LIST/3)) {
              sb.append(frames.get(i).toString());
              if (i < frames.size() - 1) {
                sb.append(", ");
              }
            } else if (i == MAX_LIST*2/3) {
              sb.append("..., ");
            }
          } else {
            sb.append(frames.get(i).toString());
            if (i < frames.size() - 1) {
              sb.append(", ");
            }
          }
        }
      }
      sb.append("]");
      return sb.toString();
    }

    @Override
    public Object clone()
    {
      return new BamCycle(frames);
    }
  }

  private static class FrameEntry {
    static final int entrySize = 12;

    public short width, height;
    public short cx, cy;
    public short index, count;    // for V2 only
    public int offset;            // for V1 only
    public boolean isCompressed;  // for V1 only

    // constructor for BAM V1 entries
    public FrameEntry(short width, short height, short cx, short cy, boolean isCompressed)
    {
      this.width = width; this.height = height;
      this.cx = cx; this.cy = cy;
      this.isCompressed = isCompressed;
      this.offset = 0;
    }

    // constructor for BAM V2 entries
    public FrameEntry(short width, short height, short cx, short cy, short index, short count)
    {
      this.width = width; this.height = height;
      this.cx = cx; this.cy = cy;
      this.index = index;
      this.count = count;
    }

    public int getOffset()
    {
      return offset | (!isCompressed ? 0x80000000 : 0);
    }
  }

  // storage class for frame data blocks of a BAM V2 structure
  private static class FrameDataV2
  {
    public static final int entrySize = 28;

    public int page;
    public int sx, sy;
    public int width, height;
    public int dx, dy;

    public FrameDataV2(int page, int sx, int sy, int width, int height, int dx, int dy)
    {
      this.page = page;
      this.sx = sx; this.sy = sy;
      this.width = width; this.height = height;
      this.dx = dx; this.dy = dy;
    }

    @Override
    public boolean equals(Object o)
    {
      if (o instanceof FrameDataV2) {
        FrameDataV2 fd = (FrameDataV2)o;
        return (fd.page == page && fd.sx == sx && fd.sy == sy &&
                fd.width == width && fd.height == height && fd.dx == dx && fd.dy == dy);
      }
      return false;
    }
  }

  private static class CycleEntry
  {
    public static final int entrySize = 4;

    public short count, index;

    public CycleEntry(short count, short index)
    {
      this.count = count;
      this.index = index;
    }
  }

}
