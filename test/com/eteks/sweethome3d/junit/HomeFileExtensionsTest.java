/*
 * HomeFileExtensionsTest.java
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
package com.eteks.sweethome3d.junit;

import java.io.File;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.viewcontroller.ContentManager.ContentType;
import com.eteks.sweethome3d.viewcontroller.HomeFileExtensions;

/**
 * Tests the home file extensions DobryDom3D reads and writes.
 */
public class HomeFileExtensionsTest extends TestCase {
  public void testReadableExtensionsStartWithDefault() {
    String [] extensions = HomeFileExtensions.getReadableExtensions();
    assertEquals(".dd3d", extensions [0]);
    assertEquals(4, extensions.length);
  }

  public void testIsHomeFileIgnoresCase() {
    assertTrue(HomeFileExtensions.isHomeFile("a/B.DD3D"));
    assertTrue(HomeFileExtensions.isHomeFile("b.dd3x"));
    assertTrue(HomeFileExtensions.isHomeFile("c.Sh3d"));
    assertTrue(HomeFileExtensions.isHomeFile("d.sh3x"));
    assertFalse(HomeFileExtensions.isHomeFile("e.sh3f"));
    assertFalse(HomeFileExtensions.isHomeFile("f.dd3d.bak"));
    assertFalse(HomeFileExtensions.isHomeFile(null));
  }

  public void testToDobryDom3DOnlyChangesLegacyPaths() {
    assertEquals("/h/foo.dd3d", HomeFileExtensions.toDobryDom3D("/h/foo.sh3d"));
    assertEquals("/h/foo.dd3x", HomeFileExtensions.toDobryDom3D("/h/foo.SH3X"));
    assertEquals("/h/foo.dd3d", HomeFileExtensions.toDobryDom3D("/h/foo.dd3d"));
    assertEquals("/h/foo.txt", HomeFileExtensions.toDobryDom3D("/h/foo.txt"));
    assertTrue(HomeFileExtensions.isLegacy("/h/foo.sh3d"));
    assertFalse(HomeFileExtensions.isLegacy("/h/foo.dd3d"));
  }

  public void testFileFilterAcceptsAllHomeExtensions() {
    FileContentManager manager = new FileContentManager(new DefaultUserPreferences());
    for (String name : new String [] {"a.dd3d", "a.dd3x", "a.sh3d", "a.sh3x"}) {
      assertTrue(name, manager.isAcceptable(new File(name).getPath(), ContentType.SWEET_HOME_3D));
    }
    assertFalse(manager.isAcceptable("a.sh3f", ContentType.SWEET_HOME_3D));
    assertEquals(".dd3d", manager.getDefaultFileExtension(ContentType.SWEET_HOME_3D));
  }
}
