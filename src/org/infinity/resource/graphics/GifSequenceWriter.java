// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2005 Jon Olav Hauglid
// See LICENSE.txt for license information
//
// Originally created by Elliot Kroo on 2009-04-25.
//
// This work is licensed under the Creative Commons Attribution 3.0 Unported License.
// To view a copy of this license, visit http://creativecommons.org/licenses/by/3.0/
// or send a letter to Creative Commons, 171 Second Street, Suite 300, San Francisco,
// California, 94105, USA.

package org.infinity.resource.graphics;

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

public class GifSequenceWriter implements AutoCloseable
{
  protected ImageWriter gifWriter;
  protected ImageWriteParam imageWriteParam;
  protected IIOMetadata imageMetaData;

  /**
   * Creates a new GifSequenceWriter
   *
   * @param outputStream the ImageOutputStream to be written to
   * @param imageType one of the imageTypes specified in BufferedImage
   * @param timeBetweenFramesMS the time between frames in miliseconds
   * @param loopContinuously whether the gif should loop repeatedly
   * @param transparentColorIndex the transparent palette index (0..255). Use -1 for none.
   * @throws IIOException if no gif ImageWriters are found
   *
   * @author Elliot Kroo (elliot[at]kroo[dot]net)
   */
  public GifSequenceWriter(ImageOutputStream outputStream, int imageType, int timeBetweenFramesMS,
                           boolean loopContinuously, int transparentColorIndex) throws IIOException, IOException
  {
    // my method to create a writer
    gifWriter = getWriter();
    imageWriteParam = gifWriter.getDefaultWriteParam();
    ImageTypeSpecifier imageTypeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(imageType);

    imageMetaData = gifWriter.getDefaultImageMetadata(imageTypeSpecifier, imageWriteParam);

    String metaFormatName = imageMetaData.getNativeMetadataFormatName();

    IIOMetadataNode root = (IIOMetadataNode)imageMetaData.getAsTree(metaFormatName);

    IIOMetadataNode graphicsControlExtensionNode = getNode(root, "GraphicControlExtension");

    String b = (transparentColorIndex >= 0) ? "TRUE" : "FALSE";
    transparentColorIndex = Math.min(255, Math.max(0, transparentColorIndex));

    graphicsControlExtensionNode.setAttribute("disposalMethod", "restoreToBackgroundColor");
    graphicsControlExtensionNode.setAttribute("userInputFlag", "FALSE");
    graphicsControlExtensionNode.setAttribute("transparentColorFlag", b);
    graphicsControlExtensionNode.setAttribute("delayTime", Integer.toString(timeBetweenFramesMS / 10));
    graphicsControlExtensionNode.setAttribute("transparentColorIndex", Integer.toString(transparentColorIndex));

    IIOMetadataNode commentsNode = getNode(root, "CommentExtensions");
    commentsNode.setAttribute("CommentExtension", "Created by MAH");

    IIOMetadataNode appEntensionsNode = getNode(root, "ApplicationExtensions");

    IIOMetadataNode child = new IIOMetadataNode("ApplicationExtension");

    child.setAttribute("applicationID", "NETSCAPE");
    child.setAttribute("authenticationCode", "2.0");

    int loop = loopContinuously ? 0 : 1;

    child.setUserObject(new byte[]{ 0x1, (byte) (loop & 0xFF), (byte)((loop >> 8) & 0xFF)});
    appEntensionsNode.appendChild(child);

    imageMetaData.setFromTree(metaFormatName, root);

    gifWriter.setOutput(outputStream);

    gifWriter.prepareWriteSequence(null);
  }

  public void writeToSequence(RenderedImage img) throws IOException
  {
    gifWriter.writeToSequence(new IIOImage(img, null, imageMetaData), imageWriteParam);
  }

  /**
   * Close this GifSequenceWriter object. This does not close the underlying
   * stream, just finishes off the GIF.
   */
  @Override
  public void close() throws IOException
  {
    gifWriter.endWriteSequence();
  }

  /**
   * Returns the first available GIF ImageWriter using
   * ImageIO.getImageWritersBySuffix("gif").
   *
   * @return a GIF ImageWriter object
   * @throws IIOException if no GIF image writers are returned
   */
  private static ImageWriter getWriter() throws IIOException
  {
    Iterator<ImageWriter> iter = ImageIO.getImageWritersBySuffix("gif");
    if(!iter.hasNext()) {
      throw new IIOException("No GIF Image Writers Exist");
    } else {
      return iter.next();
    }
  }

  /**
   * Returns an existing child node, or creates and returns a new child node (if
   * the requested node does not exist).
   *
   * @param rootNode the <tt>IIOMetadataNode</tt> to search for the child node.
   * @param nodeName the name of the child node.
   *
   * @return the child node, if found or a new node created with the given name.
   */
  private static IIOMetadataNode getNode(IIOMetadataNode rootNode, String nodeName)
  {
    int nNodes = rootNode.getLength();
    for (int i = 0; i < nNodes; i++) {
      if (rootNode.item(i).getNodeName().compareToIgnoreCase(nodeName) == 0) {
        return((IIOMetadataNode) rootNode.item(i));
      }
    }
    IIOMetadataNode node = new IIOMetadataNode(nodeName);
    rootNode.appendChild(node);
    return(node);
  }
}
