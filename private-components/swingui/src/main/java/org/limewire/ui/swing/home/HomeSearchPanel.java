package org.limewire.ui.swing.home;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.limewire.ui.swing.util.Line;

public class HomeSearchPanel extends JPanel {
    
    private final JTextField textField;
    private final JLabel all;
    private final JLabel audio;
    private final JLabel video;
    private final JLabel images;
    private final JLabel documents;
    private final JButton search;
    private final JButton searchFriends;
    
    public HomeSearchPanel() {
        
        textField = new JTextField();
        all = new JLabel("All");
        audio = new JLabel("Audio");
        video = new JLabel("Video");
        images = new JLabel("Images");
        documents = new JLabel("Documents");
        search = new JButton("Search");
        searchFriends = new JButton("Search Friends");
        
        setOpaque(false);
        
        setLayout(new GridBagLayout());
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        
        gbc.weightx = 1;
        add(Box.createGlue(), gbc);
        gbc.weightx = 0;
        
        gbc.insets = new Insets(0, 0, 5, 10);
        add(all, gbc);
        add(new Line(Color.BLACK), gbc);
        add(audio, gbc);
        add(new Line(Color.BLACK), gbc);
        add(video, gbc);
        add(new Line(Color.BLACK), gbc);
        add(images, gbc);
        add(new Line(Color.BLACK), gbc);
        add(documents, gbc);

        gbc.weightx = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        add(Box.createGlue(), gbc);
        gbc.weightx = 0;
        
        gbc.weightx = 1;
        add(textField, gbc);
        
        gbc.weightx = 0;
        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new GridBagLayout());
        add(buttonPanel, gbc);
        
        gbc.insets = new Insets(0, 0, 0, 20);
        gbc.gridwidth = GridBagConstraints.RELATIVE;
        buttonPanel.add(search, gbc);    
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        buttonPanel.add(searchFriends, gbc);
             
        setPreferredSize(new Dimension(300, 80));
        
    }
    

}
