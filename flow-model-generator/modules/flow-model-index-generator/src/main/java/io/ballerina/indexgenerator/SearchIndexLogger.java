/*
 *  Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com)
 *
 *  WSO2 LLC. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.ballerina.indexgenerator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Responsible for logging the progress of indexing packages.
 *
 * @since 2.0.0
 */
public class SearchIndexLogger {

    private static final Logger LOGGER = Logger.getLogger(SearchIndexLogger.class.getName());
    private final int totalPackages;
    private final AtomicInteger completedPackages = new AtomicInteger(0);

    static {
        // Remove default console handler
        Logger rootLogger = Logger.getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof ConsoleHandler) {
                rootLogger.removeHandler(handler);
            }
        }

        // Add custom console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(new SimpleFormatter() {
            private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy hh:mm:ss a");

            @Override
            public synchronized String format(LogRecord lr) {
                return String.format("[%s] %s%n", dateFormat.format(new Date(lr.getMillis())), lr.getMessage());
            }
        });
        LOGGER.addHandler(consoleHandler);
    }

    public SearchIndexLogger(int totalPackages) {
        this.totalPackages = totalPackages;
    }

    public void completion(String packageName) {
        int completed = completedPackages.incrementAndGet();
        LOGGER.info(
                String.format("Completed indexing the package: %s (%d / %d)", packageName, completed, totalPackages));
    }
}
