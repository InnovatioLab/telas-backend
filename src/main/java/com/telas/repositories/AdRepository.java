package com.telas.repositories;

import com.telas.entities.Ad;
import com.telas.enums.AdValidationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdRepository extends JpaRepository<Ad, UUID>, JpaSpecificationExecutor<Ad> {
  @Query("""
              SELECT ad FROM Ad ad
              JOIN ad.client c
              LEFT JOIN c.subscriptions s
              LEFT JOIN s.monitors m
              WHERE (
                  c.role <> 'ADMIN'
                  AND ad.validation = :validation
                  AND s.status = 'ACTIVE'
                  AND (s.endsAt IS NULL OR s.endsAt > CURRENT_TIMESTAMP)
                  AND m.id = :monitorId
              )
              OR c.role = 'ADMIN'
          """)
  List<Ad> findAllValidAdsForMonitor(
          @Param("validation") AdValidationType validation,
          @Param("monitorId") UUID monitorId
  );
}