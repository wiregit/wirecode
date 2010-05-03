package org.limewire.http.httpclient.robots;

import java.io.IOException;

import org.limewire.util.BaseTestCase;

/**
 * Based on test case from Heritrix the web crawler from 
 * http://crawler.archive.org/
 */
public class RobotsTxtTest extends BaseTestCase {
    
    public void testAllowAllOnEmptyString() throws Exception {
        RobotsTxt robotsTxt = new RobotsTxt("");
        assertTrue(robotsTxt.getDirectivesFor("whatever").allows("/"));
    }
    
    public void testParseRobots() throws Exception { 
        // Parse archive robots.txt with heritrix agent.
        String agent = "archive.org_bot";
        String text = "User-agent: " + agent + "\n" +
            "Disallow: /cgi-bin/\n" +
            "Disallow: /details/software\n";
        RobotsTxt robotsTxt = new RobotsTxt(text);
        assertEquals(1, robotsTxt.getUserAgents().size());
        assertNotNull(robotsTxt.getDirectivesFor("archive.org_bot"));
        assertContains(robotsTxt.getUserAgents(), agent);

        // Parse archive robots.txt with star agent.
        agent = "*";
        text = "User-agent: " + agent + "\n" +
            "Disallow: /cgi-bin/\n" +
            "Disallow: /details/software\n";
        robotsTxt = new RobotsTxt(text);
        assertEquals(1, robotsTxt.getUserAgents().size());
        assertNotNull(robotsTxt.getDirectivesFor("foo bar"));
        assertContains(robotsTxt.getUserAgents(), "");
    }
    
    public void testDirectives() throws Exception {
        RobotsTxt r = new RobotsTxt("User-agent: *\n" +
                "Disallow: /cgi-bin/\n" +
                "Disallow: /details/software\n" +
                "\n"+
                "User-agent: denybot\n" +
                "Disallow: /\n" +
                "\n"+
                "User-agent: allowbot1\n" +
                "Disallow: \n" +
                "\n"+
                "User-agent: allowbot2\n" +
                "Disallow: /foo\n" +
                "Allow: /\n"+
                "\n"+
                "User-agent: delaybot\n" +
                "Disallow: /\n" +
                "Crawl-Delay: 20\n"+
                "Allow: /images/\n");
        // bot allowed with empty disallows
        assertTrue(r.getDirectivesFor("Mozilla allowbot1 99.9").allows("/path"));
        assertTrue(r.getDirectivesFor("Mozilla allowbot1 99.9").allows("/"));
        // bot allowed with explicit allow
        assertTrue(r.getDirectivesFor("Mozilla allowbot2 99.9").allows("/path"));
        assertTrue(r.getDirectivesFor("Mozilla allowbot2 99.9").allows("/"));
        assertTrue(r.getDirectivesFor("Mozilla allowbot2 99.9").allows("/foo"));
        // bot denied with blanket deny
        assertFalse(r.getDirectivesFor("Mozilla denybot 99.9").allows("/path"));
        assertFalse(r.getDirectivesFor("Mozilla denybot 99.9").allows("/"));
        // unnamed bot with mixed catchall allow/deny
        assertTrue(r.getDirectivesFor("Mozilla anonbot 99.9").allows("/path"));
        assertFalse(r.getDirectivesFor("Mozilla anonbot 99.9").allows("/cgi-bin/foo.pl"));
        // no crawl-delay
        assertEquals(-1f,r.getDirectivesFor("Mozilla denybot 99.9").getCrawlDelay());
        // with crawl-delay 
        assertEquals(20f,r.getDirectivesFor("Mozilla delaybot 99.9").getCrawlDelay());
    }
    


    
    /**
     * Test handling of a robots.txt with extraneous HTML markup
     * @throws IOException
     */
    public void testHtmlMarkupRobots() throws Exception {
        RobotsTxt r = new RobotsTxt(                "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 3.2 Final//EN\"><HTML>\n"
                +"<HEAD>\n"
                +"<TITLE>/robots.txt</TITLE>\n"
                +"<HEAD>\n"
                +"<BODY>\n"
                +"User-agent: *<BR>\n"
                +"Disallow: /<BR>\n"
                +"Crawl-Delay: 30<BR>\n"
                +"\n"
                +"</BODY>\n"
                +"</HTML>\n");
        assertFalse(r.getDirectivesFor("anybot").allows("/index.html"));
        assertEquals(30f,r.getDirectivesFor("anybot").getCrawlDelay());
    }
}
