/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.OIDC;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests.IssuerIdentifierServerTests;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OIDCIssuerIdentifier2ServerTests extends IssuerIdentifierServerTests {

    private static final Class<?> thisClass = OIDCIssuerIdentifier2ServerTests.class;

    @BeforeClass
    public static void setupBeforeTest() throws Exception {

        msgUtils.printClassName(thisClass.toString());
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, Constants.OIDC_OP);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, Constants.OIDC_OP);

        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add(Constants.HELLOWORLD_SERVLET);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        testSettings = new TestSettings();
        testOPServer = commonSetUp(OPServerName, "server_orig.xml", Constants.OIDC_OP, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, Constants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(RSServerName, "server_issuerIdentifier.xml", Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, null, null, Constants.OIDC_OP, true, true, tokenType, certType);

        // We use a variable insert for the validationMethod config attribute which the config evaluator will think is invalid
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "validationMethod");

        targetProvider = Constants.OIDCCONFIGSAMPLE_APP;
        goodActions = Constants.BASIC_PROTECTED_RESOURCE_RS_PROTECTED_RESOURCE_ACTIONS;
        //goodActions = Constants.BASIC_RS_PROTECTED_RESOURCE_ACTIONS;

        flowType = Constants.WEB_CLIENT_FLOW;

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + Constants.HELLOWORLD_PROTECTED_RESOURCE);
        targetISSEndpoint = "localhost:" + testOPServer.getHttpDefaultPort().toString() + "/" + Constants.OIDC_ROOT + "/endpoint/" + targetProvider;
        targetISSHttpsEndpoint = "localhost:" + testOPServer.getHttpDefaultSecurePort().toString() + "/" + Constants.OIDC_ROOT + "/endpoint/" + targetProvider;

        setRealmForValidationType(testSettings);

    }

}
