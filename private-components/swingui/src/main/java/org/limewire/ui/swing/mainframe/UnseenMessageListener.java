package org.limewire.ui.swing.mainframe;
/**
 * Defines the interface for how the UI should react to messages. 
 */
public interface UnseenMessageListener {
    /**
     * Call for all messages sent from someone which you receive.
     * @param senderId the id of the person who sent the message
     * @param chatIsVisible whether the chat UI is presently shown; not currently in use
     */
    void messageReceivedFrom(String senderId, boolean chatIsVisible);
    
    /**
     * Call when the user has clicked to show an incoming message. Once the 
     * message is seen, remove it as an unseen message.
     * @param chatId the id of the person who sent the message 
     */
    void conversationSelected(String chatId);
    
    /**
     * Clears all previous messages received from one or multiple people. You 
     * should use this method when you sign off.
     */
    void clearUnseenMessages();
}
