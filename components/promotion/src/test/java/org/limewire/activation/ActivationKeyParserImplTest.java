package org.limewire.activation;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import junit.framework.Test;

import org.limewire.activation.exception.ActivationException;
import org.limewire.collection.Tuple;
import org.limewire.security.certificate.CipherProviderImpl;
import org.limewire.util.BaseTestCase;

public class ActivationKeyParserImplTest extends BaseTestCase {
    public ActivationKeyParserImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(ActivationKeyParserImplTest.class);
    }

    public void testParseBodyAndSignature() throws ActivationException {
        String toParse = ActivationConstants.ACTIVATION_KEY_BEGIN + "\nbody\n"
                + ActivationConstants.ACTIVATION_SIGN_BEGIN + "\nsignature\n"
                + ActivationConstants.ACTIVATION_SIGN_END;
        Tuple<String, String> parsed = new ActivationKeyParserImpl(null, null)
                .parseBodyAndSignature(toParse);
        assertEquals("body\n", parsed.getFirst());
        assertEquals("signature\n", parsed.getSecond());
    }

    public void testParseBodyAndSignatureWithExtraneousInfo() throws ActivationException {
        String toParse = "useless\nuseless\ninfo\n" + ActivationConstants.ACTIVATION_KEY_BEGIN
                + "\nbody\n" + ActivationConstants.ACTIVATION_SIGN_BEGIN + "\nsignature\n"
                + ActivationConstants.ACTIVATION_SIGN_END + "\nuseless!";
        Tuple<String, String> parsed = new ActivationKeyParserImpl(null, null)
                .parseBodyAndSignature(toParse);
        assertEquals("body\n", parsed.getFirst());
        assertEquals("signature\n", parsed.getSecond());
    }

    public void testParseBodyAndGGEP() throws ActivationException {
        String toParse = "foo=bar\nbar=jar\n\nI'm a signature,\nyes I am.";
        Tuple<String, String> parsed = new ActivationKeyParserImpl(null, null)
                .parseBodyAndGGEP(toParse);
        assertEquals("foo=bar\nbar=jar\n", parsed.getFirst());
        assertEquals("I'm a signature,yes I am.", parsed.getSecond());
    }

    public void testGenerate() throws NoSuchAlgorithmException, ActivationException {
        ActivationKeyParserImpl akp = new ActivationKeyParserImpl(new CipherProviderImpl(), null);
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        KeyPair keyPair = generator.generateKeyPair();
        ActivationKey activationKey = new ActivationKey();
        activationKey.setValidFrom(new Date());

        String activation = akp.generate("person=Beano Smith", activationKey, keyPair.getPrivate());
        System.out.println(activation);
        // We're encoded... See if it parses...
        ///akp.parse(activation);
        
    }
}
