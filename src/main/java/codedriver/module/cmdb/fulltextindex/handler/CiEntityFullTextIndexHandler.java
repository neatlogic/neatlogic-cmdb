/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.fulltextindex.handler;

import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.fulltextindex.core.FullTextIndexHandlerBase;
import codedriver.framework.fulltextindex.core.IFullTextIndexType;
import codedriver.framework.fulltextindex.dto.fulltextindex.FullTextIndexTypeVo;
import codedriver.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import codedriver.framework.fulltextindex.dto.globalsearch.DocumentVo;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.fulltextindex.enums.CmdbFullTextIndexType;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
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

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private AttrMapper attrMapper;

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
            List<AttrVo> attrList = attrMapper.getAttrByCiId(baseCiEntityVo.getCiId());
            Map<Long, AttrVo> attrMap = new HashMap<>();
            for (AttrVo attrVo : attrList) {
                attrMap.put(attrVo.getId(), attrVo);
            }
            CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(baseCiEntityVo.getCiId(), baseCiEntityVo.getId());
            documentVo.setTitle(ciEntityVo.getName());
            StringBuilder content = new StringBuilder();
            if (ciEntityVo.getAttrEntityList() != null && ciEntityVo.getAttrEntityList().size() > 0) {
                for (AttrEntityVo attr : ciEntityVo.getAttrEntityList()) {
                    if (CollectionUtils.isNotEmpty(attr.getValueList()) && attrMap.containsKey(attr.getAttrId())) {
                        IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attr.getAttrType());
                            /*
                            这个场景和导出类似，所以使用transferValueListToExport
                            不能使用所以使用transferValueListToDisplay，因为对于select和table属性都是通过前端进行处理的，后台不会进行转换
                            */
                        handler.transferValueListToExport(attrMap.get(attr.getAttrId()), attr.getValueList());
                        content.append("<span style=\"font-weight:bold\">").append(attr.getAttrLabel()).append("：</span>");
                        content.append("<span class=\"mr-xs\">");
                        if (CollectionUtils.isNotEmpty(attr.getValueList())) {
                            content.append(attr.getValueList().stream().map(Object::toString).collect(Collectors.joining("、")));
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
                        content.append("<span style=\"font-weight:bold\">").append(relObj.getString("label")).append("：</span>");
                        content.append("<span class=\"mr-xs\">");
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
    public void myRebuildIndex(FullTextIndexTypeVo fullTextIndexTypeVo) {
        fullTextIndexTypeVo.setPageSize(100);
        List<Long> ciEntityIdList = ciEntityMapper.getNotIndexCiEntityIdList(fullTextIndexTypeVo);
        while (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            for (Long ciEntityId : ciEntityIdList) {
                this.createIndex(ciEntityId, true);
            }
            ciEntityIdList = ciEntityMapper.getNotIndexCiEntityIdList(fullTextIndexTypeVo);
        }
    }
}
