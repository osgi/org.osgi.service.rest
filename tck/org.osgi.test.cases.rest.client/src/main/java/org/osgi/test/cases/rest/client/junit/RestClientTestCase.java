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
package org.osgi.test.cases.rest.client.junit;

import static org.osgi.test.assertj.bundle.BundleAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.dto.BundleDTO;
import org.osgi.framework.dto.ServiceReferenceDTO;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.framework.startlevel.FrameworkStartLevel;
import org.osgi.framework.startlevel.dto.BundleStartLevelDTO;
import org.osgi.framework.startlevel.dto.FrameworkStartLevelDTO;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.resource.Capability;
import org.osgi.resource.Namespace;
import org.osgi.service.rest.client.RestClient;
import org.osgi.service.rest.client.RestClientFactory;

/**
 * Tests {@link RestClient} OSGi Service.
 *
 * @author Petia Sotirova
 */
public class RestClientTestCase extends RestTestUtils {

  private ServiceReference<RestClientFactory> restClientFactoryRef;
  private RestClient restClient;

  @Override
  @BeforeEach
  public void setUp() throws Exception {
    super.setUp();
    restClientFactoryRef = context.getServiceReference(RestClientFactory.class);
		assertThat(restClientFactoryRef).as("RestClientFactory ServiceReference is not available!").isNotNull();
    RestClientFactory restClientFactory = context.getService(restClientFactoryRef);
		assertThat(restClientFactory).as("RestClientFactory service is not available!").isNotNull();
    restClient = restClientFactory.createRestClient(new URI(baseURI));
		assertThat(restClient).as("RestClient is not available!").isNotNull();
  }

  @Override
  @AfterEach
  public void tearDown() throws Exception {
    if (restClientFactoryRef != null) {
      context.ungetService(restClientFactoryRef);
    }
    super.tearDown();
  }

  @Test
  public void testFrameworkStartLevelRestClient() throws Exception {
    FrameworkStartLevel frameworkStartLevel = getFrameworkStartLevel();
    int originalStartLevel = frameworkStartLevel.getStartLevel();
    int originalInitialBundleStartLevel = frameworkStartLevel.getInitialBundleStartLevel();

    FrameworkStartLevelDTO frameworkStartLevelDTO = getRestClient().getFrameworkStartLevel();
    assertThat(frameworkStartLevelDTO).as("frameworkStartLevelDTO").isNotNull();

    assertThat(frameworkStartLevelDTO.startLevel).as("original startLevel").isEqualTo(frameworkStartLevel.getStartLevel());
    assertThat(frameworkStartLevelDTO.initialBundleStartLevel).as("original initialBundleStartLevel").isEqualTo(frameworkStartLevel.getInitialBundleStartLevel());

    FrameworkStartLevelDTO updateFWStartLevelDTO = new FrameworkStartLevelDTO();
    updateFWStartLevelDTO.startLevel = originalStartLevel;
    updateFWStartLevelDTO.initialBundleStartLevel = originalInitialBundleStartLevel + 1;

    getRestClient().setFrameworkStartLevel(updateFWStartLevelDTO);

    frameworkStartLevel = getFrameworkStartLevel();

    assertThat(frameworkStartLevel.getStartLevel()).as("startLevel after set").isEqualTo(updateFWStartLevelDTO.startLevel);
    assertThat(frameworkStartLevel.getInitialBundleStartLevel()).as("initialBundleStartLevel after set").isEqualTo(updateFWStartLevelDTO.initialBundleStartLevel);

    frameworkStartLevelDTO = getRestClient().getFrameworkStartLevel();
    assertThat(frameworkStartLevelDTO).as("frameworkStartLevelDTO").isNotNull();
    assertThat(frameworkStartLevelDTO.startLevel).as("startLevel").isEqualTo(frameworkStartLevel.getStartLevel());
    assertThat(frameworkStartLevelDTO.initialBundleStartLevel).as("initialBundleStartLevel").isEqualTo(frameworkStartLevel.getInitialBundleStartLevel());

		frameworkStartLevel.setStartLevel(originalStartLevel);
    frameworkStartLevel.setInitialBundleStartLevel(originalInitialBundleStartLevel);

    frameworkStartLevelDTO = getRestClient().getFrameworkStartLevel();
    assertThat(frameworkStartLevelDTO).as("frameworkStartLevelDTO").isNotNull();
    assertThat(frameworkStartLevelDTO.startLevel).as("updated startLevel").isEqualTo(originalStartLevel);
    assertThat(frameworkStartLevelDTO.initialBundleStartLevel).as("updated initialBundleStartLevel").isEqualTo(frameworkStartLevel.getInitialBundleStartLevel());

    updateFWStartLevelDTO = new FrameworkStartLevelDTO();
    updateFWStartLevelDTO.startLevel = -1;
    updateFWStartLevelDTO.initialBundleStartLevel = originalInitialBundleStartLevel;

    boolean receiveError = false;
    try {
      getRestClient().setFrameworkStartLevel(updateFWStartLevelDTO);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Error for updating framework start level with negative value ").isTrue();
  }

  @Test
  public void testBundleListRestClient() throws Exception {
    Collection<String> bundleCollection = getRestClient().getBundlePaths();

    Bundle[] bundles = getInstalledBundles();
    assertBundleCollection(bundles, bundleCollection);

    Bundle tb1Bundle = getBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME);
    if (tb1Bundle != null) { // test bundle is already installed => uninstall
      tb1Bundle.uninstall();
    }
    String url = context.getBundle().getEntry(TB1).toString();

    // install bundle with location
    BundleDTO result = getRestClient().installBundle(url);
    assertThat(result).as("Bundle location for installed bundle is not null").isNotNull();

    tb1Bundle = getBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME);
    assertThat(tb1Bundle).as("Test bundle TB1 is installed").isNotNull();

    assertThat(getBundleURI(result.id)).as("Bundle location").isEqualTo(getBundleURI(tb1Bundle));

    // same bundle location
    boolean receiveError = false;
    try {
      getRestClient().installBundle(url);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Install bundle by same URI").isTrue();

    // invalid bundle location
    receiveError = false;
    try {
      getRestClient().installBundle("invalid bundle location");
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Install bundle by invalid URI").isTrue();

    // install bundle with bundle content
    Bundle tb2Bundle = getBundle(TB2_TEST_BUNDLE_SYMBOLIC_NAME);
    if (tb2Bundle != null) { // test bundle is already installed => uninstall
      tb2Bundle.uninstall();
    }

    String locationHeader = "/tb2.rest.test.location";
    InputStream in = context.getBundle().getEntry(TB2).openStream();
    result = getRestClient().installBundle(locationHeader, in);

    assertThat(result).as("Bundle location for installed bundle is not null").isNotNull();

    tb2Bundle = getBundle(TB2_TEST_BUNDLE_SYMBOLIC_NAME);
    assertThat(tb2Bundle).as("Test bundle TB2 is installed").isNotNull();

	assertThat(getBundleURI(result.id)).as("Bundle location").isEqualTo(getBundleURI(tb2Bundle));

    // same bundle location
    receiveError = false;
    try {
      in = context.getBundle().getEntry(TB2).openStream();
      getRestClient().installBundle(locationHeader, in);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Install bundle by same location header").isTrue();

    // "Install bundle by invalid bundle content."
    receiveError = false;
    try {
      getRestClient().installBundle("invalid-bundle-content", new ByteArrayInputStream("invalid-bundle-content".getBytes()));
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Install bundle by invalid bundle content.").isTrue();
  }

  @Test
  public void testBundleRepresentationsListRestClient() throws Exception {
    Collection<BundleDTO> bundleRepresentationsList = getRestClient().getBundles();
    assertThat(bundleRepresentationsList).as("Bundle representations list").isNotNull();

    Bundle[] bundles = getInstalledBundles();
    assertThat(bundleRepresentationsList).as("Bundle representations list length:").hasSize(bundles.length);

    Hashtable<Long, BundleDTO> bundleRepresentations = new Hashtable<Long, BundleDTO>();
    for (Iterator<BundleDTO> i = bundleRepresentationsList.iterator(); i.hasNext();) {
      BundleDTO bundleRepresentation = i.next();
      bundleRepresentations.put(bundleRepresentation.id, bundleRepresentation);
    }

    for (Bundle bundle : bundles) {
      long bundleId = bundle.getBundleId();
      BundleDTO bundleRepresentation = bundleRepresentations.get(bundleId);
      assertThat(bundleRepresentation).as("Bundle representations list contains " + bundleId + " :").isNotNull();
      assertBundleRepresentation(bundle, bundleRepresentation);
    }

    // filter ?
  }

  @Test
  public void testBundleRestClient() throws Exception  {
    Bundle bundle = getRandomBundle();

    // GET by bundleId
    BundleDTO bundleRepresentation = getRestClient().getBundle(bundle.getBundleId());
    assertThat(bundleRepresentation).as("Bundle representation : " + bundle.getBundleId() + " :").isNotNull();
    assertBundleRepresentation(bundle, bundleRepresentation);

    long notExistingBundleId = getNotExistingBundleId();
    bundleRepresentation = getRestClient().getBundle(notExistingBundleId);
    assertThat(bundleRepresentation).as("Bundle representation for not existing bundle " + notExistingBundleId + " :").isNull();

    // GET by bundle path
    bundleRepresentation = getRestClient().getBundle(getBundlePath(bundle));
    assertThat(bundleRepresentation).as("Bundle representation : " + bundle.getBundleId() + " :").isNotNull();
    assertBundleRepresentation(bundle, bundleRepresentation);

    String url = context.getBundle().getEntry(TB11).toString();

    // update with location
    Bundle tb1Bundle = getTestBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME, TB1);
    getRestClient().updateBundle(tb1Bundle.getBundleId(), url);

    Bundle tb11Bundle = getBundle(TB11_TEST_BUNDLE_SYMBOLIC_NAME);
    assertThat(tb11Bundle).as("Updated bundle not null").isNotNull();
    assertThat(tb11Bundle.getVersion().toString()).as("Updated bundle version").isEqualTo(TB11_TEST_BUNDLE_VERSION);

    boolean receiveError = false;
    try {
      getRestClient().updateBundle(notExistingBundleId, url);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Update not existing bundle " + notExistingBundleId + " finished with error").isTrue();

    receiveError = false;
    try {
      getRestClient().updateBundle(tb1Bundle.getBundleId(), "Invalid-bundle-location");
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Update with invalid URI finished with error").isTrue();

    // update with bundle content
    Bundle tb2Bundle = getTestBundle(TB2_TEST_BUNDLE_SYMBOLIC_NAME, TB2);
    getRestClient().updateBundle(tb2Bundle.getBundleId(), context.getBundle().getEntry(TB21).openStream());

    Bundle tb21Bundle = getBundle(TB21_TEST_BUNDLE_SYMBOLIC_NAME);
    assertThat(tb21Bundle).as("Updated bundle not null").isNotNull();
    assertThat(tb21Bundle.getVersion().toString()).as("Updated bundle version").isEqualTo(TB21_TEST_BUNDLE_VERSION);

    receiveError = false;
    try {
      getRestClient().updateBundle(notExistingBundleId, context.getBundle().getEntry(TB21).openStream());
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Update not existing bundle " + notExistingBundleId + " finished with error").isTrue();

    receiveError = false;
    try {
      getRestClient().updateBundle(tb2Bundle.getBundleId(), new ByteArrayInputStream("Invalid-bundle-content".getBytes()));
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Update with invalid content finished with error").isTrue();

    long tb1BundleId = tb11Bundle.getBundleId();
    getRestClient().uninstallBundle(tb1BundleId);
    tb11Bundle = getBundle(TB11_TEST_BUNDLE_SYMBOLIC_NAME);
    assertThat(tb11Bundle).as("Framework bundle for uninstalled bundle").isNull();

    receiveError = false;
    try {
      getRestClient().uninstallBundle(tb1BundleId);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Uninstall already uninstalled bundle " + tb1BundleId + " finished with error").isTrue();

    // stop tb2 bundle throws Exception
    tb21Bundle = getBundle(TB21_TEST_BUNDLE_SYMBOLIC_NAME);
    tb21Bundle.start();

    String tb2BundlePath = getBundlePath(tb2Bundle);

    receiveError = false;
    try {
      getRestClient().uninstallBundle(tb2BundlePath);
    } catch (Exception cause) {
      receiveError = true;
    }
    // assertTrue("Uninstall bundle with error in stop method " + tb2BundlePath, receiveError);

    tb21Bundle = getBundle(TB21_TEST_BUNDLE_SYMBOLIC_NAME);
    assertThat(tb21Bundle).as("Framework bundle for uninstalled bundle " + tb2BundlePath).isNull();
  }

  @Test
  public void testBundleStateRestClient() throws Exception {
    // GET
    Bundle bundle = getRandomBundle();
    int state = getRestClient().getBundleState(bundle.getBundleId());
    assertThat(state).as("Get state by bundle ID " + bundle.getBundleId()).isEqualTo(bundle.getState());

    state = getRestClient().getBundleState(getBundlePath(bundle));
    assertThat(state).as("Bundle state by bundle path " + getBundlePath(bundle)).isEqualTo(bundle.getState());

    long notExistingBundleId = getNotExistingBundleId();
    boolean receiveError = false;
    try {
      getRestClient().getBundleState(notExistingBundleId);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Try to get state for non existing bundle " + notExistingBundleId).isTrue();

    receiveError = false;
    try {
      getRestClient().getBundleState("Illegal-bundle-path");
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Try to get state with illegal bundle path").isTrue();

    // start by bundle id
    Bundle tb1Bundle = getTestBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME, TB1);

    int tb1State = tb1Bundle.getState();
    if (tb1State == Bundle.ACTIVE) {
      tb1Bundle.stop();
    }

    getRestClient().startBundle(tb1Bundle.getBundleId());

    tb1Bundle = getBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME);  // is it needed?
    assertThat(tb1Bundle).as("New state ").isInState(Bundle.ACTIVE);

    // start by bundle path
    Bundle tb2Bundle = getTestBundle(TB2_TEST_BUNDLE_SYMBOLIC_NAME, TB2);
    if (tb2Bundle.getState() == Bundle.ACTIVE) {
      tb2Bundle.stop();
    }

    getRestClient().startBundle(getBundlePath(tb2Bundle));

    tb2Bundle = getBundle(TB2_TEST_BUNDLE_SYMBOLIC_NAME);
    assertThat(tb2Bundle).as("New state ").isInState(Bundle.ACTIVE);
    unisntallIfInstalled(TB2_TEST_BUNDLE_SYMBOLIC_NAME);

    receiveError = false;
    try {
      getRestClient().startBundle(notExistingBundleId);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Try to start non existing bundle " + notExistingBundleId).isTrue();

    // start bundle with Bundle-ActivationPolicy by bundle id
    Bundle tb3Bundle = getTestBundle(TB3_TEST_BUNDLE_SYMBOLIC_NAME, TB3);
    int tb3State = tb3Bundle.getState();
    if (tb3State == Bundle.ACTIVE || tb3State == Bundle.STARTING) {
      tb3Bundle.stop();
    }

    getRestClient().startBundle(tb3Bundle.getBundleId(), Bundle.START_ACTIVATION_POLICY);
    assertThat(tb3Bundle).as("New state for 'lazy' bundle ").isInState(Bundle.STARTING);

    // start bundle with Bundle-ActivationPolicy by bundle path
    Bundle tb4Bundle = getTestBundle(TB4_TEST_BUNDLE_SYMBOLIC_NAME, TB4);
    int tb4State = tb3Bundle.getState();
    if (tb4State == Bundle.ACTIVE || tb4State == Bundle.STARTING) {
      tb4Bundle.stop();
    }

    getRestClient().startBundle(getBundlePath(tb4Bundle), Bundle.START_ACTIVATION_POLICY);
    assertThat(tb4Bundle).as("New state for 'lazy' bundle ").isInState(Bundle.STARTING);

    // stop by bundle id
    Bundle tb21Bundle = getTestBundle(TB21_TEST_BUNDLE_SYMBOLIC_NAME, TB21);
    if (tb21Bundle.getState() != Bundle.ACTIVE) {
      tb21Bundle.start();
    }

    receiveError = false;
    try {
      getRestClient().stopBundle(tb21Bundle.getBundleId());
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Stop bundle for bundle with error in stop method " + tb21Bundle.getBundleId()).isTrue();

    // stop by bundle path
    getRestClient().stopBundle(getBundlePath(tb1Bundle));
    assertThat(tb1Bundle).as("New state ").isInState(Bundle.RESOLVED);

    // stop bundle by bundle id and options
    getRestClient().stopBundle(tb3Bundle.getBundleId(), Bundle.STOP_TRANSIENT);
    assertThat(tb3Bundle).as("New state ").isInState(Bundle.RESOLVED);

    // stop bundle by bundle path and options
    getRestClient().stopBundle(getBundlePath(tb4Bundle), Bundle.STOP_TRANSIENT);
    assertThat(tb4Bundle).as("New state ").isInState(Bundle.RESOLVED);
  }

  @Test
  public void testBundleHeaderRestClient() throws Exception {
    Bundle bundle = getRandomBundle();
    Map<String, String> bHeaders = getRestClient().getBundleHeaders(bundle.getBundleId());
    assertBundleHeaderRepresentation(bundle, bHeaders);

    bHeaders = getRestClient().getBundleHeaders(getBundlePath(bundle));
    assertBundleHeaderRepresentation(bundle, bHeaders);

    long notExistingBundleId = getNotExistingBundleId();

    boolean receiveError = false;
    try {
      getRestClient().getBundleHeaders(notExistingBundleId);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Get bundle headers for not existing bundle " + notExistingBundleId).isTrue();
  }

  @Test
  public void testBundleStartLevelRestClient() throws Exception {
    Bundle bundle = getRandomBundle();

    // get start level by bundle id
    BundleStartLevelDTO bundleStartLevelDTO = getRestClient().getBundleStartLevel(bundle.getBundleId());
    assertBundleStartLevelRepresentation(bundle, bundleStartLevelDTO);

    // get start level for non existing bundle id
    long notExistingBundleId = getNotExistingBundleId();
    boolean receiveError = false;
    try {
      getRestClient().getBundleStartLevel(notExistingBundleId);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Get bundle start level for not existing bundle " + notExistingBundleId).isTrue();

    // get start level by bundle path
    bundleStartLevelDTO = getRestClient().getBundleStartLevel(getBundlePath(bundle));
    assertBundleStartLevelRepresentation(bundle, bundleStartLevelDTO);

    // get start level for non existing bundle path
    String notExistingBundlePath = getNotExistingBundlePath();
    receiveError = false;
    try {
      getRestClient().getBundleStartLevel(notExistingBundlePath);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Get bundle start level for not existing bundle " + notExistingBundlePath).isTrue();

    // set start level by bundle id
    Bundle tb1Bundle = getTestBundle(TB1_TEST_BUNDLE_SYMBOLIC_NAME, TB1);

    int tb1StartLevel = getBundleStartLevel(tb1Bundle).getStartLevel();
    int tb1NewStartLevel = tb1StartLevel + 1;
    BundleStartLevelDTO tb1StartLevelDTO = new BundleStartLevelDTO();
    tb1StartLevelDTO.startLevel = tb1NewStartLevel;

    getRestClient().setBundleStartLevel(tb1Bundle.getBundleId(), tb1StartLevelDTO.startLevel);

    tb1StartLevel = getBundleStartLevel(tb1Bundle).getStartLevel();
    assertThat(tb1StartLevel).as("New start level ").isEqualTo(tb1NewStartLevel);

    // set start level by bundle id for non existing bundle id
    receiveError = false;
    try {
      getRestClient().setBundleStartLevel(notExistingBundleId, tb1StartLevelDTO.startLevel);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Set bundle start level for not existing bundle " + notExistingBundlePath).isTrue();

    // set illegal start level by bundle id
    receiveError = false;
    try {
      tb1StartLevelDTO.startLevel = -1;
      getRestClient().setBundleStartLevel(tb1Bundle.getBundleId(), tb1StartLevelDTO.startLevel);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Set negative bundle start level").isTrue();

    // set start level by bundle path
    Bundle tb2Bundle = getTestBundle(TB2_TEST_BUNDLE_SYMBOLIC_NAME, TB2);
    int tb2StartLevel = getBundleStartLevel(tb2Bundle).getStartLevel();
    int tb2NewStartLevel = tb2StartLevel + 1;
    BundleStartLevelDTO tb2StartLevelDTO = new BundleStartLevelDTO();
    tb2StartLevelDTO.startLevel = tb2NewStartLevel;

    getRestClient().setBundleStartLevel(getBundlePath(tb2Bundle), tb2StartLevelDTO.startLevel);
    tb2StartLevel = getBundleStartLevel(tb2Bundle).getStartLevel();
    assertThat(tb2StartLevel).as("New start level ").isEqualTo(tb2NewStartLevel);

    // set start level by bundle path for non existing bundle path
    receiveError = false;
    try {
      getRestClient().setBundleStartLevel(notExistingBundlePath, tb2StartLevelDTO.startLevel);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Set bundle start level for not existing bundle path " + notExistingBundlePath).isTrue();

    // set illegal start level by bundle path
    receiveError = false;
    try {
      tb2StartLevelDTO.startLevel = -1;
      getRestClient().setBundleStartLevel(getBundlePath(tb2Bundle), tb2StartLevelDTO.startLevel);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Set negative bundle start level").isTrue();
  }

  @Test
  public void testServiceListRestClient() throws Exception {
    String filter = null;

    Collection<String> services = getRestClient().getServicePaths();
    ServiceReference<?>[] serviceRefs = getServices(filter);
    assertServiceList(serviceRefs, services);

    filter = "(&(" + Constants.SERVICE_ID + ">=" + serviceRefs.length / 2 + ")"
        + "(" + Constants.SERVICE_ID + "<=" + serviceRefs.length + "))";

    services = getRestClient().getServicePaths(filter);
    serviceRefs = getServices(filter);
    assertServiceList(serviceRefs, services);

    String invalidFilter = "invalid-filter";
    boolean receiveError = false;
    try {
      services = getRestClient().getServicePaths(invalidFilter);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Request with invalid filter " + invalidFilter).isTrue();
  }

  @Test
  public void testServiceRepresentationsListRestClient() throws Exception {
    String filter = null;
    Collection<ServiceReferenceDTO> serviceRepresentationsDTO = getRestClient().getServiceReferences();
    ServiceReference<?>[] serviceRefs = getServices(filter);
    assertServiceRepresentationList(serviceRefs, serviceRepresentationsDTO);

    filter = "(&(" + Constants.SERVICE_ID + ">=" + serviceRefs.length / 2 + ")"
        + "(" + Constants.SERVICE_ID + "<=" + serviceRefs.length + "))";

    serviceRefs = getServices(filter);
    serviceRepresentationsDTO = getRestClient().getServiceReferences(filter);
    assertServiceRepresentationList(serviceRefs, serviceRepresentationsDTO);

    filter = "invalid-filter";
    boolean receiveError = false;
    try {
      getRestClient().getServicePaths(filter);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Request with invalid filter " + filter).isTrue();
  }

  @Test
  public void testServiceRestClient() throws Exception {
    ServiceReference<?> serviceRef = getRandomService();

    ServiceReferenceDTO serviceReferenceDTO = getRestClient().getServiceReference(getServiceId(serviceRef));
    assertService(serviceRef, serviceReferenceDTO);

    long notExistingSeriveId = System.currentTimeMillis();
    boolean receiveError = false;
    try {
      getRestClient().getServiceReference(notExistingSeriveId);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Request with non existing service id " + notExistingSeriveId).isTrue();

    serviceReferenceDTO = getRestClient().getServiceReference(getServicePath(serviceRef));
    assertService(serviceRef, serviceReferenceDTO);

    String notExistingSerivePath = "not-existing-servicve-" + System.currentTimeMillis();
    receiveError = false;
    try {
      getRestClient().getServiceReference(notExistingSerivePath);
    } catch (Exception cause) {
      receiveError = true;
    }
    assertThat(receiveError).as("Request with non existing service path " + notExistingSerivePath).isTrue();
  }

	/**
	 * A basic test that ensures the provider of the RestClientFactory service
	 * advertises the service capability
	 * 
	 * @throws Exception
	 */
	@Test
	public void testServiceCapability() throws Exception {

		List<BundleCapability> capabilities = restClientFactoryRef.getBundle()
				.adapt(BundleWiring.class)
				.getCapabilities("osgi.service");

		boolean hasCapability = false;
		boolean uses = false;

		for (Capability cap : capabilities) {
			@SuppressWarnings("unchecked")
			List<String> objectClass = (List<String>) cap.getAttributes().get("objectClass");

			if (objectClass.contains(RestClientFactory.class.getName())) {
				hasCapability = true;
				String usesDirective = cap.getDirectives().get(Namespace.CAPABILITY_USES_DIRECTIVE);
				if (usesDirective != null) {
					Set<String> packages = new HashSet<String>(Arrays.asList(usesDirective.trim().split("\\s*,\\s*")));
					uses = packages.contains("org.osgi.service.rest.client");
				}
				break;
			}
		}
		assertThat(hasCapability).as("No osgi.service capability for the RestClientFactory service").isTrue();
		assertThat(uses).as("Missing uses constraint on the osgi.service capability").isTrue();
	}

  public RestClient getRestClient() {
    return restClient;
  }

  protected void assertServiceRepresentationList(ServiceReference<?>[] serviceRefs, Collection<ServiceReferenceDTO> serviceRepresentationsDTO) {
    if (serviceRefs != null) {
      assertThat(serviceRepresentationsDTO).as("Service representation list").isNotNull();

      Hashtable<Long, ServiceReferenceDTO> serviceRepresentationHT = new Hashtable<Long, ServiceReferenceDTO>();

      for (Iterator<ServiceReferenceDTO> i = serviceRepresentationsDTO.iterator(); i.hasNext();) {
        ServiceReferenceDTO serviceReferenceDTO = i.next();
        try {
          serviceRepresentationHT.put(serviceReferenceDTO.id, serviceReferenceDTO);
        } catch (Exception cause) {
          fail(cause.getMessage());
        }
      }

      assertThat(serviceRepresentationHT).as("Service representation list size").hasSize(serviceRefs.length);
      for (ServiceReference<?> serviceRef : serviceRefs) {
        assertService(serviceRef, serviceRepresentationHT.get(serviceRef.getProperty(Constants.SERVICE_ID)));
      }
    }
  }

  protected void assertService(ServiceReference<?> serviceRef, ServiceReferenceDTO serviceReferenceDTO) {
    assertThat(serviceReferenceDTO).as("Service representation ").isNotNull();

    String[] propKeys = serviceRef.getPropertyKeys();
    Map<String, Object> propertiesDTO = serviceReferenceDTO.properties;

    if (propKeys != null) {
      assertThat(propertiesDTO.size()).as("Properties size ").isEqualTo(propKeys.length);
      for (String key : propKeys) {
        Object value = serviceRef.getProperty(key);

        assertThat(propertiesDTO.containsKey(key)).as("Service property " + key).isTrue();
        if (value instanceof String[]) {
          assertEquivalent((String[]) value, (String[]) propertiesDTO.get(key));
        } else if (value instanceof Number) {
          // Need to handle cases where round trip of Long results in
          // Integer and
          // round trip of Double results in Float.
          // But this does not handle cases where round trip of Double
          // or Float
          // result in Integer or Long
          assertThat(String.valueOf(propertiesDTO.get(key))).as("Service property value ").isEqualTo(String.valueOf(value));
        } else {
          assertThat(propertiesDTO.get(key)).as("Service property value ").isEqualTo(value);
        }
      }
    } else { // Is it possible?
      assertThat(propertiesDTO).as("No properties for service " + serviceRef.getProperty(Constants.SERVICE_ID) + " :").isNull();
    }

    assertThat(serviceReferenceDTO.bundle).as("Bundle property").isEqualTo(serviceRef.getBundle().getBundleId());

    long[] usingBundlesDTO = serviceReferenceDTO.usingBundles;
    Bundle[] usingBundles = serviceRef.getUsingBundles();

    if (usingBundles != null) {
      HashSet<Long> uBundlesSet = new HashSet<Long>();
      for (Bundle uBundle : usingBundles) {
        uBundlesSet.add(uBundle.getBundleId());
      }

      assertThat(usingBundlesDTO).as("usingBundles not null").isNotNull();
      assertThat(usingBundlesDTO.length).as("usingBundles length").isEqualTo(uBundlesSet.size());

      for (int k = 0; k < usingBundlesDTO.length; k++) {
        assertThat(uBundlesSet.contains(usingBundlesDTO[k])).as("usingBundles element " + usingBundlesDTO[k]).isTrue();
      }
    } else {
      assertThat(usingBundlesDTO).as("usingBundles is null").isNull();
    }
  }

  protected void assertServiceList(ServiceReference<?>[] serviceRefs, Collection<String> services) {
    assertThat(services).as("Service list").isNotNull();
    assertThat(services.size()).as("Service list length:").isEqualTo(serviceRefs.length);

    for (ServiceReference<?> serviceRef : serviceRefs) {
      String serviceId = serviceRef.getProperty(Constants.SERVICE_ID).toString();
      assertThat(services.contains(getServiceURI(serviceId))).as("Service list contains " + serviceId + " :").isTrue();
    }
  }

  protected void assertBundleStartLevelRepresentation(Bundle bundle, BundleStartLevelDTO bundleStartLevelDTO) {
    BundleStartLevel bundleStartLevel = getBundleStartLevel(bundle);

    assertThat(bundleStartLevelDTO.startLevel).as("startLevel:").isEqualTo(bundleStartLevel.getStartLevel());
    assertThat(bundleStartLevelDTO.activationPolicyUsed).as("activationPolicyUsed:").isEqualTo(bundleStartLevel.isActivationPolicyUsed());
    assertThat(bundleStartLevelDTO.persistentlyStarted).as("persistentlyStarted:").isEqualTo(bundleStartLevel.isPersistentlyStarted());
  }

  protected void assertBundleHeaderRepresentation(Bundle bundle, Map<String, String> bHeaders) {
    Dictionary<String, String> headers = bundle.getHeaders();
    if (headers == null) {
      assertThat(bHeaders).as("Bundle headers" + bundle.getBundleId() + ": ").isNull();
      return;
    }

    assertThat(bHeaders).as("Bundle headers" + bundle.getBundleId() + ": ").isNotNull();
    assertThat(bHeaders.size()).as("Headers size").isEqualTo(headers.size());

    for (Enumeration<String> keys = headers.keys(); keys.hasMoreElements();) {
      String key = keys.nextElement();
      assertThat(bHeaders.get(key)).as(key + ":").isEqualTo(headers.get(key));
    }
  }

  protected void assertBundleCollection(Bundle[] bundles, Collection<String> bundleCollection) {
    assertThat(bundleCollection).as("Bundle collection is not null.").isNotNull();
    assertThat(bundleCollection.size()).as("Bundle list length").isEqualTo(bundles.length);

    for (Bundle bundle : bundles) {
      long bundleId = bundle.getBundleId();
      assertThat(bundleCollection.contains(getBundleURI(bundleId))).as("Bundle list contains " + bundleId + ".").isTrue();
    }
  }

  protected void assertBundleRepresentation(Bundle bundle, BundleDTO bundleRepresentation) {
    assertThat(bundleRepresentation.lastModified).as("lastModified:").isEqualTo(bundle.getLastModified());
    assertThat(bundleRepresentation.state).as("state:").isEqualTo(bundle.getState());
    assertThat(bundleRepresentation.symbolicName).as("symbolicName:").isEqualTo(bundle.getSymbolicName());
    assertThat(bundleRepresentation.version).as("version:").isEqualTo(bundle.getVersion().toString());
  }

  protected void assertEquivalent(final String[] a1, final String[] a2) {
    assertThat(a2.length).isEqualTo(a1.length);
    for (int i = 0; i < a1.length; i++) {
      assertThat(a2[i]).isEqualTo(a1[i]);
    }
  }

}
