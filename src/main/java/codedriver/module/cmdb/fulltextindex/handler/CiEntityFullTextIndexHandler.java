/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.fulltextindex.handler;

import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerBase;
import codedriver.framework.fulltextindex.core.IFullTextIndexType;
import codedriver.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import codedriver.framework.fulltextindex.dto.globalsearch.DocumentVo;
import codedriver.module.cmdb.fulltextindex.enums.CmdbFullTextIndexType;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.nacos.common.utils.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CiEntityFullTextIndexHandler extends FullTextIndexHandlerBase {

    @Resource
    private CiEntityService ciEntityService;

    @Override
    protected String getModuleId() {
        return "cmdb";
    }

    @Override
    protected void myCreateIndex(FullTextIndexVo fullTextIndexVo) {
        Long ciEntityId = fullTextIndexVo.getTargetId();
        CiEntityVo baseCiEntityVo = ciEntityService.getCiEntityBaseInfoById(ciEntityId);
        if (baseCiEntityVo != null) {
            CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(baseCiEntityVo.getCiId(), ciEntityId);
            List<AttrEntityVo> attrEntityList = ciEntityVo.getAttrEntityList();
            if (CollectionUtils.isNotEmpty(attrEntityList)) {
                for (AttrEntityVo attrEntityVo : attrEntityList) {
                    if (CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                        if (attrEntityVo.getToCiId() != null) {
                            List<Long> ciEntityIdList = new ArrayList<>();
                            for (int i = 0; i < attrEntityVo.getValueList().size(); i++) {
                                ciEntityIdList.add(attrEntityVo.getValueList().getLong(i));
                            }
                            List<CiEntityVo> targetCiEntityList = ciEntityService.getCiEntityByIdList(attrEntityVo.getToCiId(), ciEntityIdList);
                            if (CollectionUtils.isNotEmpty(targetCiEntityList)) {
                                fullTextIndexVo.addFieldContent(attrEntityVo.getAttrId().toString(), new FullTextIndexVo.WordVo(targetCiEntityList.stream().map(CiEntityVo::getName).collect(Collectors.joining(","))));
                            }
                        } else {
                            fullTextIndexVo.addFieldContent(attrEntityVo.getAttrId().toString(), new FullTextIndexVo.WordVo(attrEntityVo.getValueList().stream().map(Object::toString).collect(Collectors.joining(","))));
                        }
                    }
                }
            }

            List<RelEntityVo> relEntityList = ciEntityVo.getRelEntityList();
            Map<Long, List<String>> relNameMap = new HashMap<>();
            if (CollectionUtils.isNotEmpty(relEntityList)) {
                for (RelEntityVo relEntityVo : relEntityList) {
                    if (!relNameMap.containsKey(relEntityVo.getRelId())) {
                        relNameMap.put(relEntityVo.getRelId(), new ArrayList<>());
                    }
                    if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                        relNameMap.get(relEntityVo.getRelId()).add(relEntityVo.getToCiEntityName());
                    } else if (relEntityVo.getDirection().equals(RelDirectionType.TO.getValue())) {
                        relNameMap.get(relEntityVo.getRelId()).add(relEntityVo.getFromCiEntityName());
                    }
                }
            }
            for (Long key : relNameMap.keySet()) {
                fullTextIndexVo.addFieldContent(key.toString(), new FullTextIndexVo.WordVo(String.join(",", relNameMap.get(key))));
            }
        }
    }

    @Override
    protected void myMakeupDocument(DocumentVo documentVo) {
        Long ciEntityId = documentVo.getTargetId();
        CiEntityVo baseCiEntityVo = ciEntityService.getCiEntityBaseInfoById(ciEntityId);
        if (baseCiEntityVo != null) {
            CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(baseCiEntityVo.getCiId(), baseCiEntityVo.getId());
            documentVo.setTitle(ciEntityVo.getName());
            StringBuilder content = new StringBuilder();
            if (ciEntityVo.getAttrEntityList() != null && ciEntityVo.getAttrEntityList().size() > 0) {
                for (AttrEntityVo attr : ciEntityVo.getAttrEntityList()) {
                    if (CollectionUtils.isNotEmpty(attr.getValueList())) {
                        //IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attr.getAttrType());
                        //handler.transferValueListToDisplay(attr.getValueList());
                        content.append("<span class=\"ml-xs\" style=\"font-weight:bold\">").append(attr.getAttrLabel()).append("：</span>");
                        content.append("<span>");
                        if (CollectionUtils.isNotEmpty(attr.getActualValueList())) {
                            for (int i = 0; i < attr.getActualValueList().size(); i++) {
                                content.append(attr.getActualValueList().getString(i)).append(" ");
                            }
                        } else {
                            content.append("-");
                        }
                        content.append("</span>");
                    }
                }
            }
            if (MapUtils.isNotEmpty(ciEntityVo.getRelEntityData())) {
                for (String key : ciEntityVo.getRelEntityData().keySet()) {
                    JSONObject relObj = ciEntityVo.getRelEntityData().getJSONObject(key);
                    if (CollectionUtils.isNotEmpty(relObj.getJSONArray("valueList"))) {
                        content.append("<span class=\"ml-xs\" style=\"font-weight:bold\">").append(relObj.getString("label")).append("：</span>");
                        content.append("<span style=\"margin-right:5px\">");
                        for (int i = 0; i < relObj.getJSONArray("valueList").size(); i++) {
                            content.append(relObj.getJSONArray("valueList").getJSONObject(i).getString("ciEntityName")).append(" ");
                        }
                        content.append("</span>");
                    }
                }
            }
            documentVo.setTargetUrl("cmdb.html#/ci/" + ciEntityVo.getCiId() + "/cientity-view/" + ciEntityVo.getId());
            documentVo.setContent(content.toString());
        }
    }

    @Override
    public IFullTextIndexType getType() {
        return CmdbFullTextIndexType.CIENTITY;
    }

    @Override
    public void rebuildIndex(Boolean isRebuildAll) {

    }
}
