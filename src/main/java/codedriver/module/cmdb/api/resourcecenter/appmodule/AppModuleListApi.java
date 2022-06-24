/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.appmodule;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.util.TableResultUtil;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * @author linbq
 * @since 2021/6/16 15:04
 **/
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class AppModuleListApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
        List<ResourceVo> resourceVoList = new ArrayList<>();
        Set<Long> idSet = new HashSet<>();
        ResourceSearchVo searchVo = paramObj.toJavaObject(ResourceSearchVo.class);
        List<Long> appSystemIdList = searchVo.getAppSystemIdList();
        if (CollectionUtils.isNotEmpty(appSystemIdList)) {
            List<Long> idList = resourceCenterMapper.getAppModuleIdListByAppSystemIdList(appSystemIdList, TenantContext.get().getDataDbName());
            idSet.addAll(idList);
        } else {
            appSystemIdList = resourceCenterMapper.getAppSystemIdList(searchVo);
            searchVo.setAppSystemIdList(appSystemIdList);
            if (CollectionUtils.isNotEmpty(appSystemIdList)) {
                List<Long> idList = resourceCenterMapper.getAppModuleIdListByAppSystemIdList(appSystemIdList, TenantContext.get().getDataDbName());
                idSet.addAll(idList);
            }
            List<Long> idList = resourceCenterMapper.getAppModuleIdList(searchVo);
            idSet.addAll(idList);
        }

        if (CollectionUtils.isNotEmpty(idSet)) {
            int rowNum = idSet.size();
            searchVo.setRowNum(rowNum);
            if (searchVo.getCurrentPage() <= searchVo.getPageCount()) {
                int fromIndex = searchVo.getStartNum();
                int toIndex = fromIndex + searchVo.getPageSize();
                toIndex = Math.min(toIndex, rowNum);
                List<Long> idList = new ArrayList<>(idSet);
                idList.sort(Comparator.reverseOrder());
                List<Long> currentPageIdList = idList.subList(fromIndex, toIndex);
                if (CollectionUtils.isNotEmpty(currentPageIdList)) {
                    resourceVoList = resourceCenterMapper.getAppModuleListByIdList(currentPageIdList, TenantContext.get().getDataDbName());
                }
            }
        }
        return TableResultUtil.getResult(resourceVoList, searchVo);
    }
}
