/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.reltype;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.cmdb.dto.ci.RelTypeVo;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Output;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.module.cmdb.dao.mapper.ci.RelTypeMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetRelTypeListApi extends PrivateApiComponentBase {

    @Autowired
    private RelTypeMapper relTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/reltype/list";
    }

    @Override
    public String getName() {
        return "获取模型关系类型列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({@Param(explode = RelTypeVo[].class)})
    @Description(desc = "获取模型关系类型列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        return relTypeMapper.getAllRelType();
    }
}
