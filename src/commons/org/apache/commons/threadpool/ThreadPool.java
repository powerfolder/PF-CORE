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
 * An interface representing some kind of thread pool which allows
 * asynchronous dispatching of Runnable tasks. It is the responsibility
 * of the Runnable task to handle exceptions gracefully. Any non handled
 * exception will typically just be logged. 
 * Though a ThreadPool implementation could have some custom Exception handler
 * 
 * @author <a href="mailto:jstrachan@apache.org">James Strachan</a>
 * @version $Revision: 1.1 $
 */
public interface ThreadPool {

    /** 
     * Dispatch a new task onto this pool 
     * to be invoked asynchronously later.
     */
    public void invokeLater(Runnable task);
}
