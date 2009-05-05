package org.limewire.ui.swing.tictactoe;

/**
 * Lists the various ways to win a Tic Tac Toe Game, including either a 
 * draw, or no one yet. _DIAG_1 has negative slope (starts in the upper 
 * left cell and goes to the bottom right cell and conversely, 
 * _DIAG_2 has positive slope (from the bottom left cell to the top right cell).
 */
public enum TicTacToeWinner {
    DRAW, NO_ONE_YET, 
    X_ACROSS_1, X_ACROSS_2, X_ACROSS_3, 
    X_DOWN_1, X_DOWN_2, X_DOWN_3,
    X_DIAG_1, X_DIAG_2,
    O_ACROSS_1, O_ACROSS_2, O_ACROSS_3, 
    O_DOWN_1, O_DOWN_2, O_DOWN_3,
    O_DIAG_1, O_DIAG_2
}

