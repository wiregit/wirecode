package org.limewire.ui.swing.pro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.TimerTask;

import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.limewire.activation.api.ActivationError;
import org.limewire.activation.api.ActivationEvent;
import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ActivationState;
import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.core.settings.InstallSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.components.HTMLPane.LoadResult;
import org.limewire.ui.swing.pro.ProNag.NagContainer;
import org.limewire.ui.swing.statusbar.ProStatusPanel;
import org.limewire.ui.swing.statusbar.ProStatusPanel.InvisibilityCondition;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProNagController {

    private final boolean isFirstLaunch;
    private final ProNag proNag;
    private final ProStatusPanel proStatusPanel;
    
    private final Provider<BlockedNotificationDialog> blockedDialogProvider;
    private final ActivationManager activationManager;
    private ActivationListener listener;
    private boolean isLicenseBlocked = false;
    private boolean isLimeWireRefreshingLicense = false;
    private boolean isNewInstall = false;
    
    private boolean nagShown;

    @Inject ProNagController(ProNag proNag, ProStatusPanel proStatusPanel, 
                             Provider<BlockedNotificationDialog> blockedDialogProvider, 
                             ActivationManager activationManager,
                             Application application) {
        isFirstLaunch = !InstallSettings.UPGRADED_TO_5.getValue();
        this.proNag = proNag;
        this.proStatusPanel = proStatusPanel;
        
        this.blockedDialogProvider = blockedDialogProvider;
        this.activationManager = activationManager;
        isLicenseBlocked = activationManager.getActivationState() == ActivationState.NOT_AUTHORIZED && activationManager.getActivationError() == ActivationError.BLOCKED_KEY;
        isLimeWireRefreshingLicense = activationManager.getActivationState() == ActivationState.REFRESHING;
        isNewInstall = application.isNewInstall();
    }

    @Inject
    public void register() {
        this.listener = new ActivationListener();
        activationManager.addListener(listener);
    }
    
    /**
     * Listens for changes that occur to the state of the ActivationManager and 
     * update the UI accordingly.
     */
    private class ActivationListener implements EventListener<ActivationEvent> {
        @Override
        public void handleEvent(final ActivationEvent event) {
            ActivationState state = event.getData(); 
            ActivationError error = event.getError();

            synchronized (this) {
                if (state == ActivationState.NOT_AUTHORIZED && error == ActivationError.BLOCKED_KEY) { 
                    isLicenseBlocked = true;
                }
                isLimeWireRefreshingLicense = (state == ActivationState.REFRESHING);
            }
        }
    }

    private boolean shouldShowBlockedLicenseNotification() {
        // if their license is blocked, then we show a blocked license message instead of the nag except in the case that they've just updated.
        // if it's an update, then they've already seen that their license is blocked in the setup screen.
        synchronized (ProNagController.this) {
            return isLicenseBlocked && !isNewInstall;
        }
    }
    
    /*
     * This starts a timer that waits until we have a response from the activation server and then
     * calls allowProNag to decide whether and which nag to show.
     */
    private void waitForActivationResult(final JLayeredPane layeredPane) {
        final TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (ProNagController.this) {
                    if (!isLimeWireRefreshingLicense) {
                        cancel();
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                allowProNag(layeredPane);
                            }
                        });
                    }
                }
            }
            
        };
        java.util.Timer timer = new java.util.Timer();
        timer.schedule(timerTask, 10, 10); 
    }

    public void allowProNag(final JLayeredPane layeredPane)  {
        if(!nagShown) {
            assert SwingUtilities.isEventDispatchThread();
           
            // if the activation manager is refreshing the activation state, let's wait for that process to finish
            // so we know whether to show a nag or a blocked license message
            if (isLimeWireRefreshingLicense) {
                waitForActivationResult(layeredPane);
                return;
            }

            nagShown = true;

            if (shouldShowBlockedLicenseNotification()) {
                blockedDialogProvider.get().setVisible(true);
            } else {
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
                                ActionListener listener = new ActionListener() {                                
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        if(proNag.isModal()) {
                                            loadModalNag();
                                        } else {
                                            loadNonModalNag(layeredPane);
                                        }                                    
                                    }
                                };
                                int delay = proNag.getDelay();
                                if(delay > 0) {
                                    Timer timer = new Timer(delay, listener);
                                    timer.setRepeats(false);
                                    timer.start();
                                } else {
                                    listener.actionPerformed(null);
                                }
                            }
                        }
                    }
                });
            }
        }
    }
    
    private void loadModalNag() {
        // not a LimeJDialog because we don't need/want the icon or the ESC action
        final JDialog dialog = new JDialog(GuiUtils.getMainFrame(), ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);
        proNag.setContainer(new NagContainer() {
            @Override
            public void dispose() {
                dialog.dispose();
            }
        });
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
    
    private void loadNonModalNag(final JLayeredPane layeredPane) {
        // heavyweight to force showing over browser
        final Panel heavyweight = new java.awt.Panel(new BorderLayout());
        Resizable resizable = new Resizable() {
            @Override
            public void resize() {
                Rectangle parentBounds = heavyweight.getParent().getBounds();
                Dimension childPreferredSize = heavyweight.getPreferredSize();
                int w = childPreferredSize.width;
                int h = childPreferredSize.height;
                heavyweight.setBounds(parentBounds.width / 2 - w / 2, parentBounds.height - h, w, h);
            }
            
            @Override
            public boolean isVisible() {
                return proNag.isVisible();
            }
        };
        
        heavyweight.add(proNag);
        
        final PanelResizer resizer = new PanelResizer(resizable);
        layeredPane.addComponentListener(resizer);        
        layeredPane.add(heavyweight, JLayeredPane.MODAL_LAYER);
        resizable.resize();
        proStatusPanel.addCondition(InvisibilityCondition.PRO_ADD_SHOWN);
        proNag.setContainer(new NagContainer() {            
            @Override
            public void dispose() {
                heavyweight.setVisible(false);
                layeredPane.remove(heavyweight);
                layeredPane.removeComponentListener(resizer);
                proStatusPanel.removeCondition(InvisibilityCondition.PRO_ADD_SHOWN);
            }
        });
    }

}
