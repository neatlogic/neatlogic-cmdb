package codedriver.module.cmdb.dependency;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import codedriver.framework.cmdb.enums.CmdbFromType;
import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceAccountMapper;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceMapper;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author longrf
 * @date 2022/3/11 5:14 下午
 */
@Service
public class ResourceAccountDependencyHandler extends CustomTableDependencyHandlerBase {

    @Resource
    ResourceAccountMapper resourceAccountMapper;

    @Resource
    ResourceMapper resourceMapper;

    @Override
    protected String getTableName() {
        return "cmdb_resourcecenter_resource_account";
    }

    @Override
    protected String getFromField() {
        return "account_id";
    }

    @Override
    protected String getToField() {
        return "resource_id";
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        if (dependencyObj == null) {
            return null;
        }
        ResourceVo obj = (ResourceVo) dependencyObj;
        JSONObject dependencyInfoConfig = new JSONObject();
        dependencyInfoConfig.put("resourceId", obj.getId());
        List<String> pathList = new ArrayList<>();
        pathList.add("资产清单");
        String lastName = obj.getIp() + (obj.getPort() != null ? (":" + obj.getPort()) : "");
        String urlFormat = "/" + TenantContext.get().getTenantUuid() + "/cmdb.html#/asset-manage?resourceId=" + obj.getId();
        return new DependencyInfoVo(obj.getId(), dependencyInfoConfig, lastName, pathList, urlFormat, this.getGroupName());
    }

    @Override
    public IFromType getFromType() {
        return CmdbFromType.RESOURCE_ACCOUNT;
    }

    @Override
    public List<DependencyInfoVo> getDependencyList(Object from, int startNum, int pageSize) {
        List<DependencyInfoVo> resultList = new ArrayList<>();
        List<Long> resourceIdList = resourceAccountMapper.getResourceIdListByAccountIdWithPage((Long) from, startNum, pageSize);
        if (resourceIdList.size() > 0) {
            List<ResourceVo> resourceList = resourceMapper.getResourceListByIdList(resourceIdList);
            for (ResourceVo vo : resourceList) {
                DependencyInfoVo dependencyInfoVo = parse(vo);
                if (dependencyInfoVo != null) {
                    resultList.add(dependencyInfoVo);
                }
            }
        }
        return resultList;
    }

}
