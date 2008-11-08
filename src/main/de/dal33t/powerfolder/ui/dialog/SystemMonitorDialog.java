package de.dal33t.powerfolder.ui.dialog;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.PFUIComponent;
import de.javasoft.synthetica.addons.SystemMonitor;
import de.javasoft.synthetica.addons.systemmonitor.CollectorFactory;

import javax.swing.JDialog;
import java.awt.BorderLayout;

public class SystemMonitorDialog extends PFUIComponent {

    private JDialog dialog;

    public SystemMonitorDialog(Controller controller) {
        super(controller);
    }

    public JDialog getUIComponent() {
        if (dialog == null) {
            initialize();
        }
        return dialog;
    }

    private void initialize() {
        dialog = new JDialog(getController().getUIController().getMainFrame().getUIComponent(),
                false);
        dialog.getContentPane().setLayout(new BorderLayout());
        SystemMonitor systemMonitor = new SystemMonitor();
        dialog.add(systemMonitor, BorderLayout.CENTER);
        dialog.setSize(500, 300);
        systemMonitor.addCollector("CPU_USAGE", CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.CPU_USAGE.id));
        systemMonitor.addCollector("THREADS", CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.THREADS.id));
        systemMonitor.addCollector("HEAP_MEMORY_USAGE", CollectorFactory.getFactory()
                .createCollector(CollectorFactory.CollectorID.HEAP_MEMORY_USAGE.id));
    }
}
