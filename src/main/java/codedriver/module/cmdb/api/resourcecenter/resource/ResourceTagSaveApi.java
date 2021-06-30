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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linbq
 * @since 2021/6/22 15:56
 **/
@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.UPDATE)
public class ResourceTagSaveApi extends PrivateApiComponentBase {

    @Resource
    private ResourceCenterMapper resourceCenterMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resource/tag/save";
    }

    @Override
    public String getName() {
        return "保存资源标签";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Input({
            @Param(name = "resourceId", type = ApiParamType.LONG, isRequired = true, desc = "资源id"),
            @Param(name = "tagIdList", type = ApiParamType.JSONARRAY, isRequired = true, desc = "标签ID列表")
    })
    @Description(desc = "保存资源标签")
    @Override
    public Object myDoService(JSONObject paramObj) throws Exception {
        JSONArray tagArray = paramObj.getJSONArray("tagIdList");
        if (CollectionUtils.isEmpty(tagArray)) {
            throw new ParamNotExistsException("tagIdList");
        }
        String schemaName = TenantContext.get().getDataDbName();
        Long resourceId = paramObj.getLong("resourceId");
        if (resourceCenterMapper.checkResourceIsExists(resourceId, schemaName) == 0) {
            throw new ResourceNotFoundException(resourceId);
        }
        List<Long> tagList = tagArray.toJavaList(Long.class);
        List<Long> tagIdList = resourceCenterMapper.checkTagIdExistsByTagIdList(tagList);
        tagList.removeAll(tagIdList);
        if (CollectionUtils.isNotEmpty(tagList)) {
            throw new ResourceCenterTagNotFoundException(tagList);
        }
        resourceCenterMapper.deleteResourceTagByResourceId(resourceId);
        List<ResourceTagVo> resourceTagVoList = new ArrayList<>();
        for (Long tagId : tagIdList) {
            resourceTagVoList.add(new ResourceTagVo(resourceId, tagId));
            if (resourceTagVoList.size() > 100) {
                resourceCenterMapper.insertIgnoreResourceTag(resourceTagVoList);
                resourceTagVoList.clear();
            }
        }
        if (CollectionUtils.isNotEmpty(resourceTagVoList)) {
            resourceCenterMapper.insertIgnoreResourceTag(resourceTagVoList);
        }
        return null;
    }
}
