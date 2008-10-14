package org.limewire.ui.swing.friends;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

import org.jdesktop.beans.AbstractBean;
import org.limewire.core.api.friend.Friend;
import org.limewire.xmpp.api.client.LimePresence;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public class ChatFriendImpl extends AbstractBean implements ChatFriend {
    private boolean chatting;
    private boolean activeConversation;
    private AtomicReference<Presence> presence;
    private String status;
    private Mode mode;
    private long chatStartTime;
    private boolean hasUnviewedMessages;
    
    ChatFriendImpl(Presence presence) {
        this.presence = new AtomicReference<Presence>(presence);
        this.status = presence.getStatus();
        this.mode = presence.getMode();
    }
    
    @Override
    public Friend getFriend() {
        return presence.get().getUser();
    }
    
    @Override
    public String getID() {
        return presence.get().getUser().getId();
    }

    @Override
    public Mode getMode() {
        return mode;
    }
    
    @Override
    public void setMode(Mode mode) {
        Mode oldMode = getMode();
        this.mode = mode;
        firePropertyChange("mode", oldMode, mode);
    }

    @Override
    public String getName() {
        return safe(presence.get().getUser().getName(), presence.get().getUser().getId());
    }
    
    /**
     * Returns <code>str2</code> if <code>str</code> is either
     * null or empty, otherwise returns <code>str</code>. 
     */
    private String safe(String str, String str2) {
        return (str == null || "".equals(str)) ? str2 : str;
    }
    
    @Override
    public String getStatus() {
        return status;
    }
    
    @Override
    public void setStatus(String status) {
        String oldStatus = getStatus();
        this.status = status;
        firePropertyChange("status", oldStatus, status);
    }

    @Override
    public boolean isChatting() {
        return chatting;
    }

    void setChatting(boolean chatting) {
        boolean oldChatting = isChatting();
        this.chatting = chatting;
        firePropertyChange("chatting", oldChatting, chatting);
    }

    @Override
    public MessageWriter createChat(MessageReader reader) {
        return presence.get().createChat(reader);
    }

    @Override
    public void startChat() {
        if (isChatting() == false) {
            chatStartTime = System.currentTimeMillis();
            setChatting(true);
        }
    }

    @Override
    public void stopChat() {
        setChatting(false);
        setActiveConversation(false);
    }

    @Override
    public long getChatStartTime() {
        return chatStartTime;
    }

    @Override
    public boolean isActiveConversation() {
        return activeConversation;
    }
    
    @Override
    public void setActiveConversation(boolean active) {
        boolean oldActiveConversation = activeConversation;
        activeConversation = active;
        firePropertyChange("activeConversation", oldActiveConversation, activeConversation);
    }

    @Override
    public boolean isSignedInToLimewire() {
        return presence.get() instanceof LimePresence;
    }

    @Override
    public boolean isReceivingUnviewedMessages() {
        return hasUnviewedMessages;
    }

    @Override
    public void setReceivingUnviewedMessages(boolean hasMessages) {
        boolean oldHasUnviewedMessages = hasUnviewedMessages;
        hasUnviewedMessages = hasMessages;
        firePropertyChange("receivingUnviewedMessages", oldHasUnviewedMessages, hasMessages);
    }

    @Override
    public boolean jidBelongsTo(String jid) {
        return presence.get().getUser().jidBelongsTo(jid);
    }

    public Presence getPresence() {
        return presence.get();
    }
    
    @Override
    public void releasePresence(Presence presence) {
        if (this.presence.get().getJID().equals(presence.getJID())) {
            this.presence.set(getHighestPriorityPresence());
        }
    }

    private Presence getHighestPriorityPresence() {
        Collection<Presence> values = presence.get().getUser().getPresences().values();
        ArrayList<Presence> presences = new ArrayList<Presence>(values);
        Collections.sort(presences, new PresenceSorter());
        return presences.size() == 0 ? null : presences.get(0);
    }
    
    private static class PresenceSorter implements Comparator<Presence> {
        @Override
        public int compare(Presence o1, Presence o2) {
            return new Integer(o1.getPriority()).compareTo(new Integer(o2.getPriority()));
        }
    }
}
