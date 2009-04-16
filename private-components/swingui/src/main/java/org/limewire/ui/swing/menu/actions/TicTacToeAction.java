package org.limewire.ui.swing.menu.actions;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.friends.chat.TicTacToeMigLayout;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class TicTacToeAction extends AbstractAction {

    private Friend friend;
    private final TicTacToeMigLayout tictactoePane;
    

    /**
     * Need to send the friendsPanel, and then when you make a challenge to play tic tac toe, 
     * show the pane/fireconversation started. basically you need the ConversationPane running so the interceptor
     * can intercept.
     * @param tictactoePane
     * @param friendsPanel
     */

    @Inject
    TicTacToeAction(TicTacToeMigLayout tictactoePane) {
        super(I18n.tr("Challenge to a &Tic-Tac-Toe game"));
        this.tictactoePane = tictactoePane;
        setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
//        System.out.println("In TicTacToeAction#actionPerformed friend: " + getFriend().getId());
//        new TicTacToeCreatePanelEvent(getFriend()).publish();

        final Friend friend = getFriend();
        if (friend != null) {
            tictactoePane.fireGameStarted(friend.getId());
        
            JFrame frame = new JFrame();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setResizable(true);
            frame.setTitle("Tic Tac Toe with " + friend.getId());            
            frame.getContentPane().setLayout(new BorderLayout());
            frame.getContentPane().add(tictactoePane, BorderLayout.CENTER);            
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            
            frame.addWindowListener(new WindowAdapter() {
                //If I challenged a friend, but he rejected the offer, don't call exitGame
                public void windowClosed(WindowEvent e) {
                    tictactoePane.exitGame();                
                }            
            });
            
        } 
        
    }

    /** Sets a new friend for this action and updates the enabledness. */
    public void setFriend(Friend friend) {
        this.friend = friend;
        setEnabled(friend != null && !friend.isAnonymous() && !friend.getFriendPresences().isEmpty());
    }

    protected Friend getFriend() {
        return friend;
    }




}
