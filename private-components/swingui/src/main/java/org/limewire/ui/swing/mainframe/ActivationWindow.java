package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import net.miginfocom.swing.MigLayout;

import org.limewire.activation.api.ActivationID;
import org.limewire.activation.api.ActivationItem;
import org.limewire.activation.api.ActivationManager;
import org.limewire.core.api.ActivationTest;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.util.LimeWireUtils;

public class ActivationWindow {

    private final ActivationManager activationManager;
    private final ApplicationServices applicationServices;
    private final ActivationTest activationTest;
    
    private final JDialog dialog;

    @Inject
    public ActivationWindow(ActivationManager activationManager, ApplicationServices applicationServices,
            ActivationTest activationTest) {
        this.activationManager = activationManager;
        this.applicationServices = applicationServices;
        this.activationTest = activationTest;
        
        dialog = new LimeJDialog(GuiUtils.getMainFrame());
        dialog.setSize(new Dimension(450, 400));            
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
            
            add(new JLabel("Version:"));
            add(new JLabel(LimeWireUtils.getLimeWireVersion()), "wrap");
            
            add(new JLabel("HTTP Server Name:"));
            add(new JLabel(LimeWireUtils.getHttpServer()), "wrap");
            
            add(new JLabel("Vendor:"));
            add(new JLabel(LimeWireUtils.getVendor()), "wrap");
            
            add(new JLabel("mCode:"));
            add(new JLabel(activationManager.getMCode()), "wrap");
            
            add(new JLabel("lid:"));
            add(new JLabel(activationManager.getLicenseKey()), "wrap");
            
            add(new JLabel("URL info"));
            add(new JLabel(LimeWireUtils.addLWInfoToUrl("", applicationServices.getMyGUID(), 
                    activationManager.isProActive(), activationManager.getLicenseKey(), activationManager.getMCode())), "wrap");
            
            addActivationModuleInformation(activationManager.getActivationItems());
            
            addTurboChargedDownloadInfo();
            addOptimizedSearchInfo();
            addTechSupportInfo();
            addAVGSupportInfo();
        }
        
        private void addActivationModuleInformation(List<ActivationItem> items) {
            add(new JLabel("MODULES"), "gaptop 20, span, wrap");
            
            for(ActivationItem item : items) {
                add(new JLabel("Name:"), "gapleft 20, gaptop 20");
                add(new JLabel(item.getLicenseName()), "gaptop 20, wrap");
                
                add(new JLabel("ID:"), "gapleft 20");
                add(new JLabel(item.getModuleID().toString()), "wrap");
                add(new JLabel("Expires:"), "gapleft 20");
                add(new JLabel(item.getDateExpired().toString()), "wrap");
                add(new JLabel("Purchased:"), "gapleft 20");
                add(new JLabel(item.getDatePurchased().toString()), "wrap");
                add(new JLabel("Status:"), "gapleft 20");
                add(new JLabel(item.getStatus().toString()), "wrap");
                add(new JLabel("URL:"), "gapleft 20");
                add(new JLabel(item.getURL()), "wrap");
            }
        }
        
        private void addTurboChargedDownloadInfo() {
            add(new JLabel("TURBO DOWNLOAD MODULE"), "gaptop 20, span, wrap");

            add(new JLabel("Is Active?"), "gapleft 20, gaptop 10");
            add(new JLabel(String.valueOf(activationManager.isActive(ActivationID.TURBO_CHARGED_DOWNLOADS_MODULE))), "gaptop 10, wrap");
            add(new JLabel("# UltraPeers:"), "gapleft 20");
            add(new JLabel(String.valueOf(activationTest.getNumUltraPeers())), "wrap");
            add(new JLabel("SWARM:"), "gapleft 20");
            add(new JLabel(""), "wrap");            
        }
        
        private void addOptimizedSearchInfo() {
            add(new JLabel("OPTIMIZED SEARCH MODULE"), "gaptop 20, span, wrap");
            
            add(new JLabel("Is Active?"), "gapleft 20, gaptop 10");
            add(new JLabel(String.valueOf(activationManager.isActive(ActivationID.OPTIMIZED_SEARCH_RESULT_MODULE))), "gaptop 10, wrap");
            add(new JLabel("# Search Results:"), "gapleft 20");
            add(new JLabel(String.valueOf(activationTest.getNumResults())), "wrap");
            add(new JLabel("Can query DHT:"), "gapleft 20");
            add(new JLabel(""), "wrap");
            add(new JLabel("What is New Request:"), "gapleft 20");
            add(new JLabel(""), "wrap");
        
        }
        
        private void addTechSupportInfo() {
            add(new JLabel("TECH SUPPORT MODULE"), "gaptop 20, span, wrap");
            
            add(new JLabel("Is Active?"), "gapleft 20, gaptop 10");
            add(new JLabel(String.valueOf(activationManager.isActive(ActivationID.TECH_SUPPORT_MODULE))), "gaptop 10, wrap");
        }
        
        private void addAVGSupportInfo() {
            add(new JLabel("AVG MODULE"), "gaptop 20, span, wrap");
            
            add(new JLabel("Is Active?"), "gapleft 20, gaptop 10");
            add(new JLabel(String.valueOf(activationManager.isActive(ActivationID.AVG_MODULE))), "gaptop 10, wrap");
        }
        
        
    }
}
