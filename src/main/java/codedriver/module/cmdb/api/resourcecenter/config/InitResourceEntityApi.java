/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.config;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.cmdb.enums.resourcecenter.Status;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterResourceFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import codedriver.module.cmdb.utils.ResourceEntityViewBuilder;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author linbq
 * @since 2022/07/20 17:19
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class InitResourceEntityApi extends PrivateApiComponentBase {
    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/init";
    }

    @Override
    public String getName() {
        return "初始化资源配置信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
    })
    @Description(desc = "初始化资源配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getAllResourceEntity();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            resourceEntityVo.setError("");
            resourceEntityVo.setStatus(Status.PENDING.getValue());
            resourceEntityMapper.updateResourceEntity(resourceEntityVo);
            String xml = resourceEntityMapper.getResourceEntityXmlByName(resourceEntityVo.getXml());
            resourceEntityVo.setXml(xml);
            ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(resourceEntityVo);
            builder.buildView();
        }
        return resourceEntityMapper.getAllResourceEntity();
    }
}
