// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.NearInfinity;
import org.infinity.icon.Icons;
import org.infinity.util.FilteredListModel;
import org.infinity.util.Misc;

public final class TextListPanel<E> extends JPanel implements DocumentListener, ListSelectionListener, ActionListener, ChangeListener
{
  private static boolean filterEnabled = false;

  private boolean sortValues = true;
  private final FilteredListModel<E> listmodel = new FilteredListModel<>(filterEnabled);
  private final JList<E> list = new JList<>();
  private final JTextField tfield = new JTextField();
  private final JToggleButton tbFilter = new JToggleButton(Icons.getIcon("Filter16.png"), filterEnabled);

  public TextListPanel(List<? extends E> values)
  {
    this(values, true);
  }

  public TextListPanel(List<? extends E> values, boolean sortValues)
  {
    super(new BorderLayout());
    this.sortValues = sortValues;
    setValues(values);
    list.setModel(listmodel);
    list.setSelectedIndex(0);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(this);
    list.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    listmodel.addFilterChangeListener(this);
    tfield.getDocument().addDocumentListener(this);

    tbFilter.setToolTipText("Toggle filtering on or off");
    tbFilter.addActionListener(this);
    // Horizontal margins are too wasteful on default l&f
    Insets margin = tbFilter.getMargin();
    margin.left = Math.min(4, margin.left);
    margin.right = Math.min(4, margin.right);
    margin.top = Math.min(4, margin.top);
    margin.bottom = Math.min(4, margin.bottom);
    tbFilter.setMargin(margin);

    JPanel pInput = new JPanel(new BorderLayout());
    pInput.add(tfield, BorderLayout.CENTER);
    pInput.add(tbFilter, BorderLayout.EAST);

    add(pInput, BorderLayout.NORTH);
    add(new JScrollPane(list), BorderLayout.CENTER);
    ensurePreferredComponentWidth(list, true);
    ensurePreferredComponentWidth(tfield, false);
  }

// --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event)
  {
    if (tfield.hasFocus()) {
      if (!filterEnabled) {
        selectClosest(tfield.getText());
      }
      listmodel.setPattern(tfield.getText());
    }
  }

  @Override
  public void removeUpdate(DocumentEvent event)
  {
    if (tfield.hasFocus()) {
      if (!filterEnabled) {
        selectClosest(tfield.getText());
      }
      listmodel.setPattern(tfield.getText());
    }
  }

  @Override
  public void changedUpdate(DocumentEvent event)
  {
  }

// --------------------- End Interface DocumentListener ---------------------

// --------------------- Begin Interface ListSelectionListener ---------------------

  @Override
  public void valueChanged(ListSelectionEvent event)
  {
    if (list.hasFocus() && list.getSelectedValue() != null) {
      if (!tfield.getText().equals(list.getSelectedValue().toString())) {
        tfield.setText(list.getSelectedValue().toString());
        listmodel.setPattern(tfield.getText());
      }
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------

// --------------------- Begin Interface ChangeListener ---------------------

  @Override
  public void stateChanged(ChangeEvent e)
  {
    // fixes issues with scrollbar visibility and visibility of selected entry
    calculatePreferredComponentHeight(list);
  }

// --------------------- End Interface ChangeListener ---------------------

// --------------------- Begin Interface ActionListener ---------------------

  @Override
  public void actionPerformed(ActionEvent e)
  {
    if (e.getSource() == tbFilter) {
      filterEnabled = tbFilter.isSelected();

      if (filterEnabled) {
        listmodel.setPattern(tfield.getText());
      }
      listmodel.setFiltered(filterEnabled);

      ensurePreferredComponentWidth(list, true);
      ensurePreferredComponentWidth(tfield, false);

      int idx = list.getSelectedIndex();
      E item = null;
      try {
        item = listmodel.get(idx);
      } catch (Exception ex) {
      }
      if (item == null || !item.toString().equals(tfield.getText())) {
        selectClosest(tfield.getText());
        idx = list.getSelectedIndex();
      }

      if (idx >= 0) {
        ensureIndexIsVisible(idx);
      }
    }
  }

// --------------------- End Interface ActionListener ---------------------

  @Override
  public synchronized void addMouseListener(MouseListener listener)
  {
    list.addMouseListener(listener);
  }

  @Override
  public void setEnabled(boolean enabled)
  {
    super.setEnabled(enabled);
    list.setEnabled(enabled);
    tfield.setEditable(enabled);
    tbFilter.setEnabled(enabled);
  }

  public void addListSelectionListener(ListSelectionListener listener)
  {
    list.addListSelectionListener(listener);
  }

  public void removeListSelectionListener(ListSelectionListener listener)
  {
    list.removeListSelectionListener(listener);
  }

  public void ensureIndexIsVisible(int i)
  {
    list.ensureIndexIsVisible(i);
  }

  public ListModel<E> getModel()
  {
    return listmodel;
  }

  public int getSelectedIndex()
  {
    return list.getSelectedIndex();
  }

  public E getSelectedValue()
  {
    return list.getSelectedValue();
  }

  public void setSelectedIndex(int index)
  {
    list.setSelectedIndex(index);
    list.ensureIndexIsVisible(index);
    tfield.setText(list.getSelectedValue().toString());
    listmodel.setPattern(list.getSelectedValue().toString());
  }

  public void setSelectedValue(E value, boolean shouldScroll)
  {
    list.setSelectedValue(value, shouldScroll);
    tfield.setText(value.toString());
    listmodel.setPattern(value.toString());
  }

  public void setValues(List<? extends E> values)
  {
    if (this.sortValues) {
      Collections.sort(values, Misc.getIgnoreCaseComparator());
    }
    listmodel.baseClear();
    listmodel.baseAddAll(values);
    tfield.setText("");
    if (list != null) {
      list.setSelectedIndex(0);
      list.ensureIndexIsVisible(0);
    }
  }

  /**
   * Selects the first list item starting with the specified text.
   * Returns the index of the selected item or -1 if not available.
   */
  private int selectClosest(String text)
  {
    int retVal = -1;
    if (!text.isEmpty() && listmodel.getSize() > 0) {
      final String pattern = text.toUpperCase(Locale.ENGLISH);
      E item = listmodel
          .elements()
          .stream()
          .filter(f -> f.toString().toUpperCase(Locale.ENGLISH).startsWith(pattern))
          .findFirst()
          .orElse(listmodel.firstElement());
      retVal = listmodel.indexOf(item);
      if (retVal >= 0) {
        list.setSelectedIndex(retVal);
        list.ensureIndexIsVisible(retVal);
      }
    }
    return retVal;
  }

  /** Recalculates preferred control width enough to fit all list items horizontally. */
  private void ensurePreferredComponentWidth(JComponent c, boolean includeScrollBar)
  {
    if (c == null)
      return;

    final Graphics g = c.getGraphics() != null ? c.getGraphics() : NearInfinity.getInstance().getGraphics();
    final FontMetrics fm = c.getFontMetrics(c.getFont());
    if (fm == null)
      return;

    final E item = listmodel.baseElements()
        .stream()
        .max((e1, e2) -> {
          double w1 = fm.getStringBounds(e1.toString(), g).getWidth();
          double w2 = fm.getStringBounds(e2.toString(), g).getWidth();
          return (int)(w1 - w2);
        })
        .get();
    if (item != null) {
      int cw = (int)fm.getStringBounds(item.toString(), g).getWidth();
      cw += c.getInsets().left;
      cw += c.getInsets().right;
      if (includeScrollBar) {
        int sbWidth = 0;
        try {
          sbWidth = ((Integer)UIManager.get("ScrollBar.width")).intValue();
        } catch (Exception ex) {
          // not all l&f styles provide UIManager value
          sbWidth = (new JScrollBar(JScrollBar.VERTICAL)).getWidth();
        }
        cw += sbWidth;
      }
      Dimension d = c.getPreferredSize();
      d.width = cw;
      c.setPreferredSize(d);
      c.invalidate();
    }
  }

  /** Enforces recalculation of preferred control height. */
  private void calculatePreferredComponentHeight(JComponent c)
  {
    if (c == null)
      return;

    int width = c.getPreferredSize().width;
    c.setPreferredSize(null);
    Dimension d = c.getPreferredSize();
    d.width = width;
    c.setPreferredSize(d);
    c.invalidate();
  }
}
