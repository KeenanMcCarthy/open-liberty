/*******************************************************************************
 * Copyright (c) 2018,2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.microprofile.opentracing13.tck;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.utils.MvnUtils;

/**
 * This is a test class that runs a whole Maven TCK as one test FAT test.
 */
@RunWith(FATRunner.class)
public class OpentracingTCKLauncher {

    @Server("OpentracingTCKServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    /*
     * CWWKG0014E - Ignore due to server.xml intermittently missing
     */
    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer("CWMOT0009W", "CWWKG0014E");
    }

    @Test
    @AllowedFFDC // The tested deployment exceptions cause FFDC so we have to allow for this.
    public void launchOpentracingTck() throws Exception {
        // Use default tck-suite.xml
        MvnUtils.runTCKMvnCmd(server, "com.ibm.ws.opentracing.1.3_fat", this.getClass() + ":launchOpentracingTck");
        Map<String, String> resultInfo = MvnUtils.getResultInfo(server);
        resultInfo.put("results_type", "MicroProfile");
        resultInfo.put("feature_name", "Open Tracing");
        resultInfo.put("feature_version", "1.3");
        MvnUtils.preparePublicationFile(resultInfo);;
    }
}
