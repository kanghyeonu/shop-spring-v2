package shop.shop_spring.product.service;

import shop.shop_spring.product.Dto.ProductCreationRequest;
import shop.shop_spring.product.Dto.ProductSearchCondition;
import shop.shop_spring.product.Dto.ProductUpdateRequest;
import shop.shop_spring.product.domain.Product;

import java.util.List;

public interface ProductService {

    Long createProduct(ProductCreationRequest product);

    List<Product> searchProducts(ProductSearchCondition productSearchCondition);

    Product findById(Long id);

    void updateProduct(String username, Long productId, ProductUpdateRequest updateRequest);

    void deleteProduct(String username, Long pd);
}
