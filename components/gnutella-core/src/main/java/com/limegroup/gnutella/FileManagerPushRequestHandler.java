package com.limegroup.gnutella;

import java.io.IOException;

/**
 * An implementation of PushRequestHandler that looks for the requested file
 * in the FileManager and launches an HTTPUploader to transfer it.
 *
 * @author Ron Vogl
 */
public class FileManagerPushRequestHandler
    implements PushRequestHandler
{
    private static FileManagerPushRequestHandler _instance;

    private FileManagerPushRequestHandler() {}

    public static FileManagerPushRequestHandler instance()
    {
        if(_instance == null)
            _instance = new FileManagerPushRequestHandler();
        return _instance;
    }

    public void handlePushRequest(PushRequest pushRequest,
                                  MessageRouter router,
                                  ActivityCallback callback)
    {
        // Ignore excess upload requests
        if (callback.getNumUploads() >=
                SettingsManager.instance().getMaxUploads())
            return ;

        // Unpack the message
        String host = new String(pushRequest.getIP());
        byte[] ip = pushRequest.getIP();
        StringBuffer buf = new StringBuffer();
        buf.append(ByteOrder.ubyte2int(ip[0])+".");
        buf.append(ByteOrder.ubyte2int(ip[1])+".");
        buf.append(ByteOrder.ubyte2int(ip[2])+".");
        buf.append(ByteOrder.ubyte2int(ip[3])+"");
        String h = buf.toString();
        int port = pushRequest.getPort();
        int index = (int)pushRequest.getIndex();
        String req_guid_hexstring =
            (new GUID(pushRequest.getClientGUID())).toString();

        FileDesc desc;
        try {
            desc = (FileDesc)FileManager.getFileManager()._files.get(index);
        }
        catch (Exception e) {
            //I'm catching Exception because I don't know
            //exactly which IndexOutOfBoundsException is
            //thrown: from normal util package or com.sun...

            //TODO?: You could connect and send 404 file
            //not found....but why bother?
            return;
        }
        String file = desc._name;

        HTTPUploader up = new HTTPUploader(h, port, index, req_guid_hexstring,
                                           callback);
        Thread t=new Thread(up);
        t.setDaemon(true);
        t.start();
    }
}
