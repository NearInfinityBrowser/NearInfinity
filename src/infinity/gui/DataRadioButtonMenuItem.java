package infinity.gui;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JRadioButtonMenuItem;

/**
 * Adds support of user-defined data to the JRadioButtonMenuItem class.
 *
 * @param <E> Specifies the type of the user-defined data that can be assigned.
 */
public class DataRadioButtonMenuItem<E> extends JRadioButtonMenuItem
{
  private E data;

  /** Creates a JRadioButtonMenuItem with no set text or icon. */
  public DataRadioButtonMenuItem()
  {
    super();
    this.data = null;
  }

  /** Creates a JRadioButtonMenuItem with an icon. */
  public DataRadioButtonMenuItem(Icon icon)
  {
    super(icon);
    this.data = null;
  }

  /** Creates a JRadioButtonMenuItem with text. */
  public DataRadioButtonMenuItem(String text)
  {
    super(text);
    this.data = null;
  }

  /** Creates a radio button menu item whose properties are taken from the Action supplied. */
  public DataRadioButtonMenuItem(Action a)
  {
    super(a);
    this.data = null;
  }

  /**
   * Creates a radio button menu item whose properties are taken from the Action supplied and
   * assigns the specified user-defined data.
   */
  public DataRadioButtonMenuItem(Action a, E data)
  {
    super(a);
    this.data = data;
  }

  /** Creates a radio button menu item with the specified text and Icon. */
  public DataRadioButtonMenuItem(String text, Icon icon)
  {
    super(text, icon);
    this.data = null;
  }

  /** Creates a radio button menu item with the specified text and selection state. */
  public DataRadioButtonMenuItem(String text, boolean selected)
  {
    super(text, selected);
    this.data = null;
  }

  /**
   * Creates a radio button menu item with the specified text, selection state and
   * user-defined data.
   */
  public DataRadioButtonMenuItem(String text, boolean selected, E data)
  {
    super(text, selected);
    this.data = data;
  }

  /** Creates a radio button menu item with the specified image and selection state, but no text. */
  public DataRadioButtonMenuItem(Icon icon, boolean selected)
  {
    super(icon, selected);
    this.data = null;
  }

  /**
   * Creates a radio button menu item with the specified image, selection state and
   * user-defined data, but no text.
   */
  public DataRadioButtonMenuItem(Icon icon, boolean selected, E data)
  {
    super(icon, selected);
    this.data = data;
  }

  /** Creates a radio button menu item that has the specified text, image, and selection state. */
  public DataRadioButtonMenuItem(String text, Icon icon, boolean selected)
  {
    super(text, icon, selected);
    this.data = null;
  }

  /**
   * Creates a radio button menu item that has the specified text, image, selection state and
   * user-defined data.
   */
  public DataRadioButtonMenuItem(String text, Icon icon, boolean selected, E data)
  {
    super(text, icon, selected);
    this.data = data;
  }

  /** Returns the attached data object. */
  public E getData()
  {
    return data;
  }

  /** Assigns a new data object to the JRadioButtonMenuItem instance. */
  public void setData(E data)
  {
    this.data = data;
  }
}
