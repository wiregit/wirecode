package org.limewire.ui.swing.friends.chat;

import org.limewire.xmpp.api.client.MessageWriter;

public interface  TicTacToePaneFactory {
    TicTacToePane create(MessageWriter writer, ChatFriend chatFriend, String loggedInID);
}
