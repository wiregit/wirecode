/**
 * $RCSfile: AudioRTPDescription.java,v $
 * $Revision: 1.2 $
 * $Date: 2008-07-01 20:44:40 $
 *
 * Copyright 2003-2005 Jive Software.
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
package org.jivesoftware.smackx.packet.audiortp;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.jingle.audiortp.AudioRTPContentHandler;
import org.jivesoftware.smackx.jingle.JingleContentHandler;
import org.jivesoftware.smackx.jingle.audiortp.PayloadType;
import org.jivesoftware.smackx.packet.Description;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Jingle content description.
 *
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public class AudioRTPDescription extends Description {
    
    public static final String NAMESPACE = "http://jabber.org/protocol/jingle/description/audio";
    
    protected String profile;

    /**
     * Creates a content description..
     */
    public AudioRTPDescription() {
        super();
    }

    protected Attribute[] getAttributes() {
        return profile != null ? new Attribute[]{new Attribute("profile", profile)} : new Attribute[]{};
    }

    public String getNamespace() {
        return NAMESPACE;
    }
    
    public AudioRTPDescription(final JinglePayloadType pt) {
        super();
        addJinglePayloadType(pt);
    }

    public JingleContentHandler createContentHandler() {
        return new AudioRTPContentHandler();
    }
    
    public void addAllSupportedPayloads() {
        addAudioPayloadTypes(((AudioRTPContentHandler)getContentHandler()).getSupportedPayloads());
    }

    /**
     * Adds a audio payload type to the packet.
     *
     * @param pt the audio payload type to add.
     */
    public void addJinglePayloadType(final JinglePayloadType pt) {
        children.add(pt);
    }

    /**
     * Adds a list of payloads to the packet.
     *
     * @param pts the payloads to add.
     */
    public void addAudioPayloadTypes(final List<PayloadType.Audio> pts) {
        Iterator ptIter = pts.iterator();
        while (ptIter.hasNext()) {
            PayloadType.Audio pt = (PayloadType.Audio) ptIter.next();
            addJinglePayloadType(new JinglePayloadType(pt));
        }
    }

    /**
     * Returns an Iterator for the audio payloads in the packet.
     *
     * @return an Iterator for the audio payloads in the packet.
     */
    public Iterator getJinglePayloadTypes() {
        return Collections.unmodifiableList(getJinglePayloadTypesList()).iterator();
    }

    /**
     * Returns a list for the audio payloads in the packet.
     *
     * @return a list for the audio payloads in the packet.
     */
    public ArrayList getJinglePayloadTypesList() {
        return (ArrayList)children;
    }

    /**
     * Return the list of Payload types contained in the description.
     *
     * @return a list of PayloadType.Audio
     */
    public ArrayList<PayloadType.Audio> getAudioPayloadTypesList() {
        ArrayList<PayloadType.Audio> result = new ArrayList<PayloadType.Audio>();
        Iterator jinglePtsIter = getJinglePayloadTypes();

        while (jinglePtsIter.hasNext()) {
            JinglePayloadType jpt = (JinglePayloadType) jinglePtsIter.next();
            if (jpt instanceof JinglePayloadType) {
                result.add(jpt.getPayloadType());
            }
        }

        return result;
    }

    /**
     * Returns a count of the audio payloads in the Jingle packet.
     *
     * @return the number of audio payloads in the Jingle packet.
     */
    public int getJinglePayloadTypesCount() {
        synchronized (children) {
            return children.size();
        }
    }

    /**
     * A payload type, contained in a descriptor.
     *
     * @author Alvaro Saurin
     */
    public static class JinglePayloadType implements PacketExtension {

        public static final String NODENAME = "payload-type";

        private PayloadType.Audio payload;

        /**
         * Create a payload type.
         *
         * @param payload the payload
         */
        public JinglePayloadType(final PayloadType.Audio payload) {
            super();
            this.payload = payload;
        }

        /**
         * Create an empty payload type.
         */
        public JinglePayloadType() {
            this(null);
        }

        /**
         * Returns the XML element name of the element.
         *
         * @return the XML element name of the element.
         */
        public String getElementName() {
            return NODENAME;
        }

        public String getNamespace() {
            return NAMESPACE;
        }

        /**
         * Get the payload represented.
         *
         * @return the payload
         */
        public PayloadType.Audio getPayloadType() {
            return payload;
        }

        /**
         * Set the payload represented.
         *
         * @param payload the payload to set
         */
        public void setPayload(final PayloadType.Audio payload) {
            this.payload = payload;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();

            if (payload != null) {
                buf.append("<").append(getElementName()).append(" ");

                // We covert here the payload type to XML
                if (payload.getId() != PayloadType.INVALID_PT) {
                    buf.append(" id=\"").append(payload.getId()).append("\"");
                }
                if (payload.getName() != null) {
                    buf.append(" name=\"").append(payload.getName()).append("\"");
                }
                if (payload.getChannels() != 0) {
                    buf.append(" channels=\"").append(payload.getChannels()).append("\"");
                }
                if (payload.getChannels() != 0) {
                    buf.append(" clockrate=\"").append(payload.getClockRate()).append("\"");
                }
                buf.append("/>");
            }
            return buf.toString();
        }
    }
}
