/*
 * HomeSaveAsTest.java
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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

import com.eteks.sweethome3d.io.DefaultUserPreferences;
import com.eteks.sweethome3d.model.Home;
import com.eteks.sweethome3d.model.HomeApplication;
import com.eteks.sweethome3d.model.HomeRecorder;
import com.eteks.sweethome3d.model.RecorderException;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.swing.FileContentManager;
import com.eteks.sweethome3d.swing.SwingViewFactory;
import com.eteks.sweethome3d.viewcontroller.HomeController;
import com.eteks.sweethome3d.viewcontroller.HomeView;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskController;
import com.eteks.sweethome3d.viewcontroller.ThreadedTaskView;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * Tests that a home opened from a Sweet Home 3D file is never saved back over that file.
 */
public class HomeSaveAsTest extends TestCase {
  private final UserPreferences preferences = new DefaultUserPreferences();
  private final List<String>    dialogSuggestions = new ArrayList<String>();
  private final List<String>    writtenNames = new ArrayList<String>();
  private CountDownLatch        written;
  private String                chosenName;

  private HomeController createController(Home home) {
    final HomeRecorder recorder = new HomeRecorder() {
        public void writeHome(Home home, String name) throws RecorderException {
          writtenNames.add(name);
          written.countDown();
        }

        public Home readHome(String name) throws RecorderException {
          throw new RecorderException("Not supported");
        }

        public boolean exists(String name) throws RecorderException {
          return false;
        }
      };
    HomeApplication application = new HomeApplication() {
        @Override
        public UserPreferences getUserPreferences() {
          return preferences;
        }

        @Override
        public HomeRecorder getHomeRecorder() {
          return recorder;
        }

        @Override
        public HomeRecorder getHomeRecorder(HomeRecorder.Type type) {
          return recorder;
        }
      };
    return new HomeController(home, application, new SwingViewFactory() {
        @Override
        public ThreadedTaskView createThreadedTaskView(String taskMessage, UserPreferences preferences,
                                                       ThreadedTaskController controller) {
          return new ThreadedTaskView() {
              public void invokeLater(Runnable runnable) {
                runnable.run();
              }

              public void setTaskRunning(boolean taskRunning, View executingView) {
              }
            };
        }

        @Override
        public HomeView createHomeView(Home home, UserPreferences preferences, HomeController controller) {
          // A proxy avoids creating a Swing view, which needs a display
          return (HomeView)Proxy.newProxyInstance(getClass().getClassLoader(), new Class [] {HomeView.class},
              new InvocationHandler() {
                public Object invoke(Object proxy, Method method, Object [] args) {
                  if (method.getName().equals("showSaveDialog")) {
                    dialogSuggestions.add((String)args [0]);
                    return chosenName;
                  } else if (method.getName().equals("invokeLater")) {
                    ((Runnable)args [0]).run();
                  } else if (method.getReturnType() == boolean.class) {
                    return Boolean.FALSE;
                  } else if (method.getReturnType() == int.class) {
                    return 0;
                  }
                  return null;
                }
              });
        }
      }, new FileContentManager(this.preferences));
  }

  private void saveAndWait(Runnable save) throws InterruptedException {
    this.written = new CountDownLatch(1);
    save.run();
    assertTrue("Home wasn't written", this.written.await(5, TimeUnit.SECONDS));
  }

  public void testSavingLegacyHomeAsksForANewName() throws InterruptedException {
    Home home = new Home();
    home.setName("/homes/foo.sh3d");
    this.chosenName = "/homes/foo.dd3d";
    final HomeController controller = createController(home);
    saveAndWait(new Runnable() {
        public void run() {
          controller.save();
        }
      });
    assertEquals(1, this.dialogSuggestions.size());
    // The dialog turns the legacy name into a .dd3d suggestion
    assertEquals("/homes/foo.sh3d", this.dialogSuggestions.get(0));
    assertEquals("/homes/foo.dd3d", this.writtenNames.get(0));
  }

  public void testSavingLegacyHomeNeverWritesTheLegacyFile() {
    Home home = new Home();
    home.setName("/homes/foo.sh3d");
    this.chosenName = null; // The user cancels
    createController(home).save();
    assertEquals(1, this.dialogSuggestions.size());
    assertTrue(this.writtenNames.isEmpty());
  }

  public void testSavingNativeHomeDoesNotAsk() throws InterruptedException {
    Home home = new Home();
    home.setName("/homes/foo.dd3d");
    final HomeController controller = createController(home);
    saveAndWait(new Runnable() {
        public void run() {
          controller.save();
        }
      });
    assertTrue(this.dialogSuggestions.isEmpty());
    assertEquals("/homes/foo.dd3d", this.writtenNames.get(0));
  }

  public void testSaveAsAndCompressSuggestsCompressedExtension() {
    Home home = new Home();
    home.setName("/homes/foo.dd3d");
    this.chosenName = null;
    createController(home).saveAsAndCompress();
    assertEquals("/homes/foo.dd3x", this.dialogSuggestions.get(0));
  }
}
