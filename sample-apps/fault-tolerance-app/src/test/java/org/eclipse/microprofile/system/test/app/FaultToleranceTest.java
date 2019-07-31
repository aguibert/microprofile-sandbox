/*
 * Copyright (c) 2019 IBM Corporation and others
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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
package org.eclipse.microprofile.system.test.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

import java.util.Collection;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;

import org.eclipse.microprofile.system.test.app.Person;
import org.eclipse.microprofile.system.test.jaxrs.JAXRSUtilities;
import org.eclipse.microprofile.system.test.jupiter.MicroProfileTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.ToxiproxyContainer.ContainerProxy;
import org.testcontainers.containers.microprofile.MicroProfileApplication;
import org.testcontainers.junit.jupiter.Container;

import com.google.common.net.MediaType;

@Disabled
@MicroProfileTest
public class FaultToleranceTest {
    
    @Container
    public static MockServerContainer mockServer = new MockServerContainer()
                    .withNetworkAliases("mockserver");
    
    @Container
    public static MicroProfileApplication<?> app = new MicroProfileApplication<>()
                    .withAppContextRoot("/myservice")
                    .withReadinessPath("/myservice/passthrough")
                    .withMpRestClient(ExternalRestServiceClient.class, "http://mockserver:" + MockServerContainer.PORT);
    
    @Container
    public static ToxiproxyContainer toxiproxy = new ToxiproxyContainer();

    @Inject
    public static PersonServiceWithPassthrough personSvc;
    
    private static Jsonb jsonb = JsonbBuilder.create();

    @Test
    public void testGetPerson() {
        Person expected = new Person("Hank", 42);
        new MockServerClient(mockServer.getContainerIpAddress(), mockServer.getServerPort())
            .when(request("/mock-passthrough/person/5"))
            .respond(response().withBody(jsonb.toJson(expected), MediaType.JSON_UTF_8));
        
        Person person5 = personSvc.getPersonFromExternalService(5);
        assertNotNull(person5);
        assertEquals("Hank", person5.name);
        assertEquals(42, person5.age);
        
        // TODO: Add @ToxiProxy annotation for REST client
        ContainerProxy proxy = toxiproxy.getProxy(app, 9080);
        String proxyURL = "http://" + proxy.getContainerIpAddress() + ":" + proxy.getProxyPort() + "/myservice";
        System.out.println("@AGG using proxy URL: " + proxyURL);
        personSvc = JAXRSUtilities.createRestClient(PersonServiceWithPassthrough.class, proxyURL);
        
        person5 = personSvc.getPersonFromExternalService(5);
        assertNotNull(person5);
        assertEquals("Hank", person5.name);
        assertEquals(42, person5.age);
        
        proxy.setConnectionCut(true);
        
        person5 = personSvc.getPersonFromExternalService(5);
        assertNotNull(person5);
        assertEquals("Hank", person5.name);
        assertEquals(42, person5.age);
    }

}