package com.example.emptySaver.controller;

import com.example.emptySaver.domain.dto.CategoryDto;
import com.example.emptySaver.domain.dto.CategoryDto.categoryType;
import com.example.emptySaver.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/category")
public class CategoryController {
    private final CategoryService categoryService;
    @GetMapping("/getAllList")
    @Operation(summary = "모든 카테고리 및 태그 조회", description = "모든 카테고리 및 그 하위 태그들을 리턴하는 API")
    public ResponseEntity<CategoryDto.res> getAllCategoryList(){
        return new ResponseEntity<>(categoryService.getAllCategories(), HttpStatus.OK);
    }
    @GetMapping("/getCategoryList")
    @Operation(summary = "모든 카테고리 목록 조회", description = "모든 카테고리 목록을 조회하는 API")
    public ResponseEntity<CategoryDto.res> getCategoryList(){
        return new ResponseEntity<>(new CategoryDto.res(categoryService.getCategoryNames()),HttpStatus.OK);
    }

    @GetMapping("/getLabels/{categoryType}")
    @Operation(summary = "해당 카테고리의 모든 라벨 조회", description = "categoryType에 맞는 하위 라벨들을 리턴하는 API")
    public ResponseEntity<categoryType> getLabelsByCategory(@PathVariable String categoryType){
        return new ResponseEntity<>(categoryService.getLabelInfoByCategoryName(categoryType),HttpStatus.OK);
    }
    @PostMapping("/interest")
    @Operation(summary = "회원의 관심사 저장",description = "회원의 관심사를 저장하는 API")
    public ResponseEntity<String> saveMemberInterest(@RequestBody CategoryDto.saveMemberInterestReq req){
        categoryService.saveInterest(req);
        return new ResponseEntity<>("saved Interest",HttpStatus.OK);
    }

}
