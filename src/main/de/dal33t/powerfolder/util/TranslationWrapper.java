package de.dal33t.powerfolder.util;

/** TranslationWrapper
 * Wrapper to access static classes from Velocity templates.
 * @author <a href="mailto:kappel@powerfolder">Christoph Kappel</a>
 */

public class TranslationWrapper {

    /** TranslationWrapper.TranslationWrapper
     * Get translation of given string id
     * @param  id  Translation string id
     */

    public String get(String id)
    {
        return Translation.get(id);
    }
}
