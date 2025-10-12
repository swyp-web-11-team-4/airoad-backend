package com.swygbro.airoad.backend.example.domain.entity;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "example")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Example extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  /**
   * Example 엔티티를 생성합니다.
   *
   * @param name Example 이름
   */
  public Example(String name) {
    this.name = name;
  }
}
