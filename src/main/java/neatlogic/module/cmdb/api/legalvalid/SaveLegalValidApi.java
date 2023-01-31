/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.legalvalid;

import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.legalvalid.LegalValidVo;
import neatlogic.framework.cmdb.exception.legalvalid.LegalValidNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.scheduler.core.IJob;
import neatlogic.framework.scheduler.core.SchedulerManager;
import neatlogic.framework.scheduler.dto.JobObject;
import neatlogic.framework.cmdb.auth.label.CI_MODIFY;
import neatlogic.module.cmdb.dao.mapper.legalvalid.LegalValidMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = CI_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveLegalValidApi extends PrivateApiComponentBase {

    @Resource
    private LegalValidMapper legalValidMapper;

    @Resource
    private SchedulerManager schedulerManager;


    @Override
    public String getToken() {
        return "/cmdb/legalvalid/save";
    }

    @Override
    public String getName() {
        return "保存合规校验规则";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id，不提供代表新增"),
            @Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "名称", xss = true, maxLength = 50),
            @Param(name = "isActive", type = ApiParamType.INTEGER, isRequired = true, desc = "是否激活", rule = "0,1"),
            @Param(name = "type", type = ApiParamType.ENUM, rule = "ci,custom", isRequired = true, desc = "校验类型"),
            @Param(name = "cron", type = ApiParamType.STRING, isRequired = true, desc = "时间表达式"),
            @Param(name = "rule", type = ApiParamType.JSONOBJECT, desc = "自定义规则")})
    @Output({@Param(explode = LegalValidVo[].class)})
    @Description(desc = "保存合规校验规则接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        LegalValidVo legalValidVo = JSONObject.toJavaObject(jsonObj, LegalValidVo.class);
        Long id = jsonObj.getLong("id");
        if (id == null) {
            legalValidMapper.insertLegalValid(legalValidVo);
        } else {
            if (legalValidMapper.getLegalValidById(id) == null) {
                throw new LegalValidNotFoundException(id);
            }
            legalValidMapper.updateLegalValid(legalValidVo);
        }
        if (StringUtils.isNotEmpty(legalValidVo.getCron())) {
            IJob handler = SchedulerManager.getHandler("neatlogic.module.cmdb.schedule.handler.LegalValidScheduleJob");
            String tenantUuid = TenantContext.get().getTenantUuid();
            JobObject newJobObject = new JobObject.Builder(legalValidVo.getId().toString(), handler.getGroupName(), handler.getClassName(), tenantUuid).withCron(legalValidVo.getCron()).addData("legalValidId", legalValidVo.getId()).build();
            schedulerManager.loadJob(newJobObject);
        }
        return null;
    }
}
