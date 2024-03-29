/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

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

package neatlogic.module.cmdb.api.resourcecenter.tag;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.tag.TagVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.common.util.PageUtil;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.resourcecenter.ResourceTagMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TagSearchApi extends PrivateApiComponentBase {

    @Resource
    private ResourceTagMapper resourceTagMapper;

    @Override
    public String getToken() {
        return "resourcecenter/tag/search";
    }

    @Override
    public String getName() {
        return "查询资源中心标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键词"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页数据条目"),
            @Param(name = "needPage", type = ApiParamType.BOOLEAN, desc = "是否需要分页，默认true")
    })
    @Output({
            @Param(name = "tbodyList", explode = TagVo[].class, desc = "标签列表"),
            @Param(explode = BasePageVo.class),
    })
    @Description(desc = "查询资源中心标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        TagVo tagVo = JSON.toJavaObject(paramObj, TagVo.class);
        List<TagVo> tagList = resourceTagMapper.searchTag(tagVo);
        resultObj.put("tbodyList", tagList);
        /*if (CollectionUtils.isNotEmpty(tagList)) {
            Boolean hasAuth = AuthActionChecker.check(RESOURCECENTER_TAG_MODIFY.class.getSimpleName());
            tagList.stream().forEach(o -> {
                OperateVo delete = new OperateVo("delete", "删除");
                o.getOperateList().add(delete);
                if (hasAuth) {
                    if (o.getAssetsCount() > 0) {
                        delete.setDisabled(1);
                        delete.setDisabledReason("当前标签已被引用，不可删除");
                    }
                } else {
                    delete.setDisabled(1);
                    delete.setDisabledReason("无权限，请联系管理员");
                }
            });
        }*/
        int rowNum = resourceTagMapper.searchTagCount(tagVo);
        tagVo.setRowNum(rowNum);
        resultObj.put("rowNum", rowNum);
        resultObj.put("pageCount", PageUtil.getPageCount(rowNum, tagVo.getPageSize()));
        resultObj.put("currentPage", tagVo.getCurrentPage());
        resultObj.put("pageSize", tagVo.getPageSize());
        return resultObj;
    }

}
