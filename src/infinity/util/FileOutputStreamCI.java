package infinity.util;

import java.io.*;

public class FileOutputStreamCI extends FileOutputStream {
  public FileOutputStreamCI (String s) throws FileNotFoundException {
    super(s.toLowerCase());
  }

  public FileOutputStreamCI (File f) throws FileNotFoundException {
    super(f);
  }
}
