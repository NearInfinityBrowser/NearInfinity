// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.updater;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Expands a {@link URL} by optional parameters that can be added, either by path or by query string.
 */
public class ParamURL {
  private final List<String> queryList = new ArrayList<>();
  private final List<String> pathList = new ArrayList<>();

  private URL url;

  public ParamURL(String url) throws Exception {
    parse(url);
  }

  /** Returns the {@link URL} part of the parameterized URL. */
  public URL getUrl() {
    return url;
  }

  /** Returns a read-only list of the path parameters. */
  public List<String> getPaths() {
    return Collections.unmodifiableList(pathList);
  }

  /** Returns a read-only list of the query parameters. */
  public List<String> getQueries() {
    return Collections.unmodifiableList(queryList);
  }

  @Override
  public String toString() {
    String paths = pathList.stream().map(s -> "/" + s).collect(Collectors.joining());
    String queries = String.join("&", queryList);
    String result = url.toExternalForm() + paths;
    if (!queries.isEmpty()) {
      result += "?" + queries;
    }
    return result;
  }

  private void parse(String url) throws MalformedURLException {
    final Pattern regUrl = Pattern.compile("^[^{]+");
    final Pattern regParam = Pattern.compile("\\{([^}]*)\\}");

    Matcher m = regUrl.matcher(url);
    if (m.find()) {
      this.url = new URL(m.group());
    }

    m = regParam.matcher(url);
    while (m.find()) {
      String param = m.group(1);
      if (param.charAt(0) == '/') {
        // path
        pathList.add(param.substring(1));
      } else if (param.charAt(0) == '?') {
        String[] items = param.substring(1).split(",");
        queryList.addAll(Arrays.asList(items));
      } else {
        throw new IllegalArgumentException("Not a valid parameterized URL: " + url);
      }
    }
  }
}
