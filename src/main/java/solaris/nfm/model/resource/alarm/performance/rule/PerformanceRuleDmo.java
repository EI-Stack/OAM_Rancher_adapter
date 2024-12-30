package solaris.nfm.model.resource.alarm.performance.rule;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.model.base.manager.DmoBase;

@Service
@Slf4j
public class PerformanceRuleDmo extends DmoBase<PerformanceRule, PerformanceRuleDao>
{}
