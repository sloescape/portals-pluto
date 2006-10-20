/*
 * Copyright 2004 The Apache Software Foundation
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
package org.apache.pluto.testsuite;

import javax.portlet.PortletConfig;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:ddewolf@apache.org">David H. DeWolf</a>
 * @author <a href="mailto:zheng@apache.org">ZHENG Zhong</a>
 * @version 1.0
 * @since Mar 9, 2005
 */
public class NoOpTest implements PortletTest {
	
	/** The test configuration. */
    private TestConfig config = null;
    
    
    // Constructor -------------------------------------------------------------
    
    /**
     * Default no-arg constructor.
     */
    public NoOpTest() {
    	// Do nothing.
    }
    
    
    // PortletTest Impl --------------------------------------------------------
    
    public String getTestSuiteName() {
        return "NoOpTest";
    }

    public Map getRenderParameters(PortletRequest request) {
        return new HashMap();
    }
    
    public TestResults doTest(PortletConfig config,
                              PortletContext context,
                              PortletRequest request,
                              PortletResponse response) {
        return new TestResults("None");
    }

    public void init(TestConfig config) {
        this.config = config;
    }

    public TestConfig getConfig() {
        return config;
    }
    
}
