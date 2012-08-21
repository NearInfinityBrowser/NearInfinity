package infinity.util;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * @author Fredrik Lindgren <lindgren.fredrik@gmail.com>
 * @since  2012-08-20
 */
public class StreamDiscarder implements Runnable
{
  private InputStream in;

  /**
   * Binds the argument stream to an instance variable
   *
   * @param in the input stream to be read from
   */
  public void setStream(InputStream in)
  {
    this.in = in;
  }

  /**
   * Reads from the provided input stream
   *
   * @return void
   */
  public void run()
  {
    try {
      InputStreamReader r = new InputStreamReader(in);
      BufferedReader b = new BufferedReader(r);
      String s = null;
      while ((s = b.readLine()) != null) {}
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
