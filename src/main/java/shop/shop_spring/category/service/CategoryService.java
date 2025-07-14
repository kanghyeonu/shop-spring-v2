package shop.shop_spring.category.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import shop.shop_spring.category.repository.CategoryRepository;
import shop.shop_spring.category.domain.Category;
import shop.shop_spring.exception.DataNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public List<Category> findByParentIsNull() {
        var result = categoryRepository.findByParentIsNull();
        if (result.size() == 0){
            throw new DataNotFoundException("DB 오류");
        }
        return result;
    }

    public List<Category> findByParentId(Long parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    public Category findById(Long categoryId) {
        Optional<Category> result = categoryRepository.findById(categoryId);
        if (result.isEmpty()){
            throw new DataNotFoundException("잘못된 카테고리 아이디");
        }
        return result.get();
    }

    // 하위 카테고리 조회
    public List<Long> getAllDescendantCategoryIds(Long categoryId){
        List<Long> categoryIds = new ArrayList<>();
        if (categoryId == null){
            return categoryIds;
        }

        Optional<Category> result = categoryRepository.findById(categoryId);
        if (result.isPresent()){
            Category category =  result.get();
            categoryIds.add(category.getId());

            findAllDescendantIds(category, categoryIds);
        }

        return categoryIds;
    }

    private void findAllDescendantIds(Category parent, List<Long> ids) {
        for (Category child : parent.getChildren()){
            ids.add(child.getId());
            if(!child.getChildren().isEmpty()){
                findAllDescendantIds(child, ids);
            }
        }
    }
}
