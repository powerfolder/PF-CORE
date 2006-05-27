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
/**
 * The idea is that the end-user can write a implementation of
 * this class that allows them have a deterministic for the
 * events that are monitored for. I.e. they can change the state
 * of other parts of their system (or not).
 *
 * One implementation of this is logging....
 * @see CommonsLoggingThreadPoolMonitor
 * @see NullThreadPoolMonitor
 */
public interface ThreadPoolMonitor {
    void handleThrowable(Class clazz, Runnable runnable, Throwable t);
}
