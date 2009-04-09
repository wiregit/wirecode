package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

/**
* If someone right clicked on a friend and then selected to challenge to a tic tac toe game
 * @author dsullivan
 *
 */
class ChallengeSomeoneToTicTacToeAction extends AbstractAction {
    public ChallengeSomeoneToTicTacToeAction() {
        super(tr("<html>{0}</html>", "Challenge to Tic Tac Toe"));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("in ChallengeSomeoneToTicTacToeAction actionPerformed");
        new TicTacToeInitiateGameEvent(1).publish();
    }
}