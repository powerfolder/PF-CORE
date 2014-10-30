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
 * $Id: MP3FileInfo.java 7864 2009-05-02 20:37:15Z bytekeeper $
 */
package de.dal33t.powerfolder.light;

/**
 * Detailed file info for a MP3 file
 * 
 * @author <a href="mailto:jan@van-oosterom.demon.nl">Jan van Osterom </a>
 * @version $Revision: 1.2 $
 */
@Deprecated
public class MP3FileInfo extends FileInfo {
    // For backward compatability.
    private static final long serialVersionUID = -9162087423525905833L;
    /** length of the mp3 song eg "3:45" in min:sec */
    private String length;
    /** biterate of the MP3 song * */
    private int bitrate;
    /** samplerate of the MP3 song * */
    private int samplerate;
    /** is this stereo?* */
    private boolean isStereo;

    /** ID3 tag information the title of the song * */
    private String title;
    /** ID3 tag information the artist * */
    private String artist;
    /** ID3 tag information the album * */
    private String album;

    private boolean isID3InfoValid = true;

    /** Serialization constructor */
    public MP3FileInfo() {
        super();
    }

    /**
     * @return Returns the album.
     */
    public String getAlbum() {
        return album;
    }

    /**
     * @return Returns the artist.
     */
    public String getArtist() {
        return artist;
    }

    /**
     * @return Returns the bitrate.
     */
    public int getBitrate() {
        return bitrate;
    }

    /**
     * @return Returns the isID3InfoValid.
     */
    public boolean isID3InfoValid() {
        return isID3InfoValid;
    }

    /**
     * @return Returns the isStereo.
     */
    public boolean isStereo() {
        return isStereo;
    }

    /**
     * @return Returns the length.
     */
    public String getLength() {
        return length;
    }

    /**
     * @return Returns the samplerate.
     */
    public int getSamplerate() {
        return samplerate;
    }

    /**
     * @return Returns the title.
     */
    public String getTitle() {
        return title;
    }
}