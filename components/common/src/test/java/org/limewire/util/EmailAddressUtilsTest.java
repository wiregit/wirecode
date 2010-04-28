package org.limewire.util;

import junit.framework.Test;

public class EmailAddressUtilsTest extends BaseTestCase {
    public static Test suite() {
        return buildTestSuite(EmailAddressUtilsTest.class);
    }

    public EmailAddressUtilsTest(final String name) {
        super(name);
    }

    public void testDeserializeByAddress() {
        unitTest("first.last@example.com", true);
        unitTest("first_last@example.com", true);
        unitTest("1234567890123456789012345678901234567890123456789012345678901234@example.com",
                true);
        unitTest("first.last@sub.do,com", false);
        unitTest("\"first\\\"last\"@example.com", true);
        unitTest("first\\@last@example.com", false);
        unitTest("\"first@last\"@example.com", true);
        unitTest("\"first\\\\last\"@example.com", true);
        unitTest(
                "x@x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x234",
                true);
        unitTest(
                "123456789012345678901234567890123456789012345678901234567890@12345678901234567890123456789012345678901234567890123456789.12345678901234567890123456789012345678901234567890123456789.123456789012345678901234567890123456789012345678901234567890123.example.com",
                true);
        unitTest("first.last@[12.34.56.78]", true);
        unitTest("first.last@[IPv6:::12.34.56.78]", true);
        unitTest("first.last@[IPv6:1111:2222:3333::4444:12.34.56.78]", true);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:6666:12.34.56.78]", true);
        unitTest("first.last@[IPv6:::1111:2222:3333:4444:5555:6666]", true);
        unitTest("first.last@[IPv6:1111:2222:3333::4444:5555:6666]", true);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:6666::]", true);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:6666:7777:8888]", true);
        unitTest(
                "first.last@x23456789012345678901234567890123456789012345678901234567890123.example.com",
                true);
        unitTest("first.last@1xample.com", true);
        unitTest("first.last@123.example.com", true);
        unitTest(
                "123456789012345678901234567890123456789012345678901234567890@12345678901234567890123456789012345678901234567890123456789.12345678901234567890123456789012345678901234567890123456789.12345678901234567890123456789012345678901234567890123456789.1234.example.com",
                false);
        unitTest("first.last", false);
        unitTest("12345678901234567890123456789012345678901234567890123456789012345@example.com",
                false);
        unitTest(".first.last@example.com", false);
        unitTest("first.last.@example.com", false);
        unitTest("first..last@example.com", false);
        unitTest("\"first\"last\"@example.com", false);
        unitTest("\"first\\last\"@example.com", true);
        unitTest("\"\"\"@example.com", false);
        unitTest("\"\\\"@example.com", false);
        unitTest("\"\"@example.com", false);
        unitTest("first\\\\@last@example.com", false);
        unitTest("first.last@", false);
        unitTest(
                "x@x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456789.x23456",
                false);
        unitTest("first.last@[.12.34.56.78]", false);
        unitTest("first.last@[12.34.56.789]", false);
        unitTest("first.last@[::12.34.56.78]", false);
        unitTest("first.last@[IPv5:::12.34.56.78]", false);
        unitTest("first.last@[IPv6:1111:2222:3333::4444:5555:12.34.56.78]", false);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:12.34.56.78]", false);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:6666:7777:12.34.56.78]", false);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:6666:7777]", false);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:6666:7777:8888:9999]", false);
        unitTest("first.last@[IPv6:1111:2222::3333::4444:5555:6666]", false);
        unitTest("first.last@[IPv6:1111:2222:3333::4444:5555:6666:7777]", false);
        unitTest("first.last@[IPv6:1111:2222:333x::4444:5555]", false);
        unitTest("first.last@[IPv6:1111:2222:33333::4444:5555]", false);
        unitTest("first.last@example.123", false);
        unitTest("first.last@com", false);
        unitTest("first.last@-xample.com", false);
        unitTest("first.last@exampl-.com", false);
        unitTest(
                "first.last@x234567890123456789012345678901234567890123456789012345678901234.example.com",
                false);
        unitTest("\"Abc\\@def\"@example.com", true);
        unitTest("\"Fred\\ Bloggs\"@example.com", true);
        unitTest("\"Joe.\\\\Blow\"@example.com", true);
        unitTest("\"Abc@def\"@example.com", true);
        unitTest("\"Fred Bloggs\"@example.com", true);
        unitTest("usermailbox@example.com", true);
        unitTest("customer/department=shipping@example.com", true);
        unitTest("$A12345@example.com", true);
        unitTest("!def!xyz%abc@example.com", true);
        unitTest("_somename@example.com", true);
        unitTest("dclo@us.ibm.com", true);
        unitTest("abc\\@def@example.com", false);
        unitTest("abc\\\\@example.com", false);
        unitTest("peter.piper@example.com", true);
        unitTest("Doug\\ \\\"Ace\\\"\\ Lovell@example.com", false);
        unitTest("\"Doug \\\"Ace\\\" L.\"@example.com", true);
        unitTest("abc@def@example.com", false);
        unitTest("abc\\\\@def@example.com", false);
        unitTest("abc\\@example.com", false);
        unitTest("@example.com", false);
        unitTest("doug@", false);
        unitTest("\"qu@example.com", false);
        unitTest("ote\"@example.com", false);
        unitTest(".dot@example.com", false);
        unitTest("dot.@example.com", false);
        unitTest("two..dot@example.com", false);
        unitTest("\"Doug \"Ace\" L.\"@example.com", false);
        unitTest("Doug\\ \\\"Ace\\\"\\ L\\.@example.com", false);
        unitTest("hello world@example.com", false);
        unitTest("gatsby@f.sc.ot.t.f.i.tzg.era.l.d.", false);
        unitTest("test@example.com", true);
        unitTest("TEST@example.com", true);
        unitTest("1234567890@example.com", true);
        unitTest("testtest@example.com", true);
        unitTest("test-test@example.com", true);
        unitTest("t*est@example.com", true);
        unitTest("+1~1+@example.com", true);
        unitTest("{_test_}@example.com", true);
        unitTest("\"[[ test ]]\"@example.com", true);
        unitTest("test.test@example.com", true);
        unitTest("\"test.test\"@example.com", true);
        unitTest("test.\"test\"@example.com", true);
        unitTest("\"test@test\"@example.com", true);
        unitTest("test@123.123.123.x123", true);
        unitTest("test@123.123.123.123", false);
        unitTest("test@[123.123.123.123]", true);
        unitTest("test@example.example.com", true);
        unitTest("test@example.example.example.com", true);
        unitTest("test.example.com", false);
        unitTest("test.@example.com", false);
        unitTest("test..test@example.com", false);
        unitTest(".test@example.com", false);
        unitTest("test@test@example.com", false);
        unitTest("test@@example.com", false);
        unitTest("-- test --@example.com", false);
        unitTest("[test]@example.com", false);
        unitTest("\"test\\test\"@example.com", true);
        unitTest("\"test\"test\"@example.com", false);
        unitTest("()[];:,><@example.com", false);
        unitTest("test@.", false);
        unitTest("test@example.", false);
        unitTest("test@.org", false);
        unitTest(
                "test@123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012.com",
                false);
        unitTest("test@example", false);
        unitTest("test@[123.123.123.123", false);
        unitTest("test@123.123.123.123]", false);
        unitTest("NotAnEmail", false);
        unitTest("@NotAnEmail", false);
        unitTest("\"test\\\\blah\"@example.com", true);
        unitTest("\"test\\blah\"@example.com", true);
        unitTest("\"test\\\rblah\"@example.com", true);
        unitTest("\"test\rblah\"@example.com", false);
        unitTest("\"test\\\"blah\"@example.com", true);
        unitTest("\"test\"blah\"@example.com", false);
        unitTest("customer/department@example.com", true);
        unitTest("_Yosemite.Sam@example.com", true);
        unitTest("~@example.com", true);
        unitTest(".wooly@example.com", false);
        unitTest("wo..oly@example.com", false);
        unitTest("pootietang.@example.com", false);
        unitTest(".@example.com", false);
        unitTest("\"Austin@Powers\"@example.com", true);
        unitTest("Ima.Fool@example.com", true);
        unitTest("\"Ima.Fool\"@example.com", true);
        unitTest("\"Ima Fool\"@example.com", true);
        unitTest("Ima Fool@example.com", false);
        unitTest("phil.h\\@\\@ck@haacked.com", false);
        unitTest("\"first\".\"last\"@example.com", true);
        unitTest("\"first\".middle.\"last\"@example.com", true);
        unitTest("\"first\\\\\"last\"@example.com", false);
        unitTest("\"first\".last@example.com", true);
        unitTest("first.\"last\"@example.com", true);
        unitTest("\"first\".\"middle\".\"last\"@example.com", true);
        unitTest("\"first.middle\".\"last\"@example.com", true);
        unitTest("\"first.middle.last\"@example.com", true);
        unitTest("\"first..last\"@example.com", true);
        unitTest("foo@[\\1.2.3.4]", false);
        unitTest("\"first\\\\\\\"last\"@example.com", true);
        unitTest("first.\"mid\\dle\".\"last\"@example.com", true);
        unitTest("Test.\r\n Folding.\r\n Whitespace@example.com", true);
        unitTest("first.\"\".last@example.com", false);
        unitTest("first\\last@example.com", false);
        unitTest("Abc\\@def@example.com", false);
        unitTest("Fred\\ Bloggs@example.com", false);
        unitTest("Joe.\\\\Blow@example.com", false);
        unitTest("first.last@[IPv6:1111:2222:3333:4444:5555:6666:12.34.567.89]", false);
        unitTest("\"test\\\r\n blah\"@example.com", false);
        unitTest("\"test\r\n blah\"@example.com", true);
        unitTest("{^c\\@**Dog^}@cartoon.com", false);
        unitTest("(foo)cal(bar)@(baz)iamcal.com(quux)", true);
        unitTest("cal@iamcal(woo).(yay)com", true);
        unitTest("\"foo\"(yay)@(hoopla)[1.2.3.4]", false);
        unitTest("cal(woo(yay)hoopla)@iamcal.com", true);
        unitTest("cal(foo\\@bar)@iamcal.com", true);
        unitTest("cal(foo\\)bar)@iamcal.com", true);
        unitTest("cal(foo(bar)@iamcal.com", false);
        unitTest("cal(foo)bar)@iamcal.com", false);
        unitTest("cal(foo\\)@iamcal.com", false);
        unitTest("first().last@example.com", true);
        unitTest("first.(\r\n middle\r\n )last@example.com", true);
        unitTest(
                "first(12345678901234567890123456789012345678901234567890)last@(1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890)example.com",
                false);
        unitTest("first(Welcome to\r\n the (\"wonderful\" (!)) world\r\n of email)@example.com",
                true);
        unitTest("pete(his account)@silly.test(his host)", true);
        unitTest("c@(Chris\'s host.)public.example", true);
        unitTest("jdoe@machine(comment). example", true);
        unitTest("1234 @ local(blah) .machine .example", true);
        unitTest("first(middle)last@example.com", false);
        unitTest("first(abc.def).last@example.com", true);
        unitTest("first(a\"bc.def).last@example.com", true);
        unitTest("first.(\")middle.last(\")@example.com", true);
        unitTest(
                "first(abc(\"def\".ghi).mno)middle(abc(\"def\".ghi).mno).last@(abc(\"def\".ghi).mno)example(abc(\"def\".ghi).mno).(abc(\"def\".ghi).mno)com(abc(\"def\".ghi).mno)",
                false);
        unitTest("first(abc\\(def)@example.com", true);
        unitTest(
                "first.last@x(1234567890123456789012345678901234567890123456789012345678901234567890).com",
                true);
        unitTest("a(a(b(c)d(e(f))g)h(i)j)@example.com", true);
        unitTest("a(a(b(c)d(e(f))g)(h(i)j)@example.com", false);
        unitTest("name.lastname@domain.com", true);
        unitTest(".@", false);
        unitTest("a@b", false);
        unitTest("@bar.com", false);
        unitTest("@@bar.com", false);
        unitTest("a@bar.com", true);
        unitTest("aaa.com", false);
        unitTest("aaa@.com", false);
        unitTest("aaa@.123", false);
        unitTest("aaa@[123.123.123.123]", true);
        unitTest("aaa@[123.123.123.123]a", false);
        unitTest("aaa@[123.123.123.333]", false);
        unitTest("a@bar.com.", false);
        unitTest("a@bar", false);
        unitTest("a-b@bar.com", true);
        unitTest("+@b.c", true);
        unitTest("+@b.com", true);
        unitTest("a@-b.com", false);
        unitTest("a@b-.com", false);
        unitTest("-@..com", false);
        unitTest("-@a..com", false);
        unitTest("a@b.co-foo.uk", true);
        unitTest("\"hello my name is\"@stutter.com", true);
        unitTest("\"Test \\\"Fail\\\" Ing\"@example.com", true);
        unitTest("valid@special.museum", true);
        unitTest("invalid@special.museum-", false);
        unitTest("shaitan@my-domain.thisisminekthx", true);
        unitTest("test@...........com", false);
        unitTest("foobar@192.168.0.1", false);
        unitTest("\"Joe\\\\Blow\"@example.com", true);
        unitTest("Invalid \\\n Folding \\\n Whitespace@example.com", false);
        unitTest("HM2Kinsists@(that comments are allowed)this.is.ok", true);
        unitTest("user%uucp!path@somehost.edu", true);
        unitTest("\"first(last)\"@example.com", true);
        unitTest(
                " \r\n (\r\n x \r\n ) \r\n first\r\n ( \r\n x\r\n ) \r\n .\r\n ( \r\n x) \r\n last \r\n ( x \r\n ) \r\n @example.com",
                true);
        unitTest("test. \r\n \r\n obs@syntax.com", true);
        unitTest("test. \r\n \r\n obs@syntax.com", true);
        unitTest("test.\r\n\r\n obs@syntax.com", false);
        unitTest("\"null \\\0\"@char.com", true);
        unitTest("\"null \0\"@char.com", false);
        unitTest("null\\\0@char.com", false);
        unitTest("email@address/resource", true);
        unitTest("\"\"@example.com", false);
    }

    private void unitTest(final String address, final boolean isValid) {
        if (isValid)
            assertTrue(EmailAddressUtils.isValidAddress(address));
        else
            assertFalse(EmailAddressUtils.isValidAddress(address));
    }
}
