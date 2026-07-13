package uk.gov.hmcts.cp.notification.acceptance;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example step definitions proving the Cucumber + Spring harness runs. Replace with real,
 * domain-organised step definitions.
 *
 * <p>Keep step definitions thin and low-complexity: a step should delegate to a helper/service,
 * not contain branching or scenario logic. Organise by domain concept and reuse across features —
 * never one step-definition class per feature file. See docs/Testing.md.
 */
public class ExampleStepDefinitions {

    @Autowired
    private ApplicationContext applicationContext;

    @Given("the service is running")
    public void the_service_is_running() {
        assertThat(applicationContext).isNotNull();
    }

    @Then("the Spring application context has initialised")
    public void the_spring_application_context_has_initialised() {
        assertThat(applicationContext.getBeanDefinitionCount()).isPositive();
    }
}
