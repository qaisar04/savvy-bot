package kz.baltabayev.repository;

import kz.baltabayev.entity.Security;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SecurityRepository extends JpaRepository<Security, Long> {

    Optional<Security> findByUuidCode(String code);

}
