package org.limewire.store.server;

import java.io.IOException;

public class HttpCoreDemo {

    public static void main(String[] args) {
        new HttpCoreDemo().realMain(args);
    }

    private void realMain(String[] args) {
        
        final LocalHttpCoreServer local = new LocalHttpCoreServer();
        final RemoteHttpCoreServer remote = new RemoteHttpCoreServer();
        
        note("local: " + local);
        note("remote: " + remote);
        
        local.start();
        remote.start();
    }

    private void note(String s) {
        System.out.println(s);
    }

}
