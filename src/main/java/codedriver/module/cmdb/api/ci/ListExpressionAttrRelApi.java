/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.ValueTextVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ListExpressionAttrRelApi extends PrivateApiComponentBase {

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/{ciId}/listexpressionattrrel";
    }

    @Override
    public String getName() {
        return "返回模型表达式属性和关系列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id", isRequired = true)})
    @Output({@Param(explode = ValueTextVo[].class)})
    @Description(desc = "返回模型表达式属性和关系列表接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        JSONArray jsonList = new JSONArray();
        for (AttrVo attrVo : attrList) {
            if (!attrVo.getType().equals("expression")) {
                JSONObject valueObj = new JSONObject();
                valueObj.put("value", "{" + attrVo.getId() + "}");
                valueObj.put("text", attrVo.getLabel());
                jsonList.add(valueObj);
            }
        }

        List<RelVo> relList = relMapper.getRelByCiId(ciId);
        if (CollectionUtils.isNotEmpty(relList)) {
            for (RelVo relVo : relList) {
                List<AttrVo> relAttrList;
                String relName;
                if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                    relAttrList = attrMapper.getAttrByCiId(relVo.getToCiId());
                    relName = relVo.getToLabel();
                } else {
                    relAttrList = attrMapper.getAttrByCiId(relVo.getFromCiId());
                    relName = relVo.getFromLabel();
                }
                if (CollectionUtils.isNotEmpty(relAttrList)) {
                    for (AttrVo attrVo : relAttrList) {
                        if (!attrVo.getType().equals("expression")) {
                            JSONObject valueObj = new JSONObject();
                            valueObj.put("value", "{" + relVo.getId() + "." + attrVo.getId() + "}");
                            valueObj.put("text", relName + "->" + attrVo.getLabel());
                            jsonList.add(valueObj);
                        }
                    }
                }
            }

           /* String[] signList = new String[]{":", "-", "_", "(", ")", "[", "]"};
            for (String sign : signList) {
                JSONObject valueObj = new JSONObject();
                valueObj.put("value", sign);
                valueObj.put("text", "分隔符\"" + sign + "\"");
                jsonList.add(valueObj);
            }*/
        }

        return jsonList;
    }
}
