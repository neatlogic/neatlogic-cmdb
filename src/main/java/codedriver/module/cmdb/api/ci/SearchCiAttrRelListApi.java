/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.ci;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.utils.RelUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiAttrRelListApi extends PrivateApiComponentBase {


    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/ci/attrrel/search";
    }

    @Override
    public String getName() {
        return "搜索模型属性和关系信息";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"), @Param(name = "keyword", type = ApiParamType.STRING, desc = "关键字，只匹配属性和关系的唯一标识")})
    @Output({@Param(type = ApiParamType.JSONARRAY)})
    @Description(desc = "搜索模型属性和关系信息接口，此接口主要用在DSL输入控件搜索属性和关系")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String keyword = jsonObj.getString("keyword");
        JSONArray returnList = new JSONArray();
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciId));
        if (StringUtils.isNotBlank(keyword)) {
            keyword = keyword.toLowerCase(Locale.ROOT);
            //因为有二级缓存，数据量也不大，可以直接在应用层过滤关键字实现搜索效果
            String finalKeyword = keyword;
            attrList = attrList.stream().filter(d -> d.getName().toLowerCase(Locale.ROOT).contains(finalKeyword)).collect(Collectors.toList());
            relList = relList.stream().filter(d -> (d.getDirection().equals(RelDirectionType.FROM.getValue()) && d.getToName().toLowerCase(Locale.ROOT).contains(finalKeyword)) || (d.getDirection().equals(RelDirectionType.TO.getValue()) && d.getFromName().toLowerCase(Locale.ROOT).contains(finalKeyword))).collect(Collectors.toList());
        }
        for (AttrVo attrVo : attrList) {
            JSONObject attrObj = new JSONObject();
            attrObj.put("id", attrVo.getId());
            attrObj.put("uid", "attr_" + attrVo.getId());
            attrObj.put("name", attrVo.getName());
            attrObj.put("label", attrVo.getLabel());
            attrObj.put("targetCiId", attrVo.getTargetCiId());
            returnList.add(attrObj);
        }
        for (RelVo relVo : relList) {
            JSONObject relObj = new JSONObject();
            relObj.put("id", relVo.getId());
            if (relVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                relObj.put("uid", "relfrom_" + relVo.getId());
                relObj.put("name", relVo.getToName());
                relObj.put("label", relVo.getToLabel());
                relObj.put("targetCiId", relVo.getToCiId());
            } else if (relVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                relObj.put("uid", "relto_" + relVo.getId());
                relObj.put("name", relVo.getFromName());
                relObj.put("label", relVo.getFromLabel());
                relObj.put("targetCiId", relVo.getFromCiId());
            }
            returnList.add(relObj);
        }
        return returnList;
    }
}
