package it.zengfx.order.javaorderconsumer.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface OrderJpaRepository extends JpaRepository<OrderEntity, Long> {
}
