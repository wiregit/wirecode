package org.limewire.facebook.service;

public interface ChatClientFactory {
    ChatClient createChatClient(FacebookFriendConnection connection); 
}
