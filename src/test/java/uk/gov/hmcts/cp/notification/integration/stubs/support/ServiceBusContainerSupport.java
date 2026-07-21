package uk.gov.hmcts.cp.notification.integration.stubs.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.azure.ServiceBusEmulatorContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class ServiceBusContainerSupport {
    public static final String COMMAND_QUEUE = "nn-send-email";
    public static final String SLICE_COMMAND_QUEUE = "nn-send-email-slice";
    public static final String RESULT_QUEUE = "nn-result-correspondence";
    public static final String SLICE_RESULT_QUEUE = "nn-result-slice";

    private static final String MSSQL_IMAGE = resolveMssqlImage();

    private static final Network NETWORK = Network.newNetwork();

    @SuppressWarnings("resource")
    private static final MSSQLServerContainer MSSQL = new MSSQLServerContainer(
            DockerImageName.parse(MSSQL_IMAGE).asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server"))
            .acceptLicense()
            .withNetwork(NETWORK);

    @SuppressWarnings("resource")
    private static final ServiceBusEmulatorContainer SERVICE_BUS = new ServiceBusEmulatorContainer(
            "mcr.microsoft.com/azure-messaging/servicebus-emulator:latest")
            .acceptLicense()
            .withConfig(MountableFile.forClasspathResource("servicebus-config.json"))
            .withNetwork(NETWORK)
            .withMsSqlServerContainer(MSSQL);

    private static final Logger MSSQL_DRIVER_LOG = Logger.getLogger("com.microsoft.sqlserver.jdbc");

    static {
        MSSQL_DRIVER_LOG.setLevel(Level.SEVERE);
        MSSQL.start();
        SERVICE_BUS.start();
    }

    private ServiceBusContainerSupport() {
    }

    public static String getConnectionString() {
        return SERVICE_BUS.getConnectionString();
    }

    public static void registerProperties(final DynamicPropertyRegistry registry) {
        registerProperties(registry, COMMAND_QUEUE);
    }

    public static void registerProperties(final DynamicPropertyRegistry registry, final String commandQueue) {
        registry.add("cp.notification.servicebus.connection-string", ServiceBusContainerSupport::getConnectionString);
        registry.add("cp.notification.servicebus.command-queue", () -> commandQueue);
    }

    private static String resolveMssqlImage() {
        final String fromProperty = System.getProperty("cp.test.mssql.image");
        if (fromProperty != null && !fromProperty.isBlank()) {
            return fromProperty;
        }
        final String fromEnv = System.getenv("CP_TEST_MSSQL_IMAGE");
        return (fromEnv != null && !fromEnv.isBlank()) ? fromEnv : "mcr.microsoft.com/mssql/server:2025-latest";
    }
}
