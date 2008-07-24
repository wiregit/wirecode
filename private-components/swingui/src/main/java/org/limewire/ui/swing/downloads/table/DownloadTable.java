package org.limewire.ui.swing.downloads.table;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.HighlighterFactory;
import org.limewire.core.api.download.DownloadItem;

public class DownloadTable extends JXTable {

	public DownloadTable(TableModel model) {
		super(model);
		setRolloverEnabled(false);
		setFocusable(false);
		setEditable(true);
		setShowGrid(false, false);
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		setHighlighters(HighlighterFactory.createSimpleStriping());
		//This doesn't work with editing on rollover
		//addHighlighter(new ColorHighlighter(HighlightPredicate.ROLLOVER_ROW, Color.CYAN, Color.BLACK));		

		DownloadRendererEditor editor = new DownloadRendererEditor();
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

		});
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
