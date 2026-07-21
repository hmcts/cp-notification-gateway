package uk.gov.hmcts.cp.notification.integration;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class Fixtures {

    private Fixtures() {
    }

    public static String load(final String path, final Map<String, String> tokens) {
        String content = load(path);
        for (final Map.Entry<String, String> token : tokens.entrySet()) {
            content = content.replace("${" + token.getKey() + "}", token.getValue());
        }
        return content;
    }

    public static String load(final String path) {
        return new String(loadBytes(path), StandardCharsets.UTF_8);
    }

    public static byte[] loadBytes(final String path) {
        try (InputStream in = Fixtures.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalArgumentException("Fixture not found on classpath: " + path);
            }
            return in.readAllBytes();
        } catch (final IOException e) {
            throw new UncheckedIOException("Failed to read fixture: " + path, e);
        }
    }
}
