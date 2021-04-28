// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.datatype;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.function.BiFunction;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.infinity.gui.StructViewer;
import org.infinity.gui.TextListPanel;
import org.infinity.gui.ViewerUtil;
import org.infinity.icon.Icons;
import org.infinity.resource.AbstractStruct;
import org.infinity.util.Misc;

/**
 * Common base class for fields that represent an integer enumeration of some values.
 * @param <T> type of the symbolic representation of the numeric value
 */
public class AbstractBitmap<T> extends Datatype implements Editable, IsNumeric
{
  // The default formatter if none is specified in the constructor
  private final BiFunction<Long, T, String> formatterDefault = (value, item) -> {
    String s;
    if (isShowAsHex()) {
      switch (getSize()) {
        case 1:
          s = String.format("0x%02X", value);
          break;
        case 2:
          s = String.format("0x%04X", value);
          break;
        case 4:
          s = String.format("0x%08X", value);
          break;
        default:
          s = String.format("0x%X", value);
      }
    } else {
      s = value.toString();
    }
    if (item != null) {
      return item.toString() + " - " + s;
    } else {
      return "Unknown - " + s;
    }
  };

  private final TreeMap<Long, T> itemMap;
  private final List<JButton> buttonList;

  private BiFunction<Long, T, String> formatter;
  private TextListPanel<FormattedData<T>> list;
  private JButton bUpdate;
  private long value;
  private boolean signed;
  private boolean sortByName;
  private boolean showAsHex;

  /**
   * Constructs a new field that represents an integer enumeration using a default formatter.
   * By default value is treated as unsigned and shown in decimal notation. List is sorted by value.
   * @param buffer the buffer object
   * @param offset offset of data in the buffer
   * @param length size of data object in bytes
   * @param name field name
   * @param items a collection of number-to-symbol mappings
   */
  public AbstractBitmap(ByteBuffer buffer, int offset, int length, String name, TreeMap<Long, T> items)
  {
    this(buffer, offset, length, name, items, null, false);
  }

  /**
   * Constructs a new field that represents an integer enumeration.
   * By default value is treated as unsigned and shown in decimal notation. List is sorted by value.
   * @param buffer the buffer object
   * @param offset offset of data in the buffer
   * @param length size of data object in bytes
   * @param name field name
   * @param items a collection of number-to-symbol mappings
   * @param formatter a function that is used to produce the textual output
   */
  public AbstractBitmap(ByteBuffer buffer, int offset, int length, String name, TreeMap<Long, T> items,
      BiFunction<Long, T, String> formatter)
  {
    this(buffer, offset, length, name, items, formatter, false);
  }

  /**
   * Constructs a new field that represents an integer enumeration.
   * By default value is shown in decimal notation. List is sorted by value.
   * @param buffer the buffer object
   * @param offset offset of data in the buffer
   * @param length size of data object in bytes
   * @param name field name
   * @param items a collection of number-to-symbol mappings
   * @param formatter a function that is used to produce the textual output
   * @param signed indicates whether numeric value is treated as a signed value
   */
  public AbstractBitmap(ByteBuffer buffer, int offset, int length, String name, TreeMap<Long, T> items,
                        BiFunction<Long, T, String> formatter, boolean signed)
  {
    super(offset, length, name);
    this.itemMap = items;
    this.signed = signed;
    this.formatter = (formatter != null) ? formatter : formatterDefault;
    this.buttonList = new ArrayList<>();
    this.bUpdate = new JButton("Update value", Icons.getIcon(Icons.ICON_REFRESH_16));
    this.buttonList.add(this.bUpdate);

    read(buffer, offset);
  }

  //--------------------- Begin Interface Writeable ---------------------

  @Override
  public void write(OutputStream os) throws IOException
  {
    writeLong(os, value);
  }

  //--------------------- End Interface Writeable ---------------------

  //--------------------- Begin Interface Readable ---------------------

  @Override
  public int read(ByteBuffer buffer, int offset)
  {
    buffer.position(offset);
    switch (getSize()) {
      case 1:
        value = buffer.get() & 0xffL;
        break;
      case 2:
        value = buffer.getShort() & 0xffffL;
        break;
      case 4:
        value = buffer.getInt() & 0xffffffffL;
        break;
      default:
        throw new IllegalArgumentException();
    }
    setValue(value);  // called to ensure correct signedness
    return offset + getSize();
  }

  //--------------------- End Interface Readable ---------------------

  //--------------------- Begin Interface IsNumeric ---------------------

  @Override
  public long getLongValue()
  {
    return value;
  }

  @Override
  public int getValue()
  {
    return (int)value;
  }

  //--------------------- End Interface IsNumeric ---------------------

  //--------------------- Begin Interface Editable ---------------------

  @Override
  public JComponent edit(ActionListener container)
  {
    FormattedData<T> selected = null;
    final List<FormattedData<T>> items = new ArrayList<>(itemMap.size());
    for (final Long key : itemMap.keySet()) {
      FormattedData<T> item = new FormattedData<>(key, itemMap.get(key), formatter);
      items.add(item);
      if (key.intValue() == value) {
        selected = item;
      }
    }

    list = new TextListPanel<>(items, sortByName);
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent event)
      {
        if (event.getClickCount() == 2) {
          container.actionPerformed(new ActionEvent(this, 0, StructViewer.UPDATE_VALUE));
        }
      }
    });
    list.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent event)
      {
        if (!event.getValueIsAdjusting()) {
          listItemChanged();
        }
      }
    });

    if (selected != null) {
      list.setSelectedValue(selected, true);
    }

    bUpdate.addActionListener(container);
    bUpdate.setActionCommand(StructViewer.UPDATE_VALUE);

    // lay out controls
    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();

    c = ViewerUtil.setGBC(c, 0, 0, 1, buttonList.size() + 2, 1.0, 1.0, GridBagConstraints.CENTER,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(list, c);

    c = ViewerUtil.setGBC(c, 1, 0, 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(new JPanel(), c);
    for (int i = 0; i < buttonList.size(); i++) {
      c = ViewerUtil.setGBC(c, 1, 1 + i, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START,
                            GridBagConstraints.HORIZONTAL, new Insets(0, 6, 8, 0), 0, 0);
      panel.add(buttonList.get(i), c);
    }
    c = ViewerUtil.setGBC(c, 1, 1 + buttonList.size(), 1, 1, 1.0, 1.0, GridBagConstraints.LINE_START,
                          GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    panel.add(new JPanel(), c);

    panel.setMinimumSize(Misc.getScaledDimension(DIM_MEDIUM));
    return panel;
  }

  @Override
  public void select()
  {
    list.ensureIndexIsVisible(list.getSelectedIndex());
  }

  @Override
  public boolean updateValue(AbstractStruct struct)
  {
    long oldValue = getLongValue();

    // updating value
    FormattedData<T> item = list.getSelectedValue();
    if (item != null) {
      setValue(item.getValue().longValue());
    } else {
      return false;
    }

    // notifying listeners
    if (getLongValue() != oldValue) {
      fireValueUpdated(new UpdateEvent(this, struct));
    }

    return true;
  }

  //--------------------- End Interface Editable ---------------------

  @Override
  public String toString()
  {
    return toString(value);
  }

  @Override
  public int hashCode()
  {
    int hash = super.hashCode();

    // taking care of signedness
    long mask = (1L << (getSize() * 8)) - 1L;
    hash = 31 * hash + Long.hashCode(value & mask);

    return hash;
  }

  @Override
  public boolean equals(Object o)
  {
    if (!super.equals(o) || !(o instanceof AbstractBitmap<?>)) {
      return false;
    }
    AbstractBitmap<?> other = (AbstractBitmap<?>)o;

    // taking care of signedness
    long mask = (1L << (getSize() * 8)) - 1L;
    boolean retVal = ((value & mask) == (other.value & mask));

    return retVal;
  }

  /** Returns the TextListPanel control used by this datatype. */
  public JComponent getUiControl()
  {
    return list;
  }

  /**
   * Returns the bitmap table used to associated numeric values with symbolic data.
   */
  public TreeMap<Long, T> getBitmap()
  {
    return itemMap;
  }

  /**
   * Returns the numeric value of the selected entry in the list control.
   * Returns {@code null} if no valid list item is selected.
   */
  public Long getSelectedValue()
  {
    Long retVal = null;
    FormattedData<T> item = list.getSelectedValue();
    if (item != null) {
      retVal = item.getValue();
    }
    return retVal;
  }

  /**
   * Returns the data object associated with the specified argument.
   * @param value a numeric value
   * @return the data object associated with the numeric value, {@code null} otherwise.
   */
  public T getDataOf(long value)
  {
    return itemMap.get(Long.valueOf(value));
  }

  /**
   * Calls the formatter on the data associated with the specified argument and returns the result.
   * @param value a numeric value
   * @return a {@code String} based on the data associated with the specified argument
   */
  public String toString(long value)
  {
    Long number = Long.valueOf(value);
    T data = itemMap.get(Long.valueOf(value));
    return formatter.apply(number, data);
  }

  /** Returns the function object that is used to create the return value of the {@link #toString()} method. */
  public BiFunction<Long, T, String> getFormatter()
  {
    return formatter;
  }

  /**
   * Assigns a new formatter function object that is used to create the textual representation of the field value.
   * @param formatter the new formatter function object. Specify {@code null} too use the default formatter.
   */
  public void setFormatter(BiFunction<Long, T, String> formatter)
  {
    if (formatter == null) {
      formatter = formatterDefault;
    }
    this.formatter = formatter;
  }

  /** Returns whether the value is treated as a signed number. */
  public boolean isSigned()
  {
    return signed;
  }

  /** Sets whether the value is treated as a signed number. The current value is converted accordingly. */
  public void setSigned(boolean b)
  {
    if (signed != b) {
      signed = b;
      setValue(getLongValue());
    }
  }

  /** Returns whether whether entries are sorted by name instead of value. */
  public boolean isSortByName()
  {
    return sortByName;
  }

  /** Sets whether whether entries are sorted by name instead of value.
   * Does nothing if this method is called after the UI control is created. */
  public void setSortByName(boolean b)
  {
    if (list == null) {
      sortByName = b;
    }
  }

  /** Returns whether numeric value is shown in hexadecimal notation. */
  public boolean isShowAsHex()
  {
    return showAsHex;
  }

  /** Sets whether numeric value is shown in hexadecimal notation. */
  public void setShowAsHex(boolean b)
  {
    showAsHex = b;
  }

  /**
   * Adds the specified data to the item list. Existing entries will be overwritten.
   * @param value the numeric value
   * @param data data associated with the value
   */
  protected void putItem(long value, T data)
  {
    itemMap.put(Long.valueOf(value), data);
  }

  /** Assigns a new value. */
  protected void setValue(long newValue)
  {
    final long oldValue = getLongValue();

    // ensure correct signedness
    switch (getSize()) {
      case 1:
        newValue = isSigned() ? ((newValue << 56) >> 56) : (newValue & 0xffL);
        break;
      case 2:
        newValue = isSigned() ? ((newValue << 48) >> 48) : (newValue & 0xffffL);
        break;
      case 4:
        newValue = isSigned() ? ((newValue << 32) >> 32) : (newValue & 0xffffffffL);
        break;
    }

    this.value = newValue;
    if (oldValue != this.value) {
      firePropertyChange(oldValue, newValue);
    }
  }

  /**
   * Allows derived classes to add custom buttons to the UI control.
   * Only effective if called before the UI control is created.
   */
  protected void addButtons(JButton... buttons)
  {
    // It makes no sense to register new buttons if the list panel has already been created
    if (list == null) {
      for (final JButton btn : buttons) {
        if (btn != null) {
          buttonList.add(btn);
        }
      }
    }
  }

  /** Called whenever the user selects a new list item. */
  protected void listItemChanged()
  {
  }

  /**
   * Helper method: returns the specified number in hexadecimal notation.
   * @param value the value to return as hexadecimal representation.
   * @param size size of the value in bytes.
   * @return String containing hexadecimal notation of the specified value.
   */
  protected String getHexValue(long value)
  {
    switch (getSize()) {
      case 1:
        return String.format("0x%02X", value);
      case 2:
        return String.format("0x%04X", value);
      case 4:
        return String.format("0x%08X", value);
      default:
        return String.format("0x%X", value);
    }
  }

  //-------------------------- INNER CLASSES --------------------------

  /**
   * A helper class used to encapsulate the formatter function object.
   */
  protected static class FormattedData<T>
  {
    private final Long value;
    private final T data;
    private final BiFunction<Long, T, String> formatter;

    public FormattedData(long value, T data, BiFunction<Long, T, String> formatter)
    {
      this.value = Long.valueOf(value);
      this.data = data;
      this.formatter = formatter;
    }

    public Long getValue() { return value; }

    public T getData() { return data; }

    public BiFunction<Long, T, String> getFormatter() { return formatter; }

    @Override
    public String toString()
    {
      if (formatter != null) {
        return formatter.apply(value, data);
      } else if (data != null) {
        return data.toString();
      } else {
        return "(null)";
      }
    }
  }
}
