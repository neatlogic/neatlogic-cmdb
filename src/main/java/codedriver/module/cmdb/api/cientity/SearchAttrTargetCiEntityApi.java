/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.exception.attr.AttrNotFoundException;
import codedriver.framework.cmdb.exception.attr.AttrTargetCiIdNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchAttrTargetCiEntityApi extends PrivateApiComponentBase {


    @Resource
    private AttrMapper attrMapper;

    @Resource
    private CiEntityService ciEntityService;

    @Override
    public String getToken() {
        return "/cmdb/attr/targetci/search";
    }

    @Override
    public String getName() {
        return "查询属性目标配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "attrId", type = ApiParamType.LONG, isRequired = true, desc = "属性id"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键字"),
            @Param(name = "valueList", type = ApiParamType.JSONARRAY, desc = "选中值列表")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "查询属性目标配置项")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long attrId = jsonObj.getLong("attrId");
        String keyword = jsonObj.getString("keyword");
        JSONArray valueList = jsonObj.getJSONArray("valueList");
        AttrVo attrVo = attrMapper.getAttrById(attrId);
        if (attrVo == null) {
            throw new AttrNotFoundException(attrId);
        }

        if (attrVo.getTargetCiId() == null) {
            throw new AttrTargetCiIdNotFoundException(attrVo.getLabel());
        }

        Long textKey = null;
        if (MapUtils.isNotEmpty(attrVo.getConfig())) {
            textKey = attrVo.getConfig().getLong("textKey");
        }

        CiEntityVo ciEntityVo = new CiEntityVo();
        ciEntityVo.setCiId(attrVo.getTargetCiId());
        if (CollectionUtils.isNotEmpty(valueList)) {
            List<Long> idList = new ArrayList<>();
            for (int i = 0; i < valueList.size(); i++) {
                try {
                    idList.add(valueList.getLong(i));
                } catch (Exception ignored) {

                }
            }
            if (CollectionUtils.isNotEmpty(idList)) {
                ciEntityVo.setIdList(idList);
            }
        }
        if (StringUtils.isNotBlank(keyword)) {
            if (textKey != null) {
                AttrFilterVo attrFilterVo = new AttrFilterVo();
                attrFilterVo.setAttrId(textKey);
                attrFilterVo.setValueList(new ArrayList<String>() {{
                    this.add(keyword);
                }});
                attrFilterVo.setExpression(SearchExpression.LI.getExpression());
                ciEntityVo.addAttrFilter(attrFilterVo);
            } else {
                ciEntityVo.setKeyword(keyword);
            }
        }
        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        JSONArray jsonList = new JSONArray();
        for (CiEntityVo ciEntity : ciEntityList) {
            JSONObject obj = new JSONObject();
            obj.put("id", ciEntity.getId());
            if (textKey != null) {
                obj.put("name", ciEntity.getAttrEntityValueByAttrId(textKey));
            } else {
                obj.put("name", StringUtils.isNotBlank(ciEntity.getName()) ? ciEntity.getName() : "-");
            }
            jsonList.add(obj);
        }
        return jsonList;
    }

}
