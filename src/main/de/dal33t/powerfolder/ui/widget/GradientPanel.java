/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.ui.widget;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.JPanel;
import javax.swing.UIManager;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class GradientPanel extends JPanel {
    public static final Color VERY_VERY_LIGHT_GRAY = new Color(255, 255, 255);
    public static final Color VERY_LIGHT_GRAY = new Color(255, 255, 255);

    private GradientPanel(Color background) {
        setBackground(background);
    }

    public static JPanel create(JPanel panel) {
        return create(panel, VERY_VERY_LIGHT_GRAY);
    }

    public static JPanel create(JPanel panel, Color color) {
        panel.setOpaque(false);
        JPanel p = new GradientPanel(color);
        // JPanel p = new GradientPanel(new Color(240, 240, 240));
        p.setOpaque(false);
        FormLayout layout = new FormLayout("fill:pref:grow", "fill:pref:grow");
        PanelBuilder builder = new PanelBuilder(layout, p);
        CellConstraints cc = new CellConstraints();
        builder.setBorder(null);
        builder.add(panel, cc.xy(1, 1));
        builder.getPanel().setOpaque(false);

        return builder.getPanel();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (isOpaque()) {
            return;
        }
        Color control = UIManager.getColor("control");
        int width = getWidth();
        int height = getHeight();

        Graphics2D g2 = (Graphics2D) g;
        Paint storedPaint = g2.getPaint();
        g2.setPaint(new GradientPaint(width, height, getBackground(),
            (int) (width / 1.7f), 0, control));
        g2.fillRect(0, 0, width, height);
        g2.setPaint(storedPaint);
    }
}
