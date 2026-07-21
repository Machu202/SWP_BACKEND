package com.mangastudio.backend.controller;

import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.service.SystemParameterService;
import com.mangastudio.backend.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/v1/system-parameters")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemParameterController {

    private final SystemParameterService parameterService;

    // Returns every system parameter.
    @GetMapping
    public ResponseEntity<List<SystemParameter>> getAllParameters() {
        return ResponseEntity.ok(parameterService.getAllParameters());
    }

    // Returns one system parameter by key.
    @GetMapping("/{key}")
    public ResponseEntity<SystemParameter> getParameterByKey(@PathVariable String key) {
        return ResponseEntity.ok(parameterService.getParameterByKey(key));
    }

    // Creates a system parameter (Admin only).
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SystemParameter> createParameter(
            @RequestParam String key, 
            @RequestParam String value,
            @RequestParam(defaultValue = "STRING") String type,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        SystemParameter newParam = parameterService.createParameter(key, value, type, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(newParam);
    }

    // Updates a system parameter (Admin only).
    @PutMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')") 
    public ResponseEntity<SystemParameter> updateParameter(
            @PathVariable String key, 
            @RequestParam String value,
            @RequestParam(required = false) String type,
            Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        return ResponseEntity.ok(parameterService.updateParameter(key, value, type, userDetails.getId()));
    }

    // Deletes a system parameter (Admin only).
    @DeleteMapping("/{key}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> deleteParameter(@PathVariable String key, Authentication authentication) {
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        parameterService.deleteParameter(key, userDetails.getId());
        return ResponseEntity.ok("System parameter '" + key + "' deleted successfully.");
    }
}
