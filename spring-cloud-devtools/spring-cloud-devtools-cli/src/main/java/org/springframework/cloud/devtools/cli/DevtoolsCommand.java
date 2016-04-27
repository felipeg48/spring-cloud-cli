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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.cli.command.AbstractCommand;
import org.springframework.boot.cli.command.status.ExitStatus;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.devtools.cli.DevtoolsProperties.Deployable;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author Spencer Gibb
 */
public class DevtoolsCommand extends AbstractCommand {

	private static final Logger logger = LoggerFactory.getLogger(DevtoolsCommand.class);

	private Map<String, DeploymentState> deployed = new LinkedHashMap<>();

	public DevtoolsCommand() {
		super("cloud", "Start Spring Cloud DevTools");
	}

	@Override
	public ExitStatus run(String... args) throws Exception {
		ConfigurableApplicationContext context = new SpringApplicationBuilder(PropertyPlaceholderAutoConfiguration.class, DevtoolsCommandConfiguration.class)
				.web(false)
				.properties("spring.config.name=cloud")
				.run(args);

		final AppDeployer deployer = context.getBean(AppDeployer.class);

		DevtoolsProperties properties = context.getBean(DevtoolsProperties.class);

		logger.debug("toDeploy {}", properties.getToDeploy());

		ArrayList<Deployable> deployables = new ArrayList<>(properties.getToDeploy());

		Deployable configServer = extractConfigServer(deployables);

		if (configServer != null) {
			String configServerId = deploy(deployer, configServer);

			AppStatus configServerStatus = deployer.status(configServerId);

			logger.info("\n\nWaiting for configserver to start.\n");

			//TODO: is there a better way to wait?
			while (configServerStatus.getState() != DeploymentState.deployed) {
			}
		}

		for (Deployable deployable : deployables) {
			deploy(deployer, deployable);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				for (String id : DevtoolsCommand.this.deployed.keySet()) {
					logger.info("Undeploying {}", id);
					deployer.undeploy(id);
				}
			}
		});

		logger.info("Type Ctrl-C to quit.");

		while (true) {
			/*for (Map.Entry<String, DeploymentState> entry : this.deployed.entrySet()) {
				String id = entry.getKey();
				DeploymentState state = entry.getValue();
				AppStatus status = deployer.status(id);
				DeploymentState newState = status.getState();
				if (state != newState) {
					logger.info("{} change status from {} to {}", id, state, newState);
					this.deployed.put(id, newState);
				}
			}*/
		}
	}

	private Deployable extractConfigServer(ArrayList<Deployable> deployables) {
		for (Iterator<Deployable> iterator = deployables.iterator(); iterator.hasNext();) {
			Deployable deployable = iterator.next();
			if ("configserver".equals(deployable.getName())) {
				iterator.remove();
				return deployable;
			}
		}
		return null;
	}

	private String deploy(AppDeployer deployer, Deployable deployable) {
		MavenResource resource = MavenResource.parse(deployable.getCoordinates());
		Map<String, String> properties = new HashMap<>();
		properties.put("server.port", String.valueOf(deployable.getPort()));
		AppDefinition definition = new AppDefinition(resource.getArtifactId(), properties);
		Map<String, String> environmentProperties = Collections.singletonMap(AppDeployer.GROUP_PROPERTY_KEY, "devtools");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, environmentProperties);

		String id = deployer.deploy(request);
		AppStatus status = deployer.status(id);
		logger.info("Status of {}: {}", id, status);
		this.deployed.put(id, status.getState());
		//TODO: stream stdout/stderr like docker-compose (with colors and prefix)
		return id;
	}
}
