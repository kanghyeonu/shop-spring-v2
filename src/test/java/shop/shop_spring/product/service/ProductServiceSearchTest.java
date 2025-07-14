package shop.shop_spring.product.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import shop.shop_spring.category.service.CategoryService;
import shop.shop_spring.category.domain.Category;
import shop.shop_spring.product.Dto.ProductSearchCondition;
import shop.shop_spring.product.repository.ProductRepository;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.domain.ProductDescription;
import shop.shop_spring.product.specification.ProductSpecification;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceSearchTest {
    @Mock
    private CategoryService categoryService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private List<Product> mockProduct;
    private Sort expectedSort;

    @BeforeEach
    void setup(){
        Product p1 = createTestProduct(1L);
        Product p2 = createTestProduct(2L);
        mockProduct = Arrays.asList(p1, p2);
        expectedSort = Sort.by(Sort.Direction.DESC, "createdAt");
    }

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
    void 전체_상품_검색(){
        // given
        ProductSearchCondition emptyCondition = new ProductSearchCondition();

        ArgumentCaptor<Specification<Product>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        when(productRepository.findAll(specCaptor.capture(), sortCaptor.capture())).
                thenReturn(mockProduct);

        // when
        List<Product> result = productService.searchProducts(emptyCondition);

        // then
        verify(productRepository, times(1))
                .findAll(any(Specification.class), eq(expectedSort));

        // specification에서 하위 카테고리 가져오는 메서드 호출x
        verifyNoInteractions(categoryService);

        Sort capturedSort = sortCaptor.getValue();
        assertNotNull(capturedSort, "Sort 객체가 존재해야함");
        assertEquals(expectedSort.toString(), capturedSort.toString(), "정렬 순서가 일치해야함");

        assertNotNull(result, "반환 결과가 있어야함");
        assertEquals(mockProduct.size(), result.size(), "반환 상품의 개수는 일치해야함");
        assertEquals(mockProduct, result, "반환 상품 리스트랑 mock이 일치해야함");
    }

    @Test
    void 상품제목기반_검색(){
        // given
        String searchTitle = "테스트";
        ProductSearchCondition searchCondition = new ProductSearchCondition();
        searchCondition.setProductTitle(searchTitle);

        Specification<Product> mockTitleSpec = mock(Specification.class);

        ArgumentCaptor<Specification<Product>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        try(MockedStatic<ProductSpecification> mockedSpec =
                    mockStatic(ProductSpecification.class)){
            mockedSpec.when(()->ProductSpecification.titleLike(searchTitle)).thenReturn(mockTitleSpec);
            when(productRepository.findAll(specCaptor.capture(), sortCaptor.capture())).thenReturn(mockProduct);

            // when
            List<Product> result = productService.searchProducts(searchCondition);

            // then
            verify(productRepository, times(1)).findAll(any(Specification.class), eq(expectedSort));

            verifyNoInteractions(categoryService);

            Sort capturedSort = sortCaptor.getValue();
            assertNotNull(capturedSort, "Sort 객체는 있어야함");
            assertEquals(expectedSort.toString(), capturedSort.toString(), "정렬 순서는 일치해야함");

            Specification<Product> capturedSpec = specCaptor.getValue();
            assertNotNull(capturedSpec, "Specification은 있어야 함");

            assertNotNull(result, "검색 결과는 있어야 함");
            assertEquals(mockProduct, result, "반환 상품과 mock 리스트는 같아야함");

        }

    }
    @Test
    void 카테고리기반_검색(){
        // given
        Long searchCategoryId = 2L;
        List<Long> descendantCategoryIds = Arrays.asList(2L, 6L, 7L);
        ProductSearchCondition searchCondition = new ProductSearchCondition();
        searchCondition.setCategoryId(searchCategoryId);

        when(categoryService.getAllDescendantCategoryIds(searchCategoryId)).thenReturn(descendantCategoryIds);

        Specification<Product> mockCategorySpec = mock(Specification.class);

        ArgumentCaptor<Specification<Product>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        try( MockedStatic<ProductSpecification> mockedSpec =
                     mockStatic(ProductSpecification.class)){
            mockedSpec.when(()->ProductSpecification.categoryIn(descendantCategoryIds)).thenReturn(mockCategorySpec);
            when(productRepository.findAll(specCaptor.capture(), sortCaptor.capture())).thenReturn(mockProduct);

            // when
            List<Product> result = productService.searchProducts(searchCondition);

            // then
            verify(productRepository, times(1)).findAll(any(Specification.class), eq(expectedSort));

            Sort capturedSort = sortCaptor.getValue();
            assertNotNull(capturedSort, "Sort 객체는 있어야함");
            assertEquals(expectedSort.toString(), capturedSort.toString(), "정렬 순서는 일치해야함");

            Specification<Product> capturedSpec = specCaptor.getValue();
            assertNotNull(capturedSpec, "Specification은 있어야 함");

            assertNotNull(result, "검색 결과는 있어야 함");
            assertEquals(mockProduct, result, "반환 상품과 mock 리스트는 같아야함");
            assertEquals(mockProduct.size(), result.size(), "반환 상품의 개수가 같아야함 ");

        }
    }

    @Test
    void 판매자이름기반_검색(){
        // given
        String searchSeller = "테스트 판매자";

        ProductSearchCondition searchCondition = new ProductSearchCondition();
        searchCondition.setSellerUsername(searchSeller);

        Specification<Product> mockCategorySpec = mock(Specification.class);

        ArgumentCaptor<Specification<Product>> specCaptor = ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

        try( MockedStatic<ProductSpecification> mockedSpec =
                     mockStatic(ProductSpecification.class)){

            mockedSpec.when(()->ProductSpecification.hasOwnerUsername(searchSeller)).thenReturn(mockCategorySpec);
            when(productRepository.findAll(specCaptor.capture(), sortCaptor.capture())).thenReturn(mockProduct);

            // when
            List<Product> result = productService.searchProducts(searchCondition);

            // then
            verify(productRepository, times(1)).findAll(any(Specification.class), eq(expectedSort));
            verifyNoInteractions(categoryService);

            Sort capturedSort = sortCaptor.getValue();
            assertNotNull(capturedSort, "Sort 객체는 있어야함");
            assertEquals(expectedSort.toString(), capturedSort.toString(), "정렬 순서는 일치해야함");

            Specification<Product> capturedSpec = specCaptor.getValue();
            assertNotNull(capturedSpec, "Specification은 있어야 함");

            assertNotNull(result, "검색 결과는 있어야 함");
            assertEquals(mockProduct, result, "반환 상품과 mock 리스트는 같아야함");
            assertEquals(mockProduct.size(), result.size(), "반환 상품의 개수가 같아야함 ");

        }
    }
}
