/**
 * Copyright 2007 The JA-SIG Collaborative.  All rights reserved.
 * See license distributed with this file and
 * available online at http://www.uportal.org/license.html
 */
package org.jasig.portal.portlet.om;

import org.apache.pluto.internal.InternalPortletPreference;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public interface IPortletPreference extends InternalPortletPreference {

    /**
     * Sets the read only state of the preference
     */
    void setReadOnly(boolean readOnly);
}