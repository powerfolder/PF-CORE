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
    FACEBOOK("http://www.facebook.com/sharer.php?u=$ORIGINAL_URL$", false),

    /**
     * To share a link on Twitter
     */
    TWITTER(
        "http://twitter.com/?status=Share, send and sync your files online with $APPNAME$: $ORIGINAL_URL$",
        true),

    /**
     * Same for Linkedin.com
     */
    LINKEDIN(
        "http://www.linkedin.com/shareArticle?mini=true&url=$ORIGINAL_URL$&title=Securely send, share and sync files. Work together online with $APPNAME$&summary=Securely send and share files. Work together online with $APPNAME$",
        true),

    /**
     * Y!
     */
    YAMMER(
        "https://www.yammer.com/home/bookmarklet?t=$TITLE$&u=$ORIGINAL_URL$",
        false),

    /**
     * Good old email
     */
    EMAIL(
        "mailto:to@email.com?SUBJECT=Share, send and sync your files online with $APPNAME$&BODY=Share, send and sync your files online with $APPNAME$: %20$ORIGINAL_URL$",
        false);

    private String template;
    private boolean replaceSpace;

    private SocialNetwork(String template, boolean replaceSpace) {
        this.template = template;
        this.replaceSpace = replaceSpace;
    }

    public String shareLink(String shareURL, String title) {
        try {
            String link = template.replace("$ORIGINAL_URL$",
                URLEncoder.encode(shareURL, "UTF-8"));
            if (StringUtils.isNotBlank(title)) {
                link = link.replace("$TITLE$",
                    URLEncoder.encode(title, "UTF-8"));
            } else {
                link = link.replace("$TITLE$", "");
            }
            if (replaceSpace) {
                link = link.replace(" ", "%20");
            }

            String appname = Translation.getTranslation("general.application.name");
            if (StringUtils.isNotBlank(appname)) {
                link = link.replace("$APPNAME$",
                    URLEncoder.encode(appname, "UTF-8"));
            } else {
                link = link.replace("$APPNAME$", "");
            }

            return link;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
