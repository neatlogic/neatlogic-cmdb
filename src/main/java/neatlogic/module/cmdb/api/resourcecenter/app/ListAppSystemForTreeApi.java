/*Copyright (C) $today.year  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.cmdb.api.resourcecenter.app;

import com.alibaba.fastjson.JSONObject;
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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
