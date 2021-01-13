package codedriver.module.cmdb.api.prop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.prop.PropMapper;
import codedriver.module.cmdb.dto.prop.PropVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class GetPropApi extends PrivateApiComponentBase {

	@Autowired
	private PropMapper propMapper;

	@Override
	public String getToken() {
		return "/cmdb/prop/get";
	}

	@Override
	public String getName() {
		return "获取基础属性详细信息";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({ @Param(name = "id", type = ApiParamType.LONG, desc = "基础属性id", isRequired = true) })
	@Output({ @Param(explode = PropVo.class) })
	@Description(desc = "获取基础属性详细信息接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		Long id = jsonObj.getLong("id");
		return propMapper.getPropById(id);
	}

}
