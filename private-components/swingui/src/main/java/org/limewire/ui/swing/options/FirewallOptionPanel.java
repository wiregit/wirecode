package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Firewall Option Panel
 */
//TODO: convert to numbertextfield
@Singleton
public class FirewallOptionPanel extends OptionPanel {

    private ListeningPortPanel listeningPortPanel;
    private RouterConfigPanel routerConfigPanel;
    
    @Inject
    public FirewallOptionPanel() {
        super();
        setLayout(new MigLayout("insets 10 10 10 10, fillx, wrap", "", ""));
        
        add(getListeningPortPanel(), "pushx, growx");
        add(getRouterConfigPanel(), "pushx, growx");
    }
    
    private OptionPanel getListeningPortPanel() {
        if(listeningPortPanel == null) {
            listeningPortPanel = new ListeningPortPanel();
        }
        return listeningPortPanel;
    }
    
    private OptionPanel getRouterConfigPanel() {
        if(routerConfigPanel == null) {
            routerConfigPanel = new RouterConfigPanel();
        }
        return routerConfigPanel;
    }
    
    @Override
    void applyOptions() {
        getListeningPortPanel().applyOptions();
        getRouterConfigPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getListeningPortPanel().hasChanged() || getRouterConfigPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getListeningPortPanel().initOptions();
        getRouterConfigPanel().initOptions();
    }
    
    private class ListeningPortPanel extends OptionPanel {

        private final String description = I18n.tr("You can set the local network port that listens for incoming connections. This port may be changed in case of conflict with another program or if a specific port number if required for direct incoming connections by your firewall.");
        private JTextField portField;
        
        private int port;
        
        public ListeningPortPanel() {
            super(I18n.tr("Listening Port"));
            
            portField = new JTextField(5);
            //TODO: change this to an integerTextField
            
            add(new MultiLineLabel(description, ReallyAdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "wrap");
            
            add(new JLabel(I18n.tr("Listen on port:")), "split");
            add(portField);
        }
        
        @Override
        void applyOptions() {
//            int currentPort = Integer.parseInt(portField.getText());
            //TODO: NetworkManager API
//            if(currentPort != port) {
//                try {
//                    NetworkSettings.PORT.setValue(port);
//                    networkManager.setListeningPort(port);
//                    port = currentPort;
//                    networkManager.portChanged();
//                } catch(IOException ioe) {
//                    GUIMediator.showError(I18n.tr("Port not available. Please select a different port."));
//                    NetworkSettings.PORT.setValue(port);
//                    portField.setText(Integer.toString(port));
//                    throw new IOException("port not available");
//                } catch(IllegalArgumentException iae) {
//                    GUIMediator.showError(I18n.tr("Please enter a port between 1 and 65535."));
//                    portField.setText(Integer.toString(port));
//                    throw new IOException("invalid port");
//                }
//            }
        }

        @Override
        boolean hasChanged() {
            return NetworkSettings.PORT.getValue() != Integer.parseInt(portField.getText());
        }

        @Override
        public void initOptions() {
            port = NetworkSettings.PORT.getValue();
            portField.setText(Integer.toString(port));
        }
    }
    
    private class RouterConfigPanel extends OptionPanel {

        private final String description = I18n.tr("Using Universal Plug n' Play, LimeWire can automatically configure your router or firewall for optinal performance. If your router does not support Universal Plug n' Play, LimeWire can be set to advertise an external port manually.");
        private final JRadioButton plugAndPlayRadioButton;
        private final JRadioButton portForwardRadioButton;
        private final JTextField portTextField;
        private final JRadioButton doNothingRadioButton;
        
        private final ButtonGroup buttonGroup;
        
        public RouterConfigPanel() {
            super(I18n.tr("Router Configuration"));
            
            plugAndPlayRadioButton = new JRadioButton();
            portForwardRadioButton = new JRadioButton();
            doNothingRadioButton = new JRadioButton();
            
            buttonGroup = new ButtonGroup();
            buttonGroup.add(plugAndPlayRadioButton);
            buttonGroup.add(portForwardRadioButton);
            buttonGroup.add(doNothingRadioButton);
            
            portTextField = new JTextField(5);
            //TODO: change this to an integerTextField
            
            add(new MultiLineLabel(description, ReallyAdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "wrap");
            
            add(plugAndPlayRadioButton, "split");
            add(new JLabel("Use Universal Plug n' Play (Recommended)"), "wrap");
            
            add(portForwardRadioButton, "split");
            add(new JLabel("Manual Port Forward:"), "split");
            add(portTextField,"wrap");
            
            add(doNothingRadioButton, "split");
            add(new JLabel("Do Nothing"));
            
            portForwardRadioButton.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    updateTextField();
                }
            });
        }
        
        @Override
        void applyOptions() {

        }

        @Override
        boolean hasChanged() {
            return false;
        }

        @Override
        public void initOptions() {
            if(ConnectionSettings.FORCE_IP_ADDRESS.getValue() && !ConnectionSettings.UPNP_IN_USE.getValue())
                portForwardRadioButton.setSelected(true);
            else if(ConnectionSettings.DISABLE_UPNP.getValue())
                doNothingRadioButton.setSelected(true);
            else
                plugAndPlayRadioButton.setSelected(true);
                      
            portTextField.setText(Integer.toString(ConnectionSettings.FORCED_PORT.getValue()));
            
            updateTextField();
        }
        
        private void updateTextField() {
            portTextField.setEnabled(portForwardRadioButton.isSelected());
            portTextField.setEditable(portForwardRadioButton.isSelected());
        }
    }

}
