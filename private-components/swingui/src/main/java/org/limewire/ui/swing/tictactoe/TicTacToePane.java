package org.limewire.ui.swing.tictactoe;


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
import org.limewire.ui.swing.friends.chat.ChatFriend;
import org.limewire.ui.swing.util.I18n;
import org.limewire.xmpp.api.client.MessageWriter;
import org.limewire.xmpp.api.client.XMPPException;

/**
 * User Interface for playing Tic Tac Toe with a friend signed into the Friends feature.
 */
public class TicTacToePane extends JPanel {
    
    private TicTacToeBoard board;    
    
    private String myID;
    private String friendsID;
    private boolean isX = false;
    
    private ChatFriend friend = null;//friend who you are playing against
    private MessageWriter messageWriter;//for sending your move. 
            
    private JPanel gamePanel = new JPanel();
        
    private JLabel playerLabel = new JLabel();
    private JLabel statusText = new JLabel();
    
    private TicTacToeJButton[] gameButtons;

    private JButton playAgainButton = new JButton();
    private JButton againButton = new JButton();

    private final int LENGTH = 60;
    private final int WIDTH = 60;
    
    private static int TOP_LEFT = 0;
    private static int TOP_CENTER = 1;
    private static int TOP_RIGHT = 2;
    
    private static int MIDDLE_LEFT = 3;
    private static int MIDDLE_CENTER = 4;
    private static int MIDDLE_RIGHT = 5;
    
    private static int BOTTOM_LEFT = 6;
    private static int BOTTOM_CENTER = 7;
    private static int BOTTOM_RIGHT = 8;

    
    public TicTacToePane(MessageWriter writer, ChatFriend chatFriend, String myID, boolean isX ) {        
        
        messageWriter = writer;
        friend = chatFriend;
        this.myID = myID;
        friendsID = chatFriend.getName();
        setPlayerX(isX);
        
        board = new TicTacToeBoard();
        
        setLayout(new MigLayout(
                "insets 12 12 6 12,fill",
                "[left]",                         // col constraints
                "[top]12[top]6[top]12[bottom]")); // row constraints
            
        initGamePanel();
        
        playerLabel.setText(I18n.tr("player"));
        
        statusText.setText(I18n.tr("Make a move"));
        
        playAgainButton.setVisible(false);        
        playAgainButton.setText(I18n.tr("Play again"));
        
        againButton.setVisible(false);
        againButton.setText(I18n.tr("Again?"));

        add(gamePanel, "center,wrap");
        add(playerLabel, "wrap");
        add(statusText, "wrap");
        add(playAgainButton, "center, hidemode 3");
        add(againButton, "center, hidemode 3");
        
        //To receive EDT events.
        EventAnnotationProcessor.subscribe(this);
                    
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

    /**
     * <code>handleCreatePane</code> in <code>ConversationPane</code> calls this method.
     * In <code>NavPanel</code>, if your right click to challenge a friend, 
     * this method is eventually called.
     */
    public void fireGameStarted() {
        myTurn(isX);
        board.initialize();

        clearButtonText();
        initialCellAppearance();                  

        if (isX) {
            playerLabel.setText(I18n.tr("You are X"));
        } else {
            playerLabel.setText(I18n.tr("You are O"));
            String message = new String();
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.INITIATE_GAME;
    
            sendMessage(message);        
        }            
    }                 

    /**
     * Sends a string to your friend's chat frame.
     */
    private void sendMessage(String message) {
        try {   
            messageWriter.writeMessage(message);
        } catch(XMPPException x) {
            x.printStackTrace();
        }        
    }
    
    //The @EventSubscriber annotation shows methods which handle internal events        
    /**
     * Handled when a friend signs out of their Friend account while a game 
     * is in progress.
     */
    @EventSubscriber
    public void handleTicTacToeSignOffFriendsEvent(TicTacToeSignOffFriendsEvent event) {

        if(event.getFriendSignedOff()) {
            statusText.setText(I18n.tr("Your friend signed out of Friends"));            
        } else {
            statusText.setText(I18n.tr("You signed out of Friends"));            
        }
        
        enableButtons(false);
    }
       
    /**
     * These events are intercepted form ConversationPane which catches from 
     * what your friend sent. When caught here, this updates your board
     * before letting you make a move.
     * <p>
     * Only the move, or special messages are sent. Win, lose or draw are
     * decided locally.
     * <p>
     * Sent from ConversationPane's interceptTicTacToeMessages.
     */
    @EventSubscriber
    public void handleTicTacToeSelectedFromFriendEvent(TicTacToeSelectedFromFriendEvent event) {   
        
        //only want to mark messages from my friend
        if(!event.getMessage().getFriendID().matches(friend.getID())) {
            return;
        }

        String tttMessage = event.getMessage().toString();        
        //sent from my friend challenging me to play again, I need to show the "Again" button
        if(tttMessage.toString().indexOf(TicTacToeMessages.PLAY_AGAIN) > -1) {
            showAgain();
        }
        //friend accepted the challenge
        if(tttMessage.toString().indexOf(TicTacToeMessages.PLAY_AGAIN_YES) > -1) {
            //at this point, you asked to play again, friend said yes, you are x
            playAgainChallengeAccepted();
            if(isX) {
                enableButtons(true);
            } else {
                enableButtons(false);                
            }
        }       
        
        if(tttMessage.toString().indexOf(TicTacToeMessages.DRAW) > -1) {
            showAgain();
        }       

        if(tttMessage.toString().indexOf(TicTacToeMessages.NO_THANKS_GAME) > -1) {
            statusText.setText(I18n.tr("Friend not interested"));
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.EXIT_GAME) > -1) {
            statusText.setText(I18n.tr("Friend quit the game"));            
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.O_MOVE) > -1) {

            markFriendsMove(tttMessage, false);
        }
        if(tttMessage.toString().indexOf(TicTacToeMessages.X_MOVE) > -1) {
            //parse statusText for move
            markFriendsMove(tttMessage, true);
        }
    }
                
    /**
     * My friend's move. 
     */
    private void markFriendsMove(String message, boolean isXMove) {
              
        int cellNum = parseMessage(message);

        if(isXMove) {    
            xMove(cellNum);
        } else {            
            oMove(cellNum);
        }

        myTurn(true);
        determineIfFriendWon();

    }

    /**
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
    /**
     * @param cellNum 0-8, one of the cells I selected.
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
        myTurn(false);
        determineIfIWon();  
        sendMessage(message);
        
    }
    
    /**
     * After receiving a friend's move, you need to determine if he won in order
     * to update your UI appropriately.
     */
    private void determineIfFriendWon() {
        
        TicTacToeJudge judge = new TicTacToeJudge();

        TicTacToeWinner winner = judge.result(board);
        //if not yet, or a draw, someone won
        if(winner == TicTacToeWinner.DRAW) {
            statusText.setText(I18n.tr(TicTacToeMessages.DRAW));
            enableButtons(false);
            hideDraw();
            statusText.setText(I18n.tr(TicTacToeMessages.DRAW));
            showPlayAgain();

        } else if(winner != TicTacToeWinner.NO_ONE_YET) {
            hideNonWin(winner);
            statusText.setText(friendsID + I18n.tr(" won"));                    
            //you lost, your friend won
            showPlayAgain();
            enableButtons(false);
        }
        
    }
    
    /**
     * Called by me for my move.
     */
    private void determineIfIWon() {
        TicTacToeJudge judge = new TicTacToeJudge();
        
        TicTacToeWinner winner = judge.result(board);
        hideNonWin(winner);
        
        if(winner != TicTacToeWinner.DRAW || winner != TicTacToeWinner.NO_ONE_YET) {
            statusText.setText(myID + I18n.tr(", you won"));                                
            return;
        }
        if(winner == TicTacToeWinner.DRAW) {
            statusText.setText(myID + I18n.tr(", draw"));                                
        }
    }
    
    /**
     * If you went second and the game ended in a draw, or if you lost, show 
     * the button to play again.
     */
    private void showPlayAgain() {
        playAgainButton.setVisible(true);
        playAgainButton.setText(I18n.tr("Play ") + friendsID + I18n.tr(" again"));
    }

    /**
     * Send a play again challenge to your friend (you get to go first), 
     * waiting for his acceptance or rejection of another game.
     */
    private void challengePlayAgain() {
        setPlayerX(true);
        myTurn(true);
        board.initialize();

        clearButtonText();
        initialCellAppearance();                  

        String message = new String();
        message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.PLAY_AGAIN;
        sendMessage(message);
    }

    /**
     * Your friend wants to play again and you are notified here with the 
     * play again button.
     */
    private void showAgain() { 
        enableButtons(false);

        statusText.setVisible(false);

        againButton.setText(friendsID + I18n.tr(" wants to play again"));
        againButton.setVisible(true);
    }

    /**
     * When your friend accepted to play another game, you get to go first.
     */
    private void playAgainChallengeAccepted() {
        //need to alternate who goes first
        setPlayerX(true);
        //board need to clear out that array
        board.initialize();

        clearButtonText();
        statusText.setText(I18n.tr("Make a pick"));
        initialCellAppearance();                  
        myTurn(true);

    }
    
    private void setPlayerX(boolean isX) {
        this.isX = isX;
        if(isX) {
            playerLabel.setText(I18n.tr("You are X"));
        } else {
            playerLabel.setText(I18n.tr("You are O"));
        }
    }

    /**
     * Updates the status text about who's turn it is and either let's you,
     * or prevents you from making a move.
     * @param myGo false if it is your friend's move
     */
    
    private void myTurn(boolean myGo) {
        if(myGo) {
            statusText.setText(myID + I18n.tr(", make a move"));
        } else {
            statusText.setText(friendsID + I18n.tr("'s pick, wait"));                
        }
        enableButtons(myGo);
    }
    
    /**
     * Sets the buttons as clickable or not.
     * @param show true to let the Tic Tac Toe cells be clickable
     */
    private void enableButtons(boolean show) {
        for(int i = 0; i < 9; i++) { 
            gameButtons[i].setEnabled(show);
        }        
    }

    /**
     * 
     * @param cellNum 0 - 8
     * @return false if there was a problem with the pick. For example, a 
     * cell was already taken.
     */
    private boolean xMove(int cellNum) {
        if(false == board.setX(cellNum)) {
            //couldn't set value
            statusText.setText(I18n.tr(TicTacToeMessages.BAD_PICK));
            return false;
        } 
        setX(cellNum);
        return true;        
    }
    /**
     * 
     * @param cellNum 0 - 8
     * @return false if there was a problem with the pick. For example, a
     * cell was already taken.
     */
    private boolean oMove(int cellNum) {

        if(false == board.setO(cellNum)) {
            //couldn't set value
            statusText.setText(I18n.tr(TicTacToeMessages.BAD_PICK));
            return false;
        }
        setO(cellNum);
        
        return true;

    }
    /**
     * Removes text from all buttons.
     */
    private void clearButtonText() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].setText("");
        }
    }

    /**
     * Sets the look of the buttons at the start of a game.
     */
    private void initialCellAppearance() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].initial();
        }
    }
    
    /**
     * Adds a <code>TicTacToeCellAction</code> for each of the nine buttons
     * you might select.
     */
    private void AddCellActions() {
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
           
    private void hideDraw() {
        for(int i = 0; i < 9; i++) {
            gameButtons[i].loser();
        }             
    }

    /**
     * Changes winning cells to be a yellow background.
     * @param winner moves the Tic Tac Toe winner selected 
     */
    private void hideNonWin(TicTacToeWinner winner) {
        
        //Across
        if (winner == TicTacToeWinner.X_ACROSS_1 || winner == TicTacToeWinner.O_ACROSS_1) {                                              
            markWin(TOP_LEFT, TOP_CENTER, TOP_RIGHT);
        }        
        if (winner == TicTacToeWinner.X_ACROSS_2 || winner == TicTacToeWinner.O_ACROSS_2) {            
            markWin(MIDDLE_LEFT, MIDDLE_CENTER, MIDDLE_RIGHT);
        }        
        if (winner == TicTacToeWinner.X_ACROSS_3 || winner == TicTacToeWinner.O_ACROSS_3) {            
            markWin(BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT);
        }
        
        //Down
        if (winner == TicTacToeWinner.X_DOWN_1 || winner == TicTacToeWinner.O_DOWN_1 ) {
            markWin(TOP_LEFT, MIDDLE_LEFT, BOTTOM_LEFT);
        }
        if (winner == TicTacToeWinner.X_DOWN_2 || winner == TicTacToeWinner.O_DOWN_2 ) {            
            markWin(TOP_CENTER, MIDDLE_CENTER, BOTTOM_CENTER);           
        }        
        if (winner == TicTacToeWinner.X_DOWN_3 || winner == TicTacToeWinner.O_DOWN_3 ) {           
            markWin(TOP_RIGHT, MIDDLE_RIGHT, BOTTOM_RIGHT);            
        }
        
        //Diagonal
        if (winner == TicTacToeWinner.X_DIAG_1 || winner == TicTacToeWinner.O_DIAG_1 ) {
            markWin(TOP_LEFT, MIDDLE_CENTER, BOTTOM_RIGHT);            
        }        
        if (winner == TicTacToeWinner.X_DIAG_2 || winner == TicTacToeWinner.O_DIAG_2 ) {
            markWin(TOP_RIGHT, MIDDLE_CENTER, BOTTOM_LEFT);
        }
    }
    
    private void markWin(int one, int two, int three) {

        gameButtons[TOP_LEFT].loser();
        gameButtons[TOP_CENTER].loser();
        gameButtons[TOP_RIGHT].loser();

        gameButtons[MIDDLE_LEFT].loser();
        gameButtons[MIDDLE_CENTER].loser();
        gameButtons[MIDDLE_RIGHT].loser();

        gameButtons[BOTTOM_LEFT].loser();
        gameButtons[BOTTOM_CENTER].loser();
        gameButtons[BOTTOM_RIGHT].loser();

        //now show winner
        gameButtons[one].winner();
        gameButtons[two].winner();
        gameButtons[three].winner();
        
    }
          
    /**
     * Each cell in the Tic Tac Toe board is a clickable button, however the cells
     * won't look like buttons to provide for a better presentation. If there's
     * a winner, the winning cells change to a yellow background to highlight
     * the win.
     */
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
        }
        private  void winner() {
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(Color.YELLOW);
            setEnabled(false);
        }
        private  void loser() {
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(Color.WHITE);
            setEnabled(false);
        }
        
        private  void initial() {
            setContentAreaFilled(false);
            setOpaque(true);
            setBackground(Color.WHITE);            
        }
    }
    
    /**
     * Sets your Tic Tac Toe selection when you click on a cell in the
     * game board.
     */
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

    /**
     * When you lose, or there was a draw and you went second, you get a chance
     * to play another game. The play again button is shown and if you click it,
     * a message is sent to your friend to confirm playing another game. You
     * then get to go first for the next game.
     */        
    private class PlayAgainAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            challengePlayAgain();            
            playAgainButton.setVisible(false);
            statusText.setVisible(true);
            enableButtons(false);
            statusText.setText(I18n.tr("Wait for your friend to confirm"));
        }
    }
    /**
     * If a game ended in a draw and you went first, or you won a game, your friend
     * gets the chance to start another game. If he does want another game, the 
     * 'again' button is shown and you need to OK playing a game. If you accept  
     * the play again, you go second.
     *
     */
    private class AgainAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {    
            statusText.setVisible(true);

            againButton.setVisible(false);
            
            setPlayerX(false);
            myTurn(false);
            board.initialize();
            clearButtonText();
            initialCellAppearance();                  

            //send a message saying yes, I want to play again            
            String message = new String();
            message = TicTacToeMessages.TICTACTOE + TicTacToeMessages.PLAY_AGAIN_YES;                        
            sendMessage(message);        
                        
        }
    }
}
