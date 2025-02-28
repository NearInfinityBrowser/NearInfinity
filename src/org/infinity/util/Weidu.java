// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.infinity.NearInfinity;
import org.infinity.gui.Center;
import org.infinity.gui.ChildFrame;
import org.infinity.gui.SortableTable;
import org.infinity.gui.TableItem;
import org.infinity.gui.ViewFrame;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.icon.Icons;
import org.infinity.resource.Profile;
import org.infinity.resource.Resource;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.key.FileResourceEntry;
import org.infinity.resource.key.ResourceEntry;
import org.infinity.util.io.FileManager;
import org.infinity.util.tuples.Couple;

/**
 * This class provides methods for performing various WeiDU-related operations.
 */
public class Weidu {
  /** Name of the WeiDU tool (without any file extension). */
  public static final String WEIDU_NAME = "weidu";

  /** Name of the platform-specific WeiDU executable. */
  public static final String WEIDU_BIN = WEIDU_NAME + Platform.EXECUTABLE_EXT;

  /**
   * Returns the absolute path of the WeiDU binary.
   * <p>
   * The method looks for the binary in
   * <ol>
   * <li>the path specified in the Preferences</li>
   * <li>the system {@code PATH}</li>
   * <li>the root path of the game</li>
   * <li>the three top-most folders of <code>${game_root}/weidu_external/tools/weidu/${platform}/${arch}</code></li>
   * </ol>
   * </p>
   *
   * @return absolute {@link Path} of the WeiDU binary if available, {@code null} otherwise.
   */
  public static Path getWeiduPath() {
    Path path = null;

    // 1. try Preferences option
    if (BrowserMenuBar.getInstance() != null && BrowserMenuBar.getInstance().getOptions() != null) {
      path = BrowserMenuBar.getInstance().getOptions().getWeiduPath();
    }

    if (path == null) {
      // 2. try system PATH
      path = Platform.resolveSystemPath(WEIDU_BIN);
    }

    if (path == null) {
      // 3. try current game root
      final Path gameRoot = Profile.getGameRoot();
      if (gameRoot != null) {
        try {
          path = gameRoot.resolve(WEIDU_BIN);
          if (!Files.isRegularFile(path)) {
            path = null;
          }
        } catch (InvalidPathException e) {
          path = null;
          Logger.debug(e);
        }
      }
    }

    if (path == null) {
      // 4. try ${game_root}/weidu_external/tools/weidu/${os}/${arch}
      Path rootPath = Profile.getGameRoot();
      if (rootPath != null) {
        try {
          rootPath = rootPath.resolve("weidu_external/tools/weidu");

          switch (Platform.OS.getCurrentOS()) {
            case MAC_OS:
              rootPath = rootPath.resolve("osx");
              break;
            case UNIX:
              rootPath = rootPath.resolve("unix");
              break;
            default:
              rootPath = rootPath.resolve("win32");
          }

          if (Platform.Arch.getCurrentArchitecture() == Platform.Arch.X86_32) {
            rootPath = rootPath.resolve("x86");
          } else {
            rootPath = rootPath.resolve("x86_64");
          }

          // checking weidu binary in the three top-most folders
          int maxPasses = 3;
          for (int pass = 0; pass < maxPasses; pass++) {
            path = rootPath.resolve(WEIDU_BIN);
            if (Files.isRegularFile(path)) {
              break;
            }
            path = null;

            if (pass == 0 &&
                Platform.OS.getCurrentOS() == Platform.OS.WINDOWS &&
                Platform.Arch.getCurrentArchitecture() != Platform.Arch.X86_32) {
              // special: also try 32-bit WeiDU binary on Windows
              rootPath = rootPath.getParent().resolve("x86");
              maxPasses++;
            } else {
              rootPath = rootPath.getParent();
            }
          }
        } catch (InvalidPathException e) {
          path = null;
          Logger.debug(e);
        }
      }
    }

    if (path != null && !path.isAbsolute()) {
      try {
        path = path.toAbsolutePath();
      } catch (IOError e) {
        Logger.debug(e);
      }
    }

    return path;
  }

  /**
   * Returns information about the availability of the WeiDU changelog operation.
   *
   * @return {@link Couple} instance with availability state and a message string if availability is {@code false}.
   */
  public static Couple<Boolean, String> isChangelogAvailable() {
    final Couple<Boolean, String> retVal = new Couple<>(true, null);

    final boolean weiduExists = (Weidu.getWeiduPath() != null);
    final boolean logExists = (FileManager.queryExisting(Profile.getGameRoot(), "weidu.log") != null);
    retVal.setValue0(weiduExists && logExists);
    if (!weiduExists) {
      retVal.setValue1("WeiDU binary not available.");
    } else if (!logExists) {
      retVal.setValue1("WeiDU.log does not exist.");
    }

    return retVal;
  }

  public static boolean performChangelog(ResourceEntry entry) throws Exception {
    if (entry == null) {
      return false;
    }

    // preparations
    final String changelogPrefix = "changelog-";
    final String changelogSuffix = ".txt";

    final Path weiduBin = getWeiduPath();
    if (weiduBin == null) {
      return false;
    }

    // ensure binary is executable
    if (!Platform.IS_WINDOWS && !Files.isExecutable(weiduBin)) {
      try {
        Files.setPosixFilePermissions(weiduBin, PosixFilePermissions.fromString("rwxr-xr-x"));
      } catch (UnsupportedOperationException e) {
        Logger.debug(e);
      }
    }

    final Path tempDir = Platform.createTempDirectory("ni-" + Profile.getSessionId());
    final Path gameRoot = Profile.getGameRoot();

    final String resFile = entry.getResourceName().toLowerCase(Locale.ROOT);
    final String resFileUpper = resFile.toUpperCase(Locale.ROOT);
    final String resName = entry.getResourceRef().toLowerCase(Locale.ROOT);
    final String resExt = entry.getExtension().toLowerCase(Locale.ROOT);

    final String changelogName = changelogPrefix + resFile + changelogSuffix;

    // cleaning up data from a previous changelog operation
    removeFiles(tempDir, Pattern.quote(resName) + "\\.\\d+\\." + Pattern.quote(resExt), Pattern.quote(changelogName));

    // performing WeiDU changelog operation
    final List<String> command = new ArrayList<>();
    command.add(weiduBin.toString());
    command.add("--change-log");
    command.add(resFile);
    command.add("--out");
    command.add(tempDir.toString());
    command.add("--out");
    command.add(tempDir.resolve(changelogName).toString());

    final ProcessBuilder pb = new ProcessBuilder(command);
    pb.directory(gameRoot.toFile());
    pb.redirectOutput(Platform.NULL_FILE);  // discard output
    final Process p = pb.start();
    boolean terminated = false;
    try {
      terminated = p.waitFor(10L, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Logger.warn(e);
    }

    if (!terminated && p.isAlive()) {
      p.destroyForcibly();
      return false;
    }

    if (p.exitValue() != 0) {
      return false;
    }

    // parsing and presenting results
    final Path changelogFile = FileManager.queryExisting(tempDir, changelogName);
    if (changelogFile != null) {
      FileDeletionHook.getInstance().registerFile(changelogFile);
    } else {
      return false;
    }

    final List<ChangeLogEntry> resultList = parseChangeLog(resFile, changelogFile);
    if (resultList.isEmpty()) {
      JOptionPane.showMessageDialog(NearInfinity.getInstance(), "No changelog entries found.", "Information",
          JOptionPane.INFORMATION_MESSAGE);
      return false;
    }

    final JButton openButton = new JButton("Open in new window", Icons.ICON_OPEN_16.getIcon());
    openButton.setMnemonic('o');
    openButton.setEnabled(false);

    final JButton saveButton = new JButton("Save...", Icons.ICON_SAVE_16.getIcon());
    saveButton.setMnemonic('s');

    final JLabel countLabel = new JLabel(resultList.size() + (resultList.size() == 1 ? " entry" : " entries") + " found");

    final ChildFrame resultFrame = new ChildFrame("Mods affecting " + resFileUpper, true);
    resultFrame.setIconImage(Icons.ICON_REFRESH_16.getIcon().getImage());
    resultFrame.getRootPane().setDefaultButton(openButton);

    final String[] columns = { "Index", "Tp2 file", "Language id", "Component id", "Component name", "Comment" };
    final Class<?>[] classes = { DataNumber.class, String.class, Integer.class, Integer.class, String.class, String.class };
    final Integer[] columnWidths = { 40, 250, 40, 40, 350, 150 };
    final SortableTable table = new SortableTable(columns, classes, columnWidths);
    for (final ChangeLogEntry changeLogEntry : resultList) {
      table.addTableItem(new ChangeLogTableItem(changeLogEntry));
    }
    table.tableComplete();
    table.setFont(Misc.getScaledFont(BrowserMenuBar.getInstance().getOptions().getScriptFont()));
    table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 1);

    // returns whether the specified table row is associated with a resource
    final Predicate<Integer> predCanOpenEntry = index -> {
      if (index >= 0) {
        final Object o = table.getModel().getValueAt(index, 0);
        return (o instanceof DataNumber<?>) && ((DataNumber<?>)o).hasData();
      }
      return false;
    };

    // opens the resource associated with the specified table row
    final Predicate<Integer> predOpenEntry = rowIndex -> {
      if (rowIndex >= 0) {
        showResourceInViewer(resultFrame, table.getModel().getValueAt(rowIndex, 0));
        return true;
      }
      return false;
    };

    table.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        openButton.setEnabled(predCanOpenEntry.test(table.getSelectedRow()));
      }
    });
    table.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseReleased(MouseEvent e) {
        if (e.getClickCount() == 2) {
          predOpenEntry.test(table.getSelectedRow());
        }
      }
    });

    openButton.addActionListener(e -> predOpenEntry.test(table.getSelectedRow()));
    saveButton.addActionListener(e -> table.saveCheckResult(resultFrame, "Changelog results for " + resFileUpper));

    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add(openButton);
    buttonPanel.add(saveButton);

    final JScrollPane scrollTable = new JScrollPane(table);
    scrollTable.getViewport().setBackground(table.getBackground());

    final JComponent contentPane = (JComponent)resultFrame.getContentPane();
    contentPane.setLayout(new BorderLayout(0, 3));
    contentPane.add(countLabel, BorderLayout.NORTH);
    contentPane.add(scrollTable, BorderLayout.CENTER);
    contentPane.add(buttonPanel, BorderLayout.SOUTH);
    contentPane.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
    resultFrame.setSize(1000, 500);
    Center.center(resultFrame, NearInfinity.getInstance().getBounds());
    resultFrame.setVisible(true);

    return true;
  }

  /**
   * Parses the content of a changelog text file and returns a list of all identified changelog entries.
   *
   * @param resourceName  Name of the game resource this changelog is based on.
   * @param changeLogFile {@link Path} of the text file containing changelog definitions.
   * @return {@link List} of {@link ChangeLogEntry} instances of the parsed changelog definition.
   * @throws Exception if one or more changelog definitions could not be parsed.
   */
  private static List<ChangeLogEntry> parseChangeLog(String resourceName, Path changeLogFile) throws Exception {
    if (resourceName == null) {
      throw new NullPointerException("Resource name is null");
    }
    if (changeLogFile == null) {
      throw new NullPointerException("Changelog file path is null");
    }

    final List<ChangeLogEntry> retVal = new ArrayList<>();
    final Path changeLogDir = changeLogFile.getParent();
    final List<String> lines = Files.readAllLines(changeLogFile, StandardCharsets.UTF_8);
    for (final String line : lines) {
      try {
        final ChangeLogEntry entry = ChangeLogEntry.parse(line, resourceName, changeLogDir);
        retVal.add(entry);
      } catch (IllegalArgumentException | NullPointerException e) {
        Logger.trace(e);
        continue;
      }
    }

    return retVal;
  }

  /**
   * Attempts to open the specified data object in a viewer.
   *
   * @param viewer {@link ChildFrame} instance of the viewer.
   * @param data   The resource to view. The following data types are supported: {@code ResourceEntry},
   *                 {@code DataNumber}, {@code Path}, {@code File} and {@code String}.
   * @return {@code true} if {@code data} could be successfully opened in a viewer, {@code false} otherwise.
   */
  private static boolean showResourceInViewer(ChildFrame viewer, Object data) {
    if (viewer == null || data == null) {
      return false;
    }

    ResourceEntry entry = null;
    if (data instanceof ResourceEntry) {
      entry = (ResourceEntry)data;
    } else if (data instanceof DataNumber<?>) {
      final DataNumber<?> number = (DataNumber<?>)data;
      if (number.getData() instanceof ResourceEntry) {
        entry = (ResourceEntry)number.getData();
      } else if (data instanceof Path) {
        final Path path = (Path)data;
        if (Files.isRegularFile(path)) {
          entry = new FileResourceEntry(path);
        }
      } else if (data instanceof File) {
        final File path = (File)data;
        if (path.isFile()) {
          entry = new FileResourceEntry(path.toPath());
        }
      } else {
        try {
          final Path path = Paths.get(data.toString());
          if (Files.isRegularFile(path)) {
            entry = new FileResourceEntry(path);
          }
        } catch (InvalidPathException e) {
          Logger.trace(e);
        }
      }
    }

    if (entry != null) {
      try {
        final Resource res = ResourceFactory.getResource(entry);
        new ViewFrame(viewer, res);
        return true;
      } catch (Exception e) {
        Logger.debug(e);
      }
    }

    return false;
  }

  /**
   * Removes all files in the specified directory that match the given pattern. Removed files are also unregistered in
   * the local {@link FileDeletionHook} instance.
   *
   * @param dir      {@link Path} object to a directory containing potential files to remove.
   * @param patterns one or more regular expressions for matching filenames to delete.
   * @return Number of deleted files.
   * @throws IOException            if the file could not be deleted.
   * @throws PatternSyntaxException if the filename pattern is not a valid regular expression.
   */
  private static int removeFiles(Path dir, String... patterns) throws IOException, PatternSyntaxException {
    int retVal = 0;

    if (dir == null || !Files.isDirectory(dir)) {
      return retVal;
    }

    // preparing pattern list
    final List<Pattern> patternList = new ArrayList<>();
    for (final String pattern : patterns) {
      if (pattern != null && !pattern.trim().isEmpty()) {
        patternList.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
      }
    }

    // processing files
    try (final Stream<Path> stream = Files.list(dir)) {
      final List<Path> fileList = stream
          .filter(path -> Files.isRegularFile(path) &&
                          patternList
                            .stream()
                            .anyMatch(pattern -> pattern.matcher(path.getFileName().toString()).matches()))
          .collect(Collectors.toList());
      for (final Path path : fileList) {
        FileDeletionHook.getInstance().unregisterFile(path);
        try {
          Files.delete(path);
          retVal++;
        } catch (NoSuchFileException e) {
          Logger.trace(e);
        }
      }
    }

    return retVal;
  }




  // -------------------------- INNER CLASSES --------------------------

  /** This class represents a parsed changelog entry. */
  private static class ChangeLogEntry {
    /**
     * Regular expression for parsing a single changelog entry.
     * <p>
     * Individual items can be queried by named groups: {@code pos}, {@code cmt}, {@code tp2}, {@code lang},
     * {@code comp}, and {@code name}.
     * </p>
     */
    private static final Pattern CHANGELOG_PATTERN  = Pattern.compile(
        "^(?<pos>\\d+):\\s" +
        "(/\\*(?<cmt>[^*]+)\\*/)?\\s" +
        "(~(?<tp2>[^~]+)~)\\s" +
        "(?<lang>\\d+)\\s" +
        "(?<comp>-?\\d+)\\s" +
        "//(?<name>.*)$");

    private final String resourceName;
    private final int index;
    private final String tp2File;
    private final int langId;
    private final int componentId;
    private final String componentName;
    private final String comment;
    private final Path filePath;

    private ChangeLogEntry(String resourceName, int index, String tp2File, int langId, int componentId, String componentName, String comment, Path filePath) {
      this.resourceName = Objects.requireNonNull(resourceName);
      this.index = index;
      this.tp2File = Objects.requireNonNull(tp2File);
      this.langId = langId;
      this.componentId = componentId;
      this.componentName = Objects.toString(componentName, "");
      this.comment = Objects.toString(comment, "");
      this.filePath = filePath;
    }

    /** Returns the resource name this changelog entry is based on. */
    public String getResourceName() {
      return resourceName;
    }

    /** Returns the changelog index of the resource. */
    public int getIndex() {
      return index;
    }

    /** Returns the relative tp2 file path of the mod component. */
    public String getTp2File() {
      return tp2File;
    }

    /** Returns the number of the mod language used at installation. */
    public int getLangId() {
      return langId;
    }

    /** Returns the mod component number. */
    public int getComponentId() {
      return componentId;
    }

    /** Returns the descriptive name of the mod component. */
    public String getComponentName() {
      return componentName;
    }

    /** Returns an optional comment generated by WeiDU. */
    public String getComment() {
      return comment;
    }

    /** Returns the {@link Path} of the associated modified file if available, {@code null} otherwise. */
    public Path getFilePath() {
      return filePath;
    }

    @Override
    public int hashCode() {
      return Objects.hash(comment, componentId, componentName, filePath, index, langId, resourceName, tp2File);
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
      ChangeLogEntry other = (ChangeLogEntry)obj;
      return Objects.equals(comment, other.comment) && componentId == other.componentId
          && Objects.equals(componentName, other.componentName) && Objects.equals(filePath, other.filePath)
          && index == other.index && langId == other.langId && Objects.equals(resourceName, other.resourceName)
          && Objects.equals(tp2File, other.tp2File);
    }

    @Override
    public String toString() {
      return "ChangeLogEntry [resourceName=" + resourceName + ", index=" + index + ", tp2File=" + tp2File + ", langId="
          + langId + ", componentId=" + componentId + ", componentName=" + componentName + ", comment=" + comment
          + ", filePath=" + filePath + "]";
    }

    /**
     * Parses a changelog definition string and returns an initialized {@code ChangeLogEntry} object.
     *
     * @param changeLogEntry {@code String} containing the definition of a single changelog entry.
     * @param resourceName   Name of the game resource this changelog is based on.
     * @param searchPath     {@link Path} of the directory where modified game resource versions can be found.
     * @return fully initialized {@link ChangeLogEntry} entry if parse operation was successful.
     * @throws ChangeLogParseException  if an unexpected issue interrupted the parse operation.
     * @throws NullPointerException     if any of the specified arguments is {@code null}.
     * @throws IllegalArgumentException if any of the specified arguments does not contain valid data.
     */
    public static ChangeLogEntry parse(String changeLogEntry, String resourceName, Path searchPath)
        throws ChangeLogParseException {
      if (changeLogEntry == null) {
        throw new NullPointerException("Entry parameter is null");
      }
      if (resourceName == null) {
        throw new NullPointerException("Resource name is null");
      }
      if (searchPath == null) {
        throw new NullPointerException("Search path is null");
      }
      if (!Files.isDirectory(searchPath)) {
        throw new IllegalArgumentException("Invalid search path specified");
      }

      changeLogEntry = changeLogEntry.trim();
      if (changeLogEntry.isEmpty()) {
        throw new IllegalArgumentException("Changelog entry is empty");
      }
      resourceName = resourceName.trim();
      if (resourceName.isEmpty()) {
        throw new IllegalArgumentException("Resource name is empty");
      }

      if (changeLogEntry.charAt(0) >= '0' && changeLogEntry.charAt(0) <= '9') { // very simple sanity check
        final Matcher matcher = CHANGELOG_PATTERN.matcher(changeLogEntry);
        if (matcher.matches()) {
          final int index;
          try {
            index = Integer.parseInt(matcher.group("pos"));
          } catch (NumberFormatException e) {
            throw new ChangeLogParseException("Parsing 'pos' group", e);
          }
          final String comment = Objects.toString(matcher.group("cmt"), "").trim();
          final String tp2File = matcher.group("tp2").trim();
          final int langId;
          try {
            langId = Integer.parseInt(matcher.group("lang"));
          } catch (NumberFormatException e) {
            throw new ChangeLogParseException("Parsing 'lang' group", e);
          }
          final int componentId;
          try {
            componentId = Integer.parseInt(matcher.group("comp"));
          } catch (NumberFormatException e) {
            throw new ChangeLogParseException("Parsing 'comp' group", e);
          }
          final String componentName = Objects.toString(matcher.group("name"), "").trim();

          final String resref;
          final String ext;
          int sepPos = resourceName.lastIndexOf('.');
          if (sepPos >= 0) {
            resref = resourceName.substring(0, sepPos);
            ext = resourceName.substring(sepPos);
          } else {
            resref = resourceName;
            ext = "";
          }
          final String modifiedFileName = String.format("%s.%05d%s", resref, index, ext);
          final Path modifiedFilePath = FileManager.queryExisting(searchPath, modifiedFileName);
          if (modifiedFilePath != null) {
            FileDeletionHook.getInstance().registerFile(modifiedFilePath);
          }
          return new ChangeLogEntry(resourceName, index, tp2File, langId, componentId, componentName, comment, modifiedFilePath);
        } else {
          throw new IllegalArgumentException("Not a valid changelog entry");
        }
      } else {
        throw new IllegalArgumentException("Not a valid changelog entry");
      }
    }
  }

  /**
   * A specialized exception class that signals if the changelog parser encountered an unexpected error.
   */
  private static class ChangeLogParseException extends Exception {
    /** Constructs a {@code ChangeLogParseException} with no detail message. */
    @SuppressWarnings("unused")
    public ChangeLogParseException() {
      super();
    }

    /** Constructs a {@code ChangeLogParseException} with the specified detail message. */
    @SuppressWarnings("unused")
    public ChangeLogParseException(String message) {
      super(message);
    }

    /**
     * Constructs a {@code ChangeLogParseException} with the specified cause
     * <tt>(cause==null ? null : cause.toString())</tt>.
     */
    @SuppressWarnings("unused")
    public ChangeLogParseException(Throwable cause) {
      super(cause);
    }

    /** Constructs a {@code ChangeLogParseException} with the specified detail message and cause. */
    public ChangeLogParseException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /**
   * Implementation of the {@link TableItem} interface for the WeiDU changelog results table.
   */
  private static class ChangeLogTableItem implements TableItem {
    private final String resourceName;
    private final DataNumber<ResourceEntry> index;
    private final String tp2File;
    private final int langId;
    private final int componentId;
    private final String componentName;
    private final String comment;

    public ChangeLogTableItem(ChangeLogEntry entry) {
      if (entry == null) {
        throw new NullPointerException("Changelog entry is null");
      }
      this.resourceName = entry.getResourceName();
      final ResourceEntry resEntry = (entry.getFilePath() != null) ? new FileResourceEntry(entry.getFilePath()) : null;
      this.index = new DataNumber<ResourceEntry>(entry.getIndex(), resEntry);
      this.tp2File = entry.getTp2File();
      this.langId = entry.getLangId();
      this.componentId = entry.getComponentId();
      this.componentName = entry.getComponentName();
      this.comment = entry.getComment();
    }

    @Override
    public Object getObjectAt(int columnIndex) {
      switch (columnIndex) {
        case 1:
          return tp2File;
        case 2:
          return langId;
        case 3:
          return componentId;
        case 4:
          return componentName;
        case 5:
          return comment;
        default:
          return index;
      }
    }

    @Override
    public int hashCode() {
      return Objects.hash(comment, componentId, componentName, index, langId, resourceName, tp2File);
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
      ChangeLogTableItem other = (ChangeLogTableItem)obj;
      return Objects.equals(comment, other.comment) && componentId == other.componentId
          && Objects.equals(componentName, other.componentName) && Objects.equals(index, other.index)
          && langId == other.langId && Objects.equals(resourceName, other.resourceName)
          && Objects.equals(tp2File, other.tp2File);
    }

    @Override
    public String toString() {
      return "Index: " + index + ", Tp2 File: " + tp2File + ", Language id: " + langId + ", Component id: "
          + componentId + ", Component name: " + componentName + ", Comment: " + comment;
    }
  }

  /**
   * This class represents a numeric value that can be associated with an arbitrary data object.
   *
   * @param <T> type of the associated data.
   */
  private static class DataNumber<T> implements Comparable<DataNumber<?>> {
    private long value;
    private T data;

    /**
     * Creates a new {@code DataNumber} object with the given parameters.
     *
     * @param value a numeric value.
     * @param data  associated data object of type T.
     */
    public DataNumber(long value, T data) {
      this.value = value;
      this.data = data;
    }

    /** Returns the numeric value of this object. */
    public long getValue() {
      return value;
    }

    /**
     * Assigns a new numeric value to this object and returns the previous value.
     *
     * @param newValue new numeric value.
     * @return the previous numeric value.
     */
    @SuppressWarnings("unused")
    public long setValue(long newValue) {
      final long retVal = value;
      value = newValue;
      return retVal;
    }

    /** Returns whether this object is associated with a non-{@code null} data object. */
    public boolean hasData() {
      return (data != null);
    }

    /** Returns the data object associated with this object. */
    public T getData() {
      return data;
    }

    /**
     * Associates this object with a new data object and returns the previous object.
     *
     * @param newData the new data object.
     * @return the previous data object.
     */
    @SuppressWarnings("unused")
    public T setData(T newData) {
      final T retVal = data;
      data = newData;
      return retVal;
    }

    @Override
    public int compareTo(DataNumber<?> o) {
      if (o == null) {
        return 1;
      }
      final long diff = getValue() - o.getValue();
      return (diff < 0L) ? -1 : (diff > 0L) ? 1 : 0;
    }

    @Override
    public int hashCode() {
      return Objects.hash(data, value);
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
      final DataNumber<?> other = (DataNumber<?>)obj;
      return Objects.equals(data, other.data) && value == other.value;
    }

    @Override
    public String toString() {
      return Long.toString(value);
    }
  }
}
