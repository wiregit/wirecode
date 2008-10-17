package com.limegroup.gnutella.filters;

import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Singleton;
import com.limegroup.gnutella.Response;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.messages.BadPacketException;
import com.limegroup.gnutella.messages.Message;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

@Singleton
public class URNFilter implements SpamFilter {
    
    private static final Log LOG = LogFactory.getLog(URNFilter.class);
    private static HashSet<URN> blacklist = new HashSet<URN>();
    // TODO: make this into a SIMPP setting
    private static String[] badURNs = new String[]{
        "urn:sha1:2GLEYBJPC6ZLVSI7IDJT5CJPGO7CKNOM",
        "urn:sha1:6AV6I5L77MQ3TTUAG5WWXE3VE7R6KMP4",
        "urn:sha1:6FPIGULR3OMOMOESYFELHS2YVV2M7ZGF",
        "urn:sha1:BAYURWNMQAF5DGVPMRQRLRIZ77R3AGDJ",
        "urn:sha1:BSR2F3A2K7PLDVKL4FM47BU2GYJEORFR",
        "urn:sha1:EOU33ELGG6Y3KN7YK3TKANKLIAX36SOY",
        "urn:sha1:F75XVBSE35WBGJT6TDRVEJGDK5LGZIQK",
        "urn:sha1:FL5SVC5KPWLUE45SOOGSOTAKQILZISFS",
        "urn:sha1:FZMBTBIMI7BMTNSDHYAQTVL4NR2AZXRS",
        "urn:sha1:GOZG2MFW5XWQHWRIFS62FWVSPDD7YUFK",
        "urn:sha1:JYMLNADQONAK3MUH7EQM57ADRE6CPRZA",
        "urn:sha1:NQBTBUTQSRE6HXSJLGIUQLPCR25WDNLD",
        "urn:sha1:OIFR7TOZMF7ZTJGUWIZCIIAHQE4AUG2K",
        "urn:sha1:OIN574PBP34ITVQYTAGUMPFUOPZOQAIQ",
        "urn:sha1:PZB2T2DCTUUP6WGLW5RHBVVFZDLGJHAE",
        "urn:sha1:Q5ABFLH6FCY37JKFOP5E3AXBTYLIOKDN",
        "urn:sha1:TAQZVN4CEHJREAKKMNO6NN5SLAP5VSW5",
        "urn:sha1:UO3FR5RVVAWDCOV7B5WEUAANTDLM65CT",
        "urn:sha1:UQFQCREXD6QAC754YXKEBQG7FS76NIN5",
        "urn:sha1:VC5RP6DJ4OX6TQWHI7UJTFXBAXUDPQ67",
        "urn:sha1:VHKHYERJO5QE7QER74MUKQM5NM4X7L2K",
        "urn:sha1:WOE2VY7W7U52CRS3756E7FTNS7S4ZR7M",
        "urn:sha1:Y64ZHRNFAYZEOZ2BMGCWY7DV2ZL2HWY4"
    };
    
    public URNFilter() {
        try {
            for(String s : badURNs)
                blacklist.add(URN.createSHA1Urn(s));
        } catch (Exception x) {
            if(LOG.isDebugEnabled())
                LOG.debug("Error creating URN blacklist: " + x);
        }
    }
    
    @Override
    public boolean allow(Message m) {
        if(m instanceof QueryReply) {
            QueryReply q = (QueryReply)m;
            try {
                for(Response r : q.getResultsArray()) {
                    for(URN u : r.getUrns()) {
                        if(blacklist.contains(u)) {
                            if(LOG.isDebugEnabled())
                                LOG.debug("Filtering response with URN " + u);
                            return false;
                        }
                    }
                }
                return true;
            } catch (BadPacketException bpe) {
                return true;
            }
        } else if(m instanceof QueryRequest) {
            QueryRequest q = (QueryRequest)m;
            for(URN u : q.getQueryUrns()) {
                if(blacklist.contains(u)) {
                    if(LOG.isDebugEnabled())
                        LOG.debug("Filtering request with URN " + u);
                    return false;
                }
            }
            return true;
        }
        return true; // Don't block other kinds of messages
    }
}