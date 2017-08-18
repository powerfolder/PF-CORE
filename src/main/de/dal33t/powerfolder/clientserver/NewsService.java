/*
 * Copyright 2004 - 20017 Christian Sprajc. All rights reserved.
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
 * $Id: FolderService.java 20771 2013-02-05 12:01:32Z krickl $
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.domain.NewsItem;

import java.util.Collection;

/**
 * PFS-2391
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface NewsService {
    final static long RESULTS_DEFAULT = 25;
    final static long RESULTS_UNLIMITED = -1;

    /**
     * @param filterAccountOID only show news of the given account, null for any
     * @param maxResults the number of maximum results
     *
     * @return the log according to the current filter settings.
     */
     Collection<NewsItem> getNews(String filterAccountOID, long maxResults);
}
