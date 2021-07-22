/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.cientity;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.ci.CiViewVo;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.enums.CiAuthType;
import codedriver.framework.cmdb.enums.GroupType;
import codedriver.framework.cmdb.enums.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CIENTITY_MODIFY;
import codedriver.module.cmdb.auth.label.CI_MODIFY;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import codedriver.module.cmdb.service.group.GroupService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AuthAction(action = CMDB_BASE.class)
@AuthAction(action = CI_MODIFY.class)
@AuthAction(action = CIENTITY_MODIFY.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityApi extends PrivateApiComponentBase {

    @Resource
    private CiEntityService ciEntityService;

    @Resource
    private CiViewMapper ciViewMapper;

    @Resource
    private CiMapper ciMapper;

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

    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
            @Param(name = "keyword", type = ApiParamType.STRING, xss = true, desc = "关键字"),
            @Param(name = "idList", type = ApiParamType.JSONARRAY, desc = "需要查询的配置项id列表）"),
            @Param(name = "needAction", type = ApiParamType.BOOLEAN, desc = "是否需要操作列，如果需要则根据用户权限返回操作列"),
            @Param(name = "needCheck", type = ApiParamType.BOOLEAN, desc = "是否需要复选列")})
    @Output({@Param(explode = BasePageVo.class),
            @Param(name = "tbodyList", type = ApiParamType.JSONARRAY, explode = CiEntityVo[].class),
            @Param(name = "theadList", type = ApiParamType.JSONARRAY, desc = "表头信息")})
    @Description(desc = "查询配置项接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        CiEntityVo ciEntityVo = JSONObject.toJavaObject(jsonObj, CiEntityVo.class);
        /*FIXME:查看权限控制仍需斟酌，主要是考虑被引用的配置项列表如果没有权限是否允许查看，目前可控制左侧模型菜单显示，不做严格禁止
        /*if (!CiAuthChecker.chain().checkCiEntityQueryPrivilege(ciEntityVo.getCiId()).check()) {
            List<Long> groupIdList = groupService.getCurrentUserGroupIdList();
            if (CollectionUtils.isNotEmpty(groupIdList)) {
                ciEntityVo.setGroupIdList(groupIdList);
            } else {
                throw new CiEntityAuthException("查看");
            }
        }*/

        boolean needAction = jsonObj.getBooleanValue("needAction");
        boolean needCheck = jsonObj.getBooleanValue("needCheck");
        // 获取视图配置，只返回需要的属性和关系
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciEntityVo.getCiId());
        ciViewVo.addShowType(ShowType.LIST.getValue());
        ciViewVo.addShowType(ShowType.ALL.getValue());
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
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
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            attrIdList = new ArrayList<>();
            relIdList = new ArrayList<>();
            for (CiViewVo ciview : ciViewList) {
                JSONObject headObj = new JSONObject();
                headObj.put("title", ciview.getItemLabel());
                switch (ciview.getType()) {
                    case "attr":
                        attrIdList.add(ciview.getItemId());
                        headObj.put("key", "attr_" + ciview.getItemId());
                        break;
                    case "relfrom":
                        relIdList.add(ciview.getItemId());
                        headObj.put("key", "relfrom_" + ciview.getItemId());
                        break;
                    case "relto":
                        relIdList.add(ciview.getItemId());
                        headObj.put("key", "relto_" + ciview.getItemId());
                        break;
                    case "const":
                        //固化属性需要特殊处理
                        headObj.put("key", "const_" + ciview.getItemName().replace("_", ""));
                        break;
                }
                theadList.add(headObj);
            }
            // if (needAction) {
            // 增加操作列，无需判断needAction，因为有“查看详情”操作
            theadList.add(new JSONObject() {
                {
                    this.put("key", "action");
                }
            });
            // }
        }

        ciEntityVo.setAttrIdList(attrIdList);
        ciEntityVo.setRelIdList(relIdList);


        List<CiEntityVo> ciEntityList;
        CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
        ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        JSONArray tbodyList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            boolean canEdit = false, canDelete = false, canViewPassword = false, canTransaction = false;
            List<Long> hasMaintainCiEntityIdList = new ArrayList<>();
            List<Long> hasReadCiEntityIdList = new ArrayList<>();
            if (needAction && ciVo.getIsVirtual().equals(0) && ciVo.getIsAbstract().equals(0)) {
                canEdit = CiAuthChecker.chain().checkCiEntityUpdatePrivilege(ciEntityVo.getCiId()).check();
                canDelete = CiAuthChecker.chain().checkCiEntityDeletePrivilege(ciEntityVo.getCiId()).check();
                canViewPassword = CiAuthChecker.chain().checkViewPasswordPrivilege(ciEntityVo.getCiId()).check();
                canTransaction = CiAuthChecker.chain().checkCiEntityTransactionPrivilege(ciEntityVo.getCiId()).check();
                // 任意权限缺失，都需要检查是否在运维群组
                if (!canEdit || !canDelete) {
                    if (CollectionUtils.isNotEmpty(ciEntityVo.getGroupIdList())) {
                        hasMaintainCiEntityIdList = CiAuthChecker.isInGroup(ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()), GroupType.MAINTAIN);
                        hasReadCiEntityIdList = CiAuthChecker.isInGroup(ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()), GroupType.READONLY);
                    }
                }
            }

            for (CiEntityVo entity : ciEntityList) {
                JSONObject entityObj = new JSONObject();
                entityObj.put("id", entity.getId());
                entityObj.put("name", entity.getName());
                entityObj.put("ciId", entity.getCiId());
                entityObj.put("ciName", entity.getCiName());
                entityObj.put("ciLabel", entity.getCiLabel());
                entityObj.put("type", entity.getTypeId());
                entityObj.put("typeName", entity.getTypeName());
                entityObj.put("attrEntityData", entity.getAttrEntityData());
                entityObj.put("relEntityData", entity.getRelEntityData());
                if (needAction && ciVo.getIsVirtual().equals(0)) {
                    JSONObject actionData = new JSONObject();
                    actionData.put(CiAuthType.CIENTITYUPDATE.getValue(), canEdit || hasMaintainCiEntityIdList.contains(entity.getId()));
                    actionData.put(CiAuthType.CIENTITYDELETE.getValue(), canDelete || hasMaintainCiEntityIdList.contains(entity.getId()));
                    actionData.put(CiAuthType.PASSWORDVIEW.getValue(), canViewPassword || hasMaintainCiEntityIdList.contains(entity.getId()) || hasReadCiEntityIdList.contains(entity.getId()));
                    actionData.put(CiAuthType.TRANSACTIONMANAGE.getValue(), canTransaction || hasMaintainCiEntityIdList.contains(entity.getId()));
                    entityObj.put("authData", actionData);
                }
                tbodyList.add(entityObj);
            }
        }
        JSONObject returnObj = new JSONObject();
        returnObj.put("pageSize", ciEntityVo.getPageSize());
        returnObj.put("pageCount", ciEntityVo.getPageCount());
        returnObj.put("rowNum", ciEntityVo.getRowNum());
        returnObj.put("currentPage", ciEntityVo.getCurrentPage());
        returnObj.put("tbodyList", tbodyList);
        returnObj.put("theadList", theadList);
        return returnObj;
    }

}
