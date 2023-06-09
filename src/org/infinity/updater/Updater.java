// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2023 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.updater;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

import org.infinity.NearInfinity;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

/**
 * Provides functions for checking, downloading and updating new versions of Near Infinity.
 */
public class Updater {
  /** Defines available autocheck intervals. */
  public enum Interval {
    HOURLY(0, "Hourly", Duration.ofHours(1)),
    DAILY(1, "Daily", Duration.ofDays(1)),
    WEEKLY(2, "Weekly", Duration.ofDays(7)),
    MONTHLY(3, "Monthly", Duration.ofDays(30));

    private final int id;
    private final String label;
    private final Duration interval;

    private Interval(int id, String label, Duration interval) {
      this.id = id;
      this.label = label;
      this.interval = interval;
    }

    /** Returns the unique identifier of this interval. */
    public int getId() {
      return id;
    }

    /** Returns a descriptive name of this interval. */
    public String getLabel() {
      return label;
    }

    /** Returns the interval time as {@link Duration} value. */
    public Duration getInterval() {
      return interval;
    }

    @Override
    public String toString() {
      return getLabel();
    }

    /** Returns the default {@code Interval} ({@code WEEKLY}). */
    public static Interval getDefault() {
      return WEEKLY;
    }

    /**
     * Returns the {@code Interval} value based on the given identifier.
     *
     * @param id Interval identifier.
     * @return Matching {@code Interval} instance, {@link #getDefault()} otherwise.
     */
    public static Interval FromId(int id) {
      for (final Interval i: Interval.values()) {
        if (i.getId() == id) {
          return i;
        }
      }
      return getDefault();
    }
  }

  // Name of the update server definition file
  private static final String UPDATE_FILENAME   = "update.xml";

  // A hardcoded list of default update servers that will be used if no update servers have been specified.
  private static final String[] DEFAULT_SERVERS = {
      "https://nearinfinitybrowser.github.io/NearInfinity/update/update.xml",
      "https://argent77.github.io/NearInfinity/update/update.xml" };

  // Number of supported update servers
  private static final int PREFS_SERVER_COUNT           = 4;

  // The preferences key format string for server URLs
  private static final String PREFS_SERVER_FMT          = "UpdateServer%d";

  // preferences key for determining whether automatic update checks are enabled
  private static final String PREFS_AUTOCHECK_UPDATES   = "UpdateAutoCheckEnabled";

  // preferences key for auto-check interval (as specified by the interval constants above)
  private static final String PREFS_AUTOCHECK_INTERAVAL = "UpdateAutoCheckInterval";

  // preferences key for the date/time of the last auto-check attempt
  private static final String PREFS_AUTOCHECK_TIMESTAMP = "UpdateAutoCheckTimeStamp";

  // preferences key for determining whether to use a proxy
  private static final String PREFS_PROXYENABLED        = "UpdateProxyEnabled";

  // preferences key for proxy host address (if any)
  private static final String PREFS_PROXYHOST           = "UpdateProxyHost";

  // preferences key for proxy port (if any)
  private static final String PREFS_PROXYPORT           = "UpdateProxyPort";

  // preferences key for storing the NI version found on the update server
  // (needed to trigger notifications only once for each new release)
  private static final String PREFS_UPDATE_VERSION      = "UpdateReleaseVersion";

  // preferences key for storing the timestamp of the file found on the update server
  // (needed to trigger notifications only once for each new release)
  private static final String PREFS_UPDATE_TIMESTAMP    = "UpdateReleaseTimestamp";

  private static Updater instance = null;

  private final List<String> serverList = new ArrayList<>();

  private Preferences prefs;
  private String version;
  private String timestamp;
  private OffsetDateTime autoCheckDate;
  private Proxy proxy;
  private Interval autoCheckInterval;
  private boolean autoCheckEnabled;
  private boolean proxyEnabled;

  /** Returns a list of predefined server URLs. */
  public static String[] getDefaultServerList() {
    return DEFAULT_SERVERS;
  }

  /** Returns the maximum supported number of updater servers. */
  public static int getMaxServerCount() {
    return PREFS_SERVER_COUNT;
  }

  public static Updater getInstance() {
    if (instance == null) {
      instance = new Updater();
    }
    return instance;
  }

  /**
   * Returns whether the specified release can be considered a new release.
   *
   * This method uses GitHub API information to determine whether a new release is available.
   *
   * @param release The GitHub release to check.
   * @param onlyOnce If {@code true} then each new release will be checked only once.
   * @return {@code true} if the specified release is considered newer, {@code false} otherwise.
   */
  public static boolean isNewRelease(UpdateInfo.Release release, boolean onlyOnce) {
    boolean retVal = false;

    if (release != null) {
      final String curVersionString;
      if (onlyOnce && !getInstance().getCurrentVersion().isEmpty()) {
        curVersionString = getInstance().getCurrentVersion();
      } else {
        curVersionString = NearInfinity.getVersion();
      }

      // checking version info
      int compare = 0;
      try {
        final long[] curVersion = getNormalizedVersion(curVersionString);
        final long[] newVersion = getNormalizedVersion(release.tagName);
        compare = compareNormalizedVersion(newVersion, curVersion);
      } catch (Exception e) {
        e.printStackTrace();
      }
      retVal = (compare > 0);

      getInstance().setCurrentVersion(release.tagName);
    }

    return retVal;
  }

  /** Returns the modification time of the current JAR's MANIFEST.MF as {@link FileTime} instance. */
  static FileTime getJarFileTimeValue() throws Exception {
    String jarPath = Utils.getJarFileName(NearInfinity.class);
    if (jarPath != null && !jarPath.isEmpty()) {
      try (JarFile jf = new JarFile(jarPath)) {
        ZipEntry manifest = jf.getEntry("META-INF/MANIFEST.MF");
        if (manifest != null) {
          return manifest.getLastModifiedTime();
        }
      } catch (IOException e) {
        throw e;
      }
    }
    return null;
  }

  /** Returns the modification time of the current JAR's MANIFEST.MF as {@link OffsetDateTime} instance. */
  static OffsetDateTime getJarFileDateTime() {
    try {
    final FileTime ft = getJarFileTimeValue();
    if (ft != null) {
      return OffsetDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault());
    }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Calculates the checksum of the current JAR file using the specified hash algorithm.
   *
   * @return The MD5 checksum of the current JAR file or empty string on error.
   */
  static String getJarFileHash() {
    String path = Utils.getJarFileName(NearInfinity.class);
    if (path != null && !path.isEmpty()) {
      Path jarPath = FileManager.resolve(path);
      if (FileEx.create(jarPath).isFile()) {
        try {
          return Utils.generateMD5Hash(new FileInputStream(path));
        } catch (IOException e) {
        }
      }
    }
    return "";
  }

  /**
   * Checks whether server1 and server2 are the same. Servers are considered the same if one server is part of or equal
   * to the other. Empty server strings always return true.
   */
  static boolean isSameServer(String server1, String server2) {
    server1 = (server1 != null) ? server1.toLowerCase(Locale.ENGLISH) : "";
    server2 = (server2 != null) ? server2.toLowerCase(Locale.ENGLISH) : "";
    if (server1.isEmpty() || server2.isEmpty()) {
      return true;
    } else {
      return (server1.startsWith(server2) || server2.startsWith(server1));
    }
  }

  /**
   * Attempts to convert the specified string into a series of {@code long} values.<br/>
   * <br/>
   * Examples:<br/>
   * <code>"v2.3-20230408"</code> is converted into <code>new long[]{2, 3, 20230408}</code><br/>
   * <code>"v2.3-20230408-1"</code> is converted into <code>new long[]{2, 3, 20230408, 1}</code><br/>
   *
   * @param s The version string to parse.
   * @return A integer array of dynamic size containing the parsed version information.
   * @throws NullPointerException If the version string is {@code null}.
   * @throws IllegalArgumentException If the version string does not contain valid integer values.
   */
  private static long[] getNormalizedVersion(String s) {
    if (s == null) {
      throw new NullPointerException("Version string cannot be null");
    }

    final Pattern regNumber = Pattern.compile("\\d+");
    final Matcher matcher = regNumber.matcher(s);

    ArrayList<Long> items = new ArrayList<>();
    while (matcher.find()) {
      items.add(Long.parseLong(matcher.group()));
    }

    if (items.isEmpty()) {
      throw new IllegalArgumentException("Version string does not contain numeric elements");
    }

    long[] retVal = new long[items.size()];
    for (int i = 0; i < retVal.length; i++) {
      retVal[i] = items.get(i).longValue();
    }

    return retVal;
  }

  /**
   * Compares the numbers in both arrays and returns the result as:
   * <ul>
   * <li>a negative number if {@code arr1} is smaller than {@code arr2}</li>
   * <li>zero if {@code arr1} and {@code arr2} are equal</li>
   * <li>a positive number if {@code arr1} is greater than {@code arr2}</li>
   * </ul>
   * Non-existing array elements are treated as 0.
   *
   * @param arr1 The first numeric array.
   * @param arr2 The second numeric array.
   * @return The computed result as described above.
   * @throws NullPointerException if any of the parameters is {@code null}.
   */
  private static int compareNormalizedVersion(long[] arr1, long[] arr2) {
    if (arr1 == null) {
      throw new NullPointerException("First parameter is null");
    }
    if (arr2 == null) {
      throw new NullPointerException("Second parameter is null");
    }

    for (int i = 0, count = Math.max(arr1.length, arr2.length); i < count; i++) {
      long v1 = i < arr1.length ? arr1[i] : 0;
      long v2 = i < arr2.length ? arr2[i] : 0;
      long result = v1 - v2;
      if (result != 0) {
        return (result < 0) ? -1 : 1;
      }
    }

    return 0;
  }

  private Updater() {
    try {
      prefs = Preferences.userNodeForPackage(getClass());
    } catch (SecurityException se) {
      prefs = null;
      se.printStackTrace();
    }

    loadUpdateSettings();
  }

  /** Provides access to the server list. */
  public List<String> getServerList() {
    return serverList;
  }

  /**
   * Adds a new update server link to the server list. Optionally checks online if the link points to a valid
   * update.xml. Does nothing if the server URL already exists.
   *
   * @param link     The update server URL.
   * @param validate Only checks link format if {@code false}. Additionally checks if link points to a valid update.xml
   *                 if {@code true}.
   * @throws IOException
   * @throws MalformedURLException
   */
  public void addServer(String link, boolean validate) throws MalformedURLException, IOException {
    if (link != null && !link.isEmpty() && serverList.size() < getMaxServerCount()) {
      if (Utils.isUrlValid(link)) {
        boolean isValid = !validate;
        if (!isValid) {
          // check availability of update.xml
          isValid = (getValidatedUpdateUrl(link) != null);
        }
        if (isValid) {
          // checking if server is already in list
          for (Iterator<String> iter = serverList.iterator(); iter.hasNext();) {
            if (isSameServer(link, iter.next())) {
              // consider both links as equal
              return;
            }
          }

          // adding server link
          serverList.add(link);
        }
      }
    }
  }

  /** Returns whether to automatically check for updates. */
  public boolean isAutoUpdateCheckEnabled() {
    return autoCheckEnabled;
  }

  /** Updates whether to automatically check for updates. */
  public void setAutoUpdateCheckEnabled(boolean set) {
    autoCheckEnabled = set;
  }

  /** Returns the current check interval value. */
  public Interval getAutoUpdateCheckInterval() {
    return autoCheckInterval;
  }

  /** Updates the check interval value (as specified by the UPDATE_INTERVAL_xxx constants). */
  public void setAutoUpdateCheckInterval(Interval interval) {
    if (interval == null) {
      interval = Interval.getDefault();
    }
    autoCheckInterval = interval;
  }

  /** Returns the last update check date. */
  public OffsetDateTime getAutoUpdateCheckDate() {
    return autoCheckDate;
  }

  /** Updates the last update check date. Specifying {@code null} will add the current date. */
  public void setAutoUpdateCheckDate(OffsetDateTime date) {
    if (date == null) {
      date = OffsetDateTime.now();
    }
    autoCheckDate = date;
  }

  /** Returns true if the last auto update check is older than the currently defined update interval. */
  public boolean hasAutoUpdateCheckDateExpired() {
    return hasAutoUpdateCheckDateExpired(getAutoUpdateCheckInterval());
  }

  /** Returns true if the last auto update check is older than specified by the {@code Interval} value. */
  public boolean hasAutoUpdateCheckDateExpired(Interval interval) {
    if (interval == null) {
      interval = Interval.getDefault();
    }
    OffsetDateTime intervalExpiredAt = getAutoUpdateCheckDate().plus(interval.getInterval());
    return intervalExpiredAt.compareTo(OffsetDateTime.now()) < 0;
  }

  /** Returns whether to use a proxy for accessing remote servers. */
  public boolean isProxyEnabled() {
    return proxyEnabled;
  }

  public void setProxyEnabled(boolean set) {
    proxyEnabled = set;
  }

  /**
   * Returns the current Proxy settings if available and enabled. More specifically, calls {@link #getProxy(boolean)}
   * with force = {@code false}.
   */
  public Proxy getProxy() {
    return getProxy(false);
  }

  /**
   * Returns the current Proxy settings if available and enabled.
   *
   * @param force Force to return proxy information even if it has been disabled.
   * @return A proxy object or {@code null} depending on availability.
   */
  public Proxy getProxy(boolean force) {
    if (proxyEnabled || force) {
      return proxy;
    } else {
      return null;
    }
  }

  /**
   * Sets up a new HTTP proxy. Specifying {@code null} or 0 for one or both parameters will remove the current proxy
   * settings.
   *
   * @param hostName The host name of the proxy address.
   * @param port     The port of the proxy address.
   */
  public void setProxy(String hostName, int port) {
    if (hostName != null && !hostName.isEmpty() && port >= 0 && port < 65536) {
      proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostName, port));
    } else {
      proxy = null;
    }
  }

  /** Returns the cached NI version. */
  public String getCurrentVersion() {
    return version;
  }

  /** Updates the cached NI version. */
  public void setCurrentVersion(String version) {
    if (version != null) {
      this.version = version;
    } else {
      this.version = "";
    }
  }

  /** Returns the cached timestamp value from the latest update check. */
  public String getCurrentTimeStamp() {
    return timestamp;
  }

  /** Updates the cached timestamp string. */
  public void setCurrentTimeStamp(String timestamp) {
    if (timestamp != null) {
      this.timestamp = timestamp;
    } else {
      this.timestamp = "";
    }
  }

  /** Loads server and update settings from stored preferences. */
  public void loadUpdateSettings() {
    // resetting values
    serverList.clear();
    autoCheckEnabled = false;
    autoCheckInterval = Interval.getDefault();
    autoCheckDate = OffsetDateTime.now();
    proxyEnabled = false;
    proxy = null;
    version = "";
    timestamp = "";

    if (prefs != null) {
      // loading server list (skipping identical server entries)
      for (int i = 0; i < getMaxServerCount(); i++) {
        String server = prefs.get(String.format(PREFS_SERVER_FMT, i), "");
        if (!server.isEmpty()) {
          // skip duplicate server URLs
          boolean isSame = false;
          for (Iterator<String> iter = serverList.iterator(); iter.hasNext();) {
            if (isSameServer(server, iter.next())) {
              isSame = true;
              break;
            }
          }
          if (!isSame) {
            serverList.add(server);
          }
        }
      }

      // loading auto update settings
      autoCheckEnabled = prefs.getBoolean(PREFS_AUTOCHECK_UPDATES, false);
      autoCheckInterval = Interval.FromId(prefs.getInt(PREFS_AUTOCHECK_INTERAVAL, Interval.getDefault().getId()));
      final String dateTime = prefs.get(PREFS_AUTOCHECK_TIMESTAMP, null);
      try {
        autoCheckDate = Utils.getDateTimeFromString(dateTime);
      } catch (DateTimeParseException e) {
        System.out.println("DateTimeParseException: " + e.getMessage());
        autoCheckDate = OffsetDateTime.now();
      }

      // loading proxy settings
      proxyEnabled = prefs.getBoolean(PREFS_PROXYENABLED, false);
      String host = prefs.get(PREFS_PROXYHOST, "");
      int port = prefs.getInt(PREFS_PROXYPORT, -1);
      if (port >= 0) {
        try {
          if (host.isEmpty()) {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(port));
          } else {
            proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port));
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      // loading autocheck-related information
      version = prefs.get(PREFS_UPDATE_VERSION, "");
      timestamp = prefs.get(PREFS_UPDATE_TIMESTAMP, "");
    }

    // Fallback: add static servers to list
    if (serverList.isEmpty()) {
      Collections.addAll(serverList, DEFAULT_SERVERS);
    }
  }

  /** Saves server and update settings to disk. */
  public boolean saveUpdateSettings() {
    if (prefs != null) {
      // saving server list
      for (int i = 0, size = getMaxServerCount(); i < size; i++) {
        String server = null;
        if (i < serverList.size()) {
          server = serverList.get(i).trim();
        }
        if (server != null && !server.isEmpty()) {
          prefs.put(String.format(PREFS_SERVER_FMT, i), server);
        } else {
          prefs.remove(String.format(PREFS_SERVER_FMT, i));
        }
      }

      // saving auto update settings
      prefs.putBoolean(PREFS_AUTOCHECK_UPDATES, autoCheckEnabled);
      prefs.putInt(PREFS_AUTOCHECK_INTERAVAL, autoCheckInterval.getId());
      prefs.put(PREFS_AUTOCHECK_TIMESTAMP, Utils.getStringFromDateTime(autoCheckDate));

      // saving proxy settings
      if (proxy != null && proxy.type() == Proxy.Type.HTTP && proxy.address() instanceof InetSocketAddress) {
        InetSocketAddress addr = (InetSocketAddress) proxy.address();
        prefs.putBoolean(PREFS_PROXYENABLED, proxyEnabled);
        prefs.put(PREFS_PROXYHOST, addr.getHostName());
        prefs.putInt(PREFS_PROXYPORT, addr.getPort());
      } else {
        prefs.putBoolean(PREFS_PROXYENABLED, false);
        prefs.remove(PREFS_PROXYHOST);
        prefs.remove(PREFS_PROXYPORT);
      }

      // saving autocheck-related information
      prefs.put(PREFS_UPDATE_VERSION, version);
      prefs.put(PREFS_UPDATE_TIMESTAMP, timestamp);

      return true;
    }
    return false;
  }

  /**
   * Checks the specified URL if it points to a valid update.xml and returns the (possibly modified) URL on success or
   * {@code null} on error.
   *
   * @param link A URL pointing to the update.xml.
   * @return A URL that is guaranteed to point to a valid update.xml or {@code null} on error.
   * @throws IOException
   * @throws MalformedURLException
   */
  public String getValidatedUpdateUrl(String link) throws MalformedURLException, IOException {
    if (Utils.isUrlValid(link)) {
      try {
        // try the specified link first
        URL url = new URL(link);
        String xml = null;
        try {
          xml = Utils.downloadText(url, getProxy(), "utf-8");
        } catch (IOException e) {
        }
        if (xml != null && UpdateInfo.isValidXml(xml, url.toExternalForm())) {
          return url.toExternalForm();
        }
        // try the specified link appended by "update.xml" second
        url = Utils.getUrl(url, UPDATE_FILENAME);
        xml = Utils.downloadText(url, getProxy(), "utf-8");
        if (xml != null && UpdateInfo.isValidXml(xml, url.toExternalForm())) {
          return url.toExternalForm();
        }
      } catch (MalformedURLException e) {
      }
    }
    return null;
  }

  /**
   * Attempts to download update information and return them as UpdateInfo object.
   *
   * @return The UpdateInfo object containing update information, or {@code null} if not available.
   * @throws Exception
   */
  public UpdateInfo loadUpdateInfo() throws Exception {
    for (Iterator<String> iter = getServerList().iterator(); iter.hasNext();) {
      try {
        URL url = new URL(iter.next());
        String xml = Utils.downloadText(url, getProxy(), "UTF-8");
        UpdateInfo info = new UpdateInfo(xml, url.toExternalForm());
        if (info.isValid()) {
          // adding alternate servers to list (if available)
          for (int i = 0, count = info.getGeneral().getServerCount(); i < count; i++) {
            try {
              addServer(info.getGeneral().getServer(i), true);
            } catch (Exception e) {
              // skip adding server on error
            }
          }

          return info;
        }
      } catch (IOException e) {
        // skip update server on error and try next
      }
    }
    return null;
  }
}
