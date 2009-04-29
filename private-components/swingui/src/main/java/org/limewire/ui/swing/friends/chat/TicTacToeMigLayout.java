package org.limewire.ui.swing.friends.chat;


import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.util.TicTacToeBoard;
import org.limewire.util.TicTacToeJudge;
import org.limewire.util.TicTacToeWinner;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;

import com.google.inject.Singleton;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.internal.base.Objects;

@Singleton
public class TicTacToeMigLayout extends JPanel {
    
    private TicTacToeBoard board;    
    
    private String myID;
    private String friendsID;
    
    private ChatFriend friend = null;
//    private TicTacToeFriend friend = null;
//    private static Map<String, ChatFriend> idToFriendMap = null;
//    private static Map<String, TicTacToeFriend> idToFriendMap = null;
//    private final EventList<TicTacToeFriend> tictactoeFriends;
//    private final EventList<ChatFriend> tictactoeFriends;

    private MessageWriter messageWriter;//for sending your move, TicTacToeMessageWriter
    
    private boolean isX = false;
            
    ///////////Ernie's work
    private JPanel gamePanel = new JPanel();
    
    private TicTacToeJButton[] gameButtons;
    
    private JLabel playerLabel = new JLabel();
    private JLabel statusText = new JLabel();
    private JButton playAgainButton = new JButton();
    private JButton againButton = new JButton();
    private final int LENGTH = 60;
    private final int WIDTH = 60;
    
       
    public TicTacToeMigLayout(@Assisted MessageWriter writer, ChatFriend chatFriend, String myID, boolean isX ) {
        
//        this.idToFriendMap = new HashMap<String, ChatFriend>();
//        this.tictactoeFriends = new BasicEventList<ChatFriend>();

        
        Objects.nonNull(writer, "writer not set");
        Objects.nonNull(chatFriend, "chat friend not set");
        Objects.nonNull(myID, "my id isn't set");
        Objects.nonNull(isX, "role not set");

        messageWriter = writer;
        friend = chatFriend;
        this.myID = myID;
        friendsID = chatFriend.getName();
        System.out.println("myid: " + myID + " my friend: " + chatFriend.getID() + " my role is x? " + isX);
        setPlayerX(isX);
        
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
        
        againButton.setVisible(false);
        againButton.setText("Again?");

        add(gamePanel, "center,wrap");
        add(playerLabel, "wrap");
        add(statusText, "wrap");
        add(playAgainButton, "center, hidemode 3");
        add(againButton, "center, hidemode 3");
        
        //to be part of the event bus
        EventAnnotationProcessor.subscribe(this);
                    
    }
    //handleCreatePane in ConversationPane calls this method.
    //In NavPanel, if your right click to challenge a friend, this method is eventually called.

    public void fireGameStarted() {
        extendChallenge(friend);
        challengeExtended(isX);
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
                
        sendMessage(message);
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
            gameButtons[i].setPreferredSize(new Dimension(LENGTH, WIDTH));
                        
            gamePanel.add(gameButtons[i], ((i % 3) == 2) ? "wrap" : null);
        }
        
        AddCellActions();
        initialCellAppearance();

    }
    
        
    private void setPlayerX(boolean isX) {
        this.isX = isX;
        if(isX) {
            playerLabel.setText("You are X");
        } else {
            playerLabel.setText("You are O");
        }
    }

    /**
     * Sends a string to your friend's chat frame.
     * @param message
     */
    private void sendMessage(String message) {
        try {   
            messageWriter.writeMessage(message);
        } catch(XMPPException x) {
            x.printStackTrace();
        }        
    }

    //TicTacToeAction calls this method. In NavPanel, if your right click to challenge a friend, this method is eventually called.
    private void challengeExtended(boolean isYourTurn) {
//    public void challengeExtended(ChatFriend tttFriend) {

//        Objects.nonNull(tttFriend, "TicTacToeFriend");
//        setPlayerX(false);
        yourTurn(isYourTurn);
        board.initialize();

        clearButtonText();
        initialCellAppearance();                  

//        yourTurn(false);
        if (isX) {
            playerLabel.setText("You are X");
        } else {
            playerLabel.setText("You are O");
        }

//        TicTacToeFriend tictactoeFriend = idToFriendMap.get(friendId);
//        if(tictactoeFriend != null) {            
//          setFriend(tttFriend);
//            setFriend(tictactoeFriend);
            String message = new String();
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.INITIATE_GAME;
                        
            sendMessage(message);        
//        } 
    
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
   

    /**
     * 
     * @param message from friend, this string includes 'tictactoe-' then a 
     * Set X/O:# example: 'tictactoe-Set X:1'
     * 
     * @return 0 - 9 which is the cell to mark as a move
     */
    private int parseMessage(String message) {
        String shortMove;
        int index = message.indexOf(":");
        
        shortMove = message.substring(index + 1, index + 1 + 1);
        return Integer.parseInt(shortMove);
    }

    //The @EventSubscriber handle methods are to handle internal events        
        
    
    
//    @EventSubscriber
//    public void handleTicTacToeCreatePanelEvent(TicTacToeCreatePanelEvent event) {        
//        System.out.println("In TicTacToeMigLayout#handleChallengeCreateBoard");
//    }

    /**
     * These events are intercepted form ConversationPane which catches from 
     * what your friend sent. When caught here, this updates your board
     * before letting you make a move.
     * <p>
     * Only the move, or special messages are sent. Win, lose or draw are
     * decided locally.
     * <p>
     * Sent from ConversationPane's tictactoeInterceptor.
     * @param event
     */
    @EventSubscriber
    public void handleTicTacToeSelectedEventFromFriend(TicTacToeSelectedEventFromFriend event) {   
        
        //only want to mark messages from my friend
        if(!event.getMessage().getFriendID().matches(friend.getID())) {
            return;
        }

        String tttMessage = event.getMessage().toString();        
//        System.out.println("message from friend: " + tttMessage);
        //sent from my friend challenging me to play again, I need to show the "Again" button
        if(tttMessage.toString().indexOf(TicTacToeMessages.PLAY_AGAIN) > -1) {
            showAgain();
        }
        //friend accepted the challenge
        if(tttMessage.toString().indexOf(TicTacToeMessages.PLAY_AGAIN_YES) > -1) {
            //at this point, you asked to play again, friend said yes, you are x
            playAgainChallengeAccepted();
        }       
        
        if(tttMessage.toString().indexOf(TicTacToeMessages.DRAW) > -1) {
            showAgain();
        }       

        if(tttMessage.toString().indexOf(TicTacToeMessages.NO_THANKS_GAME) > -1) {
            statusText.setText("Friend not interested");
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.EXIT_GAME) > -1) {
            statusText.setText("Friend quit the game");            
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.O_MOVE) > -1) {

            markFriendsMove(tttMessage, false);
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.X_MOVE) > -1) {
            //parse statusText for move
            markFriendsMove(tttMessage, true);
        }
    }
                
    //Tic Tac Toe UI related methods, like which status message to show, which cells to mark, etc.
    /**
     * my friend's move
     * @param cellNum
     */
    private void markFriendsMove(String message, boolean isXMove) {
              
        int cellNum = parseMessage(message);

        if(isXMove) {    
            xMove(cellNum);
        } else {
            
            oMove(cellNum);
        }

        yourTurn(true);
        determineIfFriendWon();

    }

    /**
     * my move
     * @param cellNum
     */
    private void makeMyMove(int cellNum) {
        
        String message = new String();
        message = TicTacToeMessages.TICTACTOE;
               
        if(isX) {
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
        determineIfIWon(message);  
        sendMessage(message);

        
    }
    /**
     * 
     * @param cellNum
     * @return
     */
    private boolean xMove(int cellNum) {
        if(false == board.setX(cellNum)) {
            //couldn't set value
            statusText.setText(TicTacToeMessages.BAD_PICK);
            return false;
        } 
        setX(cellNum);
        return true;        
    }
    /**
     * 
     * @param cellNum
     * @return
     */
    private boolean oMove(int cellNum) {

        if(false == board.setO(cellNum)) {
            //couldn't set value
            statusText.setText(TicTacToeMessages.BAD_PICK);
            return false;
        }
        setO(cellNum);
        
        return true;

    }
    
    private void determineIfFriendWon() {
        
        TicTacToeJudge judge = new TicTacToeJudge();

        TicTacToeWinner winner = judge.result(board);
        //if not yet, or a draw, someone won
        if(winner == TicTacToeWinner.DRAW) {
            statusText.setText(TicTacToeMessages.DRAW);
            enableButtons(false);
            hideDraw();
            statusText.setText(TicTacToeMessages.DRAW);
            showPlayAgain();

        } else if(winner != TicTacToeWinner.NO_ONE_YET) {
            hideNonWin(winner);
            if(winner == TicTacToeWinner.X_ACROSS_1 || winner == TicTacToeWinner.X_ACROSS_2 || winner == TicTacToeWinner.X_ACROSS_3
                    || winner == TicTacToeWinner.X_DOWN_1 || winner == TicTacToeWinner.X_DOWN_2
                    || winner == TicTacToeWinner.X_DOWN_3 || winner == TicTacToeWinner.X_DIAG_1 
                    || winner == TicTacToeWinner.X_DIAG_2) {
                //x won
                if (friendsID != null) {
                    statusText.setText(friendsID + " won");                    
                } else {                   
                    statusText.setText(TicTacToeMessages.X_WON);
                }
            } else {
                //o won
                if (friendsID != null) {
                    statusText.setText(friendsID + " won");                    
                } else {                   
                    statusText.setText(TicTacToeMessages.O_WON);
                }
            } 
            //you lost, your friend won
            showPlayAgain();

            enableButtons(false);
        }
        
    }
    
    /**
     * called by me for my move.
     * @param theMove
     */
    private void determineIfIWon(String theMove) {
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
            
            if(myID != null) {
                statusText.setText(myID + ", you won");                    
            } else {                   
                statusText.setText(TicTacToeMessages.X_WON);
            }

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

            if(myID != null) {
                statusText.setText(myID + ", you won");                    
            } else {                   
                statusText.setText(TicTacToeMessages.O_WON);            
            }
            
            return;
        }
    }
    
    private void showPlayAgain() {
        playAgainButton.setVisible(true);
        
        if(friendsID != null) {
            playAgainButton.setText("Play " + friendsID + " again");
        } else {
            playAgainButton.setText("Play again");            
        }
    }
    private void showAgain() { 
        if(friendsID != null) {
                againButton.setText(friendsID + " wants to play again");
        } else {
            againButton.setText("Again?");            
        }

        againButton.setVisible(true);
    }
    
//UI methods for buttons, etc.
    
    private void clearButtonText() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].setText("");
        }
    }

    private void initialCellAppearance() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].initial();
        }

    }
    
    void AddCellActions() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].addActionListener(new TicTacToeCellAction(i));            
        }
        playAgainButton.addActionListener(new PlayAgainAction());
        againButton.addActionListener(new AgainAction());        
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
        setPlayerX(true);
        yourTurn(true);
        board.initialize();

        clearButtonText();
        initialCellAppearance();                  

        String message = new String();
        message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.PLAY_AGAIN;
        sendMessage(message);
    }
           
    private void hideDraw() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].loser();
        }             
    }

    private void hideNonWin(TicTacToeWinner winner) {
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

//    private void addTicTacToePanel() {
//
//        gamePanel = new JPanel();
//        gamePanel.setBackground(Color.black);
//        Dimension size = new Dimension(155,155);
//        gamePanel.setPreferredSize(size);        
//        gamePanel.setMaximumSize(size);
//        gamePanel.setAlignmentX(CENTER_ALIGNMENT);
//        
//        add(gamePanel, "wrap");
//        
//        AddCellActions();
//
//        initialCellAppearance();
//    }        
    
    
    private void yourTurn(boolean yourGo) {
        if(yourGo) {
            statusText.setText(myID + ", make a move");
        } else {
            statusText.setText(friendsID + "'s pick, wait");                
        }
        enableButtons(yourGo);
    }
    
    private void enableButtons(boolean show) {
        for(int i = 0; i < 9; i++) { 
            gameButtons[i].setEnabled(show);
        }        
    }
    
    private class TicTacToeJButton extends JButton {
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
            setFocusable(false);
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
    

    private class PlayAgainAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            challengePlayAgain();            
            playAgainButton.setVisible(false);

        }
    }
    private class AgainAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {                                    
            againButton.setVisible(false);
            
            setPlayerX(false);
            yourTurn(false);
            board.initialize();
            clearButtonText();
            initialCellAppearance();                  

            //send a message saying yes, I want to play again            
            String message = new String();
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.PLAY_AGAIN_YES;                        
            sendMessage(message);        
                        
        }
    }
    
    private class TicTacToeCellAction implements ActionListener {
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
            makeMyMove(cellNum);
        }
    }       
}
