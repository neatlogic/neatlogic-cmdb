/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.appsystem;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.AppEnviromentVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/18 15:31
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppSystemListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appsystem/list";
    }

    @Override
    public String getName() {
        return "查询资源中心应用系统列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "应用系统列表")
    })
    @Description(desc = "查询资源中心应用系统列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        List<ResourceVo> resourceVoList = null;
        ResourceSearchVo searchVo = JSON.toJavaObject(paramObj, ResourceSearchVo.class);
        int rowNum = resourceCenterMapper.getAppSystemCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getAppSystemIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                resourceVoList = resourceCenterMapper.getAppSystemListByIdList(idList, TenantContext.get().getDataDbName());
            }
        }
        if (resourceVoList == null) {
            resourceVoList = new ArrayList<>();
        }
        resultObj.put("tbodyList", resourceVoList);
        resultObj.put("rowNum", rowNum);
        resultObj.put("pageCount", searchVo.getPageCount());
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", searchVo.getPageSize());
        return resultObj;
    }
}
