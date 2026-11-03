package neatlogic.module.cmdb.service.cientity;

import neatlogic.framework.cmdb.dto.cientity.AlertLevelVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;
import neatlogic.framework.cmdb.enums.alertlevel.AlertLevelType;
import neatlogic.module.cmdb.dao.mapper.cientity.AlertLevelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityAlertMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/** 在 CMDB 模块事务中持久化来源告警，与 Informant 实现模块无依赖。 */
@Service
public class InformantAlertServiceImpl implements InformantAlertService {
    @Resource
    private CiEntityAlertMapper ciEntityAlertMapper;
    @Resource
    private AlertLevelMapper alertLevelMapper;

    /** 恢复和解除关联均按来源 UUID 清理；未知级别 0 仍是活动告警。 */
    @Override
    @Transactional
    public void synchronizeAlert(CiEntityAlertVo alert, boolean recovered) {
        if (recovered || alert.getCiEntityId() == null) {
            ciEntityAlertMapper.deleteSourceCiEntityAlert(alert.getSource(), alert.getAlertUuid());
            return;
        }
        AlertLevelVo level = alertLevelMapper.getAlertLevelByNameAndType(alert.getMetricValue(), AlertLevelType.INFORMANT.getValue());
        if (level != null) {
            alert.setLevel(level.getLevel());
        } else {
            alert.setLevel(0);
        }
        ciEntityAlertMapper.upsertSourceCiEntityAlert(alert);
    }
}
