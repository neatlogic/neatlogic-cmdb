/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.dao.mapper.resourcecenter.ResourceCenterMapper;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.exception.ci.CiNotFoundException;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
@Service
public class ResourceCenterResourceServiceImpl implements ResourceCenterResourceService {
    @Resource
    ResourceCenterMapper resourceCenterMapper;

    @Resource
    private CiMapper ciMapper;

    @Override
    public ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj) {
        ResourceSearchVo searchVo = JSON.toJavaObject(jsonObj, ResourceSearchVo.class);
        Long typeId = searchVo.getTypeId();
        if (typeId != null) {
            CiVo ciVo = ciMapper.getCiById(typeId);
            if (ciVo == null) {
                throw new CiNotFoundException(typeId);
            }
            List<CiVo> ciList = ciMapper.getDownwardCiListByLR(ciVo.getLft(), ciVo.getRht());
            List<Long> ciIdList = ciList.stream().map(CiVo::getId).collect(Collectors.toList());
            List<Long> typeIdList = searchVo.getTypeIdList();
            if (CollectionUtils.isNotEmpty(typeIdList)) {
                ciIdList.retainAll(typeIdList);
            }
            searchVo.setTypeIdList(ciIdList);
        }
        List<Long> resourceIdList = null;
        if (CollectionUtils.isNotEmpty(searchVo.getProtocolIdList())) {
            List<Long> idList = resourceCenterMapper.getResourceIdListByProtocolIdList(searchVo);
            if (resourceIdList == null) {
                resourceIdList = idList;
            } else {
                resourceIdList.retainAll(idList);
            }
        }
        if (CollectionUtils.isNotEmpty(searchVo.getTagIdList())) {
            List<Long> idList = resourceCenterMapper.getResourceIdListByTagIdList(searchVo);
            if (resourceIdList == null) {
                resourceIdList = idList;
            } else {
                resourceIdList.retainAll(idList);
            }
        }
        searchVo.setIdList(resourceIdList);
        return searchVo;
    }

}
