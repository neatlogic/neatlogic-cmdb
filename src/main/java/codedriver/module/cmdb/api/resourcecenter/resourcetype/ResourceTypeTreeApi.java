/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resourcetype;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.annotation.ResourceType;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
import codedriver.framework.cmdb.dto.resourcecenter.entity.ResourceEntityBaseVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.framework.restful.annotation.*;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.CMDB_BASE;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 查询资源类型树列表接口
 * @author linbq
 * @since 2021/5/27 16:14
 **/
@Service
@AuthAction(action = CMDB_BASE.class)
@OperationType(type = OperationTypeEnum.SEARCH)
public class ResourceTypeTreeApi extends PrivateApiComponentBase {

    private static List<ResourceTypeVo> resourceTypeList = new ArrayList<>();

    static {
        Reflections reflections = new Reflections("codedriver.framework.cmdb.dto.resourcecenter.entity");
        Set<Class<? extends ResourceEntityBaseVo>> classSet = reflections.getSubTypesOf(ResourceEntityBaseVo.class);
        for (Class<? extends ResourceEntityBaseVo> c : classSet) {
            ResourceType resourceType = c.getAnnotation(ResourceType.class);
            if(resourceType != null) {
                String ciName = resourceType.ciName();
                if(StringUtils.isNotBlank(ciName)){
                    resourceTypeList.add(new ResourceTypeVo(resourceType.name(), resourceType.label(), ciName));
                }
            }
        }
    }

    @Resource
    private CiMapper ciMapper;

    @Override
    public String getToken() {
        return "resourcecenter/resourcetype/tree";
    }

    @Override
    public String getName() {
        return "查询资源类型树列表";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Output({
            @Param(explode = ResourceTypeVo[].class, desc = "资源类型树列表")
    })
    @Description(desc = "查询资源类型树列表")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
        List<ResourceTypeVo> resultList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(resourceTypeList)) {
            List<CiVo> ciVoList = new ArrayList<>();
            Map<String, String> typeNameMap = new HashMap<>();
            for(ResourceTypeVo type : resourceTypeList){
                CiVo ciVo = ciMapper.getCiByName(type.getCiName());
                if(ciVo == null){
                    throw new CiNotFoundException(type.getCiName());
                }
                ciVoList.add(ciVo);
                typeNameMap.put(type.getCiName(), type.getName());
            }
            ciVoList.sort(Comparator.comparing(CiVo::getLft));
            for(CiVo ciVo : ciVoList){
                List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                int size = ciList.size();
                List<ResourceTypeVo> resourceTypeVoList = new ArrayList<>(size);
                Map<Long, ResourceTypeVo> resourceTypeMap = new HashMap<>(size);
                for (CiVo ci : ciList) {
                    ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ci.getId(), ci.getParentCiId(), typeNameMap.get(ciVo.getName()), ci.getLabel(), ci.getName());
                    resourceTypeMap.put(resourceTypeVo.getCiId(), resourceTypeVo);
                    resourceTypeVoList.add(resourceTypeVo);
                }
                for (ResourceTypeVo resourceType : resourceTypeVoList) {
                    if (resourceType.getParentCiId() != null) {
                        ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentCiId());
                        if (parentResourceType != null) {
                            parentResourceType.addChild(resourceType);
                        } else {
                            resultList.add(resourceType);
                        }
                    }
                }
            }
        }
        return resultList;
    }
}
