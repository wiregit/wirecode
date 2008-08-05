package org.limewire.ui.swing.downloads.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.core.api.download.DownloadItem;

import ca.odell.glazedlists.EventList;

public class DownloadTable extends JXTable {
    /**
     * these consider the first element odd (not zero based)
     */
    private Color oddColor = Color.WHITE;
    private Color oddForeground = Color.BLACK;
    private Color evenColor = Color.LIGHT_GRAY;
    private Color evenForeground = Color.BLACK;
    private Color menuBackground = Color.BLUE.brighter();
    private Color menuForeground = Color.BLACK;
    
    private DownloadRendererEditor editor;
    
    private HighlightPredicate menuRowPredicate = new HighlightPredicate() {
        
        
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            if (!adapter.getComponent().isEnabled()) return false;
            
            if (adapter.getValue() instanceof DownloadItem){
                DownloadItem item = (DownloadItem)adapter.getValue();
               return (editor.isItemMenuVisible(item));
            }
            return false;
        }
        
    };

	public DownloadTable(EventList<DownloadItem> downloadItems) {
		super(new DownloadTableModel(downloadItems));
		setRolloverEnabled(false);
		setFocusable(false);
		setEditable(true);
		setShowGrid(false, false);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based to even and odd are backwards here
        setHighlighters(new ColorHighlighter(HighlightPredicate.EVEN, oddColor, oddForeground, oddColor, oddForeground),
                new ColorHighlighter(HighlightPredicate.ODD, evenColor, evenForeground, evenColor, evenForeground),
                new ColorHighlighter(menuRowPredicate, menuBackground, menuForeground));
        
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
				Component component = getComponentAt(e.getPoint());
				if (component instanceof JTable) {
					JTable table = (JTable) component;
					int col = table.columnAtPoint(e.getPoint());
					int row = table.rowAtPoint(e.getPoint());
					table.editCellAt(row, col);
				}
			}

		});

		//clears mouseover color - necessary for coordinating between multiple tables
		addMouseListener(new MouseAdapter() {
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
	   
	//TODO: get rid of this hack.  GlazedLists isn't playing nicely with the editor
	//@Override
	//public void setValueAt(Object aValue, int row, int column) {}

	
//	these are necessary if we use java 6 filtering and swingx (switched to glazed lists)
//	//JXTable screws up java 6 sorting and filtering
//	@Override
//	public int convertRowIndexToModel(int viewRowIndex) {
//		if (getRowSorter() == null) {
//			return viewRowIndex;
//		}
//		return getRowSorter().convertRowIndexToModel(viewRowIndex);
//	}
//
//	//JXTable screws up java 6 sorting and filtering
//	@Override
//	public int convertRowIndexToView(int modelRowIndex) {
//		if (getRowSorter() == null) {
//			return modelRowIndex;
//		}
//		return getRowSorter().convertRowIndexToView(modelRowIndex);
//	}
//
//	//JXTable screws up java 6 sorting and filtering
//	@Override
//	public int getRowCount() {
//		if (getRowSorter() == null) {
//			return getModel().getRowCount();
//		}
//		return getRowSorter().getViewRowCount();
//	}

	//overridden so that cell editor buttons will always work
	@Override
	public boolean isCellEditable(int row, int column) {
		return row < getRowCount();
	}
}
