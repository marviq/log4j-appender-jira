package com.marviq.util.logging;

import junit.framework.Assert;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Test;

/**
 * A collection of tests that verify that the "exception message" of linked 'cause' throwables are ignored while
 * computing the hash of a throwable.
 *
 * @see <a href="http://java.net/jira/browse/LOG4J_APPENDER_JIRA-3">LOG4J_APPENDER_JIRA-3</a>
 * @author Guus der Kinderen, guus.der.kinderen@marviq.com
 */
public class HashAndCauseTest {

    /**
     * Asserts that two ThrowableInformation instances based on equal, but distinct Throwable instances (that both have
     * an equal but distinct 'cause') are deemed equal by JIRALog4jAppender#getHash();
     */
    @Test
    public void testEqualThrowables() throws Exception {

        // setup
        // Construction of throwable instances need to happen on the same line for the stacktraces to be identical!
        final Throwable c1 = new Throwable("cause"); final Throwable c2 = new Throwable("cause");
        final Throwable t1 = new Throwable("throwable", c1); final Throwable t2 = new Throwable("throwable", c2);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(t1);
        final int resultB = JIRALog4jAppender.getHash(t2);

        // verify
        Assert.assertEquals(resultA, resultB);
    }

    /**
     * Test similar to {@link #testEqualThrowables()} but changes the message of the (non-'cause')
     * throwables. As messages should not be considered while computing the hash, hashes should be equal.
     */
    @Test
    public void testDifferentThrowableMessages() throws Exception {

        // setup
        // Construction of throwable instances need to happen on the same line for the stacktraces to be identical!
        final Throwable c1 = new Throwable("cause"); final Throwable c2 = new Throwable("cause");
        final Throwable t1 = new Throwable("throwable 1", c1); final Throwable t2 = new Throwable("throwable 2", c2);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(t1);
        final int resultB = JIRALog4jAppender.getHash(t2);

        // verify
        Assert.assertEquals(resultA, resultB);
    }

    /**
     * Test similar to {@link #testEqualThrowables()} but changes the message of the 'cause' throwables. As messages
     * should not be considered while computing the hash, hashes should be equal.
     */
    @Test
    public void testDifferentCauseMessages() throws Exception {

        // setup
        // Construction of throwable instances need to happen on the same line for the stacktraces to be identical!
        final Throwable c1 = new Throwable("cause 1"); final Throwable c2 = new Throwable("cause 2");
        final Throwable t1 = new Throwable("throwable", c1); final Throwable t2 = new Throwable("throwable", c2);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(t1);
        final int resultB = JIRALog4jAppender.getHash(t2);

        // verify
        Assert.assertEquals(resultA, resultB);
    }

    /**
     * Test similar to {@link #testEqualThrowables()} but changes 'cause' throwables (to make them unequal).
     */
    @Test
    public void testUnequalCauses() throws Exception {

        // setup
        final Throwable c1 = new Throwable("cause"); // CANNOT be on the same line as the one below (need different line numbers!)
        final Throwable c2 = new Throwable("cause"); // CANNOT be on the same line as the one above (need different line numbers!)
        // Construction of throwable instances need to happen on the same line for the stacktraces to be identical!
        final Throwable t1 = new Throwable("throwable", c1); final Throwable t2 = new Throwable("throwable", c2);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(t1);
        final int resultB = JIRALog4jAppender.getHash(t2);

        // verify
        Assert.assertFalse(resultA == resultB);
    }
}
