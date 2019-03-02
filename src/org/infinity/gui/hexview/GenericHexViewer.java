// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.hexview;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.nio.charset.Charset;

import javax.swing.JPanel;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.gui.StatusBar;
import org.infinity.resource.Closeable;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.Misc;

import tv.porst.jhexview.DataChangedEvent;
import tv.porst.jhexview.HexViewEvent;
import tv.porst.jhexview.IDataChangedListener;
import tv.porst.jhexview.IHexViewListener;
import tv.porst.jhexview.IMenuCreator;
import tv.porst.jhexview.JHexView;

/**
 * A generic hex viewer for all kinds of resources.
 */
public class GenericHexViewer extends JPanel implements IHexViewListener, Closeable, IDataChangedListener
{
  private static final String FMT_OFFSET = "%1$Xh (%1$d)";

  private final JHexView hexView;
  private final IMenuCreator menuCreator;
  private final VariableDataProvider dataProvider;

  private FindDataDialog findData;
  private boolean isModified;

  public GenericHexViewer()
  {
    this(new byte[0]);
  }

  public GenericHexViewer(ResourceEntry entry) throws Exception
  {
    this(entry.getResourceBuffer().array());
  }

  public GenericHexViewer(byte[] data)
  {
    super();

    if (data == null) {
      data = new byte[0];
    }
    this.hexView = new JHexView();
    this.dataProvider = new VariableDataProvider(data);
    this.dataProvider.addListener(this);
    this.menuCreator = new MenuCreator(this.hexView);
    this.isModified = false;

    initGui();
  }

//--------------------- Begin Interface IHexViewListener ---------------------

  @Override
  public void stateChanged(HexViewEvent event)
  {
    if (event.getSource() instanceof JHexView &&
        event.getCause() == HexViewEvent.Cause.SelectionChanged) {
      JHexView hv = (JHexView)event.getSource();
      int offset = (int)hv.getCurrentOffset();

      // updating statusbar
      updateStatusBar(offset);
    }
  }

//--------------------- End Interface IHexViewListener ---------------------

//--------------------- Begin Interface IDataChangedListener ---------------------

  @Override
  public void dataChanged(DataChangedEvent event)
  {
    if (event.getSource() == dataProvider) {
      setModified(true);
    }
  }

//--------------------- End Interface IDataChangedListener ---------------------

//--------------------- Begin Interface Closeable ---------------------

  @Override
  public void close() throws Exception
  {
    hexView.setVisible(false);
    hexView.dispose();

    if (findData != null) {
      findData.dispose();
      findData = null;
    }
  }

//--------------------- End Interface Closeable ---------------------

  @Override
  public boolean requestFocusInWindow()
  {
    return hexView.requestFocusInWindow();
  }

  /** Returns data as byte array. */
  public byte[] getData()
  {
    int len = Math.max(0, dataProvider.getDataLength());
    byte[] retVal;
    if (len > 0) {
      retVal = dataProvider.getData(0L, len);
    } else {
      retVal = new byte[0];
    }
    return retVal;
  }

  /**
   * Returns data as String with the specified character encoding.
   * Specify {@code null} to use a default ANSI charset.
   */
  public String getText(Charset cs)
  {
    if (cs == null) {
      cs = Misc.CHARSET_DEFAULT;
    }
    byte[] data = getData();
    return new String(data, cs);
  }

  /** Sets new data. Attempts to retain the current cursor position. */
  public void setData(byte[] data)
  {
    long ofs = hexView.getCurrentOffset();

    if (data != null) {
      dataProvider.setDataLength(data.length);
      dataProvider.setData(0L, data);
    } else {
      dataProvider.setDataLength(0);
      dataProvider.setData(0L, new byte[0]);
    }

    hexView.setCurrentOffset(Math.min(dataProvider.getDataLength(), ofs));
    hexView.repaint();
  }

  /**
   * Sets new data by converting the string into byte data using the specified charset.
   * @param text Text to set.
   * @param cs Character encoding of the text. Specify {@code null} to use a default ANSI charset.
   */
  public void setText(String text, Charset cs)
  {
    long ofs = hexView.getCurrentOffset();

    if (cs == null) {
      cs = Misc.CHARSET_DEFAULT;
    }

    if (text != null) {
      byte[] data = text.getBytes(cs);
      dataProvider.setDataLength(data.length);
      dataProvider.setData(0L, data);
    } else {
      dataProvider.setDataLength(0);
      dataProvider.setData(0L, new byte[0]);
    }

    hexView.setCurrentOffset(Math.min(dataProvider.getDataLength(), ofs));
    hexView.repaint();
  }

  /** Returns the offset at the current caret position. */
  public long getCurrentOffset()
  {
    return hexView.getCurrentOffset();
  }

  /** Sets the caret to a new offset. */
  public void setCurrentOffset(long offset)
  {
    hexView.setCurrentOffset(offset);
  }

  /** Returns whether data has been modified. */
  public boolean isModified()
  {
    return isModified;
  }

  public void clearModified()
  {
    setModified(false);
    hexView.clearModified();
  }

  /** Updates the offset information in NI's statusbar. */
  public void updateStatusBar()
  {
    updateStatusBar((int)hexView.getCurrentOffset());
  }

  public void addDataChangedListener(IDataChangedListener l)
  {
    if (l != null ) {
      dataProvider.addListener(l);
    }
  }

  public void removeDataChangedListener(IDataChangedListener l)
  {
    if (l != null) {
      dataProvider.removeListener(l);
    }
  }

  public void addHexViewListener(IHexViewListener l)
  {
    if (l != null) {
      hexView.addHexListener(l);
    }
  }

  public void removeHexViewListener(IHexViewListener l)
  {
    if (l != null) {
      hexView.removeHexListener(l);
    }
  }

  private void initGui()
  {
    setLayout(new BorderLayout());

    // configuring hexview
    configureHexView(hexView, dataProvider.isEditable());

    hexView.setMenuCreator(menuCreator);
    hexView.addHexListener(this);
    hexView.setData(dataProvider);
    hexView.setDefinitionStatus(hexView.getData().getDataLength() > 0 ?
        JHexView.DefinitionStatus.DEFINED : JHexView.DefinitionStatus.UNDEFINED);
    add(hexView, BorderLayout.CENTER);
  }

  private void setModified(boolean b)
  {
    isModified = b;
    if (!isModified) {
      hexView.clearModified();
    }
  }

  private void updateStatusBar(int offset)
  {
    StatusBar sb = NearInfinity.getInstance().getStatusBar();
    if (offset >= 0) {
      sb.setCursorText(String.format(FMT_OFFSET, offset));
    } else {
      sb.setCursorText("");
    }
  }
  /**
   * The auxiliary method for setup common parameters of the HEX editor: colors,
   * font and byte grid.
   *
   * @param hexView Editor for setup
   * @param isEditable if {@code true}, then editor use one colors for text,
   *        otherwise other. This colors shows editable status of the editor
   */
  public static void configureHexView(JHexView hexView, boolean isEditable)
  {
    final Color textColor = isEditable ? Color.BLACK : Color.GRAY;
    hexView.setEnabled(false);
    hexView.setDefinitionStatus(JHexView.DefinitionStatus.UNDEFINED);
    hexView.setAddressMode(JHexView.AddressMode.BIT32);
    hexView.setSeparatorsVisible(false);
    hexView.setBytesPerColumn(1);
    hexView.setBytesPerRow(16);
    hexView.setColumnSpacing(8);
    hexView.setMouseOverHighlighted(false);
    hexView.setShowModified(true);
    hexView.setCaretColor(Color.BLACK);
    hexView.setFontSize(Misc.getScaledValue(13));
    hexView.setHeaderFontStyle(Font.BOLD);
    hexView.setFontColorHeader(new Color(0x0000c0));
    hexView.setBackgroundColorOffsetView(hexView.getBackground());
    hexView.setFontColorOffsetView(hexView.getFontColorHeader());
    hexView.setBackgroundColorHexView(hexView.getBackground());
    hexView.setFontColorHexView1(textColor);
    hexView.setFontColorHexView2(textColor);
    hexView.setBackgroundColorAsciiView(hexView.getBackground());
    hexView.setFontColorAsciiView(textColor);
    hexView.setFontColorModified(Color.RED);
    hexView.setSelectionColor(new Color(0xc0c0c0));
    hexView.setColorMapEnabled(BrowserMenuBar.getInstance().getHexColorMapEnabled());
    hexView.setEnabled(true);
  }
}
