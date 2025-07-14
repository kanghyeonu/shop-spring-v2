package shop.shop_spring.product.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import shop.shop_spring.category.service.CategoryService;
import shop.shop_spring.category.domain.Category;
import shop.shop_spring.common.exception.DataNotFoundException;
import shop.shop_spring.product.Dto.ProductCreationRequest;
import shop.shop_spring.product.repository.ProductRepository;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.domain.ProductDescription;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@SpringBootTest
public class ProductServiceTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;


    private Product createTestProduct(Long id){
        Product product = new Product();
        product.setId(id);
        product.setTitle("테스트 상품");
        product.setPrice(BigDecimal.valueOf(10000));
        product.setUsername("테스트 판매자");
        product.setStockQuantity(50);
        ProductDescription productDescription = new ProductDescription();
        productDescription.setDescription("테스트 상품 상세 설명");
        product.setDescription(productDescription);
        product.setThumbnailUrl("http://example.com/thum.jpg");
        Category category = new Category();
        category.setId(1L);
        category.setName("테스트 카테고리");
        product.setCategory(category);

        return product;
    }

    @Test
    void 상품_등록_성공(){
        ProductCreationRequest request = new ProductCreationRequest();
        request.setTitle("테스트 상품");
        request.setPrice(BigDecimal.valueOf(10000));
        request.setUsername("테스트 판매자");
        request.setStockQuantity(50);
        request.setDescription("테스트 상품 상세 설명");
        request.setThumbnailUrl("http://example.com/thum.jpg");
        request.setCategoryId(1L);

        Category mockCategory = new Category();
        mockCategory.setId(1L);
        mockCategory.setName("모킹 카테고리");

        ArgumentCaptor<Product> productArgumentCaptor = ArgumentCaptor.forClass(Product.class);

        when(categoryService.findById(anyLong())).thenReturn(mockCategory);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product productToSave = invocation.getArgument(0); // save 호출 시 넘겨받은 Product 객체
            // 실제 JPA save 동작을 모방하여 ID 설정 (DB에서 할당받은 것처럼 시뮬레이션)
            if (productToSave.getId() == null) { // ID가 아직 설정되지 않았다면 (새로운 객체인 경우)
                productToSave.setId(101L); // 테스트용 가짜 ID 설정
            }
            return productToSave; // ID가 설정된 (동일한 인스턴스) 객체 반환
        });

        // when
        productService.createProduct(request);

        // then
        verify(categoryService, times(1)).findById(1L);
        verify(productRepository, times(1)).save(productArgumentCaptor.capture());

        Product capturedProduct = productArgumentCaptor.getValue();

        assertNotNull(capturedProduct);
        assertEquals(request.getUsername(), capturedProduct.getUsername(), "판매자 이름이 다름");
        assertNotNull(capturedProduct.getCategory(), "카테고리가 있어야함");
        assertEquals(mockCategory.getId(), capturedProduct.getCategory().getId(), "카테고리가 같아야함");
        assertNotNull(capturedProduct.getDescription(), "상품 설명이 설정되지 않음");
        assertEquals(request.getDescription(), capturedProduct.getDescription().getDescription(), "상품 설명이 다름");
        assertEquals(101L, capturedProduct.getId(), "저장되야하는 상품 id와 값이 다름");
    }

    @Test
    void 상품_조회_성공(){
        // given

        Long productId = 1L;
        Product product = createTestProduct(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        Product foundProduct = productService.findById(productId);

        // then
        verify(productRepository, times(1)).findById(productId);

        assertNotNull(foundProduct, "상품이 존재 해야함");
        assertEquals(productId, foundProduct.getId(), "조회 상품 id와 찾은 상품 id는 같아야함");
    }

    @Test
    void 상품_조회_실패(){
        //given
        Long nonExistingProductId = 100L;

        when(productRepository.findById(nonExistingProductId)).thenReturn(Optional.empty());

        //when & then
        DataNotFoundException thrown = assertThrows(DataNotFoundException.class, ()->{
            productService.findById(nonExistingProductId);
        }, "존재하지 않는 상품은 DataNotFoundException");

        assertEquals("삭제되거나 없는 상품임", thrown.getMessage());

        verify(productRepository, times(1)).findById(nonExistingProductId);
    }

    @Test
    void 상품_삭제_성공(){
        // given
        String username = "테스트 판매자";
        Long productId = 1L;
        Product product = createTestProduct(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when
        assertDoesNotThrow(() ->{
            productService.deleteProduct(username, productId);
        }, "소유자는 상품을 삭제 가능함");

        // then
        verify(productRepository, times(1)).deleteById(productId);
    }

    @Test
    void 상품_삭제_실패(){
        // given
        String unauthorizedUsername = "테스트 매판자";
        Long productId = 1L;
        Product product = createTestProduct(productId);

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        // when & then
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () ->{
            productService.deleteProduct(unauthorizedUsername, productId);
        }, "상품 소유자만 삭제할 수 있어야함");

        assertEquals("상품 삭제 권한이 없음", thrown.getMessage());

        verify(productRepository, never()).deleteById(anyLong());
    }


}
