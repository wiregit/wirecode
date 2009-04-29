package org.limewire.util;

/**
 * Array with 9 entries, each one a char either x or 0.
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
     * for testing
     */
   protected TicTacToeBoard(char a, char b, char c, 
                            char d, char e, char f,
                            char g, char h, char i
                               ) {
            board[0] = a;board[1] = b; board[2] = c;
            board[3] = d;board[4] = e; board[5] = f;
            board[6] = g;board[7] = h; board[8] = i;
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
    public void printBoard() {
        System.out.println(board[0] + " " + board[1] + " " + board[2]);
        System.out.println(board[3] + " " + board[4] + " " + board[5]);
        System.out.println(board[6] + " " + board[7] + " " + board[8]);
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
}
