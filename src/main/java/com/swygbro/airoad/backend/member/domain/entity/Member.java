package com.swygbro.airoad.backend.member.domain.entity;

import jakarta.persistence.*;

import com.swygbro.airoad.backend.common.domain.entity.BaseEntity;
import com.swygbro.airoad.backend.member.domain.enumerated.MemberRole;
import com.swygbro.airoad.backend.member.domain.enumerated.ProviderType;

import lombok.*;

@Entity
@Table(name = "member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, name = "email")
  private String email;

  @Column(nullable = false, name = "picture")
  private String picture;

  @Column(nullable = false, name = "name")
  private String name;

  @Column(nullable = false, name = "provider")
  @Enumerated(EnumType.STRING)
  private ProviderType provider;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, name = "role")
  private MemberRole role;
}
