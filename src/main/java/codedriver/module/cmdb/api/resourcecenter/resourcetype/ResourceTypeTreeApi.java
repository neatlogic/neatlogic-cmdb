/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter.resourcetype;

import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.annotation.ResourceType;
import codedriver.framework.cmdb.annotation.ResourceTypes;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceTypeVo;
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
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
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
        Reflections reflections = new Reflections("codedriver.framework.cmdb.dto.resourcecenter.entity", new TypeAnnotationsScanner(), new SubTypesScanner());
        Set<Class<?>> classList = reflections.getTypesAnnotatedWith(ResourceType.class, true);
        for (Class<?> c : classList) {
            ResourceType resourceType = c.getAnnotation(ResourceType.class);
            if (resourceType != null) {
                String ciName = resourceType.ciName();
                if (StringUtils.isNotBlank(ciName)) {
                    resourceTypeList.add(new ResourceTypeVo(resourceType.label(), ciName));
                }
            }
        }
        classList = reflections.getTypesAnnotatedWith(ResourceTypes.class, true);
        for (Class<?> c : classList) {
            ResourceTypes resourceTypes = c.getAnnotation(ResourceTypes.class);
            if (resourceTypes != null) {
                for (ResourceType resourceType : resourceTypes.value()) {
                    String ciName = resourceType.ciName();
                    if (StringUtils.isNotBlank(ciName)) {
                        resourceTypeList.add(new ResourceTypeVo(resourceType.label(), ciName));
                    }
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
            for(ResourceTypeVo type : resourceTypeList){
                CiVo ciVo = ciMapper.getCiByName(type.getName());
                if(ciVo == null){
                    throw new CiNotFoundException(type.getName());
                }
                ciVoList.add(ciVo);
            }
            ciVoList.sort(Comparator.comparing(CiVo::getLft));
            for(CiVo ciVo : ciVoList){
                List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
                int size = ciList.size();
                List<ResourceTypeVo> resourceTypeVoList = new ArrayList<>(size);
                Map<Long, ResourceTypeVo> resourceTypeMap = new HashMap<>(size);
                for (CiVo ci : ciList) {
                    ResourceTypeVo resourceTypeVo = new ResourceTypeVo(ci.getId(), ci.getParentCiId(), ci.getLabel(), ci.getName());
                    resourceTypeMap.put(resourceTypeVo.getId(), resourceTypeVo);
                    resourceTypeVoList.add(resourceTypeVo);
                }
                for (ResourceTypeVo resourceType : resourceTypeVoList) {
                    if (resourceType.getParentId() != null) {
                        ResourceTypeVo parentResourceType = resourceTypeMap.get(resourceType.getParentId());
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
