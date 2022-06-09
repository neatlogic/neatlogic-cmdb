/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.discovery;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.discovery.DiscoverConfCombopVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.SYNC_MODIFY;
import codedriver.module.cmdb.dao.mapper.discovery.DiscoveryMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
@AuthAction(action = SYNC_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetDiscoveryConfCombopApi extends PrivateApiComponentBase {

    @Resource
    private DiscoveryMapper discoveryMapper;


    @Override
    public String getToken() {
        return "/cmdb/discovery/combop/get";
    }

    @Override
    public String getName() {
        return "获取自动发现配置组合工具关系";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "confId", type = ApiParamType.LONG, isRequired = true, desc = "自动发现配置id")})
    @Output({@Param(explode = DiscoverConfCombopVo.class)})
    @Description(desc = "获取自动发现配置组合工具关系接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return discoveryMapper.getDiscoveryConfCombopByConfId(jsonObj.getLong("confId"));
    }

}
