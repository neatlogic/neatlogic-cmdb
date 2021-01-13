package codedriver.module.cmdb.api.prop;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.constvalue.PropHandlerType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.core.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;

@Deprecated
@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListPropHandlerTypeApi extends PrivateApiComponentBase {


	@Override
	public String getToken() {
		return "/cmdb/prop/handlertype/list";
	}

	@Override
	public String getName() {
		return "获取基础属性控件列表";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Output({ @Param(explode = ValueTextVo[].class) })
	@Description(desc = "获取基础属性控件列表接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		List<ValueTextVo> returnList = new ArrayList<>();
		for (PropHandlerType type : PropHandlerType.values()) {
			ValueTextVo valueTextVo = new ValueTextVo();
			valueTextVo.setValue(type.getValue());
			valueTextVo.setText(type.getText());
			returnList.add(valueTextVo);
		}
		return returnList;
	}

}
