package com.limegroup.gnutella.security;

import java.io.File;
import java.io.FileOutputStream;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

public class FileCertificateReaderImplTest extends LimeTestCase {

    public static Test suite() {
        return buildTestSuite(FileCertificateReaderImplTest.class);
    }

    private Mockery context;
    private CertificateParser certificateParser;
    private FileCertificateReaderImpl fileCertificateReaderImpl;
    private final String certificateString = "GAWQEFIARSGMFXPFJZLZZMNQCYIT5VG66O7NWP6VAIKFJ6SZ2E2AJRHTD63UXONQT36SA5X7F7OA|1|GCBADNZQQIASYBQHFKDERTRYAQATBAQBD4BIDAIA7V7VHAI5OUJCSUW7JKOC53HE473BDN2SHTXUIAGDDY7YBNSREZUUKXKAEJI7WWJ5RVMPVP6F6W5DB5WLTNKWZV4BHOAB2NDP6JTGBN3LTFIKLJE7T7UAI6YQELBE7O5J277LPRQ37A5VPZ6GVCTBKDYE7OB7NU6FD3BQENKUCNNBNEJS6Z27HLRLMHLSV37SEIBRTHORJAA4OAQVACLWAUEPCURQXTFSSK4YFIXLQQF7AWA46UBIDAIA67Q2BBOWTM655S54VNODNOCXXF4ZJL537I5OVAXZK5GAWPIHQJTVCWKXR25NIWKP4ZYQOEEBQC2ESFTREPUEYKAWCO346CJSRTEKNYJ4CZ5IWVD4RUUOBI5ODYV3HJTVSFXKG7YL7IQTKYXR7NRHUAJEHPGKJ4N6VBIZBCNIQPP6CWXFT4DJFC3GL2AHWVJFMQAUYO76Z5ESUA4BQQAAFAMADGSCIYVEIUKQNN6A4D7B77A62EMIURPPHQE2ILP3GJLRTUSRSK2Q4IALFHHVMRA2HOERD25DUJXGX2IQDEAFWWLVG7FOHRG2Z3H4ER66UV2UVI7X5VG3UWGJ72ZW3V5V55ELJVJ52EAFCZW4WKUP2EAIUBDQYZ63LG5PFXYY5N2RHKJOU6UUAJZUV3VTGZNFDIU6G3BMMIIDG";
    private final byte[] certificateBytes = StringUtils.toUTF8Bytes(certificateString);
    private final File file = new File(CommonUtils.getUserSettingsDir(), "simpp.cert");

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        certificateParser = context.mock(CertificateParser.class);
        fileCertificateReaderImpl = new FileCertificateReaderImpl(certificateParser);
    }
    
    @Override
    protected void tearDown() throws Exception {
        file.delete();
    }
    
    public void testRead() throws Exception {
        FileOutputStream out = new FileOutputStream(file);
        
        out.write(certificateBytes);
        out.close();
        assertEquals(certificateBytes.length, file.length());
        
        context.checking(new Expectations() {{
            one(certificateParser).parseCertificate(certificateString);
            will(returnValue(null));
        }});
        
        fileCertificateReaderImpl.read(file);
        
        context.assertIsSatisfied();
    }
    
    public void testWrite() throws Exception {
        final Certificate certificate = context.mock(Certificate.class);
        context.checking(new Expectations() {{
            one(certificate).getCertificateString();
            will(returnValue(certificateString));
        }});
        
        assertTrue(fileCertificateReaderImpl.write(certificate, file));
        assertEquals(certificateBytes.length, file.length());
        assertEquals(certificateBytes, FileUtils.readFileFully(file));
    }
}
