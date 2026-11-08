/*
 * HomeEnvironmentOzoneTest.java
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.eteks.sweethome3d.junit;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.eteks.sweethome3d.model.HomeEnvironment;

import junit.framework.TestCase;

/**
 * Tests the amount of ozone of {@link HomeEnvironment}.
 */
public class HomeEnvironmentOzoneTest extends TestCase {
  public void testDefaultAndClamp() {
    HomeEnvironment environment = new HomeEnvironment();
    assertEquals(0.35f, environment.getOzone(), 0);
    environment.setOzone(0);
    assertEquals(HomeEnvironment.MIN_OZONE, environment.getOzone(), 0);
    environment.setOzone(100);
    assertEquals(HomeEnvironment.MAX_OZONE, environment.getOzone(), 0);
    environment.setOzone(0.25f);
    assertEquals(0.25f, environment.getOzone(), 0);
  }

  public void testPropertyChange() {
    HomeEnvironment environment = new HomeEnvironment();
    final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
    environment.addPropertyChangeListener(HomeEnvironment.Property.OZONE, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          events.add(ev);
        }
      });
    environment.setOzone(0.4f);
    environment.setOzone(0.4f);
    assertEquals(1, events.size());
    assertEquals(0.35f, (Float)events.get(0).getOldValue(), 0);
    assertEquals(0.4f, (Float)events.get(0).getNewValue(), 0);
  }

  public void testSkyTransparent() {
    HomeEnvironment environment = new HomeEnvironment();
    assertFalse(environment.isPhotoSkyTransparent());
    final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
    environment.addPropertyChangeListener(HomeEnvironment.Property.PHOTO_SKY_TRANSPARENT, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          events.add(ev);
        }
      });
    environment.setPhotoSkyTransparent(true);
    environment.setPhotoSkyTransparent(true);
    assertTrue(environment.isPhotoSkyTransparent());
    assertEquals(1, events.size());
  }

  public void testSkyThroughGlassTransparent() {
    HomeEnvironment environment = new HomeEnvironment();
    assertFalse(environment.isPhotoSkyThroughGlassTransparent());
    environment.setPhotoSkyThroughGlassTransparent(true);
    assertTrue(environment.isPhotoSkyThroughGlassTransparent());
  }

  public void testSerialization() throws Exception {
    HomeEnvironment environment = new HomeEnvironment();
    environment.setOzone(0.45f);
    environment.setPhotoSkyTransparent(true);
    environment.setPhotoSkyThroughGlassTransparent(true);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    out.writeObject(environment);
    out.close();
    HomeEnvironment read = (HomeEnvironment)new ObjectInputStream(
        new ByteArrayInputStream(bytes.toByteArray())).readObject();
    assertEquals(0.45f, read.getOzone(), 0);
    assertTrue(read.isPhotoSkyTransparent());
    assertTrue(read.isPhotoSkyThroughGlassTransparent());
  }

  public void testExposureAndSunPower() {
    HomeEnvironment environment = new HomeEnvironment();
    assertEquals(0f, environment.getPhotoExposure(), 0);
    assertEquals(0f, environment.getPhotoSunPower(), 0);
    environment.setPhotoExposure(100);
    assertEquals(HomeEnvironment.MAX_EXPOSURE, environment.getPhotoExposure(), 0);
    environment.setPhotoExposure(-100);
    assertEquals(HomeEnvironment.MIN_EXPOSURE, environment.getPhotoExposure(), 0);
    environment.setPhotoSunPower(100);
    assertEquals(HomeEnvironment.MAX_SUN_POWER, environment.getPhotoSunPower(), 0);
    environment.setPhotoSunPower(-100);
    assertEquals(HomeEnvironment.MIN_SUN_POWER, environment.getPhotoSunPower(), 0);
  }

  public void testSkyLightingInterior() {
    HomeEnvironment environment = new HomeEnvironment();
    assertFalse(environment.isPhotoSkyLightingInterior());
    final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
    environment.addPropertyChangeListener(HomeEnvironment.Property.PHOTO_SKY_LIGHTING_INTERIOR, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          events.add(ev);
        }
      });
    environment.setPhotoSkyLightingInterior(true);
    environment.setPhotoSkyLightingInterior(true);
    assertTrue(environment.isPhotoSkyLightingInterior());
    assertEquals(1, events.size());
  }
}
