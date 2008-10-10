package org.limewire.xmpp.client.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatStateListener;
import org.jivesoftware.smackx.ChatStateManager;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.User;

class PresenceImpl implements Presence {

    private static final Log LOG = LogFactory.getLog(PresenceImpl.class);

    protected final org.jivesoftware.smack.packet.Presence presence;
    protected final org.jivesoftware.smack.XMPPConnection connection;
    protected final User user;

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence, org.jivesoftware.smack.XMPPConnection connection, User user) {
        this.presence = presence;
        this.connection = connection;
        this.user = user;
    }

    public MessageWriter createChat(final MessageReader reader) {
        if(LOG.isInfoEnabled()) {
            LOG.info("new chat with " + getJID());
        }
        final Chat chat = connection.getChatManager().createChat(StringUtils.parseBareAddress(getJID()), new ChatStateListener() {
            public void processMessage(Chat chat, Message message) {
                reader.readMessage(message.getBody());
            }

            public void stateChanged(Chat chat, org.jivesoftware.smackx.ChatState state) {
                reader.newChatState(ChatState.valueOf(state.toString()));
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
            
            public void setChatState(ChatState chatState) throws XMPPException {
                try {
                    ChatStateManager.getInstance(connection).setCurrentState(org.jivesoftware.smackx.ChatState.valueOf(chatState.toString()), chat);
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
                    if(chat.getParticipant().equals(getJID())) {
                        if(LOG.isInfoEnabled()) {
                            LOG.info("new incoming chat with " + getJID());
                        }
                        final MessageWriter writer = new MessageWriter() {
                            public void writeMessage(String message) throws XMPPException {
                                try {
                                    chat.sendMessage(message);
                                } catch (org.jivesoftware.smack.XMPPException e) {
                                    throw new XMPPException(e);
                                }
                            }

                            public void setChatState(ChatState chatState) throws XMPPException {
                                try {
                                    ChatStateManager.getInstance(connection).setCurrentState(org.jivesoftware.smackx.ChatState.valueOf(chatState.toString()), chat);
                                } catch (org.jivesoftware.smack.XMPPException e) {
                                    throw new XMPPException(e);
                                }
                            }
                        };
                        final MessageReader reader = listener.incomingChat(writer);
                        // TODO race condition
                        chat.addMessageListener(new ChatStateListener() {
                            public void processMessage(Chat chat, Message message) {
                                reader.readMessage(message.getBody());
                            }

                            public void stateChanged(Chat chat, org.jivesoftware.smackx.ChatState state) {
                                reader.newChatState(ChatState.valueOf(state.toString()));
                            }
                        });
                    }
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
        return presence.getMode() != null ? Mode.valueOf(presence.getMode().toString()) : Mode.available;
    }

    public String toString() {
        // TODO add StringUtils.toString(object, Method...)
        return super.toString();
    }

    @Override
    public User getUser() {
        return user;
    }
}
