package shop.shop_spring.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.shop_spring.category.service.CategoryService;
import shop.shop_spring.category.domain.Category;
import shop.shop_spring.common.exception.DataNotFoundException;
import shop.shop_spring.product.Dto.ProductCreationRequest;
import shop.shop_spring.product.Dto.ProductSearchCondition;
import shop.shop_spring.product.Dto.ProductUpdateRequest;
import shop.shop_spring.product.repository.ProductRepository;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.domain.ProductDescription;
import shop.shop_spring.product.enums.Status;
import shop.shop_spring.product.specification.ProductSpecification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService{
    private final CategoryService categoryService;
    private final ProductRepository productRepository;

    @Override
    public Long createProduct(ProductCreationRequest request) {
        
        Product newProduct = productCreationRequestToProduct(request);
        
        if (request.getCategoryId() == null){
            throw new IllegalArgumentException("상품의 카테고리의 입력값 미선택");
        }
        Category category = categoryService.findById(request.getCategoryId());
        newProduct.setCategory(category);

        if (request.getDescription() != null){
            ProductDescription productDescription  = new ProductDescription();
            productDescription.setDescription(request.getDescription());
            newProduct.setDescription(productDescription);
        }

        validateProduct(newProduct);
        productRepository.save(newProduct);

        return newProduct.getId();
    }

    private void validateProduct(Product product) {
        /**
         * 어쩌구 저쩌구 검증 내용
         */
    }

    private Product productCreationRequestToProduct(ProductCreationRequest request) {
        Product product = new Product();
        product.setTitle(request.getTitle());
        product.setPrice(request.getPrice());
        product.setUsername(request.getUsername());
        product.setStockQuantity(request.getStockQuantity());
        product.setThumbnailUrl(request.getThumbnailUrl());
        product.setStatus(Status.INACTIVE);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        return product;
    }

    @Override
    public Product findById(Long id) {
        Optional<Product> result = productRepository.findById(id);
        if (result.isEmpty()){
            throw new DataNotFoundException("삭제되거나 없는 상품임");
        }
        return result.get();
    }

    @Override
    public List<Product> searchProducts(ProductSearchCondition searchCondition) {
        Long id = searchCondition.getId();
        String title = searchCondition.getProductTitle();
        Long categoryId = searchCondition.getCategoryId();
        String seller = searchCondition.getSellerUsername();
        List<Status> statuses = searchCondition.getStatuses();

        Specification<Product> spec = Specification.where(null);
        // 상품 상태 존재
        if (statuses !=null && !statuses.isEmpty()){
            spec = spec.and(ProductSpecification.statusIn(statuses));
        }

        // 검색어가 존재
        if (title != null && !title.trim().isEmpty()){
            spec = spec.and(ProductSpecification.titleLike(title.trim()));
        }

        // 카테고리 존재
        if (categoryId != null){
            List<Long> categoryIdsToSearch = categoryService.getAllDescendantCategoryIds(categoryId);

            spec = spec.and(ProductSpecification.categoryIn(categoryIdsToSearch));
        }

        // 판매자 존재
        if (seller != null && !seller.trim().isEmpty()){
            spec = spec.and(ProductSpecification.hasOwnerUsername(seller.trim()));
        }

        Sort sortByCreatedAtDesc = Sort.by(Sort.Direction.DESC, "createdAt");

        return productRepository.findAll(spec, sortByCreatedAtDesc);
    }

    @Transactional
    @Override
    public void deleteProduct(String username, Long productId){
        Product product = findById(productId);

        if (!username.equals(product.getUsername())){
            throw new AccessDeniedException("상품 삭제 권한이 없음");
        }
        productRepository.deleteById(productId);
    }

    @Transactional
    @Override
    public void updateProduct(String username, Long productId, ProductUpdateRequest updateRequest){
        Product product = findById(productId);

        if (!username.equals(product.getUsername())){
            throw new AccessDeniedException("상품 수정 권한이 없음");
        }

        String title = updateRequest.getTitle();
        BigDecimal price = updateRequest.getPrice();
        Status status = updateRequest.getStatus();
        Integer stockQuantity = updateRequest.getStockQuantity();
        //String thumbnailUrl = updateRequest.getThumbnailUrl();
        String descriptionContent = updateRequest.getDescription();

        product.setTitle(title);
        product.setPrice(price);
        product.setStockQuantity(stockQuantity);
        //product.setThumbnailUrl(thumbnailUrl);
        ProductDescription productDescription = product.getDescription();
        if (productDescription == null){
            ProductDescription description = new ProductDescription();
            product.setDescription(description);
            productDescription = product.getDescription();
        }
        productDescription.setDescription(descriptionContent);

        if (status == null){
            status = product.getStatus();
        }
        product.setStatus(status);

        product.setUpdatedAt(LocalDateTime.now());

        validateProduct(product);

        productRepository.save(product);

    }
}
