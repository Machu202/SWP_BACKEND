package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-parameters")
@RequiredArgsConstructor
public class SystemParameterController {

    private final SystemParameterService parameterService;

    // Lấy toàn bộ cấu hình hệ thống
    @GetMapping
    public ResponseEntity<List<SystemParameter>> getAllParameters() {
        return ResponseEntity.ok(parameterService.getAllParameters());
    }

    // Lấy một cấu hình cụ thể theo Key
    @GetMapping("/{key}")
    public ResponseEntity<SystemParameter> getParameterByKey(@PathVariable String key) {
        return ResponseEntity.ok(parameterService.getParameterByKey(key));
    }

    // Chỉ Admin mới có quyền cập nhật tham số hệ thống
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<SystemParameter> updateParameter(
            @PathVariable String key, 
            @RequestParam String value) {
        return ResponseEntity.ok(parameterService.updateParameter(key, value));
    }
}