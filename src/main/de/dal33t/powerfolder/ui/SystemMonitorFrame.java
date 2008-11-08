package de.dal33t.powerfolder.ui;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.javasoft.plaf.synthetica.SyntheticaRootPaneUI;
import de.javasoft.synthetica.addons.SystemMonitor;
import de.javasoft.synthetica.addons.systemmonitor.CollectorFactory;

import javax.swing.JFrame;
import javax.swing.plaf.RootPaneUI;
import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.prefs.Preferences;

public class SystemMonitorFrame extends PFUIComponent {

    private JFrame frame;
    private SystemMonitor systemMonitor;

    public SystemMonitorFrame(Controller controller) {
        super(controller);
    }

    public JFrame getUIComponent() {
        if (frame == null) {
            initialize();
            buildUI();
        }
        return frame;
    }

    private void buildUI() {
        frame = new JFrame();
        frame.setIconImage(Icons.SYSTEM_MONITOR_IMAGE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(systemMonitor, BorderLayout.CENTER);

        Preferences prefs = getController().getPreferences();
        frame.setLocation(prefs.getInt("systemmonitor4.x", 100), prefs.getInt(
            "systemmonitor4.y", 100));

        // Pack elements
        frame.pack();

        int width = prefs.getInt("systemmonitor4.width", 300);
        int height = prefs.getInt("systemmonitor4.height", 200);
        if (width < 50) {
            width = 50;
        }
        if (height < 50) {
            height = 50;
        }
        frame.setSize(width, height);

        if (prefs.getBoolean("systemmonitor4.maximized", false)) {
            // Fix Synthetica maximization, otherwise it covers the task bar.
            // See http://www.javasoft.de/jsf/public/products/synthetica/faq#q13
            RootPaneUI ui = frame.getRootPane().getUI();
            if (ui instanceof SyntheticaRootPaneUI) {
                ((SyntheticaRootPaneUI) ui).setMaximizedBounds(frame);
            }
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        }
    }

    /**
     * Stores all current window valus.
     */
    public void storeValues() {
        // Store main window preferences
        Preferences prefs = getController().getPreferences();

        if ((frame.getExtendedState() & Frame.MAXIMIZED_BOTH) == Frame.MAXIMIZED_BOTH)
        {
            prefs.putBoolean("systemmonitor4.maximized", true);
        } else {
            prefs.putInt("systemmonitor4.x", frame.getX());
            if (frame.getWidth() > 0) {
                prefs.putInt("systemmonitor4.width", frame.getWidth());
            }
            prefs.putInt("systemmonitor4.y", frame.getY());
            if (frame.getHeight() > 0) {
                prefs.putInt("systemmonitor4.height", frame.getHeight());
            }
            prefs.putBoolean("systemmonitor4.maximized", false);
        }
    }


    private void initialize() {
        systemMonitor = new SystemMonitor();
        systemMonitor.addCollector(CollectorFactory.CollectorID.CPU_USAGE.id, CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.CPU_USAGE.id));
        systemMonitor.addCollector(CollectorFactory.CollectorID.THREADS.id, CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.THREADS.id));
        systemMonitor.addCollector(CollectorFactory.CollectorID.HEAP_MEMORY_USAGE.id, CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.HEAP_MEMORY_USAGE.id));

        systemMonitor.addCaption(CollectorFactory.CollectorID.CPU_USAGE.id,
                "CPU Usage:", "%{percentValue}.0f%%", false);
        systemMonitor.addCaption(CollectorFactory.CollectorID.THREADS.id,
                "Threads:", "%{value},.0f / %{maxValue},.0f (peak)", false);
        systemMonitor.addCaption(CollectorFactory.CollectorID.HEAP_MEMORY_USAGE.id,
                "Heap Usage:", "%{value},.2fMB / %{maxValue},.2fMB / %{percentValue}.0f%%", false);
    }
}
