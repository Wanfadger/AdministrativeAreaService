package com.wanfadger.AdministrativeareaApi.dto.reponses;

import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Paginated response wrapper for API responses")
@Setter
@Getter
public class PaginatedResponseDTO<T> extends ResponseDTO<List<T>> {

    
    @Schema(description = "Total number of items across all pages", example = "100")
    private long totalElements;
    
    @Schema(description = "Total number of pages", example = "10")
    private int totalPages;
    
    @Schema(description = "Current page number (0-based)", example = "0")
    private int page;
    
    @Schema(description = "Number of items per page", example = "10")
    private int size;
    
    @Schema(description = "Whether this is the first page", example = "true")
    private boolean hasNext;
    
    @Schema(description = "Whether this is the last page", example = "false")
    private boolean hasPrevious;


    public PaginatedResponseDTO(List<T> data) {
        super(data);
    }
} 