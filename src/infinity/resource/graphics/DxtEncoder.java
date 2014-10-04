/* Near Infinity - An Infinity Engine Browser and Editor
 * Copyright (C) 2001 - 2005 Jon Olav Hauglid
 * See LICENSE.txt for license information
 *
 * The DXT compression algorithm is based on libsquish:
 * -----------------------------------------------------------------------------
 * Copyright (c) 2006 Simon Brown                          si@sjbrown.co.uk
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * -----------------------------------------------------------------------------
 */

package infinity.resource.graphics;

/**
 * Encodes pixel data into the DXT1/DXT3/DXT5 format.
 * @author argent77
 */
public final class DxtEncoder
{
  /**
   * Supported DXT compression types
   */
  public static enum DxtType { DXT1, DXT3, DXT5 }

  /**
   * Encodes an image into a series of DXTn code blocks.
   * @param pixels The pixel data as array of integers in ARGB format.
   * @param width The width of the image (must be a multiple of 4).
   * @param height The height of the image (must be a multiple of 4).
   * @param dxtType The compression type to use.
   * @return A data block containing the DXT-encoded image.
   * @throws Exception
   */
  static public byte[] encodeImage(int[] pixels, int width, int height, DxtType dxtType) throws Exception
  {
    // consistency check
    if (dxtType == null)
      throw new Exception("No DXT type specified");
    if (width <= 0 || height <= 0)
      throw new Exception("Invalid width or height specified");
    if ((width & 3) != 0 || (height & 3) != 0)
      throw new Exception("Width and height must be a multiple of 4");
    if (pixels == null || pixels.length < width*height)
      throw new Exception("Insufficient source data.");

    int size = calcImageSize(width, height, dxtType);
    byte[] output = new byte[size];
    try {
      encodeImage(pixels, width, height, output, dxtType);
    } catch (Exception e) {
      output = null;
      throw e;
    }

    return output;
  }

  /**
   * Encodes an image into a series of DXTn code blocks.
   * @param pixels The pixel data as array of integers in ARGB format.
   * @param width The width of the image (must be a multiple of 4).
   * @param height The height of the image (must be a multiple of 4).
   * @param output The storage space for the compressed data.
   * @param dxtType The compression type to use.
   * @throws Exception
   */
  static public void encodeImage(int[] pixels, int width, int height, byte[] output,
                              DxtType dxtType) throws Exception
  {
    // consistency check
    if (dxtType == null)
      throw new Exception("No DXT type specified");
    if (width <= 0 || height <= 0)
      throw new Exception("Invalid width or height specified");
    if ((width & 3) != 0 || (height & 3) != 0)
      throw new Exception("Width and height must be a multiple of 4");
    if (pixels == null || pixels.length < width*height)
      throw new Exception("Insufficient source data.");
    if (output == null || output.length < calcImageSize(width, height, dxtType))
      throw new Exception(String.format("Insufficient space in output array. Needed: %1$d bytes, available: %2$d bytes",
                          calcImageSize(width, height, dxtType), (output == null) ? 0 : output.length));

    int outputOfs = 0;    // points to the end of encoded data
    int bw = width / 4;
    int bh = height / 4;
    int[] inBlock = new int[16];
    byte[] outBlock = new byte[calcBlockSize(dxtType)];
    for (int y = 0; y < bh; y++) {
      for (int x = 0; x < bw; x++) {
        // create 4x4 block of pixels for DXTn compression
        int ofs = (y*4)*width + (x*4);
        for (int i = 0; i < 4; i++, ofs+=width) {
          System.arraycopy(pixels, ofs, inBlock, i*4, 4);
        }

        // compress pixel block
        encodeBlock(inBlock, outBlock, dxtType);
        System.arraycopy(outBlock, 0, output, outputOfs, outBlock.length);
        outputOfs += outBlock.length;
      }
    }
  }

  /**
   * Encodes a single 4x4 block of pixel data in ARGB format into a DXTn code block.
   * @param pixels The block of pixels to encode.
   * @param block Data block to store the compressed DXTn data in.
   * @param dxtType The DXT type to use.
   */
  public static void encodeBlock(int[] pixels, byte[] block, DxtType dxtType) throws Exception
  {
    if (pixels == null || pixels.length < 16)
      throw new Exception("Insufficient source data.");
    if (block == null || block.length < calcBlockSize(dxtType))
      throw new Exception(String.format("Insufficient output space. Needed: %1$d bytes, available: %2$d bytes",
                          calcBlockSize(dxtType), (block == null) ? 0 : block.length));

    byte[] colorBlock = new byte[8];
    byte[] alphaBlock = null;
    if (dxtType == DxtType.DXT3 || dxtType == DxtType.DXT5) {
      alphaBlock = new byte[8];
    }

    // create the minimal point set
    ColorSet colors = new ColorSet(pixels, dxtType);

    // check the compression type and compress color
    ColorFit fit = null;
    if (colors.getCount() == 1) {
      // always do a single color fit
      fit = new SingleColorFit(colors, dxtType);
    } else if (colors.getCount() == 0) {
      // do a range fit
      fit = new RangeFit(colors, dxtType);
    } else {
      // default to a cluster fit
      fit = new ClusterFit(colors, dxtType);
    }
    fit.compress(colorBlock);

    // compress alpha separately if necessary
    if (dxtType == DxtType.DXT3) {
      Alpha.compressAlphaDxt3(pixels, alphaBlock);
    } else if (dxtType == DxtType.DXT5) {
      Alpha.compressAlphaDxt5(pixels, alphaBlock);
    }

    int ofs = 0;
    if (alphaBlock != null) {
      for (int i = 0; i < alphaBlock.length; i++, ofs++) {
        block[ofs] = alphaBlock[i];
      }
    }
    for (int i = 0; i < colorBlock.length; i++, ofs++) {
      block[ofs] = colorBlock[i];
    }
  }


  /**
   * Calculates the size of a single DXT encoded 4x4 block of pixels.
   * @param dxtType The desired compression.
   * @return The number of bytes to hold the compressed data of a 4x4 block of pixels.
   */
  public static int calcBlockSize(DxtType dxtType)
  {
    if (dxtType != null) {
      switch (dxtType) {
        case DXT1:
          return 8;
        case DXT3:
        case DXT5:
          return 16;
      }
    }
    return 0;
  }

  /**
   * Calculates the compressed size of an image.
   * @param width The width in pixels (will be rounded up to a multiple of 4).
   * @param height The height in pixels (will be rounded up to a multiple of 4).
   * @param dxtType The desired compression type.
   * @return The number of bytes required to hold the compressed image data.
   */
  public static int calcImageSize(int width, int height, DxtType dxtType)
  {
    if (width <= 0 || height <= 0 || dxtType == null)
      return 0;

    width += (4 - (width & 3)) & 3;
    height += (4 - (height & 3)) & 3;

    return (width*height)/16 * calcBlockSize(dxtType);
  }


// --------------------------- INNER CLASSES ---------------------------

  private static class ColorSet
  {
    private final Vec3[] points;
    private final float[] weights;
    private final int[] remap;
    private int count;
    private boolean transparent;

    // Extracts the color component at the specified pos (0..3 = blue,green,red,alpha)
    public static int argb(int color, int pos)
    {
      return (color >>> ((pos & 3) << 3)) & 0xff;
    }

    public ColorSet(int[] pixels, DxtType dxtType)
    {
      points = new Vec3[16];
      weights = new float[16];
      remap = new int[16];
      count = 0;
      transparent = false;

      boolean isDXT1 = (dxtType == DxtType.DXT1);

      // create minimal set
      for (int i = 0; i < 16; i++) {
        // check for transparent pixels when using DXT1
        if (isDXT1 && argb(pixels[i], 3) < 128) {
          remap[i] = -1;
          transparent = true;
          continue;
        }

        // loop over previous points for a match
        for (int j = 0; ; j++) {
          // allocate new points
          if (j == i) {
            // normalize coordinates to [0, 1]
            float x = (float)argb(pixels[i], 2) / 255.0f;
            float y = (float)argb(pixels[i], 1) / 255.0f;
            float z = (float)argb(pixels[i], 0) / 255.0f;

            // ensure there is always non-zero weight even for zero alpha
            float w = (float)(argb(pixels[i], 3) + 1) / 256.0f;

            // add the points
            points[count] = new Vec3(x, y, z);
            weights[count] = w;
            remap[i] = count;

            count++;
            break;
          }

          // check for a match
          boolean match = (argb(pixels[i], 0) == argb(pixels[j], 0) &&
                           argb(pixels[i], 1) == argb(pixels[j], 1) &&
                           argb(pixels[i], 2) == argb(pixels[j], 2) &&
                           (argb(pixels[j], 3) >= 128 || !isDXT1));
          if (match) {
            // get the index of the match
            int index = remap[j];
            // ensure there is always non-zero weight even for zero alpha
            float w = (float)(argb(pixels[i], 3) + 1) / 256.0f;
            // map to this point and increase the weight
            weights[index] += w;
            remap[i] = index;
            break;
          }
        }
      }

      // square root the weights
      for (int i = 0; i < count; i++) {
        weights[i] = (float)Math.sqrt(weights[i]);
      }
    }

    public int getCount() { return count; }

    public Vec3[] getPoints() { return points; }

    public float[] getWeights() { return weights; }

    public boolean isTransparent() { return transparent; }

    public void remapIndices(int[] source, int[] target)
    {
      for (int i = 0; i < 16; i++) {
        int j = remap[i];
        target[i] = (j == -1) ? 3 : source[j];
      }
    }
  }


  private static abstract class ColorFit
  {
    protected ColorSet colors;
    protected DxtType dxtType;

    public ColorFit(ColorSet colors, DxtType dxtType)
    {
      this.colors = colors;
      this.dxtType = dxtType;
    }

    public void compress(byte[] block)
    {
      boolean isDXT1 = (dxtType == DxtType.DXT1);
      if (isDXT1) {
        compress3(block);
        if (!colors.isTransparent()) {
          compress4(block);
        }
      } else {
        compress4(block);
      }
    }

    protected abstract void compress3(byte[] block);
    protected abstract void compress4(byte[] block);
  }


  private static class SingleColorFit extends ColorFit
  {
    private final int[] color;

    private Vec3 start, end;
    private int index;
    private int error;
    private int bestError;

    public SingleColorFit(ColorSet colors, DxtType dxtType)
    {
      super(colors, dxtType);
      color = new int[3];
      start = new Vec3();
      end = new Vec3();

      // grab the single color
      Vec3 value = this.colors.getPoints()[0];
      color[0] = Misc.floatToInt(255.0f*value.x(), 255);
      color[1] = Misc.floatToInt(255.0f*value.y(), 255);
      color[2] = Misc.floatToInt(255.0f*value.z(), 255);

      // initialize the best error
      bestError = Integer.MAX_VALUE;
    }

    @Override
    protected void compress3(byte[] block)
    {
      // build the table of lookups
      SingleColorLookup[][] lookups = new SingleColorLookup[][]{
          Lookups.lookup53, Lookups.lookup63, Lookups.lookup53
      };

      // find the best end-points and index
      computeEndPoints(lookups);

      // build the block if we win
      if (error < bestError) {
        // remap indices
        int[] indices = new int[16];
        colors.remapIndices(new int[]{index}, indices);

        // save the block
        ColorBlock.writeColorBlock3(start, end, indices, block);

        // save the error
        bestError = error;
      }
    }

    @Override
    protected void compress4(byte[] block)
    {
      // build the table of lookups
      SingleColorLookup[][] lookups = new SingleColorLookup[][]{
          Lookups.lookup54, Lookups.lookup64, Lookups.lookup54
      };

      // find the best end-points and index
      computeEndPoints(lookups);

      // build the block if we win
      if (error < bestError) {
        // remap indices
        int[] indices = new int[16];
        colors.remapIndices(new int[]{index}, indices);

        // save the block
        ColorBlock.writeColorBlock4(start, end, indices, block);

        // save the error
        bestError = error;
      }
    }

    protected void computeEndPoints(SingleColorLookup[][] lookups)
    {
      // check each index combination (endpoint or intermediate)
      this.error = Integer.MAX_VALUE;
      for (int index = 0; index < 2; index++) {
        SourceBlock[] sources = new SourceBlock[3];
        int error = 0;
        for (int channel = 0; channel < 3; channel++) {
          // grab the lookup table and index for this channel
          SingleColorLookup[] lookup = lookups[channel];
          int target = color[channel];

          // store a pointer to the source for this channel
          sources[channel] = lookup[target].sources[index];

          // accumulate the error
          int diff = sources[channel].error;
          error += diff * diff;
        }

        // keep it if the error is lower
        if (error < this.error) {
          start = new Vec3((float)sources[0].start / 31.0f,
                           (float)sources[1].start / 63.0f,
                           (float)sources[2].start / 31.0f);
          end = new Vec3((float)sources[0].end / 31.0f,
                         (float)sources[1].end / 63.0f,
                         (float)sources[2].end / 31.0f);
          this.index = 2 * index;
          this.error = error;
        }
      }
    }
  }

  private static class RangeFit extends ColorFit
  {
    private Vec3 metric, start, end;
    private float bestError;

    public RangeFit(ColorSet colors, DxtType dxtType)
    {
      super(colors, dxtType);

      // using perceptual metric for color error
      metric = new Vec3(0.2126f, 0.7152f, 0.0722f);

      // initialize the best error
      bestError = Float.MAX_VALUE;

      // cache some values
      int count = this.colors.getCount();
      Vec3[] values = this.colors.getPoints();
      float[] weights = this.colors.getWeights();

      // get the covariance matrix
      Sym3x3 covariance = Sym3x3.computeWeightedCovariance(count, values, weights);

      // compute the principle component
      Vec3 principle = Sym3x3.computePrincipleComponent(covariance);

      // get the min and max range as the codebook endpoints
      Vec3 start = new Vec3();
      Vec3 end = new Vec3();
      if (count > 0) {
        float min, max;

        // compute the range
        start = end = values[0];
        min = max = Vec3.dot(values[0], principle);
        for (int i = 1; i < count; i++) {
          float val = Vec3.dot(values[i], principle);
          if (val < min) {
            start = values[i];
            min = val;
          } else if (val > max) {
            end = values[i];
            max = val;
          }
        }
      }

      // clamp the output to [0, 1]
      final Vec3 one = new Vec3(1.0f);
      final Vec3 zero = new Vec3(0.0f);
      start = Vec3.min(one, Vec3.max(zero, start));
      end = Vec3.min(one, Vec3.max(zero, end));

      // clamp the grid and save
      final Vec3 grid = new Vec3(31.0f, 63.0f, 31.0f);
      final Vec3 gridrcp = new Vec3(1.0f/31.0f, 1.0f/63.0f, 1.0f/31.0f);
      final Vec3 half = new Vec3(0.5f);
      this.start = Vec3.truncate(Vec3.mul(grid, start).add(half)).mul(gridrcp);
      this.end = Vec3.truncate(Vec3.mul(grid, end).add(half)).mul(gridrcp);
    }

    @Override
    protected void compress3(byte[] block)
    {
      // cache some values
      int count = colors.getCount();
      Vec3[] values = colors.getPoints();

      // create a codebook
      Vec3[] codes = new Vec3[3];
      codes[0] = start;
      codes[1] = end;
      codes[2] = Vec3.mul(start, 0.5f).add(Vec3.mul(end, 0.5f));

      // match each point to the closest code
      int[] closest = new int[16];
      float error = 0.0f;
      for (int i = 0; i < count; i++) {
        // find the closest code
        float dist = Float.MAX_VALUE;
        int idx = 0;
        for (int j = 0; j < 3; j++) {
          float d = Vec3.lengthSquared(Vec3.mul(metric, Vec3.sub(values[i], codes[j])));
          if (d < dist) {
            dist = d;
            idx = j;
          }
        }

        // save the index
        closest[i] = idx;

        // accumulate the error
        error += dist;
      }

      // save this scheme if it wins
      if (error < bestError) {
        // remap the indices
        int[] indices = new int[16];
        colors.remapIndices(closest, indices);

        // save the block
        ColorBlock.writeColorBlock3(start, end, indices, block);

        // save the error
        bestError = error;
      }
    }

    @Override
    protected void compress4(byte[] block)
    {
      // cache some values
      int count = colors.getCount();
      Vec3[] values = colors.getPoints();

      // create a codebook
      Vec3[] codes = new Vec3[4];
      codes[0] = start;
      codes[1] = end;
      codes[2] = Vec3.mul(start, 2.0f/3.0f).add(Vec3.mul(end, 1.0f/3.0f));
      codes[3] = Vec3.mul(start, 1.0f/3.0f).add(Vec3.mul(end, 2.0f/3.0f));

      // match each point to the closest code
      int[] closest = new int[16];
      float error = 0.0f;
      for (int i = 0; i < count; i++) {
        // find the closest code
        float dist = Float.MAX_VALUE;
        int idx = 0;
        for (int j = 0; j < 4; j++) {
          float d = Vec3.lengthSquared(Vec3.mul(metric, Vec3.sub(values[i], codes[j])));
          if (d < dist) {
            dist = d;
            idx = j;
          }
        }

        // save the index
        closest[i] = idx;

        // accumulate the error
        error += dist;
      }

      // save this scheme if it wins
      if (error < bestError) {
        // remap the indices
        int[] indices = new int[16];
        colors.remapIndices(closest, indices);

        // save the block
        ColorBlock.writeColorBlock4(start, end, indices, block);

        // save the error
        bestError = error;
      }
    }
  }

  private static class ClusterFit extends ColorFit
  {
    private static final int IterationCount = 8;

    private final int[] order;
    private final Vec4[] pointsWeights;

    private Vec3 principle;
    private Vec4 xsum_wsum;
    private Vec4 metric;
    private Vec4 bestError;

    public ClusterFit(ColorSet colors, DxtType dxtType)
    {
      super(colors, dxtType);
      order = new int[16*IterationCount];
      pointsWeights = new Vec4[16];

      // initialize the best error
      bestError = new Vec4(Float.MAX_VALUE);

      // using perceptual metric for color error
      metric = new Vec4(0.2126f, 0.7152f, 0.0722f, 0.0f);

      // get the covariance matrix
      Sym3x3 covariance = Sym3x3.computeWeightedCovariance(this.colors.getCount(),
                                                           this.colors.getPoints(),
                                                           this.colors.getWeights());

      // compute the principle component
      principle = Sym3x3.computePrincipleComponent(covariance);
    }

    @Override
    protected void compress3(byte[] block)
    {
      // declare variables
      final int count = colors.getCount();
      final Vec4 two = new Vec4(2.0f);
      final Vec4 one = new Vec4(1.0f);
      final Vec4 halfHalf2 = new Vec4(0.5f, 0.5f, 0.5f, 0.25f);
      final Vec4 zero = new Vec4(0.0f);
      final Vec4 half = new Vec4(0.5f);
      final Vec4 grid = new Vec4(31.0f, 63.0f, 31.0f, 0.0f);
      final Vec4 gridrcp = new Vec4(1.0f/31.0f, 1.0f/63.0f, 1.0f/31.0f, 0.0f);

      // prepare an ordering using the principle axis
      constructOrdering(principle, 0);

      // check all possible clusters and iterate on the total order
      Vec4 bestStart = new Vec4();
      Vec4 bestEnd = new Vec4();
      Vec4 bestError = this.bestError;
      int[] bestIndices = new int[16];
      int bestIteration = 0;
      int bestI = 0, bestJ = 0;

      // loop over iterations (we avoid the case that all points in first or last cluster)
      for (int iterIndex = 0; ; ) {
        // first cluster [0, i) is at the start
        Vec4 part0 = new Vec4();
        for (int i = 0; i < count; i++) {
          // second cluster [i, j) is half along
          Vec4 part1 = (i == 0) ? (Vec4)pointsWeights[0].clone() : new Vec4();
          int jmin = (i == 0) ? 1 : i;
          for (int j = jmin; ; ) {
            // last cluster [j, count) is at the end
            Vec4 part2 = Vec4.sub(xsum_wsum, part1).sub(part0);

            // compute least squares terms directly
            Vec4 alphaXSum = Vec4.multiplyAdd(part1, halfHalf2, part0);
            Vec4 alpha2Sum = alphaXSum.splatW();

            Vec4 betaXSum = Vec4.multiplyAdd(part1, halfHalf2, part2);
            Vec4 beta2Sum = betaXSum.splatW();

            Vec4 alphaBetaSum = Vec4.mul(part1, halfHalf2).splatW();

            // compute the least squares optimal points
            Vec4 factor = Vec4.reciprocal(Vec4.negMulSub(alphaBetaSum, alphaBetaSum, Vec4.mul(alpha2Sum, beta2Sum)));
            Vec4 a = Vec4.negMulSub(betaXSum, alphaBetaSum, Vec4.mul(alphaXSum, beta2Sum)).mul(factor);
            Vec4 b = Vec4.negMulSub(alphaXSum, alphaBetaSum, Vec4.mul(betaXSum, alpha2Sum)).mul(factor);

            // clamp to the grid
            a = Vec4.min(one, Vec4.max(zero, a));
            b = Vec4.min(one, Vec4.max(zero, b));
            a = Vec4.truncate(Vec4.multiplyAdd(grid, a, half)).mul(gridrcp);
            b = Vec4.truncate(Vec4.multiplyAdd(grid, b, half)).mul(gridrcp);

            // compute the error (we skip the constant xxsum)
            Vec4 e1 = Vec4.multiplyAdd(Vec4.mul(a, a), alpha2Sum, Vec4.mul(b, b).mul(beta2Sum));
            Vec4 e2 = Vec4.negMulSub(a, alphaXSum, Vec4.mul(a, b).mul(alphaBetaSum));
            Vec4 e3 = Vec4.negMulSub(b, betaXSum, e2);
            Vec4 e4 = Vec4.multiplyAdd(two, e3, e1);

            // apply the metric to the error terms
            Vec4 e5 = Vec4.mul(e4, metric);
            Vec4 error = Vec4.add(e5.splatX(), e5.splatY()).add(e5.splatZ());

            // keep the solution if it wins
            if (Vec4.compareAnyLessThan(error, bestError)) {
              bestStart = a;
              bestEnd = b;
              bestI = i;
              bestJ = j;
              bestError = error;
              bestIteration = iterIndex;
            }

            // advance
            if (j == count)
              break;
            part1.add(pointsWeights[j]);
            j++;
          }
          // advance
          part0.add(pointsWeights[i]);
        }
        // stop if we didn't improve in this iteration
        if (bestIteration != iterIndex)
          break;

        // advance if possible
        iterIndex++;
        if (iterIndex == IterationCount)
          break;

        // stop if a new iteration is an ordering that has already been tried
        Vec3 axis = Vec4.sub(bestEnd, bestStart).getVec3();
        if (!constructOrdering(axis, iterIndex))
          break;
      }

      // save the block if necessary
      if (Vec4.compareAnyLessThan(bestError, this.bestError)) {
        // remap the indices
        int orderIdx = 16*bestIteration;
        int[] unordered = new int[16];
        for (int m = 0; m < bestI; m++)
          unordered[order[orderIdx+m]] = 0;
        for (int m = bestI; m < bestJ; m++)
          unordered[order[orderIdx+m]] = 2;
        for (int m = bestJ; m < count; m++)
          unordered[order[orderIdx+m]] = 1;

        colors.remapIndices(unordered, bestIndices);

        // save the block
        ColorBlock.writeColorBlock3(bestStart.getVec3(), bestEnd.getVec3(), bestIndices, block);

        // save the error
        this.bestError = bestError;
      }
    }

    @Override
    protected void compress4(byte[] block)
    {
      // declare variables
      final int count = colors.getCount();
      final Vec4 two = new Vec4(2.0f);
      final Vec4 one = new Vec4(1.0f);
      final Vec4 oneThirdOneThird2 = new Vec4(1.0f/3.0f, 1.0f/3.0f, 1.0f/3.0f, 1.0f/9.0f);
      final Vec4 twoThirdsTwoThirds2 = new Vec4(2.0f/3.0f, 2.0f/3.0f, 2.0f/3.0f, 4.0f/9.0f);
      final Vec4 twoNineth = new Vec4(2.0f/9.0f);
      final Vec4 zero = new Vec4(0.0f);
      final Vec4 half = new Vec4(0.5f);
      final Vec4 grid = new Vec4(31.0f, 63.0f, 31.0f, 0.0f);
      final Vec4 gridrcp = new Vec4(1.0f/31.0f, 1.0f/63.0f, 1.0f/31.0f, 0.0f);

      // prepare an ordering using the principle axis
      constructOrdering(principle, 0);

      // check all possible clusters and iterate on the total order
      Vec4 bestStart = new Vec4();
      Vec4 bestEnd = new Vec4();
      Vec4 bestError = this.bestError;
      int[] bestIndices = new int[16];
      int bestIteration = 0;
      int bestI = 0, bestJ = 0, bestK = 0;

      // loop over iterations (we avoid the case all points in first or last cluster)
      for (int iterIndex = 0; ; ) {
        // first cluster [0, i) is at the start
        Vec4 part0 = new Vec4();
        for (int i = 0; i < count; i++) {
          // second cluster [i, j) is one third along
          Vec4 part1 = new Vec4();
          for (int j = i; ; ) {
            // third cluster [j, k) is two thirds along
            Vec4 part2 = (j == 0) ? (Vec4)pointsWeights[0].clone() : new Vec4();
            int kmin = (j == 0) ? 1 : j;
            for (int k = kmin; ; ) {
              // last cluster [k, count) is at the end
              Vec4 part3 = Vec4.sub(xsum_wsum, part2).sub(part1).sub(part0);

              // compute least squares terms directly
              Vec4 alphaXSum = Vec4.multiplyAdd(part2, oneThirdOneThird2,
                                                Vec4.multiplyAdd(part1, twoThirdsTwoThirds2, part0));
              Vec4 alpha2Sum = alphaXSum.splatW();

              Vec4 betaXSum = Vec4.multiplyAdd(part1, oneThirdOneThird2,
                                               Vec4.multiplyAdd(part2, twoThirdsTwoThirds2, part3));
              Vec4 beta2Sum = betaXSum.splatW();

              Vec4 alphaBetaSum = Vec4.mul(twoNineth, Vec4.add(part1, part2).splatW());

              // compute the least-squares optimal points
              Vec4 factor = Vec4.reciprocal(Vec4.negMulSub(alphaBetaSum, alphaBetaSum,
                                            Vec4.mul(alpha2Sum, beta2Sum)));
              Vec4 a = Vec4.negMulSub(betaXSum, alphaBetaSum, Vec4.mul(alphaXSum, beta2Sum)).mul(factor);
              Vec4 b = Vec4.negMulSub(alphaXSum, alphaBetaSum, Vec4.mul(betaXSum, alpha2Sum)).mul(factor);

              // clamp to the grid
              a = Vec4.min(one, Vec4.max(zero, a));
              b = Vec4.min(one, Vec4.max(zero, b));
              a = Vec4.truncate(Vec4.multiplyAdd(grid, a, half)).mul(gridrcp);
              b = Vec4.truncate(Vec4.multiplyAdd(grid, b, half)).mul(gridrcp);

              // compute the error (we skip the constant xxsum)
              Vec4 e1 = Vec4.multiplyAdd(Vec4.mul(a, a), alpha2Sum, Vec4.mul(b, b).mul(beta2Sum));
              Vec4 e2 = Vec4.negMulSub(a, alphaXSum, Vec4.mul(a, b).mul(alphaBetaSum));
              Vec4 e3 = Vec4.negMulSub(b, betaXSum, e2);
              Vec4 e4 = Vec4.multiplyAdd(two, e3, e1);

              // apply the metric to the error term
              Vec4 e5 = Vec4.mul(e4, metric);
              Vec4 error = Vec4.add(e5.splatX(), e5.splatY()).add(e5.splatZ());

              // keep the solution if it wins
              if (Vec4.compareAnyLessThan(error, bestError)) {
                bestStart = a;
                bestEnd = b;
                bestError = error;
                bestI = i;
                bestJ = j;
                bestK = k;
                bestIteration = iterIndex;
              }
              // advance
              if (k == count)
                break;
              part2.add(pointsWeights[k]);
              k++;
            }
            // advance
            if (j == count)
              break;
            part1.add(pointsWeights[j]);
            j++;
          }
          // advance
          part0.add(pointsWeights[i]);
        }
        // stop if we didn't improve in this iteration
        if (bestIteration != iterIndex)
          break;

        // advance if possible
        iterIndex++;
        if (iterIndex == IterationCount)
          break;

        // stop if a new iteration is an ordering that has already been tried
        Vec3 axis = Vec4.sub(bestEnd, bestStart).getVec3();
        if (!constructOrdering(axis, iterIndex))
          break;
      }

      // save the block if necessary
      if (Vec4.compareAnyLessThan(bestError, this.bestError)) {
        // remap the indices
        int orderIdx = 16*bestIteration;
        int[] unordered = new int[16];
        for (int m = 0; m < bestI; m++)
          unordered[order[orderIdx+m]] = 0;
        for (int m = bestI; m < bestJ; m++)
          unordered[order[orderIdx+m]] = 2;
        for (int m = bestJ; m < bestK; m++)
          unordered[order[orderIdx+m]] = 3;
        for (int m = bestK; m < count; m++)
          unordered[order[orderIdx+m]] = 1;

        colors.remapIndices(unordered, bestIndices);

        // save the block
        ColorBlock.writeColorBlock4(bestStart.getVec3(), bestEnd.getVec3(), bestIndices, block);

        // save the error
        this.bestError = bestError;
      }
    }

    private boolean constructOrdering(Vec3 axis, int iteration)
    {
      // cache some values
      final int count = colors.getCount();
      final Vec3[] values = colors.getPoints();

      // build list of dot products
      float[] dps = new float[16];
      int orderIdx = 16*iteration;
      for (int i = 0; i < count; i++) {
        dps[i] = Vec3.dot(values[i], axis);
        order[orderIdx+i] = i;
      }

      // stable sort using them
      for (int i = 0; i < count; i++) {
        for (int j = i; j > 0 && dps[j] < dps[j-1]; j--) {
          float tmp1 = dps[j]; dps[j] = dps[j-1]; dps[j-1] = tmp1;
          int tmp2 = order[orderIdx+j]; order[orderIdx+j] = order[orderIdx+j-1]; order[orderIdx+j-1] = tmp2;
        }
      }

      // check this ordering is unique
      for (int it = 0; it < iteration; it++) {
        int prevIdx = 16*it;
        boolean same = true;
        for (int i = 0; i < count; i++) {
          if (order[orderIdx+i] != order[prevIdx+i]) {
            same = false;
            break;
          }
        }
        if (same)
          return false;
      }

      // copy the ordering and weight all the points
      final Vec3[] unweighted = colors.getPoints();
      final float[] weights = colors.getWeights();
      xsum_wsum = new Vec4();
      for (int i = 0; i < count; i++) {
        int j = order[orderIdx+i];
        Vec4 p = new Vec4(unweighted[j].x(), unweighted[j].y(), unweighted[j].z(), 1.0f);
        Vec4 w = new Vec4(weights[j]);
        Vec4 x = Vec4.mul(p, w);
        pointsWeights[i] = x;
        xsum_wsum.add(x);
      }
      return true;
    }
  }


  private static final class ColorBlock
  {
    public static void writeColorBlock3(Vec3 start, Vec3 end, int[] indices, byte[] block)
    {
      // get the packed values
      int a = floatTo565(start);
      int b = floatTo565(end);

      // remap the indices
      int[] remapped = new int[16];
      if (a <= b) {
        // use the indices directly
        for (int i = 0; i < 16; i++) {
          remapped[i] = indices[i];
        }
      } else {
        // swap a and b
        int tmp = a; a = b; b = tmp;
        for (int i = 0; i < 16; i++) {
          if (indices[i] == 0) {
            remapped[i] = 1;
          } else if (indices[i] == 1) {
            remapped[i] = 0;
          } else {
            remapped[i] = indices[i];
          }
        }
      }

      // write the block
      writeColorBlock(a, b, remapped, block);
    }

    public static void writeColorBlock4(Vec3 start, Vec3 end, int[] indices, byte[] block)
    {
      // get the packed values
      int a = floatTo565(start);
      int b = floatTo565(end);

      // remap the indices
      int[] remapped = new int[16];
      if (a < b) {
        // swap a and b
        int tmp = a; a = b; b = tmp;
        for (int i = 0; i < 16; i++) {
          remapped[i] = (indices[i] ^ 1) & 3;
        }
      } else if (a == b) {
        // use index 0
        for (int i = 0; i < 16; i++) {
          remapped[i] = 0;
        }
      } else {
        // use the indices directly
        for (int i = 0; i < 16; i++)
          remapped[i] = indices[i];
      }

      // write the block
      writeColorBlock(a, b, remapped, block);
    }

    private static int floatTo565(Vec3 color)
    {
      // get the components in the correct range
      int r = Misc.floatToInt(31.0f*color.x(), 31);
      int g = Misc.floatToInt(63.0f*color.y(), 63);
      int b = Misc.floatToInt(31.0f*color.z(), 31);

      // pack the color into a single value
      return ((r << 11) | (g << 5) | b) & 0xffff;
    }

    private static void writeColorBlock(int a, int b, int[] indices, byte[] block)
    {
      // write the endpoints
      block[0] = (byte)(a & 0xff);
      block[1] = (byte)((a >>> 8) & 0xff);
      block[2] = (byte)(b & 0xff);
      block[3] = (byte)((b >>> 8) & 0xff);

      // write the indices
      for (int i = 0; i < 4; i++) {
        int idx = 4*i;
        block[i+4] = (byte)((indices[idx+0]) | (indices[idx+1] << 2) |
                            (indices[idx+2] << 4) | (indices[idx+3] << 6));
      }
    }
  }

  private static final class Alpha
  {
    public static void compressAlphaDxt3(int[] pixels, byte[] block)
    {
      // quantize and pack the alpha values pairwise
      for (int i = 0; i < 8; i++) {
        // quantize down to 4 bits
        float alpha1 = (float)ColorSet.argb(pixels[2*i], 3) * (15.0f / 255.0f);
        float alpha2 = (float)ColorSet.argb(pixels[2*i+1], 3) * (15.0f / 255.0f);
        int quant1 = Misc.floatToInt(alpha1, 15);
        int quant2 = Misc.floatToInt(alpha2, 15);

        // pack into the byte
        block[i] = (byte)(quant1 | (quant2 << 4));
      }
    }

    public static void compressAlphaDxt5(int[] pixels, byte[] block)
    {
      // get the range for 5-alpha and 7-alpha interpolation
      int min5 = 255;
      int max5 = 0;
      int min7 = 255;
      int max7 = 0;
      for (int i = 0; i < 16; i++) {
        // incorporate into the min/max
        int value = ColorSet.argb(pixels[i], 3);
        if (value < min7)
          min7 = value;
        if (value > max7)
          max7 = value;
        if (value != 0 && value < min5)
          min5 = value;
        if (value != 255 && value > max5)
          max5 = value;
      }

      // handle the case that no valid range was found
      if (min5 > max5)
        min5 = max5;
      if (min7 > max7)
        min7 = max7;

      // fix the range to be the minimum in each case
      int[] minmax = new int[2];
      minmax[0] = min5; minmax[1] = max5;
      fixRange(minmax, 5);
      min5 = minmax[0]; max5 = minmax[1];
      minmax[0] = min7; minmax[1] = max7;
      fixRange(minmax, 7);
      min7 = minmax[0]; max7 = minmax[1];

      // set up the 5-alpha code book
      int[] codes5 = new int[8];
      codes5[0] = min5;
      codes5[1] = max5;
      for (int i = 1; i < 5; i++) {
        codes5[i+1] = ((5 - i)*min5 + i*max5) / 5;
      }
      codes5[6] = 0;
      codes5[7] = 255;

      // set up the 7-alpha code book
      int[] codes7 = new int[8];
      codes7[0] = min7;
      codes7[1] = max7;
      for (int i = 1; i < 7; i++) {
        codes7[i+1] = ((7 - i)*min7 + i*max7) / 7;
      }

      // fit the data to both code books
      int[] indices5 = new int[16];
      int[] indices7 = new int[16];
      int err5 = fitCodes(pixels, codes5, indices5);
      int err7 = fitCodes(pixels, codes7, indices7);

      // save the block with the least error
      if (err5 <= err7) {
        writeAlphaBlock5(min5, max5, indices5, block);
      } else {
        writeAlphaBlock7(min7, max7, indices7, block);
      }
    }

    private static void fixRange(int[] minMax, int steps)
    {
      if (minMax[1] - minMax[0] < steps)
        minMax[1] = Math.min(minMax[0] + steps, 255);
      if (minMax[1] - minMax[0] < steps)
        minMax[0] = Math.max(0, minMax[1] - steps);
    }

    private static int fitCodes(int[] pixels, int[] codes, int[] indices)
    {
      // fit each alpha value to the code book
      int err = 0;
      for (int i = 0; i < 16; i++) {
        // find the least error and corresponding index
        int value = ColorSet.argb(pixels[i], 3);
        int least = Integer.MAX_VALUE;
        int index = 0;
        for (int j = 0; j < 8; j++) {
          // get the squared error from this code
          int dist = value - codes[j];
          dist *= dist;

          // compare with the best so far
          if (dist < least) {
            least = dist;
            index = j;
          }
        }

        // save this index and accumulate the error
        indices[i] = index;
        err += least;
      }

      // return the total error
      return err;
    }

    private static void writeAlphaBlock(int alpha0, int alpha1, int[] indices, byte[] block)
    {
      // write the first two bytes
      block[0] = (byte)(alpha0 & 0xff);
      block[1] = (byte)(alpha1 & 0xff);

      // pack the indices with 3 bits each
      for (int i = 0, srcIdx = 0, dstIdx = 2; i < 2; i++) {
        // pack 8 3-bit values
        int value = 0;
        for (int j = 0; j < 8; j++) {
          int index = indices[srcIdx++];
          value |= (index << (3*j));
        }

        // store in 3 bytes
        for (int j = 0; j < 3; j++) {
          byte b = (byte)((value >>> (8*j)) & 0xff);
          block[dstIdx++] = b;
        }
      }
    }

    private static void writeAlphaBlock5(int alpha0, int alpha1, int[] indices, byte[] block)
    {
      // check the relative values of the endpoints
      if (alpha0 > alpha1) {
        // swap the indices
        int[] swapped = new int[16];
        for (int i = 0; i < 16; i++) {
          int index = indices[i];
          if (index == 0) {
            swapped[i] = 1;
          } else if (index == 1) {
            swapped[i] = 0;
          } else if (index <= 5) {
            swapped[i] = 7 - index;
          } else {
            swapped[i] = index;
          }
        }

        // write the block
        writeAlphaBlock(alpha1, alpha0, swapped, block);

      } else {
        // write the block
        writeAlphaBlock(alpha0, alpha1, indices, block);
      }
    }

    private static void writeAlphaBlock7(int alpha0, int alpha1, int[] indices, byte[] block)
    {
      if (alpha0 < alpha1) {
        // swap the indices
        int[] swapped = new int[16];
        for (int i = 0; i < 16; i++) {
          int index = indices[i];
          if (index == 0) {
            swapped[i] = 1;
          } else if (index == 1) {
            swapped[i] = 0;
          } else {
            swapped[i] = 9 - index;
          }
        }

        // write the block
        writeAlphaBlock(alpha1, alpha0, swapped, block);
      } else {
        // write the block
        writeAlphaBlock(alpha0, alpha1, indices, block);
      }
    }
  }


  private static final class SourceBlock
  {
    public final int start, end, error;

    public SourceBlock(int start, int end, int error)
    {
      this.start = start;
      this.end = end;
      this.error = error;
    }
  }

  private static final class SingleColorLookup
  {
    public final SourceBlock[] sources;

    public SingleColorLookup(SourceBlock sb1, SourceBlock sb2)
    {
      sources = new SourceBlock[]{sb1, sb2};
    }
  }

  private static final class Lookups
  {
    public static int SIZE = 256;
    public static SingleColorLookup[] lookup53 = new SingleColorLookup[SIZE];
    public static SingleColorLookup[] lookup63 = new SingleColorLookup[SIZE];
    public static SingleColorLookup[] lookup54 = new SingleColorLookup[SIZE];
    public static SingleColorLookup[] lookup64 = new SingleColorLookup[SIZE];
    static {
      lookup53[0] = new SingleColorLookup(new SourceBlock(0, 0, 0), new SourceBlock(0, 0, 0));
      lookup53[1] = new SingleColorLookup(new SourceBlock(0, 0, 1), new SourceBlock(0, 0, 1));
      lookup53[2] = new SingleColorLookup(new SourceBlock(0, 0, 2), new SourceBlock(0, 0, 2));
      lookup53[3] = new SingleColorLookup(new SourceBlock(0, 0, 3), new SourceBlock(0, 1, 1));
      lookup53[4] = new SingleColorLookup(new SourceBlock(0, 0, 4), new SourceBlock(0, 1, 0));
      lookup53[5] = new SingleColorLookup(new SourceBlock(1, 0, 3), new SourceBlock(0, 1, 1));
      lookup53[6] = new SingleColorLookup(new SourceBlock(1, 0, 2), new SourceBlock(0, 1, 2));
      lookup53[7] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(0, 2, 1));
      lookup53[8] = new SingleColorLookup(new SourceBlock(1, 0, 0), new SourceBlock(0, 2, 0));
      lookup53[9] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(0, 2, 1));
      lookup53[10] = new SingleColorLookup(new SourceBlock(1, 0, 2), new SourceBlock(0, 2, 2));
      lookup53[11] = new SingleColorLookup(new SourceBlock(1, 0, 3), new SourceBlock(0, 3, 1));
      lookup53[12] = new SingleColorLookup(new SourceBlock(1, 0, 4), new SourceBlock(0, 3, 0));
      lookup53[13] = new SingleColorLookup(new SourceBlock(2, 0, 3), new SourceBlock(0, 3, 1));
      lookup53[14] = new SingleColorLookup(new SourceBlock(2, 0, 2), new SourceBlock(0, 3, 2));
      lookup53[15] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(0, 4, 1));
      lookup53[16] = new SingleColorLookup(new SourceBlock(2, 0, 0), new SourceBlock(0, 4, 0));
      lookup53[17] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(0, 4, 1));
      lookup53[18] = new SingleColorLookup(new SourceBlock(2, 0, 2), new SourceBlock(0, 4, 2));
      lookup53[19] = new SingleColorLookup(new SourceBlock(2, 0, 3), new SourceBlock(0, 5, 1));
      lookup53[20] = new SingleColorLookup(new SourceBlock(2, 0, 4), new SourceBlock(0, 5, 0));
      lookup53[21] = new SingleColorLookup(new SourceBlock(3, 0, 3), new SourceBlock(0, 5, 1));
      lookup53[22] = new SingleColorLookup(new SourceBlock(3, 0, 2), new SourceBlock(0, 5, 2));
      lookup53[23] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 6, 1));
      lookup53[24] = new SingleColorLookup(new SourceBlock(3, 0, 0), new SourceBlock(0, 6, 0));
      lookup53[25] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 6, 1));
      lookup53[26] = new SingleColorLookup(new SourceBlock(3, 0, 2), new SourceBlock(0, 6, 2));
      lookup53[27] = new SingleColorLookup(new SourceBlock(3, 0, 3), new SourceBlock(0, 7, 1));
      lookup53[28] = new SingleColorLookup(new SourceBlock(3, 0, 4), new SourceBlock(0, 7, 0));
      lookup53[29] = new SingleColorLookup(new SourceBlock(4, 0, 4), new SourceBlock(0, 7, 1));
      lookup53[30] = new SingleColorLookup(new SourceBlock(4, 0, 3), new SourceBlock(0, 7, 2));
      lookup53[31] = new SingleColorLookup(new SourceBlock(4, 0, 2), new SourceBlock(1, 7, 1));
      lookup53[32] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(1, 7, 0));
      lookup53[33] = new SingleColorLookup(new SourceBlock(4, 0, 0), new SourceBlock(0, 8, 0));
      lookup53[34] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(0, 8, 1));
      lookup53[35] = new SingleColorLookup(new SourceBlock(4, 0, 2), new SourceBlock(2, 7, 1));
      lookup53[36] = new SingleColorLookup(new SourceBlock(4, 0, 3), new SourceBlock(2, 7, 0));
      lookup53[37] = new SingleColorLookup(new SourceBlock(4, 0, 4), new SourceBlock(0, 9, 0));
      lookup53[38] = new SingleColorLookup(new SourceBlock(5, 0, 3), new SourceBlock(0, 9, 1));
      lookup53[39] = new SingleColorLookup(new SourceBlock(5, 0, 2), new SourceBlock(3, 7, 1));
      lookup53[40] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(3, 7, 0));
      lookup53[41] = new SingleColorLookup(new SourceBlock(5, 0, 0), new SourceBlock(0, 10, 0));
      lookup53[42] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(0, 10, 1));
      lookup53[43] = new SingleColorLookup(new SourceBlock(5, 0, 2), new SourceBlock(0, 10, 2));
      lookup53[44] = new SingleColorLookup(new SourceBlock(5, 0, 3), new SourceBlock(0, 11, 1));
      lookup53[45] = new SingleColorLookup(new SourceBlock(5, 0, 4), new SourceBlock(0, 11, 0));
      lookup53[46] = new SingleColorLookup(new SourceBlock(6, 0, 3), new SourceBlock(0, 11, 1));
      lookup53[47] = new SingleColorLookup(new SourceBlock(6, 0, 2), new SourceBlock(0, 11, 2));
      lookup53[48] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(0, 12, 1));
      lookup53[49] = new SingleColorLookup(new SourceBlock(6, 0, 0), new SourceBlock(0, 12, 0));
      lookup53[50] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(0, 12, 1));
      lookup53[51] = new SingleColorLookup(new SourceBlock(6, 0, 2), new SourceBlock(0, 12, 2));
      lookup53[52] = new SingleColorLookup(new SourceBlock(6, 0, 3), new SourceBlock(0, 13, 1));
      lookup53[53] = new SingleColorLookup(new SourceBlock(6, 0, 4), new SourceBlock(0, 13, 0));
      lookup53[54] = new SingleColorLookup(new SourceBlock(7, 0, 3), new SourceBlock(0, 13, 1));
      lookup53[55] = new SingleColorLookup(new SourceBlock(7, 0, 2), new SourceBlock(0, 13, 2));
      lookup53[56] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 14, 1));
      lookup53[57] = new SingleColorLookup(new SourceBlock(7, 0, 0), new SourceBlock(0, 14, 0));
      lookup53[58] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 14, 1));
      lookup53[59] = new SingleColorLookup(new SourceBlock(7, 0, 2), new SourceBlock(0, 14, 2));
      lookup53[60] = new SingleColorLookup(new SourceBlock(7, 0, 3), new SourceBlock(0, 15, 1));
      lookup53[61] = new SingleColorLookup(new SourceBlock(7, 0, 4), new SourceBlock(0, 15, 0));
      lookup53[62] = new SingleColorLookup(new SourceBlock(8, 0, 4), new SourceBlock(0, 15, 1));
      lookup53[63] = new SingleColorLookup(new SourceBlock(8, 0, 3), new SourceBlock(0, 15, 2));
      lookup53[64] = new SingleColorLookup(new SourceBlock(8, 0, 2), new SourceBlock(1, 15, 1));
      lookup53[65] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(1, 15, 0));
      lookup53[66] = new SingleColorLookup(new SourceBlock(8, 0, 0), new SourceBlock(0, 16, 0));
      lookup53[67] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(0, 16, 1));
      lookup53[68] = new SingleColorLookup(new SourceBlock(8, 0, 2), new SourceBlock(2, 15, 1));
      lookup53[69] = new SingleColorLookup(new SourceBlock(8, 0, 3), new SourceBlock(2, 15, 0));
      lookup53[70] = new SingleColorLookup(new SourceBlock(8, 0, 4), new SourceBlock(0, 17, 0));
      lookup53[71] = new SingleColorLookup(new SourceBlock(9, 0, 3), new SourceBlock(0, 17, 1));
      lookup53[72] = new SingleColorLookup(new SourceBlock(9, 0, 2), new SourceBlock(3, 15, 1));
      lookup53[73] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(3, 15, 0));
      lookup53[74] = new SingleColorLookup(new SourceBlock(9, 0, 0), new SourceBlock(0, 18, 0));
      lookup53[75] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(0, 18, 1));
      lookup53[76] = new SingleColorLookup(new SourceBlock(9, 0, 2), new SourceBlock(0, 18, 2));
      lookup53[77] = new SingleColorLookup(new SourceBlock(9, 0, 3), new SourceBlock(0, 19, 1));
      lookup53[78] = new SingleColorLookup(new SourceBlock(9, 0, 4), new SourceBlock(0, 19, 0));
      lookup53[79] = new SingleColorLookup(new SourceBlock(10, 0, 3), new SourceBlock(0, 19, 1));
      lookup53[80] = new SingleColorLookup(new SourceBlock(10, 0, 2), new SourceBlock(0, 19, 2));
      lookup53[81] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(0, 20, 1));
      lookup53[82] = new SingleColorLookup(new SourceBlock(10, 0, 0), new SourceBlock(0, 20, 0));
      lookup53[83] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(0, 20, 1));
      lookup53[84] = new SingleColorLookup(new SourceBlock(10, 0, 2), new SourceBlock(0, 20, 2));
      lookup53[85] = new SingleColorLookup(new SourceBlock(10, 0, 3), new SourceBlock(0, 21, 1));
      lookup53[86] = new SingleColorLookup(new SourceBlock(10, 0, 4), new SourceBlock(0, 21, 0));
      lookup53[87] = new SingleColorLookup(new SourceBlock(11, 0, 3), new SourceBlock(0, 21, 1));
      lookup53[88] = new SingleColorLookup(new SourceBlock(11, 0, 2), new SourceBlock(0, 21, 2));
      lookup53[89] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(0, 22, 1));
      lookup53[90] = new SingleColorLookup(new SourceBlock(11, 0, 0), new SourceBlock(0, 22, 0));
      lookup53[91] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(0, 22, 1));
      lookup53[92] = new SingleColorLookup(new SourceBlock(11, 0, 2), new SourceBlock(0, 22, 2));
      lookup53[93] = new SingleColorLookup(new SourceBlock(11, 0, 3), new SourceBlock(0, 23, 1));
      lookup53[94] = new SingleColorLookup(new SourceBlock(11, 0, 4), new SourceBlock(0, 23, 0));
      lookup53[95] = new SingleColorLookup(new SourceBlock(12, 0, 4), new SourceBlock(0, 23, 1));
      lookup53[96] = new SingleColorLookup(new SourceBlock(12, 0, 3), new SourceBlock(0, 23, 2));
      lookup53[97] = new SingleColorLookup(new SourceBlock(12, 0, 2), new SourceBlock(1, 23, 1));
      lookup53[98] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(1, 23, 0));
      lookup53[99] = new SingleColorLookup(new SourceBlock(12, 0, 0), new SourceBlock(0, 24, 0));
      lookup53[100] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(0, 24, 1));
      lookup53[101] = new SingleColorLookup(new SourceBlock(12, 0, 2), new SourceBlock(2, 23, 1));
      lookup53[102] = new SingleColorLookup(new SourceBlock(12, 0, 3), new SourceBlock(2, 23, 0));
      lookup53[103] = new SingleColorLookup(new SourceBlock(12, 0, 4), new SourceBlock(0, 25, 0));
      lookup53[104] = new SingleColorLookup(new SourceBlock(13, 0, 3), new SourceBlock(0, 25, 1));
      lookup53[105] = new SingleColorLookup(new SourceBlock(13, 0, 2), new SourceBlock(3, 23, 1));
      lookup53[106] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(3, 23, 0));
      lookup53[107] = new SingleColorLookup(new SourceBlock(13, 0, 0), new SourceBlock(0, 26, 0));
      lookup53[108] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(0, 26, 1));
      lookup53[109] = new SingleColorLookup(new SourceBlock(13, 0, 2), new SourceBlock(0, 26, 2));
      lookup53[110] = new SingleColorLookup(new SourceBlock(13, 0, 3), new SourceBlock(0, 27, 1));
      lookup53[111] = new SingleColorLookup(new SourceBlock(13, 0, 4), new SourceBlock(0, 27, 0));
      lookup53[112] = new SingleColorLookup(new SourceBlock(14, 0, 3), new SourceBlock(0, 27, 1));
      lookup53[113] = new SingleColorLookup(new SourceBlock(14, 0, 2), new SourceBlock(0, 27, 2));
      lookup53[114] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(0, 28, 1));
      lookup53[115] = new SingleColorLookup(new SourceBlock(14, 0, 0), new SourceBlock(0, 28, 0));
      lookup53[116] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(0, 28, 1));
      lookup53[117] = new SingleColorLookup(new SourceBlock(14, 0, 2), new SourceBlock(0, 28, 2));
      lookup53[118] = new SingleColorLookup(new SourceBlock(14, 0, 3), new SourceBlock(0, 29, 1));
      lookup53[119] = new SingleColorLookup(new SourceBlock(14, 0, 4), new SourceBlock(0, 29, 0));
      lookup53[120] = new SingleColorLookup(new SourceBlock(15, 0, 3), new SourceBlock(0, 29, 1));
      lookup53[121] = new SingleColorLookup(new SourceBlock(15, 0, 2), new SourceBlock(0, 29, 2));
      lookup53[122] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(0, 30, 1));
      lookup53[123] = new SingleColorLookup(new SourceBlock(15, 0, 0), new SourceBlock(0, 30, 0));
      lookup53[124] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(0, 30, 1));
      lookup53[125] = new SingleColorLookup(new SourceBlock(15, 0, 2), new SourceBlock(0, 30, 2));
      lookup53[126] = new SingleColorLookup(new SourceBlock(15, 0, 3), new SourceBlock(0, 31, 1));
      lookup53[127] = new SingleColorLookup(new SourceBlock(15, 0, 4), new SourceBlock(0, 31, 0));
      lookup53[128] = new SingleColorLookup(new SourceBlock(16, 0, 4), new SourceBlock(0, 31, 1));
      lookup53[129] = new SingleColorLookup(new SourceBlock(16, 0, 3), new SourceBlock(0, 31, 2));
      lookup53[130] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(1, 31, 1));
      lookup53[131] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(1, 31, 0));
      lookup53[132] = new SingleColorLookup(new SourceBlock(16, 0, 0), new SourceBlock(4, 28, 0));
      lookup53[133] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(4, 28, 1));
      lookup53[134] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(2, 31, 1));
      lookup53[135] = new SingleColorLookup(new SourceBlock(16, 0, 3), new SourceBlock(2, 31, 0));
      lookup53[136] = new SingleColorLookup(new SourceBlock(16, 0, 4), new SourceBlock(4, 29, 0));
      lookup53[137] = new SingleColorLookup(new SourceBlock(17, 0, 3), new SourceBlock(4, 29, 1));
      lookup53[138] = new SingleColorLookup(new SourceBlock(17, 0, 2), new SourceBlock(3, 31, 1));
      lookup53[139] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(3, 31, 0));
      lookup53[140] = new SingleColorLookup(new SourceBlock(17, 0, 0), new SourceBlock(4, 30, 0));
      lookup53[141] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(4, 30, 1));
      lookup53[142] = new SingleColorLookup(new SourceBlock(17, 0, 2), new SourceBlock(4, 30, 2));
      lookup53[143] = new SingleColorLookup(new SourceBlock(17, 0, 3), new SourceBlock(4, 31, 1));
      lookup53[144] = new SingleColorLookup(new SourceBlock(17, 0, 4), new SourceBlock(4, 31, 0));
      lookup53[145] = new SingleColorLookup(new SourceBlock(18, 0, 3), new SourceBlock(4, 31, 1));
      lookup53[146] = new SingleColorLookup(new SourceBlock(18, 0, 2), new SourceBlock(4, 31, 2));
      lookup53[147] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(5, 31, 1));
      lookup53[148] = new SingleColorLookup(new SourceBlock(18, 0, 0), new SourceBlock(5, 31, 0));
      lookup53[149] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(5, 31, 1));
      lookup53[150] = new SingleColorLookup(new SourceBlock(18, 0, 2), new SourceBlock(5, 31, 2));
      lookup53[151] = new SingleColorLookup(new SourceBlock(18, 0, 3), new SourceBlock(6, 31, 1));
      lookup53[152] = new SingleColorLookup(new SourceBlock(18, 0, 4), new SourceBlock(6, 31, 0));
      lookup53[153] = new SingleColorLookup(new SourceBlock(19, 0, 3), new SourceBlock(6, 31, 1));
      lookup53[154] = new SingleColorLookup(new SourceBlock(19, 0, 2), new SourceBlock(6, 31, 2));
      lookup53[155] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(7, 31, 1));
      lookup53[156] = new SingleColorLookup(new SourceBlock(19, 0, 0), new SourceBlock(7, 31, 0));
      lookup53[157] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(7, 31, 1));
      lookup53[158] = new SingleColorLookup(new SourceBlock(19, 0, 2), new SourceBlock(7, 31, 2));
      lookup53[159] = new SingleColorLookup(new SourceBlock(19, 0, 3), new SourceBlock(8, 31, 1));
      lookup53[160] = new SingleColorLookup(new SourceBlock(19, 0, 4), new SourceBlock(8, 31, 0));
      lookup53[161] = new SingleColorLookup(new SourceBlock(20, 0, 4), new SourceBlock(8, 31, 1));
      lookup53[162] = new SingleColorLookup(new SourceBlock(20, 0, 3), new SourceBlock(8, 31, 2));
      lookup53[163] = new SingleColorLookup(new SourceBlock(20, 0, 2), new SourceBlock(9, 31, 1));
      lookup53[164] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(9, 31, 0));
      lookup53[165] = new SingleColorLookup(new SourceBlock(20, 0, 0), new SourceBlock(12, 28, 0));
      lookup53[166] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(12, 28, 1));
      lookup53[167] = new SingleColorLookup(new SourceBlock(20, 0, 2), new SourceBlock(10, 31, 1));
      lookup53[168] = new SingleColorLookup(new SourceBlock(20, 0, 3), new SourceBlock(10, 31, 0));
      lookup53[169] = new SingleColorLookup(new SourceBlock(20, 0, 4), new SourceBlock(12, 29, 0));
      lookup53[170] = new SingleColorLookup(new SourceBlock(21, 0, 3), new SourceBlock(12, 29, 1));
      lookup53[171] = new SingleColorLookup(new SourceBlock(21, 0, 2), new SourceBlock(11, 31, 1));
      lookup53[172] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(11, 31, 0));
      lookup53[173] = new SingleColorLookup(new SourceBlock(21, 0, 0), new SourceBlock(12, 30, 0));
      lookup53[174] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(12, 30, 1));
      lookup53[175] = new SingleColorLookup(new SourceBlock(21, 0, 2), new SourceBlock(12, 30, 2));
      lookup53[176] = new SingleColorLookup(new SourceBlock(21, 0, 3), new SourceBlock(12, 31, 1));
      lookup53[177] = new SingleColorLookup(new SourceBlock(21, 0, 4), new SourceBlock(12, 31, 0));
      lookup53[178] = new SingleColorLookup(new SourceBlock(22, 0, 3), new SourceBlock(12, 31, 1));
      lookup53[179] = new SingleColorLookup(new SourceBlock(22, 0, 2), new SourceBlock(12, 31, 2));
      lookup53[180] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(13, 31, 1));
      lookup53[181] = new SingleColorLookup(new SourceBlock(22, 0, 0), new SourceBlock(13, 31, 0));
      lookup53[182] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(13, 31, 1));
      lookup53[183] = new SingleColorLookup(new SourceBlock(22, 0, 2), new SourceBlock(13, 31, 2));
      lookup53[184] = new SingleColorLookup(new SourceBlock(22, 0, 3), new SourceBlock(14, 31, 1));
      lookup53[185] = new SingleColorLookup(new SourceBlock(22, 0, 4), new SourceBlock(14, 31, 0));
      lookup53[186] = new SingleColorLookup(new SourceBlock(23, 0, 3), new SourceBlock(14, 31, 1));
      lookup53[187] = new SingleColorLookup(new SourceBlock(23, 0, 2), new SourceBlock(14, 31, 2));
      lookup53[188] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(15, 31, 1));
      lookup53[189] = new SingleColorLookup(new SourceBlock(23, 0, 0), new SourceBlock(15, 31, 0));
      lookup53[190] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(15, 31, 1));
      lookup53[191] = new SingleColorLookup(new SourceBlock(23, 0, 2), new SourceBlock(15, 31, 2));
      lookup53[192] = new SingleColorLookup(new SourceBlock(23, 0, 3), new SourceBlock(16, 31, 1));
      lookup53[193] = new SingleColorLookup(new SourceBlock(23, 0, 4), new SourceBlock(16, 31, 0));
      lookup53[194] = new SingleColorLookup(new SourceBlock(24, 0, 4), new SourceBlock(16, 31, 1));
      lookup53[195] = new SingleColorLookup(new SourceBlock(24, 0, 3), new SourceBlock(16, 31, 2));
      lookup53[196] = new SingleColorLookup(new SourceBlock(24, 0, 2), new SourceBlock(17, 31, 1));
      lookup53[197] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(17, 31, 0));
      lookup53[198] = new SingleColorLookup(new SourceBlock(24, 0, 0), new SourceBlock(20, 28, 0));
      lookup53[199] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(20, 28, 1));
      lookup53[200] = new SingleColorLookup(new SourceBlock(24, 0, 2), new SourceBlock(18, 31, 1));
      lookup53[201] = new SingleColorLookup(new SourceBlock(24, 0, 3), new SourceBlock(18, 31, 0));
      lookup53[202] = new SingleColorLookup(new SourceBlock(24, 0, 4), new SourceBlock(20, 29, 0));
      lookup53[203] = new SingleColorLookup(new SourceBlock(25, 0, 3), new SourceBlock(20, 29, 1));
      lookup53[204] = new SingleColorLookup(new SourceBlock(25, 0, 2), new SourceBlock(19, 31, 1));
      lookup53[205] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(19, 31, 0));
      lookup53[206] = new SingleColorLookup(new SourceBlock(25, 0, 0), new SourceBlock(20, 30, 0));
      lookup53[207] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(20, 30, 1));
      lookup53[208] = new SingleColorLookup(new SourceBlock(25, 0, 2), new SourceBlock(20, 30, 2));
      lookup53[209] = new SingleColorLookup(new SourceBlock(25, 0, 3), new SourceBlock(20, 31, 1));
      lookup53[210] = new SingleColorLookup(new SourceBlock(25, 0, 4), new SourceBlock(20, 31, 0));
      lookup53[211] = new SingleColorLookup(new SourceBlock(26, 0, 3), new SourceBlock(20, 31, 1));
      lookup53[212] = new SingleColorLookup(new SourceBlock(26, 0, 2), new SourceBlock(20, 31, 2));
      lookup53[213] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(21, 31, 1));
      lookup53[214] = new SingleColorLookup(new SourceBlock(26, 0, 0), new SourceBlock(21, 31, 0));
      lookup53[215] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(21, 31, 1));
      lookup53[216] = new SingleColorLookup(new SourceBlock(26, 0, 2), new SourceBlock(21, 31, 2));
      lookup53[217] = new SingleColorLookup(new SourceBlock(26, 0, 3), new SourceBlock(22, 31, 1));
      lookup53[218] = new SingleColorLookup(new SourceBlock(26, 0, 4), new SourceBlock(22, 31, 0));
      lookup53[219] = new SingleColorLookup(new SourceBlock(27, 0, 3), new SourceBlock(22, 31, 1));
      lookup53[220] = new SingleColorLookup(new SourceBlock(27, 0, 2), new SourceBlock(22, 31, 2));
      lookup53[221] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(23, 31, 1));
      lookup53[222] = new SingleColorLookup(new SourceBlock(27, 0, 0), new SourceBlock(23, 31, 0));
      lookup53[223] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(23, 31, 1));
      lookup53[224] = new SingleColorLookup(new SourceBlock(27, 0, 2), new SourceBlock(23, 31, 2));
      lookup53[225] = new SingleColorLookup(new SourceBlock(27, 0, 3), new SourceBlock(24, 31, 1));
      lookup53[226] = new SingleColorLookup(new SourceBlock(27, 0, 4), new SourceBlock(24, 31, 0));
      lookup53[227] = new SingleColorLookup(new SourceBlock(28, 0, 4), new SourceBlock(24, 31, 1));
      lookup53[228] = new SingleColorLookup(new SourceBlock(28, 0, 3), new SourceBlock(24, 31, 2));
      lookup53[229] = new SingleColorLookup(new SourceBlock(28, 0, 2), new SourceBlock(25, 31, 1));
      lookup53[230] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(25, 31, 0));
      lookup53[231] = new SingleColorLookup(new SourceBlock(28, 0, 0), new SourceBlock(28, 28, 0));
      lookup53[232] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(28, 28, 1));
      lookup53[233] = new SingleColorLookup(new SourceBlock(28, 0, 2), new SourceBlock(26, 31, 1));
      lookup53[234] = new SingleColorLookup(new SourceBlock(28, 0, 3), new SourceBlock(26, 31, 0));
      lookup53[235] = new SingleColorLookup(new SourceBlock(28, 0, 4), new SourceBlock(28, 29, 0));
      lookup53[236] = new SingleColorLookup(new SourceBlock(29, 0, 3), new SourceBlock(28, 29, 1));
      lookup53[237] = new SingleColorLookup(new SourceBlock(29, 0, 2), new SourceBlock(27, 31, 1));
      lookup53[238] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(27, 31, 0));
      lookup53[239] = new SingleColorLookup(new SourceBlock(29, 0, 0), new SourceBlock(28, 30, 0));
      lookup53[240] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(28, 30, 1));
      lookup53[241] = new SingleColorLookup(new SourceBlock(29, 0, 2), new SourceBlock(28, 30, 2));
      lookup53[242] = new SingleColorLookup(new SourceBlock(29, 0, 3), new SourceBlock(28, 31, 1));
      lookup53[243] = new SingleColorLookup(new SourceBlock(29, 0, 4), new SourceBlock(28, 31, 0));
      lookup53[244] = new SingleColorLookup(new SourceBlock(30, 0, 3), new SourceBlock(28, 31, 1));
      lookup53[245] = new SingleColorLookup(new SourceBlock(30, 0, 2), new SourceBlock(28, 31, 2));
      lookup53[246] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(29, 31, 1));
      lookup53[247] = new SingleColorLookup(new SourceBlock(30, 0, 0), new SourceBlock(29, 31, 0));
      lookup53[248] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(29, 31, 1));
      lookup53[249] = new SingleColorLookup(new SourceBlock(30, 0, 2), new SourceBlock(29, 31, 2));
      lookup53[250] = new SingleColorLookup(new SourceBlock(30, 0, 3), new SourceBlock(30, 31, 1));
      lookup53[251] = new SingleColorLookup(new SourceBlock(30, 0, 4), new SourceBlock(30, 31, 0));
      lookup53[252] = new SingleColorLookup(new SourceBlock(31, 0, 3), new SourceBlock(30, 31, 1));
      lookup53[253] = new SingleColorLookup(new SourceBlock(31, 0, 2), new SourceBlock(30, 31, 2));
      lookup53[254] = new SingleColorLookup(new SourceBlock(31, 0, 1), new SourceBlock(31, 31, 1));
      lookup53[255] = new SingleColorLookup(new SourceBlock(31, 0, 0), new SourceBlock(31, 31, 0));

      lookup63[0] = new SingleColorLookup(new SourceBlock(0, 0, 0), new SourceBlock(0, 0, 0));
      lookup63[1] = new SingleColorLookup(new SourceBlock(0, 0, 1), new SourceBlock(0, 1, 1));
      lookup63[2] = new SingleColorLookup(new SourceBlock(0, 0, 2), new SourceBlock(0, 1, 0));
      lookup63[3] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(0, 2, 1));
      lookup63[4] = new SingleColorLookup(new SourceBlock(1, 0, 0), new SourceBlock(0, 2, 0));
      lookup63[5] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(0, 3, 1));
      lookup63[6] = new SingleColorLookup(new SourceBlock(1, 0, 2), new SourceBlock(0, 3, 0));
      lookup63[7] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(0, 4, 1));
      lookup63[8] = new SingleColorLookup(new SourceBlock(2, 0, 0), new SourceBlock(0, 4, 0));
      lookup63[9] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(0, 5, 1));
      lookup63[10] = new SingleColorLookup(new SourceBlock(2, 0, 2), new SourceBlock(0, 5, 0));
      lookup63[11] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 6, 1));
      lookup63[12] = new SingleColorLookup(new SourceBlock(3, 0, 0), new SourceBlock(0, 6, 0));
      lookup63[13] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 7, 1));
      lookup63[14] = new SingleColorLookup(new SourceBlock(3, 0, 2), new SourceBlock(0, 7, 0));
      lookup63[15] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(0, 8, 1));
      lookup63[16] = new SingleColorLookup(new SourceBlock(4, 0, 0), new SourceBlock(0, 8, 0));
      lookup63[17] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(0, 9, 1));
      lookup63[18] = new SingleColorLookup(new SourceBlock(4, 0, 2), new SourceBlock(0, 9, 0));
      lookup63[19] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(0, 10, 1));
      lookup63[20] = new SingleColorLookup(new SourceBlock(5, 0, 0), new SourceBlock(0, 10, 0));
      lookup63[21] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(0, 11, 1));
      lookup63[22] = new SingleColorLookup(new SourceBlock(5, 0, 2), new SourceBlock(0, 11, 0));
      lookup63[23] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(0, 12, 1));
      lookup63[24] = new SingleColorLookup(new SourceBlock(6, 0, 0), new SourceBlock(0, 12, 0));
      lookup63[25] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(0, 13, 1));
      lookup63[26] = new SingleColorLookup(new SourceBlock(6, 0, 2), new SourceBlock(0, 13, 0));
      lookup63[27] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 14, 1));
      lookup63[28] = new SingleColorLookup(new SourceBlock(7, 0, 0), new SourceBlock(0, 14, 0));
      lookup63[29] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 15, 1));
      lookup63[30] = new SingleColorLookup(new SourceBlock(7, 0, 2), new SourceBlock(0, 15, 0));
      lookup63[31] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(0, 16, 1));
      lookup63[32] = new SingleColorLookup(new SourceBlock(8, 0, 0), new SourceBlock(0, 16, 0));
      lookup63[33] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(0, 17, 1));
      lookup63[34] = new SingleColorLookup(new SourceBlock(8, 0, 2), new SourceBlock(0, 17, 0));
      lookup63[35] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(0, 18, 1));
      lookup63[36] = new SingleColorLookup(new SourceBlock(9, 0, 0), new SourceBlock(0, 18, 0));
      lookup63[37] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(0, 19, 1));
      lookup63[38] = new SingleColorLookup(new SourceBlock(9, 0, 2), new SourceBlock(0, 19, 0));
      lookup63[39] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(0, 20, 1));
      lookup63[40] = new SingleColorLookup(new SourceBlock(10, 0, 0), new SourceBlock(0, 20, 0));
      lookup63[41] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(0, 21, 1));
      lookup63[42] = new SingleColorLookup(new SourceBlock(10, 0, 2), new SourceBlock(0, 21, 0));
      lookup63[43] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(0, 22, 1));
      lookup63[44] = new SingleColorLookup(new SourceBlock(11, 0, 0), new SourceBlock(0, 22, 0));
      lookup63[45] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(0, 23, 1));
      lookup63[46] = new SingleColorLookup(new SourceBlock(11, 0, 2), new SourceBlock(0, 23, 0));
      lookup63[47] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(0, 24, 1));
      lookup63[48] = new SingleColorLookup(new SourceBlock(12, 0, 0), new SourceBlock(0, 24, 0));
      lookup63[49] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(0, 25, 1));
      lookup63[50] = new SingleColorLookup(new SourceBlock(12, 0, 2), new SourceBlock(0, 25, 0));
      lookup63[51] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(0, 26, 1));
      lookup63[52] = new SingleColorLookup(new SourceBlock(13, 0, 0), new SourceBlock(0, 26, 0));
      lookup63[53] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(0, 27, 1));
      lookup63[54] = new SingleColorLookup(new SourceBlock(13, 0, 2), new SourceBlock(0, 27, 0));
      lookup63[55] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(0, 28, 1));
      lookup63[56] = new SingleColorLookup(new SourceBlock(14, 0, 0), new SourceBlock(0, 28, 0));
      lookup63[57] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(0, 29, 1));
      lookup63[58] = new SingleColorLookup(new SourceBlock(14, 0, 2), new SourceBlock(0, 29, 0));
      lookup63[59] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(0, 30, 1));
      lookup63[60] = new SingleColorLookup(new SourceBlock(15, 0, 0), new SourceBlock(0, 30, 0));
      lookup63[61] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(0, 31, 1));
      lookup63[62] = new SingleColorLookup(new SourceBlock(15, 0, 2), new SourceBlock(0, 31, 0));
      lookup63[63] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(1, 31, 1));
      lookup63[64] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(1, 31, 0));
      lookup63[65] = new SingleColorLookup(new SourceBlock(16, 0, 0), new SourceBlock(0, 32, 0));
      lookup63[66] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(2, 31, 0));
      lookup63[67] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(0, 33, 0));
      lookup63[68] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(3, 31, 0));
      lookup63[69] = new SingleColorLookup(new SourceBlock(17, 0, 0), new SourceBlock(0, 34, 0));
      lookup63[70] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(4, 31, 0));
      lookup63[71] = new SingleColorLookup(new SourceBlock(17, 0, 2), new SourceBlock(0, 35, 0));
      lookup63[72] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(5, 31, 0));
      lookup63[73] = new SingleColorLookup(new SourceBlock(18, 0, 0), new SourceBlock(0, 36, 0));
      lookup63[74] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(6, 31, 0));
      lookup63[75] = new SingleColorLookup(new SourceBlock(18, 0, 2), new SourceBlock(0, 37, 0));
      lookup63[76] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(7, 31, 0));
      lookup63[77] = new SingleColorLookup(new SourceBlock(19, 0, 0), new SourceBlock(0, 38, 0));
      lookup63[78] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(8, 31, 0));
      lookup63[79] = new SingleColorLookup(new SourceBlock(19, 0, 2), new SourceBlock(0, 39, 0));
      lookup63[80] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(9, 31, 0));
      lookup63[81] = new SingleColorLookup(new SourceBlock(20, 0, 0), new SourceBlock(0, 40, 0));
      lookup63[82] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(10, 31, 0));
      lookup63[83] = new SingleColorLookup(new SourceBlock(20, 0, 2), new SourceBlock(0, 41, 0));
      lookup63[84] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(11, 31, 0));
      lookup63[85] = new SingleColorLookup(new SourceBlock(21, 0, 0), new SourceBlock(0, 42, 0));
      lookup63[86] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(12, 31, 0));
      lookup63[87] = new SingleColorLookup(new SourceBlock(21, 0, 2), new SourceBlock(0, 43, 0));
      lookup63[88] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(13, 31, 0));
      lookup63[89] = new SingleColorLookup(new SourceBlock(22, 0, 0), new SourceBlock(0, 44, 0));
      lookup63[90] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(14, 31, 0));
      lookup63[91] = new SingleColorLookup(new SourceBlock(22, 0, 2), new SourceBlock(0, 45, 0));
      lookup63[92] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(15, 31, 0));
      lookup63[93] = new SingleColorLookup(new SourceBlock(23, 0, 0), new SourceBlock(0, 46, 0));
      lookup63[94] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(0, 47, 1));
      lookup63[95] = new SingleColorLookup(new SourceBlock(23, 0, 2), new SourceBlock(0, 47, 0));
      lookup63[96] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(0, 48, 1));
      lookup63[97] = new SingleColorLookup(new SourceBlock(24, 0, 0), new SourceBlock(0, 48, 0));
      lookup63[98] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(0, 49, 1));
      lookup63[99] = new SingleColorLookup(new SourceBlock(24, 0, 2), new SourceBlock(0, 49, 0));
      lookup63[100] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(0, 50, 1));
      lookup63[101] = new SingleColorLookup(new SourceBlock(25, 0, 0), new SourceBlock(0, 50, 0));
      lookup63[102] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(0, 51, 1));
      lookup63[103] = new SingleColorLookup(new SourceBlock(25, 0, 2), new SourceBlock(0, 51, 0));
      lookup63[104] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(0, 52, 1));
      lookup63[105] = new SingleColorLookup(new SourceBlock(26, 0, 0), new SourceBlock(0, 52, 0));
      lookup63[106] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(0, 53, 1));
      lookup63[107] = new SingleColorLookup(new SourceBlock(26, 0, 2), new SourceBlock(0, 53, 0));
      lookup63[108] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(0, 54, 1));
      lookup63[109] = new SingleColorLookup(new SourceBlock(27, 0, 0), new SourceBlock(0, 54, 0));
      lookup63[110] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(0, 55, 1));
      lookup63[111] = new SingleColorLookup(new SourceBlock(27, 0, 2), new SourceBlock(0, 55, 0));
      lookup63[112] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(0, 56, 1));
      lookup63[113] = new SingleColorLookup(new SourceBlock(28, 0, 0), new SourceBlock(0, 56, 0));
      lookup63[114] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(0, 57, 1));
      lookup63[115] = new SingleColorLookup(new SourceBlock(28, 0, 2), new SourceBlock(0, 57, 0));
      lookup63[116] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(0, 58, 1));
      lookup63[117] = new SingleColorLookup(new SourceBlock(29, 0, 0), new SourceBlock(0, 58, 0));
      lookup63[118] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(0, 59, 1));
      lookup63[119] = new SingleColorLookup(new SourceBlock(29, 0, 2), new SourceBlock(0, 59, 0));
      lookup63[120] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(0, 60, 1));
      lookup63[121] = new SingleColorLookup(new SourceBlock(30, 0, 0), new SourceBlock(0, 60, 0));
      lookup63[122] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(0, 61, 1));
      lookup63[123] = new SingleColorLookup(new SourceBlock(30, 0, 2), new SourceBlock(0, 61, 0));
      lookup63[124] = new SingleColorLookup(new SourceBlock(31, 0, 1), new SourceBlock(0, 62, 1));
      lookup63[125] = new SingleColorLookup(new SourceBlock(31, 0, 0), new SourceBlock(0, 62, 0));
      lookup63[126] = new SingleColorLookup(new SourceBlock(31, 0, 1), new SourceBlock(0, 63, 1));
      lookup63[127] = new SingleColorLookup(new SourceBlock(31, 0, 2), new SourceBlock(0, 63, 0));
      lookup63[128] = new SingleColorLookup(new SourceBlock(32, 0, 2), new SourceBlock(1, 63, 1));
      lookup63[129] = new SingleColorLookup(new SourceBlock(32, 0, 1), new SourceBlock(1, 63, 0));
      lookup63[130] = new SingleColorLookup(new SourceBlock(32, 0, 0), new SourceBlock(16, 48, 0));
      lookup63[131] = new SingleColorLookup(new SourceBlock(32, 0, 1), new SourceBlock(2, 63, 0));
      lookup63[132] = new SingleColorLookup(new SourceBlock(32, 0, 2), new SourceBlock(16, 49, 0));
      lookup63[133] = new SingleColorLookup(new SourceBlock(33, 0, 1), new SourceBlock(3, 63, 0));
      lookup63[134] = new SingleColorLookup(new SourceBlock(33, 0, 0), new SourceBlock(16, 50, 0));
      lookup63[135] = new SingleColorLookup(new SourceBlock(33, 0, 1), new SourceBlock(4, 63, 0));
      lookup63[136] = new SingleColorLookup(new SourceBlock(33, 0, 2), new SourceBlock(16, 51, 0));
      lookup63[137] = new SingleColorLookup(new SourceBlock(34, 0, 1), new SourceBlock(5, 63, 0));
      lookup63[138] = new SingleColorLookup(new SourceBlock(34, 0, 0), new SourceBlock(16, 52, 0));
      lookup63[139] = new SingleColorLookup(new SourceBlock(34, 0, 1), new SourceBlock(6, 63, 0));
      lookup63[140] = new SingleColorLookup(new SourceBlock(34, 0, 2), new SourceBlock(16, 53, 0));
      lookup63[141] = new SingleColorLookup(new SourceBlock(35, 0, 1), new SourceBlock(7, 63, 0));
      lookup63[142] = new SingleColorLookup(new SourceBlock(35, 0, 0), new SourceBlock(16, 54, 0));
      lookup63[143] = new SingleColorLookup(new SourceBlock(35, 0, 1), new SourceBlock(8, 63, 0));
      lookup63[144] = new SingleColorLookup(new SourceBlock(35, 0, 2), new SourceBlock(16, 55, 0));
      lookup63[145] = new SingleColorLookup(new SourceBlock(36, 0, 1), new SourceBlock(9, 63, 0));
      lookup63[146] = new SingleColorLookup(new SourceBlock(36, 0, 0), new SourceBlock(16, 56, 0));
      lookup63[147] = new SingleColorLookup(new SourceBlock(36, 0, 1), new SourceBlock(10, 63, 0));
      lookup63[148] = new SingleColorLookup(new SourceBlock(36, 0, 2), new SourceBlock(16, 57, 0));
      lookup63[149] = new SingleColorLookup(new SourceBlock(37, 0, 1), new SourceBlock(11, 63, 0));
      lookup63[150] = new SingleColorLookup(new SourceBlock(37, 0, 0), new SourceBlock(16, 58, 0));
      lookup63[151] = new SingleColorLookup(new SourceBlock(37, 0, 1), new SourceBlock(12, 63, 0));
      lookup63[152] = new SingleColorLookup(new SourceBlock(37, 0, 2), new SourceBlock(16, 59, 0));
      lookup63[153] = new SingleColorLookup(new SourceBlock(38, 0, 1), new SourceBlock(13, 63, 0));
      lookup63[154] = new SingleColorLookup(new SourceBlock(38, 0, 0), new SourceBlock(16, 60, 0));
      lookup63[155] = new SingleColorLookup(new SourceBlock(38, 0, 1), new SourceBlock(14, 63, 0));
      lookup63[156] = new SingleColorLookup(new SourceBlock(38, 0, 2), new SourceBlock(16, 61, 0));
      lookup63[157] = new SingleColorLookup(new SourceBlock(39, 0, 1), new SourceBlock(15, 63, 0));
      lookup63[158] = new SingleColorLookup(new SourceBlock(39, 0, 0), new SourceBlock(16, 62, 0));
      lookup63[159] = new SingleColorLookup(new SourceBlock(39, 0, 1), new SourceBlock(16, 63, 1));
      lookup63[160] = new SingleColorLookup(new SourceBlock(39, 0, 2), new SourceBlock(16, 63, 0));
      lookup63[161] = new SingleColorLookup(new SourceBlock(40, 0, 1), new SourceBlock(17, 63, 1));
      lookup63[162] = new SingleColorLookup(new SourceBlock(40, 0, 0), new SourceBlock(17, 63, 0));
      lookup63[163] = new SingleColorLookup(new SourceBlock(40, 0, 1), new SourceBlock(18, 63, 1));
      lookup63[164] = new SingleColorLookup(new SourceBlock(40, 0, 2), new SourceBlock(18, 63, 0));
      lookup63[165] = new SingleColorLookup(new SourceBlock(41, 0, 1), new SourceBlock(19, 63, 1));
      lookup63[166] = new SingleColorLookup(new SourceBlock(41, 0, 0), new SourceBlock(19, 63, 0));
      lookup63[167] = new SingleColorLookup(new SourceBlock(41, 0, 1), new SourceBlock(20, 63, 1));
      lookup63[168] = new SingleColorLookup(new SourceBlock(41, 0, 2), new SourceBlock(20, 63, 0));
      lookup63[169] = new SingleColorLookup(new SourceBlock(42, 0, 1), new SourceBlock(21, 63, 1));
      lookup63[170] = new SingleColorLookup(new SourceBlock(42, 0, 0), new SourceBlock(21, 63, 0));
      lookup63[171] = new SingleColorLookup(new SourceBlock(42, 0, 1), new SourceBlock(22, 63, 1));
      lookup63[172] = new SingleColorLookup(new SourceBlock(42, 0, 2), new SourceBlock(22, 63, 0));
      lookup63[173] = new SingleColorLookup(new SourceBlock(43, 0, 1), new SourceBlock(23, 63, 1));
      lookup63[174] = new SingleColorLookup(new SourceBlock(43, 0, 0), new SourceBlock(23, 63, 0));
      lookup63[175] = new SingleColorLookup(new SourceBlock(43, 0, 1), new SourceBlock(24, 63, 1));
      lookup63[176] = new SingleColorLookup(new SourceBlock(43, 0, 2), new SourceBlock(24, 63, 0));
      lookup63[177] = new SingleColorLookup(new SourceBlock(44, 0, 1), new SourceBlock(25, 63, 1));
      lookup63[178] = new SingleColorLookup(new SourceBlock(44, 0, 0), new SourceBlock(25, 63, 0));
      lookup63[179] = new SingleColorLookup(new SourceBlock(44, 0, 1), new SourceBlock(26, 63, 1));
      lookup63[180] = new SingleColorLookup(new SourceBlock(44, 0, 2), new SourceBlock(26, 63, 0));
      lookup63[181] = new SingleColorLookup(new SourceBlock(45, 0, 1), new SourceBlock(27, 63, 1));
      lookup63[182] = new SingleColorLookup(new SourceBlock(45, 0, 0), new SourceBlock(27, 63, 0));
      lookup63[183] = new SingleColorLookup(new SourceBlock(45, 0, 1), new SourceBlock(28, 63, 1));
      lookup63[184] = new SingleColorLookup(new SourceBlock(45, 0, 2), new SourceBlock(28, 63, 0));
      lookup63[185] = new SingleColorLookup(new SourceBlock(46, 0, 1), new SourceBlock(29, 63, 1));
      lookup63[186] = new SingleColorLookup(new SourceBlock(46, 0, 0), new SourceBlock(29, 63, 0));
      lookup63[187] = new SingleColorLookup(new SourceBlock(46, 0, 1), new SourceBlock(30, 63, 1));
      lookup63[188] = new SingleColorLookup(new SourceBlock(46, 0, 2), new SourceBlock(30, 63, 0));
      lookup63[189] = new SingleColorLookup(new SourceBlock(47, 0, 1), new SourceBlock(31, 63, 1));
      lookup63[190] = new SingleColorLookup(new SourceBlock(47, 0, 0), new SourceBlock(31, 63, 0));
      lookup63[191] = new SingleColorLookup(new SourceBlock(47, 0, 1), new SourceBlock(32, 63, 1));
      lookup63[192] = new SingleColorLookup(new SourceBlock(47, 0, 2), new SourceBlock(32, 63, 0));
      lookup63[193] = new SingleColorLookup(new SourceBlock(48, 0, 2), new SourceBlock(33, 63, 1));
      lookup63[194] = new SingleColorLookup(new SourceBlock(48, 0, 1), new SourceBlock(33, 63, 0));
      lookup63[195] = new SingleColorLookup(new SourceBlock(48, 0, 0), new SourceBlock(48, 48, 0));
      lookup63[196] = new SingleColorLookup(new SourceBlock(48, 0, 1), new SourceBlock(34, 63, 0));
      lookup63[197] = new SingleColorLookup(new SourceBlock(48, 0, 2), new SourceBlock(48, 49, 0));
      lookup63[198] = new SingleColorLookup(new SourceBlock(49, 0, 1), new SourceBlock(35, 63, 0));
      lookup63[199] = new SingleColorLookup(new SourceBlock(49, 0, 0), new SourceBlock(48, 50, 0));
      lookup63[200] = new SingleColorLookup(new SourceBlock(49, 0, 1), new SourceBlock(36, 63, 0));
      lookup63[201] = new SingleColorLookup(new SourceBlock(49, 0, 2), new SourceBlock(48, 51, 0));
      lookup63[202] = new SingleColorLookup(new SourceBlock(50, 0, 1), new SourceBlock(37, 63, 0));
      lookup63[203] = new SingleColorLookup(new SourceBlock(50, 0, 0), new SourceBlock(48, 52, 0));
      lookup63[204] = new SingleColorLookup(new SourceBlock(50, 0, 1), new SourceBlock(38, 63, 0));
      lookup63[205] = new SingleColorLookup(new SourceBlock(50, 0, 2), new SourceBlock(48, 53, 0));
      lookup63[206] = new SingleColorLookup(new SourceBlock(51, 0, 1), new SourceBlock(39, 63, 0));
      lookup63[207] = new SingleColorLookup(new SourceBlock(51, 0, 0), new SourceBlock(48, 54, 0));
      lookup63[208] = new SingleColorLookup(new SourceBlock(51, 0, 1), new SourceBlock(40, 63, 0));
      lookup63[209] = new SingleColorLookup(new SourceBlock(51, 0, 2), new SourceBlock(48, 55, 0));
      lookup63[210] = new SingleColorLookup(new SourceBlock(52, 0, 1), new SourceBlock(41, 63, 0));
      lookup63[211] = new SingleColorLookup(new SourceBlock(52, 0, 0), new SourceBlock(48, 56, 0));
      lookup63[212] = new SingleColorLookup(new SourceBlock(52, 0, 1), new SourceBlock(42, 63, 0));
      lookup63[213] = new SingleColorLookup(new SourceBlock(52, 0, 2), new SourceBlock(48, 57, 0));
      lookup63[214] = new SingleColorLookup(new SourceBlock(53, 0, 1), new SourceBlock(43, 63, 0));
      lookup63[215] = new SingleColorLookup(new SourceBlock(53, 0, 0), new SourceBlock(48, 58, 0));
      lookup63[216] = new SingleColorLookup(new SourceBlock(53, 0, 1), new SourceBlock(44, 63, 0));
      lookup63[217] = new SingleColorLookup(new SourceBlock(53, 0, 2), new SourceBlock(48, 59, 0));
      lookup63[218] = new SingleColorLookup(new SourceBlock(54, 0, 1), new SourceBlock(45, 63, 0));
      lookup63[219] = new SingleColorLookup(new SourceBlock(54, 0, 0), new SourceBlock(48, 60, 0));
      lookup63[220] = new SingleColorLookup(new SourceBlock(54, 0, 1), new SourceBlock(46, 63, 0));
      lookup63[221] = new SingleColorLookup(new SourceBlock(54, 0, 2), new SourceBlock(48, 61, 0));
      lookup63[222] = new SingleColorLookup(new SourceBlock(55, 0, 1), new SourceBlock(47, 63, 0));
      lookup63[223] = new SingleColorLookup(new SourceBlock(55, 0, 0), new SourceBlock(48, 62, 0));
      lookup63[224] = new SingleColorLookup(new SourceBlock(55, 0, 1), new SourceBlock(48, 63, 1));
      lookup63[225] = new SingleColorLookup(new SourceBlock(55, 0, 2), new SourceBlock(48, 63, 0));
      lookup63[226] = new SingleColorLookup(new SourceBlock(56, 0, 1), new SourceBlock(49, 63, 1));
      lookup63[227] = new SingleColorLookup(new SourceBlock(56, 0, 0), new SourceBlock(49, 63, 0));
      lookup63[228] = new SingleColorLookup(new SourceBlock(56, 0, 1), new SourceBlock(50, 63, 1));
      lookup63[229] = new SingleColorLookup(new SourceBlock(56, 0, 2), new SourceBlock(50, 63, 0));
      lookup63[230] = new SingleColorLookup(new SourceBlock(57, 0, 1), new SourceBlock(51, 63, 1));
      lookup63[231] = new SingleColorLookup(new SourceBlock(57, 0, 0), new SourceBlock(51, 63, 0));
      lookup63[232] = new SingleColorLookup(new SourceBlock(57, 0, 1), new SourceBlock(52, 63, 1));
      lookup63[233] = new SingleColorLookup(new SourceBlock(57, 0, 2), new SourceBlock(52, 63, 0));
      lookup63[234] = new SingleColorLookup(new SourceBlock(58, 0, 1), new SourceBlock(53, 63, 1));
      lookup63[235] = new SingleColorLookup(new SourceBlock(58, 0, 0), new SourceBlock(53, 63, 0));
      lookup63[236] = new SingleColorLookup(new SourceBlock(58, 0, 1), new SourceBlock(54, 63, 1));
      lookup63[237] = new SingleColorLookup(new SourceBlock(58, 0, 2), new SourceBlock(54, 63, 0));
      lookup63[238] = new SingleColorLookup(new SourceBlock(59, 0, 1), new SourceBlock(55, 63, 1));
      lookup63[239] = new SingleColorLookup(new SourceBlock(59, 0, 0), new SourceBlock(55, 63, 0));
      lookup63[240] = new SingleColorLookup(new SourceBlock(59, 0, 1), new SourceBlock(56, 63, 1));
      lookup63[241] = new SingleColorLookup(new SourceBlock(59, 0, 2), new SourceBlock(56, 63, 0));
      lookup63[242] = new SingleColorLookup(new SourceBlock(60, 0, 1), new SourceBlock(57, 63, 1));
      lookup63[243] = new SingleColorLookup(new SourceBlock(60, 0, 0), new SourceBlock(57, 63, 0));
      lookup63[244] = new SingleColorLookup(new SourceBlock(60, 0, 1), new SourceBlock(58, 63, 1));
      lookup63[245] = new SingleColorLookup(new SourceBlock(60, 0, 2), new SourceBlock(58, 63, 0));
      lookup63[246] = new SingleColorLookup(new SourceBlock(61, 0, 1), new SourceBlock(59, 63, 1));
      lookup63[247] = new SingleColorLookup(new SourceBlock(61, 0, 0), new SourceBlock(59, 63, 0));
      lookup63[248] = new SingleColorLookup(new SourceBlock(61, 0, 1), new SourceBlock(60, 63, 1));
      lookup63[249] = new SingleColorLookup(new SourceBlock(61, 0, 2), new SourceBlock(60, 63, 0));
      lookup63[250] = new SingleColorLookup(new SourceBlock(62, 0, 1), new SourceBlock(61, 63, 1));
      lookup63[251] = new SingleColorLookup(new SourceBlock(62, 0, 0), new SourceBlock(61, 63, 0));
      lookup63[252] = new SingleColorLookup(new SourceBlock(62, 0, 1), new SourceBlock(62, 63, 1));
      lookup63[253] = new SingleColorLookup(new SourceBlock(62, 0, 2), new SourceBlock(62, 63, 0));
      lookup63[254] = new SingleColorLookup(new SourceBlock(63, 0, 1), new SourceBlock(63, 63, 1));
      lookup63[255] = new SingleColorLookup(new SourceBlock(63, 0, 0), new SourceBlock(63, 63, 0));

      lookup54[0] = new SingleColorLookup(new SourceBlock(0, 0, 0), new SourceBlock(0, 0, 0));
      lookup54[1] = new SingleColorLookup(new SourceBlock(0, 0, 1), new SourceBlock(0, 1, 1));
      lookup54[2] = new SingleColorLookup(new SourceBlock(0, 0, 2), new SourceBlock(0, 1, 0));
      lookup54[3] = new SingleColorLookup(new SourceBlock(0, 0, 3), new SourceBlock(0, 1, 1));
      lookup54[4] = new SingleColorLookup(new SourceBlock(0, 0, 4), new SourceBlock(0, 2, 1));
      lookup54[5] = new SingleColorLookup(new SourceBlock(1, 0, 3), new SourceBlock(0, 2, 0));
      lookup54[6] = new SingleColorLookup(new SourceBlock(1, 0, 2), new SourceBlock(0, 2, 1));
      lookup54[7] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(0, 3, 1));
      lookup54[8] = new SingleColorLookup(new SourceBlock(1, 0, 0), new SourceBlock(0, 3, 0));
      lookup54[9] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(1, 2, 1));
      lookup54[10] = new SingleColorLookup(new SourceBlock(1, 0, 2), new SourceBlock(1, 2, 0));
      lookup54[11] = new SingleColorLookup(new SourceBlock(1, 0, 3), new SourceBlock(0, 4, 0));
      lookup54[12] = new SingleColorLookup(new SourceBlock(1, 0, 4), new SourceBlock(0, 5, 1));
      lookup54[13] = new SingleColorLookup(new SourceBlock(2, 0, 3), new SourceBlock(0, 5, 0));
      lookup54[14] = new SingleColorLookup(new SourceBlock(2, 0, 2), new SourceBlock(0, 5, 1));
      lookup54[15] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(0, 6, 1));
      lookup54[16] = new SingleColorLookup(new SourceBlock(2, 0, 0), new SourceBlock(0, 6, 0));
      lookup54[17] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(2, 3, 1));
      lookup54[18] = new SingleColorLookup(new SourceBlock(2, 0, 2), new SourceBlock(2, 3, 0));
      lookup54[19] = new SingleColorLookup(new SourceBlock(2, 0, 3), new SourceBlock(0, 7, 0));
      lookup54[20] = new SingleColorLookup(new SourceBlock(2, 0, 4), new SourceBlock(1, 6, 1));
      lookup54[21] = new SingleColorLookup(new SourceBlock(3, 0, 3), new SourceBlock(1, 6, 0));
      lookup54[22] = new SingleColorLookup(new SourceBlock(3, 0, 2), new SourceBlock(0, 8, 0));
      lookup54[23] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 9, 1));
      lookup54[24] = new SingleColorLookup(new SourceBlock(3, 0, 0), new SourceBlock(0, 9, 0));
      lookup54[25] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 9, 1));
      lookup54[26] = new SingleColorLookup(new SourceBlock(3, 0, 2), new SourceBlock(0, 10, 1));
      lookup54[27] = new SingleColorLookup(new SourceBlock(3, 0, 3), new SourceBlock(0, 10, 0));
      lookup54[28] = new SingleColorLookup(new SourceBlock(3, 0, 4), new SourceBlock(2, 7, 1));
      lookup54[29] = new SingleColorLookup(new SourceBlock(4, 0, 4), new SourceBlock(2, 7, 0));
      lookup54[30] = new SingleColorLookup(new SourceBlock(4, 0, 3), new SourceBlock(0, 11, 0));
      lookup54[31] = new SingleColorLookup(new SourceBlock(4, 0, 2), new SourceBlock(1, 10, 1));
      lookup54[32] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(1, 10, 0));
      lookup54[33] = new SingleColorLookup(new SourceBlock(4, 0, 0), new SourceBlock(0, 12, 0));
      lookup54[34] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(0, 13, 1));
      lookup54[35] = new SingleColorLookup(new SourceBlock(4, 0, 2), new SourceBlock(0, 13, 0));
      lookup54[36] = new SingleColorLookup(new SourceBlock(4, 0, 3), new SourceBlock(0, 13, 1));
      lookup54[37] = new SingleColorLookup(new SourceBlock(4, 0, 4), new SourceBlock(0, 14, 1));
      lookup54[38] = new SingleColorLookup(new SourceBlock(5, 0, 3), new SourceBlock(0, 14, 0));
      lookup54[39] = new SingleColorLookup(new SourceBlock(5, 0, 2), new SourceBlock(2, 11, 1));
      lookup54[40] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(2, 11, 0));
      lookup54[41] = new SingleColorLookup(new SourceBlock(5, 0, 0), new SourceBlock(0, 15, 0));
      lookup54[42] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(1, 14, 1));
      lookup54[43] = new SingleColorLookup(new SourceBlock(5, 0, 2), new SourceBlock(1, 14, 0));
      lookup54[44] = new SingleColorLookup(new SourceBlock(5, 0, 3), new SourceBlock(0, 16, 0));
      lookup54[45] = new SingleColorLookup(new SourceBlock(5, 0, 4), new SourceBlock(0, 17, 1));
      lookup54[46] = new SingleColorLookup(new SourceBlock(6, 0, 3), new SourceBlock(0, 17, 0));
      lookup54[47] = new SingleColorLookup(new SourceBlock(6, 0, 2), new SourceBlock(0, 17, 1));
      lookup54[48] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(0, 18, 1));
      lookup54[49] = new SingleColorLookup(new SourceBlock(6, 0, 0), new SourceBlock(0, 18, 0));
      lookup54[50] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(2, 15, 1));
      lookup54[51] = new SingleColorLookup(new SourceBlock(6, 0, 2), new SourceBlock(2, 15, 0));
      lookup54[52] = new SingleColorLookup(new SourceBlock(6, 0, 3), new SourceBlock(0, 19, 0));
      lookup54[53] = new SingleColorLookup(new SourceBlock(6, 0, 4), new SourceBlock(1, 18, 1));
      lookup54[54] = new SingleColorLookup(new SourceBlock(7, 0, 3), new SourceBlock(1, 18, 0));
      lookup54[55] = new SingleColorLookup(new SourceBlock(7, 0, 2), new SourceBlock(0, 20, 0));
      lookup54[56] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 21, 1));
      lookup54[57] = new SingleColorLookup(new SourceBlock(7, 0, 0), new SourceBlock(0, 21, 0));
      lookup54[58] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 21, 1));
      lookup54[59] = new SingleColorLookup(new SourceBlock(7, 0, 2), new SourceBlock(0, 22, 1));
      lookup54[60] = new SingleColorLookup(new SourceBlock(7, 0, 3), new SourceBlock(0, 22, 0));
      lookup54[61] = new SingleColorLookup(new SourceBlock(7, 0, 4), new SourceBlock(2, 19, 1));
      lookup54[62] = new SingleColorLookup(new SourceBlock(8, 0, 4), new SourceBlock(2, 19, 0));
      lookup54[63] = new SingleColorLookup(new SourceBlock(8, 0, 3), new SourceBlock(0, 23, 0));
      lookup54[64] = new SingleColorLookup(new SourceBlock(8, 0, 2), new SourceBlock(1, 22, 1));
      lookup54[65] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(1, 22, 0));
      lookup54[66] = new SingleColorLookup(new SourceBlock(8, 0, 0), new SourceBlock(0, 24, 0));
      lookup54[67] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(0, 25, 1));
      lookup54[68] = new SingleColorLookup(new SourceBlock(8, 0, 2), new SourceBlock(0, 25, 0));
      lookup54[69] = new SingleColorLookup(new SourceBlock(8, 0, 3), new SourceBlock(0, 25, 1));
      lookup54[70] = new SingleColorLookup(new SourceBlock(8, 0, 4), new SourceBlock(0, 26, 1));
      lookup54[71] = new SingleColorLookup(new SourceBlock(9, 0, 3), new SourceBlock(0, 26, 0));
      lookup54[72] = new SingleColorLookup(new SourceBlock(9, 0, 2), new SourceBlock(2, 23, 1));
      lookup54[73] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(2, 23, 0));
      lookup54[74] = new SingleColorLookup(new SourceBlock(9, 0, 0), new SourceBlock(0, 27, 0));
      lookup54[75] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(1, 26, 1));
      lookup54[76] = new SingleColorLookup(new SourceBlock(9, 0, 2), new SourceBlock(1, 26, 0));
      lookup54[77] = new SingleColorLookup(new SourceBlock(9, 0, 3), new SourceBlock(0, 28, 0));
      lookup54[78] = new SingleColorLookup(new SourceBlock(9, 0, 4), new SourceBlock(0, 29, 1));
      lookup54[79] = new SingleColorLookup(new SourceBlock(10, 0, 3), new SourceBlock(0, 29, 0));
      lookup54[80] = new SingleColorLookup(new SourceBlock(10, 0, 2), new SourceBlock(0, 29, 1));
      lookup54[81] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(0, 30, 1));
      lookup54[82] = new SingleColorLookup(new SourceBlock(10, 0, 0), new SourceBlock(0, 30, 0));
      lookup54[83] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(2, 27, 1));
      lookup54[84] = new SingleColorLookup(new SourceBlock(10, 0, 2), new SourceBlock(2, 27, 0));
      lookup54[85] = new SingleColorLookup(new SourceBlock(10, 0, 3), new SourceBlock(0, 31, 0));
      lookup54[86] = new SingleColorLookup(new SourceBlock(10, 0, 4), new SourceBlock(1, 30, 1));
      lookup54[87] = new SingleColorLookup(new SourceBlock(11, 0, 3), new SourceBlock(1, 30, 0));
      lookup54[88] = new SingleColorLookup(new SourceBlock(11, 0, 2), new SourceBlock(4, 24, 0));
      lookup54[89] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(1, 31, 1));
      lookup54[90] = new SingleColorLookup(new SourceBlock(11, 0, 0), new SourceBlock(1, 31, 0));
      lookup54[91] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(1, 31, 1));
      lookup54[92] = new SingleColorLookup(new SourceBlock(11, 0, 2), new SourceBlock(2, 30, 1));
      lookup54[93] = new SingleColorLookup(new SourceBlock(11, 0, 3), new SourceBlock(2, 30, 0));
      lookup54[94] = new SingleColorLookup(new SourceBlock(11, 0, 4), new SourceBlock(2, 31, 1));
      lookup54[95] = new SingleColorLookup(new SourceBlock(12, 0, 4), new SourceBlock(2, 31, 0));
      lookup54[96] = new SingleColorLookup(new SourceBlock(12, 0, 3), new SourceBlock(4, 27, 0));
      lookup54[97] = new SingleColorLookup(new SourceBlock(12, 0, 2), new SourceBlock(3, 30, 1));
      lookup54[98] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(3, 30, 0));
      lookup54[99] = new SingleColorLookup(new SourceBlock(12, 0, 0), new SourceBlock(4, 28, 0));
      lookup54[100] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(3, 31, 1));
      lookup54[101] = new SingleColorLookup(new SourceBlock(12, 0, 2), new SourceBlock(3, 31, 0));
      lookup54[102] = new SingleColorLookup(new SourceBlock(12, 0, 3), new SourceBlock(3, 31, 1));
      lookup54[103] = new SingleColorLookup(new SourceBlock(12, 0, 4), new SourceBlock(4, 30, 1));
      lookup54[104] = new SingleColorLookup(new SourceBlock(13, 0, 3), new SourceBlock(4, 30, 0));
      lookup54[105] = new SingleColorLookup(new SourceBlock(13, 0, 2), new SourceBlock(6, 27, 1));
      lookup54[106] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(6, 27, 0));
      lookup54[107] = new SingleColorLookup(new SourceBlock(13, 0, 0), new SourceBlock(4, 31, 0));
      lookup54[108] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(5, 30, 1));
      lookup54[109] = new SingleColorLookup(new SourceBlock(13, 0, 2), new SourceBlock(5, 30, 0));
      lookup54[110] = new SingleColorLookup(new SourceBlock(13, 0, 3), new SourceBlock(8, 24, 0));
      lookup54[111] = new SingleColorLookup(new SourceBlock(13, 0, 4), new SourceBlock(5, 31, 1));
      lookup54[112] = new SingleColorLookup(new SourceBlock(14, 0, 3), new SourceBlock(5, 31, 0));
      lookup54[113] = new SingleColorLookup(new SourceBlock(14, 0, 2), new SourceBlock(5, 31, 1));
      lookup54[114] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(6, 30, 1));
      lookup54[115] = new SingleColorLookup(new SourceBlock(14, 0, 0), new SourceBlock(6, 30, 0));
      lookup54[116] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(6, 31, 1));
      lookup54[117] = new SingleColorLookup(new SourceBlock(14, 0, 2), new SourceBlock(6, 31, 0));
      lookup54[118] = new SingleColorLookup(new SourceBlock(14, 0, 3), new SourceBlock(8, 27, 0));
      lookup54[119] = new SingleColorLookup(new SourceBlock(14, 0, 4), new SourceBlock(7, 30, 1));
      lookup54[120] = new SingleColorLookup(new SourceBlock(15, 0, 3), new SourceBlock(7, 30, 0));
      lookup54[121] = new SingleColorLookup(new SourceBlock(15, 0, 2), new SourceBlock(8, 28, 0));
      lookup54[122] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(7, 31, 1));
      lookup54[123] = new SingleColorLookup(new SourceBlock(15, 0, 0), new SourceBlock(7, 31, 0));
      lookup54[124] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(7, 31, 1));
      lookup54[125] = new SingleColorLookup(new SourceBlock(15, 0, 2), new SourceBlock(8, 30, 1));
      lookup54[126] = new SingleColorLookup(new SourceBlock(15, 0, 3), new SourceBlock(8, 30, 0));
      lookup54[127] = new SingleColorLookup(new SourceBlock(15, 0, 4), new SourceBlock(10, 27, 1));
      lookup54[128] = new SingleColorLookup(new SourceBlock(16, 0, 4), new SourceBlock(10, 27, 0));
      lookup54[129] = new SingleColorLookup(new SourceBlock(16, 0, 3), new SourceBlock(8, 31, 0));
      lookup54[130] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(9, 30, 1));
      lookup54[131] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(9, 30, 0));
      lookup54[132] = new SingleColorLookup(new SourceBlock(16, 0, 0), new SourceBlock(12, 24, 0));
      lookup54[133] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(9, 31, 1));
      lookup54[134] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(9, 31, 0));
      lookup54[135] = new SingleColorLookup(new SourceBlock(16, 0, 3), new SourceBlock(9, 31, 1));
      lookup54[136] = new SingleColorLookup(new SourceBlock(16, 0, 4), new SourceBlock(10, 30, 1));
      lookup54[137] = new SingleColorLookup(new SourceBlock(17, 0, 3), new SourceBlock(10, 30, 0));
      lookup54[138] = new SingleColorLookup(new SourceBlock(17, 0, 2), new SourceBlock(10, 31, 1));
      lookup54[139] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(10, 31, 0));
      lookup54[140] = new SingleColorLookup(new SourceBlock(17, 0, 0), new SourceBlock(12, 27, 0));
      lookup54[141] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(11, 30, 1));
      lookup54[142] = new SingleColorLookup(new SourceBlock(17, 0, 2), new SourceBlock(11, 30, 0));
      lookup54[143] = new SingleColorLookup(new SourceBlock(17, 0, 3), new SourceBlock(12, 28, 0));
      lookup54[144] = new SingleColorLookup(new SourceBlock(17, 0, 4), new SourceBlock(11, 31, 1));
      lookup54[145] = new SingleColorLookup(new SourceBlock(18, 0, 3), new SourceBlock(11, 31, 0));
      lookup54[146] = new SingleColorLookup(new SourceBlock(18, 0, 2), new SourceBlock(11, 31, 1));
      lookup54[147] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(12, 30, 1));
      lookup54[148] = new SingleColorLookup(new SourceBlock(18, 0, 0), new SourceBlock(12, 30, 0));
      lookup54[149] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(14, 27, 1));
      lookup54[150] = new SingleColorLookup(new SourceBlock(18, 0, 2), new SourceBlock(14, 27, 0));
      lookup54[151] = new SingleColorLookup(new SourceBlock(18, 0, 3), new SourceBlock(12, 31, 0));
      lookup54[152] = new SingleColorLookup(new SourceBlock(18, 0, 4), new SourceBlock(13, 30, 1));
      lookup54[153] = new SingleColorLookup(new SourceBlock(19, 0, 3), new SourceBlock(13, 30, 0));
      lookup54[154] = new SingleColorLookup(new SourceBlock(19, 0, 2), new SourceBlock(16, 24, 0));
      lookup54[155] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(13, 31, 1));
      lookup54[156] = new SingleColorLookup(new SourceBlock(19, 0, 0), new SourceBlock(13, 31, 0));
      lookup54[157] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(13, 31, 1));
      lookup54[158] = new SingleColorLookup(new SourceBlock(19, 0, 2), new SourceBlock(14, 30, 1));
      lookup54[159] = new SingleColorLookup(new SourceBlock(19, 0, 3), new SourceBlock(14, 30, 0));
      lookup54[160] = new SingleColorLookup(new SourceBlock(19, 0, 4), new SourceBlock(14, 31, 1));
      lookup54[161] = new SingleColorLookup(new SourceBlock(20, 0, 4), new SourceBlock(14, 31, 0));
      lookup54[162] = new SingleColorLookup(new SourceBlock(20, 0, 3), new SourceBlock(16, 27, 0));
      lookup54[163] = new SingleColorLookup(new SourceBlock(20, 0, 2), new SourceBlock(15, 30, 1));
      lookup54[164] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(15, 30, 0));
      lookup54[165] = new SingleColorLookup(new SourceBlock(20, 0, 0), new SourceBlock(16, 28, 0));
      lookup54[166] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(15, 31, 1));
      lookup54[167] = new SingleColorLookup(new SourceBlock(20, 0, 2), new SourceBlock(15, 31, 0));
      lookup54[168] = new SingleColorLookup(new SourceBlock(20, 0, 3), new SourceBlock(15, 31, 1));
      lookup54[169] = new SingleColorLookup(new SourceBlock(20, 0, 4), new SourceBlock(16, 30, 1));
      lookup54[170] = new SingleColorLookup(new SourceBlock(21, 0, 3), new SourceBlock(16, 30, 0));
      lookup54[171] = new SingleColorLookup(new SourceBlock(21, 0, 2), new SourceBlock(18, 27, 1));
      lookup54[172] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(18, 27, 0));
      lookup54[173] = new SingleColorLookup(new SourceBlock(21, 0, 0), new SourceBlock(16, 31, 0));
      lookup54[174] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(17, 30, 1));
      lookup54[175] = new SingleColorLookup(new SourceBlock(21, 0, 2), new SourceBlock(17, 30, 0));
      lookup54[176] = new SingleColorLookup(new SourceBlock(21, 0, 3), new SourceBlock(20, 24, 0));
      lookup54[177] = new SingleColorLookup(new SourceBlock(21, 0, 4), new SourceBlock(17, 31, 1));
      lookup54[178] = new SingleColorLookup(new SourceBlock(22, 0, 3), new SourceBlock(17, 31, 0));
      lookup54[179] = new SingleColorLookup(new SourceBlock(22, 0, 2), new SourceBlock(17, 31, 1));
      lookup54[180] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(18, 30, 1));
      lookup54[181] = new SingleColorLookup(new SourceBlock(22, 0, 0), new SourceBlock(18, 30, 0));
      lookup54[182] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(18, 31, 1));
      lookup54[183] = new SingleColorLookup(new SourceBlock(22, 0, 2), new SourceBlock(18, 31, 0));
      lookup54[184] = new SingleColorLookup(new SourceBlock(22, 0, 3), new SourceBlock(20, 27, 0));
      lookup54[185] = new SingleColorLookup(new SourceBlock(22, 0, 4), new SourceBlock(19, 30, 1));
      lookup54[186] = new SingleColorLookup(new SourceBlock(23, 0, 3), new SourceBlock(19, 30, 0));
      lookup54[187] = new SingleColorLookup(new SourceBlock(23, 0, 2), new SourceBlock(20, 28, 0));
      lookup54[188] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(19, 31, 1));
      lookup54[189] = new SingleColorLookup(new SourceBlock(23, 0, 0), new SourceBlock(19, 31, 0));
      lookup54[190] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(19, 31, 1));
      lookup54[191] = new SingleColorLookup(new SourceBlock(23, 0, 2), new SourceBlock(20, 30, 1));
      lookup54[192] = new SingleColorLookup(new SourceBlock(23, 0, 3), new SourceBlock(20, 30, 0));
      lookup54[193] = new SingleColorLookup(new SourceBlock(23, 0, 4), new SourceBlock(22, 27, 1));
      lookup54[194] = new SingleColorLookup(new SourceBlock(24, 0, 4), new SourceBlock(22, 27, 0));
      lookup54[195] = new SingleColorLookup(new SourceBlock(24, 0, 3), new SourceBlock(20, 31, 0));
      lookup54[196] = new SingleColorLookup(new SourceBlock(24, 0, 2), new SourceBlock(21, 30, 1));
      lookup54[197] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(21, 30, 0));
      lookup54[198] = new SingleColorLookup(new SourceBlock(24, 0, 0), new SourceBlock(24, 24, 0));
      lookup54[199] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(21, 31, 1));
      lookup54[200] = new SingleColorLookup(new SourceBlock(24, 0, 2), new SourceBlock(21, 31, 0));
      lookup54[201] = new SingleColorLookup(new SourceBlock(24, 0, 3), new SourceBlock(21, 31, 1));
      lookup54[202] = new SingleColorLookup(new SourceBlock(24, 0, 4), new SourceBlock(22, 30, 1));
      lookup54[203] = new SingleColorLookup(new SourceBlock(25, 0, 3), new SourceBlock(22, 30, 0));
      lookup54[204] = new SingleColorLookup(new SourceBlock(25, 0, 2), new SourceBlock(22, 31, 1));
      lookup54[205] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(22, 31, 0));
      lookup54[206] = new SingleColorLookup(new SourceBlock(25, 0, 0), new SourceBlock(24, 27, 0));
      lookup54[207] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(23, 30, 1));
      lookup54[208] = new SingleColorLookup(new SourceBlock(25, 0, 2), new SourceBlock(23, 30, 0));
      lookup54[209] = new SingleColorLookup(new SourceBlock(25, 0, 3), new SourceBlock(24, 28, 0));
      lookup54[210] = new SingleColorLookup(new SourceBlock(25, 0, 4), new SourceBlock(23, 31, 1));
      lookup54[211] = new SingleColorLookup(new SourceBlock(26, 0, 3), new SourceBlock(23, 31, 0));
      lookup54[212] = new SingleColorLookup(new SourceBlock(26, 0, 2), new SourceBlock(23, 31, 1));
      lookup54[213] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(24, 30, 1));
      lookup54[214] = new SingleColorLookup(new SourceBlock(26, 0, 0), new SourceBlock(24, 30, 0));
      lookup54[215] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(26, 27, 1));
      lookup54[216] = new SingleColorLookup(new SourceBlock(26, 0, 2), new SourceBlock(26, 27, 0));
      lookup54[217] = new SingleColorLookup(new SourceBlock(26, 0, 3), new SourceBlock(24, 31, 0));
      lookup54[218] = new SingleColorLookup(new SourceBlock(26, 0, 4), new SourceBlock(25, 30, 1));
      lookup54[219] = new SingleColorLookup(new SourceBlock(27, 0, 3), new SourceBlock(25, 30, 0));
      lookup54[220] = new SingleColorLookup(new SourceBlock(27, 0, 2), new SourceBlock(28, 24, 0));
      lookup54[221] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(25, 31, 1));
      lookup54[222] = new SingleColorLookup(new SourceBlock(27, 0, 0), new SourceBlock(25, 31, 0));
      lookup54[223] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(25, 31, 1));
      lookup54[224] = new SingleColorLookup(new SourceBlock(27, 0, 2), new SourceBlock(26, 30, 1));
      lookup54[225] = new SingleColorLookup(new SourceBlock(27, 0, 3), new SourceBlock(26, 30, 0));
      lookup54[226] = new SingleColorLookup(new SourceBlock(27, 0, 4), new SourceBlock(26, 31, 1));
      lookup54[227] = new SingleColorLookup(new SourceBlock(28, 0, 4), new SourceBlock(26, 31, 0));
      lookup54[228] = new SingleColorLookup(new SourceBlock(28, 0, 3), new SourceBlock(28, 27, 0));
      lookup54[229] = new SingleColorLookup(new SourceBlock(28, 0, 2), new SourceBlock(27, 30, 1));
      lookup54[230] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(27, 30, 0));
      lookup54[231] = new SingleColorLookup(new SourceBlock(28, 0, 0), new SourceBlock(28, 28, 0));
      lookup54[232] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(27, 31, 1));
      lookup54[233] = new SingleColorLookup(new SourceBlock(28, 0, 2), new SourceBlock(27, 31, 0));
      lookup54[234] = new SingleColorLookup(new SourceBlock(28, 0, 3), new SourceBlock(27, 31, 1));
      lookup54[235] = new SingleColorLookup(new SourceBlock(28, 0, 4), new SourceBlock(28, 30, 1));
      lookup54[236] = new SingleColorLookup(new SourceBlock(29, 0, 3), new SourceBlock(28, 30, 0));
      lookup54[237] = new SingleColorLookup(new SourceBlock(29, 0, 2), new SourceBlock(30, 27, 1));
      lookup54[238] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(30, 27, 0));
      lookup54[239] = new SingleColorLookup(new SourceBlock(29, 0, 0), new SourceBlock(28, 31, 0));
      lookup54[240] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(29, 30, 1));
      lookup54[241] = new SingleColorLookup(new SourceBlock(29, 0, 2), new SourceBlock(29, 30, 0));
      lookup54[242] = new SingleColorLookup(new SourceBlock(29, 0, 3), new SourceBlock(29, 30, 1));
      lookup54[243] = new SingleColorLookup(new SourceBlock(29, 0, 4), new SourceBlock(29, 31, 1));
      lookup54[244] = new SingleColorLookup(new SourceBlock(30, 0, 3), new SourceBlock(29, 31, 0));
      lookup54[245] = new SingleColorLookup(new SourceBlock(30, 0, 2), new SourceBlock(29, 31, 1));
      lookup54[246] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(30, 30, 1));
      lookup54[247] = new SingleColorLookup(new SourceBlock(30, 0, 0), new SourceBlock(30, 30, 0));
      lookup54[248] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(30, 31, 1));
      lookup54[249] = new SingleColorLookup(new SourceBlock(30, 0, 2), new SourceBlock(30, 31, 0));
      lookup54[250] = new SingleColorLookup(new SourceBlock(30, 0, 3), new SourceBlock(30, 31, 1));
      lookup54[251] = new SingleColorLookup(new SourceBlock(30, 0, 4), new SourceBlock(31, 30, 1));
      lookup54[252] = new SingleColorLookup(new SourceBlock(31, 0, 3), new SourceBlock(31, 30, 0));
      lookup54[253] = new SingleColorLookup(new SourceBlock(31, 0, 2), new SourceBlock(31, 30, 1));
      lookup54[254] = new SingleColorLookup(new SourceBlock(31, 0, 1), new SourceBlock(31, 31, 1));
      lookup54[255] = new SingleColorLookup(new SourceBlock(31, 0, 0), new SourceBlock(31, 31, 0));

      lookup64[0] = new SingleColorLookup(new SourceBlock(0, 0, 0), new SourceBlock(0, 0, 0));
      lookup64[1] = new SingleColorLookup(new SourceBlock(0, 0, 1), new SourceBlock(0, 1, 0));
      lookup64[2] = new SingleColorLookup(new SourceBlock(0, 0, 2), new SourceBlock(0, 2, 0));
      lookup64[3] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(0, 3, 1));
      lookup64[4] = new SingleColorLookup(new SourceBlock(1, 0, 0), new SourceBlock(0, 3, 0));
      lookup64[5] = new SingleColorLookup(new SourceBlock(1, 0, 1), new SourceBlock(0, 4, 0));
      lookup64[6] = new SingleColorLookup(new SourceBlock(1, 0, 2), new SourceBlock(0, 5, 0));
      lookup64[7] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(0, 6, 1));
      lookup64[8] = new SingleColorLookup(new SourceBlock(2, 0, 0), new SourceBlock(0, 6, 0));
      lookup64[9] = new SingleColorLookup(new SourceBlock(2, 0, 1), new SourceBlock(0, 7, 0));
      lookup64[10] = new SingleColorLookup(new SourceBlock(2, 0, 2), new SourceBlock(0, 8, 0));
      lookup64[11] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 9, 1));
      lookup64[12] = new SingleColorLookup(new SourceBlock(3, 0, 0), new SourceBlock(0, 9, 0));
      lookup64[13] = new SingleColorLookup(new SourceBlock(3, 0, 1), new SourceBlock(0, 10, 0));
      lookup64[14] = new SingleColorLookup(new SourceBlock(3, 0, 2), new SourceBlock(0, 11, 0));
      lookup64[15] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(0, 12, 1));
      lookup64[16] = new SingleColorLookup(new SourceBlock(4, 0, 0), new SourceBlock(0, 12, 0));
      lookup64[17] = new SingleColorLookup(new SourceBlock(4, 0, 1), new SourceBlock(0, 13, 0));
      lookup64[18] = new SingleColorLookup(new SourceBlock(4, 0, 2), new SourceBlock(0, 14, 0));
      lookup64[19] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(0, 15, 1));
      lookup64[20] = new SingleColorLookup(new SourceBlock(5, 0, 0), new SourceBlock(0, 15, 0));
      lookup64[21] = new SingleColorLookup(new SourceBlock(5, 0, 1), new SourceBlock(0, 16, 0));
      lookup64[22] = new SingleColorLookup(new SourceBlock(5, 0, 2), new SourceBlock(1, 15, 0));
      lookup64[23] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(0, 17, 0));
      lookup64[24] = new SingleColorLookup(new SourceBlock(6, 0, 0), new SourceBlock(0, 18, 0));
      lookup64[25] = new SingleColorLookup(new SourceBlock(6, 0, 1), new SourceBlock(0, 19, 0));
      lookup64[26] = new SingleColorLookup(new SourceBlock(6, 0, 2), new SourceBlock(3, 14, 0));
      lookup64[27] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 20, 0));
      lookup64[28] = new SingleColorLookup(new SourceBlock(7, 0, 0), new SourceBlock(0, 21, 0));
      lookup64[29] = new SingleColorLookup(new SourceBlock(7, 0, 1), new SourceBlock(0, 22, 0));
      lookup64[30] = new SingleColorLookup(new SourceBlock(7, 0, 2), new SourceBlock(4, 15, 0));
      lookup64[31] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(0, 23, 0));
      lookup64[32] = new SingleColorLookup(new SourceBlock(8, 0, 0), new SourceBlock(0, 24, 0));
      lookup64[33] = new SingleColorLookup(new SourceBlock(8, 0, 1), new SourceBlock(0, 25, 0));
      lookup64[34] = new SingleColorLookup(new SourceBlock(8, 0, 2), new SourceBlock(6, 14, 0));
      lookup64[35] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(0, 26, 0));
      lookup64[36] = new SingleColorLookup(new SourceBlock(9, 0, 0), new SourceBlock(0, 27, 0));
      lookup64[37] = new SingleColorLookup(new SourceBlock(9, 0, 1), new SourceBlock(0, 28, 0));
      lookup64[38] = new SingleColorLookup(new SourceBlock(9, 0, 2), new SourceBlock(7, 15, 0));
      lookup64[39] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(0, 29, 0));
      lookup64[40] = new SingleColorLookup(new SourceBlock(10, 0, 0), new SourceBlock(0, 30, 0));
      lookup64[41] = new SingleColorLookup(new SourceBlock(10, 0, 1), new SourceBlock(0, 31, 0));
      lookup64[42] = new SingleColorLookup(new SourceBlock(10, 0, 2), new SourceBlock(9, 14, 0));
      lookup64[43] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(0, 32, 0));
      lookup64[44] = new SingleColorLookup(new SourceBlock(11, 0, 0), new SourceBlock(0, 33, 0));
      lookup64[45] = new SingleColorLookup(new SourceBlock(11, 0, 1), new SourceBlock(2, 30, 0));
      lookup64[46] = new SingleColorLookup(new SourceBlock(11, 0, 2), new SourceBlock(0, 34, 0));
      lookup64[47] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(0, 35, 0));
      lookup64[48] = new SingleColorLookup(new SourceBlock(12, 0, 0), new SourceBlock(0, 36, 0));
      lookup64[49] = new SingleColorLookup(new SourceBlock(12, 0, 1), new SourceBlock(3, 31, 0));
      lookup64[50] = new SingleColorLookup(new SourceBlock(12, 0, 2), new SourceBlock(0, 37, 0));
      lookup64[51] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(0, 38, 0));
      lookup64[52] = new SingleColorLookup(new SourceBlock(13, 0, 0), new SourceBlock(0, 39, 0));
      lookup64[53] = new SingleColorLookup(new SourceBlock(13, 0, 1), new SourceBlock(5, 30, 0));
      lookup64[54] = new SingleColorLookup(new SourceBlock(13, 0, 2), new SourceBlock(0, 40, 0));
      lookup64[55] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(0, 41, 0));
      lookup64[56] = new SingleColorLookup(new SourceBlock(14, 0, 0), new SourceBlock(0, 42, 0));
      lookup64[57] = new SingleColorLookup(new SourceBlock(14, 0, 1), new SourceBlock(6, 31, 0));
      lookup64[58] = new SingleColorLookup(new SourceBlock(14, 0, 2), new SourceBlock(0, 43, 0));
      lookup64[59] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(0, 44, 0));
      lookup64[60] = new SingleColorLookup(new SourceBlock(15, 0, 0), new SourceBlock(0, 45, 0));
      lookup64[61] = new SingleColorLookup(new SourceBlock(15, 0, 1), new SourceBlock(8, 30, 0));
      lookup64[62] = new SingleColorLookup(new SourceBlock(15, 0, 2), new SourceBlock(0, 46, 0));
      lookup64[63] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(0, 47, 0));
      lookup64[64] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(1, 46, 0));
      lookup64[65] = new SingleColorLookup(new SourceBlock(16, 0, 0), new SourceBlock(0, 48, 0));
      lookup64[66] = new SingleColorLookup(new SourceBlock(16, 0, 1), new SourceBlock(0, 49, 0));
      lookup64[67] = new SingleColorLookup(new SourceBlock(16, 0, 2), new SourceBlock(0, 50, 0));
      lookup64[68] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(2, 47, 0));
      lookup64[69] = new SingleColorLookup(new SourceBlock(17, 0, 0), new SourceBlock(0, 51, 0));
      lookup64[70] = new SingleColorLookup(new SourceBlock(17, 0, 1), new SourceBlock(0, 52, 0));
      lookup64[71] = new SingleColorLookup(new SourceBlock(17, 0, 2), new SourceBlock(0, 53, 0));
      lookup64[72] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(4, 46, 0));
      lookup64[73] = new SingleColorLookup(new SourceBlock(18, 0, 0), new SourceBlock(0, 54, 0));
      lookup64[74] = new SingleColorLookup(new SourceBlock(18, 0, 1), new SourceBlock(0, 55, 0));
      lookup64[75] = new SingleColorLookup(new SourceBlock(18, 0, 2), new SourceBlock(0, 56, 0));
      lookup64[76] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(5, 47, 0));
      lookup64[77] = new SingleColorLookup(new SourceBlock(19, 0, 0), new SourceBlock(0, 57, 0));
      lookup64[78] = new SingleColorLookup(new SourceBlock(19, 0, 1), new SourceBlock(0, 58, 0));
      lookup64[79] = new SingleColorLookup(new SourceBlock(19, 0, 2), new SourceBlock(0, 59, 0));
      lookup64[80] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(7, 46, 0));
      lookup64[81] = new SingleColorLookup(new SourceBlock(20, 0, 0), new SourceBlock(0, 60, 0));
      lookup64[82] = new SingleColorLookup(new SourceBlock(20, 0, 1), new SourceBlock(0, 61, 0));
      lookup64[83] = new SingleColorLookup(new SourceBlock(20, 0, 2), new SourceBlock(0, 62, 0));
      lookup64[84] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(8, 47, 0));
      lookup64[85] = new SingleColorLookup(new SourceBlock(21, 0, 0), new SourceBlock(0, 63, 0));
      lookup64[86] = new SingleColorLookup(new SourceBlock(21, 0, 1), new SourceBlock(1, 62, 0));
      lookup64[87] = new SingleColorLookup(new SourceBlock(21, 0, 2), new SourceBlock(1, 63, 0));
      lookup64[88] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(10, 46, 0));
      lookup64[89] = new SingleColorLookup(new SourceBlock(22, 0, 0), new SourceBlock(2, 62, 0));
      lookup64[90] = new SingleColorLookup(new SourceBlock(22, 0, 1), new SourceBlock(2, 63, 0));
      lookup64[91] = new SingleColorLookup(new SourceBlock(22, 0, 2), new SourceBlock(3, 62, 0));
      lookup64[92] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(11, 47, 0));
      lookup64[93] = new SingleColorLookup(new SourceBlock(23, 0, 0), new SourceBlock(3, 63, 0));
      lookup64[94] = new SingleColorLookup(new SourceBlock(23, 0, 1), new SourceBlock(4, 62, 0));
      lookup64[95] = new SingleColorLookup(new SourceBlock(23, 0, 2), new SourceBlock(4, 63, 0));
      lookup64[96] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(13, 46, 0));
      lookup64[97] = new SingleColorLookup(new SourceBlock(24, 0, 0), new SourceBlock(5, 62, 0));
      lookup64[98] = new SingleColorLookup(new SourceBlock(24, 0, 1), new SourceBlock(5, 63, 0));
      lookup64[99] = new SingleColorLookup(new SourceBlock(24, 0, 2), new SourceBlock(6, 62, 0));
      lookup64[100] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(14, 47, 0));
      lookup64[101] = new SingleColorLookup(new SourceBlock(25, 0, 0), new SourceBlock(6, 63, 0));
      lookup64[102] = new SingleColorLookup(new SourceBlock(25, 0, 1), new SourceBlock(7, 62, 0));
      lookup64[103] = new SingleColorLookup(new SourceBlock(25, 0, 2), new SourceBlock(7, 63, 0));
      lookup64[104] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(16, 45, 0));
      lookup64[105] = new SingleColorLookup(new SourceBlock(26, 0, 0), new SourceBlock(8, 62, 0));
      lookup64[106] = new SingleColorLookup(new SourceBlock(26, 0, 1), new SourceBlock(8, 63, 0));
      lookup64[107] = new SingleColorLookup(new SourceBlock(26, 0, 2), new SourceBlock(9, 62, 0));
      lookup64[108] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(16, 48, 0));
      lookup64[109] = new SingleColorLookup(new SourceBlock(27, 0, 0), new SourceBlock(9, 63, 0));
      lookup64[110] = new SingleColorLookup(new SourceBlock(27, 0, 1), new SourceBlock(10, 62, 0));
      lookup64[111] = new SingleColorLookup(new SourceBlock(27, 0, 2), new SourceBlock(10, 63, 0));
      lookup64[112] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(16, 51, 0));
      lookup64[113] = new SingleColorLookup(new SourceBlock(28, 0, 0), new SourceBlock(11, 62, 0));
      lookup64[114] = new SingleColorLookup(new SourceBlock(28, 0, 1), new SourceBlock(11, 63, 0));
      lookup64[115] = new SingleColorLookup(new SourceBlock(28, 0, 2), new SourceBlock(12, 62, 0));
      lookup64[116] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(16, 54, 0));
      lookup64[117] = new SingleColorLookup(new SourceBlock(29, 0, 0), new SourceBlock(12, 63, 0));
      lookup64[118] = new SingleColorLookup(new SourceBlock(29, 0, 1), new SourceBlock(13, 62, 0));
      lookup64[119] = new SingleColorLookup(new SourceBlock(29, 0, 2), new SourceBlock(13, 63, 0));
      lookup64[120] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(16, 57, 0));
      lookup64[121] = new SingleColorLookup(new SourceBlock(30, 0, 0), new SourceBlock(14, 62, 0));
      lookup64[122] = new SingleColorLookup(new SourceBlock(30, 0, 1), new SourceBlock(14, 63, 0));
      lookup64[123] = new SingleColorLookup(new SourceBlock(30, 0, 2), new SourceBlock(15, 62, 0));
      lookup64[124] = new SingleColorLookup(new SourceBlock(31, 0, 1), new SourceBlock(16, 60, 0));
      lookup64[125] = new SingleColorLookup(new SourceBlock(31, 0, 0), new SourceBlock(15, 63, 0));
      lookup64[126] = new SingleColorLookup(new SourceBlock(31, 0, 1), new SourceBlock(24, 46, 0));
      lookup64[127] = new SingleColorLookup(new SourceBlock(31, 0, 2), new SourceBlock(16, 62, 0));
      lookup64[128] = new SingleColorLookup(new SourceBlock(32, 0, 2), new SourceBlock(16, 63, 0));
      lookup64[129] = new SingleColorLookup(new SourceBlock(32, 0, 1), new SourceBlock(17, 62, 0));
      lookup64[130] = new SingleColorLookup(new SourceBlock(32, 0, 0), new SourceBlock(25, 47, 0));
      lookup64[131] = new SingleColorLookup(new SourceBlock(32, 0, 1), new SourceBlock(17, 63, 0));
      lookup64[132] = new SingleColorLookup(new SourceBlock(32, 0, 2), new SourceBlock(18, 62, 0));
      lookup64[133] = new SingleColorLookup(new SourceBlock(33, 0, 1), new SourceBlock(18, 63, 0));
      lookup64[134] = new SingleColorLookup(new SourceBlock(33, 0, 0), new SourceBlock(27, 46, 0));
      lookup64[135] = new SingleColorLookup(new SourceBlock(33, 0, 1), new SourceBlock(19, 62, 0));
      lookup64[136] = new SingleColorLookup(new SourceBlock(33, 0, 2), new SourceBlock(19, 63, 0));
      lookup64[137] = new SingleColorLookup(new SourceBlock(34, 0, 1), new SourceBlock(20, 62, 0));
      lookup64[138] = new SingleColorLookup(new SourceBlock(34, 0, 0), new SourceBlock(28, 47, 0));
      lookup64[139] = new SingleColorLookup(new SourceBlock(34, 0, 1), new SourceBlock(20, 63, 0));
      lookup64[140] = new SingleColorLookup(new SourceBlock(34, 0, 2), new SourceBlock(21, 62, 0));
      lookup64[141] = new SingleColorLookup(new SourceBlock(35, 0, 1), new SourceBlock(21, 63, 0));
      lookup64[142] = new SingleColorLookup(new SourceBlock(35, 0, 0), new SourceBlock(30, 46, 0));
      lookup64[143] = new SingleColorLookup(new SourceBlock(35, 0, 1), new SourceBlock(22, 62, 0));
      lookup64[144] = new SingleColorLookup(new SourceBlock(35, 0, 2), new SourceBlock(22, 63, 0));
      lookup64[145] = new SingleColorLookup(new SourceBlock(36, 0, 1), new SourceBlock(23, 62, 0));
      lookup64[146] = new SingleColorLookup(new SourceBlock(36, 0, 0), new SourceBlock(31, 47, 0));
      lookup64[147] = new SingleColorLookup(new SourceBlock(36, 0, 1), new SourceBlock(23, 63, 0));
      lookup64[148] = new SingleColorLookup(new SourceBlock(36, 0, 2), new SourceBlock(24, 62, 0));
      lookup64[149] = new SingleColorLookup(new SourceBlock(37, 0, 1), new SourceBlock(24, 63, 0));
      lookup64[150] = new SingleColorLookup(new SourceBlock(37, 0, 0), new SourceBlock(32, 47, 0));
      lookup64[151] = new SingleColorLookup(new SourceBlock(37, 0, 1), new SourceBlock(25, 62, 0));
      lookup64[152] = new SingleColorLookup(new SourceBlock(37, 0, 2), new SourceBlock(25, 63, 0));
      lookup64[153] = new SingleColorLookup(new SourceBlock(38, 0, 1), new SourceBlock(26, 62, 0));
      lookup64[154] = new SingleColorLookup(new SourceBlock(38, 0, 0), new SourceBlock(32, 50, 0));
      lookup64[155] = new SingleColorLookup(new SourceBlock(38, 0, 1), new SourceBlock(26, 63, 0));
      lookup64[156] = new SingleColorLookup(new SourceBlock(38, 0, 2), new SourceBlock(27, 62, 0));
      lookup64[157] = new SingleColorLookup(new SourceBlock(39, 0, 1), new SourceBlock(27, 63, 0));
      lookup64[158] = new SingleColorLookup(new SourceBlock(39, 0, 0), new SourceBlock(32, 53, 0));
      lookup64[159] = new SingleColorLookup(new SourceBlock(39, 0, 1), new SourceBlock(28, 62, 0));
      lookup64[160] = new SingleColorLookup(new SourceBlock(39, 0, 2), new SourceBlock(28, 63, 0));
      lookup64[161] = new SingleColorLookup(new SourceBlock(40, 0, 1), new SourceBlock(29, 62, 0));
      lookup64[162] = new SingleColorLookup(new SourceBlock(40, 0, 0), new SourceBlock(32, 56, 0));
      lookup64[163] = new SingleColorLookup(new SourceBlock(40, 0, 1), new SourceBlock(29, 63, 0));
      lookup64[164] = new SingleColorLookup(new SourceBlock(40, 0, 2), new SourceBlock(30, 62, 0));
      lookup64[165] = new SingleColorLookup(new SourceBlock(41, 0, 1), new SourceBlock(30, 63, 0));
      lookup64[166] = new SingleColorLookup(new SourceBlock(41, 0, 0), new SourceBlock(32, 59, 0));
      lookup64[167] = new SingleColorLookup(new SourceBlock(41, 0, 1), new SourceBlock(31, 62, 0));
      lookup64[168] = new SingleColorLookup(new SourceBlock(41, 0, 2), new SourceBlock(31, 63, 0));
      lookup64[169] = new SingleColorLookup(new SourceBlock(42, 0, 1), new SourceBlock(32, 61, 0));
      lookup64[170] = new SingleColorLookup(new SourceBlock(42, 0, 0), new SourceBlock(32, 62, 0));
      lookup64[171] = new SingleColorLookup(new SourceBlock(42, 0, 1), new SourceBlock(32, 63, 0));
      lookup64[172] = new SingleColorLookup(new SourceBlock(42, 0, 2), new SourceBlock(41, 46, 0));
      lookup64[173] = new SingleColorLookup(new SourceBlock(43, 0, 1), new SourceBlock(33, 62, 0));
      lookup64[174] = new SingleColorLookup(new SourceBlock(43, 0, 0), new SourceBlock(33, 63, 0));
      lookup64[175] = new SingleColorLookup(new SourceBlock(43, 0, 1), new SourceBlock(34, 62, 0));
      lookup64[176] = new SingleColorLookup(new SourceBlock(43, 0, 2), new SourceBlock(42, 47, 0));
      lookup64[177] = new SingleColorLookup(new SourceBlock(44, 0, 1), new SourceBlock(34, 63, 0));
      lookup64[178] = new SingleColorLookup(new SourceBlock(44, 0, 0), new SourceBlock(35, 62, 0));
      lookup64[179] = new SingleColorLookup(new SourceBlock(44, 0, 1), new SourceBlock(35, 63, 0));
      lookup64[180] = new SingleColorLookup(new SourceBlock(44, 0, 2), new SourceBlock(44, 46, 0));
      lookup64[181] = new SingleColorLookup(new SourceBlock(45, 0, 1), new SourceBlock(36, 62, 0));
      lookup64[182] = new SingleColorLookup(new SourceBlock(45, 0, 0), new SourceBlock(36, 63, 0));
      lookup64[183] = new SingleColorLookup(new SourceBlock(45, 0, 1), new SourceBlock(37, 62, 0));
      lookup64[184] = new SingleColorLookup(new SourceBlock(45, 0, 2), new SourceBlock(45, 47, 0));
      lookup64[185] = new SingleColorLookup(new SourceBlock(46, 0, 1), new SourceBlock(37, 63, 0));
      lookup64[186] = new SingleColorLookup(new SourceBlock(46, 0, 0), new SourceBlock(38, 62, 0));
      lookup64[187] = new SingleColorLookup(new SourceBlock(46, 0, 1), new SourceBlock(38, 63, 0));
      lookup64[188] = new SingleColorLookup(new SourceBlock(46, 0, 2), new SourceBlock(47, 46, 0));
      lookup64[189] = new SingleColorLookup(new SourceBlock(47, 0, 1), new SourceBlock(39, 62, 0));
      lookup64[190] = new SingleColorLookup(new SourceBlock(47, 0, 0), new SourceBlock(39, 63, 0));
      lookup64[191] = new SingleColorLookup(new SourceBlock(47, 0, 1), new SourceBlock(40, 62, 0));
      lookup64[192] = new SingleColorLookup(new SourceBlock(47, 0, 2), new SourceBlock(48, 46, 0));
      lookup64[193] = new SingleColorLookup(new SourceBlock(48, 0, 2), new SourceBlock(40, 63, 0));
      lookup64[194] = new SingleColorLookup(new SourceBlock(48, 0, 1), new SourceBlock(41, 62, 0));
      lookup64[195] = new SingleColorLookup(new SourceBlock(48, 0, 0), new SourceBlock(41, 63, 0));
      lookup64[196] = new SingleColorLookup(new SourceBlock(48, 0, 1), new SourceBlock(48, 49, 0));
      lookup64[197] = new SingleColorLookup(new SourceBlock(48, 0, 2), new SourceBlock(42, 62, 0));
      lookup64[198] = new SingleColorLookup(new SourceBlock(49, 0, 1), new SourceBlock(42, 63, 0));
      lookup64[199] = new SingleColorLookup(new SourceBlock(49, 0, 0), new SourceBlock(43, 62, 0));
      lookup64[200] = new SingleColorLookup(new SourceBlock(49, 0, 1), new SourceBlock(48, 52, 0));
      lookup64[201] = new SingleColorLookup(new SourceBlock(49, 0, 2), new SourceBlock(43, 63, 0));
      lookup64[202] = new SingleColorLookup(new SourceBlock(50, 0, 1), new SourceBlock(44, 62, 0));
      lookup64[203] = new SingleColorLookup(new SourceBlock(50, 0, 0), new SourceBlock(44, 63, 0));
      lookup64[204] = new SingleColorLookup(new SourceBlock(50, 0, 1), new SourceBlock(48, 55, 0));
      lookup64[205] = new SingleColorLookup(new SourceBlock(50, 0, 2), new SourceBlock(45, 62, 0));
      lookup64[206] = new SingleColorLookup(new SourceBlock(51, 0, 1), new SourceBlock(45, 63, 0));
      lookup64[207] = new SingleColorLookup(new SourceBlock(51, 0, 0), new SourceBlock(46, 62, 0));
      lookup64[208] = new SingleColorLookup(new SourceBlock(51, 0, 1), new SourceBlock(48, 58, 0));
      lookup64[209] = new SingleColorLookup(new SourceBlock(51, 0, 2), new SourceBlock(46, 63, 0));
      lookup64[210] = new SingleColorLookup(new SourceBlock(52, 0, 1), new SourceBlock(47, 62, 0));
      lookup64[211] = new SingleColorLookup(new SourceBlock(52, 0, 0), new SourceBlock(47, 63, 0));
      lookup64[212] = new SingleColorLookup(new SourceBlock(52, 0, 1), new SourceBlock(48, 61, 0));
      lookup64[213] = new SingleColorLookup(new SourceBlock(52, 0, 2), new SourceBlock(48, 62, 0));
      lookup64[214] = new SingleColorLookup(new SourceBlock(53, 0, 1), new SourceBlock(56, 47, 0));
      lookup64[215] = new SingleColorLookup(new SourceBlock(53, 0, 0), new SourceBlock(48, 63, 0));
      lookup64[216] = new SingleColorLookup(new SourceBlock(53, 0, 1), new SourceBlock(49, 62, 0));
      lookup64[217] = new SingleColorLookup(new SourceBlock(53, 0, 2), new SourceBlock(49, 63, 0));
      lookup64[218] = new SingleColorLookup(new SourceBlock(54, 0, 1), new SourceBlock(58, 46, 0));
      lookup64[219] = new SingleColorLookup(new SourceBlock(54, 0, 0), new SourceBlock(50, 62, 0));
      lookup64[220] = new SingleColorLookup(new SourceBlock(54, 0, 1), new SourceBlock(50, 63, 0));
      lookup64[221] = new SingleColorLookup(new SourceBlock(54, 0, 2), new SourceBlock(51, 62, 0));
      lookup64[222] = new SingleColorLookup(new SourceBlock(55, 0, 1), new SourceBlock(59, 47, 0));
      lookup64[223] = new SingleColorLookup(new SourceBlock(55, 0, 0), new SourceBlock(51, 63, 0));
      lookup64[224] = new SingleColorLookup(new SourceBlock(55, 0, 1), new SourceBlock(52, 62, 0));
      lookup64[225] = new SingleColorLookup(new SourceBlock(55, 0, 2), new SourceBlock(52, 63, 0));
      lookup64[226] = new SingleColorLookup(new SourceBlock(56, 0, 1), new SourceBlock(61, 46, 0));
      lookup64[227] = new SingleColorLookup(new SourceBlock(56, 0, 0), new SourceBlock(53, 62, 0));
      lookup64[228] = new SingleColorLookup(new SourceBlock(56, 0, 1), new SourceBlock(53, 63, 0));
      lookup64[229] = new SingleColorLookup(new SourceBlock(56, 0, 2), new SourceBlock(54, 62, 0));
      lookup64[230] = new SingleColorLookup(new SourceBlock(57, 0, 1), new SourceBlock(62, 47, 0));
      lookup64[231] = new SingleColorLookup(new SourceBlock(57, 0, 0), new SourceBlock(54, 63, 0));
      lookup64[232] = new SingleColorLookup(new SourceBlock(57, 0, 1), new SourceBlock(55, 62, 0));
      lookup64[233] = new SingleColorLookup(new SourceBlock(57, 0, 2), new SourceBlock(55, 63, 0));
      lookup64[234] = new SingleColorLookup(new SourceBlock(58, 0, 1), new SourceBlock(56, 62, 1));
      lookup64[235] = new SingleColorLookup(new SourceBlock(58, 0, 0), new SourceBlock(56, 62, 0));
      lookup64[236] = new SingleColorLookup(new SourceBlock(58, 0, 1), new SourceBlock(56, 63, 0));
      lookup64[237] = new SingleColorLookup(new SourceBlock(58, 0, 2), new SourceBlock(57, 62, 0));
      lookup64[238] = new SingleColorLookup(new SourceBlock(59, 0, 1), new SourceBlock(57, 63, 1));
      lookup64[239] = new SingleColorLookup(new SourceBlock(59, 0, 0), new SourceBlock(57, 63, 0));
      lookup64[240] = new SingleColorLookup(new SourceBlock(59, 0, 1), new SourceBlock(58, 62, 0));
      lookup64[241] = new SingleColorLookup(new SourceBlock(59, 0, 2), new SourceBlock(58, 63, 0));
      lookup64[242] = new SingleColorLookup(new SourceBlock(60, 0, 1), new SourceBlock(59, 62, 1));
      lookup64[243] = new SingleColorLookup(new SourceBlock(60, 0, 0), new SourceBlock(59, 62, 0));
      lookup64[244] = new SingleColorLookup(new SourceBlock(60, 0, 1), new SourceBlock(59, 63, 0));
      lookup64[245] = new SingleColorLookup(new SourceBlock(60, 0, 2), new SourceBlock(60, 62, 0));
      lookup64[246] = new SingleColorLookup(new SourceBlock(61, 0, 1), new SourceBlock(60, 63, 1));
      lookup64[247] = new SingleColorLookup(new SourceBlock(61, 0, 0), new SourceBlock(60, 63, 0));
      lookup64[248] = new SingleColorLookup(new SourceBlock(61, 0, 1), new SourceBlock(61, 62, 0));
      lookup64[249] = new SingleColorLookup(new SourceBlock(61, 0, 2), new SourceBlock(61, 63, 0));
      lookup64[250] = new SingleColorLookup(new SourceBlock(62, 0, 1), new SourceBlock(62, 62, 1));
      lookup64[251] = new SingleColorLookup(new SourceBlock(62, 0, 0), new SourceBlock(62, 62, 0));
      lookup64[252] = new SingleColorLookup(new SourceBlock(62, 0, 1), new SourceBlock(62, 63, 0));
      lookup64[253] = new SingleColorLookup(new SourceBlock(62, 0, 2), new SourceBlock(63, 62, 0));
      lookup64[254] = new SingleColorLookup(new SourceBlock(63, 0, 1), new SourceBlock(63, 63, 1));
      lookup64[255] = new SingleColorLookup(new SourceBlock(63, 0, 0), new SourceBlock(63, 63, 0));
    }
  }


  public static class Vec3
  {
    float vx, vy, vz;

    public static Vec3 neg(Vec3 v)
    {
      return new Vec3(-v.vx, -v.vy, -v.vz);
    }

    public static Vec3 add(Vec3 v1, Vec3 v2)
    {
      return new Vec3(v1.vx + v2.vx, v1.vy + v2.vy, v1.vz + v2.vz);
    }

    public static Vec3 sub(Vec3 v1, Vec3 v2)
    {
      return new Vec3(v1.vx - v2.vx, v1.vy - v2.vy, v1.vz - v2.vz);
    }

    public static Vec3 mul(Vec3 v1, Vec3 v2)
    {
      return new Vec3(v1.vx * v2.vx, v1.vy * v2.vy, v1.vz * v2.vz);
    }

    public static Vec3 mul(Vec3 v, float s)
    {
      return new Vec3(v.vx * s, v.vy * s, v.vz * s);
    }

    public static Vec3 div(Vec3 v1, Vec3 v2)
    {
      return new Vec3(v1.vx / v2.vx, v1.vy / v2.vy, v1.vz / v2.vz);
    }

    public static Vec3 div(Vec3 v, float s)
    {
      float t = 1.0f / s;
      return new Vec3(v.vx * t, v.vy * t, v.vz * t);
    }

    public static float dot(Vec3 v1, Vec3 v2)
    {
      return v1.vx*v2.vx + v1.vy*v2.vy + v1.vz*v2.vz;
    }

    public static Vec3 min(Vec3 v1, Vec3 v2)
    {
      v1.fixNaN(); v2.fixNaN();
      return new Vec3(Math.min(v1.vx, v2.vx), Math.min(v1.vy, v2.vy), Math.min(v1.vz, v2.vz));
    }

    public static Vec3 max(Vec3 v1, Vec3 v2)
    {
      v1.fixNaN(); v2.fixNaN();
      return new Vec3(Math.max(v1.vx, v2.vx), Math.max(v1.vy, v2.vy), Math.max(v1.vz, v2.vz));
    }

    public static Vec3 truncate(Vec3 v)
    {
      return new Vec3((v.vx > 0.0f) ? (float)Math.floor(v.vx) : (float)Math.ceil(v.vx),
                      (v.vy > 0.0f) ? (float)Math.floor(v.vy) : (float)Math.ceil(v.vy),
                      (v.vz > 0.0f) ? (float)Math.floor(v.vz) : (float)Math.ceil(v.vz));
    }

    public static float lengthSquared(Vec3 v)
    {
      return v.vx*v.vx + v.vy*v.vy + v.vz*v.vz;
    }

    public Vec3()
    {
      vx = 0.0f; vy = 0.0f; vz = 0.0f;
    }

    public Vec3(float s)
    {
      vx = s; vy = s; vz = s;
    }

    public Vec3(float x, float y, float z)
    {
      vx = x; vy = y; vz = z;
    }

    @Override
    public boolean equals(Object o)
    {
      if (o instanceof Vec3) {
        return ((Vec3)o).x() == vx && ((Vec3)o).y() == vy && ((Vec3)o).z() == vz;
      } else if (o instanceof Vec4) {
        return ((Vec4)o).x() == vx && ((Vec4)o).y() == vy && ((Vec4)o).z() == vz && ((Vec4)o).w() == 0.0f;
      } else {
        return false;
      }
    }

    @Override
    public String toString()
    {
      return "(" + vx + ", " + vy + ", " + vz + ")";
    }

    @Override
    public Object clone()
    {
      return new Vec3(vx, vy, vz);
    }

    public float x() { return vx; }
    public float y() { return vy; }
    public float z() { return vz; }

    public Vec3 add(Vec3 v)
    {
      vx += v.vx;
      vy += v.vy;
      vz += v.vz;
      return this;
    }

    public Vec3 sub(Vec3 v)
    {
      vx -= v.vx;
      vy -= v.vy;
      vz -= v.vz;
      return this;
    }

    public Vec3 mul(Vec3 v)
    {
      vx *= v.vx;
      vy *= v.vy;
      vz *= v.vz;
      return this;
    }

    public Vec3 mul(float s)
    {
      vx *= s;
      vy *= s;
      vz *= s;
      return this;
    }

    public Vec3 div(Vec3 v)
    {
      vx /= v.vx;
      vy /= v.vy;
      vz /= v.vz;
      return this;
    }

    public Vec3 div(float s)
    {
      float t = 1.0f / s;
      vx *= t;
      vy *= t;
      vz *= t;
      return this;
    }

    public float dot(Vec3 v)
    {
      return vx*v.vx + vy*v.vy + vz*v.vz;
    }

    // Converts NaN values into a defined number
    private Vec3 fixNaN()
    {
      if (Float.isNaN(vx)) vx = 0.0f;
      if (Float.isNaN(vy)) vy = 0.0f;
      if (Float.isNaN(vz)) vz = 0.0f;
      return this;
    }
  }


  public static class Vec4
  {
    float vx, vy, vz, vw;

//    public static Vec4 neg(Vec4 v)
//    {
//      return new Vec4(-v.vx, -v.vy, -v.vz, -v.vw);
//    }

    public static Vec4 add(Vec4 v1, Vec4 v2)
    {
      return new Vec4(v1.vx + v2.vx, v1.vy + v2.vy, v1.vz + v2.vz, v1.vw + v2.vw);
    }

    public static Vec4 sub(Vec4 v1, Vec4 v2)
    {
      return new Vec4(v1.vx - v2.vx, v1.vy - v2.vy, v1.vz - v2.vz, v1.vw - v2.vw);
    }

    public static Vec4 mul(Vec4 v1, Vec4 v2)
    {
      return new Vec4(v1.vx * v2.vx, v1.vy * v2.vy, v1.vz * v2.vz, v1.vw * v2.vw);
    }

    public static Vec4 multiplyAdd(Vec4 v1, Vec4 v2, Vec4 v3)
    {
      return new Vec4(v1.vx*v2.vx + v3.vx, v1.vy*v2.vy + v3.vy, v1.vz*v2.vz + v3.vz, v1.vw*v2.vw + v3.vw);
    }

    public static Vec4 negMulSub(Vec4 v1, Vec4 v2, Vec4 v3)
    {
      return new Vec4(v3.vx - v1.vx*v2.vx, v3.vy - v1.vy*v2.vy, v3.vz - v1.vz*v2.vz, v3.vw - v1.vw*v2.vw);
    }

    public static Vec4 reciprocal(Vec4 v)
    {
      return new Vec4(1.0f / v.vx, 1.0f / v.vy, 1.0f / v.vz, 1.0f / v.vw);
    }

    public static Vec4 min(Vec4 v1, Vec4 v2)
    {
      v1.fixNaN(); v2.fixNaN();
      return new Vec4(Math.min(v1.vx, v2.vx),
                      Math.min(v1.vy, v2.vy),
                      Math.min(v1.vz, v2.vz),
                      Math.min(v1.vw, v2.vw));

    }

    public static Vec4 max(Vec4 v1, Vec4 v2)
    {
      v1.fixNaN(); v2.fixNaN();
      return new Vec4(Math.max(v1.vx, v2.vx),
                      Math.max(v1.vy, v2.vy),
                      Math.max(v1.vz, v2.vz),
                      Math.max(v1.vw, v2.vw));
    }

    public static Vec4 truncate(Vec4 v)
    {
      return new Vec4((v.vx > 0.0f) ? (float)Math.floor(v.vx) : (float)Math.ceil(v.vx),
                      (v.vy > 0.0f) ? (float)Math.floor(v.vy) : (float)Math.ceil(v.vy),
                      (v.vz > 0.0f) ? (float)Math.floor(v.vz) : (float)Math.ceil(v.vz),
                      (v.vw > 0.0f) ? (float)Math.floor(v.vw) : (float)Math.ceil(v.vw));
    }

    public static boolean compareAnyLessThan(Vec4 v1, Vec4 v2)
    {
      return (v1.vx < v2.vx || v1.vy < v2.vy || v1.vz < v2.vz || v1.vw < v2.vw);
    }

    public Vec4()
    {
      vx = 0.0f; vy = 0.0f; vz = 0.0f; vw = 0.0f;
    }

    public Vec4(float s)
    {
      vx = s; vy = s; vz = s; vw = s;
    }

    public Vec4(float x, float y, float z, float w)
    {
      vx = x; vy = y; vz = z; vw = w;
    }

    @Override
    public boolean equals(Object o)
    {
      if (o instanceof Vec4) {
        return ((Vec4)o).x() == vx && ((Vec4)o).y() == vy && ((Vec4)o).z() == vz && ((Vec4)o).w() == vw;
      } else if (o instanceof Vec3) {
        return ((Vec3)o).x() == vx && ((Vec3)o).y() == vy && ((Vec3)o).z() == vz && vw == 0.0f;
      } else {
        return false;
      }
    }

    @Override
    public String toString()
    {
      return "(" + vx + ", " + vy + ", " + vz + ", " + vw + ")";
    }

    @Override
    public Object clone()
    {
      return new Vec4(vx, vy, vz, vw);
    }

    public Vec3 getVec3() { return new Vec3(vx, vy, vz); }

    public Vec4 splatX() { return new Vec4(vx); }
    public Vec4 splatY() { return new Vec4(vy); }
    public Vec4 splatZ() { return new Vec4(vz); }
    public Vec4 splatW() { return new Vec4(vw); }

    public float x() { return vx; }
    public float y() { return vy; }
    public float z() { return vz; }
    public float w() { return vw; }

    public Vec4 add(Vec4 v)
    {
      vx += v.vx;
      vy += v.vy;
      vz += v.vz;
      vw += v.vw;
      return this;
    }

    public Vec4 sub(Vec4 v)
    {
      vx -= v.vx;
      vy -= v.vy;
      vz -= v.vz;
      vw -= v.vw;
      return this;
    }

    public Vec4 mul(Vec4 v)
    {
      vx *= v.vx;
      vy *= v.vy;
      vz *= v.vz;
      vw *= v.vw;
      return this;
    }

    // Converts NaN values into a defined number
    private Vec4 fixNaN()
    {
      if (Float.isNaN(vx)) vx = 0.0f;
      if (Float.isNaN(vy)) vy = 0.0f;
      if (Float.isNaN(vz)) vz = 0.0f;
      if (Float.isNaN(vw)) vw = 0.0f;
      return this;
    }
  }


  public static class Sym3x3
  {
    private static final float FLT_EPSILON = Float.intBitsToFloat(0x34000000); // = 1.1920929e-7

    private final float[] m;

    public static Sym3x3 computeWeightedCovariance(int count, Vec3[] points, float[] weights)
    {
      // computing the centroid
      float total = 0.0f;
      Vec3 centroid = new Vec3();
      for (int i = 0; i < count; i++) {
        total += weights[i];
        centroid.add(Vec3.mul(points[i], weights[i]));
      }
      centroid.div(total);

      // accumulating the covariance matrix
      Sym3x3 covariance = new Sym3x3(0.0f);
      for (int i = 0; i < count; i++) {
        Vec3 a = Vec3.sub(points[i], centroid);
        Vec3 b = Vec3.mul(a, weights[i]);

        covariance.m[0] += a.x()*b.x();
        covariance.m[1] += a.x()*b.y();
        covariance.m[2] += a.x()*b.z();
        covariance.m[3] += a.y()*b.y();
        covariance.m[4] += a.y()*b.z();
        covariance.m[5] += a.z()*b.z();
      }

      return covariance;
    }

    public static Vec3 computePrincipleComponent(Sym3x3 matrix)
    {
      // computing the cubic coefficients
      float c0 = matrix.m[0] * matrix.m[3] * matrix.m[5] +
                 2.0f * matrix.m[1] * matrix.m[2] * matrix.m[4] -
                 matrix.m[0] * matrix.m[4] * matrix.m[4] -
                 matrix.m[3] * matrix.m[2] * matrix.m[2] -
                 matrix.m[5] * matrix.m[1] * matrix.m[1];
      float c1 = matrix.m[0] * matrix.m[3] +
                 matrix.m[0] * matrix.m[5] +
                 matrix.m[3] * matrix.m[5] -
                 matrix.m[1] * matrix.m[1] -
                 matrix.m[2] * matrix.m[2] -
                 matrix.m[4] * matrix.m[4];
      float c2 = matrix.m[0] + matrix.m[3] + matrix.m[5];

      // computing the quadratic coefficients
      float a = c1 - (1.0f/3.0f)*c2*c2;
      float b = (-2.0f/27.0f)*c2*c2*c2 + (1.0f/3.0f)*c1*c2 - c0;

      // computing the root count check
      float Q = 0.25f*b*b + (1.0f/27.0f)*a*a*a;

      // testing the multiplicity
      if (FLT_EPSILON < Q) {
        // only one root, which implies we have a multiple of the identity
        return new Vec3(1.0f);
      } else if (Q < -FLT_EPSILON) {
        // three distinct roots
        double theta = Math.atan2(Math.sqrt(-Q), -0.5*b);
        double rho = Math.sqrt(0.25*b*b - Q);

        float rt = (float)Math.pow(rho, 1.0/3.0);
        float ct = (float)Math.cos(theta/3.0);
        float st = (float)Math.sin(theta/3.0);

        float l1 = (1.0f / 3.0f)*c2 + 2.0f*rt*ct;
        float l2 = (1.0f / 3.0f)*c2 - rt*(ct + (float)Math.sqrt(3.0)*st);
        float l3 = (1.0f / 3.0f)*c2 - rt*(ct - (float)Math.sqrt(3.0)*st);

        // pick the larger
        if (Math.abs(l2) > Math.abs(l1))
          l1 = l2;
        if (Math.abs(l3) > Math.abs(l1))
          l1 = l3;

        // getting the eigenvector
        return getMultiplicity1Evector(matrix, l1);
      } else {    // if (-FLT_EPSILON <= Q && Q <= FLT_EPSILON)
        // two roots
        float rt = (float)((b < 0.0f) ? -Math.pow(-0.5*b, 1.0/3.0) : Math.pow(0.5*b, 1.0/3.0));

        float l1 = (1.0f/3.0f)*c2 + rt;   // repeated
        float l2 = (1.0f/3.0f)*c2 - 2.0f*rt;

        // getting the eigenvector
        if (Math.abs(l1) > Math.abs(l2)) {
          return getMultiplicity2Evector(matrix, l1);
        } else {
          return getMultiplicity1Evector(matrix, l2);
        }
      }
    }

    private static Vec3 getMultiplicity1Evector(Sym3x3 matrix, float evalue)
    {
      if (matrix == null)
        throw new NullPointerException();

      // computing M
      Sym3x3 m = new Sym3x3();
      m.m[0] = matrix.m[0] - evalue;
      m.m[1] = matrix.m[1];
      m.m[2] = matrix.m[2];
      m.m[3] = matrix.m[3] - evalue;
      m.m[4] = matrix.m[4];
      m.m[5] = matrix.m[5] - evalue;

      // computing U
      Sym3x3 u = new Sym3x3();
      u.m[0] = m.m[3]*m.m[5] - m.m[4]*m.m[4];
      u.m[1] = m.m[2]*m.m[4] - m.m[1]*m.m[5];
      u.m[2] = m.m[1]*m.m[4] - m.m[2]*m.m[3];
      u.m[3] = m.m[0]*m.m[5] - m.m[2]*m.m[2];
      u.m[4] = m.m[1]*m.m[2] - m.m[4]*m.m[0];
      u.m[5] = m.m[0]*m.m[3] - m.m[1]*m.m[1];

      // finding the largest component
      float mc = Math.abs(u.m[0]);
      int mi = 0;
      for (int i = 1; i < 6; i++) {
        float c = Math.abs(u.m[i]);
        if (c > mc) {
          mc = c;
          mi = i;
        }
      }

      // picking the column with this component
      switch (mi) {
        case 0:
          return new Vec3(u.m[0], u.m[1], u.m[2]);
        case 1:
        case 3:
          return new Vec3(u.m[1], u.m[3], u.m[4]);
        default:
          return new Vec3(u.m[2], u.m[4], u.m[5]);
      }
    }

    private static Vec3 getMultiplicity2Evector(Sym3x3 matrix, float evalue)
    {
      if (matrix == null)
        throw new NullPointerException();

      // computing M
      Sym3x3 m = new Sym3x3();
      m.m[0] = matrix.m[0] - evalue;
      m.m[1] = matrix.m[1];
      m.m[2] = matrix.m[2];
      m.m[3] = matrix.m[3] - evalue;
      m.m[4] = matrix.m[4];
      m.m[5] = matrix.m[5] - evalue;

      // finding the largest component
      float mc = Math.abs(m.m[0]);
      int mi = 0;
      for (int i = 1; i < 6; i++) {
        float c = Math.abs(m.m[i]);
        if (c > mc) {
          mc = c;
          mi = i;
        }
      }

      // picking the first eigenvector based on this index
      switch (mi) {
        case 0:
        case 1:
          return new Vec3(-m.m[1], m.m[0], 0.0f);
        case 2:
          return new Vec3(m.m[2], 0.0f, -m.m[0]);
        case 3:
        case 4:
          return new Vec3(0.0f, -m.m[4], m.m[3]);
        default:
          return new Vec3(0.0f, -m.m[5], m.m[4]);
      }
    }

    public Sym3x3()
    {
      m = new float[6];
      for (int i = 0; i < m.length; i++) {
        m[i] = 0.0f;
      }
    }

    public Sym3x3(float s)
    {
      m = new float[6];
      for (int i = 0; i < m.length; i++) {
        m[i] = s;
      }
    }

    @Override
    public String toString()
    {
      return String.format("(%1$f, %2$f, %3$f, %4$f, %5$f, %6$f)", m[0], m[1], m[2], m[3], m[4], m[5]);
    }

    @Override
    public Object clone()
    {
      Sym3x3 s = new Sym3x3();
      s.m[0] = m[0]; s.m[1] = m[1]; s.m[2] = m[2]; s.m[3] = m[3]; s.m[4] = m[4]; s.m[5] = m[5];
      return s;
    }

    public float get(int idx)
    {
      return m[idx];
    }

    public Sym3x3 set(int idx, float s)
    {
      m[idx] = s;
      return this;
    }
  }

  private static final class Misc
  {
    public static int floatToInt(float a, int limit)
    {
      // use ANSI round-to-zero behavior to get round-to-nearest
      int i = (int)(a + 0.5f);

      // clamp to the limit
      if (i < 0)
        i = 0;
      else if (i > limit)
        i = limit;

      return i;
    }
  }
}
