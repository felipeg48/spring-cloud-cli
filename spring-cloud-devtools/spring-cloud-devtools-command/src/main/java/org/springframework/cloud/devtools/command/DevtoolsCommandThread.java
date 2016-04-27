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

package org.springframework.cloud.devtools.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.devtools.command.DevtoolsProperties.Deployable;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.OrderComparator;

/**
 * @author Spencer Gibb
 */
@SuppressWarnings("unused")
public class DevtoolsCommandThread extends Thread {

	private static final Logger logger = LoggerFactory.getLogger(DevtoolsCommandThread.class);

	private Map<String, DeploymentState> deployed = new LinkedHashMap<>();

	private String[] args;

	public DevtoolsCommandThread(ClassLoader classLoader, String[] args) {
		super("spring-cloud-devtools");
		this.args = args;
		setContextClassLoader(classLoader);
		setDaemon(true);
	}

	@Override
	public void run() {
		final ConfigurableApplicationContext context = new SpringApplicationBuilder(PropertyPlaceholderAutoConfiguration.class, DevtoolsCommandConfiguration.class)
				.web(false)
				.properties("spring.config.name=cloud", "banner.location=devtools-banner.txt")
				.run(args);

		final AppDeployer deployer = context.getBean(AppDeployer.class);

		DevtoolsProperties properties = context.getBean(DevtoolsProperties.class);

		ArrayList<Deployable> deployables = new ArrayList<>(properties.getToDeploy());
		OrderComparator.sort(deployables);

		logger.debug("toDeploy {}", properties.getToDeploy());

		for (Deployable deployable : deployables) {
			deploy(deployer, deployable, properties);
		}

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {
				for (String id : DevtoolsCommandThread.this.deployed.keySet()) {
					logger.info("Undeploying {}", id);
					deployer.undeploy(id);
				}
				context.close();
			}
		});

		logger.info("\n\nType Ctrl-C to quit.\n");

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

	private String deploy(AppDeployer deployer, Deployable deployable, DevtoolsProperties properties) {
		MavenResource resource = MavenResource.parse(deployable.getCoordinates());
		Map<String, String> resourceProps = new HashMap<>();
		resourceProps.put("server.port", String.valueOf(deployable.getPort()));
		AppDefinition definition = new AppDefinition(resource.getArtifactId(), resourceProps);
		Map<String, String> environmentProperties = Collections.singletonMap(AppDeployer.GROUP_PROPERTY_KEY, "devtools");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, environmentProperties);

		String id = deployer.deploy(request);
		AppStatus status = deployer.status(id);
		logger.info("Status of {}: {}", id, status);
		this.deployed.put(id, status.getState());
		//TODO: stream stdout/stderr like docker-compose (with colors and prefix)

		if (deployable.isWaitUntilStarted()) {
			try {
				AppStatus appStatus = getAppStatus(deployer, id);

				String description = deployable.getName() != null ? deployable.getName() : resource.getArtifactId();
				logger.info("\n\nWaiting for {} to start.\n", description);

				while (appStatus.getState() != DeploymentState.deployed
						&& appStatus.getState() != DeploymentState.failed) {
					Thread.sleep(properties.getStatusSleepMillis());
					appStatus = getAppStatus(deployer, id);
				}
			} catch (Exception e) {
				logger.error("error updating status of " + id, e);
			}
		}

		return id;
	}

	private AppStatus getAppStatus(AppDeployer deployer, String id) {
		AppStatus appStatus = deployer.status(id);
		this.deployed.put(id, appStatus.getState());
		return appStatus;
	}


}
