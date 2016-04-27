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
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;

/**
 * @author Spencer Gibb
 */
@ConfigurationProperties(prefix = "spring.cloud.devtools")
public class DevtoolsProperties {

	private List<Deployable> toDeploy = new ArrayList<>();

	public List<Deployable> getToDeploy() {
		return toDeploy;
	}

	public void setToDeploy(List<Deployable> toDeploy) {
		this.toDeploy = toDeploy;
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("DevtoolsProperties{");
		sb.append("toDeploy=").append(toDeploy);
		sb.append('}');
		return sb.toString();
	}

	public static class Deployable implements Ordered {
		private String coordinates;
		private String name;
		private int port = 0;
		private boolean waitUntilStarted;
		private int order = 0;

		public String getCoordinates() {
			return coordinates;
		}

		public void setCoordinates(String coordinates) {
			this.coordinates = coordinates;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public boolean isWaitUntilStarted() {
			return waitUntilStarted;
		}

		public void setWaitUntilStarted(boolean waitUntilStarted) {
			this.waitUntilStarted = waitUntilStarted;
		}

		@Override
		public int getOrder() {
			return order;
		}

		public void setOrder(int order) {
			this.order = order;
		}

		@Override
		public String toString() {
			final StringBuffer sb = new StringBuffer("Deployable{");
			sb.append("coordinates='").append(coordinates).append('\'');
			sb.append(", name='").append(name).append('\'');
			sb.append(", port=").append(port);
			sb.append(", waitUntilStarted=").append(waitUntilStarted);
			sb.append(", order=").append(order);
			sb.append('}');
			return sb.toString();
		}
	}
}
