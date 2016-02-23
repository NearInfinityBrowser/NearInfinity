// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.key;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Locale;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

import org.infinity.NearInfinity;
import org.infinity.gui.BrowserMenuBar;
import org.infinity.resource.ResourceFactory;
import org.infinity.resource.are.AreResource;
import org.infinity.resource.cre.CreResource;
import org.infinity.resource.itm.ItmResource;
import org.infinity.resource.other.EffResource;
import org.infinity.resource.other.VvcResource;
import org.infinity.resource.pro.ProResource;
import org.infinity.resource.spl.SplResource;
import org.infinity.resource.sto.StoResource;
import org.infinity.search.SearchOptions;
import org.infinity.util.DynamicArray;
import org.infinity.util.io.FileInputStreamNI;
import org.infinity.util.io.FileReaderNI;

public abstract class ResourceEntry implements Comparable<ResourceEntry>
{
  // list of file extensions not shown in the resource tree
  private static final HashSet<String> skippedExtensions = new HashSet<String>();

  static {
    skippedExtensions.add("BAK");
    skippedExtensions.add("BIF");
  }

  private String searchString;

  static int[] getLocalFileInfo(File file)
  {
    try {
      InputStream is = new BufferedInputStream(new FileInputStreamNI(file));
      byte[] data = null;
      try {
        data = FileReaderNI.readBytes(is, 16);
      } finally {
        is.close();
      }
      if (data != null) {
        String sig = new String(data, 0, 4);
        String ver = new String(data, 4, 4);
        if ("TIS ".equals(sig) && "V1  ".equals(ver)) {
          return new int[]{ DynamicArray.getInt(data, 8), DynamicArray.getInt(data, 12) };
        } else if (file.getName().toUpperCase(Locale.ENGLISH).endsWith(".TIS")) {
          int tileSize = 64 * 64 + 4 * 256;
          int tileCount = (int)file.length() / tileSize;
          return new int[]{ tileCount, tileSize };
        } else {
          return new int[]{ (int)file.length() };
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o == this) {
      return true;
    } else if (o instanceof ResourceEntry) {
      return getResourceName().equalsIgnoreCase(((ResourceEntry)o).getResourceName());
    }
    return false;
  }

// --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(ResourceEntry entry)
  {
    if (entry == this) {
      return 0;
    } else {
      return getResourceName().compareToIgnoreCase(entry.getResourceName());
    }
  }

// --------------------- End Interface Comparable ---------------------

  public File getActualFile()
  {
    return getActualFile(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public ImageIcon getIcon()
  {
    return ResourceFactory.getKeyfile().getIcon(getExtension());
  }

  public byte[] getResourceData() throws Exception
  {
    return getResourceData(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public InputStream getResourceDataAsStream() throws Exception
  {
    return getResourceDataAsStream(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public int[] getResourceInfo() throws Exception
  {
    return getResourceInfo(
            NearInfinity.getInstance() != null && BrowserMenuBar.getInstance().ignoreOverrides());
  }

  public String getSearchString()
  {
    if (searchString == null) {
      try {
        String extension = getExtension();
        if (extension.equalsIgnoreCase("CRE"))
          searchString = CreResource.getSearchString(getResourceData());
        else if (extension.equalsIgnoreCase("ITM"))
          searchString = ItmResource.getSearchString(getResourceData());
        else if (extension.equalsIgnoreCase("SPL"))
          searchString = SplResource.getSearchString(getResourceData());
        else if (extension.equalsIgnoreCase("STO"))
          searchString = StoResource.getSearchString(getResourceData());
      } catch (Exception e) {
        if (NearInfinity.getInstance() != null && !BrowserMenuBar.getInstance().ignoreReadErrors())
          JOptionPane.showMessageDialog(NearInfinity.getInstance(), "Error reading " + toString(), "Error",
                                        JOptionPane.ERROR_MESSAGE);
        searchString = "Error";
        e.printStackTrace();
      }
    }
    return searchString;
  }

  /**
   * Returns whether the current resource matches all of the search options specified in the
   * SearchOptions argument.
   * @param searchOptions Contains the options to check the resource against.
   * @return <code>true</code> if all options are matching, <code>false</code> otherwise.
   */
  public boolean matchSearchOptions(SearchOptions searchOptions)
  {
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
  public boolean isVisible()
  {
    return !skippedExtensions.contains(getExtension().toUpperCase(Locale.ENGLISH));
  }

  protected abstract File getActualFile(boolean ignoreoverride);

  public abstract String getExtension();

  public abstract byte[] getResourceData(boolean ignoreoverride) throws Exception;

  protected abstract InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception;

  public abstract int[] getResourceInfo(boolean ignoreoverride) throws Exception;

  public abstract String getResourceName();

  public abstract String getTreeFolder();

  public abstract boolean hasOverride();
}