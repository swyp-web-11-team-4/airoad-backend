package com.swygbro.airoad.backend.example.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.swygbro.airoad.backend.common.exception.BusinessException;
import com.swygbro.airoad.backend.example.domain.dto.ExampleResponse;
import com.swygbro.airoad.backend.example.domain.entity.Example;
import com.swygbro.airoad.backend.example.exception.ExampleErrorCode;
import com.swygbro.airoad.backend.example.infrastructure.ExampleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExampleService implements ExampleUseCase {

  private final ExampleRepository exampleRepository;

  @Override
  public ExampleResponse getHelloMessage() {
    return ExampleResponse.of("Hello, World!");
  }

  @Override
  public ExampleResponse getExampleById(Long id) {
    Example example =
        exampleRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(ExampleErrorCode.EXAMPLE_NOT_FOUND));
    return ExampleResponse.from(example);
  }

  @Override
  @Transactional
  public ExampleResponse createExample(String name) {
    if (name == null || name.trim().isEmpty()) {
      throw new BusinessException(ExampleErrorCode.INVALID_EXAMPLE_NAME);
    }

    Example example = new Example(name);
    Example savedExample = exampleRepository.save(example);
    return ExampleResponse.from(savedExample);
  }

  @Override
  @Transactional
  public void deleteExample(Long id) {
    exampleRepository.deleteById(id);
  }
}
