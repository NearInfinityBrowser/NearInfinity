// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2020 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.search.advanced;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/** Manages import and export of Advanced Search configuration files in XML format. */
public class XmlConfig
{
  // DTD used for validation when importing Xml configuration
  private static final String DTD =
      "\n<!DOCTYPE advancedsearch [\n" +
      "<!ELEMENT advancedsearch (resourcetype, mode?, filter*)>\n" +
      "<!ATTLIST advancedsearch version CDATA #REQUIRED>\n" +
      "<!ELEMENT resourcetype (#PCDATA)>\n" +
      "<!ELEMENT mode (#PCDATA)>\n" +
      "<!ELEMENT filter (structure, field, pattern, invert)>\n" +
      "<!ELEMENT structure (child*)>\n" +
      "<!ATTLIST structure group (0|1) \"1\">\n" +
      "<!ATTLIST structure recursive (0|1) \"0\">\n" +
      "<!ATTLIST structure regex (0|1) \"0\">\n" +
      "<!ELEMENT child (#PCDATA)>\n" +
      "<!ELEMENT field (input+)>\n" +
      "<!ATTLIST field type (name|ofsrel|ofsabs) #REQUIRED>\n" +
      "<!ELEMENT input (#PCDATA)>\n" +
      "<!ATTLIST input type (value|case|regex|min|max|ext|mode) \"value\">\n" +
      "<!ELEMENT pattern (input+)>\n" +
      "<!ATTLIST pattern type (text|number|resource|bitfield) #REQUIRED>\n" +
      "<!ELEMENT invert (#PCDATA)>\n" +
      "]>\n";

  private static final String XML_NODE_ADVANCED_SEARCH = "advancedsearch";
  private static final String XML_NODE_RESOURCE_TYPE = "resourcetype";
  private static final String XML_NODE_MODE = "mode";
  private static final String XML_NODE_FILTER = "filter";
  private static final String XML_NODE_STRUCTURE = "structure";
  private static final String XML_NODE_CHILD = "child";
  private static final String XML_NODE_FIELD = "field";
  private static final String XML_NODE_INPUT = "input";
  private static final String XML_NODE_PATTERN = "pattern";
  private static final String XML_NODE_INVERT = "invert";

  private static final String XML_ATTR_VERSION = "version";
  private static final String XML_ATTR_GROUP = "group";
  private static final String XML_ATTR_RECURSIVE = "recursive";
  private static final String XML_ATTR_REGEX = "regex";
  private static final String XML_ATTR_TYPE = "type";

  private static final String XML_FIELDTYPE_NAME = "name";
  private static final String XML_FIELDTYPE_OFS_REL = "ofsrel";
  private static final String XML_FIELDTYPE_OFS_ABS = "ofsabs";

  private static final String XML_PATTERNTYPE_TEXT = "text";
  private static final String XML_PATTERNTYPE_NUMBER = "number";
  private static final String XML_PATTERNTYPE_RESOURCE = "resource";
  private static final String XML_PATTERNTYPE_BITFIELD = "bitfield";

  private static final String XML_INPUTTYPE_VALUE = "value";
  private static final String XML_INPUTTYPE_CASE = "case";
  private static final String XML_INPUTTYPE_REGEX = "regex";
  private static final String XML_INPUTTYPE_MIN = "min";
  private static final String XML_INPUTTYPE_MAX = "max";
  private static final String XML_INPUTTYPE_EXT = "ext";
  private static final String XML_INPUTTYPE_MODE = "mode";

  private final List<SearchOptions> filters;
  private String resourceType;
  private AdvancedSearch.FilterMode filterMode;

  protected XmlConfig(Document xmlDoc) throws SAXException
  {
    filters = new Vector<>();
    parse(xmlDoc);
  }

  /**
   * Exports the specified Advanced Search configuration as XML data into the specified file.
   * @param xmlFile Output file for configuration data. Old content will be overwritten.
   * @param resourceType Resource type to search.
   * @param filterMode How to combine search filters.
   * @param filters List of search filters.
   * @return Success state of the export operation.
   * @throws IOException
   * @throws ParserConfigurationException
   */
  public static boolean Export(File xmlFile, String resourceType, AdvancedSearch.FilterMode filterMode, List<SearchOptions> filters)
      throws IOException, ParserConfigurationException
  {
    try (FileOutputStream fos = new FileOutputStream(xmlFile)) {
      return Export(fos, resourceType, filterMode, filters);
    }
  }

  /**
   * Exports the specified Advanced Search configuration as XML data into the specified output stream.
   * @param xmlOut Output stream for XML configuration data.
   * @param resourceType Resource type to search.
   * @param filterMode How to combine search filters.
   * @param filters List of search filters.
   * @return Success state of the export operation.
   * @throws IOException
   */
  public static boolean Export(OutputStream xmlOut, String resourceType, AdvancedSearch.FilterMode filterMode, List<SearchOptions> filters)
      throws IOException
  {
    if (xmlOut == null)
      throw new IllegalArgumentException("No output stream specified.");

    Vector<String> restypes = AdvancedSearch.getAvailableResourceTypes();
    if (resourceType == null || !restypes.contains(resourceType.trim().toUpperCase()))
      throw new IllegalArgumentException("Invalid resource type specified.");

    if (filterMode == null)
      throw new IllegalArgumentException("Invalid filter mode specified.");

    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setValidating(true);
    DocumentBuilder builder = null;
    try {
      builder = domFactory.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      e.printStackTrace();
      return false;
    }
    Document document = builder.newDocument();

    // node: advancedsearch
    Element root = document.createElement(XML_NODE_ADVANCED_SEARCH);
    root.setAttribute(XML_ATTR_VERSION, "1");
    document.appendChild(root);

    // node: advancedsearch > resourcetype
    Element el = document.createElement(XML_NODE_RESOURCE_TYPE);
    el.setTextContent(resourceType);
    root.appendChild(el);

    // node: advancedsearch > mode
    el = document.createElement(XML_NODE_MODE);
    switch (filterMode) {
      case MatchAny:
        el.setTextContent("or");
        break;
      case MatchOne:
        el.setTextContent("xor");
        break;
      default:
        el.setTextContent("and");
    }
    root.appendChild(el);

    // node: advancedsearch > filter
    for (SearchOptions so : filters) {
      Element filter = document.createElement(XML_NODE_FILTER);

      // node: advancedsearch > filter > structure
      Element structure = document.createElement(XML_NODE_STRUCTURE);
      structure.setAttribute(XML_ATTR_GROUP, boolString(so.isStructureGroup()));
      structure.setAttribute(XML_ATTR_RECURSIVE, boolString(so.isStructureRecursive()));
      structure.setAttribute(XML_ATTR_REGEX, boolString(so.isStructureRegex()));
      for (String s : so.getStructure()) {
        Element child = document.createElement(XML_NODE_CHILD);
        child.setTextContent(s);
        structure.appendChild(child);
      }
      filter.appendChild(structure);

      // node: advancedsearch > filter > field
      Element field = document.createElement(XML_NODE_FIELD);
      switch (so.getSearchType()) {
        case ByName:
          field.setAttribute(XML_ATTR_TYPE, XML_FIELDTYPE_NAME);
          break;
        case ByRelativeOffset:
          field.setAttribute(XML_ATTR_TYPE, XML_FIELDTYPE_OFS_REL);
          break;
        case ByAbsoluteOffset:
          field.setAttribute(XML_ATTR_TYPE, XML_FIELDTYPE_OFS_ABS);
          break;
      }

      // node: advancedsearch > filter > field > input
      if (so.getSearchType() == SearchOptions.FieldMode.ByName) {
        el = document.createElement(XML_NODE_INPUT);
        el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_VALUE);
        el.setTextContent(so.getSearchName());
        field.appendChild(el);

        el = document.createElement(XML_NODE_INPUT);
        el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_CASE);
        el.setTextContent(boolString(so.isSearchNameCaseSensitive()));
        field.appendChild(el);

        el = document.createElement(XML_NODE_INPUT);
        el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_REGEX);
        el.setTextContent(boolString(so.isSearchNameRegex()));
        field.appendChild(el);
      } else {
        el = document.createElement(XML_NODE_INPUT);
        el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_VALUE);
        el.setTextContent(Integer.toString(so.getSearchOffset()));
        field.appendChild(el);
      }
      filter.appendChild(field);


      // node: advancedsearch > filter > pattern
      Element pattern = document.createElement(XML_NODE_PATTERN);
      switch (so.getValueType()) {
        case Text:
          pattern.setAttribute(XML_ATTR_TYPE, XML_PATTERNTYPE_TEXT);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_VALUE);
          el.setTextContent(so.getValueText());
          pattern.appendChild(el);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_CASE);
          el.setTextContent(boolString(so.isValueTextCaseSensitive()));
          pattern.appendChild(el);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_REGEX);
          el.setTextContent(boolString(so.isValueTextRegex()));
          pattern.appendChild(el);
          break;
        case Number:
          pattern.setAttribute(XML_ATTR_TYPE, XML_PATTERNTYPE_NUMBER);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_MIN);
          el.setTextContent(Integer.toString(so.getValueNumberMin()));
          pattern.appendChild(el);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_MAX);
          el.setTextContent(Integer.toString(so.getValueNumberMax()));
          pattern.appendChild(el);
          break;
        case Resource:
          pattern.setAttribute(XML_ATTR_TYPE, XML_PATTERNTYPE_RESOURCE);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_VALUE);
          el.setTextContent(so.getValueResourceRef());
          pattern.appendChild(el);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_EXT);
          el.setTextContent(so.getValueResourceType());
          pattern.appendChild(el);
          break;
        case Bitfield:
          pattern.setAttribute(XML_ATTR_TYPE, XML_PATTERNTYPE_BITFIELD);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_VALUE);
          el.setTextContent(String.format("0x%x", so.getValueBitfield()));
          pattern.appendChild(el);
          el = document.createElement(XML_NODE_INPUT);
          el.setAttribute(XML_ATTR_TYPE, XML_INPUTTYPE_MODE);
          switch (so.getBitfieldMode()) {
            case Exact: el.setTextContent("exact"); break;
            case And:   el.setTextContent("and"); break;
            case Or:    el.setTextContent("or"); break;
            case Xor:   el.setTextContent("xor"); break;
          }
          pattern.appendChild(el);
          break;
      }
      filter.appendChild(pattern);

      // node: advancedsearch > filter > invert
      Element invert = document.createElement(XML_NODE_INVERT);
      invert.setTextContent(boolString(so.isInvertMatch()));
      filter.appendChild(invert);

      root.appendChild(filter);
    }

    // write xml dom to output
    try {
      TransformerFactory transFactory = TransformerFactory.newInstance();
      Transformer transformer = transFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");  // create "human readable" xml data
      DOMSource domSource = new DOMSource(document);
      StreamResult sr = new StreamResult(xmlOut);
      transformer.transform(domSource, sr);
    } catch (TransformerException e) {
      e.printStackTrace();
      return false;
    }

    return true;
  }

  /**
   * Imports the specified XML file containing Advanced Search configuration data.
   * @param xmlFile XML file with Advanced Search configuration data.
   * @return An XmlImport instance configured with data from the specified xml file.
   * @throws FileNotFoundException
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public static XmlConfig Import(File xmlFile)
      throws FileNotFoundException, IOException, ParserConfigurationException, SAXException
  {
    try (InputStream is = new FileInputStream(xmlFile)) {
      return Import(is);
    }
  }

  /**
   * Imports XML data from the specified input stream containing Advanced Search configuration data.
   * @param xmlIn XML input stream containing Advanced Search configuration data.
   * @return An XmlImport instance configured with data from the specified xml input.
   * @throws IOException
   * @throws ParserConfigurationException
   * @throws SAXException
   */
  public static XmlConfig Import(InputStream xmlIn)
      throws IOException, ParserConfigurationException, SAXException
  {
    if (xmlIn == null)
      throw new IllegalArgumentException("No input stream specified.");

    // getting xml data as string
    StringBuilder sb = new StringBuilder();
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      int length;
      while ((length = xmlIn.read(buffer)) != -1) {
        baos.write(buffer, 0, length);
      }
      sb.append(baos.toString("UTF-8"));
    }

    // inject DTD definition directly after xml declaration
    int pos = sb.indexOf("<?xml");
    if (pos >= 0) {
      pos = sb.indexOf("?>", pos);
      if (pos >= 0) {
        sb.insert(pos + 2, DTD);
      }
    }

    // build XML DOM structure
    DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
    domFactory.setValidating(true);
    domFactory.setIgnoringElementContentWhitespace(true);

    DocumentBuilder builder = domFactory.newDocumentBuilder();
    // DocumentBuilder complains if no error handler is set up
    builder.setErrorHandler(new ErrorHandler() {
      @Override
      public void warning(SAXParseException exception) throws SAXException { exception.printStackTrace(); }
      @Override
      public void fatalError(SAXParseException exception) throws SAXException { exception.printStackTrace(); }
      @Override
      public void error(SAXParseException exception) throws SAXException { exception.printStackTrace(); }
    });

    Document document = null;
    try (ByteArrayInputStream bais = new ByteArrayInputStream(sb.toString().getBytes())) {
      document = builder.parse(bais);
    }

    return new XmlConfig(document);
  }

  /** Returns the resource type from the imported xml configuration. */
  public String getResourceType() { return resourceType; }

  /** Returns the filter mode from the imported xml configuration. */
  public AdvancedSearch.FilterMode getFilterMode() { return filterMode; }

  /** Returns the list of SearchOptions filters from the imported xml configuration. */
  public List<SearchOptions> getFilters() { return Collections.unmodifiableList(filters); }

  private void parse(Document xmlDoc) throws SAXException
  {
    if (xmlDoc == null)
      throw new IllegalArgumentException("No xml document specified.");

    NodeList rootNodeList = xmlDoc.getElementsByTagName(XML_NODE_ADVANCED_SEARCH);
    if (rootNodeList.getLength() == 0)
      throw new SAXException("No advanced search configuration found.");

    Node node;

    // node: advancedsearch
    Node rootNode = rootNodeList.item(0);
    node = rootNode.getAttributes().getNamedItem(XML_ATTR_VERSION);
    try {
      int version = Integer.parseInt(node.getNodeValue());
      if (version != 1)
        throw new SAXException("Unsupported advanced search configuration version: " + version);
    } catch (NumberFormatException e) {
      throw new SAXException("Invalid advanced search configuration version: " + node.getNodeValue());
    }

    NodeList childNodes = rootNode.getChildNodes();
    for (int idx = 0, cnt = childNodes.getLength(); idx < cnt; idx++) {
      node = childNodes.item(idx);
      switch (node.getNodeName()) {
        case XML_NODE_RESOURCE_TYPE:
          parseResourceType(node);
          break;
        case XML_NODE_MODE:
          parseMode(node);
          break;
        case XML_NODE_FILTER:
          parseFilter(node);
          break;
        default:
          throw new SAXException("Unsupported element: advancedsearch > " + node.getNodeName());
      }
    }
  }

  // node: advancedsearch > resourcetype
  private void parseResourceType(Node node) throws SAXException
  {
    if (node.getNodeName().equals(XML_NODE_RESOURCE_TYPE)) {
      Vector<String> restypes = AdvancedSearch.getAvailableResourceTypes();
      String restype = getNodeText(node).trim().toUpperCase();
      int idx = restypes.indexOf(restype);
      if (idx >= 0) {
        resourceType = restypes.get(idx);
      } else {
        throw new SAXException("Unsupported resource type: " + restype);
      }
    }
  }

  // node: advancedsearch > mode
  private void parseMode(Node node) throws SAXException
  {
    if (node.getNodeName().equals(XML_NODE_MODE)) {
      String mode = getNodeText(node).trim().toUpperCase();
      switch (mode) {
        case "AND":
          filterMode = AdvancedSearch.FilterMode.MatchAll;
          break;
        case "OR":
          filterMode = AdvancedSearch.FilterMode.MatchAny;
          break;
        case "XOR":
          filterMode = AdvancedSearch.FilterMode.MatchOne;
          break;
        default:
          throw new SAXException("Unsupported filter mode: " + mode);
      }
    }
  }

  // node: advancedsearch > filter
  private void parseFilter(Node node) throws SAXException
  {
    if (node.getNodeName().equals(XML_NODE_FILTER)) {
      NodeList filterNodes = node.getChildNodes();
      SearchOptions so = new SearchOptions();
      for (int idx = 0, cnt = filterNodes.getLength(); idx < cnt; idx++) {
        node = filterNodes.item(idx);
        switch (node.getNodeName()) {
          case XML_NODE_STRUCTURE:
            parseFilterStructure(node, so);
            break;
          case XML_NODE_FIELD:
            parseFilterField(node, so);
            break;
          case XML_NODE_PATTERN:
            parseFilterPattern(node, so);
            break;
          case XML_NODE_INVERT:
            parseFilterInvert(node, so);
            break;
          default:
            throw new SAXException("Unsupported element: advancedsearch > filter > " + node.getNodeName());
        }
      }
      filters.add(so);
    }
  }

  // node: advancedsearch > filter > structure
  private void parseFilterStructure(Node node, SearchOptions so) throws SAXException
  {
    if (node.getNodeName().equals(XML_NODE_STRUCTURE)) {
      Node att = node.getAttributes().getNamedItem(XML_ATTR_GROUP);
      if (att != null) {
        try {
          so.setStructureGroup(parseNumber(att.getNodeValue()) != 0);
        } catch (NumberFormatException e) {
          throw new SAXException("Invalid \"group\" attribute value in advancedsearch > filter > structure: " + att.getNodeValue());
        }
      } else {
        so.setStructureGroup(true);
      }
      att = node.getAttributes().getNamedItem(XML_ATTR_RECURSIVE);
      if (att != null) {
        try {
          so.setStructureRecursive(parseNumber(att.getNodeValue()) != 0);
        } catch (NumberFormatException e) {
          throw new SAXException("Invalid \"recursive\" attribute value in advancedsearch > filter > structure: " + att.getNodeValue());
        }
      } else {
        so.setStructureRecursive(false);
      }
      att = node.getAttributes().getNamedItem(XML_ATTR_REGEX);
      if (att != null) {
        try {
          so.setStructureRegex(parseNumber(att.getNodeValue()) != 0);
        } catch (NumberFormatException e) {
          throw new SAXException("Invalid \"regex\" attribute value in advancedsearch > filter > structure: " + att.getNodeValue());
        }
      } else {
        so.setStructureRegex(false);
      }

      NodeList childNodes = node.getChildNodes();
      for (int idx = 0, cnt = childNodes.getLength(); idx < cnt; idx++) {
        node = childNodes.item(idx);
        if (node.getNodeName().equals(XML_NODE_CHILD)) {
          so.getStructure().add(getNodeText(node).trim());
        } else {
          throw new SAXException("Unsupported element: advancedsearch > filter > structure > " + node.getNodeName());
        }
      }
    }
  }

  // node: advancedsearch > filter > field
  private void parseFilterField(Node node, SearchOptions so) throws SAXException
  {
    if (node.getNodeName().equals(XML_NODE_FIELD)) {
      Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
      if (att == null)
        throw new SAXException("Attribute \"type\" required for advancedsearch > filter > field");
      String attValue = att.getNodeValue().trim().toLowerCase();
      switch (attValue) {
        case XML_FIELDTYPE_NAME:
          parseFilterFieldName(node.getChildNodes(), so);
          break;
        case XML_FIELDTYPE_OFS_REL:
          parseFilterFieldOffset(node.getChildNodes(), so, SearchOptions.FieldMode.ByRelativeOffset);
          break;
        case XML_FIELDTYPE_OFS_ABS:
          parseFilterFieldOffset(node.getChildNodes(), so, SearchOptions.FieldMode.ByAbsoluteOffset);
          break;
        default:
          throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > field: " + attValue);
      }
    }
  }

  // node: advancedsearch > filter > field (type=name)
  private void parseFilterFieldName(NodeList nodes, SearchOptions so) throws SAXException
  {
    String fieldName = null;
    boolean fieldCase = false, fieldRegex = false;
    for (int idx = 0, cnt = nodes.getLength(); idx < cnt; idx++) {
      Node node = nodes.item(idx);
      if (node.getNodeName().equals(XML_NODE_INPUT)) {
        Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
        String attValue = (att != null) ? att.getNodeValue() : XML_INPUTTYPE_VALUE;
        switch (attValue) {
          case XML_INPUTTYPE_VALUE:
            fieldName = getNodeText(node).trim();
            break;
          case XML_INPUTTYPE_CASE:
            try {
              fieldCase = parseNumber(getNodeText(node)) != 0;
            } catch (NumberFormatException e) {
              throw new SAXException("Unsupported content in advancedsearch > filter > field > input (type=case): " + getNodeText(node));
            }
            break;
          case XML_INPUTTYPE_REGEX:
            try {
              fieldRegex = parseNumber(getNodeText(node)) != 0;
            } catch (NumberFormatException e) {
              throw new SAXException("Unsupported content in advancedsearch > filter > field > input (type=regex): " + getNodeText(node));
            }
            break;
          case XML_INPUTTYPE_EXT:
          case XML_INPUTTYPE_MIN:
          case XML_INPUTTYPE_MAX:
          case XML_INPUTTYPE_MODE:
            // ignore
            break;
          default:
            throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > field > input: " + attValue);
        }
      } else {
        throw new SAXException("Unsupported element: advancedsearch > filter > field > " + node.getNodeName());
      }
    }

    if (fieldName != null) {
      so.setSearchName(fieldName, fieldCase, fieldRegex);
    } else {
      throw new SAXException("No input value specified in advancedsearch > filter > field (type=name)");
    }
  }

  // node: advancedsearch > filter > field (type=ofsrel or type=ofsabs)
  private void parseFilterFieldOffset(NodeList nodes, SearchOptions so, SearchOptions.FieldMode fieldMode) throws SAXException
  {
    for (int idx = 0, cnt = nodes.getLength(); idx < cnt; idx++) {
      Node node = nodes.item(idx);
      if (node.getNodeName().equals(XML_NODE_INPUT)) {
        Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
        String attValue = (att != null) ? att.getNodeValue() : XML_INPUTTYPE_VALUE;
        switch (attValue) {
          case XML_INPUTTYPE_VALUE:
            try {
              so.setSearchOffset(fieldMode, parseNumber(getNodeText(node)));
              return;
            } catch (NumberFormatException e) {
              throw new SAXException("Unsupported content in advancedsearch > filter > field > input: " + getNodeText(node));
            }
          case XML_INPUTTYPE_CASE:
          case XML_INPUTTYPE_REGEX:
          case XML_INPUTTYPE_EXT:
          case XML_INPUTTYPE_MIN:
          case XML_INPUTTYPE_MAX:
          case XML_INPUTTYPE_MODE:
            // ignore
            break;
          default:
            throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > field > input: " + attValue);
        }
      } else {
        throw new SAXException("Unsupported element: advancedsearch > filter > field > " + node.getNodeName());
      }
    }

    String type = (fieldMode == SearchOptions.FieldMode.ByRelativeOffset) ? XML_FIELDTYPE_OFS_REL : XML_FIELDTYPE_OFS_ABS;
    throw new SAXException(String.format("No input value specified in advancedsearch > filter > field (type=%s)", type));
  }

  // node: advancedsearch > filter > pattern
  private void parseFilterPattern(Node node, SearchOptions so) throws SAXException
  {
    if (node.getNodeName().equals(XML_NODE_PATTERN)) {
      Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
      if (att == null)
        throw new SAXException("Attribute \"type\" required for advancedsearch > filter > pattern");
      String attValue = att.getNodeValue().trim().toLowerCase();
      switch (attValue) {
        case XML_PATTERNTYPE_TEXT:
          parseFilterPatternText(node.getChildNodes(), so);
          break;
        case XML_PATTERNTYPE_NUMBER:
          parseFilterPatternNumber(node.getChildNodes(), so);
          break;
        case XML_PATTERNTYPE_RESOURCE:
          parseFilterPatternResource(node.getChildNodes(), so);
          break;
        case XML_PATTERNTYPE_BITFIELD:
          parseFilterPatternBitfield(node.getChildNodes(), so);
          break;
        default:
          throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > pattern: " + attValue);
      }
    }
  }

  // node: advancedsearch > filter > pattern (type=text)
  private void parseFilterPatternText(NodeList nodes, SearchOptions so) throws SAXException
  {
    String valueText = null;
    boolean valueCase = false, valueRegex = false;
    for (int idx = 0, cnt = nodes.getLength(); idx < cnt; idx++) {
      Node node = nodes.item(idx);
      if (node.getNodeName().equals(XML_NODE_INPUT)) {
        Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
        String attValue = (att != null) ? att.getNodeValue() : XML_INPUTTYPE_VALUE;
        switch (attValue) {
          case XML_INPUTTYPE_VALUE:
            valueText = getNodeText(node).trim();
            break;
          case XML_INPUTTYPE_CASE:
            try {
              valueCase = parseNumber(getNodeText(node)) != 0;
            } catch (NumberFormatException e) {
              throw new SAXException("Unsupported content in advancedsearch > filter > pattern > input (type=case): " + getNodeText(node));
            }
            break;
          case XML_INPUTTYPE_REGEX:
            try {
              valueRegex = parseNumber(getNodeText(node)) != 0;
            } catch (NumberFormatException e) {
              throw new SAXException("Unsupported content in advancedsearch > filter > pattern > input (type=regex): " + getNodeText(node));
            }
            break;
          case XML_INPUTTYPE_MIN:
          case XML_INPUTTYPE_MAX:
          case XML_INPUTTYPE_EXT:
          case XML_INPUTTYPE_MODE:
            // ignore
            break;
          default:
            throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > pattern > input: " + attValue);
        }
      } else {
        throw new SAXException("Unsupported element: advancedsearch > filter > pattern > " + node.getNodeName());
      }
    }

    if (valueText != null) {
      so.setValueText(valueText, valueCase, valueRegex);
    } else {
      throw new SAXException("No input value specified in advancedsearch > filter > pattern (type=text)");
    }
  }

  // node: advancedsearch > filter > pattern (type=number)
  private void parseFilterPatternNumber(NodeList nodes, SearchOptions so) throws SAXException
  {
    int valueMin = 0, valueMax = 0;
    boolean checkedValueMin = false, checkedValueMax = false;
    for (int idx = 0, cnt = nodes.getLength(); idx < cnt; idx++) {
      Node node = nodes.item(idx);
      if (node.getNodeName().equals(XML_NODE_INPUT)) {
        Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
        String attValue = (att != null) ? att.getNodeValue() : XML_INPUTTYPE_VALUE;
        switch (attValue) {
          case XML_INPUTTYPE_MIN:
            try {
              valueMin = parseNumber(getNodeText(node));
              checkedValueMin = true;
            } catch (NumberFormatException e) {
              throw new SAXException("Invalid content in advancedsearch > filter > pattern > input (type=min): " + getNodeText(node));
            }
            break;
          case XML_INPUTTYPE_MAX:
            try {
              valueMax = parseNumber(getNodeText(node));
              checkedValueMax = true;
            } catch (NumberFormatException e) {
              throw new SAXException("Invalid content in advancedsearch > filter > pattern > input (type=max): " + getNodeText(node));
            }
            break;
          case XML_INPUTTYPE_VALUE:
          case XML_INPUTTYPE_CASE:
          case XML_INPUTTYPE_REGEX:
          case XML_INPUTTYPE_EXT:
          case XML_INPUTTYPE_MODE:
            // ignore
            break;
          default:
            throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > pattern > input: " + attValue);
        }
      } else {
        throw new SAXException("Unsupported element: advancedsearch > filter > pattern > " + node.getNodeName());
      }
    }

    if (!checkedValueMin && !checkedValueMax)
      throw new SAXException("No min or max values specified in advancedsearch > filter > pattern (type=number)");

    if (checkedValueMin && !checkedValueMax) {
      valueMax = valueMin;
      checkedValueMax = true;
    }
    if (checkedValueMax && !checkedValueMin) {
      valueMin = valueMax;
      checkedValueMin = true;
    }
    so.setValueNumber(valueMin, valueMax);
  }

  // node: advancedsearch > filter > pattern (type=resource)
  private void parseFilterPatternResource(NodeList nodes, SearchOptions so) throws SAXException
  {
    String valueRef = null, valueExt = null;
    boolean checkedValueRef = false, checkedValueExt = false;
    for (int idx = 0, cnt = nodes.getLength(); idx < cnt; idx++) {
      Node node = nodes.item(idx);
      if (node.getNodeName().equals(XML_NODE_INPUT)) {
        Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
        String attValue = (att != null) ? att.getNodeValue() : XML_INPUTTYPE_VALUE;
        switch (attValue) {
          case XML_INPUTTYPE_VALUE:
            valueRef = getNodeText(node).trim();
            checkedValueRef = true;
            break;
          case XML_INPUTTYPE_EXT:
            valueExt = getNodeText(node).trim().toUpperCase();
            checkedValueExt = true;
            break;
          case XML_INPUTTYPE_CASE:
          case XML_INPUTTYPE_REGEX:
          case XML_INPUTTYPE_MIN:
          case XML_INPUTTYPE_MAX:
          case XML_INPUTTYPE_MODE:
            // ignore
            break;
          default:
            throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > pattern > input: " + attValue);
        }
      } else {
        throw new SAXException("Unsupported element: advancedsearch > filter > pattern > " + node.getNodeName());
      }
    }

    if (!checkedValueRef)
      throw new SAXException("No input value specified in advancedsearch > filter > pattern (type=resource)");
    valueRef = valueRef.trim().toUpperCase();
    int pos = valueRef.lastIndexOf('.');
    if (pos >= 0) {
      if (!checkedValueExt) {
        valueExt = valueRef.substring(pos + 1);
        checkedValueExt = true;
      }
      valueRef = valueRef.substring(0, pos);
    }
    so.setValueResource(valueRef, valueExt);
  }

  // node: advancedsearch > filter > pattern (type=bitfield)
  private void parseFilterPatternBitfield(NodeList nodes, SearchOptions so) throws SAXException
  {
    int value = 0;
    SearchOptions.BitFieldMode bitfieldMode = SearchOptions.BitFieldMode.Exact;
    boolean checkedValue = false;
    for (int idx = 0, cnt = nodes.getLength(); idx < cnt; idx++) {
      Node node = nodes.item(idx);
      if (node.getNodeName().equals(XML_NODE_INPUT)) {
        Node att = node.getAttributes().getNamedItem(XML_ATTR_TYPE);
        String attValue = (att != null) ? att.getNodeValue() : XML_INPUTTYPE_VALUE;
        switch (attValue) {
          case XML_INPUTTYPE_VALUE:
            try {
              value = parseNumber(getNodeText(node));
              checkedValue = true;
            } catch (NumberFormatException e) {
              throw new SAXException("Invalid content in advancedsearch > filter > pattern > input (type=value): " + getNodeText(node));
            }
            break;
          case XML_INPUTTYPE_MODE:
          {
            String mode = getNodeText(node).toLowerCase();
            switch (mode) {
              case "exact":
                bitfieldMode = SearchOptions.BitFieldMode.Exact;
                break;
              case "and":
                bitfieldMode = SearchOptions.BitFieldMode.And;
                break;
              case "or":
                bitfieldMode = SearchOptions.BitFieldMode.Or;
                break;
              case "xor":
                bitfieldMode = SearchOptions.BitFieldMode.Xor;
                break;
              default:
                throw new SAXException("Invalid bitfield mode in advancedsearch > filter > pattern > input (type=mode): " + getNodeText(node));
            }
            break;
          }
          case XML_INPUTTYPE_MIN:
          case XML_INPUTTYPE_MAX:
          case XML_INPUTTYPE_CASE:
          case XML_INPUTTYPE_REGEX:
          case XML_INPUTTYPE_EXT:
            // ignore
            break;
          default:
            throw new SAXException("Invalid \"type\" attribute value in advancedsearch > filter > pattern > input: " + attValue);
        }
      } else {
        throw new SAXException("Unsupported element: advancedsearch > filter > pattern > " + node.getNodeName());
      }
    }

    if (!checkedValue)
      throw new SAXException("No input value specified in advancedsearch > filter > pattern (type=bitfield)");
    so.setValueBitfield(value, bitfieldMode);
  }

  // node: advancedsearch > filter > invert
  private void parseFilterInvert(Node node, SearchOptions so) throws SAXException
  {
    if (node.getNodeName().equals(XML_NODE_INVERT)) {
      try {
        so.setInvertMatch(parseNumber(getNodeText(node)) != 0);
      } catch (NumberFormatException e) {
        throw new SAXException("Unsupported content in advancedsearch > filter > invert: " + getNodeText(node));
      }
    }
  }

  // Converts text into a decimal or hexadecimal number.
  private int parseNumber(String text) throws NumberFormatException
  {
    text = text.trim().toLowerCase();
    if (text.startsWith("0x"))
      return Integer.parseInt(text.substring(2), 16);
    else
      return Integer.parseInt(text);
  }

//  /**
//   * Converts special xml characters into entities.
//   * @param text The text to convert.
//   * @param isAttribute Whether attribute-specific should be escaped.
//   * @return Text with entities.
//   */
//  private static String escapeString(String text, boolean isAttribute)
//  {
//    StringBuilder sb = new StringBuilder();
//    if (text != null) {
//      for (int i = 0, len = text.length(); i < len; i++) {
//        char ch = text.charAt(i);
//        switch (ch) {
//          case '<': sb.append("&lt;"); break;
//          case '>': sb.append("&gt;"); break;
//          case '&': sb.append("&amp;"); break;
//          case '"': if (isAttribute) sb.append("&quot;"); else sb.append(ch); break;
//          case '\'': if (isAttribute) sb.append("&apos;"); else sb.append(ch); break;
//          default:
//            if (ch > 0x7e)
//              sb.append("&#" + ((int)ch) + ";");
//            else
//              sb.append(ch);
//        }
//      }
//    }
//    return sb.toString();
//  }

//  /**
//   * Converts entities in the specified string back to characters.
//   * @param text Text with entities.
//   * @return Text without entities.
//   */
//  private static String unescapeString(String text)
//  {
//    StringBuilder sb = new StringBuilder();
//    if (text != null) {
//      for (int i = 0, len = text.length(); i < len; i++) {
//        char ch = text.charAt(i);
//        if (ch == '&') {
//          // possibly an entity?
//          boolean isEntity = false;
//          StringBuilder entity = new StringBuilder();
//          int j = i;
//          for (; j < len; j++) {
//            char ch2 = text.charAt(j);
//            entity.append(ch2);
//            if (ch2 == ';')
//              break;
//          }
//          String s = entity.toString();
//          switch (s) {
//            case "&lt;": sb.append('<'); isEntity = true; break;
//            case "&gt;": sb.append('>'); isEntity = true; break;
//            case "&amp;": sb.append('&'); isEntity = true; break;
//            case "&quot;": sb.append('"'); isEntity = true; break;
//            case "&apos;": sb.append('\''); isEntity = true; break;
//            default:
//              if (Pattern.matches("^&#[0-9]+;$", s)) {
//                try {
//                  char c = (char)Integer.parseInt(s.substring(2, s.length() - 1));
//                  sb.append(c);
//                  isEntity = true;
//                } catch (NumberFormatException e) {
//                  e.printStackTrace();
//                }
//              }
//          }
//          if (isEntity) {
//            // char pointer should continue after entity
//            i = j;
//            continue;
//          }
//        }
//
//        // regular character
//        sb.append(ch);
//      }
//    }
//    return sb.toString();
//  }

  /** Helper method: Attempts to extract the textual content of an xml node. */
  private static String getNodeText(Node node)
  {
    String retVal = "";
    if (node != null) {
      if (node.getNodeType() == Node.TEXT_NODE) {
        retVal = node.getNodeValue();
      } else if (node.hasChildNodes()) {
        node = node.getChildNodes().item(0);
        if (node.getNodeType() == Node.TEXT_NODE)
          retVal = node.getNodeValue();
      }
    }
    return retVal;
  }

  /** Helper method: Converts boolean value into the strings "0" (false) or "1" (true). */
  private static String boolString(boolean v)
  {
    return v ? "1" : "0";
  }
}
