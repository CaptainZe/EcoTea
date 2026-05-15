package com.appsinnova.admin.business.repository.tea;

import com.appsinnova.admin.business.domain.tea.TeaPartner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TeaPartnerRepository extends JpaRepository<TeaPartner, Long>, JpaSpecificationExecutor<TeaPartner> {

    void deleteByIdIn(List<Long> idList);

    Optional<TeaPartner> findFirstByUserId(Long userId);

    boolean existsByUserIdAndIdNot(Long userId, Long id);
}
