// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public final class TextListPanel extends JPanel implements DocumentListener, ListSelectionListener
{
  private boolean sortValues = true;
  private final Comparator ignorecasecomparator = new IgnoreCaseComparator();
  private final DefaultListModel listmodel = new DefaultListModel();
  private final JList list;
  private final JTextField tfield = new JTextField(10);

  public TextListPanel(List values) {
    this(values, true);
  }
  public TextListPanel(List values, boolean sortValues)
  {
    this.sortValues = sortValues;
    setValues(values);
    list = new JList(listmodel);
    list.setSelectedIndex(0);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.addListSelectionListener(this);
    list.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    tfield.getDocument().addDocumentListener(this);

    setLayout(new BorderLayout());
    add(tfield, BorderLayout.NORTH);
    add(new JScrollPane(list), BorderLayout.CENTER);
  }

// --------------------- Begin Interface DocumentListener ---------------------

  public void insertUpdate(DocumentEvent event)
  {
    if (tfield.hasFocus())
      selectClosest(tfield.getText());
  }

  public void removeUpdate(DocumentEvent event)
  {
    if (tfield.hasFocus())
      selectClosest(tfield.getText());
  }

  public void changedUpdate(DocumentEvent event)
  {
  }

// --------------------- End Interface DocumentListener ---------------------


// --------------------- Begin Interface ListSelectionListener ---------------------

  public void valueChanged(ListSelectionEvent event)
  {
    if (list.hasFocus() && list.getSelectedValue() != null)
      tfield.setText(list.getSelectedValue().toString());
  }

// --------------------- End Interface ListSelectionListener ---------------------

  public synchronized void addMouseListener(MouseListener listener)
  {
    list.addMouseListener(listener);
  }

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

  public void ensureIndexIsVisible(int i)
  {
    list.ensureIndexIsVisible(i);
  }

  public ListModel getModel()
  {
    return listmodel;
  }

  public int getSelectedIndex()
  {
    return list.getSelectedIndex();
  }

  public Object getSelectedValue()
  {
    return list.getSelectedValue();
  }

  public void setSelectedIndex(int index)
  {
    list.setSelectedIndex(index);
    list.ensureIndexIsVisible(index);
    tfield.setText(list.getSelectedValue().toString());
  }

  public void setSelectedValue(Object value, boolean shouldScroll)
  {
    list.setSelectedValue(value, shouldScroll);
    tfield.setText(value.toString());
  }

  public void setValues(List values)
  {
    if (this.sortValues) {
      Collections.sort(values, ignorecasecomparator);
    }
    listmodel.clear();
    for (int i = 0; i < values.size(); i++)
      listmodel.addElement(values.get(i));
    tfield.setText("");
    if (list != null) {
      list.setSelectedIndex(0);
      list.ensureIndexIsVisible(0);
    }
  }

  private void selectClosest(String text)
  {
    ListModel lm = list.getModel();
    int selected = 0;
    while (selected < lm.getSize() && text.compareToIgnoreCase(lm.getElementAt(selected).toString()) > 0)
      selected++;
    if (selected == lm.getSize())
      selected--;
    list.setSelectedIndex(selected);
    list.ensureIndexIsVisible(selected);
  }

// -------------------------- INNER CLASSES --------------------------

  private static final class IgnoreCaseComparator implements Comparator
  {
    private IgnoreCaseComparator()
    {
    }

    public int compare(Object o1, Object o2)
    {
      return o1.toString().compareToIgnoreCase(o2.toString());
    }

    public boolean equals(Object obj)
    {
      return toString().equalsIgnoreCase(obj.toString());
    }
  }
}

