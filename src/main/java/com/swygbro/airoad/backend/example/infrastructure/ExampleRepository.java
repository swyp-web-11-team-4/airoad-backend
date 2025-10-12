package com.swygbro.airoad.backend.example.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.swygbro.airoad.backend.example.domain.entity.Example;

public interface ExampleRepository extends JpaRepository<Example, Long> {}
