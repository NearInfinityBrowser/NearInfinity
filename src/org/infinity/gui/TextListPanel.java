// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.resource.HasIcon;
import org.infinity.resource.IconCache;
import org.infinity.util.Misc;
import org.infinity.util.SimpleListModel;

public final class TextListPanel<E> extends JPanel implements DocumentListener, ListSelectionListener
{
  private boolean sortValues = true;
  private final SimpleListModel<E> listmodel = new SimpleListModel<>();
  private final JList<E> list;
  private final JTextField tfield = new JTextField(10);

  public TextListPanel(List<? extends E> values)
  {
    this(values, true);
  }

  public TextListPanel(List<? extends E> values, boolean sortValues)
  {
    this.sortValues = sortValues;
    setValues(values);
    list = new JList<>(listmodel);
    list.setSelectedIndex(0);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(this);
    list.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    list.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus)
      {
        final JLabel label = (JLabel)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof HasIcon) {
          label.setIcon(IconCache.getIcon((HasIcon) value));
        }
        return label;
      }
    });
    tfield.getDocument().addDocumentListener(this);

    setLayout(new BorderLayout());
    add(tfield, BorderLayout.NORTH);
    add(new JScrollPane(list), BorderLayout.CENTER);
  }

// --------------------- Begin Interface DocumentListener ---------------------

  @Override
  public void insertUpdate(DocumentEvent event)
  {
    if (tfield.hasFocus())
      selectClosest(tfield.getText());
  }

  @Override
  public void removeUpdate(DocumentEvent event)
  {
    if (tfield.hasFocus())
      selectClosest(tfield.getText());
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
      tfield.setText(list.getSelectedValue().toString());
    }
  }

// --------------------- End Interface ListSelectionListener ---------------------

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
  }

  public void setSelectedValue(E value, boolean shouldScroll)
  {
    list.setSelectedValue(value, shouldScroll);
    tfield.setText(value.toString());
  }

  public void setValues(List<? extends E> values)
  {
    if (this.sortValues) {
      Collections.sort(values, Misc.getIgnoreCaseComparator());
    }
    listmodel.clear();
    for (int i = 0; i < values.size(); i++) {
      listmodel.addElement(values.get(i));
    }
    tfield.setText("");
    if (list != null) {
      list.setSelectedIndex(0);
      list.ensureIndexIsVisible(0);
    }
  }

  private void selectClosest(String text)
  {
    int selected = 0;
    if (!text.isEmpty()) {
      text = text.toUpperCase(Locale.ENGLISH);
      for (int size = listmodel.getSize(); selected < size; selected++) {
        final String s = listmodel.getElementAt(selected).toString().toUpperCase(Locale.ENGLISH);
        if (s.startsWith(text)) {
          break;
        }
      }
    }
    if (selected < listmodel.getSize()) {
      list.setSelectedIndex(selected);
      list.ensureIndexIsVisible(selected);
    }
  }
}

