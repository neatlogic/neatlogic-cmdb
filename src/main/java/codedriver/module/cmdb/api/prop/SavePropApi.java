package codedriver.module.cmdb.api.prop;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.prop.PropMapper;
import codedriver.module.cmdb.dto.prop.PropVo;
import codedriver.module.cmdb.exception.prop.PropLabelIsExistsException;
import codedriver.module.cmdb.exception.prop.PropNameIsExistsException;

@Service
@OperationType(type = OperationTypeEnum.UPDATE)
public class SavePropApi extends PrivateApiComponentBase {

	@Autowired
	private PropMapper propMapper;

	@Override
	public String getToken() {
		return "/cmdb/prop/save";
	}

	@Override
	public String getName() {
		return "保存基础属性";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({ @Param(name = "id", type = ApiParamType.LONG, desc = "id,不提供代表新增"), @Param(name = "name", type = ApiParamType.STRING, desc = "英文名称", xss = true, isRequired = true), @Param(name = "label", type = ApiParamType.STRING, desc = "中文名称", xss = true, isRequired = true), @Param(name = "description", type = ApiParamType.STRING, desc = "说明"), @Param(name = "handler", type = ApiParamType.STRING, desc = "控件", isRequired = true),
			@Param(name = "config", type = ApiParamType.JSONOBJECT, desc = "控件配置") })
	@Output({ @Param(name = "id", type = ApiParamType.LONG, desc = "基础属性id") })
	@Description(desc = "保存基础属性接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		PropVo propVo = JSONObject.toJavaObject(jsonObj, PropVo.class);
		if (propMapper.checkPropNameIsExists(propVo) > 0) {
			throw new PropNameIsExistsException(propVo.getName());
		}
		if (propMapper.checkPropLabelIsExists(propVo) > 0) {
			throw new PropLabelIsExistsException(propVo.getLabel());
		}
		if (jsonObj.getLong("id") == null) {
			propMapper.insertProp(propVo);
		} else {
			propMapper.updateProp(propVo);
		}
		return propVo.getId();
	}

}
