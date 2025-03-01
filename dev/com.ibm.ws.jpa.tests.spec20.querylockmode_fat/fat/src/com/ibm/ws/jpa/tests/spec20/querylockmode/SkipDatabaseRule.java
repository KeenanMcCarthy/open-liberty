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

package com.ibm.ws.jpa.tests.spec20.querylockmode;

import java.util.regex.Pattern;

import org.junit.internal.AssumptionViolatedException;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *
 */
public class SkipDatabaseRule implements TestRule {

    private String database;

    public void setDatabase(String databaseName) {
        this.database = databaseName;
    }

    public boolean isSkipping() {
        return (database != null
                && !Pattern.compile("derby", Pattern.CASE_INSENSITIVE).matcher(database).find());
    }

    @Override
    public Statement apply(Statement arg0, Description arg1) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                System.out.println("Checking DB to skip tests");
                if (isSkipping()) {
                    throw new AssumptionViolatedException("Database is not Derby. Skipping test!");
                } else {
                    System.out.println("Not Skipping");
                    arg0.evaluate();
                }
            }
        };
    }

}
