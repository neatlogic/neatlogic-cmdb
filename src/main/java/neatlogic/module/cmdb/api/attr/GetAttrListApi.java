/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.attr;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetAttrListApi extends PrivateApiComponentBase {

    @Autowired
    private AttrMapper attrMapper;

    @Override
    public String getToken() {
        return "/cmdb/attr/getlist";
    }

    @Override
    public String getName() {
        return "获取属性详细信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "idList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "属性id列表")})
    @Output({@Param(explode = AttrVo[].class)})
    @Description(desc = "获取属性详细信息接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        JSONArray idList = jsonObj.getJSONArray("idList");
        List<Long> attrIdList = new ArrayList<>();
        for (int i = 0; i < idList.size(); i++) {
            attrIdList.add(idList.getLong(i));
        }
        return attrMapper.getAttrByIdList(attrIdList);
    }
}
