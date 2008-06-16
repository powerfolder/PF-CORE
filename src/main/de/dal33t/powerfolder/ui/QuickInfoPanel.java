/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
*
* This file is part of PowerFolder.
*
* PowerFolder is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation.
*
* PowerFolder is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with PowerFolder. If not, see <http://www.gnu.org/licenses/>.
*
* $Id$
*/
package de.dal33t.powerfolder.ui;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.JPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * The panel the contains the most important and concentrated information about
 * a element (e.g. Folder)<p>
 * TODO Refactor to QuickInfoBuilder
 * 
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.2 $
 */
public abstract class QuickInfoPanel extends PFUIComponent {
    private JPanel panel;

    private JComponent picto;
    private JComponent headerText;
    private JComponent infoText1;
    private JComponent infoText2;

    /** This is an optional component, located at the far right of the panel. */
    private JComponent rightComponent;

    protected QuickInfoPanel(Controller controller) {
        super(controller);
    }

    /**
     * Create the top part of the panel which contains the most concentrated
     * informations
     * 
     * @return the component.
     */
    public JComponent getUIComponent() {
        if (panel == null) {
            // Init components
            initComponents();
            // Init general components
            initComponents0();

            // Build ui
            FormLayout layout = new FormLayout("pref, 14dlu, pref, 14dlu, right:pref:grow",
                "top:pref, 7dlu, pref, 3dlu, top:pref:grow");
            PanelBuilder builder = new PanelBuilder(layout);
            builder.setBorder(Borders.DLU14_BORDER);
            CellConstraints cc = new CellConstraints();
            builder.add(picto, cc.xywh(1, 1, 1, 5));
            if (rightComponent != null) {
                builder.add(rightComponent, cc.xywh(5, 1, 1, 5));
            }
            builder.add(headerText, cc.xy(3, 1));

            builder.add(infoText1, cc.xywh(3, 3, 1, 1));
            builder.add(infoText2, cc.xywh(3, 5, 1, 1));

            panel = builder.getPanel();
            panel.setBackground(Color.WHITE);
        }
        return panel;
    }

    /**
     * Initalizes the components
     */
    private void initComponents0() {
        headerText = getHeaderText();
        infoText1 = getInfoText1();
        infoText2 = getInfoText2();
        picto = getPicto();
        rightComponent = getRightComponent();
    }

    // Implementing part ******************************************************

    /**
     * Overwrite if you want to initalize components before the other JComponent
     * getters are getting called.
     */
    protected void initComponents() {
    }

    /**
     * @return the picto for this panel. Displayed on the left upper side
     */
    protected abstract JComponent getPicto();

    /**
     * Recommended construction via
     * <code>SimpleComponentFactory#createBiggerTextLabel(String)</code>
     * 
     * @see SimpleComponentFactory#createBiggerTextLabel(String)
     * @return The header text. Upper text. Should usually a bigger font
     */
    protected abstract JComponent getHeaderText();

    /**
     * Recommended construction via
     * <code>SimpleComponentFactory#createBigTextLabel(String)</code>
     * 
     * @see SimpleComponentFactory#createBigTextLabel(String)
     * @return First line of info. Use a bigger, but not to big font
     */
    protected abstract JComponent getInfoText1();

    /**
     * Recommended construction via
     * <code>SimpleComponentFactory#createBigTextLabel(String)</code>
     *
     * @see SimpleComponentFactory#createBigTextLabel(String)
     * @return Second line of info. Use a bigger, but not to big font
     */
    protected abstract JComponent getInfoText2();

    /**
     * Optional component that is displayed far right on the panel.
     * Override if required.
     *
     * @return Optional far-right component.
     */
    protected JComponent getRightComponent() {
        return null;
    }

}
