package com.limegroup.gnutella.privategroups;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class PrivateGroupsScreen extends JPanel {
    
    
    public JTextField msgField;
    public static PGRPClient client; 
    
    private PrivateGroupsScreen(){
        initFrame();   
    }
    
    private void initFrame(){
        
        JFrame f = new JFrame("This is a test");
        f.setSize(400, 150);
        Container content = f.getContentPane();
        content.setBackground(Color.white);
        content.setLayout(new GridLayout(0,1)); 
        
        msgField = new JTextField();
        
        content.add(msgField);
        
        JButton sendBtn = new JButton("Send");
        sendBtn.setSize(10, 5);
        sendBtn.addActionListener(new SendButtonListener());
        
        content.add(sendBtn);
        
        f.setVisible(true);
        
        
    }
    
    public void printMsg(String msg){
        msgField.setText(msg);
    }

    public static void main(String[] args) {
        PrivateGroupsScreen screen = new PrivateGroupsScreen();
        
        //start PGRPClient stuff
        client = new PGRPClient(PGRPClient.connectToServerNoPort("10.254.0.30"));
        client.loginAccount("user2", "password1");
        screen.printMsg("login successful");
        
    }
    
    private class SendButtonListener implements ActionListener{

        public void actionPerformed(ActionEvent e) {
            String msg = msgField.getText();
            client.sendMessage("user1", msg);
        }
        
        
    }
    
}
