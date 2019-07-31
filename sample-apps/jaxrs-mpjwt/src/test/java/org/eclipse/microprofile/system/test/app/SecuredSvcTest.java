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

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;

import org.eclipse.microprofile.system.test.jupiter.MicroProfileTest;
import org.eclipse.microprofile.system.test.jwt.JwtConfig;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.microprofile.MicroProfileApplication;
import org.testcontainers.junit.jupiter.Container;

@MicroProfileTest
public class SecuredSvcTest {

    @Container
    public static MicroProfileApplication<?> app = new MicroProfileApplication<>()
                    .withAppContextRoot("/myservice")
                    .withReadinessPath("/myservice/app/data/ping");

    @Inject
    @JwtConfig(claims = { "groups=users" })
    public static SecuredService securedSvc;

    @Inject
    @JwtConfig(claims = { "groups=wrong" })
    public static SecuredService misSecuredSvc;

    @Inject
    public static SecuredService noJwtSecuredSvc;

    @Test
    public void testHeaders() {
        System.out.println(securedSvc.getHeaders()); // for debugging
    }

    @Test
    public void testGetSecuredInfo() {
        String result = securedSvc.getSecuredInfo();
        assertTrue(result.contains("this is some secured info"));
    }

    @Test
    public void testGetSecuredInfoBadJwt() {
        // user will be authenticated but not in role, expect 403
        assertThrows(ForbiddenException.class, () -> misSecuredSvc.getSecuredInfo());
    }

    @Test
    public void testGetSecuredInfoNoJwt() {
        // no user, expect 401
        assertThrows(NotAuthorizedException.class, () -> noJwtSecuredSvc.getSecuredInfo());
    }

}