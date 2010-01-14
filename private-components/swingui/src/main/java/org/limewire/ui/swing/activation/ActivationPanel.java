package org.limewire.ui.swing.activation;

import java.awt.BorderLayout;
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
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.listener.EventListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.activation.ActivationWarningPanel.Mode;
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
    private Icon unsupportedIcon;
    
    private final static String NO_LICENSE_PANEL = "NO_LICENSE_PANEL";
    private final static String EDIT_PANEL = "EDIT_PANEL";
    private final static String OK_PANEL = "OK_PANEL";
    
    private final ActivationManager activationManager;
    private ActivationListener listener;

    private JPanel activationPanel;
    private JDialog dialog;
    private LicenseKeyTextField licenseField;
    private LicenseKeyPanel licenseKeyPanel;
    private ActivationWarningPanel warningPanel;
    private JButton editButton;
    private JXLayer tableJXLayer;
    private ActivationTable table;
    ColoredBusyLabel tableOverlayBusyLabel;
    private JLabel licenseKeyErrorLabel;
    private JLabel licenseTableErrorLabel;
    private JLabel licenseTableInfoLabel;
    private UnsupportedMessagePanel unsupportedMessagePanel;
    private JPanel cardPanel;
    private CardLayout cardLayout;
    
    private EventList<ActivationItem> eventList;
    
    Map<String, ButtonPanel> cardMap = new HashMap<String, ButtonPanel>();
    private String selectedCard = null;
    private boolean isRefreshing = false;
    
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
        
        licenseField = new LicenseKeyTextField();
        licenseField.setFont(font);
        licenseField.setForeground(fontColor);
        licenseField.addActionListener(new ActivateAction("", ""));
        TextFieldClipboardControl.install(licenseField);

        licenseKeyPanel = new LicenseKeyPanel(licenseField);
        
        warningPanel = new ActivationWarningPanel();
        
        editButton = new JButton(new EditAction());
        
        licenseKeyErrorLabel = new JLabel(" ");
        licenseKeyErrorLabel.setFont(font);
        licenseKeyErrorLabel.setForeground(errorColor);
        
        licenseTableErrorLabel = new JLabel();
        licenseTableErrorLabel.setFont(font);
        licenseTableErrorLabel.setForeground(errorColor);
        
        licenseTableInfoLabel = new JLabel(I18n.tr("Some modules have not been activated."));
        licenseTableInfoLabel.setFont(font);
        licenseTableInfoLabel.setForeground(Color.GRAY);
        licenseTableInfoLabel.setVisible(areThereProblematicModules());
        
        unsupportedMessagePanel = new UnsupportedMessagePanel();
        unsupportedMessagePanel.setVisible(false);
        
        table = new ActivationTable(eventList);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        tableJXLayer = new JXLayer<JComponent>(scrollPane);
        scrollPane.setMinimumSize(new Dimension(350, 75 + eventList.size() * 29));
        scrollPane.setPreferredSize(new Dimension(350, 75 + eventList.size() * 29));
        tableOverlayBusyLabel = new ColoredBusyLabel(new Dimension(20,20));
        JPanel busyLabelPanel = new JPanel(new MigLayout("align 50% 50%"));
        busyLabelPanel.add(Box.createVerticalStrut(10), "wrap");
        busyLabelPanel.add(tableOverlayBusyLabel);
        busyLabelPanel.setOpaque(false);
        tableJXLayer.getGlassPane().setLayout(new BorderLayout());
        tableJXLayer.getGlassPane().add(busyLabelPanel, BorderLayout.CENTER);
        tableJXLayer.getGlassPane().setVisible(false);

        activationPanel.add(licenseKeyErrorLabel, "skip 1, span, wrap");
        
        activationPanel.add(licenseKey, "gapright 10");
        activationPanel.add(licenseKeyPanel, "grow, push");
        activationPanel.add(warningPanel.getComponent(), "gapleft 6, aligny 50%");
        activationPanel.add(editButton, "gapleft 40, wrap");
        
        activationPanel.add(licenseTableErrorLabel, "span, growx, gaptop 6, gapbottom 6, hidemode 3");
        activationPanel.add(licenseTableInfoLabel, "span, growx, gaptop 6, gapbottom 6, hidemode 0, wrap");
        
        activationPanel.add(tableJXLayer, "span, grow, wrap, gapbottom 10");
        
        //activationPanel.add(unsupportedMessagePanel, "hidemode 3, span, grow, wrap");
        
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setOpaque(false);
        
        activationPanel.add(cardPanel, "span, growx, wrap, gapbottom 20");
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
        dialog.setTitle(I18n.tr("LimeWire"));
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
            cardMap.get(selectedCard).setActivationEnabled(state != ActivationState.ACTIVATING);
        }
        switch(state) {
        case UNINITIALIZED:
        case NOT_ACTIVATED:
            editButton.setVisible(false);
            licenseKeyPanel.setEditable(true);
            warningPanel.setActivationMode(Mode.EMPTY);
            selectCard(NO_LICENSE_PANEL);
            setRefreshing(false);
            eventList.clear();
            return;
        case ACTIVATING:
            licenseTableInfoLabel.setVisible(false);
            if (!isRefreshing)
            {
                editButton.setVisible(false);
                licenseKeyPanel.setEditable(false);
                warningPanel.setActivationMode(Mode.SPINNER);
                if(selectedCard != null && !selectedCard.equals(NO_LICENSE_PANEL) && !selectedCard.equals(EDIT_PANEL)) {
                    selectCard(NO_LICENSE_PANEL);
                }
                return;
            }
            else
            {
                eventList.clear();
                tableOverlayBusyLabel.setBusy(true);
                tableJXLayer.getGlassPane().setVisible(true);
                return;
            }
        case ACTIVATED:
            editButton.setVisible(true);
            licenseKeyPanel.setEditable(false);
            warningPanel.setActivationMode(Mode.EMPTY);
            selectCard(OK_PANEL);
            
            tableOverlayBusyLabel.setBusy(false);
            tableJXLayer.getGlassPane().setVisible(false);

            eventList.clear();
            eventList.addAll(activationManager.getActivationItems());
            
            setRefreshing(false);
            
            licenseTableInfoLabel.setVisible(areThereProblematicModules());

            return;
        }
        throw new IllegalStateException("Unknown state: " + state);
    }
    
    private void setActivationError(ActivationError error) {
        switch(error) {
        case NO_ERROR:
            setLicenseKeyErrorVisible(false);
            setLicenseTableErrorVisible(false);
            return;
        case NO_KEY:
            setLicenseKeyErrorVisible(false);
            setLicenseTableErrorVisible(false);
            return;
        case EXPIRED_KEY:
            licenseTableErrorLabel.setText(I18n.tr("Your license has expired. Click Renew to renew your license."));
            licenseTableErrorLabel.setForeground(errorColor);
            setLicenseKeyErrorVisible(false);
            setLicenseTableErrorVisible(true);
            return;
        case INVALID_KEY:
            clearTable();
            licenseKeyErrorLabel.setText(I18n.tr("Sorry, the key you entered is invalid. Please try again."));
            setLicenseKeyErrorVisible(true);
            setLicenseTableErrorVisible(false);
            warningPanel.setActivationMode(Mode.WARNING);
            return;
        case BLOCKED_KEY:
            clearTable();
            licenseKeyErrorLabel.setText(I18n.tr("Sorry, the key you entered is blocked. It's already in use."));
            setLicenseKeyErrorVisible(true);
            setLicenseTableErrorVisible(false);
            warningPanel.setActivationMode(Mode.WARNING);
            return;
        case COMMUNICATION_ERROR:
            licenseTableErrorLabel.setText(I18n.tr("There was an error refreshing. Please try again."));
            licenseTableErrorLabel.setForeground(errorColor);
            setLicenseKeyErrorVisible(false);
            setLicenseTableErrorVisible(true);
            return;
        }
        throw new IllegalStateException("Unknown state: " + error);
    }
    
    private boolean areThereProblematicModules() {
        for (ActivationItem item : activationManager.getActivationItems()) {
            if (item.getStatus() != ActivationItem.Status.ACTIVE) {
                return true;
            }
        }
        return false;
    }
    
    private void setRefreshing(boolean refreshing) {
        if (refreshing) {
            
        } else {
            tableOverlayBusyLabel.setBusy(false);
            tableJXLayer.getGlassPane().setVisible(false);
        }
        this.isRefreshing = refreshing;
    }
    
    private void clearTable() {
        eventList.clear();
        eventList.add(new LostLicenseItem());
    }
    
    private void enterEditState() {
        editButton.setVisible(false);
        licenseKeyPanel.setEditable(true);
        licenseField.requestFocusInWindow();
        licenseField.selectAll();
        warningPanel.setActivationMode(Mode.EMPTY);

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
    
    private void setLicenseKeyErrorVisible(boolean isVisible) {
        licenseKeyErrorLabel.setVisible(isVisible);
    }
    
    private void setLicenseTableErrorVisible(boolean isVisible) {
        licenseTableErrorLabel.setVisible(isVisible);
    }   
    
    private abstract class ButtonPanel extends JPanel {
        
        public ButtonPanel() {
            super(new MigLayout("fillx, gap 0, insets 0"));
            setOpaque(false);
        }
        
        public abstract void setActivationEnabled(boolean enabled);
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
        public void setActivationEnabled(boolean enabled) {
            activateButton.setEnabled(enabled);
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
        public void setActivationEnabled(boolean enabled){
            updateButton.setEnabled(enabled);
        }
    }

    private class ActivatedButtonPanel extends ButtonPanel {

        public ActivatedButtonPanel() {
            JButton refreshButton = new JButton(new RefreshAction(I18n.tr("Refresh"), I18n.tr("Refresh the list of modules associated with the key")));
            JButton okButton = new JButton(new OKDialogAction());
            
            add(refreshButton, "push");
            add(okButton, "split, alignx 100%, tag ok, wrap");
        }
        
        @Override
        public void setActivationEnabled(boolean enabled){
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
            enterEditState();
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
            activationManager.activateKey(licenseField.getText().trim().replaceAll("-", ""));
        }
    }
    
    private class RefreshAction extends AbstractAction {
        public RefreshAction(String name, String description) {
            putValue(Action.NAME, name);
            putValue(Action.SHORT_DESCRIPTION, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            ActivationPanel.this.isRefreshing = true;
            activationManager.activateKey(licenseField.getText().trim().replaceAll("-", ""));
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
