/*
 * ID3v2UserDefinedURL.java
 *
 * Created on March 17, 2003, 11:29 AM
 */

package helliker.id3;

/**
 *
 * @author  Jan van Oosterom
 */
public class ID3v2UserDefinedURL {
    private String url;
    private String description;
    /** Creates a new instance of ID3v2UserDefinedURL */
    public ID3v2UserDefinedURL() {
    }
    
    public void setDescription(String description) {        
        this.description = description;
    }    
    
    public void setURL(String url) {                
        this.url = url;
    }
    
    public String getURL() {
        return url;
    }
    
}
