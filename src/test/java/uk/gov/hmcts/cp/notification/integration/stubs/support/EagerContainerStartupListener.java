package uk.gov.hmcts.cp.notification.integration.stubs.support;

import org.junit.platform.launcher.LauncherSession;
import org.junit.platform.launcher.LauncherSessionListener;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

public class EagerContainerStartupListener implements LauncherSessionListener {

    private static final String ENABLE_FLAG = "cp.test.eagerContainers";

    private static final List<String> SUPPORT_CLASSES = List.of(
            "uk.gov.hmcts.cp.notification.integration.stubs.support.PostgresContainerSupport",
            "uk.gov.hmcts.cp.notification.integration.stubs.support.ServiceBusContainerSupport",
            "uk.gov.hmcts.cp.notification.integration.stubs.support.AzuriteContainerSupport",
            "uk.gov.hmcts.cp.notification.integration.stubs.support.WireMockSupport");

    @Override
    public void launcherSessionOpened(final LauncherSession session) {
        if (!Boolean.getBoolean(ENABLE_FLAG)) {
            return;
        }
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final long startNanos = System.nanoTime();
        for (final String className : SUPPORT_CLASSES) {
            try {
                Class.forName(className, true, classLoader);
            } catch (final ClassNotFoundException e) {
                throw new IllegalStateException("Could not eagerly start container support: " + className, e);
            }
        }
        recordStartupSeconds((System.nanoTime() - startNanos) / 1_000_000_000d);
    }

    private void recordStartupSeconds(final double seconds) {
        final Path report = Path.of("build", "container-startup.seconds");
        try {
            Files.createDirectories(report.getParent());
            Files.writeString(report, String.format(Locale.ROOT, "%.2f", seconds));
        } catch (final IOException e) {
            throw new IllegalStateException("Could not record container startup time", e);
        }
    }
}
