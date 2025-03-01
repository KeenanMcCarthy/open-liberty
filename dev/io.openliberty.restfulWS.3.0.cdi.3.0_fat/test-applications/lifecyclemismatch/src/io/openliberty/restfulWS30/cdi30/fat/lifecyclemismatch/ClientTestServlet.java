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
package io.openliberty.restfulWS30.cdi30.fat.lifecyclemismatch;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;

@WebServlet("/ClientTestServlet")
public class ClientTestServlet extends HttpServlet {

    private static final long serialVersionUID = 7188707949976646396L;

    private static final String moduleName = "lifecyclemismatch";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        PrintWriter pw = resp.getWriter();

        String testMethod = req.getParameter("test");
        if (testMethod == null) {
            pw.write("no test to run");
            return;
        }

        runTest(testMethod, pw, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doGet(req, resp);
    }

    private void runTest(String testMethod, PrintWriter pw, HttpServletRequest req, HttpServletResponse resp) {
        try {
            Method testM = this.getClass().getDeclaredMethod(testMethod, new Class[] { Map.class, StringBuilder.class });
            Map<String, String> m = new HashMap<String, String>();

            Iterator itr = req.getParameterMap().keySet().iterator();

            while (itr.hasNext()) {
                String key = (String) itr.next();
                if (key.indexOf("@") == 0) {
                    m.put(key.substring(1), req.getParameter(key));
                }
            }

            m.put("serverIP", req.getLocalAddr());
            m.put("serverPort", String.valueOf(req.getLocalPort()));

            StringBuilder ret = new StringBuilder();
            testM.invoke(this, m, ret);
            pw.write(ret.toString());

        } catch (Exception e) {
            e.printStackTrace(pw);
        }
    }

    public void testResource(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        String resourcePath = param.get("resourcePath");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        String res = null;
        try {
            System.out.println("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/rest");
            res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/rest")
                            .path(resourcePath)
                            .request()
                            .get(String.class);
        } catch (Exception e) {
            res = "[Error]:" + e.toString();
        } finally {
            c.close();
            ret.append(res);
        }

    }

    public void testProvider(Map<String, String> param, StringBuilder ret) {
        String serverIP = param.get("serverIP");
        String serverPort = param.get("serverPort");
        String resourcePath = param.get("resourcePath");

        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        String res = null;
        try {
            res = c.target("http://" + serverIP + ":" + serverPort + "/" + moduleName + "/rest/ProviderResource")
                            .path(resourcePath)
                            .path("jordan")
                            .request()
                            .get(String.class);
        } catch (Exception e) {
            res = "[Error]:" + e.toString();
        } finally {
            c.close();
            ret.append(res);
        }
    }
}
