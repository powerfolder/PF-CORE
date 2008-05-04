package de.dal33t.powerfolder.transfer;

import java.io.IOException;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.light.FileInfo;

public interface DownloadManagerFactory {
    DownloadManager createDownloadManager(Controller controller, FileInfo file, boolean automatic) throws IOException;
}
