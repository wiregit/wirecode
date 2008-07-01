/**
 * $RCSfile: MediaReceivedListener.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-07-01 20:44:40 $11-07-2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle.media;

/**
 * Listener for new Incoming Media Streams
 */
public interface MediaReceivedListener {

    /**
     * Called when new Media is received.
     */
    public void mediaReceived(String participant);

}
