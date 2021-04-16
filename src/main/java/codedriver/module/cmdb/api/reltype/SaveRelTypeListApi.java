/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.reltype;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RELTYPE_MODIFY;
import codedriver.module.cmdb.dao.mapper.ci.RelTypeMapper;
import codedriver.framework.cmdb.dto.ci.RelTypeVo;
import codedriver.framework.cmdb.exception.reltype.RelTypeNameIsExistsException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = RELTYPE_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class SaveRelTypeListApi extends PrivateApiComponentBase {

    @Autowired
    private RelTypeMapper relTypeMapper;

    @Override
    public String getToken() {
        return "/cmdb/reltype/save";
    }

    @Override
    public String getName() {
        return "保存模型关系类型";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "id", type = ApiParamType.LONG, desc = "关系类型id，不提供代表添加"), @Param(name = "name",
        type = ApiParamType.STRING, maxLength = 10, isRequired = true, xss = true, desc = "关系类型名称")})
    @Description(desc = "保存模型关系类型接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        RelTypeVo relTypeVo = JSONObject.toJavaObject(jsonObj, RelTypeVo.class);
        if (relTypeMapper.checkRelTypeNameIsExists(relTypeVo) > 0) {
            throw new RelTypeNameIsExistsException(relTypeVo.getName());
        }
        if (jsonObj.getLong("id") == null) {
            relTypeMapper.insertRelType(relTypeVo);
        } else {
            relTypeMapper.updateRelType(relTypeVo);
        }
        return relTypeVo.getId();
    }
}
