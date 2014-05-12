package infinity.gui;

import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

/**
 * A simple wrapper around {@link RSyntaxTextArea} to extend certain functionalities.
 * @author argent77
 */
public class InfinityTextArea extends RSyntaxTextArea
{

  public InfinityTextArea()
  {
    super();
  }

  public InfinityTextArea(RSyntaxDocument doc)
  {
    super(doc);
  }

  public InfinityTextArea(String text)
  {
    super(text);
  }

  public InfinityTextArea(int textMode)
  {
    super(textMode);
  }

  public InfinityTextArea(int rows, int cols)
  {
    super(rows, cols);
  }

  public InfinityTextArea(String text, int rows, int cols)
  {
    super(text, rows, cols);
  }

  public InfinityTextArea(RSyntaxDocument doc, String text, int rows, int cols)
  {
    super(doc, text, rows, cols);
  }

  @Override
  public void setText(String text)
  {
    // removes carriage return characters from line separators
    if (text != null) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < text.length(); i++) {
        char c = text.charAt(i);
        if (c != '\r') {
          sb.append(c);
        }
      }
      super.setText(sb.toString());
    } else {
      super.setText(null);
    }
  }
}
