package org.limewire.util;


public class TicTacToeJudge {

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
    //For testing
    public static void main(String[] args) {
        TicTacToeBoard b1 = new TicTacToeBoard('x', 'x', 'x', 
                                               'o', 'o', 'x', 
                                               'o', 'o', 'e');
        TicTacToeJudge test = new TicTacToeJudge();
        TicTacToeWinner w = null;
        w = test.result(b1);
        if (w != TicTacToeWinner.X_ACROSS_1) {
            System.out.println("x won failure");
        }

        TicTacToeBoard x2 = new TicTacToeBoard('o', 'x', 'x', 
                                               'x', 'x', 'x', 
                                               'o', 'o', 'e');
        w = test.result(x2);
        if (w != TicTacToeWinner.X_ACROSS_2) {
            System.out.println("x 2 won failure");
        }

        TicTacToeBoard x3 = new TicTacToeBoard('o', 'x', 'o', 
                                               'o', 'o', 'x', 
                                               'x', 'x', 'x');
        w = test.result(x3);
        if (w != TicTacToeWinner.X_ACROSS_3) {
            System.out.println("x 3 won failure");
        }
        
        TicTacToeBoard xd1 = new TicTacToeBoard( 'x', 'x', 'o', 
                                                'o', 'x', 'x', 
                                                'x', 'o', 'x');

        w = test.result(xd1);
        if (w != TicTacToeWinner.X_DIAG_1) {
            System.out.println("X won failure - diagonal 1");
        }
        TicTacToeBoard xd2 = new TicTacToeBoard( 'x', 'o', 'x', 
                                                 'o', 'x', 'x', 
                                                 'x', 'o', 'o');

        w = test.result(xd2);
        if (w != TicTacToeWinner.X_DIAG_2) {
            System.out.println("X won failure - diagonal 2");
        }

        
        TicTacToeBoard b2 = new TicTacToeBoard( 'x', 'x', 'e', 
                'o', 'o', 'x', 
                'o', 'o', 'e');

        w = test.result(b2);
        if (w != TicTacToeWinner.NO_ONE_YET) {
            System.out.println("not yet failure");
        }

        TicTacToeBoard b3 = new TicTacToeBoard( 'x', 'x', 'o', 
                                                'o', 'x', 'x', 
                                                'x', 'o', 'o');

        w = test.result(b3);
        if (w != TicTacToeWinner.DRAW) {
            System.out.println("draw failure");
        }

        TicTacToeBoard b4 = new TicTacToeBoard( 'o', 'x', 'o', 
                                                'o', 'o', 'x', 
                                                'x', 'o', 'o');

        w = test.result(b4);
        if (w != TicTacToeWinner.O_DIAG_1) {
            System.out.println("O won failure - diagonal 1");
        }
        TicTacToeBoard od2 = new TicTacToeBoard( 'x', 'x', 'o', 
                                                 'o', 'o', 'x', 
                                                 'o', 'x', 'o');

        w = test.result(od2);
        if (w != TicTacToeWinner.O_DIAG_2) {
            System.out.println("o won failure - diagonal 2");
        }
        TicTacToeBoard o1 = new TicTacToeBoard( 'o', 'o', 'o', 
                                                'o', 'x', 'o', 
                                                'x', 'o', 'x');

        w = test.result(o1);
        if (w != TicTacToeWinner.O_ACROSS_1) {
            System.out.println("o won failure - across 1");
        }

        TicTacToeBoard b6 = new TicTacToeBoard( 'x', 'x', 'o', 
                                                'o', 'o', 'o', 
                                                'o', 'o', 'x');

        w = test.result(b6);
        if (w != TicTacToeWinner.O_ACROSS_2) {
            System.out.println("o won failure - across 2");
        }
        TicTacToeBoard b7 = new TicTacToeBoard( 'x', 'x', 'o', 
                                                'o', 'o', 'x', 
                                                'o', 'o', 'o');

        w = test.result(b7);
        if (w != TicTacToeWinner.O_ACROSS_3) {
            System.out.println("o won failure - across 3");
        }

        TicTacToeBoard od1 = new TicTacToeBoard( 'o', 'x', 'o', 
                                                'o', 'o', 'x', 
                                                'o', 'o', 'x');

        w = test.result(od1);
        if (w != TicTacToeWinner.O_DOWN_1) {
            System.out.println("o won failure - down 1");
        }
        
        TicTacToeBoard b8 = new TicTacToeBoard( 'x', 'o', 'o', 
                                                'o', 'o', 'x', 
                                                'o', 'o', 'x');

        w = test.result(b8);
        if (w != TicTacToeWinner.O_DOWN_2) {
            System.out.println("o won failure - down 2");
        }

        TicTacToeBoard b9 = new TicTacToeBoard( 'x', 'o', 'o', 
                                                'o', 'x', 'o', 
                                                'o', 'x', 'o');

        w = test.result(b9);
        if (w != TicTacToeWinner.O_DOWN_3) {
            System.out.println("o won failure - down 3");
        }
        System.out.println("Finished TicTactoeJudge test");        
    }
}