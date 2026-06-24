package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.entity.SystemParameter;
import com.mangastudio.backend.repository.SystemParameterRepository;
import com.mangastudio.backend.service.SystemParameterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemParameterServiceImpl implements SystemParameterService {

    private final SystemParameterRepository parameterRepository;

    @Override
    public List<SystemParameter> getAllParameters() {
        return parameterRepository.findAll();
    }

    @Override
    public SystemParameter getParameterByKey(String key) {
        return parameterRepository.findByParamKey(key)
                .orElseThrow(() -> new RuntimeException("Error: Parameter key not found: " + key));
    }

    // [BỔ SUNG] Triển khai tạo mới
    @Override
    @Transactional
    public SystemParameter createParameter(String key, String value) {
        // Kiểm tra xem Key đã tồn tại trong Database chưa
        if (parameterRepository.findByParamKey(key).isPresent()) {
            throw new RuntimeException("Error: Parameter key already exists: " + key);
        }
        
        SystemParameter newParam = SystemParameter.builder()
                .paramKey(key)
                .paramValue(value)
                .build();
                
        return parameterRepository.save(newParam);
    }

    @Override
    @Transactional
    public SystemParameter updateParameter(String key, String value) {
        SystemParameter parameter = parameterRepository.findByParamKey(key)
                .orElseThrow(() -> new RuntimeException("Error: Parameter key not found: " + key));
        
        parameter.setParamValue(value);
        return parameterRepository.save(parameter);
    }

    // [BỔ SUNG] Triển khai xóa
    @Override
    @Transactional
    public void deleteParameter(String key) {
        SystemParameter parameter = parameterRepository.findByParamKey(key)
                .orElseThrow(() -> new RuntimeException("Error: Parameter key not found: " + key));
                
        parameterRepository.delete(parameter);
    }
}