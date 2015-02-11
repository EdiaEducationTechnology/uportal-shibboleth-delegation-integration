/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.portlet.container.services;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.portlet.PortletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.Validate;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pluto.container.PortletContainerException;
import org.apache.pluto.container.PortletWindow;
import org.apache.pluto.container.UserInfoService;
import org.apache.pluto.container.om.portlet.PortletApplicationDefinition;
import org.apache.pluto.container.om.portlet.UserAttribute;
import org.jasig.portal.portlet.om.IPortletDefinition;
import org.jasig.portal.portlet.om.IPortletEntity;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.registry.IPortletDefinitionRegistry;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.url.IPortalRequestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.stereotype.Service;

@Service("samlAssertionUserInfoService")
public class SamlAssertionUserInfoService implements UserInfoService {

  private IPortletWindowRegistry portletWindowRegistry;
  private IPortletDefinitionRegistry portletDefinitionRegistry;
  private IPortalRequestUtils portalRequestUtils;
  protected final Log logger = LogFactory.getLog(getClass());

  /**
   * @return the portalRequestUtils
   */
  public IPortalRequestUtils getPortalRequestUtils() {
    return portalRequestUtils;
  }

  /**
   * @param portalRequestUtils
   *          the portalRequestUtils to set
   */
  @Required
  @Autowired
  public void setPortalRequestUtils(IPortalRequestUtils portalRequestUtils) {
    Validate.notNull(portalRequestUtils);
    this.portalRequestUtils = portalRequestUtils;
  }

  /**
   * The default name of the preferences attribute used to pass the SAML assertion to the portlet.
   */
  private String samlAssertionKey = "samlAssertion";

  /**
   * The default name of the preferences attribute used to pass the IdP public keys to the portlet.
   */
  private String idpPublicKeysKey = "idpPublicKeys";

  /**
   * The default name of the session attribute used to pass the SAML assertion to the portlet.
   */
  private String samlAssertionSessionKey = "SAML Assertion";

  /**
   * The default name of the session attribute used to pass the IdP public keys to the portlet.
   */
  private String idpPublicKeysSessionKey = "IdP Public Keys";

  /**
   * @param idpPublicKeysKey the idpPublicKeysKey to set
   */
  public void setIdpPublicKeysKey(String idpPublicKeysKey) {
    this.idpPublicKeysKey = idpPublicKeysKey;
  }

  /**
   * @param idpPublicKeysSessionKey the idpPublicKeysSessionKey to set
   */
  public void setIdpPublicKeysSessionKey(String idpPublicKeysSessionKey) {
    this.idpPublicKeysSessionKey = idpPublicKeysSessionKey;
  }

  /**
   * @return the samlAssertionSessionKey
   */
  public String getSamlAssertionSessionKey() {
    return samlAssertionSessionKey;
  }

  /**
   * @param samlAssertionSessionKey the samlAssertionSessionKey to set
   */
  public void setSamlAssertionSessionKey(String samlAssertionSessionKey) {
    this.samlAssertionSessionKey = samlAssertionSessionKey;
  }

  /**
   * @return the portletWindowRegistry
   */
  public IPortletWindowRegistry getPortletWindowRegistry() {
    return this.portletWindowRegistry;
  }

  /**
   * @param portletWindowRegistry
   *          the portletWindowRegistry to set
   */
  @Required
  @Autowired
  public void setPortletWindowRegistry(IPortletWindowRegistry portletWindowRegistry) {
    this.portletWindowRegistry = portletWindowRegistry;
  }

  /**
   * @return the portletDefinitionRegistry
   */
  public IPortletDefinitionRegistry getPortletDefinitionRegistry() {
    return this.portletDefinitionRegistry;
  }

  /**
   * @param portletDefinitionRegistry
   *          the portletDefinitionRegistry to set
   */
  @Required
  @Autowired
  public void setPortletDefinitionRegistry(IPortletDefinitionRegistry portletDefinitionRegistry) {
    this.portletDefinitionRegistry = portletDefinitionRegistry;
  }

  /**
   * @return name of the key to save the SAM assertion under
   */
  public String getSamlAssertionKey() {
    return samlAssertionKey;
  }

  /**
   * @param samlAssertionKey
   *          name of the key to save the SAML assertionunder
   */
  public void setSamlAssertionKey(String samlAssertionKey) {
    this.samlAssertionKey = samlAssertionKey;
  }

  /*
   * (non-Javadoc)
   * @see org.apache.pluto.spi.optional.UserInfoService#getUserInfo(javax.portlet.PortletRequest, org.apache.pluto.PortletWindow)
   */
  public Map<String, String> getUserInfo(PortletRequest request, PortletWindow portletWindow) throws PortletContainerException {

    Map<String, String> userInfo = new LinkedHashMap<String, String>();

    // check to see if a SAML assertion is expected by this portlet
    if (isSamlAssertionRequested(request, portletWindow)) {
      final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);

      // if it is, attempt to request it from the session
      HttpSession session = httpServletRequest.getSession();
      String samlArtifact = (String)session.getAttribute(samlAssertionSessionKey);
      
      if (samlArtifact != null) {
        userInfo.put(this.samlAssertionKey, samlArtifact);
      }
      else
        logger.warn("Portlet " + portletWindow.getPortletDefinition().getPortletName() + " requested SAML assertion, but none was provided.");
    }

    // check to see if IdP public keys are expected by this portlet
    if (areIdPKeysRequested(request, portletWindow)) {
      final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);

      // if they are, attempt to request them from the session
      HttpSession session = httpServletRequest.getSession();
      String idpArtifact = (String)session.getAttribute(idpPublicKeysSessionKey);
      
      if (idpArtifact != null) {
        userInfo.put(this.idpPublicKeysKey, idpArtifact);
      }
      else
        logger.warn("Portlet " + portletWindow.getPortletDefinition().getPortletName() + " requested IdP public key, but none was provided.");
    }
    return userInfo;
  }

  /**
   * Determine whether the portlet expects a SAML assertion as one of the user attributes.
   * 
   * @param request
   *          portlet request
   * @param plutoPortletWindow
   *          portlet window
   * @return <code>true</code> if a CAS proxy ticket is expected, <code>false</code> otherwise
   * @throws PortletContainerException
   *           if expeced attributes cannot be determined
   */
  protected boolean isSamlAssertionRequested(PortletRequest request, PortletWindow plutoPortletWindow) throws PortletContainerException {

    // get the list of requested user attributes
    final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);
    final IPortletWindow portletWindow = this.portletWindowRegistry.convertPortletWindow(httpServletRequest, plutoPortletWindow);
    final IPortletEntity portletEntity = portletWindow.getPortletEntity();
    
    final IPortletDefinition portletDefinition = portletEntity.getPortletDefinition();
    final PortletApplicationDefinition portletApplicationDescriptor = this.portletDefinitionRegistry
            .getParentPortletApplicationDescriptor(portletDefinition.getPortletDefinitionId());

    // check to see if the SAML assertion key is one of the requested user attributes
    List<? extends UserAttribute> requestedUserAttributes = portletApplicationDescriptor.getUserAttributes();
    for (final UserAttribute userAttributeDD : requestedUserAttributes) {
      final String attributeName = userAttributeDD.getName();
      if (attributeName.equals(this.samlAssertionKey))
        return true;
    }
    // if the SAML assertion key wasn't found in the list of requested attributes
    return false;

  }

  /**
   * Determine whether the portlet expects IdP public keys as one of the user attributes.
   * 
   * @param request
   *          portlet request
   * @param plutoPortletWindow
   *          portlet window
   * @return <code>true</code> if a CAS proxy ticket is expected, <code>false</code> otherwise
   * @throws PortletContainerException
   *           if expeced attributes cannot be determined
   */
  protected boolean areIdPKeysRequested(PortletRequest request, PortletWindow plutoPortletWindow) throws PortletContainerException {

    // get the list of requested user attributes
    final HttpServletRequest httpServletRequest = this.portalRequestUtils.getPortletHttpRequest(request);
    final IPortletWindow portletWindow = this.portletWindowRegistry.convertPortletWindow(httpServletRequest, plutoPortletWindow);
    final IPortletEntity portletEntity = portletWindow.getPortletEntity();
    final IPortletDefinition portletDefinition = portletEntity.getPortletDefinition();
    final PortletApplicationDefinition portletApplicationDescriptor = this.portletDefinitionRegistry
            .getParentPortletApplicationDescriptor(portletDefinition.getPortletDefinitionId());

    // check to see if the SAML assertion key is one of the requested user attributes
    List<? extends UserAttribute> requestedUserAttributes = portletApplicationDescriptor.getUserAttributes();
    for (final UserAttribute userAttributeDD : requestedUserAttributes) {
      final String attributeName = userAttributeDD.getName();
      if (attributeName.equals(this.idpPublicKeysKey))
        return true;
    }
    // if the SAML assertion key wasn't found in the list of requested attributes
    return false;

  }

}
