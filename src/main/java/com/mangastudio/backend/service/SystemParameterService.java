package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.SystemParameter;
import java.util.List;

public interface SystemParameterService {
    List<SystemParameter> getAllParameters();
    SystemParameter getParameterByKey(String key);
    
    // [BỔ SUNG] Hàm tạo mới tham số
    SystemParameter createParameter(String key, String value);
    
    SystemParameter updateParameter(String key, String value);
    
    // [BỔ SUNG] Hàm xóa tham số
    void deleteParameter(String key);
}