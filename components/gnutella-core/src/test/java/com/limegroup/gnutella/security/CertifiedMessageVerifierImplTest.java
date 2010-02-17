package com.limegroup.gnutella.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;

import com.limegroup.gnutella.security.CertifiedMessageVerifier.CertifiedMessage;

public class CertifiedMessageVerifierImplTest extends BaseTestCase {
    
    public static Test suite() {
        return buildTestSuite(CertifiedMessageVerifierImplTest.class);
    }

    private Mockery context;
    private CertificateProvider certificateProvider;
    private CertifiedMessageVerifierImpl certifiedMessageVerifierImpl;
    private CertifiedMessage message;
    private Certificate certificate;
    private PublicKey publicKey;
    private byte[] signedPayload;
    private byte[] signature;
    private CertificateVerifier certificateVerifier;
    
    private static KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("DSA");
        gen.initialize(1024, new SecureRandom());
        return gen.generateKeyPair();
    }
    
    private static byte[] sign(byte[] data, PrivateKey privateKey) throws Exception {
        Signature signer = Signature.getInstance("DSA");
        signer.initSign(privateKey);
        signer.update(data);
        return signer.sign();        
    }
    
    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        certificateProvider = context.mock(CertificateProvider.class);
        certificateVerifier = context.mock(CertificateVerifier.class);
        certifiedMessageVerifierImpl = new CertifiedMessageVerifierImpl(certificateProvider, certificateVerifier);
        message = context.mock(CertifiedMessage.class);
        certificate = context.mock(Certificate.class);
        
        KeyPair keyPair = generateKeyPair();
        publicKey = keyPair.getPublic();
        signedPayload = StringUtils.toUTF8Bytes("hello cert");
        signature = sign(signedPayload, keyPair.getPrivate());
    }
    
    public void testMessageWithNewerCertificateVerifies() throws Exception {
        context.checking(new Expectations() {{
            one(message).getCertificate();
            will(returnValue(certificate));
            
            allowing(message).getKeyVersion();
            will(returnValue(1));
            
            allowing(certificate).getKeyVersion();
            will(returnValue(1));
            
            one(certificateVerifier).verify(certificate);
            will(returnValue(certificate));
        }});
        
        context.checking(new ValidMessageExpectations());
        
        certifiedMessageVerifierImpl.verify(message, null);
                
        context.assertIsSatisfied();
    }
    
    public void testMessageWithLesserKeyVersionThanCertificateFails() throws Exception {
        context.checking(new Expectations() {{
            one(message).getCertificate();
            will(returnValue(null));
            
            one(certificateProvider).get();
            will(returnValue(certificate));
            
            allowing(message).getKeyVersion();
            will(returnValue(0));
            
            allowing(certificate).getKeyVersion();
            will(returnValue(1));
        }});
        
        try {
            certifiedMessageVerifierImpl.verify(message, null);
            fail("exception expected");
        } catch (SignatureException se) {
        }
        
        context.assertIsSatisfied();
    }
    
    public void testMessageWithGreaterKeyVersionTriggersHttpFetch() throws Exception {
        context.checking(new Expectations() {{
            one(message).getCertificate();
            will(returnValue(null));
            
            one(certificateProvider).get();
            will(returnValue(certificate));
            
            allowing(message).getKeyVersion();
            will(returnValue(2));
            
            allowing(certificate).getKeyVersion();
            will(returnValue(1));
            
            // causes http fetch, but returns same certificate
            one(certificateProvider).getFromHttp(null);
            will(returnValue(certificate));
        }});
        
        try {
            certifiedMessageVerifierImpl.verify(message, null);
            fail("exception expected");
        } catch (SignatureException se) {
        }
        
        context.assertIsSatisfied();
    }
    
    public void testMessageWithInvalidPayloadFails() throws Exception {
        context.checking(new Expectations() {{
            one(message).getCertificate();
            will(returnValue(null));
            
            one(certificateProvider).get();
            will(returnValue(certificate));
            
            allowing(message).getKeyVersion();
            will(returnValue(1));
            
            allowing(certificate).getKeyVersion();
            will(returnValue(1));
        }});
        
        context.checking(new InvalidSignedPayloadExpectations());
        
        try {
            certifiedMessageVerifierImpl.verify(message, null);
            fail("exception expected");
        } catch (SignatureException se) {
        }
        
        context.assertIsSatisfied();
    }
    
    public void testMessageWithInvalidSignatureFails() throws Exception {
        context.checking(new Expectations() {{
            one(message).getCertificate();
            will(returnValue(null));
            
            one(certificateProvider).get();
            will(returnValue(certificate));
            
            allowing(message).getKeyVersion();
            will(returnValue(1));
            
            allowing(certificate).getKeyVersion();
            will(returnValue(1));
        }});
        
        context.checking(new InvalidSignatureExpectations());
        
        try {
            certifiedMessageVerifierImpl.verify(message, null);
            fail("exception expected");
        } catch (SignatureException se) {
        }
        
        context.assertIsSatisfied();
    }
    
    private class ValidMessageExpectations extends Expectations {{
        allowing(message).getSignature();
        will(returnValue(signature));
        
        allowing(message).getSignedPayload();
        will(returnValue(signedPayload));
        
        allowing(certificate).getPublicKey();
        will(returnValue(publicKey));
    }};
    
    private class InvalidSignatureExpectations extends Expectations {{
        allowing(message).getSignature();
        will(returnValue(new byte[] { 0, 1, 2, 3 }));
        
        allowing(message).getSignedPayload();
        will(returnValue(signedPayload));
        
        allowing(certificate).getPublicKey();
        will(returnValue(publicKey));
    }};
    
    private class InvalidSignedPayloadExpectations extends Expectations {{
        allowing(message).getSignature();
        will(returnValue(signature));
        
        allowing(message).getSignedPayload();
        will(returnValue(new byte[] { 0, 1, 2, 3 }));
        
        allowing(certificate).getPublicKey();
        will(returnValue(publicKey));
    }};
}
