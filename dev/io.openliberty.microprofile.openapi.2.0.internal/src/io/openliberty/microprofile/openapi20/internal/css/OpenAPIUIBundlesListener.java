package io.openliberty.microprofile.openapi20.internal.css;

import java.util.Set;

import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * ServiceListener implementation that alerts OpenAPIUIBundleUpdater when all
 * the expected bundles are ready
 */
public class OpenAPIUIBundlesListener implements ServiceListener {

    private final Set<String> expectedBundleNames;

    public OpenAPIUIBundlesListener(Set<String> expectedBundleNames) {
        this.expectedBundleNames = expectedBundleNames;
    }

    @Override
    public void serviceChanged(ServiceEvent event) {
        ServiceReference<Bundle> sb = (ServiceReference<Bundle>) event.getServiceReference();
        String bundleKey = (String) sb.getProperty("web.module.key");
        if (bundleKey != null) {
            System.out.println("bundleKey: " + bundleKey);
            String bundleName = bundleKey.substring(0, bundleKey.indexOf('#'));
            if (expectedBundleNames.contains(bundleName))
                expectedBundleNames.remove(bundleName);
            if (expectedBundleNames.isEmpty())
                OpenAPIUIBundlesUpdater.unlockThread();
        }
    }

}
