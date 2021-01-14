package codedriver.module.cmdb.api.cientity;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.GroupType;
import codedriver.framework.cmdb.constvalue.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import codedriver.module.cmdb.dto.ci.CiViewVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.service.ci.CiAuthChecker;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiViewMapper ciViewMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private GroupMapper groupMapper;

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
        // TODO Auto-generated method stub
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
        boolean hasManageAuth = AuthActionChecker.check("CI_MODIFY", "CIENTITY_MODIFY");
        if (!hasManageAuth) {
            // 拥有模型管理权限查询所有配置项
            hasManageAuth = CiAuthChecker.hasCiManagePrivilege(ciEntityVo.getCiId());
        }

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
                if (ciview.getType().equals("attr")) {
                    attrIdList.add(ciview.getItemId());
                    headObj.put("key", "attr_" + ciview.getItemId());
                } else if (ciview.getType().equals("relfrom")) {
                    relIdList.add(ciview.getItemId());
                    headObj.put("key", "relfrom_" + ciview.getItemId());
                } else {
                    relIdList.add(ciview.getItemId());
                    headObj.put("key", "relto_" + ciview.getItemId());
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

        if (!hasManageAuth && !CiAuthChecker.hasCiEntityQueryPrivilege(ciEntityVo.getCiId())) {
            // 没有模型维护权限并且没有配置项查询权限，则需要通过消费组或维护组进行过滤
            String userUuid = UserContext.get().getUserUuid(true);
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<Long> groupIdList = groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
            ciEntityVo.setGroupIdList(groupIdList);
        }

        List<CiEntityVo> ciEntityList = new ArrayList<>();
       /* if (CollectionUtils.isNotEmpty(ciEntityVo.getAttrFilterList())
            || CollectionUtils.isNotEmpty(ciEntityVo.getAttrFilterList())) {
            IElasticSearchHandler<CiEntityVo, List<CiEntityVo>> handler =
                ElasticSearchHandlerFactory.getHandler("cientity");
            ciEntityList = handler.search(ciEntityVo);
        } else {*/
            ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        //}
        JSONArray tbodyList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            boolean canEdit = hasManageAuth, canDelete = hasManageAuth, canTransaction = hasManageAuth;
            List<Long> hasAuthCiEntityIdList = new ArrayList<>();
            if (needAction) {
                canEdit = !canEdit ? CiAuthChecker.hasCiEntityUpdatePrivilege(ciEntityVo.getCiId()) : canEdit;
                canDelete = !canDelete ? CiAuthChecker.hasCiEntityDeletePrivilege(ciEntityVo.getCiId()) : canDelete;
                canTransaction =
                    !canTransaction ? CiAuthChecker.hasTransactionPrivilege(ciEntityVo.getCiId()) : canTransaction;
                // 任意权限缺失，都需要检查是否在运维群组
                if (!canEdit || !canDelete || !canTransaction) {
                    if (CollectionUtils.isNotEmpty(ciEntityVo.getGroupIdList())) {
                        hasAuthCiEntityIdList = CiAuthChecker.isInGroup(
                                ciEntityList.stream().map(CiEntityVo::getId).collect(Collectors.toList()),
                                GroupType.MATAIN);
                    }
                }
            }

            for (CiEntityVo entity : ciEntityList) {
                JSONObject entityObj = new JSONObject();
                entityObj.put("id", entity.getId());
                entityObj.put("name", entity.getName());
                entityObj.put("attrEntityData", entity.getAttrEntityData());
                entityObj.put("relEntityData", entity.getRelEntityData());
                if (needAction) {
                    JSONObject actionData = new JSONObject();
                    if (canEdit) {
                        actionData.put("canEdit", true);
                    } else {
                        // 如果模型没有权限，则根据是否在运维群组授权
                        actionData.put("canEdit", hasAuthCiEntityIdList.contains(entity.getId()));
                    }
                    if (canDelete) {
                        actionData.put("canDelete", true);
                    } else {
                        // 如果模型没有权限，则根据是否在运维群组授权
                        actionData.put("canDelete", hasAuthCiEntityIdList.contains(entity.getId()));
                    }
                    if (canTransaction) {
                        actionData.put("canTransaction", true);
                    } else {
                        // 如果模型没有权限，则根据是否在运维群组授权
                        actionData.put("canTransaction", hasAuthCiEntityIdList.contains(entity.getId()));
                    }
                    entityObj.put("actionData", actionData);
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
