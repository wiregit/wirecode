package org.limewire.ui.swing.pro;

import java.awt.Dialog.ModalityType;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.HTMLPane.LoadResult;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.statusbar.ProStatusPanel;
import org.limewire.ui.swing.statusbar.ProStatusPanel.InvisibilityCondition;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;

public class ProNagController {

    private final boolean isFirstLaunch;
    private final Application application;
    private final ProNag proNag;
    private final ProStatusPanel proStatusPanel;
    
    private boolean nagShown;

    @Inject ProNagController(Application application, ProNag proNag, ProStatusPanel proStatusPanel) {
        isFirstLaunch = !InstallSettings.UPGRADED_TO_5.getValue();
        this.proNag = proNag;
        this.application = application;
        this.proStatusPanel = proStatusPanel;
    }

    public void allowProNag()  {
        if(!application.isProVersion() && !nagShown) {
            assert SwingUtilities.isEventDispatchThread();
            nagShown = true;
           
            proNag.loadContents(isFirstLaunch).addFutureListener(new EventListener<FutureEvent<LoadResult>>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(FutureEvent<LoadResult> event) {
                    switch (event.getType()) {
                    case SUCCESS:
                        // note: success doesn't mean it hit the server, success
                        // means it loaded without throwing an Exception.
                        // it could have loaded the offline page.
                        // (event.getResult() == LoadResult.SERVER_PAGE means it hit the server,
                        //  event.getResult() == LoadResult.OFFLINE_PAGE means it is using the offline page)
                        if(proNag.hasContent()) {
                            // not a LimeJDialog because we don't need/want the icon or the ESC action
                            JDialog dialog = new JDialog(GuiUtils.getMainFrame(), ModalityType.APPLICATION_MODAL);
                            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                            dialog.setResizable(false);
                            proNag.setContainer(dialog);
                            if(proNag.isUndecorated()) {
                                dialog.setUndecorated(true);
                            }
                            if(proNag.getTitle() != null) {
                                dialog.setTitle(proNag.getTitle());
                            }
                            dialog.getContentPane().add(proNag);
                            dialog.pack();
                            dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
                            proStatusPanel.addCondition(InvisibilityCondition.PRO_ADD_SHOWN);
                            dialog.setVisible(true);
                            proStatusPanel.removeCondition(InvisibilityCondition.PRO_ADD_SHOWN);
                        }
                    }
                }
            });
        }
    }

}
