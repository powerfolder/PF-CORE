/*
 * Copyright 1999-2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.threadpool;

import java.util.LinkedList;

import de.dal33t.powerfolder.util.Logger;

/** 
 * A multithreaded blocking queue which is very useful for 
 * implementing producer-consumer style threading patterns.
 * <p>
 * Multiple blocking threads can wait for items being added
 * to the queue while other threads add to the queue.
 * <p>
 * Non blocking and timout based modes of access are possible as well.
 *
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision: 1.1 $
 */
public class MTQueue {

    /** The Log to which logging calls will be made. */
    private Logger log = Logger.getLogger(MTQueue.class);


    private LinkedList list = new LinkedList();
    private long defaultTimeout = 10000;

    public MTQueue() {
    }

    /**
     * Returns the current number of object in the queue
     */
    public synchronized int size() {
        return list.size();
    }

    /** 
     * adds a new object to the end of the queue.
     * At least one thread will be notified.
     */
    public synchronized void add(Object object) {
        list.add( object );
        notify();
    }

    /** 
     * Removes the first object from the queue, blocking until one is available.
     * Note that this method will never return null and could block forever.
     */
    public synchronized Object remove() {
        while (true) {
            Object answer = removeNoWait();
            if ( answer != null ) {
                return answer;
            }
            try {
                wait( defaultTimeout );
            }
            catch (InterruptedException e) {
                log.error( "Thread was interrupted: " + e, e );
            }
        }
    }

    /** 
     * Removes the first object from the queue, blocking only up to the given
     * timeout time.
     */
    public synchronized Object remove(long timeout) {
        Object answer = removeNoWait();
        if (answer == null) {
            try {
                wait( timeout );
            }
            catch (InterruptedException e) {
                log.error( "Thread was interrupted: " + e, e );
            }
            answer = removeNoWait();
        }
        return answer;
    }

    /** 
     * Removes the first object from the queue without blocking.
     * This method will return immediately with an item from the queue or null.
     * 
     * @return the first object removed from the queue or null if the
     * queue is empty
     */
    public synchronized Object removeNoWait() {
        if ( ! list.isEmpty() ) {
            return list.removeFirst();
        }
        return null;
    }

}
