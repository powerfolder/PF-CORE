package helliker.id3;
/*
 * ID3v2Comment.java
 *
 * Created on March 14, 2003, 10:52 AM
 */



/**
 * Note that this a a data class only, changes with not be set to the ID3v2Tag.
 * Call the method MP3File.setComment() for that.
 * @author  Jan van Oosterom
 */
public class ID3v2Comment {
    private String language;
    private String description;
    private String text;
    
    /** Creates a new instance of ID3v2Comment */
    public ID3v2Comment() {
    }
    
    public void setDescription(String description) {        
        this.description = description;
    }
    
    public void setLanguage(String language) {        
        this.language = language;
    }
    
    public void setText(String text) {        
        this.text = text;
    }
    
    public String getText() {
        return text;
    }
}
