/*
 * Copyright 2011,2012 Marviq B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.marviq.util.logging;

import java.util.Arrays;
import junit.framework.Assert;
import org.apache.log4j.Category;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.junit.Test;

public class JIRALog4jAppenderTest {

    /**
     * Asserts that two ThrowableInformation instances based on the same throwable are deemed equal by
     * JIRALog4jAppender#getHash();
     */
    @Test
    public void testEqualHashForSingleThrowable() throws Exception {

        // setup
        final Throwable throwable = new Throwable();
        final ThrowableInformation tiA = new ThrowableInformation(throwable);
        final ThrowableInformation tiB = new ThrowableInformation(throwable);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(tiA);
        final int resultB = JIRALog4jAppender.getHash(tiB);

        // verify
        Assert.assertEquals(resultA, resultB);
    }
    
    /**
     * Tests the trimming feature of summary text creation, in order to stay within
     * the maximum summary length of Jira.
     * @see <a href="http://java.net/jira/browse/LOG4J_APPENDER_JIRA-1">Problem occurring previously with summary length</a>
     */
    @Test
    public void testCreateSummaryText() throws Exception {
        final LoggingEvent le = new LoggingEvent(null, Logger.getLogger("LOGGER_NAME_WITH_A_LENGTH_OF_31"), Priority.ERROR, "MESSAGE_WITH_A_LENGTH_OF_27", null);

        // Length of summary will be sum of:
        //   27   "(Auto-generated, labelled '"
        //   ..   label
        //    3   "') "
        //   31   "LOGGER_NAME_WITH_A_LENGTH_OF_31"
        //    1   ":"
        //   27   "MESSAGE_WITH_A_LENGTH_OF_27"
        //  --- +
        //   89   PLUS label length
        final char[] labelCharsJustWithinLimits = new char[JIRALog4jAppender.MAXIMUM_SUMMARY_LENGTH - 89];
        final char[] labelCharsJustBeyondLimits = new char[JIRALog4jAppender.MAXIMUM_SUMMARY_LENGTH - 89 + 1];
        Arrays.fill(labelCharsJustWithinLimits, '*');
        Arrays.fill(labelCharsJustBeyondLimits, '*');
        
        final String summaryForJustWithinLimits = JIRALog4jAppender.getSummary(le, new String(labelCharsJustWithinLimits));
        final String summaryForJustBeyondLimits = JIRALog4jAppender.getSummary(le, new String(labelCharsJustBeyondLimits));
                
        Assert.assertTrue(
            "Summary for just within limits was not as expected: " + summaryForJustWithinLimits,
            summaryForJustWithinLimits.endsWith("MESSAGE_WITH_A_LENGTH_OF_27")
        );
        Assert.assertTrue(
            "Summary for just beyond limits was not as expected: " + summaryForJustBeyondLimits,
            summaryForJustBeyondLimits.endsWith("MESSAGE_WITH_A_LENGTH_ ...")
        );
    }

    /**
     * Asserts that two ThrowableInformation instances based on equal, but distinct Throwable instances are deemed
     * equal by JIRALog4jAppender#getHash();
     */
    @Test
    public void testEqualHashForEqualThrowables() throws Exception {

        // setup
        final String message = "text";
        // Construction of throwable instances need to happen on the same line for the stacktraces to be identical!
        final Throwable t1 = new Throwable(message); final Throwable t2 = new Throwable(message);
        final ThrowableInformation tiA = new ThrowableInformation(t1);
        final ThrowableInformation tiB = new ThrowableInformation(t2);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(tiA);
        final int resultB = JIRALog4jAppender.getHash(tiB);

        // verify
        Assert.assertEquals(resultA, resultB);
    }

    /**
     * Asserts that two ThrowableInformation instances, based Throwable instances that have different text messages but
     * are equal otherwise, are deemed equal by JIRALog4jAppender#getHash();
     */
    @Test
    public void testEqualHashForThrowablesWithDifferentMessages() throws Exception {

        // setup
        // Construction of throwable instances need to happen on the same line for the stacktraces to be identical!
        final Throwable t1 = new Throwable("one"); final Throwable t2 = new Throwable("two");
        final ThrowableInformation tiA = new ThrowableInformation(t1);
        final ThrowableInformation tiB = new ThrowableInformation(t2);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(tiA);
        final int resultB = JIRALog4jAppender.getHash(tiB);

        // verify
        Assert.assertEquals(resultA, resultB);
    }

    /**
     * Asserts that two ThrowableInformation instances, based distinct Throwable instances, are deemed unequal by
     * JIRALog4jAppender#getHash();
     */
    @Test
    public void testNonEqualHashForDifferentThrowables() throws Exception {

        // setup
        final Throwable t1 = new Throwable(); // CANNOT be on the same line as the one below (need different line numbers!)
        final Throwable t2 = new Throwable(); // CANNOT be on the same line as the one above (need different line numbers!)
        final ThrowableInformation tiA = new ThrowableInformation(t1);
        final ThrowableInformation tiB = new ThrowableInformation(t2);

        // do magic
        final int resultA = JIRALog4jAppender.getHash(tiA);
        final int resultB = JIRALog4jAppender.getHash(tiB);

        // verify
        Assert.assertTrue(resultA != resultB);
    }

    /**
     * Asserts that when the argument to JIRALog4jAppender#getHash() is <tt>null</tt>, a runtime exception is thrown.
     */
    @Test(expected=RuntimeException.class)
    public void testNullArgumentThrowsException() {
        JIRALog4jAppender.getHash(null);
    }

    /**
     * Usage test: assert that a Log instance can log several equal as well as distinct messages, when using our
     * appender (configuration is done in the log4j.xml file from the resources directory)
     */
    @Test
    public void testLogSimilarOnes() throws Exception {

        // setup
        final Logger log = Logger.getLogger(this.getClass());
        final Throwable throwable = new Throwable();

        // do magic
        try {
            log.error("This is another test which should get a comment.", throwable);
            log.error("It should be OK to have a different message that goes with the same throwable.", throwable);
            log.error("Third occurrence....", throwable);
            log.error("Still same throwable....", throwable);
            log.error("And now for something completely different... ", new Throwable());
        } catch (Throwable t) {
            // verify
            Assert.fail();
        }

        Thread.sleep(5000); // give the async logging process time to catch up.
    }

    /**
     * Usage test: assert that a Log instance can be constructed using our appender (configuration is done in the
     * log4j.xml file from the resources directory)
     *
     * Note that this test will register a lot of errors (as the configuration in log4j.xml is likely to contain dummy
     * data), but should not throw any Exception itself.
     */
    @Test
    public void testLogNew() throws Exception {

        // setup
        final Logger log = Logger.getLogger(this.getClass());

        // do magic
        try {
            log.error("This is a test", new Throwable());
        } catch (Throwable t) {
            // verify
            Assert.fail();
        }

        Thread.sleep(5000); // give the async logging process time to catch up.
    }

}
