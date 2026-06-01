package com.atci.quizhub.masterdata;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MasterDataService {
    private final TechStackRepository stacks;
    private final TopicRepository topics;
    public MasterDataService(TechStackRepository stacks, TopicRepository topics) {
        this.stacks = stacks; this.topics = topics;
    }
    public List<TechStack> allStacks() { return stacks.findAll(); }
    public List<Topic> topicsForStack(Long stackId) { return topics.findByStackId(stackId); }
}
