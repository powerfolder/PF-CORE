package de.dal33t.powerfolder.disk.problem;

import de.dal33t.powerfolder.ui.WikiLinks;
import de.dal33t.powerfolder.util.Translation;

public class DeviceDisconnectedProblem extends Problem {

    @Override
    public String getDescription() {
        return Translation.getTranslation("folder_problem.device_disconnected");
    }

    @Override
    public String getWikiLinkKey() {
        return WikiLinks.PROBLEM_DEVICE_DISCONNECTED;
    }

}
