/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
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
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/
package org.osgi.test.cases.rest.junit;

import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * Tests REST Management Service XML Representations.
 *
 * @author Petia Sotirova
 */
public class RestServiceXMLTestCase extends RestTestUtils {

  private Validator validator;
  private DocumentBuilder documentBuilder;

  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
    URL schemaResource = getClass().getResource("schema-rest.xsd");
    Schema schema = factory.newSchema(new StreamSource(schemaResource.openStream()));
    validator = schema.newValidator();

    documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
  }

  // 5.1.1
  @Test
  public void testFrameworkStartLevel() throws IOException, ParserConfigurationException, SAXException, TransformerConfigurationException,
      TransformerException {
    FrameworkStartLevel frameworkStartLevel = getFrameworkStartLevel();
    int originalStartLevel = frameworkStartLevel.getStartLevel();
    int originalInitialBundleStartLevel = frameworkStartLevel.getInitialBundleStartLevel();

    Element xmlStartLevel = getXMLObject(FW_START_LEVEL_URI, FW_START_LEVEL_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertFWStartLevel(frameworkStartLevel, xmlStartLevel);

    int startLevel = originalStartLevel;
    int initialBundleStartLevel = originalInitialBundleStartLevel + 1;
    updateFWStartLevel(startLevel, initialBundleStartLevel, HttpURLConnection.HTTP_NO_CONTENT, null);

    frameworkStartLevel = getFrameworkStartLevel();

    assertThat(frameworkStartLevel.getStartLevel()).as("startLevel after PUT.").isEqualTo(startLevel);
    assertThat(frameworkStartLevel.getInitialBundleStartLevel()).as("initialBundleStartLevel after PUT.").isEqualTo(initialBundleStartLevel);

    xmlStartLevel = getXMLObject(FW_START_LEVEL_URI, FW_START_LEVEL_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertFWStartLevel(frameworkStartLevel, xmlStartLevel);

    frameworkStartLevel.setStartLevel(originalStartLevel);
    frameworkStartLevel.setInitialBundleStartLevel(originalInitialBundleStartLevel);

    // Updated frameworkStartLevel
    xmlStartLevel = getXMLObject(FW_START_LEVEL_URI, FW_START_LEVEL_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertFWStartLevel(frameworkStartLevel, xmlStartLevel);
  }

  // tests list bundles and install bundle from invalid location/content.
  // successful install is covered by RestServiceJSONTestCase
  @Test
  public void testBundleList() throws BundleException, IOException, ParserConfigurationException, SAXException,
      TransformerConfigurationException, TransformerException {
    Element xmlBundleList = getXMLObject(BUNDLE_LIST_URI, BUNDLE_LIST_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);

    Bundle[] bundles = getInstalledBundles();
    assertBundleListRepresentation(bundles, xmlBundleList);

    xmlBundleList = getXMLObject(getBundleListURI(getFilter(TEST_BUNDLE_SYMBOLIC_NAME)), BUNDLE_LIST_CONTENT_TYPE_XML,
      HttpURLConnection.HTTP_OK);
    assertBundleListRepresentation(new Bundle[]{getBundle(TEST_BUNDLE_SYMBOLIC_NAME)}, xmlBundleList);

    Element xmlBundleException = installBundle(BUNDLE_LIST_URI, null, "invalid bundle location", null, true,
      HttpURLConnection.HTTP_BAD_REQUEST);
    assertBundleException(xmlBundleException, "Install bundle by invalid URI.");

    xmlBundleException = installBundle(BUNDLE_LIST_URI, null, "invalid bundle location", null, false, HttpURLConnection.HTTP_BAD_REQUEST);
    assertBundleException(xmlBundleException, "Install bundle by invalid bundle content.");
  }

  // 5.1.2.2
  @Test
  public void testBundleRepresentationsList() throws IOException, ParserConfigurationException, SAXException,
      TransformerConfigurationException, TransformerException {
    Element xmlBundleRepresentationsList = getXMLObject(BUNDLE_REPRESENTATIONS_LIST_URI, BUNDLE_REPRESENTATIONS_LIST_CONTENT_TYPE_XML,
      HttpURLConnection.HTTP_OK);

    Bundle[] bundles = getInstalledBundles();
    assertBundleRepresentationList(bundles, xmlBundleRepresentationsList);

    // test with filter
    xmlBundleRepresentationsList = getXMLObject(getBundleRepresentationListURI(getFilter(TEST_BUNDLE_SYMBOLIC_NAME)),
      BUNDLE_REPRESENTATIONS_LIST_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    Bundle testBundle = getBundle(TEST_BUNDLE_SYMBOLIC_NAME);
    assertBundleRepresentationList(new Bundle[]{testBundle}, xmlBundleRepresentationsList);
  }

  // 5.1.3
  @Test
  public void testBundle() throws IOException, ParserConfigurationException, SAXException, TransformerConfigurationException,
      TransformerException, BundleException {
    // GET
    Bundle bundle = getRandomBundle();
    Element xmlBundleRepresentation = getXMLObject(getBundleURI(bundle.getBundleId()), BUNDLE_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertThat(xmlBundleRepresentation).as("Bundle representation : " + bundle.getBundleId() + " :").isNotNull();

    assertBundleRepresentation(bundle, xmlBundleRepresentation);

    long notExistingBundleId = getNotExistingBundleId();
    Object object = getXMLObject(getBundleURI(notExistingBundleId), BUNDLE_CONTENT_TYPE_XML, HttpURLConnection.HTTP_NOT_FOUND);
    assertThat(object).as("Get not existing bundle " + notExistingBundleId + " :").isNull();

    // PUT with location
    Bundle tb1Bundle = getTestBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME, TB1);
    Element result = updateBundle(getBundleURI(tb1Bundle.getBundleId()), context.getBundle().getEntry(TB11), null, true,
      HttpURLConnection.HTTP_NO_CONTENT, null);
    assertThat(result).as("Update bundle by location " + tb1Bundle.getBundleId() + " :").isNull();

    result = updateBundle(getBundleURI(notExistingBundleId), context.getBundle().getEntry(TB11), null, true,
      HttpURLConnection.HTTP_NOT_FOUND, null);
    assertThat(result).as("Update not existing bundle " + notExistingBundleId + " :").isNull();

    result = updateBundle(getBundleURI(tb1Bundle.getBundleId()), null, "invalid bundle location", true, HttpURLConnection.HTTP_BAD_REQUEST,
      BUNDLE_EXCEPTION_CONTENT_TYPE_XML);
    assertBundleException(result, "Update bundle with invalid location " + tb1Bundle.getBundleId());

    // PUT with bundle content
    Bundle tb2Bundle = getTestBundle(TB2_TEST_BUNDLE_SYMBOLIC_NAME, TB2);

    result = updateBundle(getBundleURI(tb2Bundle.getBundleId()), context.getBundle().getEntry(TB21), null, false,
      HttpURLConnection.HTTP_NO_CONTENT, null);
    assertThat(result).as("Update bundle by content " + tb2Bundle.getBundleId() + " :").isNull();

    result = updateBundle(getBundleURI(notExistingBundleId), context.getBundle().getEntry(TB21), null, false,
      HttpURLConnection.HTTP_NOT_FOUND, null);
    assertThat(result).as("Update not existing bundle " + notExistingBundleId + " :").isNull();

    result = updateBundle(getBundleURI(tb2Bundle.getBundleId()), null, "invalid bundle location", true, HttpURLConnection.HTTP_BAD_REQUEST,
      BUNDLE_EXCEPTION_CONTENT_TYPE_XML);

    assertBundleException(result, "Update bundle with invalid content " + tb2Bundle.getBundleId());

    // DELETE
    result = uninstallBundle(getBundleURI(tb1Bundle.getBundleId()), HttpURLConnection.HTTP_NO_CONTENT);
    assertThat(result).as("Uninstall bundle " + tb1Bundle.getBundleId() + " :").isNull();
    assertThat(context.getBundle(tb1Bundle.getBundleId())).as("Framework bundle " + tb1Bundle.getBundleId() + " :").isNull();

    result = uninstallBundle(getBundleURI(tb1Bundle.getBundleId()), HttpURLConnection.HTTP_NOT_FOUND);
    assertThat(result).as("Uninstall already uninstalled bundle " + tb1Bundle.getBundleId() + " :").isNull();

    // stop tb2 bundle throws Exception
    tb2Bundle.start();
    result = uninstallBundle(getBundleURI(tb2Bundle.getBundleId()), HttpURLConnection.HTTP_NO_CONTENT);
  }

  // 5.1.4
  @Test
  public void testBundleState() throws IOException, ParserConfigurationException, SAXException, TransformerConfigurationException,
      TransformerException, BundleException {
    // GET
    Bundle bundle = getRandomBundle();
    Element xmlBundleStateRepresentation = getXMLObject(getBundleStateURI(bundle.getBundleId()), BUNDLE_STATE_CONTENT_TYPE_XML,
      HttpURLConnection.HTTP_OK);
    assertThat(xmlBundleStateRepresentation).as("Bundle state " + bundle.getBundleId() + " :").isNotNull();
    assertBundleStateRepresentation(bundle, xmlBundleStateRepresentation);

    long notExistingBundleId = getNotExistingBundleId();

    xmlBundleStateRepresentation = getXMLObject(getBundleStateURI(notExistingBundleId), BUNDLE_STATE_CONTENT_TYPE_XML,
      HttpURLConnection.HTTP_NOT_FOUND);
    assertThat(xmlBundleStateRepresentation).as("Bundle state for not existing bundle " + notExistingBundleId + " :").isNull();

    // PUT
    Bundle tb1Bundle = getTestBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME, TB1);

    int tb1State = tb1Bundle.getState();

    int newState = tb1State == Bundle.INSTALLED ? Bundle.ACTIVE : Bundle.RESOLVED;
    xmlBundleStateRepresentation = updateBundleState(getBundleStateURI(tb1Bundle.getBundleId()), newState, -1, HttpURLConnection.HTTP_OK,
      APPLICATION_XML, APPLICATION_XML, BUNDLE_STATE_CONTENT_TYPE_XML);
    assertThat(xmlBundleStateRepresentation).as("Bundle state updated  " + tb1Bundle.getBundleId() + " :").isNotNull();

    assertThat(tb1Bundle.getState()).as("New state ").isEqualTo(newState);
    assertBundleStateRepresentation(tb1Bundle, xmlBundleStateRepresentation);

    // start bundle with Bundle-ActivationPolicy by bundle id
    Bundle tb3Bundle = getTestBundle(TB3_TEST_BUNDLE_SYMBOLIC_NAME, TB3);
    int tb3State = tb3Bundle.getState();
    if (tb3State == Bundle.ACTIVE || tb3State == Bundle.STARTING) {
      tb3Bundle.stop();
    }

    xmlBundleStateRepresentation = updateBundleState(getBundleStateURI(tb3Bundle.getBundleId()), Bundle.ACTIVE,
      Bundle.START_ACTIVATION_POLICY, HttpURLConnection.HTTP_OK, APPLICATION_XML, APPLICATION_XML, BUNDLE_STATE_CONTENT_TYPE_XML);
    assertThat(xmlBundleStateRepresentation).as("Bundle state updated  " + tb3Bundle.getBundleId() + " :").isNotNull();

    assertThat(tb3Bundle).as("New state for 'lazy' bundle ").isInState(Bundle.STARTING);
    assertBundleStateRepresentation(tb3Bundle, xmlBundleStateRepresentation);

    xmlBundleStateRepresentation = updateBundleState(getBundleStateURI(notExistingBundleId), newState, -1,
      HttpURLConnection.HTTP_NOT_FOUND, APPLICATION_XML, APPLICATION_XML, null);
    assertThat(xmlBundleStateRepresentation).as("Bundle state updated for not existing bundle " + notExistingBundleId + " :").isNull();

    Bundle tb21Bundle = getTestBundle(TB21_TEST_BUNDLE_SYMBOLIC_NAME, TB21);
    if (tb21Bundle.getState() != Bundle.ACTIVE) {
      tb21Bundle.start();
    }

    Element exceptionelement = updateBundleState(getBundleStateURI(tb21Bundle.getBundleId()), Bundle.RESOLVED, -1,
      HttpURLConnection.HTTP_BAD_REQUEST, APPLICATION_XML, APPLICATION_XML, BUNDLE_EXCEPTION_CONTENT_TYPE_XML);
    assertBundleException(exceptionelement, "Stop bundle for bundle with error in stop method  " + tb21Bundle.getBundleId());

    // stop bundle with options
    xmlBundleStateRepresentation = updateBundleState(getBundleStateURI(tb3Bundle.getBundleId()), Bundle.RESOLVED, Bundle.STOP_TRANSIENT,
      HttpURLConnection.HTTP_OK, APPLICATION_XML, APPLICATION_XML, BUNDLE_STATE_CONTENT_TYPE_XML);
    assertThat(xmlBundleStateRepresentation).as("Bundle state updated  " + tb3Bundle.getBundleId() + " :").isNotNull();

    assertThat(tb3Bundle).as("New state ").isInState(Bundle.RESOLVED);
    assertBundleStateRepresentation(tb3Bundle, xmlBundleStateRepresentation);
  }

  // 5.1.5
  @Test
  public void testBundleHeader() throws IOException, ParserConfigurationException, SAXException, TransformerConfigurationException,
      TransformerException {
    // GET
    Bundle bundle = getRandomBundle();
    Element xmlBundleHeaderRepresentation = getXMLObject(getBundleHeaderURI(bundle.getBundleId()), BUNDLE_HEADER_CONTENT_TYPE_XML,
        HttpURLConnection.HTTP_OK);
    assertThat(xmlBundleHeaderRepresentation).as("Bundle header " + bundle.getBundleId() + ": ").isNotNull();
    assertBundleHeaderRepresentation(bundle, xmlBundleHeaderRepresentation);

    long notExistingBundleId = getNotExistingBundleId();
    xmlBundleHeaderRepresentation = getXMLObject(getBundleHeaderURI(notExistingBundleId), BUNDLE_HEADER_CONTENT_TYPE_XML,
      HttpURLConnection.HTTP_NOT_FOUND);
    assertThat(xmlBundleHeaderRepresentation).as("Bundle header for not existing bundle " + notExistingBundleId + " :").isNull();
  }

  // 5.1.6
  @Test
  public void testBundleStartLevel() throws IOException, ParserConfigurationException, SAXException, TransformerConfigurationException,
      TransformerException, BundleException {
    // GET
    Bundle bundle = getRandomBundle();
    Element xmlBundleStartLevelRepresentation = getXMLObject(getBundleStartLevelURI(bundle.getBundleId()),
        BUNDLE_START_LEVEL_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertBundleStartLevelRepresentation(bundle, xmlBundleStartLevelRepresentation);

    long notExistingBundleId = getNotExistingBundleId();
    xmlBundleStartLevelRepresentation = getXMLObject(getBundleStartLevelURI(notExistingBundleId), BUNDLE_START_LEVEL_CONTENT_TYPE_XML,
      HttpURLConnection.HTTP_NOT_FOUND);
    assertThat(xmlBundleStartLevelRepresentation).as("Bundle start level for not existing bundle " + notExistingBundleId + " :").isNull();

    // PUT
    Bundle tb1Bundle = getTestBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME, TB1);

    int tb1StartLevel = getBundleStartLevel(tb1Bundle).getStartLevel();
    int newStartLevel = tb1StartLevel + 1;
    xmlBundleStartLevelRepresentation = updateBundleStartLevel(getBundleStartLevelURI(tb1Bundle.getBundleId()), newStartLevel,
      HttpURLConnection.HTTP_OK, APPLICATION_XML, APPLICATION_XML);

    tb1Bundle = getTestBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME, TB1);
    assertBundleStartLevelRepresentation(tb1Bundle, xmlBundleStartLevelRepresentation);

    tb1StartLevel = getBundleStartLevel(tb1Bundle).getStartLevel();
    assertThat(tb1StartLevel).as("New start level ").isEqualTo(newStartLevel);

    xmlBundleStartLevelRepresentation = updateBundleStartLevel(getBundleStartLevelURI(notExistingBundleId), newStartLevel,
      HttpURLConnection.HTTP_NOT_FOUND, APPLICATION_XML, APPLICATION_XML);
    assertThat(xmlBundleStartLevelRepresentation).as("Bundle start level updated for not existing bundle " + notExistingBundleId + " :").isNull();

    updateBundleStartLevel(getBundleStartLevelURI(tb1Bundle.getBundleId()), -1, HttpURLConnection.HTTP_BAD_REQUEST,
      APPLICATION_XML, APPLICATION_XML);
  }

  // 5.1.7.1
  @Test
  public void testServiceList() throws IOException, InvalidSyntaxException, ParserConfigurationException, SAXException,
      TransformerConfigurationException, TransformerException {
    String filter = null;
    ServiceReference<?>[] serviceRefs = getServices(filter);

    Element xmlServiceList = getXMLObject(getServiceListURI(filter), SERVICE_LIST_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertServiceList(xmlServiceList, serviceRefs);

    filter = "(&(" + Constants.SERVICE_ID + ">=" + serviceRefs.length / 2 + ")"
            + "(" + Constants.SERVICE_ID + "<=" + serviceRefs.length + "))";
    serviceRefs = getServices(filter);
    xmlServiceList = getXMLObject(getServiceListURI(filter), SERVICE_LIST_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertServiceList(xmlServiceList, serviceRefs);

    String invalidFilterURI = SERVICE_LIST_URI + "?filter=invalid-filter";
    xmlServiceList = getXMLObject(invalidFilterURI, null, HttpURLConnection.HTTP_BAD_REQUEST);
    assertThat(xmlServiceList).as("Request with invalid filter " + invalidFilterURI).isNull();
  }

  // 5.1.7.2
  @Test
  public void testServiceRepresentationsList() throws IOException, InvalidSyntaxException, ParserConfigurationException, SAXException,
      TransformerConfigurationException, TransformerException {
    String filter = null;
    ServiceReference<?>[] serviceRefs = getServices(filter);
    Element xmlServiceRepresentationsList = getXMLObject(getServiceRepresentationListURI(filter),
        SERVICE_REPRESENTATIONS_LIST_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertServiceRepresentationList(xmlServiceRepresentationsList, serviceRefs);

    filter = "(&(" + Constants.SERVICE_ID + ">=" + serviceRefs.length / 2 + ")"
            + "(" + Constants.SERVICE_ID + "<=" + serviceRefs.length + "))";

    serviceRefs = getServices(filter);
    xmlServiceRepresentationsList = getXMLObject(getServiceRepresentationListURI(filter), SERVICE_REPRESENTATIONS_LIST_CONTENT_TYPE_XML,
      HttpURLConnection.HTTP_OK);
    assertServiceRepresentationList(xmlServiceRepresentationsList, serviceRefs);

    filter = "invalid-filter";
    xmlServiceRepresentationsList = getXMLObject(getServiceRepresentationListURI(filter), null, HttpURLConnection.HTTP_BAD_REQUEST);
    assertThat(xmlServiceRepresentationsList).as("Request with invalid filter '" + filter + "'").isNull();
  }

  // 5.1.8
  @Test
  public void testService() throws Exception {
    ServiceReference<?> serviceRef = getRandomService();

    // GET
    Element xmlServiceRepresentation = getXMLObject(getServiceURI(serviceRef), SERVICE_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertThat(xmlServiceRepresentation).as("Service representation " + serviceRef + " :").isNotNull();

    assertService(xmlServiceRepresentation, serviceRef);

    String notExistingSeriveId = "not-existing-servicve-" + System.currentTimeMillis();
    xmlServiceRepresentation = getXMLObject(getServiceURI(notExistingSeriveId), SERVICE_CONTENT_TYPE_XML, HttpURLConnection.HTTP_NOT_FOUND);
    assertThat(xmlServiceRepresentation).as("Service representation for not existing service " + notExistingSeriveId + " :").isNull();
  }

  // 5.6.1 The Extensions Resource
  @Test
  public void testExtensions() throws IOException, BundleException, ParserConfigurationException, SAXException,
      TransformerConfigurationException, TransformerException {
    Element xmlExtensions = getXMLObject("extensions", EXTENSIONS_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertExtensions(xmlExtensions, null, 0);

    Bundle bundle = getTestBundle(TB5_TEST_BUNDLE_SYMBOLIC_NAME, TB5);
    bundle.start();

    xmlExtensions = getXMLObject(EXTENSIONS_URI, EXTENSIONS_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertExtensions(xmlExtensions, Arrays.asList(new String[]{"REST TCK Extension", "contributions/extension"})/* name, path*/, 1);

    bundle = getTestBundle(TB6_TEST_BUNDLE_SYMBOLIC_NAME, TB6);
    bundle.start();

    xmlExtensions = getXMLObject(EXTENSIONS_URI, EXTENSIONS_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertExtensions(xmlExtensions, Arrays.asList(new String[]{"REST TCK Extension", "contributions/extension",
        "REST Extension full URI", "http://127.0.0.1/ct/rest/extension"})/* name, path, name, path*/, 2);

    bundle.stop();
    xmlExtensions = getXMLObject(EXTENSIONS_URI, EXTENSIONS_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertExtensions(xmlExtensions, Arrays.asList(new String[]{"REST TCK Extension", "contributions/extension"}), 1);

    bundle = getTestBundle(TB5_TEST_BUNDLE_SYMBOLIC_NAME, TB5);
    bundle.stop();

    xmlExtensions = getXMLObject("extensions", EXTENSIONS_CONTENT_TYPE_XML, HttpURLConnection.HTTP_OK);
    assertExtensions(xmlExtensions, null, 0);
  }


  protected void assertFWStartLevel(FrameworkStartLevel frameworkStartLevel, Element element) {
    String startLevelResult = getTextValue(element, "startLevel");
    String initialBundleStartLevelResult = getTextValue(element, "initialBundleStartLevel");

    assertThat(startLevelResult).as("startLevel element missing.").isNotNull();
    assertThat(initialBundleStartLevelResult).as("initialBundleStartLevel element missing").isNotNull();

    assertThat(Integer.parseInt(startLevelResult)).as("startLevel is not equal.").isEqualTo(frameworkStartLevel.getStartLevel());
    assertThat(Integer.parseInt(initialBundleStartLevelResult)).as("initialBundleStartLevel is not equal.").isEqualTo(frameworkStartLevel.getInitialBundleStartLevel());
  }

  protected void assertBundleRepresentation(Bundle bundle, Element element) {
    String lastModifiedStr = getTextValue(element, "lastModified");
    assertThat(lastModifiedStr).as("lastModified element missing.").isNotNull();

    String locationStr = getTextValue(element, "location");
    assertThat(locationStr).as("location element missing.").isNotNull();

    String stateStr = getTextValue(element, "state");
    assertThat(stateStr).as("state element missing.").isNotNull();

    String symbolicNameStr = getTextValue(element, "symbolicName");
    assertThat(symbolicNameStr).as("symbolicName element missing.").isNotNull();

    String versionStr = getTextValue(element, "version");
    assertThat(versionStr).as("version element missing.").isNotNull();

    assertThat(Long.valueOf(lastModifiedStr).longValue()).as("lastModified:").isEqualTo(bundle.getLastModified());
    assertThat(locationStr).as("location:").isEqualTo(bundle.getLocation());
    assertThat(Integer.valueOf(stateStr).intValue()).as("state:").isEqualTo(bundle.getState());
    assertThat(symbolicNameStr).as("symbolicName:").isEqualTo(bundle.getSymbolicName());
    assertThat(versionStr).as("version:").isEqualTo(bundle.getVersion().toString());
  }

  protected void assertBundleListRepresentation(Bundle[] bundles, Element xmlBundleList) {
    assertThat(xmlBundleList).as("Bundle list not null.").isNotNull();

    NodeList bundleList = xmlBundleList.getElementsByTagName("uri");
    assertThat(bundleList.getLength()).as("Bundle list length:").isEqualTo(bundles.length);

    HashSet<String> bundleURIs = new HashSet<String>();
    for (int k = 0; k < bundleList.getLength(); k++) {
      Element bundleElement = (Element)bundleList.item(k);
      String uri = getTextValue(bundleElement);
      bundleURIs.add(uri);
    }

    for (Bundle bundle : bundles) {
      String bundleURI = getBundleURI(bundle.getBundleId());
      assertThat(bundleURIs.contains(bundleURI)).as("Bundle list contains " + bundleURI + " :").isTrue();
    }
  }

  protected void assertBundleRepresentationList(Bundle[] bundles, Element xmlBundleList) {
    assertThat(xmlBundleList).as("Bundle list not null.").isNotNull();
    NodeList bundleList = xmlBundleList.getElementsByTagName("bundle");

    assertThat(bundleList.getLength()).as("Bundle representations list length:").isEqualTo(bundles.length);

    Hashtable<String, Element> bundleRepresentations = new Hashtable<String, Element>();
    for (int k = 0; k < bundleList.getLength(); k++) {
      Element bundleElement = (Element)bundleList.item(k);
      String id = getTextValue(bundleElement, "id");

      bundleRepresentations.put(id, bundleElement);
    }

    for (Bundle bundle : bundles) {
      long bundleId = bundle.getBundleId();
      Element bundleRepresentation = bundleRepresentations.get(String.valueOf(bundleId));
      assertThat(bundleRepresentation).as("Bundle representations list contains " + bundleId + " :").isNotNull();
      assertBundleRepresentation(bundle, bundleRepresentation);
    }
  }

  protected void assertBundleStateRepresentation(Bundle bundle, Element bundleState) {
    String stateStr = getTextValue(bundleState, "state");
    assertThat(stateStr).as("Bundle state.").isNotNull();

    assertThat(Integer.valueOf(stateStr).intValue()).as("state:").isEqualTo(bundle.getState());
  }

  protected void assertBundleHeaderRepresentation(Bundle bundle, Element bundleHeader) {
    assertThat(bundleHeader).as("Bundle header not null.").isNotNull();
    NodeList headerList = bundleHeader.getElementsByTagName("entry");

    Hashtable<String, String> responseHeaders = new Hashtable<String, String>();
    for (int k = 0; k < headerList.getLength(); k++) {
      Element entryElement = (Element)headerList.item(k);
      String key = entryElement.getAttribute("key");
      String value = entryElement.getAttribute("value");

      responseHeaders.put(key, value);
    }

    Dictionary<String, String> headers = bundle.getHeaders();

    assertThat(responseHeaders.size()).as("Headers size").isEqualTo(headers.size());

    for (Enumeration<String> keys = headers.keys(); keys.hasMoreElements();) {
      String key = keys.nextElement();
      assertThat(responseHeaders.get(key)).as(key + ":").isEqualTo(headers.get(key));
    }
  }

  protected void assertBundleStartLevelRepresentation(Bundle bundle, Element bundleStartLevelRepresentation) {
    assertThat(bundleStartLevelRepresentation).as("Bundle StartLevel not null.").isNotNull();

    String startLevelStr = getTextValue(bundleStartLevelRepresentation, "startLevel");
    assertThat(startLevelStr).as("startLevel not null.").isNotNull();

    String activationPolicyUsedStr = getTextValue(bundleStartLevelRepresentation, "activationPolicyUsed");
    assertThat(activationPolicyUsedStr).as("activationPolicyUsed not null.").isNotNull();

    String persistentlyStartedStr = getTextValue(bundleStartLevelRepresentation, "persistentlyStarted");
    assertThat(persistentlyStartedStr).as("persistentlyStarted not null.").isNotNull();

    BundleStartLevel bundleStartLevel = getBundleStartLevel(bundle);

    assertThat(Integer.valueOf(startLevelStr).intValue()).as("startLevel:").isEqualTo(bundleStartLevel.getStartLevel());
    assertThat(Boolean.valueOf(activationPolicyUsedStr).booleanValue()).as("activationPolicyUsed:").isEqualTo(bundleStartLevel.isActivationPolicyUsed());
    assertThat(Boolean.valueOf(persistentlyStartedStr).booleanValue()).as("persistentlyStarted:").isEqualTo(bundleStartLevel.isPersistentlyStarted());
  }

  protected void assertServiceList(Element xmlServiceList, ServiceReference<?>[] serviceRefs) {
    assertThat(xmlServiceList).as("Service list").isNotNull();

    NodeList serviceList = xmlServiceList.getElementsByTagName("uri");
    assertThat(serviceList).as("Service uri list ").isNotNull();
    assertThat(serviceList.getLength()).as("Service list length:").isEqualTo(serviceRefs.length);

    HashSet<String> serviceURIs = new HashSet<String>();
    for (int k = 0; k < serviceList.getLength(); k++) {
      Element serviceURIElement = (Element)serviceList.item(k);
      String uri = getTextValue(serviceURIElement);
      serviceURIs.add(uri);
    }

    for (ServiceReference<?> serviceRef : serviceRefs) {
      String serviceURI = getServiceURI(serviceRef.getProperty(Constants.SERVICE_ID).toString());
      assertThat(serviceURIs.contains(serviceURI)).as("Service list contains " + serviceURI + ": ").isTrue();
    }
  }

  protected void assertServiceRepresentationList(Element xmlServiceRepresentationList, ServiceReference<?>[] serviceRefs) {
    if (serviceRefs != null) {
      assertThat(xmlServiceRepresentationList).as("Service representation list").isNotNull();

      NodeList serviceList = xmlServiceRepresentationList.getElementsByTagName("service");
      assertThat(serviceList).as("Service list").isNotNull();

      Hashtable<Object, Element> serviceRepresentationHT = new Hashtable<Object, Element>();

      for (int k = 0; k < serviceList.getLength(); k++) {
        Element serviceElement = (Element)serviceList.item(k);
        try {
          NodeList propertiesList = serviceElement.getElementsByTagName("properties");
          assertThat(propertiesList).as("Service representation properties ").isNotNull();
          Element propertiesElement = (Element)propertiesList.item(0);

          NodeList propertyList = propertiesElement.getElementsByTagName("property");
          assertThat(propertyList).as("Property list").isNotNull();

          for (int i = 0; i < propertyList.getLength(); i++) {
            Element propertyElement = (Element)propertyList.item(i);
            String name = propertyElement.getAttribute("name");
            if (Constants.SERVICE_ID.equals(name)) {

              String serviceId = propertyElement.getAttribute("value");
              serviceRepresentationHT.put(serviceId, serviceElement);
            }
          }

        } catch (Exception cause) {
          fail(cause.getMessage());
        }
      }

      assertThat(serviceRepresentationHT.size()).as("Service representation list size").isEqualTo(serviceRefs.length);
      for (ServiceReference<?> serviceRef : serviceRefs) {
        String id = String.valueOf(serviceRef.getProperty(Constants.SERVICE_ID));
        debug("Assert service:" + id, null);
        assertService(serviceRepresentationHT.get(id), serviceRef);
      }
    }
  }

  protected void assertBundleException(Element xmlBundleException, String assertMessage) {
    assertThat(xmlBundleException).as(assertMessage).isNotNull();

    String typecodeResult = getTextValue(xmlBundleException, "typecode");
    String messageResult = getTextValue(xmlBundleException, "message");

    assertThat(Integer.parseInt(typecodeResult) >= 0).as(assertMessage).isTrue(); // some of BundleException error codes
    assertThat(messageResult != null).as("Error message is not null.").isTrue();
  }

  /*
<service>
  <id>10</id>
  <properties>
    <property name="prop1" value="val1"/>
    <property name="prop2" type="Float" value="2.82"/>
    ...
    <property name="prop3" type="Boolean" value="true"/>
  </properties>
  <bundle>bundleURI</bundle>
  <usingBundles>
    <bundle>bundleURI</bundle>
    <bundle>bundleURI</bundle>
    ...
    <bundle>bundleURI</bundle>
  </usingBundles>
</service>
   */
  protected void assertService(Element xmlServiceRepresentation, ServiceReference<?> serviceRef) {
    assertThat(xmlServiceRepresentation).as("Service representation ").isNotNull();

    String serviceId = serviceRef.getProperty(Constants.SERVICE_ID).toString();

    String idResult = getTextValue(xmlServiceRepresentation, "id");
    assertThat(idResult).as("Id property.").isNotNull();

    String bundleResult = getTextValue(xmlServiceRepresentation, "bundle");
    assertThat(bundleResult).as("Bundle property.").isEqualTo(getBundleURI(serviceRef.getBundle()));

    Bundle[] usingBundles = serviceRef.getUsingBundles();
    NodeList nodes = xmlServiceRepresentation.getElementsByTagName("usingBundles");
    if (usingBundles != null) {
      HashSet<String> uBundlesSet = new HashSet<String>();
      Element element = (Element)nodes.item(0);
      NodeList bundles = element.getElementsByTagName("bundle");
      assertThat(bundles).as("usingBundles not null.").isNotNull();
      for (int k = 0; k < bundles.getLength(); k++) {
        Element bundle = (Element)bundles.item(k);
        String value = bundle.getFirstChild().getNodeValue();
        uBundlesSet.add(value);
      }

      assertThat(uBundlesSet.size()).as("usingBundles length.").isEqualTo(usingBundles.length);

      for (int k = 0; k < usingBundles.length; k++) {
        assertThat(uBundlesSet.contains(getBundleURI(usingBundles[k]))).as("usingBundle " + usingBundles[k].getBundleId()).isTrue();
      }
    } else {
      if (nodes != null && nodes.getLength() != 0) {
        Element element = (Element)nodes.item(0);
        NodeList bundles = element.getElementsByTagName("bundle");

        assertThat(bundles == null || bundles.getLength() == 0).as("usingBundles.").isTrue();
      }
    }

    String[] propKeys = serviceRef.getPropertyKeys();
    NodeList props = xmlServiceRepresentation.getElementsByTagName("properties");

    if (propKeys != null) {
      Hashtable<String, List<String>> xmlProps = new Hashtable<String, List<String>>();
      Element propsElement = (Element)props.item(0);

      NodeList propertyNodeList = propsElement.getElementsByTagName("property");
      assertThat(propertyNodeList).as("properties not null.").isNotNull();

      for (int k = 0; k < propertyNodeList.getLength(); k++) {
        Element propertyElement = (Element)propertyNodeList.item(k);
        String name = propertyElement.getAttribute("name");

        List<String> val = new ArrayList<String>();
        xmlProps.put(name, val);

				if (propertyElement.hasAttribute("value")) {
					String valueAttribute = propertyElement.getAttribute("value");
					val.add(valueAttribute);
				} else {
					String valueContent = propertyElement.getTextContent();
					if (valueContent != null) {
						String[] values = valueContent.split("\\n");
						for (String value : values) {
							String v = value.trim();
							if (v.length() > 0)
								val.add(v);
						}
					}
				}
      }

      assertThat(xmlProps.size()).as("Properties size.").isEqualTo(propKeys.length);
      for (String key : propKeys) {
        Object value = serviceRef.getProperty(key);

        List<String> propertyList = xmlProps.get(key);
        assertThat(propertyList).as("Service property " + key).isNotNull();

        if (value.getClass().isArray()) {
          int length = Array.getLength(value);

          debug("Service property '" + key + "' has Array value", null);
          debug("Property value from xml:" + propertyList, null);
          debug("Property value :" + Arrays.toString((Object[])value), null);

          assertThat(propertyList.size()).as("Service '" + serviceId + "'. Array property '" + key + "' value size.").isEqualTo(length);

          for (int k = 0; k < length; k++) {
            Object arrayValue = Array.get(value, k);
            assertThat(propertyList.contains(arrayValue.toString())).as("Service '" + serviceId + "'. Array property '" + key + "' arrayValue:'" + arrayValue + "'.").isTrue();
          }

        } else {
          assertThat(propertyList.size() == 1).as("Single property value.").isTrue();
          if (value instanceof Number) {
            assertThat(Integer.parseInt(propertyList.get(0))).as("Service number property " + key).isEqualTo(((Number)value).intValue());
          } else {
            assertThat(propertyList.get(0)).as("Service property value for " + key).isEqualTo(value);
          }
        }
      }
    } else {
      assertThat(props == null || props.getLength() == 0).as("No properties for service '" + serviceId + "'.").isTrue();
    }
  }

  protected void assertExtensions(Element xmlExtensionsList, List<String> extProps /* name, path, name, path*/,
      int expectedExtensionsCount) {
    if (expectedExtensionsCount == 0) {
      assertThat(xmlExtensionsList == null || !xmlExtensionsList.hasChildNodes()).as("No extensions.").isTrue();
      return;
    }

    assertThat(xmlExtensionsList).as("Extensions are not null.").isNotNull();

    NodeList extNodeList = xmlExtensionsList.getElementsByTagName("extension");
    assertThat(extNodeList.getLength()).as("Extensions size is " + expectedExtensionsCount + ".").isEqualTo(expectedExtensionsCount);

    HashMap<String, String> extensions = new HashMap<String, String>();
    for (int k = 0; k < extNodeList.getLength(); k++) {
      Element extElement = (Element)extNodeList.item(k);

      String name = getTextValue(extElement, "name");
      String path = getTextValue(extElement, "path");

      assertThat(name).as("Name is not null.").isNotNull();
      assertThat(path).as("Path is not null.").isNotNull();

      extensions.put(name, path);
    }

    for (int k = 0; k < extProps.size(); k++) {
      String expectedName = extProps.get(k++);
      String expectedPath = extProps.get(k);

      String path = extensions.get(expectedName);

      assertThat(path != null).as("Extensions list contains " + expectedName + ".").isTrue();
      assertThat(path).as("Extension path.").isEqualTo(expectedPath);
    }
  }

  protected void updateFWStartLevel(int startLevel, int initialBundleStartLevel, int expectedStatusCode, String acceptType)
      throws ParserConfigurationException, TransformerConfigurationException, TransformerException {
    Document doc = getDocumentBuilder().newDocument();
    Element rootNode = doc.createElement("frameworkStartLevel");
    doc.appendChild(rootNode);

    Element startLevelElement = doc.createElement("startLevel");
    rootNode.appendChild(startLevelElement);
    startLevelElement.setTextContent(String.valueOf(startLevel));

    Element initialBundleStartLevelElement = doc.createElement("initialBundleStartLevel");
    rootNode.appendChild(initialBundleStartLevelElement);
    initialBundleStartLevelElement.setTextContent(String.valueOf(initialBundleStartLevel));

    String strBody = documentToString(doc);
    debug("strBody:" + strBody, null);

    executeRequest(FW_START_LEVEL_URI, "PUT", APPLICATION_XML, acceptType, null, expectedStatusCode, null, strBody);
  }

  protected Element installBundle(String requestURI, URL url, String invalidLocation, String locationHeader, boolean byLocation,
      int expectedStatusCode) throws IOException, ParserConfigurationException, SAXException {
    InputStream is = null;
    if (byLocation) {
      is = executeRequest(requestURI, "POST", "text/plain", APPLICATION_XML, BUNDLE_EXCEPTION_CONTENT_TYPE_XML, expectedStatusCode,
        null /* additionalProps */, invalidLocation == null ? url.toString() : invalidLocation);
    } else {
      HashMap<String, String> additionalProps = new HashMap<String, String>();
      if (locationHeader != null) {
        additionalProps.put("Content-Location", locationHeader);
      }

      is = executeRequest(requestURI, "POST", "vnd.osgi.bundle", APPLICATION_XML, BUNDLE_EXCEPTION_CONTENT_TYPE_XML, expectedStatusCode,
        additionalProps, invalidLocation == null ? url.openStream() : new ByteArrayInputStream(invalidLocation.getBytes()));
    }

    try {
      if (expectedStatusCode == HttpURLConnection.HTTP_BAD_REQUEST) { // BundleException
        return getXMLElement(is);
      }
    } finally {
      if (is != null) {
        try {
          is.close();
				} catch (Throwable i) {
          /**/
        }
      }
    }

    return null;
  }

  protected Element updateBundle(String requestURI, URL url, String invalidLocation, boolean byLocation, int expectedStatusCode,
      String expectedContentType) throws IOException, ParserConfigurationException, SAXException {
    InputStream is = null;
    if (byLocation) {
      is = executeRequest(requestURI, "PUT", "text/plain", APPLICATION_XML, expectedContentType, expectedStatusCode,
        null /* additionalProps */, invalidLocation == null ? url.toString() : invalidLocation);
    } else {
      is = executeRequest(requestURI, "PUT", "vnd.osgi.bundle", APPLICATION_XML, expectedContentType, expectedStatusCode,
        null /* additionalProps */, invalidLocation == null ? url.openStream() : new ByteArrayInputStream(invalidLocation.getBytes()));
    }

    try {
      return getXMLElement(is);
    } finally {
      if (is != null) {
        try {
          is.close();
				} catch (Throwable i) {
          /**/
        }
      }
    }
  }

  protected Element uninstallBundle(String requestURI, int expectedStatusCode) throws IOException, SAXException,
      ParserConfigurationException {
    InputStream is = executeRequest(requestURI, "DELETE", null, APPLICATION_XML, null, expectedStatusCode,
      null /* additionalProps */, null);

    try {
      return getXMLElement(is);
    } finally {
      if (is != null) {
        try {
          is.close();
				} catch (Throwable i) {
          /**/
        }
      }
    }
  }

  protected Element updateBundleState(String requestURI, int newState, int options, int expectedResponseCode, String acceptType,
      String contentType, String expectedContentType) throws IOException, SAXException, ParserConfigurationException,
      TransformerConfigurationException, TransformerException {
    Document doc = getDocumentBuilder().newDocument();
    Element rootNode = doc.createElement("bundleState");
    doc.appendChild(rootNode);

    Element stateElement = doc.createElement("state");
    rootNode.appendChild(stateElement);
    stateElement.setTextContent(String.valueOf(newState));

    if (options != -1) {
      Element optionsElement = doc.createElement("options");
      rootNode.appendChild(optionsElement);
      optionsElement.setTextContent(String.valueOf(options));
    }

    String strBody = documentToString(doc);
    debug("strBody:" + strBody, null);

    InputStream is = executeRequest(requestURI, "PUT", contentType, acceptType, expectedContentType, expectedResponseCode, null, strBody);

    // Bundle state representation or BundleException
    try {
      return getXMLElement(is);
    } finally {
      if (is != null) {
        try {
          is.close();
				} catch (Throwable i) {
          /**/
        }
      }
    }
  }

  protected Element updateBundleStartLevel(String requestURI, int newStartLevel, int expectedResponseCode, String acceptType,
      String contentType) throws IOException, TransformerConfigurationException, SAXException, ParserConfigurationException,
      TransformerException {
    Document doc = getDocumentBuilder().newDocument();
    Element rootNode = doc.createElement("bundleStartLevel");
    doc.appendChild(rootNode);

    Element startLevelElement = doc.createElement("startLevel");
    rootNode.appendChild(startLevelElement);
    startLevelElement.setTextContent(String.valueOf(newStartLevel));

    String strBody = documentToString(doc);
    debug("strBody:" + strBody, null);

    InputStream is = executeRequest(requestURI, "PUT", contentType, acceptType, null, expectedResponseCode, null, strBody);

    try {
      if (expectedResponseCode == HttpURLConnection.HTTP_OK || expectedResponseCode == HttpURLConnection.HTTP_NOT_FOUND) {
        return getXMLElement(is);
      }
    } finally {
      if (is != null) {
        try {
          is.close();
				} catch (Throwable i) {
          /**/
        }
      }
    }

    return null;
  }

  protected Element getXMLObject(String uri, String expectedContentType, int expectedResponseCode)
      throws IOException, ParserConfigurationException, SAXException {
    InputStream is = executeRequest(uri, "GET", null, APPLICATION_XML, expectedContentType, expectedResponseCode, null, null);

    try {
      if (expectedResponseCode == HttpURLConnection.HTTP_OK) {
        return getXMLElement(is);
      }
    } finally {
      if (is != null) {
        try {
          is.close();
				} catch (Throwable i) {
          /**/
        }
      }
    }

    return null;
  }

  protected InputStream executeRequest(String uri, String method, String contentType, String acceptType, String expectedContentType,
      int expectedResponseCode, Map<String, String> additionalProps, Object body) {
    HttpURLConnection connection = null;
    InputStream in = null;
    try {
      connection = getHttpConnection(baseURI + uri, method, acceptType, contentType, additionalProps);
      if (body != null) {
        connection.setDoInput(true);
        connection.setDoOutput(true);
        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        if (body instanceof String) {
          out.writeBytes((String)body);
        } else if (body instanceof InputStream) {
          InputStream is = (InputStream)body;
          try {
            byte[] buffer = new byte[1024];
            int bytesRead = 0;
            while ((bytesRead = is.read(buffer, 0, 1024)) != -1) {
              out.write(buffer, 0, bytesRead);
            }
          } finally {
            try {
              is.close();
						} catch (Throwable i) {
							/**/
            }
          }
        }
        out.flush();
        out.close();
      }

      connection.connect();

      int responseCode = connection.getResponseCode();
      assertThat(responseCode).as("Response code").isEqualTo(expectedResponseCode);

      if (responseCode == HttpURLConnection.HTTP_OK) {
        String responseContentType = connection.getContentType();
        debug("Response ContentType:" + responseContentType, null);
        if (expectedContentType != null) {
          assertThat((responseContentType != null) && (responseContentType.startsWith(expectedContentType))).as("ContentType").isTrue();
        }
        in = connection.getInputStream();
      } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
        String responseContentType = connection.getContentType();
        debug("Response ContentType:" + responseContentType, null);
        if (expectedContentType != null) {
          assertThat((responseContentType != null) && (responseContentType.startsWith(expectedContentType))).as("ContentType").isTrue();
        }

        in = connection.getErrorStream();
      }

      return in;
    } catch (IOException cause) {
      fail(cause.getMessage());
    }

    return null;
  }

  private Element getXMLElement(InputStream is) throws IOException, ParserConfigurationException, SAXException {
    if (is != null) {
      InputStream data = validate(is);
      Document document = getDocumentBuilder().parse(data);
			if (debugOn) {
				printDocument(document, System.out);
			}
      return document.getDocumentElement();
    }

    return null;
  }

	public static void printDocument(Document doc, OutputStream out)
			throws IOException {
		try {
			TransformerFactory tf = TransformerFactory.newInstance();
			Transformer transformer = tf.newTransformer();
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"no");
			transformer.setOutputProperty(OutputKeys.METHOD, "xml");
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");

			transformer.transform(new DOMSource(doc),
					new StreamResult(new OutputStreamWriter(out, "UTF-8")));
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}

  protected InputStream validate(InputStream data) throws SAXException, IOException {
    if (validateXMLRepresentations) {
      byte[] byteArrCopy = toByteArray(data);
      Source source = new StreamSource(new ByteArrayInputStream(byteArrCopy));
      validator.validate(source);

      return new ByteArrayInputStream(byteArrCopy);
    }

    return data;
  }

  private String getTextValue(Element ele, String tagName) {
    String textVal = null;
    NodeList nl = ele.getElementsByTagName(tagName);
		if (nl == null) {
			return textVal;
		}
		int len = nl.getLength();
		for (int i = 0; i < len; i++) {
			Element el = (Element) nl.item(i);
			if (ele.isSameNode(el.getParentNode())) {
				textVal = getTextValue(el);
				break;
			}
		}

    return textVal;
  }

  private String getTextValue(Element element) {
    return element.getFirstChild().getNodeValue();
  }

  private String documentToString(Document doc) throws TransformerConfigurationException, TransformerException {
    StringWriter writer = new StringWriter();
    StreamResult result = new StreamResult(writer);
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.transform(new DOMSource(doc), result);
    return writer.toString();
  }

  private DocumentBuilder getDocumentBuilder() throws ParserConfigurationException {
    return documentBuilder;
  }

}
