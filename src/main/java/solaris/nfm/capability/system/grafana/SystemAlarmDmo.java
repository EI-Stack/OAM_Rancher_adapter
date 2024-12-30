package solaris.nfm.capability.system.grafana;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import solaris.nfm.model.base.manager.DmoBase;

@Service
@Slf4j
public class SystemAlarmDmo extends DmoBase<SystemAlarm, SystemAlarmDao>
{}
