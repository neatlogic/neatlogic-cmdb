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

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.batchimport.ImportAuditVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.batchimport.ImportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchBatchImportAuditApi extends PrivateApiComponentBase {

    @Autowired
    private ImportMapper importMapper;

    @Override
    public String getToken() {
        return "/cmdb/import/audit/search";
    }

    @Override
    public String getName() {
        return "nmcab.searchbatchimportauditapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "currentPage",
                    type = ApiParamType.INTEGER,
                    desc = "common.currentpage"),
            @Param(name = "pageSize",
                    type = ApiParamType.INTEGER,
                    desc = "common.pagesize"),
            @Param(name = "needPage",
                    type = ApiParamType.BOOLEAN,
                    desc = "ocommn.isneedpage")
    })
    @Output({
            @Param(name = "tbodyList",
                    type = ApiParamType.JSONARRAY,
                    explode = ImportAuditVo[].class,
                    desc = "common.loglist"),
            @Param(explode = BasePageVo.class)
    })
    @Description(desc = "nmcab.searchbatchimportauditapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject returnObj = new JSONObject();
        ImportAuditVo importAuditVo = JSONObject.toJavaObject(paramObj, ImportAuditVo.class);
        if (importAuditVo.getNeedPage()) {
            int rowNum = importMapper.searchImportAuditCount(importAuditVo);
            returnObj.put("pageSize", importAuditVo.getPageSize());
            returnObj.put("currentPage", importAuditVo.getCurrentPage());
            returnObj.put("rowNum", rowNum);
            returnObj.put("pageCount", importAuditVo.getPageCount());
        }
        List<ImportAuditVo> list = importMapper.searchImportAudit(importAuditVo);
        returnObj.put("tbodyList", list);
        return returnObj;
    }
}
