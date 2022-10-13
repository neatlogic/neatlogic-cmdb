/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.app;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import codedriver.framework.cmdb.auth.label.CMDB;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAppSystemForSelectApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appsystem/list/forselect";
    }
    @Override
    public String getName() {
        return "查询资源应用列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "默认值列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ResourceVo[].class, desc = "资源应用列表")
    })
    @Description(desc = "查询资源应用列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        BasePageVo searchVo = paramObj.toJavaObject(BasePageVo.class);
        JSONArray defaultValue = searchVo.getDefaultValue();
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            List<Long> idList = defaultValue.toJavaList(Long.class);
            List<ResourceVo> resourceList = resourceMapper.searchAppSystemListByIdList(idList);
            return TableResultUtil.getResult(resourceList);
        } else {
            int rowNum = resourceMapper.searchAppSystemCount(searchVo);
            if (rowNum > 0) {
                searchVo.setRowNum(rowNum);
                if (searchVo.getNeedPage()) {
                    List<Long> idList = resourceMapper.searchAppSystemIdList(searchVo);
                    List<ResourceVo> resourceList = resourceMapper.searchAppSystemListByIdList(idList);
                    return TableResultUtil.getResult(resourceList, searchVo);
                } else {
                    List<ResourceVo> allResourceList = new ArrayList<>();
                    int pageCount = searchVo.getPageCount();
                    for (int currentPage = 1; currentPage <= pageCount; currentPage++) {
                        List<Long> idList = resourceMapper.searchAppSystemIdList(searchVo);
                        List<ResourceVo> resourceList = resourceMapper.searchAppSystemListByIdList(idList);
                        allResourceList.addAll(resourceList);
                    }
                    return TableResultUtil.getResult(allResourceList, searchVo);
                }
            }
        }
        return null;
    }
}
