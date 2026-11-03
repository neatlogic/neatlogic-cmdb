package neatlogic.module.cmdb.service.cientity;

import neatlogic.framework.cmdb.dto.cientity.CiEntityAlertVo;

/** 将已校验的 Informant 服务状态投影为 CMDB 活动告警。 */
public interface InformantAlertService {
    /** 按 UUID 更新或消除告警，正常返回表示数据库事务已提交。 */
    void synchronizeAlert(CiEntityAlertVo alert, boolean recovered);
}
