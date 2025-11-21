package com.swygbro.airoad.backend.member.domain.entity;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.common.infrastructure.encryption.StringEncryptor;

import lombok.*;

@Entity
@Table(name = "member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

  @Convert(converter = StringEncryptor.class)
  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable = false, unique = true)
  private String emailHash;

  @Column(nullable = false)
  private String imageUrl;

  @Convert(converter = StringEncryptor.class)
  @Column(nullable = false)
  private String name;

  @Column(nullable = false, unique = true)
  private String nameHash;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private ProviderType provider;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private MemberRole role;
}
