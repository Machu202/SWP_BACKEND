package com.mangastudio.backend.service;

import com.mangastudio.backend.entity.SystemParameter;
import java.util.List;

public interface SystemParameterService {
    List<SystemParameter> getAllParameters();
    SystemParameter getParameterByKey(String key);
    SystemParameter updateParameter(String key, String value);
}