package org.limewire.ui.swing.search.store;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;
import javax.swing.JPanel;

import org.limewire.ui.swing.browser.Browser;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.search.model.VisualStoreResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.MozillaPanel.VisibilityMode;

/**
 * Main container for the File Info dialog for store results.
 */
public class StoreResultInfoPanel extends JPanel {

    private final StoreController storeController;
    
    private MozillaPanel mozillaPanel;
    
    /**
     * Constructs a StoreResultInfoPanel using the specified services.
     */
    public StoreResultInfoPanel(StoreController storeController) {
        this.storeController = storeController;
        
        initComponents();
        
        // TODO add listeners to handle action links like download 
        
    }
    
    /**
     * Initializes the components in the container.
     */
    private void initComponents() {
        setLayout(new BorderLayout());
        
        mozillaPanel = new Browser(VisibilityMode.FORCED_HIDDEN, 
                VisibilityMode.FORCED_HIDDEN, VisibilityMode.DEFAULT);
        mozillaPanel.setPreferredSize(new Dimension(420, 540));
        
        add(mozillaPanel, BorderLayout.CENTER);
    }
    
    /**
     * Displays the File Info dialog for the specified store result.
     */
    public void display(VisualStoreResult vsr) {
        // Load result info into browser.
        mozillaPanel.load(storeController.getInfoURI(vsr));
        
        // Get main frame.
        Frame owner = GuiUtils.getMainFrame();
        
        // Create dialog.
        JDialog dialog = new LimeJDialog(owner);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
        dialog.setTitle(I18n.tr("{0} Properties", vsr.getFileName()));

        // Set content.
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        
        // Position dialog and display.
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
