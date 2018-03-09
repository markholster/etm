package com.jecstar.etm.gui.search;

import cucumber.api.CucumberOptions;
import cucumber.api.junit.Cucumber;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features = {
        "classpath:features/search/search.feature"
        , "classpath:features/search/template.feature"
        , "classpath:features/search/transaction.feature"})
public class SearchIntegrationTest {

}
