package com.ecommerce.project.payload;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartDTO {
    private Long cartId;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Double totalPrice = 0.0;

    private List<ProductDTO> products = new ArrayList<>();
}
