/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.config;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
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
 * @since 2021/11/9 11:26
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class ConfigSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterConfigMapper resourceCenterConfigMapper;
    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getToken() {
        return "resourcecenter/config/save";
    }
    @Override
    public String getName() {
        return "保存资源中心配置信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "id", type = ApiParamType.LONG, desc = "主键id"),
            @Param(name = "config", type = ApiParamType.STRING, isRequired = true, desc = "配置信息")
    })
    @Output({
            @Param(name = "Return", explode = ResourceCenterConfigVo.class, desc = "配置信息")
    })
    @Description(desc = "保存资源中心配置信息")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceCenterConfigVo resourceCenterConfigVo = paramObj.toJavaObject(ResourceCenterConfigVo.class);
        ResourceCenterConfigVo oldResourceCenterConfigVo = resourceCenterConfigMapper.getResourceCenterConfigLimitOne();
        if (oldResourceCenterConfigVo != null) {
            //如果配置没有修改，不重新生成视图
            if (Objects.equals(oldResourceCenterConfigVo.getConfig(), resourceCenterConfigVo.getConfig())) {
                List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getAllResourceEntity();
                resourceCenterConfigVo.setResourceEntityList(resourceEntityList);
                resourceCenterConfigVo.setConfig(null);
                return resourceCenterConfigVo;
            }
        }
        Long id = paramObj.getLong("id");
        if (id == null) {
            resourceCenterConfigMapper.insertResourceCenterConfig(resourceCenterConfigVo);
        } else {
            if (resourceCenterConfigMapper.checkResourceCenterConfigIsExists(id) == 0) {
                resourceCenterConfigMapper.insertResourceCenterConfig(resourceCenterConfigVo);
            } else {
                resourceCenterConfigMapper.updateResourceCenterConfig(resourceCenterConfigVo);
            }
        }
//        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(resourceCenterConfigVo.getConfig());
//        builder.buildView();
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getAllResourceEntity();
        resourceCenterConfigVo.setResourceEntityList(resourceEntityList);
        resourceCenterConfigVo.setConfig(null);
        return resourceCenterConfigVo;
    }
}
