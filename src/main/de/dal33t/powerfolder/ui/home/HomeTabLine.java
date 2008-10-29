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
 * $Id: HomeTabLine.java 5495 2008-10-24 04:59:13Z harry $
 */
package de.dal33t.powerfolder.ui.home;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.util.Format;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.text.DecimalFormat;

/**
 * Class to render a value - label line in the HomeTab.
 * It can be configured
 * a) to just show a value and description,
 * b) show a value and description only is non-zero, or
 * c) show alternate line for zero value.
 *
 * Includes a 3dlu space under the detil (if displayed). 
 *
 */
public class HomeTabLine extends PFUIComponent {

    private static final String ZERO_CARD = "Z";
    private static final String NORMAL_CARD = "N";

    private JPanel uiComponent;
    private final String normalLabelText;
    private final String zeroLabelText;
    private final boolean hideOnZero;
    private final boolean castInt;

    private JPanel cardPanel;
    private CardLayout cardLayout;

    private JLabel valueLabel;
    private JLabel normalLabel;
    private JLabel zeroLabel;

    /**
     * Constructor - shows zeroLabelText if value is zero.
     *
     * @param controller
     * @param normalLabelText
     *                  the text to show to the right of the value
     * @param zeroLabelText
     *                  text to replace line if zero. If null, normal line
     *                  still displays for zero.
     * @param hideOnZero
     *                  hides entire uiComponent if true if value zero
     * @param castInt
     *                  cast value to int for display
     */
    public HomeTabLine(Controller controller, String normalLabelText,
                       String zeroLabelText, boolean hideOnZero, boolean castInt)
    {
        super(controller);
        this.normalLabelText = normalLabelText;
        this.zeroLabelText = zeroLabelText;
        this.hideOnZero = hideOnZero;
        this.castInt = castInt;
    }

    /**
     * Set the numeric value.
     *
     * @param value
     */
    public void setValue(double value) {

        logFiner("Setting value:" + value);

        // Check that component is built.
        getUIComponent();

        if (Double.compare(value, 0.0d) == 0) {
            logFiner("Value is zero");
            if (hideOnZero) {
                logFiner("Hiding uiComponent");
                uiComponent.setVisible(false);
            } else {
                 displayLine(value);
            }
        } else {
            logFiner("Value not zero");
            if (hideOnZero) {
                logFiner("Showing uiComponent");
                uiComponent.setVisible(true);
            }
            displayLine(value);
        }
    }

    /**
     * Update the normal text label
     *
     * @param normalLabelText
     */
    public void setNormalLabelText(String normalLabelText) {
        normalLabel.setText(normalLabelText);
    }

    /**
     * Display a line for a value.
     *
     * @param value
     */
    private void displayLine(double value) {
        logFiner("Displaying line");
        if (zeroLabelText != null && Double.compare(value, 0.0d) == 0) {
            displayZeroLine();
        } else {
            displayValueLine(value);
        }
    }

    /**
     * Display the zero card.
     */
    private void displayZeroLine() {
        logFiner("Displaying zero line");
        cardLayout.show(cardPanel, ZERO_CARD);
    }

    /**
     * Display the normal value card.
     * @param value
     */
    private void displayValueLine(double value) {
        logFiner("Displaying normal line");
        cardLayout.show(cardPanel, NORMAL_CARD);
        if (castInt) {
            valueLabel.setText(String.valueOf((int) value));
        } else {
            DecimalFormat numberFormat = Format.getNumberFormat();
            valueLabel.setText(numberFormat.format(value));
        }
    }

    /**
     * Get the line to display.
     *
     * @return
     */
    public JPanel getUIComponent() {
        if (uiComponent == null) {
            initComponents();
            buildUIComponent();
        }
        return uiComponent;
    }

    /**
     * Build the ui panel.
     */
    private void buildUIComponent() {

        // Add zero card.
        cardPanel.add(zeroLabel, ZERO_CARD);

        // Add normal card.
        FormLayout nzLayout = new FormLayout("right:30dlu, 3dlu, pref:grow",
            "pref");
        PanelBuilder nzBuilder = new PanelBuilder(nzLayout);
        CellConstraints cc = new CellConstraints();

        nzBuilder.add(valueLabel, cc.xy(1, 1));
        nzBuilder.add(normalLabel, cc.xy(3, 1));

        cardPanel.add(nzBuilder.getPanel(), NORMAL_CARD);

        // Build ui component.
        FormLayout layout = new FormLayout("pref:grow", "pref, 3dlu");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.add(cardPanel, cc.xy(1, 1));

        uiComponent = builder.getPanel();
    }

    /**
     * Initialize components.
     */
    private void initComponents() {

        cardPanel = new JPanel();
        cardPanel.setLayout(new CardLayout());
        cardLayout = (CardLayout) cardPanel.getLayout();

        valueLabel = new JLabel();
        normalLabel = new JLabel(normalLabelText);
        if (zeroLabelText == null) {
            zeroLabel = new JLabel("?");
        }  else {
            zeroLabel = new JLabel(zeroLabelText);
        }
    }

}
