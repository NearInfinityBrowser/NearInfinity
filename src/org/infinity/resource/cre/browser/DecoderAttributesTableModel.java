// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.browser;

import java.awt.Component;
import java.util.Iterator;
import java.util.TreeMap;

import javax.swing.DefaultListSelectionModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.infinity.resource.cre.decoder.SpriteDecoder;
import org.infinity.resource.cre.decoder.util.DecoderAttribute;
import org.infinity.util.Misc;

/**
 * A table model that handles key/value pairs of animation attributes stored in {@code SpriteDecoder} instances.
 */
public class DecoderAttributesTableModel extends AbstractTableModel
{
  private final TreeMap<String, String> attributes = new TreeMap<>();

  private SpriteDecoder decoder;
  // working array for map keys to speed up lookup
  private String[] keys;

  public DecoderAttributesTableModel()
  {
    this(null);
  }

  public DecoderAttributesTableModel(SpriteDecoder decoder)
  {
    super();
    setDecoder(decoder);
  }

  /** Returns the currently assigned {@code SpriteDecoder}. */
  public SpriteDecoder getDecoder() { return decoder; }

  /**
   * Assigns a {@code SpriteDecoder} instance to the table model and updates the list of available
   * animation attributes.
   * @param decoder the {@code SpriteDecoder} instance
   */
  public void setDecoder(SpriteDecoder decoder)
  {
    if (this.decoder == null && decoder != null ||
        this.decoder != null && !this.decoder.equals(decoder)) {
      this.decoder = decoder;
      reload();
    }
  }

  /**
   * Discards the current list of animation attributes and loads a new list from the
   * defined {@code SpriteDecoder} instance.
   */
  public void reload()
  {
    int oldSize = attributes.size();
    attributes.clear();
    if (oldSize > 0) {
      fireTableRowsDeleted(0, oldSize - 1);
    }

    if (decoder != null) {
      // Special: add animation id to list of attributes
      attributes.put(Misc.prettifySymbol("animation_id"), String.format("0x%04x (%d)", decoder.getAnimationId(), decoder.getAnimationId()));

      for (final Iterator<DecoderAttribute> iter = decoder.getAttributeIterator(); iter.hasNext(); ) {
        DecoderAttribute att = iter.next();

        // skip selected attributes
        if (SpriteDecoder.KEY_ANIMATION_TYPE.equals(att)) {
          continue;
        }

        String key = "";
        String value = "";
        switch (att.getType()) {
          case BOOLEAN:
          {
            Boolean b = decoder.getAttribute(att);
            if (b != null) {
              key = att.getName();
              value = b.toString();
            }
            break;
          }
          case DECIMAL:
          {
            Double d = decoder.getAttribute(att);
            if (d != null) {
              key = att.getName();
              value = d.toString();
            }
            break;
          }
          case INT:
          {
            Integer n = decoder.getAttribute(att);
            if (n != null) {
              key = att.getName();
              value = n.toString();
            }
            break;
          }
          case STRING:
          {
            String s = decoder.getAttribute(att);
            if (s != null) {
              key = att.getName();
              value = s;
            }
            break;
          }
          default:
          {
            Object o = decoder.getAttribute(att);
            if (o != null) {
              key = att.getName();
              value = o.toString();
            }
          }
        }

        if (!key.isEmpty()) {
          attributes.put(Misc.prettifySymbol(key), value);
        }
      }
      keys = attributes.keySet().toArray(new String[attributes.keySet().size()]);
      fireTableRowsInserted(0, keys.length - 1);
    }
  }

  @Override
  public int getRowCount()
  {
    return attributes.size();
  }

  @Override
  public int getColumnCount()
  {
    return 2;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex)
  {
    if (rowIndex >= 0 && rowIndex < keys.length && columnIndex >= 0 && columnIndex < 2) {
      String key = keys[rowIndex];
      switch (columnIndex) {
        case 0:
          return key;
        case 1:
          return attributes.get(key);
      }
    }
    return "";
  }

//-------------------------- INNER CLASSES --------------------------

  public static class AttributesColumnModel extends DefaultTableColumnModel
  {
    public AttributesColumnModel()
    {
      super();
      TableColumn column1 = new TableColumn(0, 125, new DefaultTableCellRenderer(), null);
      column1.setHeaderValue("Name");
      column1.setMinWidth(16);
      addColumn(column1);
      TableColumn column2 = new TableColumn(1, 125, new DefaultTableCellRenderer(), null);
      column2.setHeaderValue("Value");
      column2.setMinWidth(16);
      addColumn(column2);
    }
  }

  public static class AttributesListSelectionModel extends DefaultListSelectionModel
  {
    public AttributesListSelectionModel()
    {
      super();
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
  }

  public static class AttributesHeaderRenderer implements TableCellRenderer
  {
    private final DefaultTableCellRenderer renderer;

    public AttributesHeaderRenderer(JTable table)
    {
      super();
      this.renderer = (DefaultTableCellRenderer)table.getTableHeader().getDefaultRenderer();
      this.renderer.setHorizontalAlignment(SwingConstants.LEADING);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                   int row, int column)
    {
      return renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
  }
}
