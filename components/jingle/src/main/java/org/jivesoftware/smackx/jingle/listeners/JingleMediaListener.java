/**
 * $RCSfile: JingleMediaListener.java,v $
 * $Revision: 1.1.2.2 $
 * $Date: 2008-05-29 18:46:39 $11-07-2006
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

package org.jivesoftware.smackx.jingle.listeners;

import org.jivesoftware.smackx.packet.Description;

/**
 * Interface for listening to jmf events.
 * @author Thiago Camargo
 */
public interface JingleMediaListener extends JingleListener {
    /**
     * Notification that the jmf has been negotiated and established.
     *
     * @param description
     */
    public void mediaEstablished(Description description);

    /**
     * Notification that a payload type must be cancelled
     *
     * @param description
     */
    public void mediaClosed(Description description);
}
