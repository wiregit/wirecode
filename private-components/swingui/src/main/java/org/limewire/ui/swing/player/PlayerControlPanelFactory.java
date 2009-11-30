package org.limewire.ui.swing.player;

import org.limewire.core.api.file.CategoryManager;
import org.limewire.ui.swing.components.decorators.SliderBarDecorator;
import org.limewire.ui.swing.library.LibraryMediator;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class PlayerControlPanelFactory {
    private final Provider<PlayerMediator> videoMediator;
    private final Provider<PlayerMediator> audioMediator;
    private final SliderBarDecorator sliderBarDecorator;
    private final LibraryMediator libraryMediator;
    private final CategoryManager categoryManager;
    
    @Inject
    public PlayerControlPanelFactory(@Video Provider<PlayerMediator> videoMediator,
                                     @Audio Provider<PlayerMediator> audioMediator,
                                     LibraryMediator libraryMediator,
                                     CategoryManager categoryManager,
                                     SliderBarDecorator sliderBarDecorator) {
        this.videoMediator = videoMediator;
        this.audioMediator = audioMediator;
        this.sliderBarDecorator = sliderBarDecorator;
        this.libraryMediator = libraryMediator;
        this.categoryManager = categoryManager;
    }

    public PlayerControlPanel createAudioControlPanel() {
        return createControlPanel(audioMediator);
    }

    public PlayerControlPanel createVideoControlPanel() {
        return createControlPanel(videoMediator);
    }
    
    private PlayerControlPanel createControlPanel(Provider<PlayerMediator> mediator){
        return new PlayerControlPanel(mediator, libraryMediator, categoryManager, sliderBarDecorator);
    }
}
