// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import org.infinity.NearInfinity;
import org.infinity.datatype.SpellProtType;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.util.IdsMap;
import org.infinity.util.IdsMapCache;
import org.infinity.util.IdsMapEntry;
import org.infinity.util.Misc;
import org.infinity.util.Table2da;
import org.infinity.util.Table2daCache;
import org.tinylog.Logger;

/**
 * Conversion tool for encoding or decoding {@code SPLPROT.2DA} filter definitions.
 */
public class SplProtFrame extends ChildFrame implements ActionListener, DocumentListener {
  private static final String SPLPROT_NAME = "SPLPROT.2DA";

  private SplProtStatControls statControls;

  private JButton encodeButton;

  private JTextField filterInput;
  private JButton decodeButton;

  private JButton openSplProtButton;
  private JButton copyClipboardButton;
  private JButton addToSplProtButton;

  private StatusBar statusBar;
  private Timer messageTimer;

  /**
   * Attempts to parse the specified splprot filter definition and returns it as list of {@link SplProtEntry} instances.
   *
   * @param filter Filter definition string.
   * @param includeHeader Specifies whether the filter definition header should be included in the returned list.
   * @return a {@link List} containing (optional) header, stat, value and relation entries.
   * @throws Exception if the parse operation failed.
   */
  public static List<SplProtEntry> decodeFilter(String filter, boolean includeHeader) throws Exception {
    if (filter == null) {
      return null;
    }

    List<SplProtEntry> retVal = new ArrayList<>();
    final String[] tokens = filter.split("\\s+");
    final int startIndex = (tokens.length > 3) ? 1 : 0;
    final int count = Math.min(4, tokens.length);
    for (int i = startIndex; i < count; i++) {
      String token = tokens[i].toLowerCase();
      final int radix = (token.contains("0x")) ? 16 : 10;
      token = token.replace("0x", "");
      SplProtEntry entry = null;
      try {
        entry = new SplProtEntry(Long.parseLong(token, radix));
      } catch (NumberFormatException e) {
        Logger.warn(e);
        if (i > startIndex) {
          entry = new SplProtEntry(0L);
        } else {
          throw new Exception("Unable to parse value at column " + (i + 1) + ": " + tokens[i]);
        }
      }
      retVal.add(entry);
    }

    while (retVal.size() < 3) {
      retVal.add(new SplProtEntry(0L));
    }

    if (includeHeader) {
      final String label;
      if (startIndex > 0) {
        // simply use existing header
        label = tokens[0];
      } else {
        // use next available row index if possible
        final Table2da table = Table2daCache.get(SPLPROT_NAME);
        if (table != null) {
          label = Long.toString(table.getRowCount());
        } else {
          label = "0";
        }
      }
      retVal.add(0, new SplProtEntry(SplProtEntry.INVALID, label));
    }

    return retVal;
  }

  /**
   * Performs a quick analysis of the specified text string and returns whether it can be potentially decoded into a
   * splprot definition.
   *
   * @param text an arbitrary text {@code String}.
   * @return {@code true} if the {@code text} parameter is a potential splprot definition, {@code false} otherwise.
   */
  private static boolean isValidFilter(String text) {
    boolean retVal = false;

    if (text != null) {
      final String[] items = text.split("\\s+");
      retVal = items.length > 0 && !items[0].isEmpty();
    }

    return retVal;
  }

  /**
   * Creates a splprot filter definition from the specified parameters.
   *
   * @param autoLabel Specify {@code true} to auto-generate a descriptive label for the filter definition. Specify
   *                    {@code false} to add a simple numeric index instead.
   * @param stat      stat or extended stat index.
   * @param value     {@code VALUE} parameter of the given stat.
   * @param relation  {@code RELATION} parameter of the given stat.
   * @return Filter definition string if successful, {@code null} otherwise.
   */
  public static String encodeFilter(boolean autoLabel, SplProtEntry statEntry, SplProtEntry valueEntry,
      SplProtEntry relationEntry) {
    // filter label
    final Table2da table = Table2daCache.get(SPLPROT_NAME);
    final int labelIndex = (table != null) ? table.getRowCount() : 0;
    final String label = autoLabel
        ? getAutoLabel(labelIndex, statEntry.longValue(), valueEntry.longValue(), relationEntry.longValue())
        : Integer.toString(labelIndex);

    // filter parameters
    final String stat = statEntry.toString(false, true);
    final String value = valueEntry.toString(false, true);
    final String relation = relationEntry.toString(false, true);

    final int tabSize = 8;
    String retVal = label;

    int space = tabSize - (retVal.length() % tabSize);
    retVal += Misc.generate(space, ' ') + stat;

    space = tabSize - (retVal.length() % tabSize);
    retVal += Misc.generate(space, ' ') + value;

    space = tabSize - (retVal.length() % tabSize);
    retVal += Misc.generate(space, ' ') + relation;

    return retVal;
  }

  /**
   * Returns a descriptive splprot entry label based on the given parameters.
   *
   * @param index    the SPLPROT.2DA row index.
   * @param stat     numeric stat value.
   * @param value    numeric value parameter.
   * @param relation numeric relation parameter.,
   * @return Descriptive label as string.
   */
  private static String getAutoLabel(int index, long stat, long value, long relation) {
    String label = Integer.toString(index) + "_";
    switch ((int)stat) {
      case 0x100: // Source is target
        label += "SOURCE";
        break;
      case 0x101: // Source is not target
        label += "!SOURCE";
        break;
      case 0x102: // Personal space
        label += "PERSONALSPACE" + getRelationDesc(relation) + getValueDesc(value);
        break;
      case 0x103: // Match entries
        label += "ENTRIES=(" + getValueDesc(value) + "||" + relation + ")";
        break;
      case 0x104: // Not match entries
        label += "ENTRIES!=(" + getValueDesc(value) + "||" + relation + ")";
        break;
      case 0x105: // Moral alignment match
        if (relation == 1) {
          label += "MORALALIGNMENT_MATCHESCASTER";
        } else if (relation == 5) {
          label += "!MORALALIGNMENT_MATCHESCASTER";
        } else {
          label += "MORALALIGNMENT" + getRelationDesc(relation) + getValueDesc(value);
        }
        break;
      case 0x107: // Time of day
        label += "TIMEOFDAY=" + getValueDesc(value) + "-" + relation;
        break;
      case 0x108: // Ethical alignment match
        if (relation == 1) {
          label += "ETHICALALIGNMENT_MATCHESCASTER";
        } else if (relation == 5) {
          label += "!ETHICALALIGNMENT_MATCHESCASTER";
        } else {
          label += "ETHICALALIGNMENT" + getRelationDesc(relation) + getValueDesc(value);
        }
        break;
      case 0x109: // Evasion check
        label += "EVASIONCHECK";
        break;
      case 0x113: // Source and target allies
        if (relation == 1) {
          label += "ALLIES";
        } else if (relation == 5) {
          label += "!ALLIES";
        } else {
          label += "ALLIES" + getRelationDesc(relation) + getValueDesc(value);
        }
        break;
      case 0x114: // Source and target enemies
        if (relation == 1) {
          label += "ENEMIES";
        } else if (relation == 5) {
          label += "!ENEMIES";
        } else {
          label += "ENEMIES" + getRelationDesc(relation) + getValueDesc(value);
        }
        break;
      case 0x115: // # summoned creatures
        label += "SUMMONEDNUM" + getRelationDesc(relation) + getValueDesc(value);
        break;
      case 0x116: // Chapter
        label += "CHAPTER" + getRelationDesc(relation) + getValueDesc(value);
        break;
      case 0x106: // AREATYPE.IDS
        label += "AREATPYE" + getRelationDesc(relation) + getIdsDesc(IdsFile.AREATYPE, value);
        break;
      case 0x10a: // EA.IDS
        label += "EA" + getRelationDesc(relation) + getIdsDesc(IdsFile.EA, value);
        break;
      case 0x10b: // GENERAL.IDS
        label += "GENERAL" + getRelationDesc(relation) + getIdsDesc(IdsFile.GENERAL, value);
        break;
      case 0x10c: // RACE.IDS
        label += "RACE" + getRelationDesc(relation) + getIdsDesc(IdsFile.RACE, value);
        break;
      case 0x10d: // CLASS.IDS
        label += "CLASS" + getRelationDesc(relation) + getIdsDesc(IdsFile.CLASS, value);
        break;
      case 0x10e: // SPECIFIC.IDS
        label += "SPECIFIC" + getRelationDesc(relation) + getIdsDesc(IdsFile.SPECIFIC, value);
        break;
      case 0x10f: // GENDER.IDS
        label += "GENDER" + getRelationDesc(relation) + getIdsDesc(IdsFile.GENDER, value);
        break;
      case 0x110: // ALIGN.IDS
        label += "ALIGNMENT" + getRelationDesc(relation) + getIdsDesc(IdsFile.ALIGN, value);
        break;
      case 0x111: // STATE.IDS
        label += "STATE" + getRelationDesc(relation) + getIdsDesc(IdsFile.STATE, value);
        break;
      case 0x112: // SPLSTATE.IDS
        label += "SPLSTATE" + getRelationDesc(relation) + getIdsDesc(IdsFile.SPLSTATE, value);
        break;
      default:  // regular stats
        label += (stat >= 0 && stat < 256) ? getStatDesc(stat) : "UNKNOWN_" + stat;
        label += getRelationDesc(relation) + getValueDesc(value);
        break;
    }

    return label;
  }

  /**
   * Returns a symbolic label from the specified IDS file if possible. Otherwise a numeric value is returned in decimal
   * or hexadecimal notation.
   */
  private static String getIdsDesc(IdsFile idsFile, long value) {
    if (value == -1L) {
      return "n";
    }

    final List<SplProtEntry> list = SplProtParameterControls.createIdsFileList(idsFile);
    final SplProtEntry match = list
        .stream()
        .filter(entry -> entry.longValue() == value)
        .findAny()
        .orElse(null);
    if (match != null) {
      return match.getLabel();
    } else {
      return idsFile.getFormattedValue(value);
    }
  }

  /** Returns a descriptive string of the specified STATS.IDS value. */
  private static String getStatDesc(long stat) {
    String symbol = Long.toString(stat);
    if (stat >= 0 && stat < 256) {
      final IdsMap map = IdsMapCache.get(IdsFile.STATS.getIdsFile());
      if (map != null) {
        final IdsMapEntry entry = map.get(stat);
        if (entry != null) {
          symbol = entry.getSymbol();
        }
      }
    }
    return "STAT(" + symbol + ")";
  }

  /** Returns a string representation of the specified value. Returns a special symbol for value={@code -1}. */
  private static String getValueDesc(long value) {
    return (value != -1L) ? Long.toString(value) : "n";
  }

  /** Returns a descriptive of the specified relation code. */
  private static String getRelationDesc(long relation) {
    final String[] relationDesc = {
        "<=", "=", "<", ">", ">=", "!=", "BITS<=", "BITS>=", "BITS=", "BITS!=", "BITS>", "BITS<"
    };
    if (relation >= 0 && relation < relationDesc.length) {
      return relationDesc[(int)relation];
    } else {
      return "??";
    }
  }

  public SplProtFrame() {
    super(SPLPROT_NAME + " filter converter");
    setIconImage(Icons.ICON_HISTORY_16.getIcon().getImage());
    init();
  }

  /** Reinitializes cached data and resets the current dialog state to the default. */
  public void reset() {
    getRelationControls().reset();
    getValueControls().reset();
    statControls.reset();
  }

  /** Returns the {@link SplProtStatControls} instance. */
  public SplProtStatControls getStatControls() {
    return statControls;
  }

  /** Returns the {@link SplProtValueControls} instance. */
  public SplProtValueControls getValueControls() {
    return (SplProtValueControls)statControls.getParameter(SplProtStatControls.ParameterType.VALUE, true);
  }

  /** Returns the {@link SplProtRelationControls} instance. */
  public SplProtRelationControls getRelationControls() {
    return (SplProtRelationControls)statControls.getParameter(SplProtStatControls.ParameterType.RELATION, true);
  }

  /** Displays a message in the status bar until for a duration based on the message length. */
  public void setMessage(String message) {
    final int delay = 2000 + (Objects.nonNull(message) ? message.length() * 100 : 0);
    setMessage(message, delay);
  }

  /**
   * Displays a message in the status bar for the specified amount of time.
   *
   * @param message Text message to display.
   * @param delayMs Delay, in milliseconds, until the message is cleared. Specify 0 or a negative value for permanent
   *                  display.
   */
  public void setMessage(String message, int delayMs) {
    if (messageTimer != null) {
      messageTimer.stop();
      messageTimer = null;
    }

    statusBar.setMessage(Objects.isNull(message) ? "" : message);

    if (delayMs > 0) {
      messageTimer = new Timer(delayMs, e -> {
        statusBar.setMessage("");
        if (messageTimer != null) {
          messageTimer.stop();
          messageTimer = null;
        }
      });
      messageTimer.start();
    }
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (e.getSource() == encodeButton) {
      performFilterEncode();
    } else if (e.getSource() == decodeButton) {
      performFilterDecode();
    } else if (e.getSource() == openSplProtButton) {
      final ResourceEntry entry = ResourceFactory.getResourceEntry(SPLPROT_NAME);
      if (entry != null) {
        new ViewFrame(this, ResourceFactory.getResource(entry));
      } else {
        // should never happen
        final String msg = "Resource does not exist: " + SPLPROT_NAME;
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        Logger.warn(msg);
      }
    } else if (e.getSource() == copyClipboardButton) {
      if (copyToClipboard(filterInput.getText())) {
        setMessage("Filter definition copied to the clipboard.");
      }
    } else if (e.getSource() == addToSplProtButton) {
      final int result = JOptionPane.showConfirmDialog(this, "Add filter definition to " + SPLPROT_NAME + "?", "Error",
          JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
      if (result == JOptionPane.YES_OPTION) {
        try {
          if (performAddToSplProt(filterInput.getText(), true)) {
            setMessage("Filter definition has been successfully added to " + SPLPROT_NAME);
          } else {
            setMessage("Operation cancelled.");
          }
        } catch (Exception ex) {
          Logger.error(ex);
          final String msg;
          if (ex.getMessage() != null && !ex.getMessage().isEmpty()) {
            msg = ex.getMessage();
          } else {
            msg = ex.getClass().getSimpleName();
          }
          JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  @Override
  public void insertUpdate(DocumentEvent e) {
    filterInputUpdated();
  }

  @Override
  public void removeUpdate(DocumentEvent e) {
    filterInputUpdated();
  }

  @Override
  public void changedUpdate(DocumentEvent e) {
    filterInputUpdated();
  }

  /** Updates various UI control states based on the current content of the filter input text field. */
  private void filterInputUpdated() {
    final boolean isValid = isValidFilter(filterInput.getText());
    copyClipboardButton.setEnabled(isValid);
    decodeButton.setEnabled(isValid);
    addToSplProtButton.setEnabled(isValid);
  }

  /**
   * Converts the current selection of stat, value and relation controls into a valid splprot filter definition string.
   */
  private void performFilterEncode() {
    final boolean autoLabel = statControls.getAutoCreateLabelBox().isSelected();

    final SplProtEntry statEntry = statControls.getSelectedEntry();

    SplProtParameterControls ctrl = statControls.getParameter(SplProtStatControls.ParameterType.VALUE, false);
    final SplProtEntry valueEntry;
    if (ctrl.isCustomValueEnabled() && ctrl.getCustomValueBox().isSelected()) {
      valueEntry = SplProtEntry.getCustomValue();
    } else {
      valueEntry = ctrl.getParameter();
    }

    ctrl = statControls.getParameter(SplProtStatControls.ParameterType.RELATION, false);
    final SplProtEntry relationEntry;
    if (ctrl.isCustomValueEnabled() && ctrl.getCustomValueBox().isSelected()) {
      relationEntry = SplProtEntry.getCustomValue();
    } else {
      relationEntry = ctrl.getParameter();
    }

    filterInput.setText(encodeFilter(autoLabel, statEntry, valueEntry, relationEntry));
  }

  /** Parses a filter code string and applies it to the stat, value and relation controls. */
  private void performFilterDecode() {
    try {
      final List<SplProtEntry> entries = decodeFilter(filterInput.getText(), false);
      statControls.getStatBox().setSelectedItem(entries.get(0));
      statControls.getParameter(SplProtStatControls.ParameterType.VALUE, false).setParameter(entries.get(1));
      statControls.getParameter(SplProtStatControls.ParameterType.RELATION, false).setParameter(entries.get(2));
      decodeButton.setEnabled(false);
    } catch (Exception e) {
      setMessage(e.getMessage());
    }
  }

  /**
   * Adds the specified filter definition to {@code SPLPROT.2DA}.
   *
   * @param text  Filter definition string.
   * @param align Specifies whether to realign {@code SPLPROT.2DA} table columns for improved readability.
   * @return {@code true} if the filter definition was added successfully, {@code false} if the operation was cancelled
   *         by the user.
   * @throws Exception thrown if the filter could not be added.
   */
  private boolean performAddToSplProt(String text, boolean align) throws Exception {
    Table2da splProtTable = Table2daCache.get(SPLPROT_NAME);
    if (splProtTable == null) {
      throw new Exception("Resource not found: " + SPLPROT_NAME);
    }

    // creating output line
    final List<SplProtEntry> list = decodeFilter(text, true);
    final int stat = list.get(1).intValue();

    // check if definition already exists
    final SplProtEntry statEntry = new SplProtEntry(stat, stat >= 0x100, 1);
    final SplProtEntry valueEntry = SplProtValueControls.getValueOf(stat, list.get(2).longValue());
    final SplProtEntry relationEntry = SplProtRelationControls.getRelationOf(stat, list.get(3).longValue());
    for (int row = 0, count = splProtTable.getRowCount(); row < count; row++) {
      final SplProtEntry refStatEntry = SplProtEntry.valueOf(splProtTable.get(row, 1), 0L);
      if (statEntry.equals(refStatEntry)) {
        final SplProtEntry refValueEntry = SplProtEntry.valueOf(splProtTable.get(row, 2), 0L);
        final SplProtEntry refRelationEntry = SplProtEntry.valueOf(splProtTable.get(row, 3), 0L);
        if (valueEntry.equals(refValueEntry) && relationEntry.equals(refRelationEntry)) {
          final String msg = "An identical filter definition exists at row index " + row + ".\nAdd new entry anyway?";
          final int choice = JOptionPane.showConfirmDialog(this, msg, "Question", JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE);
          if (choice != JOptionPane.YES_OPTION) {
            return false;
          }
        }
      }
    }

    // special handling: definition label
    String label = list.get(0).getLabel();
    long rowIndex = splProtTable.getRowCount();
    final Matcher m = Pattern.compile("^\\d+").matcher(label);
    if (m.find()) {
      try {
        final String match = m.group();
        long index = Long.parseLong(match);
        if (index != rowIndex) {
          final Object[] options = {"Adjust", "Keep", "Cancel"};
          final String msg =
              "Index of the filter label differs from the calculated row index (" + index + " != " + rowIndex + ")."
              + "\nAdjust row index or keep existing?";
          final int choice = JOptionPane.showOptionDialog(this, msg, "Question", JOptionPane.YES_NO_CANCEL_OPTION,
              JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
          switch (choice) {
            case JOptionPane.YES_OPTION:  // Adjust
              label = Long.toString(rowIndex) + label.substring(match.length());
              break;
            case JOptionPane.NO_OPTION:   // Keep
              break;
            default:  // Cancel
              return false;
          }
        }
      } catch (NumberFormatException ex) {
      }
    }

    final String line = label
        + "\t" + statEntry.toString(false, true)
        + "\t" + valueEntry.toString(false, true)
        + "\t" + relationEntry.toString(false, true)
        + "\r\n";

    // loading and expanding source table
    final String output;
    if (align) {
      output = PlainTextResource.alignTableColumnsUniform(splProtTable.assemble() + line);
    } else {
      output = splProtTable.assemble() + line;
    }

    // writing table to disk
    final Path outFile = ResourceFactory.getDefaultSavePath(splProtTable.getResourceEntry());
    Files.write(outFile, output.getBytes(Profile.getDefaultCharset()));
    Table2daCache.cacheInvalid(splProtTable.getResourceEntry());
    ResourceFactory.registerResource(outFile, false);
    SplProtParameterControls.resetSplProtTableList();
    statControls.applyStat(statControls.getSelectedEntry());

    return true;
  }

  /**
   * Copies the given text to the system clipboard.
   *
   * @param text Text string to copy.
   */
  private boolean copyToClipboard(String text) {
    if (text != null) {
      final Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
      final StringSelection sel = new StringSelection(text);
      clip.setContents(sel, null);
      return true;
    }
    return false;
  }

  /** Initializes the dialog UI. */
  private void init() {
    statusBar = new StatusBar(false, false);
    statusBar.setMessage("");

    statControls = new SplProtStatControls();

    // evening out default control widths
    final int defBoxWidth = statControls.getStatBox().getPreferredSize().width * 2 / 3;
    Dimension dim = new Dimension(statControls.getStatBox().getPreferredSize());
    dim.width = defBoxWidth;
    statControls.getStatBox().setPreferredSize(new Dimension(dim));
    dim = new Dimension(statControls.getStatBox().getMinimumSize());
    dim.width =  8;
    statControls.getStatBox().setMinimumSize(new Dimension(dim));

    dim = new Dimension(getValueControls().getParameterBox().getPreferredSize());
    dim.width = defBoxWidth;
    getValueControls().getParameterBox().setPreferredSize(new Dimension(dim));
    dim = new Dimension(getValueControls().getParameterBox().getMinimumSize());
    dim.width =  8;
    getValueControls().getParameterBox().setMinimumSize(new Dimension(dim));

    dim = new Dimension(getRelationControls().getParameterBox().getPreferredSize());
    dim.width = defBoxWidth;
    getRelationControls().getParameterBox().setPreferredSize(new Dimension(dim));
    dim = new Dimension(getRelationControls().getParameterBox().getMinimumSize());
    dim.width =  8;
    getRelationControls().getParameterBox().setMinimumSize(new Dimension(dim));

    encodeButton = new JButton("Encode");
    encodeButton.addActionListener(this);

    decodeButton = new JButton("Decode");
    decodeButton.setEnabled(false);
    decodeButton.addActionListener(this);

    openSplProtButton = new JButton("Open " + SPLPROT_NAME);
    openSplProtButton.setEnabled(ResourceFactory.resourceExists(SPLPROT_NAME));
    openSplProtButton.addActionListener(this);

    copyClipboardButton = new JButton("Copy to clipboard");
    copyClipboardButton.setEnabled(false);
    copyClipboardButton.addActionListener(this);

    addToSplProtButton = new JButton("Add to " + SPLPROT_NAME + "...");
    addToSplProtButton.setToolTipText("Adds the current filter definition to " + SPLPROT_NAME);
    addToSplProtButton.setEnabled(false);
    addToSplProtButton.addActionListener(this);

    filterInput = new JTextField();
    filterInput.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
    // force TAB and SHIFT+TAB as regular input
    filterInput.setFocusTraversalKeysEnabled(false);
    filterInput.getDocument().putProperty(PlainDocument.tabSizeAttribute, Integer.valueOf(4));
    filterInput.getDocument().addDocumentListener(this);
    filterInput.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        processKeyPressed(filterInput, e);
      }
    });

    final JLabel filterCodeLabel = new JLabel("Filter definition:");

    final GridBagConstraints gbc = new GridBagConstraints();

    final JPanel mainPanel = new JPanel(new GridBagLayout());
    // stat selection labels
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    mainPanel.add(statControls.getLabel(), gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 16, 0, 0), 0, 0);
    mainPanel.add(getRelationControls().getLabel(), gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 16, 0, 0), 0, 0);
    mainPanel.add(getValueControls().getLabel(), gbc);
    ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);

    // stat selection controls and encode button
    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(4, 0, 0, 0), 0, 0);
    mainPanel.add(statControls.getStatBox(), gbc);
    ViewerUtil.setGBC(gbc, 1, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(4, 16, 0, 0), 0, 0);
    mainPanel.add(getRelationControls().getParameterBox(), gbc);
    ViewerUtil.setGBC(gbc, 2, 1, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.BOTH,
        new Insets(4, 16, 0, 0), 0, 0);
    mainPanel.add(getValueControls().getParameterBox(), gbc);
    ViewerUtil.setGBC(gbc, 3, 1, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 8, 0, 0), 0, 0);
    mainPanel.add(encodeButton, gbc);

    // control-specific extra options
    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 0, 0, 0), 0, 0);
    mainPanel.add(statControls.getAutoCreateLabelBox(), gbc);
    ViewerUtil.setGBC(gbc, 1, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 16, 0, 0), 0, 0);
    mainPanel.add(getRelationControls().getCustomValueBox(), gbc);
    ViewerUtil.setGBC(gbc, 2, 2, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 16, 0, 0), 0, 0);
    mainPanel.add(getValueControls().getCustomValueBox(), gbc);
    ViewerUtil.setGBC(gbc, 3, 2, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(8, 8, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);

    // filter code label
    ViewerUtil.setGBC(gbc, 0, 3, 3, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(16, 0, 0, 0), 0, 0);
    mainPanel.add(filterCodeLabel, gbc);
    ViewerUtil.setGBC(gbc, 3, 3, 1, 1, 0.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.NONE,
        new Insets(16, 8, 0, 0), 0, 0);
    mainPanel.add(new JPanel(), gbc);

    // filter code output and decode button
    ViewerUtil.setGBC(gbc, 0, 4, 3, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 0, 0, 0), 0, 0);
    mainPanel.add(filterInput, gbc);
    ViewerUtil.setGBC(gbc, 3, 4, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(4, 8, 0, 0), 0, 0);
    mainPanel.add(decodeButton, gbc);

    // button panel
    final JPanel buttonPanel = new JPanel(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 0, 0, 0), 0, 0);
    buttonPanel.add(openSplProtButton, gbc);
    ViewerUtil.setGBC(gbc, 1, 0, 1, 1, 1.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    buttonPanel.add(new JPanel(), gbc);
    ViewerUtil.setGBC(gbc, 2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    buttonPanel.add(copyClipboardButton, gbc);
    ViewerUtil.setGBC(gbc, 3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.LINE_START, GridBagConstraints.NONE,
        new Insets(0, 8, 0, 0), 0, 0);
    buttonPanel.add(addToSplProtButton, gbc);

    final JPanel pane = (JPanel) getContentPane();
    pane.setLayout(new GridBagLayout());
    ViewerUtil.setGBC(gbc, 0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(8, 8, 0, 8), 0, 0);
    pane.add(mainPanel, gbc);
    ViewerUtil.setGBC(gbc, 0, 1, 1, 1, 1.0, 0.0, GridBagConstraints.FIRST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(16, 8, 8, 8), 0, 0);
    pane.add(buttonPanel, gbc);
    ViewerUtil.setGBC(gbc, 0, 2, 1, 1, 1.0, 1.0, GridBagConstraints.LAST_LINE_START, GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0), 0, 0);
    pane.add(statusBar, gbc);

    pack();
    dim = new Dimension(getSize());
    dim.width = dim.width / 2;
    setMinimumSize(dim);
    Center.center(this, NearInfinity.getInstance().getBounds());
  }

  /** Handles TAB key presses in the specified {@link JTextField} component. */
  private void processKeyPressed(JTextField textField, KeyEvent e) {
    if (textField == null || e == null) {
      return;
    }

    if (e.getKeyCode() != KeyEvent.VK_TAB) {
      return;
    }

    final int tabSize;
    final Object attr = filterInput.getDocument().getProperty(PlainDocument.tabSizeAttribute);
    if (attr instanceof Number) {
      tabSize = ((Number)attr).intValue();
    } else {
      tabSize = 8;
    }

    final Document doc = textField.getDocument();
    final boolean isShift = (e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0;
    int pos = textField.getCaretPosition();
    if (isShift) {
      // Shift+Tab
      try {
        boolean processed = false;
        if (pos > 0) {
          final char ch = doc.getText(pos - 1, 1).charAt(0);
          if (ch == '\t') {
            // remove tab character
            pos--;
            doc.remove(pos, 1);
            processed = true;
          }
        }
        if (!processed && pos >= tabSize) {
          final String s = doc.getText(pos - tabSize, tabSize);
          if (s.matches(" {" + tabSize + "}")) {
            // remove "tab size" space characters
            pos -= tabSize;
            doc.remove(pos, tabSize);
            processed = true;
          }
        }
      } catch (BadLocationException | IndexOutOfBoundsException e1) {
      }
    } else {
      // Tab
      try {
        doc.insertString(pos, "\t", null);
      } catch (BadLocationException e1) {
      }
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  /**
   * Specialized {@link DefaultComboBoxModel} that is initialized with SPLPROT.2DA entries.
   */
  private static class SplProtStatModel extends DefaultComboBoxModel<SplProtEntry> {
    /** Array of pseudo stat labels (starting at value 0x100) */
    private static final String[] STAT_LABELS = {
        "Source is target", "Source is not target", "Personal space", "Match entries", "Not match entries",
        "Moral alignment match", "AREATYPE.IDS", "Time of day", "Ethical alignment match", "Evasion check",
        "EA.IDS", "GENERAL.IDS", "RACE.IDS", "CLASS.IDS", "SPECIFIC.IDS", "GENDER.IDS", "ALIGN.IDS", "STATE.IDS",
        "SPLSTATE.IDS", "Source and target allies", "Source and target enemies", "# summoned creatures", "Chapter"
    };

    private final TreeMap<Integer, SplProtEntry> statsMap = new TreeMap<>();

    public SplProtStatModel() {
      reset();
    }

    /** Returns a mapping of numeric id -> {@link SplProtEntry} pairs. */
    @SuppressWarnings("unused")
    public SortedMap<Integer, SplProtEntry> getStatsMap() {
      return Collections.unmodifiableSortedMap(statsMap);
    }

    /** Reinitializes list of SplProt stat entries. */
    public void reset() {
      removeAllElements();

      statsMap.clear();
      // adding defined stats
      final IdsMap statsIds = IdsMapCache.get(IdsFile.STATS.getIdsFile());
      for (final IdsMapEntry entry : statsIds.getAllValues()) {
        final int value = (int)entry.getID();
        final String label = entry.getSymbol();
        statsMap.put(value, new SplProtEntry(value, label));
      }

      // adding undefined stats
      statsMap.computeIfAbsent(0, idx -> new SplProtEntry(idx, "CURHITPOINTS"));
      for (int i = 1; i < 256; i++) {
        statsMap.computeIfAbsent(i, idx -> new SplProtEntry(idx, "<UNDEFINED_" + idx + ">"));
      }

      // adding pseudo stats
      for (int i = 0; i < STAT_LABELS.length; i++) {
        final String label = "Special: " + STAT_LABELS[i];
        final int value = 0x100 + i;
        statsMap.put(value, new SplProtEntry(value, label, true, 1));
      }

      // populating model
      for (final SplProtEntry spe : statsMap.values()) {
        addElement(spe);
      }
    }
  }

  /**
   * Manages UI controls related to the splprot stat selection.
   */
  private static class SplProtStatControls implements ItemListener {
    /** Available stat parameters. */
    public enum ParameterType {
      VALUE, RELATION
    }

    private final JLabel statLabel = new JLabel("Stat:");
    private final SplProtStatModel statModel = new SplProtStatModel();
    private final JComboBox<SplProtEntry> statBox = new JComboBox<>(statModel);
    private final JCheckBox autoGenerateLabelBox = new JCheckBox("Auto-generate label", true);

    private final SplProtValueControls valuePanel = new SplProtValueControls(this);
    private final SplProtRelationControls relationPanel = new SplProtRelationControls(this);

    public SplProtStatControls() {
      init();
    }

    /** Returns the currently selected stat value as {@link SplProtEntry} instance. */
    public SplProtEntry getSelectedEntry() {
      SplProtEntry retVal = null;
      if (statBox.getSelectedIndex() >= 0) {
        retVal = statModel.getElementAt(statBox.getSelectedIndex());
      }
      return retVal;
    }

    /** Returns the {@link JLabel} instance associated with the list control. */
    public JLabel getLabel() {
      return statLabel;
    }

    /** Returns the {@link JComboBox} instance for the "stat entry" list control. */
    public JComboBox<SplProtEntry> getStatBox() {
      return statBox;
    }

    /** Returns the {@link SplProtStatModel} instance associated with the "stat entry" list control. */
    @SuppressWarnings("unused")
    public SplProtStatModel getStatModel() {
      return statModel;
    }

    /** Returns the {@link JCheckBox} instance for the "auto-generate label" control. */
    public JCheckBox getAutoCreateLabelBox() {
      return autoGenerateLabelBox;
    }

    /**
     * Returns the panel associated with the specified parameter type in logical or absolute order.
     *
     * @param type     {@link ParameterType} associated with the parameter panel.
     * @param absolute Specify {@code true} to get the panel directly associated with the parameter type. Specify
     *                   {@code false} to get the panel that is currently configured for the specified parameter type.
     * @return {@link SplProtParameterControls} associated with the specified type.
     */
    public SplProtParameterControls getParameter(ParameterType type, boolean absolute) {
      if (type != null) {
        final SplProtParameterControls vp, rp;
        if (!absolute && relationPanel.isCustomValueEnabled()) {
          vp = relationPanel;
          rp = valuePanel;
        } else {
          vp = valuePanel;
          rp = relationPanel;
        }

        switch (type) {
          case VALUE:
            return vp;
          case RELATION:
            return rp;
        }
      }
      return null;
    }

    /** Reinitializes the splprot list. */
    public void reset() {
      statModel.reset();
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
      if (e.getSource() == statBox) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          if (e.getItem() instanceof SplProtEntry) {
            applyStat((SplProtEntry)e.getItem());
          }
        }
      }
    }

    private void applyStat(SplProtEntry statEntry) {
      valuePanel.setStatEntry(statEntry);
      relationPanel.setStatEntry(statEntry);
    }

    private void init() {
      statBox.setEditable(false);
      statBox.addItemListener(this);
      applyStat(getSelectedEntry());
    }
  }

  /** List of IDS files supported by splprot definitions. */
  private enum IdsFile {
    /** AREATYPE.IDS */
    AREATYPE("AREATYPE.IDS", true, 4),
    /** EA.IDS */
    EA("EA.IDS", false, 1),
    /** GENERAL.IDS */
    GENERAL("GENERAL.IDS", false, 1),
    /** RACE.IDS */
    RACE("RACE.IDS", false, 1),
    /** CLASS.IDS */
    CLASS("CLASS.IDS", false, 1),
    /** SPECIFIC.IDS */
    SPECIFIC("SPECIFIC.IDS", false, 1),
    /** GENDER.IDS */
    GENDER("GENDER.IDS", false, 1),
    /** ALIGN.IDS */
    ALIGN("ALIGN.IDS", true, 2),
    /** STATE.IDS */
    STATE("STATE.IDS", true, 8),
    /** SPLSTATE.IDS */
    SPLSTATE("SPLSTATE.IDS", false, 1),
    /** STATS.IDS */
    STATS("STATS.IDS", false, 1)
    ;

    private final String idsFile;
    private final boolean showAsHex;
    private final int minDigits;
    IdsFile(String idsFile, boolean showAsHex, int minDigits) {
      this.idsFile = idsFile;
      this.showAsHex = showAsHex;
      this.minDigits = minDigits;
    }

    /** Returns the IDS filename. */
    public String getIdsFile() {
      return idsFile;
    }

    /** Returns whether the IDS value should be displayed in hexadecimal notation. */
    public boolean isShowAsHex() {
      return showAsHex;
    }

    /** Returns the minimum number of digits if the IDS value is displayed in hexadecimal notation. */
    public int getMinDigits() {
      return minDigits;
    }

    /**
     * Returns the specified value as a formatted string based on the IdsFile definition.
     *
     * @param value A numeric value.
     * @return Formatted {@code String} of the specified value.
     */
    public String getFormattedValue(long value) {
      if (showAsHex) {
        final String fmt = String.format("0x%%0%dx", Math.max(1, minDigits));
        return String.format(fmt, value);
      } else {
        return Long.toString(value);
      }
    }
  }

  /**
   * Specialization of the {@link BasicComboBoxEditor} with additional features.
   */
  private static class SplProtComboBoxEditor extends BasicComboBoxEditor {
    private boolean ignoreNextUpdate;

    public SplProtComboBoxEditor() {
      super();
    }

    @Override
    public JTextField getEditorComponent() {
      return editor;
    }

    @Override
    protected JTextField createEditorComponent() {
      final JTextField retVal = new JTextField("", 9);
      return retVal;
    }

    @Override
    public void setItem(Object anObject) {
      if (isNextUpdateIgnored()) {
        resetIgnoreNextUpdate();
      } else {
        super.setItem(anObject);
      }
    }

    @Override
    public void addActionListener(ActionListener l) {
      super.addActionListener(l);
    }

    @Override
    public void removeActionListener(ActionListener l) {
      super.removeActionListener(l);
    }

    /** Returns whether the next update of the combobox list selection should be ignored. */
    public synchronized boolean isNextUpdateIgnored() {
      return ignoreNextUpdate;
    }

    /** Instructs the editor to ignore the next update of the combobox list selection. */
    public synchronized void setIgnoreNextUpdate() {
      ignoreNextUpdate = true;
    }

    /** Instructs the editor to restore handling of combobox list selections. */
    public synchronized void resetIgnoreNextUpdate() {
      ignoreNextUpdate = false;
    }
  }

  /**
   * Specialized {@link JComboBox} for representing a splprot parameter.
   */
  private static class SplProtComboBox extends JComboBox<SplProtEntry> implements PopupMenuListener, DocumentListener {
    private final SplProtComboBoxEditor editor;

    private boolean quickSearch;

    /**
     * Creates an editable {@code SplProtComboBox} for {@link SplProtEntry} items with a default data model and enabled
     * quick search option.
     */
    public SplProtComboBox() {
      this(true);
    }

    /**
     * Creates an editable {@code SplProtComboBox} for {@link SplProtEntry} items with a default data model.
     *
     * @param quickSearch Specifies whether entered text in the editable input field should automatically select the
     *                      first match in the combobox list when the popup is visible.
     */
    public SplProtComboBox(boolean quickSearch) {
      super();
      this.editor = new SplProtComboBoxEditor();
      setEditor(editor);
      init();
      setQuickSearchEnabled(quickSearch);
    }

    @Override
    public DefaultComboBoxModel<SplProtEntry> getModel() {
      return (DefaultComboBoxModel<SplProtEntry>)super.getModel();
    }

    /**
     * Returns whether the quick search option is enabled.
     * <p>
     * With the quick search option enabled any entered text in the input field will automatically jump to the first
     * matching combobox list item if the popup is visible.
     * </p>
     */
    @SuppressWarnings("unused")
    public boolean isQuickSearchEnabled() {
      return quickSearch;
    }

    /**
     * Specifies whether the quick search option is enabled.
     *
     * @param b Specify {@code true} to auto-jump to the first matching combobox list item when the user enters text in
     *            the editable input field.
     */
    public void setQuickSearchEnabled(boolean b) {
      quickSearch = b;
    }

    @Override
    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
      editor.getEditorComponent().selectAll();
    }

    @Override
    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    @Override
    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      editorChanged();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      editorChanged();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      editorChanged();
    }

    /** Performs the quick jump operation if the popup is visible. */
    private void editorChanged() {
      if (!isPopupVisible()) {
        return;
      }

      final String text = editor.getEditorComponent().getText();
      final int itemIndex = findAnyItem(text, getSelectedIndex());
      if (itemIndex >= 0 && itemIndex != getSelectedIndex()) {
        // update must be invoked asynchronously to prevent freezes
        SwingUtilities.invokeLater(() -> {
          editor.setIgnoreNextUpdate();
          setSelectedIndex(itemIndex);
        });
      }
    }

    private int findAnyItem(String text, int startIndex) {
      int retVal = -1;
      if (text == null || text.isEmpty()) {
        return retVal;
      }

      startIndex = Math.max(0, Math.min(getModel().getSize() - 1, startIndex));
      text = text.toLowerCase();

      final DefaultComboBoxModel<SplProtEntry> model = getModel();
      for (int idx = startIndex, loop = 0, count = model.getSize();
           loop == 0 || idx < startIndex;
           idx = (idx + 1) % count) {
        if (idx == count - 1) {
          loop++;
        }

        final SplProtEntry entry = model.getElementAt(idx);
        if (entry.toString().toLowerCase().contains(text)) {
          retVal = idx;
          break;
        }
      }

      return retVal;
    }

    private void init() {
      setEditable(true);
      addPopupMenuListener(this);
      editor.getEditorComponent().getDocument().addDocumentListener(this);
    }
  }

  /**
   * Abstract class that implements common traits for the value and relation parameters of splprot stats.
   */
  private static abstract class SplProtParameterControls implements ActionListener {
    /** Cache for parameter lists. Key can be a unique identifier or IDS filename. */
    private static final HashMap<String, List<SplProtEntry>> LIST_CACHE = new HashMap<>();

    private final SplProtStatControls statControls;
    private final String defaultPrompt;
    private final JLabel label;
    private final SplProtComboBox parameterBox;
    private final JCheckBox customValueBox;

    private SplProtEntry statEntry;

    protected SplProtParameterControls(SplProtStatControls statControls, String defaultPrompt) {
      this.statControls = Objects.requireNonNull(statControls);
      this.defaultPrompt = Objects.requireNonNull(defaultPrompt);
      this.label = new JLabel(this.defaultPrompt);
      this.parameterBox = new SplProtComboBox();
      this.customValueBox = new JCheckBox("Custom value");
      this.customValueBox.setToolTipText("Use custom value from effect parameter.");
      this.customValueBox.addActionListener(this);
    }

    /** Returns the {@link SplProtStatControls} instance associated with this parameter. */
    @SuppressWarnings("unused")
    public SplProtStatControls getStatControls() {
      return statControls;
    }

    /**
     * Returns the currently selected stat entry.
     *
     * @return {@link SplProtEntry} of the currently selected stat.
     */
    public SplProtEntry getStatEntry() {
      return statEntry;
    }

    /**
     * Selects the specified stat entry and triggers an initialization request for the current parameter component.
     *
     * @param statEntry {@link SplProtEntry} instance of the new stat entry.
     */
    public void setStatEntry(SplProtEntry statEntry) {
      this.statEntry = statEntry;

      // try to preserve the old parameter value
      final Object oldItem = parameterBox.getSelectedItem();
      final SplProtEntry oldEntry;
      if (oldItem != null) {
        oldEntry = SplProtEntry.valueOf(oldItem.toString(), 0L);
      } else {
        oldEntry = new SplProtEntry(0);
      }

      statEntrySelected();

      // restore and adapt old parameter value
      final int idx = parameterBox.getModel().getIndexOf(oldEntry);
      if (idx >= 0) {
        parameterBox.setSelectedIndex(idx);
      } else {
        parameterBox.setSelectedItem(oldEntry.longValue());
      }
    }

    /** Returns the label text. */
    @SuppressWarnings("unused")
    public String getPrompt() {
      return label.getText();
    }

    /** Sets a new label text. Specify {@code null} to restore the default label text. */
    public void setPrompt(String newText) {
      label.setText(newText != null ? newText : defaultPrompt);
    }

    /**
     * Returns the current parameter definition as {@link SplProtEntry} instance. Custom strings in the editable input
     * field of the list control are converted into a {@code SplProtEntry} object if possible.
     *
     * @return {@link SplProtEntry} object of the current parameter value if successful, an invalid definition
     *         otherwise.
     * @see SplProtEntry#isValid()
     */
    public SplProtEntry getParameter() {
      SplProtEntry retVal = null;

      Object item = getParameterBox().getSelectedItem();
      if (item instanceof SplProtEntry) {
        retVal = (SplProtEntry)item;
      } else {
        try {
          retVal = SplProtEntry.valueOf(String.valueOf(item), 0L);

          // return the original list value if possible
          int idx = getParameterBox().getModel().getIndexOf(retVal);
          if (idx >= 0) {
            retVal = getParameterBox().getModel().getElementAt(idx);
          }
        } catch (Exception e) {
          retVal = new SplProtEntry();
        }
      }
      return retVal;
    }

    /**
     * Attempts to select a list item that matches specified item parameter. Selects a default item if no matching
     * list items were found.
     *
     * @param item Item of arbitrary type that is used to determine a parameter list item.
     */
    public void setParameter(Object item) {
      if (item == null) {
        item = "0";
      }

      final SplProtEntry entry;
      if (item instanceof SplProtEntry) {
        entry = (SplProtEntry)item;
      } else {
        entry = SplProtEntry.valueOf(item.toString(), 0L);
      }

      final int index = parameterBox.getModel().getIndexOf(entry);
      if (index >= 0) {
        parameterBox.setSelectedIndex(index);
      } else {
        parameterBox.setSelectedItem(entry);
      }
    }

    /** Returns the {@link JLabel} instance used by this panel. */
    public JLabel getLabel() {
      return label;
    }

    /** Returns the {@link SplProtComboBox} instance used by this panel. */
    public SplProtComboBox getParameterBox() {
      return parameterBox;
    }

    /** Returns the {@link JCheckBox} instance for the "custom value" control. */
    public JCheckBox getCustomValueBox() {
      return customValueBox;
    }

    /** Returns whether the "custom value" checkbox is currently visible for this class instance. */
    public boolean isCustomValueEnabled() {
      return customValueBox.isVisible();
    }

    /** Specify whether the "custom value" checkbox should be visible for this class instance. */
    public void setCustomValueEnabled(boolean b) {
      if (!b) {
        customValueBox.setSelected(false);
      }
      customValueBox.setVisible(b);
    }

    /**
     * Returns whether the current stat entry supports this parameter.
     *
     * @return {@code true} if the current stat entry supports the parameter, {@code false} otherwise.
     */
    public abstract boolean isParameterEnabled();

    /** Resets any cached data. */
    public void reset() {
      LIST_CACHE.clear();
      parameterBox.getModel().removeAllElements();
      statEntrySelected();
    }

    /**
     * This method is used to initialize the panel controls and called whenever a new stat entry is assigned to the
     * control. Override to expand functionality.
     */
    protected void statEntrySelected() {
      customValueBox.setSelected(false);
      parameterBox.setEnabled(isParameterEnabled());
      parameterBox.setSelectedIndex(-1);
      customValueBox.setEnabled(isParameterEnabled());
    }

    /**
     * Returns whether relative order of VALUE and RELATION entries are reversed.
     * This is done for a small number of special stats to improve user accessibility.
     */
    protected boolean isParameterOrderReversed() {
      if (statEntry == null) {
        return false;
      }

      switch (statEntry.intValue()) {
        case 0x103: // Match entries
        case 0x104: // Not match entries
        case 0x107: // Time of day
          return true;
        default:
          return false;
      }
    }

    /**
     * Populates the combobox model with the content of the specified list.
     *
     * @param items List of {@link SplProtEntry} instances to add to the combobox model.
     */
    protected void populateParameterBox(List<SplProtEntry> items) {
      if (items != null) {
        final DefaultComboBoxModel<SplProtEntry> model = parameterBox.getModel();
        for (final SplProtEntry entry : items) {
          if (entry != null) {
            model.addElement(entry);
          }
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource() == customValueBox) {
        parameterBox.setEnabled(!customValueBox.isSelected());
      }
    }

    /**
     * Creates a list with entries conforming to the specified parameters.
     *
     * @param items     Array of numeric values.
     * @param showAsHex Whether to display numeric value in hexadecimal notation.
     * @param minDigits Minimum number of digits to display if value is shown in hexadecimal notation.
     * @return List of {@link SplProtEntry} instances based on the specified parameters.
     */
    public static List<SplProtEntry> createList(Number[] items, boolean showAsHex, int minDigits) {
      if (items != null) {
        final String key = Integer.toHexString(items.hashCode());
        final List<SplProtEntry> list = LIST_CACHE.computeIfAbsent(key, s -> {
          final List<SplProtEntry> retVal = new ArrayList<>(items.length);
          for (final Number item : items) {
            retVal.add(new SplProtEntry(item.longValue(), showAsHex, minDigits));
          }
          return retVal;
        });
        return list;
      }
      return Collections.emptyList();
    }

    /**
     * Convenience method that creates a list with entries from the specified {@link IdsFile} enum.
     *
     * @param idsFile {@link IdsFile} enum with information about the IDS file to use.
     * @return List of {@link SplProtEntry} instances based on the specified parameters.
     */
    public static List<SplProtEntry> createIdsFileList(IdsFile idsFile) {
      if (idsFile != null) {
        return createIdsFileList(idsFile.getIdsFile(), idsFile.isShowAsHex(), idsFile.getMinDigits());
      }
      return Collections.emptyList();
    }

    /**
     * Creates a list with entries from the specified IDS file.
     *
     * @param idsFile   IDS filename to load entries from.
     * @param showAsHex Whether to display numeric value in hexadecimal notation.
     * @param minDigits Minimum number of digits to display if value is shown in hexadecimal notation.
     * @return List of {@link SplProtEntry} instances based on the specified parameters.
     */
    public static List<SplProtEntry> createIdsFileList(String idsFile, boolean showAsHex, int minDigits) {
      final IdsMap idsMap = IdsMapCache.get(idsFile);
      if (idsMap != null) {
        final String key = idsFile.toLowerCase();
        final List<SplProtEntry> list = LIST_CACHE.computeIfAbsent(key, s -> {
          final List<SplProtEntry> retVal = new ArrayList<>(idsMap.size());
          for (final long value : idsMap.getKeys()) {
            final IdsMapEntry entry = idsMap.get(value);
            if (entry != null) {
              retVal.add(new SplProtEntry(value, entry.getSymbol(), showAsHex, minDigits));
            }
          }
          return retVal;
        });
        return list;
      }
      return Collections.emptyList();
    }

    /**
     * Removes the content of the specified IDS file from the internal cache. Call this function if the content of the
     * IDS file has changed.
     *
     * @param idsFile {@link IdsFile} enum with information about the IDS file to reset.
     */
    @SuppressWarnings("unused")
    public static void resetIdsFileList(IdsFile idsFile) {
      if (idsFile != null) {
        final String key = idsFile.getIdsFile().toLowerCase();
        LIST_CACHE.remove(key);
        IdsMapCache.remove(ResourceFactory.getResourceEntry(idsFile.getIdsFile()));
      }
    }

    /**
     * Creates a list with human-readable entries from SPLPROT.2DA itself.
     *
     * @return List of {@link SplProtEntry} instances based on the specified parameters.
     */
    public static List<SplProtEntry> createSplProtTableList() {
      final String key = SPLPROT_NAME.toLowerCase();
      final List<SplProtEntry> list = LIST_CACHE.computeIfAbsent(key, s -> {
        final String[] types = SpellProtType.getTypeTable();
        final List<SplProtEntry> retVal = new ArrayList<>(types.length);
        for (int i = 0; i < types.length; i++) {
          retVal.add(new SplProtEntry(i, types[i], false, 1));
        }
        return retVal;
      });
      return list;
    }

    /**
     * Removes the content of {@code SPLPROT.2DA} from the internal cache. Call this function if the content of the file
     * has changed.
     */
    public static void resetSplProtTableList() {
      final String key = SPLPROT_NAME.toLowerCase();
      LIST_CACHE.remove(key);
      SpellProtType.resetTypeTable();
    }

    /**
     * Creates a list with a set of relational operators.
     *
     * @param equalOnly Specify {@code true} to add only "equal" and "not equal". Otherwise, all available relations are
     *                    added.
     * @return List of {@link SplProtEntry} instances based on the specified parameters.
     */
    public static List<SplProtEntry> createRelationList(boolean equalOnly) {
      final String key = equalOnly ? "equalrelations" : "allrelations";
      final List<SplProtEntry> list = LIST_CACHE.computeIfAbsent(key, s -> {
        final List<SplProtEntry> retVal = new ArrayList<>(equalOnly ? 2 : SpellProtType.RELATION_ARRAY.length);
        if (equalOnly) {
          retVal.add(new SplProtEntry(1, SpellProtType.RELATION_ARRAY[1]));
          retVal.add(new SplProtEntry(5, SpellProtType.RELATION_ARRAY[5]));
        } else {
          for (int i = 0; i < SpellProtType.RELATION_ARRAY.length; i++) {
            retVal.add(new SplProtEntry(i, SpellProtType.RELATION_ARRAY[i]));
          }
        }
        return retVal;
      });
      return list;
    }
  }

  /**
   * Specialization of {@link SplProtParameterControls} that manages the value parameter.
   */
  private static class SplProtValueControls extends SplProtParameterControls {
    /**
     * Returns a {@link SplProtEntry} instance for the specified stat and value parameters.
     *
     * @param stat  Splprot stat number.
     * @param value Value parameter.
     * @return {@link SplProtEntry} instance if match is found, {@code null} otherwise.
     */
    public static SplProtEntry getValueOf(int stat, long value) {
      SplProtEntry retVal = null;

      switch (stat) {
        case 0x100: // Source is target
        case 0x101: // Source is not target
        case 0x105: // Moral alignment match
        case 0x108: // Ethical alignment match
        case 0x109: // Evasion check
        case 0x113: // Source and target allies
        case 0x114: // Source and target enemies
          retVal = new SplProtEntry(SplProtEntry.INVALID);
          break;
        case 0x106: // AREATYPE.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.AREATYPE), value);
          break;
        case 0x10a: // EA.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.EA), value);
          break;
        case 0x10b: // GENERAL.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.GENERAL), value);
          break;
        case 0x10c: // RACE.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.RACE), value);
          break;
        case 0x10d: // CLASS.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.CLASS), value);
          break;
        case 0x10e: // SPECIFIC.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.SPECIFIC), value);
          break;
        case 0x10f: // GENDER.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.GENDER), value);
          break;
        case 0x110: // ALIGN.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.ALIGN), value);
          break;
        case 0x111: // STATE.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.STATE), value);
          break;
        case 0x112: // SPLSTATE.IDS
          retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.SPLSTATE), value);
          break;
        default:
          if (stat >= 0 && stat < 0x100) {
            retVal = findValueOf(SplProtParameterControls.createIdsFileList(IdsFile.STATS), value);
          } else {
            retVal = new SplProtEntry(value);
          }
      }

      return retVal;
    }

    /** Used internally to fetch a matching {@link SplProtEntry} from the given list. */
    private static SplProtEntry findValueOf(List<SplProtEntry> entryList, long value) {
      SplProtEntry retVal = null;

      if (entryList != null && !entryList.isEmpty()) {
        // shortcut: we only need the general format parameters
        retVal = entryList.get(0).derive(value, null);
      } else {
        retVal = new SplProtEntry(value);
      }

      return retVal;
    }

    public SplProtValueControls(SplProtStatControls statControls) {
      super(statControls, "Value:");
      init();
    }

    @Override
    public boolean isParameterEnabled() {
      if (getStatEntry() != null) {
        switch (getStatEntry().intValue()) {
          case 0x100: // Source is target
          case 0x101: // Source is not target
          case 0x105: // Moral alignment match
          case 0x108: // Ethical alignment match
          case 0x109: // Evasion check
          case 0x113: // Source and target allies
          case 0x114: // Source and target enemies
            return false;
        }
      }
      return true;
    }

    @Override
    protected void statEntrySelected() {
      super.statEntrySelected();
      setCustomValueEnabled(!isParameterOrderReversed());

      final SplProtComboBox cb = getParameterBox();
      cb.hidePopup();
      cb.getModel().removeAllElements();
      cb.setSelectedItem("0");
      setPrompt(null);

      if (getStatEntry() == null) {
        return;
      }

      switch (getStatEntry().intValue()) {
        case 0x100: // Source is target
        case 0x101: // Source is not target
        case 0x105: // Moral alignment match
        case 0x108: // Ethical alignment match
        case 0x109: // Evasion check
        case 0x113: // Source and target allies
        case 0x114: // Source and target enemies
          // value is ignored
          cb.setSelectedItem("");
          break;
        case 0x107: // Time of day
        {
          final Integer[] range = new Integer[24];
          Arrays.setAll(range, i -> i);
          populateParameterBox(createList(range, false, 1));
          setPrompt("To:");
          break;
        }
        case 0x102: // Personal space
        case 0x115: // # summoned creatures
        case 0x116: // Chapter
          // numeric value only
          break;
        case 0x103: // Match entries
        case 0x104: // Not match entries
          populateParameterBox(createSplProtTableList());
          setPrompt("Second entry:");
          break;
        case 0x106: // AREATYPE.IDS
          populateParameterBox(createIdsFileList(IdsFile.AREATYPE));
          break;
        case 0x10a: // EA.IDS
          populateParameterBox(createIdsFileList(IdsFile.EA));
          break;
        case 0x10b: // GENERAL.IDS
          populateParameterBox(createIdsFileList(IdsFile.GENERAL));
          break;
        case 0x10c: // RACE.IDS
          populateParameterBox(createIdsFileList(IdsFile.RACE));
          break;
        case 0x10d: // CLASS.IDS
          populateParameterBox(createIdsFileList(IdsFile.CLASS));
          break;
        case 0x10e: // SPECIFIC.IDS
          populateParameterBox(createIdsFileList(IdsFile.SPECIFIC));
          break;
        case 0x10f: // GENDER.IDS
          populateParameterBox(createIdsFileList(IdsFile.GENDER));
          break;
        case 0x110: // ALIGN.IDS
          populateParameterBox(createIdsFileList(IdsFile.ALIGN));
          break;
        case 0x111: // STATE.IDS
          populateParameterBox(createIdsFileList(IdsFile.STATE));
          break;
        case 0x112: // SPLSTATE.IDS
          populateParameterBox(createIdsFileList(IdsFile.SPLSTATE));
          break;
        default:    // any STATS.IDS value
          // numeric value only
          break;
      }

      if (cb.getModel().getSize() > 0) {
        cb.setSelectedIndex(0);
      }

      cb.setEnabled(isParameterEnabled());
      getCustomValueBox().setEnabled(isParameterEnabled());
    }

    private void init() {
    }
  }

  /**
   * Specialization of {@link SplProtParameterControls} that manages the relation parameter.
   */
  private static class SplProtRelationControls extends SplProtParameterControls {
    /**
     * Returns a {@link SplProtEntry} instance for the specified stat and relation parameters.
     *
     * @param stat     Splprot stat number.
     * @param relation Relation parameter.
     * @return {@link SplProtEntry} instance if match is found, {@code null} otherwise.
     */
    public static SplProtEntry getRelationOf(int stat, long relation) {
      SplProtEntry retVal = null;

      switch (stat) {
        case 0x100: // Source is target
        case 0x101: // Source is not target
        case 0x109: // Evasion check
          retVal = new SplProtEntry(SplProtEntry.INVALID);
          break;
        default:
          retVal = new SplProtEntry(relation);
      }

      return retVal;
    }

    public SplProtRelationControls(SplProtStatControls statControls) {
      super(statControls, "Relation:");
      init();
    }

    @Override
    public boolean isParameterEnabled() {
      if (getStatEntry() != null) {
        switch (getStatEntry().intValue()) {
          case 0x100: // Source is target
          case 0x101: // Source is not target
          case 0x109: // Evasion check
            return false;
        }
      }
      return true;
    }

    @Override
    protected void statEntrySelected() {
      super.statEntrySelected();
      setCustomValueEnabled(isParameterOrderReversed());

      final SplProtComboBox cb = getParameterBox();
      cb.hidePopup();
      cb.getModel().removeAllElements();
      cb.setSelectedItem("0");
      setPrompt(null);

      if (getStatEntry() == null) {
        return;
      }

      switch (getStatEntry().intValue()) {
        case 0x100: // Source is target
        case 0x101: // Source is not target
        case 0x109: // Evasion check
          // relation is ignored
          cb.setSelectedItem("");
          break;
        case 0x103: // Match entries
        case 0x104: // Not match entries
          populateParameterBox(createSplProtTableList());
          setPrompt("First entry:");
          break;
        case 0x107: // Time of day
        {
          // numeric value only
          final Integer[] range = new Integer[24];
          Arrays.setAll(range, i -> i);
          populateParameterBox(createList(range, false, 1));
          setPrompt("From:");
          break;
        }
        case 0x105: // Moral alignment match
        case 0x108: // Ethical alignment match
        case 0x113: // Source and target allies
        case 0x114: // Source and target enemies
          // provide only "equal" and "not equal"
          populateParameterBox(createRelationList(true));
          break;
        case 0x102: // Personal space
        case 0x106: // AREATYPE.IDS
        case 0x10a: // EA.IDS
        case 0x10b: // GENERAL.IDS
        case 0x10c: // RACE.IDS
        case 0x10d: // CLASS.IDS
        case 0x10e: // SPECIFIC.IDS
        case 0x10f: // GENDER.IDS
        case 0x110: // ALIGN.IDS
        case 0x111: // STATE.IDS
        case 0x112: // SPLSTATE.IDS
        case 0x115: // # summoned creatures
        case 0x116: // Chapter
        default:
          populateParameterBox(createRelationList(false));
          break;
      }

      if (cb.getModel().getSize() > 0) {
        cb.setSelectedIndex(0);
      }
    }

    private void init() {
    }
  }

  /**
   * Provides detailed information about a single splprot entry.
   */
  private static class SplProtEntry extends Number implements Comparable<SplProtEntry> {
    /** Magic number that represents an invalid or unused placeholder value. */
    private static final long INVALID = Long.MIN_VALUE;

    /** Placeholder value representation. */
    private static final String PLACEHOLDER = "*";

    /** Represents a custom value entry. */
    private static final SplProtEntry CUSTOM_VALUE = new SplProtEntry(-1L);

    private final String label;
    private final long value;

    private boolean showHexValue;
    private int minDigits;

    /** Returns the {@link SplProtEntry} instance that represents a custom VALUE entry. */
    public static SplProtEntry getCustomValue() {
      return CUSTOM_VALUE;
    }

    /**
     * Attempts to create a {@code SplProtEntry} object from the specified string.
     *
     * @param input    {@code String} with a potential splprot definition. Definition must contain a decimal or
     *                   hexadecimal number (can be wrapped in parentheses), and may contain an optional label string.
     * @param defValue Value to apply if the parsing was unsuccessful.
     * @return {@link SplProtEntry} entry with parsed input if successful, with invalid number otherwise.
     * @see #isValid()
     */
    public static SplProtEntry valueOf(String input, long defValue) {
      if (input == null) {
        throw new NullPointerException("Argument is null");
      }

      long value = defValue;
      String label = null;
      boolean showAsHex = false;

      // defining a regular expression with negative lookahead to match only the last occurrence in a string
      final String regex = "[+-]?(0x[0-9a-f]+|[1-9]\\d*|0)";
      final Pattern pattern = Pattern.compile(regex + "(?!.*(" + regex + "))", Pattern.CASE_INSENSITIVE);

      // parsing value
      final Matcher m = pattern.matcher(input);
      if (m.find()) {
        String number = m.group();
        showAsHex = number.toLowerCase().contains("0x");
        final int radix = showAsHex ? 16 : 10;
        number = number.replaceAll("0[xX]", "");
        try {
          value = Long.parseLong(number, radix);

          // removing number from input string
          int pos1 = m.start();
          while (pos1 > 0 && input.charAt(pos1 - 1) == '(') {
            pos1--;
          }
          int pos2 = m.end();
          while (pos2 < input.length() && input.charAt(pos2) == ')') {
            pos2++;
          }
          input = input.substring(0, pos1) + input.substring(pos2);
        } catch (NumberFormatException e) {
        }
      }

      // parsing label
      if (value != defValue) {
        label = input.trim();
      }

      return new SplProtEntry(value, label, showAsHex, 1);
    }

    /** Initializes a splprot entry that represents an invalid or unused placeholder value. */
    public SplProtEntry() {
      this(INVALID, null, false, 1);
    }

    /**
     * Initializes a new splprot entry without descriptive label.
     *
     * @param value Numeric value associated with the entry.
     */
    public SplProtEntry(long value) {
      this(value, null, false, 1);
    }

    /**
     * Initializes a new splprot entry without descriptive label.
     *
     * @param value        Numeric value associated with the entry.
     * @param showHexValue Specifies whether the {@code value} should be represented in hexadecimal notation.
     * @param minDigits    Specifies the min. number of digits for the {@code value} if displayed in hexadecimal
     *                       notation.
     */
    public SplProtEntry(long value, boolean showHexValue, int minDigits) {
      this(value, null, showHexValue, minDigits);
    }

    /**
     * Initializes a new splprot entry.
     *
     * @param value Numeric value associated with the entry.
     * @param label Descriptive name of the entry.
     */
    public SplProtEntry(long value, String label) {
      this(value, label, false, 1);
    }

    /**
     * Initializes a new splprot entry.
     *
     * @param value        Numeric value associated with the entry.
     * @param label        Descriptive name of the entry.
     * @param showHexValue Specifies whether the {@code value} should be represented in hexadecimal notation.
     * @param minDigits    Specifies the min. number of digits for the {@code value} if displayed in hexadecimal
     *                       notation.
     */
    public SplProtEntry(long value, String label, boolean showHexValue, int minDigits) {
      this.value = value;
      this.label = (label != null) ? label : "";
      setShowHexValue(showHexValue);
      setMinDigits(minDigits);
    }

    /** Returns the descriptive name of the splprot entry. Returns empty string if unavailable. */
    public String getLabel() {
      return label;
    }

    /** Returns whether the associated {@code value} entry should be represented in hexadecimal notation. */
    public boolean isShowHexValue() {
      return showHexValue;
    }

    /** Specifies whether the associated {@code value} entry should be represented in hexadecimal notation. */
    public void setShowHexValue(boolean b) {
      showHexValue = b;
    }

    /**
     * Returns the min. number of digits of the associated {@code value} entry if {@link #isShowHexValue()} returns
     * {@code true}.
     */
    public int getMinDigits() {
      return minDigits;
    }

    /**
     * Specifies the min. number of digits of the associated {@code value} entry if {@link #isShowHexValue()} returns
     * {@code true}.
     */
    public void setMinDigits(int v) {
      minDigits = Math.max(1, Math.min(16, v));
    }

    /**
     * Returns whether the numeric value represents a valid number.
     * <p>
     * An invalid number is represented as an asterisk ({@code *}) by the {@link #toString()} method.
     * </p>
     *
     * @return {@code true} if the entry represents a valid number, {@code false} otherwise.
     */
    public boolean isValid() {
      return value != INVALID;
    }

    /**
     * Returns a new {@link SplProtEntry} instance with the specified numeric value. Everything else is inherited
     * from the parent object.
     */
    @SuppressWarnings("unused")
    public SplProtEntry derive(long value) {
      return new SplProtEntry(value, getLabel(), isShowHexValue(), getMinDigits());
    }

    /**
     * Returns a new {@link SplProtEntry} instance with the specified label. Everything else is inherited
     * from the parent object.
     */
    @SuppressWarnings("unused")
    public SplProtEntry derive(String label) {
      return new SplProtEntry(longValue(), label, isShowHexValue(), getMinDigits());
    }

    /**
     * Returns a new {@link SplProtEntry} instance with the specified numeric value and label . Everything else is
     * inherited from the parent object.
     */
    public SplProtEntry derive(long value, String label) {
      return new SplProtEntry(value, label, isShowHexValue(), getMinDigits());
    }

    /** Returns the numeric value of the splprot entry as an {@code int}, which may involve loss of precision. */
    @Override
    public int intValue() {
      return (int) value;
    }

    /** Returns the numeric value of the splprot entry as a {@code long}. */
    @Override
    public long longValue() {
      return value;
    }

    /** Returns the numeric value of the splprot entry as a {@code float}. */
    @Override
    public float floatValue() {
      return value;
    }

    /** Returns the numeric value of the splprot entry as a {@code double}. */
    @Override
    public double doubleValue() {
      return value;
    }

    @Override
    public int compareTo(SplProtEntry o) {
      return (int)(value - o.value);
    }

    @Override
    public int hashCode() {
      return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      SplProtEntry other = (SplProtEntry)obj;
      return value == other.value;
    }

    @Override
    public String toString() {
      return toString(true, true);
    }

    /**
     * Returns a string representation of this object.
     *
     * @param showLabel Defines whether the entry label should be printed.
     * @param showValue Defines whether the numeric entry value should be printed. Number is shown in parentheses if
     *                    {@code showLabel} is {@code true}.
     * @return A formatted string representation based on the specified parameters.
     */
    public String toString(boolean showLabel, boolean showValue) {
      if (!isValid()) {
        return PLACEHOLDER;
      }

      String retVal = "";
      if (showLabel) {
        retVal = getLabel();
      }

      if (showValue) {
        String s;
        if (isShowHexValue()) {
          final String fmt = String.format("0x%%0%dx", getMinDigits());
          s = String.format(fmt, longValue());
        } else {
          s = Long.toString(longValue());
        }

        if (!retVal.isEmpty()) {
          retVal += " (" + s + ")";
        } else {
          retVal = s;
        }
      }

      return retVal;
    }
  }
}
