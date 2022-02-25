/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */


package codedriver.module.cmdb.publicapi;

import codedriver.framework.cmdb.dto.cientity.CiEntityInspectVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.publicapi.PublicApiComponentBase;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class UpdateCiEntityInspectStatusApi extends PublicApiComponentBase {


    @Resource
    private CiEntityMapper ciEntityMapper;


    @Override
    public String getToken() {
        return "cmdb/cientity/updateinspectstatus";
    }

    @Override
    public String getName() {
        return "修改配置项巡检状态";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "jobId", type = ApiParamType.LONG, isRequired = true, desc = "作业id"),
            @Param(name = "ciEntityId", type = ApiParamType.LONG, isRequired = true, desc = "配置项id"),
            @Param(name = "inspectStatus", type = ApiParamType.ENUM, rule = "normal,warn,critical,fatal", isRequired = true, desc = "巡检状态"),
            @Param(name = "inspectTime", type = ApiParamType.LONG, isRequired = true, desc = "巡检时间，格式：距1970年1月1日0时0分0秒的豪秒数")})
    @Output({})
    @Description(desc = "修改配置项巡检状态接口，自动化巡检时使用此接口更新巡检状态")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setId(paramObj.getLong("ciEntityId"));
        ciEntityVo.setInspectTime(new Date(paramObj.getLong("inspectTime")));
        ciEntityVo.setInspectStatus(paramObj.getString("inspectStatus"));
//        ciEntityMapper.updateCiEntityInspectStatus(ciEntityVo);
        CiEntityInspectVo ciEntityInspectVo = new CiEntityInspectVo(paramObj.getLong("jobId"),paramObj.getLong("ciEntityId"),new Date(paramObj.getLong("inspectTime")),paramObj.getString("inspectStatus"));
        ciEntityMapper.insertCiEntityInspect(ciEntityInspectVo);
        return null;
    }


}
