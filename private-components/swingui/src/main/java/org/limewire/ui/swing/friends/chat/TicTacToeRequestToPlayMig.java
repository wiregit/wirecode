package org.limewire.ui.swing.friends.chat;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TicTacToeRequestToPlayMig extends JPanel  {

    public JButton yes;
    public JButton no;

    private JLabel statusText; //will be messages to the user

    TicTacToeRequestToPlayMig() {
        InitializeButtons();
        AddButtons();
    }
    public void InitializeButtons(){
        statusText = new JLabel("Do you want to play Tic-Tac-Toe");
        yes = new JButton("Yes");
        no = new JButton("No thanks");

    }
    public void AddButtons() {
        add(statusText, "wrap");
        add(yes);
        add(no, "wrap");
    }


}
