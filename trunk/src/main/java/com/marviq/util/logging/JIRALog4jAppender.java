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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * An {@link org.apache.log4j.Appender} that registers events in a JIRA instance.
 *
 * Only events logged at level ERROR or higher are being logged by this implementation. If events were logged without a
 * Throwable instance, they are ignored by this appender.
 *
 * This appender keeps an internal cache of JIRA issues that have been created by instances of this class. If a previous
 * event triggered this appender to create a JIRA issue, then comments on that issue will be created for similar events.
 *
 * Events are deemed 'similar' if their stacktraces are identical. The exception message is explicitly not part of the
 * identifying part of an event.
 *
 * The cache that is used to store events and corresponding JIRA keys is not persisted in any way. Cache entries will
 * not survive a restart of the JVM.
 *
 * @author Guus der Kinderen, guus.der.kinderen@marviq.com
 */
public class JIRALog4jAppender extends AppenderSkeleton {

    /** class#getSimpleName() for this appender. */
    private static final String SIMPLE_NAME = JIRALog4jAppender.class.getSimpleName();

    /**
     * The cache that holds earlier reported issues. This is used when evaluating if a new JIRA issue needs to be
     * created, or if the exception should be added as a comment to an existing JIRA issue.
     */
    private static final Cache<Integer, String> CACHE = CacheBuilder.newBuilder()
            .maximumSize(5000)
            .expireAfterAccess(7, TimeUnit.DAYS)
            .build();

    private String url;
    private String username;
    private String password;
    private String projectkey;
    private String label;
    private String assignee;

    public JIRALog4jAppender() {
        setDefaultLayout();
    }

    private void setDefaultLayout() {
        PatternLayout patternLayout = new PatternLayout();
        patternLayout.setConversionPattern(PatternLayout.TTCC_CONVERSION_PATTERN);
        layout = patternLayout;
    }

    public JIRALog4jAppender(String url, String username, String password, String projectkey) {
        setDefaultLayout();
        this.url = url;
        this.username = username;
        this.password = password;
        this.projectkey = projectkey;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProjectkey() {
        return projectkey;
    }

    public void setProjectkey(String projectkey) {
        this.projectkey = projectkey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public boolean requiresLayout() {
        return true;
    }

    public void close() {
        synchronized (CACHE) {
            CACHE.invalidateAll();
        }
    }

    protected void append(LoggingEvent event) {
        if (!event.getLevel().isGreaterOrEqual(Level.ERROR)) {
            // Ignore events that have been logged at a level lower than ERROR.
            return;
        }

        ThrowableInformation ti = event.getThrowableInformation();
        if (ti == null || ti.getThrowableStrRep().length <= 1) {
            // Ignore events that have been logged without a Throwable.
            return;
        }

        if (username == null || password == null) {
            LogLog.warn(SIMPLE_NAME + ": Missing authentication details. " +
                    "Please configure log4j-jira-appender properly.");
            return;
        }

        final int hash = getHash(ti);

        final String title;
        if (label != null && label.trim().length() > 0) {
            title = "(Auto-generated, labelled '" + label + "') " + event.getLoggerName() + ':' + event.getMessage();
        } else {
            title = "(Auto-generated) " + event.getLoggerName() + ':' + event.getMessage();
        }

        try {
            LogLog.debug(SIMPLE_NAME + ": Creating ticket in project " + projectkey);

            final JiraClientContainer clientContainer = getClientContainer();

            synchronized (CACHE) {
                final String existingKey = CACHE.getIfPresent(hash);
                if (existingKey == null) {
                    // Create a new JIRA issue.
                    final Hashtable<String, String> issue = new Hashtable<String, String>();
                    issue.put("project", projectkey);
                    issue.put("summary", title);
                    issue.put("description", getText(event, false));
                    issue.put("assignee", assignee);
                    issue.put("type", "1");

                    final List params = new ArrayList();
                    params.add(clientContainer.token);
                    params.add(issue);

                    final Map<String, String> newIssue =
                            (Map<String, String>) clientContainer.client.execute("jira1.createIssue", params);
                    final String newIssueKey = newIssue.get("key");

                    LogLog.debug(SIMPLE_NAME + ": Created ticket " + newIssueKey);
                    CACHE.put(hash, newIssueKey);
                } else {
                    // Add comment to existing issue.
                    final List params = new ArrayList();
                    params.add(clientContainer.token);
                    params.add(existingKey);
                    params.add(getText(event, true));
                    clientContainer.client.execute("jira1.addComment", params);

                    LogLog.debug(SIMPLE_NAME + ": Updated ticket " + existingKey);
                }
            }
        } catch (MalformedURLException e) {
            LogLog.error(SIMPLE_NAME + ": Failed to create or update ticket in project " + projectkey, e);
        } catch (XmlRpcException e) {
            LogLog.error(SIMPLE_NAME + ": Failed to create or update ticket in project " + projectkey, e);
        }
    }

    /**
     * Calculates a semi-unique hash for the Throwable that is being logged. Note that this method ignores the
     * exception message by design (allowing a message to contain instance-specific information).
     *
     * The hash code generated by this method is used by this class to determine if a JIRA issue has already been
     * generated for this particular problem.
     *
     * @param t log4j's internal representation of throwables (must not be <tt>null</tt>).
     * @return a hash code value for the stack trace that is part of the argument.
     */
    static int getHash(ThrowableInformation t) {
        final String[] strRep = t.getThrowableStrRep();
        int hash = 0;
        for (int i = 1; i < strRep.length; i++) {
            hash = 31 * hash + (strRep[i] != null ? strRep[i].hashCode() : 0);
        }
        return hash;
    }

    protected String getText(LoggingEvent event, boolean skipStack) {
        final StringBuilder text = new StringBuilder();
        text.append("The following was logged by the application:\n");
        text.append("{code}\n");
        text.append(layout.format(event).trim());

        if (skipStack) {
            text.append("\n...\n(Stacktrace omitted as it is identical to the one in the description of this issue).\n");
        } else {
            for (final String line : event.getThrowableInformation().getThrowableStrRep()) {
                text.append('\n').append(line);
            }
        }

        text.append("{code}\n");
        return text.toString();
    }

    private JiraClientContainer getClientContainer() throws MalformedURLException, XmlRpcException {
        final JiraClientContainer clientContainer = new JiraClientContainer();

        LogLog.debug(SIMPLE_NAME + ": Connecting to xml-rpc host on " + url);

        final XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
        config.setServerURL(new URL(url + "/rpc/xmlrpc"));
        clientContainer.client = new XmlRpcClient();
        clientContainer.client.setConfig(config);

        final List params = new ArrayList();
        params.add(username);
        params.add(password);

        LogLog.debug(SIMPLE_NAME + ": Attempting to login to JIRA installation at " + url + " as " + username);

        clientContainer.token = (String) clientContainer.client.execute("jira1.login", params);
        return clientContainer;
    }

    static class JiraClientContainer {
        public XmlRpcClient client;
        public String token;
    }
}