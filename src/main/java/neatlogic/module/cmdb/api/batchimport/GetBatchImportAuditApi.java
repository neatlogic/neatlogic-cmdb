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

package neatlogic.module.cmdb.api.batchimport;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.batchimport.ImportAuditVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetBatchImportAuditApi extends PrivateApiComponentBase {

    @Autowired
    private ImportMapper importMapper;

    @Override
    public String getToken() {
        return "/cmdb/import/audit/get";
    }

    @Override
    public String getName() {
        return "获取单个配置项导入日志";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true)})
    @Output({@Param(explode = ImportAuditVo.class)})
    @Description(desc = "获取单个配置项导入日志接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        return importMapper.getImportAuditById(paramObj.getLong("id"));
    }
}
