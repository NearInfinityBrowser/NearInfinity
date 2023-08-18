// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.infinity.NearInfinity;
import org.infinity.gui.menu.BrowserMenuBar;
import org.infinity.resource.Profile;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.itm.ItmResource;
import org.infinity.resource.other.EffResource;
import org.infinity.resource.other.VvcResource;
import org.infinity.resource.pro.ProResource;
import org.infinity.resource.spl.SplResource;
import org.infinity.resource.sto.StoResource;
import org.infinity.resource.text.PlainTextResource;
import org.infinity.search.SearchOptions;
import org.infinity.util.io.FileEx;
import org.infinity.util.io.StreamUtils;

public abstract class ResourceEntry implements Comparable<ResourceEntry> {
  // list of file extensions not shown in the resource tree
  private static final HashSet<String> SKIPPED_EXTENSIONS = new HashSet<>();

  static {
    SKIPPED_EXTENSIONS.add("BAK");
    SKIPPED_EXTENSIONS.add("BIF");
  }

  private String searchString;

  static int[] getLocalFileInfo(Path file) {
    if (file != null && FileEx.create(file).isFile()) {
      try (SeekableByteChannel ch = Files.newByteChannel(file, StandardOpenOption.READ)) {
        ByteBuffer bb = StreamUtils.getByteBuffer((int) ch.size());
        if (ch.read(bb) < ch.size()) {
          throw new IOException();
        }
        bb.position(0);

        String sig, ver;
        if (bb.remaining() >= 8) {
          byte[] buf = new byte[4];
          bb.get(buf);
          sig = new String(buf);
          bb.get(buf);
          ver = new String(buf);
        } else {
          sig = ver = "";
        }
        if ("TIS ".equals(sig) && "V1  ".equals(ver)) {
          if (bb.limit() > 16) {
            int v1 = bb.getInt(8);
            int v2 = bb.getInt(12);
            return new int[] { v1, v2 };
          } else {
            throw new IOException("Unexpected end of file");
          }
        } else if (file.getFileName().toString().toUpperCase(Locale.ENGLISH).endsWith(".TIS")) {
          int tileSize = 0;
          if (bb.remaining() > 16) {
            tileSize = bb.getInt(12);
          }
          if (tileSize > 0) {
            int tileCount = bb.limit() / tileSize;
            return new int[] { tileCount, tileSize };
          } else {
            throw new Exception("Invalid TIS tile size");
          }
        } else {
          return new int[] { (int) ch.size() };
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public int hashCode() {
    return Objects.hash(searchString);
  }

  @Override
  public boolean equals(Object o) {
    return equals(o, false);
  }

  /**
   * Indicates whether the specified object argument is equal to this one.
   *
   * @param o     the reference object with which to compare.
   * @param exact whether to compare path in addition to resource name.
   * @return {@code true} if this object is the same as the obj argument; {@code false} otherwise.
   */
  public boolean equals(Object o, boolean exact) {
    if (o == this) {
      return true;
    } else if (o instanceof ResourceEntry) {
      ResourceEntry entry = (ResourceEntry) o;
      boolean bRet = getResourceName().equalsIgnoreCase(entry.getResourceName());
      if (bRet && exact) {
        if (getActualPath() != entry.getActualPath()) {
          if (getActualPath() != null) {
            bRet = getActualPath().equals(entry.getActualPath());
          } else {
            bRet = entry.getActualPath().equals(getActualPath());
          }
        }
      }
      return bRet;
    }
    return false;
  }

  // --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(ResourceEntry entry) {
    if (entry == this) {
      return 0;
    } else {
      return getResourceName().compareToIgnoreCase(entry.getResourceName());
    }
  }

  // --------------------- End Interface Comparable ---------------------

  public Path getActualPath() {
    return getActualPath((NearInfinity.getInstance() != null) && BrowserMenuBar.getInstance().getOptions().ignoreOverrides());
  }

  public ImageIcon getIcon() {
    return ResourceFactory.getKeyfile().getIcon(getExtension());
  }

  public long getResourceSize() {
    return getResourceSize((NearInfinity.getInstance() != null) && BrowserMenuBar.getInstance().getOptions().ignoreOverrides());
  }

  public ByteBuffer getResourceBuffer() throws Exception {
    return getResourceBuffer((NearInfinity.getInstance() != null) && BrowserMenuBar.getInstance().getOptions().ignoreOverrides());
  }

  public InputStream getResourceDataAsStream() throws Exception {
    return getResourceDataAsStream(
        (NearInfinity.getInstance() != null) && BrowserMenuBar.getInstance().getOptions().ignoreOverrides());
  }

  public int[] getResourceInfo() throws Exception {
    return getResourceInfo((NearInfinity.getInstance() != null) && BrowserMenuBar.getInstance().getOptions().ignoreOverrides());
  }

  /**
   * Returns localized name of the resource. This string used in game interface.
   */
  public String getSearchString() {
    if (searchString == null) {
      try {
        String extension = getExtension().toUpperCase();
        if (extension.equals("CRE") || extension.equals("CHR")) {
          try (InputStream is = getResourceDataAsStream()) {
            searchString = CreResource.getSearchString(is);
          }
        } else if (extension.equals("ITM")) {
          try (InputStream is = getResourceDataAsStream()) {
            searchString = ItmResource.getSearchString(is);
          }
        } else if (extension.equals("SPL")) {
          try (InputStream is = getResourceDataAsStream()) {
            searchString = SplResource.getSearchString(is);
          }
        } else if (extension.equals("STO")) {
          try (InputStream is = getResourceDataAsStream()) {
            searchString = StoResource.getSearchString(is);
          }
        } else if (extension.equals("ARE")) {
          searchString = AreResource.getSearchString(this);
        } else if (extension.equals("PRO")) {
          searchString = ProResource.getSearchString(this);
        } else if (extension.equals("INI")) {
          searchString = PlainTextResource.getSearchString(this);
        }
      } catch (Exception e) {
        if ((NearInfinity.getInstance() != null) && !BrowserMenuBar.getInstance().getOptions().ignoreReadErrors()) {
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error reading " + toString(), "Error",
              JOptionPane.ERROR_MESSAGE);
        }
        searchString = "Error";
        e.printStackTrace();
      }
    }
    return searchString;
  }

  /**
   * Returns whether the current resource matches all of the search options specified in the SearchOptions argument.
   *
   * @param searchOptions Contains the options to check the resource against.
   * @return {@code true} if all options are matching, {@code false} otherwise.
   */
  public boolean matchSearchOptions(SearchOptions searchOptions) {
    if (searchOptions != null && !searchOptions.isEmpty()) {
      if ("ARE".equalsIgnoreCase(searchOptions.getResourceType())) {
        return AreResource.matchSearchOptions(this, searchOptions);
      } else if ("CRE".equalsIgnoreCase(searchOptions.getResourceType())) {
        return CreResource.matchSearchOptions(this, searchOptions);
      } else if ("EFF".equalsIgnoreCase(searchOptions.getResourceType())) {
        return EffResource.matchSearchOptions(this, searchOptions);
      } else if ("ITM".equalsIgnoreCase(searchOptions.getResourceType())) {
        return ItmResource.matchSearchOptions(this, searchOptions);
      } else if ("PRO".equalsIgnoreCase(searchOptions.getResourceType())) {
        return ProResource.matchSearchOptions(this, searchOptions);
      } else if ("SPL".equalsIgnoreCase(searchOptions.getResourceType())) {
        return SplResource.matchSearchOptions(this, searchOptions);
      } else if ("STO".equalsIgnoreCase(searchOptions.getResourceType())) {
        return StoResource.matchSearchOptions(this, searchOptions);
      } else if ("VVC".equalsIgnoreCase(searchOptions.getResourceType())) {
        return VvcResource.matchSearchOptions(this, searchOptions);
      }
    }
    return false;
  }

  /**
   * Indicates whether the resource is visible for Near Infinity.
   */
  public boolean isVisible() {
    // Visibility conditions:
    // 1. Options->Show Unknown Resource Types == true OR resource type is supported
    // 2. NOT Resource type part of skippedExtensions
    // 3. Filename length is valid
    int resLen = getResourceRef().length();
    boolean bRet = (BrowserMenuBar.isInstantiated() && BrowserMenuBar.getInstance().getOptions().showUnknownResourceTypes())
        || Profile.isResourceTypeSupported(getExtension())
            && !SKIPPED_EXTENSIONS.contains(getExtension().toUpperCase(Locale.ENGLISH)) && (resLen >= 0 && resLen <= 8);
    return bRet;
  }

  protected abstract Path getActualPath(boolean ignoreOverride);

  public abstract long getResourceSize(boolean ignoreOverride);

  /** Returns the type of the resource (extension without leading dot). */
  public abstract String getExtension();

  public abstract ByteBuffer getResourceBuffer(boolean ignoreOverride) throws Exception;

  public abstract InputStream getResourceDataAsStream(boolean ignoreOverride) throws Exception;

  public abstract int[] getResourceInfo(boolean ignoreOverride) throws Exception;

  /** Returns the full resource name (name dot extension) */
  public abstract String getResourceName();

  /** Returns the resource name without extension. */
  public abstract String getResourceRef();

  /** Returns name of folder in the resource tree in which this entry appears. */
  public abstract String getTreeFolderName();

  public abstract ResourceTreeFolder getTreeFolder();

  public abstract boolean hasOverride();

  public boolean isSound() {
    String extension = getExtension().toUpperCase();

    return extension.equals("WAV") || extension.equals("MUS") || extension.equals("ACM");
  }
}
