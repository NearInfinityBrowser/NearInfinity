// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.datatype.ItemTypeBitmap;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.sto.StoResource;
import org.infinity.util.Logger;

/**
 * Opens an application-modal dialog where the user can define a custom sort order for item categories.
 * This sort order can be used to sort "Item for sale" entries in store resources.
 */
public class ItemCategoryOrderDialog extends JDialog implements ActionListener, ListSelectionListener {
  /** Preferences base name key for the item category sort order list */
  private static final String PREF_STO_CATEGORY_ORDER_FMT= "ItemCategoryOrder";

  private static final KeyStroke ESC_KEY = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

  private final JButton okButton = new JButton("Apply", Icons.ICON_CHECK_16.getIcon());
  private final JButton cancelButton = new JButton("Cancel", Icons.ICON_CHECK_NOT_16.getIcon());
  private final JButton resetButton = new JButton("Reset", Icons.ICON_REFRESH_16.getIcon());
  private final JButton reverseOrderButton = new JButton("Reverse");
  private final JButton moveUpButton = new JButton(Icons.ICON_UP_16.getIcon());
  private final JButton moveDownButton = new JButton(Icons.ICON_DOWN_16.getIcon());

  private final DefaultListModel<ItemCategory> listModel = new DefaultListModel<>();
  private final JList<ItemCategory> list = new JList<>(listModel);

  private final boolean isPST;

  private boolean accepted = false;

  public ItemCategoryOrderDialog(Window parent, boolean isPST) {
    super(parent, "Customize item category sort order", Dialog.ModalityType.APPLICATION_MODAL);
    this.isPST = isPST;
    init();
  }

  /** Returns whether the dialog was closed by applying a new item category sort order. */
  public boolean isAccepted() {
    return accepted;
  }

  /**
   * Accepts the current item category sort order.
   * Dialog will be closed and the current sort order state is preserved.
   */
  public void accept() {
    accepted = true;
    setVisible(false);

    // storing current category sort order
    storeCategoryIndices(getPreferencesKey(), getCategoryIndices());
  }

  /** Returns the indices of the current item category sort order. */
  public int[] getCategoryIndices() {
    final int[] indexMap = new int[listModel.size()];
    for (int i = 0, size = listModel.size(); i < size; i++) {
      indexMap[listModel.get(i).getIndex()] = i;
    }
    return indexMap;
  }

  /** Cancels the dialog. Dialog will be closed and changes are discarded. */
  public void cancel() {
    accepted = false;
    setVisible(false);
  }

  /** Resets the item category sort order to their initial state. */
  public void reset() {
    final int[] indexMap = getDefaultCategoryIndices(isPST);
    initList(indexMap);
    list.setSelectedIndex(0);
    list.ensureIndexIsVisible(0);
  }

  /** Reverses the current sort order of the item categories. */
  public void reverseOrder() {
    int minSelectedIndex = list.getSelectionModel().getMinSelectionIndex();
    int maxSelectedIndex = list.getSelectionModel().getMaxSelectionIndex();

    int topIndex = 0;
    int bottomIndex = listModel.size() - 1;
    while (topIndex < bottomIndex) {
      final ItemCategory itemBottom = listModel.remove(bottomIndex);
      final ItemCategory itemTop = listModel.remove(topIndex);
      listModel.add(topIndex, itemBottom);
      listModel.add(bottomIndex, itemTop);
      topIndex++;
      bottomIndex--;
    }

    // updating item selection
    if (minSelectedIndex >= 0) {
      minSelectedIndex = listModel.size() - 1 - minSelectedIndex;
      maxSelectedIndex = listModel.size() - 1 - maxSelectedIndex;
      list.getSelectionModel().setSelectionInterval(maxSelectedIndex, minSelectedIndex);
      list.ensureIndexIsVisible(maxSelectedIndex);
      list.ensureIndexIsVisible(minSelectedIndex);
    }
  }

  /** Moves the selected list items up (towards the beginning) by one step. */
  public void moveUp() {
    final int minIdx = list.getSelectionModel().getMinSelectionIndex();
    final int maxIdx = list.getSelectionModel().getMaxSelectionIndex();
    if (minIdx > 0) {
      for (int idx = minIdx; idx <= maxIdx; idx++) {
        final ItemCategory item = listModel.remove(idx);
        listModel.add(idx - 1, item);
      }
      list.getSelectionModel().setSelectionInterval(minIdx - 1, maxIdx - 1);
    }
  }

  /** Moves the selected list items down (towards the end) by one step. */
  public void moveDown() {
    final int minIdx = list.getSelectionModel().getMinSelectionIndex();
    final int maxIdx = list.getSelectionModel().getMaxSelectionIndex();
    if (maxIdx < listModel.size() - 1) {
      for (int idx = maxIdx; idx >= minIdx; idx--) {
        final ItemCategory item = listModel.remove(idx);
        listModel.add(idx + 1, item);
      }
      list.getSelectionModel().setSelectionInterval(minIdx + 1, maxIdx + 1);
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == okButton) {
      accept();
    } else if (e.getSource() == cancelButton) {
      cancel();
    } else if (e.getSource() == resetButton) {
      reset();
    } else if (e.getSource() == reverseOrderButton) {
      reverseOrder();
    } else if (e.getSource() == moveUpButton) {
      moveUp();
    } else if (e.getSource() == moveDownButton) {
      moveDown();
    }
  }

  @Override
  public void valueChanged(ListSelectionEvent e) {
    final int minIdx = list.getMinSelectionIndex();
    final int maxIdx = list.getMaxSelectionIndex();
    moveUpButton.setEnabled(minIdx > 0);
    moveDownButton.setEnabled(maxIdx >= 0 && maxIdx < listModel.size() - 1);
  }

  /** Populates the list control with item category entries based on the specified category index map. */
  private void initList(int[] indexMap) {
    final String[] defCategories = isPST ? ItemTypeBitmap.CATEGORIES11_ARRAY : ItemTypeBitmap.CATEGORIES_ARRAY;

    // generating item category name list dynamically
    final String[] categories;
    final TreeMap<Long, String> catMap = ItemTypeBitmap.getItemCategories();
    int numItems = catMap.keySet().stream().max((a, b) -> (int) (a - b)).orElse(-1L).intValue() + 1;
    numItems = Math.max(numItems, defCategories.length);
    if (numItems > 0) {
      categories = new String[numItems];
      for (final Map.Entry<Long, String> entry : catMap.entrySet()) {
        final int key = entry.getKey().intValue();
        if (key >= 0 && key < categories.length) {
          categories[key] = entry.getValue();
        }
      }
    } else {
      // falling back to predefined list if categories are not available
      categories = isPST ? ItemTypeBitmap.CATEGORIES11_ARRAY : ItemTypeBitmap.CATEGORIES_ARRAY;
    }

    listModel.clear();
    if (indexMap != null) {
      // we need the inversed index map
      final int[] invMap = new int[indexMap.length];
      for (int i = 0; i < indexMap.length; i++) {
        invMap[indexMap[i]] = i;
      }

      for (final int catIdx : invMap) {
        // use default category list if category name is not defined in generated list
        String catName = null;
        if (catIdx >= 0 && catIdx < categories.length) {
          if (categories[catIdx] != null) {
            catName = categories[catIdx];
          } else if (catIdx < defCategories.length) {
            catName = defCategories[catIdx];
          }
        }

        if (catName != null) {
          listModel.add(listModel.size(), new ItemCategory(catIdx, catName));
        } else {
          Logger.debug("Item category name not available for index {}", catIdx);
        }
      }
    }
  }

  private void init() {
    final int[] defIndexMap = getDefaultCategoryIndices(isPST);
    // loading item category sort order
    // from preferences?
    int[] indexMap = loadCategoryIndices(getPreferencesKey());
    if (indexMap != null && indexMap.length != defIndexMap.length) {
      // invalid map
      indexMap = null;
    }

    // or default values from ItemTypeBitmap class
    if (indexMap == null) {
      indexMap = Arrays.copyOf(defIndexMap, defIndexMap.length);
    }

    // initializing dialog list
    initList(indexMap);

    // setting up user controls
    okButton.addActionListener(this);
    cancelButton.addActionListener(this);
    resetButton.addActionListener(this);
    resetButton.setToolTipText("Reset item categories to suggested sort order.");
    reverseOrderButton.addActionListener(this);
    reverseOrderButton.setToolTipText("Reverse current item category sort order.");

    moveUpButton.setMargin(new Insets(16, 4, 16, 4));
    moveUpButton.addActionListener(this);
    moveDownButton.setMargin(new Insets(16, 4, 16, 4));
    moveDownButton.addActionListener(this);

    list.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
    list.addListSelectionListener(this);

    // calculating dimensions
    final Dimension prefSize = list.getPreferredSize();
    if (prefSize != null && prefSize.width > 0 && prefSize.height > 0) {
      final int prefW = prefSize.width * 3 / 2;
      final int prefH = prefSize.height * 20 / listModel.size();
      prefSize.width = prefW;
      prefSize.height = prefH;
    }

    JScrollPane scroll = new JScrollPane(list);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scroll.setPreferredSize(prefSize);

    // setting up UI
    final Container pane = getContentPane();
    pane.setLayout(new GridBagLayout());

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    final GridBagConstraints gbc = new GridBagConstraints();

    // Side bar with up/down buttons
    final JPanel sideBar = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    sideBar.add(moveUpButton, gbc);
    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(8, 0, 0, 0), 0, 0);
    sideBar.add(moveDownButton, gbc);
    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.VERTICAL,
        new Insets(0, 0, 0, 0), 0, 0);
    sideBar.add(new JPanel(), gbc);

    // bottom bar with reverse/reset buttons
    final JPanel bottomBar = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    bottomBar.add(reverseOrderButton, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 4, 0, 0), 0, 0);
    bottomBar.add(new JPanel(), gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    bottomBar.add(resetButton, gbc);

    // dialog buttons (ok, cancel)
    final JPanel buttonBar = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    buttonBar.add(new JPanel(), gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 4), 0, 0);
    buttonBar.add(okButton, gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 4, 0, 0), 0, 0);
    buttonBar.add(cancelButton, gbc);
    ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    buttonBar.add(new JPanel(), gbc);

    // main panel
    final JLabel caption = new JLabel("Item categories:");
    ViewerUtil.setGBC(gbc, 0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(caption, gbc);

    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
        new Insets(4, 0, 0, 0), 0, 0);
    mainPanel.add(scroll, gbc);
    ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 0.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.VERTICAL,
        new Insets(4, 8, 0, 0), 0, 0);
    mainPanel.add(sideBar, gbc);

    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(bottomBar, gbc);
    ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);

    ViewerUtil.setGBC(gbc, 0, 3, 2, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(16, 0, 0, 0), 0, 0);
    mainPanel.add(buttonBar, gbc);

    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 1.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.BOTH,
        new Insets(8, 8, 8, 8), 0, 0);
    pane.add(mainPanel, gbc);

    // setting up a usable minimum dialog size
    final Dimension dim = getPreferredSize();
    setMinimumSize(new Dimension(dim.width, dim.height * 2 / 3));

    pack();
    setResizable(true);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    setLocationRelativeTo(getOwner());

    if (!listModel.isEmpty()) {
      list.getSelectionModel().setSelectionInterval(0, 0);
      list.ensureIndexIsVisible(0);
    }
    list.requestFocusInWindow();

    // ESC cancels dialog
    final String closeDialogKey = "CloseDialog";
    getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(ESC_KEY, closeDialogKey);
    getRootPane().getActionMap().put(closeDialogKey, new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        cancel();
      }
    });

    setVisible(true);
  }

  /** Returns the Preferences key for the current item category list. */
  public String getPreferencesKey() {
    return getPreferencesKey(isPST);
  }

  /**
   * Returns the Preferences key for the specified store type.
   *
   * @param isPST Whether the store belongs to a PST game.
   * @return Preferences key as string.
   */
  public static String getPreferencesKey(boolean isPST) {
    return PREF_STO_CATEGORY_ORDER_FMT + Profile.getGame().name();
  }

  /**
   * Loads an integer array from the preferences.
   *
   * @param prefKey The Preferences key to use.
   * @return {@code int[]} array of item category indices. Returns {@code null} if no preferences entry was found.
   */
  public static int[] loadCategoryIndices(String prefKey) {
    int[] retVal = null;

    final Preferences prefs = Preferences.userNodeForPackage(StoResource.class);
    final String data = prefs.get(prefKey, null);
    if (data != null) {
      final String[] items = data.split(",");
      retVal = new int[items.length];
      for (int i = 0; i < items.length; i++) {
        try {
          final int value = Integer.parseInt(items[i]);
          retVal[i] = Math.max(0, value);
        } catch (NumberFormatException e) {
          Logger.debug(e);
        }
      }
    }

    return retVal;
  }

  /**
   * Stores the specified integer array in the preferences.
   *
   * @param prefKey The Preferences key to use.
   * @param indices Integer array to store. Specify {@code null} to remove an existing preferences entry.
   */
  public static void storeCategoryIndices(String prefKey, int[] indices) {
    final Preferences prefs = Preferences.userNodeForPackage(StoResource.class);

    if (indices == null) {
      prefs.remove(prefKey);
    } else {
      final String data =
          Arrays.stream(indices).mapToObj(Integer::toString).collect(Collectors.joining(","));
      prefs.put(prefKey, data);
    }
  }

  /** Returns the default index array of item categories for the specified store type. */
  public static int[] getDefaultCategoryIndices(boolean isPST) {
    return isPST ? ItemTypeBitmap.SUGGESTED_CATEGORY_ORDER_PST : ItemTypeBitmap.SUGGESTED_CATEGORY_ORDER;
  }

  // -------------------------- INNER CLASSES --------------------------

  private static class ItemCategory implements Comparable<ItemCategory> {
    private final int index;
    private final String name;

    public ItemCategory(int index, String name) {
      this.index = index;
      this.name = name;
    }

    public int getIndex() {
      return index;
    }

    public String getName() {
      return name;
    }

    @Override
    public String toString() {
      return getName() + " (" + getIndex() + ")";
    }

    @Override
    public int hashCode() {
      return Objects.hash(index);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      ItemCategory other = (ItemCategory)obj;
      return index == other.index;
    }

    @Override
    public int compareTo(ItemCategory o) {
      if (o == null) {
        return 1;
      }

      return getIndex() - o.getIndex();
    }
  }
}
