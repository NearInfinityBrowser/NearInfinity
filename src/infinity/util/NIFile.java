package infinity.util;

import infinity.util.FileCI;

import java.io.File;
import java.net.URI;

/**
 * @author argent77
 * @since 2013-08-22
 */
public class NIFile
{
  public static File getFile(String pathname)
  {
    return new FileCI(pathname);
  }

  public static File getFile(URI uri)
  {
    return new File(uri);
  }

  public static File getFile(String parent, String child)
  {
    return new FileCI(parent, child);
  }

  public static File getFile(File parent, String child)
  {
    return new FileCI(parent, child);
  }

  /**
   * Returns an abstract pathname from the first existing full pathname consisting of an entry from parentList and
   * child. If no existing pathname can been found, then the last parentList entry will be used to create an abstract
   * pathname.
   * @param parentList List of potential pathnames to use.
   * @param child The child pathname string.
   * @return An abstract pathname representation.
   * @throws NullPointerException if parentList or the resulting pathname are empty
   */
  public static File getFile(String[] parentList, String child)
  {
    File file = null;
    if (parentList != null) {
      for (final String parent: parentList) {
        File tmp = new FileCI(parent, child);
        if (tmp.exists()) {
          file = tmp;
          break;
        }
      }
    }
    if (file == null) {
      if (parentList != null && parentList.length > 0) {
        file = new FileCI(parentList[parentList.length - 1], child);
      } else
        throw new NullPointerException("Empty parent list");
    }
    return file;
  }

  /**
   * Creates an abstract pathname from the first existing full pathname consisting of an entry from parentList and
   * child. If no existing pathname can been found, then the last parentList entry will be used to create an abstract
   * pathname.
   * @param parentList List of potential abstract pathnames to use.
   * @param child The child pathname string.
   * @return An abstract pathname representation.
   * @throws NullPointerException if parentList or the resulting pathname are empty
   */
  public static File getFile(File[] parentList, String child)
  {
    File file = null;
    if (parentList != null) {
      for (final File f: parentList) {
        File tmp = new FileCI(f, child);
        if (tmp.exists()) {
          file = tmp;
          break;
        }
      }
    }
    if (file == null) {
      if (parentList != null && parentList.length > 0) {
        file = new FileCI(parentList[parentList.length - 1], child);
      } else
        throw new NullPointerException("Empty parent list");
    }
    return file;
  }
}
