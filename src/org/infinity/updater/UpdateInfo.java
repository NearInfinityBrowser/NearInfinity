// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.updater;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.infinity.util.Misc;
import org.infinity.util.io.StreamUtils;
import org.infinity.util.tuples.Couple;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Provides access to update information stored in "update.xml".
 */
public class UpdateInfo {
  /** File type of data to be downloaded. */
  public enum FileType {
    /** Indicates an unknown or unsupported file type. */
    UNKNOWN,
    /** No further processing necessary. */
    ORIGINAL,
    /** Unpack first available file from the zip archive. */
    ZIP,
    /** Uncompress gzip file. */
    GZIP
  }

  // Supported node and attribute names
  private static final String NODE_UPDATE     = "update";
  private static final String NODE_GENERAL    = "general";
  private static final String NODE_RELEASE    = "release";
  private static final String NODE_SERVER     = "server";
  private static final String NODE_INFO       = "info";
  private static final String NODE_LINK       = "link";
  private static final String NODE_GITHUB     = "github";
  private static final String NODE_NAME       = "name";
  private static final String NODE_URL        = "url";
  private static final String ATTR_VERSION    = "version";
  private static final String ATTR_TYPE       = "type";

  private General general;
  private Release release;
  private int version;

  /**
   * Checks if the specified string contains valid update.xml data. <b>Note:</b> This is an expensive operation.
   *
   * @param s        The string to check.
   * @param systemId Base path for relative URIs (required for Doctype reference).
   * @return {@code true} if the string conforms to the update.xml specification, {@code false} otherwise.
   */
  public static boolean isValidXml(String s, String systemId) {
    try {
      return isValidXml(new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8.name())), systemId);
    } catch (UnsupportedEncodingException e) {
    }
    return false;
  }

  /**
   * Checks if the specified file contains valid update.xml data. <b>Note:</b> This is an expensive operation.
   *
   * @param f The file to read data from.
   * @return {@code true} if the file content conforms to the update.xml specification, {@code false} otherwise.
   */
  public static boolean isValidXml(Path f) {
    try (InputStream is = StreamUtils.getInputStream(f)) {
      return isValidXml(is, f.getParent().toAbsolutePath().toString());
    } catch (IOException e) {
    }
    return false;
  }

  /**
   * Checks if the specified input stream points to valid update.xml data. <b>Note:</b> This is an expensive operation.
   *
   * @param is       The input stream to read data from.
   * @param systemId Base path for relative URIs (required for Doctype reference).
   * @return {@code true} if the data from the stream conforms to the update.xml specification, {@code false} otherwise.
   */
  public static boolean isValidXml(InputStream is, String systemId) {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      dBuilder.setErrorHandler(new XmlErrorHandler());
      Document doc = dBuilder.parse(is, systemId);
      return NODE_UPDATE.equals(doc.getDocumentElement().getNodeName());
    } catch (Exception e) {
    }
    return false;
  }

  /**
   * Read update information from the given InputStream object.
   *
   * @param is       InputStream object pointing to the update data in XML format.
   * @param systemId Base path for relative URIs (required for Doctype reference).
   */
  public UpdateInfo(InputStream is, String systemId) throws Exception {
    parseXml(is, systemId);
  }

  /**
   * Read update information from the specified file.
   *
   * @param f The file containing update information in XML format.
   */
  public UpdateInfo(Path f) throws Exception {
    try (InputStream is = StreamUtils.getInputStream(f)) {
      parseXml(is, f.getParent().toAbsolutePath().toString());
    }
  }

  /**
   * Read update information from the specified string.
   *
   * @param s        The text string containing update information in XML format.
   * @param systemId Base path for relative URIs (required for Doctype reference).
   */
  public UpdateInfo(String s, String systemId) throws Exception {
    parseXml(new ByteArrayInputStream(s.getBytes("UTF-8")), systemId);
  }

  /**
   * Provides access to the General section of the update.xml.
   */
  public General getGeneral() {
    return general;
  }

  /**
   * Provides access to the the GitHub section specified by the user in the Server Settings or returns {@code null} if
   * not available.
   */
  public Release getRelease() {
    return release;
  }

  // Returns whether all mandatory fields have been initialized correctly.
  public boolean isValid() {
    boolean retVal = true;
    if (getGeneral() != null) {
      retVal &= getGeneral().isValid();
    }

    if (getRelease() == null) {
      retVal = false;
    }

    return retVal;
  }

  /**
   * Returns the specification version of the current update.xml. Any version > 0 is valid. Newer versions are supposed
   * to be backwards compatible.
   *
   * @return The specification version of the update.xml.
   */
  public int getUpdateInfoVersion() {
    return version;
  }

  private void parseXml(InputStream is, String systemId) throws Exception {
    if (is != null) {
      // reading XML data
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      dBuilder.setErrorHandler(new XmlErrorHandler());
      Document doc = dBuilder.parse(is, systemId);
      doc.getDocumentElement().normalize();

      DocumentType docType = doc.getDoctype();
      if (docType == null) {
        throw new Exception("Update.xml: No DTD specified");
      }

      // initializing data
      Element elemRoot = doc.getDocumentElement();
      if (!elemRoot.getNodeName().equals(NODE_UPDATE)) {
        throw new Exception("Update.xml: Unsupported root node name: " + elemRoot.getNodeName());
      }
      version = Misc.toNumber(elemRoot.getAttribute(ATTR_VERSION), 0);
      if (version < 2) {
        throw new Exception("Update.xml: Unsupported or missing specification version: " + version);
      }

      // initializing general section
      NodeList generalList = elemRoot.getElementsByTagName(NODE_GENERAL);
      for (int idx = 0, size = generalList.getLength(); idx < size; idx++) {
        Node n = generalList.item(idx);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
          // General section is optional
          general = parseGeneral((Element) n);
          break;
        }
      }

      // initializing release sections
      NodeList releaseList = elemRoot.getElementsByTagName(NODE_RELEASE);
      for (int idx = 0, size = releaseList.getLength(); idx < size; idx++) {
        Node n = releaseList.item(idx);
        if (n.getNodeType() == Node.ELEMENT_NODE) {
          final Element elemRelease = (Element) n;
          release = parseGitHub(elemRelease);
        }
      }
    }
  }

  /** Parses the child elements of the specified "general" node and return it as {@code General} instance. */
  private General parseGeneral(Element elemGeneral) throws Exception {
    if (elemGeneral == null || !elemGeneral.getNodeName().equals(NODE_GENERAL)) {
      throw new Exception("Update.xml: Node \"" + NODE_GENERAL + "\" expected");
    }

    List<String> serverList = new ArrayList<>();
    List<Couple<String, String>> infoList = new ArrayList<>();

    NodeList children = elemGeneral.getChildNodes();
    for (int idx = 0, size = children.getLength(); idx < size; idx++) {
      Node node = children.item(idx);
      if (node.getNodeType() != Node.ELEMENT_NODE) {
        continue;
      }
      if (node.getNodeName().equals(NODE_SERVER)) {
        NodeList list = node.getChildNodes();
        for (int j = 0, listSize = list.getLength(); j < listSize; j++) {
          node = list.item(j);
          if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }
          if (node.getNodeName().equals(NODE_LINK)) {
            try {
              URL url = new URL(node.getTextContent().trim());
              serverList.add(url.toExternalForm());
            } catch (MalformedURLException e) {
              // don't add invalid URLs
            }
          }
        }
      } else if (node.getNodeName().equals(NODE_INFO)) {
        NodeList list = node.getChildNodes();
        Node n1 = null, n2 = null;
        for (int j = 0, listSize = list.getLength(); j < listSize; j++) {
          Node n = list.item(j);
          if (n.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }
          if (n.getNodeName().equals(NODE_NAME)) {
            n1 = n;
          } else if (n.getNodeName().equals(NODE_LINK)) {
            n2 = n;
          }
        }
        if (n1 != null && n2 != null) {
          try {
            String name = n1.getTextContent().trim();
            URL url = new URL(n2.getTextContent().trim());
            infoList.add(Couple.with(name, url.toExternalForm()));
          } catch (MalformedURLException e) {
            // don't add invalid URLs
          }
        }
      }
    }

    return new General(serverList, infoList);
  }

  /** Parses the child elements of the specified "github" node and return it as {@code GitHubRelease} instance. */
  private Release parseGitHub(Element elemRelease) throws Exception {
    if (elemRelease == null || !elemRelease.getNodeName().equals(NODE_RELEASE)) {
      throw new Exception("Update.xml: Node \"" + NODE_RELEASE + "\" expected");
    }

    final List<String> jsonLinks = new ArrayList<>();

    // preprocessing available child elements in "release" section
    NodeList children = elemRelease.getChildNodes();
    Element elemGitHub = null;
    for (int i = 0, size = children.getLength(); i < size; i++) {
      Node node = children.item(i);
      if (node.getNodeType() == Node.ELEMENT_NODE) {
        if (node.getNodeName().equals(NODE_GITHUB)) {
          elemGitHub = (Element) node;
        }
      }
    }

    // processing required element "github"
    if (elemGitHub == null || !elemGitHub.getNodeName().equals(NODE_GITHUB)) {
      throw new Exception("Update.xml: Missing \"" + NODE_GITHUB + "\" node");
    }
    children = elemGitHub.getChildNodes();
    for (int idx = 0, size = children.getLength(); idx < size; idx++) {
      Element elem = null;
      if (children.item(idx).getNodeType() == Node.ELEMENT_NODE) {
        elem = (Element) children.item(idx);
      } else {
        continue;
      }
      if (elem.getNodeName().equals(NODE_URL)) {
        final String jsonType = elem.getAttribute(ATTR_TYPE).trim();
        if (jsonType.equalsIgnoreCase("json")) {
          final String s = elem.getTextContent().trim();
          if (!s.isEmpty()) {
            jsonLinks.add(elem.getTextContent().trim());
          }
        }
      }
    }

    return new Release(jsonLinks);
  }

  // -------------------------- INNER CLASSES --------------------------

  // Manages "General" information
  public static class General {
    private final List<String> servers = new ArrayList<>();
    private final List<Couple<String, String>> information = new ArrayList<>();

    private General(List<String> servers, List<Couple<String, String>> information) throws Exception {
      if (servers != null) {
        this.servers.addAll(servers);
      }
      if (information != null) {
        this.information.addAll(information);
      }
    }

    /** Returns number of available alternate update servers. */
    public int getServerCount() {
      return servers.size();
    }

    /** Returns the URL of the specified alternate server or {@code null} if not available. */
    public String getServer(int index) {
      if (index >= 0 && index < getServerCount()) {
        return servers.get(index);
      } else {
        return null;
      }
    }

    /** Returns number of available links to related websites. */
    public int getInformationCount() {
      return information.size();
    }

    /** Returns name of the specified related website or {@code null} if not available. */
    public String getInformationName(int index) {
      if (index >= 0 && index < getInformationCount()) {
        return information.get(index).getValue0();
      } else {
        return null;
      }
    }

    /** Returns URL of the specified related website or {@code null} if not available. */
    public String getInformationLink(int index) {
      if (index >= 0 && index < getInformationCount()) {
        return information.get(index).getValue1();
      } else {
        return null;
      }
    }

    /** Checks whether data has been initialized correctly. */
    public boolean isValid() {
      for (int i = 0, size = getServerCount(); i < size; i++) {
        String url = getServer(i);
        if (!Utils.isUrlValid(url)) {
          return false;
        }
      }
      for (int i = 0, size = getInformationCount(); i < size; i++) {
        String name = getInformationName(i);
        String url = getInformationLink(i);
        if (name == null || name.isEmpty() || !Utils.isUrlValid(url)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * Provides "Release" information parsed from a GitHub API JSON file.
   */
  public static class Release {
    /** List of assets attached to this release. */
    public final List<Asset> assets = new ArrayList<>();

    public String nodeId;
    /** Internal identifier of the release. */
    public int id;
    /** URL for this release information (JSON) */
    public URL url;
    /**
     * URL to assets information of this release (JSON).
     * Provides the same information as the {@code assets} attribute.
     */
    public URL assetsUrl;
    /** Parameterized URL for uploading assets to the release. */
    public ParamURL uploadUrl;
    /** URL to html version of the release (HTML) */
    public URL htmlUrl;
    /** Information about the release author. */
    public User author;
    /** Name of the release tag. */
    public String tagName;
    /** Specifies the commitish value that determines where the Git tag is created from. */
    public String target;
    /** Title of the release. */
    public String name;
    /** {@code true} to create a draft (unpublished) release, {@code false} to create a published one. */
    public boolean draft;
    /** Whether to identify the release as a prerelease or a full release. */
    public boolean prerelease;
    /** Timestamp when the release was created. */
    public OffsetDateTime createdAt;
    /** Timestamp when the release was published. */
    public OffsetDateTime publishedAt;
    /** URL to the automatically generated source tarball of the release. */
    public URL tarballUrl;
    /** URL to the automatically generated source zipball of the release. */
    public URL zipballUrl;
    /** Description text of the release, in Markdown format. */
    public String body;
    /** Information about social reaction for this release. */
    public Reactions reactions;

    public Release(List<String> jsonLinks) throws Exception {
      if (jsonLinks == null || jsonLinks.isEmpty()) {
        throw new Exception(String.format("Update.xml: Missing \"%s\" node in %s section", NODE_GITHUB, NODE_RELEASE));
      }

      for (final String link: jsonLinks) {
        final URL url = new URL(link);
        String json = Utils.downloadText(url, Updater.getInstance().getProxy(), StandardCharsets.UTF_8);
        parseJson(json);
        break;
      }
    }

    /**
     * Returns the default asset which points to the zipped {@code NearInfinity.jar} file.
     *
     * @return {@link Asset} instance of the zipped NearInfinity.jar file,
     *         or {@code null} if asset doesn't exist.
     */
    public Asset getDefaultAsset() {
      final String[] patterns = {
          // Matches:
          // NearInfinity-v2.3-20230618-1.zip
          // NearInfinity-v2.3.2023.6.18.zip
          // NearInfinity-20230618.zip
          // NearInfinity.zip
          "NearInfinity(-v?\\d+(\\.\\d+)*)?(-\\d+(-.+)?)?\\.zip",
          "NearInfinity\\.jar",
      };
      for (final Asset asset: assets) {
        for (final String pattern: patterns) {
          Matcher m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(asset.name);
          if (m.matches()) {
            return asset;
          }
        }
      }

      return null;
    }

    /** Parses the specified JSON string and initializes this class instance. */
    private void parseJson(String jsonString) throws Exception {
      // https://stleary.github.io/JSON-java/index.html
      JSONObject root = new JSONObject(jsonString);
      for (Iterator<String> iter = root.keys(); iter.hasNext(); ) {
        final String key = iter.next();
        switch (key) {
          case "url":
            url = new URL(root.getString(key));
            break;
          case "assets_url":
            assetsUrl = new URL(root.getString(key));
            break;
          case "upload_url":
            uploadUrl = new ParamURL(root.getString(key));
            break;
          case "html_url":
            htmlUrl = new URL(root.getString(key));
            break;
          case "id":
            id = root.getInt(key);
            break;
          case "author":
            author = new User(root.getJSONObject(key));
            break;
          case "node_id":
            nodeId = root.getString(key);
            break;
          case "tag_name":
            tagName = root.getString(key);
            break;
          case "target_commitish":
            target = root.getString(key);
            break;
          case "name":
            name = root.getString(key);
            break;
          case "draft":
            draft = root.getBoolean(key);
            break;
          case "prerelease":
            prerelease = root.getBoolean(key);
            break;
          case "created_at":
            createdAt = Utils.getDateTimeFromString(root.getString(key));
            break;
          case "published_at":
            publishedAt = Utils.getDateTimeFromString(root.getString(key));
            break;
          case "assets": {
            final JSONArray array = root.getJSONArray(key);
            for (int i = 0, len = array.length(); i < len; i++) {
              final Asset asset = new Asset(array.getJSONObject(i));
              assets.add(asset);
            }
            break;
          }
          case "tarball_url":
            tarballUrl = new URL(root.getString(key));
            break;
          case "zipball_url":
            zipballUrl = new URL(root.getString(key));
            break;
          case "body":
            body = root.optString(key);
            break;
          case "reactions":
            reactions = new Reactions(root.getJSONObject(key));
            break;
          default:
            System.out.printf("Release parser: Skipping unknown key \"%s\"\n", key);
        }
      }

      // validation
      final List<String> errors = new ArrayList<>();
      if (assetsUrl == null) {
        errors.add("Assets URL cannot be null");
      }
      if (uploadUrl == null) {
        errors.add("Upload URL cannot be null");
      }
      if (tarballUrl == null) {
        errors.add("Tar ball URL cannot be null");
      }
      if (zipballUrl == null) {
        errors.add("Zip ball URL cannot be null");
      }
      if (createdAt == null) {
        errors.add("Creation time cannot be null");
      }
      if (publishedAt == null) {
        errors.add("Publication time cannot be null");
      }
      if (nodeId == null) {
        errors.add("Node ID cannot be null");
      }
      if (author == null) {
        errors.add("Author cannot be null");
      }
      if (htmlUrl == null) {
        errors.add("HTML URL cannot be null");
      }
      if (name == null) {
        errors.add("Release name cannot be null");
      }
      if (tagName == null) {
        errors.add("Tag name cannot be null");
      }
      if (target == null) {
        errors.add("Target cannot be null");
      }
      if (url == null) {
        errors.add("Self URL cannot be null");
      }

      if (!errors.isEmpty()) {
        if (errors.size() == 1) {
          throw new NullPointerException(errors.get(0));
        } else {
          throw new NullPointerException("Update information are currently not available.");
        }
      }
    }
  }

  /** Provides GitHub user information. */
  public static class User {
    public String nodeId;
    /** Internal identifier of the user. */
    public int id;
    /** Login name of the user. */
    public String login;
    /** URL to the user's avatar graphics. */
    public URL avatarUrl;
    public String gravatarId;
    /** URL to user information (JSON) */
    public URL url;
    /** URL to user front page (HTML). */
    public URL htmlUrl;
    /** URL to follower of user (JSON) */
    public URL followersUrl;
    /** Parameterized URL to the user following this user (JSON) */
    public ParamURL followingUrl;
    /** Parameterized URL to user's gists information (JSON) */
    public ParamURL gistsUrl;
    /** Parameterized URL to user's starred information (JSON) */
    public ParamURL starredUrl;
    /** URL to user's subscriptions (watching) information (JSON) */
    public URL subscriptionsUrl;
    /** URL to user's organizations information (JSON) */
    public URL organizationsUrl;
    /**  URL to user's own repositories information (JSON) */
    public URL reposUrl;
    /** Parameterized URL to user's events information (JSON) */
    public URL eventsUrl;
    /** URL to user's received events information (JSON) */
    public URL receivedEventsUrl;
    /** The user type. */
    public String type;
    public boolean siteAdmin;

    private User(JSONObject jsonUser) throws Exception {
      JSONObject root = Objects.requireNonNull(jsonUser, "JSON user object cannot be null");
      for (Iterator<String> iter = root.keys(); iter.hasNext(); ) {
        final String key = iter.next();
        switch (key) {
          case "login":
            login = root.getString(key);
            break;
          case "id":
            id = root.getInt(key);
            break;
          case "node_id":
            nodeId = root.getString(key);
            break;
          case "avatar_url":
            avatarUrl = new URL(root.getString(key));
            break;
          case "gravatar_id":
            gravatarId = root.getString(key);
            break;
          case "url":
            url = new URL(root.getString(key));
            break;
          case "html_url":
            htmlUrl = new URL(root.getString(key));
            break;
          case "followers_url":
            followersUrl = new URL(root.getString(key));
            break;
          case "following_url":
            followingUrl = new ParamURL(root.getString(key));
            break;
          case "gists_url":
            gistsUrl = new ParamURL(root.getString(key));
            break;
          case "starred_url":
            starredUrl = new ParamURL(root.getString(key));
            break;
          case "subscriptions_url":
            subscriptionsUrl = new URL(root.getString(key));
            break;
          case "organizations_url":
            organizationsUrl = new URL(root.getString(key));
            break;
          case "repos_url":
            reposUrl = new URL(root.getString(key));
            break;
          case "events_url":
            eventsUrl = new URL(root.getString(key));
            break;
          case "received_events_url":
            receivedEventsUrl = new URL(root.getString(key));
            break;
          case "type":
            type = root.getString(key);
            break;
          case "site_admin":
            siteAdmin = root.getBoolean(key);
            break;
          default:
            System.out.printf("User parser: Skipping unknown key \"%s\"\n", key);
        }
      }

      // validation
      final List<String> errors = new ArrayList<>();
      if (avatarUrl == null) {
        errors.add("Avatar URL cannot be null");
      }
      if (eventsUrl == null) {
        errors.add("Events URL cannot be null");
      }
      if (followersUrl == null) {
        errors.add("Followers URL cannot be null");
      }
      if (followingUrl == null) {
        errors.add("Following URL cannot be null");
      }
      if (gistsUrl == null) {
        errors.add("Gists URL cannot be null");
      }
      if (gravatarId == null) {
        errors.add("Gravatar ID cannot be null");
      }
      if (htmlUrl == null) {
        errors.add("HTML URL cannot be null");
      }
      if (nodeId == null) {
        errors.add("Node ID cannot be null");
      }
      if (login == null) {
        errors.add("Login name cannot be null");
      }
      if (organizationsUrl == null) {
        errors.add("Organizations URL cannot be null");
      }
      if (receivedEventsUrl == null) {
        errors.add("Received events URL cannot be null");
      }
      if (reposUrl == null) {
        errors.add("Repositories URL cannot be null");
      }
      if (starredUrl == null) {
        errors.add("Starred URL cannot be null");
      }
      if (subscriptionsUrl == null) {
        errors.add("Subscriptions URL cannot be null");
      }
      if (type == null) {
        errors.add("User type cannot be null");
      }
      if (url == null) {
        errors.add("Self URL cannot be null");
      }

      if (!errors.isEmpty()) {
        if (errors.size() == 1) {
          throw new NullPointerException(errors.get(0));
        } else {
          throw new NullPointerException("Update information are currently not available.");
        }
      }
    }
  }

  public static class Asset {
    /** Available states of the release asset. */
    public enum State {
      UPLOADED,
      OPEN,
    }

    public String nodeId;
    /** Internal identifier of the asset. */
    public int id;
    /** URL for this asset information (JSON) */
    public URL url;
    /** Filename of the asset. */
    public String name;
    /** An optional label for this asset. */
    public String label;
    /** User information about the uploader of this asset. */
    public User uploader;
    /** Content type of the asset. */
    public String contentType;
    /** State of the release asset. */
    public State state;
    /** File size of the asset, in bytes. */
    public long size;
    /** Total number of downloads initiated for this asset at the time of request. */
    public int downloadCount;
    /** Timestamp when the asset was created. */
    public OffsetDateTime createdAt;
    /** Timestamp when the asset was last updated. */
    public OffsetDateTime updatedAt;
    /** URL for the direct asset download. */
    public URL browserDownloadUrl;

    private Asset(JSONObject jsonAsset) throws Exception {
      JSONObject root = Objects.requireNonNull(jsonAsset, "JSON asset object cannot be null");
      for (Iterator<String> iter = root.keys(); iter.hasNext(); ) {
        final String key = iter.next();
        switch (key) {
          case "url":
            url = new URL(root.getString(key));
            break;
          case "id":
            id = root.getInt(key);
            break;
          case "node_id":
            nodeId = root.getString(key);
            break;
          case "name":
            name = root.getString(key);
            break;
          case "label":
            label = root.optString(key, "");
            break;
          case "uploader":
            uploader = new User(root.getJSONObject(key));
            break;
          case "content_type":
            contentType = root.getString(key);
            break;
          case "state": {
              switch (root.getString(key).toLowerCase()) {
                case "uploaded": state = State.UPLOADED; break;
                case "open": state = State.OPEN; break;
                default: throw new Exception(String.format("Asset parser: Unknown state value \"%s\"", key));
              }
              break;
          }
          case "size":
            size = root.getLong(key);
            break;
          case "download_count":
            downloadCount = root.getInt(key);
            break;
          case "created_at":
            createdAt = Utils.getDateTimeFromString(root.getString(key));
            break;
          case "updated_at":
            updatedAt = Utils.getDateTimeFromString(root.getString(key));
            break;
          case "browser_download_url":
            browserDownloadUrl = new URL(root.getString(key));
            break;
          default:
            System.out.printf("Asset parser: Skipping unknown key \"%s\"\n", key);
        }
      }

      // validation
      final List<String> errors = new ArrayList<>();
      if (name == null) {
        errors.add("Asset name cannot be null");
      }
      if (contentType == null) {
        errors.add("Content type cannot be null");
      }
      if (state == null) {
        errors.add("Asset state cannot be null");
      }
      if (url == null) {
        errors.add("Self URL cannot be null");
      }
      if (nodeId == null) {
        errors.add("Node ID cannot be null");
      }
      if (uploader == null) {
        errors.add("Uploader info cannot be null");
      }
      if (browserDownloadUrl == null) {
        errors.add("Browser download URL cannot be null");
      }
      if (createdAt == null) {
        errors.add("Creation time cannot be null");
      }
      if (updatedAt == null) {
        errors.add("Update time cannot be null");
      }

      if (!errors.isEmpty()) {
        if (errors.size() == 1) {
          throw new NullPointerException(errors.get(0));
        } else {
          throw new NullPointerException("Update information are currently not available.");
        }
      }
    }
  }

  public static class Reactions {
    /** URL for detailed user information who performed the reactions (JSON) */
    public URL url;
    /** Total number of available reactions. */
    public int totalCount;
    /** Number of the {@code +1} reaction. */
    public int plusOne;
    /** Number of the {@code -1} reaction. */
    public int minusOne;
    /** Number of the {@code laugh} reaction. */
    public int laugh;
    /** Number of the {@code hooray} reaction. */
    public int hooray;
    /** Number of the {@code confused} reaction. */
    public int confused;
    /** Number of the {@code heart} reaction. */
    public int heart;
    /** Number of the {@code rocket} reaction. */
    public int rocket;
    /** Number of the {@code eyes} reaction. */
    public int eyes;

    private Reactions(JSONObject jsonReactions) throws Exception {
      JSONObject root = Objects.requireNonNull(jsonReactions, "JSON asset object cannot be null");
      for (Iterator<String> iter = root.keys(); iter.hasNext(); ) {
        final String key = iter.next();
        switch (key) {
          case "url":
            url = new URL(root.getString(key));
            break;
          case "total_count":
            totalCount = root.getInt(key);
            break;
          case "+1":
            plusOne = root.getInt(key);
            break;
          case "-1":
            minusOne = root.getInt(key);
            break;
          case "laugh":
            laugh = root.getInt(key);
            break;
          case "hooray":
            hooray = root.getInt(key);
            break;
          case "confused":
            confused = root.getInt(key);
            break;
          case "heart":
            heart = root.getInt(key);
            break;
          case "rocket":
            rocket = root.getInt(key);
            break;
          case "eyes":
            eyes = root.getInt(key);
            break;
          default:
            System.out.printf("Reactions parser: Skipping unknown key \"%s\"\n", key);
        }
      }

      // validation
      Objects.requireNonNull(url, "Self URL cannot be null");
    }
  }


  private static class XmlErrorHandler implements ErrorHandler {
    public XmlErrorHandler() {
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
      throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
      throw exception;
    }
  }
}
