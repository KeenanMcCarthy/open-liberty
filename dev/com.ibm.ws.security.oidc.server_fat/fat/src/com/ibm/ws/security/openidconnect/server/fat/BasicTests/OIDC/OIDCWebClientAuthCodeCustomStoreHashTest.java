/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.OIDC;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings.StoreType;
import com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests.genericWebClientAuthCodeCommonTest;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "com.ibm.ws.security.registry.EntryNotFoundException" }) // Defect 261748
@RunWith(FATRunner.class)
public class OIDCWebClientAuthCodeCustomStoreHashTest extends genericWebClientAuthCodeCommonTest {

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    private static final Class<?> thisClass = OIDCWebClientAuthCodeCustomStoreHashTest.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {
        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test, run user feature");
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKZ0001I.*" + Constants.OIDCCONFIGMONGODB_START_APP);
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_DERBY_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        testSettings = new TestSettings();
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.server-1.0_fat", "server_customstore.xml",
                                   Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, Constants.USE_MONGODB, extraMsgs, null,
                                   Constants.OIDC_OP, true, true, tokenType, certType, Constants.JUNIT_REPORTING);

        testSettings.setClientName("dclient01");
        testSettings.setClientID("dclient01");
        testSettings.setAuthorizeEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(),
                                                                  Constants.ENDPOINT_TYPE, Constants.OIDCCONFIGDERBY_APP, Constants.AUTHORIZE_ENDPOINT));
        testSettings.setTokenEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE,
                                                              Constants.OIDCCONFIGDERBY_APP, Constants.TOKEN_ENDPOINT));
        testSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(),
                                                                              Constants.OIDC_TAI_ROOT, Constants.SSODEMO));
        testSettings.setStoreType(StoreType.CUSTOM);
        setForClientSecretHash();
    }

}
