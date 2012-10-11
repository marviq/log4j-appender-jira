Marviq's JIRA Appender for Log4j README

The content of this project can be used as a standard Appender for the Log4j logging framework. The appender provided by
this project will (attempt to) log events as (or in) Atlassian JIRA issues.

Example configuration can be found in the log4j.xml file in the /example/ directory of this distribution. Please note
that this appender makes use of the XML-RPC interface of the JIRA instance. Make sure that it is enabled.

BEWARE!
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
