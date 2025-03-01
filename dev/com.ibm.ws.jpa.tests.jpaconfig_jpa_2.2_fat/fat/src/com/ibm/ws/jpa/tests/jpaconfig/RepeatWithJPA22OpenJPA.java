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

package com.ibm.ws.jpa.tests.jpaconfig;

import com.ibm.ws.testtooling.jpaprovider.JPAPersistenceProvider;

import componenttest.rules.repeater.RepeatTestAction;

/**
 *
 */
public class RepeatWithJPA22OpenJPA implements RepeatTestAction {
    public static final String ID = "JPA22_OPENJPA";

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String toString() {
        return "Switch to JPA Container 2.2 feature and use OpenJPA for JPA persistence provider";
    }

    @Override
    public void setup() throws Exception {
        FATSuite.repeatPhase = "openjpa22-cfg.xml";
        FATSuite.provider = JPAPersistenceProvider.DEFAULT;
    }

    @Override
    public String getID() {
        return ID;
    }
}
