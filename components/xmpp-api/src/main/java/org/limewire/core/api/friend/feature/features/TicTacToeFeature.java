package org.limewire.core.api.friend.feature.features;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.friend.feature.Feature;

/**
 * Defines Tic Tac Toe support.
 */
public class TicTacToeFeature extends Feature<TicTacToeSupport> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/tic-tac-toe/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public TicTacToeFeature(TicTacToeSupport feature) {
        super(feature, ID);
    }
}
