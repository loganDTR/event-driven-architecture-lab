package it.zengfx.order.javaorderconsumer.config;

import it.zengfx.order.javaorderconsumer.application.port.in.HandleOrderCreatedUseCase;
import it.zengfx.order.javaorderconsumer.application.port.out.SaveOrderPort;
import it.zengfx.order.javaorderconsumer.application.service.HandleOrderCreatedService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    HandleOrderCreatedUseCase handleOrderCreatedUseCase(SaveOrderPort saveOrderPort) {
        return new HandleOrderCreatedService(saveOrderPort);
    }
}
