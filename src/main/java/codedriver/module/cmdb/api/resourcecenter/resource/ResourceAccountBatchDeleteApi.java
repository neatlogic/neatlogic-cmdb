/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterAccountNotFoundException;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceNotFoundException;
import codedriver.framework.common.constvalue.ApiParamType;
import codedriver.framework.exception.type.ParamNotExistsException;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.Input;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.annotation.Param;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/22 15:57
 **/
@Service
@Transactional
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceAccountBatchDeleteApi extends PrivateApiComponentBase {

    @Resource
    private ResourceMapper resourceMapper;
    @Resource
    private ResourceAccountMapper resourceAccountMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/account/batch/delete";
    }

    @Override
    public String getName() {
        return "批量删除资源账号";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "资源id列表"),
            @Param(name = "accountIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "账号id列表")
    })
    @Description(desc = "批量删除资源账号")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray resourceIdArray = paramObj.getJSONArray("resourceIdList");
        if (CollectionUtils.isEmpty(resourceIdArray)) {
            throw new ParamNotExistsException("resourceIdList");
        }
        JSONArray accountIdArray = paramObj.getJSONArray("accountIdList");
        if (CollectionUtils.isEmpty(accountIdArray)) {
            throw new ParamNotExistsException("accountIdList");
        }

        List<Long> resourceIdList = resourceIdArray.toJavaList(Long.class);
        List<Long> existResourceIdList = resourceMapper.checkResourceIdListIsExists(resourceIdList);
        if (resourceIdList.size() > existResourceIdList.size()) {
            List<Long> notFoundIdList = ListUtils.removeAll(resourceIdList, existResourceIdList);
            if (CollectionUtils.isNotEmpty(notFoundIdList)) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Long resourceId : notFoundIdList) {
                    stringBuilder.append(resourceId);
                    stringBuilder.append("、");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                throw new ResourceNotFoundException(stringBuilder.toString());
            }
        }

        List<Long> accountIdList = accountIdArray.toJavaList(Long.class);
        List<Long> existAccountIdList = resourceAccountMapper.checkAccountIdListIsExists(accountIdList);
        if (accountIdList.size() > existAccountIdList.size()) {
            List<Long> notFoundIdList = ListUtils.removeAll(accountIdList, existAccountIdList);
            if (CollectionUtils.isNotEmpty(notFoundIdList)) {
                StringBuilder stringBuilder = new StringBuilder();
                for (Long accountId : notFoundIdList) {
                    stringBuilder.append(accountId);
                    stringBuilder.append("、");
                }
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                throw new ResourceCenterAccountNotFoundException(stringBuilder.toString());
            }
        }
        resourceAccountMapper.deleteResourceAccountByResourceIdListAndAccountIdList(resourceIdList, accountIdList);
        return null;
    }
}
