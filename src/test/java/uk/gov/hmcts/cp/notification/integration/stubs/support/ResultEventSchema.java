package uk.gov.hmcts.cp.notification.integration.stubs.support;

import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public final class ResultEventSchema {
    public static final String NOTIFICATION_SENT = "notification-sent.schema.json";
    public static final String NOTIFICATION_FAILED = "notification-failed.schema.json";

    private static final Path CONTRACTS_DIR = Path.of("contracts");
    private static final JsonSchemaFactory FACTORY =
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);

    private ResultEventSchema() {
    }

    public static void assertConformsTo(final String schemaFile, final String payloadJson) {
        final Set<ValidationMessage> errors = load(schemaFile).validate(payloadJson, InputFormat.JSON);
        assertThat(errors)
                .as("result-event payload must satisfy the golden-master schema %s", schemaFile)
                .isEmpty();
    }

    private static JsonSchema load(final String schemaFile) {
        try {
            return FACTORY.getSchema(Files.readString(CONTRACTS_DIR.resolve(schemaFile)));
        } catch (final IOException e) {
            throw new UncheckedIOException("Unable to read golden-master schema: " + schemaFile, e);
        }
    }
}
