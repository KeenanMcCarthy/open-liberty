/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.openapi20.internal.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiPredicate;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.eclipse.microprofile.openapi.models.servers.Server;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.microprofile.openapi20.internal.validation.OASValidationResult;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidationResult.ValidationEvent.Severity;
import io.openliberty.microprofile.openapi20.internal.validation.OASValidator;
import io.openliberty.microprofile.openapi20.internal.validation.ValidatorUtils;
import io.smallrye.openapi.api.constants.OpenApiConstants;
import io.smallrye.openapi.api.models.OpenAPIImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.info.InfoImpl;
import io.smallrye.openapi.runtime.io.Format;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.io.info.InfoReader;

public class OpenAPIUtils {
    private static final TraceComponent tc = Tr.register(OpenAPIUtils.class);

    /**
     * The createBaseOpenAPIDocument method creates a default OpenAPI model object.
     *
     * @return OpenAPI
     * The default OpenAPI model
     */
    @Trivial
    public static OpenAPI createBaseOpenAPIDocument() {
        /*
         * The default OpenAPI document needs to be identical to the one that would be generated by the SmallRye
         * implementation.
         */
        OpenAPI openAPI = new OpenAPIImpl();
        openAPI.setOpenapi(OpenApiConstants.OPEN_API_VERSION);
        openAPI.paths(new PathsImpl());
        openAPI.info(new InfoImpl().title(Constants.DEFAULT_OPENAPI_DOC_TITLE).version(Constants.DEFAULT_OPENAPI_DOC_VERSION));
        if (LoggingUtils.isEventEnabled(tc)) {
            Tr.event(tc, "Created base OpenAPI document");
        }
        return openAPI;
    }

    /**
     * The getSerializedJsonDocument method is generates an OpenAPI document from the specified model in the specified
     * format.
     *
     * @param openapi
     *     The OpenAPI model
     * @param format
     *     The format of the generated document
     * @return String
     * The generated OpenAPI document in JSON format
     */
    @Trivial
    @FFDCIgnore(IOException.class)
    public static String getOpenAPIDocument(final OpenAPI openAPIModel, final Format format) {
        // Create the variable to return
        String oasResult = null;

        // Make sure that we have a valid document
        if (openAPIModel != null) {
            try {
                oasResult = OpenApiSerializer.serialize(openAPIModel, format);
            } catch (IOException e) {
                if (LoggingUtils.isEventEnabled(tc)) {
                    Tr.event(tc, "Failed to serialize OpenAPI document: " + e.getMessage());
                }
            }
        }

        return oasResult;
    }

    /**
     * The validateDocument method validates the generated OpenAPI model and logs any warnings/errors.
     *
     * @param document
     *     The OpenAPI document (model) to validate
     */
    @Trivial
    public static void validateDocument(OpenAPI document) {
        final OASValidator validator = new OASValidator();
        final OASValidationResult result = validator.validate(document);
        final StringBuilder sbError = new StringBuilder();
        final StringBuilder sbWarnings = new StringBuilder();
        if (result.hasEvents()) {
            result.getEvents().stream().forEach(v -> {
                final String message = ValidatorUtils.formatMessage(ValidationMessageConstants.VALIDATION_MESSAGE, v.message, v.location);
                if (v.severity == Severity.ERROR) {
                    sbError.append("\n - " + message);
                } else if (v.severity == Severity.WARNING) {
                    sbWarnings.append("\n - " + message);
                }
            });

            String errors = sbError.toString();
            if (!errors.isEmpty()) {
                Tr.error(tc, MessageConstants.OPENAPI_DOCUMENT_VALIDATION_ERROR, errors + "\n");
            }

            String warnings = sbWarnings.toString();
            if (!warnings.isEmpty()) {
                Tr.warning(tc, MessageConstants.OPENAPI_DOCUMENT_VALIDATION_WARNING, warnings + "\n");
            }
        }
    }

    /**
     * The containsServersDefinition method checks whether the specified OpenAPI model defines any servers.
     *
     * @param openAPI
     *     The OpenAPI model
     * @return boolean
     * True iff the OpenAPI model already defines servers
     */
    @Trivial
    public static boolean containsServersDefinition(final OpenAPI openAPIModel) {
        // Create the variable to return
        boolean containsServers = false;

        // Return true if the model contains at least one server definition
        if (openAPIModel != null && openAPIModel.getServers() != null && openAPIModel.getServers().size() > 0) {
            containsServers = true;
        }

        return containsServers;
    }

    /**
     * The getOpenAPIModelServers method creates a list of server defintions based on the specified ServerInfo object
     * and application path.
     *
     * @param serverInfo
     *     The ServerInfo object to use when creating the new servers model
     * @param contextRoot
     *     The contextRoot for the application that is being processed
     * @return List<Server>
     * The list of Server objects
     */
    public static List<Server> getOpenAPIModelServers(final ServerInfo serverInfo, final String applicationPath) {
        // Create the variable to return
        List<Server> servers = new ArrayList<>();

        final int httpPort = serverInfo.getHttpPort();
        final int httpsPort = serverInfo.getHttpsPort();
        final String host = serverInfo.getHost();

        if (httpPort > 0) {
            String port = httpPort == 80 ? Constants.STRING_EMPTY : (Constants.STRING_COLON + httpPort);
            String url = Constants.SCHEME_HTTP + host + port;
            if (applicationPath != null) {
                url += applicationPath;
            }
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Adding OpenAPI model server: " + url);
            }
            Server server = OASFactory.createServer();
            server.setUrl(url);
            servers.add(server);
        }

        if (httpsPort > 0) {
            String port = httpsPort == 443 ? Constants.STRING_EMPTY : (Constants.STRING_COLON + httpsPort);
            String secureUrl = Constants.SCHEME_HTTPS + host + port;
            if (applicationPath != null) {
                secureUrl += applicationPath;
            }
            if (LoggingUtils.isEventEnabled(tc)) {
                Tr.event(tc, "Adding OpenAPI model server: " + secureUrl);
            }
            Server secureServer = OASFactory.createServer();
            secureServer.setUrl(secureUrl);
            servers.add(secureServer);
        }

        return servers;
    }

    /**
     * The isDefaultOpenApiModel method checks whether the OpenAPI model specified is a default OpenAPI model generated
     * by the SmallRye implementation.
     *
     * @param model
     *     The OpenAPI model to check
     * @return boolean
     * True if the OpenAPI model is a default model, false otherwise.
     */
    public static boolean isDefaultOpenApiModel(OpenAPI model) {

        // Create the variable to return
        boolean isDefault = false;

        /*
         * The SmallRye implementation generates an OpenAPI model regardless of whether the application contains any
         * OAS or JAX-RS annotations. The default model that is generated is of the form:
         *
         * openapi: 3.0.1
         * info:
         * title: Generated API
         * version: "1.0"
         * servers:
         * - url: http://localhost:8010
         * - url: https://localhost:8020
         * paths: {}
         *
         * This makes detecting whether the application is an OAS application a little more problematic. We need to
         * introspect the generated OpenAPI model object to determine whether it is a real model instance or just a
         * default.
         */
        if (model.getOpenapi().equals(OpenApiConstants.OPEN_API_VERSION)
            && model.getInfo() != null
            && model.getInfo().getContact() == null
            && model.getInfo().getDescription() == null
            && model.getInfo().getLicense() == null
            && model.getInfo().getTermsOfService() == null
            && model.getInfo().getTitle().equals(Constants.DEFAULT_OPENAPI_DOC_TITLE)
            && model.getInfo().getVersion().equals(Constants.DEFAULT_OPENAPI_DOC_VERSION)
            && model.getPaths() != null
            && model.getPaths().getPathItems() == null
            && model.getComponents() == null
            && model.getExtensions() == null
            && model.getExternalDocs() == null
            && model.getSecurity() == null
            && model.getServers() == null
            && model.getTags() == null) {
            isDefault = true;
        }

        return isDefault;
    }

    private OpenAPIUtils() {
        // This class is not meant to be instantiated.
    }

    /**
     * Create a shallow copy of an OpenAPI model
     * <p>
     * This allows us to replace the servers and info sections without modifying the original model.
     *
     * @param model the original OpenAPI model
     * @return shallow copy of {@code model}
     */
    public static OpenAPI shallowCopy(OpenAPI model) {
        OpenAPI result = OASFactory.createOpenAPI();

        // Shallow copy each part
        result.setOpenapi(model.getOpenapi());
        result.setComponents(model.getComponents());
        result.setExtensions(model.getExtensions());
        result.setExternalDocs(model.getExternalDocs());
        result.setInfo(model.getInfo());
        result.setPaths(model.getPaths());
        result.setSecurity(model.getSecurity());
        result.setServers(model.getServers());
        result.setTags(model.getTags());

        return result;
    }

    @FFDCIgnore(JsonProcessingException.class)
    public static Info getConfiguredInfo(Config config) {
        Optional<String> infoJson = config.getOptionalValue(Constants.MERGE_INFO_CONFIG, String.class);
        if (!infoJson.isPresent()) {
            return null;
        }

        try {
            JsonNode infoNode = new ObjectMapper().readTree(infoJson.get());
            Info info = InfoReader.readInfo(infoNode);
            if (info.getTitle() != null && info.getVersion() != null) {
                return info;
            } else {
                Tr.warning(tc, MessageConstants.OPENAPI_MERGE_INFO_INVALID_CWWKO1664W, Constants.MERGE_INFO_CONFIG, infoJson.get());
                return null;
            }
        } catch (JsonProcessingException ex) {
            Tr.warning(tc, MessageConstants.OPENAPI_MERGE_INFO_PARSE_ERROR_CWWKO1665W, Constants.MERGE_INFO_CONFIG, infoJson.get(), ex.toString());
            return null;
        }
    }

    /**
     * Check whether all elements of {@code collection} are equal to each other using the given equality function
     * <p>
     * Actually assumes that equals is implemented properly and just checks that the first element is equal to all others
     * <p>
     * If {@code collection} contains less than two elements, this method will always return {@code true}.
     *
     * @param <T> the element type
     * @param collection the collection of elements to test for equality
     * @param comparator the function to use to test equality
     * @return {@code true} if all elements of {@code collection} are equal, {@code false} otherwise
     */
    public static <T> boolean allEqual(Collection<? extends T> collection, BiPredicate<? super T, ? super T> comparator) {
        Iterator<? extends T> i = collection.iterator();
        if (!i.hasNext()) {
            return true;
        }

        T first = i.next();
        while (i.hasNext()) {
            if (!equals(first, i.next(), comparator)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Tests if two objects are equal, using {@code comparator} to test their equality if both {@code a} and {@code b} are not {@code null}.
     *
     * @param <T> the type of {@code a} and {@code b}
     * @param a the first object
     * @param b the second object
     * @param comparator the comparison function
     * @return {@code true} if {@code a} and {@code b} are equal, {@code false} otherwise
     */
    public static <T> boolean equals(T a, T b, BiPredicate<? super T, ? super T> comparator) {
        if (a == null) {
            return b == null ? true : false;
        } else {
            return b == null ? false : comparator.test(a, b);
        }
    }

    /**
     * Converts {@code null} to an empty map
     *
     * @param in a map, or {@code null}
     * @return an empty map if {@code in} is {@code null}, otherwise {@code in}
     */
    @Trivial
    public static <K, V> Map<K, V> notNull(Map<K, V> in) {
        if (in == null) {
            return Collections.emptyMap();
        } else {
            return in;
        }
    }

    /**
     * Converts {@code null} to an empty list
     *
     * @param in a list, or {@code null}
     * @return an empty list if {@code in} is {@code null}, otherwise {@code in}
     */
    @Trivial
    public static <V> List<V> notNull(List<V> in) {
        if (in == null) {
            return Collections.emptyList();
        } else {
            return in;
        }
    }
}
