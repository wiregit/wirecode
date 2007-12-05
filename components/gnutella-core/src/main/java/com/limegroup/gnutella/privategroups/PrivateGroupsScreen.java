package com.limegroup.gnutella.privategroups;

import java.awt.Color;
import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jivesoftware.smack.packet.Message;
import org.limewire.listener.Event;
import org.limewire.listener.EventListener;
import org.limewire.util.PrivateGroupsUtils;

public class PrivateGroupsScreen extends JPanel implements EventListener {
    
    
    private JTextField msgField;
    private JTextArea incomingMsgArea;
    private JScrollPane areaScrollPane;
    private ChatManager chatManager; 
    private String remoteUserName;
    private String localUserName;
    
    public PrivateGroupsScreen(String remoteUserName, String localUsername, ChatManager chatManager){
        this.remoteUserName = remoteUserName;
        this.localUserName = localUsername;
        this.chatManager = chatManager;
        initFrame();  
    }
    
    private void initFrame(){
        
        JFrame f = new JFrame("Conversation with " + remoteUserName);
        f.addWindowListener(new FrameListener());
        f.setSize(400, 600);
        Container content = f.getContentPane();
        content.setBackground(Color.white);
        content.setLayout(new GridLayout(0,1)); 
        
        msgField = new JTextField();
        
        msgField.addKeyListener(new KeyAdapter(){

            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ENTER){
                    sendMessage();
                }
                
            }
        });
        
        incomingMsgArea = new JTextArea();
        
        JScrollPane areaScrollPane = new JScrollPane(incomingMsgArea);
        areaScrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        //areaScrollPane.setPreferredSize(new Dimension(250, 250));
        
        
        content.add(msgField);
        content.add(areaScrollPane);
        
        JButton sendBtn = new JButton("Send");
        sendBtn.setSize(10, 5);
        sendBtn.addActionListener(new SendButtonListener());
        
        content.add(sendBtn);
        
        f.setVisible(true);
 
    }
    
    public void printMessage(String username, String msg){
        incomingMsgArea.append(username + " said: " + msg + "\n");
    }
    
//    private void printMsg(String msg){
//        msgField.setText(msg);
//    }
    
    private void sendMessage(){
        String msg = msgField.getText();

        chatManager.send(PrivateGroupsUtils.createMessage(localUserName, remoteUserName, msg));
        msgField.setText("");
    }
    
    private class SendButtonListener implements ActionListener{

        public void actionPerformed(ActionEvent e) {
            sendMessage();
        }
    }
    
    private class FrameListener extends WindowAdapter{
        
        //override window closing
        public void windowClosing(WindowEvent e) {
            chatManager.closeChatManager();
            System.exit(0);
        }
    }

    public void handleEvent(Event event) {
        if (event instanceof MessageEvent){
            Message msg = (Message) event.getSource();
            printMessage(msg.getFrom(), msg.getBody());
        }
    }
}
