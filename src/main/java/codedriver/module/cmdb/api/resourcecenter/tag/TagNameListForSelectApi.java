/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.tag;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
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
@AuthAction(action = CMDB_BASE.class)
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
