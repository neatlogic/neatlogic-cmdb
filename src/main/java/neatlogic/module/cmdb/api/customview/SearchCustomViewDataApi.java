/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.api.customview;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.customview.CustomViewAttrVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewConditionVo;
import neatlogic.framework.cmdb.dto.customview.CustomViewVo;
import neatlogic.framework.cmdb.enums.customview.SearchMode;
import neatlogic.framework.cmdb.exception.customview.CustomViewNotFoundException;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.customview.CustomViewMapper;
import neatlogic.module.cmdb.service.customview.CustomViewDataService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCustomViewDataApi extends PrivateApiComponentBase {
    @Resource
    private CustomViewMapper customViewMapper;
    @Resource
    private CustomViewDataService customViewDataService;

    @Override
    public String getName() {
        return "nmcac.searchcustomviewdataapi.getname";
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/data/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "term.cmdb.viewid"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "term.cmdb.viewname"),
            @Param(name = "searchMode", type = ApiParamType.ENUM, rule = "normal,group,data", isRequired = true, desc = "nmcac.searchcustomviewdataapi.input.param.desc.searchmode"),
            @Param(name = "groupBy", type = ApiParamType.STRING, desc = "nmcac.searchcustomviewdataapi.input.param.desc.groupby"),
            @Param(name = "attrFilterList", type = ApiParamType.JSONARRAY, desc = "nmcac.exportcientityapi.input.param.desc.attrfilterlist"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "mode", type = ApiParamType.ENUM, rule = "page,api", desc = "nmcac.searchcustomviewdataapi.input.param.desc.mode")
    })
    @Output({@Param(name = "dataList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcustomviewdataapi.output.param.desc.datalist"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize")})
    @Example(example = "{\"id\":588094621147136,\"keyword\":\"\",\"pageSize\":20,\"searchMode\":\"normal\",\"currentPage\":1,\"attrFilterList\":[{\"attrUuid\":\"546d7fb7276e40f889cd131e22bb547a\",\"valueList\":[\"192.168.0.22\"],\"expression\":\"like\",\"type\":\"attr\"}]}")
    @Description(desc = "nmcac.searchcustomviewdataapi.getname")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long id = paramObj.getLong("id");
        String name = paramObj.getString("name");
        if (id == null && StringUtils.isBlank(name)) {
            throw new ParamNotExistsException("id", "name");
        }
        String mode = paramObj.getString("mode");
        if (StringUtils.isBlank(mode)) {
            mode = "page";
        }
        CustomViewConditionVo customViewConditionVo = JSONObject.toJavaObject(paramObj, CustomViewConditionVo.class);
        CustomViewVo customViewVo = null;
        if (id != null) {
            customViewVo = customViewMapper.getCustomViewById(id);
        } else if (StringUtils.isNotBlank(name)) {
            customViewVo = customViewMapper.getCustomViewByName(name);
        }
        if (customViewVo == null) {
            throw new CustomViewNotFoundException(name);
        }
        customViewConditionVo.setCustomViewId(customViewVo.getId());
        JSONObject returnObj = new JSONObject();

        if (customViewConditionVo.getSearchMode().equals(SearchMode.NORMAL.getValue())) {
            returnObj.put("dataList", customViewDataService.searchCustomViewData(customViewConditionVo));
            returnObj.put("dataCount", customViewConditionVo.getRowNum());
            returnObj.put("dataLimit", customViewConditionVo.getLimit());
        } else if (customViewConditionVo.getSearchMode().equals(SearchMode.GROUP.getValue())) {
            returnObj.put("dataList", customViewDataService.searchCustomViewDataGroup(customViewConditionVo));
            returnObj.put("dataCount", customViewConditionVo.getRowNum());
            returnObj.put("dataLimit", customViewConditionVo.getLimit());
        } else {
            returnObj.put("dataList", customViewDataService.searchCustomViewDataFlatten(customViewConditionVo));
        }
        if (CollectionUtils.isNotEmpty(returnObj.getJSONArray("dataList")) && mode.equals("api")) {
            List<CustomViewAttrVo> attrList = customViewMapper.getCustomViewAttrByCustomViewId(new CustomViewAttrVo(customViewConditionVo.getCustomViewId()));
            JSONArray newDataList = new JSONArray();
            for (int i = 0; i < returnObj.getJSONArray("dataList").size(); i++) {
                JSONObject dataObj = returnObj.getJSONArray("dataList").getJSONObject(i);
                JSONObject newDataObj = new JSONObject();
                for (CustomViewAttrVo attrVo : attrList) {
                    newDataObj.put(attrVo.getAlias(), dataObj.get(attrVo.getUuid()));
                }
                newDataList.add(newDataObj);
            }
            returnObj.put("dataList", newDataList);
        }
        returnObj.put("pageSize", customViewConditionVo.getPageSize());
        returnObj.put("currentPage", customViewConditionVo.getCurrentPage());
        return returnObj;
    }


}
