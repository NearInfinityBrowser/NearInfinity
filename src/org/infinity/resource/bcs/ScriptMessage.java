// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.bcs;

import org.infinity.resource.bcs.parser.Token;

/**
 * Contains information to pinpoint issues in script code or sources.
 */
public class ScriptMessage implements Comparable<ScriptMessage> {
  private final String message;
  private final Token firstToken, lastToken;
  private final int line;
  private final int col;

  public ScriptMessage(String message) {
    this(message, null);
  }

  public ScriptMessage(String message, Token token) {
    this(message, token, token);
  }

  public ScriptMessage(String message, Token firstToken, Token lastToken) {
    this.message = (message != null) ? message : "";
    this.firstToken = firstToken;
    this.lastToken = lastToken;
    this.line = this.col = -1;
  }

  public ScriptMessage(String message, int line) {
    this(message, line, 1);
  }

  public ScriptMessage(String message, int line, int col) {
    this.message = (message != null) ? message : "";
    this.firstToken = null;
    this.lastToken = null;
    this.line = line;
    this.col = col;
  }

  public String getMessage() {
    return message;
  }

  public boolean hasToken() {
    return (firstToken != null);
  }

  public Token getFirstToken() {
    return firstToken;
  }

  public Token getLastToken() {
    return lastToken;
  }

  public int getLine() {
    return (firstToken != null) ? firstToken.beginLine : line;
  }

  public int getColumn() {
    return (firstToken != null) ? firstToken.beginColumn : col;
  }

  public int getEndLine() {
    return (lastToken != null) ? lastToken.endLine : line;
  }

  public int getEndColumn() {
    return (lastToken != null) ? lastToken.endColumn : col;
  }

  @Override
  public String toString() {
    String retVal = getMessage();
    int line = getLine();
    int col = getColumn();
    if (line > 0) {
      retVal += " [line: " + line;
      if (col > 0) {
        retVal += ", col: " + col;
      }
      retVal += "]";
    }

    return retVal;
  }

  @Override
  public int compareTo(ScriptMessage o) {
    int retVal = getLine() - o.getLine();
    if (retVal == 0) {
      retVal = getColumn() - o.getColumn();
      if (retVal == 0) {
        retVal = getMessage().compareTo(o.getMessage());
      }
    }
    return retVal;
  }

}
