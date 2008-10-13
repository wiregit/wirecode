package org.limewire.ui.swing.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.TransferHandler;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.MattePainter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.core.api.library.FriendShareListEvent;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.listener.ListenerSupport;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.mainframe.SectionHeading;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.NavigatorUtils;
import org.limewire.ui.swing.sharing.dragdrop.SharingTransferHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;

public class FilesSharingSummaryPanel extends JPanel {
    
    private final SectionHeading title;
    
    @Resource private Color topButtonSelectionGradient;
    @Resource private Color bottomButtonSelectionGradient;
    
    private final ShareButton gnutellaButton;
    private final ShareButton friendButton;
          
    @Inject
    FilesSharingSummaryPanel(final ShareListManager shareListManager, GnutellaSharePanel gnutellaSharePanel, 
            FriendSharePanel friendSharePanel, final Navigator navigator, ListenerSupport<FriendShareListEvent> shareSupport) {
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        title = new SectionHeading(I18n.tr("Files I'm Sharing"));
        title.setName("FilesSharingSummaryPanel.title");
        
        NavItem gnutellaNav = navigator.createNavItem(NavCategory.SHARING, GnutellaSharePanel.NAME, gnutellaSharePanel);
        gnutellaButton = new ShareButton(NavigatorUtils.getNavAction(gnutellaNav));
        gnutellaButton.setName("FilesSharingSummaryPanel.gnutella");
        gnutellaButton.setText("0");
        gnutellaButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
        gnutellaButton.setTransferHandler(buildGnutellaTransferHandler(shareListManager, navigator));
        
        NavItem friendNav = navigator.createNavItem(NavCategory.SHARING, FriendSharePanel.NAME, friendSharePanel);
        friendButton = new ShareButton(NavigatorUtils.getNavAction(friendNav));
        friendButton.setName("FilesSharingSummaryPanel.friends");   
        friendButton.setText("0");
        friendButton.setGradients(topButtonSelectionGradient, bottomButtonSelectionGradient);
        friendButton.setTransferHandler(buildFriendTransferHandler(navigator));
		
		setLayout(new MigLayout("insets 0, gap 0", "", ""));

        add(title, "span, wrap");
        add(gnutellaButton, "alignx left");
        add(friendButton, "alignx right");
        
        shareListManager.getGnutellaShareList().getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>() {
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                gnutellaButton.setText(GuiUtils.toLocalizedInteger(listChanges.getSourceList().size()));                
            }
        });        

        shareListManager.getCombinedFriendShareLists().getSwingModel().addListEventListener(new ListEventListener<LocalFileItem>() {
            @Override
            public void listChanged(ListEvent<LocalFileItem> listChanges) {
                friendButton.setText(GuiUtils.toLocalizedInteger(listChanges.getSourceList().size()));
            }
        });
    }

   /**
    * Builds a transfer handler for the friend button.
    * 
    * When an item is dragged onto the friend button, the button flashes while waiting for 750ms. 
    * If the mouse is still over the component at the end of the time then the navigator switches the
    * view to the friend share window.
    * 
    * Items cannot be dropped view this transfer handler.
    */
    private TransferHandler buildFriendTransferHandler(final Navigator navigator) {
        TransferHandler transferHandler = new TransferHandler() {
            private Timer timer = null;
            private Timer flashTimer = null;
            @Override
            public boolean canImport(TransferSupport support) {
 
                if(timer == null || !timer.isRunning()) {
                    timer = new ComponentHoverTimer(750, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NavItem navItem = navigator.getNavItem(NavCategory.SHARING, FriendSharePanel.NAME);
                        navItem.select();
                    }
                }, friendButton);
                    
                timer.setRepeats(false);
                timer.start();
                }
                
                if(flashTimer == null || !flashTimer.isRunning()) {
                    flashTimer = new FlashTimer(250, navigator, friendButton);
                    flashTimer.setInitialDelay(250);
                    flashTimer.start();
                }
                return super.canImport(support);
            }  
        };
        return transferHandler;
    }

    /**
     * Builds a transfer handler for the gnutella button.
     * 
     * When an item is dragged onto the gnutella button, the button flashes while waiting for 750ms. 
     * If the mouse is still over the component at the end of the time then the navigator switches the
     * view to the gnutella share window.
     * 
     * If the dragged items are dropped onto the gnutella button, they are added to the gnutella library. 
     * This is done via delegation to a SharingTransferHandler;
     */
    private TransferHandler buildGnutellaTransferHandler(final ShareListManager shareListManager,
            final Navigator navigator) {
            TransferHandler transferHandler = new TransferHandler() {
            private final SharingTransferHandler sharingTransferHandler = new SharingTransferHandler(shareListManager.getGnutellaShareList());
            private Timer timer = null;
            private Timer flashTimer = null;
            @Override
            public boolean canImport(TransferSupport support) {
 
                if(timer == null || !timer.isRunning()) {
                    timer = new ComponentHoverTimer(750, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        NavItem navItem = navigator.getNavItem(NavCategory.SHARING, GnutellaSharePanel.NAME);
                        navItem.select();
                    }
                }, gnutellaButton);
                    
                timer.setRepeats(false);
                timer.start();
                }
                
                if(flashTimer == null || !flashTimer.isRunning()) {
                    flashTimer = new FlashTimer(250, navigator, gnutellaButton);
                    flashTimer.setInitialDelay(250);
                    flashTimer.start();
                }
                return sharingTransferHandler.canImport(support);
            }
            
            @Override
            public boolean importData(TransferSupport support) {
                return sharingTransferHandler.importData(support);
            }
            
        };
        
        return transferHandler;
    }
    
    /**
     * A button that uses a painter to draw the background if
     * its Action.SELECTED_KEY property is true.
     */
    private static class ShareButton extends IconButton {
        
        private Boolean flash = Boolean.FALSE;
        
        public ShareButton(Action navAction) {
            super(navAction);
        }
        
        public void setFlash(Boolean flash) {
            this.flash = flash;        
            repaint();
        }

        public void setGradients(Color topGradient, Color bottomGradient) {
            getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        repaint();
                    }
                }
            });
            
            final Painter<JXButton> oldPainter = getBackgroundPainter();
            setBackgroundPainter(new MattePainter<JXButton>(
                    new GradientPaint(new Point2D.Double(0, 0), topGradient, 
                            new Point2D.Double(0, 1), bottomGradient,
                            false), true) {
                @Override
                public void doPaint(Graphics2D g, JXButton component, int width,
                        int height) {
                    //while flashing we simulate the button being selected.
                    if(Boolean.TRUE.equals(flash) || Boolean.TRUE.equals(getAction().getValue(Action.SELECTED_KEY))) {
                        super.doPaint(g, component, width, height);
                    } else {
                        oldPainter.paint(g, component, width, height);
                    }
                }
            });
        }   
    }
    
    private final class FlashTimer extends Timer {
       
        private FlashTimer(int delay, final Navigator navigator, final ShareButton shareButton) {
            super(delay, null);
            final FlashTimer flashTimer = this;
            shareButton.setFlash(Boolean.TRUE);
            addActionListener(new ActionListener() {
                        int count = 0;
                        public void actionPerformed(ActionEvent e) {
                            
                            if( count % 2  == 1) {
                                shareButton.setFlash(Boolean.TRUE);
                            } else {
                                shareButton.setFlash(Boolean.FALSE);
                            }
                            
                            if(count == 2) {
                              flashTimer.stop();
                            }
                            count++;
                        }
                    });
        }
    }
    
    /**
     * Helper class that given a component will execute the given listener if 
     * the mouse is hovering over the component at the time the timer fires.
     */
    private class ComponentHoverTimer extends Timer {
        public ComponentHoverTimer(int delay, final ActionListener listener, final Component component) {
            super(delay, new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    Point point =  MouseInfo.getPointerInfo().getLocation();
                    SwingUtilities.convertPointFromScreen(point, component);
                    if(component.contains(point)) {
                        listener.actionPerformed(e);
                    }
                }
                
            });
        }
    }
}
