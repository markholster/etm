/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.integration.test.core;

import com.consol.citrus.container.SequenceAfterTest;
import com.consol.citrus.dsl.endpoint.CitrusEndpoints;
import com.consol.citrus.dsl.runner.TestRunner;
import com.consol.citrus.dsl.runner.TestRunnerAfterTestSupport;
import com.consol.citrus.selenium.endpoint.SeleniumBrowser;
import org.openqa.selenium.remote.BrowserType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import java.io.File;

@Configuration
public class CitrusConfiguration {

    @Bean("firefox")
    public SeleniumBrowser firefox() {
        System.setProperty("webdriver.gecko.driver", new File("../etm-integration-test-core/drivers/geckodriver").getAbsolutePath());
        return CitrusEndpoints
                .selenium()
                .browser()
                .type(BrowserType.FIREFOX)
                .build();
    }

    @Bean("chrome")
    public SeleniumBrowser chrome() {
        System.setProperty("webdriver.chrome.driver", "../etm-integration-test-core/drivers/chromedriver");
        return CitrusEndpoints
                .selenium()
                .browser()
                .type(BrowserType.CHROME)
                .build();
    }

    @Bean
    @DependsOn("firefox")
    public SequenceAfterTest afterFirefoxTest(@Qualifier("firefox") SeleniumBrowser browser) {
        return closeBrowserAfterTestSupport(browser);
    }

    @Bean
    @DependsOn("chrome")
    public SequenceAfterTest afterChromeTest(@Qualifier("chrome") SeleniumBrowser browser) {
        return closeBrowserAfterTestSupport(browser);
    }

    private TestRunnerAfterTestSupport closeBrowserAfterTestSupport(SeleniumBrowser browser) {
        return new TestRunnerAfterTestSupport() {
            @Override
            public void afterTest(TestRunner runner) {
                runner.selenium(builder -> builder.browser(browser).stop());
            }
        };
    }
}
