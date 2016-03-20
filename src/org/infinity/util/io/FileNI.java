// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.util.io;

import java.io.File;
import java.net.URI;
import java.util.List;

/**
 * Overrides java.io.File to support case sensitive file systems.
 * Contains additional static methods for retrieving files from multiple search paths.
 */
public class FileNI extends File
{
  public FileNI(String pathname)
  {
    super(FileLookup.getInstance().queryFilePath(pathname));
  }

  public FileNI(URI uri)
  {
    // URI not officially supported
    super(uri);
  }

  public FileNI(String parent, String child)
  {
    super(FileLookup.getInstance().queryFilePath(new File(parent, child)));
  }

  public FileNI(File parent, String child)
  {
    super(FileLookup.getInstance().queryFilePath(new File(parent, child)));
  }


  public static File getFile(String pathname)
  {
    return new FileNI(pathname);
  }

  public static File getFile(URI uri)
  {
    return new FileNI(uri);
  }

  public static File getFile(String parent, String child)
  {
    return new FileNI(parent, child);
  }

  public static File getFile(File parent, String child)
  {
    return new FileNI(parent, child);
  }

  @Override
  public boolean delete()
  {
    FileLookup.getInstance().remove(this);
    return super.delete();
  }

  @Override
  public boolean mkdir()
  {
    boolean retVal = super.mkdir();
    if (retVal) {
      FileLookup.getInstance().add(this);
    }
    return retVal;
  }

  @Override
  public boolean mkdirs()
  {
    boolean retVal = super.mkdirs();
    if (retVal) {
      FileLookup.getInstance().add(this);
    }
    return retVal;
  }

  @Override
  public boolean renameTo(File dest)
  {
    String src = getPath();
    boolean retVal = super.renameTo(dest);
    if (retVal) {
      FileLookup.getInstance().rename(src, dest.getPath());
    }
    return retVal;
  }

  /**
   * Returns an abstract pathname from the first existing full pathname consisting of an entry
   * from parentList and child. If no existing pathname can been found, then the last
   * parentList entry will be used to create an abstract pathname.
   * @param parentList List of potential pathnames to use.
   * @param child The child pathname string.
   * @return An abstract pathname representation.
   * @throws NullPointerException if parentList or the resulting pathname are empty
   */
  public static File getFile(List<?> parentList, String child)
  {
    if (parentList != null) {
      return getFile(parentList.toArray(), child);
    } else {
      return null;
    }
  }

  /**
   * Returns an abstract pathname from the first existing full pathname consisting of an entry
   * from parentList and child. If no existing pathname can been found, then the last
   * parentList entry will be used to create an abstract pathname.
   * @param parentList Array of potential pathnames to use.
   * @param child The child pathname string.
   * @return An abstract pathname representation.
   * @throws NullPointerException if parentList or the resulting pathname are empty
   */
  public static File getFile(Object[] parentList, String child)
  {
    File file = null;
    if (parentList != null) {
      for (final Object parent: parentList) {
        File tmp = new FileNI(parent.toString(), child);
        if (tmp.exists()) {
          file = tmp;
          break;
        }
      }
    }
    if (file == null) {
      if (parentList != null && parentList.length > 0) {
        file = new FileNI(parentList[parentList.length - 1].toString(), child);
      } else
        throw new NullPointerException("Empty parent list");
    }
    return file;
  }
}
