
package de.dal33t.powerfolder.net;

/* 
 * The DynDns interface is designed to provide a common protocol 
 * for objects that wish to update DynDns IP address. 
 * For example, DynDns is implemented by class DynDnsOrg.
 * 
 * @author Albena Roshelova
 */


public interface DynDns {
    
    /*
     * Update the host IP with the DynDns
     * 
     * @param   updateData: the DynDns account data (username, password, host).
     * @return  the server response.
     */
   public int update(DynDnsUpdateData updateData);   
   
   /*
    * The method gets the user data: username, password, host
    * 
    * @return  
    */
   
   public DynDnsUpdateData getDynDnsUpdateData();
   
   
   public void setDynDnsManager(DynDnsManager manager);
   
   
   /*
    * Gets the DynDns error
    * 
    * @return  the error text.
    */
   public String getErrorText();
   
   
   /*
    * Gets the DynDns error
    * 
    * @return the error text.
    */
   public String getErrorShortText();
   
}