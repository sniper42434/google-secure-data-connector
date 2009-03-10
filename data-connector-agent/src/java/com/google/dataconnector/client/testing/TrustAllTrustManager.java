/* Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
package com.google.dataconnector.client.testing;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Used for testing, this trust manager allows all certs.  Only use for testing!
 * 
 * @author rayc@google.com (Ray Colline)
 */
public class TrustAllTrustManager implements TrustManager, X509TrustManager {

  public X509Certificate[] getAcceptedIssuers() {
    return null;
  }
  public boolean isServerTrusted(X509Certificate[] certs) {
    return true;
  }
  public boolean isClientTrusted(X509Certificate[] certs) {
    return true;
  } 
  public void checkServerTrusted(X509Certificate[] certs, String authType) 
      throws CertificateException {
    return;
  }
  public void checkClientTrusted(X509Certificate[] certs, String authType) 
      throws CertificateException {
    return;
  }
}
