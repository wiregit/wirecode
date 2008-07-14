/**
 * $RCSfile: Jingle.java,v $
 * $Revision: 1.3 $
 * $Date: 2008-07-14 19:23:00 $
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * An Jingle sub-packet, which is used by XMPP clients to exchange info like
 * descriptions and transports. <p/> The following link summarizes the
 * requirements of Jingle IM: <a
 * href="http://www.jabber.org/jeps/jep-0166.html">Valid tags</a>.
 * <p/>
 * <p/> Warning: this is an non-standard protocol documented by <a
 * href="http://www.jabber.org/jeps/jep-0166.html">JEP-166</a>. Because this is
 * a non-standard protocol, it is subject to change.
 *
 * @author Alvaro Saurin
 */
public class Jingle extends IQ {

    // static

    public static final String NAMESPACE = "http://jabber.org/protocol/jingle";

    public static final String NODENAME = "jingle";

    // non-static

    private String sid; // The session id

    private Action action; // The action associated to the Jingle

    private String initiator; // The initiator as a "user@host/resource"

    private String responder; // The responder

    // Sub-elements of a Jingle object.

    private Content content;

    private JingleContentInfo contentInfo;

    public Jingle(Action action, Content content) {
        this(content);
        this.action = action;
    }
    
    /**
     * A constructor where the main components can be initialized.
     */
    public Jingle(Content content, final JingleContentInfo mi,
                  final String sid) {
        super();

        this.content = content;

        setContentInfo(mi);
        setSid(sid);

        // Set null all other fields in the packet
        initiator = null;
        responder = null;
        action = null;
    }

    /**
     * Constructor with a description.
     *
     * @param descr a description
     */
    public Jingle(final Content content) {
        super();

        this.content = content;

        // Set null all other fields in the packet
        initiator = null;
        responder = null;

        // Some default values for the most common situation...
        // TODO 
        // TODO action = Jingle.Action.TRANSPORTINFO;
        // TODO action = Jingle.Action.DESCRIPTIONINFO;
        this.setType(IQ.Type.SET);
    }

    /**
     * Constructor with a content info.
     *
     * @param info The content info
     */
    public Jingle(final JingleContentInfo info) {
        super();

        setContentInfo(info);

        initiator = null;
        responder = null;

        action = Jingle.Action.SESSIONINFO;
        this.setType(IQ.Type.SET);
    }

    /**
     * A constructor where the action can be specified.
     *
     * @param action The action.
     */
    public Jingle(final Jingle.Action action) {
        this(null, null, null);
        this.action = action;

        // In general, a Jingle with an action is used in a SET packet...
        this.setType(IQ.Type.SET);
    }

    /**
     * A constructor where the session ID can be specified.
     *
     * @param sid The session ID related to the negotiation.
     * @see #setSid(String)
     */
    public Jingle(final String sid) {
        this(null, null, sid);
    }

    /**
     * The default constructor
     */
    public Jingle() {
        super();
    }

    /**
     * Set the session ID related to this session. The session ID is a unique
     * identifier generated by the initiator. This should match the XML Nmtoken
     * production so that XML character escaping is not needed for characters
     * such as &.
     *
     * @param sid the session ID
     */
    public final void setSid(final String sid) {
        this.sid = sid;
    }

    /**
     * Returns the session ID related to the session. The session ID is a unique
     * identifier generated by the initiator. This should match the XML Nmtoken
     * production so that XML character escaping is not needed for characters
     * such as &.
     *
     * @return Returns the session ID related to the session.
     * @see #setSid(String)
     */
    public String getSid() {

        return sid;
    }

    /**
     * Returns the XML element name of the extension sub-packet root element.
     * Always returns "jingle"
     *
     * @return the XML element name of the packet extension.
     */
    public static String getElementName() {
        return NODENAME;
    }

    /**
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is always
     * "http://jabber.org/protocol/jingle"
     *
     * @return the XML namespace of the packet extension.
     */
    public static String getNamespace() {
        return NAMESPACE;
    }

    /**
     * @return the audioInfo
     */
    public JingleContentInfo getContentInfo() {
        return contentInfo;
    }

    /**
     * @param contentInfo the audioInfo to set
     */
    public void setContentInfo(final JingleContentInfo contentInfo) {
        this.contentInfo = contentInfo;
    }

    /**
     * Get the action specified in the packet
     *
     * @return the action
     */
    public Action getAction() {
        return action;
    }

    /**
     * Set the action in the packet
     *
     * @param action the action to set
     */
    public void setAction(final Action action) {
        this.action = action;
    }

    /**
     * Get the initiator. The initiator will be the full JID of the entity that
     * has initiated the flow (which may be different to the "from" address in
     * the IQ)
     *
     * @return the initiator
     */
    public String getInitiator() {
        return initiator;
    }

    /**
     * Set the initiator. The initiator must be the full JID of the entity that
     * has initiated the flow (which may be different to the "from" address in
     * the IQ)
     *
     * @param initiator the initiator to set
     */
    public void setInitiator(final String initiator) {
        this.initiator = initiator;
    }

    /**
     * Get the responder. The responder is the full JID of the entity that has
     * replied to the initiation (which may be different to the "to" addresss in
     * the IQ).
     *
     * @return the responder
     */
    public String getResponder() {
        return responder;
    }

    /**
     * Set the responder. The responder must be the full JID of the entity that
     * has replied to the initiation (which may be different to the "to"
     * addresss in the IQ).
     *
     * @param resp the responder to set
     */
    public void setResponder(final String resp) {
        responder = resp;
    }

    public Content getContent() {
        return content;
    }
    
    public void setContent(Content content) {
        this.content = content;
    }

    /**
     * Get a hash key for the session this packet belongs to.
     *
     * @param sid       The session id
     * @param initiator The initiator
     * @return A hash key
     */
    public static int getSessionHash(final String sid, final String initiator) {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (initiator == null ? 0 : initiator.hashCode());
        result = PRIME * result + (sid == null ? 0 : sid.hashCode());
        return result;
    }

    /**
     * Return the XML representation of the packet.
     *
     * @return the XML string
     */
    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(getElementName());
        buf.append(" xmlns=\"").append(getNamespace()).append("\"");
        if (getInitiator() != null) {
            buf.append(" initiator=\"").append(getInitiator()).append("\"");
        }
        if (getResponder() != null) {
            buf.append(" responder=\"").append(getResponder()).append("\"");
        }
        if (getAction() != null) {
            buf.append(" action=\"").append(getAction()).append("\"");
        }
        if (getSid() != null) {
            buf.append(" sid=\"").append(getSid()).append("\"");
        }
        buf.append(">");
        //TODO Update to accept more than one content per session (XEP-0166)

        if(content != null) {
            buf.append(content.toXML());
        }

        // and the same for audio jmf info
        if (contentInfo != null) {
            buf.append(contentInfo.toXML());
        }

        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

    /**
     * The "action" in the jingle packet, as an enum.
     */
    public static enum Action {

        CONTENTACCEPT, CONTENTADD, CONTENTMODIFY,
        CONTENTREMOVE, CONTENTREPLACE, DESCRIPTIONINFO, SESSIONACCEPT,
        SESSIONINFO, SESSIONINITIATE,
        SESSIONTERMINATE, TRANSPORTACCEPT, 
        TRANSPORTINFO;

        private static String names[] = {"content-accept", "content-add", "content-modify",
                "content-remove", "content-replace","description-info", "session-accept", "session-info", "session-initiate",
                "session-terminate", "transport-accept", 
                "transport-info"};

        /**
         * Returns the String value for an Action.
         */

        public String toString() {
            return names[this.ordinal()];
        }

        /**
         * Returns the Action for a String value.
         */
        public static Action getAction(String str) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(str)) return Action.values()[i];
            }
            return null;
        }

    }

}
