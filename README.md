Marviq's JIRA Appender for Log4j

The content of this project can be used as a standard Appender for the Log4j logging framework. The appender provided by
this project will (attempt to) log events as (or in) Atlassian JIRA issues.

Two types of binaries are included in this distribution:
 * log4j-appender-jira-X.Y.Z.jar is an archive that includes all third party dependencies. The package names of their
   classes are modified, which prevents classloading issues.
 * original-log4j-appender-jira-X.Y.Z.jar is an archive that includes only the non-third-party code. Its dependencies
   are included in the /lib/ directory of this distribution.

Example configuration can be found in the log4j.xml file in the /example/ directory of this distribution. Please note
that this appender makes use of the XML-RPC interface of the JIRA instance. Make sure that it is enabled.

### BEWARE!
Logging straight to JIRA adds considerable overhead to your application. As with all appenders, this load is most likely
added to the JVM instance that's also executing your application code. Consider this before you start using the
Log4j-appender for JIRA!

It is advisable to wrap the Log4j-appender for JIRA in an asynchronous appender, that is configured to be non-blocking
(which means that it will drop events after its buffer is full). This will reduce the likelihood that a continuous
stream of to-be-logged events floods both your network IO as well as the JIRA instance you're logging to. It does,
however, not rule out any chance of this happening!

Only events that are logged on level ERROR (or higher) and were generated based on a Throwable are eligible for
logging by this appender.

This appender keeps an internal cache of JIRA issues that have been created. If a previous event triggered this appender
to create a JIRA issue, then comments on that issue (rather than a new JIRA issue) will be created for similar events.
Events are deemed 'similar' if their stacktraces are identical. The exception message is explicitly not part of the
identifying part of an event. The cache that is used to store events and corresponding JIRA keys is not persisted in any
way. Cache entries will not survive a restart of the JVM. Additionally, events that have not been raised again for some
time will be flushed from the cache.

### Configuration Example

It is advisable to wrap the Log4j-appender for JIRA in an asynchronous appender, that is configured to be non-blocking
(which means that it will drop events after its buffer is full). This will reduce the likelihood that a continuous
stream of to-be-logged events floods both your network IO as well as the JIRA instance you're logging to. It does,
however, not rule out any chance of this happening!

```
    <?xml version="1.0" encoding="UTF-8" ?>
    <!DOCTYPE log4j:configuration SYSTEM "log4j.dtd">

    <log4j:configuration xmlns:log4j='http://jakarta.apache.org/log4j/'>

        <appender name="STDOUT" class="org.apache.log4j.ConsoleAppender">
            <param name="Target" value="System.out"/>
            <layout class="org.apache.log4j.PatternLayout">
                <param name="ConversionPattern" value="%d %-5p [%t] %c - %m%n"/>
            </layout>
        </appender>

        <appender name="JIRA" class="com.marviq.util.logging.JIRALog4jAppender">

            <!-- The URL on which JIRA is accessible (the XML-RPC interface must be enabled) -->
            <param name="url" value="https://jira.example.org/"/>

            <!-- The JIRA username of the account that will be creating issues. -->
            <param name="username" value="USERNAME-OF-REPORTER"/>

            <!-- The password that authenticates 'username' -->
            <param name="password" value="PASSWORD-FOR-REPORTER"/>

            <!-- The JIRA project key in which issues will be created -->
            <param name="projectkey" value="PROJECTKEY"/>

            <!-- The JIRA username of the assignee of issues created by this appender -->
            <param name="assignee" value="USERNAME-OF-ASSIGNEE"/>

            <!-- Optional: a text label used to distinguish issues created by this appender from others with otherwise
                 similar configuration (e.g: "ACC server"). -->
            <param name="label" value="unit-testing"/>

            <layout class="org.apache.log4j.PatternLayout">
                <param name="ConversionPattern" value="%d{ISO8601} %-5p [%t] %c - %m%n"/>
            </layout>

            <filter class="org.apache.log4j.varia.LevelRangeFilter">
                <param name="levelMin" value="ERROR"/>
            </filter>

        </appender>

        <!-- It is advisable to wrap the Log4j-appender for JIRA in an asynchronous appender, that is configured to be
             non-blocking (which means that it will drop events after its buffer is full). This will reduce the likelihood
             that a continuous stream of to-be-logged events floods both your network IO as well as the JIRA instance you're
             logging to. It does, however, not rule out any chance of this happening! -->
        <appender name="ASYNC-JIRA" class="org.apache.log4j.AsyncAppender">
            <!-- blocking=false causes events that can't be buffered to be discarded (otherwise, calls to add those event to
                 the buffer would be blocking!) -->
            <param name="blocking" value="false"/>
            <appender-ref ref="JIRA"/>
        </appender>

        <root>
            <priority value="ALL"/>
            <!-- Be careful not to use the original 'jira' appender, but the asynchronous wrapper instead. -->
            <appender-ref ref="ASYNC-JIRA"/>
            <appender-ref ref="STDOUT"/>
        </root>

    </log4j:configuration>
```
