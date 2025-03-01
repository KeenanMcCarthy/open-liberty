/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cdi.impl;

import java.net.URL;

import org.jboss.weld.bootstrap.spi.BeansXml;
import org.osgi.service.component.annotations.Component;

import com.ibm.ws.cdi.internal.interfaces.BeansXmlParser;
import com.ibm.ws.cdi.internal.interfaces.WebSphereCDIDeployment;

/**
 *
 */
@Component(name = "com.ibm.ws.cdi.impl.CDI20BeansXmlParserImpl", service = { BeansXmlParser.class }, property = { "service.vendor=IBM" })
public class CDI20BeansXmlParserImpl implements BeansXmlParser {

    /** {@inheritDoc} */
    @Override
    public BeansXml parse(WebSphereCDIDeployment cdiDeployment, URL beansXmlUrl) {
        return cdiDeployment.getBootstrap().parse(beansXmlUrl);
    }

}
