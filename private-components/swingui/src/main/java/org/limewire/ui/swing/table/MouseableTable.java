package org.limewire.ui.swing.table;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.util.GuiUtils;

//TODO comment this beast
public class MouseableTable extends JXTable {
    

	
	private TablePopupHandler popupHandler;

	private TableDoubleClickHandler doubleClickHandler;
	
	private TableColors colors = new TableColors();
	
	public MouseableTable(){
		initialize();
	}
	
	public MouseableTable(TableModel model){
		super(model);
		initialize();
	}
	
	
	public void setPopupHandler(TablePopupHandler popupHandler){
		this.popupHandler = popupHandler;
	}
	
	public void setDoubleClickHandler(TableDoubleClickHandler tableDoubleClickHandler){
		this.doubleClickHandler = tableDoubleClickHandler;
	}
	
	
	protected void initialize(){	
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setCellSelectionEnabled(false);
		setRowSelectionAllowed(true);

		//HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
		setHighlighters(
		        new ColorHighlighter(HighlightPredicate.EVEN, colors.evenColor, colors.evenForeground, colors.selectionColor, colors.selectionForeground),
                new ColorHighlighter(HighlightPredicate.ODD, colors.oddColor, colors.oddForeground, colors.selectionColor, colors.selectionForeground),
                new ColorHighlighter(new MenuHighlightPredicate(this), colors.menuRowColor,  colors.menuRowForeground, colors.menuRowColor, colors.menuRowForeground));
		
		//so that mouseovers will work within table
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
                int col = columnAtPoint(e.getPoint());
                int row = rowAtPoint(e.getPoint());
                editCellAt(row, col);
            }

		});

		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {//adding this to editor messes up popups

				int col = columnAtPoint(e.getPoint());
				int row = rowAtPoint(e.getPoint());

				if (row >= 0 && col >= 0) {
					if (doubleClickHandler != null) {
						Component component = e.getComponent();
						//launch file on double click unless the click is on a button
						if (e.getClickCount() == 2
								&& !(component.getComponentAt(e.getPoint()) instanceof JButton)) {
							doubleClickHandler.handleDoubleClick(row);
						}
					}
					TableCellEditor editor = getCellEditor();
					if (editor != null) {
						// force update editor colors
						prepareEditor(editor, row, col);
						// editor.repaint() takes about a second to show sometimes
						repaint();
					}
				}
			}

			@Override
			public void mouseExited(MouseEvent e) {//clears mouseover color - necessary for coordinating between multiple tables
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
                if (e.isPopupTrigger() && popupHandler != null) {
                    int col = columnAtPoint(e.getPoint());
                    int row = rowAtPoint(e.getPoint());
                    if (row >= 0 && col >= 0) {
                        popupHandler.maybeShowPopup(e.getComponent(), e.getX(), e.getY());
                        TableCellEditor editor = getCellEditor();
                        if (editor != null) {
							// force update editor colors
							prepareEditor(editor, row, col);
							// editor.repaint() takes about a second to show sometimes
							repaint();
                        }
                    }
                }
            }

		});
	}

    // gets rid of default editor color so that editors are colored by highlighters and selection color is shown
    @Override
    public Component prepareEditor(TableCellEditor editor, int row, int column) {
        Component comp = super.prepareEditor(editor, row, column);
        
        if (compoundHighlighter != null) {
            ComponentAdapter adapter = getComponentAdapter(row, column);
            comp = compoundHighlighter.highlight(comp, adapter);
        }
        
        return comp;
    }
	          
    /**
     * @return whether or not a popup menu is showing on the row
     */
    public boolean isMenuShowing(int row){
    	if(popupHandler != null){
    		return popupHandler.isPopupShowing(row);
    	}
    	return false;
    }
    
    @Override
    public boolean isCellEditable(int row, int col){
        boolean hasClassEditor = false;//getDefaultEditor(getValueAt(row, col).getClass()) != null;
    	boolean hasColumnEditor = getColumnModel().getColumn(col).getCellEditor() != null;
    	return hasClassEditor || hasColumnEditor;
    }
    

	/**
	 * Does this row have a popup menu showing?
	 */
	public static class MenuHighlightPredicate implements HighlightPredicate {

		private MouseableTable table;

		public MenuHighlightPredicate(MouseableTable table) {
			this.table = table;
		}

		public boolean isHighlighted(Component renderer,
				ComponentAdapter adapter) {
			if (!adapter.getComponent().isEnabled())
				return false;

			return table.isMenuShowing(adapter.row);
		}

	}
	
	public static class TableColors {
	    //TODO: inject these
	    /**
	     * these consider the first element even (zero based)
	     */
	    @Resource
	    public Color evenColor;
	    @Resource
	    public Color evenForeground;
	    @Resource
	    public Color oddColor;
	    @Resource
	    public Color oddForeground;
	    @Resource
	    public Color menuRowColor;
	    @Resource
	    public Color menuRowForeground;    
	    @Resource
	    public Color selectionColor;
	    @Resource
	    public Color selectionForeground;
	    
	    public TableColors(){
	        GuiUtils.assignResources(TableColors.this);
	    }
	    
	}
}
