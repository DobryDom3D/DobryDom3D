/*
 * ModelMaterialsComponent.java 26 oct. 2012
 *
 * Sweet Home 3D, Copyright (c) 2024 Space Mushrooms <info@sweethome3d.com>
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
package com.eteks.sweethome3d.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import javax.media.j3d.Background;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ImageComponent;
import javax.media.j3d.ImageComponent2D;
import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.vecmath.Color3f;

import com.eteks.sweethome3d.j3d.ModelManager;
import com.eteks.sweethome3d.model.HomeMaterial;
import com.eteks.sweethome3d.model.HomeTexture;
import com.eteks.sweethome3d.model.PieceOfFurniture;
import com.eteks.sweethome3d.model.UserPreferences;
import com.eteks.sweethome3d.tools.OperatingSystem;
import com.eteks.sweethome3d.viewcontroller.ModelMaterialsController;
import com.eteks.sweethome3d.viewcontroller.TextureChoiceController;
import com.eteks.sweethome3d.viewcontroller.View;

/**
 * Button giving access to materials editor. When the user clicks
 * on this button a dialog appears to let him choose materials.
 */
public class ModelMaterialsComponent extends JButton implements View {
  /**
   * Creates a texture button.
   */
  public ModelMaterialsComponent(final UserPreferences preferences,
                                 final ModelMaterialsController controller) {
    setText(SwingTools.getLocalizedLabelText(preferences, ModelMaterialsComponent.class, "modifyButton.text"));
    if (!OperatingSystem.isMacOSX()) {
      setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
          ModelMaterialsComponent.class, "modifyButton.mnemonic")).getKeyCode());
    }
    // Add a listener to update materials
    addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent ev) {
          final ModelMaterialsPanel texturePanel = new ModelMaterialsPanel(preferences, controller);
          texturePanel.displayView(ModelMaterialsComponent.this);
        }
      });
  }

  /**
   * A panel that displays available textures in a list to let user make choose one.
   */
  private static class ModelMaterialsPanel extends JPanel {
    private final ModelMaterialsController controller;

    private JLabel                 previewLabel;
    private ModelPreviewComponent  previewComponent;
    private JLabel                 materialsLabel;
    private JList                  materialsList;
    private JLabel                 colorAndTextureLabel;
    private JRadioButton           defaultColorAndTextureRadioButton;
    private JRadioButton           invisibleRadioButton;
    private JRadioButton           colorRadioButton;
    private ColorButton            colorButton;
    private JRadioButton           textureRadioButton;
    private JComponent             textureComponent;
    private JLabel                 shininessLabel;
    private JSlider                shininessSlider;
    private JSpinner                       shininessSpinner;

    private JLabel                         transparencyLabel;
    private JSlider                        transparencySlider;
    private JSpinner                       transparencySpinner;

    private JLabel                         reflectionLabel;
    private JSlider                        reflectionSlider;
    private JSpinner                       reflectionSpinner;

    private PropertyChangeListener textureChangeListener;
    private boolean                        updatingControls;

    public ModelMaterialsPanel(UserPreferences preferences,
                               ModelMaterialsController controller) {
      super(new GridBagLayout());
      this.controller = controller;
      createComponents(preferences, controller);
      setMnemonics(preferences);
      layoutComponents();
    }

    /**
     * Renderer used to display the materials in list.
     */
    private class MaterialListCellRenderer extends DefaultListCellRenderer {
      private Font              defaultFont;
      private Icon              emptyIcon        = new Icon() {
                                                   public void paintIcon(Component c, Graphics g, int x, int y) {
                                                   }

                                                   public int getIconHeight() {
                                                     return defaultFont.getSize();
                                                   }

                                                   public int getIconWidth() {
                                                     return getIconHeight();
                                                   }
                                                 };

      @Override
      public Component getListCellRendererComponent(final JList list, Object value, int index, boolean isSelected,
                                                    boolean cellHasFocus) {
        // Initialize fonts if not done
        if (this.defaultFont == null) {
          this.defaultFont = getFont();
        }
        HomeMaterial material = (HomeMaterial)value;
        super.getListCellRendererComponent(list, material.getName(), index, isSelected, cellHasFocus);
        HomeTexture materialTexture = material.getTexture();
        Integer materialColor = material.getColor();
        HomeMaterial defaultMaterial = ((MaterialsListModel)materialsList.getModel()).getDefaultMaterialAt(index);
        if (materialTexture == null && materialColor == null) {
          materialTexture = defaultMaterial.getTexture();
          if (materialTexture == null) {
            materialColor = defaultMaterial.getColor();
          }
        }
        if (materialTexture != null) {
          // Display material texture image with an icon
          final HomeTexture texture = materialTexture;
          setIcon(new Icon() {
            public int getIconHeight() {
              return defaultFont.getSize();
            }

            public int getIconWidth() {
              return getIconHeight();
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
              Icon icon = IconManager.getInstance().getIcon(texture.getImage(), getIconHeight(), list);
              if (icon.getIconWidth() != icon.getIconHeight()) {
                Graphics2D g2D = (Graphics2D)g;
                AffineTransform previousTransform = g2D.getTransform();
                g2D.translate(x, y);
                g2D.scale((float)icon.getIconHeight() / icon.getIconWidth(), 1);
                icon.paintIcon(c, g2D, 0, 0);
                g2D.setTransform(previousTransform);
              } else {
                icon.paintIcon(c, g, x, y);
              }
            }
          });
        } else if (materialColor != null && (materialColor.intValue() & 0xFF000000) != 0) {
          // Display material color with an icon
          final Color color = new Color(materialColor);
          setIcon(new Icon() {
            public int getIconHeight() {
              return getFont().getSize();
            }

            public int getIconWidth() {
              return getIconHeight();
            }

            public void paintIcon(Component c, Graphics g, int x, int y) {
              int squareSize = getIconHeight();
              g.setColor(color);
              g.fillRect(x + 2, y + 2, squareSize - 3, squareSize - 3);
              g.setColor(c.getParent().getParent().getForeground());
              g.drawRect(x + 1, y + 1, squareSize - 2, squareSize - 2);
            }
          });
        } else {
          setIcon(this.emptyIcon);
        }
        return this;
      }
    }

    /**
     * Creates and initializes components.
     */
    private void createComponents(final UserPreferences preferences,
                                  final ModelMaterialsController controller) {
      this.materialsLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "materialsLabel.text"));
      final MaterialsListModel materialsListModel = new MaterialsListModel(controller);
      this.materialsList = new JList(materialsListModel);
      this.materialsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      this.materialsList.setCellRenderer(new MaterialListCellRenderer());

      this.previewLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "previewLabel.text"));
      this.previewComponent = new ModelPreviewComponent(true);
      this.previewComponent.setFocusable(false);
      this.previewComponent.setOpaque(true);
      this.previewComponent.setBackground(new Color(0xE5E5E5));

      ModelManager.getInstance().loadModel(controller.getModel(), new ModelManager.ModelObserver() {
          public void modelUpdated(BranchGroup modelRoot) {
            final MaterialsListModel materialsListModel = (MaterialsListModel)materialsList.getModel();
            previewComponent.setModel(controller.getModel(), controller.getModelFlags(), controller.getModelRotation(),
                controller.getModelWidth(), controller.getModelDepth(), controller.getModelHeight());
            previewComponent.setModelMaterials(materialsListModel.getMaterials());
            previewComponent.setModelTransformations(controller.getModelTransformations());
            materialsListModel.addListDataListener(new ListDataListener() {
                public void contentsChanged(ListDataEvent ev) {
                  previewComponent.setModelMaterials(materialsListModel.getMaterials());
              previewComponent.repaint();
                }

                public void intervalRemoved(ListDataEvent ev) {
                }

                public void intervalAdded(ListDataEvent ev) {
                }
              });
          }

          public void modelError(Exception ex) {
            // Should happen only if model is missing
            previewLabel.setVisible(false);
            previewComponent.setVisible(false);
          }
        });
      this.previewComponent.addMouseListener(new MouseAdapter() {
          @Override
          public void mousePressed(MouseEvent ev) {
            HomeMaterial pickedMaterial = previewComponent.getPickedMaterial();
            if (pickedMaterial != null) {
              for (int i = 0, n = materialsListModel.getSize(); i < n; i++) {
                HomeMaterial material = materialsListModel.getDefaultMaterialAt(i);
                if (material.getName() != null
                    && material.getName().equals(pickedMaterial.getName())) {

                // Check if the item is already selected
                boolean isSelected = materialsList.isSelectedIndex(i);

                // If holding control/meta (multi-select gesture), toggle just this item.
                // Otherwise, clicking a picked part should select it exclusively OR keep
                // multi-selection intact depending on whether it's part of the current group.
                if (ev.isControlDown() || ev.isMetaDown()) {
                    if (isSelected) {
                      materialsList.removeSelectionInterval(i, i);
                    } else {
                      materialsList.addSelectionInterval(i, i);
                    }
                  } else {
                  // Standard click: if it's already part of a multi-selection,
                  // do not collapse the selection to just this one item.
                  // Only select it exclusively if nothing or only this was selected.
                  int[] selectedIndices = materialsList.getSelectedIndices();
                  if (selectedIndices.length <= 1 || !isSelected) {
                    materialsList.setSelectedIndex(i);
                  }
                  // If it's part of a multi-selection group, leave the group untouched
                  // so properties can still be edited in batch.
                }

                  materialsList.ensureIndexIsVisible(i);
                break; // Material found and handled, exit loop
                }
              }
            }
          }
        });

      this.colorAndTextureLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "colorAndTextureLabel.text"));

      // Create color and texture image radio buttons properties
      this.defaultColorAndTextureRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "defaultColorAndTextureRadioButton.text"));
      final ChangeListener defaultChoiceChangeListener = new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (defaultColorAndTextureRadioButton.isEnabled() && defaultColorAndTextureRadioButton.isSelected()) {int [] selectedIndices = materialsList.getSelectedIndices();
            for (int index : selectedIndices) {
                HomeMaterial material = (HomeMaterial)materialsList.getModel().getElementAt(index);
              materialsListModel.setMaterialAtInternal(
                    new HomeMaterial(material.getName(), null, null, material.getShininess(), material.getTransparency(), material.getReflection()),
                    index);
              }
            materialsListModel.fireMaterialsChanged(selectedIndices);
            updateMaterials();
            }
          }
        };
      this.defaultColorAndTextureRadioButton.addChangeListener(defaultChoiceChangeListener);

      this.invisibleRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "invisibleRadioButton.text"));
      final ChangeListener invisibleChoiceChangeListener = new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (invisibleRadioButton.isEnabled() && invisibleRadioButton.isSelected()) {int [] selectedIndices = materialsList.getSelectedIndices();
            for (int index : selectedIndices) {
                HomeMaterial material = (HomeMaterial)materialsList.getModel().getElementAt(index);
              materialsListModel.setMaterialAtInternal(
                    new HomeMaterial(material.getName(), 0, null, material.getShininess(), material.getTransparency(), material.getReflection()),
                    index);
              }
            materialsListModel.fireMaterialsChanged(selectedIndices);
            updateMaterials();
            }
          }
        };
      this.invisibleRadioButton.addChangeListener(invisibleChoiceChangeListener);

      this.colorRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "colorRadioButton.text"));
      final ChangeListener colorChoiceChangeListener = new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (colorRadioButton.isEnabled() && colorRadioButton.isSelected()) {int [] selectedIndices = materialsList.getSelectedIndices();
            Integer selectedColor = colorButton.getColor();
            for (int index : selectedIndices) {
                HomeMaterial material = (HomeMaterial)materialsList.getModel().getElementAt(index);
                HomeMaterial defaultMaterial = materialsListModel.getDefaultMaterialAt(index);
                Integer color = defaultMaterial.getColor() != selectedColor || defaultMaterial.getTexture() != null
                    ? selectedColor
                    : null;
              materialsListModel.setMaterialAtInternal(
                    new HomeMaterial(material.getName(), color, null, material.getShininess(), material.getTransparency(), material.getReflection()),
                    index);
              }
            materialsListModel.fireMaterialsChanged(selectedIndices);
            updateMaterials();
            }
          }
        };
      this.colorRadioButton.addChangeListener(colorChoiceChangeListener);
      this.colorButton = new ColorButton(preferences);
      this.colorButton.setColorDialogTitle(preferences.getLocalizedString(ModelMaterialsComponent.class, "colorDialog.title"));
      final PropertyChangeListener colorChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            if (updatingControls) {
            return;
          }
          if (!colorRadioButton.isSelected()) {
              colorRadioButton.setSelected(true);
            } else {
              colorChoiceChangeListener.stateChanged(null);
            }
          }
        };
      this.colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY, colorChangeListener);

      final TextureChoiceController textureController = controller.getTextureController();
      this.textureRadioButton = new JRadioButton(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "textureRadioButton.text"));
      final ChangeListener textureChoiceChangeListener = new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
            if (textureRadioButton.isEnabled() && textureRadioButton.isSelected()) {int [] selectedIndices = materialsList.getSelectedIndices();
            HomeTexture selectedTexture = textureController.getTexture();
            for (int index : selectedIndices) {
                HomeMaterial material = (HomeMaterial)materialsList.getModel().getElementAt(index);
                HomeTexture defaultTexture = materialsListModel.
                    getDefaultMaterialAt(index).getTexture();
                HomeTexture texture = defaultTexture != selectedTexture
                    ? selectedTexture
                    : null;
              materialsListModel.setMaterialAtInternal(
                    new HomeMaterial(material.getName(), null, texture, material.getShininess(), material.getTransparency(), material.getReflection()),
                    index);
              }
            materialsListModel.fireMaterialsChanged(selectedIndices);
            updateMaterials();
            }
          }
        };
      this.textureRadioButton.addChangeListener(textureChoiceChangeListener);
      this.textureComponent = (JComponent)textureController.getView();
      this.textureChangeListener = new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            if (updatingControls) {
            return;
          }
          if (!textureRadioButton.isSelected()) {
              textureRadioButton.setSelected(true);
            } else {
              textureChoiceChangeListener.stateChanged(null);
            }
          }
        };
      // Listener added and removed in displayView()

      ButtonGroup buttonGroup = new ButtonGroup();
      buttonGroup.add(this.defaultColorAndTextureRadioButton);
      buttonGroup.add(this.invisibleRadioButton);
      buttonGroup.add(this.colorRadioButton);
      buttonGroup.add(this.textureRadioButton);

      // --- Shininess (Defaults to 50%) ---

      this.shininessLabel = new JLabel(SwingTools.getLocalizedLabelText(preferences,
          ModelMaterialsComponent.class, "shininessLabel.text"));
      this.shininessSlider = new JSlider(0, 100, 50);
      JLabel mattLabel = new JLabel(preferences.getLocalizedString(
          ModelMaterialsComponent.class, "mattLabel.text"));
      JLabel shinyLabel = new JLabel(preferences.getLocalizedString(
          ModelMaterialsComponent.class, "shinyLabel.text"));
      Dictionary<Integer,JComponent> shininessSliderLabelTable = new Hashtable<Integer,JComponent>();
      shininessSliderLabelTable.put(0, mattLabel);
      shininessSliderLabelTable.put(100, shinyLabel);
      this.shininessSlider.setLabelTable(shininessSliderLabelTable);
      this.shininessSlider.setPaintLabels(true);
      this.shininessSlider.setPaintTicks(true);
      this.shininessSlider.setMajorTickSpacing(10);

      final SpinnerNumberModel shininessSpinnerModel = new SpinnerNumberModel(50, 0, 100, 1);
      this.shininessSpinner = new JSpinner(shininessSpinnerModel);
      ((JSpinner.DefaultEditor)this.shininessSpinner.getEditor()).getTextField().setColumns(3);
      final ChangeListener shininessChangeListener = new ChangeListener() {
          public void stateChanged(ChangeEvent ev) {
          if (shininessSlider.getValueIsAdjusting()) {
            return;
          }
          int [] selectedIndices = materialsList.getSelectedIndices();
          if (selectedIndices.length == 0) {
            return;
          }

          int shininess = shininessSlider.getValue();

            for (int index : selectedIndices) {
            HomeMaterial material = (HomeMaterial)materialsList.getModel().getElementAt(index);

            // ALWAYS apply the explicit float value
            Float newShininess = shininess / 100f;

            materialsListModel.setMaterialAtInternal(new HomeMaterial(material.getName(), material.getColor(),
                material.getTexture(), newShininess, material.getTransparency(), material.getReflection()), index);
          }

          materialsListModel.fireMaterialsChanged(selectedIndices);
          updateMaterials();
        }
      };
      this.shininessSlider.addChangeListener(shininessChangeListener);

      // Keep the slider and the spinner mirroring each other's value.
      this.shininessSlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          int sliderValue = shininessSlider.getValue();
          if (shininessSpinnerModel.getNumber().intValue() != sliderValue) {
            shininessSpinnerModel.setValue(sliderValue);
          }
        }
      });
      shininessSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          int spinnerValue = shininessSpinnerModel.getNumber().intValue();
          if (shininessSlider.getValue() != spinnerValue) {
            shininessSlider.setValue(spinnerValue);
          }
        }
      });

      // --- Transparency (Defaults to 0%) ---
      this.transparencyLabel = new JLabel(
          SwingTools.getLocalizedLabelText(preferences, ModelMaterialsComponent.class, "transparencyLabel.text"));
      this.transparencySlider = new JSlider(0, 100, 0);
      JLabel opaqueLabel = new JLabel(
          preferences.getLocalizedString(ModelMaterialsComponent.class, "opaqueLabel.text"));
      JLabel transparentLabel = new JLabel(
          preferences.getLocalizedString(ModelMaterialsComponent.class, "transparentLabel.text"));
      Dictionary<Integer, JComponent> transparencySliderLabelTable = new Hashtable<Integer, JComponent>();
      transparencySliderLabelTable.put(0, opaqueLabel);
      transparencySliderLabelTable.put(100, transparentLabel);
      this.transparencySlider.setLabelTable(transparencySliderLabelTable);
      this.transparencySlider.setPaintLabels(true);
      this.transparencySlider.setPaintTicks(true);
      this.transparencySlider.setMajorTickSpacing(10);

      final SpinnerNumberModel transparencySpinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
      this.transparencySpinner = new JSpinner(transparencySpinnerModel);
      ((JSpinner.DefaultEditor)this.transparencySpinner.getEditor()).getTextField().setColumns(3);

      final ChangeListener transparencyChangeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          if (transparencySlider.getValueIsAdjusting()) {
            return;
          }
          int [] selectedIndices = materialsList.getSelectedIndices();
          if (selectedIndices.length == 0) {
            return;
          }

          int transparency = transparencySlider.getValue();

          for (int index : selectedIndices) {
              HomeMaterial material = (HomeMaterial)materialsList.getModel().getElementAt(index);

            // ALWAYS apply the explicit float value
            Float newTransparency = transparency / 100f;

            materialsListModel.setMaterialAtInternal(new HomeMaterial(material.getName(), material.getColor(),
                material.getTexture(), material.getShininess(), newTransparency, material.getReflection()), index);
          }

          materialsListModel.fireMaterialsChanged(selectedIndices);
          updateMaterials();
        }
      };
      this.transparencySlider.addChangeListener(transparencyChangeListener);

      this.transparencySlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
              int sliderValue = transparencySlider.getValue();
          if
              (transparencySpinnerModel.getNumber().intValue() != sliderValue) {
            transparencySpinnerModel.setValue(sliderValue);
          }
        }
      });
      transparencySpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          int spinnerValue = transparencySpinnerModel.getNumber().intValue();
          if (transparencySlider.getValue() != spinnerValue) {
            transparencySlider.setValue(spinnerValue);
          }
        }
      });

      // --- Reflection (Defaults to 0%) ---
      // REMARK: Requires extended HomeMaterial with getReflection() and
      // 6-parameter constructor
      this.reflectionLabel = new JLabel("Reflection:"); // Ideally fetch from
                                                        // preferences.getLocalizedString(...)
      this.reflectionSlider = new JSlider(0, 100, 0);
      Dictionary<Integer, JComponent> reflectionSliderLabelTable = new Hashtable<Integer, JComponent>();
      reflectionSliderLabelTable.put(0, new JLabel("Matte"));
      reflectionSliderLabelTable.put(100, new JLabel("Mirror"));
      this.reflectionSlider.setLabelTable(reflectionSliderLabelTable);
      this.reflectionSlider.setPaintLabels(true);
      this.reflectionSlider.setPaintTicks(true);
      this.reflectionSlider.setMajorTickSpacing(10);

      final SpinnerNumberModel reflectionSpinnerModel = new SpinnerNumberModel(0, 0, 100, 1);
      this.reflectionSpinner = new JSpinner(reflectionSpinnerModel);
      ((JSpinner.DefaultEditor)this.reflectionSpinner.getEditor()).getTextField().setColumns(3);

      final ChangeListener reflectionChangeListener = new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          if (reflectionSlider.getValueIsAdjusting()) {
            return;
          }
          int [] selectedIndices = materialsList.getSelectedIndices();
          if (selectedIndices.length == 0) {
            return;
          }

          int reflection = reflectionSlider.getValue();

          for (int index : selectedIndices) {
            HomeMaterial material = (HomeMaterial)materialsList.getModel().getElementAt(index);

            // SAFEGUARD: Prevent pure 0.0f to avoid crashing the custom renderer.
            // If the slider is at 0, we send a microscopic fraction (0.001f). 
            // Alternatively, if your engine expects 'null' for no reflection, change this to: newReflection = null;
            Float newReflection;
            if (reflection == 0) {
                newReflection = 0.001f; 
            } else {
                newReflection = reflection / 100f;
            }

            materialsListModel.setMaterialAtInternal(
                  new HomeMaterial(material.getName(), material.getColor(), material.getTexture(), 
                material.getShininess(), 
                material.getTransparency(), 
                newReflection),
                  index);
            }

          materialsListModel.fireMaterialsChanged(selectedIndices);
          updateMaterials();
          }
        };
      this.reflectionSlider.addChangeListener(reflectionChangeListener);
      
      this.reflectionSlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          int sliderValue = reflectionSlider.getValue();
          if (reflectionSpinnerModel.getNumber().intValue() != sliderValue) {
            reflectionSpinnerModel.setValue(sliderValue);
          }
        }
      });
      reflectionSpinnerModel.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent ev) {
          int spinnerValue = reflectionSpinnerModel.getNumber().intValue();
          if (reflectionSlider.getValue() != spinnerValue) {
            reflectionSlider.setValue(spinnerValue);
          }
        }
      });

      this.materialsList.getSelectionModel().addListSelectionListener(
          new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent ev) {
              if (ev.getValueIsAdjusting()) {
            return;
          }

          if (!materialsList.isSelectionEmpty()) {
                defaultColorAndTextureRadioButton.removeChangeListener(defaultChoiceChangeListener);
                invisibleRadioButton.removeChangeListener(invisibleChoiceChangeListener);
                colorRadioButton.removeChangeListener(colorChoiceChangeListener);
                textureRadioButton.removeChangeListener(textureChoiceChangeListener);
                colorButton.removePropertyChangeListener(ColorButton.COLOR_PROPERTY, colorChangeListener);

            shininessSlider.removeChangeListener(shininessChangeListener);
            transparencySlider.removeChangeListener(transparencyChangeListener);
            reflectionSlider.removeChangeListener(reflectionChangeListener);

            updatingControls = true;
            try {
                HomeMaterial material = (HomeMaterial)materialsList.getSelectedValue();
                HomeTexture texture = material.getTexture();
                Integer color = material.getColor();
                HomeMaterial defaultMaterial = materialsListModel.getDefaultMaterialAt(materialsList.getSelectedIndex());
                if (color == null && texture == null) {
                  defaultColorAndTextureRadioButton.setSelected(true);
                  // Display default color or texture in buttons
                  texture = defaultMaterial.getTexture();
                  if (texture != null) {
                    colorButton.setColor(null);
                  textureController.setTexture(texture);
                  } else {
                    color = defaultMaterial.getColor();
                    if (color != null) {
                      textureController.setTexture(null);
                      colorButton.setColor(color);
                    }
                  }
                } else if (texture != null) {
                  textureRadioButton.setSelected(true);
                  colorButton.setColor(null);
                  textureController.setTexture(texture);
                } else if ((color.intValue() & 0xFF000000) == 0) {
                  invisibleRadioButton.setSelected(true);
                  // Display default color or texture in buttons
                  texture = defaultMaterial.getTexture();
                  if (texture != null) {
                    colorButton.setColor(null);
                  textureController.setTexture(texture);
                  } else {
                    color = defaultMaterial.getColor();
                    if (color != null) {
                      textureController.setTexture(null);
                      colorButton.setColor(color);
                    }
                  }
                } else {
                  colorRadioButton.setSelected(true);
                  textureController.setTexture(null);
                  colorButton.setColor(color);
                }

              int [] selectedIndices = materialsList.getSelectedIndices();
              Float commonShininess = null;
              boolean shininessShare = true;
              Float commonTransparency = null;
              boolean transparencyShare = true;
              Float commonReflection = null;
              boolean reflectionShare = true;

              for (int i = 0; i < selectedIndices.length; i++) {
                int idx = selectedIndices [i];
                HomeMaterial mat = (HomeMaterial)materialsList.getModel().getElementAt(idx);
                HomeMaterial defMat = materialsListModel.getDefaultMaterialAt(idx);

                Float s = mat.getShininess() != null
                    ? mat.getShininess()
                    : defMat.getShininess();
                Float t = mat.getTransparency() != null
                    ? mat.getTransparency()
                    : defMat.getTransparency();
                Float r = mat.getReflection() != null
                    ? mat.getReflection()
                    : defMat.getReflection();

                if (i == 0) {
                  commonShininess = s;
                  commonTransparency = t;
                  commonReflection = r;
                } else {
                if (shininessShare && (commonShininess == null
                      ? s != null
                      : !commonShininess.equals(s)))
                    shininessShare = false;
                  if (transparencyShare && (commonTransparency == null
                      ? t != null
                      : !commonTransparency.equals(t)))
                    transparencyShare = false;
                  if (reflectionShare && (commonReflection == null
                      ? r != null
                      : !commonReflection.equals(r)))
                    reflectionShare = false;
                }
              }

              // Sync Shininess Slider
              if (shininessShare && commonShininess != null) {
                  shininessSlider.setValue(Math.round(commonShininess * 100));
                } else {
                Float s = material.getShininess() != null
                    ? material.getShininess()
                    : defaultMaterial.getShininess();
                  shininessSlider.setValue(s != null
                    ? Math.round(s * 100)
                    : 50);
              }

              // Sync Transparency Slider
              if(transparencyShare && commonTransparency != null) {
                transparencySlider.setValue(Math.round(commonTransparency * 100));
              } else {
                Float t = material.getTransparency() != null
                    ? material.getTransparency()
                    :defaultMaterial.getTransparency();
                transparencySlider.setValue(t != null
                    ? Math.round(t * 100)
                    : 0);
              }

              // Sync Reflection Slider
              if (reflectionShare && commonReflection != null) {
                reflectionSlider.setValue(Math.round(commonReflection * 100));
              } else {
                Float r = material.getReflection() != null ? material.getReflection()
                    : defaultMaterial.getReflection();
                reflectionSlider.setValue(r != null
                    ? Math.round(r * 100) : 0);
                }
            } finally {
              updatingControls = false;
                defaultColorAndTextureRadioButton.addChangeListener(defaultChoiceChangeListener);
                invisibleRadioButton.addChangeListener(invisibleChoiceChangeListener);
                colorRadioButton.addChangeListener(colorChoiceChangeListener);
                textureRadioButton.addChangeListener(textureChoiceChangeListener);
                colorButton.addPropertyChangeListener(ColorButton.COLOR_PROPERTY, colorChangeListener);

              shininessSlider.addChangeListener(shininessChangeListener);
              transparencySlider.addChangeListener(transparencyChangeListener);
              reflectionSlider.addChangeListener(reflectionChangeListener);
              }
          }
              enableComponents();
            }
          });

      this.materialsList.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent ev) {
          // Open color or texture choice dialogs on double clicks
          if (ev.getClickCount() == 2 && !materialsList.isSelectionEmpty()) {
            if (colorButton.getColor() != null) {
              colorButton.doClick(200);
            } else if (controller.getTextureController().getTexture() != null
                       && textureComponent instanceof AbstractButton) {
              ((AbstractButton)textureComponent).doClick(200);
            }
          }
        }
      });

      if (this.materialsList.getModel().getSize() > 0) {
        this.materialsList.setSelectedIndex(0);
      } else {
        // Add a listener that will select first row as soon as the list contains some data
        this.materialsList.getModel().addListDataListener(new ListDataListener() {
            public void intervalAdded(ListDataEvent ev) {
              materialsList.setSelectedIndex(0);
              ((ListModel)ev.getSource()).removeListDataListener(this);
            }

            public void contentsChanged(ListDataEvent ev) {
              intervalAdded(ev);
            }

            public void intervalRemoved(ListDataEvent ev) {
            }
          });
      }

      enableComponents();
    }

    /**
     * Updates the edited materials in both the dialog preview and the
     * controller used by the main 3D view.
     *
     * This method is called directly after a material property is changed
     * instead of relying only on a ListModel event. The list event is kept
     * for refreshing the material list, while this method explicitly pushes
     * the updated material array to the preview and to the controller.
     */
      private void updateMaterials () {
      MaterialsListModel materialsListModel = (MaterialsListModel)this.materialsList.getModel();
      HomeMaterial [] materials = materialsListModel.getMaterials();

      // Update the dialog preview immediately.
          if (this.previewComponent != null) {
        this.previewComponent.setModelMaterials(materials);
        this.previewComponent.repaint();
        }

      // Update the controller immediately so the main 3D view can refresh
      // while the material dialog is still open.
      this.controller.setMaterials(materials);
              }

    /**
     * Enables editing components according to current selection in materials
     * list.
     */
    private void enableComponents() {
      boolean selectionEmpty = this.materialsList.isSelectionEmpty();
      defaultColorAndTextureRadioButton.setEnabled(!selectionEmpty);
      invisibleRadioButton.setEnabled(!selectionEmpty);
      textureRadioButton.setEnabled(!selectionEmpty);
      textureComponent.setEnabled(!selectionEmpty);
      colorRadioButton.setEnabled(! selectionEmpty);
      colorButton.setEnabled(!selectionEmpty);
      shininessSlider.setEnabled(!selectionEmpty);
      shininessSpinner.setEnabled(!selectionEmpty);
      transparencySlider.setEnabled(!selectionEmpty);
      transparencySpinner.setEnabled(!selectionEmpty);
      reflectionSlider.setEnabled(!selectionEmpty);
      reflectionSpinner.setEnabled(!selectionEmpty);
    }

    /**
     * Sets components mnemonics and label / component associations.
     */
    private void setMnemonics(UserPreferences preferences) {
      if (!OperatingSystem.isMacOSX()) {
        this.materialsLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            ModelMaterialsComponent.class, "materialsLabel.mnemonic")).getKeyCode());
        this.materialsLabel.setLabelFor(this.materialsList);
        this.defaultColorAndTextureRadioButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            ModelMaterialsComponent.class, "defaultColorAndTextureRadioButton.mnemonic")).getKeyCode());
        this.invisibleRadioButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            ModelMaterialsComponent.class, "invisibleRadioButton.mnemonic")).getKeyCode());
        this.colorRadioButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            ModelMaterialsComponent.class, "colorRadioButton.mnemonic")).getKeyCode());
        this.textureRadioButton.setMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            ModelMaterialsComponent.class, "textureRadioButton.mnemonic")).getKeyCode());
        this.shininessLabel.setDisplayedMnemonic(KeyStroke.getKeyStroke(preferences.getLocalizedString(
            ModelMaterialsComponent.class, "shininessLabel.mnemonic")).getKeyCode());
        this.shininessLabel.setLabelFor(this.shininessSlider);
        this.transparencyLabel.setDisplayedMnemonic(KeyStroke
            .getKeyStroke(preferences.getLocalizedString(ModelMaterialsComponent.class, "transparencyLabel.mnemonic"))
            .getKeyCode());
        this.transparencyLabel.setLabelFor(this.transparencySlider);
      }
    }

    /**
     * Layouts components in panel with their labels. Compacted UI Architecture.
     */
    private void layoutComponents() {
      int standardGap = Math.round(3 * SwingTools.getResolutionScale()); // Reduced
                                                                         // for
                                                                         // tighter
                                                                         // vertical
                                                                         // fit
      // Preview
      add(this.previewLabel, new GridBagConstraints(
          0, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, 10), 0, 0));
      float resolutionScale = SwingTools.getResolutionScale();
      this.previewComponent.setPreferredSize(new Dimension((int)(250 * resolutionScale), (int)(250 * resolutionScale)));
      add(this.previewComponent, new GridBagConstraints(
          0, 1, 1, 9, 0.5, 1, GridBagConstraints.NORTH,
          GridBagConstraints.BOTH, new Insets(2, 0, 0, 15), 0, 0));

      // Materials list
      add(this.materialsLabel, new GridBagConstraints(
          1, 0, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, 15), 0, 0));
      JScrollPane scrollPane = new JScrollPane(this.materialsList);
      Dimension preferredSize = scrollPane.getPreferredSize();
      scrollPane.setPreferredSize(new Dimension(Math.max(150, preferredSize.width), preferredSize.height));
      add(scrollPane, new GridBagConstraints(
          1, 1, 1, 9, 0.5, 1, GridBagConstraints.CENTER,
          GridBagConstraints.BOTH, new Insets(0, 0, 0, 15), 0, 0));
      SwingTools.installFocusBorder(this.materialsList);

      // Color and Texture
      add(this.colorAndTextureLabel, new GridBagConstraints(
          2, 0, 3, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, 0), 0, 0));
      add(this.defaultColorAndTextureRadioButton, new GridBagConstraints(
          2, 1, 3, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, 0), 0, 0));
      add(this.invisibleRadioButton, new GridBagConstraints(
          2, 2, 3, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, 0), 0, 0));
      add(this.colorRadioButton, new GridBagConstraints(
          2, 3, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
      add(this.colorButton, new GridBagConstraints(
          3, 3, 2, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, 0), 0, 0));
      add(this.textureRadioButton, new GridBagConstraints(
          2, 4, 1, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, standardGap), 0, 0));
      add(this.textureComponent, new GridBagConstraints(
          3, 4, 2, 1, 0, 0, GridBagConstraints.LINE_START,
          GridBagConstraints.NONE, new Insets(0, 0, standardGap, 0), 0, 0));
      this.textureComponent.setPreferredSize(this.colorButton.getPreferredSize());

      // --- Sliders and Spinners ---
      // Put Label, Slider, and Spinner on the exact same row (Col 2, 3, 4) to
      // eliminate vertical bloat

      // Shininess Row (Row 5)
      add(this.shininessLabel, new GridBagConstraints(
          2, 5, 1, 1, 0, 0, GridBagConstraints.LINE_END,
          GridBagConstraints.NONE, new Insets(10, 0, standardGap, standardGap), 0, 0));
      add(this.shininessSlider, new GridBagConstraints(3, 5, 1, 1, 1, 0, GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL, new Insets(10, 0, standardGap, standardGap), 0, 0));
      add(this.shininessSpinner, new GridBagConstraints(4, 5, 1, 1, 0, 0, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(10, 0, standardGap, 0), 0, 0));

      // Transparency Row (Row 6)
      add(this.transparencyLabel, new GridBagConstraints(
          2, 6, 1, 1, 0, 0, GridBagConstraints.LINE_END,
          GridBagConstraints.NONE, new Insets(standardGap, 0, standardGap, standardGap), 0, 0));
      add(this.transparencySlider, new GridBagConstraints(3, 6, 1, 1, 1, 0, GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL, new Insets(standardGap, 0, standardGap, standardGap), 0, 0));
      add(this.transparencySpinner, new GridBagConstraints(4, 6, 1, 1, 0, 0, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(standardGap, 0, standardGap, 0), 0, 0));

      // Reflection Row (Row 7)
      add(this.reflectionLabel, new GridBagConstraints( 2, 7, 1, 1, 0, 0, GridBagConstraints.LINE_END,
          GridBagConstraints.NONE, new Insets(standardGap, 0, standardGap, standardGap), 0, 0));
      add(this.reflectionSlider, new GridBagConstraints(3, 7, 1, 1, 1, 0, GridBagConstraints.CENTER,
          GridBagConstraints.HORIZONTAL, new Insets(standardGap, 0, standardGap, standardGap),0, 0));
      add(this.reflectionSpinner, new GridBagConstraints(4, 7, 1, 1, 0,0, GridBagConstraints.CENTER,
          GridBagConstraints.NONE, new Insets(standardGap, 0, standardGap, 0), 0, 0));
    }

    public void displayView(View parent) {
      // Backup original materials before any live edits
      final HomeMaterial [] originalMaterials = this.controller.getMaterials();
      final HomeMaterial [] backupMaterials = originalMaterials != null
          ? originalMaterials.clone()
          : null;

      // Show panel in a resizable modal dialog
      final JOptionPane optionPane = new JOptionPane(this, JOptionPane.PLAIN_MESSAGE,
          JOptionPane.OK_CANCEL_OPTION);
      JComponent parentComponent = SwingUtilities.getRootPane((JComponent)parent);
      final JDialog dialog = optionPane.createDialog(parentComponent, controller.getDialogTitle());
      dialog.applyComponentOrientation(parentComponent != null
          ? parentComponent.getComponentOrientation()
          : ComponentOrientation.getOrientation(Locale.getDefault()));
      dialog.setResizable(true);
      // Pack again because resize decorations may have changed dialog preferred size
      dialog.pack();
      dialog.setMinimumSize(getPreferredSize());
      this.controller.getTextureController().addPropertyChangeListener(
          TextureChoiceController.Property.TEXTURE, this.textureChangeListener);
      // Add a listener that transfer focus to focusable field of texture panel when dialog is shown
      dialog.addComponentListener(new ComponentAdapter() {
          @Override
          public void componentShown(ComponentEvent ev) {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().focusNextComponent(ModelMaterialsPanel.this);
            dialog.removeComponentListener(this);
          }
        });
      // Run a timer that will make blink the selected material in preview
      final MaterialBlinker selectedMaterialBlinker = new MaterialBlinker();
      selectedMaterialBlinker.start();
      this.materialsList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
          public void valueChanged(ListSelectionEvent ev) {
          if (!ev.getValueIsAdjusting()) {
            selectedMaterialBlinker.restart();
          }
        }
      });

      dialog.setVisible(true);
      dialog.dispose();

      selectedMaterialBlinker.stop();

      this.controller.getTextureController().removePropertyChangeListener(
          TextureChoiceController.Property.TEXTURE, this.textureChangeListener);
      if (Integer.valueOf(JOptionPane.OK_OPTION).equals(optionPane.getValue())) {
        this.controller.setMaterials(((MaterialsListModel)this.materialsList.getModel()).getMaterials());
      } else {
        this.controller.setMaterials(backupMaterials);
      }
    }


    /**
     * Model for materials list.
     */
    private static class MaterialsListModel extends AbstractListModel {
      private HomeMaterial [] defaultMaterials;
      private HomeMaterial [] materials;

      public MaterialsListModel(final ModelMaterialsController controller) {
        this.materials = controller.getMaterials();
        ModelManager.getInstance().loadModel(controller.getModel(),
          new ModelManager.ModelObserver() {
            public void modelUpdated(BranchGroup modelRoot) {
              defaultMaterials = ModelManager.getInstance().getMaterials(
                  modelRoot, (controller.getModelFlags() & PieceOfFurniture.HIDE_EDGE_COLOR_MATERIAL) != 0, controller.getModelCreator());
              if (materials != null) {
              // Keep only materials that are defined in default materials set
              // (the list can be different if the model loader interprets
              // differently a 3D model file
              // or if materials come from a paste style action
                HomeMaterial [] updatedMaterials = new HomeMaterial [defaultMaterials.length];
                boolean foundInDefaultMaterials = false;
                for (int i = 0; i < defaultMaterials.length; i++) {
                  String materialName = defaultMaterials [i].getName();
                  for (int j = 0; j < materials.length; j++) {
                    if (materials [j] != null
                        && materials [j].getName().equals(materialName)) {
                      updatedMaterials [i] = materials [j];
                      foundInDefaultMaterials = true;
                      break;
                    }
                  }
                }
                if (foundInDefaultMaterials) {
                  materials = updatedMaterials;
                } else {
                  materials = null;
                }
              }
              fireContentsChanged(MaterialsListModel.this, 0, defaultMaterials.length);
            }

            public void modelError(Exception ex) {
              // Let the list be empty
            }
          });
      }

      public Object getElementAt(int index) {
        if (this.materials != null
            && this.materials [index] != null
            && this.materials [index].getName() != null
            && this.materials [index].getName().equals(this.defaultMaterials [index].getName())) {
          return this.materials [index];
        } else {
          // Providing explicitly typed null values to the 6-parameter
          // constructor fallback
          return new HomeMaterial(this.defaultMaterials [index].getName(), (Integer) null, (HomeTexture) null,
              (Float) null, (Float) null, (Float) null);
        }
      }

      public int getSize() {
        if (this.defaultMaterials != null) {
          return this.defaultMaterials.length;
        } else {
          return 0;
        }
      }

      public HomeMaterial getDefaultMaterialAt(int index) {
        return this.defaultMaterials [index];
      }

      /**
       * Sets the material at the given <code>index</code>.
       */
      public void setMaterialAt(HomeMaterial material, int index) {
        setMaterialAtInternal(material, index);
        fireContentsChanged(this, index, index);
      }

      /**
       * Sets the material at the given <code>index</code> without firing a
       * contents changed event.
       *
       * This method is used when several materials are edited together so that
       * only one list data event is fired after all selected materials have been
       * updated.
       */
      private void setMaterialAtInternal(HomeMaterial material, int index) {
        // Updated to account for reflection
        if (material.getColor() == null
            && material.getTexture() == null
            && material.getShininess() == null
            && material.getTransparency() == null && material.getReflection() == null) {
          if (this.materials != null) {
            this.materials [index] = null;
            boolean containsOnlyNull = true;
            for (HomeMaterial m : this.materials) {
              if (m != null) {
                containsOnlyNull = false;
                break;
              }
            }
            if (containsOnlyNull) {
              this.materials = null;
            }
          }
        } else {
          if (this.materials == null || this.materials.length != this.defaultMaterials.length) {
            this.materials = new HomeMaterial [this.defaultMaterials.length];
          }
          this.materials [index] = material;
        }
      }

      /**
       * Fires one contents changed event after several materials have been
       * modified.
       */
      public void fireMaterialsChanged(int [] indices) {
        if (indices != null && indices.length > 0) {
        fireContentsChanged(this, 0, getSize() - 1);
      }
      }

      /**
       * Returns the edited materials.
       */
      public HomeMaterial [] getMaterials() {
        return this.materials == null
            ? null
            : this.materials.clone();
      }
    }

    /**
     * A timer that makes blink the selected material in preview.
     */
    private class MaterialBlinker extends Timer {
      public MaterialBlinker() {
        super(500, null);
        addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
              toggleBlinkingState();
            }
          });
      }

      private void toggleBlinkingState() {
        MaterialsListModel listModel = (MaterialsListModel)materialsList.getModel();
        if (listModel.getSize() > 1) {
          HomeMaterial [] materials = listModel.getMaterials();

          // FIX: Swapped the delay conditions.
          // Show the blink color for 100ms, and REAL materials for 1000ms.
          if (getDelay() != 100) {
            setDelay(100); // Stay in blink state for 100ms
            if (materials == null) {
              materials = new HomeMaterial [listModel.getSize()];
            } else {
              materials = materials.clone();
            }
            for (int index : materialsList.getSelectedIndices()) {
              HomeMaterial defaultMaterial = listModel.getDefaultMaterialAt(index);
              HomeMaterial selectedMaterial = materials [index] != null
                  ? materials [index]
                  : defaultMaterial;
              int blinkColor = materialsList.getSelectionBackground().darker().getRGB();
              if (selectedMaterial.getTexture() == null) {
                Integer selectedColor = selectedMaterial.getColor();
                if (selectedColor == null) {
                  selectedColor = defaultMaterial.getColor();
                  if (selectedColor == null) {
                    selectedColor = 0xFF000000;
                  }
                }
                int red   = (selectedColor >> 16) & 0xFF;
                int green = (selectedColor >> 8) & 0xFF;
                int blue  = selectedColor & 0xFF;
                if (Math.max(red, Math.max(green, blue)) > 0x77) {
                  // Display a darker color for a bright color
                  blinkColor = new Color(selectedColor).darker().darker().getRGB();
                } else if ((red + green + blue) / 3 > 0x0F) {
                  // Display a brighter color for a dark color
                  blinkColor = new Color(selectedColor).brighter().brighter().getRGB();
                }
              }
              // Passed selectedMaterial.getReflection() to preserve state
              // during blink cycles
              materials [index] =
                  new HomeMaterial(selectedMaterial.getName(), blinkColor, null, selectedMaterial.getShininess(), selectedMaterial.getTransparency(),
                  selectedMaterial.getReflection());
            }
          } else {
            // FIX: Restore the assigned materials and wait 1000ms
            setDelay(1000);
          }
          previewComponent.setModelMaterials(materials);
          previewComponent.repaint();
        }
      }

      @Override
      public void restart() {
        setInitialDelay(100);
        // FIX: Set this to 1000 so the very first tick triggers the 100ms blink
        // block above
        setDelay(1000);
        super.restart();
      }
    }
  }
}