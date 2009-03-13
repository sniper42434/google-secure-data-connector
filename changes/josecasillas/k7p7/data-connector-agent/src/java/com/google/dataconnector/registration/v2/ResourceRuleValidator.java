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

import com.google.dataconnector.registration.v2.ResourceRule.AppTag;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Validates a resource rule and produces errors.
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class ResourceRuleValidator {
  
  private static final int MAX_PORT = 65535;
  
  /**
   * Iterates through the provided list of resource rules and validates each one against the
   * required runtime configuration.
   * 
   * @param resourceRules a list of resource rules
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validateRuntime(List<ResourceRule> resourceRules) 
      throws ResourceException {
    for (ResourceRule resourceRule : resourceRules) {
      validateRuntime(resourceRule);
    }
  }
  
  /**
   * Iterates through the provided list of resource rules and validates each one against the
   * required configuration.
   * 
   * @param resourceRules a list of resource rules
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validate(List<ResourceRule> resourceRules) 
      throws ResourceException {
    
    // Bail if we have 0 rules.  The only known way to get here is if you leave off "repeatable"
    // attribute from the <entity> element in the resourceRules.xml file.  Normally if you 
    // specify no rules in that file, a different worse error happens.
    // See bug: 1538670
    if (resourceRules.size() == 0 ) {
      throw new ResourceException("Must specify atleast one rule.  This may be caused by an XML" +
          "parsing issue where you must specify '<entity repeatable=\"true\">' even if one" +
          "rule is specified."); 
    }
     
    // Go through each rule and validate it and make sure no two resources have the same ruleNum.
    Set<Integer> seenRuleNums = new HashSet<Integer>();
    for (ResourceRule resourceRule : resourceRules) {
      validate(resourceRule);
      if (seenRuleNums.contains(resourceRule.getRuleNum())) {
        throw new ResourceException("Duplicate <ruleNum/> entries not allowed. Resource: " + 
            resourceRule.getRuleNum());
      } else {
        seenRuleNums.add(resourceRule.getRuleNum());
      }
    }
  }
  
  /**
   * Validates a single resource rule against the required runtime configuration.  
   * 
   * @param resourceRule a resource rule configured for runtime usage.
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validateRuntime(ResourceRule resourceRule) 
      throws ResourceException {
    
    // All Config time validation and ...
    validate(resourceRule);
    
    // httpProxyPort - required for all URLEXACT resources
    Integer httpProxyPort = resourceRule.getHttpProxyPort();
    if (httpProxyPort != null) {
      if (httpProxyPort > MAX_PORT || httpProxyPort < 0) {
        throw new ResourceException("HttpProxyPort " + httpProxyPort + " out of range.");
      }
    } else if (resourceRule.getPatternType().trim().equals(ResourceRule.URLEXACT)) {
      throw new ResourceException("'httpProxyPort' required for each " + ResourceRule.HTTPID +
          "resource");
    }
    
    // socksServerPort 
    Integer socksServerPort = resourceRule.getSocksServerPort();
    if (socksServerPort != null) {
      if (socksServerPort > MAX_PORT || socksServerPort < 0) {
        throw new ResourceException("socksServerPort " + socksServerPort + " out of range.");
      }
    } else {
      throw new ResourceException("'socksServerPort' required for each resource");
    }
    
    // secretKey
    if (resourceRule.getSecretKey() == null) {
      throw new ResourceException("Rule is missing secret key");
    }
  }
  
  /**
   * Convenience method to wrap rule number with message.
   */
  private void throwResourceException(int ruleNum, String message) throws ResourceException {
    throw new ResourceException("Resource " + ruleNum + " " + message);
  }
  
  /**
   * Validates a single resource rule against the required configuration.  
   * 
   * @param resourceRule a resource rule at configuration time.
   * @throws ResourceException if there are any invalid rules.  The message contained in
   * the exception will indicate the error.
   */
  public void validate(ResourceRule resourceRule) throws ResourceException {
    
    // ruleNum
    if (resourceRule.getRuleNum() <= 0) {
      throw new ResourceException("Resource " + resourceRule.getPattern() + 
          " must have <ruleNum/> greater than 0.");
    }
    int ruleNum = resourceRule.getRuleNum();
    
    // clientId
    
    if (resourceRule.getClientId() == null) {
      throwResourceException(ruleNum, " 'clientId' field must be present");
    }
    if (resourceRule.getClientId().trim().contains(" ")) {
      throwResourceException(ruleNum, " 'clientId' field " + resourceRule.getClientId() + 
            " must not contain any white space.");
    }
    
    // allowed entities
    
    if (resourceRule.getAllowedEntities() == null) {
      throwResourceException(ruleNum, "at least one 'allowedEntities' field must be present");
    }
      
    for (String allowedEntity : resourceRule.getAllowedEntities()) {
      if (allowedEntity.trim().contains(" ")) {
        throwResourceException(ruleNum, " 'allowedEntities' field " + allowedEntity + 
            " must not contain any white space.");
      }
      if (!allowedEntity.trim().contains("@")) {
        throwResourceException(ruleNum, " 'allowedEntities' field " + allowedEntity + 
            " must be a valid fully qualified email address");
      }
    }
    
    // appids 
    
    if (resourceRule.getApps() == null) {
      throwResourceException(ruleNum, " at least one 'app' field must be present");
    }
    
    for (AppTag app : resourceRule.getApps()) {
      String container = app.getContainer();
      String appId = app.getAppId();
      if (container.trim().contains(" ") || appId.trim().contains(" ")) {
        throwResourceException(ruleNum, " 'appIds' field <" + container + ":" + appId + 
            "> must not contain any white space.");
      }
    }
    
    // pattern
    
    String pattern = resourceRule.getPattern();
    if (pattern == null) {
      throwResourceException(ruleNum, " 'pattern' must be present.");
    }
    
    pattern = pattern.trim();
    if (pattern.contains(" ")) {
      throwResourceException(ruleNum,  " 'pattern' field " + pattern + 
          " must not contain any white space.");
    }
    if (!pattern.startsWith(ResourceRule.HTTPID) && 
        !pattern.startsWith(ResourceRule.HTTPSID) && 
        !pattern.startsWith(ResourceRule.SOCKETID)) {
      throwResourceException(ruleNum, " Invalid pattern: " + pattern);
    }
    
    //  pattern type
    
    String patternType = resourceRule.getPatternType();
    if (patternType == null) {
      throwResourceException(ruleNum, " 'patternType' missing for " + pattern);
    }
    
    // Verify URLEXACT is set ONLY on http rules.
    if (patternType.equals(ResourceRule.URLEXACT)) {
      if (pattern.startsWith(ResourceRule.HTTPSID) || pattern.startsWith(ResourceRule.SOCKETID)) {
        throwResourceException(ruleNum, " Pattern type: URLEXACT works only with http.  Use " +
            "HOSTPORT for https or socket");
      }
    } else if (patternType.equals(ResourceRule.HOSTPORT)) {
      // URI should have no path data if using HOSTPORT.
      if (pattern.startsWith(ResourceRule.HTTPSID) || pattern.startsWith(ResourceRule.HTTPID)) {
        URI uri = null;
        try {
          uri = new URI(pattern);
        } catch (URISyntaxException e) {
          throwResourceException(ruleNum, " Invalid pattern URL: "+ e.getMessage());
        } 
        if (uri.getPath().length() > 1) {
          throwResourceException(ruleNum, " 'pattern' " + pattern + " cannot contain any path " +
              "elements when using HOSTPORT pattern type");
        }
      }
    } else if (patternType.equals(ResourceRule.REGEX)) {
      // TODO(rayc) remove when REGEX is officially removed.
    } else {
      throwResourceException(ruleNum, " 'patternType' " + patternType + " not supported.");
    }
  }
}