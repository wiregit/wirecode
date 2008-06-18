package org.limewire.xmpp.client;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.packet.Message;

public class PresenceImpl implements Presence {
    protected final org.jivesoftware.smack.packet.Presence presence;
    protected final XMPPConnection connection;

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence, XMPPConnection connection) {
        this.presence = presence;
        this.connection = connection;        
    }

    public MessageWriter newChat(final MessageReader reader) {
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
}
