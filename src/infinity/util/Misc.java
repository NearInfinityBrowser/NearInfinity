package infinity.util;

/**
 * A general-purpose class containing useful function not fitting elsewhere.
 */
public class Misc
{

  /**
   * Replaces any file extension with the specified one.
   * @param fileName The original filename.
   * @param newExt The new file extension (specified without period).
   * @return The modified filename.
   */
  public static String replaceFileExtension(String fileName, String newExt)
  {
    if (fileName != null) {
      newExt = (newExt == null) ? "" : "." + newExt;
      int pos = fileName.lastIndexOf('.');
      if (pos >= 0) {
        fileName = fileName.substring(0, pos) + newExt;
      } else {
        fileName = fileName + newExt;
      }
    }
    return fileName;
  }

  /**
   * Replaces the file extension only if the old extension matches oldExt.
   * @param fileName The original filename.
   * @param oldExt The file extension to replace (specified without period).
   * @param newExt The new file extension (specified without period).
   * @return The modified filename.
   */
  public static String replaceFileExtension(String fileName, String oldExt, String newExt)
  {
    if (fileName != null) {
      if (oldExt == null) {
        oldExt = "";
      }
      newExt = (newExt == null) ? "" : "." + newExt;
      int pos = fileName.lastIndexOf('.');
      if (pos >= 0) {
        if (fileName.substring(pos+1).equalsIgnoreCase(oldExt)) {
          fileName = fileName.substring(0, pos) + newExt;
        }
      } else if (oldExt.isEmpty()) {
        fileName = fileName + newExt;
      }
    }
    return fileName;
  }


  // Contains static functions only
  private Misc() {}
}
