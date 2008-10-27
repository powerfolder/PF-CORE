package de.dal33t.powerfolder.ui.folders;

import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.PFUIComponent;
import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.ui.widget.JButton3Icons;
import de.dal33t.powerfolder.ui.widget.JButtonMini;

import javax.swing.*;
import javax.swing.border.Border;
import static javax.swing.JLabel.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;

public class ExpandableFolderView extends PFUIComponent {

    private final Folder folder;

    private JButtonMini expandCollapseButton;

    private JPanel uiComponent;

    private JPanel lowerPanel;

    private AtomicBoolean expanded;

    public ExpandableFolderView(Controller controller, Folder folder) {
        super(controller);
        this.folder = folder;
    }

    private void buildUI() {

        initComponent();

        // Build ui
        FormLayout upperLayout = new FormLayout("pref, 3dlu, pref, pref:grow, 3dlu, pref, 3dlu, pref",
            "pref");
        PanelBuilder upperBuilder = new PanelBuilder(upperLayout);
        CellConstraints cc = new CellConstraints();

        upperBuilder.add(new JLabel(Icons.DIRECTORY), cc.xy(1, 1));
        upperBuilder.add(new JLabel(folder.getName()), cc.xy(3, 1));
        upperBuilder.add(new JButtonMini(Icons.DOWNLOAD_ACTIVE), cc.xy(6, 1));
        upperBuilder.add(expandCollapseButton, cc.xy(8, 1));

        JPanel upperPanel = upperBuilder.getPanel();
        upperPanel.setBorder(BorderFactory.createEtchedBorder());

        // Build ui
        FormLayout lowerLayout = new FormLayout("pref, 3dlu, pref:grow",
            "pref");
        PanelBuilder lowerBuilder = new PanelBuilder(lowerLayout);

        lowerBuilder.add(new JLabel(folder.getLocalBase().getAbsolutePath()), cc.xy(1, 1));

        lowerPanel = lowerBuilder.getPanel();
        lowerPanel.setVisible(false);

        // Build ui
        FormLayout outerLayout = new FormLayout("3dlu, pref:grow, 3dlu",
            "pref, 3dlu, pref, 3dlu");
        PanelBuilder outerBuilder = new PanelBuilder(outerLayout);

        outerBuilder.add(upperPanel, cc.xy(2, 1));
        outerBuilder.add(lowerPanel, cc.xy(2, 3));
        uiComponent = outerBuilder.getPanel();

    }

    private void initComponent() {
        expanded = new AtomicBoolean();

        expandCollapseButton = new JButtonMini(Icons.EXPAND);
        expandCollapseButton.addActionListener(new MyActionListener());
    }

    public JPanel getUIComponent() {
        if (uiComponent == null) {
            buildUI();
        }
        return uiComponent;
    }

    public String getFolderName() {
        return folder.getName();
    }

    private class MyActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            boolean exp = expanded.get();
            if (exp) {
                expanded.set(false);
                expandCollapseButton.setIcon(Icons.EXPAND);
                lowerPanel.setVisible(false);
            } else {
                expanded.set(true);
                expandCollapseButton.setIcon(Icons.COLLAPSE);
                lowerPanel.setVisible(true);
            }
        }
    }
}
