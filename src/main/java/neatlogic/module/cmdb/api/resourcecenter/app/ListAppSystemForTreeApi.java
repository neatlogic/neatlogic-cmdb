/*
Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package neatlogic.module.cmdb.api.resourcecenter.app;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.resourcecenter.AppModuleVo;
import neatlogic.framework.cmdb.dto.resourcecenter.AppSystemVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAppSystemForTreeApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;

    @Override
    public String getToken() {
        return "resourcecenter/appsystem/list/fortree";
    }

    @Override
    public String getName() {
        return "查询资源应用列表树";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = AppSystemVo[].class, desc = "资源应用列表")
    })
    @Description(desc = "查询资源应用列表")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<AppSystemVo> tbodyList = new ArrayList<>();
        BasePageVo searchVo = paramObj.toJavaObject(BasePageVo.class);
        String keyword = searchVo.getKeyword();
        int count = resourceMapper.getAppSystemIdListCountByKeyword(keyword);
        if (count > 0) {
            searchVo.setRowNum(count);
            List<Long> appSystemIdList = resourceMapper.getAppSystemIdListByKeyword(searchVo);
            if (CollectionUtils.isEmpty(appSystemIdList)) {
                return TableResultUtil.getResult(tbodyList, searchVo);
            }
            tbodyList = resourceMapper.getAppSystemListByIdList(appSystemIdList);
            if (StringUtils.isNotEmpty(searchVo.getKeyword())) {
                List<AppModuleVo> appModuleList = resourceMapper.getAppModuleListByKeywordAndAppSystemIdList(keyword, appSystemIdList);
                if (CollectionUtils.isNotEmpty(appModuleList)) {
                    Map<Long, List<AppModuleVo>> appModuleMap = new HashMap<>();
                    for (AppModuleVo appModuleVo : appModuleList) {
                        appModuleMap.computeIfAbsent(appModuleVo.getAppSystemId(), key -> new ArrayList<>()).add(appModuleVo);
                    }
                    for (AppSystemVo appSystemVo : tbodyList) {
                        List<AppModuleVo> appModuleVoList = appModuleMap.get(appSystemVo.getId());
                        if (CollectionUtils.isNotEmpty(appModuleVoList)) {
                            appSystemVo.setAppModuleList(appModuleVoList);
                            appSystemVo.setIsHasModule(1);
                        }
                    }
                }
            } else {
                List<Long> hasModuleAppSystemIdList = resourceMapper.getHasModuleAppSystemIdListByAppSystemIdList(appSystemIdList);
                if (CollectionUtils.isNotEmpty(hasModuleAppSystemIdList)) {
                    for (AppSystemVo appSystemVo : tbodyList) {
                        if (hasModuleAppSystemIdList.contains(appSystemVo.getId())) {
                            appSystemVo.setIsHasModule(1);
                        }
                    }
                }
            }
        }
        return TableResultUtil.getResult(tbodyList, searchVo);
    }
}
