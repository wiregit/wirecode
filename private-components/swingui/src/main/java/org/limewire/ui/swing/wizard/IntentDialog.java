package org.limewire.ui.swing.wizard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.painter.GenericBarPainter;
import org.limewire.ui.swing.painter.GreenButtonBackgroundPainter;
import org.limewire.ui.swing.painter.LightButtonBackgroundPainter;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.util.SystemUtils;

public class IntentDialog extends LimeJDialog {

    private final Color backgroundColor = Color.WHITE;
    private final Font headingFont = new Font("Arial", Font.BOLD, 16);
    private final Font normalFont = new Font("Arial", Font.PLAIN, 14);
    private final Font buttonFont = new Font("Arial", Font.BOLD, 14);
    
    
    private final Color headerGradientTop = new Color(0xf4f4f4);
    private final Color headerGradientBottom = new Color(0xd7d7d7);
    private final Paint headerTopBorder1 = new Color(0xffffff);
    private final Paint headerTopBorder2 = PainterUtils.TRASPARENT; 
    private final Paint headerBottomBorder1 = new Color(0x696969);
    private final Paint headerBottomBorder2 = new Color(0xffffff);
    
    
    private final JLabel headingLabel;
    private final MultiLineLabel bodyLabel;
    private final JLabel agreeLabel;
    private final JButton linkButton;
    private final JLabel languageLabel;
    private final JXButton agreeButton;
    private final JXButton exitButton;
    
    private final String learnMoreURL = "http://www.limewire.com/learnMore/intent";
    
    private final Action urlAction = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent e) {
           NativeLaunchUtils.openURL(learnMoreURL);
        }
    };
    
    private boolean agreed = false;
    
    public IntentDialog(String version){
        super();
        
        ResizeUtils.forceSize(this, new Dimension(514,402));
                
        setTitle("LimeWire " + version);
        setModal(true);
        setResizable(false);
        setAlwaysOnTop(true);
       
        JPanel contentPane = new JPanel(new BorderLayout());
        contentPane.setBackground(backgroundColor);
        add(contentPane);
        JPanel panel = new JPanel(new MigLayout("nogrid, insets 0, gap 0"));
        panel.setOpaque(false);
        
        headingLabel = new JLabel();
        headingLabel.setFont(headingFont);
        agreeLabel = new MultiLineLabel("", 450);
        agreeLabel.setFont(normalFont);
        linkButton = new HyperlinkButton(urlAction);
        linkButton.setFont(normalFont);
        FontUtils.underline(linkButton);
        linkButton.setForeground(new Color(0x2152a6));
        bodyLabel = new MultiLineLabel("", 450);        
        bodyLabel.setFont(normalFont);
        languageLabel = new JLabel();
        languageLabel.setFont(normalFont);
        agreeButton = new JXButton();
        decorateButton(agreeButton);
        agreeButton.setBackgroundPainter(new GreenButtonBackgroundPainter());
        exitButton = new JXButton();
        decorateButton(exitButton);
        exitButton.setBackgroundPainter(new LightButtonBackgroundPainter(6,6));

        JComboBox languageDropDown = createLanguageDropDown(normalFont);
                
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel outerPanel = new JPanel(new BorderLayout());
        JPanel innerPanel = new JPanel(new MigLayout("gap 6, insets 0, fill"));
        JPanel langInnerPanel = new JPanel(new MigLayout("flowy, gap 4, insets 0, fill"));
        
        bottomPanel.setOpaque(false);
        outerPanel.setOpaque(false);
        innerPanel.setOpaque(false);
        langInnerPanel.setOpaque(false);
        
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0,14,14,14));
        
        JXPanel headerBar = new JXPanel(new MigLayout("insets 14, gap 0, fill"));
        headerBar.setBackgroundPainter(new GenericBarPainter<JXPanel>(
                new GradientPaint(0,0, headerGradientTop, 0,1, headerGradientBottom, false),
                headerTopBorder1, headerTopBorder2, headerBottomBorder1, headerBottomBorder2));
        
        int indent = 14;
        headerBar.add(headingLabel, "grow, wrap");
        panel.add(bodyLabel, "gapleft " + indent + ", gaptop 10, wrap");
        panel.add(linkButton, "gapleft " + indent +  ", gaptop 20, wrap");
        panel.add(agreeLabel, "gapleft " + indent +  ", gaptop 70, wrap");

        langInnerPanel.add(languageLabel);
        langInnerPanel.add(languageDropDown);
        bottomPanel.add(langInnerPanel, BorderLayout.WEST);
        
        innerPanel.add(agreeButton);
        innerPanel.add(exitButton);
        outerPanel.add(innerPanel, BorderLayout.SOUTH);
        bottomPanel.add(outerPanel, BorderLayout.EAST);
        
        setTextContents();
        
        contentPane.add(headerBar, BorderLayout.NORTH);
        contentPane.add(panel, BorderLayout.CENTER);
        contentPane.add(bottomPanel, BorderLayout.SOUTH);
        
        pack();        
    }
    
    /**
     * Helper method to prep (size/unskin) the agree and exit buttons for painting
     */
    private void decorateButton(JXButton button) {
        button.setFont(buttonFont);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(0,10,3,10));
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        button.setMinimumSize(new Dimension(105, 32));
        button.setPreferredSize(new Dimension(105, 32));
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
    
    /**
     * Creates the language combo box, attempts to guess an appropriate *available* locale to select,
     *  and sets the application locale to that match.
     */
    private JComboBox createLanguageDropDown(Font normalFont) {
        final JComboBox languageDropDown = new JComboBox();
        Locale[] locales = LanguageUtils.getLocales(normalFont);
        languageDropDown.setRenderer(new LocaleRenderer());
        languageDropDown.setFont(normalFont);
        languageDropDown.setModel(new DefaultComboBoxModel(locales));
        
        // Attempt to guess the default locale and set accordingly
        languageDropDown.setSelectedItem(LanguageUtils.guessBestAvailableLocale(locales));
        
        // Make sure the drop down and the set locale match.  This may cause the default OS
        //  language to be overridden to English in the case of a bad guess.  This has always been
        //  a problem but in this case at least it will be obvious that the users language is being
        //  overridden.
        LanguageUtils.setLocale((Locale)languageDropDown.getSelectedItem());
                
        languageDropDown.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    Locale locale = (Locale) languageDropDown.getSelectedItem();
                    LanguageUtils.setLocale(locale);
                    setTextContents();
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
            }
            
            return this;
        }
    }

    
    /**
     * Allows a soft localised refresh of the text within the panel based
     *  on the language selected in the combo box.
     */
    private void setTextContents() {

        String heading  = I18n.tr("State Your Intent");
        String bodyText1
        = I18n.tr("LimeWire Basic and LimeWire PRO are peer-to-peer programs for sharing authorized files only.  Installing and using either program does not constitute a license for obtaining or distributing unauthorized content.");
        String linkText = I18n.tr("Learn more");
        String agreementText = I18n.tr("By clicking \"I Agree\", you agree to not use LimeWire for copyright infringement.");
        String languageText = I18n.tr("Choose your language");
        
        
        Action exitAction = new AbstractAction(I18n.tr("Exit")){
            @Override
            public void actionPerformed(ActionEvent e) {
                finish(false);
            }
        };
        
        Action agreeAction = new AbstractAction(I18n.tr("I Agree")){
            @Override
            public void actionPerformed(ActionEvent e) {
                finish(true);
            }
        };    
        
        headingLabel.setText(heading);
        bodyLabel.setText(bodyText1);
        linkButton.setText(linkText);
        agreeLabel.setText(agreementText);
        languageLabel.setText(languageText);
        exitButton.setAction(exitAction);
        agreeButton.setAction(agreeAction);
    }
}
