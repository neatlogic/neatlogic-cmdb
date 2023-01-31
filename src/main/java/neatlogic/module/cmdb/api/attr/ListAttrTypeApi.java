/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.attr;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.dto.ci.AttrTypeVo;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
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
        return AttrValueHandlerFactory.getAttrTypeList();
    }

    @Override
    public String getToken() {
        return "/cmdb/attrtype/list";
    }
}
