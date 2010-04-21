package org.limewire.ui.swing.pro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.limewire.activation.api.ActivationManager;
import org.limewire.activation.api.ModuleCodeEvent;
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
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

public class ProNagController {

    private final boolean isFirstLaunch;
    private final ProNag proNag;
    private final ProStatusPanel proStatusPanel;
    
    private final ActivationManager activationManager;
    private MCodeListener listener;
    private boolean waitingForMCode = true;

    private JLayeredPane layeredPane;
    private boolean nagShown;

    @Inject ProNagController(ProNag proNag, ProStatusPanel proStatusPanel, 
                             ActivationManager activationManager,
                             Application application) {
        isFirstLaunch = !InstallSettings.UPGRADED_TO_5.getValue();
        this.proNag = proNag;
        this.proStatusPanel = proStatusPanel;

        this.activationManager = activationManager;
        waitingForMCode = !activationManager.isMCodeUpToDate(); 
    }

    @Inject
    public void register() {
        if (!activationManager.isMCodeUpToDate()) {
            this.listener = new MCodeListener();
            activationManager.addMCodeListener(listener);
        } else {
            waitingForMCode = !activationManager.isMCodeUpToDate(); 
        }
    }

    /**
     * Listens for when the ActivationManager has the correct mcode (encoded list of pro features)
     * to send in the request for the nag.
     */
    private class MCodeListener implements EventListener<ModuleCodeEvent> {
        @SwingEDTEvent
        public void handleEvent(final ModuleCodeEvent event) {
            waitingForMCode = false;
            if (isNagReady()) {
                showNag();
            }
        }
    }

    public void showNagIfReady(final JLayeredPane layeredPane)  {
        SwingUtils.invokeNowOrLater(new Runnable() {
            public void run() {
                ProNagController.this.layeredPane = layeredPane;

                if (isNagReady())
                    showNag();
            }
        });
    }
    
    /*
     * We don't want to show the nag until we've been given the application's layered pane and until the activation manager
     * has refreshed its mcode (its encoded list of pro features) that we send in the nag request.
     */
    private boolean isNagReady() {
        return !waitingForMCode && layeredPane != null;
    }
    
    private void showNag() {
        if(!nagShown) {
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
