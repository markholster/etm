package com.jecstar.etm.gui.settings.cluster;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {
                "classpath:features/settings/ldap.feature"
        })
public class ClusterSettingsIntegrationTest {
}
