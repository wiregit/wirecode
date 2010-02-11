package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.core.api.ActivationTest;
import org.limewire.core.api.Application;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ActivationWindow {

    private final ActivationManager activationManager;
    private final Application application;
    private final ActivationTest activationTest;
    
    private final JDialog dialog;

    @Inject
    public ActivationWindow(ActivationManager activationManager, Application application,
            ActivationTest activationTest) {
        this.activationManager = activationManager;
        this.application = application;
        this.activationTest = activationTest;
        
        dialog = new LimeJDialog(GuiUtils.getMainFrame());
        dialog.setSize(new Dimension(550, 720));            
        dialog.setTitle(I18n.tr("Activation Dump Test"));
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        dialog.add(new JScrollPane(new ActivationPanel()));
    }
        
    public void showDialog() {
        if (dialog.getParent().isVisible()) {
            dialog.setLocationRelativeTo(dialog.getParent());
        } else { 
            dialog.setLocation(GuiUtils.getScreenCenterPoint(dialog));
        }
        
        dialog.setVisible(true);
    }
    
    private class ActivationPanel extends JPanel {
        
        public ActivationPanel() {
            super(new MigLayout(""));
            
            add(createField("Version:"));
            
            add(createField(application.getVersion()), "wrap");
            
            add(createField("HTTP Server Name:"));
            add(createField(activationTest.getHttpServer()), "wrap");
            
            add(createField("Vendor:"));
            add(createField(activationTest.getVendor()), "wrap");
            
            add(createField("mCode:"));
            add(createField(activationManager.getModuleCode()), "wrap");
            
            add(createField("lid:"));
            add(createField(activationManager.getLicenseKey()), "wrap");
//            
//            add(createField("Host:"));
//            add(createField(ActivationSettings.ACTIVATION_HOST.toString()), "wrap");
            
            add(createField("URL info"));
            add(createField(application.addClientInfoToUrl("")));
            
            addActivationModuleInformation(activationManager.getActivationItems());
            
            addTurboChargedDownloadInfo();
            addOptimizedSearchInfo();
            addTechSupportInfo();
            addAVGSupportInfo();
        }
        
        
        
        private void addActivationModuleInformation(List<ActivationItem> items) {
            add(createField("MODULES"), "gaptop 20, span, wrap");
            
            for(ActivationItem item : items) {
                add(createField("Name:"), "gapleft 20, gaptop 20");
                add(createField(item.getLicenseName()), "gaptop 20, wrap");
                
                add(createField("ID:"), "gapleft 20");
                add(createField(item.getModuleID().toString()), "wrap");
                add(createField("Expires:"), "gapleft 20");
                add(createField(item.getDateExpired().toString()), "wrap");
                add(createField("Purchased:"), "gapleft 20");
                add(createField(item.getDatePurchased().toString()), "wrap");
                add(createField("Status:"), "gapleft 20");
                add(createField(item.getStatus().toString()), "wrap");
                add(createField("URL:"), "gapleft 20");
                add(createField(item.getURL()), "wrap");
            }
        }
        
        private void addTurboChargedDownloadInfo() {
            add(createField("TURBO DOWNLOAD MODULE"), "gaptop 20, span, wrap");

            add(createField("Is Active?"), "gapleft 20, gaptop 10");
            add(createField(String.valueOf(activationManager.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE))), "gaptop 10, wrap");
            add(createField("# UltraPeers:"), "gapleft 20");
            add(createField(String.valueOf(activationTest.getNumUltraPeers())), "wrap");
            add(createField("SWARM:"), "gapleft 20");
            add(createField(""), "wrap");            
        }
        
        private void addOptimizedSearchInfo() {
            add(createField("OPTIMIZED SEARCH MODULE"), "gaptop 20, span, wrap");
            
            add(createField("Is Active?"), "gapleft 20, gaptop 10");
            add(createField(String.valueOf(activationManager.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE))), "gaptop 10, wrap");
            add(createField("# Search Results:"), "gapleft 20");
            add(createField(String.valueOf(activationTest.getNumResults())), "wrap");
            add(createField("Can query DHT:"), "gapleft 20");
            add(createField(""), "wrap");
            add(createField("What is New Request:"), "gapleft 20");
            add(createField(""), "wrap");
        
        }
        
        private void addTechSupportInfo() {
            add(createField("TECH SUPPORT MODULE"), "gaptop 20, span, wrap");
            
            add(createField("Is Active?"), "gapleft 20, gaptop 10");
            add(createField(String.valueOf(activationManager.isActive(ActivationID.TECH_SUPPORT_MODULE))), "gaptop 10, wrap");
        }
        
        private void addAVGSupportInfo() {
            add(createField("AVG MODULE"), "gaptop 20, span, wrap");
            
            add(createField("Is Active?"), "gapleft 20, gaptop 10");
            add(createField(String.valueOf(activationManager.isActive(ActivationID.AVG_MODULE))), "gaptop 10, wrap");
        }
        
    }
    
    private static JTextField createField(String text) {
        
        JTextField text1 = new JTextField(text);
        text1.setOpaque(false);
        text1.setBorder(BorderFactory.createEmptyBorder());
        text1.setEditable(false);
        text1.setCaretPosition(0);
        text1.setMargin(new Insets(0,4,0,4));
        return text1;
    }
    
}
