package org.limewire.ui.swing.friends.chat;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.util.TicTacToeBoard;
import org.limewire.util.TicTacToeJudge;
import org.limewire.util.TicTacToeWinner;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.IncomingChatListener;
import org.limewire.xmpp.api.client.MessageReader;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.Presence;
import org.limewire.xmpp.api.client.User;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPException;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;

@Singleton
public class TicTacToeMigLayout extends JPanel {
    
    private TicTacToeBoard board;    
    
    private String myID;
    private TicTacToeFriend friend;
    private static Map<String, TicTacToeFriend> idToFriendMap = null;
    private final EventList<TicTacToeFriend> tictactoeFriends;

    private MessageWriter messageWriter;//for sending your move, TicTacToeMessageWriter
    
    private boolean isX = false;
    private TicTacToeResult lastGameStatus = TicTacToeResult.DRAW;
            
    ///////////Ernie's work
    private JPanel gamePanel = new JPanel();
    
    private TicTacToeJButton[] gameButtons;
    
    private JLabel playerLabel = new JLabel();
    private JLabel statusText = new JLabel();
    private JButton playAgainButton = new JButton();
    
    public TicTacToeMigLayout() {
        this(null);
        System.out.println("in TicTacToeMigLayout default constructor");        
    }
        
    public TicTacToeMigLayout(@Assisted MessageWriter writer) {
        
        this.idToFriendMap = new HashMap<String, TicTacToeFriend>();
        this.tictactoeFriends = new BasicEventList<TicTacToeFriend>();

                       
        board = new TicTacToeBoard();
        
        //Ernie's work
        setLayout(new MigLayout(
                "insets 12 12 6 12,fill",
                "[left]",                         // col constraints
                "[top]12[top]6[top]12[bottom]")); // row constraints
            
        initGamePanel();
        
        playerLabel.setText("player");
        
        statusText.setText("Make a move");
        
        playAgainButton.setVisible(false);
        playAgainButton.setText("Play again");
        
        add(gamePanel, "center,wrap");
        add(playerLabel, "wrap");
        add(statusText, "wrap");
        add(playAgainButton, "center");
    
        EventAnnotationProcessor.subscribe(this);
                    
    }

    @Inject void register(ListenerSupport<XMPPConnectionEvent> connectionSupport,
            ListenerSupport<FileOfferEvent> fileOfferEventListenerSupport,
            ListenerSupport<FriendPresenceEvent> presenceSupport) {

        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch(event.getType()) {
                case DISCONNECTED:
                    break;
                }
            }
        });

        presenceSupport.addListener(new EventListener<FriendPresenceEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendPresenceEvent event) {
                handlePresenceEvent(event);
            }
        });
        
    }

    private void initGamePanel() {
        gamePanel.setBackground(Color.BLACK);
        gamePanel.setLayout(new MigLayout(
            "insets 0 0 0 0,fill",
            "[left]6[left]6[left]",  // col constraints
            "[top]6[top]6[top]"));   // row constraints
        
        gameButtons = new TicTacToeJButton[9];
        for (int i = 0; i < gameButtons.length; i++) {
            gameButtons[i] = new TicTacToeJButton(i);
            gameButtons[i].setPreferredSize(new Dimension(60, 60));
            
            // Change button appearance. 
            //gameButtons[i].setContentAreaFilled(false);
            //gameButtons[i].setOpaque(true);
            //gameButtons[i].setBackground(Color.LIGHT_GRAY);
            
            gamePanel.add(gameButtons[i], ((i % 3) == 2) ? "wrap" : null);
        }
        
        AddCellActions();
        initialCellAppearance();

    }
    
    public void setWriter(MessageWriter writer) {
        if(writer == null) {
            System.out.println("that writer is null in miglayout");
        } else {
            System.out.println("set that writer in miglayout");
        }
        messageWriter = writer;
    }
        
    public void setPlayerX(boolean isX) {
        this.isX = isX;
        if(isX) {
            playerLabel.setText("You are X");
        } else {
            playerLabel.setText("You are O");
        }
    }
    boolean isPlayerX() {
        return isX;
    }
    
    public void setLoggedInID(String id) {
        this.myID = id;
    }

    String getLoggedInID() {
        return myID;
    }
    //When the friend is accepted, set the writer. important note for the documentation

    public void setFriend(String friendId) {
        
        friend = idToFriendMap.get(friendId);
        if(friend != null) { 
            if(!createWriter()) {
                System.out.println("couldn't create the writer 1");
            }
        }
    }

    //When the friend is accepted, set the writer. important note for the documentation
    public void setFriend(TicTacToeFriend friend) {
        this.friend = friend;
        if(!createWriter()) {
            System.out.println("couldn't create the writer 2");
        }

    }

    /**
     * Remove from the friends list only when:
     *
     * 1. The user (buddy) associated with the tictactoefriend is no longer signed in, AND
     * 2. The chat has been closed (by clicking on the "x" on the friend in the friend's list)
     *
     * @param tictactoeFriend the ChatFriend to decide whether to remove (no null check)
     * @return true if chatFriend should be removed.
     */
    private boolean shouldRemoveFromFriendsList(TicTacToeFriend tictactoeFriend) {
        return (!tictactoeFriend.isPlaying()) && (!tictactoeFriend.isSignedIn());
    }
    
    //TODO: it's all about the presence ... do we want a tictactoefriend or just a chat friend?
    private void handlePresenceEvent(FriendPresenceEvent event) {
        final Presence presence = (Presence)event.getSource();
        final User user = presence.getUser();
        TicTacToeFriend tictactoeFriend = idToFriendMap.get(user.getId());
        
        switch(event.getType()) {
        case ADDED:
            if(tictactoeFriend == null) {
                tictactoeFriend = new TicTacToeFriendImpl(presence);
                tictactoeFriends.add(tictactoeFriend);
                idToFriendMap.put(user.getId(), tictactoeFriend);
                
            } 

            final TicTacToeFriend tictactoeFriendForIncomingMove = tictactoeFriend;
            
            //TODO: still using the word chat which is confusing since it's not chat's
            //that it's listening for.
            IncomingChatListener incomingChatListener = new IncomingChatListener() {
                public MessageReader incomingChat(MessageWriter writer) {

                    return new TicTacToeMessageReader(tictactoeFriendForIncomingMove);
                }
            };
            user.setChatListenerIfNecessary(incomingChatListener);
            tictactoeFriend.update();
            break;
        case UPDATE:
            if (tictactoeFriend != null) {
                tictactoeFriend.update();
            }
            break;
        case REMOVED:
            if (tictactoeFriend != null) {
                if (shouldRemoveFromFriendsList(tictactoeFriend)) {
                    tictactoeFriends.remove(idToFriendMap.remove(user.getId()));
                }
                tictactoeFriend.update();
            }
            break;
        }
    }

    //TicTacToeAction calls this method
    public void challengeExtended(String friendId) {

        setPlayerX(false);
        yourTurn(false);
        board.initialize();

        clearButtonText();
        initialCellAppearance();                  

        yourTurn(false);
        if (isX) {
            playerLabel.setText("You are X");
        } else {
            playerLabel.setText("You are O");
        }
        
        TicTacToeFriend tictactoeFriend = idToFriendMap.get(friendId);
        if(tictactoeFriend != null) {            
            setFriend(tictactoeFriend);
            String message = new String();
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.INITIATE_GAME;
                        
            sendMessage(message);        
        }
    }
    
    void sendMessage(String message) {
        try {   
            if(messageWriter == null ) {
                if(!createWriter()) {
                    return;
                }
                messageWriter.writeMessage(message);
            }else {
                messageWriter.writeMessage(message);
            }
        } catch(XMPPException x) {
            x.printStackTrace();
        }        
    }
    
    void sendMessage(String message, TicTacToeFriend friend) {
        if(friend == null) { 
            sendMessage(message);
            return;
        }
        if(messageWriter == null) {
            if(!createWriter()) {
                System.out.println("didn't create writer cause friend is null-2");
                return;
            }
        }
        try {   
            messageWriter.writeMessage(message);
        } catch(XMPPException x) {
            x.printStackTrace();
        }
    }    
    
    public void fireGameStarted(String friendId) {
        
        ChatFriend tttFriend = idToFriendMap.get(friendId);
        if(tttFriend != null) {
            extendChallenge(tttFriend);
        }
    }

    /**
     * The tic tac toe game uses the conversationpane to send messages back and forth. Therefore, when you start a game
     * you need to kick off a ConversationSelectedEvent.
     * @param tttFriend who you challenged to play a game
     */
    private void extendChallenge(ChatFriend tttFriend) {
        MessageWriter writerWithEventDispatch = null;
        if (tttFriend.isSignedIn()) {
            MessageWriter writer = tttFriend.createChat(new MessageReaderImpl(tttFriend));
            writerWithEventDispatch = new MessageWriterImpl(myID, tttFriend, writer);
        }
        new ConversationSelectedEvent(tttFriend, writerWithEventDispatch, true).publish();
    }    
    
    
    boolean createWriter() {
        if(friend != null) {
            MessageWriter writer = friend.createTicTacToe(new TicTacToeMessageReader(friend));                        
            messageWriter = new TicTacToeMessageWriter(friend.getID(), friend, writer);  
            return true;
        } else {
            return false;
        }
                
    }             

    public boolean createWriter(TicTacToeFriend myfriend) {
        friend = myfriend;
        if(friend != null) {
            MessageWriter writer = friend.createTicTacToe(new TicTacToeMessageReader(friend));
            messageWriter = new TicTacToeMessageWriter("local id", friend, writer);           
            System.out.println("create that writer 2");
            return true;
        } else {
            return false;
        }
    }

    /**
     * notified from the frame that my window is closed. meaning, i don't want to 
     * play a game. send a message to your buddy 
     */
    
    public void exitGame() {
        String message = new String();
        message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.NO_THANKS_GAME;
        
        TicTacToeJudge judge = new TicTacToeJudge();

        TicTacToeWinner winner = judge.result(board);
        //did you quit the game early, before it was decided who won/lost or a draw
        if(winner == TicTacToeWinner.NO_ONE_YET && board.cellsSelected() != 0) {
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.EXIT_GAME;
        }                       
                
        sendMessage(message, friend);
    }

    /**
     * 
     * @param message from friend, this string includes 'tictactoe-' then a 
     * Set X/O:# example: 'tictactoe-Set X:1'
     * 
     * @return 0 - 9 which is the cell to mark as a move
     */
    int parseMessage(String message) {
        String shortMove;
        int index = message.indexOf(":");
        
        shortMove = message.substring(index + 1, index + 1 + 1);
        return Integer.parseInt(shortMove);
    }

    //The @EventSubscriber handle methods are to handle internal events        
        
    /**
     * These events are intercepted form ConversationPane which catches from 
     * what your friend sent. When caught here, this updates your board
     * before letting you make a move.
     * <p>
     * Only the move, or special messages are sent. Win, lose or draw are
     * decided locally.
     * 
     * @param event
     */
    @EventSubscriber
    public void handleTicTacToeSelectedEventFromFriend(TicTacToeSelectedEventFromFriend event) {   
        
        String tttMessage = event.getMessage().toString();        
        System.out.println("message from friend: " + tttMessage);
        if(tttMessage.toString().indexOf(TicTacToeMessages.PLAY_AGAIN) > -1) {
            showPlayAgain(false);
        }

        if(tttMessage.toString().indexOf(TicTacToeMessages.NO_THANKS_GAME) > -1) {
            statusText.setText("Friend not interested");
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.EXIT_GAME) > -1) {
            statusText.setText("Friend quit the game");            
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.O_MOVE) > -1) {
            System.out.println("handleTicTacToeSelectedEventFromFriend 1: " + tttMessage);

            markFriendsMove(parseMessage(tttMessage));
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.X_MOVE) > -1) {
            System.out.println("handleTicTacToeSelectedEventFromFriend 2: " + tttMessage);
            //parse statusText for move
            markFriendsMove(parseMessage(tttMessage));
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.DRAW) > -1) {
            statusText.setText(TicTacToeMessages.DRAW);
            hideDraw();
            showPlayAgain(false);            
        }       
    }
            
    @EventSubscriber
    public void handleTicTacToeSelectedEvent(TicTacToeEvent event) {   
        
        //Need to translate message to a move
        String tttMessage = event.getMessage().toString();        
        
        if(tttMessage.toString().indexOf(TicTacToeMessages.O_MOVE) > -1) {
            statusText.setText(TicTacToeMessages.X_NEXT_MOVE);
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.X_MOVE) > -1) {
            statusText.setText(TicTacToeMessages.O_NEXT_MOVE);
        }
    }
    
    //Tic Tac Toe UI related methods, like which status message to show, which cells to mark, etc.
    /**
     * my friend's move
     * @param cellNum
     */
    public void markFriendsMove(int cellNum) {
              

        //need to switch on the actual cell, need to know if it's x
        //or o
        //X goes first
//        System.out.println("cells selected: " + board.cellsSelected() + " friend: is x: " + isPlayerX() + " mod: " + (board.cellsSelected() % 2));

        if(board.cellsSelected() % 2 == 0) {    
            xMove(cellNum);
        } else {
            
            oMove(cellNum);
        }
        
        TicTacToeJudge judge = new TicTacToeJudge();

        TicTacToeWinner winner = judge.result(board);
        //if not yet, or a draw, someone won
        if(winner == TicTacToeWinner.DRAW) {
            statusText.setText(TicTacToeMessages.DRAW);
            enableButtons(false);
            hideDraw();
            
            String message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.DRAW;
            statusText.setText(TicTacToeMessages.DRAW);
            sendMessage(message, friend);

        } else if(winner != TicTacToeWinner.NO_ONE_YET) {
            hideNonWin(winner);
            if(winner == TicTacToeWinner.X_ACROSS_1 || winner == TicTacToeWinner.X_ACROSS_2 || winner == TicTacToeWinner.X_ACROSS_3
                    || winner == TicTacToeWinner.X_DOWN_1 || winner == TicTacToeWinner.X_DOWN_2
                    || winner == TicTacToeWinner.X_DOWN_3 || winner == TicTacToeWinner.X_DIAG_1 
                    || winner == TicTacToeWinner.X_DIAG_2) {
                //x won
                statusText.setText(TicTacToeMessages.X_WON);
            } else {
                //o won
                statusText.setText(TicTacToeMessages.O_WON);
            } 
            //you lost, your friend won
            lastGameStatus = TicTacToeResult.LOST;
            enableButtons(false);
        } else {
            yourTurn(true);
        }
    }

    /**
     * my move
     * @param cellNum
     */
    public void makeMove(int cellNum) {
        
        String message = new String();
        message = TicTacToeMessages.TICTACTOE;
               
        //need to switch on the actual cell, need to know if it's x
        //or o
        System.out.println("TicTacToeMigLayout cells selected: " + board.cellsSelected() + " is x: " + isPlayerX() + " mod: " + (board.cellsSelected() % 2));
        if(board.cellsSelected() % 2 == 0) {    
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.X_MOVE + cellNum;
            if(!xMove(cellNum)) {
                //you made a bad pick, pick again.
                return;
            }
        } else {
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.O_MOVE + cellNum;
           
            if(!oMove(cellNum)) {
                //you made a bad pick, pick again
                return;
            }
        }
        yourTurn(false);
        determineWinner(message);  
        
    }
    /**
     * 
     * @param cellNum
     * @return
     */
    boolean xMove(int cellNum) {
        if(false == board.setX(cellNum)) {
            //couldn't set value
            statusText.setText(TicTacToeMessages.BAD_PICK);
            return false;
        } 
        setX(cellNum);
        if(isPlayerX()) {
            yourTurn(true);
        }
        return true;        
    }
    /**
     * 
     * @param cellNum
     * @return
     */
    boolean oMove(int cellNum) {

        if(false == board.setO(cellNum)) {
            //couldn't set value
            statusText.setText(TicTacToeMessages.BAD_PICK);
            return false;
        }
        setO(cellNum);
        if(!isPlayerX()) {
            yourTurn(false);
        }
        return true;

    }
    
    /**
     * called by me for my move.
     * @param theMove
     */
    void determineWinner(String theMove) {
        TicTacToeJudge judge = new TicTacToeJudge();
        
        TicTacToeWinner winner = judge.result(board);
        hideNonWin(winner);
        if(winner == TicTacToeWinner.X_ACROSS_1 ||
                winner == TicTacToeWinner.X_ACROSS_2 ||
                winner == TicTacToeWinner.X_ACROSS_3 ||
                winner == TicTacToeWinner.X_DOWN_1 ||
                winner == TicTacToeWinner.X_DOWN_2 ||
                winner == TicTacToeWinner.X_DOWN_3 ||
                winner == TicTacToeWinner.X_DIAG_1 ||
                winner == TicTacToeWinner.X_DIAG_2
                ) {
            //let the friend figure out that i won
            sendMessage(theMove, friend);
            statusText.setText(TicTacToeMessages.X_WON);
            lastGameStatus = TicTacToeResult.WON;

//            move = 10;//game over
            //only let the person who lost play again
            //the winner sees the play again screen so the loser can pick first
            showPlayAgain(true);
            return;
        }
        
        if(winner == TicTacToeWinner.O_ACROSS_1 ||
                winner == TicTacToeWinner.O_ACROSS_2 ||
                winner == TicTacToeWinner.O_ACROSS_3 ||
                winner == TicTacToeWinner.O_DOWN_1 ||
                winner == TicTacToeWinner.O_DOWN_2 ||
                winner == TicTacToeWinner.O_DOWN_3 ||
                winner == TicTacToeWinner.O_DIAG_1 ||
                winner == TicTacToeWinner.O_DIAG_2) {

            lastGameStatus = TicTacToeResult.WON;

            sendMessage(theMove, friend);

            statusText.setText(TicTacToeMessages.O_WON);            
//            move = 10;//game over
            showPlayAgain(true);
            
            return;
        }
        if(winner == TicTacToeWinner.DRAW) {
            //send the last pick, friend wills end back that it's a draw
            sendMessage(theMove, friend);
        }
        if(winner == TicTacToeWinner.NO_ONE_YET) {
            sendMessage(theMove, friend);
        }           
       
//        move = board.cellsSelected();
    }
    
    void showPlayAgain(boolean showChallenge) {
        if(showChallenge) {
            playAgainButton.setVisible(true);
            playAgainButton.setText("Play again");
        } else {
            playAgainButton.setVisible(true);
            playAgainButton.setText("Again");            
        }
    }
    
//UI methods for buttons, etc.
    
    void clearButtonText() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].setText("");
        }
    }

  void initialCellAppearance() {
      for(int i = 0; i < 9; i++) {
          gameButtons[i].initial();
      }

  }
    
    void AddCellActions() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].addActionListener(new TicTacToeCellAction(i));            
        }
        playAgainButton.addActionListener(new PlayAgainAction());

    }
    private void setX(int cell) {
        gameButtons[cell].setText("X");
    }
    private void setO(int cell) {
        gameButtons[cell].setText("O");
    }

    void playAgainChallengeAccepted() {
        //need to alternate who goes first
        setPlayerX(true);
        yourTurn(true);
        //board need to clear out that array
        board.initialize();

        clearButtonText();
        statusText.setText("Make a pick");
        initialCellAppearance();                  

    }
    //you are challenging the friend to play again
    //You went first the previous time, so now you are o and go second
    //You'll send a play again challenge
    void challengePlayAgain() {
        setPlayerX(false);
        yourTurn(false);
        board.initialize();

        clearButtonText();
        initialCellAppearance();                  

        String message = new String();
        message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.PLAY_AGAIN;
        sendMessage(message, friend);
    }
           
    public void hideDraw() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].loser();
        }             
    }

    public void hideNonWin(TicTacToeWinner winner) {
        if (winner == TicTacToeWinner.X_ACROSS_1 || winner == TicTacToeWinner.O_ACROSS_1) {
                                              
            gameButtons[0].winner();
            gameButtons[1].winner();
            gameButtons[2].winner();

            gameButtons[3].loser();
            gameButtons[4].loser();
            gameButtons[5].loser();
            gameButtons[6].loser();
            gameButtons[7].loser();
            gameButtons[8].loser();
        }
        if (winner == TicTacToeWinner.X_ACROSS_2 || winner == TicTacToeWinner.O_ACROSS_2) {
            
            gameButtons[3].winner();
            gameButtons[4].winner();
            gameButtons[5].winner();

            gameButtons[0].loser();
            gameButtons[1].loser();
            gameButtons[2].loser();
            gameButtons[6].loser();
            gameButtons[7].loser();
            gameButtons[8].loser();
            
        }
        
        if (winner == TicTacToeWinner.X_ACROSS_3 || winner == TicTacToeWinner.O_ACROSS_3) {
            
            gameButtons[6].winner();
            gameButtons[7].winner();
            gameButtons[8].winner();

            gameButtons[0].loser();
            gameButtons[1].loser();
            gameButtons[2].loser();
            gameButtons[3].loser();
            gameButtons[4].loser();
            gameButtons[5].loser();

        }
        if (winner == TicTacToeWinner.X_DOWN_1 || winner == TicTacToeWinner.O_DOWN_1 ) {
            gameButtons[0].winner();
            gameButtons[3].winner();
            gameButtons[6].winner();
            
            gameButtons[1].loser();
            gameButtons[2].loser();
            gameButtons[4].loser();
            gameButtons[5].loser();
            gameButtons[7].loser();
            gameButtons[8].loser();

        }

        if (winner == TicTacToeWinner.X_DOWN_2 || winner == TicTacToeWinner.O_DOWN_2 ) {
            
            gameButtons[1].winner();
            gameButtons[4].winner();
            gameButtons[7].winner();
            
            gameButtons[0].loser();
            gameButtons[2].loser();
            gameButtons[3].loser();
            gameButtons[5].loser();
            gameButtons[6].loser();
            gameButtons[8].loser();
           
        }
        if (winner == TicTacToeWinner.X_DOWN_3 || winner == TicTacToeWinner.O_DOWN_3 ) {
            
            gameButtons[2].winner();
            gameButtons[5].winner();
            gameButtons[8].winner();
            
            gameButtons[0].loser();
            gameButtons[1].loser();
            gameButtons[3].loser();
            gameButtons[4].loser();
            gameButtons[6].loser();
            gameButtons[7].loser();
            
        }
        if (winner == TicTacToeWinner.X_DIAG_1 || winner == TicTacToeWinner.O_DIAG_1 ) {
            
            gameButtons[0].winner();
            gameButtons[4].winner();
            gameButtons[8].winner();
            
            gameButtons[1].loser();
            gameButtons[2].loser();
            gameButtons[3].loser();
            gameButtons[5].loser();
            gameButtons[6].loser();
            gameButtons[7].loser();
            
        }
        if (winner == TicTacToeWinner.X_DIAG_2 || winner == TicTacToeWinner.O_DIAG_2 ) {
            gameButtons[2].winner();
            gameButtons[4].winner();
            gameButtons[6].winner();
            
            gameButtons[0].loser();
            gameButtons[1].loser();
            gameButtons[3].loser();
            gameButtons[5].loser();
            gameButtons[7].loser();
            gameButtons[8].loser();
        }
    }

    public void addTicTacToePanel() {

        gamePanel = new JPanel();
        gamePanel.setBackground(Color.black);
        Dimension size = new Dimension(155,155);
        gamePanel.setPreferredSize(size);        
        gamePanel.setMaximumSize(size);
        gamePanel.setAlignmentX(CENTER_ALIGNMENT);
        
        add(gamePanel, "wrap");
        
        AddCellActions();

        initialCellAppearance();
    }        
    
    
    public void yourTurn(boolean yourGo) {
        if(yourGo) {
            statusText.setText("Make a move");
        } else {
            statusText.setText("Wait your turn");                
        }
        enableButtons(yourGo);
    }
    
    public void enableButtons(boolean show) {
        for(int i = 0; i < 9; i++) { 
            gameButtons[i].setEnabled(show);
        }        
    }
    
    
    public class TicTacToeJButton extends JButton {
        public int cellNum;

        public TicTacToeJButton(int cellNum) {
            super(" ");
            this.cellNum = cellNum;
            //min and max size
            Dimension d = new Dimension(45, 45);
            setMaximumSize(d);
            setMinimumSize(d);  
            setPreferredSize(d);
            setSize(d);       
            setBorderPainted(false);
//            setContentAreaFilled(true);//this will give you the button look
        }
        public void winner() {
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(Color.YELLOW);
        }
        public void loser() {
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(Color.WHITE);
            setEnabled(false);
        }
        
        public void initial() {
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(Color.WHITE);            
        }
    }
    

    public class PlayAgainAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //The person who won the last game can challenge
            //to play again
            boolean makesChallenge = false;
            //if a draw, then O goes first, so X needs to extend challenge
            if(lastGameStatus == TicTacToeResult.DRAW) {
                makesChallenge = isPlayerX();
            } else {
                if(lastGameStatus == TicTacToeResult.WON) {
                    makesChallenge = true;
                }
            }
            
            if (makesChallenge) {
                challengePlayAgain();            
            } else {
                playAgainChallengeAccepted();
            }
            initialCellAppearance();
            clearButtonText();

            playAgainButton.setVisible(false);

        }
    }
    public class TicTacToeCellAction implements ActionListener {
        int cellNum;
        TicTacToeCellAction(int cellNum){
            this.cellNum = cellNum;                        
        }
        /**
         * Need to pass this person's move to the other person.
         * Need to get that person's move and store in board.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            if(friend != null) {
                System.out.println("TicTacToeCellAction#actionPerformed - my friend: " + friend.getID());
            } else {
                System.out.println("TicTacToeCellAction#actionPerformed - my friend is null");
                if(messageWriter == null) {
                    System.out.println("TicTacToeCellAction#actionPerformed - and the writer is null");                    
                } else {
                    System.out.println("TicTacToeCellAction#actionPerformed - and the writer is not null");                    
                }
            }
            makeMove(cellNum);
        }
    }
    public enum TicTacToeResult {
        WON, LOST, DRAW
    }
}
