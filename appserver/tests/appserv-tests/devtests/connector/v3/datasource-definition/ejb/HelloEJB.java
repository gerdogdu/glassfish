/*
 * Copyright (c) 2017, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.s1asdev.ejb.ejb30.hello.session3;

import jakarta.ejb.Stateless;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;

import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.persistence.EntityManager;
import javax.naming.InitialContext;
import jakarta.annotation.sql.*;
import javax.sql.DataSource;
import java.sql.Connection;


@DataSourceDefinitions(
        value = {
                //pure annotation that has url and no standard properties
                //test-case for issue-14918
		@DataSourceDefinition(name="java:app/jdbc/appds_driver",
       	 		className="org.apache.derby.jdbc.ClientDriver",
        		user="dbuser",
        		password="dbpassword",
        		transactional=false,
        		url="jdbc:derby://localhost:1527/testdb;create=true"
		),

                @DataSourceDefinition(name = "java:global/env/HelloEJB_DataSource",
                        className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                        minPoolSize = 0,
                        initialPoolSize = 0,
                        user = "APP",
                        password = "APP",
                        databaseName = "hello-ejb-global",
                        properties = {"connectionAttributes=;create=true"}

                ),

                @DataSourceDefinition(name = "java:comp/env/HelloEJB_DataSource",
                        minPoolSize = 0,
                        initialPoolSize = 0,
                        className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                        user = "APP",
                        password = "APP",
                        databaseName = "hello-ejb-comp",
                        properties = {"connectionAttributes=;create=true"}
                ),
                @DataSourceDefinition(name = "java:module/env/HelloEJB_DataSource",
                        minPoolSize = 0,
                        initialPoolSize = 0,
                        className = "org.apache.derby.jdbc.EmbeddedXADataSource",
                        user = "APP",
                        password = "APP",
                        databaseName = "hello-ejb-module",
                        properties = {"connectionAttributes=;create=true"}
                ),
                // annotation + DD where URL specified in annotation wins
                //test-case for issue-14918
		@DataSourceDefinition(name="java:module/env/HelloEJB_DataSource_driver",
        		className="org.apache.derby.jdbc.ClientDriver",
        		user="dbuser",
        		password="dbpassword",
        		transactional=false,
        		url="jdbc:derby://localhost:1527/testdb;create=true"
		)
        }
)

@Stateless
public class HelloEJB implements Hello {

    public void hello() {

        boolean appDSDriver = lookupDataSource("java:app/jdbc/appds_driver", true);
        boolean moduleDSDriver= lookupDataSource("java:module/env/HelloEJB_DataSource_driver", true);

        boolean global = lookupDataSource("java:global/env/HelloEJB_DataSource", true);
        boolean comp = lookupDataSource("java:comp/env/HelloEJB_DataSource", true);
        boolean moduleHelloEjb = lookupDataSource("java:module/env/HelloEJB_DataSource", true);

        boolean globalHelloStatefulEJB = lookupDataSource("java:global/env/HelloStatefulEJB_DataSource", true);
        boolean compHelloStatefulEJB = lookupDataSource("java:comp/env/HelloStatefulEJB_DataSource", false);
        boolean appHelloStatefulEjb = lookupDataSource("java:app/env/HelloStatefulEJB_DataSource", true);

        boolean globalServlet = lookupDataSource("java:global/env/Servlet_DataSource", true);
        boolean compServlet = lookupDataSource("java:comp/env/Servlet_DataSource", false);
        boolean appServletDataSource = lookupDataSource("java:app/env/Servlet_DataSource", true);
        boolean moduleServletDataSource = lookupDataSource("java:module/env/Servlet_DataSource", false);

        boolean globalServlet_DD_DataSource = lookupDataSource("java:global/env/Servlet_DD_DataSource", true);
        boolean compServlet_DD_DataSource = lookupDataSource("java:comp/env/Servlet_DD_DataSource", false);

        boolean globalHelloStateful_DD_DataSource = lookupDataSource("java:global/env/HelloStatefulEJB_DD_DataSource", true);
        boolean compHelloStateful_DD_DataSource = lookupDataSource("java:comp/env/HelloStatefulEJB_DD_DataSource", false);

        boolean globalHello_DD_DataSource = lookupDataSource("java:global/env/HelloEJB_DD_DataSource", true);
        boolean compHello_DD_DataSource = lookupDataSource("java:comp/env/HelloEJB_DD_DataSource", false);


        if (appDSDriver && moduleDSDriver && global && comp && globalHelloStatefulEJB && !compHelloStatefulEJB && globalServlet
                && !compServlet && appServletDataSource && globalServlet_DD_DataSource && !compServlet_DD_DataSource
                && globalHelloStateful_DD_DataSource && !compHelloStateful_DD_DataSource &&
                globalHello_DD_DataSource && compHello_DD_DataSource && appHelloStatefulEjb &&
                moduleHelloEjb && !moduleServletDataSource) {
            System.out.println("HelloEJB successful datasource definitions lookup");
        } else {
            System.out.println("HelloEJB datasource definitions lookup failure");
            throw new RuntimeException("HelloEJB failure");
        }


        System.out.println("In HelloEJB::hello()");
    }

    private boolean lookupDataSource(String dataSourceName, boolean expectSuccess) {
        Connection c = null;
        try {
            InitialContext ic = new InitialContext();
            System.out.println("lookup dataSource : " + dataSourceName);
            DataSource ds = (DataSource) ic.lookup(dataSourceName);
            c = ds.getConnection();
            System.out.println("got connection : " + c);
            return true;
        } catch (Exception e) {
            if(expectSuccess){
                e.printStackTrace();
            }
            return false;
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {
            }
        }
    }
}
