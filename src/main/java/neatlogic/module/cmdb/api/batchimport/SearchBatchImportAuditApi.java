/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

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
