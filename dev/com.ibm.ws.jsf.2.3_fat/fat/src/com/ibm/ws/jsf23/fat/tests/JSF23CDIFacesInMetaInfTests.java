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
package com.ibm.ws.jsf23.fat.tests;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.ws.jsf23.fat.CDITestBase;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is one of four CDI test applications, with configuration loaded in the following manner:
 * CDIFacesInMetaInf - META-INF/faces-config.xml
 *
 * We're extending CDITestBase, which has common test code.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JSF23CDIFacesInMetaInfTests extends CDITestBase {

    @Server("jsf23CDIFacesInMetaInfServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, "CDIFacesInMetaInf.war",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.factory",
                                      "com.ibm.ws.jsf23.fat.cdi.common.beans.injected",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories",
                                      "com.ibm.ws.jsf23.fat.cdi.common.managed.factories.client.window");

        // Start the server and use the class name so we can find logs easily.
        server.startServer(JSF23CDIFacesInMetaInfTests.class.getSimpleName() + ".log");

    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom Navigation Handler
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testNavigationHandlerInjection_CDIFacesInMetaInf() throws Exception {
        testNavigationHandlerInjectionByApp("CDIFacesInMetaInf", server);
    }

    /**
     * Test to ensure that CDI 2.0 injection works for a custom EL Resolver
     * Field and Method injection, but no Constructor injection.
     * Also tested are use of request and session scope and use of qualifiers.
     *
     * @throws Exception. Content of the response should show if a specific injection failed.
     *
     */
    @Test
    public void testELResolverInjection_CDIFacesInMetaInf() throws Exception {
        testELResolverInjectionByApp("CDIFacesInMetaInf", server);
    }

    /**
     * Test method and field injection for Custom resource handler. No intercepter or constructor injection on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomResourceHandlerInjections_CDIFacesInMetaInf() throws Exception {
        testCustomResourceHandlerInjectionsByApp("CDIFacesInMetaInf", server);

    }

    /**
     * Test method and field injection on custom state manager. No intercepter or constructor tests on this.
     *
     * Would like to do something more than look for message in logs, a future improvement.
     *
     * @throws Exception
     */
    @Test
    public void testCustomStateManagerInjections_CDIFacesInMetaInf() throws Exception {
        testCustomStateManagerInjectionsByApp("CDIFacesInMetaInf", server);
    }

    /**
     * Test that hits most of the managed factory classes, and system-event listener, and phase-listener. See faces-config.xml for details.
     * Most factories use delegate constructor method, so they are limited to tested basic field and method injection.
     *
     * Test Field and Method injection in system-event and phase listeners. No Constructor injection.
     *
     * Tests also use app scope as
     * request/session are not available to these managed classes that I can tell.
     *
     * @throws Exception
     */
    @Test
    public void testFactoryAndOtherScopeInjections_CDIFacesInMetaInf() throws Exception {
        testFactoryAndOtherAppScopedInjectionsByApp("CDIFacesInMetaInf", server);
    }

}
