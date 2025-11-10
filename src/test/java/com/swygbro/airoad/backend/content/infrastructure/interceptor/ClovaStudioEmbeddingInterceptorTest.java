package com.swygbro.airoad.backend.content.infrastructure.interceptor;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;

import com.swygbro.airoad.backend.ai.infrastructure.interceptor.ClovaStudioEmbeddingInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;

/** ClovaStudioEmbeddingInterceptor 단위 테스트 */
@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ClovaStudioEmbeddingInterceptorTest {

  @Mock private HttpRequest request;

  @Mock private ClientHttpRequestExecution execution;

  @Mock private ClientHttpResponse response;

  private ClovaStudioEmbeddingInterceptor interceptor;

  private static final String CLOVA_STUDIO_URL =
      "https://clovastudio.stream.ntruss.com/v1/openai/embeddings";

  @BeforeEach
  void setUp() {
    interceptor = new ClovaStudioEmbeddingInterceptor();
  }

  @Nested
  class Clova_Studio_Embedding_API_요청_시 {

    @Test
    void 배열_형식의_input을_문자열로_변환할_수_있다() throws IOException {
      // given: Clova Studio URL과 배열 형식의 요청 본문
      URI clovaUri = URI.create(CLOVA_STUDIO_URL);
      String originalBody = "{\"model\":\"text-embedding-ada-002\",\"input\":[\"테스트 텍스트\"]}";
      byte[] bodyBytes = originalBody.getBytes(StandardCharsets.UTF_8);

      given(request.getURI()).willReturn(clovaUri);
      given(execution.execute(eq(request), any(byte[].class))).willReturn(response);

      // when: 인터셉터 실행
      interceptor.intercept(request, bodyBytes, execution);

      // then: input 배열이 문자열로 변환되어 실행됨
      ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
      then(execution).should(times(1)).execute(eq(request), bodyCaptor.capture());

      String modifiedBody = new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
      assertThat(modifiedBody).contains("\"input\":\"테스트 텍스트\"");
      assertThat(modifiedBody).doesNotContain("\"input\":[");
    }

    @Test
    void 복잡한_JSON_구조에서도_input만_정확히_변환할_수_있다() throws IOException {
      // given: 여러 필드가 있는 복잡한 요청 본문
      URI clovaUri = URI.create(CLOVA_STUDIO_URL);
      String originalBody =
          "{\"model\":\"text-embedding-ada-002\",\"input\":[\"서울 여행 추천\"],\"encoding_format\":\"float\"}";
      byte[] bodyBytes = originalBody.getBytes(StandardCharsets.UTF_8);

      given(request.getURI()).willReturn(clovaUri);
      given(execution.execute(eq(request), any(byte[].class))).willReturn(response);

      // when: 인터셉터 실행
      interceptor.intercept(request, bodyBytes, execution);

      // then: input만 변환되고 다른 필드는 유지됨
      ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
      then(execution).should(times(1)).execute(eq(request), bodyCaptor.capture());

      String modifiedBody = new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
      assertThat(modifiedBody).contains("\"input\":\"서울 여행 추천\"");
      assertThat(modifiedBody).contains("\"model\":\"text-embedding-ada-002\"");
      assertThat(modifiedBody).contains("\"encoding_format\":\"float\"");
    }

    @Test
    void 한글_텍스트도_정확히_변환할_수_있다() throws IOException {
      // given: 한글이 포함된 요청 본문
      URI clovaUri = URI.create(CLOVA_STUDIO_URL);
      String originalBody = "{\"input\":[\"경복궁은 조선시대 궁궐입니다\"]}";
      byte[] bodyBytes = originalBody.getBytes(StandardCharsets.UTF_8);

      given(request.getURI()).willReturn(clovaUri);
      given(execution.execute(eq(request), any(byte[].class))).willReturn(response);

      // when: 인터셉터 실행
      interceptor.intercept(request, bodyBytes, execution);

      // then: 한글이 손실 없이 변환됨
      ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
      then(execution).should(times(1)).execute(eq(request), bodyCaptor.capture());

      String modifiedBody = new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
      assertThat(modifiedBody).contains("\"input\":\"경복궁은 조선시대 궁궐입니다\"");
    }
  }

  @Nested
  class 다른_API_요청_시 {
    @Test
    void Clova_Studio가_아닌_URL은_변환하지_않는다() throws IOException {
      // given: 다른 API URL
      URI otherUri = URI.create("https://api.openai.com/v1/embeddings");
      String originalBody = "{\"input\":[\"테스트\"]}";
      byte[] bodyBytes = originalBody.getBytes(StandardCharsets.UTF_8);

      given(request.getURI()).willReturn(otherUri);
      given(execution.execute(eq(request), any(byte[].class))).willReturn(response);

      // when: 인터셉터 실행
      interceptor.intercept(request, bodyBytes, execution);

      // then: 원본 그대로 전달됨
      ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
      then(execution).should(times(1)).execute(eq(request), bodyCaptor.capture());

      String resultBody = new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
      assertThat(resultBody).isEqualTo(originalBody);
      assertThat(resultBody).contains("\"input\":[");
    }
  }

  @Nested
  class 엣지_케이스_처리_시 {
    @Test
    void 특수문자가_포함된_텍스트도_처리할_수_있다() throws IOException {
      // given: 특수문자가 포함된 input
      URI clovaUri = URI.create(CLOVA_STUDIO_URL);
      String originalBody = "{\"input\":[\"Hello! 안녕하세요? @#$%\"]}";
      byte[] bodyBytes = originalBody.getBytes(StandardCharsets.UTF_8);

      given(request.getURI()).willReturn(clovaUri);
      given(execution.execute(eq(request), any(byte[].class))).willReturn(response);

      // when: 인터셉터 실행
      interceptor.intercept(request, bodyBytes, execution);

      // then: 특수문자가 포함된 채로 변환됨
      ArgumentCaptor<byte[]> bodyCaptor = ArgumentCaptor.forClass(byte[].class);
      then(execution).should(times(1)).execute(eq(request), bodyCaptor.capture());

      String modifiedBody = new String(bodyCaptor.getValue(), StandardCharsets.UTF_8);
      assertThat(modifiedBody).contains("\"input\":\"Hello! 안녕하세요? @#$%\"");
    }

    @Test
    void 응답을_정상적으로_반환할_수_있다() throws IOException {
      // given: 정상 요청 설정
      URI clovaUri = URI.create(CLOVA_STUDIO_URL);
      String originalBody = "{\"input\":[\"테스트\"]}";
      byte[] bodyBytes = originalBody.getBytes(StandardCharsets.UTF_8);

      given(request.getURI()).willReturn(clovaUri);
      given(execution.execute(eq(request), any(byte[].class))).willReturn(response);

      // when: 인터셉터 실행
      ClientHttpResponse result = interceptor.intercept(request, bodyBytes, execution);

      // then: 응답이 정상적으로 반환됨
      assertThat(result).isEqualTo(response);
    }
  }
}
