/*
 * Copyright(c) 2023 NeatLogic Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package neatlogic.module.cmdb.legalvalid;

import neatlogic.framework.asynchronization.thread.NeatLogicThread;
import neatlogic.framework.asynchronization.threadpool.CachedThreadPool;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.ci.RelVo;
import neatlogic.framework.cmdb.dto.cientity.AttrEntityVo;
import neatlogic.framework.cmdb.dto.cientity.AttrFilterVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelEntityVo;
import neatlogic.framework.cmdb.dto.group.ConditionGroupVo;
import neatlogic.framework.cmdb.dto.group.ConditionVo;
import neatlogic.framework.cmdb.dto.legalvalid.IllegalCiEntityVo;
import neatlogic.framework.cmdb.dto.legalvalid.LegalValidVo;
import neatlogic.framework.cmdb.enums.RelDirectionType;
import neatlogic.framework.cmdb.enums.RelRuleType;
import neatlogic.framework.cmdb.enums.SearchExpression;
import neatlogic.framework.cmdb.enums.legalvalid.LegalValidType;
import neatlogic.framework.cmdb.exception.attrtype.AttrTypeNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiUniqueAttrNotFoundException;
import neatlogic.framework.cmdb.exception.ci.CiUniqueRuleException;
import neatlogic.framework.cmdb.exception.cientity.*;
import neatlogic.framework.cmdb.exception.validator.AttrInValidatedException;
import neatlogic.framework.cmdb.exception.validator.ValidatorNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.cmdb.validator.core.IValidator;
import neatlogic.framework.cmdb.validator.core.ValidatorFactory;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.util.javascript.JavascriptUtil;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.RelMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import neatlogic.module.cmdb.dao.mapper.legalvalid.IllegalCiEntityMapper;
import neatlogic.module.cmdb.dao.mapper.legalvalid.LegalValidMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
        CachedThreadPool.execute(new NeatLogicThread("LEGAL-VALID-" + legalValidVo.getId()) {
            @Override
            protected void execute() {
                CiEntityVo pCiEntityVo = new CiEntityVo();
                pCiEntityVo.setCiId(legalValidVo.getCiId());
                pCiEntityVo.setPageSize(100);
                //检查是否存在不需要join所有属性和关系
                pCiEntityVo.setAttrIdList(new ArrayList<Long>() {{
                    this.add(0L);
                }});
                pCiEntityVo.setRelIdList(new ArrayList<Long>() {{
                    this.add(0L);
                }});
                List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(pCiEntityVo);
                while (CollectionUtils.isNotEmpty(ciEntityList)) {
                    for (CiEntityVo ciEntityVo : ciEntityList) {
                        //查找完整的配置项信息
                        ciEntityVo.setLimitAttrEntity(false);
                        ciEntityVo.setLimitRelEntity(false);
                        ciEntityVo = ciEntityService.getCiEntityById(ciEntityVo);
                        illegalCiEntityMapper.deleteCiEntityIllegal(ciEntityVo.getId(), legalValidVo.getId());
                        if (legalValidVo.getType().equals(LegalValidType.CI.getValue())) {
                            List<ApiRuntimeException> errorList = validateCiEntity(ciEntityVo);
                            if (CollectionUtils.isNotEmpty(errorList)) {
                                JSONArray errorMsgList = new JSONArray();
                                for (ApiRuntimeException ex : errorList) {
                                    errorMsgList.add(ex.getMessage());
                                }
                                IllegalCiEntityVo illegalCiEntityVo = new IllegalCiEntityVo();
                                illegalCiEntityVo.setCiId(ciEntityVo.getCiId());
                                illegalCiEntityVo.setCiEntityId(ciEntityVo.getId());
                                illegalCiEntityVo.setLegalValidId(legalValidVo.getId());
                                illegalCiEntityVo.setError(errorMsgList);
                                illegalCiEntityMapper.insertCiEntityIllegal(illegalCiEntityVo);
                            }
                        } else if (legalValidVo.getType().equals(LegalValidType.CUSTOM.getValue())) {
                            List<ApiRuntimeException> errorList = validateCiEntity(ciEntityVo, legalValidVo.getRule());
                            if (CollectionUtils.isNotEmpty(errorList)) {
                                JSONArray errorMsgList = new JSONArray();
                                for (ApiRuntimeException ex : errorList) {
                                    errorMsgList.add(ex.getMessage());
                                }
                                IllegalCiEntityVo illegalCiEntityVo = new IllegalCiEntityVo();
                                illegalCiEntityVo.setCiId(ciEntityVo.getCiId());
                                illegalCiEntityVo.setCiEntityId(ciEntityVo.getId());
                                illegalCiEntityVo.setLegalValidId(legalValidVo.getId());
                                illegalCiEntityVo.setError(errorMsgList);
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

    private static List<ApiRuntimeException> validateCiEntity(CiEntityVo ciEntityVo, JSONObject ruleObj) {
        List<ApiRuntimeException> errorList = new ArrayList<>();
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
                //将配置项参数处理成指定格式，格式和表达式相关，不能随意修改格式
                JSONObject paramObj = new JSONObject();
                JSONObject dataObj = new JSONObject();
                JSONObject defineObj = new JSONObject();
                if (MapUtils.isNotEmpty(ciEntityVo.getAttrEntityData())) {
                    for (String key : ciEntityVo.getAttrEntityData().keySet()) {
                        defineObj.put(key, ciEntityVo.getAttrEntityData().getJSONObject(key).getString("label"));
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
                        defineObj.put(key, ciEntityVo.getRelEntityData().getJSONObject(key).getString("label"));
                        dataObj.put(key, valueList);
                    }
                }
                paramObj.put("define", defineObj);
                paramObj.put("data", dataObj);
                paramObj.put("condition", conditionObj);
                try {
                    JavascriptUtil.runExpression(paramObj, script.toString());
                } catch (Exception e) {
                    errorList.add(new ApiRuntimeException(e.getMessage()));
                }
            }
        }
        return errorList;
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
                AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrVo.getId());
                if (attrVo.getIsRequired().equals(1)) {
                    if (attrEntityVo == null) {
                        errorList.add(new AttrEntityValueEmptyException(attrVo.getLabel()));
                    } else if (CollectionUtils.isEmpty(attrEntityVo.getValueList())) {
                        errorList.add(new AttrEntityValueEmptyException(attrVo.getLabel()));
                    }
                }

                //检查是否允许多选
                if (attrVo.getTargetCiId() != null && MapUtils.isNotEmpty(attrVo.getConfig()) && attrVo.getConfig().containsKey("isMultiple") && attrVo.getConfig().getString("isMultiple").equals("0")) {
                    if (attrEntityVo != null && CollectionUtils.isNotEmpty(attrEntityVo.getValueList()) && attrEntityVo.getValueList().size() > 1) {
                        throw new AttrEntityMultipleException(attrVo);
                    }
                }

                /* 校验值是否符合数据类型*/
                if (attrEntityVo != null && CollectionUtils.isNotEmpty(attrEntityVo.getValueList()) && !attrVo.isNeedTargetCi()) {
                    IAttrValueHandler attrHandler = AttrValueHandlerFactory.getHandler(attrVo.getType());
                    if (attrHandler != null) {
                        attrHandler.valid(attrVo, attrEntityVo.getValueList());
                    } else {
                        throw new AttrTypeNotFoundException(attrVo.getType());
                    }
                }

                /*  调用校验器校验数据合法性，只有非引用型属性才需要 */
                if (attrEntityVo != null && CollectionUtils.isNotEmpty(attrEntityVo.getValueList()) && StringUtils.isNotBlank(attrVo.getValidatorHandler()) && !attrVo.isNeedTargetCi()) {
                    IValidator validator = ValidatorFactory.getValidator(attrVo.getValidatorHandler());
                    if (validator != null) {
                        try {
                            validator.valid(attrVo, attrEntityVo.getValueList());
                        } catch (ValidatorNotFoundException | AttrInValidatedException ex) {
                            errorList.add(ex);
                        }
                    }
                }

                /* 检查属性是否唯一： */
                if (attrVo.getIsUnique().equals(1)) {
                    if (attrEntityVo != null && CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
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
            }
        }

        // 校验关系信息
        for (RelVo relVo : relList) {
            // 判断当前配置项处于from位置的规则
            List<RelEntityVo> fromRelEntityList = ciEntityVo.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.FROM.getValue());
            // 判断当前配置项处于to位置的规则
            List<RelEntityVo> toRelEntityList = ciEntityVo.getRelEntityByRelIdAndDirection(relVo.getId(), RelDirectionType.TO.getValue());

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
                        Optional<RelEntityVo> op = checkFromRelEntityList.stream().filter(r -> !r.getFromCiEntityId().equals(ciEntityVo.getId())).findFirst();
                        op.ifPresent(relEntityVo -> errorList.add(new RelEntityIsUsedException(RelDirectionType.FROM, relVo, relEntityVo)));
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
                        Optional<RelEntityVo> op = checkFromRelEntityList.stream().filter(r -> !r.getToCiEntityId().equals(ciEntityVo.getId())).findFirst();
                        if (op.isPresent()) {
                            throw new RelEntityIsUsedException(RelDirectionType.TO, relVo, op.get());
                        }
                    }
                }
            }
        }

        //校验唯一规则
        if (CollectionUtils.isNotEmpty(ciVo.getUniqueAttrIdList())) {
            CiEntityVo ciEntityConditionVo = new CiEntityVo();
            ciEntityConditionVo.setCiId(ciEntityVo.getCiId());
            //检查是否存在不需要join所有属性和关系
            ciEntityConditionVo.setAttrIdList(new ArrayList<Long>() {{
                this.add(0L);
            }});
            ciEntityConditionVo.setRelIdList(new ArrayList<Long>() {{
                this.add(0L);
            }});
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
                        errorList.add(new CiUniqueRuleException(ciVo));
                    }
                }
            }
        }
        return errorList;
    }

}
