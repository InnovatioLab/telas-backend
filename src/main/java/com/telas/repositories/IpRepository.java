package com.telas.repositories;

import com.telas.entities.Ip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface IpRepository extends JpaRepository<Ip, UUID> {
  Optional<Ip> findByIpAddress(String ipAddress);

  List<Ip> findAllByIpAddressNotInAndBoxIsNull(Set<String> ipAddresses);
}
