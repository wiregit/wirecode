package org.limewire.ui.swing.library.sharing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.ComboPopup;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXLabel;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.friend.Friend;
import org.limewire.ui.swing.components.LimeEditableComboBox;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.components.ShapeDialogComponent;
import org.limewire.ui.swing.library.sharing.model.LibraryShareModel;
import org.limewire.ui.swing.painter.TextShadowPainter;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.TextFilterator;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import ca.odell.glazedlists.matchers.MatcherEditor.Event;
import ca.odell.glazedlists.swing.EventComboBoxModel;
import ca.odell.glazedlists.swing.EventTableModel;

/**
 * This is used internally by ShareWidget.  Needs refactoring.
 */
class LibrarySharePanel extends JXPanel implements PropertyChangeListener, Disposable, ShapeDialogComponent {

    private static final int SHARED_ROW_COUNT = 20;
    private static final int BORDER_INSETS = 5;
    private static final int BORDER_BUFFER = BORDER_INSETS + 4;
  //  private static final int HGAP = 5;
    @Resource
    private int panelWidth;
    
    private JXPanel titlePanel;
    private JXPanel tablePanel;
    private JXPanel bottomPanel;
    @Resource
    private Color bottomPanelTopGradient;
    @Resource
    private Color bottomPanelBottomGradient;
    
    private GradientPaint bottomGradient;
    
    private LibraryShareModel shareModel;
    
    private JTextField inputField;
    


    /**
     * all unshared friends
     */    
    private SortedList<SharingTarget> noShareFriendList;
    /**
     * filtered list of unshared friends
     */
    private FilterList<SharingTarget> noShareFilterList;
    /**
     * list of shared friends
     */
    private EventList<SharingTarget> shareFriendList;

    /** list of all friends */
    private Collection<Friend> allFriends;
    /** list of all friends */
    private SortedList<SharingTarget> allFriendsSorted;

    private JScrollPane shareScroll;

    private MouseableTable shareTable;
    private JComboBox friendCombo;
    private LimeEditableComboBox comboPanel;
    
    private boolean bottomPanelHidden = false;
    private boolean tableHidden = false;

    private JXLabel friendLabel;
    private JLabel topLabel;
    private JXLabel bottomLabel;
    private JLabel titleLabel;
    
    private JXPanel mainPanel;
    private JButton closeButton;
    @Resource
    private Icon closeIcon;
    @Resource
    private Icon closeIconRollover;
    @Resource
    private Icon closeIconPressed;   
     
    
    private ShapeDialog dialog;
    
    private ComboPopup comboPopup;
    
    private TextMatcherEditor<SharingTarget> textMatcher;
    
    @Resource
    private Icon removeIcon;
    @Resource
    private Icon removeIconRollover;
    @Resource
    private Icon removeIconPressed;   
    @Resource
    private int shareTableIndent = 15;
    @Resource
    private Color shadowColor;
    @Resource
    private Color dividerColor;
    @Resource
    private Color borderColor;
    
    private Action up = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!friendCombo.isPopupVisible()){
                friendCombo.showPopup();
            } else {
                int selRow = friendCombo.getSelectedIndex();
                if (selRow > 0) {
                    selRow--;
                    friendCombo.setSelectedIndex(selRow);
                }
            }
        }
    };

    private Action down = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            if(!friendCombo.isPopupVisible()){
                friendCombo.showPopup();
            } else {
                int selRow = friendCombo.getSelectedIndex();
                if (selRow < noShareFilterList.size() - 1) {
                    selRow++;
                    friendCombo.setSelectedIndex(selRow);
                }
            }
        }
    };    
    
    
    public LibrarySharePanel(Collection<Friend> allFriends, ShapeDialog dialog) {
        this.allFriends = allFriends;
        this.dialog = dialog;
        initialize();
    }
  
    
    private void initialize(){
        GuiUtils.assignResources(this);   
        
        initializeLists();

        initializeShareTable();

        initializeComboBox();
        
        initializeLabels();
        
        initializePanels();
        
        initializePainters();
        
        initializeButton();
        
        addComponents();
        
        setKeyStrokes(this);
        setKeyStrokes(mainPanel);
        setKeyStrokes(inputField); 
    }
  
    private void initializeButton() {
        closeButton = new IconButton(closeIcon, closeIconRollover, closeIconPressed);
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                dialog.setVisible(false);
            }
        });
    }


    private void initializePanels(){        
        
        setLayout(new MigLayout());
        setOpaque(false);
        
        mainPanel = new JXPanel(new MigLayout("nocache, fill, ins 0 6 0 6 , gap 0! 0!, novisualpadding"));
        mainPanel.setOpaque(false);

        titlePanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        titlePanel.setOpaque(false);
        
        tablePanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        tablePanel.setOpaque(false);
        
        bottomPanel = new JXPanel(new MigLayout("nocache, fill, ins 0 0 0 0 , gap 0! 0!, novisualpadding"));
        bottomPanel.setOpaque(false);

        mainPanel.setMinimumSize(new Dimension(panelWidth, 0));
        mainPanel.setMaximumSize(new Dimension(panelWidth, Integer.MAX_VALUE));
    }
    
    private void initializePainters(){
        //TODO: sizes and shapes
        
        setBackgroundPainter(new Painter() {

            @Override
            public void paint(Graphics2D g, Object object, int width, int height) {
                int arc = 10;
                Area shadowArea = new Area(new RoundRectangle2D.Float(mainPanel.getLocation().x - BORDER_INSETS, mainPanel.getLocation().y - BORDER_INSETS, mainPanel.getWidth() + BORDER_INSETS * 3, mainPanel.getHeight() + BORDER_INSETS * 3, arc, arc));
                RoundRectangle2D.Float panelShape = new RoundRectangle2D.Float(mainPanel.getLocation().x, mainPanel.getLocation().y, mainPanel.getWidth(),
                        mainPanel.getHeight() + 2, arc, arc);
                Area panelArea = new Area(panelShape);
                Point bottomLocation = SwingUtilities.convertPoint(mainPanel, bottomPanel.getLocation(), LibrarySharePanel.this);
                Area bottomArea = new Area(new Rectangle2D.Float(0, bottomLocation.y, getWidth(), 1000));
               
                bottomArea.intersect(panelArea);
                shadowArea.subtract(panelArea);
                
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_SPEED);
                //TODO it's a pretty crappy shadow but good enough for now
                g2.setColor(shadowColor);
                g2.fill(shadowArea);
                g2.setColor(Color.WHITE);
                g2.fill(panelArea);        
                if (bottomPanel.isVisible()) {
                    bottomGradient = new GradientPaint(0, bottomLocation.y, bottomPanelTopGradient,
                            0, bottomLocation.y + bottomPanel.getHeight(),
                            bottomPanelBottomGradient);
                    g2.setPaint(bottomGradient);
                    g2.fill(bottomArea);
                    g2.setColor(dividerColor);
                    g2.drawLine((int)panelShape.x, bottomLocation.y, (int)(panelShape.x + panelShape.getWidth()), bottomLocation.y);
                }
                g2.setColor(borderColor);
                g2.draw(panelShape);
                panelShape.x++;
                panelShape.y++;
                g2.draw(panelShape);
                g2.dispose();
            }
        });
        
    }

    private void initializeLabels() {
        titleLabel = new JLabel();
        FontUtils.bold(titleLabel);
        topLabel = new JLabel(I18n.tr("Currently sharing with"));
        friendLabel = new JXLabel(I18n.tr("Start typing a friend's name"));
        friendLabel.setForeground(Color.WHITE);
        friendLabel.setForegroundPainter(new TextShadowPainter());
        
        Dimension labelSize = friendLabel.getPreferredSize().width > topLabel.getPreferredSize().width ? 
                friendLabel.getPreferredSize() : topLabel.getPreferredSize();
        friendLabel.setPreferredSize(labelSize);
        
       // topLabel.setPreferredSize(labelSize);
        
        bottomLabel = new MultiLineLabel("", panelWidth);
        bottomLabel.setForeground(Color.WHITE);
     //   bottomLabel.setForegroundPainter(new TextShadowPainter());
    }

    private void initializeLists() {
        allFriendsSorted = GlazedListsFactory.sortedList(new BasicEventList<SharingTarget>(), new SharingTargetComparator());
        
        shareFriendList = new BasicEventList<SharingTarget>();
        shareFriendList.addListEventListener(new ListEventListener<SharingTarget>() {
            @Override
            public void listChanged(ListEvent<SharingTarget> listChanges) {
                adjustFriendLabelVisibility();
            }
        });
        
        noShareFriendList = GlazedListsFactory.sortedList(new BasicEventList<SharingTarget>(), new SharingTargetComparator());

        //using TextComponentMatcherEditor would cause problems because it also uses DocumentListener so we 
        //have no guarantee about the order of sorting and selecting
        TextFilterator<SharingTarget> textFilter = new TextFilterator<SharingTarget>() {
            @Override
            public void getFilterStrings(List<String> baseList, SharingTarget element) {
                baseList.add(element.getFriend().getName());
                baseList.add(element.getFriend().getId());
            }
        };
        textMatcher = new TextMatcherEditor<SharingTarget>(textFilter);
        
        final Matcher<SharingTarget> alwaysIncludeGnutellaMatcher = new Matcher<SharingTarget>() {
            @Override
            public boolean matches(SharingTarget item) {
                if (item.isGnutellaNetwork()) {
                    return true;
                }
                return textMatcher.getMatcher().matches(item);
            }
        };
        
        noShareFilterList = GlazedListsFactory.filterList(noShareFriendList, alwaysIncludeGnutellaMatcher);    
        
      
        
          

        // force list to filter and sort when textMatcher is updated
        textMatcher.addMatcherEditorListener(new MatcherEditor.Listener<SharingTarget>() {
            @Override
            public void changedMatcher(Event<SharingTarget> matcherEvent) {
                noShareFilterList.setMatcher(alwaysIncludeGnutellaMatcher);
                Comparator<SharingTarget> comparator = (inputField.getText() == null || inputField.getText().equals("")) ? 
                        new SharingTargetComparator() : new FilteredSharingTargetComparator();
                noShareFriendList.setComparator(comparator);
            }
        });

    }

    private void addComponents() {
        
        titlePanel.add(titleLabel, "aligny top, alignx left");
        titlePanel.add(closeButton, "aligny top, alignx right");
        
        tablePanel.add(topLabel, "hidemode 3, wrap");        
        tablePanel.add(shareScroll, "gaptop 6, gapbottom 6, growx");
        
        bottomPanel.add(friendLabel, "gaptop 6, hidemode 3, wrap");
        bottomPanel.add(comboPanel, "growx, gaptop 4, gapbottom 6, hidemode 3, wrap");
        bottomPanel.add(bottomLabel, "gapbottom 6, hidemode 3");
        

        mainPanel.add(titlePanel, "growx, gaptop 6, gapbottom 6, wrap");
        mainPanel.add(tablePanel, "growx, wrap, hidemode 3");
        mainPanel.add(bottomPanel, "growx, hidemode 3");
        
        add(mainPanel, "growx, gapleft " + BORDER_BUFFER + ", gapright " + BORDER_BUFFER + ", gaptop " + BORDER_BUFFER + ", gapbottom " + BORDER_BUFFER); 
    }

    private void initializeComboBox() {
        comboPanel = new LimeEditableComboBox();
        comboPanel.setPreferredSize(new Dimension(230, 24));
        comboPanel.setMaximumSize(comboPanel.getPreferredSize());   
        comboPanel.setMinimumSize(comboPanel.getPreferredSize());
        friendCombo = comboPanel.getComboBox();
        friendCombo.setModel(new EventComboBoxModel<SharingTarget>(noShareFilterList));
        initializeInputField();       
        
        comboPopup = comboPanel.getPopup();           
        // mouseClicked does not register on the combo box popup so we have to
        // use mousePressed and mouseReleased. (This is a workaround for
        // ActionEvents not being fired when the selected row is clicked)   
        MouseListener shareListener = new MouseAdapter() {
            private int pressedRow;
            @Override
            public void mousePressed(MouseEvent e) {
                pressedRow = comboPopup.getList().locationToIndex(e.getPoint());
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (pressedRow == comboPopup.getList().locationToIndex(e.getPoint())) {
                    shareSelectedFriend();
                }
            }
        };
        
        comboPopup.getList().addMouseListener(shareListener);
    }

    //must be called after friendCombo is initialized
    private void initializeInputField() {        
        inputField = comboPanel.getTextField();
        
        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(noShareFilterList.size() == 0 || 
                        !friendCombo.isPopupVisible() && ("".equals(inputField.getText()) || inputField.getText() == null)){
                    dialog.setVisible(false);
                } else if (friendCombo.getSelectedIndex() >= 0) {
                    shareSelectedFriend();
                }
            }
        });
        
        
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                update();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
               update();
            }
            
            private void update() {
                textMatcher.setFilterText(inputField.getText().split(" "));
                if (noShareFilterList.size() > 0) {
                    friendCombo.setSelectedIndex(0);
                  showPopup();
                }
            }
            
        });
    }

    //ensures popup is correct size when list filter is relaxed
    private void showPopup() {
        if (friendCombo.isShowing()) {
            friendCombo.hidePopup();
            friendCombo.showPopup();
        }
    }


    private void initializeShareTable() {
        final int actionCol = 0;
        shareTable = new MouseableTable(new EventTableModel<SharingTarget>(shareFriendList, new LibraryShareTableFormat(actionCol)));
        shareTable.setTableHeader(null);
        final ShareRendererEditor removeEditor = new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed);
        removeEditor.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
               unshareFriend(removeEditor.getFriend());
               removeEditor.cancelCellEditing();
               inputField.requestFocusInWindow();
            }            
        });
        removeEditor.setOpaque(false);
        //do nothing ColorHighlighter eliminates default striping
        shareTable.setHighlighters(new ColorHighlighter());
        shareTable.setRowHeight(removeEditor.getPreferredSize().height + 4);
        shareTable.getColumnModel().getColumn(actionCol).setCellEditor(removeEditor);
        shareTable.getColumnModel().getColumn(actionCol).setPreferredWidth(removeEditor.getPreferredSize().width + 4);    
        shareTable.getColumnModel().getColumn(actionCol).setMaxWidth(removeEditor.getPreferredSize().width + 4);    
        shareTable.getColumnModel().getColumn(actionCol).setCellRenderer(new ShareRendererEditor(removeIcon, removeIconRollover, removeIconPressed));      
        shareTable.setShowGrid(false);   
        shareTable.setIntercellSpacing(new Dimension(0, 0));
        shareTable.setOpaque(false);
        shareTable.setColumnSelectionAllowed(false);
        shareTable.setRowSelectionAllowed(false);
        
        
        shareScroll = new JScrollPane(shareTable);
        shareScroll.setBorder(new EmptyBorder(0, shareTableIndent, 0, 0));
        shareScroll.setOpaque(false);
        shareScroll.getViewport().setOpaque(false);
    
    }

    
    public void setTitleLabel(String text){
        titleLabel.setText(text);
    }
    
    public void setTopLabel(String text){
        topLabel.setText(text);
    }
    
    public void setComboLabelText(String text){
        friendLabel.setText(text);
    }

    public void setBottomLabel(String text){
        bottomLabel.setText(text);
    }
    

    private void adjustFriendLabelVisibility() {
        friendLabel.setVisible(friendCombo.isVisible());
    }
    
    private void shareSelectedFriend() {
        int index = friendCombo.getSelectedIndex();
        if (index > -1) {
            shareFriend(noShareFilterList.get(index));
            if (noShareFilterList.size() == 0) {
                inputField.setText(null);
            }
            resetRowSelection(index);

            if (noShareFriendList.size() > 0) {
                showPopup();
            }
        }
    }
    
    public void setComboBoxVisible(boolean visible){
        bottomPanel.setVisible(visible);
        bottomPanelHidden = !visible;
    }
    

    public void setTableVisible(boolean visible) {
        tableHidden = !visible;
        tablePanel.setVisible(visible);
    }

    public void setShareModel(LibraryShareModel shareModel){
        this.shareModel = shareModel;
        shareModel.addPropertyChangeListener(this);
    }
    
    public LibraryShareModel getShareModel(){
        return shareModel;
    }

    
    private void setKeyStrokes(JComponent component) {
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "up");
        component.getActionMap().put("up", up);
        component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "down");
        component.getActionMap().put("down", down);       
    }

    
    public void show(Component c) {       
        inputField.setText(null);
        if (noShareFilterList.size() > 0) {
            friendCombo.setSelectedIndex(0);
        }
        reloadSharedBuddies();
        adjustSize();
        dialog.show(this);
        inputField.requestFocusInWindow();
    }
    
    public boolean contains(Component c) {
        for (; c != null; c = c.getParent()) {
            if (c == this || c == comboPopup) {
                return true;
            }
        }
        return false;
    }



    private void resetRowSelection(int oldSelRow) {
        if(noShareFilterList.size() == 0){
            return;
        }
        
        if(oldSelRow == -1){
            friendCombo.setSelectedIndex(0);
        } else if(noShareFilterList.size() > oldSelRow){
            friendCombo.setSelectedIndex(oldSelRow);
        } else if ((oldSelRow - 1) >= 0 && (oldSelRow - 1) < noShareFilterList.size()) {
            friendCombo.setSelectedIndex(oldSelRow - 1);
        }
    }
    
    
    private void shareFriend(SharingTarget friend) {
        shareFriendList.add(friend);
        noShareFriendList.remove(friend);

        shareModel.shareFriend(friend);
        
        adjustSize();
        if (shareScroll.getVerticalScrollBar().isShowing()){
            shareTable.ensureRowVisible(shareTable.getRowCount() - 1);
        }
        //bit heavy handed here but it ensures that changes are reflected everywhere
        GuiUtils.getMainFrame().repaint();
    }
    
    
    private void unshareFriend(SharingTarget friend) {
        shareFriendList.remove(friend);
        noShareFriendList.add(friend);
        
        shareModel.unshareFriend(friend);
        
        adjustSize();
      //bit heavy handed here but it ensures that changes are reflected everywhere
        GuiUtils.getMainFrame().repaint();
    }
    
    private void adjustSize(){
        adjustFriendLabelVisibility();
        bottomPanel.setVisible(!bottomPanelHidden && noShareFriendList.size() > 0);
        
        if (!tableHidden ) {
            int visibleRows = (shareTable.getRowCount() < SHARED_ROW_COUNT) ? shareTable.getRowCount() : SHARED_ROW_COUNT;
            shareTable.setVisibleRowCount(visibleRows);
            tablePanel.setVisible(visibleRows > 0);
        }
        
        setSize(getPreferredSize());        
    }
    
    
    
    public void dispose() {
        noShareFriendList.dispose();
        noShareFilterList.dispose();
        allFriendsSorted.dispose();
        
        ((EventTableModel)shareTable.getModel()).dispose();
        ((EventComboBoxModel)comboPanel.getComboBox().getModel()).dispose();
    }
    
    
    private void reloadSharedBuddies() {
        shareFriendList.clear();
        noShareFriendList.clear();
        allFriendsSorted.clear();

        if (shareModel.isGnutellaNetworkSharable()) {
            loadFriend(SharingTarget.GNUTELLA_SHARE);
        }
        
        for (Friend friend : allFriends) {
            allFriendsSorted.add(new SharingTarget(friend));
        }
        
        for (SharingTarget target : allFriendsSorted){            
            loadFriend(target);
        }
    }
    
    private void loadFriend(SharingTarget friend) {
        if (isShared(friend)) {
            shareFriendList.add(friend);
        } else {
            noShareFriendList.add(friend);
        }
    }
    
    private boolean isShared(SharingTarget friend){
        return shareModel.isShared(friend);
    }   
    
            
    private static class LibraryShareTableFormat implements WritableTableFormat<SharingTarget> {
        private int editColumn;
        
        public LibraryShareTableFormat(int editedColumn){
            this.editColumn = editedColumn;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return null;
        }

        @Override
        public Object getColumnValue(SharingTarget baseObject, int column) {
            if(column == editColumn){
                return baseObject;
            }
            return baseObject.getFriend().getRenderName();
        }

        @Override
        public boolean isEditable(SharingTarget baseObject, int column) {
            return true;
        }

        @Override
        public SharingTarget setColumnValue(SharingTarget baseObject, Object editedValue, int column) {
            return baseObject;
        }

    }
    
    private static class SharingTargetComparator implements Comparator<SharingTarget>{

        @Override
        public int compare(SharingTarget o1, SharingTarget o2) {
            if (o1 == o2){
                return 0;
            }
            if(o1.isGnutellaNetwork()){
                return -1;
            }
            if(o2.isGnutellaNetwork()){
                return 1;
            }
            return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
        }
        
    }
    
    private static class FilteredSharingTargetComparator implements Comparator<SharingTarget>{

        @Override
        public int compare(SharingTarget o1, SharingTarget o2) {
            if (o1 == o2){
                return 0;
            }
            if(o1.isGnutellaNetwork()){
                return 1;
            }
            if (o2.isGnutellaNetwork()) {
                    return -1;
            }
            return o1.getFriend().getRenderName().compareTo(o2.getFriend().getRenderName());
        }
        
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // the share model file or category has changed - reload friends.
        reloadSharedBuddies();
    }
    
}
