package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.core.api.library.SharedFileList;
import org.limewire.core.api.library.SharedFileListManager;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.NavItem;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.PainterUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.EventTableModel;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class SharedFileCountPopupPanel extends Panel implements Resizable {
   
    private static final String ICON_COLUMN_ID = "Icon";
    private static final String NAME_COLUMN_ID = "Name";
    private static final String BLANK_COLUMN_ID = "Blank";
    private static final String FILES_COLUMN_ID = "Files";
    private static final int ROW_HEIGHT = 18;
    
    @Resource private Color dividerForeground = PainterUtils.TRASPARENT;;
    @Resource private Color rolloverBackground = PainterUtils.TRASPARENT;
    @Resource private Color activeBackground = PainterUtils.TRASPARENT;
    @Resource private Color activeBorder = PainterUtils.TRASPARENT;
    @Resource private Color border = PainterUtils.TRASPARENT;
    @Resource private Font headerFont;
   
    @Resource private Icon closeIcon;
    @Resource private Icon closeIconRollover;
    @Resource private Icon closeIconPressed;
    
    @Resource private Icon publicIcon;
    @Resource private Icon listSharedIcon;
    
    @Resource private Font listTextFont;
    @Resource private Color listTextForeground;
    @Resource private Font signInTextFont;
    
    private final SharedFileCountPanel sharedFileCountPanel;
    private final SharedFileListManager shareListManager;
    private final Provider<LoginPopupPanel> loginPanelProvider;
    private final XMPPService xmppService;
    private final ListenerSupport<XMPPConnectionEvent> connectionSupport;
    private final Navigator navigator;

    private Timer repaintTimer = null; 
    
    private JXPanel frame = null;
    private MouseableTable table = null;
    private VisiblityMatcher matcher = null;
    private FilterList<SharedFileList> filteredSharedFileLists;
    
    private final AbstractAction closeAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            sharedFileCountPanel.repaint();
        }
    };
    private HyperlinkButton signIntoFriendsButton = null;
        
    @Inject
    public SharedFileCountPopupPanel(SharedFileCountPanel sharedFileCountPanel,
            SharedFileListManager shareListManager,
            Provider<LoginPopupPanel> loginPanelProvider,
            XMPPService xmppService,
            ListenerSupport<XMPPConnectionEvent> connectionSupport,
            Navigator navigator) {
        super(new BorderLayout());
        
        this.sharedFileCountPanel = sharedFileCountPanel;
        this.shareListManager = shareListManager;
        this.loginPanelProvider = loginPanelProvider;
        this.xmppService = xmppService;
        this.connectionSupport = connectionSupport;
        this.navigator = navigator;
        
        GuiUtils.assignResources(this);
        
        setUpButton();
        
        setVisible(false);
    }

    @Inject
    public void register() {
        sharedFileCountPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                setVisible(!isVisible() && shareListManager.getSharedFileCount() != 0);
                sharedFileCountPanel.repaint();
            }
        });
        
        sharedFileCountPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                if (isVisible()) {
                    resize();
                }
            }
        });
    }
    
    @SuppressWarnings("unchecked")
    private void initContent() {
        
        frame = new JXPanel(new BorderLayout());
        frame.setBorder(BorderFactory.createMatteBorder(1, 1, 0, 1, border));
        frame.setPreferredSize(new Dimension(220, 160));
        
        JPanel topBarPanel = new JPanel(new MigLayout("gap 0, insets 0, fill"));
        topBarPanel.setBackground(border);
        ResizeUtils.forceHeight(topBarPanel, 21);
        
        JLabel titleBarLabel = new JLabel(I18n.tr("Sharing Lists"));
        titleBarLabel.setOpaque(false);
        titleBarLabel.setForeground(Color.WHITE);
        titleBarLabel.setFont(headerFont);
        IconButton closeButton = new IconButton(closeIcon, closeIconRollover, closeIconPressed);
        closeButton.addActionListener(closeAction);
        closeButton.setOpaque(false);
        
        topBarPanel.add(titleBarLabel, "gapleft 4, dock west");
        topBarPanel.add(closeButton, "gapright 3, dock east");
        
        frame.add(topBarPanel, BorderLayout.NORTH);
        
        JPanel contentPanel = new JPanel(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        
        scrollPane.setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(5,4,4,4));
                               
        filteredSharedFileLists = new FilterList<SharedFileList>(shareListManager.getModel());
        
        matcher = new VisiblityMatcher();
        filteredSharedFileLists.setMatcher(matcher);
        
        table = new MouseableTable(new EventTableModel<SharedFileList>(filteredSharedFileLists,
                new TableFormat<SharedFileList>() {
                    @Override
                    public int getColumnCount() {
                        return 4;
                    }
                    @Override
                    public String getColumnName(int column) {
                        if (column == 0) {
                            return ICON_COLUMN_ID;
                        }
                        else if (column == 1) {
                            return NAME_COLUMN_ID;
                        } 
                        else if (column == 2) {
                            return BLANK_COLUMN_ID;
                        }
                        else {
                            return FILES_COLUMN_ID;
                        }
                    }
                                        
                    @Override
                    public Object getColumnValue(SharedFileList baseObject, int column) {
                        if (column == 0) {
                            if (baseObject.isPublic()) {
                                return publicIcon;
                            }
                            else {
                                return listSharedIcon;
                            }
                        }
                        else if (column == 1) {
                            return baseObject.getCollectionName();
                        }
                        else if (column == 2) {
                            return null;
                        }
                        else {
                            return I18n.trn("{0} file", "{0} files", baseObject.size());
                        }
                    }
        }));
     
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        
        TableColumn iconColumn = table.getColumn(ICON_COLUMN_ID); 
        iconColumn.setCellRenderer(new IconRenderer());
        iconColumn.setPreferredWidth(publicIcon.getIconWidth());
        iconColumn.setMaxWidth(publicIcon.getIconWidth());
        
        LinkRenderer linkRenderer = new LinkRenderer();
        linkRenderer.setFont(listTextFont);
        LinkEditor linkEditor = new LinkEditor();
        linkEditor.setFont(listTextFont);
        TableColumn nameColumn = table.getColumn(NAME_COLUMN_ID);
        nameColumn.setCellRenderer(linkRenderer);
        nameColumn.setCellEditor(linkEditor);
        
        TableColumn blankColumn = table.getColumn(BLANK_COLUMN_ID);
        blankColumn.setPreferredWidth(20);
        blankColumn.setMinWidth(20);
        blankColumn.setMaxWidth(20);
        
        table.getColumn(FILES_COLUMN_ID).setPreferredWidth(0);
        
        table.setFont(listTextFont);
        table.setForeground(listTextForeground);
        table.setOpaque(false);
        table.setShowGrid(false);
        table.setFocusable(false);
        table.setCellSelectionEnabled(false);
        table.setRowHeight(ROW_HEIGHT);
        table.setStripeHighlighterEnabled(false);
        table.setStripesPainted(false);
     
        repaintTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filteredSharedFileLists.setMatcher(matcher);
                resize();
                repaint();
            }
        });
        repaintTimer.setRepeats(false);
        
        final ListEventListener repaintListener = new RepaintListener();
               
        for ( SharedFileList item : shareListManager.getModel() ) {
            item.getSwingModel().addListEventListener(repaintListener);
            item.getFriendIds().addListEventListener(repaintListener);
        }
        
        shareListManager.getModel().addListEventListener(new ListEventListener<SharedFileList>() {
            @Override
            public void listChanged(ListEvent<SharedFileList> listChanges) {
                while(listChanges.next()) {
                    if (listChanges.getType() == ListEvent.INSERT) {
                        
                        SharedFileList newList = listChanges.getSourceList().get(listChanges.getIndex());
                        
                        newList.getModel().addListEventListener(repaintListener);
                        newList.getFriendIds().addListEventListener(repaintListener);
                    }
                }
                repaintTimer.start();
            }
        });
       
        contentPanel.add(table, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new MigLayout("gap 0, insets 0, align center"));
        bottomPanel.setOpaque(false);
        
        setUpAndAddSignInButton(bottomPanel);
        
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        
        frame.add(scrollPane, BorderLayout.CENTER);

        add(frame, BorderLayout.CENTER);
    }
    
    private void setUpAndAddSignInButton(final JPanel panel) {
        if (shouldShowSignInButton()) {
            signIntoFriendsButton = new HyperlinkButton(new AbstractAction(I18n.tr("Sign in to share with friends")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    loginPanelProvider.get().setVisible(true);
                }
            });
            signIntoFriendsButton.setFont(signInTextFont);

            panel.add(signIntoFriendsButton);
            
            connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
                @SwingEDTEvent
                @Override
                public void handleEvent(XMPPConnectionEvent event) {
                    if (!shouldShowSignInButton()) {
                        connectionSupport.removeListener(this);
                        panel.remove(signIntoFriendsButton);
                        signIntoFriendsButton = null;
                        validate();
                        resize();
                        repaint();
                    }
                }
            });
        }
    }
    
    private boolean shouldShowSignInButton() {
        if (xmppService.isLoggedIn()) {
            return false;
        }
        if (shareListManager.getModel().size() == 1) {
            return true;
        }
        
        for ( SharedFileList list : shareListManager.getModel() ) {
            if (!list.isPublic()) {
                if (list.getFriendIds().size() != 0) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        
        if (table == null && visible) {
            initContent();
        }
        
        if (visible) {
            resize();
            validate();
            frame.repaint();
        }
    }
    
    private int getRowsToShow() {
        int rows = filteredSharedFileLists.size();
        return (rows > 10) ? 10 : rows;
    }
    
    @Override
    public void resize() {
        Rectangle parentBounds = getParent().getBounds();
        Dimension childPreferredSize = frame.getPreferredSize();
        
        int w = (int) childPreferredSize.getWidth();
        int h = 32 + ROW_HEIGHT*getRowsToShow() + (signIntoFriendsButton == null ? 0 : 15);
        
        setBounds((int)sharedFileCountPanel.getBounds().getX(),
                (int)parentBounds.getHeight() - h, w, h);
    }
    
    private void setUpButton() {
        sharedFileCountPanel.setBorder(BorderFactory.createEmptyBorder(0,8,0,8));
        sharedFileCountPanel.setFocusPainted(false);
        sharedFileCountPanel.setBorderPainted(false);
        sharedFileCountPanel.setFocusable(false);
        sharedFileCountPanel.setOpaque(false);
        sharedFileCountPanel.setBackgroundPainter(new StatusBarPopupButtonPainter());
    }

    private class StatusBarPopupButtonPainter extends AbstractPainter<JXButton>{

        public StatusBarPopupButtonPainter() {
            setAntialiasing(false);
            setCacheable(false);
        }

        @Override
        protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
            if(SharedFileCountPopupPanel.this.isVisible()) {
                g.setPaint(activeBackground);
                g.fillRect(0, 0, width, height);
                g.setPaint(border);
                g.drawLine(0, 0, 0, height-1);
                g.drawLine(0, height-1, width-1, height-1);
                g.drawLine(width-1, 0, width-1, height-1);
            } else if (object.getModel().isRollover() && shareListManager.getSharedFileCount() != 0) {
                g.setPaint(rolloverBackground);
                g.fillRect(0, 2, width-1, height-2);
                g.setPaint(activeBorder);
                g.drawLine(0, 1, 0, height-1);
                g.drawLine(width-1, 1, width-1, height-1);
            }
            else {
                g.setPaint(dividerForeground);
                g.drawLine(0, 3, 0, height-4);
                g.drawLine(width-1, 3, width-1, height-4);
            }
        }    
    }
    
    private class RepaintListener implements ListEventListener {
        @Override
        public void listChanged(ListEvent listChanges) {
            repaintTimer.start();
        }
    }
    
    private static class IconRenderer extends JLabel implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            if (!(value instanceof Icon)) {
                return null;
            }
            
            setIcon((Icon) value);
            return this;
        }
    }
    
    private static class LinkRenderer extends HyperlinkButton implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            
            if (value == null) {
                return null;
            }
            
            setText(value.toString());

            return this;         
        }
        
        /**
         * Used to filter out evil foreground calls from the default JXTable Highlighter
         */
        @Override
        public void setForeground(Color c) {
        }
    }
    
    private class LinkEditor extends AbstractCellEditor implements TableCellEditor {

        private final HyperlinkButton button = new HyperlinkButton(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // TODO: navigate to the specific list?  (talk to everrrettt) 
                NavItem item = navigator.getNavItem(NavCategory.LIBRARY, I18n.tr("My Library"));
                item.select();
            }
        });
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                boolean isSelected, int row, int column) {
            
            if (value == null) {
                return null;
            }
            
            button.setText(value.toString());

            return button;         
        }
        
        @Override
        public Object getCellEditorValue() {
            return button;
        }
        
        public void setFont(Font f) {
            button.setFont(f);
        }
    }
    
    private static class VisiblityMatcher implements Matcher<SharedFileList> {
        @Override
        public boolean matches(SharedFileList item) {
            return item.size() != 0 && 
                    (   item.isPublic() 
                     || (item.getFriendIds().size() != 0)
                    );
        }
    }
}
