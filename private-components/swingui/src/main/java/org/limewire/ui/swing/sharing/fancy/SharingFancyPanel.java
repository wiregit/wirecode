package org.limewire.ui.swing.sharing.fancy;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.VerticalLayout;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.images.ThumbnailManager;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.sharing.SharingShortcutPanel;
import org.limewire.ui.swing.sharing.dragdrop.SharingTransferHandler;
import org.limewire.ui.swing.sharing.table.SharingAudioTableFormat;
import org.limewire.ui.swing.sharing.table.SharingDefaultTableFormat;
import org.limewire.ui.swing.sharing.table.SharingIconTableFormat;
import org.limewire.ui.swing.table.IconLabelRenderer;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.gui.TableFormat;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class SharingFancyPanel extends JPanel implements Scrollable {

    private final String music = I18n.tr(Category.AUDIO.toString());
    private final String video = I18n.tr(Category.VIDEO.toString());
    private final String image = I18n.tr(Category.IMAGE.toString());
    private final String doc = I18n.tr(Category.DOCUMENT.toString());
    private final String program = I18n.tr(Category.PROGRAM.toString());
    private final String other = I18n.tr(Category.OTHER.toString());
    
    private final CategoryIconManager categoryIconManager;
    
    private final Map<String, SharingFancyTablePanel> tables = new HashMap<String, SharingFancyTablePanel>();

    private SharingFancyListPanel imageList;
    
    private SharingShortcutPanel shortcuts;
    
    private final JScrollPane scrollPane;
    
    private IconLabelRenderer iconLabelRenderer;
    
    private final SharingTransferHandler transferHandler;
    
    private final EnumMap<Category, FilterList<LocalFileItem>> filterLists = new EnumMap<Category, FilterList<LocalFileItem>>(Category.class);
       
    @AssistedInject
    public SharingFancyPanel(CategoryIconManager categoryIconManager, IconManager iconManager, ThumbnailManager thumbnailManager,
            @Assisted EventList<LocalFileItem> eventList, @Assisted JScrollPane scrollPane,  @Assisted LocalFileList originalList) {

        this.categoryIconManager = categoryIconManager;
        
        GuiUtils.assignResources(this); 
        
        this.scrollPane = scrollPane;
        this.scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        this.transferHandler = new SharingTransferHandler(originalList, false);

        createFilteredLists(eventList);
        
        addTable(music, Category.AUDIO, originalList, new SharingAudioTableFormat(), true);       
        addTable(video, Category.VIDEO, originalList, new SharingDefaultTableFormat(), false);
        addTable(doc, Category.DOCUMENT, originalList, new SharingIconTableFormat(), false);       
        addTable(program, Category.PROGRAM, originalList, new SharingDefaultTableFormat(), false);       
        addTable(other, Category.OTHER, originalList, new SharingDefaultTableFormat(), true);
        
        imageList = new SharingFancyListPanel(image, filterLists.get(Category.IMAGE),
                transferHandler, originalList, 
                categoryIconManager.getIcon(Category.IMAGE), thumbnailManager);
        
        
        imageList.getList().addMouseListener(new MultiTableMouseListener());
        
        
        TableColumn tc = tables.get(doc).getTable().getColumn(0);
        iconLabelRenderer = new IconLabelRenderer(iconManager);
        tc.setCellRenderer(iconLabelRenderer);
        
        shortcuts = new SharingShortcutPanel(
                new String[]{music, video, image, doc, program, other},
                new Action[]{new BookmarkJumpAction(this, tables.get(music)),
                             new BookmarkJumpAction(this, tables.get(video)),
                             new BookmarkJumpAction(this, imageList),
                             new BookmarkJumpAction(this, tables.get(doc)),
                             new BookmarkJumpAction(this, tables.get(program)),
                             new BookmarkJumpAction(this, tables.get(other))},
                getFilteredListsInOrderOf(
                                Category.AUDIO, Category.VIDEO,
                                Category.IMAGE, Category.DOCUMENT,
                                Category.PROGRAM, Category.OTHER),
                new boolean[]{true,true,true,true,false,false});

       setLayout(new VerticalLayout());
        
       add(shortcuts);
        
       add(tables.get(music));
       add(tables.get(video));
       add(imageList);
       add(tables.get(doc));
       add(tables.get(program));
       add(tables.get(other));

       this.setTransferHandler(transferHandler);
    }
    
    /**
     * Constructs a SharingFancyTablePanel for a given Category
     */
    private void addTable(String title, Category category, LocalFileList originalList, TableFormat<LocalFileItem> tableFormat, boolean displayHeader) {
       
        SharingFancyTablePanel tablePanel = new SharingFancyTablePanel(title, filterLists.get(category), 
                tableFormat, displayHeader, transferHandler, originalList, categoryIconManager.getIcon(category));
        
        tablePanel.getTable().addMouseListener(new MultiTableMouseListener());
        tables.put(title, tablePanel); 
    }
        
    public void setModel(EventList<LocalFileItem> eventList, LocalFileList fileList) {
        //TODO: these need to be lazily created and stored somewhere, FileList?? so we don't keep recreating them
        //		GlazedLists.multiMap would be perfect if they supported EventLists instead of Lists
        createFilteredLists(eventList);
        
        tables.get(music).setModel(filterLists.get(Category.AUDIO), fileList);
        tables.get(video).setModel(filterLists.get(Category.VIDEO), fileList);
        imageList.setModel(filterLists.get(Category.IMAGE), fileList);
        tables.get(doc).setModel(filterLists.get(Category.DOCUMENT), fileList);
        tables.get(program).setModel(filterLists.get(Category.PROGRAM), fileList);
        tables.get(other).setModel(filterLists.get(Category.OTHER), fileList);
        
        TableColumn tc = tables.get(doc).getTable().getColumn(0);
        tc.setCellRenderer(iconLabelRenderer);
        
        transferHandler.setModel(fileList);
        
        scrollPane.getViewport().setViewPosition(new Point(0, 0));
        shortcuts.setModel(getFilteredListsInOrderOf(
                Category.AUDIO, Category.VIDEO,
                Category.IMAGE, Category.DOCUMENT,
                Category.PROGRAM, Category.OTHER));
    }
    
    private List<FilterList<LocalFileItem>> getFilteredListsInOrderOf(Category... categories) {
        List<FilterList<LocalFileItem>> list = new ArrayList<FilterList<LocalFileItem>>(categories.length);
        for(Category category : categories) {
            list.add(filterLists.get(category));
        }
        return list;
    }
    
    private void createFilteredLists(EventList<LocalFileItem> eventList) {
        // First dispose of the old ones!
        final List<FilterList<LocalFileItem>> oldLists = new ArrayList<FilterList<LocalFileItem>>(filterLists.values());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                for(FilterList<?> filterList : oldLists) {
                    filterList.dispose();
                }
            }
        });
        
        filterLists.put(Category.AUDIO, GlazedListsFactory.filterList(eventList, new CategoryFilter(Category.AUDIO)));
        filterLists.put(Category.VIDEO, GlazedListsFactory.filterList(eventList, new CategoryFilter(Category.VIDEO)));
        filterLists.put(Category.IMAGE, GlazedListsFactory.filterList(eventList, new CategoryFilter(Category.IMAGE)));
        filterLists.put(Category.DOCUMENT, GlazedListsFactory.filterList(eventList, new CategoryFilter(Category.DOCUMENT)));
        filterLists.put(Category.PROGRAM, GlazedListsFactory.filterList(eventList, new CategoryFilter(Category.PROGRAM)));
        filterLists.put(Category.OTHER, GlazedListsFactory.filterList(eventList, new CategoryFilter(Category.OTHER)));
    }
    
    /**
     * UnSelects other tables and lists when a table or list is selected
     */
    private class MultiTableMouseListener extends MouseAdapter {
        
        @Override
        public void mouseClicked(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                if(e.getComponent() instanceof JList) {
                    for (SharingFancyTablePanel table : tables.values()) {
                        table.getTable().clearSelection();
                    }
                }
                else {
                    JTable selectedTable = (JTable) e.getComponent();
                    for (SharingFancyTablePanel table : tables.values()) {
                        if (table.getTable() != selectedTable) {
                            table.getTable().clearSelection();
                        }
                    }
                    imageList.getList().clearSelection();
                }
            }
        }
    }
    
    /**
     * When the action is performed, the scrollpane is repositioned 
     * to display the linkComponent in the ViewPort. If no scrollBar
     * is currently displayed, the action will not be performed.
     */
    private class BookmarkJumpAction extends AbstractAction {

        private JComponent parentContainer;
        private JComponent linkContainer;
        
        public BookmarkJumpAction(JComponent parentComponent, JComponent linkComponent) {
            this.parentContainer = parentComponent;
            this.linkContainer = linkComponent;
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if( parentContainer.getVisibleRect().height < parentContainer.getPreferredSize().height ) {
                int position = linkContainer.getLocation().y;
                if(position + parentContainer.getVisibleRect().height > parentContainer.getHeight())
                    position = parentContainer.getSize().height - parentContainer.getVisibleRect().height;

                scrollPane.getViewport().setViewPosition(new Point(parentContainer.getLocation().x, position));
            }
        }
    }
    
    /**
     * Overrides getPreferredSize. getPreferredSize is used by scrollable to 
     * determine how big to make the scrollableViewPort. Uses the parent 
     * component to set the appropriate size thats currently visible. This
     * prevents the image list from growing too wide and never shrinking again.
     */
    @Override
    public Dimension getPreferredSize() {
        Dimension dimension = super.getPreferredSize();
        if (getParent() == null)
            return dimension;
        return new Dimension(getParent().getWidth(), dimension.height);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return false;
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return getPosition(visibleRect, orientation, direction);
    }
    
    /**
     * Scrolls to the appropriate location based on the height of a thumbnail image.
     */
    private int getPosition(Rectangle visibleRect, int orientation, int direction) {
        int currentPosition = 0;
        if (orientation == SwingConstants.HORIZONTAL)
            currentPosition = visibleRect.x;
        else
            currentPosition = visibleRect.y;
    
        int height = imageList.getList().getFixedCellHeight();
        
        if (direction < 0) {
            int newPosition = currentPosition - (currentPosition / height) * height;
            return (newPosition == 0) ? height : newPosition;
        } else {
            return ((currentPosition / height) + 1) * height - currentPosition;
        }
    }
}
