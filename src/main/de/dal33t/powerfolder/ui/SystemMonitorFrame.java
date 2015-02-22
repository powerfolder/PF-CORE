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
 * $Id: SystemMonitorFrame.java 5773 2008-11-08 11:23:56Z harry $
 */
package de.dal33t.powerfolder.ui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.plaf.RootPaneUI;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.util.Translation;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;
import de.javasoft.synthetica.addons.SystemMonitor;
import de.javasoft.synthetica.addons.systemmonitor.CollectorFactory;

/**
 * This class displays a system monitor frame.
 */
public class SystemMonitorFrame extends PFUIComponent {

    private JFrame uiComponent;
    private SystemMonitor systemMonitor;

    /**
     * Constructor
     *
     * @param controller
     */
    public SystemMonitorFrame(Controller controller) {
        super(controller);
    }

    /**
     * Gets the ui component.
     */
    public JFrame getUIComponent() {
        if (uiComponent == null) {
            initialize();
            buildUI();
        }
        return uiComponent;
    }

    /**
     * Builds the ui component.
     */
    private void buildUI() {
        uiComponent = new JFrame(Translation.get("exp.system_monitor.title"));
        uiComponent.setIconImage(Icons.getImageById(Icons.SYSTEM_MONITOR));
        uiComponent.getContentPane().setLayout(new BorderLayout());
        uiComponent.getContentPane().add(systemMonitor, BorderLayout.CENTER);

        Preferences prefs = getController().getPreferences();
        uiComponent.setLocation(prefs.getInt("systemmonitor4.x", 100), prefs.getInt(
            "systemmonitor4.y", 100));

        // Pack elements
        uiComponent.pack();

        int width = prefs.getInt("systemmonitor4.width", 300);
        int height = prefs.getInt("systemmonitor4.height", 200);
        if (width < 50) {
            width = 50;
        }
        if (height < 50) {
            height = 50;
        }
        uiComponent.setSize(width, height);

        if (prefs.getBoolean("systemmonitor4.maximized", false)) {
            // Fix Synthetica maximization, otherwise it covers the task bar.
            // See http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
            RootPaneUI ui = uiComponent.getRootPane().getUI();
            if (ui instanceof SyntheticaRootPaneUI) {
                ((SyntheticaRootPaneUI) ui).setMaximizedBounds(uiComponent);
            }
            uiComponent.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    /**
     * Stores all current window valus.
     */
    public void storeValues() {

        if (uiComponent == null) {
            return;
        }

        // Store main window preferences
        Preferences prefs = getController().getPreferences();

        if ((uiComponent.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
        {
            prefs.putBoolean("systemmonitor4.maximized", true);
        } else {
            prefs.putInt("systemmonitor4.x", uiComponent.getX());
            if (uiComponent.getWidth() > 0) {
                prefs.putInt("systemmonitor4.width", uiComponent.getWidth());
            }
            prefs.putInt("systemmonitor4.y", uiComponent.getY());
            if (uiComponent.getHeight() > 0) {
                prefs.putInt("systemmonitor4.height", uiComponent.getHeight());
            }
            prefs.putBoolean("systemmonitor4.maximized", false);
        }
    }

    /**
     * Initializes the system monitor.
     */
    private void initialize() {
        systemMonitor = new SystemMonitor();
        systemMonitor.addCollector(CollectorFactory.CollectorID.CPU_USAGE.id,
                CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.CPU_USAGE.id));
        systemMonitor.addCollector(CollectorFactory.CollectorID.THREADS.id,
                CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.THREADS.id));
        systemMonitor.addCollector(CollectorFactory.CollectorID.HEAP_MEMORY_USAGE.id,
                CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.HEAP_MEMORY_USAGE.id));

        systemMonitor.addCaption(CollectorFactory.CollectorID.CPU_USAGE.id,
                Translation.get("exp.system_monitor.cpu_usage"),
                "%{percentValue}.0f%%", false);
        systemMonitor.addCaption(CollectorFactory.CollectorID.THREADS.id,
                Translation.get("exp.system_monitor.threads"),
                "%{value},.0f / %{maxValue},.0f (peak)", false);
        systemMonitor.addCaption(CollectorFactory.CollectorID.HEAP_MEMORY_USAGE.id,
                Translation.get("exp.system_monitor.heap_usage"),
                "%{value},.2fMB / %{maxValue},.2fMB / %{percentValue}.0f%%", false);
    }
}
