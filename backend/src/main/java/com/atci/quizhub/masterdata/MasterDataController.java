package com.atci.quizhub.masterdata;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masterdata")
public class MasterDataController {
    private final MasterDataService service;
    public MasterDataController(MasterDataService service) { this.service = service; }

    @GetMapping("/stacks")
    public List<Map<String, Object>> stacks() {
        return service.allStacks().stream()
                .map(s -> Map.<String,Object>of("id", s.getId(), "name", s.getName()))
                .toList();
    }

    @GetMapping("/topics")
    public List<Map<String, Object>> topics(@RequestParam Long stackId) {
        return service.topicsForStack(stackId).stream()
                .map(t -> Map.<String,Object>of("id", t.getId(), "name", t.getName()))
                .toList();
    }
}
