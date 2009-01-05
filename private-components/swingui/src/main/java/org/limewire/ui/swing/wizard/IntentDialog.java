package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.SystemUtils;

public class IntentDialog extends LimeJDialog {

    private final Color backgroundColor = Color.WHITE;
    private final Font headingFont = new Font("Arial", Font.BOLD, 16);
    private final Font normalFont = new Font("Arial", Font.PLAIN, 14);
    
    
    private final String title = I18n.tr("LimeWire 5 Alpha");
    private final String heading  = " ";
    
    private final String bodyText1 
    = I18n.tr("LimeWire Basic and LimeWire PRO are peer-to-peer programs for sharing authorized files only.  Installing and using either program does not constitute a license for obtaining or distributing unauthorized content.");
    
    private final String linkText = I18n.tr("Learn more");
    
    private final String agreementText = I18n.tr("By clicking \"I Agree\", you agree to not use LimeWire 5 for copyright infringement.");
    
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
    
    public IntentDialog(){
        super();
        
        setMinimumSize(new Dimension(590,400));
        setMaximumSize(new Dimension(590,400));
        setPreferredSize(new Dimension(590,400));
        setSize(new Dimension(590,400));
        
        setTitle(title);
        setModal(true);
        setResizable(false);
        setAlwaysOnTop(true);
       
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(backgroundColor);
        add(contentPane);
        JPanel panel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
        panel.setOpaque(false);
        
        JLabel headingLabel = new JLabel(heading);
        headingLabel.setFont(headingFont);
        JLabel agreeLabel = new MultiLineLabel(agreementText, 500);
        agreeLabel.setFont(normalFont);
        HyperlinkButton linkButton = new HyperlinkButton(linkText, urlAction);
        linkButton.setFont(normalFont);
        linkButton.setForeground(new Color(0x2152a6));
        MultiLineLabel bodyLabel = new MultiLineLabel(bodyText1, 500);        
        bodyLabel.setFont(normalFont);
        JButton agreeButton = new JButton(agreeAction);
        JButton exitButton = new JButton(exitAction);
        
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);
        JPanel innerPanel = new JPanel(new FlowLayout());
        innerPanel.setOpaque(false);
        innerPanel.add(agreeButton);
        innerPanel.add(exitButton);
        bottomPanel.add(innerPanel, BorderLayout.EAST);
        
        int indent = 50;
        panel.add(headingLabel, "gapleft 10, gaptop 15, wrap");
        panel.add(bodyLabel, "gapleft " + indent + ", gaptop 20, wrap");
        panel.add(linkButton, "gapleft " + indent +  ", gaptop 20, wrap");
        panel.add(agreeLabel, "gapleft " + indent +  ", gaptop 70, wrap");
        
        JComboBox languageDropDown = createLanguageDropDown(normalFont);        
        panel.add(languageDropDown, "gapleft " + indent +  ", gaptop 70, wrap");
        
        contentPane.add(panel, BorderLayout.NORTH);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        
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
        setLocationRelativeTo(null);
        setVisible(true);
        return agreed;
    }
    
    
    private static JComboBox createLanguageDropDown(Font normalFont) {
        final JComboBox languageDropDown = new JComboBox();
        Locale[] locales = LanguageUtils.getLocales(normalFont);
        languageDropDown.setRenderer(new LocaleRenderer());
        languageDropDown.setFont(normalFont);
        languageDropDown.setModel(new DefaultComboBoxModel(locales));
        Locale locale = LanguageUtils.guessLocale();
        languageDropDown.setSelectedItem(locale);
        languageDropDown.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Locale locale = (Locale) languageDropDown.getSelectedItem();
                    LanguageUtils.setLocale(locale);
                }
            }
            
        });
        
        return languageDropDown;
    }
    
    
    private static class LocaleRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            
            if (value instanceof Locale) {
                Locale locale = (Locale) value;
                setText(locale.getDisplayName(locale));
            } else {
                setIcon(null);
            }
            
            return this;
        }
    }

}
