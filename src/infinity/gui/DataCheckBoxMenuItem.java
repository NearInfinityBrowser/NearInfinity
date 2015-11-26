// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.gui;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;

/**
 * Adds support of user-defined data to the JCheckBoxButtonMenuItem class.
 */
public class DataCheckBoxMenuItem extends JCheckBoxMenuItem
{
  private Object data;

  /** Creates a JCheckBoxMenuItem with no set text or icon. */
  public DataCheckBoxMenuItem()
  {
    super();
    this.data = null;
  }

  /** Creates a JCheckBoxMenuItem whose properties are taken from the Action supplied. */
  public DataCheckBoxMenuItem(Action a)
  {
    this(a, null);
  }

  /** Creates a JCheckBoxMenuItem whose properties are taken from the Action supplied and userdefined data. */
  public DataCheckBoxMenuItem(Action a, Object data)
  {
    super(a);
    this.data = data;
  }

  /** Creates a JCheckBoxMenuItem with an icon. */
  public DataCheckBoxMenuItem(Icon icon)
  {
    this(icon, null);
  }

  /** Creates a JCheckBoxMenuItem with an icon and userdefined data. */
  public DataCheckBoxMenuItem(Icon icon, Object data)
  {
    super(icon);
    this.data = data;
  }

  /** Creates a JCheckBoxMenuItem with text. */
  public DataCheckBoxMenuItem(String text)
  {
    this(text, null);
  }

  /** Creates a JCheckBoxMenuItem with text and userdefined data. */
  public DataCheckBoxMenuItem(String text, Object data)
  {
    super(text);
    this.data = data;
  }

  /** Creates a JCheckBoxMenuItem with the specified text and selection state. */
  public DataCheckBoxMenuItem(String text, boolean b)
  {
    this(text, b, null);
  }

  /** Creates a JCheckBoxMenuItem with the specified text, selection state and userdefined data. */
  public DataCheckBoxMenuItem(String text, boolean b, Object data)
  {
    super(text, b);
    this.data = data;
  }

  /** Creates a JCheckBoxMenuItem with the specified text and icon. */
  public DataCheckBoxMenuItem(String text, Icon icon)
  {
    this(text, icon, null);
  }

  /** Creates a JCheckBoxMenuItem with the specified text, icon and userdefined data. */
  public DataCheckBoxMenuItem(String text, Icon icon, Object data)
  {
    super(text, icon);
    this.data = data;
  }

  /** Creates a JCheckBoxMenuItem with the specified text, icon and selection state. */
  public DataCheckBoxMenuItem(String text, Icon icon, boolean b)
  {
    this(text, icon, b, null);
  }

  /**
   * Creates a JCheckBoxMenuItem with the specified text, icon, selection state and userdefined data.
   */
  public DataCheckBoxMenuItem(String text, Icon icon, boolean b, Object data)
  {
    super(text, icon, b);
    this.data = data;
  }

  /** Returns the attached data object. */
  public Object getData()
  {
    return data;
  }

  /** Assigns a new data object to the JRadioButtonMenuItem instance. */
  public void setData(Object data)
  {
    this.data = data;
  }
}
