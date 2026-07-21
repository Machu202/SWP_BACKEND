package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.SystemParameter;
import java.util.List;

public interface SystemParameterService {
    List<SystemParameter> getAllParameters();
    SystemParameter getParameterByKey(String key);
    
    // Creates a system parameter.
    SystemParameter createParameter(String key, String value, String type, Long currentUserId);
    
    SystemParameter updateParameter(String key, String value, String type, Long currentUserId);
    
    // Deletes a system parameter.
    void deleteParameter(String key, Long currentUserId);
}
