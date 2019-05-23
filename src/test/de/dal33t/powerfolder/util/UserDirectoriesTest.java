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
package de.dal33t.powerfolder.util;

import static com.liferay.nativity.util.OSDetector.isWindows;
import static de.dal33t.powerfolder.util.UserDirectories.getDesktopDirectory;
import static de.dal33t.powerfolder.util.UserDirectories.getDocumentsReported;
import static de.dal33t.powerfolder.util.UserDirectories.getMusicReported;
import static de.dal33t.powerfolder.util.UserDirectories.getPicturesReported;
import static de.dal33t.powerfolder.util.UserDirectories.getUserDirectories;
import static de.dal33t.powerfolder.util.UserDirectories.getUserDirectoriesFiltered;
import static de.dal33t.powerfolder.util.UserDirectories.getVideosReported;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

import de.dal33t.powerfolder.Controller;
import de.dal33t.powerfolder.disk.Folder;
import de.dal33t.powerfolder.disk.FolderRepository;

public class UserDirectoriesTest {
    private Controller mockedController = mock(Controller.class);
    private Properties mockedProperties = mock(Properties.class);
    private FolderRepository mockedFolderRepository = mock(FolderRepository.class);
    private Folder mockedFolder = mock(Folder.class);
    private Path mockedPath = mock(Path.class);

    @Test
    public void testGetUserDirectoriesFilteredShouldReturnEmptyCollection() {
        when(mockedController.getConfig()).thenReturn(mockedProperties);
        when(mockedProperties.getProperty(anyString())).thenReturn("false");
        assertTrue(getUserDirectoriesFiltered(mockedController, false).isEmpty());
    }

    @Test
    public void testGetUserDirectoriesFiltered() {
        when(mockedController.getConfig()).thenReturn(mockedProperties);
        when(mockedProperties.getProperty(anyString())).thenReturn("true");
        when(mockedController.getFolderRepository()).thenReturn(mockedFolderRepository);
        when(mockedFolderRepository.getFolders()).thenReturn(Arrays.asList(mockedFolder));
        when(mockedFolder.getLocalBase()).thenReturn(mockedPath);
        //when(mockedPath.equals()).thenReturn(true);
        assertFalse(getUserDirectoriesFiltered(mockedController, true).isEmpty());
    }

    @Test
    public void shouldGetDesktopDirectory() {
        assertNotNull(getDesktopDirectory());
    }

    @Test
    public void testGetUserDirectoriesShouldReturnEmpty() {
        when(mockedController.getConfig()).thenReturn(mockedProperties);
        when(mockedProperties.getProperty(anyString())).thenReturn("false");
        Map<String, UserDirectory> userDirectories = getUserDirectories(mockedController);
        assertTrue(userDirectories.isEmpty());
    }

    @Test
    public void testGetUserDirectories() {
        when(mockedController.getConfig()).thenReturn(mockedProperties);
        when(mockedProperties.getProperty(anyString())).thenReturn("true");
        Map<String, UserDirectory> userDirectories = getUserDirectories(mockedController);
        assertFalse(userDirectories.isEmpty());
    }

    @Test
    public void testWindowsDirectories() {
        assumeTrue(isWindows());
        assertNotNull(getDocumentsReported());
        assertNotNull(getMusicReported());
        assertNotNull(getVideosReported());
        assertNotNull(getPicturesReported());
    }

    @Test
    public void testDirectoriesNotInwindows() {
        assumeFalse(isWindows());
        assertNull(getDocumentsReported());
        assertNull(getVideosReported());
        assertNull(getPicturesReported());
        assertNull(getMusicReported());
    }
}
