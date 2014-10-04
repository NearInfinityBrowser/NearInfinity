// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import java.awt.BorderLayout;
import java.awt.event.MouseListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
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

public final class TextListPanel extends JPanel implements DocumentListener, ListSelectionListener
{
  private boolean sortValues = true;
  private final Comparator<Object> ignorecasecomparator = new IgnoreCaseComparator();
  private final DefaultListModel listmodel = new DefaultListModel();
  private final JList list;
  private final JTextField tfield = new JTextField(10);

  public TextListPanel(List<? extends Object> values)
  {
    this(values, true);
  }

  public TextListPanel(List<? extends Object> values, boolean sortValues)
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
    if (list.hasFocus() && list.getSelectedValue() != null)
      tfield.setText(list.getSelectedValue().toString());
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

  public void setValues(List<? extends Object> values)
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

  private static final class IgnoreCaseComparator implements Comparator<Object>
  {
    private IgnoreCaseComparator()
    {
    }

    @Override
    public int compare(Object o1, Object o2)
    {
      return o1.toString().compareToIgnoreCase(o2.toString());
    }

    @Override
    public boolean equals(Object obj)
    {
      return toString().equalsIgnoreCase(obj.toString());
    }
  }
}

