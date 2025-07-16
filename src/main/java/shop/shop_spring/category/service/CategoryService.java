package shop.shop_spring.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.shop_spring.category.repository.CategoryRepository;
import shop.shop_spring.category.domain.Category;
import shop.shop_spring.common.exception.DataNotFoundException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findByParentIsNull() {
        var result = categoryRepository.findByParentIsNull();
        if (result.size() == 0){
            throw new DataNotFoundException("상위 카테고리가 존재하지 않음");
        }
        return result;
    }

    public List<Category> findByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public Category findById(Long categoryId) {
        Category result = getCategoryOrThrow(categoryId);
        return result;
    }

    // 하위 카테고리 조회
    public List<Long> getAllDescendantCategoryIds(Long categoryId) {
        if (categoryId == null) return Collections.emptyList();

        List<Category> allCategories = categoryRepository.findAll();

        Optional<Category> targetCategoryOpt = allCategories.stream()
                .filter( c -> categoryId.equals(c.getId()))
                .findFirst();

        List<Long> categoryIds = new ArrayList<>();
        if (targetCategoryOpt.isPresent()) {
            Category targetCategory = targetCategoryOpt.get();
            categoryIds.add(targetCategory.getId());
            findAllDescendantIds(targetCategory, categoryIds, allCategories); // 재귀 함수 호출
        }
//        // 기존 코드
//        Category category = getCategoryOrThrow(categoryId);
//        List<Long> categoryIds = new ArrayList<>();
//        categoryIds.add(category.getId());
//
//        findAllDescendantIds(category, categoryIds);

        return categoryIds;
    }

    private void findAllDescendantIds(Category parent, List<Long> ids, List<Category> allCategories) {
        // 메모리에서 부모의 자식 카테고리 찾기
        List<Category> children = allCategories.stream()
                .filter(c -> c.getParent() != null && c.getParent().getId().equals(parent.getId()))
                .toList();

        for (Category child : children) {
            ids.add(child.getId());
            // 자식의 자식들을 메모리에서 다시 탐색
            findAllDescendantIds(child, ids, allCategories);
        }
    }

    private Category getCategoryOrThrow(Long id){
        return categoryRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("잘못된 카테고리 아이디"));
    }

}
