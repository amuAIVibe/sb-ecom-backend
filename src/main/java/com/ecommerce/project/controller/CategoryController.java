package com.ecommerce.project.controller;

import java.util.ArrayList;
import java.util.List;

import com.ecommerce.project.config.AppConstants;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.coyote.Request;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;


@RestController
@RequestMapping("/api")
public class CategoryController {


    //@Autowired(Field Injection is not recommended)
    @Autowired
    private CategoryService categoryService;

    //Constructor Injection
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    //@GetMapping("/public/categories")
    @Tag(name = "Category APIs", description = "APIs for managing categories")
    @Operation(summary = "Get Category", description = "API to get all categories")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Categories are fetched successfully"),
            @ApiResponse(responseCode = "401", description = "Not Authenticated", content = @Content),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content)
    })
    @RequestMapping(value = "/public/categories", method = RequestMethod.GET)
    public ResponseEntity<CategoryResponse> getCategories(
            @RequestParam(name = "pageNumber", defaultValue = AppConstants.PAGE_NUMBER, required = false)
            Integer pageNumber,

            @RequestParam(name = "pageSize", defaultValue = AppConstants.PAGE_SIZE, required = false)
            Integer pageSize,

            @RequestParam(name = "sortBy", defaultValue = AppConstants.SORT_CATEGORIES_BY, required = false)
            String sortBy,

            @RequestParam(name = "sortOrder", defaultValue = AppConstants.SORT_DIR, required = false)
            String sortOrder
    ){
        CategoryResponse categoryResponse = categoryService.getAllCategories(
                pageNumber, pageSize, sortBy, sortOrder
        );
        return new ResponseEntity<>(categoryResponse, HttpStatus.OK);
    }

    //@PostMapping("/public/categories")
    @Tag(name = "Category APIs", description = "APIs for managing categories")
    @RequestMapping(value = "/public/categories", method = RequestMethod.POST)
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryDTO categoryDTO){
        CategoryDTO savedCategoryDTO = categoryService.createCategory(categoryDTO);
        return new ResponseEntity<>(savedCategoryDTO, HttpStatus.CREATED);
    }

    //@PutMapping("/public/categories/{categoryId}")
    @Tag(name = "Category APIs", description = "APIs for managing categories")
    @RequestMapping(value = "/public/categories/{categoryId}", method = RequestMethod.PUT)
    public ResponseEntity<CategoryDTO> updateCategory(@Valid @RequestBody CategoryDTO categoryDTO,
                                                 @PathVariable Long categoryId){
            CategoryDTO updatedCategoryDTO = categoryService.updateCategory(categoryDTO, categoryId);
            return new ResponseEntity<>(
                    updatedCategoryDTO,
                    HttpStatus.OK
            );
    }

    //@DeleteMapping("/admin/categories/{categoryId}")
    @Tag(name = "Category APIs", description = "APIs for managing categories")
    @RequestMapping(value = "/admin/categories/{categoryId}", method = RequestMethod.DELETE)
    public ResponseEntity<CategoryDTO> deleteCategory(
            @Parameter(description = "Category Id you want to delete") @PathVariable Long categoryId
    ){
            CategoryDTO deleteCategory = categoryService.deleteCategory(categoryId);
            return ResponseEntity.status(HttpStatus.OK).body(deleteCategory);
    }
}
