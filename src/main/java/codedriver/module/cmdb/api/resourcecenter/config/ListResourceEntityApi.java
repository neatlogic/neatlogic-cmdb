/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.config;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author linbq
 * @since 2021/11/9 11:28
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListResourceEntityApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/list";
    }

    @Override
    public String getName() {
        return "获取资源中心配置列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(name = "Return", explode = ResourceEntityVo[].class)})
    @Description(desc = "获取资源中心配置列表接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return resourceEntityMapper.getAllResourceEntity();
    }
}
