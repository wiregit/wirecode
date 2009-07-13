package org.limewire.ui.swing.mainframe;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.BusyPainter;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchResult;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventListener;
import org.limewire.listener.EventUtils;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.friends.actions.BrowseFriendsAction;
import org.limewire.ui.swing.friends.actions.FriendServiceItem;
import org.limewire.ui.swing.friends.actions.LoginAction;
import org.limewire.ui.swing.friends.actions.LogoutAction;
import org.limewire.ui.swing.friends.login.AutoLoginService;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;

import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class FriendsButton extends LimeComboBox {
    
    
    @Resource private Icon friendOnlineIcon;
    @Resource private Icon friendOfflineIcon;
    @Resource private Icon friendOnlineSelectedIcon;
    @Resource private Icon friendOfflineSelectedIcon;

    @Resource private Icon friendLoadingIcon;
    @Resource private Icon friendNewFilesIcon;
    
    @Resource private Color borderForeground = PainterUtils.TRASPARENT;
    @Resource private Color borderInsideRightForeground = PainterUtils.TRASPARENT;
    @Resource private Color borderInsideBottomForeground = PainterUtils.TRASPARENT;
    @Resource private Color dividerForeground = PainterUtils.TRASPARENT;
    
    private boolean newResultsAvailable = false;
    private final BusyPainter busyPainter;
    private Timer busy;
    
    @Inject
    public FriendsButton(ComboBoxDecorator comboBoxDecorator,
            final Provider<FriendServiceItem> serviceItemProvider,
            final Provider<BrowseFriendsAction> browseFriendsActionProvider,
            final Provider<LoginAction> loginActionProvider,
            final Provider<LogoutAction> logoutActionProvider,
            final AutoLoginService autoLoginService,
            final EventBean<FriendConnectionEvent> friendConnectionEventBean) {
        GuiUtils.assignResources(this);
        
        comboBoxDecorator.decorateIconComboBox(this);
        setToolTipText(I18n.tr("Browse Friends' Files"));
        setText(null);
        setIconTextGap(1);

        setForegroundPainter(new AbstractPainter<JXButton>() {
            @Override
            protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
                Icon icon = null;
                if (object.getModel().isPressed()) {
                    icon = object.getPressedIcon();
                }
                if (icon == null) {
                    icon = object.getIcon();
                }
                icon.paintIcon(object, g, 0, (height-icon.getIconHeight())/2);
                
                // the width/heigth offset is very specific to the loading img.
                if(busy != null) {
                    g.translate(21, 22);
                    busyPainter.paint(g, object, width, height);
                    g.translate(-21, -22);
                }
            }
        });
        
        busyPainter = new BusyPainter() {
            @Override
            protected void init(Shape point, Shape trajectory, Color b, Color h) {
                super.init(getScaledDefaultPoint(8), 
                        getScaledDefaultTrajectory(8),
                        Color.decode("#acacac"),  Color.decode("#545454"));
            }
        };
        
        addMouseListener(new ActionHandListener());
        
        final JPopupMenu menu = new JPopupMenu();
        overrideMenuNoRestyle(menu);
        menu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                menu.removeAll();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                menu.removeAll();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                menu.add(serviceItemProvider.get());

                FriendConnection friendConnection = EventUtils.getSource(friendConnectionEventBean);
                boolean signedIn = friendConnection != null && friendConnection.isLoggedIn();
                boolean loggingIn = autoLoginService.isAttemptingLogin()
                        || (friendConnection != null && friendConnection.isLoggingIn());

                JMenuItem browseFriendMenuItem = new JMenuItem(browseFriendsActionProvider.get());
                menu.add(browseFriendMenuItem);
                browseFriendMenuItem.setEnabled(signedIn);
                
                
                if (signedIn) {
                    menu.add(new JMenuItem(logoutActionProvider.get()));
                } else {
                    JMenuItem loginMenuItem;
                    if (loggingIn) {
                        loginMenuItem = new JMenuItem(I18n.tr(LoginAction.DISPLAY_TEXT));
                        loginMenuItem.setEnabled(false);
                    } 
                    else {
                        loginMenuItem = new JMenuItem(loginActionProvider.get());
                    }
                    menu.add(loginMenuItem);
                }
            }
        });
        
        menu.setBorder(new Border() {
            @Override
            public Insets getBorderInsets(Component c) {
                return new Insets(3,1,3,2);
            }
            @Override
            public boolean isBorderOpaque() {
                return false;
            }
            @Override
            public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
                g.setColor(dividerForeground);
                g.drawLine(0, 0, getPressedIcon().getIconWidth()-1, 0);                
                g.setColor(borderForeground);
                g.drawLine(0, 0, 0, height-1);
                g.drawLine(0, height-1, width-1, height-1);
                g.drawLine(width-1, height-1, width-1, 0);
                g.drawLine(getPressedIcon().getIconWidth(), 0, width-1, 0);
                g.setColor(borderInsideRightForeground);
                g.drawLine(width-2, height-2, width-2, 1);
                g.drawLine(width-1, height-1, width-1, height-1);
                g.setColor(borderInsideBottomForeground);
                g.drawLine(1, height-2, width-2, height-2);
                g.drawLine(0, height-1, 0, height-1);
                
            }
        });
        
        setPopupPosition(new Point(0, -4));
    }
    
    private void setIconFromEvent(FriendConnectionEvent event) {
        FriendConnectionEvent.Type eventType = event == null ? null : event.getType();
        if(eventType == null) {
            eventType = FriendConnectionEvent.Type.DISCONNECTED;
        }
        
        switch(eventType) {
        case CONNECT_FAILED:
        case DISCONNECTED:
            newResultsAvailable = false;
            setIcon(friendOfflineIcon);
            setPressedIcon(friendOfflineSelectedIcon);
            stopAnimation();
            break;
        case CONNECTED:
            if(newResultsAvailable) {
               setIcon(friendNewFilesIcon);
            } else { 
                setIcon(friendOnlineIcon);
                setPressedIcon(friendOnlineSelectedIcon);
            }
            stopAnimation();
            break;
        case CONNECTING:
            setIcon(friendLoadingIcon);
            startAnimation();
        }
    }

    
    @Inject
    void register(final EventBean<FriendConnectionEvent> connectBean, ListenerSupport<FriendConnectionEvent> connectionSupport, RemoteLibraryManager remoteLibraryManager) {
        

        setIconFromEvent(connectBean.getLastEvent());
        
        //update icon as connection events come in.
        connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(FriendConnectionEvent event) {
                setIconFromEvent(event);
            }
        });
        
        //change to new files icon when inserts are detected
        remoteLibraryManager.getAllFriendsFileList().getSwingModel().addListEventListener(new ListEventListener<SearchResult>() {
            public void listChanged(ListEvent<SearchResult> listChanges) {
                while(listChanges.next()) {
                    if(listChanges.getType() == ListEvent.INSERT) {
                        newResultsAvailable = true;
                        FriendConnection friendConnection = EventUtils.getSource(connectBean);
                        if(friendConnection != null && friendConnection.isLoggedIn()) {
                            setIconFromEvent(new FriendConnectionEvent(friendConnection, FriendConnectionEvent.Type.CONNECTED));
                        }
                        break;//breaking only need to run once.
                    }
                }
            }; 
         });
        
        //clear new files icon when button is clicked
        addActionListener(new AbstractAction() {
           @Override
            public void actionPerformed(ActionEvent e) {
               FriendConnection friendConnection = EventUtils.getSource(connectBean);
               if(friendConnection != null && friendConnection.isLoggedIn()) {
                   newResultsAvailable = false;
                   setIconFromEvent(new FriendConnectionEvent(friendConnection, FriendConnectionEvent.Type.CONNECTED));
               }
            } 
        });
    }
    
    // animation code ripped from JXBusyLabel
    private void startAnimation() {
        if(busy != null) {
            stopAnimation();
        }
        
        busy = new Timer(100, new ActionListener() {
            int frame = busyPainter.getPoints();
            public void actionPerformed(ActionEvent e) {
                frame = (frame+1)%busyPainter.getPoints();
                busyPainter.setFrame(frame);
                repaint();
            }
        });
        busy.start();
    }    
    
    private void stopAnimation() {
        if (busy != null) {
            busy.stop();
            busyPainter.setFrame(-1);
            repaint();
            busy = null;
        }
    }
}
