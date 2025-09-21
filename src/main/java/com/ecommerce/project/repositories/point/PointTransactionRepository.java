package com.ecommerce.project.repositories.point;

import com.ecommerce.project.model.PointTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {
}
