package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;

import java.util.Optional;


public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlanEntity, Long> {

    List<SubscriptionPlanEntity> findByActiveTrueOrderByPriceCentsAsc();

    Optional<SubscriptionPlanEntity> findByCode(String code);
}
