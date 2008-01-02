/**
 * Copyright 2007 The JA-SIG Collaborative.  All rights reserved.
 * See license distributed with this file and
 * available online at http://www.uportal.org/license.html
 */
package org.jasig.portal.channels.portlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Set;

import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.OptionalContainerServices;
import org.apache.pluto.PortletContainer;
import org.apache.pluto.PortletContainerException;
import org.apache.pluto.descriptors.portlet.PortletDD;
import org.apache.pluto.spi.optional.PortletRegistryService;
import org.jasig.portal.ChannelCacheKey;
import org.jasig.portal.ChannelRuntimeData;
import org.jasig.portal.ChannelStaticData;
import org.jasig.portal.PortalControlStructures;
import org.jasig.portal.PortalEvent;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletDefinitionId;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletEntityId;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.om.IPortletWindowId;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlet.registry.IPortletEntityRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.portlet.url.IPortletRequestParameterManager;
import org.jasig.portal.portlet.url.RequestType;
import org.jasig.portal.security.IPerson;
import org.springframework.beans.factory.annotation.Required;

/**
 * Implementation of ISpringPortletChannel that delegates rendering a portlet to the injected {@link PortletContainer}.
 * The portlet to render is determined in  {@link #initSession(ChannelStaticData, PortalControlStructures)} using the
 * channel publish and subscribe ids.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class SpringPortletChannelImpl implements ISpringPortletChannel {
    protected static final String PORTLET_WINDOW_ID_PARAM = SpringPortletChannelImpl.class.getName() + ".portletWindowId";
    
    protected final Log logger = LogFactory.getLog(this.getClass());
    
    private IPortletDefinitionRegistry portletDefinitionRegistry;
    private IPortletEntityRegistry portletEntityRegistry;
    private IPortletWindowRegistry portletWindowRegistry;
    private OptionalContainerServices optionalContainerServices;
    private PortletContainer portletContainer;
    private IPortletRequestParameterManager portletRequestParameterManager;
    
    
    
    /**
     * @return the portletDefinitionRegistry
     */
    public IPortletDefinitionRegistry getPortletDefinitionRegistry() {
        return portletDefinitionRegistry;
    }
    /**
     * @param portletDefinitionRegistry the portletDefinitionRegistry to set
     */
    @Required
    public void setPortletDefinitionRegistry(IPortletDefinitionRegistry portletDefinitionRegistry) {
        this.portletDefinitionRegistry = portletDefinitionRegistry;
    }
    /**
     * @return the portletEntityRegistry
     */
    public IPortletEntityRegistry getPortletEntityRegistry() {
        return portletEntityRegistry;
    }
    /**
     * @param portletEntityRegistry the portletEntityRegistry to set
     */
    @Required
    public void setPortletEntityRegistry(IPortletEntityRegistry portletEntityRegistry) {
        this.portletEntityRegistry = portletEntityRegistry;
    }

    /**
     * @return the portletWindowRegistry
     */
    public IPortletWindowRegistry getPortletWindowRegistry() {
        return portletWindowRegistry;
    }
    /**
     * @param portletWindowRegistry the portletWindowRegistry to set
     */
    @Required
    public void setPortletWindowRegistry(IPortletWindowRegistry portletWindowRegistry) {
        this.portletWindowRegistry = portletWindowRegistry;
    }

    /**
     * @return the optionalContainerServices
     */
    public OptionalContainerServices getOptionalContainerServices() {
        return optionalContainerServices;
    }
    /**
     * @param optionalContainerServices the optionalContainerServices to set
     */
    @Required
    public void setOptionalContainerServices(OptionalContainerServices optionalContainerServices) {
        this.optionalContainerServices = optionalContainerServices;
    }

    /**
     * @return the portletContainer
     */
    public PortletContainer getPortletContainer() {
        return portletContainer;
    }
    /**
     * @param portletContainer the portletContainer to set
     */
    @Required
    public void setPortletContainer(PortletContainer portletContainer) {
        this.portletContainer = portletContainer;
    }

    /**
     * @return the portletRequestParameterManager
     */
    public IPortletRequestParameterManager getPortletRequestParameterManager() {
        return portletRequestParameterManager;
    }
    /**
     * @param portletRequestParameterManager the portletRequestParameterManager to set
     */
    @Required
    public void setPortletRequestParameterManager(IPortletRequestParameterManager portletRequestParameterManager) {
        this.portletRequestParameterManager = portletRequestParameterManager;
    }

    
    //***** Helper methods for the class *****//
    
    /**
     * Get the id for the window instance described by the channel static data and portal control structures.
     * 
     * @param channelStaticData The static description data for the channel.
     * @param portalControlStructures Information about the current request/reponse.
     * @return The window instance id.
     */
    protected String getWindowInstanceId(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures) {
        return this.getWindowInstanceId(channelStaticData, portalControlStructures, null);
    }

    /**
     * Get the id for the window instance described by the channel static data, portal control structures, and channel
     * runtime data.
     * 
     * @param channelStaticData The static description data for the channel.
     * @param portalControlStructures Information about the current request/reponse.
     * @param channelRuntimeData Portal provided information for the current request.
     * @return The window instance id.
     */
    protected String getWindowInstanceId(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures, ChannelRuntimeData channelRuntimeData) {
        return channelStaticData.getChannelSubscribeId();
    }
    
    /**
     * Sets the portlet window ID to use for the specified channel static data.
     * 
     * @param channelStaticData The static description data for the channel.
     * @param portletWindowId The ID to associate with the static data.
     */
    protected void setPortletWidnowId(ChannelStaticData channelStaticData, IPortletWindowId portletWindowId) {
        //Probably not 'ok' to put data directly to the Hashtable that ChannelStaticData extends but it is the only way
        //an object scoped to just this channel instance can be stored
        channelStaticData.put(PORTLET_WINDOW_ID_PARAM, portletWindowId);
    }

    /**
     * Gets the portlet window ID to use for the specified channel static data.
     * 
     * @param channelStaticData The static description data for the channel.
     * @return The ID associated with the static data.
     */
    protected IPortletWindowId getPortletWindowId(ChannelStaticData channelStaticData) {
        return (IPortletWindowId)channelStaticData.get(PORTLET_WINDOW_ID_PARAM);
    }
    
    //***** ISpringPortletChannel methods *****//

    /* (non-Javadoc)
     * @see org.jasig.portal.channels.portlet.ISpringPortletChannel#initSession(org.jasig.portal.ChannelStaticData, org.jasig.portal.PortalControlStructures)
     */
    public void initSession(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures) {
        //Get/create the portlet entity to init
        final String channelPublishId = channelStaticData.getChannelPublishId();
        final IPortletDefinition portletDefinition = this.portletDefinitionRegistry.getPortletDefinition(Integer.parseInt(channelPublishId));
        if (portletDefinition == null) {
            throw new IllegalStateException("No IPortletDefinition exists for channelPublishId: " + channelPublishId);
        }
        
        final IPortletDefinitionId portletDefinitionId = portletDefinition.getPortletDefinitionId();
        final String channelSubscribeId = channelStaticData.getChannelSubscribeId();
        final IPerson person = channelStaticData.getPerson();
        final IPortletEntity portletEntity = this.portletEntityRegistry.getOrCreatePortletEntity(portletDefinitionId, channelSubscribeId, person);

        //Get/create the portlet window to init
        final HttpServletRequest httpServletRequest = portalControlStructures.getHttpServletRequest();
        final String windowInstanceId = this.getWindowInstanceId(channelStaticData, portalControlStructures);
        final IPortletEntityId portletEntityId = portletEntity.getPortletEntityId();
        final IPortletWindow portletWindow = this.portletWindowRegistry.getOrCreatePortletWindow(httpServletRequest, windowInstanceId, portletEntityId);
        
        this.setPortletWidnowId(channelStaticData, portletWindow.getPortletWindowId());

        //init the portlet window
        final HttpServletResponse httpServletResponse = portalControlStructures.getHttpServletResponse();
        try {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Loading portlet window '" + portletWindow + "' with underlying channel: " + channelStaticData);
            }
            
            this.portletContainer.doLoad(portletWindow, httpServletRequest, httpServletResponse);
        }
        catch (PortletException pe) {
            throw new PortletLoadFailureException("The portlet window '" + portletWindow + "' threw an exception while being loaded. Underlying channel: " + channelStaticData, portletWindow, pe);
        }
        catch (PortletContainerException pce) {
            throw new PortletLoadFailureException("The portlet container threw an exception while loading portlet window '" + portletWindow + "'. Underlying channel: " + channelStaticData, portletWindow, pce);
        }
        catch (IOException ioe) {
            throw new PortletLoadFailureException("The portlet window '" + portletWindow + "' threw an exception while being loaded. Underlying channel: " + channelStaticData, portletWindow, ioe);
        }
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.channels.portlet.ISpringPortletChannel#action(org.jasig.portal.ChannelStaticData, org.jasig.portal.PortalControlStructures, org.jasig.portal.ChannelRuntimeData)
     */
    public void action(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures, ChannelRuntimeData channelRuntimeData) {
        //Get the portlet window
        final HttpServletRequest httpServletRequest = portalControlStructures.getHttpServletRequest();
        final IPortletWindowId portletWindowId = this.getPortletWindowId(channelStaticData);
        if (portletWindowId == null) {
            throw new IllegalStateException("No IPortletWindowId exists in the ChannelStaticData, has initSession been called? ChannelStaticData: " + channelStaticData);
        }
        
        final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpServletRequest, portletWindowId);
        
        //Execute the action, 
         final HttpServletResponse httpServletResponse = portalControlStructures.getHttpServletResponse();
        try {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Executing portlet action for window '" + portletWindow + "' with underlying channel: " + channelStaticData);
            }
            
            this.portletContainer.doAction(portletWindow, httpServletRequest, httpServletResponse);
        }
        catch (PortletException pe) {
            throw new PortletLoadFailureException("The portlet window '" + portletWindow + "' threw an exception while executing action. Underlying channel: " + channelStaticData, portletWindow, pe);
        }
        catch (PortletContainerException pce) {
            throw new PortletLoadFailureException("The portlet container threw an exception while executing action on portlet window '" + portletWindow + "'. Underlying channel: " + channelStaticData, portletWindow, pce);
        }
        catch (IOException ioe) {
            throw new PortletLoadFailureException("The portlet window '" + portletWindow + "' threw an exception while executing action. Underlying channel: " + channelStaticData, portletWindow, ioe);
        }
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.channels.portlet.ISpringPortletChannel#generateKey(org.jasig.portal.ChannelStaticData, org.jasig.portal.PortalControlStructures, org.jasig.portal.ChannelRuntimeData)
     */
    public ChannelCacheKey generateKey(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures, ChannelRuntimeData channelRuntimeData) {
        final ChannelCacheKey cacheKey = new ChannelCacheKey();
        cacheKey.setKeyScope(ChannelCacheKey.INSTANCE_KEY_SCOPE);
        cacheKey.setKey("INSTANCE_EXPIRATION_CACHE_KEY");
        cacheKey.setKeyValidity(System.currentTimeMillis());
        
        return cacheKey;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.channels.portlet.ISpringPortletChannel#isCacheValid(org.jasig.portal.ChannelStaticData, org.jasig.portal.PortalControlStructures, org.jasig.portal.ChannelRuntimeData, java.lang.Object)
     */
    public boolean isCacheValid(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures, ChannelRuntimeData channelRuntimeData, Object validity) {
        //Get the portlet window
        final HttpServletRequest httpServletRequest = portalControlStructures.getHttpServletRequest();
        final IPortletWindowId portletWindowId = this.getPortletWindowId(channelStaticData);
        final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpServletRequest, portletWindowId);
        
        //If this window is targeted invalidate the cache
        final Set<IPortletWindowId> targetedPortletWindowIds = this.portletRequestParameterManager.getTargetedPortletWindowIds(httpServletRequest);
        if (targetedPortletWindowIds.contains(portletWindowId)) {
            return false;
        }
        
        //Get the root definition
        final IPortletEntity portletEntity = this.portletWindowRegistry.getParentPortletEntity(httpServletRequest, portletWindowId);
        final IPortletDefinition portletDefinition = this.portletEntityRegistry.getParentPortletDefinition(portletEntity.getPortletEntityId());
        
        //Get the portlet deployment
        final String portletApplicationId = portletDefinition.getPortletApplicationId();
        final String portletName = portletDefinition.getPortletName();
        final PortletDD portletDescriptor;
        try {
            final PortletRegistryService portletRegistryService = this.optionalContainerServices.getPortletRegistryService();
            portletDescriptor = portletRegistryService.getPortletDescriptor(portletApplicationId, portletName);
        }
        catch (PortletContainerException pce) {
            this.logger.warn("Could not retrieve PortletDD for portlet window '" + portletWindow + "' to determine caching configuration. Marking content cache invalid and continuing.", pce);
            return false;
        }
        
        if (portletDescriptor == null) {
            this.logger.warn("Could not retrieve PortletDD for portlet window '" + portletWindow + "' to determine caching configuration. Marking content cache invalid and continuing.");
            return false;
        }
        
        //If the descriptor value is unset return immediately
        final int descriptorExpirationCache = portletDescriptor.getExpirationCache();
        if (descriptorExpirationCache == PortletDD.EXPIRATION_CACHE_UNSET) {
            return false;
        }
        
        //Use value from window if it is set
        final Integer windowExpirationCache = portletWindow.getExpirationCache();
        final int expirationCache;
        if (windowExpirationCache != null) {
            expirationCache = windowExpirationCache;
        }
        else {
            expirationCache = descriptorExpirationCache;
        }
        
        //set to 0 (never cache)
        if (expirationCache == 0) {
            return false;
        }
        //set to -1 (never expire)
        else if (expirationCache == -1) {
            return true;
        }
        //If no validity object re-render to be safe
        else if (validity == null) {
            return false;
        }
        
        final long lastRenderTime = ((Long)validity).longValue();
        final long now = System.currentTimeMillis();
        
        //If the expiration time since last render has not passed return true
        return lastRenderTime + (expirationCache * 1000) >= now;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.channels.portlet.ISpringPortletChannel#render(org.jasig.portal.ChannelStaticData, org.jasig.portal.PortalControlStructures, org.jasig.portal.ChannelRuntimeData, java.io.PrintWriter)
     */
    public void render(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures, ChannelRuntimeData channelRuntimeData, PrintWriter printWriter) {
      //Get the portlet window
        final HttpServletRequest httpServletRequest = portalControlStructures.getHttpServletRequest();
        final IPortletWindowId portletWindowId = this.getPortletWindowId(channelStaticData);
        final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpServletRequest, portletWindowId);
        
        //Execute the action, 
        final HttpServletResponse httpServletResponse = portalControlStructures.getHttpServletResponse();
        final ContentRedirectingHttpServletResponse contentRedirectingHttpServletResponse = new ContentRedirectingHttpServletResponse(httpServletResponse, printWriter); 
        try {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("Rendering portlet for window '" + portletWindow + "' with underlying channel: " + channelStaticData);
            }
            
            this.portletContainer.doRender(portletWindow, httpServletRequest, contentRedirectingHttpServletResponse);
            contentRedirectingHttpServletResponse.flushBuffer();
        }
        catch (PortletException pe) {
            throw new PortletLoadFailureException("The portlet window '" + portletWindow + "' threw an exception while executing action. Underlying channel: " + channelStaticData, portletWindow, pe);
        }
        catch (PortletContainerException pce) {
            throw new PortletLoadFailureException("The portlet container threw an exception while executing action on portlet window '" + portletWindow + "'. Underlying channel: " + channelStaticData, portletWindow, pce);
        }
        catch (IOException ioe) {
            throw new PortletLoadFailureException("The portlet window '" + portletWindow + "' threw an exception while executing action. Underlying channel: " + channelStaticData, portletWindow, ioe);
        }
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.channels.portlet.ISpringPortletChannel#getTitle(org.jasig.portal.ChannelStaticData, org.jasig.portal.PortalControlStructures, org.jasig.portal.ChannelRuntimeData)
     */
    public String getTitle(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures, ChannelRuntimeData channelRuntimeData) {
        final HttpServletRequest httpServletRequest = portalControlStructures.getHttpServletRequest();
        final String title = (String)httpServletRequest.getAttribute(IPortletAdaptor.ATTRIBUTE__PORTLET_TITLE);
        
        if (this.logger.isDebugEnabled()) {
            this.logger.debug("Retrieved title '" + title + "' from request for: " + channelStaticData);
        }
        
        return title;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.channels.portlet.ISpringPortletChannel#portalEvent(org.jasig.portal.ChannelStaticData, org.jasig.portal.PortalControlStructures, org.jasig.portal.PortalEvent)
     */
    public void portalEvent(ChannelStaticData channelStaticData, PortalControlStructures portalControlStructures, PortalEvent portalEvent) {
        switch (portalEvent.getEventNumber()) {
            case PortalEvent.SESSION_DONE: {
                //TODO invalidate portlet's session
            }
            break;
            
            case PortalEvent.UNSUBSCRIBE: {
                final String channelSubscribeId = channelStaticData.getChannelSubscribeId();
                final IPerson person = channelStaticData.getPerson();
                final IPortletEntity portletEntity = this.portletEntityRegistry.getPortletEntity(channelSubscribeId, person);
                
                //TODO delete portlet windows for entity from the windowRegistry since there is no cascade from entity to window?
                
                this.portletEntityRegistry.deletePortletEntity(portletEntity);
            }
            break;
            
            //All of these events require the portlet re-render so the portlet parameter
            //manager is notified of the render request targing the portlet
            case PortalEvent.MINIMIZE_EVENT:
            case PortalEvent.MAXIMIZE_EVENT:
            case PortalEvent.EDIT_BUTTON_EVENT:
            case PortalEvent.HELP_BUTTON_EVENT:
            case PortalEvent.ABOUT_BUTTON_EVENT: {
                final HttpServletRequest httpServletRequest = portalControlStructures.getHttpServletRequest();
                final IPortletWindowId portletWindowId = this.getPortletWindowId(channelStaticData);
                final IPortletWindow portletWindow = this.portletWindowRegistry.getPortletWindow(httpServletRequest, portletWindowId);
                this.portletRequestParameterManager.setRequestType(httpServletRequest, portletWindowId, RequestType.RENDER);

                switch (portalEvent.getEventNumber()) {
                    case PortalEvent.MINIMIZE_EVENT: {
                        portletWindow.setWindowState(WindowState.MINIMIZED);
                    }
                    break;
                    case PortalEvent.MAXIMIZE_EVENT: {
                        portletWindow.setWindowState(WindowState.NORMAL);
                    }
                    break;
                    case PortalEvent.EDIT_BUTTON_EVENT: {
                        portletWindow.setPortletMode(PortletMode.EDIT);
                    }
                    break;
                    case PortalEvent.HELP_BUTTON_EVENT: {
                        portletWindow.setPortletMode(PortletMode.HELP);
                    }
                    break;
                    case PortalEvent.ABOUT_BUTTON_EVENT: {
                        portletWindow.setPortletMode(IPortletAdaptor.ABOUT);
                    }
                    break;
                }
            }
            break;
            
            default: {
                this.logger.info("Don't know how to handle event of type: " + portalEvent.getEventName() + "(" + portalEvent.getEventNumber() + ")");
            }
            break;
        }
    }
}
