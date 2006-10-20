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
package org.apache.pluto.descriptors.portlet;

import java.util.List;
import java.util.ArrayList;

/**
 * Portlet preference definition in portlet.xml.
 * 
 * @see PortletPreferencesDD
 * 
 * @author <a href="mailto:ddewolf@apache.org">David H. DeWolf</a>
 * @author <a href="mailto:zheng@apache.org">ZHENG Zhong</a>
 * @since Jun 29, 2005
 */
public class PortletPreferenceDD {
	
	/** The preference name. */
    private String name = null;
    
    /** The preference values. */
    private List values = new ArrayList();
    
    /** Flag indicating if this preference is marked as read-only. */
    private boolean readOnly = false;
    
    
    // Public Methods ----------------------------------------------------------
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List getValues() {
        return values;
    }
    
    public void setValues(List values) {
        this.values = values;
    }
    
    public boolean isReadOnly() {
    	return readOnly;
    }
    
    public void setReadOnly(boolean readOnly) {
    	this.readOnly = readOnly;
    }
    
}