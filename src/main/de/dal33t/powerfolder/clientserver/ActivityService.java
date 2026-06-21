/*
 * Copyright 2004 - 2024 Christian Sprajc. All rights reserved.
 * Copyright 2024 - 2026 EINBERG UG (haftungsbeschränkt). All rights reserved.
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
 */
package de.dal33t.powerfolder.clientserver;

import de.dal33t.powerfolder.domain.NewsItem;
import de.dal33t.powerfolder.light.AccountInfo;

import java.util.Collection;

/**
 * PFS-2391
 *
 * @author <a href="mailto:totmacher@powerfolder.com">Christian Sprajc</a>
 * @version $Revision: 1.5 $
 */
public interface ActivityService {
    final static long RESULTS_DEFAULT = 100;
    final static long RESULTS_UNLIMITED = -1;
    /** Upper bound for the full, paged activity feed (endless scrolling). */
    final static long RESULTS_FEED_MAX = 5000;

    /**
     * Retrieve activity/news items from THIS cluster node only.
     *
     * @param forAccount the account to retrieve the activity for (the logged-in account)
     * @param maxResults the number of maximum results
     *
     * @return the log according to the current filter settings.
     */
     Collection<NewsItem> getNewsFromLocalServer(AccountInfo forAccount, long maxResults);

    /**
     * Central call: retrieve and summarize activity/news items across ALL
     * nodes of the own cluster (federation is not included). Implementations
     * fan out to each cluster node's {@link #getNewsFromLocalServer} and aggregate the
     * results.
     *
     * @param forAccount the account to retrieve the activity for (the logged-in account)
     * @param maxResults the number of maximum results per node
     *
     * @return the summarized log across the whole cluster.
     */
     Collection<NewsItem> getNewsFromAllClusterNodes(AccountInfo forAccount, long maxResults);
}
