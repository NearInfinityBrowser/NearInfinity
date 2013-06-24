package infinity.util;

import java.io.*;

public class FileInputStreamCI extends FileInputStream {
  public FileInputStreamCI (String s) throws FileNotFoundException {
    super(s.toLowerCase());
  }

  public FileInputStreamCI (File f) throws FileNotFoundException {
    super(f);
  }
}
