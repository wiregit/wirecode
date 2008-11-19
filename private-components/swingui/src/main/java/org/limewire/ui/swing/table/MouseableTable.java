package org.limewire.ui.swing.table;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.ui.swing.util.GuiUtils;

import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;

public class MouseableTable extends StripedJXTable {

	private TablePopupHandler popupHandler;

	private TableDoubleClickHandler rowDoubleClickHandler;
	
	private TableColumnDoubleClickHandler columnDoubleClickHandler;
	
	private TableColors colors = newTableColors();
	
	private boolean stripesPainted = false;
	
	protected MouseMotionListener mouseOverEditorListener;
	
	public MouseableTable() {
		initialize();
	}
	
	protected TableColors newTableColors() {
        return new TableColors();
    }
	
	public TableColors getTableColors() {
	    return colors;
	}
	
	public MouseableTable(TableModel model) {
		super(model);
		initialize();
	}

	public void setPopupHandler(TablePopupHandler popupHandler) {
		this.popupHandler = popupHandler;
	}
	
	public void setDoubleClickHandler(TableDoubleClickHandler tableDoubleClickHandler) {
		this.rowDoubleClickHandler = tableDoubleClickHandler;
	}
	
	public void setColumnDoubleClickHandler(TableColumnDoubleClickHandler columnDoubleClickHandler) {
        this.columnDoubleClickHandler = columnDoubleClickHandler;
    }
	   
	   
	protected void initialize() {	
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		setCellSelectionEnabled(false);
		setRowSelectionAllowed(true);

		//HighlightPredicate.EVEN and HighlightPredicate.ODD are zero based
		setHighlighters(colors.getEvenHighLighter(), 
		                colors.getOddHighLighter(),
		                new ColorHighlighter(new MenuHighlightPredicate(this), colors.menuRowColor,  colors.menuRowForeground, colors.menuRowColor, colors.menuRowForeground));
		
		//so that mouseovers will work within table		
		mouseOverEditorListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                // Get the table cell that the mouse is over.
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                
                // If the cell is editable and
                // it's not already being edited ...
                if (isCellEditable(row, col) && (row != getEditingRow() || col != getEditingColumn())) {
                    editCellAt(row, col);
                }
            }
        };
        
		addMouseMotionListener(mouseOverEditorListener);
		
		addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {//adding this to editor messes up popups

				int col = columnAtPoint(e.getPoint());
				int row = rowAtPoint(e.getPoint());

				if (row >= 0 && col >= 0) {
					if (rowDoubleClickHandler != null || columnDoubleClickHandler != null) {
						Component component = e.getComponent();
						//launch file on double click unless the click is on a button
						if (e.getClickCount() == 2
								&& !(component.getComponentAt(e.getPoint()) instanceof JButton)) {
                            if (rowDoubleClickHandler != null) {
                                rowDoubleClickHandler.handleDoubleClick(row);
                            }
                            if (columnDoubleClickHandler != null) {
                                columnDoubleClickHandler.handleDoubleClick(col);
                            }
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
			
//            //This isn't necessary if we aren't doing mouseover highlighting
//			@Override
//			public void mouseExited(MouseEvent e) {
//                // clears mouseover color -
//                // necessary for coordinating between multiple tables
//
//				TableCellEditor editor = getCellEditor();
//				Component component = e.getComponent();
//
//				// if component isn't editor we shouldn't be editing
//				if (editor != null && component != editor) {
//					// check subcomponent too - this prevents
//                    // color from flashing when mousing over the buttons
//					Component componentAtPoint =
//                        component.getComponentAt(e.getPoint());
//
//					if (componentAtPoint != editor) {
//                        // Mark commented the following line.
//						//editor.cancelCellEditing();
//					}
//				}
//			}
			
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
                        popupHandler.maybeShowPopup(
                            e.getComponent(), e.getX(), e.getY());
                        TableCellEditor editor = getCellEditor();
                        if (editor != null) {
                            editor.cancelCellEditing();
                        }
                    }
                }
            }

		});
	}
	//Don't set the cell value when editing is cancelled
	@Override
    public void editingStopped(ChangeEvent e) {
        TableCellEditor editor = getCellEditor();
        if (editor != null) {          
            removeEditor();
        }
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
    public boolean isMenuShowing(int row) {
    	if(popupHandler != null) {
    		return popupHandler.isPopupShowing(row);
    	}
    	return false;
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (row >= getRowCount() || col >= getColumnCount() || row < 0 || col < 0) {
            return false;
        }
    	return getColumnModel().getColumn(col).getCellEditor() != null;
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
	    @Resource
	    private Color disabledForegroundColor;
	    
	    private ColorHighlighter evenHighLighter;
	    
	    private ColorHighlighter oddHighlighter;
	    
	    public TableColors() {
	        GuiUtils.assignResources(TableColors.this);
	        
	        evenHighLighter = new ColorHighlighter(HighlightPredicate.EVEN, evenColor, evenForeground, selectionColor, selectionForeground);
	        oddHighlighter = new ColorHighlighter(HighlightPredicate.ODD, oddColor, oddForeground, selectionColor, selectionForeground);
	    }
	    
	    public ColorHighlighter getEvenHighLighter() {
	        return evenHighLighter;
	    }
	    
	    public ColorHighlighter getOddHighLighter() {
	        return oddHighlighter;
	    }
	    
	    public Color getDisabledForegroundColor() {
	        return disabledForegroundColor;
	    }
	}
	
    @Override
    public void setDefaultEditor(Class clazz, TableCellEditor editor) {
        boolean usesEventTableModel = getModel() instanceof EventTableModel;
        boolean usesAdvancedTableFormat = false;
        TableFormat tableFormat = null;

        if (usesEventTableModel) {
            tableFormat = ((EventTableModel) getModel()).getTableFormat();
            usesAdvancedTableFormat =
                tableFormat instanceof AdvancedTableFormat;
        }

        if (usesEventTableModel && usesAdvancedTableFormat) {
            AdvancedTableFormat format = (AdvancedTableFormat) tableFormat;
            for (int i = 0; i < getModel().getColumnCount(); i++) {
                Class columnClass = format.getColumnClass(i);
                if (columnClass == clazz) {
                    getColumnModel().getColumn(i).setCellEditor(editor);
                }
            }
        } else {
            super.setDefaultEditor(clazz, editor);
        }
    }

    @Override
    public void setDefaultRenderer(Class clazz, TableCellRenderer renderer) {
        boolean usesEventTableModel = getModel() instanceof EventTableModel;
        boolean usesAdvancedTableFormat = false;
        TableFormat tableFormat = null;

        if (usesEventTableModel) {
            tableFormat = ((EventTableModel) getModel()).getTableFormat();
            usesAdvancedTableFormat =
                tableFormat instanceof AdvancedTableFormat;
        }

        if (usesEventTableModel && usesAdvancedTableFormat) {
            AdvancedTableFormat format = (AdvancedTableFormat) tableFormat;
            for (int i = 0; i < getModel().getColumnCount(); i++) {
                Class columnClass = format.getColumnClass(i);
                if (columnClass == clazz) {
                    getColumnModel().getColumn(i).setCellRenderer(renderer);
                    break;
                }
            }
        } else {
            super.setDefaultRenderer(clazz, renderer);
        }
    }
    
    public void setStripesPainted(boolean painted){
        stripesPainted = painted;
    }
    /**
     * The parent paints all the real rows then the remaining space is calculated
     * and appropriately painted with grid lines and background colors. These 
     * rows are not selectable.
     */
    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (stripesPainted) {
            super.paintEmptyRows(g);
        }
    }
}