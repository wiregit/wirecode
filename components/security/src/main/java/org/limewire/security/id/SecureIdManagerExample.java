package org.limewire.security.id;

import java.util.Arrays;
import org.limewire.core.settings.SecuritySettings;
import org.limewire.io.GUID;
import org.limewire.security.id.SecureIdManager.Identity;
import org.limewire.util.Base32;

public class SecureIdManagerExample {

    public static void main(String[] args) throws Exception {        
        // alice
        SecureIdManager aliceIdManager = new SecureIdManager(new GUID(), "aliceRemoteKeys.txt");
        System.out.println("alice id "+aliceIdManager.getMyId());
        // bob
        SecuritySettings.SIGNATURE_PUBLIC_KEY.set("set it to this line to rise an exception, otherwise alice and bob will have same keys");
        SecureIdManager bobIdManager = new SecureIdManager(new GUID(), "bobRemoteKeys.txt");        
        System.out.println("bob id "+bobIdManager.getMyId());

        // alice generates a request which is basically an identity
        Identity request = aliceIdManager.getMyIdentity();
        
        // System.out.println(Base32.encode(request.getDHKey().getEncoded()));
        System.out.println("public key request {id, signature public key, dh public key, signature} size:\n raw data: "+
                request.getGuid().bytes().length +" "+
                request.getSignatureKey().getEncoded().length +" "+
                request.getDHPublicComponent().toByteArray().length +" "+
                request.getSignature().length);

        System.out.println(" base32 encoded: "+
                Base32.encode(request.getGuid().bytes()).length() +" "+
                Base32.encode(request.getSignatureKey().getEncoded()).length() +" "+
                Base32.encode(request.getDHPublicComponent().toByteArray()).length() +" "+
                Base32.encode(request.getSignature()).length());

        // bob process the request
        boolean goodRequest= bobIdManager.processIdentity(request);
        Identity reply = null;
        if(goodRequest){
            System.out.println("alice's request looks good, gonna reply.");
            reply = bobIdManager.getMyIdentity();
        }else{
            System.out.println("alice's request looks bad, gonna quit.");
            System.exit(-1);
        }
        // alice process the reply
        aliceIdManager.processIdentity(reply);
        System.out.println("Now, alice and bob know each other and share a key.\n");
        
        // example of authenticated message reply
        // alice generates a message with a nonce        
        String requestData = "who has a blowfish book?";
        System.out.println("alice asks "+requestData);
        byte[] challengeStoredLocally = aliceIdManager.nonce();
        String message = Base32.encode(aliceIdManager.getMyId().bytes()) +"|"+ requestData +"|"+ Base32.encode(challengeStoredLocally);
        //System.out.println(Base32.encode(challengeStoredLocally));
        
        // somehow bob receives the message and processes it
        int separater1 = message.indexOf("|");
        int separater2 = message.indexOf("|",separater1+1);
        GUID requesterGUID = new GUID(Base32.decode(message.substring(0, separater1)));        
        String alicesChallenge = message.substring(separater2+1);
        
        String replyData = "bob has a blowfish spec!";        
        String toSign = Base32.encode(bobIdManager.getMyId().bytes()) +"|"+ replyData +"|"+ alicesChallenge;
        // bob does both signature and mac for fun
        String sigStr = Base32.encode(bobIdManager.sign(toSign.getBytes()));
        String macStr = Base32.encode(bobIdManager.hamc(requesterGUID, toSign.getBytes()));
        String signedMessageReply = toSign +"|"+ sigStr; 
        String macedMessageReply = toSign +"|"+ macStr;
        
        // alice gets the replies and process them
        /* (I) process the signed reply */
        // 1) parse the reply
        separater1 = signedMessageReply.indexOf("|");
        separater2 = signedMessageReply.indexOf("|",separater1+1);
        int separater3 = signedMessageReply.indexOf("|",separater2+1);
        GUID replierGUID = new GUID(Base32.decode(signedMessageReply.substring(0, separater1)));
        byte[] sig = Base32.decode(signedMessageReply.substring(separater3));
        byte[] challengeFormMessage = Base32.decode(signedMessageReply.substring(separater2+1, separater3));
        byte[] toVerify = signedMessageReply.substring(0, separater3).getBytes();
        String replyStr = signedMessageReply.substring(separater1+1, separater2);
        // 2) verify the challenge 
        if( ! Arrays.equals(challengeStoredLocally, challengeFormMessage)){
            System.out.println("wrong challenge in reply");
            System.exit(-1);
        }
        // 3) verify the signature        
        if( ! aliceIdManager.verifySignature(replierGUID, toVerify, sig)){
            System.out.println("bad signature in reply");
            System.exit(-1);
        }
        // 4) use the reply
        System.out.println("alice got a signed reply: "+replyStr);

        /* (II) process the maced reply */
        // 1) parse the reply
        separater1 = macedMessageReply.indexOf("|");
        separater2 = macedMessageReply.indexOf("|",separater1+1);
        separater3 = macedMessageReply.indexOf("|",separater2+1);
        replierGUID = new GUID(Base32.decode(macedMessageReply.substring(0, separater1)));
        byte[] mac = Base32.decode(macedMessageReply.substring(separater3));
        challengeFormMessage = Base32.decode(macedMessageReply.substring(separater2+1, separater3));
        toVerify = macedMessageReply.substring(0, separater3).getBytes();
        replyStr = macedMessageReply.substring(separater1+1, separater2);
        // 2) verify the challenge 
        if( ! Arrays.equals(challengeStoredLocally, challengeFormMessage)){
            System.out.println("wrong challenge in reply");
            System.exit(-1);
        }
        // 3) verify the mac
        if( ! aliceIdManager.verifyHmac(replierGUID, toVerify, mac)){
            System.out.println("bad mac in reply");
            System.exit(-1);
        }
        // 4) use the reply
        System.out.println("alice got a maced reply: "+replyStr);
    }
}
