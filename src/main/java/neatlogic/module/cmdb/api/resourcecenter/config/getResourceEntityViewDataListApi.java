package neatlogic.module.cmdb.api.resourcecenter.config;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.TenantContext;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.dao.mapper.SchemaMapper;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceEntityMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class getResourceEntityViewDataListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceEntityMapper resourceEntityMapper;

    @Resource
    private SchemaMapper schemaMapper;

    @Override
    public String getName() {
        return "nmcarc.getresourceentityviewdatalistapi.getname";
    }

    @Input({
            @Param(name = "name", type = ApiParamType.STRING, isRequired = true, desc = "common.name"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
    })
    @Output({
            @Param(name = "Return", type = ApiParamType.JSONOBJECT, desc = "common.tbodylist")
    })
    @Description(desc = "nmcarc.getresourceentityviewdatalistapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        String name = paramObj.getString("name");
        JSONArray theadList = new JSONArray();
        List<String> columnNameList = schemaMapper.getTableOrViewAllColumnNameList(TenantContext.get().getDataDbName(), name);
        for (String columnName : columnNameList) {
            JSONObject thead = new JSONObject();
            thead.put("key", columnName);
            thead.put("title", columnName);
            theadList.add(thead);
        }
        BasePageVo basePageVo = paramObj.toJavaObject(BasePageVo.class);
        int rowNum = resourceEntityMapper.getResourceEntityViewDataCount(name);
        if (rowNum == 0) {
            return new JSONObject();
        }
        basePageVo.setRowNum(rowNum);
        List<Map<String, Object>> tbodyList = resourceEntityMapper.getResourceEntityViewDataList(name, basePageVo.getStartNum(), basePageVo.getPageSize());
        return TableResultUtil.getResult(theadList, tbodyList, basePageVo);
    }

    @Override
    public String getToken() {
        return "resourcecenter/resourceentity/viewdata/list";
    }
}
