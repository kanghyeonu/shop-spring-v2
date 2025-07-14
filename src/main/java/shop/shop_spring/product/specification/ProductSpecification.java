package shop.shop_spring.product.specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;
import shop.shop_spring.product.domain.Product;
import shop.shop_spring.product.enums.Status;

import java.util.List;

public class ProductSpecification {
    public static Specification<Product> statusIn(List<Status> statuses){
        return (root, query, criteriaBuilder) -> {
            if (statuses == null || statuses.isEmpty()){
                return null;
            }
            Path<Status> statusPath = root.get("status");

            CriteriaBuilder.In<Status> inClause = criteriaBuilder.in(statusPath);
            for (Status status : statuses){
                inClause.value(status);
            }
            return inClause;
        };
    }

    public static Specification<Product> titleLike(String title){ // <- 대소문자 구분 없음
        return (root, query, criteriaBuilder) -> {
            if (title == null || title.trim().isEmpty()){
                return null;
            }
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), "%" + title.toLowerCase() + "%");
        };
    }

    public static Specification<Product> categoryIn(List<Long> categoryIds){
        return (root, query, criteriaBuilder) -> {
             if (categoryIds == null || categoryIds.isEmpty()){
                 return null;
             }
             return root.get("category").get("id").in(categoryIds);
        };
    }

    public static Specification<Product> hasOwnerUsername(String username){
        return (root, query, criteriaBuilder) -> {
            if (username == null || username.trim().isEmpty()){
                return null;
            }
            return criteriaBuilder.equal(root.get("username"), username.trim());
        };
    }
}
