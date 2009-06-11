package org.limewire.ui.swing.friends.login;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import net.miginfocom.swing.MigLayout;

import com.google.inject.Inject;

public class ServiceSelectionLoginPanel extends JPanel {
    
    @Inject
    public ServiceSelectionLoginPanel() {
        super(new BorderLayout());
        setOpaque(false);
        
        JPanel topPanel = new JPanel(new MigLayout("insets 0, gap 0, alignx center, flowy"));
        
        JLabel shareLabel = new JLabel(I18n.tr("Share"));
        shareLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        topPanel.add(shareLabel, "gaptop 8");
        
        JPanel shareIcon = new JPanel();
        shareIcon.setBackground(Color.blue);
        ResizeUtils.forceSize(shareIcon, new Dimension(153,90));
        topPanel.add(shareIcon, "gapright 14, wrap");
        
        JLabel browseLabel = new JLabel(I18n.tr("Browse"));
        browseLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        topPanel.add(browseLabel);
        
        JPanel browseIcon = new JPanel();
        browseIcon.setBackground(Color.blue);
        ResizeUtils.forceSize(browseIcon, new Dimension(153,90));
        topPanel.add(browseIcon, "gapright 14, wrap");
        
        JLabel chatLabel = new JLabel(I18n.tr("Chat"));
        chatLabel.setFont(new Font("Arial", Font.PLAIN, 18));
        topPanel.add(chatLabel);
        
        JPanel chatIcon = new JPanel();
        chatIcon.setBackground(Color.blue);
        ResizeUtils.forceSize(chatIcon, new Dimension(153,90));
        topPanel.add(chatIcon);
        
        JPanel bottomPanel = new JPanel(new MigLayout("gap 0, insets 0, align center"));
        bottomPanel.setOpaque(false);
        JLabel signOnMessageLabel = new JLabel(I18n.tr("Sign on with any of the services below:"));
        signOnMessageLabel.setOpaque(false);
        bottomPanel.add(signOnMessageLabel, "dock north");
        
        JPanel selectionPanel = new JPanel(new MigLayout("gap 0, insets 0, alignx center, filly"));
        selectionPanel.setOpaque(false);
        selectionPanel.add(new JButton("Facebook"), "gaptop 10, gapafter 30");
        selectionPanel.add(new JButton("Gmail"), "wrap");
        selectionPanel.add(new JButton("LiveJournal"), "gaptop 30");
        selectionPanel.add(new JButton(I18n.tr("Other")));
        bottomPanel.add(selectionPanel);
        
        add(topPanel, BorderLayout.NORTH);
        add(bottomPanel, BorderLayout.CENTER);
    }

}
