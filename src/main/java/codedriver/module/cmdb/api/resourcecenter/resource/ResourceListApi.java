/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询资源中心数据列表接口
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/list";
    }

    @Override
    public String getName() {
        return "查询资源中心数据列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "模糊搜索"),
            @Param(name = "viewName", type = ApiParamType.STRING, desc = "视图名"),
            @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "数据列表")
    })
    @Description(desc = "查询资源中心数据列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        List<ResourceVo> resourceVoList = null;
        ResourceSearchVo searchVo = JSON.toJavaObject(jsonObj, ResourceSearchVo.class);
        Long typeId = searchVo.getTypeId();
        if(typeId != null) {
            CiVo ciVo = ciMapper.getCiById(typeId);
            if(ciVo == null){
                throw new CiNotFoundException(typeId);
            }
            List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            if(CollectionUtils.isNotEmpty(ciList)){
                List<Long> typeIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
                searchVo.setTypeIdList(typeIdList);
            }
        }
        int rowNum = resourceCenterMapper.getResourceCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<Long> idList = resourceCenterMapper.getResourceIdList(searchVo);
            if (CollectionUtils.isNotEmpty(idList)) {
                resourceVoList = resourceCenterMapper.getResourceListByIdList(idList, TenantContext.get().getDataDbName());
                for(ResourceVo resourceVo : resourceVoList){
                    List<String> tagNameList = resourceCenterMapper.getTagNameListByResourceId(resourceVo.getId());
                    resourceVo.setTagList(tagNameList);
                }
            }
        }
        if(resourceVoList == null){
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
