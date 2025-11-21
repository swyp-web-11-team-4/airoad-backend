package com.swygbro.airoad.backend.common.infrastructure.encryption;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.DirectDecrypter;
import com.nimbusds.jose.crypto.DirectEncrypter;
import com.swygbro.airoad.backend.common.exception.DecryptionException;
import com.swygbro.airoad.backend.common.exception.EncryptionException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 문자열 필드 암호화를 위한 JPA AttributeConverter 이메일, provider, 토큰 등 모든 문자열 데이터 암호화에 사용합니다. */
@Converter
@Slf4j
@RequiredArgsConstructor
public class StringEncryptor implements AttributeConverter<String, String> {
  private final DirectEncrypter encrypter;
  private final DirectDecrypter decrypter;

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null) {
      return null;
    }
    try {
      JWEHeader header = new JWEHeader(JWEAlgorithm.DIR, EncryptionMethod.A256GCM);

      JWEObject jweObject = new JWEObject(header, new Payload(attribute));
      jweObject.encrypt(encrypter);
      return jweObject.serialize();
    } catch (Exception e) {
      log.error("데이터 암호화 실패: {}", attribute, e);
      throw new EncryptionException("데이터 암호화 실패", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      return null;
    }
    try {
      JWEObject jweObject = JWEObject.parse(dbData);
      jweObject.decrypt(decrypter);
      return jweObject.getPayload().toString();
    } catch (Exception e) {
      log.error("데이터 복호화 실패", e);
      throw new DecryptionException("데이터 복호화 실패", e);
    }
  }
}
