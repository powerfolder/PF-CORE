/*
 * Copyright 2004 - 2010 Christian Sprajc. All rights reserved.
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
 * $Id: PowerFolder.java 11731 2010-03-14 07:14:34Z harry $
 */
package de.dal33t.powerfolder.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * #2072: Help class to share links on social networks.
 * 
 * @author sprajc
 */
public enum SocialNetwork {

    /**
     * Share on FB.
     */
    FACEBOOK("http://www.facebook.com/sharer.php?u=$ORIGINAL_URL$"),

    /**
     * To share a link on Twitter
     */
    TWITTER(
        "http://twitter.com/?status=Share and send your files online with $ORIGINAL_URL$"),

    /**
     * Same for Linkedin.com
     */
    LINKEDIN(
        "http://www.linkedin.com/shareArticle?mini=true&url=$ORIGINAL_URL$&title=Securely send and share files. Work together online with PowerFolder&summary=Securely send and share files. Work together online with PowerFolder"),

    /**
     * Good old email
     */
    EMAIL(
        "mailto:to@email.com?SUBJECT=Share and send your file online&BODY=Share and send your files with %20$ORIGINAL_URL$");

    private String template;

    private SocialNetwork(String template) {
        this.template = template;
    }

    public String shareLink(String shareURL) {
        try {
            return template.replace("$ORIGINAL_URL$", URLEncoder.encode(
                shareURL, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
