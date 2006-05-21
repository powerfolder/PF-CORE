package de.dal33t.powerfolder.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;
import de.dal33t.powerfolder.disk.SyncProfile;
import de.dal33t.powerfolder.util.Translation;

public class FolderDetailsHandler extends AbstractVeloHandler implements
    Handler
{
    public FolderDetailsHandler(Controller controller) {
        super(controller);
    }

    public class FolderDetails {
        private String name;
        private String ID;
        private String syncProfileTranslatedName;
        private String profileID;
        private String powerFolderLink;
        FolderDetails(String name, String ID, String syncProfileTranslatedName,
            String profileID, String powerFolderLink)
        {
            this.name = name;
            this.ID = ID;
            this.syncProfileTranslatedName = syncProfileTranslatedName;
            this.profileID = profileID;
            this.powerFolderLink = powerFolderLink;
        }

        public String getID() {
            return ID;
        }

        public String getName() {
            return name;
        }

        public String getSyncProfileTranslatedName() {
            return syncProfileTranslatedName;
        }

        public String getProfileID() {
            return profileID;
        }

        public String getPowerFolderLink() {
            return powerFolderLink;
        }
    }

    public class ProfileDetails {
        private String name;
        private String ID;

        public ProfileDetails(String ID, String name) {
            this.name = name;
            this.ID = ID;
        }

        public String getID() {
            return ID;
        }

        public String getTranslatedName() {
            return name;
        }
    }

    public void doRequest(HTTPRequest httpRequest) {
        FolderRepository repo = getController().getFolderRepository();
        List<Folder> folders = repo.getFoldersAsSortedList();
        Map<String, String> params = httpRequest.getQueryParams();
        if (params != null && params.containsKey("FolderID")) {
            for (Folder folder : folders) {
                if (folder.getId().equals(params.get("FolderID"))) {
                    SyncProfile profile = folder.getSyncProfile();
                    FolderDetails folderDetails = new FolderDetails(folder
                        .getName(), folder.getId(), Translation
                        .getTranslation(profile.getTranslationId()), profile
                        .getId(), folder.getInvitation().toPowerFolderLink());
                    //log().warn("folder found: " + folder.getName());
                    context.put("folderDetails", folderDetails);
                    break;
                }
            }

        }

        SyncProfile[] profiles = SyncProfile.DEFAULT_SYNC_PROFILES;
        List<ProfileDetails> profileDetails = new ArrayList<ProfileDetails>();
        for (SyncProfile profile : profiles) {
            profileDetails.add(new ProfileDetails(profile.getId(), Translation
                .getTranslation(profile.getTranslationId())));
        }
        context.put("profileDetails", profileDetails);
    }

    public String getTemplateFilename() {
        return "folderdetails.vm";
    }

}
