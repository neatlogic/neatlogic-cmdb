/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.tag;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.tag.TagVo;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.tag.CmdbTagMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchTagApi extends PrivateApiComponentBase {

    @Autowired
    private CmdbTagMapper cmdbTagMapper;


    @Override
    public String getToken() {
        return "/cmdb/tag/search";
    }

    @Override
    public String getName() {
        return "查询配置项标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字", xss = true)})
    @Output({@Param(explode = TagVo.class)})
    @Description(desc = "查询配置项标签接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        TagVo tagVo = JSONObject.toJavaObject(jsonObj, TagVo.class);
        return cmdbTagMapper.searchTagList(tagVo);
    }

}
