package com.jecstar.etm.gui.login;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(
        features = {
                "classpath:features/login/login.feature"
        })
public class LoginIntegrationTest {
}
