/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.appmodule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;

/**
 * @author linbq
 * @since 2021/6/16 15:04
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppModuleListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appmodule/list";
    }

    @Override
    public String getName() {
        return "查询资源中应用模块列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索"),
            @Param(name = "appSystemIdList", type = ApiParamType.JSONARRAY, desc = "应用系统id列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "应用模块列表")
    })
    @Description(desc = "查询资源中应用模块列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        int count = resourceMapper.searchAppModuleCount(searchVo, TenantContext.get().getDataDbName());
        int count2 = resourceMapper.searchAppModuleCountNew(searchVo);
        System.out.println(count);
        System.out.println(count2);
        if (!Objects.equals(count, count2)) {
            System.out.println("a");
        }
        if (count > 0) {
            searchVo.setRowNum(count);
            List<Long> idList = resourceMapper.searchAppModuleIdListNew(searchVo);
            List<ResourceVo> resourceList2 = resourceMapper.searchAppModuleNew(idList, TenantContext.get().getDataDbName());
            List<ResourceVo> resourceList = resourceMapper.searchAppModule(searchVo, TenantContext.get().getDataDbName());
            System.out.println(JSONObject.toJSONString(resourceList));
            System.out.println(JSONObject.toJSONString(resourceList2));
            if (!Objects.equals(JSONObject.toJSONString(resourceList), JSONObject.toJSONString(resourceList2))) {
                System.out.println("b");
            }
            return TableResultUtil.getResult(resourceList, searchVo);
        }
        return null;
    }
}
