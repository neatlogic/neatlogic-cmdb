/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dsl.DslSearchManager;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityByDslApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private RelMapper relMapper;

    @Override
    public String getToken() {
        return "/cmdb/cientity/dsl/search";
    }

    @Override
    public String getName() {
        return "使用dsl查询配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "dsl", type = ApiParamType.STRING, isRequired = true, desc = "查询表达式，逻辑运算符支持&&、||，关系运算符支持：==、>=、<=、>、<、!=、include、exclude。如果需要搜索关系或引用属性" +
                    "字段，可以使用a.b表示，例如env.name == \"STG\" && (port == 80 || port == 443 )"),
            @Param(name = "attrList", type = ApiParamType.JSONARRAY, desc = "需要返回的属性，不定义则返回所有属性，空数组代表不返回任何属性"),
            @Param(name = "relList", type = ApiParamType.JSONARRAY, desc = "需要返回的关系，不定义则返回所有关系，空数组代表不返回任何关系")})
    @Output({@Param(explode = BasePageVo.class), @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class), @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "使用dsl查询配置项接口")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        Long ciId = paramObj.getLong("ciId");
        List<Long> ciEntityIdList = DslSearchManager.build(paramObj.getLong("ciId"), paramObj.getString("dsl")).search();
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            JSONArray pAttrList = paramObj.getJSONArray("attrList");
            List<Long> attrIdList = null;
            if (CollectionUtils.isNotEmpty(pAttrList)) {
                List<AttrVo> attrList = attrMapper.getAttrByCiId(ciId);
                attrIdList = new ArrayList<>();
                for (AttrVo attrVo : attrList) {
                    if (pAttrList.stream().allMatch(d -> d.toString().equalsIgnoreCase(attrVo.getName()) || d.toString().equalsIgnoreCase(attrVo.getLabel()))) {
                        attrIdList.add(attrVo.getId());
                    }
                }
            }
            JSONArray pRelList = paramObj.getJSONArray("relList");
            List<Long> relIdList = null;
            if (CollectionUtils.isNotEmpty(pRelList)) {
                List<RelVo> relList = relMapper.getRelByCiId(ciId);
                relIdList = new ArrayList<>();
                for (RelVo relVo : relList) {
                    if (pRelList.stream().allMatch(d ->
                            (d.toString().equalsIgnoreCase(relVo.getToName()) || d.toString().equalsIgnoreCase(relVo.getToLabel()) && relVo.getDirection().equals(RelDirectionType.FROM.getValue()))
                                    ||
                                    (d.toString().equalsIgnoreCase(relVo.getFromName()) || d.toString().equalsIgnoreCase(relVo.getFromLabel()) && relVo.getDirection().equals(RelDirectionType.TO.getValue()))
                    )) {
                        relIdList.add(relVo.getId());
                    }
                }
            }
            CiEntityVo ciEntityVo = new CiEntityVo();
            ciEntityVo.setCiId(ciId);
            ciEntityVo.setIdList(ciEntityIdList);
            ciEntityVo.setAttrIdList(attrIdList);
            ciEntityVo.setRelIdList(relIdList);
            return ciEntityService.searchCiEntity(ciEntityVo);
        }
        return null;
    }


}
