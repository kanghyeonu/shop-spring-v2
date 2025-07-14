package shop.shop_spring.category;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import shop.shop_spring.category.repository.CategoryRepository;
import shop.shop_spring.category.service.CategoryService;
import shop.shop_spring.category.domain.Category;
import shop.shop_spring.exception.DataNotFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {
    @InjectMocks
    private CategoryService categoryService;
    @Mock
    private CategoryRepository categoryRepository;

    @Test
    void 상위_카테고리_조회_성공(){
        // given
        Category category1 = new Category();
        category1.setId(1L);
        category1.setName("의류");
        category1.setParent(null);

        Category category2 = new Category();
        category2.setId(2L);
        category2.setName("가전/디지털");
        category2.setParent(null);

        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(category1, category2));

        // when
        List<Category> parentCategories = categoryService.findByParentIsNull();

        // then
        assertEquals(2, parentCategories.size(), "2개의 상위 카테고리가 조회되어야함");
        assertEquals("의류", parentCategories.get(0).getName(), "첫 번째 카테고리는 '의류'");
        assertEquals("가전/디지털", parentCategories.get(1).getName(), "두 번째 카테고리는 '가전/디지털'");

    }

    @Test
    void 상위_카테고리_조회_실패(){
        // given
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of());

        // when & then
        assertThrows(DataNotFoundException.class, ()->{
            categoryService.findByParentIsNull();
        }, "상위 카테고리가 없다면 오류");
    }

    @Test
    void 상세_카테고리_조회(){
        // given
        Long parentId = 2L;
        Category category1 = new Category();
        category1.setId(5L);
        category1.setName("에어컨");

        Category category2 = new Category();
        category2.setId(6L);
        category2.setName("냉장고");

        when(categoryRepository.findByParentId(parentId)).thenReturn(List.of(category1, category2));

        // when
        List<Category> childrenCategory = categoryService.findByParentId(parentId);

        // then
        assertEquals(2, childrenCategory.size(), "2개의 하위 카테고리가 조회되어야함");
        assertEquals("에어컨", childrenCategory.get(0).getName(), "첫 번째 카테고리는 '에어컨'");
        assertEquals("냉장고", childrenCategory.get(1).getName(), "두 번째 카테고리는 '냉장고'");
    }

    @Test
    void 상세_카테고리_조회_비었음(){
        // given
        Long nonParentId = 100L;

        when(categoryRepository.findByParentId(nonParentId)).thenReturn(List.of());
        // when
        List<Category> emptyChildrenCategory = categoryService.findByParentId(nonParentId);

        // then
        assertNotNull(emptyChildrenCategory, "반환된 리스트는 null이 아님");
        assertTrue(emptyChildrenCategory.isEmpty(), "하위 카테고리가 비어있어야함");
        assertEquals(0, emptyChildrenCategory.size(), "하위 카테고리 개수는 0개");
    }
}
