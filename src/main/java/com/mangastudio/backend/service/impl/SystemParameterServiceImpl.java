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

    @Override
    @Transactional
    public SystemParameter updateParameter(String key, String value) {
        SystemParameter parameter = parameterRepository.findByParamKey(key)
                .orElseThrow(() -> new RuntimeException("Error: Parameter key not found: " + key));
        
        parameter.setParamValue(value);
        return parameterRepository.save(parameter);
    }
}