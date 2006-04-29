package de.dal33t.powerfolder.light;

import helliker.id3.*;

import java.io.File;

import de.dal33t.powerfolder.disk.Folder;

/**
 * Detailed file info for a MP3 file
 * 
 * @author <a href="mailto:jan@van-oosterom.demon.nl">Jan van Osterom </a>
 * @version $Revision: 1.2 $
 */
public class MP3FileInfo extends FileInfo {
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
    
    /** contructs a MP3 fileInfo object* */
    public MP3FileInfo(Folder folder, File localFile) {
        super(folder, localFile);
        if (localFile.exists()) {
            if (!localFile.getName().toUpperCase().endsWith(".MP3")) {
                throw new IllegalArgumentException("Not a MP3 file: "
                    + localFile.getName());
            }
            try {
                MP3File mp3File = new MP3File(localFile);
                length = mp3File.getPlayingTimeString();
                bitrate = mp3File.getBitRate();
                samplerate = mp3File.getSampleRate();
                isStereo = mp3File.getMPEGChannelMode().toUpperCase().indexOf(
                    "STEREO") >= 0;
                if (mp3File.id3v1Exists()) {
                    ID3v1Tag id3v1 = mp3File.getID3v1();
                    title = id3v1.getTitle();
                    artist = id3v1.getArtist();
                    album = id3v1.getAlbum();
                }
                if (mp3File.id3v2Exists()) { //v2 tag overwrites v1 tag if
                    // available
                    ID3v2Tag id3v2 = mp3File.getID3v2();
                    try {
                        if (id3v2.getFrameDataString(ID3v2Frames.TITLE)
                            .length() > 0)
                        {
                            title = id3v2.getFrameDataString(ID3v2Frames.TITLE);
                        }
                        if (id3v2.getFrameDataString(
                            ID3v2Frames.LEAD_PERFORMERS).length() > 0)
                        {
                            artist = id3v2
                                .getFrameDataString(ID3v2Frames.LEAD_PERFORMERS);
                        }
                        if (id3v2.getFrameDataString(ID3v2Frames.ALBUM)
                            .length() > 0)
                        {
                            album = id3v2.getFrameDataString(ID3v2Frames.ALBUM);
                        }
                    } catch (ID3v2FormatException id3v2FormatException) {
                    } // on error v1 data is used if not yet overwritten by
                    // valid v2 info
                }
            } catch (Exception e) {
                isID3InfoValid = false;
            }
        }
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