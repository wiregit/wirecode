package org.limewire.ui.swing.friends.settings;

/**
 * Configuration necessary to create a facebook <code>FriendConnection</code>
 * via <code>FriendConnectionFactory.login(FriendConnectionConfiguration configuration)</code>
 */
public interface FacebookFriendAccountConfiguration extends FriendAccountConfiguration {
    /**
     * Loads the cookies from the browser where
     * the user logged in to facebook.
     */
    void loadCookies();

    /**
     * Clears the cookies from the browser so that
     * facebook sessions are NOT maintained across LW sessions
     */
    void clearCookies();

    /**
     * @param autoLogin whether this connection should be automatically
     * created on LW startup
     */
    void setAutoLogin(boolean autoLogin);

    /**
     * @return whether this connection should be automatically
     * created on LW startup
     */
    boolean isAutologin();
}
