// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JMenuItem;

/**
 * Adds support of user-defined data to the JMenuItem class.
 *
 * @param <E> Specifies the type of the user-defined data that can be assigned.
 */
public class DataMenuItem<E> extends JMenuItem
{
  private E data;

  /** Creates a JMenuItem with no set text, icon or user-defined data. */
  public DataMenuItem()
  {
    super();
    this.data = null;
  }

  /** Creates a JMenuItem with the specified icon. */
  public DataMenuItem(Icon icon)
  {
    super(icon);
    this.data = null;
  }

  /** Creates a JMenuItem with the specified text. */
  public DataMenuItem(String text)
  {
    super(text);
    this.data = null;
  }

  /** Creates a menu item whose properties are taken from the specified Action. */
  public DataMenuItem(Action a)
  {
    super(a);
    this.data = null;
  }

  /** Creates a JMenuItem with the specified text and icon. */
  public DataMenuItem(String text, Icon icon)
  {
    super(text, icon);
    this.data = null;
  }

  /** Creates a JMenuItem with the specified text, icon and user-defined data. */
  public DataMenuItem(String text, Icon icon, E data)
  {
    super(text, icon);
    this.data = data;
  }

  /** Creates a JMenuItem with the specified text and keyboard mnemonic. */
  public DataMenuItem(String text, int mnemonic)
  {
    super(text, mnemonic);
    this.data = null;
  }

  /** Creates a JMenuItem with the specified text, keyboard mnemonic and user-defined data. */
  public DataMenuItem(String text, int mnemonic, E data)
  {
    super(text, mnemonic);
    this.data = data;
  }

  /** Returns the attached data object. */
  public E getData()
  {
    return data;
  }

  /** Assigns a new data object to the JMenuItem instance. */
  public void setData(E data)
  {
    this.data = data;
  }
}
