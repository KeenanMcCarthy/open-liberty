/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.client.fat;

import org.junit.ClassRule;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.ibm.ws.jaxrs20.client.fat.test.BasicClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.ClientContextInjectionTest;
import com.ibm.ws.jaxrs20.client.fat.test.ComplexClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.HandleResponsesTest;
import com.ibm.ws.jaxrs20.client.fat.test.IBMJson4JProvidersTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20ClientAsyncInvokerTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20ClientInvocationTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20ClientSyncInvokerTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRS20WithClientFeatureEnabledTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClient100ContinueTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientCallbackTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientLtpaTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLDefaultTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLFiltersTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLProxyAuthTest;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLTestNoLibertySSLCfg;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientSSLTestNoLibertySSLFeature;
import com.ibm.ws.jaxrs20.client.fat.test.JAXRSClientStandaloneTest;
import com.ibm.ws.jaxrs20.client.fat.test.JacksonProvidersTest;
import com.ibm.ws.jaxrs20.client.fat.test.JsonPProvidersTest;
import com.ibm.ws.jaxrs20.client.fat.test.PathParamTest;
import com.ibm.ws.jaxrs20.client.fat.test.ProxyClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.ThirdpartyJerseyClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.TimeoutClientTest;
import com.ibm.ws.jaxrs20.client.fat.test.XmlBindingTest;

import componenttest.custom.junit.runner.AlwaysPassesTest;
import componenttest.rules.repeater.FeatureReplacementAction;
import componenttest.rules.repeater.JakartaEE9Action;
import componenttest.rules.repeater.RepeatTests;

@RunWith(Suite.class)
@SuiteClasses({
                AlwaysPassesTest.class,
                BasicClientTest.class,
                ClientContextInjectionTest.class,
                ComplexClientTest.class,
                HandleResponsesTest.class,
                IBMJson4JProvidersTest.class,
                JacksonProvidersTest.class,
                JAXRS20ClientAsyncInvokerTest.class,
                JAXRS20ClientInvocationTest.class,
                JAXRS20ClientSyncInvokerTest.class,
                JAXRS20WithClientFeatureEnabledTest.class,
                JAXRSClient100ContinueTest.class,
                JAXRSClientCallbackTest.class,
                JAXRSClientLtpaTest.class,
                JAXRSClientSSLProxyAuthTest.class,
                JAXRSClientSSLDefaultTest.class,
                JAXRSClientSSLFiltersTest.class,
                //JAXRSClientSSLTest.class,
                JAXRSClientSSLTestNoLibertySSLCfg.class,
                JAXRSClientSSLTestNoLibertySSLFeature.class,
                JAXRSClientStandaloneTest.class,
                JsonPProvidersTest.class,
                PathParamTest.class,
                ProxyClientTest.class,
                ThirdpartyJerseyClientTest.class,
                TimeoutClientTest.class,
                XmlBindingTest.class
})
public class FATSuite {
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModificationInFullMode()
    .andWith(FeatureReplacementAction.EE8_FEATURES().withID("JAXRS-2.1"))
    .andWith(new JakartaEE9Action().alwaysAddFeature("jsonb-2.0"));
}
