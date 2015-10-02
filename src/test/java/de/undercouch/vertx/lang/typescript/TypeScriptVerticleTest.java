// Copyright 2015 Michel Kraemer
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.undercouch.vertx.lang.typescript;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.RunTestOnContext;
import io.vertx.ext.unit.junit.VertxUnitRunnerWithParametersFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import de.undercouch.vertx.lang.typescript.compiler.NodeCompiler;

/**
 * Tests various verticles written in TypeScript
 * @author Michel Kraemer
 */
@RunWith(Parameterized.class)
@Parameterized.UseParametersRunnerFactory(VertxUnitRunnerWithParametersFactory.class)
public class TypeScriptVerticleTest {
  @Rule
  public RunTestOnContext runTestOnContext = new RunTestOnContext();
  
  private Vertx vertx;
  
  @Parameterized.Parameters
  public static Iterable<Boolean> useNodeCompiler() {
    if (NodeCompiler.supportsNode()) {
      if (Boolean.parseBoolean(System.getenv("TRAVIS"))) {
        return Arrays.asList(true);
      }
      return Arrays.asList(true, false);
    } else {
      return Arrays.asList(false);
    }
  }
  
  public TypeScriptVerticleTest(boolean useNodeCompiler) {
    System.setProperty(TypeScriptVerticleFactory.PROP_NAME_DISABLE_NODE_COMPILER,
        String.valueOf(!useNodeCompiler));
  }
  
  @Before
  public void before(TestContext context) {
    vertx = Vertx.vertx();
  }
  
  /**
   * Gets a free socket port
   * @return the port
   * @throws IOException if the port could not be determined
   */
  private static int getAvailablePort() throws IOException {
    ServerSocket s = null;
    try {
      s = new ServerSocket(0);
      return s.getLocalPort();
    } finally {
      if (s != null) {
        s.close();
      }
    }
  }
  
  private void doTest(String verticle, String message, TestContext context) throws IOException {
    Async async = context.async();
    int port = getAvailablePort();
    JsonObject config = new JsonObject().put("port", port);
    DeploymentOptions options = new DeploymentOptions().setConfig(config);
    vertx.deployVerticle(verticle, options, context.asyncAssertSuccess(deploymentID -> {
      vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
        response.bodyHandler(buffer -> {
          context.assertEquals(message, buffer.toString());
          vertx.undeploy(deploymentID, context.asyncAssertSuccess(r -> async.complete()));
        });
      });
    }));
  }
  
  /**
   * Tests if a simple HTTP server can be deployed. Relies on the current
   * working directory being the project's root.
   * @throws Exception if something goes wrong
   */
  @Test
  public void simpleServer(TestContext context) throws Exception {
    doTest("src/test/resources/simpleServer.ts", "Hello", context);
  }
  
  /**
   * Tests if a simple HTTP server using routing can be deployed. Relies on
   * the current working directory being the project's root.
   * @throws Exception if something goes wrong
   */
  @Test
  public void routingServer(TestContext context) throws Exception {
    doTest("src/test/resources/routingServer.ts", "Hello Routing", context);
  }
  
  /**
   * Tests if a simple HTTP server using modules can be deployed. Relies on
   * the current working directory being the project's root.
   * @throws Exception if something goes wrong
   */
  @Test
  public void moduleServer(TestContext context) throws Exception {
    doTest("src/test/resources/moduleServer.ts", "Hello Module", context);
  }
}
