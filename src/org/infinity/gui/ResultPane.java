// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.util.Misc;

/**
 * A customizable panel for presenting data records in a table layout and associated component, such as title or button
 * controls.
 *
 * @param T {@link JTable} or derived table class.
 */
public class ResultPane<T extends JTable> extends JPanel implements ActionListener, ListSelectionListener {
  private final ArrayList<JButton> buttonList = new ArrayList<>();

  private final JLabel title;
  private final T table;
  private final JComponent controlsPanel;
  private final StatusBar statusBar;

  private Consumer<ActionEvent> actionPerformed;
  private Consumer<ListSelectionEvent> tableSelectionChanged;
  private Consumer<MouseEvent> tableAction;

  /**
   * Creates a new panel for a results table and associated controls. Does not add title and status bar.
   *
   * @param table   The results table as {@link T} object.
   * @param buttons Set of buttons that are shown below the table component. Buttons are laid out sequentially and
   *                  horizontally centered. Specify {@code null} to omit the button bar.
   */
  public ResultPane(T table, JButton[] buttons) {
    this(table, buttons, null, false, false);
  }

  /**
   * Creates a new panel for a results table and associated controls. Does not add a status bar.
   *
   * @param table   The results table as {@link T} object.
   * @param buttons Set of buttons that are shown below the table component. Buttons are laid out sequentially and
   *                  horizontally centered. Specify {@code null} to omit the button bar.
   * @param title   An optional title string that is displayed above the table control. Specify {@code null} to omit it.
   *                  It is horizontally centered by default.
   */
  public ResultPane(T table, JButton[] buttons, String title) {
    this(table, buttons, title, false, false);
  }

  /**
   * Creates a new panel for a results table and associated controls.
   *
   * @param table            The results table as {@link T} object.
   * @param buttons          Set of buttons that are shown below the table component. Buttons are laid out sequentially
   *                           and horizontally centered. Specify {@code null} to omit the button bar.
   * @param title            An optional title string that is displayed above the table control. Specify {@code null} to
   *                           omit it. It is horizontally centered by default.
   * @param showStatus       Indicates whether a status bar should be added.
   * @param showCursorStatus Indicates whether the status bar should display a separate section for the cursor status.
   *                           Ignored if {@code showStatus} is {@code false}.
   */
  public ResultPane(T table, JButton[] buttons, String title, boolean showStatus, boolean showCursorStatus) {
    this(table, createButtonPanel(buttons), title, showStatus, showCursorStatus);
    initButtons(this);
  }

  /**
   * Creates a new panel for a results table and associated controls. Does not add title and status bar.
   *
   * @param table         The results table as {@link T} object.
   * @param controlsPanel A control panel that is added below the table control. Specify {@code null} to omit it.
   */
  public ResultPane(T table, JComponent controlsPanel) {
    this(table, controlsPanel, null, false, false);
  }

  /**
   * Creates a new panel for a results table and associated controls. Does not add a status bar.
   *
   * @param table         The results table as {@link T} object.
   * @param controlsPanel A control panel that is added below the table control. Specify {@code null} to omit it.
   * @param title         An optional title string that is displayed above the table control. Specify {@code null} to
   *                        omit it. It is horizontally centered by default.
   */
  public ResultPane(T table, JComponent controlsPanel, String title) {
    this(table, controlsPanel, title, false, false);
  }

  /**
   * Creates a new panel for a results table and associated controls.
   *
   * @param table            The results table as {@link T} object.
   * @param controlsPanel    A control panel that is added below the table control. Specify {@code null} to omit it.
   * @param title            An optional title string that is displayed above the table control. Specify {@code null} to
   *                           omit it. It is horizontally centered by default.
   * @param showStatus       Indicates whether a status bar should be added.
   * @param showCursorStatus Indicates whether the status bar should display a separate section for the cursor status.
   *                           Ignored if {@code showStatus} is {@code false}.
   */
  public ResultPane(T table, JComponent controlsPanel, String title, boolean showStatus, boolean showCursorStatus) {
    super(new BorderLayout(0, 0));
    this.table = Objects.requireNonNull(table);
    this.table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
    this.table.setRowHeight(this.table.getFontMetrics(this.table.getFont()).getHeight() + 1);
    this.table.getSelectionModel().addListSelectionListener(this);

    if (title != null) {
      this.title = new JLabel(title, SwingConstants.CENTER);
      this.title.setFont(this.title.getFont().deriveFont(this.title.getFont().getSize2D() + 2.0f));
    } else {
      this.title = null;
    }

    this.controlsPanel = (controlsPanel != null) ? controlsPanel : new JPanel();

    if (showStatus) {
      this.statusBar = new StatusBar(showCursorStatus, false);
      this.table.getSelectionModel().addListSelectionListener(this.statusBar);
      this.statusBar.setMessage("");
      this.statusBar.setCursorText("");
    } else {
      this.statusBar = null;
    }

    init();
  }

  /** Returns the {@link T} instance associated with this result pane. */
  public T getTable() {
    return table;
  }

  /**
   * Returns the control panel associated with this result pane. This is either the controls panel specified in the
   * constructor or a button panel created with the array of {@link JButton} instances specified in the constructor.
   */
  public JComponent getControlsPanel() {
    return controlsPanel;
  }

  /**
   * Returns the {@link JButton} at the specified position in the control panel. Returns {@code null} if the pane has
   * not been explicitly constructed with a button bar.
   */
  public JButton getButton(int index) {
    if (index >= 0 && index < buttonList.size()) {
      return buttonList.get(index);
    }
    return null;
  }

  /**
   * A convenience method that returns the current message string from the status bar. Returns an empty string if no
   * status bar was defined.
   */
  public String getStatusMessage() {
    return (statusBar != null) ? statusBar.getMessage() : "";
  }

  /** Returns the title string if available. Otherwise, an empty string is returned. */
  public String getTitle() {
    return (title != null) ? title.getText() : "";
  }

  /** Sets the title string to the specified text. Does nothing if no title is specified. */
  public void setTitle(String text) {
    if (text == null) {
      text = "";
    }
    if (title != null) {
      title.setText(text);
    }
  }

  /**
   * Returns the alignment of the title label's content along the X axis. Returns {@code -1} if title is not defined.
   */
  public int getTitleAlignment() {
    if (title != null) {
      return title.getHorizontalAlignment();
    }
    return -1;
  }

  /** Sets the alignment of the title label's content along the X axis. */
  public void setTitleAlignment(int alignment) {
    if (title != null) {
      title.setHorizontalAlignment(alignment);
    }
  }

  /**
   * A convenience method that sets the message string in the status bar. Does nothing if no status bar was defined.
   */
  public void setStatusMessage(String msg) {
    if (statusBar != null) {
      final String s = statusBar.getCursorText();
      statusBar.setMessage(msg);
      statusBar.setCursorText(s);
    }
  }

  /**
   * A convenience method that returns the current cursor message string from the status bar. Returns an empty string if
   * no status bar or cursor section was defined.
   */
  public String getStatusCursorMessage() {
    return (statusBar != null) ? statusBar.getCursorText() : "";
  }

  /**
   * A convenience method that sets the cursor message string in the status bar. Does nothing if no status bar or cursor
   * section was defined.
   */
  public void setStatusCursorMessage(String msg) {
    if (statusBar != null) {
      statusBar.setCursorText(msg);
    }
  }

  /**
   * Returns the {@link Consumer} function that is called whenever an {@code actionPerformed} event is triggered on the
   * buttons in the controls panel.
   */
  public Consumer<ActionEvent> getOnActionPerformed() {
    return actionPerformed;
  }

  /**
   * Defines a {@link Consumer} function that is called whenever an {@code actionPerformed} event is triggered on the
   * buttons in the controls panel. Specify {@code null} to remove the function.
   */
  public ResultPane<T> setOnActionPerformed(Consumer<ActionEvent> actionPerformed) {
    this.actionPerformed = actionPerformed;
    return this;
  }

  /**
   * Returns the {@link Consumer} function that is called whenever a double click is performed on the results table.
   */
  public Consumer<MouseEvent> getOnTableAction() {
    return tableAction;
  }

  /**
   * Defines a {@link Consumer} function that is called whenever a double-click is performed on the results table.
   * Specify {@code null} to remove the function.
   */
  public ResultPane<T> setOnTableAction(Consumer<MouseEvent> tableAction) {
    this.tableAction = tableAction;
    return this;
  }

  /**
   * Returns the {@link Consumer} function that is called whenever a {@code valueChanged} event is triggered by the
   * table selection model.
   */
  public Consumer<ListSelectionEvent> getOnTableSelectionChanged() {
    return tableSelectionChanged;
  }

  /**
   * Defines a {@link Consumer} function that is called whenever a {@code valueChanged} event is triggered by the
   * table selection model. Specify {@code null} to remove the function.
   */
  public ResultPane<T> setOnTableSelectionChanged(Consumer<ListSelectionEvent> selectionChanged) {
    this.tableSelectionChanged = selectionChanged;
    return this;
  }

  /** Initializes the UI controls. */
  private void init() {
    final JPanel mainPanel = new JPanel(new BorderLayout(0, 3));
    if (title != null) {
      mainPanel.add(title, BorderLayout.NORTH);
    }

    final JScrollPane scroll = new JScrollPane(table);
    scroll.getViewport().setBackground(table.getBackground());
    mainPanel.add(scroll, BorderLayout.CENTER);

    // double-click on table triggers action event
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent event) {
        if (event.getClickCount() == 2 && tableAction != null) {
          tableAction.accept(event);
        }
      }
    });

    if (controlsPanel != null) {
      mainPanel.add(controlsPanel, BorderLayout.SOUTH);
    }

    add(mainPanel, BorderLayout.CENTER);
    if (statusBar != null) {
      add(statusBar, BorderLayout.SOUTH);
    }
    final Dimension defSize = new Dimension(getPreferredSize());
    defSize.width = defSize.width * 3 / 2;
    setPreferredSize(defSize);
  }

  /** Registers all buttons in the control panel internally and adds the specified action listener. */
  private void initButtons(ActionListener l) {
    if (l == null) {
      return;
    }

    for (int i = 0, count = controlsPanel.getComponentCount(); i < count; i++) {
      final Component c = controlsPanel.getComponent(i);
      if (c instanceof JButton) {
        final JButton b = (JButton)c;
        buttonList.add(b);
        b.removeActionListener(l);
        b.addActionListener(l);
      }
    }
  }

  /** Returns a fully configured button panel. */
  private static JComponent createButtonPanel(JButton[] buttons) {
    JPanel panel = null;
    if (buttons != null) {
      panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      for (final JButton button : buttons) {
        if (button != null) {
          panel.add(button);
        }
      }
    }
    return panel;
  }

  // --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e) {
    if (actionPerformed != null) {
      actionPerformed.accept(e);
    }
  }

  // --------------------- End Interface ActionListener ---------------------

  // --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent e) {
    if (tableSelectionChanged != null) {
      tableSelectionChanged.accept(e);
    }
  }

  // --------------------- End Interface ListSelectionListener ---------------------
}
