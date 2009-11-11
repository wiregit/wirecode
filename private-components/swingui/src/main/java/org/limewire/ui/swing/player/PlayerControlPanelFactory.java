package org.limewire.ui.swing.player;

import org.limewire.ui.swing.components.decorators.SliderBarDecorator;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class PlayerControlPanelFactory {
    private final Provider<PlayerMediator> videoMediator;
    private final Provider<PlayerMediator> audioMediator;
    private final SliderBarDecorator sliderBarDecorator;

    @Inject
    public PlayerControlPanelFactory(@Video Provider<PlayerMediator> videoMediator,
            @Audio Provider<PlayerMediator> audioMediator, SliderBarDecorator sliderBarDecorator) {
        this.videoMediator = videoMediator;
        this.audioMediator = audioMediator;
        this.sliderBarDecorator = sliderBarDecorator;
    }

    public PlayerControlPanel createAudioControlPanel() {
        return createControlPanel(audioMediator);
    }

    public PlayerControlPanel createVideoControlPanel() {
        return createControlPanel(videoMediator);
    }
    
    private PlayerControlPanel createControlPanel(Provider<PlayerMediator> mediator){
        return new PlayerControlPanel(mediator, sliderBarDecorator);
    }
}
