/*
* Copyright 2004 - 2008 Christian Sprajc. All rights reserved.
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
* $Id: Bench.java 8022 2009-05-21 07:46:07Z harry $
*/
package de.dal33t.powerfolder.bench;

import java.util.Map;

/**
 * Base class for bench tests. Implement getResults() to return the various
 * tests. Implement winnerIsGreatest() true if the greates rusult is the winner.
 */
public abstract class Bench {

    /**
     * Run all tests and get results. Then find out which got the best result.
     */
    public final void run() {
        Map<String, Comparable> map = getResults();
        Comparable winner = null;
        String winnerName = "";
        for (String name : map.keySet()) {
            Comparable comparable = map.get(name);
            System.out.println(name + " = " + comparable);
            if (winner == null || (winnerIsGreatest()
                    ? comparable.compareTo(winner) > 0
                    : winner.compareTo(comparable) > 0)) {
                winner = comparable;
                winnerName = name;
            }
        }
        System.out.println("-------");
        System.out.println("Winner is: " + winnerName);
    }

    /**
     * Implement to return a Map of test names with results of the test.
     *
     * @return
     */
    protected abstract Map<String, Comparable> getResults();

    /**
     * Implement to return true if the winner is the one with the greatest
     * result.
     *
     * @return
     */
    protected abstract boolean winnerIsGreatest();

}
