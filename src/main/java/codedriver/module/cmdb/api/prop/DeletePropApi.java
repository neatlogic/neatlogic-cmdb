/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.prop;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.PROP_MODIFY;
import codedriver.module.cmdb.dao.mapper.prop.PropMapper;
import codedriver.framework.cmdb.dto.prop.PropVo;
import codedriver.module.cmdb.exception.prop.PropIsInUsedException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@AuthAction(action = PROP_MODIFY.class)
@OperationType(type = OperationTypeEnum.DELETE)
public class DeletePropApi extends PrivateApiComponentBase {

	@Autowired
	private PropMapper propMapper;

	@Override
	public String getToken() {
		return "/cmdb/prop/delete";
	}

	@Override
	public String getName() {
		return "删除基础属性";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({ @Param(name = "id", type = ApiParamType.LONG, desc = "id", isRequired = true) })
	@Description(desc = "删除基础属性接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long id = jsonObj.getLong("id");
		if (propMapper.checkPropIsInUsed(id) > 0) {
			PropVo propVo = propMapper.getPropById(id);
			throw new PropIsInUsedException(propVo.getName());
		}
		propMapper.deletePropById(id);
		return null;
	}

}
