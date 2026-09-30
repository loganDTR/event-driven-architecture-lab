package it.zengfx.order.javaorderconsumer.adapter.out.persistence;

import it.zengfx.order.javaorderconsumer.application.port.out.SaveOrderPort;
import it.zengfx.order.javaorderconsumer.domain.model.Order;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class JpaOrderPersistenceAdapter implements SaveOrderPort {

    private final OrderJpaRepository repository;

    public JpaOrderPersistenceAdapter(OrderJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Order order) {
        repository.save(OrderPersistenceMapper.toEntity(
                Objects.requireNonNull(order, "order cannot be null")
        ));
    }
}
