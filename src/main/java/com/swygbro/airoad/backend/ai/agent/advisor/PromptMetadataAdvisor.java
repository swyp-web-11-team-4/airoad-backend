package com.swygbro.airoad.backend.ai.agent.advisor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import lombok.Builder;
import reactor.core.publisher.Flux;

public class PromptMetadataAdvisor implements CallAdvisor, StreamAdvisor {

  /** Advisor 컨텍스트에서 메타데이터를 식별하는 키 */
  public static final String METADATA_KEY = "PROMPT_METADATA";

  private final List<MetadataEntry> metadata;

  @Builder
  private PromptMetadataAdvisor(List<MetadataEntry> metadata) {
    this.metadata = (metadata != null) ? metadata : new ArrayList<>();
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public int getOrder() {
    return 0;
  }

  @Override
  public ChatClientResponse adviseCall(
      ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
    ChatClientRequest modifiedRequest = addMetadataMessages(chatClientRequest);
    return callAdvisorChain.nextCall(modifiedRequest);
  }

  @Override
  public Flux<ChatClientResponse> adviseStream(
      ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {
    ChatClientRequest modifiedRequest = addMetadataMessages(chatClientRequest);
    return streamAdvisorChain.nextStream(modifiedRequest);
  }

  private ChatClientRequest addMetadataMessages(ChatClientRequest chatClientRequest) {
    Map<String, Object> context = chatClientRequest.context();

    @SuppressWarnings("unchecked")
    List<MetadataEntry> paramMetadata = (List<MetadataEntry>) context.get(METADATA_KEY);
    List<MetadataEntry> allMetadata = new ArrayList<>(this.metadata);

    if (paramMetadata != null && !paramMetadata.isEmpty()) {
      allMetadata.addAll(paramMetadata);
    }

    if (allMetadata.isEmpty()) {
      return chatClientRequest;
    }

    for (MetadataEntry entry : allMetadata) {
      Message message =
          switch (entry.messageType()) {
            case SYSTEM -> new SystemMessage(entry.content());
            case USER -> new UserMessage(entry.content());
            default ->
                throw new IllegalArgumentException(
                    "Unsupported message type: " + entry.messageType());
          };
      chatClientRequest.prompt().getInstructions().add(message);
    }

    return chatClientRequest;
  }

  /**
   * 시스템 메시지 메타데이터를 생성하는 편의 메서드
   *
   * @param contents 시스템 메시지 내용들
   * @return 시스템 타입의 메타데이터 엔트리 리스트
   */
  public static List<MetadataEntry> systemMetadata(String... contents) {
    return Arrays.stream(contents).map(c -> new MetadataEntry(MessageType.SYSTEM, c)).toList();
  }

  /**
   * 유저 메시지 메타데이터를 생성하는 편의 메서드
   *
   * @param contents 유저 메시지 내용들
   * @return 유저 타입의 메타데이터 엔트리 리스트
   */
  public static List<MetadataEntry> userMetadata(String... contents) {
    return Arrays.stream(contents).map(c -> new MetadataEntry(MessageType.USER, c)).toList();
  }

  /**
   * 메타데이터 엔트리
   *
   * @param messageType 메시지 타입 (SYSTEM 또는 USER)
   * @param content 메시지 내용
   */
  public record MetadataEntry(MessageType messageType, String content) {}
}
