package com.limegroup.gnutella.plugins;

import java.io.File;

/** This interface is the least common denominator for players of various media
    files. It simply provides a start and stop method.  Nothing else regarding
    the semantics of the player is specified.
*/
public interface MinimalPlayerInterface {

    /** Start the player with the given input file.
     */
    public void play(File toStart);

    /** Stop the player from playing.
     */
    public void stop();

    /** @return a string array of supported file types, ie "mp3" .
     */
    public String[] getSupportedFileExtensions();

}
