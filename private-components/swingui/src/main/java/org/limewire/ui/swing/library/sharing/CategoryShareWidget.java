package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.CategoryShareModel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;

public class CategoryShareWidget implements ShareWidget<Category> {
private LibrarySharePanel sharePanel;
    
    public CategoryShareWidget(ShareListManager shareListManager, EventList<SharingTarget> allFriends, ShapeDialog shapeDialog){
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog);
        sharePanel.setShareModel(new CategoryShareModel(shareListManager));
    }
    
    @Override
    public void show(Component c) {
        sharePanel.show(null);
    }
    

    @Override
    public void dispose() {
        sharePanel.dispose();
    }
    

    @Override
    public void setShareable(Category category) {
        ((CategoryShareModel)sharePanel.getShareModel()).setCategory(category);
        String catStr = category.getSingularName();
        sharePanel.setTitleLabel(I18n.tr("Share {0} collection", catStr));
        sharePanel.setTopLabel("Sharing collection with:");
        sharePanel.setBottomLabel(
                I18n.tr("Sharing your {0} collection shares new {1} files that automatically get added to your Library", catStr, catStr.toLowerCase()));
   
    }

}
