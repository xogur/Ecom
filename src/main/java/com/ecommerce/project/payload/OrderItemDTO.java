package com.ecommerce.project.payload;

import com.ecommerce.project.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long orderItemId;
    private ProductDTO product;
    private Integer quantity;
    private double discount;
    private double orderedProductPrice;

    public OrderItemDTO(Long orderItemId, ProductDTO product, Integer quantity, double orderedProductPrice) {
    }

//    public OrderItemDTO(Long orderItemId, @NotBlank @Size(min = 3, message = "Product name must contain atleast 3 characters") ProductDTO productName, Integer quantity, double orderedProductPrice) {
//    }


}