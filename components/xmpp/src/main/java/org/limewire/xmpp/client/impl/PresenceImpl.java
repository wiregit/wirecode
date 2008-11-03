package org.limewire.xmpp.client.impl;

import java.net.URI;
import java.util.Map;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ChatStateListener;
import org.jivesoftware.smackx.ChatStateManager;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.io.Address;
import org.limewire.listener.EventListenerList;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.ChatState;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPException;

class PresenceImpl implements Presence {

    private static final Log LOG = LogFactory.getLog(PresenceImpl.class);

    private final org.jivesoftware.smack.packet.Presence presence;
    private final org.jivesoftware.smack.XMPPConnection connection;
    private final User user;
    private Map<URI, Feature> features;
    private EventListenerList<FeatureEvent> featureListeners;

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence,
                 org.jivesoftware.smack.XMPPConnection connection, User user) {
        this.presence = presence;
        this.connection = connection;
        this.user = user;
        features = new ConcurrentHashMap<URI, Feature>();
        featureListeners = new EventListenerList<FeatureEvent>();
    }

    PresenceImpl(org.jivesoftware.smack.packet.Presence presence,
                 org.jivesoftware.smack.XMPPConnection connection, PresenceImpl currentPresence) {
        this(presence, connection, currentPresence.getUser());
        this.features = currentPresence.features;
        this.featureListeners = currentPresence.featureListeners;
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

    public Collection<Feature> getFeatures() {
        return features.values();
    }

    public Feature getFeature(URI id) {
        return features.get(id);
    }

    public boolean hasFeatures(URI... id) {
        for(URI uri : id) {
            if(getFeature(uri) == null) {
                return false;
            }
        }
        return true;
    }

    public Friend getFriend() {
        return user;
    }

    public String getPresenceId() {
        return getJID();
    }

    public Address getPresenceAddress() {
        return null;
    }

    public ListenerSupport<FeatureEvent> getFeatureListenerSupport() {
        return featureListeners;
    }

    public void addFeature(Feature feature) {
        features.put(feature.getID(), feature);
        featureListeners.broadcast(new FeatureEvent(feature, Feature.EventType.FEATURE_ADDED));
    }

    public void removeFeature(URI id) {
        Feature feature = features.remove(id);
        if(feature != null) {
            featureListeners.broadcast(new FeatureEvent(feature, Feature.EventType.FEATURE_REMOVED));
        }
    }
}
