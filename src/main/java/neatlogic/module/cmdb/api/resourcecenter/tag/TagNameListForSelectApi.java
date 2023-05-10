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

package neatlogic.module.cmdb.api.resourcecenter.tag;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.auth.label.CMDB;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.dto.ValueTextVo;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/7/7 10:15
 **/
@Service
@AuthAction(action = CMDB.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TagNameListForSelectApi extends PrivateApiComponentBase {

    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/tag/name/list/forselect";
    }

    @Override
    public String getName() {
        return "查询资源中心标签名称列表（下拉列表专用）";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "模糊搜索"),
            @Param(name = "defaultValue", type = ApiParamType.JSONARRAY, desc = "用于回显的标签列表"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = ValueTextVo[].class, desc = "标签列表")
    })
    @Description(desc = "查询资源中心标签名称列表（下拉列表专用）")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        List<ValueTextVo> tbodyList = new ArrayList<>();
        JSONObject resultObj = new JSONObject();
        resultObj.put("tbodyList", tbodyList);
        JSONArray defaultValue = paramObj.getJSONArray("defaultValue");
        if (CollectionUtils.isNotEmpty(defaultValue)) {
            for (int i = 0; i < defaultValue.size(); i++) {
                String tagName = defaultValue.getString(i);
                tbodyList.add(new ValueTextVo(tagName, tagName));
            }
            return resultObj;
        }
        TagVo searchVo = JSON.toJavaObject(paramObj, TagVo.class);
        int rowNum = resourceTagMapper.getTagCount(searchVo);
        if (rowNum > 0) {
            searchVo.setRowNum(rowNum);
            List<String> tagNameList = resourceTagMapper.getTagNameListForSelect(searchVo);
            if (CollectionUtils.isNotEmpty(tagNameList)) {
                for (String tagName : tagNameList) {
                    tbodyList.add(new ValueTextVo(tagName, tagName));
                }
            }
        }
        resultObj.put("rowNum", rowNum);
        resultObj.put("pageCount", searchVo.getPageCount());
        resultObj.put("currentPage", searchVo.getCurrentPage());
        resultObj.put("pageSize", searchVo.getPageSize());
        return resultObj;
    }
}
