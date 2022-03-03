package com.ticker.common.controller;

import com.ticker.common.service.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

/**
 * The type Base controller.
 *
 * @param <S> the type parameter
 */
@Controller
public class BaseController<S extends BaseService> {

    @Autowired
    private S baseService;

    /**
     * Gets memory statistics.
     *
     * @return the memory statistics
     */
    @GetMapping("memory-status")
    public ResponseEntity<Map<String, String>> getMemoryStatistics() {
        return new ResponseEntity<>(baseService.getMemoryStatistics(), HttpStatus.OK);
    }

    /**
     * Gets executor details.
     *
     * @return the executor details
     */
    @GetMapping("/executors")
    public ResponseEntity<Map<String, Map<String, Integer>>> getExecutorDetails() {
        return new ResponseEntity<>(baseService.getExecutorDetailMap(), HttpStatus.OK);
    }
}
