/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.kernel.boot;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * This test bucket tests the server startup process.
 */
public class ServerStartAsServiceTest {
    private static final Class<?> c = ServerStartAsServiceTest.class;

    private static final String SERVER_NAME_1 = "ServerStartAsServiceTest1";
    private static final String SERVER_NAME_2 = "ServerStartAsServiceTest2";
    private static final String SERVER_NAME_3 = "ServerStartAsServiceTest3";

    private static LibertyServer server;

    @ClassRule
    public static final TestRule onWinRule = new OnlyRunOnWinRule();

    @After
    public void after() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @Test
    /**
     * test Liberty Server to Register, Start, Stop and Unregister as a Windows Service
     *
     * @throws Exception
     */
    public void testWinServiceLifeCycle() throws Exception {
        final String METHOD_NAME = "testWinServiceLifeCycle";
        Log.entering(c, METHOD_NAME);

        Log.info(c, METHOD_NAME, "calling LibertyServerFactory.getLibertyServer(SERVER_NAME, ON): " + SERVER_NAME_1);
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME_1, LibertyServerFactory.WinServiceOption.ON);

        Log.info(c, METHOD_NAME, "calling server.startServer()");
        server.startServer();

        Log.info(c, METHOD_NAME, "calling server.waitForStringInLog('CWWKF0011I')");
        server.waitForStringInLog("CWWKF0011I");

        assertTrue("the server should have been started", server.isStarted());

        Log.info(c, METHOD_NAME, "calling server.stopServer(): " + SERVER_NAME_1);
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    @Test
    /**
     * test Liberty Server to Register, Start, Stop and Unregister as a Windows Service
     * test snoop can be installed and accessed.
     *
     * @throws Exception
     */
    public void testWinServiceAppAccess() throws Exception {
        final String METHOD_NAME = "testWinServiceAppAccess";
        Log.entering(c, METHOD_NAME);

        Log.info(c, METHOD_NAME, "calling LibertyServerFactory.getLibertyServer(SERVER_NAME, ON): " + SERVER_NAME_2);
        server = LibertyServerFactory.getLibertyServer(SERVER_NAME_2, LibertyServerFactory.WinServiceOption.ON);

        Log.info(c, METHOD_NAME, "calling server.startServer()");
        server.startServer();

        Log.info(c, METHOD_NAME, "calling server.waitForStringInLog('CWWKF0011I')");
        server.waitForStringInLog("CWWKF0011I");

        assertTrue("the server should have been started", server.isStarted());

        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/snoop");
        Log.info(c, METHOD_NAME, "Calling Snoop Application with URL=" + url.toString());
        HttpURLConnection con = getHttpConnection(url);
        BufferedReader br = getConnectionStream(con);
        String line = br.readLine();
        assertTrue("The response did not contain the \'Snoop Servlet\'",
                   line.contains("Snoop Servlet"));

        Log.info(c, METHOD_NAME, "return line: " + line);

        Log.info(c, METHOD_NAME, "calling server.stopServer(): " + SERVER_NAME_2);
        server.stopServer();

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * Test Liberty Server to Register, Start, Stop and Unregister as a Windows Service with
     * the WLP_OUTPUT_DIR variable being set in server.env file.
     *
     * @throws Exception
     */
    @Test
    public void testWinServiceLifeCycleUsingWLP_OUTPUT_DIR() throws Exception {
        final String METHOD_NAME = "testWinServiceLifeCycleUsingWLP_OUTPUT_DIR";
        Log.entering(c, METHOD_NAME);

        try {

            Log.info(c, METHOD_NAME, "calling LibertyServerFactory.getLibertyServer(SERVER_NAME, ON): " + SERVER_NAME_3);
            server = LibertyServerFactory.getLibertyServer(SERVER_NAME_3, LibertyServerFactory.WinServiceOption.ON);

            String logDir = "WLP_OUTPUT_DIR=" + server.getServerRoot() + File.separator + "tempLogs" + File.separator;
            server.setLogsRoot(server.getServerRoot() + File.separator + "tempLogs" + File.separator + SERVER_NAME_3 + File.separator + "logs" + File.separator);
            Log.info(c, METHOD_NAME, "creating /etc/server.env");
            String serverEnvCreate = createServerEnvFile(logDir, server.getInstallRoot());

            Log.info(c, METHOD_NAME, "server.env location = " + serverEnvCreate);
            assertTrue("the server.env file was not created.", serverEnvCreate.contains("server.env"));

            Log.info(c, METHOD_NAME, "calling server.startServer()");
            server.startServer(true, false);

            Log.info(c, METHOD_NAME, "calling server.waitForStringInLog('CWWKF0011I')");
            server.waitForStringInLog("CWWKF0011I");

            assertTrue("the server should have been started", server.isStarted());

            Log.info(c, METHOD_NAME, "calling server.stopServer(): " + SERVER_NAME_3);
            server.stopServer();

        } finally {
            Log.info(c, METHOD_NAME, "deleting /etc/server.env ");
            String serverEnvDelete = deleteServerEnvFile(server.getInstallRoot());
            assertTrue("the server.env file was not deleted.", serverEnvDelete.contains("server.env"));
        }

        Log.exiting(c, METHOD_NAME);
    }

    /**
     * This method is used to get a connection stream from an HTTP connection. It
     * gives the output from the webpage that it gets from the connection
     *
     * @param con The connection to the HTTP address
     * @return The Output from the webpage
     */
    private static BufferedReader getConnectionStream(HttpURLConnection con) throws IOException {
        InputStream is = con.getInputStream();
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        return br;
    }

    /**
     * This method creates a connection to a webpage and then reutrns the connection
     *
     * @param url The Http Address to connect to
     * @return The connection to the http address
     */
    private static HttpURLConnection getHttpConnection(URL url) throws IOException, ProtocolException {
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestMethod("GET");
        return con;
    }

    /**
     * Create the <wlp.install.root>/etc/server.env file
     *
     * @param value
     * @param wlpDir
     * @throws IOException
     */
    private String createServerEnvFile(String value, String wlpDir) throws IOException {
        File wlpEtcDir = new File(wlpDir, "etc");
        File serverEnvFile = new File(wlpEtcDir, "server.env");

        String serverEnv = serverEnvFile.getAbsolutePath();

        if (!wlpEtcDir.exists()) {
            wlpEtcDir.mkdirs();
        }

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(serverEnvFile);
            fos.write(value.getBytes());
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

        return serverEnv;
    }

    /**
     * Deletes the server.env file
     *
     * @param wlpDir
     * @throws IOException
     */
    private String deleteServerEnvFile(String wlpDir) throws IOException {
        File wlpEtcDir = new File(wlpDir, "etc");
        File serverEnvFile = new File(wlpEtcDir, "server.env");

        String serverEnv = serverEnvFile.getAbsolutePath();

        serverEnvFile.delete();

        return serverEnv;
    }
}
