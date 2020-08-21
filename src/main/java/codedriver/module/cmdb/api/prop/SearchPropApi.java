package codedriver.module.cmdb.api.prop;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.common.util.PageUtil;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.ApiComponentBase;
import codedriver.module.cmdb.dao.mapper.prop.PropMapper;
import codedriver.module.cmdb.dto.prop.PropVo;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchPropApi extends ApiComponentBase {

	@Autowired
	private PropMapper propMapper;

	@Override
	public String getToken() {
		return "/cmdb/prop/search";
	}

	@Override
	public String getName() {
		return "查询基础属性";
	}

	@Override
	public String getConfig() {
		return null;
	}

	@Input({ @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字", xss = true), @Param(name = "typeId", type = ApiParamType.LONG, desc = "类型id"), @Param(name = "handler", type = ApiParamType.STRING, desc = "控件") })
	@Output({ @Param(explode = BasePageVo.class), @Param(name = "tbodyList", explode = PropVo[].class) })
	@Description(desc = "查询基础属性接口")
	@Override
	public Object myDoService(JSONObject jsonObj) throws Exception {
		PropVo propVo = JSONObject.toJavaObject(jsonObj, PropVo.class);
		JSONObject returnObj = new JSONObject();
		List<PropVo> propList = propMapper.searchProp(propVo);
		returnObj.put("tbodyList", propList);
		if (propVo.getNeedPage() && CollectionUtils.isNotEmpty(propList)) {
			int rowNum = propMapper.searchPropCount(propVo);
			returnObj.put("currentPage", propVo.getCurrentPage());
			returnObj.put("pageSize", propVo.getPageSize());
			returnObj.put("rowNum", rowNum);
			returnObj.put("pageCount", PageUtil.getPageCount(rowNum, propVo.getPageSize()));
		}
		return returnObj;
	}

}
