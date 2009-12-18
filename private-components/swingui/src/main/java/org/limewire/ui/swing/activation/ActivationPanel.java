package org.limewire.ui.swing.activation;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.limewire.activation.api.ActivationManager;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ActivationPanel {

    @Resource
    private int width;
    @Resource
    private int height;
    @Resource
    private Font font;
    @Resource
    private Color fontColor;
    @Resource
    private Color errorColor;
    
    private final static String NO_LICENSE_PANEL = "NO_LICENSE_PANEL";
    private final static String EDIT_PANEL = "EDIT_PANEL";
    private final static String OK_PANEL = "OK_PANEL";
    
    /**
     * States the UI can exist in.
     */
    enum ActivationState {
        NO_LICENSE,
        ACTIVATING,
        INVALID_KEY,
        EDIT_KEY,
        EXPIRED_KEY,
        ACTIVATED
    };
    
    private JPanel activationPanel;
    private JDialog dialog;
    private JTextField licenseField;
    private JButton editButton;
    private JXTable table;
    private JLabel licenseKeyErrorLabel;
    private JLabel licenseExpirationLabel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    Map<String, JComponent> cardMap = new HashMap<String, JComponent>();
    
    @Inject
    public ActivationPanel(ActivationManager activationManager) {
        
        activationPanel = new JPanel(new MigLayout("fill, insets 20 20 20 20"));
        
        initComponents();
    }
    
    private void initComponents() {
        GuiUtils.assignResources(this);   
        
        activationPanel.setPreferredSize(new Dimension(width, height));
        activationPanel.setBackground(GuiUtils.getMainFrame().getBackground());
        
        JLabel licenseKey = new JLabel(I18n.tr("License Key" + ":"));
        licenseKey.setFont(font);
        licenseKey.setForeground(fontColor);
        
        licenseField = new JTextField();
        licenseField.setFont(font);
        licenseField.setForeground(fontColor);
        TextFieldClipboardControl.install(licenseField);
        
        editButton = new JButton(new EditAction());
        
        licenseKeyErrorLabel = new JLabel(I18n.tr("Sorry, the key you entered is invalid. Please trye again."));
        licenseKeyErrorLabel.setFont(font);
        licenseKeyErrorLabel.setForeground(errorColor);
        
        licenseExpirationLabel = new JLabel(I18n.tr("Your license has expired. Click Renew to renew your license."));
        licenseExpirationLabel.setFont(font);
        licenseExpirationLabel.setForeground(errorColor);
        
        table = new JXTable();
        JScrollPane scrollPane = new JScrollPane(table);
//        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        
        activationPanel.add(licenseKeyErrorLabel, "skip 1, span 2, growx, wrap");
        
        activationPanel.add(licenseKey, "gapright 10");
        activationPanel.add(licenseField, "grow, push");
        activationPanel.add(editButton, "wrap");
        
        activationPanel.add(licenseExpirationLabel, "span, growx, wrap");
        
        activationPanel.add(scrollPane, "span, grow, wrap, gapbottom 20");
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        
        activationPanel.add(cardPanel, "span, growx, wrap");
    }
    
    public void show() {
        //TODO: ActivationManager.getActivationState();
        //TODO: load the table with data ActivationManager.getActivationData()
        setActivationState(ActivationState.NO_LICENSE);
        
        dialog = new LimeJDialog();
        dialog.setModal(true);
        dialog.setResizable(false);
        dialog.setTitle(I18n.tr("Limewire"));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.add(activationPanel);
        dialog.pack();
        
        dialog.addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
                dispose();
            }

            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {
//                if(okButton != null)
//                    okButton.requestFocusInWindow();
            }
        }); 
        
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setVisible(true);
    }
    
    private void dispose() {
        if(dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }
        
    private void setActivationState(ActivationState state) {
        switch(state) {
            case NO_LICENSE:
                editButton.setVisible(false);
                setLicenseKeyErrorVisible(false);
                setLicenseExperiationVisible(false);
                licenseField.setEditable(true);
                selectCard(NO_LICENSE_PANEL);
                return;
            case ACTIVATING:
                editButton.setVisible(false);
                setLicenseKeyErrorVisible(false);
                setLicenseExperiationVisible(false);
                licenseField.setEditable(false);
                selectCard(NO_LICENSE_PANEL);
                return;
            case INVALID_KEY:
                editButton.setVisible(false);
                setLicenseKeyErrorVisible(true);
                setLicenseExperiationVisible(false);
                licenseField.setEditable(false);
                selectCard(NO_LICENSE_PANEL);
                return;
            case EDIT_KEY:
                editButton.setVisible(false);
                setLicenseKeyErrorVisible(false);
                setLicenseExperiationVisible(false);
                licenseField.setEditable(true);
                selectCard(EDIT_PANEL);
                return;
            case EXPIRED_KEY:
                editButton.setVisible(false);
                setLicenseKeyErrorVisible(false);
                setLicenseExperiationVisible(true);
                licenseField.setEditable(false);
                selectCard(OK_PANEL);
                return;
            case ACTIVATED:
                editButton.setVisible(true);
                setLicenseKeyErrorVisible(false);
                setLicenseExperiationVisible(false);
                licenseField.setEditable(false);
                selectCard(OK_PANEL);
                return;
        }
        throw new IllegalStateException("Unknown state: " + state);
    }
    
    private void selectCard(String card) {
        if(!cardMap.containsKey(card)) {
            if(card.equals(NO_LICENSE_PANEL)) {
                JPanel panel = getNoLicenseButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            } else if(card.equals(OK_PANEL)) {
                JPanel panel = getActivatedLicenseButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            } else if(card.equals(EDIT_PANEL)) {
                JPanel panel = getEditLicenseButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            }
        }
        cardLayout.show(cardPanel, card);
     }
    
    private void setLicenseKeyErrorVisible(boolean isVisible) {
        licenseKeyErrorLabel.setVisible(isVisible);
    }
    
    private void setLicenseExperiationVisible(boolean isVisible) {
        licenseExpirationLabel.setVisible(isVisible);
    }
    
    private JPanel getNoLicenseButtonPanel() {
        JPanel p = new JPanel(new MigLayout("fillx, insets 0"));
        p.setOpaque(false);
        
        JButton goPro = new JButton(new UrlAction(I18n.tr("Go Pro"),"http://www.limewire.com/client_redirect/?page=gopro"));
        JButton activate = new JButton(new ActivateAction());
        JButton later = new JButton(new OKDialogAction(I18n.tr("Later"), I18n.tr("Activate License at a later time")));
        
        p.add(goPro, "push");
        p.add(activate, "split, tag ok");
        p.add(later, "tag cancel");
        
        return p;
    }
    
    private JPanel getEditLicenseButtonPanel() {
        JPanel p = new JPanel(new MigLayout("fillx, insets 0"));
        p.setOpaque(false);
        
        JButton updateButton = new JButton(new UpdateAction());
        JButton cancelButton = new JButton(new CancelAction());
        
        p.add(updateButton, "alignx 100%, tag ok, split");
        p.add(cancelButton, "wrap, tag cancel");
        
        return p;
    }
    
    private JPanel getActivatedLicenseButtonPanel() {
        JPanel p = new JPanel(new MigLayout("fillx, insets 0"));
        p.setOpaque(false);
        
        JButton okButton = new JButton(new OKDialogAction());
        
        p.add(okButton, "alignx 100%, wrap");
        
        return p;
    }
    
    private class EditAction extends AbstractAction {

        public EditAction() {
            putValue(Action.NAME, I18n.tr("Edit Key"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Edit the saved License Key"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            setActivationState(ActivationState.EDIT_KEY);
            licenseField.requestFocusInWindow();
            licenseField.selectAll();
        }
    }
    
    private class CancelAction extends AbstractAction {
        public CancelAction() {
            putValue(Action.NAME, I18n.tr("Cancel"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Cancel editing"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            setActivationState(ActivationState.ACTIVATED);
        }
    }
    
    //TODO: this seems exactly the same as the UpdateAction
    private class ActivateAction extends AbstractAction {
        public ActivateAction() {
            putValue(Action.NAME, I18n.tr("Activate"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Activate the License Key"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO: try contacting the server
            //TODO: key check to avoid hitting the server unnecessarily
            //TODO: what state is this??
//            setActivationState(ActivationState.ACTIVATED);
            setActivationState(ActivationState.ACTIVATING);
        }
    }
    
    private class UpdateAction extends AbstractAction {
        public UpdateAction() {
            putValue(Action.NAME, I18n.tr("Update"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Update the saved key"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO: try contacting the server
            //TODO: key check to avoid hitting the server unnecessarily
            //TODO: what state is this??
            setActivationState(ActivationState.ACTIVATING);
        }
    }
}
