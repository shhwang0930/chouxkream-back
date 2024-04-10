package com.kream.chouxkream.productsize;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductSizeRepository extends JpaRepository<ProductSize, Long> {
    Optional<ProductSize> findById(Long productSizeNo);
}
