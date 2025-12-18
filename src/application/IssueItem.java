package application;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IssueItem(
        Long id,
        String title,
        String description,
        String priority,
        String state,
        String type,
        String path,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long creatorId
) {}
