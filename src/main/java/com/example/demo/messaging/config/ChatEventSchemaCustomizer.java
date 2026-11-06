package com.example.demo.messaging.config;

import com.example.demo.messaging.dto.ChatEventDto;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ChatEventSchemaCustomizer implements OpenApiCustomizer {

    @Override
    @SuppressWarnings("rawtypes")
    public void customise(io.swagger.v3.oas.models.OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        Map<String, Schema> schemas = ModelConverters.getInstance().readAll(ChatEventDto.class);
        schemas.forEach((name, schema) -> {
            if (openApi.getComponents().getSchemas() == null
                    || !openApi.getComponents().getSchemas().containsKey(name)) {
                schema.setAdditionalProperties(false);
                openApi.getComponents().addSchemas(name, schema);
            }
        });
    }
}
