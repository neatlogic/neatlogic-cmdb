/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resource;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceTagVo;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterTagNotFoundException;
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
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/22 15:57
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceTagBatchAddApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/tag/batch/add";
    }

    @Override
    public String getName() {
        return "批量添加资源标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "资源id列表"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签ID列表")
    })
    @Description(desc = "批量添加资源标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray resourceIdArray = paramObj.getJSONArray("resourceIdList");
        if (CollectionUtils.isEmpty(resourceIdArray)) {
            throw new ParamNotExistsException("resourceIdList");
        }
        JSONArray tagArray = paramObj.getJSONArray("tagIdList");
        if (CollectionUtils.isEmpty(tagArray)) {
            throw new ParamNotExistsException("tagIdList");
        }

        String schemaName = TenantContext.get().getDataDbName();
        List<Long> resourceIdList = resourceIdArray.toJavaList(Long.class);
        List<Long> existResourceIdList = resourceCenterMapper.checkResourceIdListIsExists(resourceIdList, schemaName);
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
        List<Long> tagList = tagArray.toJavaList(Long.class);
        List<Long> tagIdList = resourceCenterMapper.checkTagIdExistsByTagIdList(tagList);
        tagList.removeAll(tagIdList);
        if (CollectionUtils.isNotEmpty(tagList)) {
            throw new ResourceCenterTagNotFoundException(tagList);
        }

        List<ResourceTagVo> resourceTagVoList = new ArrayList<>();
        for (Long resourceId : resourceIdList) {
            for (Long tagId : tagIdList) {
                resourceTagVoList.add(new ResourceTagVo(resourceId, tagId));
                if (resourceTagVoList.size() > 100) {
                    resourceCenterMapper.insertIgnoreResourceTag(resourceTagVoList);
                    resourceTagVoList.clear();
                }
            }
        }
        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
            resourceCenterMapper.insertIgnoreResourceTag(resourceTagVoList);
        }
        return null;
    }
}
