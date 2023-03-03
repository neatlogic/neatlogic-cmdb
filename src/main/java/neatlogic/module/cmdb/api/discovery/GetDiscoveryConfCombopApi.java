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

package neatlogic.module.cmdb.api.discovery;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.discovery.DiscoverConfCombopVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.SYNC_MODIFY;
import neatlogic.module.cmdb.dao.mapper.discovery.DiscoveryMapper;
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
