package org.limewire.ui.swing.library.nav;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.icon.EmptyIcon;
import org.jdesktop.swingx.painter.BusyPainter;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LibraryState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.listener.EventBroadcaster;
import org.limewire.ui.swing.components.ActionLabel;
import org.limewire.ui.swing.library.FriendLibraryMediator;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

import ca.odell.glazedlists.EventList;

public class NavPanel extends JXPanel {
    
    @Resource private Icon removeLibraryIcon;
    @Resource private Icon removeLibraryHoverIcon;
    
    @Resource private Color selectedBackground;
    
    @Resource private Font selectedTextFont;
    @Resource private Color selectedTextColor;
    @Resource private Font failedTextFont;
    @Resource private Font textFont;
    @Resource private Color textColor;        
    
    private final RemoteLibraryManager remoteLibraryManager;
    private final FriendLibraryMediator libraryPanel;
    
    private final Friend friend;
    private final ActionLabel categoryLabel;
    private final JXBusyLabel statusIcon;
    private final Action action;
    
    private NavList parentList;
    private MouseListener removeListener;
    
    @AssistedInject
    NavPanel(@Assisted Action action,
             @Assisted Friend friend,
             @Assisted FriendLibraryMediator libraryPanel,
            RemoteLibraryManager remoteLibraryManager, final EventBroadcaster<FriendSelectEvent> friendSelectBroadcaster) {
        super(new MigLayout("insets 0, gap 0, fill"));
        
        GuiUtils.assignResources(this);
        
        setOpaque(false);
        
        this.action = action;
        this.friend = friend;           
        this.libraryPanel = libraryPanel;
        this.remoteLibraryManager = remoteLibraryManager;        
        
        categoryLabel = new ActionLabel(action, false);
        categoryLabel.setFont(textFont);
        categoryLabel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));
        categoryLabel.setMinimumSize(new Dimension(0, 20));
        categoryLabel.setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        categoryLabel.setText(friend.getRenderName());
        statusIcon = new JXBusyLabel(new Dimension(12, 12));
        statusIcon.setOpaque(false);
        
        add(categoryLabel, "gapbefore 0, grow, push, alignx left");
        add(statusIcon, "alignx right, gapafter 4, hidemode 3, wrap");
        unbusy();
        
        action.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if(Boolean.TRUE.equals(evt.getNewValue())) {
                        requestFocus();
                        setBackground(selectedBackground);
                        categoryLabel.setForeground(selectedTextColor);
                        categoryLabel.setFont(selectedTextFont);
                        setOpaque(true);
                    } else {
                        setBackground(null);
                        categoryLabel.setForeground(textColor);
                        categoryLabel.setFont(textFont);
                        setOpaque(false);
                    }
                }
            }
        });
        
        action. addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                if (evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                    if (evt.getNewValue().equals(Boolean.TRUE)) {
                        friendSelectBroadcaster.broadcast(new FriendSelectEvent(NavPanel.this.friend));
                    } else {
                        friendSelectBroadcaster.broadcast(new FriendSelectEvent(null));
                    }
                }
            }
        });
    }
    
    Action getAction() {
        return action;
    }
    
    public void setPanelIcon(Icon icon) {
        categoryLabel.setIcon(icon);
    }
    
    public void setPanelFont(Font font) {
        this.textFont = font;
        if(!Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
            categoryLabel.setFont(font);
        }
    }
    
    public void setSelectedFont(Font font) {
        this.selectedTextFont = font;
        if(Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
            categoryLabel.setFont(font);
        }
    }
    
    public void setFontColor(Color color) {
        this.textColor = color;
        if(!Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
            categoryLabel.setForeground(color);
        }
    }
    
    public void setSelectedFontColor(Color color) {
        this.selectedTextColor = color;
        if(Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY))) {
            categoryLabel.setForeground(color);
        }    
    }
    
    NavList getParentList() {
        return parentList;
    }
    
    void setParentList(NavList parentList) {
        this.parentList = parentList;
    }
    
    void addActionListener(ActionListener listener) {
        categoryLabel.addActionListener(listener);
    }
    
    private void busy() {
        removeEjectListener();
        BusyPainter painter = statusIcon.getBusyPainter();
        statusIcon.setIcon(new EmptyIcon(12, 12));
        statusIcon.setBusyPainter(painter);
        statusIcon.setVisible(true);
        statusIcon.setBusy(true);
    }
    
    private void unbusy() {
        if(friend.isAnonymous()) {
            statusIcon.setVisible(true);
            statusIcon.setBusy(false);
            statusIcon.setIcon(removeLibraryIcon);
            addEjectListener();
        } else {
            removeEjectListener();
            statusIcon.setVisible(false);
            statusIcon.setBusy(false);
            statusIcon.setIcon(new EmptyIcon(12, 12));
        }
    }
    
    private void removeEjectListener() {
        if(removeListener != null) {
            statusIcon.removeMouseListener(removeListener);
            removeListener = null;
        }
    }
    
    private void addEjectListener() {
        if (removeListener == null) {
            removeListener = new ActionHandListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) { 
                    remoteLibraryManager.removeFriendLibrary(friend);
                }
            }) {
                @Override
                public void mouseEntered(MouseEvent e) {
                    super.mouseEntered(e);
                    statusIcon.setIcon(removeLibraryHoverIcon);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    super.mouseExited(e);
                    statusIcon.setIcon(removeLibraryIcon);
                }
            };
            statusIcon.addMouseListener(removeListener);
        }
    }
    
    public void updateLibraryState(LibraryState libraryState) {
        switch(libraryState) {
        case FAILED_TO_LOAD:
            categoryLabel.setFont(failedTextFont);
            unbusy();
            break;
        case LOADED:
            categoryLabel.setFont(textFont);
            unbusy();
            break;
        case LOADING:
            categoryLabel.setFont(textFont);
            busy();
            break;
        }
    }
    
    public void updateLibrary(EventList<RemoteFileItem> eventList, LibraryState state) {
        libraryPanel.showLibraryPanel(eventList, state);
    }

    public boolean hasSelection() {
        return Boolean.TRUE.equals(action.getValue(Action.SELECTED_KEY));
    }
    
    public void select() { 
        action.actionPerformed(null);
    }
    
    public void removeBrowse() {
        if(libraryPanel != null) {
            unbusy();
            libraryPanel.showLibraryCard();
        }
    }
    
    public Friend getFriend() {
        return friend;
    }
}
