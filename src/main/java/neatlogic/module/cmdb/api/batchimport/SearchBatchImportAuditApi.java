/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
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
