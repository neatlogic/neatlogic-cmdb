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

package neatlogic.module.cmdb.api.cientity;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.auth.core.AuthAction;
import neatlogic.framework.auth.core.AuthActionChecker;
import neatlogic.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import neatlogic.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import neatlogic.framework.cmdb.auth.label.CMDB_BASE;
import neatlogic.framework.cmdb.auth.label.RESOURCECENTER_ACCOUNT_MODIFY;
import neatlogic.framework.cmdb.crossover.ISearchCiEntityApiCrossoverService;
import neatlogic.framework.cmdb.dto.ci.AttrVo;
import neatlogic.framework.cmdb.dto.ci.CiViewVo;
import neatlogic.framework.cmdb.dto.ci.CiVo;
import neatlogic.framework.cmdb.dto.cientity.CiEntityVo;
import neatlogic.framework.cmdb.dto.cientity.RelCiEntityFilterVo;
import neatlogic.framework.cmdb.dto.cientity.SortVo;
import neatlogic.framework.cmdb.enums.CiAuthType;
import neatlogic.framework.cmdb.enums.ShowType;
import neatlogic.framework.cmdb.enums.group.GroupType;
import neatlogic.framework.cmdb.exception.ci.CiNotFoundException;
import neatlogic.framework.cmdb.utils.RelUtil;
import neatlogic.framework.common.constvalue.ApiParamType;
import neatlogic.framework.common.dto.BasePageVo;
import neatlogic.framework.exception.type.ParamNotExistsException;
import neatlogic.framework.restful.annotation.*;
import neatlogic.framework.restful.constvalue.OperationTypeEnum;
import neatlogic.framework.restful.core.privateapi.PrivateApiComponentBase;
import neatlogic.framework.util.$;
import neatlogic.module.cmdb.dao.mapper.ci.AttrMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiMapper;
import neatlogic.module.cmdb.dao.mapper.ci.CiViewMapper;
import neatlogic.module.cmdb.dsl.DslSearchManager;
import neatlogic.module.cmdb.service.ci.CiAuthChecker;
import neatlogic.module.cmdb.service.cientity.CiEntityService;
import neatlogic.module.cmdb.service.group.GroupService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityApi extends PrivateApiComponentBase implements ISearchCiEntityApiCrossoverService {//FIXME 内部暂时使用Crossover的方式调用该接口
    static final String IP_OBJECT = "IPObject";
    static final String PARENT = "parent";
    static final String CHILD = "child";

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

    @Resource
    private AttrMapper attrMapper;

    @Resource
    private GroupService groupService;


    @Override
    public String getToken() {
        return "/cmdb/cientity/search";
    }

    @Override
    public String getName() {
        return "nmcac.searchcientityapi.getname";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "term.cmdb.ciid"),
            @Param(name = "ciName", type = ApiParamType.STRING, desc = "term.cmdb.ciuniquename"),
            @Param(name = "name", type = ApiParamType.STRING, desc = "名称，用于匹配配置项名称"),
            @Param(name = "keyword", type = ApiParamType.STRING, desc = "common.keyword"),
            @Param(name = "keywordAsName", type = ApiParamType.BOOLEAN, desc = "关键字同时匹配配置项名称"),
            @Param(name = "dsl", type = ApiParamType.STRING, desc = "nmcac.searchcientitybydslapi.input.param.desc.dsl"),
            @Param(name = "groupId", type = ApiParamType.LONG, desc = "nmcac.searchcientityapi.input.param.desc.groupid"),
            @Param(name = "attrFilterList", type = ApiParamType.JSONARRAY, desc = "nmcac.exportcientityapi.input.param.desc.attrfilterlist"),
            @Param(name = "relFilterList", type = ApiParamType.JSONARRAY, desc = "nmcac.exportcientityapi.input.param.desc.relfilterlist"),
            @Param(name = "globalAttrFilterList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityapi.input.param.desc.globalattrfilterlist"),
            @Param(name = "showAttrRelList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityapi.input.param.desc.condition"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityapi.input.param.desc.idlist"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needaction"),
            @Param(name = "needCheck", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needcheck"),
            @Param(name = "needError", type = ApiParamType.BOOLEAN, desc = "是否需要异常列"),
            @Param(name = "needAccount", type = ApiParamType.BOOLEAN, desc = "是否需要返回账号"),
            @Param(name = "needExpand", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needexpand"),
            @Param(name = "needActionType", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.needactiontype"),
            @Param(name = "relCiEntityId", type = ApiParamType.LONG, desc = "nmcac.searchcientityapi.input.param.desc.relcientityid"),
            @Param(name = "relId", type = ApiParamType.LONG, desc = "nmcac.searchcientityapi.input.param.desc.relid"),
            @Param(name = "direction", type = ApiParamType.STRING, desc = "nmcac.searchcientityapi.input.param.desc.direction"),
            @Param(name = "mode", type = ApiParamType.ENUM, rule = "page,dialog", desc = "nmcac.searchcientityapi.input.param.desc.mode"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "common.pagesize"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "common.currentpage"),
            @Param(name = "globalAttrStrictMode", type = ApiParamType.BOOLEAN, desc = "全局属性严格模式，严格模式下不返回没有全局属性的配置项，只用在配置项搜索"),
            @Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchcientityapi.input.param.desc.cientitylist"),
            @Param(name = "attrId", type = ApiParamType.LONG, desc = "nmcac.searchcientityapi.input.param.desc.attrid"),
            @Param(name = "fromCiEntityId", type = ApiParamType.LONG, desc = "nmcac.searchcientityapi.input.param.desc.fromcientityid"),
            @Param(name = "sortConfig", type = ApiParamType.JSONOBJECT, desc = "nmcac.searchcientityapi.input.param.desc.sort"),
            @Param(name = "isAllColumn", type = ApiParamType.ENUM, rule = "0,1", desc = "nmcac.searchcientityapi.input.param.desc.isall"),
            @Param(name = "isLimitRelEntity", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.islimitrel"),
            @Param(name = "isLimitAttrEntity", type = ApiParamType.BOOLEAN, desc = "nmcac.searchcientityapi.input.param.desc.islimitattr")
    })
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "nmcac.searchattrtargetcientityapi.output.param.desc"),
//            @Param(name = "attrFilterList", type = ApiParamType.JSONARRAY, desc = "后端实际参与查询的属性过滤条件")
            })
    @Description(desc = "nmcac.searchcientityapi.getname")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String ciName = jsonObj.getString("ciName");
        String dsl = jsonObj.getString("dsl");
        Integer currentPage = jsonObj.getInteger("currentPage");
        Integer pageSize = jsonObj.getInteger("pageSize");

        if (ciId == null && StringUtils.isBlank(ciName)) {
            throw new ParamNotExistsException("ciId", "ciName");
        }
        if (ciId == null && StringUtils.isNotBlank(ciName)) {
            CiVo ciVo = ciMapper.getCiByName(ciName);
            if (ciVo == null) {
                throw new CiNotFoundException(ciName);
            }
            jsonObj.put("ciId", ciVo.getId());
        }
        CiEntityVo ciEntityVo = JSON.toJavaObject(jsonObj, CiEntityVo.class);

        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityVo.getCiId());
        Map<Long, AttrVo> attrMap = new HashMap<>();
        for (AttrVo attrVo : attrList) {
            attrMap.put(attrVo.getId(), attrVo);
        }
        // 引用表属性只接受合法的引用ID，避免空值或展示对象进入MyBatis过滤条件。
//        normalizeInvokeAttrFilterList(ciEntityVo, attrMap);
        JSONObject sortConfig = jsonObj.getJSONObject("sortConfig");
        if (MapUtils.isNotEmpty(sortConfig)) {
            List<SortVo> sortConfigList = new ArrayList<>();
            for (String key : sortConfig.keySet()) {
                AttrVo attrVo = attrMap.get(Long.parseLong(key.replace("attr_", "")));
                if (attrVo != null) {
                    IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
                    // 引用表属性没有可用于ORDER BY的动态字段。
                    if (handler != null && handler.isCanSort() && !handler.isInvokeAttr()) {
                        sortConfigList.add(new SortVo(attrVo.getCiId(), attrVo.getId(), sortConfig.getString(key)));
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(sortConfigList)) {
                ciEntityVo.setSortList(sortConfigList);
            }
        }
        Long groupId = jsonObj.getLong("groupId");
        boolean hasAuth = true;
        //FIXME:查看权限控制仍需斟酌，主要是考虑被引用的配置项列表如果没有权限是否允许查看，目前可控制左侧模型菜单显示，不做严格禁止
        if (!CiAuthChecker.chain().checkCiEntityQueryPrivilege(ciEntityVo.getCiId()).check()) {
            List<Long> groupIdList = groupService.getCurrentUserGroupIdList();
            if (groupId != null) {
                groupIdList.removeIf(g -> !g.equals(groupId));
            }
            if (CollectionUtils.isNotEmpty(groupIdList)) {
                ciEntityVo.setGroupIdList(groupIdList);
            } else {
                //throw new CiEntityAuthException(ciVo, "查看");
                hasAuth = false;
            }
        } else {
            if (groupId != null) {
                ciEntityVo.setGroupIdList(new ArrayList<Long>() {{
                    this.add(groupId);
                }});
            }
        }
        JSONObject returnObj = new JSONObject();
        if (!hasAuth) {
            CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
            returnObj.put("error", "您没有查看“" + ciVo.getLabel() + "(" + ciVo.getName() + ")”配置项的权限");
        } else {
            Long relCiEntityId = jsonObj.getLong("relCiEntityId");
            Long relId = jsonObj.getLong("relId");
            String direction = jsonObj.getString("direction");
            if (relCiEntityId != null && relId != null && StringUtils.isNotBlank(direction)) {
                List<RelCiEntityFilterVo> relCiEntityFilterList = new ArrayList<>();
                relCiEntityFilterList.add(new RelCiEntityFilterVo(relId, relCiEntityId, direction));
                ciEntityVo.setRelCiEntityFilterList(relCiEntityFilterList);
            }
            JSONArray ciEntityObjList = jsonObj.getJSONArray("ciEntityList");
            boolean needAction = jsonObj.getBooleanValue("needAction");
            boolean needCheck = jsonObj.getBooleanValue("needCheck");
            boolean needExpand = jsonObj.getBooleanValue("needExpand");
            boolean needError = jsonObj.getBooleanValue("needError");
            boolean needActionType = jsonObj.getBooleanValue("needActionType");
            boolean needAccount = jsonObj.getBooleanValue("needAccount");

            JSONArray showAttrRelList = jsonObj.getJSONArray("showAttrRelList");
            Set<String> showAttrRelSet = new HashSet<>();
            if (CollectionUtils.isNotEmpty(showAttrRelList)) {
                for (int i = 0; i < showAttrRelList.size(); i++) {
                    showAttrRelSet.add(showAttrRelList.getString(i));
                }
            }
            String mode = jsonObj.getString("mode");
            // 获取视图配置，只返回需要的属性和关系
            CiViewVo ciViewVo = new CiViewVo();
            ciViewVo.setCiId(ciEntityVo.getCiId());
            if (!Objects.equals(jsonObj.getInteger("isAllColumn"), 1)) {
                ciViewVo.addShowType(ShowType.LIST.getValue());
                ciViewVo.addShowType(ShowType.ALL.getValue());
            }
            List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo), ciEntityVo.getCiId());
            List<Long> attrIdList = null;
            List<Long> relIdList = null;
            List<Long> globalAttrIdList = null;
            JSONArray theadList = new JSONArray();
            if (needCheck) {
                // 增加复选列
                theadList.add(new JSONObject() {
                    {
                        this.put("key", "selection");
                    }
                });
            }
            if (needExpand) {
                // 增加下拉展开控制列
                theadList.add(new JSONObject() {
                    {
                        this.put("key", "expander");
                    }
                });
            }
            if (needError) {
                // 增加状态列
                theadList.add(new JSONObject() {
                    {
                        this.put("key", "_error");
                        this.put("title", "异常");
                    }
                });
            }
            if (needActionType) {
                // 增加复选列
                theadList.add(new JSONObject() {
                    {
                        this.put("key", "actionType");
                        this.put("title", $.t("common.actiontype"));
                    }
                });
            }
            JSONArray sortList = new JSONArray();
            if (CollectionUtils.isNotEmpty(ciViewList)) {
                attrIdList = new ArrayList<>();
                relIdList = new ArrayList<>();
                globalAttrIdList = new ArrayList<>();
                for (CiViewVo ciview : ciViewList) {
                    JSONObject headObj = new JSONObject();
                    headObj.put("title", ciview.getAliasOrLabel());
                    switch (ciview.getType()) {
                        case "attr":
                            if (CollectionUtils.isEmpty(showAttrRelSet) || showAttrRelSet.contains("attr_" + ciview.getItemId())) {
                                attrIdList.add(ciview.getItemId());
                                headObj.put("key", "attr_" + ciview.getItemId());
                                theadList.add(headObj);
                                AttrVo attrVo = attrMap.get(ciview.getItemId());
                                if (attrVo != null) {
                                    IAttrValueHandler handler = AttrValueHandlerFactory.getHandler(attrVo.getType());
                                    if (handler != null && handler.isCanSort()) {
                                        sortList.add("attr_" + attrVo.getId());
                                    }
                                }
                            }
                            break;
                        case "relfrom":
                            if (CollectionUtils.isEmpty(showAttrRelSet) || showAttrRelSet.contains("relfrom_" + ciview.getItemId())) {
                                relIdList.add(ciview.getItemId());
                                headObj.put("key", "relfrom_" + ciview.getItemId());
                                theadList.add(headObj);
                            }
                            break;
                        case "relto":
                            if (CollectionUtils.isEmpty(showAttrRelSet) || showAttrRelSet.contains("relto_" + ciview.getItemId())) {
                                relIdList.add(ciview.getItemId());
                                headObj.put("key", "relto_" + ciview.getItemId());
                                theadList.add(headObj);
                            }
                            break;
                        case "global":
                            if (CollectionUtils.isEmpty(showAttrRelSet) || showAttrRelSet.contains("global_" + ciview.getItemId())) {
                                globalAttrIdList.add(ciview.getItemId());
                                headObj.put("key", "global_" + ciview.getItemId());
                                theadList.add(headObj);
                            }
                            break;
                        case "const":
                            //固化属性需要特殊处理
                            headObj.put("key", "const_" + ciview.getItemName().replace("_", ""));
                            theadList.add(headObj);
                            break;
                        default:
                            break;
                    }
                }
            }

            if (needAction || !"dialog".equals(mode)) {
                // 增加操作列，无需判断needAction，因为有“查看详情”操作
                theadList.add(new JSONObject() {
                    {
                        this.put("key", "action");
                    }
                });
            }

            //把需要显示的属性和关系设进去，后台会进行自动过滤
            ciEntityVo.setAttrIdList(attrIdList);
            ciEntityVo.setRelIdList(relIdList);
            ciEntityVo.setGlobalAttrIdList(globalAttrIdList);
            ciEntityVo.setNeedAccount(needAccount);

            List<CiEntityVo> ciEntityList;
            CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
            if (ciEntityObjList == null) {
                if (StringUtils.isBlank(dsl)) {
                    ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
                } else {
                    DslSearchManager searchManager = DslSearchManager.build(ciEntityVo.getCiId(), dsl)
                            .withCurrentPage(currentPage).withPageSize(pageSize).withNeedRowCount(true);
                    if (CollectionUtils.isNotEmpty(ciEntityVo.getGroupIdList())) {
                        searchManager.withGroupIdList(ciEntityVo.getGroupIdList());
                    }
                    List<Long> idList = searchManager.search();
                    ciEntityVo.setIdList(idList);
                    ciEntityVo.setRowNum(searchManager.getRowNum());
                    ciEntityVo.setPageCount(searchManager.getPageCount());
                    ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
                }
            } else {
                ciEntityList = new ArrayList<>();
                for (int i = 0; i < ciEntityObjList.size(); i++) {
                    JSONObject obj = ciEntityObjList.getJSONObject(i);
                    CiEntityVo entity = JSON.toJavaObject(obj, CiEntityVo.class);
                    for (String k : obj.keySet().stream().filter(d -> d.startsWith("#")).collect(Collectors.toList())) {
                        entity.addSpecialParameter(k, obj.get(k));
                    }
                    ciEntityList.add(entity);
                }
            }
            JSONArray tbodyList = new JSONArray();
            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                boolean canEdit = false;
                boolean canDelete = false;
                boolean canViewPassword = false;
                boolean canTransaction = false;
                boolean hasResourceCenterAccountModify = false;
                List<Long> canAccountManagementIdList = new ArrayList<>();
                List<Long> hasMaintainCiEntityIdList = new ArrayList<>();
                List<Long> hasReadCiEntityIdList = new ArrayList<>();
                if (ciEntityObjList == null && needAction && ciVo.getIsVirtual().equals(0) /*&& ciVo.getIsAbstract().equals(0)*/) {
                    canEdit = CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciEntityVo.getCiId()).check();
                    canDelete = CiAuthChecker.chain().checkCiEntityDeletePrivilege(ciEntityVo.getCiId()).check();
                    canViewPassword = CiAuthChecker.chain().checkViewPasswordPrivilege(ciEntityVo.getCiId()).check();
                    canTransaction = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciEntityVo.getCiId()).check();
                    // 任意权限缺失，都需要检查是否在运维群组
                    if (!canEdit || !canDelete) {
                        if (CollectionUtils.isNotEmpty(ciEntityVo.getGroupIdList())) {
                            hasMaintainCiEntityIdList = CiAuthChecker.isCiEntityInGroup(ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()), GroupType.MAINTAIN);
                            hasReadCiEntityIdList = CiAuthChecker.isCiEntityInGroup(ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()), GroupType.READONLY);
                        }
                    }
                    // 前端页面显示“账号管理”按钮的条件是当前用户有“资源中心-账号管理权限”，且那行数据对应的配置项模型是IP软硬件模型或其后代模型
                    // 判断当前用户是否有“资源中心-账号管理权限”
                    hasResourceCenterAccountModify = AuthActionChecker.check(RESOURCECENTER_ACCOUNT_MODIFY.class);
                    if (hasResourceCenterAccountModify) {
                        // 获取IP软硬件配置项模型信息，如果找不到IP软硬件配置项模型信息，则所有行数据都不显示“账号管理”按钮
                        CiVo ipObjectCiVo = ciMapper.getCiByName(IP_OBJECT);
                        if (ipObjectCiVo != null) {
                            // 判断当前页的配置项模型与IP软硬件配置项模型之间的关系
                            // 1.当前页的配置项模型是IP软硬件配置项模型的祖先，这种情况需要进一步逐行判断那行数据对应的配置项模型是不是IP软硬件模型或其后代模型，如果是，则显示“账号管理”按钮，否则不显示“账号管理”按钮
                            // 2.当前页的配置项模型是IP软硬件配置项模型的后代，这种情况所有行数据都显示“账号管理”按钮
                            // 3.其他情况所有行数据都不显示“账号管理”按钮
                            String result = judgmentRelation(ipObjectCiVo, ciVo);
                            Map<Long, Long> ciEntityIdToCiIdMap = ciEntityList.stream().collect(Collectors.toMap(CiEntityVo::getId, CiEntityVo::getCiId));
                            if (Objects.equals(result, PARENT)) {
                                List<CiVo> rowCiVoList = ciMapper.getCiByIdList(new ArrayList<>(ciEntityIdToCiIdMap.values()));
                                Map<Long, CiVo> rowCiVoMap = rowCiVoList.stream().collect(Collectors.toMap(CiVo::getId, e -> e));
                                for (Map.Entry<Long, Long> entry : ciEntityIdToCiIdMap.entrySet()) {
                                    CiVo rowCiVo = rowCiVoMap.get(entry.getValue());
                                    if (rowCiVo == null) {
                                        continue;
                                    }
                                    String rowResult = judgmentRelation(ipObjectCiVo, rowCiVo);
                                    if (Objects.equals(rowResult, CHILD)) {
                                        canAccountManagementIdList.add(entry.getKey());
                                    }
                                }
                            } else if (Objects.equals(result, CHILD)) {
                                canAccountManagementIdList.addAll(ciEntityIdToCiIdMap.keySet());
                            }
                        }
                    }
                }
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                for (CiEntityVo entity : ciEntityList) {
                    JSONObject entityObj = new JSONObject();
                    entityObj.put("id", entity.getId());
                    entityObj.put("uuid", entity.getUuid());
                    entityObj.put("name", entity.getName());
                    entityObj.put("ciId", entity.getCiId());
                    entityObj.put("rootCiId", entity.getRootCiId());
                    entityObj.put("ciName", entity.getCiName());
                    entityObj.put("ciIcon", entity.getCiIcon());
                    entityObj.put("ciLabel", entity.getCiLabel());
                    entityObj.put("type", entity.getTypeId());
                    entityObj.put("typeName", entity.getTypeName());
                    entityObj.put("inspectTime", entity.getInspectTime() != null ? sdf.format(entity.getInspectTime()) : null);
                    entityObj.put("inspectStatus", makeupStatus(entity.getInspectStatus()));
                    entityObj.put("monitorTime", entity.getMonitorTime() != null ? sdf.format(entity.getMonitorTime()) : null);
                    entityObj.put("monitorStatus", makeupStatus(entity.getMonitorStatus()));
                    entityObj.put("renewTime", entity.getRenewTime() != null ? sdf.format(entity.getRenewTime()) : null);
                    entityObj.put("actionType", entity.getActionType());
                    entityObj.put("attrEntityData", entity.getAttrEntityData());
                    entityObj.put("globalAttrEntityData", entity.getGlobalAttrEntityData());
                    entityObj.put("relEntityData", entity.getRelEntityData());
                    entityObj.put("maxRelEntityCount", entity.getMaxRelEntityCount());
                    entityObj.put("maxAttrEntityCount", entity.getMaxAttrEntityCount());
                    entityObj.put("account", entity.getAccount());
                    if (ciEntityObjList == null && needAction && ciVo.getIsVirtual().equals(0)) {
                        JSONObject actionData = new JSONObject();
                        actionData.put(CiAuthType.CIENTITYUPDATE.getValue(), canEdit || hasMaintainCiEntityIdList.contains(entity.getId()));
                        actionData.put(CiAuthType.CIENTITYDELETE.getValue(), canDelete || hasMaintainCiEntityIdList.contains(entity.getId()));
                        actionData.put(CiAuthType.PASSWORDVIEW.getValue(), canViewPassword || hasMaintainCiEntityIdList.contains(entity.getId()) || hasReadCiEntityIdList.contains(entity.getId()));
                        actionData.put(CiAuthType.TRANSACTIONMANAGE.getValue(), canTransaction || hasMaintainCiEntityIdList.contains(entity.getId()));
                        actionData.put(CiAuthType.ACCOUNTMANAGEMENT.getValue(), hasResourceCenterAccountModify && canAccountManagementIdList.contains(entity.getId()));
                        entityObj.put("authData", actionData);
                    } else if (ciEntityObjList != null && needAction) {
                        JSONObject actionData = new JSONObject();
                        //用于表单组件的判断，如果是更新或添加操作时才会出现编辑按钮
                        if (entityObj.getString("actionType") != null
                                && (entityObj.getString("actionType").equals("update")
                                || entityObj.getString("actionType").equals("insert"))) {
                            actionData.put(CiAuthType.CIENTITYUPDATE.getValue(), true);
                        } else {
                            actionData.put(CiAuthType.CIENTITYUPDATE.getValue(), false);
                        }
                        actionData.put(CiAuthType.CIENTITYDELETE.getValue(), true);
                        actionData.put(CiAuthType.PASSWORDVIEW.getValue(), true);
                        actionData.put(CiAuthType.TRANSACTIONMANAGE.getValue(), true);
                        entityObj.put("authData", actionData);
                    }
                    if (needAction) {
                        if (ciVo.getIsVirtual().equals(1)) {
                            entityObj.put("isDisabled", true);//禁用前端复选框
                        } else {
                            JSONObject actionData = entityObj.getJSONObject("authData");
                            if (MapUtils.isEmpty(actionData)) {
                                entityObj.put("isDisabled", true);//禁用前端复选框
                            } else {
                                if (!actionData.getBoolean(CiAuthType.CIENTITYUPDATE.getValue()) && !actionData.getBoolean(CiAuthType.CIENTITYDELETE.getValue())) {
                                    entityObj.put("isDisabled", true);//禁用前端复选框
                                }
                            }
                        }
                    }
                    if (MapUtils.isNotEmpty(entity.getSpecialParamterMap())) {
                        for (String key : entity.getSpecialParamterMap().keySet()) {
                            entityObj.put(key, entity.getSpecialParamterMap().get(key));
                        }
                    }
                    tbodyList.add(entityObj);
                }
            }
            returnObj.put("keywordList", ciEntityVo.getKeywordList());
            returnObj.put("pageSize", ciEntityVo.getPageSize());
            returnObj.put("pageCount", ciEntityVo.getPageCount());
            returnObj.put("rowNum", ciEntityVo.getRowNum());
            returnObj.put("currentPage", ciEntityVo.getCurrentPage());
            returnObj.put("tbodyList", tbodyList);
            returnObj.put("theadList", theadList);
            returnObj.put("sortList", sortList);
        }
        // 返回最终生效的条件，前端据此恢复复杂属性的多选值和搜索状态。
//        returnObj.put("attrFilterList", ciEntityVo.getAttrFilterList());
        return returnObj;
    }

    /**
     * 规范化保存到cmdb_attr_invoke表的属性过滤条件。
     * 等于条件仅保留合法的位置类型和正整数引用ID，空条件和不支持的表达式不参与查询；为空、不为空无需取值。
     *
     * @param ciEntityVo 配置项搜索条件
     * @param attrMap    当前模型可用属性
     */
//    private void normalizeInvokeAttrFilterList(CiEntityVo ciEntityVo, Map<Long, AttrVo> attrMap) {
//        if (CollectionUtils.isEmpty(ciEntityVo.getAttrFilterList())) {
//            return;
//        }
//        Iterator<AttrFilterVo> iterator = ciEntityVo.getAttrFilterList().iterator();
//        while (iterator.hasNext()) {
//            AttrFilterVo attrFilterVo = iterator.next();
//            AttrVo attrVo = attrMap.get(attrFilterVo.getAttrId());
//            IAttrValueHandler handler = attrVo == null ? null : AttrValueHandlerFactory.getHandler(attrVo.getType());
//            if (handler == null || !handler.isInvokeAttr()) {
//                continue;
//            }
//            String expression = attrFilterVo.getExpression();
//            if (Objects.equals(expression, SearchExpression.NULL.getExpression())
//                    || Objects.equals(expression, SearchExpression.NOTNULL.getExpression())) {
//                // 空值判断由引用记录是否存在决定，不应携带位置ID。
//                attrFilterVo.setValueList(new JSONArray());
//                continue;
//            }
//            if (!Objects.equals(expression, SearchExpression.EQ.getExpression())) {
//                iterator.remove();
//                continue;
//            }
//            JSONArray invokeValueList = normalizeInvokeValueList(attrFilterVo.getValueList());
//            if (CollectionUtils.isEmpty(invokeValueList)) {
//                iterator.remove();
//            } else {
//                attrFilterVo.setValueList(invokeValueList);
//            }
//        }
//        System.out.println("ciEntityVo.getAttrFilterList() = " + JSONObject.toJSONString(ciEntityVo.getAttrFilterList()));
//    }

    /**
     * 校验机房位置多选值，并统一为可供MyBatis绑定的类型和引用ID对象。
     *
     * @param valueList 前端结构化属性值列表
     * @return 去重后的合法机房位置引用列表
     */
//    private JSONArray normalizeInvokeValueList(JSONArray valueList) {
//        JSONArray invokeValueList = new JSONArray();
//        if (CollectionUtils.isEmpty(valueList)) {
//            return invokeValueList;
//        }
//        Set<String> invokeValueSet = new LinkedHashSet<>();
//        for (Object value : valueList) {
//            // 兼容FastJSON将通用Map反序列化为对象的场景，标量值仍按新契约丢弃。
//            Object jsonValue = JSON.toJSON(value);
//            if (!(jsonValue instanceof JSONObject)) {
//                continue;
//            }
//            JSONObject invokeValue = (JSONObject) jsonValue;
//            String type = normalizeInvokeType(invokeValue.getString("type"));
//            String invokeIdText = invokeValue.getString("invokeId");
//            if (StringUtils.isBlank(type) || StringUtils.isBlank(invokeIdText)) {
//                continue;
//            }
//            try {
//                long invokeId = Long.parseLong(invokeIdText.trim());
//                String uniqueKey = type.toLowerCase(Locale.ROOT) + ":" + invokeId;
//                if (invokeId > 0L && invokeValueSet.add(uniqueKey)) {
//                    // invokeId按字符串返回，避免JavaScript大整数精度丢失导致树节点无法回显。
//                    invokeValueList.add(new JSONObject().fluentPut("type", type).fluentPut("invokeId", Long.toString(invokeId)));
//                }
//            } catch (NumberFormatException ignored) {
//                // 非法引用ID不能进入cmdb_attr_invoke查询条件。
//            }
//        }
//        return invokeValueList;
//    }

    /**
     * 将机房位置层级名称统一为前后端约定的引用类型。
     *
     * @param type 前端位置类型
     * @return 合法引用类型，非法类型返回null
     */
//    private String normalizeInvokeType(String type) {
//        if (StringUtils.isBlank(type)) {
//            return null;
//        }
//        switch (type.trim().toLowerCase(Locale.ROOT)) {
//            case "datacenter":
//                return "datacenter";
//            case "computerroom":
//                return "computerRoom";
//            case "cabinet":
//                return "cabinet";
//            default:
//                return null;
//        }
//    }

    /**
     * 判断配置项之间的父子兄弟关系
     *
     * @param ipObjectCiVo IP软硬件配置项信息
     * @param ciVo         其他配置项信息
     * @return 返回结果
     */
    private String judgmentRelation(CiVo ipObjectCiVo, CiVo ciVo) {
        if (ipObjectCiVo.getLft() != null && ipObjectCiVo.getRht() != null && ciVo.getLft() != null && ciVo.getRht() != null) {
            if (ipObjectCiVo.getLft() <= ciVo.getLft() && ipObjectCiVo.getRht() >= ciVo.getRht()) {
                return CHILD;
            } else if (ipObjectCiVo.getLft() >= ciVo.getLft() && ipObjectCiVo.getRht() <= ciVo.getRht()) {
                return PARENT;
            }
        }
        return null;
    }

    private String makeupStatus(String status) {
        if (StringUtils.isNotBlank(status)) {
            switch (status) {
                case "fatal":
                    return "<span class=\"text-error\">" + status.toUpperCase(Locale.ROOT) + "</span>";
                case "warn":
                    return "<span class=\"text-warning\">" + status.toUpperCase(Locale.ROOT) + "</span>";
                case "critical":
                    return "<span class=\"text-error\">" + status.toUpperCase(Locale.ROOT) + "</span>";
                default:
                    return "<span class=\"text-success\">" + status.toUpperCase(Locale.ROOT) + "</span>";
            }
        }
        return "";
    }
}
