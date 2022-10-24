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
package io.openliberty.microprofile.openapi20.internal.css;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * ServiceListener implementation that alerts OpenAPIUIBundleUpdater when all
 * the expected bundles are ready
 */
public class OpenAPIUIBundlesListener implements ServiceListener {

    private ConcurrentHashMap<String, Boolean> expectedBundleNames;
    private CountDownLatch countDownLatch;

    public OpenAPIUIBundlesListener(ConcurrentHashMap<String, Boolean> expectedBundleNames, CountDownLatch countDownLatch, ServiceReference<Bundle>[] refs) {
        this.expectedBundleNames = expectedBundleNames;
        this.countDownLatch = countDownLatch;
        checkExistingServices(refs);
    }

    private void removeIfExpectedBundle(ServiceReference<Bundle> ref) {
        String bundleKey = (String) ref.getProperty("web.module.key");
        String bundleName = bundleKey.substring(0, bundleKey.indexOf('#'));
        if (expectedBundleNames.contains(bundleName)) {
            expectedBundleNames.remove(bundleName);
            countDownLatch.countDown();
        }
    }

    private void checkExistingServices(ServiceReference<Bundle>[] refs) {
        if (refs != null) {
            for (ServiceReference<Bundle> ref : refs) {
                removeIfExpectedBundle(ref);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void serviceChanged(ServiceEvent event) {
        ServiceReference<Bundle> ref = (ServiceReference<Bundle>) event.getServiceReference();
        removeIfExpectedBundle(ref);
    }
}
