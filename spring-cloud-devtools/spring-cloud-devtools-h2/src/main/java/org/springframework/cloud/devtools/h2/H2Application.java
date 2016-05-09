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

package org.springframework.cloud.devtools.h2;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.h2.tools.Console;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

/**
 * @author Spencer Gibb
 */
@SpringBootApplication
public class H2Application {
	private static final Log log = LogFactory.getLog(H2Application.class);

	public static void main(String[] args) {
		SpringApplication.run(H2Application.class, args);
	}

	@SuppressWarnings("unused")
	@Service
	static class H2Server implements SmartLifecycle {
		private AtomicBoolean running = new AtomicBoolean(false);

		private Console console;

		@Override
		public boolean isAutoStartup() {
			return true;
		}

		@Override
		public void stop(Runnable callback) {
			stop();
			callback.run();
		}

		@Override
		public void start() {
			if (this.running.compareAndSet(false, true)) {
				try {
					log.info("Starting H2 Server");
					this.console = new Console();
					this.console.runTool("-tcp", "-pg", "-web",  "-tcpAllowOthers");
				} catch (Exception e) {
					ReflectionUtils.rethrowRuntimeException(e);
				}
			}
		}

		@Override
		public void stop() {
			if (this.running.compareAndSet(true, false)) {
				log.info("Stoping H2 Server");
				this.console.shutdown();
			}
		}

		@Override
		public boolean isRunning() {
			return this.running.get();
		}

		@Override
		public int getPhase() {
			return 0;
		}
	}
}
