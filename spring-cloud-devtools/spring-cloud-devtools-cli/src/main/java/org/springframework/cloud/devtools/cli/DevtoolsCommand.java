/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.devtools.cli;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.options.OptionHandler;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.local.LocalAppDeployer;
import org.springframework.cloud.deployer.spi.local.LocalDeployerProperties;

import joptsimple.OptionSet;

/**
 * @author Spencer Gibb
 */
public class DevtoolsCommand extends OptionParsingCommand {

	private static final Logger logger = LoggerFactory.getLogger(DevtoolsCommand.class);

	public DevtoolsCommand() {
		super("devtools", "Start Spring Cloud DevTools", new DevtoolsOptionHandler());
	}

	private static class DevtoolsOptionHandler extends OptionHandler {

		private Map<String, DeploymentState> deployed = new LinkedHashMap<>();

		@Override
		protected ExitStatus run(OptionSet options) throws Exception {
			logger.info("Starting Spring Cloud Devtools...");
			//TODO: print banner

			//TODO: configurable AppDeployer? (ie deploy to CF)
			//TODO: use options to populate LocalDeployerProperties
			final LocalAppDeployer deployer = new LocalAppDeployer(new LocalDeployerProperties());

			String configServerId = deploy(deployer, "org.springframework.cloud.devtools", "spring-cloud-devtools-configserver", "1.1.0.BUILD-SNAPSHOT", 8888);

			AppStatus configServerStatus = deployer.status(configServerId);

			logger.info("\n\nWaiting for configserver to start.\n");
			//TODO: is there a better way to wait?
			while (configServerStatus.getState() != DeploymentState.deployed) {
			}

			deploy(deployer, "org.springframework.cloud.devtools", "spring-cloud-devtools-eureka", "1.1.0.BUILD-SNAPSHOT", 8761);

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					for (String id : DevtoolsOptionHandler.this.deployed.keySet()) {
						logger.info("Undeploying id: {}", id);
						deployer.undeploy(id);
						logger.info("Status of {}: {}", id, deployer.status(id));
					}
				}
			});

			logger.info("Type Ctrl-C to quit.");

			while (true) {
				for (Map.Entry<String, DeploymentState> entry : this.deployed.entrySet()) {
					/*String id = entry.getKey();
					DeploymentState state = entry.getValue();
					AppStatus status = deployer.status(id);
					DeploymentState newState = status.getState();
					if (state != newState) {
						logger.info("{} change status from {} to {}", id, state, newState);
						this.deployed.put(id, newState);
					}*/
				}
			}
		}

		private String deploy(LocalAppDeployer deployer, String groupId, String artifactId, String version, int port) {
			String id = deployer.deploy(createAppDeploymentRequest(groupId, artifactId, version, port));
			AppStatus status = deployer.status(id);
			logger.info("Status of {}: {}", id, status);
			this.deployed.put(id, status.getState());
			//TODO: stream stdout/stderr like docker-compose (with colors and prefix)
			return id;
		}
	}


	private static AppDeploymentRequest createAppDeploymentRequest(String groupId, String artifactId, String version, int port) {
		MavenResource resource = new MavenResource.Builder(new MavenProperties())
				.groupId(groupId)
				.artifactId(artifactId)
				.version(version)
				.extension("jar")
				.build();
		Map<String, String> properties = new HashMap<>();
		properties.put("server.port", String.valueOf(port));
		AppDefinition definition = new AppDefinition(artifactId, properties);
		Map<String, String> environmentProperties = Collections.singletonMap(AppDeployer.GROUP_PROPERTY_KEY, "devtools");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, environmentProperties);
		return request;
	}
}
