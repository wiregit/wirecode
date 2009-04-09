package org.limewire.ui.swing.friends.chat;

import static org.limewire.ui.swing.util.I18n.tr;

import org.limewire.core.api.friend.FriendPresence;

/**
 * This is the offer message that is displayed in a person's chat screen when someone challenges you 
 * to a game of tic tac toe.
 * @author dsullivan
 *
 */
public class MessageTicTacToeOfferImpl extends AbstractMessageImpl implements MessageTicTacToeOffer {

    private static final String LATER_TEXT = tr("Play now, or " +
            "{0}reject{1}.","<a href=\"" + TicTacToeMessages.REJECT_GAME + "\">", "</a>");

    private FriendPresence sourcePresence;


    public MessageTicTacToeOfferImpl(String senderName, String friendId, Type type, FriendPresence sourcePresence) {
        super(senderName, friendId, type);
        this.sourcePresence = sourcePresence;
    }


    @Override
    public FriendPresence getPresence() {
        return sourcePresence;
    }

    @Override
    public String format() {
        boolean isIncoming = (getType() == Message.Type.Received);
        return isIncoming ? formatIncoming() : formatOutgoing();
    }

    private String formatOutgoing() {
        String tictactoeOfferSent = tr("Challenging {0} to play TicTacToe with you ...", getFriendID());
        return tictactoeOfferSent + formatButtonText("TicTacToe challenge", false);
    }

    private String formatButtonText(String buttonText, boolean buttonEnabled) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("<br/>")
            .append("<form action=\"\"><input type=\"hidden\" name=\"fileid\" value=\"")
            .append("Play TicTacToe?")
            .append("\"/><input type=\"submit\" value=\"")
            .append(buttonText)
            .append(buttonEnabled ? "\"/>" : ":disabled\"/>")
            .append("</form><br/>");
        return bldr.toString();
    }

    private String formatIncoming() {

        String tictactoeOfferReceived = tr("{0} wants to play Tic Tac Toe with you.", getFriendID());

        String defaultMessage = tictactoeOfferReceived + formatButtonText(tr("Accept the challenge"), true)
        + LATER_TEXT;
        
        return defaultMessage;
    }
    
    @Override
    public String toString() {
        return "MessageTicTacToeOfferImpl sender: " + getSenderName() + " friendID: " + getFriendID();
    }

    
}
