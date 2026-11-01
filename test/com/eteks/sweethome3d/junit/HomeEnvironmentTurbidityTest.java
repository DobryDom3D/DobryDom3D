/*
 * HomeEnvironmentTurbidityTest.java
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
 * Tests the sky turbidity of {@link HomeEnvironment}.
 */
public class HomeEnvironmentTurbidityTest extends TestCase {
  public void testDefaultAndClamp() {
    HomeEnvironment environment = new HomeEnvironment();
    assertEquals(6f, environment.getTurbidity(), 0);
    environment.setTurbidity(0);
    assertEquals(HomeEnvironment.MIN_TURBIDITY, environment.getTurbidity(), 0);
    environment.setTurbidity(100);
    assertEquals(HomeEnvironment.MAX_TURBIDITY, environment.getTurbidity(), 0);
    environment.setTurbidity(3.5f);
    assertEquals(3.5f, environment.getTurbidity(), 0);
  }

  public void testPropertyChange() {
    HomeEnvironment environment = new HomeEnvironment();
    final List<PropertyChangeEvent> events = new ArrayList<PropertyChangeEvent>();
    environment.addPropertyChangeListener(HomeEnvironment.Property.TURBIDITY, new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent ev) {
          events.add(ev);
        }
      });
    environment.setTurbidity(4);
    environment.setTurbidity(4);
    assertEquals(1, events.size());
    assertEquals(6f, (Float)events.get(0).getOldValue(), 0);
    assertEquals(4f, (Float)events.get(0).getNewValue(), 0);
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
    environment.setTurbidity(2.5f);
    environment.setPhotoSkyTransparent(true);
    environment.setPhotoSkyThroughGlassTransparent(true);
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    out.writeObject(environment);
    out.close();
    HomeEnvironment read = (HomeEnvironment)new ObjectInputStream(
        new ByteArrayInputStream(bytes.toByteArray())).readObject();
    assertEquals(2.5f, read.getTurbidity(), 0);
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
