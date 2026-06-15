package br.com.iforce.praxis.simulation.persistence.repository;

import br.com.iforce.praxis.simulation.persistence.entity.SimulationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SimulationRepository extends JpaRepository<SimulationEntity, String> {
}
