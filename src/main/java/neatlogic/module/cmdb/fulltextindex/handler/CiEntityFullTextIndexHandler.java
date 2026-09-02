/*
 *
 * Copyright (C) 2025  TechSure Co., Ltd.  All Rights Reserved.
 * This file is part of the NeatLogic software.
 * Licensed under the NeatLogic Sustainable Use License (NSUL), Version 4.x – 2025.
 * You may use this file only in compliance with the License.
 * See the LICENSE file distributed with this work for the full license text.
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 */

package neatlogic.module.cmdb.fulltextindex.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
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
import neatlogic.framework.fulltextindex.utils.FullTextIndexUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.fulltextindex.enums.CmdbFullTextIndexType;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

@Service
public class CiEntityFullTextIndexHandler extends FullTextIndexHandlerBase {
    private static final Semaphore semaphore = new Semaphore(5);


    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiEntityMapper ciEntityMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private AttrEntityMapper attrEntityMapper;

    @Override
    protected String getModuleId() {
        return "cmdb";
    }

    /**
     * 初始化专有名词入字典
     */
    @Override
    protected <T> void myInitialTerms(T attrVo) {
        List<AttrVo> attrList = new ArrayList<>();
        if (attrVo == null) {
            AttrVo pAttrVo = new AttrVo();
            pAttrVo.setIsTerm(1);
            attrList = attrMapper.searchAttr(pAttrVo);
        } else if (attrVo instanceof AttrVo) {
            AttrVo pattrVo = (AttrVo) attrVo;
            if (pattrVo.getCiId() != null) {
                attrList.add(pattrVo);
            }
        }
        Set<Long> ciIdSet = new HashSet<>();
        if (CollectionUtils.isNotEmpty(attrList)) {
            for (AttrVo attr : attrList) {
                if (attr.getTargetCiId() == null && attr.getNeedCiEntityColumn()) {
                    List<String> wordList = attrEntityMapper.getAttrValueByCiId(attr);
                    FullTextIndexUtil.addWord(wordList);
                } else if (attr.getTargetCiId() != null) {
                    if (!ciIdSet.contains(attr.getTargetCiId())) {
                        List<String> wordList = ciEntityMapper.getCiEntityNameByCiId(attr.getTargetCiId());
                        FullTextIndexUtil.addWord(wordList);
                        ciIdSet.add(attr.getTargetCiId());
                    }
                }
            }
        }
    }


    /**
     * 添加专有名词入字典
     */
    @Override
    public void addTerms(String... term) {
        FullTextIndexUtil.addWord(term);
    }

    @Override
    protected void myCreateIndex(FullTextIndexVo fullTextIndexVo) {
        Long ciEntityId = fullTextIndexVo.getTargetId();
        CiEntityVo baseCiEntityVo = ciEntityService.getCiEntityBaseInfoById(ciEntityId);
        if (baseCiEntityVo != null) {
            CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(baseCiEntityVo.getCiId(), ciEntityId, 100, 100);

            if (ciEntityVo == null) {
                return;
            }
            List<AttrEntityVo> attrEntityList = ciEntityVo.getAttrEntityList();
            List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityVo.getCiId());
            if (CollectionUtils.isNotEmpty(attrEntityList)) {
                for (AttrEntityVo attrEntityVo : attrEntityList) {
                    /*
                      2025-2-21前的策略
                      由于expression属性的计算是异步进行的，
                      在处理全文检索索引的时候，
                      表达式字段可能还没计算完毕，
                      这会导致索引数据错误获取到修改前的记录，
                      导致搜索结果异常，所以先排除掉expression属性的数据
                     */
                    /*
                      2025-2-21后的策略
                      由于增加了线程锁，创建索引的线程会等待表达式字段重建完毕才会进行，因此不需要再限制表达式属性
                     */
                    if (/*!attrEntityVo.getAttrType().equalsIgnoreCase("expression") &&*/ (CollectionUtils.isNotEmpty(attrEntityVo.getValueList()))) {
                        AttrVo termAttrVo = null;
                        Optional<AttrVo> attrOp = attrList.stream().filter(d -> Objects.equals(d.getIsTerm(), 1) && d.getId().equals(attrEntityVo.getAttrId())).findFirst();
                        if (attrOp.isPresent()) {
                            termAttrVo = attrOp.get();
                        }
                        if (attrEntityVo.getToCiId() != null) {
                            List<Long> ciEntityIdList = new ArrayList<>();
                            for (int i = 0; i < attrEntityVo.getValueList().size(); i++) {
                                ciEntityIdList.add(attrEntityVo.getValueList().getLong(i));
                            }
                            List<CiEntityVo> targetCiEntityList = ciEntityService.getCiEntityNameByIdList(attrEntityVo.getToCiId(), ciEntityIdList);
                            if (CollectionUtils.isNotEmpty(targetCiEntityList)) {
                                String v = "";
                                for (CiEntityVo targetCiEntityVo : targetCiEntityList) {
                                    if (StringUtils.isNotBlank(targetCiEntityVo.getName())) {
                                        if (StringUtils.isNotBlank(v)) {
                                            v += ",";
                                        }
                                        v = v + targetCiEntityVo.getName();
                                    }
                                }
                                if (StringUtils.isNotBlank(v)) {
                                    fullTextIndexVo.addFieldContent(attrEntityVo.getAttrId().toString(), new FullTextIndexVo.WordVo(v));
                                    if (termAttrVo != null) {
                                        this.addTerms(v.split(","));
                                    }
                                }
                            }
                        } else {
                            String word = attrEntityVo.getActualValueList().stream().map(Object::toString).collect(Collectors.joining(","));
                            fullTextIndexVo.addFieldContent(attrEntityVo.getAttrId().toString(), new FullTextIndexVo.WordVo(word));
                            if (termAttrVo != null) {
                                this.addTerms(word.split(","));
                            }
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
        CiEntityFullTextIndexHandler handler = this;
        fullTextIndexTypeVo.setPageSize(500);
        fullTextIndexTypeVo.setCurrentPage(1);
        //为了增量重建索引时，能实现补充缺少属性的效果，因此不管全量重建还是增量重建，都需要遍历所有配置项
        List<Long> ciEntityIdList = ciEntityMapper.searchCiEntityIdForFulltextIndex(fullTextIndexTypeVo);
        //List<Long> ciEntityIdList = ciEntityMapper.getNotIndexCiEntityIdList(fullTextIndexTypeVo);
        while (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            for (Long ciEntityId : ciEntityIdList) {
                try {
                    semaphore.acquire();
                    CachedThreadPool.execute(new NeatLogicThread("FULLTEXTINDEX-REBUILD-CIENTITY-" + ciEntityId) {
                        @Override
                        protected void execute() {
                            try {
                                handler.createIndex(ciEntityId, true);
                            } finally {
                                semaphore.release();
                            }
                        }
                    });
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            //ciEntityIdList = ciEntityMapper.getNotIndexCiEntityIdList(fullTextIndexTypeVo);
            fullTextIndexTypeVo.setCurrentPage(fullTextIndexTypeVo.getCurrentPage() + 1);
            ciEntityIdList = ciEntityMapper.searchCiEntityIdForFulltextIndex(fullTextIndexTypeVo);
        }
    }

}
