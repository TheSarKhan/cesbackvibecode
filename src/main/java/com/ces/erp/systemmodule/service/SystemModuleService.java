package com.ces.erp.systemmodule.service;

import com.ces.erp.systemmodule.dto.SystemModuleResponse;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SystemModuleService {

    private final SystemModuleRepository systemModuleRepository;

    public List<SystemModuleResponse> getAll() {
        return systemModuleRepository.findAllByOrderByOrderIndexAsc().stream()
                .map(SystemModuleResponse::from)
                .toList();
    }
}
