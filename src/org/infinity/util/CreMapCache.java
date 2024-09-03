// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.infinity.NearInfinity;
import org.infinity.gui.StatusBar;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.key.ResourceEntry;
import org.tinylog.Logger;

/**
 * Maintains a list of script names to CRE resource mappings.
 */
public final class CreMapCache {
  private static final Map<String, Set<ResourceEntry>> SCRIPT_NAMES_CRE = new HashMap<>();
  private static final Set<String> SCRIPT_NAMES_ARE = new HashSet<>();

  private static boolean initialized = false;

  public static void creInvalid(ResourceEntry entry) {
    if (entry != null) {
      SCRIPT_NAMES_CRE.remove(normalized(entry.getResourceName()));
    }
  }

  public static void init() {
    if (!isInitialized()) {
      initialize();
    }
  }

  public static void clearCache() {
    if (isInitialized()) {
      SCRIPT_NAMES_CRE.clear();
      SCRIPT_NAMES_ARE.clear();
      initialized = false;
    }
  }

  public static void reset() {
    clearCache();
    init();
  }

  public static boolean isInitialized() {
    return initialized;
  }

  public static boolean hasScriptName(String name) {
    ensureInitialized(-1);
    if (isInitialized() && name != null) {
      return SCRIPT_NAMES_CRE.containsKey(normalized(name));
    }
    return false;
  }

  public static boolean hasCreScriptName(String name) {
    ensureInitialized(-1);
    if (isInitialized() && name != null) {
      return SCRIPT_NAMES_CRE.containsKey(normalized(name));
    } else {
      return false;
    }
  }

  public static boolean hasAreScriptName(String name) {
    ensureInitialized(-1);
    if (isInitialized() && name != null) {
      return SCRIPT_NAMES_ARE.contains(normalized(name));
    } else {
      return false;
    }
  }

  public static Set<ResourceEntry> getCreForScriptName(String name) {
    ensureInitialized(-1);
    if (isInitialized() && name != null) {
      return SCRIPT_NAMES_CRE.get(normalized(name));
    }
    return null;
  }

  public static Set<String> getCreScriptNames() {
    ensureInitialized(-1);
    if (isInitialized()) {
      return SCRIPT_NAMES_CRE.keySet();
    } else {
      return new HashSet<>();
    }
  }

  /** Waits until indexing process has finished or time out occurred. */
  private static boolean ensureInitialized(int timeOutMS) {
    long timeOut = (timeOutMS >= 0) ? System.nanoTime() + (timeOutMS * 1000000L) : -1L;
    while (!isInitialized() && (timeOut == -1L || System.nanoTime() < timeOut)) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        Logger.trace(e);
      }
    }

    return isInitialized();
  }

  private static String normalized(String s) {
    if (s != null) {
      return s.replace(" ", "").toLowerCase(Locale.ENGLISH);
    } else {
      return "";
    }
  }

  private static void initialize() {
    if (!isInitialized()) {
      Runnable worker = () -> {
        StatusBar statusBar = NearInfinity.getInstance().getStatusBar();
        String message = "Gathering creature and area names ...";
        String oldMessage = null;
        if (statusBar != null) {
          oldMessage = statusBar.getMessage();
          statusBar.setMessage(message);
        }

        try (final Threading threadPool = new Threading()) {
          List<ResourceEntry> files = ResourceFactory.getResources("CRE");
          // Including CHR resources to reduce number of warnings in IWD/IWD2 if NPC mods are installed
          files.addAll(ResourceFactory.getResources("CHR", Profile.getProperty(Profile.Key.GET_GAME_EXTRA_FOLDERS)));
          for (final ResourceEntry entry1 : files) {
            if (entry1 == null) {
              continue;
            }

            threadPool.submit(new CreWorker(entry1));
          }

          SCRIPT_NAMES_ARE.add("none"); // default script name for many CRE resources
          for (final ResourceEntry entry2 : ResourceFactory.getResources("ARE")) {
            if (entry2 == null) {
              continue;
            }

            threadPool.submit(new AreWorker(entry2));
          }

          for (final ResourceEntry entry3 : ResourceFactory.getResources("INI")) {
            if (entry3 == null) {
              continue;
            }

            threadPool.submit(new IniWorker(entry3));
          }

          threadPool.shutdown();
          try {
            threadPool.awaitTermination(60, TimeUnit.SECONDS);
          } catch (InterruptedException e) {
            Logger.error(e);
          }
        } catch (Exception e) {
          Logger.trace(e);
        }

        if (statusBar != null && statusBar.getMessage().startsWith(message)) {
          statusBar.setMessage(oldMessage);
        }

        initialized = true;
      };
      new Thread(worker).start();
    }
  }

  // -------------------------- INNER CLASSES --------------------------

  private static final class CreWorker implements Runnable {
    final ResourceEntry entry;

    public CreWorker(ResourceEntry entry) {
      this.entry = entry;
    }

    @Override
    public void run() {
      try {
        CreResource.addScriptName(SCRIPT_NAMES_CRE, entry);
      } catch (Exception e) {
        Logger.error(e);
      }
    }
  }

  private static final class AreWorker implements Runnable {
    final ResourceEntry entry;

    public AreWorker(ResourceEntry entry) {
      this.entry = entry;
    }

    @Override
    public void run() {
      try {
        AreResource.addScriptNames(SCRIPT_NAMES_ARE, entry.getResourceBuffer());
      } catch (Exception e) {
        Logger.error(e);
      }
    }
  }

  private static final class IniWorker implements Runnable {
    final ResourceEntry entry;

    public IniWorker(ResourceEntry entry) {
      this.entry = entry;
    }

    @Override
    public void run() {
      try {
        final String name = entry.getResourceName();
        if (name.length() >= 10 && ResourceFactory.resourceExists(name.replace(".INI", ".ARE"))) {
          final IniMap ini = IniMapCache.get(entry);
          if (ini != null) {
            for (final IniMapSection section : ini) {
              final IniMapEntry mapEntry = section.getEntry("script_name");
              if (mapEntry != null) {
                final String s = normalized(mapEntry.getValue());
                if (!s.isEmpty() && s.charAt(0) != '[') {
                  synchronized (SCRIPT_NAMES_ARE) {
                    SCRIPT_NAMES_ARE.add(s);
                  }
                }
              }
            }
          }
        }
      } catch (Exception e) {
        Logger.error(e);
      }
    }
  }
}
