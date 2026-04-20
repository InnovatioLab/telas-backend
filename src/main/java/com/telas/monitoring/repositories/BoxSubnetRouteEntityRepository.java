package com.telas.monitoring.repositories;

import com.telas.monitoring.entities.BoxSubnetRouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BoxSubnetRouteEntityRepository extends JpaRepository<BoxSubnetRouteEntity, UUID> {

    List<BoxSubnetRouteEntity> findByBox_IdOrderByCidrAsc(UUID boxId);

    void deleteByBox_Id(UUID boxId);
}
