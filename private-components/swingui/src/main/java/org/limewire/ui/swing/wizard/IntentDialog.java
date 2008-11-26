package org.limewire.ui.swing.wizard;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.SystemUtils;

public class IntentDialog extends LimeJDialog {

    private final String title = I18n.tr("LimeWire 5 - Some legal stff");
    private final String heading  = I18n.tr("State your intent");
    
    private final String bodyText1 = I18n.tr("LimeWire Basic and LimeWire PRO are peer-to-peer programs for sharing the authorized files only.  " +
    		"Installing and using either program does not constitute a license for obtaining or distributing " +
    		"unauthorized content.");
    
    private final String linkText = I18n.tr("Learn more");
    
    private final String agreementText = I18n.tr("By clicking \"I Agree\", you agree to not use LimeWire 5 for copyright infringement");
    
    private final String learnMoreURL = "http://www.limewire.com/learnMore/intent";
    
    private final Action urlAction = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent e) {
           NativeLaunchUtils.openURL(learnMoreURL);
        }
    };
    
    private boolean agreed = false;
    
    private final Action exitAction = new AbstractAction(I18n.tr("Exit")){
        @Override
        public void actionPerformed(ActionEvent e) {
            finish(false);
        }
    };
    
    private final Action agreeAction = new AbstractAction(I18n.tr("I Agree")){
        @Override
        public void actionPerformed(ActionEvent e) {
            finish(true);
        }
    };    
       
    private Color backgroundColor = Color.WHITE;
    
    public IntentDialog(){
        super();
        setTitle(title);
        setModal(true);
        
        JPanel panel = new JPanel(new MigLayout());
        panel.setBackground(backgroundColor);
        
        JLabel headingLabel = new JLabel(heading);
        FontUtils.bold(headingLabel);
        
        HyperLinkButton linkButton = new HyperLinkButton(linkText, urlAction);
        
        JLabel agreeLabel = new JLabel(agreementText);
        
        MultiLineLabel bodyLabel = new MultiLineLabel(bodyText1, agreeLabel.getPreferredSize().width);        
        
        JButton agreeButton = new JButton(agreeAction);
        
        JButton exitButton = new JButton(exitAction);
        
        
        int leftIndent = 50;
        panel.add(headingLabel, "wrap");
        panel.add(bodyLabel, "gapleft " + leftIndent +  ", gapright " + leftIndent + ", gaptop 50, wrap");
        panel.add(linkButton, "gapleft " + leftIndent  +  ", gapright " + leftIndent +  ", wrap");
        panel.add(agreeLabel, "gapleft " + leftIndent  +  ", gapright " + leftIndent +  ", gaptop 80, gapbottom 100, pushy, wrap");
        panel.add(agreeButton, "tag ok");
        panel.add(exitButton, "tag cancel");
        
        add(panel);
        pack();        
    }
    
    @Override
    public void addNotify() {
        super.addNotify();
        setAlwaysOnTop(true);
        SystemUtils.setWindowTopMost(this);
    }
    
    private void finish(boolean agreed){
        this.agreed = agreed;
        setVisible(false);
        dispose();
    }
    
    public boolean confirmLegal(){
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screenSize.width - getWidth()) / 2, (screenSize.height - getHeight()) / 2);
        setVisible(true);
        return agreed;
    }

}
