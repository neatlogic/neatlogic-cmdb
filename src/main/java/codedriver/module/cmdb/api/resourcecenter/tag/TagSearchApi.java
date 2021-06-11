/*
 * Copyright(c) 2021. TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.tag;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.dto.OperateVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_TAG_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class TagSearchApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

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
            @Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", explode = TagVo[].class, desc = "标签列表")
    })
    @Description(desc = "查询资源中心标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONObject resultObj = new JSONObject();
        TagVo tagVo = JSON.toJavaObject(paramObj, TagVo.class);
        List<TagVo> tagList = resourceCenterMapper.searchTag(tagVo);
        resultObj.put("tbodyList", tagList);
        if (CollectionUtils.isNotEmpty(tagList)) {
            Boolean hasAuth = AuthActionChecker.check(RESOURCECENTER_TAG_MODIFY.class.getSimpleName());
            tagList.stream().forEach(o -> {
                OperateVo delete = new OperateVo("delete", "删除");
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
        }
        int rowNum = resourceCenterMapper.searchTagCount(tagVo);
        tagVo.setRowNum(rowNum);
        resultObj.put("rowNum", rowNum);
        resultObj.put("pageCount", PageUtil.getPageCount(rowNum, tagVo.getPageSize()));
        resultObj.put("currentPage", tagVo.getCurrentPage());
        resultObj.put("pageSize", tagVo.getPageSize());
        return resultObj;
    }

}
