package org.jivesoftware.smackx.jingle.states;

import org.jivesoftware.smackx.packet.Jingle;

public class PendingState implements State{

//    private final ActiveState activeState;
//    private final EndedState endedState;

    public PendingState(ActiveState activeState, EndedState endedState){
//        this.activeState = activeState;
//        this.endedState = endedState;
    }

    public State dispatch(Jingle jin) {
//        Jingle jout;
        State nextState = this;
        if(Jingle.Action.CONTENTACCEPT.equals(jin.getAction())) {
            handleContentAccept(jin);
        } /*else if(Jingle.Action.CONTENTMODIFY.equals(jin.getAction())) {
            jout = handleContentModify(jin);
        } else if(Jingle.Action.CONTENTREMOVE.equals(jin.getAction())) {
            jout = handleContentRemove(jin);
        } else if(Jingle.Action.SESSIONINFO.equals(jin.getAction())) {
            jout = handleSessionInfo(jin);
        } else if(Jingle.Action.TRANSPORTINFO.equals(jin.getAction())) {
            jout = handleTransportInfo(jin);
        } else if(Jingle.Action.SESSIONACCEPT.equals(jin.getAction())) {
            jout = handleSessionAccept(jin);
            nextState = activeState;
        } else if(Jingle.Action.SESSIONTERMINATE.equals(jin.getAction())) {
            jout = handleSessionTerminate(jin);
            nextState = endedState;
        }   */
        return nextState;
    }

    private Jingle handleContentAccept(Jingle jin) {
        return null;  //To change body of created methods use File | Settings | File Templates.
    }
}
