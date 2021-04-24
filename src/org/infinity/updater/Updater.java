// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.updater;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;

import org.infinity.NearInfinity;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.FileManager;

/**
 * Provides functions for checking, downloading and updating new versions of Near Infinity.
 */
public class Updater
{
  // Auto-check interval constants
  static final int UPDATE_INTERVAL_SESSION    = 0;
  static final int UPDATE_INTERVAL_DAILY      = 1;
  static final int UPDATE_INTERVAL_PER_WEEK   = 2;  // default
  static final int UPDATE_INTERVAL_PER_MONTH  = 3;

  // Name of the update server definition file
  private static final String UPDATE_FILENAME   = "update.xml";

  // A hardcoded list of default update servers that will be used if no update servers have been specified.
  private static final String[] DEFAULT_SERVERS = {
    "https://nearinfinitybrowser.github.io/NearInfinity/update/update.xml",
    "https://argent77.github.io/NearInfinity/update/update.xml"
  };

  // Number of supported update servers
  private static final int PREFS_SERVER_COUNT           = 4;

  // The preferences key format string for server URLs
  private static final String PREFS_SERVER_FMT          = "UpdateServer%d";

  // preferences key for determining whether to check for stable NI releases only
  private static final String PREFS_STABLEONLY          = "UpdateStableReleasesOnly";

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

  // preferences key for storing the hash found on the update server
  // (needed to trigger notifications only once for each new release)
  private static final String PREFS_UPDATE_HASH       = "UpdateReleaseHash";

  // preferences key for storing the NI version found on the update server
  // (needed to trigger notifications only once for each new release)
  private static final String PREFS_UPDATE_VERSION    = "UpdateReleaseVersion";

  // preferences key for storing the timestamp of the file found on the update server
  // (needed to trigger notifications only once for each new release)
  private static final String PREFS_UPDATE_TIMESTAMP  = "UpdateReleaseTimestamp";

  private static Updater instance = null;

  private final List<String> serverList = new ArrayList<>();

  private Preferences prefs;
  private String hash, version, timestamp;
  private Calendar autoCheckDate;
  private Proxy proxy;
  private int autoCheckInterval;
  private boolean stableOnly, autoCheckEnabled, proxyEnabled;

  /** Returns a list of predefined server URLs. */
  public static String[] getDefaultServerList()
  {
    return DEFAULT_SERVERS;
  }

  /** Returns the maximum supported number of updater servers. */
  public static int getMaxServerCount()
  {
    return PREFS_SERVER_COUNT;
  }

  public static Updater getInstance()
  {
    if (instance == null) {
      instance = new Updater();
    }
    return instance;
  }

  /**
   * Returns whether the specified release can be considered a new release.
   * @param release The release to check.
   * @param onlyOnce If {@code true}, each new release will be checked only once.
   * @return {@code true} if the specified release is considered newer, {@code false} otherwise.
   */
  public static boolean isNewRelease(UpdateInfo.Release release, boolean onlyOnce)
  {
    boolean isNewer = false;
    if (release != null && release.isValid()) {
      String curHash = null;
      String curVersion = null;
      Calendar curCal = null;

      if (onlyOnce && !getInstance().getCurrentHash().isEmpty() &&
          !(getInstance().getCurrentTimeStamp().isEmpty() || getInstance().getCurrentVersion().isEmpty())) {
        curHash = getInstance().getCurrentHash();
        curVersion = getInstance().getCurrentVersion();
        curCal = Utils.toCalendar(getInstance().getCurrentTimeStamp());
      } else {
        curHash = getJarFileHash();
        curVersion = NearInfinity.getVersion();
        curCal = getJarFileDate();
      }

      if (curHash != null && !curHash.isEmpty()) {
        String newHash = release.getHash();
        String newVersion = release.getVersion();
        Calendar newCal = release.getTimeStamp();

        isNewer = !curHash.equalsIgnoreCase(newHash);
        if (curCal != null && newCal != null) {
          isNewer &= (curCal.compareTo(newCal) < 0);
        } else if (curVersion != null && newVersion != null) {
          isNewer &= !curVersion.equalsIgnoreCase(newVersion);
        }

        getInstance().setCurrentHash(newHash);
        getInstance().setCurrentVersion(newVersion);
        getInstance().setCurrentTimeStamp(Utils.toTimeStamp(newCal));
      }
    }
    return isNewer;
  }

  /** Returns the modification time of the current JAR's MANIFEST.MF. */
  static Calendar getJarFileDate()
  {
    String jarPath = Utils.getJarFileName(NearInfinity.class);
    if (jarPath != null && !jarPath.isEmpty()) {
      try (JarFile jf = new JarFile(jarPath)) {
      ZipEntry manifest = jf.getEntry("META-INF/MANIFEST.MF");
        if (manifest != null) {
          Calendar cal = Calendar.getInstance();
          if (manifest.getTime() >= 0L) {
            cal.setTimeInMillis(manifest.getTime());
            return cal;
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  /**
   * Calculates the checksum of the current JAR file using the specified hash algorithm.
   * @return The MD5 checksum of the current JAR file or empty string on error.
   */
  static String getJarFileHash()
  {
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
   * Checks whether server1 and server2 are the same.
   * Servers are considered the same if one server is part of or equal to the other.
   * Empty server strings always return true.
   */
  static boolean isSameServer(String server1, String server2)
  {
    server1 = (server1 != null) ? server1.toLowerCase(Locale.ENGLISH) : "";
    server2 = (server2 != null) ? server2.toLowerCase(Locale.ENGLISH) : "";
    if (server1.isEmpty() || server2.isEmpty()) {
      return true;
    } else {
      return (server1.startsWith(server2) || server2.startsWith(server1));
    }
  }

  private Updater()
  {
    try {
      prefs = Preferences.userNodeForPackage(getClass());
    } catch (SecurityException se) {
      prefs = null;
      se.printStackTrace();
    }

    loadUpdateSettings();
  }

  /** Provides access to the server list. */
  public List<String> getServerList()
  {
    return serverList;
  }

  /**
   * Adds a new update server link to the server list. Optionally checks online if the link points
   * to a valid update.xml. Does nothing if the server URL already exists.
   * @param link The update server URL.
   * @param validate Only checks link format if {@code false}. Additionally checks if
   *                 link points to a valid update.xml if {@code true}.
   * @throws IOException
   * @throws MalformedURLException
   */
  public void addServer(String link, boolean validate) throws MalformedURLException, IOException
  {
    if (link != null && !link.isEmpty() && serverList.size() < getMaxServerCount()) {
      if (Utils.isUrlValid(link)) {
        boolean isValid = (validate == false);
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

  /** Returns whether to look for stable releases only. */
  public boolean isStableOnly()
  {
    return stableOnly;
  }

  /** Returns whether to consider only stable releases when checking for updates. */
  public void setStableOnly(boolean set)
  {
    stableOnly = set;
  }

  /** Returns whether to automatically check for updates. */
  public boolean isAutoUpdateCheckEnabled()
  {
    return autoCheckEnabled;
  }

  /** Updates whether to automatically check for updates. */
  public void setAutoUpdateCheckEnabled(boolean set)
  {
    autoCheckEnabled = set;
  }

  /** Returns the current check interval value (as specified by the UPDATE_INTERVAL_xxx constants). */
  public int getAutoUpdateCheckInterval()
  {
    return autoCheckInterval;
  }

  /** Updates the check interval value (as specified by the UPDATE_INTERVAL_xxx constants). */
  public void setAutoUpdateCheckInterval(int value)
  {
    if (value < 0) value = 0;
    if (value > UPDATE_INTERVAL_PER_MONTH) value = UPDATE_INTERVAL_PER_MONTH;
    autoCheckInterval = value;
  }

  /** Returns the last update check date. */
  public Calendar getAutoUpdateCheckDate()
  {
    return autoCheckDate;
  }

  /** Updates the last update check date. Specifying {@code null} will add the current date. */
  public void setAutoUpdateCheckDate(Calendar cal)
  {
    if (cal != null) {
      autoCheckDate = cal;
    } else {
      autoCheckDate = Calendar.getInstance();
    }
  }

  /** Returns true if the last auto update check is older than the currently defined update interval. */
  public boolean hasAutoUpdateCheckDateExpired()
  {
    return hasAutoUpdateCheckDateExpired(getAutoUpdateCheckInterval());
  }

  /** Returns true if the last auto update check is older than specified by the UPDATE_INTERVAL_xxx constant. */
  public boolean hasAutoUpdateCheckDateExpired(int value)
  {
    switch (value) {
      case UPDATE_INTERVAL_SESSION:
      {
        return true;
      }
      case UPDATE_INTERVAL_DAILY:
      {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        return (getAutoUpdateCheckDate().compareTo(cal) < 0);
      }
      case UPDATE_INTERVAL_PER_WEEK:
      {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_MONTH, -1);
        return (getAutoUpdateCheckDate().compareTo(cal) < 0);
      }
      case UPDATE_INTERVAL_PER_MONTH:
      {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONDAY, -1);
        return (getAutoUpdateCheckDate().compareTo(cal) < 0);
      }
    }
    return false;
  }

  /** Returns whether to use a proxy for accessing remote servers. */
  public boolean isProxyEnabled()
  {
    return proxyEnabled;
  }

  public void setProxyEnabled(boolean set)
  {
    proxyEnabled = set;
  }

  /**
   * Returns the current Proxy settings if available and enabled.
   * More specifically, calls {@link #getProxy(boolean)} with force = {@code false}.
   */
  public Proxy getProxy()
  {
    return getProxy(false);
  }

  /**
   * Returns the current Proxy settings if available and enabled.
   * @param force Force to return proxy information even if it has been disabled.
   * @return A proxy object or {@code null} depending on availability.
   */
  public Proxy getProxy(boolean force)
  {
    if (proxyEnabled || force) {
      return proxy;
    } else {
      return null;
    }
  }

  /**
   * Sets up a new HTTP proxy. Specifying {@code null} or 0 for one or both parameters will
   * remove the current proxy settings.
   * @param hostName The host name of the proxy address.
   * @param port The port of the proxy address.
   */
  public void setProxy(String hostName, int port)
  {
    if (hostName != null && !hostName.isEmpty() && port >= 0 && port < 65536) {
      proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostName, port));
    } else {
      proxy = null;
    }
  }

  /** Updates hash and timestamp from given Release info object. */
  public void updateReleaseInfo(UpdateInfo.Release release)
  {
    if (release != null && release.isValid()) {
      setCurrentHash(release.getHash());
      setCurrentTimeStamp(release.getTimeStampString());
    }
  }

  /** Returns the cached hash string from the latest update check. */
  public String getCurrentHash()
  {
    return hash;
  }

  /** Updates the cached hash string. */
  public void setCurrentHash(String hash)
  {
    if (hash != null) {
      this.hash = hash;
    } else {
      this.hash = "";
    }
  }

  /** Returns the cached NI version. */
  public String getCurrentVersion()
  {
    return version;
  }

  /** Updtes the cached NI version. */
  public void setCurrentVersion(String version)
  {
    if (version != null) {
      this.version = version;
    } else {
      this.version = "";
    }
  }

  /** Returns the cached timestamp value from the latest update check. */
  public String getCurrentTimeStamp()
  {
    return timestamp;
  }

  /** Updates the cached timestamp string. */
  public void setCurrentTimeStamp(String timestamp)
  {
    if (timestamp != null) {
      this.timestamp = timestamp;
    } else {
      this.timestamp = "";
    }
  }

  /** Loads server and update settings from stored preferences. */
  public void loadUpdateSettings()
  {
    // resetting values
    serverList.clear();
    stableOnly = false;
    autoCheckEnabled = false;
    autoCheckInterval = UPDATE_INTERVAL_PER_WEEK;
    autoCheckDate = Calendar.getInstance();
    proxyEnabled = false;
    proxy = null;
    hash = "";
    version = "";
    timestamp = "";

    if (prefs != null) {
      // loading server list (skipping identical server entries)
      stableOnly = prefs.getBoolean(PREFS_STABLEONLY, false);
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
      autoCheckInterval = prefs.getInt(PREFS_AUTOCHECK_INTERAVAL, UPDATE_INTERVAL_PER_WEEK);
      Calendar cal = Utils.toCalendar(prefs.get(PREFS_AUTOCHECK_TIMESTAMP, null));
      if (cal != null) {
        autoCheckDate = cal;
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
      hash = prefs.get(PREFS_UPDATE_HASH, "");
      version = prefs.get(PREFS_UPDATE_VERSION, "");
      timestamp = prefs.get(PREFS_UPDATE_TIMESTAMP, "");
    }

    // Fallback: add static servers to list
    if (serverList.isEmpty()) {
      Collections.addAll(serverList, DEFAULT_SERVERS);
    }
  }

  /** Saves server and update settings to disk. */
  public boolean saveUpdateSettings()
  {
    if (prefs != null) {
      // saving server list
      prefs.putBoolean(PREFS_STABLEONLY, stableOnly);
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
      prefs.putInt(PREFS_AUTOCHECK_INTERAVAL, autoCheckInterval);
      prefs.put(PREFS_AUTOCHECK_TIMESTAMP, Utils.toTimeStamp(autoCheckDate));

      // saving proxy settings
      if (proxy != null && proxy.type() == Proxy.Type.HTTP &&
          proxy.address() instanceof InetSocketAddress) {
        InetSocketAddress addr = (InetSocketAddress)proxy.address();
        prefs.putBoolean(PREFS_PROXYENABLED, proxyEnabled);
        prefs.put(PREFS_PROXYHOST, addr.getHostName());
        prefs.putInt(PREFS_PROXYPORT, addr.getPort());
      } else {
        prefs.putBoolean(PREFS_PROXYENABLED, false);
        prefs.remove(PREFS_PROXYHOST);
        prefs.remove(PREFS_PROXYPORT);
      }

      // saving autocheck-related information
      prefs.put(PREFS_UPDATE_HASH, hash);
      prefs.put(PREFS_UPDATE_VERSION, version);
      prefs.put(PREFS_UPDATE_TIMESTAMP, timestamp);

      return true;
    }
    return false;
  }

  /**
   * Checks the specified URL if it points to a valid update.xml and returns the
   * (possibly modified) URL on success or {@code null} on error.
   * @param link A URL pointing to the update.xml.
   * @return A URL that is guaranteed to point to a valid update.xml or {@code null} on error.
   * @throws IOException
   * @throws MalformedURLException
   */
  public String getValidatedUpdateUrl(String link) throws MalformedURLException, IOException
  {
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
   * @return The UpdateInfo object containing update information, or {@code null} if not available.
   */
  public UpdateInfo loadUpdateInfo()
  {
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
      } catch (Exception e) {
        // skip update server on error and try next
      }
    }
    return null;
  }
}
