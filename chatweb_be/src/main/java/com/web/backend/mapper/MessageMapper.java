package com.web.backend.mapper;

import com.web.backend.controller.request.ChatMessageRequest;
import com.web.backend.controller.response.ChatMessageResponse;
import com.web.backend.model.ChatMessage;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MessageMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "timestamp", ignore = true)
    @Mapping(target = "read", ignore = true)
    ChatMessage toEntity(ChatMessageRequest request);

    ChatMessageResponse toResponse(ChatMessage entity);
}