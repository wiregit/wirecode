/**
 * $RCSfile: JingleMediaManager.java,v $
 * $Revision: 1.1.2.1 $
 * $Date: 2008-05-27 19:39:56 $11-07-2006
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

import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.JingleSession;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides necessary Jingle Session jmf methods and behavior.
 * <p/>
 * The goal of this class is to provide a flexible way to make JingleManager control jmf streaming APIs without implement them.
 * For instance you can implement a file transfer using java sockets or a VOIP Media Manager using JMF.
 * You can implement many JingleMediaManager according to you necessity.
 *
 * @author Thiago Camargo
 */
public abstract class JingleMediaManager {

    /**
     * Return all supported Payloads for this Manager
     *
     * @return The Payload List
     */
    public abstract List<PayloadType> getPayloads();

    /**
     * Returns the Preferred PayloadType of the Media Manager
     *
     * @return The PayloadType
     */
    public PayloadType getPreferredPayloadType() {
        return getPayloads().size() > 0 ? getPayloads().get(0) : null;
    }

    /**
     * Create a Media Session Implementation
     *
     * @param payloadType
     * @param remote
     * @param local
     * @return
     */
    public abstract JingleMediaSession createMediaSession(PayloadType payloadType, final TransportCandidate remote, final TransportCandidate local, JingleSession jingleSession);

  }