/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.fat;

import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.jaxrs.fat.restmetrics.MetricsUnmappedUncheckedException;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
public class RestMetricsTest {

    private final static Class<?> c = RestMetricsTest.class;

     // Array to hold the names that identify the methods in metrics 2.3
    static final String[] METHOD_STRINGS = {
                                            "optionsMethod",
                                            "headMethod",
                                            "headFallbackMethod",
                                            "AsyncResponse",
                                            "getMultiParamMessage_java.lang.String",
                                            "getMultiParamMessage_java.lang.String_java.lang.String",
                                            "postMessage_java.lang.String",
                                            "putMessage_java.lang.String",
                                            "deleteMessage",
                                            "getCheckedException_java.lang.String",
                                            "getUncheckedException_java.lang.String",
                                            "getAbortTest"};

    static final int CHECKED_METHOD_INDEX = 9;
    static final int UNCHECKED_METHOD_INDEX = 10;
    static final int ABORT_METHOD_INDEX = 11;

    static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") +
                    "/restmetrics/rest/";

    private static final String METRICSWAR = "restmetrics";

    private static final String METRICS_URL_STRING = "/metrics/base/REST.request";

    private final String MAPPEDURI = getBaseTestUri(METRICSWAR, "rest", "restmetrics");

    private static final int BUFFER = 2000; // margin or error in ms for response times.

    private static final int EXPECTEDRT = 200; // Expected min. response time for a single request.

    private int method_Index = 0;

    private final int[] methodCounts = new int[METHOD_STRINGS.length];

    private final double[] responseTimes = new double[METHOD_STRINGS.length];

    @Server("com.ibm.ws.jaxrs.fat.restmetrics")
    public static LibertyServer server;


    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, METRICSWAR, "com.ibm.ws.jaxrs.fat.restmetrics");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            Log.error(c, "setup", e, "Exception while starting server");
        }

        // Pause for the smarter planet message
        assertNotNull("The smarter planet message did not get printed on server",
                      server.waitForStringInLog("CWWKF0011I"));

        // wait for LTPA key to be available to avoid CWWKS4000E
        //TODO: make sure to remove this check when MP 5.0 is available - where MP Metrics supports EE9
        //      metrics loads LTPA, so when we use MP Metrics in MP 5, we'll need to verify that LTPA is started
        if (!JakartaEE9Action.isActive()) {
            assertNotNull("CWWKS4105I.* not received on server",
                      server.waitForStringInLog("CWWKS4105I.*"));
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer("MetricsUnmappedUncheckedException", "MetricsUnmappedCheckedException",
                              "Fault: Unmapped Checked","SRVE0777E", "SRVE0315E");
        }
    }

    public CloseableHttpClient getHttpClient() {
        return HttpClientBuilder.create().build();
    }

    public void resetHttpClient(CloseableHttpClient client) throws IOException {
        client.close();
    }

    private static int getPort() {
        return server.getHttpDefaultPort();
    }

    private static String getHost() {
        return server.getHostname();
    }


    @Test
    public void testCoreMethods() throws IOException {

        // Execute a get method with varying numbers of parameters.
        runOptionsMethod(method_Index, 200, "/restmetrics/rest/restmetrics", "Metrics!");

        runHeadMethod(++method_Index, 204, "/restmetrics/rest/restmetrics", null);

        runHeadMethod(++method_Index, 200, "/restmetrics/rest/restmetrics/fallback", "Metrics!");

        runGetMethod(++method_Index, 200, "/restmetrics/rest/restmetrics/async", "Metrics!");

        runGetMethod(++method_Index, 200, "/restmetrics/rest/restmetrics/test", "Metrics!");

        runGetMethod(++method_Index, 200, "/restmetrics/rest/restmetrics/test/test", "Metrics!");

        // Execute post method 3 times.
        runPostMethod(++method_Index);

        runPostMethod(method_Index);

        runPostMethod(method_Index);

        // Execute put method 4 times.
        runPutMethod(++method_Index);

        runPutMethod(method_Index);

        runPutMethod(method_Index);

        runPutMethod(method_Index);

        // Execute delete method once.
        runDeleteMethod(++method_Index);

        ArrayList<String> metricsList = getMetricsStrings(200, METRICS_URL_STRING, false);

        //Confirm the metrics information is available.
        for (int i = 0; i <= method_Index; i++) {
            // Confirm the metrics data
            responseTimes[i] = checkMetrics(metricsList, i);
            // Confirm the monitor data
            runCheckMonitorStats(200, i);
        }
    }

    @Test
    public void testAbort() throws IOException {

        runAbortGetMethod(ABORT_METHOD_INDEX, 200, "/restmetrics/rest/restmetrics/abortTest", "Metrics!");

        // Expect a 200 when metrics from other methods have already been collected. However, if this test is run first or by itself
        // no metrics should exist and a 404 would be expected.
        boolean allow404 = true;
        ArrayList<String> metricsList = getMetricsStrings(200, METRICS_URL_STRING, allow404);

        // getMetricsStrings() doesn't run for EE9 so metricsList will be null in that case
        if ((metricsList != null) && (metricsList.contains("abortTest"))) {
            fail("The /restmetrics/rest/restmetrics/abortTest method should not have run following the ContainerRequestFilter abort so no metrics information should be collected." + metricsList.toString());
        }
        ensureEmptyMonitorStats(ABORT_METHOD_INDEX);
    }

    @Test
    @AllowedFFDC({"org.apache.cxf.interceptor.Fault", "org.jboss.resteasy.spi.UnhandledException"})
    public void testCheckedExceptions() throws IOException {


        runGetCheckedExceptionMethod(CHECKED_METHOD_INDEX, 200, "/restmetrics/rest/restmetrics/checked/mappedChecked", "Mapped Checked");

        runGetCheckedExceptionMethod(CHECKED_METHOD_INDEX, 500, "/restmetrics/rest/restmetrics/checked/unmappedChecked", "Unmapped Checked");

        ArrayList<String> metricsList = getMetricsStrings(200, METRICS_URL_STRING, false);

        // Confirm the metrics data
        responseTimes[CHECKED_METHOD_INDEX] = checkMetrics(metricsList, CHECKED_METHOD_INDEX);

        // Confirm the monitor data
        runCheckMonitorStats(200, CHECKED_METHOD_INDEX);
    }

    @Test
    @AllowedFFDC({"com.ibm.ws.jaxrs.fat.restmetrics.MetricsUnmappedUncheckedException", "org.jboss.resteasy.spi.UnhandledException"})
    public void testUncheckedExceptions() throws IOException {

        runGetUncheckedExceptionMethod(UNCHECKED_METHOD_INDEX, 200, "/restmetrics/rest/restmetrics/unchecked/mappedUnchecked", "Mapped Unchecked");

        runGetUncheckedExceptionMethod(UNCHECKED_METHOD_INDEX, 500, "/restmetrics/rest/restmetrics/unchecked/unmappedUnchecked", "Unmapped Unchecked");


        ArrayList<String> metricsList = getMetricsStrings(200, METRICS_URL_STRING, false);

        // Confirm the metrics data
        responseTimes[UNCHECKED_METHOD_INDEX] = checkMetrics(metricsList, UNCHECKED_METHOD_INDEX);

        // Confirm the monitor data
        runCheckMonitorStats(200, UNCHECKED_METHOD_INDEX);

    }

    private void runCheckMonitorStats(int exprc, int index) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() +
                          "/restmetrics/rest/restmetrics/" + index +
                          "/" + methodCounts[index] + "/" + responseTimes[index]);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            if (retcode != exprc) {
                fail("Bad return Code getting Monitor stats. Expected " + exprc + "Got "
                     + retcode);
                return;
            }
            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }

                if (lines.indexOf("Passed!") < 0)
                    fail("Missing success message in output. " + lines);
            }

            return;
        } finally {
            con.disconnect();
        }
    }

    private void ensureEmptyMonitorStats(int index) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() +
                          "/restmetrics/rest/restmetrics/ensureempty/" + index);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            if (retcode != 200) {
                fail("Bad return Code getting Monitor stats. Expected 200, " + "Got "
                     + retcode);
                return;
            }
            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String sep = System.getProperty("line.separator");
            StringBuilder lines = new StringBuilder();
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                lines.append(line).append(sep);
            }

            if (lines.indexOf("Passed!") < 0)
                fail("Missing success message in output. " + lines);

             return;
        } finally {
            con.disconnect();
        }
    }

    private void runOptionsMethod(int index, int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("OPTIONS");

            retcode = con.getResponseCode();

            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }

                if (lines.indexOf(testOut) < 0)
                    fail("Missing success message in output. " + lines);

                // Increment the count only if successful execution or mapped exception.
                methodCounts[index]++;
            }

            if (retcode != exprc)
                fail("Bad return Code from Resource Method. Expected: " + exprc + ", Received: "
                     + retcode);

            return;
        } finally {
            con.disconnect();
        }
    }

    private void runHeadMethod(int index, int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("HEAD");

            retcode = con.getResponseCode();

            if (retcode == exprc) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }

                if (lines.length() != 0)
                    fail("Head method returned: " + lines);

                // Increment the count only if successful execution or mapped exception.
                methodCounts[index]++;
            }

            if (retcode != exprc)
                fail("Bad return Code from Resource Method. Expected: " + exprc + ", Received: "
                     + retcode + ", Index = " + index);

            return;
        } finally {
            con.disconnect();
        }
    }

    private void runGetMethod(int index, int exprc, String requestUri, String testOut) throws IOException {
        URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }

                if (lines.indexOf(testOut) < 0)
                    fail("Missing success message in output. " + lines);

                // Increment the count only if successful execution or mapped exception.
                methodCounts[index]++;
            }

            if (retcode != exprc)
                fail("Bad return Code from Get. Expected: " + exprc + ", Received: "
                     + retcode);

            return;
        } finally {
            con.disconnect();
        }
    }
    private void runAbortGetMethod(int index, int exprc, String requestUri, String testOut) throws IOException {

         URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
        int retcode;
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        try {
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            InputStream is = con.getInputStream();

            if (is != null) {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }
                // The response code should be 200, but no resource method should have run so the stream
                // should be empty.
                if (lines.length() > 0)
                    fail("The abortTest resource method should not have run. InputStream = " + lines);

            }

            if (retcode != exprc) {
                fail("Bad return Code from Get. Expected: " + exprc + ", Received: "
                     + retcode);
            }

            return;
        } finally {
            con.disconnect();
        }
    }

    private void runGetCheckedExceptionMethod(int index, int exprc, String requestUri, String testOut) {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
            int retcode;
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }

                if (lines.indexOf(testOut) < 0)
                    fail("Missing success message in output. Expected: " + testOut
                         + ", Received:  " + lines);

                // Increment the count only if successful execution.
                methodCounts[index]++;


            }

            if (retcode != exprc) {
                fail("Bad return Code from Get. Expected: " + exprc + ", Received: "
                     + retcode);
            }

            return;
        }
        catch (Exception e) {
            fail ("Unexpected exception thrown processing index = " + index + ":  " + e);
        }
        finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private void runGetUncheckedExceptionMethod(int index, int exprc, String requestUri, String testOut) throws IOException, MetricsUnmappedUncheckedException {
        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + getHost() + ":" + getPort() + requestUri);
            int retcode;
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();

            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);

                String sep = System.getProperty("line.separator");
                StringBuilder lines = new StringBuilder();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.append(line).append(sep);
                }

                if (lines.indexOf(testOut) < 0)
                    fail("Missing success message in output. Expected: " + testOut
                         + ", Received:  " + lines);

                // Increment the count only if successful execution.
                methodCounts[index]++;

            }

            if (retcode != exprc) {
                fail("Bad return Code from Get. Expected: " + exprc + ", Received: "
                     + retcode);
            }

            return;
        }
        catch (Exception e) {
            fail ("Unexpected exception thrown:  " + e.getMessage());
        }

        finally {
            if (con != null) {
                con.disconnect();
            }
        }
    }

    private void runPostMethod(int index) throws IOException {

        HttpPost postMethod = new HttpPost(MAPPEDURI + "/post1");
        org.apache.http.entity.
        StringEntity entity = new StringEntity("Post");
        entity.setContentType("text/*");
        postMethod.setEntity(entity);
        CloseableHttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(postMethod);

            assertEquals(200, resp.getStatusLine().getStatusCode());

            // Increment the count
            methodCounts[index]++;

        } finally {
            resetHttpClient(client);
        }
    }

    private void runPutMethod(int index) throws IOException {
        HttpPut putMethod = new HttpPut(MAPPEDURI + "/put1");
        StringEntity entity = new StringEntity("Put");
        entity.setContentType("text/*");
        putMethod.setEntity(entity);

        CloseableHttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(putMethod);

            assertEquals(200, resp.getStatusLine().getStatusCode());

            // Increment the count
            methodCounts[index]++;

        } finally {
            resetHttpClient(client);
        }

    }


    private void runDeleteMethod(int index) throws IOException {
        HttpDelete deleteMethod = new HttpDelete(MAPPEDURI + "/delete1");

        CloseableHttpClient client = getHttpClient();

        try {
            HttpResponse resp = client.execute(deleteMethod);
            assertEquals(204, resp.getStatusLine().getStatusCode());

            // Increment the count
            methodCounts[index]++;

        } finally {
            resetHttpClient(client);
        }
    }

    private ArrayList<String> getMetricsStrings(int expectedRc, String metricString, boolean allow404) {
        //TODO: remove this check when MP 5.0 is available - where MP Metrics supports EE9:
        if (JakartaEE9Action.isActive()) {
            return null;
        }

        HttpURLConnection con = null;
        try {
            URL url = new URL("http://" + getHost() + ":" + getPort() + metricString);
            int retcode;
            con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setRequestMethod("GET");

            retcode = con.getResponseCode();
            // Since these tests an run asynchronously it is possible that a testcase that does not expect metrics to be
            // collected (for example testAbort) may run first or be specified as the only test to run.  In
            // this case a 404 is expected.
            if ((retcode != expectedRc) &&
                ((retcode == 404) && (!allow404))){

                fail("Bad return Code from Metrics method call. Expected " + expectedRc + ": Received "
                     + retcode + ", URL = " + url.toString());

                return null;
            }

            if (retcode == 200) {
                InputStream is = con.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);

                BufferedReader br = new BufferedReader(isr);

                ArrayList<String> lines = new ArrayList<String>();
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    lines.add(line);
                }
                return lines;
            }
            return null;

        } catch (Exception e) {
            fail("Caught unexpected exception: " + e);
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }

    }


    private float checkMetrics(ArrayList<String> lines, int index) {
        //TODO: remove this check when MP 5.0 is available - where MP Metrics supports EE9:
        if (JakartaEE9Action.isActive()) {
            return -1.0f;
        }

        float responseTime = -1;
        int value = -1;


        // Read the lines from the output looking for the SimpleTimer output for the request count total
        // and response time.
        for (String line : lines) {

            if ((line.contains(METHOD_STRINGS[index])) &&
                           (line.contains("base_REST_request_total"))) {
                String stringValue = line.substring(line.indexOf("}") + 2);
                value = Integer.parseInt(stringValue);
            }

            if ((line.contains(METHOD_STRINGS[index])) &&
                            (line.contains("base_REST_request_elapsedTime"))) {
                String stringValue = line.substring(line.indexOf("}") + 2);
                responseTime = Float.parseFloat(stringValue) * 1000; //convert to ms
            }

            if ((value != -1) && (responseTime != -1)) {
                break;
            }
        }
        if (value == -1) {
            fail("Failure: base_REST_request_total not found for method " + METHOD_STRINGS[index]);
        } else if (value != methodCounts[index]) {
            fail("Failure:  Expected Request Count of " + methodCounts[index] +
                 " but received Request Count of " + value + ":  index = " + index);
        }

        int minExpectedResponseTime = EXPECTEDRT * methodCounts[index];
        int maxExpectedResponseTime = minExpectedResponseTime + BUFFER;

        if (responseTime == -1) {
            fail("Failure: base_REST_request_elapsedTime not found for method" + METHOD_STRINGS[index]);
        } else if (!((minExpectedResponseTime <= responseTime) &&
                        (responseTime <= maxExpectedResponseTime))) {
            fail("Failure:  Expected response time >= " + minExpectedResponseTime +
                 " and <= " + maxExpectedResponseTime +
                 " but received a response time of " + responseTime + ":  index = " + index);
            return -1;
        }

        return responseTime;


    }
}
