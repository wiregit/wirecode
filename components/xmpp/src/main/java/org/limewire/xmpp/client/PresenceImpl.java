package org.limewire.xmpp.client;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class PresenceImpl implements Presence {

    private static final Log LOG = LogFactory.getLog(PresenceImpl.class);

    protected final org.jivesoftware.smack.packet.Presence presence;
    protected final org.jivesoftware.smack.XMPPConnection connection;

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence, org.jivesoftware.smack.XMPPConnection connection) {
        this.presence = presence;
        this.connection = connection;        
    }

    public MessageWriter createChat(final MessageReader reader) {
        LOG.info("new chat with " + getJID());
        final Chat chat = connection.getChatManager().createChat(getJID(), new MessageListener() {
            public void processMessage(Chat chat, Message message) {
                reader.readMessage(message.getBody());
            }
        });
        return new MessageWriter() {
            public void writeMessage(String message) throws XMPPException {
                try {
                    chat.sendMessage(message);
                } catch (org.jivesoftware.smack.XMPPException e) {
                    throw new XMPPException(e);
                }
            }
        };
    }

    public void setIncomingChatListener(final IncomingChatListener listener) {
        connection.getChatManager().addChatListener(new ChatManagerListener() {
            public void chatCreated(final Chat chat, boolean createdLocally) {
                if(!createdLocally) {
                    LOG.info("new incoming chat with " + getJID());
                    final MessageWriter writer = new MessageWriter() {
                        public void writeMessage(String message) throws XMPPException {
                            try {
                                chat.sendMessage(message);
                            } catch (org.jivesoftware.smack.XMPPException e) {
                                throw new XMPPException(e);
                            }
                        }
                    };
                    final MessageReader reader = listener.incomingChat(writer);
                    chat.addMessageListener(new MessageListener() {
                        public void processMessage(Chat chat, Message message) {
                            reader.readMessage(message.getBody());
                        }                        
                    });
                }
            }
        });
    }

    public String getJID() {
        return presence.getFrom();
    }

    public Type getType() {
        return Type.valueOf(presence.getType().toString());
    }

    public String getStatus() {
        return presence.getStatus();
    }

    public int getPriority() {
        return presence.getPriority();
    }

    public Mode getMode() {
        return Mode.valueOf(presence.getMode().toString());
    }

    public String toString() {
        // TODO add StringUtils.toString(object, Method...)
        return super.toString();
    }
}
