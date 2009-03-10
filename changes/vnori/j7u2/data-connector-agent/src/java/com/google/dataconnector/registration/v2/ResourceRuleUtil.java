/* Copyright 2008 Google Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.google.dataconnector.registration.v2;

import com.google.dataconnector.registration.v2.RegistrationV2Annotations.MyHostname;
import com.google.feedserver.util.BeanUtil;
import com.google.feedserver.util.XmlUtil;
import com.google.inject.Inject;

import org.xml.sax.SAXException;

import java.beans.IntrospectionException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.net.SocketFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Utility methods to convert resource strings into {@link ResourceRule} list.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceRuleUtil {

  private static final String TOP_LEVEL_ELEMENT = "feed";
  private static final String ENTITY = "entity";
  private static final String ALL = "all";

  // Dependencies
  private XmlUtil xmlUtil;
  private BeanUtil beanUtil;
  private SocketFactory socketFactory;
  private SocketAddress ephemeralLocalhostAddress;

  /**
   * Creates a new resource util with provided dependcies
   * 
   * @param xmlUtil utility to convert XML into map properties
   * @param beanUtil utility to covert properties map into a bean.
   * @param ephemeralLocalhostAddress a created InetAddress resolved to localhost.
   * @param socketFactory a way to get sockets such that we can bind with to get ephemeral ports.
   */
  @Inject
  public ResourceRuleUtil(XmlUtil xmlUtil, BeanUtil beanUtil, SocketFactory socketFactory, 
      @MyHostname SocketAddress ephemeralLocalhostAddress) {
    this.xmlUtil = xmlUtil;
    this.beanUtil = beanUtil;
    this.ephemeralLocalhostAddress = ephemeralLocalhostAddress;
    this.socketFactory = socketFactory;
  }

  /**
   * Creates secret keys for each of the resources.
   * 
   * @param resourceRules a list of resourceRules.
   */
  public void setSecretKeys(List<ResourceRule> resourceRules) {
    for (ResourceRule resourceRule : resourceRules) {
      resourceRule.setSecretKey(new Random().nextLong());
    }
  }
  
  /**
   * Gets ephermal port for each rule that is used to bind an Apache VirtualHost proxy to. 
   * 
   * @param resourceRules list of resource rules for this domain.
   * @throws ResourceException if any socket errors occur while trying to bind.
   */
  public void getVirtualHostBindPortsAndSetHttpProxyPorts(List<ResourceRule> resourceRules) 
      throws ResourceException {
    for (ResourceRule resourceRule : resourceRules) {
      // We only get ports for HTTP type rules as these traverse the apache proxy.
      if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPID)) {
        continue;
      }
      try {
        // In order for the OS to return us an available ephemeral port we have to bind a socket
        // to "127.0.0.1:0" we get the port then close the socket.  Its a bit lame but I have
        // not found a better way to do it.
        Socket socket = socketFactory.createSocket();
        socket.bind(ephemeralLocalhostAddress);
        resourceRule.setHttpProxyPort(socket.getLocalPort());
        socket.close();
      } catch (IOException e) {
        throw new ResourceException("Error while trying to obtain ephemeral ports.", e);
      }
    }
  }

  /**
   * Finds all rules that either match our client ID specified or match "all". We remove any rules
   * that are not for "us". This method modifies the list in-place.  The idea here is that 
   * ResourceRules config files could be the same for all clients.  When we move to config in the
   * cloud, there will only be one feed of resource rules.
   * 
   * @param resourceRules that correctly matches our client.
   */
  public void getThisClientRulesAndSetId(List<ResourceRule> resourceRules, 
      String myClientId) {
    List<ResourceRule> removalList = new ArrayList<ResourceRule>();
    for (ResourceRule resourceRule : resourceRules) {
      if (!resourceRule.getClientId().equals(ALL) && 
          !resourceRule.getClientId().equals(myClientId)) {
        removalList.add(resourceRule);
      } else {
        resourceRule.setClientId(myClientId);
      }
    }
    // Delete out the entries not for this client.
    resourceRules.removeAll(removalList);
  }

  /**
   * Sets the socks server listen port for these resource rules
   * 
   * @param resourceRules a list of resourceRules.
   * @param socksServerPort the first port number to use for each http rule.
   */
  public void setSocksServerPort(List<ResourceRule> resourceRules, int socksServerPort) {
    for (ResourceRule resourceRule : resourceRules) {
      resourceRule.setSocksServerPort(socksServerPort);
    }
  }

  /**
   * Return a populated {@link ResourceRule} bean from the given payload-in-content entity XML.
   * 
   * @param entityXmlText payload-in-content XML representing a ResourceRule
   * @return a populated Resource Rule bean.
   * @throws ResourceException if any XML parsing or conversion errors occur.
   */
  public ResourceRule getResourceRuleFromEntityXml(String entityXmlText) throws ResourceException {
    try {
      ResourceRule resourceRule = new ResourceRule();
      beanUtil.convertPropertiesToBean(xmlUtil.convertXmlToProperties(entityXmlText), resourceRule);
      return resourceRule;
    } catch (IllegalArgumentException e) {
      throw new ResourceException(e);
    } catch (IntrospectionException e) {
      throw new ResourceException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceException(e);
    } catch (SAXException e) {
      throw new ResourceException(e);
    } catch (IOException e) {
      throw new ResourceException(e);
    } catch (ParserConfigurationException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * Returns XML generated from {@link ResourceRule} bean.
   * 
   * @param resourceRule a populated bean.
   * @return entity XML for this bean.
   * @throws ResourceException if any conversion errors occur.
   */
  public String getEntityXmlFromResourceRule(ResourceRule resourceRule) throws ResourceException {
    try {
      return xmlUtil.convertPropertiesToXml(beanUtil.convertBeanToProperties(resourceRule));
    } catch (IllegalArgumentException e) {
      throw new ResourceException(e);
    } catch (IntrospectionException e) {
      throw new ResourceException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * Returns a list of {@link ResourceRule} from a feed encompassing one or more payload-in-content
   * entity XML representing a property configured Resource Rule.
   * @param xmlFeedText The feed XML with &lt;feed&gt; as the top-level element.
   * @return a list of {@link ResourceRule}.
   * @throws ResourceException if any XML parsing or conversion errors occur.
   */
  @SuppressWarnings("unchecked")
  public List<ResourceRule> getResourceRules(String xmlFeedText) throws ResourceException {
    List<ResourceRule> resourceRules = new ArrayList<ResourceRule>();
    try {
      Map<String,Object> entities = xmlUtil.convertXmlToProperties(xmlFeedText, TOP_LEVEL_ELEMENT);
      if (entities.containsKey(ENTITY)) {
        if (entities.get(ENTITY) instanceof Object[]) { 
          for (Object ruleProperties :  (Object[]) entities.get(ENTITY)) {
            if (ruleProperties instanceof HashMap) {
              ResourceRule resourceRule = new ResourceRule();  
              beanUtil.convertPropertiesToBean((HashMap) ruleProperties, resourceRule);
              resourceRules.add(resourceRule);
            }
          }
        }
      }
      return resourceRules;
    } catch (SAXException e) {
      throw new ResourceException(e);
    } catch (IOException e) {
      throw new ResourceException(e);
    } catch (ParserConfigurationException e) {
      throw new ResourceException(e);
    } catch (IllegalArgumentException e) {
      throw new ResourceException(e);
    } catch (IntrospectionException e) {
      throw new ResourceException(e);
    } catch (IllegalAccessException e) {
      throw new ResourceException(e);
    } catch (InvocationTargetException e) {
      throw new ResourceException(e);
    }
  }

  /**
   * For HTTPS rules use the URL util to extract the host from the URL pattern.
   * 
   * @param resourceRule the resource rule to exact host from.
   * @return a hostname.
   */
  public String getHostnameFromRule(ResourceRule resourceRule) {
    if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPSID)) {
      throw new RuntimeException("Can only invoke on HTTPS rules");
    }
    try {
      URL url = new URL(resourceRule.getPattern());
      return url.getHost();
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * For HTTPS rules use the URL util to extract the port from the URL pattern.
   * 
   * @param resourceRule the resource rule to exact host from.
   * @return a port.
   */
  public Integer getPortFromRule(ResourceRule resourceRule) {
    if (!resourceRule.getPattern().startsWith(ResourceRule.HTTPSID)) {
      throw new RuntimeException("Can only invoke on HTTPS rules");
    }
    try {
      URL url = new URL(resourceRule.getPattern());
      return (url.getPort() == -1 ? 443 : url.getPort());
    } catch (MalformedURLException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * creates the following system resource rules
   * 
   * 1. healthz rule:  http://localhost:portnum/<clientId>/__SDCINTERNAL__/healthz
   * 2. rules to allow access to healthcheck feeds (TODO - after enabling routing options to be
   * declared for each resource rule)
   * 
   * @param user the userid who should be allowed to access this resource
   * @param domain the domain the above user belongs to
   * @param clientId the clientId this resource is attached to
   * @param port the port HealthzRequestHandler is listening on
   * @param healthzGadgetUsers the users who are allowed access to the healthz gadget
   * @return the list of system resources created
   */
  public List<ResourceRule> createSystemRules(String user, String domain, String clientId, 
      int port, String healthzGadgetUsers) {
    List<ResourceRule> systemRules = new ArrayList<ResourceRule>();
    int nextRuleNum = Integer.MAX_VALUE;
    
    // if the input param healthzGadgetUsers is not null, add the input user to the list of users
    String[] allowedEntities;
    String implicitUser = user + "@" + domain;
    if (healthzGadgetUsers != null) {
      allowedEntities = (healthzGadgetUsers + "," + implicitUser).split(",");
    } else {
      allowedEntities = new String[] {implicitUser};
    }
    
    // create healthz rule
    ResourceRule healthzRule = new ResourceRule();
    healthzRule.setAllowedEntities(allowedEntities);
    healthzRule.setClientId(clientId);
    healthzRule.setAppIds(new String[] { ".*" });
    // assign name of Integer.MAX_VALUE
    healthzRule.setRuleNum(nextRuleNum--);
    healthzRule.setPattern(ResourceRule.HTTPID + "localhost:" + port + "/" + clientId + 
	  "/__SDCINTERNAL__/healthz");
    healthzRule.setPatternType(ResourceRule.URLEXACT);
    systemRules.add(healthzRule);
    return systemRules;
  }
  
  /**
   * For legacy clients, sets rule number from deprecated name. 
   * TODO(rayc) remove when we deprecate TT2 and older clients.
   */
  @Deprecated
  public void setRuleNumFromName(List<ResourceRule> resourceRules) throws ResourceException {
    for (ResourceRule resourceRule : resourceRules) {
      if (resourceRule.getName() != null) {
        try {
          resourceRule.setRuleNum(Integer.valueOf(resourceRule.getName()));
        } catch (NumberFormatException e) {
          throw new ResourceException(e);
        }
      }
    }
  }
}