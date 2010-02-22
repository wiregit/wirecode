package org.limewire.ui.swing.options;

import java.awt.Color;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import net.miginfocom.swing.MigLayout;

/**
 * Abstract Option panel for initializing and saving the options within the
 * panel.
 */
public abstract class OptionPanel extends JPanel {

    public static class ApplyOptionResult {
        private boolean restartRequired;

        private boolean successful;

        ApplyOptionResult() {
        }

        ApplyOptionResult(boolean restartReq, boolean isSuccess) {
            setRestartRequired(restartReq);
            setSuccessful(isSuccess);
        }

        ApplyOptionResult(ApplyOptionResult res) {
            setRestartRequired(res.isRestartRequired());
            setSuccessful(res.isSuccessful());
        }

        public boolean isRestartRequired() {
            return restartRequired;
        }

        public void setRestartRequired(boolean restartRequired) {
            this.restartRequired = restartRequired;
        }

        public boolean isSuccessful() {
            return successful;
        }

        public void setSuccessful(boolean successful) {
            this.successful = successful;
        }
        
        public void applyResult(ApplyOptionResult res){
            setRestartRequired(isRestartRequired() || res.isRestartRequired());
            setSuccessful(isSuccessful() && res.isSuccessful());
        }
    }
    
    public OptionPanel() {
    }

    public OptionPanel(String title) {
        setBorder(BorderFactory.createTitledBorder(null, title, 
                TitledBorder.DEFAULT_JUSTIFICATION, 
                TitledBorder.DEFAULT_POSITION, 
                new Font("Dialog", Font.BOLD, 12), new Color(0x313131)));
        setLayout(new MigLayout("insets 4, fill, nogrid"));
        setOpaque(false);
    }

    /**
     * Initializes the options for this panel. Listeners should not be attached
     * in this method. It will be called multiple times as the options dialog is
     * brought up. To prevent memory leaks or recreating the same components
     * this method should only setup the options and rearrange components as
     * necessary. More heavy weight tasks that should only be done once should
     * be done in the constructor.
     */
    public abstract void initOptions();

    abstract ApplyOptionResult applyOptions();

    abstract boolean hasChanged();
}
