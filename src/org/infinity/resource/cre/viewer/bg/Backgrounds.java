// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 - 2021 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.resource.cre.viewer.bg;

import java.awt.Color;
import java.awt.Image;
import java.awt.Point;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.UIManager;

import org.infinity.resource.Profile;

/**
 * Backgrounds for the creature animation browser.
 */
public final class Backgrounds
{
  public static final BackgroundInfo BG_WILDERNESS    = new BackgroundInfo("Sword Coast wilderness tileset", new Color(0x282f12),
                                                                           "bg_wilderness.jpg", new Point(640, 632),
                                                                           EnumSet.complementOf(EnumSet.of(Profile.Game.PST, Profile.Game.PSTEE)));
  public static final BackgroundInfo BG_CAVE          = new BackgroundInfo("Cave tileset", new Color(0x010101),
                                                                           "bg_cave.jpg", new Point(640, 554),
                                                                           EnumSet.complementOf(EnumSet.of(Profile.Game.PST, Profile.Game.PSTEE)));
  public static final BackgroundInfo BG_CITY_PST      = new BackgroundInfo("Sigil city tileset", new Color(0x494131),
                                                                           "pst_city.jpg", new Point(640, 580),
                                                                           EnumSet.of(Profile.Game.PST, Profile.Game.PSTEE));
  public static final BackgroundInfo BG_COLOR_NONE        = new BackgroundInfo("System color", null);
  public static final BackgroundInfo BG_COLOR_WHITE       = new BackgroundInfo("White color", Color.WHITE);
  public static final BackgroundInfo BG_COLOR_BLACK       = new BackgroundInfo("Black color", Color.BLACK);
  public static final BackgroundInfo BG_COLOR_LIGHT_GRAY  = new BackgroundInfo("Light gray color", Color.LIGHT_GRAY);
  public static final BackgroundInfo BG_COLOR_GRAY        = new BackgroundInfo("Gray color", Color.GRAY);
  public static final BackgroundInfo BG_COLOR_RED         = new BackgroundInfo("Red color", Color.RED);
  public static final BackgroundInfo BG_COLOR_GREEN       = new BackgroundInfo("Green color", Color.GREEN);
  public static final BackgroundInfo BG_COLOR_BLUE        = new BackgroundInfo("Blue color", Color.BLUE);
  public static final BackgroundInfo BG_COLOR_YELLOW      = new BackgroundInfo("Yellow color", Color.YELLOW);
  public static final BackgroundInfo BG_COLOR_MAGENTA     = new BackgroundInfo("Magenta color", Color.MAGENTA);
  public static final BackgroundInfo BG_COLOR_CYAN        = new BackgroundInfo("Cyan color", Color.CYAN);

  public static final List<BackgroundInfo> BackgroundList = new ArrayList<BackgroundInfo>() {{
    add(BG_COLOR_NONE);     add(BG_WILDERNESS);   add(BG_CAVE);             add(BG_CITY_PST);
    add(BG_COLOR_WHITE);    add(BG_COLOR_BLACK);  add(BG_COLOR_LIGHT_GRAY); add(BG_COLOR_GRAY);
    add(BG_COLOR_RED);      add(BG_COLOR_GREEN);  add(BG_COLOR_BLUE);       add(BG_COLOR_YELLOW);
    add(BG_COLOR_MAGENTA);  add(BG_COLOR_CYAN);
  }};

  /** Returns a list of background info instances available for the specified game. */
  public static List<BackgroundInfo> getBackgrounds(Profile.Game game)
  {
    return BackgroundList
        .stream()
        .filter(bi -> bi.games.contains((game != null) ? game : Profile.getGame()))
        .collect(Collectors.toList());
  }

  /**
   * Returns an {@link Image} object of the specified graphics filename.
   * @param fileName The graphics filename.
   * @return The {@link Image} object, or {@code null} on error.
   */
  public static Image getImage(String fileName)
  {
    return getImage(null, fileName);
  }

  /**
   * Returns an {@link Image} object of the specified graphics filename.
   * @param c A class located in the same package as the specified graphics file. The full package name
   *          of the class will be used to determine the correct path of the graphics file.
   * @param fileName The graphics filename.
   * @return The {@link Image} object, or {@code null} on error.
   */
  public static Image getImage(Class<?> c, String fileName)
  {
    Image retVal = null;
    if (fileName == null || fileName.isEmpty()) {
      return retVal;
    }

    try {
      URL url = getValidURL(c, fileName);
      retVal = ImageIO.read(url);
    } catch (Exception e) {
      e.printStackTrace();
    }
    return retVal;
  }

  // Returns a URL instance that points to the specified filename
  private static URL getValidURL(Class<?> c, String fileName)
  {
    URL retVal = null;
    if (fileName != null && !fileName.isEmpty()) {
      if (c == null) {
        retVal = ClassLoader.getSystemResource(fileName);
      }
      if (retVal == null) {
        if (c == null) {
          c = Backgrounds.class;
        }
        String basePath = c.getPackage().getName().replace('.', '/');
        String separator = (fileName.charAt(0) == '/') ? "" : "/";
        retVal = ClassLoader.getSystemResource(basePath + separator + fileName);
      }
    }
    return retVal;
  }

  private Backgrounds() {}

//-------------------------- INNER CLASSES --------------------------

  public static class BackgroundInfo
  {
    private final EnumSet<Profile.Game> games;
    private final String label;
    private final String imageName;
    private final Color color;
    private final Point center;

    public BackgroundInfo(String label, Color bgColor)
    {
      this(label, bgColor, null, null, null);
    }

    public BackgroundInfo(String label, Color bgColor, String imageName, Point center, EnumSet<Profile.Game> games)
    {
      this.label = Objects.requireNonNull(label, "Description label required");
      this.color = bgColor;
      this.imageName = imageName;
      this.center = center;
      this.games = (games != null) ? games : EnumSet.allOf(Profile.Game.class);
    }

    /** Returns the set of games which are compatible with this {@code BackgroundInfo}. */
    public EnumSet<Profile.Game> getGames() { return games; }

    /** Returns the descriptive label of the background entry. */
    public String getLabel() { return label; }

    /** Returns the background image filename. Returns {@code null} if background image has not been defined. */
    public String getImageName() { return imageName; }

    /** Returns the background image. Returns {@code null} if no background image is defined. */
    public Image getImage() { return hasImage() ? Backgrounds.getImage(imageName) : null; }

    /** Returns the background color. */
    public Color getColor() {
      Color retVal = (color != null) ? color : UIManager.getColor("Panel.background");
      if (retVal == null) {
        retVal = Color.LIGHT_GRAY;
      }
      return retVal;
    }

    /** Returns the center point of the associated image. Returns {@code null} if center has not been defined. */
    public Point getCenter() { return center; }

    /** Returns whether the background is available for the current game. */
    public boolean isAvailable() { return (games == null) || games.contains(Profile.getGame()); }

    /** Returns whether the background provides an image. */
    public boolean hasImage() { return imageName != null; }

    @Override
    public String toString() { return label; }
  }
}
