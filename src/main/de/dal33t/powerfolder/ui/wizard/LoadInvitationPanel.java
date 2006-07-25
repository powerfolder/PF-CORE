/* $Id: LoadInvitationPanel.java,v 1.11 2006/03/04 11:16:39 bytekeeper Exp $
 */
package de.dal33t.powerfolder.ui.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.List;

import javax.swing.*;

import jwf.WizardPanel;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.Member;
import de.dal33t.powerfolder.message.Invitation;
import de.dal33t.powerfolder.ui.Icons;
import de.dal33t.powerfolder.util.InvitationUtil;
import de.dal33t.powerfolder.util.Translation;
import de.dal33t.powerfolder.util.ui.ComplexComponentFactory;
import de.dal33t.powerfolder.util.ui.SimpleComponentFactory;

/**
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc </a>
 * @version $Revision: 1.11 $
 */
public class LoadInvitationPanel extends PFWizardPanel {
    private boolean initalized = false;

    private JLabel folderNameLabel;
    private JLabel fromLabel;
    private JLabel invitorLabel;
    private JComponent locationField;
    private ValueModel locationModel;
    private Invitation invitation;
    private JCheckBox makeInviterFriend;

    public LoadInvitationPanel(Controller controller) {
        super(controller);
    }

    // Application logic ******************************************************

    private void loadInvitation(String file) {
        if (file == null) {
            return;
        }
        invitation = InvitationUtil.load(new File(file));
        log().warn("Loaded invitation " + invitation);
        if (invitation != null) {
            String text = invitation.folder.name + " (";
            if (invitation.folder.secret) {
                text += "private";
            } else {
                text += "public";
            }
            text += ")";

            folderNameLabel.setText(text);
            folderNameLabel.setIcon(Icons.getIconFor(invitation.folder));

            Member node = invitation.invitor.getNode(getController());
            invitorLabel.setText(node != null
                ? node.getNick()
                : invitation.invitor.nick);
            invitorLabel.setIcon(Icons.getIconFor(node));
            fromLabel.setVisible(true);
            if (node == null || !node.isFriend()) {
                makeInviterFriend.setText(Translation.getTranslation(
                    "wizard.loadinvitation.addtofriends", invitorLabel.getText()));
                makeInviterFriend.setSelected(invitation.folder.secret);
                makeInviterFriend.setVisible(true);
            }
        } else {
            folderNameLabel.setText("<unable to read file>");
            folderNameLabel.setIcon(null);
            invitorLabel.setText("");
            invitorLabel.setIcon(null);
            fromLabel.setVisible(false);
            makeInviterFriend.setVisible(false);
        }
    }

    // From WizardPanel *******************************************************

    public synchronized void display() {
        if (!initalized) {
            buildUI();
        }
    }

    public boolean hasNext() {
        return invitation != null;
    }

    public boolean validateNext(List list) {
        return true;
    }

    public WizardPanel next() {
        // Add invitor to friends, when folder is private
        if (makeInviterFriend.isSelected()) {
            invitation.invitor.getNode(getController(), true).setFriend(true);
        }

        // Choose location...
        // Configure
        getWizardContext().setAttribute(
            ChooseDiskLocationPanel.FOLDERINFO_ATTRIBUTE, invitation.folder);

        // Not prompt for send invitation afterwards
        getWizardContext().setAttribute(
            ChooseDiskLocationPanel.SEND_INVIATION_AFTERWARDS, Boolean.FALSE);

        return new ChooseDiskLocationPanel(getController());
    }

    public boolean canFinish() {
        return false;
    }

    public void finish() {
    }

    // UI building ************************************************************

    /**
     * Builds the ui
     */
    private void buildUI() {
        // init
        initComponents();

        // setBorder(new TitledBorder(Translation
        // .getTranslation("wizard.loadinvitation.title"))); //Load invitation
        setBorder(Borders.EMPTY_BORDER);

        FormLayout layout = new FormLayout(
            "20dlu, pref, 15dlu, left:pref",
            "5dlu, pref, 15dlu, pref, 4dlu, pref, 14dlu, pref, 2dlu, pref, 4dlu, pref, 4dlu, pref, pref:grow");

        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cc = new CellConstraints();

        builder.add(createTitleLabel(Translation
            .getTranslation("wizard.loadinvitation.select")), cc.xy(4, 2)); // Select
        // invitation
        // file
        // Add current wizard pico
        builder.add(new JLabel((Icon) getWizardContext().getAttribute(
            PFWizard.PICTO_ICON)), cc.xywh(2, 4, 1, 3, CellConstraints.DEFAULT,
            CellConstraints.TOP));

        builder.addLabel(Translation
            .getTranslation("wizard.loadinvitation.selectfile"), cc.xy(4, 4));// Please
        // select
        // invitation
        // file
        builder.add(locationField, cc.xy(4, 6));
        builder.add(folderNameLabel, cc.xy(4, 8));
        builder.add(fromLabel, cc.xy(4, 10));
        builder.add(invitorLabel, cc.xy(4, 12));
        builder.add(makeInviterFriend, cc.xy(4, 14));
        // initalized
        initalized = true;
    }

    /**
     * Initalizes all nessesary components
     */
    private void initComponents() {
        locationModel = new ValueHolder();

        // Behavior
        locationModel.addValueChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                loadInvitation((String) evt.getNewValue());
                updateButtons();
            }
        });

        folderNameLabel = SimpleComponentFactory.createLabel();
        fromLabel = SimpleComponentFactory.createLabel(Translation
            .getTranslation("general.from"));
        fromLabel.setVisible(false);
        invitorLabel = SimpleComponentFactory.createLabel();

        locationField = ComplexComponentFactory.createFileSelectionField(
            Translation.getTranslation("wizard.loadinvitation.choosefile"),
            locationModel, JFileChooser.FILES_AND_DIRECTORIES, InvitationUtil
                .createInvitationsFilefilter(), null); // Choose the invitation
        // file
        // Ensure minimum dimension
        Dimension dims = locationField.getPreferredSize();
        dims.width = Sizes.dialogUnitXAsPixel(147, locationField);
        locationField.setPreferredSize(dims);
        locationField.setBackground(Color.WHITE);
        
        makeInviterFriend = new JCheckBox(Translation
            .getTranslation("wizard.loadinvitation.addtofriends"));
        makeInviterFriend.setOpaque(false);
        makeInviterFriend.setVisible(false);
    }
}