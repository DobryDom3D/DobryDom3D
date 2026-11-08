/*
 * HomeFileExtensions.java
 *
 * This file is part of DobryDom3D, a fork of Sweet Home 3D 7.5, modified by DobryDom3D contributors since August 2026.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.viewcontroller;

import java.util.Locale;

/**
 * The file extensions of home files.
 * DobryDom3D reads <code>.dd3d</code>, <code>.dd3x</code> and the legacy Sweet Home 3D
 * <code>.sh3d</code> and <code>.sh3x</code> files, but only ever writes <code>.dd3d</code>
 * and <code>.dd3x</code>, so that files created by Sweet Home 3D stay untouched.
 */
public final class HomeFileExtensions {
  /** Extension of the home files written by DobryDom3D. */
  public static final String DEFAULT    = ".dd3d";
  /** Extension of the compressed home files written by DobryDom3D. */
  public static final String COMPRESSED = ".dd3x";
  /** Extension of the home files written by Sweet Home 3D. */
  public static final String LEGACY_DEFAULT    = ".sh3d";
  /** Extension of the compressed home files written by Sweet Home 3D. */
  public static final String LEGACY_COMPRESSED = ".sh3x";

  /** All the readable extensions, the first one being the default extension of saved homes. */
  private static final String [] ALL = {DEFAULT, COMPRESSED, LEGACY_DEFAULT, LEGACY_COMPRESSED};

  private HomeFileExtensions() {
    // This class contains only static methods
  }

  /**
   * Returns the extensions of the home files that can be opened, the default one first.
   */
  public static String [] getReadableExtensions() {
    return ALL.clone();
  }

  /**
   * Returns <code>true</code> if <code>path</code> has one of the readable home extensions.
   */
  public static boolean isHomeFile(String path) {
    return path != null
        && (hasExtension(path, DEFAULT) || hasExtension(path, COMPRESSED) || isLegacy(path));
  }

  /**
   * Returns <code>true</code> if <code>path</code> is a file written by Sweet Home 3D,
   * that DobryDom3D must not overwrite.
   */
  public static boolean isLegacy(String path) {
    return path != null
        && (hasExtension(path, LEGACY_DEFAULT) || hasExtension(path, LEGACY_COMPRESSED));
  }

  /**
   * Returns <code>path</code> with its legacy extension replaced by the matching DobryDom3D
   * extension (<code>.sh3d</code> becomes <code>.dd3d</code>, <code>.sh3x</code> becomes
   * <code>.dd3x</code>). Other paths are returned unchanged.
   */
  public static String toDobryDom3D(String path) {
    if (hasExtension(path, LEGACY_DEFAULT)) {
      return replaceExtension(path, LEGACY_DEFAULT, DEFAULT);
    } else if (hasExtension(path, LEGACY_COMPRESSED)) {
      return replaceExtension(path, LEGACY_COMPRESSED, COMPRESSED);
    }
    return path;
  }

  private static boolean hasExtension(String path, String extension) {
    return path != null && path.toLowerCase(Locale.ROOT).endsWith(extension);
  }

  private static String replaceExtension(String path, String from, String to) {
    return path.substring(0, path.length() - from.length()) + to;
  }
}
