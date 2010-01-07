package org.limewire.ui.swing.activation;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.ColoredBusyLabel;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.TextFieldClipboardControl;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;

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
    @Resource 
    private Icon licenseErrorIcon;
    @Resource
    private Icon unsupportedIcon;
    
    private final static String NO_LICENSE_PANEL = "NO_LICENSE_PANEL";
    private final static String EDIT_PANEL = "EDIT_PANEL";
    private final static String OK_PANEL = "OK_PANEL";
    
    private final ActivationManager activationManager;
    private ActivationListener listener;

    
    private JPanel activationPanel;
    private JDialog dialog;
    private JTextField licenseField;
    private ColoredBusyLabel busyLabel;
    private JLabel licenseErrorLabel;
    private JButton editButton;
    private ActivationTable table;
    private JLabel licenseKeyErrorLabel;
    private JLabel licenseExpirationLabel;
    private UnsupportedMessagePanel unsupportedMessagePanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private EventList<ActivationItem> eventList;
    
    Map<String, ButtonPanel> cardMap = new HashMap<String, ButtonPanel>();
    private String selectedCard = null;
    
    @Inject
    public ActivationPanel(ActivationManager activationManager) {
        this.activationManager = activationManager;
        listener = new ActivationListener();
        
        eventList = new BasicEventList<ActivationItem>();
        clearTable();
        
        activationPanel = new JPanel(new MigLayout("fill, gap 0, insets 10 20 18 20"));
        
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
        licenseField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_ENTER && licenseField.isEditable()) {
                    activationManager.activateKey(licenseField.getText().trim());
                }
            }
        });
        TextFieldClipboardControl.install(licenseField);
        
        busyLabel = new ColoredBusyLabel(new Dimension(20,20));
        busyLabel.setVisible(false);
        
        licenseErrorLabel = new JLabel(licenseErrorIcon);
        licenseErrorLabel.setVisible(false);
        
        editButton = new JButton(new EditAction());
        
        licenseKeyErrorLabel = new JLabel(" ");
        licenseKeyErrorLabel.setFont(font);
        licenseKeyErrorLabel.setForeground(errorColor);
        
        licenseExpirationLabel = new JLabel(I18n.tr("Your license has expired. Click Renew to renew your license."));
        licenseExpirationLabel.setFont(font);
        licenseExpirationLabel.setForeground(errorColor);
        
        unsupportedMessagePanel = new UnsupportedMessagePanel();
//        unsupportedMessagePanel.setVisible(false);
        
        table = new ActivationTable(eventList);
        JScrollPane scrollPane = new JScrollPane(table);
        
        activationPanel.add(licenseKeyErrorLabel, "skip 1, span 2, growx, wrap");
        
        activationPanel.add(licenseKey, "gapright 10");
        activationPanel.add(licenseField, "grow, push");
        activationPanel.add(busyLabel, "gapleft 6, aligny 50%, hidemode 2");
        activationPanel.add(licenseErrorLabel, "gapleft 6, aligny 50%, hidemode 2");
        activationPanel.add(editButton, "gapleft 40, wrap");
        
        activationPanel.add(licenseExpirationLabel, "span, growx, gaptop 6, gapbottom 6, wrap");
        
        activationPanel.add(scrollPane, "span, grow, wrap, gapbottom 20");
        
//        activationPanel.add(unsupportedMessagePanel, "span, grow, wrap");
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        
        activationPanel.add(cardPanel, "span, growx, wrap");
    }
    
    @Inject
    public void register() {
        activationManager.addListener(listener);
    }

    public void show() {
        // Setting the ActivationState and ErrorState initialize all of the
        // fields and views before being shown.
        licenseField.setText(activationManager.getLicenseKey());
        setActivationState(activationManager.getActivationState());
        setActivationError(activationManager.getActivationError());
        
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
        if(listener != null) {
            activationManager.removeListener(listener);
        }
        if(dialog != null) {
            dialog.dispose();
            dialog = null;
        }
    }
        
    /**
     * Updates the UI information based on the current state of the 
     * ActivationManager.
     */
    private void setActivationState(ActivationState state) {
        if(selectedCard != null) {
            cardMap.get(selectedCard).setState(state);
        }
        switch(state) {
        case UNINITIALIZED:
        case NOT_ACTIVATED:
            editButton.setVisible(false);
            licenseField.setEditable(true);
            setBusyLabel(false);
            selectCard(NO_LICENSE_PANEL);
            return;
        case ACTIVATING:
            editButton.setVisible(false);
            licenseField.setEditable(false);
            setBusyLabel(true);
            if(!selectedCard.equals(NO_LICENSE_PANEL) && !selectedCard.equals(EDIT_PANEL)) {
                selectCard(NO_LICENSE_PANEL);
            }
            return;
        case ACTIVATED:
            editButton.setVisible(true);
            licenseField.setEditable(false);
            setBusyLabel(false);
            selectCard(OK_PANEL);

            eventList.clear();
            eventList.addAll(activationManager.getActivationItems());
            return;
        }
        throw new IllegalStateException("Unknown state: " + state);
    }
    
    private void setActivationError(ActivationError error) {
        switch(error) {
        case NO_ERROR:
            setLicenseKeyErrorVisible(false);
            setLicenseExperiationVisible(false);
            licenseErrorLabel.setVisible(false);
            return;
        case NO_KEY:
            setLicenseKeyErrorVisible(false);
            setLicenseExperiationVisible(false);
            licenseErrorLabel.setVisible(false);
            return;
        case EXPIRED_KEY:
            setLicenseKeyErrorVisible(false);
            setLicenseExperiationVisible(true);
            licenseErrorLabel.setVisible(false);
            return;
        case INVALID_KEY:
            clearTable();
            licenseKeyErrorLabel.setText(I18n.tr("Sorry, the key you entered is invalid. Please try again."));
            setLicenseKeyErrorVisible(true);
            setLicenseExperiationVisible(false);
            return;
        case BLOCKED_KEY:
            clearTable();
            licenseKeyErrorLabel.setText(I18n.tr("Sorry, the key you entered is blocked. It's already in use."));
            setLicenseKeyErrorVisible(true);
            setLicenseExperiationVisible(false);
            licenseErrorLabel.setVisible(true);
            return;
        }
        throw new IllegalStateException("Unknown state: " + error);
    }
    
    private void clearTable() {
        eventList.clear();
        eventList.add(new LostLicenseItem());
    }
    
    private void setEditState() {
        editButton.setVisible(false);
        licenseField.setEditable(true);
        licenseField.requestFocusInWindow();
        licenseField.selectAll();
        setBusyLabel(false);
        selectCard(EDIT_PANEL);
    }
    
    private void selectCard(String card) {
        if(!cardMap.containsKey(card)) {
            if(card.equals(NO_LICENSE_PANEL)) {
                ButtonPanel panel = new NoLicenseButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            } else if(card.equals(OK_PANEL)) {
                ButtonPanel panel = new ActivatedButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            } else if(card.equals(EDIT_PANEL)) {
                ButtonPanel panel = new EditButtonPanel();
                cardMap.put(card, panel);
                cardPanel.add(panel, card);
            }
        }
        selectedCard = card;
        cardLayout.show(cardPanel, card);
    }
    
    private void setBusyLabel(boolean isVisible) {
        busyLabel.setVisible(isVisible);
        busyLabel.setBusy(isVisible);
    }
    
    private void setLicenseKeyErrorVisible(boolean isVisible) {
        licenseKeyErrorLabel.setVisible(isVisible);
    }
    
    private void setLicenseExperiationVisible(boolean isVisible) {
        licenseExpirationLabel.setVisible(isVisible);
    }   
    
    private abstract class ButtonPanel extends JPanel {
        
        public ButtonPanel() {
            super(new MigLayout("fillx, gap 0, insets 0"));
            setOpaque(false);
        }
        
        public abstract void setState(ActivationState state);
    }
    
    private class NoLicenseButtonPanel extends ButtonPanel {
        private JButton activateButton;
        
        public NoLicenseButtonPanel() {
            JButton goProButton = new JButton(new UrlAction(I18n.tr("Go Pro"),"http://www.limewire.com/client_redirect/?page=gopro"));
            activateButton = new JButton(new ActivateAction(I18n.tr("Activate"), I18n.tr("Activate the License Key")));
            JButton laterButton = new JButton(new OKDialogAction(I18n.tr("Later"), I18n.tr("Activate License at a later time")));
            
            add(goProButton, "push");
            add(activateButton, "split, tag ok");
            add(laterButton, "tag cancel, wrap");
        }
        
        @Override
        public void setState(ActivationState state) {
            activateButton.setEnabled(state != ActivationState.ACTIVATING);
        }
    }

    private class EditButtonPanel extends ButtonPanel {
        private JButton updateButton;
        
        public EditButtonPanel() {
            updateButton = new JButton(new ActivateAction(I18n.tr("Update"), I18n.tr("Update the saved key")));
            JButton cancelButton = new JButton(new CancelAction());
            
            add(updateButton, "alignx 100%, gapright 10, tag ok, split");
            add(cancelButton, "wrap, tag cancel");
        }
        
        @Override
        public void setState(ActivationState state) {
            updateButton.setEnabled(state != ActivationState.ACTIVATING);
        }
    }
    
    private class ActivatedButtonPanel extends ButtonPanel {

        public ActivatedButtonPanel() {
            JButton okButton = new JButton(new OKDialogAction());
            
            add(okButton, "alignx 100%, tag ok, wrap");
        }
        
        @Override
        public void setState(ActivationState state) {
            //NO STATE CHANGE
        }
    }
    
    private class UnsupportedMessagePanel extends JPanel {
        public UnsupportedMessagePanel() {
            
            add(new JLabel(unsupportedIcon), "aligny 50%, spany");
            add(new MultiLineLabel(I18n.tr("You're current version of LimeWire does not support one or more of your purchased features.")),"grow");
        }
    }
    
    private class EditAction extends AbstractAction {

        public EditAction() {
            putValue(Action.NAME, I18n.tr("Edit Key"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Edit the saved License Key"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            setEditState();
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
    
    private class ActivateAction extends AbstractAction {
        public ActivateAction(String name, String description) {
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            activationManager.activateKey(licenseField.getText().trim());
        }
    }
    
    private class ActivationListener implements EventListener<ActivationEvent> {
        @Override
        public void handleEvent(final ActivationEvent event) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    setActivationState(event.getData());
                    setActivationError(event.getError());                  
                }
            });
        }
    }
}
