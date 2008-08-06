package org.limewire.ui.swing.downloads.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.jdesktop.swingx.decorator.Highlighter;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.EventList;

public class DownloadTable extends JXTable {
    /**
     * these consider the first element even (zero based)
     */
    @Resource
    private Color oddColor;
    @Resource
    private Color oddForeground;
    @Resource
    private Color evenColor;
    @Resource
    private Color evenForeground;
    @Resource
    private Color menuRowBackground;
    @Resource
    private Color menuRowForeground;
    
    private DownloadRendererEditor editor;
    
    private HighlightPredicate menuRowPredicate = new MenuHighlightPredicate();
    private Highlighter menuRowHighlighter;

	public DownloadTable(EventList<DownloadItem> downloadItems) {
		super(new DownloadTableModel(downloadItems));
		GuiUtils.assignResources(this);
		setRolloverEnabled(false);
		setFocusable(false);
		setEditable(true);
		setShowGrid(false, false);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
        setHighlighters(new ColorHighlighter(HighlightPredicate.ODD, oddColor, oddForeground, oddColor, oddForeground),
                new ColorHighlighter(HighlightPredicate.EVEN, evenColor, evenForeground, evenColor, evenForeground));
        addMenuRowHighlighter();
		// This doesn't work with editing on rollover - create custom
        // HighlightPredicate that detects editing to change editor color
		//addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.CYAN, Color.BLACK));		

		editor = new DownloadRendererEditor();
        editor.initializeEditor(downloadItems);
		DownloadRendererEditor renderer = new DownloadRendererEditor();
		
		setDefaultEditor(DownloadItem.class, editor);
		setDefaultRenderer(DownloadItem.class, renderer);
		
		setRowHeight(renderer.getPreferredSize().height);

		//so that mouseovers will work within table
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
                int col = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());
                editCellAt(row, col);
            }

		});

		//clears mouseover color - necessary for coordinating between multiple tables
		addMouseListener(new MouseAdapter() {
		    @Override
		    public void mouseClicked(MouseEvent e){//adding this to editor messes up popups
                if (e.getClickCount() == 2){
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    DownloadItem item = (DownloadItem)getValueAt(row, col);
                    throw new RuntimeException("Implement launch " + item.getTitle() + "!");
                }
            }
		    
			@Override
			public void mouseExited(MouseEvent e) {
				TableCellEditor editor = getCellEditor();
				Component component = e.getComponent();
				//if component isn't editor we shouldn't be editing
				if (editor != null && component != editor) {
					//check subcomponent too - this prevents color from flashing
					//when mousing over the buttons
					if (component == null
							|| component.getComponentAt(e.getPoint()) != editor) {
						editor.cancelCellEditing();
					}

				}
			}
			
			  @Override
            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    editor.showPopupMenu(e.getComponent(), e.getX(), e.getY());
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    //update editor colors
                    prepareEditor(editor, row, col);
                    //editor.repaint() takes about a second to show
                    repaint();
                }
            }

		});
		
	}
	

    public void addMenuRowHighlighter() {
        if(menuRowHighlighter == null){
            menuRowHighlighter = new ColorHighlighter(menuRowPredicate, menuRowBackground, menuRowForeground);
        }
        addHighlighter(menuRowHighlighter);
    }


    // gets rid of default editor color so that editors are colored by highlighters
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component comp = super.prepareEditor(editor, row, column);
        ComponentAdapter adapter = getComponentAdapter(row, column);
        if (compoundHighlighter != null) {
            comp = compoundHighlighter.highlight(comp, adapter);
        }
        return comp;
    }
	   

	//overridden so that cell editor buttons will always work
	@Override
	public boolean isCellEditable(int row, int column) {
		return row < getRowCount();
	}
	
	
	/**
	 * Does this row have a popup menu showing?
	 */
    private class MenuHighlightPredicate implements HighlightPredicate {        
        
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            if (!adapter.getComponent().isEnabled()) return false;
            
            if (adapter.getValue() instanceof DownloadItem){
                DownloadItem item = (DownloadItem)adapter.getValue();
               return (editor.isItemMenuVisible(item));
            }
            return false;
        }
        
    }
}
