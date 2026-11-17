package com.onlineshop.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateCartRequest {
    @NotNull @Min(0) @Max(999) private Integer quantity;
}
