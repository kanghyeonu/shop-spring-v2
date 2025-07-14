package shop.shop_spring.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.shop_spring.category.service.CategoryService;
import shop.shop_spring.category.domain.Category;

import shop.shop_spring.product.Dto.ProductUpdateRequest;
import shop.shop_spring.product.repository.ProductRepository;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.domain.ProductDescription;
import shop.shop_spring.product.enums.Status;


import java.math.BigDecimal;
import org.springframework.security.access.AccessDeniedException;


import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceUpdateTest {
    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductRepository productRepository;

    @Spy
    @InjectMocks
    private ProductServiceImpl productService;

    private Long existingProductId = 1L;
    private String sellerUsername = "테스트 판매자";
    private Product existingProduct;
    private ProductUpdateRequest updateRequest;


    @BeforeEach
    void setUp(){
        existingProduct = createTestProduct(existingProductId);
    }

    private Product createTestProduct(Long id){
        Product product = new Product();
        product.setId(id);
        product.setTitle("테스트 상품");
        product.setPrice(BigDecimal.valueOf(10000));
        product.setUsername("테스트 판매자");
        product.setStockQuantity(50);
        product.setStatus(Status.ACTIVE);
        ProductDescription productDescription = new ProductDescription();
        productDescription.setDescription("테스트 상품 상세 설명");
        product.setDescription(productDescription);
        //product.setThumbnailUrl("http://example.com/thum.jpg");
        Category category = new Category();
        category.setId(1L);
        category.setName("테스트 카테고리");
        product.setCategory(category);

        return product;
    }

    private void createUpdateRequest(){
        updateRequest = new ProductUpdateRequest();

        updateRequest.setTitle("수정될 상품 이름");
        updateRequest.setDescription("수정될 상품 설명");
        updateRequest.setPrice(BigDecimal.valueOf(500));
        updateRequest.setStockQuantity(100);
        updateRequest.setStatus(Status.INACTIVE);
        //updateRequest.setThumbnailUrl("http://example.com/thum2.jpg");
    }

    @Test
    void 상품_전체_수정_성공(){
        //given
        createUpdateRequest();

        doReturn(existingProduct).when(productService).findById(existingProductId);

        // when
        productService.updateProduct(sellerUsername, existingProductId, updateRequest);

        // Then
        verify(productService, times(1)).findById(existingProductId);

        assertEquals(updateRequest.getTitle(), existingProduct.getTitle());
        assertEquals(updateRequest.getPrice(), existingProduct.getPrice());
        assertEquals(updateRequest.getStatus(), existingProduct.getStatus());
        assertEquals(updateRequest.getDescription(), existingProduct.getDescription().getDescription());
        assertEquals(updateRequest.getStockQuantity(), existingProduct.getStockQuantity());
        //assertEquals(updateRequest.getThumbnailUrl(), existingProduct.getThumbnailUrl());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());

        assertSame(existingProduct, productCaptor.getValue());
    }

    @Test
    void 수정_실패_소유자아님(){
        // Given
        createUpdateRequest();
        String unauthorizedUsername = "나쁜 사람";
        doReturn(existingProduct).when(productService).findById(existingProductId);

        // when & then
        AccessDeniedException thrown = assertThrows(AccessDeniedException.class, () ->{
            productService.updateProduct(unauthorizedUsername, existingProductId, updateRequest);
        });

        assertEquals("상품 수정 권한이 없음", thrown.getMessage());

        verify(productService, times(1)).findById(existingProductId);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void 상품_설명_없음을_있음으로_갱신(){
        // given
        createUpdateRequest();
        existingProduct.setDescription(null);

        doReturn(existingProduct).when(productService).findById(existingProductId);

        // when
        productService.updateProduct(sellerUsername, existingProductId, updateRequest);

        // Then
        verify(productService, times(1)).findById(existingProductId);

        assertEquals(updateRequest.getTitle(), existingProduct.getTitle());
        assertEquals(updateRequest.getPrice(), existingProduct.getPrice());
        assertEquals(updateRequest.getStatus(), existingProduct.getStatus());
        assertEquals(updateRequest.getStockQuantity(), existingProduct.getStockQuantity());

        assertNotNull(existingProduct.getDescription());
        assertEquals(updateRequest.getDescription(), existingProduct.getDescription().getDescription());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());
        assertSame(existingProduct, productCaptor.getValue());

    }
}
