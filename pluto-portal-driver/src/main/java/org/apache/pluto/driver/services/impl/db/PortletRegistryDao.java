/*
 * Copyright 2003,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.pluto.driver.services.impl.db;

import org.apache.pluto.driver.services.portal.PortletApplicationConfig;
import org.apache.pluto.driver.services.portal.PortletWindowConfig;
import org.apache.pluto.optional.db.support.AbstractDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.Iterator;
import java.sql.*;

/**
 * TODO JavaDoc
 *
 * @author <a href="mailto:ddewolf@apache.org">David H. DeWolf</a>:
 * @version 1.0
 * @since Nov 24, 2005
 */
public class PortletRegistryDao extends AbstractDao {

    private static final Log LOG = LogFactory.getLog(PortletRegistryDao.class);

    public PortletRegistryDao(DataSource dataSource) {
        super(dataSource);
    }

    public void seedPortletApplications(Collection portletApplications)
    throws SQLException {

        boolean autoCommit = false;
        Connection conn = null;

        PreparedStatement appTestStmt = null;
        PreparedStatement portletTestStmt = null;
        PreparedStatement appStmt = null;
        PreparedStatement pStmt = null;


        try {
            conn = getConnection();
            autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            appTestStmt = conn.prepareStatement(TEST_APP_SQL);
            portletTestStmt = conn.prepareStatement(TEST_PORTLET_SQL);

            appStmt = conn.prepareStatement(CREATE_APP_SQL);
            pStmt = conn.prepareStatement(CREATE_PORTLET_SQL);

            Iterator it = portletApplications.iterator();
            while(it.hasNext()) {
                PortletApplicationConfig app = (PortletApplicationConfig)it.next();

                appTestStmt.setString(1, app.getContextPath());
                ResultSet rs = appTestStmt.executeQuery();

                if(!rs.next() || rs.getInt(1) < 1) {
                    if(LOG.isDebugEnabled()) {
                        LOG.debug("Auto-Publishing Portlet Application: "+app.getContextPath());
                    }
                    appStmt.setString(1, app.getContextPath());
                    appStmt.addBatch();
                }

                rs.close();

                Iterator portlets = app.getPortlets().iterator();
                while(portlets.hasNext()) {
                    PortletWindowConfig window = (PortletWindowConfig)portlets.next();
                    portletTestStmt.setString(1, app.getContextPath());
                    portletTestStmt.setString(2, window.getPortletName());
                    rs = portletTestStmt.executeQuery();

                    if(!rs.next() || rs.getInt(1) < 1) {
                        if(LOG.isDebugEnabled()) {
                            LOG.debug("Auto-Publishing Portlet: "+app.getContextPath() + " --> " + window.getPortletName());
                        }

                        pStmt.setString(1, app.getContextPath());
                        pStmt.setString(2, window.getPortletName());
                        pStmt.addBatch();
                    }
               }

                appStmt.executeBatch();
                pStmt.executeBatch();
            }
            conn.commit();
        }
        catch(SQLException sql) {
            if(conn != null)
                conn.rollback();
            LOG.error("Unable to seed portlet application.", sql);
            throw sql;
        }
        finally {
            if(conn != null)
                conn.setAutoCommit(autoCommit);
            cleanup(null, appTestStmt, null);
            cleanup(null, pStmt, null);
            cleanup(conn, appStmt, null);
        }
    }

    protected Object instantiate(ResultSet rs) throws SQLException {
        return new Object();
    }

    private static final String TEST_APP_SQL =
        "SELECT count(*) FROM portlet_app WHERE app_context = ?";

    private static final String TEST_PORTLET_SQL =
        "SELECT count(*) " +
        "  FROM portlet p, portlet_app pa " +
        " WHERE pa.app_context = ? " +
        "   AND p.portlet_app_id = pa.portlet_app_id " +
        "   AND p.portlet_name = ? ";

    private static final String CREATE_APP_SQL =
        "INSERT INTO portlet_app (app_context) VALUES (?)";

    public static final String CREATE_PORTLET_SQL =
        "INSERT INTO portlet (portlet_app_id, portlet_name) VALUES (" +
        "       (SELECT portlet_app_id FROM portlet_app WHERE app_context = ?), ?" +
        ")";
}