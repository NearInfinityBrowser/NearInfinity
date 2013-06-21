package infinity.util;

import java.io.*;

public class FileReaderCI extends FileReader {
  public FileReaderCI (String s) throws FileNotFoundException {
    super(s.toLowerCase());
  }

  public FileReaderCI (File f) throws FileNotFoundException {
    super(f);
  }
}
