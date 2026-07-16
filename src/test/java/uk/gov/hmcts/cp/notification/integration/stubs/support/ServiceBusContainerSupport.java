package uk.gov.hmcts.cp.notification.integration.stubs.support;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.models.SubQueue;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.azure.ServiceBusEmulatorContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.mssqlserver.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public final class ServiceBusContainerSupport {
    public static final String COMMAND_QUEUE = "nn-send-email";

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

    static {
        MSSQL.start();
        SERVICE_BUS.start();
    }

    private ServiceBusContainerSupport() {
    }

    public static String getConnectionString() {
        return SERVICE_BUS.getConnectionString();
    }

    public static void registerProperties(final DynamicPropertyRegistry registry) {
        registry.add("cp.notification.servicebus.connection-string", ServiceBusContainerSupport::getConnectionString);
        registry.add("cp.notification.servicebus.command-queue", () -> COMMAND_QUEUE);
    }

    public static ServiceBusClientBuilder.ServiceBusSenderClientBuilder aServiceBusSenderClientBuilder() {
        return new ServiceBusClientBuilder()
                .connectionString(getConnectionString())
                .sender()
                .queueName(COMMAND_QUEUE);
    }

    public static ServiceBusClientBuilder.ServiceBusReceiverClientBuilder aDeadLetterReceiver() {
        return aServiceBusReceiverClientBuilder().subQueue(SubQueue.DEAD_LETTER_QUEUE);
    }

    private static ServiceBusClientBuilder.ServiceBusReceiverClientBuilder aServiceBusReceiverClientBuilder() {
        return new ServiceBusClientBuilder()
                .connectionString(getConnectionString())
                .receiver()
                .queueName(COMMAND_QUEUE);
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
