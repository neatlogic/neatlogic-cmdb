/*
 * Copyright(c) 2022 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.attrvaluehandler.core.AttrValueHandlerFactory;
import codedriver.framework.cmdb.attrvaluehandler.core.IAttrValueHandler;
import codedriver.framework.cmdb.crossover.ISearchCiEntityApiCrossoverService;
import codedriver.framework.cmdb.dto.ci.AttrVo;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.cientity.RelCiEntityFilterVo;
import codedriver.framework.cmdb.dto.cientity.SortVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.cmdb.enums.group.GroupType;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.cmdb.utils.RelUtil;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.service.group.GroupService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityApi extends PrivateApiComponentBase implements ISearchCiEntityApiCrossoverService {//FIXME 内部暂时使用Crossover的方式调用该接口

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
        return "查询配置项";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, desc = "模型id"),
            @Param(name = "ciName", type = ApiParamType.STRING, desc = "模型唯一标识"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键字"),
            @Param(name = "dsl", type = ApiParamType.STRING, desc = "DSL语句"),
            @Param(name = "groupId", type = ApiParamType.LONG, desc = "团体id"),
            @Param(name = "attrFilterList", type = ApiParamType.JSONARRAY, desc = "属性过滤条件"),
            @Param(name = "relFilterList", type = ApiParamType.JSONARRAY, desc = "关系过滤条件"),
            @Param(name = "showAttrRelList", type = ApiParamType.JSONARRAY, desc = "需要显示的字段列表，包括属性关系和常量"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "需要查询的配置项id列表）"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要则根据用户权限返回操作列"),
            @Param(name = "needCheck", type = ApiParamType.BOOLEAN, desc = "是否需要复选列"),
            @Param(name = "needExpand", type = ApiParamType.BOOLEAN, desc = "是否需要显示展开控制列"),
            @Param(name = "needActionType", type = ApiParamType.BOOLEAN, desc = "是否需要操作类型列，一般在表单控件中使用，用于标记数据是新增还是修改还是删除"),
            @Param(name = "relCiEntityId", type = ApiParamType.LONG, desc = "关系配置项id"),
            @Param(name = "relId", type = ApiParamType.LONG, desc = "关系id"),
            @Param(name = "direction", type = ApiParamType.STRING, desc = "当前模型在关系中的位置"),
            @Param(name = "mode", type = ApiParamType.ENUM, rule = "page,dialog", desc = "dialog模式不会显示详情连接"),
            @Param(name = "pageSize", type = ApiParamType.INTEGER, desc = "每页大小"),
            @Param(name = "currentPage", type = ApiParamType.INTEGER, desc = "当前页"),
            @Param(name = "ciEntityList", type = ApiParamType.JSONARRAY, desc = "配置项结果集，如果提供则不会进行搜索，补充头部信息后直接返回"),
            @Param(name = "attrId", type = ApiParamType.LONG, desc = "关系id（通过引用配置项查询引用属性时使用）"),
            @Param(name = "fromCiEntityId", type = ApiParamType.LONG, desc = "引用配置项id（通过引用配置项查询引用属性时使用）"),
            @Param(name = "sortConfig", type = ApiParamType.JSONOBJECT, desc = "排序规则，范例：{\"attr_xxxxx\":\"DESC\",\"attr_yyyyy\":\"ASC\"}"),
            @Param(name = "isAllColumn", type = ApiParamType.ENUM, rule = "0,1", desc = "是否返回所有列数据"),
            @Param(name = "isLimitRelEntity", type = ApiParamType.BOOLEAN, desc = "是否限制返回的关系数据"),
            @Param(name = "isLimitAttrEntity", type = ApiParamType.BOOLEAN, desc = "是否限制返回的引用属性数据")
    })
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "查询配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        Long ciId = jsonObj.getLong("ciId");
        String ciName = jsonObj.getString("ciName");
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
        CiEntityVo ciEntityVo = JSONObject.toJavaObject(jsonObj, CiEntityVo.class);
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityVo.getCiId());
        Map<Long, AttrVo> attrMap = new HashMap<>();
        for (AttrVo attrVo : attrList) {
            attrMap.put(attrVo.getId(), attrVo);
        }
        JSONObject sortConfig = jsonObj.getJSONObject("sortConfig");
        if (MapUtils.isNotEmpty(sortConfig)) {
            List<SortVo> sortConfigList = new ArrayList<>();
            for (String key : sortConfig.keySet()) {
                AttrVo attrVo = attrMap.get(Long.parseLong(key.replace("attr_", "")));
                if (attrVo != null) {
                    sortConfigList.add(new SortVo(attrVo.getCiId(), attrVo.getId(), sortConfig.getString(key)));
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
            boolean needActionType = jsonObj.getBooleanValue("needActionType");
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
            List<CiViewVo> ciViewList = RelUtil.ClearCiViewRepeatRel(ciViewMapper.getCiViewByCiId(ciViewVo));
            List<Long> attrIdList = null, relIdList = null;
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
            if (needActionType) {
                // 增加复选列
                theadList.add(new JSONObject() {
                    {
                        this.put("key", "actionType");
                        this.put("title", "操作类型");
                    }
                });
            }
            JSONArray sortList = new JSONArray();
            if (CollectionUtils.isNotEmpty(ciViewList)) {
                attrIdList = new ArrayList<>();
                relIdList = new ArrayList<>();
                for (CiViewVo ciview : ciViewList) {
                    JSONObject headObj = new JSONObject();
                    headObj.put("title", ciview.getItemLabel());
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
                        case "const":
                            //固化属性需要特殊处理
                            headObj.put("key", "const_" + ciview.getItemName().replace("_", ""));
                            theadList.add(headObj);
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

            List<CiEntityVo> ciEntityList;
            CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
            if (ciEntityObjList == null) {
                ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
            } else {
                ciEntityList = new ArrayList<>();
                for (int i = 0; i < ciEntityObjList.size(); i++) {
                    ciEntityList.add(JSONObject.toJavaObject(ciEntityObjList.getJSONObject(i), CiEntityVo.class));
                }
            }
            JSONArray tbodyList = new JSONArray();
            if (CollectionUtils.isNotEmpty(ciEntityList)) {
                boolean canEdit = false, canDelete = false, canViewPassword = false, canTransaction = false;
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
                    entityObj.put("relEntityData", entity.getRelEntityData());
                    entityObj.put("maxRelEntityCount", entity.getMaxRelEntityCount());
                    entityObj.put("maxAttrEntityCount", entity.getMaxAttrEntityCount());
                    if (ciEntityObjList == null && needAction && ciVo.getIsVirtual().equals(0)) {
                        JSONObject actionData = new JSONObject();
                        actionData.put(CiAuthType.CIENTITYUPDATE.getValue(), canEdit || hasMaintainCiEntityIdList.contains(entity.getId()));
                        actionData.put(CiAuthType.CIENTITYDELETE.getValue(), canDelete || hasMaintainCiEntityIdList.contains(entity.getId()));
                        actionData.put(CiAuthType.PASSWORDVIEW.getValue(), canViewPassword || hasMaintainCiEntityIdList.contains(entity.getId()) || hasReadCiEntityIdList.contains(entity.getId()));
                        actionData.put(CiAuthType.TRANSACTIONMANAGE.getValue(), canTransaction || hasMaintainCiEntityIdList.contains(entity.getId()));
                        entityObj.put("authData", actionData);
                    } else if (ciEntityObjList != null && needAction) {
                        JSONObject actionData = new JSONObject();
                        //用于表单组件的判断，如果是更新或添加操作时才会出现编辑按钮
                        if (entityObj.containsKey("actionType")
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
                    tbodyList.add(entityObj);
                }
            }
            returnObj.put("pageSize", ciEntityVo.getPageSize());
            returnObj.put("pageCount", ciEntityVo.getPageCount());
            returnObj.put("rowNum", ciEntityVo.getRowNum());
            returnObj.put("currentPage", ciEntityVo.getCurrentPage());
            returnObj.put("tbodyList", tbodyList);
            returnObj.put("theadList", theadList);
            returnObj.put("sortList", sortList);
        }
        return returnObj;
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
