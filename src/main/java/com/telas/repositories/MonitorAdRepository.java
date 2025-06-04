package com.telas.repositories;

import com.telas.entities.MonitorAd;
import com.telas.entities.MonitorAdPK;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface MonitorAdRepository extends JpaRepository<MonitorAd, MonitorAdPK>, JpaSpecificationExecutor<MonitorAd> {

}