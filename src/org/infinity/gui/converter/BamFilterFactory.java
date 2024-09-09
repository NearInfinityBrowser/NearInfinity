// Near Infinity - An Infinity Engine Browser and Editor
// Copyright (C) 2001 Jon Olav Hauglid
// See LICENSE.txt for license information

package org.infinity.gui.converter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.infinity.util.Logger;

public class BamFilterFactory {
  private static final List<FilterInfo> FILTER_INFO_LIST = new ArrayList<>();

  static {
    // Registering individual BAM filters
    FILTER_INFO_LIST.add(
        new FilterInfo(BamFilterColorBCG.getFilterName(), BamFilterColorBCG.getFilterDesc(), BamFilterColorBCG.class));
    FILTER_INFO_LIST.add(
        new FilterInfo(BamFilterColorHSL.getFilterName(), BamFilterColorHSL.getFilterDesc(), BamFilterColorHSL.class));
    FILTER_INFO_LIST.add(
        new FilterInfo(BamFilterColorLab.getFilterName(), BamFilterColorLab.getFilterDesc(), BamFilterColorLab.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterColorBalance.getFilterName(), BamFilterColorBalance.getFilterDesc(),
        BamFilterColorBalance.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterColorReplace.getFilterName(), BamFilterColorReplace.getFilterDesc(),
        BamFilterColorReplace.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterColorSwap.getFilterName(), BamFilterColorSwap.getFilterDesc(),
        BamFilterColorSwap.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterColorInvert.getFilterName(), BamFilterColorInvert.getFilterDesc(),
        BamFilterColorInvert.class));

    FILTER_INFO_LIST.add(new FilterInfo(BamFilterTransformResize.getFilterName(),
        BamFilterTransformResize.getFilterDesc(), BamFilterTransformResize.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterTransformRotate.getFilterName(),
        BamFilterTransformRotate.getFilterDesc(), BamFilterTransformRotate.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterTransformMirror.getFilterName(),
        BamFilterTransformMirror.getFilterDesc(), BamFilterTransformMirror.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterTransformTrim.getFilterName(), BamFilterTransformTrim.getFilterDesc(),
        BamFilterTransformTrim.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterTransformCenter.getFilterName(),
        BamFilterTransformCenter.getFilterDesc(), BamFilterTransformCenter.class));

    FILTER_INFO_LIST.add(new FilterInfo(BamFilterOutputDefault.getFilterName(), BamFilterOutputDefault.getFilterDesc(),
        BamFilterOutputDefault.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterOutputCombine.getFilterName(), BamFilterOutputCombine.getFilterDesc(),
        BamFilterOutputCombine.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterOutputSplitted.getFilterName(), BamFilterOutputSplitted.getFilterDesc(),
        BamFilterOutputSplitted.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterOutputImage.getFilterName(), BamFilterOutputImage.getFilterDesc(),
        BamFilterOutputImage.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterOutputGif.getFilterName(), BamFilterOutputGif.getFilterDesc(),
        BamFilterOutputGif.class));
    FILTER_INFO_LIST.add(new FilterInfo(BamFilterOutputOverlay.getFilterName(), BamFilterOutputOverlay.getFilterDesc(),
        BamFilterOutputOverlay.class));

    FILTER_INFO_LIST.sort((a, b) -> a.getName().compareTo(b.getName()));
  }

  /** Returns the number of registered BAM filters. */
  public static int getFilterInfoSize() {
    return FILTER_INFO_LIST.size();
  }

  /** Returns a FilterInfo object at the specified list index. */
  public static FilterInfo getFilterInfo(int index) {
    if (index >= 0 && index < FILTER_INFO_LIST.size()) {
      return FILTER_INFO_LIST.get(index);
    } else {
      return null;
    }
  }

  /** Returns a FilterInfo object of the specified name. */
  public static FilterInfo getFilterInfo(String filterName) {
    if (filterName != null && !filterName.isEmpty()) {
      for (final FilterInfo fi : FILTER_INFO_LIST) {
        if (fi.getName().equalsIgnoreCase(filterName)) {
          return fi;
        }
      }
    }
    return null;
  }

  /** Returns a list of all class types compatible with the specified class. */
  public static Collection<Class<? extends BamFilterBase>> getFiltersOf(Class<? extends BamFilterBase> classType) {
    Collection<Class<? extends BamFilterBase>> retVal = new ArrayList<>();
    if (classType != null) {
      for (FilterInfo element : FILTER_INFO_LIST) {
        if (classType.isAssignableFrom(element.getFilterClass())) {
          retVal.add(element.getFilterClass());
        }
      }
    }
    return retVal;
  }

  /**
   * Creates an instance of the specified class type.
   *
   * @param filterClass The class type to create an instance from.
   * @return The filter class instance, or {@code null} on error.
   */
  public static BamFilterBase createInstance(ConvertToBam parent, Class<? extends BamFilterBase> filterClass) {
    if (filterClass != null) {
      try {
        return filterClass.getConstructor(ConvertToBam.class).newInstance(parent);
      } catch (Throwable t) {
        Logger.error(t);
      }
    }
    return null;
  }

  /**
   * Creates a normalized filter list based on the specified argument. It moves existing output filter to the end of the
   * list. The filter {@code BamFilterOutputDefault} will be added to the end of the list if no output filter has been
   * found.
   */
  public static List<BamFilterBase> normalizeFilterList(ConvertToBam parent, List<BamFilterBase> filterList) {
    List<BamFilterBase> retList = new ArrayList<>();
    List<BamFilterBase> tmpList = new ArrayList<>();
    if (filterList != null) {
      for (BamFilterBase bamFilterBase : filterList) {
        if (bamFilterBase instanceof BamFilterBaseOutput) {
          tmpList.add(bamFilterBase);
        } else {
          retList.add(bamFilterBase);
        }
      }
    }

    // adding output filters last
    if (!tmpList.isEmpty()) {
      retList.addAll(tmpList);
    } else {
      retList.add(BamFilterFactory.createInstance(parent, BamFilterOutputDefault.class));
    }

    return retList;
  }

  // -------------------------- INNER CLASSES --------------------------

  public static final class FilterInfo implements Comparable<FilterInfo> {
    private final String name;
    private final String description;
    private final Class<? extends BamFilterBase> filterClass;

    public FilterInfo(String name, String desc, Class<? extends BamFilterBase> filterClass) {
      this.name = name;
      this.description = desc;
      this.filterClass = filterClass;
    }

    /** Returns the filter name. */
    public String getName() {
      return name;
    }

    /** Returns the filter description. */
    public String getDescription() {
      return description;
    }

    /** Returns the filter class type. */
    public Class<? extends BamFilterBase> getFilterClass() {
      return filterClass;
    }

    @Override
    public String toString() {
      String prefix;
      if (BamFilterBaseColor.class.isAssignableFrom(filterClass)) {
        prefix = "Color";
      } else if (BamFilterBaseTransform.class.isAssignableFrom(filterClass)) {
        prefix = "Transform";
      } else if (BamFilterBaseOutput.class.isAssignableFrom(filterClass)) {
        prefix = "Output";
      } else {
        prefix = "Filter";
      }
      return prefix + ": " + name;
    }

    @Override
    public int compareTo(FilterInfo o) {
      // Sorts by Color->Transform->Output
      if (BamFilterBaseColor.class.isAssignableFrom(filterClass)) {
        if (BamFilterBaseColor.class.isAssignableFrom(o.filterClass)) {
          return 0;
        } else {
          return -1;
        }
      } else if (BamFilterBaseTransform.class.isAssignableFrom(filterClass)) {
        if (BamFilterBaseColor.class.isAssignableFrom(o.filterClass)) {
          return 1;
        } else if (BamFilterBaseTransform.class.isAssignableFrom(o.filterClass)) {
          return 0;
        } else {
          return -1;
        }
      } else {
        if (BamFilterBaseOutput.class.isAssignableFrom(o.filterClass)) {
          return 0;
        } else {
          return 1;
        }
      }
    }
  }
}
