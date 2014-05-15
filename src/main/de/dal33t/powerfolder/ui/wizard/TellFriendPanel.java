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
package de.dal33t.powerfolder.ui.wizard;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JComponent;

import jwf.WizardPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.ui.util.Icons;
import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.ui.util.Help;
import de.dal33t.powerfolder.ui.widget.ActionLabel;
import de.dal33t.powerfolder.ui.widget.LinkLabel;
import de.dal33t.powerfolder.util.SocialNetwork;
import de.dal33t.powerfolder.util.Translation;

/**
 * @author <a href="mailto:harry@powerfolder.com">Harry Glasgow</a>
 * @version $Revision: 4.0$
 */
public class TellFriendPanel extends PFWizardPanel {

    private LinkLabel fbLabel;
    private LinkLabel twitterLabel;
    private LinkLabel linkedInLabel;
    private ActionLabel emailLabel;
    private LinkLabel infoLabel;

    public TellFriendPanel(Controller controller) {
        super(controller);
    }

    protected JComponent buildContent() {
        FormLayout layout = new FormLayout(
            "pref, 7dlu, pref, 7dlu, pref, 7dlu, pref",
                "pref, 30dlu, pref");
        PanelBuilder builder = new PanelBuilder(layout);
        builder.setBorder(createFewContentBorder());
        CellConstraints cc = new CellConstraints();

        builder.add(fbLabel.getUIComponent(), cc.xy(1, 1));
        builder.add(twitterLabel.getUIComponent(), cc.xy(3, 1));
        builder.add(linkedInLabel.getUIComponent(), cc.xy(5, 1));
        builder.add(emailLabel.getUIComponent(), cc.xy(7, 1));

        builder.add(infoLabel.getUIComponent(), cc.xyw(1, 3, 7));

        return builder.getPanel();
    }

    protected String getTitle() {
        return Translation.getTranslation("wizard.tell_friend.title");
    }

    @SuppressWarnings("serial")
    protected void initComponents() {
        String fbLink = SocialNetwork.FACEBOOK.shareLink(getController()
            .getOSClient().getRegisterURLReferral(), null);
        fbLabel = new LinkLabel(getController(), "", fbLink);
        fbLabel.setIcon(Icons.getIconById(Icons.FACEBOOK_BUTTON));

        String twitterLink = SocialNetwork.TWITTER.shareLink(getController()
            .getOSClient().getRegisterURLReferral(), null);
        twitterLabel = new LinkLabel(getController(), "", twitterLink);
        twitterLabel.setIcon(Icons.getIconById(Icons.TWITTER_BUTTON));

        String linkedInLink = SocialNetwork.LINKEDIN.shareLink(getController()
            .getOSClient().getRegisterURLReferral(), null);
        linkedInLabel = new LinkLabel(getController(), "", linkedInLink);
        linkedInLabel.setIcon(Icons.getIconById(Icons.LINKEDIN_BUTTON));

        // TODO Yammer

        emailLabel = new ActionLabel(getController(), new AbstractAction("") {
            public void actionPerformed(ActionEvent e) {
                getWizard().next();
            }
        });
        emailLabel.setIcon(Icons.getIconById(Icons.EMAIL_BUTTON));

        infoLabel = new LinkLabel(getController(), Translation
            .getTranslation("pro.wizard.activation.learn_more"), Help
            .getWikiArticleURL(getController(),
                WikiLinks.REFERRAL_REWARD_SYSTEM));
        infoLabel.convertToBigLabel();
    }

    public boolean hasNext() {
        return false;
    }

    public WizardPanel next() {
        return new TellFriendEmailPanel(getController());
    }

    public boolean validateNext() {
        return true;
    }
}