/*
 * Copyright (c)  2022 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.resourcecenter.resource;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import neatlogic.module.cmdb.service.resourcecenter.resource.IResourceCenterResourceService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListResourceCustomApi extends PrivateApiComponentBase {
    @Resource
    ResourceMapper resourceMapper;

    @Resource
    private IResourceCenterResourceService resourceCenterResourceService;

    @Override
    public String getName() {
        return "高级查询资源中心数据列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "searchMode", type = ApiParamType.STRING, xss = true, desc = "搜索模式：value|text，默认搜索value"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "conditionGroupList", type = ApiParamType.JSONARRAY, desc = "条件组"),
            @Param(name = "conditionGroupRelList", type = ApiParamType.JSONARRAY, desc = "条件组之间的关系"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表")
    })
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        ResourceSearchVo resourceSearch = JSONObject.toJavaObject(paramObj, ResourceSearchVo.class);
        List<ResourceVo> resourceList = new ArrayList<>();
        StringBuilder sqlSb = new StringBuilder();
        resourceSearch.buildConditionWhereSql(sqlSb, resourceSearch);
        int rowNum = resourceMapper.getResourceCountByDynamicCondition(resourceSearch, sqlSb.toString());
        if (rowNum == 0) {
            return TableResultUtil.getResult(resourceList, resourceSearch);
        }
        resourceSearch.setRowNum(rowNum);
        List<Long> idList =  resourceMapper.getResourceIdListByDynamicCondition(resourceSearch, sqlSb.toString());
        resourceList = resourceMapper.getResourceListByIdList(idList);
        if (CollectionUtils.isNotEmpty(resourceList)) {
            resourceCenterResourceService.addTagAndAccountInformation(resourceList);
        }
        //排序
        List<ResourceVo> resultList = new ArrayList<>();
        for (Long id : idList) {
            for (ResourceVo resourceVo : resourceList) {
                if (Objects.equals(id, resourceVo.getId())) {
                    resultList.add(resourceVo);
                    break;
                }
            }
        }
        return TableResultUtil.getResult(resultList, resourceSearch);
    }

    @Override
    public String getToken() {
        return "resourcecenter/resource/custom/list";
    }
}
