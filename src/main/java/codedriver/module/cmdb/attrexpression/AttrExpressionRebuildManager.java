/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.attrexpression;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.cmdb.dto.attrexpression.AttrExpressionRelVo;
import codedriver.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrExpressionRebuildAuditMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

@Service
public class AttrExpressionRebuildManager {
    private final static String EXPRESSION_TYPE = "expression";
    private final static Logger logger = LoggerFactory.getLogger(AttrExpressionRebuildManager.class);
    private final static BlockingQueue<RebuildAuditVo> rebuildQueue = new LinkedBlockingQueue<>();
    private static RelEntityMapper relEntityMapper;
    private static AttrExpressionRebuildAuditMapper attrExpressionRebuildAuditMapper;
    private static AttrMapper attrMapper;
    private static CiEntityService ciEntityService;

    @Autowired
    public AttrExpressionRebuildManager(RelEntityMapper _relEntityMapper, AttrMapper _attrMapper, CiEntityService _ciEntityService, AttrExpressionRebuildAuditMapper _attrExpressionRebuildAuditMapper) {
        relEntityMapper = _relEntityMapper;
        attrMapper = _attrMapper;
        ciEntityService = _ciEntityService;
        attrExpressionRebuildAuditMapper = _attrExpressionRebuildAuditMapper;
    }

    @PostConstruct
    public void init() {
        Thread t = new Thread(new CodeDriverThread("ATTR-EXPRESSION-REBUILD-MANAGER") {
            @Override
            protected void execute() {
                RebuildAuditVo rebuildAuditVo = null;
                while (true) {
                    try {
                        rebuildAuditVo = rebuildQueue.take();
                        CachedThreadPool.execute(new Builder(rebuildAuditVo));
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                        break;
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    static class Builder extends CodeDriverThread {
        private final RebuildAuditVo rebuildAuditVo;

        public Builder(RebuildAuditVo _rebuildAuditVo) {
            super(_rebuildAuditVo.getUserContext(), _rebuildAuditVo.getTenantContext());
            rebuildAuditVo = _rebuildAuditVo;
        }

        @Override
        protected void execute() {
            //有配置项id的说明需要更新关联配置项的属性
            if (rebuildAuditVo.getCiId() != null && rebuildAuditVo.getCiEntityId() != null && StringUtils.isNotBlank(rebuildAuditVo.getAttrIds())) {
                //根据修改的配置项找到会影响的模型属性列表
                List<AttrExpressionRelVo> expressionAttrList = attrMapper.getExpressionAttrByValueAttrIdList(rebuildAuditVo.getCiId(), Arrays.stream(rebuildAuditVo.getAttrIds().split(",")).map(Long::parseLong).collect(Collectors.toList()));
                if (CollectionUtils.isNotEmpty(expressionAttrList)) {
                    //查找当前配置项所关联的配置项，看是否在受影响模型列表里
                    List<RelEntityVo> relEntityList = relEntityMapper.getRelEntityByCiEntityId(rebuildAuditVo.getCiEntityId());
                    if (CollectionUtils.isNotEmpty(relEntityList)) {
                        List<CiEntityVo> changeCiEntityList = new ArrayList<>();
                        for (RelEntityVo relEntityVo : relEntityList) {
                            if (relEntityVo.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                if (expressionAttrList.stream().anyMatch(a -> a.getExpressionCiId().equals(relEntityVo.getToCiId()))) {
                                    CiEntityVo ciEntityVo = new CiEntityVo();
                                    ciEntityVo.setId(relEntityVo.getToCiEntityId());
                                    ciEntityVo.setCiId(relEntityVo.getToCiId());
                                    changeCiEntityList.add(ciEntityVo);
                                }
                            } else {
                                if (expressionAttrList.stream().anyMatch(a -> a.getExpressionCiId().equals(relEntityVo.getFromCiId()))) {
                                    CiEntityVo ciEntityVo = new CiEntityVo();
                                    ciEntityVo.setId(relEntityVo.getFromCiEntityId());
                                    ciEntityVo.setCiId(relEntityVo.getFromCiId());
                                    changeCiEntityList.add(ciEntityVo);
                                }
                            }
                        }

                        if (CollectionUtils.isNotEmpty(changeCiEntityList)) {
                            for (CiEntityVo ciEntityVo : changeCiEntityList) {
                                updateExpressionAttr(ciEntityVo);
                            }
                        }
                    }
                }
            } else if (rebuildAuditVo.getCiId() != null && rebuildAuditVo.getCiEntityId() == null && StringUtils.isNotBlank(rebuildAuditVo.getAttrIds())) {
                //没有配置项信息的则是因为表达式发生了修改，所以需要更新模型下所有配置项信息
                CiEntityVo ciEntityVo = new CiEntityVo();
                ciEntityVo.setCiId(rebuildAuditVo.getCiId());
                ciEntityVo.setPageSize(100);
                ciEntityVo.setCurrentPage(1);
                ciEntityVo.setCiEntityIdStart(rebuildAuditVo.getCiEntityIdStart());
                List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntityBaseInfo(ciEntityVo);
                while (CollectionUtils.isNotEmpty(ciEntityList)) {
                    for (CiEntityVo ciEntity : ciEntityList) {
                        updateExpressionAttr(ciEntity, Arrays.stream(rebuildAuditVo.getAttrIds().split(",")).map(Long::parseLong).collect(Collectors.toList()));
                        //更新修改进度
                        rebuildAuditVo.setCiEntityIdStart(ciEntity.getId());
                        attrExpressionRebuildAuditMapper.updateAttrExpressionRebuildAuditCiEntityIdStartById(rebuildAuditVo);
                    }
                    ciEntityVo.setCurrentPage(ciEntityVo.getCurrentPage() + 1);
                    ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
                }
            }
            //清除日志
            attrExpressionRebuildAuditMapper.deleteAttrExpressionRebuildAuditById(rebuildAuditVo.getId());
        }

        /**
         * 更新配置项指定的表达式属性
         *
         * @param ciEntityVo 配置项信息
         * @param attrIdList 需要更新的属性列表
         */
        private void updateExpressionAttr(CiEntityVo ciEntityVo, List<Long> attrIdList) {
            List<AttrVo> expressionList = attrMapper.getAttrByCiId(ciEntityVo.getCiId());
            List<AttrVo> expressionAttrList = expressionList.stream().filter(attr -> attr.getType().equals(EXPRESSION_TYPE)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(attrIdList)) {
                expressionAttrList.removeIf(attrVo -> attrIdList.stream().noneMatch(aid -> aid.equals(attrVo.getId())));
            }
            if (CollectionUtils.isNotEmpty(expressionAttrList)) {
                CiEntityVo newCiEntityVo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), ciEntityVo.getId());
                CiEntityVo saveCiEntityVo = new CiEntityVo();
                saveCiEntityVo.setCiId(newCiEntityVo.getCiId());
                saveCiEntityVo.setId(newCiEntityVo.getId());
                for (AttrVo attrVo : expressionAttrList) {
                    StringBuilder expressionValue = new StringBuilder();
                    if (attrVo.getConfig().containsKey("expression") && attrVo.getConfig().get("expression") instanceof JSONArray) {
                        for (int i = 0; i < attrVo.getConfig().getJSONArray("expression").size(); i++) {
                            String expression = attrVo.getConfig().getJSONArray("expression").getString(i);
                            if (expression.startsWith("{") && expression.endsWith("}")) {
                                expression = expression.substring(1, expression.length() - 1);
                                if (expression.contains(".")) {
                                    //关系属性
                                    Long relId = Long.parseLong(expression.split("\\.")[0]);
                                    Long attrId = Long.parseLong(expression.split("\\.")[1]);
                                    List<RelEntityVo> relEntityList = newCiEntityVo.getRelEntityByRelId(relId);
                                    if (CollectionUtils.isNotEmpty(relEntityList)) {
                                        RelEntityVo rel = relEntityList.get(0);
                                        CiEntityVo relCiEntityVo;
                                        if (rel.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                            relCiEntityVo = ciEntityService.getCiEntityById(rel.getToCiId(), rel.getToCiEntityId());
                                        } else {
                                            relCiEntityVo = ciEntityService.getCiEntityById(rel.getFromCiId(), rel.getFromCiEntityId());
                                        }
                                        if (relCiEntityVo != null) {
                                            AttrEntityVo attrEntityVo = relCiEntityVo.getAttrEntityByAttrId(attrId);
                                            if (attrEntityVo != null) {
                                                expressionValue.append(attrEntityVo.getActualValueList().stream().map(Object::toString).collect(Collectors.joining(",")));
                                            }
                                        }
                                    }
                                } else {
                                    //自己的属性
                                    Long attrId = Long.parseLong(expression);
                                    AttrEntityVo attrEntityVo = newCiEntityVo.getAttrEntityByAttrId(attrId);
                                    if (attrEntityVo != null) {
                                        expressionValue.append(attrEntityVo.getActualValueList().stream().map(Object::toString).collect(Collectors.joining(",")));
                                    }
                                }
                            } else {
                                expressionValue.append(expression);
                            }
                        }
                        JSONObject attrEntityObj = new JSONObject();
                        attrEntityObj.put("ciId", attrVo.getCiId());
                        attrEntityObj.put("type", attrVo.getType());
                        attrEntityObj.put("valueList", new JSONArray() {{
                            this.add(expressionValue);
                        }});
                        saveCiEntityVo.addAttrEntityData(attrVo.getId(), attrEntityObj);
                    }
                }
                ciEntityService.updateCiEntity(saveCiEntityVo);
            }
        }

        /**
         * 更新配置项所有表达式属性
         *
         * @param ciEntityVo 配置项信息
         */
        private void updateExpressionAttr(CiEntityVo ciEntityVo) {
            updateExpressionAttr(ciEntityVo, null);
        }
    }

    public static void rebuild(RebuildAuditVo rebuildAuditVo) {
        rebuildQueue.offer(rebuildAuditVo);
    }
}
