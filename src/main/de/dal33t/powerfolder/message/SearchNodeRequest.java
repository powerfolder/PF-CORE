// $Id: SearchNodeRequest.java,v 1.1 2006/03/06 22:40:31 bytekeeper Exp $
package de.dal33t.powerfolder.message;

/**
 * A search request for nodes.
 * 
 * @author Dennis "Dante" Waldherr
 * @version $Revision: 1.1 $
 */
public class SearchNodeRequest extends Message {
    private static final long serialVersionUID = 100L;

    public String searchString;
    
    public SearchNodeRequest(String searchString) {
        this.searchString = searchString;
    }

    public SearchNodeRequest() {
    }
    
    public String toString() {
        return "Search for '" + searchString + "'";
    }
}
