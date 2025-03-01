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

package io.openliberty.ejbcontainer.fat.timer.auto.np.web;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
public abstract class AbstractServlet extends FATServlet {
    public abstract void setup() throws Exception;

    public abstract void cleanup() throws Exception;
}
