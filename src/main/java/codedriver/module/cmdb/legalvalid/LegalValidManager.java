/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.legalvalid;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadpool.CachedThreadPool;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.ci.RelVo;
import codedriver.framework.cmdb.dto.cientity.AttrEntityVo;
import codedriver.framework.cmdb.dto.cientity.AttrFilterVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelEntityVo;
import codedriver.framework.cmdb.dto.group.ConditionGroupVo;
import codedriver.framework.cmdb.dto.group.ConditionVo;
import codedriver.framework.cmdb.dto.legalvalid.IllegalCiEntityVo;
import codedriver.framework.cmdb.dto.legalvalid.LegalValidVo;
import codedriver.framework.cmdb.enums.RelDirectionType;
import codedriver.framework.cmdb.enums.RelRuleType;
import codedriver.framework.cmdb.enums.SearchExpression;
import codedriver.framework.cmdb.enums.legalvalid.LegalValidType;
import codedriver.framework.cmdb.exception.ci.CiUniqueAttrNotFoundException;
import codedriver.framework.cmdb.exception.ci.CiUniqueRuleException;
import codedriver.framework.cmdb.exception.cientity.*;
import codedriver.framework.cmdb.exception.validator.AttrInValidatedException;
import codedriver.framework.cmdb.exception.validator.ValidatorNotFoundException;
import codedriver.framework.cmdb.validator.core.IValidator;
import codedriver.framework.cmdb.validator.core.ValidatorFactory;
import codedriver.framework.exception.core.ApiRuntimeException;
import codedriver.framework.util.javascript.JavascriptUtil;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.legalvalid.IllegalCiEntityMapper;
import codedriver.module.cmdb.dao.mapper.legalvalid.LegalValidMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.framework.cmdb.utils.RelUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class LegalValidManager {
    private final static String EXPRESSION_TYPE = "expression";
    private static final Logger logger = LoggerFactory.getLogger(LegalValidManager.class);

    private static AttrMapper attrMapper;

    private static RelMapper relMapper;

    private static CiMapper ciMapper;

    private static CiEntityMapper ciEntityMapper;

    private static RelEntityMapper relEntityMapper;

    private static CiEntityService ciEntityService;

    private static LegalValidMapper legalValidMapper;

    private static IllegalCiEntityMapper illegalCiEntityMapper;

    @Autowired
    public LegalValidManager(AttrMapper _attrMapper, RelMapper _relMapper, CiMapper _ciMapper, CiEntityMapper _ciEntityMapper, RelEntityMapper _relEntityMapper, CiEntityService _ciEntityService, LegalValidMapper _legalValidMapper, IllegalCiEntityMapper _illegalCiEntityMapper) {
        attrMapper = _attrMapper;
        relMapper = _relMapper;
        ciMapper = _ciMapper;
        ciEntityMapper = _ciEntityMapper;
        relEntityMapper = _relEntityMapper;
        ciEntityService = _ciEntityService;
        legalValidMapper = _legalValidMapper;
        illegalCiEntityMapper = _illegalCiEntityMapper;
    }

    public static void doValid(LegalValidVo legalValidVo) {
        CachedThreadPool.execute(new CodeDriverThread("LEGAL-VALID-" + legalValidVo.getId()) {
            @Override
            protected void execute() {
                CiEntityVo pCiEntityVo = new CiEntityVo();
                pCiEntityVo.setCiId(legalValidVo.getCiId());
                pCiEntityVo.setPageSize(100);
                List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(pCiEntityVo);
                while (CollectionUtils.isNotEmpty(ciEntityList)) {
                    for (CiEntityVo ciEntityVo : ciEntityList) {
                        illegalCiEntityMapper.deleteCiEntityIllegal(ciEntityVo.getId(), legalValidVo.getId());
                        if (legalValidVo.getType().equals(LegalValidType.CI.getValue())) {
                            List<ApiRuntimeException> errorList = validateCiEntity(ciEntityVo);
                            if (CollectionUtils.isNotEmpty(errorList)) {
                                JSONArray errorMsgList = new JSONArray();
                                for (ApiRuntimeException ex : errorList) {
                                    errorMsgList.add(ex.getMessage(true));
                                }
                                IllegalCiEntityVo illegalCiEntityVo = new IllegalCiEntityVo();
                                illegalCiEntityVo.setCiId(ciEntityVo.getCiId());
                                illegalCiEntityVo.setCiEntityId(ciEntityVo.getId());
                                illegalCiEntityVo.setLegalValidId(legalValidVo.getId());
                                illegalCiEntityVo.setError(errorMsgList);
                                illegalCiEntityMapper.insertCiEntityIllegal(illegalCiEntityVo);
                            }
                        } else if (legalValidVo.getType().equals(LegalValidType.CUSTOM.getValue())) {
                            if (validateCiEntity(ciEntityVo, legalValidVo.getRule())) {
                                IllegalCiEntityVo illegalCiEntityVo = new IllegalCiEntityVo();
                                illegalCiEntityVo.setCiId(ciEntityVo.getCiId());
                                illegalCiEntityVo.setCiEntityId(ciEntityVo.getId());
                                illegalCiEntityVo.setLegalValidId(legalValidVo.getId());
                                illegalCiEntityMapper.insertCiEntityIllegal(illegalCiEntityVo);
                            }
                        }
                    }
                    pCiEntityVo.setCurrentPage(pCiEntityVo.getCurrentPage() + 1);
                    ciEntityList = ciEntityService.searchCiEntity(pCiEntityVo);
                }
            }
        });
    }

    private static boolean validateCiEntity(CiEntityVo ciEntityVo, JSONObject ruleObj) {
        boolean isAllMatch = false;
        if (ciEntityVo != null && MapUtils.isNotEmpty(ruleObj)) {
            JSONArray conditionGroupList = ruleObj.getJSONArray("conditionGroupList");
            JSONArray conditionGroupRelList = ruleObj.getJSONArray("conditionGroupRelList");
            if (CollectionUtils.isNotEmpty(conditionGroupList)) {
                //构造脚本
                StringBuilder script = new StringBuilder();
                JSONObject conditionObj = new JSONObject();
                for (int i = 0; i < conditionGroupList.size(); i++) {
                    ConditionGroupVo conditionGroupVo = JSONObject.toJavaObject(conditionGroupList.getJSONObject(i), ConditionGroupVo.class);
                    if (i > 0 && CollectionUtils.isNotEmpty(conditionGroupRelList)) {
                        if (conditionGroupRelList.size() >= i) {
                            String joinType = conditionGroupRelList.getString(i - 1);
                            script.append(joinType.equals("and") ? " && " : " || ");
                        } else {
                            //数据异常跳出
                            break;
                        }
                    }
                    script.append("(").append(conditionGroupVo.buildScript()).append(")");
                    if (CollectionUtils.isNotEmpty(conditionGroupVo.getConditionList())) {
                        for (ConditionVo conditionVo : conditionGroupVo.getConditionList()) {
                            conditionObj.put(conditionVo.getUuid(), conditionVo.getValueList());
                        }
                    }

                }

                JSONObject paramObj = new JSONObject();
                //将配置项参数处理成指定格式，格式和表达式相关，不能随意修改格式
                JSONObject dataObj = new JSONObject();
                if (MapUtils.isNotEmpty(ciEntityVo.getAttrEntityData())) {
                    for (String key : ciEntityVo.getAttrEntityData().keySet()) {
                        dataObj.put(key, ciEntityVo.getAttrEntityData().getJSONObject(key).getJSONArray("valueList"));
                    }
                }
                if (MapUtils.isNotEmpty(ciEntityVo.getRelEntityData())) {
                    for (String key : ciEntityVo.getRelEntityData().keySet()) {
                        //转换格式
                        JSONArray valueList = new JSONArray();
                        if (CollectionUtils.isNotEmpty(ciEntityVo.getRelEntityData().getJSONObject(key).getJSONArray("valueList"))) {
                            for (int i = 0; i < ciEntityVo.getRelEntityData().getJSONObject(key).getJSONArray("valueList").size(); i++) {
                                JSONObject entityObj = ciEntityVo.getRelEntityData().getJSONObject(key).getJSONArray("valueList").getJSONObject(i);
                                valueList.add(entityObj.getLong("ciEntityId"));
                            }
                        }
                        dataObj.put(key, valueList);
                    }
                }
                paramObj.put("data", dataObj);
                paramObj.put("condition", conditionObj);
                try {
                    isAllMatch = JavascriptUtil.runExpression(paramObj, script.toString());
                } catch (ScriptException | NoSuchMethodException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
        return isAllMatch;
    }

    /**
     * 验证配置项是否合规
     *
     * @param ciEntityVo 配置项信息
     * @return 是否合规
     */
    private static List<ApiRuntimeException> validateCiEntity(CiEntityVo ciEntityVo) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityVo.getCiId());
        List<RelVo> relList = RelUtil.ClearRepeatRel(relMapper.getRelByCiId(ciEntityVo.getCiId()));
        CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
        List<ApiRuntimeException> errorList = new ArrayList<>();

        for (AttrVo attrVo : attrList) {
            if (!attrVo.getType().equals(EXPRESSION_TYPE)) {
                AttrEntityVo attrEntityVo =
                        ciEntityVo.getAttrEntityByAttrId(attrVo.getId());
                if (attrVo.getIsRequired().equals(1)) {
                    if (attrEntityVo == null) {
                        errorList.add(new AttrEntityValueEmptyException(attrVo.getLabel()));
                    } else if (CollectionUtils.isEmpty(attrEntityVo.getValueList())) {
                        errorList.add(new AttrEntityValueEmptyException(attrVo.getLabel()));
                    }
                }

                /* 检查属性是否唯一： */
                if (attrVo.getIsUnique().equals(1)) {
                    if (attrEntityVo != null
                            && CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                        if (attrVo.isNeedTargetCi()) {
                            List<Long> toCiEntityIdList = new ArrayList<>();
                            for (int i = 0; i < attrEntityVo.getValueList().size(); i++) {
                                //如果不是id,则代表是新添加配置项，这时候不需要判断属性唯一
                                if (attrEntityVo.getValueList().get(i) instanceof Number) {
                                    Long tmpId = attrEntityVo.getValueList().getLong(i);
                                    if (tmpId != null) {
                                        toCiEntityIdList.add(tmpId);
                                    }
                                }
                            }
                            if (CollectionUtils.isNotEmpty(toCiEntityIdList)) {
                                int attrEntityCount = ciEntityMapper.getAttrEntityCountByAttrIdAndValue(ciEntityVo.getId(), attrVo.getId(), toCiEntityIdList);
                                if (attrEntityCount > 0) {
                                    List<CiEntityVo> toCiEntityList = ciEntityMapper.getCiEntityBaseInfoByIdList(toCiEntityIdList);
                                    errorList.add(new AttrEntityDuplicateException(ciVo, attrVo.getLabel(), toCiEntityList.stream().map(CiEntityVo::getName).collect(Collectors.toList())));
                                }
                            }
                        } else {
                            //检查配置项表对应字段是否已被其他配置项使用
                            int count = ciEntityMapper.getCiEntityCountByAttrIdAndValue(ciEntityVo.getId(), attrVo, attrEntityVo.getValue());
                            if (count > 0) {
                                errorList.add(new AttrEntityDuplicateException(ciVo, attrVo.getLabel(), attrEntityVo.getValueList()));
                            }
                        }
                    }
                }

                //检查是否允许多选
                if (attrVo.getTargetCiId() != null && MapUtils.isNotEmpty(attrVo.getConfig()) && attrVo.getConfig().containsKey("isMultiple") && attrVo.getConfig().getString("isMultiple").equals("0")) {
                    if (attrEntityVo != null && CollectionUtils.isNotEmpty(attrEntityVo.getValueList()) && attrEntityVo.getValueList().size() > 1) {
                        throw new AttrEntityMultipleException(attrVo);
                    }
                }

                /*  调用校验器校验数据合法性，只有非引用型属性才需要 */
                if (attrEntityVo != null && CollectionUtils.isNotEmpty(attrEntityVo.getValueList())
                        && StringUtils.isNotBlank(attrVo.getValidatorHandler()) && !attrVo.isNeedTargetCi()) {
                    IValidator validator = ValidatorFactory.getValidator(attrVo.getValidatorHandler());
                    if (validator != null) {
                        try {
                            validator.valid(attrVo.getLabel(), attrEntityVo.getValueList(),
                                    attrVo.getValidatorId());
                        } catch (ValidatorNotFoundException | AttrInValidatedException ex) {
                            errorList.add(ex);
                        }
                    }
                }
            }
        }

        // 校验关系信息
        for (RelVo relVo : relList) {
            // 判断当前配置项处于from位置的规则
            List<RelEntityVo> fromRelEntityList =
                    ciEntityVo.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue());
            // 判断当前配置项处于to位置的规则
            List<RelEntityVo> toRelEntityList =
                    ciEntityVo.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue());

            // 标记当前模型是在关系的上端或者下端
            boolean isFrom = false;
            boolean isTo = false;
            if (relVo.getFromCiId().equals(ciEntityVo.getCiId())) {
                isFrom = true;
            }
            if (relVo.getToCiId().equals(ciEntityVo.getCiId())) {
                isTo = true;
            }

            if (CollectionUtils.isEmpty(fromRelEntityList)) {
                if (isFrom && relVo.getToIsRequired().equals(1)) {
                    errorList.add(new RelEntityNotFoundException(relVo.getToLabel()));
                }
            } else {
                if (fromRelEntityList.size() > 1) {
                    // 检查关系是否允许重复
                    if (RelRuleType.O.getValue().equals(relVo.getToRule())) {
                        errorList.add(new RelEntityMultipleException(relVo.getToLabel()));
                    }
                    if (relVo.getFromIsUnique().equals(1)) {
                        errorList.add(new RelEntityIsUsedException(RelDirectionType.FROM, relVo, false));
                    }
                }
                //检查关系唯一
                if (relVo.getToIsUnique().equals(1)) {
                    for (RelEntityVo fromRelEntityVo : fromRelEntityList) {
                        List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByToCiEntityIdAndRelId(fromRelEntityVo.getToCiEntityId(), relVo.getId(), null);
                        if (checkFromRelEntityList.stream().anyMatch(r -> !r.getFromCiEntityId().equals(ciEntityVo.getId()))) {
                            errorList.add(new RelEntityIsUsedException(RelDirectionType.FROM, relVo));
                        }
                    }
                }
            }

            if (CollectionUtils.isEmpty(toRelEntityList)) {
                if (isTo && relVo.getFromIsRequired().equals(1)) {
                    errorList.add(new RelEntityNotFoundException(relVo.getFromLabel()));
                }
            } else {
                if (toRelEntityList.size() > 1) {
                    // 检查关系是否允许重复
                    if (RelRuleType.O.getValue().equals(relVo.getFromRule())) {
                        errorList.add(new RelEntityMultipleException(relVo.getFromLabel()));
                    }
                    if (relVo.getToIsUnique().equals(1)) {
                        errorList.add(new RelEntityIsUsedException(RelDirectionType.TO, relVo, false));
                    }
                }
                //检查关系唯一
                if (relVo.getFromIsUnique().equals(1)) {
                    for (RelEntityVo toRelEntityVo : toRelEntityList) {
                        List<RelEntityVo> checkFromRelEntityList = relEntityMapper.getRelEntityByFromCiEntityIdAndRelId(toRelEntityVo.getFromCiEntityId(), relVo.getId(), null);
                        if (checkFromRelEntityList.stream().anyMatch(r -> !r.getToCiEntityId().equals(ciEntityVo.getId()))) {
                            throw new RelEntityIsUsedException(RelDirectionType.TO, relVo);
                        }
                    }
                }
            }
        }

        //校验唯一规则
        if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
            CiEntityVo ciEntityConditionVo = new CiEntityVo();
            ciEntityConditionVo.setCiId(ciEntityVo.getCiId());
            for (Long attrId : ciVo.getUniqueAttrIdList()) {
                AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrId);
                if (attrEntityVo != null) {
                    AttrFilterVo filterVo = new AttrFilterVo();
                    filterVo.setAttrId(attrId);
                    filterVo.setExpression(SearchExpression.EQ.getExpression());
                    filterVo.setValueList(attrEntityVo.getValueList().stream().map(Object::toString).collect(Collectors.toList()));
                    ciEntityConditionVo.addAttrFilter(filterVo);
                } else {
                    Optional<AttrVo> op = attrList.stream().filter(a -> a.getId().equals(attrId)).findFirst();
                    if (op.isPresent()) {
                        errorList.add(new CiUniqueAttrNotFoundException(op.get()));
                    } else {
                        errorList.add(new CiUniqueAttrNotFoundException(ciVo.getLabel()));
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(ciEntityConditionVo.getAttrFilterList())) {
                List<CiEntityVo> checkList = ciEntityService.searchCiEntity(ciEntityConditionVo);
                for (CiEntityVo checkCiEntity : checkList) {
                    if (!checkCiEntity.getId().equals(ciEntityVo.getId())) {
                        errorList.add(new CiUniqueRuleException());
                    }
                }
            }
        }
        return errorList;
    }

}
