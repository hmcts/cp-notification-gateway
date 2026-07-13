package uk.gov.hmcts.cp.notification.acceptance;

import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit-Platform suite that runs all Cucumber acceptance scenarios under
 * {@code src/test/resources/features}, executed by {@code ./gradlew test}.
 *
 * <p>Named {@code AcceptanceTest} (ends in {@code Test}) so it is discovered by the {@code test} task
 * regardless of any name filter, paralleling the integration convention {@code *IntegrationTest}.
 *
 * <p>Gradle discovers tests by class, so this suite is the entry point that hands the feature files to
 * the Cucumber engine — without it the scenarios are silently skipped. Glue (step-definition package)
 * is configured in {@code src/test/resources/junit-platform.properties}. Do not duplicate this into a
 * second runner: one suite runs every feature once.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
public class AcceptanceTest {
}
