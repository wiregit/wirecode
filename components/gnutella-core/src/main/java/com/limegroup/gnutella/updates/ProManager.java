package com.limegroup.gnutella.updates;

import com.limegroup.gnutella.util.*;
import com.limegroup.gnutella.*;
import com.sun.net.ssl.SSLContext;
import com.sun.net.ssl.TrustManagerFactory;
import com.sun.net.ssl.TrustManager;
import javax.net.ssl.*;
import javax.net.*;
import java.net.*;
import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.*;

/**
 * Manages, buying of PRO from within LimeWire. Also, tells the rest of the code
 * if the instance of LimeWire has been upgraded to pro on this machine
 * @author Sumeet Thadani
 */
public class ProManager {
    
    private String macAddress = null;

    /**
     * @return true if we can find the MAC address of this machine - for
     * purposes of unique identifier, and we have a high enough version of java
     * to send server credit card info with SSL.
     */
    public boolean canBuyPro() {
        MacAddressFinder maf = new MacAddressFinder();
        this.macAddress = maf.getMacAddress();
        return(macAddress!=null && CommonUtils.isJava13OrLater());
    }
    
    public boolean buyPro(String ccNumber, String expDate)  {
        try {
        Assert.that(canBuyPro());
		Security.addProvider(
			new com.sun.net.ssl.internal.ssl.Provider());
        CertificateFactory certFact = CertificateFactory.getInstance("X.509");
        FileInputStream fis = new FileInputStream("core/lib/limecert.cert");
        Certificate cert = certFact.generateCertificate(fis);
        KeyStore store = KeyStore.getInstance("JKS");
        store.load(null,null);
        store.setCertificateEntry("lime",cert);        
        TrustManagerFactory tf = TrustManagerFactory.getInstance("SunX509","SunJSSE");
        tf.init(store);
        SSLContext context = SSLContext.getInstance("SSLv3");
        com.sun.net.ssl.TrustManager[] mans = 
                          (com.sun.net.ssl.TrustManager[])tf.getTrustManagers();
        context.init(null,mans,null);
        SSLSocketFactory factory = context.getSocketFactory();
        SSLSocket socket = (SSLSocket)factory.createSocket(
                             InetAddress.getByName("127.0.0.1"),7000);
        //Force the socket to do the SSL handshake now.
        socket.startHandshake();
        OutputStream os = socket.getOutputStream();
        while(true) {
            String a = "Sumeet\r\n";
            System.out.println("\t\t\t"+a);
            os.write(a.getBytes());
            os.flush();
            Thread.sleep(10000);
        }
        //Limewires will ship with the certificate that we use at the client
        //end to authenticate the server. This is however not fool proof - what
        //happens if someone manages to replace that certificate?
        //We will use the other key pair that ships with limewire to have
        //the server sign something unique from the client. 
        //Lets send out client GUID, and have the server sign it.
        
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        //1. open a secure socket to LimeWire server.
        //2. Send the Credit card info as well as macAddress.
        //3. Server will send back a signed file, save it.
        //4. close the socket.
        return true;
    }
    
    public boolean isPro() {
        MacAddressFinder maf = new MacAddressFinder();
        this.macAddress = maf.getMacAddress();
        if(macAddress==null)
            return false;
        //1. read the file -- no file - we are not pro
        //2. get expiry date from the files plain text
        //3. conbine expiry date with macAddress 
        //4.  verify that the signature in the file.
        //5. If not verified return false
        //6. If verified  and  currTimeMillis() < expiry time return true
        //7. Else return false
        return false;
    }

    public static void main(String[] args) {
        try {
            ProManager man = new ProManager();
            man.buyPro("Sumeet", "Ashish");
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Sumeet: test failed");
        }
    }
}
