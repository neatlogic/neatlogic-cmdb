/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.fulltextindex.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.fulltextindex.core.FullTextIndexHandlerBase;
import neatlogic.framework.fulltextindex.core.IFullTextIndexType;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexTypeVo;
import neatlogic.framework.fulltextindex.dto.fulltextindex.FullTextIndexVo;
import neatlogic.framework.fulltextindex.dto.globalsearch.DocumentVo;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.fulltextindex.enums.CmdbFullTextIndexType;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
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
                    /*
                      由于expression属性的计算是异步进行的，
                      在处理全文检索索引的时候，
                      表达式字段可能还没计算完毕，
                      这会导致索引数据错误获取到修改前的记录，
                      导致搜索结果异常，所以先排除掉expression属性的数据
                     */
                    if (!attrEntityVo.getAttrType().equalsIgnoreCase("expression") && (CollectionUtils.isNotEmpty(attrEntityVo.getValueList()))) {
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
                                String word = attrEntityVo.getValueList().stream().map(Object::toString).collect(Collectors.joining(","));
                                fullTextIndexVo.addFieldContent(attrEntityVo.getAttrId().toString(), new FullTextIndexVo.WordVo(word));
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
            relNameMap.keySet().forEach(key -> fullTextIndexVo.addFieldContent(key.toString(), new FullTextIndexVo.WordVo(String.join(",", relNameMap.get(key)))));
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
            if (ciEntityVo.getAttrEntityList() != null && !ciEntityVo.getAttrEntityList().isEmpty()) {
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
