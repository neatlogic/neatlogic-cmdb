package codedriver.module.cmdb.api.cientity;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import codedriver.framework.asynchronization.threadlocal.UserContext;
import codedriver.framework.auth.core.AuthActionChecker;
import codedriver.framework.cmdb.constvalue.GroupType;
import codedriver.framework.cmdb.constvalue.ShowType;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.common.dto.BasePageVo;
import codedriver.framework.dao.mapper.TeamMapper;
import codedriver.framework.reminder.core.OperationTypeEnum;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Output;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.dao.mapper.ci.CiViewMapper;
import codedriver.module.cmdb.dao.mapper.group.GroupMapper;
import codedriver.module.cmdb.dto.ci.CiViewVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.service.ci.CiAuthService;
import codedriver.module.cmdb.service.cientity.CiEntityService;

@Service
@OperationType(type = OperationTypeEnum.SEARCH)
public class SearchCiEntityApi extends PrivateApiComponentBase {

    @Autowired
    private CiEntityService ciEntityService;

    @Autowired
    private CiAuthService ciAuthService;

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

    @SuppressWarnings("serial")
    @Input({@Param(name = "ciId", type = ApiParamType.LONG, isRequired = true, desc = "模型id"),
        @Param(name = "needCheckAuth", type = ApiParamType.BOOLEAN, desc = "是否需要检查操作权限，如果需要检查操作权限，会根据结果返回action列")})
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
            hasManageAuth = ciAuthService.hasCiManagePrivilege(ciEntityVo.getCiId());
        }

        boolean needCheckAuth = jsonObj.getBooleanValue("needCheckAuth");

        // 获取视图配置，只返回需要的属性和关系
        CiViewVo ciViewVo = new CiViewVo();
        ciViewVo.setCiId(ciEntityVo.getCiId());
        ciViewVo.addShowType(ShowType.LIST.getValue());
        ciViewVo.addShowType(ShowType.ALL.getValue());
        List<CiViewVo> ciViewList = ciViewMapper.getCiViewByCiId(ciViewVo);
        List<Long> attrIdList = null, relIdList = null;
        JSONArray theadList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciViewList)) {
            attrIdList = new ArrayList<>();
            relIdList = new ArrayList<>();
            for (CiViewVo ciview : ciViewList) {
                JSONObject headObj = new JSONObject();
                headObj.put("title", ciview.getItemName());
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
            if (needCheckAuth) {
                // 增加操作列
                theadList.add(new JSONObject() {
                    {
                        this.put("key", "action");
                    }
                });
            }
        }

        ciEntityVo.setAttrIdList(attrIdList);
        ciEntityVo.setRelIdList(relIdList);

        if (!hasManageAuth && !ciAuthService.hasCiEntityQueryPrivilege(ciEntityVo.getCiId())) {
            // 没有模型维护权限并且没有配置项查询权限，则需要通过消费组或维护组进行过滤
            String userUuid = UserContext.get().getUserUuid(true);
            List<String> teamUuidList = teamMapper.getTeamUuidListByUserUuid(userUuid);
            List<String> roleUuidList = UserContext.get().getRoleUuidList();
            List<Long> groupIdList = groupMapper.getGroupIdByUserUuid(userUuid, teamUuidList, roleUuidList);
            ciEntityVo.setGroupIdList(groupIdList);
        }

        List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
        JSONArray tbodyList = new JSONArray();
        if (CollectionUtils.isNotEmpty(ciEntityList)) {
            boolean canEdit = hasManageAuth, canDelete = hasManageAuth, canTransaction = hasManageAuth;
            List<Long> hasAuthCiEntityIdList = new ArrayList<>();
            if (needCheckAuth) {
                canEdit = !canEdit ? ciAuthService.hasCiEntityUpdatePrivilege(ciEntityVo.getCiId()) : canEdit;
                canDelete = !canDelete ? ciAuthService.hasCiEntityDeletePrivilege(ciEntityVo.getCiId()) : canDelete;
                canTransaction =
                    !canTransaction ? ciAuthService.hasTransactionPrivilege(ciEntityVo.getCiId()) : canTransaction;
                // 任意权限缺失，都需要检查是否在运维群组
                if (!canEdit || !canDelete || !canTransaction) {
                    if (CollectionUtils.isNotEmpty(ciEntityVo.getGroupIdList())) {
                        hasAuthCiEntityIdList = ciAuthService.isInGroup(
                            ciEntityList.stream().map(entity -> entity.getId()).collect(Collectors.toList()),
                            GroupType.MATAIN);
                    }
                }
            }

            for (CiEntityVo entity : ciEntityList) {
                JSONObject entityObj = new JSONObject();
                entityObj.put("id", entity.getId());
                entityObj.put("attrEntityData", entity.getAttrEntityData());
                entityObj.put("relEntityData", entity.getRelEntityData());
                if (needCheckAuth) {
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
