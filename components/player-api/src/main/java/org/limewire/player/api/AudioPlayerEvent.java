package org.limewire.player.api;


/**
 * This event is fired by the AudioPlayer everytime the state of the player changes.
 * The changed state along with the updated value is passed along when applicable
 */
public interface AudioPlayerEvent {
    

    public PlayerState getState();

    public double getValue();
}
