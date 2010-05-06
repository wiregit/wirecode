package org.limewire.ui.swing.player;

import java.io.File;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class PlayerMediatorImpl implements PlayerMediator {

    private final CategoryManager categoryManager;
    
    @Inject
    PlayerMediatorImpl(CategoryManager categoryManager) {
        this.categoryManager = categoryManager;
    }

    @Override
    public void play(LocalFileItem localFileItem) {
        play(localFileItem.getFile());
    }
    
    @Override
    public void playOrLaunchNatively(File file) {
        play(file);
    }
    
    private void play(File file) {
        NativeLaunchUtils.safeLaunchFile(file, categoryManager);
    }
}
