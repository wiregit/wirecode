package org.limewire.ui.swing.tictactoe;

/**
 * Array with 9 entries, each one a char either x, o, or e, where e is 
 * to mark an empty cell. Cells are initially set to e.
 *
 */
public class TicTacToeBoard {
    protected char [] board = new char[9];
    
    public TicTacToeBoard() {
        initialize();
    }
    public void initialize() {
        for(int index = 0; index < 9; index++) {
            board[index] = 'e';//e for empty
        }        
    }

    /**
     * Protected for testing to create a board directly.
     */
   protected TicTacToeBoard(char topLeft, char topCenter, char topRight, 
                            char middleLeft, char middleCenter, char middleRight,
                            char bottomLeft, char bottomCenter, char bottomRight
                               ) {
            board[0] = topLeft;     board[1] = topCenter;       board[2] = topRight;
            board[3] = middleLeft;  board[4] = middleCenter;    board[5] = middleRight;
            board[6] = bottomLeft;  board[7] = bottomCenter;    board[8] = bottomRight;
    }
    
    public boolean setX(int pos) {
        if(board[pos] != 'e') {
            return false;
        }
        board[pos] = 'x';
        return true;
    }
    
    public boolean setO(int pos) {
        if(board[pos] != 'e') {
            return false;
        }
        board[pos] = 'o';
        return true;
    }
    
    public char[] getBoard() {
        return board;
    }

    public int cellsSelected() {
        int spots = 0;
        for(int i = 0; i < 9; i++) {
            if(board[i] != 'e') {
                spots++;
            }
        }
        return spots;
    }    
    
    @Override
    public String toString() {
        String boardValues =  board[0] + " " + board[1] + " " + board[2] + "\n" +
                              board[3] + " " + board[4] + " " + board[5] + "\n" +
                              board[6] + " " + board[7] + " " + board[8]; 
        return boardValues;
    }
}
