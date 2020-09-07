package codedriver.module.cmdb.api.cientity;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.cmdb.constvalue.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dto.ci.CiViewVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.service.ci.CiAuthService;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiAuthService ciAuthService;

    @Autowired
    private CiViewMapper ciViewMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/search";
    }

    @Override
    public String getName() {
        return "查询配置项";
    }

    @Override
    public String getConfig() {
        // TODO Auto-generated method stub
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id")})
    @Output({@Param(explode = BasePageVo.class),
        @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
        @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "查询配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityVo ciEntityVo = JSONObject.toJavaObject(jsonObj, CiEntityVo.class);

        // 获取视图配置，只返回需要的属性和关系
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciEntityVo.getCiId());
        ciViewVo.addShowType(ShowType.LIST.getValue());
        ciViewVo.addShowType(ShowType.ALL.getValue());
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        List<Long> attrIdList = null, relIdList = null;
        JSONArray theadList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            attrIdList = new ArrayList<>();
            relIdList = new ArrayList<>();
            for (CiViewVo ciview : ciViewList) {
                JSONObject headObj = new JSONObject();
                headObj.put("title", ciview.getItemName());
                if (ciview.getType().equals("attr")) {
                    attrIdList.add(ciview.getItemId());
                    headObj.put("key", "attr_" + ciview.getItemId());
                } else if (ciview.getType().equals("relfrom")) {
                    relIdList.add(ciview.getItemId());
                    headObj.put("key", "relfrom_" + ciview.getItemId());
                } else {
                    relIdList.add(ciview.getItemId());
                    headObj.put("key", "relto_" + ciview.getItemId());
                }
                theadList.add(headObj);
            }
        }

        ciEntityVo.setAttrIdList(attrIdList);
        ciEntityVo.setRelIdList(relIdList);
        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);

        JSONObject returnObj = new JSONObject();
        returnObj.put("pageSize", ciEntityVo.getPageSize());
        returnObj.put("pageCount", ciEntityVo.getPageCount());
        returnObj.put("rowNum", ciEntityVo.getRowNum());
        returnObj.put("currentPage", ciEntityVo.getCurrentPage());
        returnObj.put("tbodyList", ciEntityList);
        returnObj.put("theadList", theadList);
        return returnObj;
    }

}
