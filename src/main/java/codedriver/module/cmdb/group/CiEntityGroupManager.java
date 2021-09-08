/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.group;

import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.group.CiEntityGroupVo;
import codedriver.framework.cmdb.dto.group.CiGroupVo;
import codedriver.framework.transaction.core.AfterTransactionJob;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class CiEntityGroupManager {

    private static CiEntityService ciEntityService;

    private static GroupMapper groupMapper;

    @Autowired
    public CiEntityGroupManager(CiEntityService _ciEntityService, GroupMapper _groupMapper) {
        ciEntityService = _ciEntityService;
        groupMapper = _groupMapper;
    }


    public static void groupCiEntity(Long pCiId, Long pCiEntityId) {
        CiEntityVo pCiEntityVo = new CiEntityVo();
        pCiEntityVo.setId(pCiEntityId);
        pCiEntityVo.setCiId(pCiId);
        AfterTransactionJob<CiEntityVo> afterTransactionJob = new AfterTransactionJob<>();
        Set<CiEntityGroupVo> checkSet = new HashSet<>();
        afterTransactionJob.execute(pCiEntityVo, vo -> {
            List<CiGroupVo> ciGroupVoList = groupMapper.getCiGroupByCiId(vo.getCiId());
            if (CollectionUtils.isNotEmpty(ciGroupVoList)) {
                CiEntityVo ciEntityVo = ciEntityService.getCiEntityById(vo.getCiId(), vo.getId());
                if (ciEntityVo != null) {
                    for (CiGroupVo group : ciGroupVoList) {
                        CiEntityGroupVo ciEntityGroupVo = new CiEntityGroupVo(ciEntityVo.getId(), group.getGroupId(), group.getId());
                        if (!checkSet.contains(ciEntityGroupVo)) {
                            if (matchRule(ciEntityVo, group)) {
                                groupMapper.insertCiEntityGroup(ciEntityGroupVo);
                                checkSet.add(ciEntityGroupVo);
                            } else {
                                groupMapper.deleteCiEntityGroupByCiEntityIdAndCiGroupId(ciEntityVo.getId(), group.getId());
                            }
                        }
                    }
                }
            }
        });
    }

    public static void groupCi(Long ciId) {

    }

    private static boolean matchRule(CiEntityVo ciEntityVo, CiGroupVo ciGroupVo) {
        JSONObject ruleObj = ciGroupVo.getRule();
        if (ciEntityVo != null && MapUtils.isNotEmpty(ruleObj)) {
            Map<Integer, Boolean> checkResultMap = new HashMap<>();
            /*for (int i = 0; i < ruleList.size(); i++) {
                checkResultMap.put(i, false);
                JSONObject rule = ruleList.getJSONObject(i);
                String exp = rule.getString("expression");
                SearchExpression expression = SearchExpression.get(exp);
                if (expression != null) {
                    Long attrId = rule.getLong("attrId");
                    JSONArray valueList = rule.getJSONArray("valueList");
                    if (attrId != null) {
                        AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrId);
                        if (expression == SearchExpression.NULL) {
                            if (attrEntityVo == null || CollectionUtils.isEmpty(attrEntityVo.getValueList())) {
                                checkResultMap.put(i, true);
                            }
                        } else if (expression == SearchExpression.NOTNULL) {
                            if (attrEntityVo != null && CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                                checkResultMap.put(i, true);
                            }
                        } else if (expression == SearchExpression.EQ) {
                            if (attrEntityVo != null && CollectionUtils.isNotEmpty(valueList) && CollectionUtils.isNotEmpty(attrEntityVo.getValueList()) && CollectionUtils.isEqualCollection(valueList, attrEntityVo.getValueList())) {
                                checkResultMap.put(i, true);
                            }
                        } else if (expression == SearchExpression.LI) {
                            if (attrEntityVo != null && CollectionUtils.isNotEmpty(valueList) && CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                                if (attrEntityVo.getToCiId() != null && attrEntityVo.getValueList().containsAll(valueList)) {
                                    //引用类型属性比较
                                    checkResultMap.put(i, true);
                                } else if (attrEntityVo.getToCiId() == null) {
                                    //正常属性比较
                                    String av = attrEntityVo.getValue();
                                    String v = getValue(valueList);
                                    if (StringUtils.isNotBlank(av) && StringUtils.isNotBlank(v) && av.contains(v)) {
                                        checkResultMap.put(i, true);
                                    }
                                }
                            }
                        } else if (expression == SearchExpression.NE) {
                            if (attrEntityVo != null && CollectionUtils.isNotEmpty(valueList) && CollectionUtils.isNotEmpty(attrEntityVo.getValueList()) && !CollectionUtils.isEqualCollection(valueList, attrEntityVo.getValueList())) {
                                checkResultMap.put(i, true);
                            }
                        } else if (expression == SearchExpression.NL) {
                            if (attrEntityVo != null && CollectionUtils.isNotEmpty(valueList) && CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                                if (attrEntityVo.getToCiId() != null && !attrEntityVo.getValueList().containsAll(valueList)) {
                                    //引用类型属性比较
                                    checkResultMap.put(i, true);
                                } else if (attrEntityVo.getToCiId() == null) {
                                    //正常属性比较
                                    String av = attrEntityVo.getValue();
                                    String v = getValue(valueList);
                                    if (StringUtils.isNotBlank(av) && StringUtils.isNotBlank(v) && !av.contains(v)) {
                                        checkResultMap.put(i, true);
                                    }
                                }
                            }
                        }
                    }
                }
            }*/
            boolean isAllMatch = true;
            for (int key : checkResultMap.keySet()) {
                if (!checkResultMap.get(key)) {
                    isAllMatch = false;
                    break;
                }
            }
            return isAllMatch;
        }
        return false;
    }

    private static String getValue(JSONArray valueList) {
        if (CollectionUtils.isNotEmpty(valueList)) {
            if (valueList.size() == 1) {
                return valueList.getString(0);
            } else {
                return valueList.toString();
            }
        }
        return null;
    }

    public static void main(String[] arg) {
        JSONArray valueList1 = new JSONArray();
        JSONArray valueList2 = new JSONArray();
        valueList1.add("1");
        valueList1.add("2");

        valueList2.add("1");
        System.out.println(valueList1.containsAll(valueList2));
    }

}
