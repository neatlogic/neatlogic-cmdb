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

package neatlogic.module.cmdb.group;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.asynchronization.threadlocal.UserContext;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.group.*;
import neatlogic.framework.cmdb.enums.group.Status;
import neatlogic.framework.dao.mapper.TeamMapper;
import neatlogic.framework.exception.core.ApiRuntimeException;
import neatlogic.framework.transaction.core.AfterTransactionJob;
import neatlogic.framework.util.javascript.JavascriptUtil;
import neatlogic.module.cmdb.dao.mapper.group.GroupMapper;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class CiEntityGroupManager {
    private static final Logger logger = LoggerFactory.getLogger(CiEntityGroupManager.class);
    private static CiEntityService ciEntityService;

    private static GroupMapper groupMapper;

    private static TeamMapper teamMapper;

    @Autowired
    public CiEntityGroupManager(CiEntityService _ciEntityService, GroupMapper _groupMapper, TeamMapper _teamMapper) {
        ciEntityService = _ciEntityService;
        groupMapper = _groupMapper;
        teamMapper = _teamMapper;
    }


    public static void groupCiEntity(Long pCiId, Long pCiEntityId) {
        CiEntityVo pCiEntityVo = new CiEntityVo();
        pCiEntityVo.setId(pCiEntityId);
        pCiEntityVo.setCiId(pCiId);
        AfterTransactionJob<CiEntityVo> afterTransactionJob = new AfterTransactionJob<>("CIENTITY-GROUP-HANDLER");
        afterTransactionJob.execute(pCiEntityVo, vo -> {
            List<CiGroupVo> ciGroupVoList = groupMapper.getCiGroupByCiId(vo.getCiId());
            if (CollectionUtils.isNotEmpty(ciGroupVoList)) {
                Set<CiEntityGroupVo> checkSet = new HashSet<>();
                Set<Long> groupIdSet = new HashSet<>();
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
                            groupIdSet.add(group.getGroupId());
                        }
                    }
                }
                for (Long groupId : groupIdSet) {
                    //重新计算配置项数量
                    int count = groupMapper.getCiEntityCountByGroupId(groupId);
                    GroupVo g = new GroupVo();
                    g.setId(groupId);
                    g.setCiEntityCount(count);
                    groupMapper.updateGroupCiEntityCount(g);
                }
            }
        });
    }

    private static List<Long> getAttrIdFromRule(JSONObject ruleObj) {
        Set<Long> attrIdList = new HashSet<>();
        attrIdList.add(0L);//默认值，预防一个属性都没有的情况下变成搜索所有属性
        if (MapUtils.isNotEmpty(ruleObj)) {
            JSONArray conditionGroupList = ruleObj.getJSONArray("conditionGroupList");
            if (CollectionUtils.isNotEmpty(conditionGroupList)) {
                for (int i = 0; i < conditionGroupList.size(); i++) {
                    JSONArray conditionList = conditionGroupList.getJSONObject(i).getJSONArray("conditionList");
                    if (CollectionUtils.isNotEmpty(conditionList)) {
                        for (int j = 0; j < conditionList.size(); j++) {
                            JSONObject conditionObj = conditionList.getJSONObject(j);
                            if (conditionObj.getString("type").equals("attr")) {
                                attrIdList.add(Long.parseLong(conditionObj.getString("id").replace("attr_", "")));
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(attrIdList);
    }

    private static List<Long> getRelIdFromRule(JSONObject ruleObj) {
        Set<Long> relIdList = new HashSet<>();
        relIdList.add(0L);//默认值，预防一个关系都没有的情况下变成搜索所有关系
        if (MapUtils.isNotEmpty(ruleObj)) {
            JSONArray conditionGroupList = ruleObj.getJSONArray("conditionGroupList");
            if (CollectionUtils.isNotEmpty(conditionGroupList)) {
                for (int i = 0; i < conditionGroupList.size(); i++) {
                    JSONArray conditionList = conditionGroupList.getJSONObject(i).getJSONArray("conditionList");
                    if (CollectionUtils.isNotEmpty(conditionList)) {
                        for (int j = 0; j < conditionList.size(); j++) {
                            JSONObject conditionObj = conditionList.getJSONObject(j);
                            if (conditionObj.getString("type").equals("relfrom")) {
                                relIdList.add(Long.parseLong(conditionObj.getString("id").replace("relfrom_", "")));
                            } else if (conditionObj.getString("type").equals("relto")) {
                                relIdList.add(Long.parseLong(conditionObj.getString("id").replace("relto_", "")));
                            }
                        }
                    }
                }
            }
        }
        return new ArrayList<>(relIdList);
    }

    public static void group(GroupVo groupVo) {
        groupVo.setStatus(Status.DOING.getValue());
        groupMapper.updateGroupStatus(groupVo);
        AfterTransactionJob<GroupVo> afterTransactionJob = new AfterTransactionJob<>("CIENTITY-GROUP-HANDLER");
        afterTransactionJob.execute(groupVo, gVo -> {
            if (gVo != null && CollectionUtils.isNotEmpty(gVo.getCiGroupList())) {
                try {
                    if (gVo.getIsSync().equals(1)) {
                        groupMapper.deleteCiEntityGroupByGroupId(gVo.getId());
                        gVo.setCiEntityCount(0);
                        groupMapper.updateGroupCiEntityCount(gVo);
                    }
                    for (CiGroupVo ciGroupVo : gVo.getCiGroupList()) {
                        Set<CiEntityGroupVo> checkSet = new HashSet<>();
                        CiEntityVo pCiEntityVo = new CiEntityVo();
                        pCiEntityVo.setPageSize(100);
                        pCiEntityVo.setCurrentPage(1);
                        pCiEntityVo.setCiId(ciGroupVo.getCiId());
                        pCiEntityVo.setAttrIdList(getAttrIdFromRule(ciGroupVo.getRule()));
                        pCiEntityVo.setRelIdList(getRelIdFromRule(ciGroupVo.getRule()));

                        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(pCiEntityVo);
                        while (CollectionUtils.isNotEmpty(ciEntityList)) {
                            for (CiEntityVo ciEntityVo : ciEntityList) {
                                CiEntityGroupVo ciEntityGroupVo = new CiEntityGroupVo(ciEntityVo.getId(), gVo.getId(), ciGroupVo.getId());
                                if (!checkSet.contains(ciEntityGroupVo)) {
                                    if (matchRule(ciEntityVo, ciGroupVo)) {
                                        groupMapper.insertCiEntityGroup(ciEntityGroupVo);
                                        checkSet.add(ciEntityGroupVo);
                                    }
                                }
                            }
                            pCiEntityVo.setCurrentPage(pCiEntityVo.getCurrentPage() + 1);
                            ciEntityList = ciEntityService.searchCiEntity(pCiEntityVo);
                        }
                    }
                    gVo.setError("");
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                    String message = "";
                    if (ex instanceof ApiRuntimeException) {
                        message = ((ApiRuntimeException) ex).getMessage();
                    } else {
                        message = ExceptionUtils.getStackTrace(ex);
                        logger.error(ex.getMessage(), ex);
                    }
                    gVo.setError(message);
                } finally {
                    //重新计算配置项数量,每100条更新一次，目的是为了计算进度
                    int count = groupMapper.getCiEntityCountByGroupId(gVo.getId());
                    gVo.setCiEntityCount(count);
                    groupMapper.updateGroupCiEntityCount(gVo);
                    gVo.setStatus(Status.DONE.getValue());
                    groupMapper.updateGroupStatus(gVo);
                }
            } else {
                if (gVo != null) {
                    gVo.setError("当前团体没有关联任何配置项模型");
                    gVo.setStatus(Status.DONE.getValue());
                    groupMapper.updateGroupStatus(gVo);
                }
            }
        });
    }

    public static boolean checkCiEntityIsInUserGroup(CiEntityVo ciEntityVo) {
        String userUuid = UserContext.get().getUserUuid();
        List<String> teamUuidList = UserContext.get().getTeamUuidList();
        List<String> roleUuidList = UserContext.get().getRoleUuidList();
        List<GroupVo> groupList = groupMapper.getGroupByUserUuid(userUuid, teamUuidList, roleUuidList);
        if (CollectionUtils.isNotEmpty(groupList)) {
            for (GroupVo groupVo : groupList) {
                if (CollectionUtils.isNotEmpty(groupVo.getCiGroupList())) {
                    for (CiGroupVo ciGroupVo : groupVo.getCiGroupList()) {
                        if (matchRule(ciEntityVo, ciGroupVo)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void groupCi(Long ciId) {

    }

    private static boolean matchRule(CiEntityVo ciEntityVo, CiGroupVo ciGroupVo) {
        JSONObject ruleObj = ciGroupVo.getRule();
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

                //将配置项参数处理成指定格式，格式和表达式相关，不能随意修改格式
                JSONObject paramObj = new JSONObject();
                JSONObject dataObj = new JSONObject();
                JSONObject defineObj = new JSONObject();
                if (MapUtils.isNotEmpty(ciEntityVo.getGlobalAttrEntityData())) {
                    for (String key : ciEntityVo.getGlobalAttrEntityData().keySet()) {
                        JSONObject attrObj = ciEntityVo.getGlobalAttrEntityData().getJSONObject(key);
                        defineObj.put(key, attrObj.getString("attrLabel"));
                        if (CollectionUtils.isNotEmpty(attrObj.getJSONArray("valueList"))) {
                            JSONArray valueList = new JSONArray();
                            for (int i = 0; i < attrObj.getJSONArray("valueList").size(); i++) {
                                JSONObject valueObj = attrObj.getJSONArray("valueList").getJSONObject(i);
                                valueList.add(valueObj.getLong("id"));
                            }
                            dataObj.put(key, valueList);
                        }
                    }
                }
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
                    isAllMatch = JavascriptUtil.runExpression(paramObj, script.toString());
                } catch (ApiRuntimeException ex) {
                    logger.error(ex.getMessage(), ex);
                    //忽略内部异常
                } catch (Exception ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
        return isAllMatch;
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
