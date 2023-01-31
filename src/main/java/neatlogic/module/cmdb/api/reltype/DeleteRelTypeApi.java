/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.api.reltype;

import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.annotation.Description;
import neatlogic.framework.restful.annotation.Input;
import neatlogic.framework.restful.annotation.OperationType;
import neatlogic.framework.restful.annotation.Param;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.cmdb.auth.label.RELTYPE_MODIFY;
import neatlogic.module.cmdb.dao.mapper.ci.RelTypeMapper;
import neatlogic.framework.cmdb.exception.reltype.RelTypeIsInUsedException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AuthAction(action = RELTYPE_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
@Transactional
public class DeleteRelTypeApi extends PrivateApiComponentBase {

    @Autowired
    private RelTypeMapper relTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/reltype/delete";
    }

    @Override
    public String getName() {
        return "删除模型关系类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, isRequired = true, desc = "关系类型id")})
    @Description(desc = "删除模型关系类型接口，已经在使用中的关系类型不允许删除")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long id = jsonObj.getLong("id");
        if (relTypeMapper.checkRelTypeIsInUsed(id) > 0) {
            throw new RelTypeIsInUsedException();
        }
        return relTypeMapper.deleteRelTypeById(id);
    }
}
