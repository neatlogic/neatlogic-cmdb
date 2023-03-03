/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.resourcecenter.config;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.resourcecenter.config.ResourceEntityVo;
import neatlogic.framework.cmdb.enums.resourcecenter.Status;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_MODIFY;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import neatlogic.module.cmdb.utils.ResourceEntityViewBuilder;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author linbq
 * @since 2022/07/20 17:19
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class RebuildResourceEntityApi extends PrivateApiComponentBase {
    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/rebuild";
    }

    @Override
    public String getName() {
        return "重建所有资源视图";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "重建所有资源视图接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<ResourceEntityVo> resourceEntityList = resourceEntityMapper.getAllResourceEntity();
        for (ResourceEntityVo resourceEntityVo : resourceEntityList) {
            resourceEntityVo.setError("");
            resourceEntityVo.setStatus(Status.PENDING.getValue());
            resourceEntityMapper.updateResourceEntityStatusAndError(resourceEntityVo);
            String xml = resourceEntityMapper.getResourceEntityXmlByName(resourceEntityVo.getName());
            if (StringUtils.isNotBlank(xml)) {
                resourceEntityVo.setXml(xml);
                ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(resourceEntityVo);
                builder.buildView();
            }
        }
        return resourceEntityMapper.getAllResourceEntity();
    }
}
