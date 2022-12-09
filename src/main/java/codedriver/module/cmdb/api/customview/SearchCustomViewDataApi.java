/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.customview;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.customview.CustomViewAttrVo;
import codedriver.framework.cmdb.dto.customview.CustomViewConditionVo;
import codedriver.framework.cmdb.dto.customview.CustomViewVo;
import codedriver.framework.cmdb.enums.customview.SearchMode;
import codedriver.framework.cmdb.exception.customview.CustomViewNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.customview.CustomViewMapper;
import codedriver.module.cmdb.service.customview.CustomViewDataService;
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
        return "查询自定义视图数据";
    }

    @Override
    public String getToken() {
        return "/cmdb/customview/data/search";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "视图id"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "视图名称"),
            @Param(name = "searchMode", type = ApiParamType.ENUM, rule = "normal,group,data", isRequired = true, desc = "搜索模式：normal或group"),
            @Param(name = "groupBy", type = ApiParamType.STRING, desc = "分组属性的uuid"),
            @Param(name = "attrFilterList", type = ApiParamType.JSONARRAY, desc = "属性过滤条件"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小"),
            @Param(name = "mode", type = ApiParamType.ENUM, rule = "page,api", desc = "搜索模式，支持page和api两种，主要影响返回的数据结构，page是默认模式，用于页面展示，api会返回字段名，用于api调用")
    })
    @Output({@Param(name = "dataList", type = ApiParamType.JSONARRAY, desc = "结果集"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页码"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小")})
    @Example(example = "{\"id\":588094621147136,\"keyword\":\"\",\"pageSize\":20,\"searchMode\":\"normal\",\"currentPage\":1,\"attrFilterList\":[{\"attrUuid\":\"546d7fb7276e40f889cd131e22bb547a\",\"valueList\":[\"192.168.0.22\"],\"expression\":\"like\",\"type\":\"attr\"}]}")
    @Description(desc = "查询自定义视图数据接口")
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
            if (customViewVo == null) {
                throw new CustomViewNotFoundException(id);
            }
        } else if (StringUtils.isNotBlank(name)) {
            customViewVo = customViewMapper.getCustomViewByName(name);
            if (customViewVo == null) {
                throw new CustomViewNotFoundException(name);
            }
        }
        customViewConditionVo.setCustomViewId(customViewVo.getId());
        JSONObject returnObj = new JSONObject();
        if (customViewConditionVo.getSearchMode().equals(SearchMode.NORMAL.getValue())) {
            returnObj.put("dataList", customViewDataService.searchCustomViewData(customViewConditionVo));
        } else if (customViewConditionVo.getSearchMode().equals(SearchMode.GROUP.getValue())) {
            returnObj.put("dataList", customViewDataService.searchCustomViewDataGroup(customViewConditionVo));
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
