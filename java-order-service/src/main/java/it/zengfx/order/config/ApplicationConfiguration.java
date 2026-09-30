package it.zengfx.order.config;

import it.zengfx.order.application.port.in.CreateOrderUseCase;
import it.zengfx.order.application.port.out.PublishOrderCreatedPort;
import it.zengfx.order.application.service.CreateOrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    CreateOrderUseCase createOrderUseCase(
            PublishOrderCreatedPort publishOrderCreatedPort
    ) {
        return new CreateOrderService(publishOrderCreatedPort);
    }
}
