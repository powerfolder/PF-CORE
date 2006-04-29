/* AboutBoxDialog.java */

package de.dal33t.powerfolder.ui.dialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.factories.ButtonBarFactory;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.BuildStrings;
import de.dal33t.powerfolder.util.ManuallyInvokedUpdateChecker;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.Util;
import de.dal33t.powerfolder.util.ui.BaseDialog;
import de.dal33t.powerfolder.util.ui.LinkedTextBuilder;
import de.dal33t.powerfolder.util.ui.ScrollableTextPane;

/**
 * Handles the display of the About Box dialog. Holds 2 tabs: about and team.
 * 
 * @author <a href=mailto:xsj@users.sourceforge.net">Daniel Harabor </a>
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @author <A HREF="mailto:schaatser@powerfolder.com">Jan van Oosterom</A>
 * @version $Revision: 1.19 $
 */
public class AboutBoxDialog extends BaseDialog {
    private Object[] contributers = {"Christian Sprajc", "Albena Roshelova",
        "Bernhard Rutkowsky", "Dane Smith", "Michael Petrovic-Brings",
        "Florian Lahr", "Oliver Häusler", "Peter Hüren", "Daniel Harabor",
        "Thorsten Lenze", "Jan van Oosterom", "Arun Jacob Elias", "Gabriele",
        "Fernando Silveira", "Garada", "Keblo", "Dennis Waldherr",
        "Jason Sallis"};
    private JTabbedPane tabbedPanel;
    private ScrollableTextPane scrollableTextPane;

    public AboutBoxDialog(Controller controller, boolean modal) {
        super(controller, modal, false); // border
        // sort list
        List contr = new LinkedList();
        for (Object contibuter : contributers) {
            contr.add(contibuter);
        }
        Collections.sort(contr);
        contributers = contr.toArray();
    }

    protected Component getButtonBar() {
        JButton okButton;
        JButton checkForUpdatesButton;
        JComponent buttonBar;

        okButton = createOKButton(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });

        checkForUpdatesButton = createCheckForUpdatesButton(new ActionListener()
        {
            public void actionPerformed(ActionEvent e) {
                checkForUpdates();
            }
        });

        okButton.setMnemonic('O');
        checkForUpdatesButton.setMnemonic('U');

        buttonBar = ButtonBarFactory.buildCenteredBar(checkForUpdatesButton,
            okButton);

        return buttonBar;
    }

    protected Component getContent() {
        if (tabbedPanel == null) {
            initComponents();
        }
        return tabbedPanel;

    }

    private void initComponents() {
        tabbedPanel = new JTabbedPane();
        PanelBuilder builder = LinkedTextBuilder.build(Translation
            .getTranslation("about.dialog.text", Controller.PROGRAM_VERSION));
        builder.setBorder(Borders.createEmptyBorder("0, 7dlu, 20dlu, 10dlu"));

        tabbedPanel.addTab(
            Translation.getTranslation("about.dialog_about_tab"), builder
                .getPanel());
        BuildStrings HTML = new BuildStrings(
            "<HTML><BODY { font-family: Verdana,Arial, Helvetica, sans-serif; font-size: 10px; color: #000000; background : #FFFFFF; }>");
        for (Object contibuter : contributers) {
            HTML.append(contibuter + "<BR>");
        }
        HTML.append("</BODY></HTML>");
        scrollableTextPane = new ScrollableTextPane(HTML.toString(), 1500);
        Util.setZeroHeight(scrollableTextPane);        

        tabbedPanel.addTab(Translation
            .getTranslation("about.dialog_contributers_tab"),
            scrollableTextPane);

        tabbedPanel.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (tabbedPanel.getSelectedIndex() == 1) {
                    startScroller();
                } else {
                    scrollableTextPane.stop();
                }
            }
        });

    }

    
    private void startScroller() {        
        scrollableTextPane.start();
        // add a stop if about is closed:
        JDialog dialog = getUIComponent();
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e)
            {
                scrollableTextPane.stop();
            }
        });
    }

    protected Icon getIcon() {
        return Icons.LOGO96X96;
    }

    public String getTitle() {
        return Translation.getTranslation("about.dialog.title");
    }

    /**
     * Creates an internationlaized check for updates button
     * 
     * @param listener
     *            the listener to be put on the button
     * @return
     */
    protected JButton createCheckForUpdatesButton(ActionListener listener) {
        JButton checkForUpdatesButton = new JButton(Translation
            .getTranslation("about.dialog.check_for_updates"));
        checkForUpdatesButton.addActionListener(listener);
        return checkForUpdatesButton;
    }

    /**
     * Invokes update checker to check for application updates
     */
    private void checkForUpdates() {
        new ManuallyInvokedUpdateChecker(getController()).start();
    }
}