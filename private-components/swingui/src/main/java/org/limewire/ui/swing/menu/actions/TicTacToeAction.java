package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.ChatFrame;
import org.limewire.ui.swing.friends.chat.CreateTicTacToeFrameEvent;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class TicTacToeAction extends AbstractAction {

    private Friend friend;
//    private final TicTacToeMigLayout tictactoePane;
    private final ChatFrame chatFrame;


    /**
     * Need to send the friendsPanel, and then when you make a challenge to play tic tac toe, 
     * show the pane/fireconversation started. basically you need the ConversationPane running so the interceptor
     * can intercept.
     */

    @Inject
    TicTacToeAction(ChatFrame chatFrame) {
//    TicTacToeAction(TicTacToeMigLayout tictactoePane, ChatFrame chatFrame) {
        super(I18n.tr("Challenge to a &Tic-Tac-Toe game"));
//        this.tictactoePane = tictactoePane;
        this.chatFrame = chatFrame;
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        System.out.println("In TicTacToeAction#actionPerformed friend: " + getFriend().getId());
//        new TicTacToeCreatePanelEvent(getFriend()).publish();


        final Friend friend = getFriend();
        if (friend != null) {
//            tictactoePane.fireGameStarted(friend.getId());
            chatFrame.setVisibility(true);
            chatFrame.fireConversationStarted(friend.getId());
            //If you are initiating a game of Tic Tac Toe, then you are O and go second 
            
            System.out.println("TicTacToeAction: initiate game with " + friend.getId());
            new CreateTicTacToeFrameEvent(friend.getId(), friend.getFirstName(), false).publish();


//            chatFrame.setVisibility(true);
//            chatFrame.fireConversationStarted(friend.getName());
//
//        
//            System.out.println("friend getName: " + friend.getName());
//            System.out.println("friend getId(): " + friend.getId());
//            System.out.println("friend getFirstName(): " + friend.getFirstName());
//            System.out.println("friend getRenderName(): " + friend.getRenderName());
//            
//            new CreateTicTacToeFrameEvent(friend.getName(), tictactoePane).publish();
//            JFrame frame = new JFrame();
//            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//            frame.setResizable(true);
//            frame.setTitle("Tic Tac Toe with " + friend.getId());            
//            frame.getContentPane().setLayout(new BorderLayout());
//            frame.getContentPane().add(tictactoePane, BorderLayout.CENTER);            
//            frame.pack();
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//            
//            frame.addWindowListener(new WindowAdapter() {
//                //If I challenged a friend, but he rejected the offer, don't call exitGame
//                public void windowClosed(WindowEvent e) {
//                    tictactoePane.exitGame();                
//                }            
//            });
//            
        } 
        
    }

    /** Sets a new friend for this action and updates the enabledness. */
    public void setFriend(Friend friend) {
        this.friend = friend;
        setEnabled(friend != null);
    }

    protected Friend getFriend() {
        return friend;
    }




}
