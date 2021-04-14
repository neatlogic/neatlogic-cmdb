/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.attr;

import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.framework.cmdb.dto.ci.AttrTypeVo;
import codedriver.framework.cmdb.enums.AttrType;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListAttrTypeApi extends PrivateApiComponentBase {
    @Override
    public String getName() {
        return "获取属性类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = AttrTypeVo[].class)})
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<AttrTypeVo> attrTypeList = new ArrayList<>();
        for (AttrType at : AttrType.values()) {
            attrTypeList.add(new AttrTypeVo(at));
        }
        return attrTypeList;
    }

    @Override
    public String getToken() {
        return "/cmdb/attrtype/list";
    }
}
