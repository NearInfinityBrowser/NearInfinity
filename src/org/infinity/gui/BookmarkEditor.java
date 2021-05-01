// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar.Bookmark;
import org.infinity.resource.Profile;
import org.infinity.util.Platform;
import org.infinity.util.SimpleListModel;
import org.infinity.util.io.FileManager;

/**
 * Edit or remove bookmarked games.
 */
public class BookmarkEditor extends JDialog implements ActionListener, FocusListener, ListSelectionListener, ItemListener
{
  private final SimpleListModel<Bookmark> modelEntries = new SimpleListModel<>();
  private final JList<Bookmark> listEntries = new JList<>(modelEntries);
  private final JButton bUp = new JButton("Up");
  private final JButton bDown = new JButton("Down");
  private final JButton bRemove = new JButton("Remove");
  private final JButton bClear = new JButton("Clear");
  private final JButton bOK = new JButton("OK");
  private final JButton bCancel = new JButton("Cancel");
  private final JButton bBinPathAdd = new JButton("+");
  private final JButton bBinPathRemove = new JButton("-");
  private final JTextField tfName = new JTextField();
  private final JTextField tfPath = createReadOnlyField(null, true);
  private final DefaultComboBoxModel<Platform.OS> cbPlatformModel =
      new DefaultComboBoxModel<>(BrowserMenuBar.Bookmark.getSupportedOS());
  private final JComboBox<Platform.OS> cbPlatform = new JComboBox<>(cbPlatformModel);
  private final EnumMap<Platform.OS, DefaultListModel<Path>> listBinPathModels = new EnumMap<>(Platform.OS.class);
  private final JList<Path> listBinPaths = new JList<>();

  private final List<BrowserMenuBar.Bookmark> listBookmarks = new ArrayList<>();

  private boolean accepted;

  public static List<BrowserMenuBar.Bookmark> editBookmarks(List<BrowserMenuBar.Bookmark> bookmarks)
  {
    BookmarkEditor dlg = new BookmarkEditor(NearInfinity.getInstance(), bookmarks);
    List<BrowserMenuBar.Bookmark> retVal = dlg.getBookmarkList();
    dlg.dispose();
    dlg = null;
    return retVal;
  }

  private BookmarkEditor(Window owner, List<BrowserMenuBar.Bookmark> bookmarks)
  {
    super(owner, "Bookmark Editor", Dialog.ModalityType.APPLICATION_MODAL);
    init(bookmarks);
  }

  private void init(List<BrowserMenuBar.Bookmark> bookmarks)
  {
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), this);
    getRootPane().getActionMap().put(this, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e)
      {
        cancel();
      }
    });

    GridBagConstraints gbc = new GridBagConstraints();

    listEntries.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listEntries.addListSelectionListener(this);

    // creating bookmark details panel
    JPanel pDetails = new JPanel(new GridBagLayout());
    JLabel lName = new JLabel("Name:");
    JLabel lPath = new JLabel("Path:");
    JLabel lBinPath = new JLabel("Game executable:");
    tfName.addFocusListener(this);
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pDetails.add(lName, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pDetails.add(tfName, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pDetails.add(lPath, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pDetails.add(tfPath, gbc);

    // adding binary path support
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.NONE, new Insets(8, 0, 0, 0), 0, 0);
    pDetails.add(lBinPath, gbc);

    // Subpanel: platform-specific path selection
    JPanel pGames = new JPanel(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 2, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pGames.add(cbPlatform, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 2, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.BOTH, new Insets(0, 8, 0, 0), 0, 0);


    listBinPaths.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    listBinPaths.setVisibleRowCount(2);
    listBinPaths.setToolTipText("Leave empty to use game-specific defaults.");
    JScrollPane spBinList = new JScrollPane(listBinPaths);
    pGames.add(spBinList, gbc);
    bBinPathAdd.addActionListener(this);
    gbc = ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pGames.add(bBinPathAdd, gbc);
    bBinPathRemove.addActionListener(this);
    gbc = ViewerUtil.setGBC(gbc, 2, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(4, 8, 0, 0), 0, 0);
    pGames.add(bBinPathRemove, gbc);

    gbc = ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 8, 0, 0), 0, 0);
    pDetails.add(pGames, gbc);

    // creating edit buttons panel
    JPanel pEdit = new JPanel(new GridBagLayout());
    bUp.setEnabled(false);
    bUp.addActionListener(this);
    bDown.setEnabled(false);
    bDown.addActionListener(this);
    bRemove.setEnabled(false);
    bRemove.addActionListener(this);
    bClear.setEnabled(false);
    bClear.addActionListener(this);
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pEdit.add(bUp, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pEdit.add(bDown, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(24, 0, 0, 0), 0, 0);
    pEdit.add(bRemove, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 3, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pEdit.add(bClear, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 4, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.VERTICAL, new Insets(0, 0, 0, 0), 0, 0);
    pEdit.add(new JPanel(), gbc);

    // creating dialog buttons panel
    JPanel pButtons = new JPanel(new GridBagLayout());
    bOK.addActionListener(this);
    bCancel.addActionListener(this);
    Dimension d = new Dimension();
    d.width = Math.max(bOK.getPreferredSize().width, bCancel.getPreferredSize().width);
    d.height = Math.max(bOK.getPreferredSize().height, bCancel.getPreferredSize().height);
    bOK.setPreferredSize(d);
    bCancel.setPreferredSize(d);
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(new JPanel(), gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(bOK, gbc);
    gbc = ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 8, 0, 0), 0, 0);
    pButtons.add(bCancel, gbc);
    gbc = ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    pButtons.add(new JPanel(), gbc);

    JPanel pMain = new JPanel(new GridBagLayout());
    JScrollPane spList = new JScrollPane(listEntries);
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(spList, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 1.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.VERTICAL, new Insets(0, 8, 0, 0), 0, 0);
    pMain.add(pEdit, gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(8, 0, 0, 0), 0, 0);
    pMain.add(pDetails, gbc);
    gbc = ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0);
    pMain.add(new JPanel(), gbc);
    gbc = ViewerUtil.setGBC(gbc, 0, 2, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(16, 0, 0, 0), 0, 0);
    pMain.add(pButtons, gbc);

    // putting all together
    setLayout(new GridBagLayout());
    gbc = ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.CENTER,
                            GridBagConstraints.BOTH, new Insets(8, 8, 8, 8), 0, 0);
    add(pMain, gbc);

    pack();
    setMinimumSize(getPreferredSize());
    d = new Dimension(getPreferredSize());
    d.width = (d.width * 4) / 3;
    d.height = (d.height * 4) / 3;
    setPreferredSize(d);
    pack();

    initData(bookmarks);

    setLocationRelativeTo(getOwner());
    setVisible(true);
  }

  private void initData(List<BrowserMenuBar.Bookmark> bookmarks)
  {
    // making deep copy of provided list
    if (bookmarks != null) {
      this.listBookmarks.clear();
      for (Iterator<BrowserMenuBar.Bookmark> iter = bookmarks.iterator(); iter.hasNext();) {
        try {
          this.listBookmarks.add((BrowserMenuBar.Bookmark)iter.next().clone());
        } catch (CloneNotSupportedException e) {
          // unused
        }
      }
    }

    int platformIdx = Math.max(cbPlatformModel.getIndexOf(Platform.getPlatform()), 0);
    cbPlatform.setSelectedIndex(platformIdx);
    for (int idx = 0; idx < cbPlatformModel.getSize(); idx++)
      listBinPathModels.put(cbPlatformModel.getElementAt(idx), new DefaultListModel<Path>());
    listBinPaths.setModel(getBinPathModel());
    listBinPaths.addListSelectionListener(this);
    cbPlatform.addItemListener(this);

    if (listBookmarks != null) {
      for (Iterator<BrowserMenuBar.Bookmark> iter = listBookmarks.iterator(); iter.hasNext();) {
        modelEntries.addElement(iter.next());
      }
      if (!modelEntries.isEmpty()) {
        listEntries.setSelectedIndex(0);
        bRemove.setEnabled(true);
        bClear.setEnabled(true);
        bUp.setEnabled(false);
        bDown.setEnabled(modelEntries.size() > 1);
      }
    }
  }

  // Returns the (updated) bookmark list
  private List<BrowserMenuBar.Bookmark> getBookmarkList()
  {
    return accepted() ? listBookmarks : null;
  }

  private boolean accepted()
  {
    return accepted;
  }

  // Called when pressed the OK button
  private void accept()
  {
    // updating bookmark list
    listBookmarks.clear();
    for (int i = 0, size = modelEntries.size(); i < size; i++) {
      listBookmarks.add(modelEntries.get(i));
    }
    accepted = true;
    setVisible(false);
  }

  // Called when pressing the Cancel button
  private void cancel()
  {
    accepted = false;
    setVisible(false);
  }

  // Creates a read-only text field, optionally with visible caret
  private static JTextField createReadOnlyField(String text, boolean showCaret)
  {
    JTextField tf = new JTextField();
    if (showCaret) {
      tf.addFocusListener(new FocusListener() {
        @Override
        public void focusLost(FocusEvent e)
        {
          JTextField tf = (JTextField)e.getSource();
          tf.getCaret().setVisible(false);
        }
        @Override
        public void focusGained(FocusEvent e)
        {
          JTextField tf = (JTextField)e.getSource();
          tf.getCaret().setVisible(true);
        }
      });
    }
    tf.setEditable(false);
    tf.setFont(UIManager.getFont("Label.font"));
    if (text != null) {
      tf.setText(text);
      tf.setCaretPosition(0);
    }
    return tf;
  }

  private void updateEntry(int index)
  {
    bUp.setEnabled(index > 0);
    bDown.setEnabled(index < modelEntries.size() - 1);
    bRemove.setEnabled(index >= 0);
    bClear.setEnabled(!modelEntries.isEmpty());

    if (index >= 0) {
      BrowserMenuBar.Bookmark bookmark = modelEntries.get(index);
      tfName.setText(bookmark.getName());
      tfName.setSelectionStart(0);
      tfName.setSelectionEnd(0);
      tfPath.setText(bookmark.getPath());
      tfPath.setSelectionStart(0);
      tfPath.setSelectionEnd(0);

      for (int idx = 0; idx < cbPlatformModel.getSize(); idx++) {
        Platform.OS os = cbPlatformModel.getElementAt(idx);
        getBinPathModel(os).clear();
        List<String> paths = bookmark.getBinaryPaths(os);
        for (int i = 0; i < paths.size(); i++) {
          Path path = FileManager.resolve(paths.get(i));
          if (path != null)
            getBinPathModel(os).addElement(path);
        }
      }
      if (listBinPaths.getModel().getSize() > 0)
        listBinPaths.setSelectedIndex(0);
    } else {
      tfName.setText("");
      tfPath.setText("");
      getBinPathModel().clear();
    }
  }

  // Updates all binary path lists for the selected bookmark
  private void updateBinPaths()
  {
    BrowserMenuBar.Bookmark bookmark = listEntries.getSelectedValue();
    for (int i = 0; i < cbPlatformModel.getSize(); i++) {
      Platform.OS os = cbPlatformModel.getElementAt(i);
      DefaultListModel<Path> model = getBinPathModel(os);
      List<String> pathList = new ArrayList<>();
      for (Enumeration<Path> iter = model.elements(); iter.hasMoreElements(); )
        pathList.add(iter.nextElement().toString());
      bookmark.setBinaryPaths(os, pathList);
    }
  }

  // Returns the currently selected ListModel for the binary path list
  private DefaultListModel<Path> getBinPathModel()
  {
    return getBinPathModel(cbPlatformModel.getElementAt(cbPlatform.getSelectedIndex()));
  }

  // Returns the specified ListModel for the binary path list
  private DefaultListModel<Path> getBinPathModel(Platform.OS os)
  {
    return listBinPathModels.get(os);
  }

  // Adds a new executable file entry to the current binary file list
  private void addBinPathInteractive()
  {
    JFileChooser fc = new JFileChooser(Profile.getGameRoot().toFile());
    fc.setDialogTitle("Select game executable");
    FileFilter exeFilter = null;
    fc.removeChoosableFileFilter(fc.getAcceptAllFileFilter());
    if (cbPlatform.getSelectedItem() == Platform.OS.Windows) {
      exeFilter = new FileNameExtensionFilter("Executable Files", "exe", "lnk", "cmd", "bat", "ps1", "pif");
    } else {
      exeFilter = new FileFilter() {
        @Override public String getDescription() { return "Executable Files"; }
        @Override public boolean accept(File f) { return !f.isFile() || Platform.IS_WINDOWS || f.canExecute(); } };
    }
    if (exeFilter != null)
      fc.addChoosableFileFilter(exeFilter);
    fc.addChoosableFileFilter(fc.getAcceptAllFileFilter());
    if (exeFilter != null)
      fc.setFileFilter(exeFilter);
    if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      Path path = fc.getSelectedFile().toPath();
      try {
        path = Profile.getGameRoot().relativize(path);
      } catch (IllegalArgumentException ex) {
      }
      DefaultListModel<Path> model = getBinPathModel();
      boolean exists = false;
      for (int i = 0; i < model.getSize(); i++) {
        if (path.equals(model.get(i))) {
          exists = true;
          listBinPaths.setSelectedIndex(i);
          listBinPaths.ensureIndexIsVisible(i);
          JOptionPane.showMessageDialog(this, "Selected game executable is already listed.",
                                        "Game executable", JOptionPane.WARNING_MESSAGE);
          break;
        }
      }
      if (!exists) {
        model.addElement(path);
        listBinPaths.setSelectedValue(path, true);
        updateBinPaths();
      }
    }
  }

  // Removes the selected executable file entry from the current binary file list
  private void removeBinPathInteractive()
  {
    Path path = listBinPaths.getSelectedValue();
    if (path != null) {
      if (JOptionPane.showConfirmDialog(this, "Remove game executable:\n" + path, "Game executable",
                                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
        if (getBinPathModel().removeElement(path)) {
          updateBinPaths();
        }
      }
    } else {
      JOptionPane.showMessageDialog(this, "No game executable selected.", "Game executable", JOptionPane.WARNING_MESSAGE);
    }
  }

  //--------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent event)
  {
    if (event.getSource() == bOK) {
      accept();
    } else if (event.getSource() == bCancel) {
      cancel();
    } else if (event.getSource() == bUp) {
      int oldIdx = listEntries.getSelectedIndex();
      if (oldIdx > 0) {
        int newIdx = oldIdx - 1;
        Bookmark obj = modelEntries.remove(oldIdx);
        modelEntries.add(newIdx, obj);
        listEntries.setSelectedIndex(newIdx);
        updateEntry(newIdx);
      }
    } else if (event.getSource() == bDown) {
      int oldIdx = listEntries.getSelectedIndex();
      if (oldIdx >= 0 && oldIdx < modelEntries.size() - 1) {
        int newIdx = oldIdx + 1;
        Bookmark obj = modelEntries.remove(oldIdx);
        modelEntries.add(newIdx, obj);
        listEntries.setSelectedIndex(newIdx);
        updateEntry(newIdx);
      }
    } else if (event.getSource() == bRemove) {
      int idx = listEntries.getSelectedIndex();
      if (idx >= 0) {
        modelEntries.remove(idx);
        idx = Math.min(idx, modelEntries.size() - 1);
        listEntries.setSelectedIndex(idx);
        updateEntry(idx);
      }
    } else if (event.getSource() == bClear) {
      if (!modelEntries.isEmpty()) {
        if (JOptionPane.showConfirmDialog(this, "Remove all bookmarks?", "Question",
                                          JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
          modelEntries.clear();
          updateEntry(-1);
        }
      }
    } else if (event.getSource() == bBinPathAdd) {
      addBinPathInteractive();
    } else if (event.getSource() == bBinPathRemove) {
      removeBinPathInteractive();
    }
  }

  //--------------------- End Interface ActionListener ---------------------

  //--------------------- Begin Interface FocusListener ---------------------

  @Override
  public void focusGained(FocusEvent event)
  {
  }

  @Override
  public void focusLost(FocusEvent event)
  {
    if (event.getSource() == tfName) {
      int idx = listEntries.getSelectedIndex();
      if (idx >= 0) {
        BrowserMenuBar.Bookmark bookmark = modelEntries.get(idx);
        if (!tfName.getText().trim().isEmpty()) {
          // update name in selected entry
          bookmark.setName(tfName.getText().trim());
          listEntries.repaint();
        } else {
          // restore name from selected entry
          tfName.setText(bookmark.getName());
        }
      } else {
        tfName.setText("");
      }
    }
  }

  //--------------------- End Interface FocusListener ---------------------

  //--------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (event.getSource() == listEntries) {
      updateEntry(listEntries.getSelectedIndex());
    } else if (event.getSource() == listBinPaths) {
      bBinPathRemove.setEnabled(listBinPaths.getSelectedIndex() >= 0);
    }
  }

  //--------------------- End Interface ListSelectionListener ---------------------

  //--------------------- Begin Interface ItemListener ---------------------

  @Override
  public void itemStateChanged(ItemEvent event)
  {
    if (event.getSource() == cbPlatform) {
      DefaultListModel<Path> model = listBinPathModels.get(cbPlatformModel.getElementAt(cbPlatform.getSelectedIndex()));
      if (model != null)
        listBinPaths.setModel(model);
      else
        throw new NullPointerException();
    }
  }

  //--------------------- Begin Interface ItemListener ---------------------
}
