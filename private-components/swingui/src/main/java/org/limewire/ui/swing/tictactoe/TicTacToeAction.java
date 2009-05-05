package org.limewire.ui.swing.tictactoe;

import java.awt.event.ActionEvent;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Responds to the right click action from a friend's name in the My Library 
 * when you challenge a friend to play Tic Tac Toe. You can only challenge the 
 * friend if he supports Tic Tac Toe. Upon the select action, publishing
 * <code>CreateTicTacToeFrameEvent</code> tells the <code>ConversationPane</code> 
 * to create the Tic Tac Toe frame.
 *
 */
public class TicTacToeAction extends AbstractAction {

    private Friend friend;
    private final ChatFrame chatFrame;


    @Inject
    TicTacToeAction(ChatFrame chatFrame) {
        super(I18n.tr("Challenge to a &Tic-Tac-Toe game"));
        this.chatFrame = chatFrame;
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        final Friend friend = getFriend();
        if (friend != null) {
            chatFrame.setVisibility(true);
            chatFrame.fireConversationStarted(friend.getId());
            //If you are initiating a game of Tic Tac Toe, then you are O and go second 
            
            new CreateTicTacToeFrameEvent(friend.getId(), friend.getFirstName(), false).publish();

        } 
        
    }

    /** Sets a new friend for this action. */
    public void setFriend(Friend friend) {
        this.friend = friend;
        setEnabled(friend != null);
    }

    protected Friend getFriend() {
        return friend;
    }




}
