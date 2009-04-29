package org.limewire.ui.swing.friends.chat;

import java.beans.PropertyChangeListener;

import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.Presence.Mode;

/**
 * Defines the interface for a friend as represented in the UI for chat. The 
 * friend's ID is his full email address (for example, 'userName@host.com'), whereas
 * his name could be 'Steve', or whatever you have set for him.
 * @author Mario Aquino, Object Computing, Inc.
 *
 */
public interface ChatFriend {

    /**
     * @return User object used by this chat friend
     */
    User getUser();

    /**
     * @return the user id corresponding to this chat friend.  This is typically the id
     * used by this chat friend to sign on (for example 'userName@host.com').
     */
    String getID();

    /**
     * This is the nick name you have set for a friend, as defined in XMPP. You 
     * can set this either through your email account settings, or when you added
     * the friend through LimeWire. For example, this could return 'Made Up' for 
     * getID of 'userName@host.com'.
     * 
     * @return The display String identifying this chat friend
     */
    String getName();

    /**
     * The status message is a custom message the friend can set. For example, 
     * the friend might set his status as 'surfing the web' in his chat program.
     * The status might be an empty string.
     * 
     * @return the status message
     */
    String getStatus();

    /**
     * @return the presence status ("available", "do not disturb", etc)
     */
    Mode getMode();

    /**
     * @return true if this chat has been marked as started, but has
     * not been stopped.
     */
    boolean isChatting();

    /**
     * Returns true if the current chat is the active chat. An active conversation
     * means that the chat is visible to the end user
     *
     * @return true if the current chat is active
     */
    boolean isActiveConversation();

    /**
     * Sets the active status of the current chat
     *
     * @param active true to set the conversation as active
     */
    void setActiveConversation(boolean active);

    /**
     * @return true if any presences of the user are signed in thru LimeWire
     */
    boolean isSignedInToLimewire();

    /**
     * @return true if this chat friend is currently signed in
     */
    boolean isSignedIn();

    /**
     * If not yet started, marks the current chat as started
     */
    void startChat();

    /**
     * If chat is currently started, marks the chat as stopped
     */
    void stopChat();

    /**
     * Gets the time at which the chat started.
     * For example, normally a chat can be considered started upon the first sign
     * of communication between the current connection and this chat user
     *
     * @return start chat time in milliseconds
     */
    long getChatStartTime();

    /**
     *
     * @return whether or not this chat user has received messages that have
     * yet to be displayed in the chat window
     */
    boolean hasReceivedUnviewedMessages();

    /**
     * Set whether or not this chat user has received messages that have yet to be displayed
     * in the chat window
     *
     * @param hasMessages true if this chat user has received messages not yet displayed
     */
    void setReceivedUnviewedMessages(boolean hasMessages);

    /**
     * Creates and wires together the necessary objects for
     * sending and receiving messages.
     *
     * @param reader the chat implementation calls into the {@link MessageReader} upon
     *        receiving messages and updates in chat state
     * @return messageWriter {@link MessageWriter} implementation on which methods are called to send messages
     * and update chat state.
     */
    MessageWriter createChat(MessageReader reader);

    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);

    /**
     * Updates the state of this chat friend based on its underlying attributes, for instance 
     * the mode and status of the current active presence
     */
    void update();
}
