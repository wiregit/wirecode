package org.limewire.ui.swing.tictactoe;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

/**
 * Returns  the {@link TicTacToeWinner} depending on the current {@link TicTacToeBoard}.
 */

public class TicTacToeJudge {

    //Create a logging component
    private static final Log LOG = LogFactory.getLog(TicTacToeJudge.class);

    /*
     * 0 | 1 | 2
     * 3 | 4 | 5
     * 6 | 7 | 8
     * 
     * win: 
     * across
     * 0 & 1 & 2
     * 3 & 4 & 5
     * 6 & 7 & 8
     * 
     */
    
    public TicTacToeWinner result(TicTacToeBoard b) {
        char []board = b.getBoard();

        //To view the debug statements, you need to enable the logging
        //The quickest way is to use LimeWire's Console tab in the Advanced Tools.
        //LimeWire needs to have loaded this class at least once before it is 
        //loaded into Advanced Tools. 
        //Otherwise, you can add this class to the log4j.properties file as 
        //specified at: http://wiki.limewire.org/index.php?title=Debug_logging
        //and use LimeWire or another tool to view the debug statements.
        LOG.debugf("{0}{1}", "\n", b.toString());
        
        /*
         * 0 | 1 | 2
         * 3 | 4 | 5
         * 6 | 7 | 8
         */
     
        /* across
         * 0 & 1 & 2
         * 3 & 4 & 5
         * 6 & 7 & 8
         */

        if (board[0] == board[1] && board[1] == board[2] && board[1] != 'e' ) {
            if (board[ 0 ] == 'x') {                
                return TicTacToeWinner.X_ACROSS_1;
            }
            return TicTacToeWinner.O_ACROSS_1;
        }
        if (board[3] == board[4] && board[4] == board[5] && board[4] != 'e' ){
            if (board[3 ] == 'x') {
                return TicTacToeWinner.X_ACROSS_2;
            }
            return TicTacToeWinner.O_ACROSS_2;
        }
        if (board[6] == board[7] && board[7] == board[8] && board[7] != 'e' ) {
            if (board[6 ] == 'x') {
                return TicTacToeWinner.X_ACROSS_3;
            }
            return TicTacToeWinner.O_ACROSS_3;
        }
        
        /*
         * 0 | 1 | 2
         * 3 | 4 | 5
         * 6 | 7 | 8
         */

        /* down
         * 0 & 3 & 6
         * 1 & 4 & 7
         * 2 & 5 & 8
         */
        if (board[0 ] == board [3] && board [3] == board [6] && board[3] != 'e' ) {
            if (board[0 ] == 'x') {
                return TicTacToeWinner.X_DOWN_1;
            }
            return TicTacToeWinner.O_DOWN_1;
        }
        if (board[1] == board [4] && board [4] == board[7] && board[4] != 'e' ) {
            if (board[1 ] == 'x') {
                return TicTacToeWinner.X_DOWN_2;
            }
            return TicTacToeWinner.O_DOWN_2;        
        }
        if (board[2] == board[5] && board[5] == board[8] && board[5] != 'e' ) {
            if (board[2 ] == 'x') {
                return TicTacToeWinner.X_DOWN_3;
            }
            return TicTacToeWinner.O_DOWN_3;
        }
                
        /*
         * 0 | 1 | 2
         * 3 | 4 | 5
         * 6 | 7 | 8
         */

        /* diagonal
         * 0 & 4 & 8
         * 2 & 4 & 6
         */
        if (board[0] == board[4] && board[4] == board[8] && board[4] != 'e' ) {
            if (board[0 ] == 'x') {
                return TicTacToeWinner.X_DIAG_1;
            }
            return TicTacToeWinner.O_DIAG_1;
    
        }
        if (board[2] == board[4] && board[4] == board[6] && board[4] != 'e' ) {
            if (board[2] == 'x') {
                return TicTacToeWinner.X_DIAG_2;
            }
            return TicTacToeWinner.O_DIAG_2;
            
        }
        //check for no winner yet
        for(int index = 0; index < 9; index++) {
            if(board[index] == 'e')
                return TicTacToeWinner.NO_ONE_YET;
        }
        return TicTacToeWinner.DRAW;
    }
}