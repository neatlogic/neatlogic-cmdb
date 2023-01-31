/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package neatlogic.module.cmdb.attrexpression;

import neatlogic.framework.asynchronization.thread.CodeDriverThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.cmdb.dto.attrexpression.AttrExpressionRelVo;
import neatlogic.framework.cmdb.dto.attrexpression.RebuildAuditVo;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.framework.util.SnowflakeUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.AttrExpressionRebuildAuditMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
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
import java.util.List;
import java.util.Objects;
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
    private static CiMapper ciMapper;
    private static CiEntityService ciEntityService;

    @Autowired
    public AttrExpressionRebuildManager(RelEntityMapper _relEntityMapper, AttrMapper _attrMapper, CiMapper _ciMapper, CiEntityService _ciEntityService, AttrExpressionRebuildAuditMapper _attrExpressionRebuildAuditMapper) {
        relEntityMapper = _relEntityMapper;
        attrMapper = _attrMapper;
        ciMapper = _ciMapper;
        ciEntityService = _ciEntityService;
        attrExpressionRebuildAuditMapper = _attrExpressionRebuildAuditMapper;
    }

    @PostConstruct
    public void init() {
        Thread t = new Thread(new CodeDriverThread("ATTR-EXPRESSION-REBUILD-MANAGER") {
            @Override
            protected void execute() {
                RebuildAuditVo rebuildAuditVo;
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

    static class ExpressionGroupAttr {
        public enum Type {
            ATTR,
            CONST
        }

        public ExpressionGroupAttr(String _attr, Type _type) {
            attr = _attr;
            type = _type;
        }

        private String attr;
        private Type type;

        public String getAttr() {
            return attr;
        }

        public void setAttr(String attr) {
            this.attr = attr;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }
    }

    static class ExpressionGroup {
        public enum Type {
            REL,
            ATTR,
            CONST
        }

        private final Long key;
        private final String direction;
        private final Type type;
        private final List<ExpressionGroupAttr> attrList = new ArrayList<>();

        public Type getType() {
            return type;
        }

        public ExpressionGroup(Long key, String direction, Type type, String attr, ExpressionGroupAttr.Type attrType) {
            this.key = key;
            this.type = type;
            this.direction = direction;
            this.attrList.add(new ExpressionGroupAttr(attr, attrType));
        }

        public void addAttr(String attr, ExpressionGroupAttr.Type attrType) {
            attrList.add(new ExpressionGroupAttr(attr, attrType));
        }

        public Long getKey() {
            return key;
        }

        public String getDirection() {
            return direction;
        }

        public List<ExpressionGroupAttr> getAttrList() {
            return attrList;
        }

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
            if (rebuildAuditVo.getCiId() != null && rebuildAuditVo.getCiEntityId() != null && rebuildAuditVo.getType().equals(RebuildAuditVo.Type.INVOKED.getValue()) && CollectionUtils.isNotEmpty(rebuildAuditVo.getAttrIdList())) {
                //根据修改的配置项找到会影响的模型属性列表
                List<AttrExpressionRelVo> expressionAttrList = attrMapper.getExpressionAttrRelByValueCiIdAndAttrIdList(rebuildAuditVo.getCiId(), rebuildAuditVo.getAttrIdList());
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
            } else if (rebuildAuditVo.getCiId() != null && rebuildAuditVo.getCiEntityId() != null && rebuildAuditVo.getType().equals(RebuildAuditVo.Type.INVOKE.getValue())) {
                if (CollectionUtils.isNotEmpty(rebuildAuditVo.getAttrIdList())) {
                    //如果修改的属性中没有表达式属性，则不做任何修改
                    List<AttrExpressionRelVo> attrList = attrMapper.getExpressionAttrRelByValueCiIdAndAttrIdList(rebuildAuditVo.getCiId(), rebuildAuditVo.getAttrIdList());
                    if (CollectionUtils.isNotEmpty(attrList) && attrList.stream().anyMatch(attr -> attr.getExpressionCiId().equals(rebuildAuditVo.getCiId()))) {
                        updateExpressionAttr(new CiEntityVo(rebuildAuditVo.getCiId(), rebuildAuditVo.getCiEntityId()));
                    }
                } else {
                    updateExpressionAttr(new CiEntityVo(rebuildAuditVo.getCiId(), rebuildAuditVo.getCiEntityId()));
                }
            } else if (rebuildAuditVo.getCiId() != null && rebuildAuditVo.getCiEntityId() == null && StringUtils.isNotBlank(rebuildAuditVo.getAttrIds())) {
                //没有配置项信息的则是因为表达式发生了修改，所以需要更新模型下所有配置项信息，如果是抽象模型修改，则需要更新所有子模型的信息
                CiVo ciVo = ciMapper.getCiById(rebuildAuditVo.getCiId());
                if (ciVo != null) {
                    List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                    CiEntityVo ciEntityVo = new CiEntityVo();
                    //ciEntityVo.setCiId(rebuildAuditVo.getCiId());
                    ciEntityVo.setCiIdList(ciList.stream().map(CiVo::getId).collect(Collectors.toList()));
                    ciEntityVo.setPageSize(100);
                    ciEntityVo.setCurrentPage(1);
                    ciEntityVo.setCiEntityIdStart(rebuildAuditVo.getCiEntityIdStart());
                    List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntityBaseInfo(ciEntityVo);
                    while (CollectionUtils.isNotEmpty(ciEntityList)) {
                        for (CiEntityVo ciEntity : ciEntityList) {
                            updateExpressionAttr(ciEntity, rebuildAuditVo.getAttrIdList());
                            //更新修改进度
                            rebuildAuditVo.setCiEntityIdStart(ciEntity.getId());
                            attrExpressionRebuildAuditMapper.updateAttrExpressionRebuildAuditCiEntityIdStartById(rebuildAuditVo);
                        }
                        ciEntityVo.setCurrentPage(ciEntityVo.getCurrentPage() + 1);
                        ciEntityList = ciEntityService.searchCiEntityBaseInfo(ciEntityVo);
                    }
                }
            }
            //清除日志
            attrExpressionRebuildAuditMapper.deleteAttrExpressionRebuildAuditById(rebuildAuditVo.getId());
        }

        /**
         * 更新配置项指定的表达式属性
         */
        private void updateExpressionAttr(CiEntityVo ciEntityVo, List<Long> attrIdList) {
            List<AttrVo> expressionList = attrMapper.getAttrByCiId(ciEntityVo.getCiId());
            List<AttrVo> expressionAttrList = expressionList.stream().filter(attr -> attr.getType().equals(EXPRESSION_TYPE)).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(attrIdList)) {
                expressionAttrList.removeIf(attrVo -> attrIdList.stream().noneMatch(aid -> aid.equals(attrVo.getId())));
            }
            if (CollectionUtils.isNotEmpty(expressionAttrList)) {
                CiEntityVo newCiEntityVo = ciEntityService.getCiEntityById(ciEntityVo.getCiId(), ciEntityVo.getId());
                CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
                CiEntityVo saveCiEntityVo = new CiEntityVo();
                saveCiEntityVo.setCiId(newCiEntityVo.getCiId());
                saveCiEntityVo.setId(newCiEntityVo.getId());
                for (AttrVo attrVo : expressionAttrList) {
                    if (attrVo.getConfig().containsKey("expression") && attrVo.getConfig().get("expression") instanceof JSONArray) {
                        List<ExpressionGroup> groupList = new ArrayList<>();
                        for (int i = 0; i < attrVo.getConfig().getJSONArray("expression").size(); i++) {
                            String expression = attrVo.getConfig().getJSONArray("expression").getString(i);
                            //如果是属性表达式
                            if (expression.startsWith("{") && expression.endsWith("}")) {
                                String exp = expression.substring(1, expression.length() - 1);
                                //如果包含关系
                                if (exp.contains(".")) {
                                    Long relId = Long.parseLong(exp.split("\\.")[0]);
                                    String attrId = exp.split("\\.")[1];
                                    String direction = exp.split("\\.")[2];
                                    if (CollectionUtils.isNotEmpty(groupList)) {
                                        ExpressionGroup expressionGroup = groupList.get(groupList.size() - 1);
                                        if (expressionGroup.getType() == ExpressionGroup.Type.REL && expressionGroup.getKey().equals(relId) && expressionGroup.getDirection().equals(direction)) {
                                            expressionGroup.addAttr(attrId, ExpressionGroupAttr.Type.ATTR);
                                        } else {
                                            groupList.add(new ExpressionGroup(relId, direction, ExpressionGroup.Type.REL, attrId, ExpressionGroupAttr.Type.ATTR));
                                        }
                                    } else {
                                        groupList.add(new ExpressionGroup(relId, direction, ExpressionGroup.Type.REL, attrId, ExpressionGroupAttr.Type.ATTR));
                                    }

                                } else {
                                    //用新id作为key，适配同一个属性重复出现的场景
                                    groupList.add(new ExpressionGroup(SnowflakeUtil.uniqueLong(), null, ExpressionGroup.Type.ATTR, exp, ExpressionGroupAttr.Type.ATTR));
                                }
                            } else {
                                if (CollectionUtils.isNotEmpty(groupList)) {
                                    ExpressionGroup expressionGroup = groupList.get(groupList.size() - 1);
                                    if (expressionGroup.getType() == ExpressionGroup.Type.REL) {
                                        expressionGroup.addAttr(expression, ExpressionGroupAttr.Type.CONST);
                                    } else {
                                        //用新id作为key，适配同一个常量重复出现的场景
                                        groupList.add(new ExpressionGroup(SnowflakeUtil.uniqueLong(), null, ExpressionGroup.Type.CONST, expression, ExpressionGroupAttr.Type.CONST));
                                    }
                                } else {
                                    //用新id作为key，适配同一个常量重复出现的场景
                                    groupList.add(new ExpressionGroup(SnowflakeUtil.uniqueLong(), null, ExpressionGroup.Type.CONST, expression, ExpressionGroupAttr.Type.CONST));
                                }
                            }
                        }
                        StringBuilder expressionValue = new StringBuilder();
                        for (ExpressionGroup expressionGroup : groupList) {
                            if (expressionGroup.getType() == ExpressionGroup.Type.REL) {
                                List<RelEntityVo> relEntityList = newCiEntityVo.getRelEntityByRelIdAndDirection(expressionGroup.getKey(), expressionGroup.getDirection());
                                if (CollectionUtils.isNotEmpty(relEntityList)) {
                                    List<String> expressionValueList = new ArrayList<>();
                                    for (RelEntityVo relEntity : relEntityList) {
                                        CiEntityVo relCiEntityVo;
                                        if (relEntity.getDirection().equals(RelDirectionType.FROM.getValue())) {
                                            relCiEntityVo = ciEntityService.getCiEntityById(relEntity.getToCiId(), relEntity.getToCiEntityId());
                                        } else {
                                            relCiEntityVo = ciEntityService.getCiEntityById(relEntity.getFromCiId(), relEntity.getFromCiEntityId());
                                        }
                                        if (relCiEntityVo != null) {
                                            StringBuilder groupValue = new StringBuilder();
                                            for (ExpressionGroupAttr expressionGroupAttr : expressionGroup.getAttrList()) {
                                                if (expressionGroupAttr.getType() == ExpressionGroupAttr.Type.ATTR) {
                                                    AttrEntityVo attrEntityVo = relCiEntityVo.getAttrEntityByAttrId(Long.parseLong(expressionGroupAttr.getAttr()));
                                                    if (attrEntityVo != null) {
                                                        groupValue.append(attrEntityVo.getActualValueList().stream().map(Object::toString).collect(Collectors.joining(",")));
                                                    }
                                                } else {
                                                    groupValue.append(expressionGroupAttr.getAttr());
                                                }
                                            }
                                            expressionValueList.add(groupValue.toString());
                                        }
                                    }
                                    if (CollectionUtils.isNotEmpty(expressionValueList)) {
                                        expressionValue.append(String.join(",", expressionValueList));
                                    }
                                }
                            } else if (expressionGroup.getType() == ExpressionGroup.Type.ATTR) {
                                Long attrId = Long.parseLong(expressionGroup.getAttrList().get(0).getAttr());
                                AttrEntityVo attrEntityVo = newCiEntityVo.getAttrEntityByAttrId(attrId);
                                if (attrEntityVo != null) {
                                    expressionValue.append(attrEntityVo.getActualValueList().stream().map(Object::toString).collect(Collectors.joining(",")));
                                }
                            } else if (expressionGroup.getType() == ExpressionGroup.Type.CONST) {
                                expressionValue.append(expressionGroup.getAttrList().get(0).getAttr());
                            }
                        }

                        JSONObject attrEntityObj = new JSONObject();
                        attrEntityObj.put("ciId", attrVo.getCiId());
                        attrEntityObj.put("type", attrVo.getType());
                        attrEntityObj.put("valueList", new JSONArray() {{
                            this.add(expressionValue.toString());
                        }});
                        saveCiEntityVo.addAttrEntityData(attrVo.getId(), attrEntityObj);
                        //更新配置项名称
                        if (Objects.equals(ciVo.getNameAttrId(), attrVo.getId())) {
                            saveCiEntityVo.setName(expressionValue.toString());
                            ciEntityService.updateCiEntityName(saveCiEntityVo);
                        }
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
        attrExpressionRebuildAuditMapper.insertAttrExpressionRebuildAudit(rebuildAuditVo);
        AfterTransactionJob<RebuildAuditVo> job = new AfterTransactionJob<>("CIENTITY-EXPRESSION-ATTR-BUILDER");
        job.execute(rebuildAuditVo, rebuildQueue::offer);
    }

    public static void rebuild(List<RebuildAuditVo> rebuildAuditList) {
        if (CollectionUtils.isNotEmpty(rebuildAuditList)) {
            for (RebuildAuditVo auditVo : rebuildAuditList) {
                attrExpressionRebuildAuditMapper.insertAttrExpressionRebuildAudit(auditVo);
            }
            AfterTransactionJob<List<RebuildAuditVo>> job = new AfterTransactionJob<>("CIENTITY-EXPRESSION-ATTR-BUILDER");
            job.execute(rebuildAuditList, pRebuildAuditList -> {
                for (RebuildAuditVo auditVo : pRebuildAuditList) {
                    rebuildQueue.offer(auditVo);
                }
            });
        }
    }

}
