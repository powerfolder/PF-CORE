/*
 * Copyright 2004 - 2019 Christian Sprajc. All rights reserved.
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
 */
package de.dal33t.powerfolder.util.os.Win32;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.distribution.Distribution;
import de.dal33t.powerfolder.distribution.PowerFolderBasic;
import de.dal33t.powerfolder.distribution.PowerFolderGeneric;
import de.dal33t.powerfolder.distribution.PowerFolderPro;
import de.dal33t.powerfolder.util.Reject;
import de.dal33t.powerfolder.util.os.OSUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Spacetree {
    private static final String CLSID = "{5107667c-149a-47c8-b0c9-e4bf9132f17d}";

    private String name;
    private Path pathToExe;
    private Path targetDir;

    public static boolean isSupported(Controller c) {
        return OSUtil.isWindowsSystem() && !OSUtil.isSystemService() && c.isUIEnabled();
    }

    public static final Spacetree create(Controller controller) {
        Reject.ifNull(controller, "Controller");
        Distribution d = controller.getDistribution();
        String name = d.getName();
        if (d instanceof PowerFolderBasic || d instanceof PowerFolderPro || d instanceof PowerFolderGeneric) {
            name = d.getBinaryName();
        }
        String exeName = d.getBinaryName() + ".exe";
        Path pathToExe = WinUtils.getProgramInstallationPath().resolve(exeName);

        Path baseDir = controller.getFolderRepository().getFoldersBasedir();
        return new Spacetree(name, pathToExe, baseDir);
    }

    private Spacetree(String name, Path pathToExe, Path targetDir) {
        Reject.ifBlank(name, "Name");
        Reject.ifNull(pathToExe, "Path to exe");
        Reject.ifFalse(Files.exists(pathToExe), "Exe file with icon not found: " + pathToExe);
        Reject.ifNull(targetDir, "Target dir");
        this.name = name;
        this.pathToExe = pathToExe;
        this.targetDir = targetDir;
    }

    public void install() throws IOException {
        regAddCLSID(" /ve /t REG_SZ /d \"" + name + "\" /f");
        regAddCLSID("\\DefaultIcon /VE /T REG_EXPAND_SZ /D \"" + pathToExe + ",0\" /F");
        regAddCLSID(" /v System.IsPinnedToNameSpaceTree /t REG_DWORD /d 0x1 /f");
        regAddCLSID(" /v SortOrderIndex /t REG_DWORD /d 0x42 /f");
        regAddCLSID("\\InProcServer32 /ve /t REG_EXPAND_SZ /d %systemroot%\\system32\\shell32.dll /f");
        regAddCLSID("\\Instance /v CLSID /t REG_SZ /d {0E5AAE11-A475-4c5b-AB00-C66DE400274E} /f");
        regAddCLSID("\\Instance\\InitPropertyBag /v Attributes /t REG_DWORD /d 0x11 /f");
        regAddCLSID("\\Instance\\InitPropertyBag /v TargetFolderPath /t REG_EXPAND_SZ /d \"" + targetDir + "\" /f");
        regAddCLSID("\\ShellFolder /v FolderValueFlags /t REG_DWORD /d 0x28 /f");
        regAddCLSID("\\ShellFolder /v Attributes /t REG_DWORD /d 0xF080004D /f");
        regAdd("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Desktop\\NameSpace\\" + CLSID + " /ve /t REG_SZ /d \"" + name + "\" /f");
        regAdd("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\HideDesktopIcons\\NewStartPanel /v " + CLSID + " /t REG_DWORD /d 0x1 /f");

        /**
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d} /ve /t REG_SZ /d "PowerFolder" /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d}\DefaultIcon /VE /T REG_EXPAND_SZ /D "%%ProgramFiles(x86)%%\PowerFolder.com\PowerFolder\PowerFolder.exe,0" /F
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d} /v System.IsPinnedToNameSpaceTree /t REG_DWORD /d 0x1 /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d} /v SortOrderIndex /t REG_DWORD /d 0x42 /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d}\InProcServer32 /ve /t REG_EXPAND_SZ /d %%systemroot%%\system32\shell32.dll /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d}\Instance /v CLSID /t REG_SZ /d {0E5AAE11-A475-4c5b-AB00-C66DE400274E} /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d}\Instance\InitPropertyBag /v Attributes /t REG_DWORD /d 0x11 /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d}\Instance\InitPropertyBag /v TargetFolderPath /t REG_EXPAND_SZ /d "%userprofile%\PowerFolders" /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d}\ShellFolder /v FolderValueFlags /t REG_DWORD /d 0x28 /f
         reg add HKCU\Software\Classes\CLSID\{5107667c-149a-47c8-b0c9-e4bf9132f17d}\ShellFolder /v Attributes /t REG_DWORD /d 0xF080004D /f
         reg add HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\Desktop\NameSpace\{5107667c-149a-47c8-b0c9-e4bf9132f17d} /ve /t REG_SZ /d "PowerFolder" /f
         reg add HKCU\Software\Microsoft\Windows\CurrentVersion\Explorer\HideDesktopIcons\NewStartPanel /v {5107667c-149a-47c8-b0c9-e4bf9132f17d} /t REG_DWORD /d 0x1 /f
         **/
    }

    public static void uninstall() throws IOException {
        regDelete("HKCU\\Software\\Classes\\CLSID\\" + CLSID + " /f");
        regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\Desktop\\NameSpace\\" + CLSID + " /f");
        regDelete("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Explorer\\HideDesktopIcons\\NewStartPanel /f");
    }

    private static void regAddCLSID(String param) throws IOException {
        regAdd("HKCU\\Software\\Classes\\CLSID\\" + CLSID + param);
    }

    private static void regAdd(String param) throws IOException {
        String command = "reg add " + param;
        Runtime.getRuntime().exec(command);
    }


    private static void regDelete(String param) throws IOException {
        String command = "reg delete " + param;
        Runtime.getRuntime().exec(command);
    }

    public static void main(String... args) throws IOException {
        Path installPath = WinUtils.getProgramInstallationPath();
        Spacetree st = new Spacetree("PowerFolder",
                installPath.resolve("PowerFolder.exe"),
                Paths.get("%userprofile%\\PowerFolders")
        );
        st.uninstall();
        st.install();
    }
}
