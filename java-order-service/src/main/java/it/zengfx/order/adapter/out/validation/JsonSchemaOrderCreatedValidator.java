package it.zengfx.order.adapter.out.validation;

import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import it.zengfx.order.application.exception.EventContractViolationException;
import it.zengfx.order.application.port.out.ValidateOrderCreatedPort;
import it.zengfx.order.domain.event.OrderCreatedEvent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Component
public class JsonSchemaOrderCreatedValidator
        implements ValidateOrderCreatedPort {

    private static final String SCHEMA_PATH =
            "contracts/order-created/v2.schema.json";

    private final ObjectMapper objectMapper;
    private final Schema schema;

    public JsonSchemaOrderCreatedValidator(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
        this.schema = loadSchema();
    }

    @Override
    public void validate(OrderCreatedEvent event) {
        Objects.requireNonNull(
                event,
                "OrderCreatedEvent must not be null"
        );

        try {
            String json = objectMapper.writeValueAsString(event);
            validateJson(json);
        } catch (EventContractViolationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new EventContractViolationException(
                    "Unable to serialize OrderCreated event for validation",
                    exception
            );
        }
    }

    void validateJson(String json) {
        List<com.networknt.schema.Error> errors =
                schema.validate(
                        json,
                        InputFormat.JSON,
                        executionContext ->
                                executionContext.executionConfig(
                                        config -> config
                                                .formatAssertionsEnabled(true)
                                )
                );

        if (errors.isEmpty()) {
            return;
        }

        List<String> violations = errors.stream()
                .map(error -> "%s: %s".formatted(
                        error.getInstanceLocation(),
                        error.getMessage()
                ))
                .toList();

        throw new EventContractViolationException(
                "OrderCreated event violates JSON Schema V2",
                violations
        );
    }

    private Schema loadSchema() {
        ClassPathResource resource =
                new ClassPathResource(SCHEMA_PATH);

        try (var inputStream = resource.getInputStream()) {
            String schemaContent = new String(
                    inputStream.readAllBytes(),
                    StandardCharsets.UTF_8
            );

            SchemaRegistry registry =
                    SchemaRegistry.withDefaultDialect(
                            SpecificationVersion.DRAFT_2020_12
                    );

            return registry.getSchema(
                    schemaContent,
                    InputFormat.JSON
            );

        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to load JSON Schema from classpath: "
                            + SCHEMA_PATH,
                    exception
            );
        }
    }
}