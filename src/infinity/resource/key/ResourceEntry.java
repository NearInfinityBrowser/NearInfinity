// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package infinity.resource.key;

import infinity.NearInfinity;
import infinity.gui.BrowserMenuBar;
import infinity.resource.ResourceFactory;
import infinity.resource.are.AreResource;
import infinity.resource.cre.CreResource;
import infinity.resource.itm.ItmResource;
import infinity.resource.other.EffResource;
import infinity.resource.other.VvcResource;
import infinity.resource.pro.ProResource;
import infinity.resource.spl.SplResource;
import infinity.resource.sto.StoResource;
import infinity.search.SearchOptions;
import infinity.util.DynamicArray;
import infinity.util.Filereader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.ImageIcon;
import javax.swing.JOptionPane;

public abstract class ResourceEntry implements Comparable<ResourceEntry>
{
  private String searchString;

  static int[] getLocalFileInfo(File file)
  {
    if (file.getName().toUpperCase().endsWith(".TIS")) {
      try {
        InputStream is = new BufferedInputStream(new FileInputStream(file));
        byte data[] = Filereader.readBytes(is, 24);
        is.close();
        if (!new String(data, 0, 4).equalsIgnoreCase("TIS ")) {
          int tilesize = 64 * 64 + 4 * 256;
          int tilecount = (int)file.length() / tilesize;
          return new int[]{tilecount, tilesize};
        }
        return new int[]{DynamicArray.getInt(data, 8), DynamicArray.getInt(data, 12)};
      } catch (IOException e) {
        e.printStackTrace();
      }
      return null;
    }
    return new int[]{(int)file.length()};
  }

// --------------------- Begin Interface Comparable ---------------------

  @Override
  public int compareTo(ResourceEntry entry)
  {
    return getResourceName().compareToIgnoreCase(entry.getResourceName());
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

  protected abstract File getActualFile(boolean ignoreoverride);

  public abstract String getExtension();

  public abstract byte[] getResourceData(boolean ignoreoverride) throws Exception;

  protected abstract InputStream getResourceDataAsStream(boolean ignoreoverride) throws Exception;

  public abstract int[] getResourceInfo(boolean ignoreoverride) throws Exception;

  public abstract String getResourceName();

  public abstract String getTreeFolder();

  public abstract boolean hasOverride();
}