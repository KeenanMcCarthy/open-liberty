/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package jpahelper.databasemanagement;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64.Encoder;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.UserTransaction;

/**
 *
 */
@WebServlet(urlPatterns = { "/DMS" })
public class DatabaseManagementServlet extends HttpServlet {
    @Resource
    private UserTransaction tx;

    @Resource(lookup = "jdbc/JPA_JTA_DS")
    private DataSource dsJta;

    @Resource(lookup = "jdbc/JPA_NJTA_DS")
    private DataSource dsRl;

    private boolean propsRead = false;
    private String productName = null;
    private String productVersion = null;
    private String jdbcDriverVersion = null;
    private String jdbcURL = null;
    private String jdbcUser = null;

    @PostConstruct
    private void initialise() {
        try {
            System.out.println("Initializing DatabaseManagementServlet");
            getInfo();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        execRequest(req, resp);
    }

    private void execRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String command = req.getParameter("command");

        if ("EXECDDL".equals(command)) {
            executeDDL(req, resp);
        } else if ("GETINFO".equals(command)) {
            Properties properties = getInfo();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(properties);
            oos.flush();
            oos.close();

            final Encoder base64Encoder = java.util.Base64.getEncoder();
            final String base64EncodedData = base64Encoder.encodeToString(baos.toByteArray());

            final PrintWriter pw = resp.getWriter();
            pw.println(base64EncodedData);

        } else {
            resp.sendError(400);
        }
    }

    private Properties getInfo() throws ServletException, IOException {
        Connection conn = null;
        Properties properties = null;
        try {
            System.out.println("DatabaseManagement servicing getInfo request...");
            if (!propsRead) {
                conn = dsRl.getConnection();
                final DatabaseMetaData dbMeta = conn.getMetaData();

                productName = dbMeta.getDatabaseProductName();
                productVersion = dbMeta.getDatabaseProductVersion();
                jdbcDriverVersion = dbMeta.getDatabaseProductVersion();
                jdbcURL = dbMeta.getURL();
                jdbcUser = dbMeta.getUserName();

                propsRead = true;
            }

            properties = new Properties();
            properties.put("dbproduct_name", productName);
            properties.put("dbproduct_version", productVersion);
            properties.put("jdbcdriver_version", jdbcDriverVersion);
            properties.put("jdbc_url", jdbcURL);
            properties.put("jdbc_username", jdbcUser);

            return properties;

//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
//            ObjectOutputStream oos = new ObjectOutputStream(baos);
//            oos.writeObject(properties);
//            oos.flush();
//            oos.close();
//
//            final Encoder base64Encoder = java.util.Base64.getEncoder();
//            final String base64EncodedData = base64Encoder.encodeToString(baos.toByteArray());
//
//            final PrintWriter pw = resp.getWriter();
//            pw.println(base64EncodedData);
        } catch (SQLException e) {
            throw new ServletException(e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (Throwable t) {
                }
            }
            System.out.println("DatabaseManagement getInfo request service complete.");
            System.out.println("Properties: \n" + properties);
        }

    }

    private void executeDDL(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final String ddlScriptName = req.getParameter("ddl.script.name");
        final boolean swallowErrors = Boolean.valueOf(req.getParameter("swallow.errors"));
        final String overrideDefaultSchema = req.getParameter("override.default.schema");
        final ClassLoader cl = DatabaseManagementServlet.class.getClassLoader();
        final URL ddlScriptURL = cl.getResource(ddlScriptName);

        if (ddlScriptURL == null) {
            throw new ServletException("Unable to locate resource " + ddlScriptName);
        }

        StringBuilder sbHeader = new StringBuilder();
        sbHeader.append("\n");
        sbHeader.append("Database Management Servlet DDL Execution:\n");
        sbHeader.append("ddlScriptName = " + ddlScriptName + "\n");
        sbHeader.append("swallowErrors = " + swallowErrors + "\n");
        sbHeader.append("overrideDefaultSchema = " + overrideDefaultSchema + "\n");
        System.out.println(sbHeader);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final InputStream is = ddlScriptURL.openStream();
        final byte[] buffer = new byte[1024];
        int bytesRead = 0;

        while ((bytesRead = is.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        is.close();

        final String ddl = baos.toString();
        final String[] commands = breakApartDDLIntoCommands(ddl);

        final PrintWriter pw = resp.getWriter();
        try {
            tx.begin();
            final Connection conn = dsJta.getConnection();
            final DatabaseMetaData dbMeta = conn.getMetaData();
            dumpDBMeta(dbMeta);

            final Statement stmt = conn.createStatement();
            final String defaultSchemaName = (overrideDefaultSchema == null || "".equals(overrideDefaultSchema.trim())) ? //
                            sanitize(dbMeta.getUserName()) : // Usually the default schema name
                            sanitize(overrideDefaultSchema); // But always allow for test client to override

            int totalCount = 0, successCount = 0;
            int cmdIdx = 0;
            for (String command : commands) {
                final String sql = command.replace("${schemaname}", defaultSchemaName).trim();
                if ("".equals(sql)) {
                    continue;
                }
                if (sql.startsWith("#")) {
                    // Commented out sql line
                    continue;
                }

                cmdIdx++;

                totalCount++;
                try {
                    System.out.println(cmdIdx + "] Executing: " + sql);
                    pw.println(cmdIdx + "] Executing: " + sql);

                    if (sql.toLowerCase().startsWith("select")) {
                        ResultSet rs = stmt.executeQuery(sql);
                        System.out.println("Successful query, Result Set:");
                        pw.println("  <br>  Successful query, Result Set:");
                        final ResultSetMetaData rsmd = rs.getMetaData();
                        final int colCount = rsmd.getColumnCount();
                        int index = 0;
                        while (rs.next()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(++index).append(": ");

                            for (int col = 1; col <= colCount; col++) {
                                if (col != 1) {
                                    sb.append(", ");
                                }
                                sb.append(rs.getObject(col));
                            }

                            pw.println(sb);
                        }
                    } else if (stmt.execute(sql)) {
                        System.out.println("Successful execution, Result Set:");
                        pw.println("  <br>  Successful execution, Result Set:");

                        final ResultSet rs = stmt.getResultSet();
                        final ResultSetMetaData rsmd = rs.getMetaData();
                        final int colCount = rsmd.getColumnCount();
                        int index = 0;
                        while (rs.next()) {
                            StringBuilder sb = new StringBuilder();
                            sb.append(++index).append(": ");

                            for (int col = 1; col <= colCount; col++) {
                                if (col != 1) {
                                    sb.append(", ");
                                }
                                sb.append(rs.getObject(col));
                            }

                            pw.println(sb);
                        }
                    } else {
                        System.out.println("Successful execution, update count = " + stmt.getUpdateCount());
                        pw.println("  <br>  Successful execution, update count = " + stmt.getUpdateCount());
                    }

                    successCount++;
                } catch (Exception e) {
                    if (!swallowErrors) {
                        pw.println("  <br>  SQL Execution failed: " + e);
                    }
                    if (!sql.startsWith("DROP")) {
                        System.out.println("SQL Execution failed: " + e);
                        e.printStackTrace();
                    }

                } finally {
                    pw.println("  <br>  ");
                }
            }

            System.out.println("SQL Executed: Total = " + totalCount + " Successful = " + successCount);

            tx.commit();
        } catch (Exception e) {
            throw new ServletException(e);
        } finally {
            pw.close();
        }
    }

    private String[] breakApartDDLIntoCommands(String ddl) {
        return ddl.split(";\n");

//        return null;
    }

    /**
     * Sanitize input to thwart sql injection attacks.
     *
     * @param in
     * @return
     */
    private String sanitize(String in) {
        if (in == null) {
            return null;
        }

        return in.replaceAll("[^A-Za-z0-9_]", "");
    }

    private void dumpDBMeta(DatabaseMetaData dbMeta) throws SQLException {
        final String dbProductName = dbMeta.getDatabaseProductName();
        final String dbProductVersion = dbMeta.getDatabaseProductVersion();
        final String jdbcDriverVersion = dbMeta.getDriverVersion();
        final String jdbcURL = dbMeta.getURL();
        final String username = dbMeta.getUserName();

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        sb.append("################################################################################\n");
        sb.append("DBMeta Dump:\n");
        sb.append("DB Product Name: ").append(dbProductName).append("\n");
        sb.append("DB Product Version: ").append(dbProductVersion).append("\n");
        sb.append("JDBC Driver Version: ").append(jdbcDriverVersion).append("\n");
        sb.append("DB URL: ").append(jdbcURL).append("\n");
        sb.append("DB Username: ").append(username).append("\n");

        // DB2 on Z requires stored procedures to be installed in order for schemas to be introspected.
        // Because it generates FFDC should this fail (complicating problem determination), we will
        // not attempt to dump schemas on DB2/Z
        if (dbProductName != null && "DB2".equals(dbProductName) && dbProductVersion != null && dbProductVersion.startsWith("DSN")) {
            sb.append("DB/2 on z/OS detected -- Not Dumping Schemas");
        } else {
            sb.append("DB Schemas:\n");
            try {
                ResultSet schemas = dbMeta.getSchemas();
                while (schemas.next()) {
                    sb.append("  ").append(schemas.getString("TABLE_SCHEM")).append("\n");
                }
            } catch (Throwable t) {
                // Ouch
                sb.append("   DB SCHEMAS NOT AVAILABLE -- " + t.getMessage());
                t.printStackTrace();
            }
        }

        sb.append("################################################################################\n");
        System.out.println(sb);
    }

}
