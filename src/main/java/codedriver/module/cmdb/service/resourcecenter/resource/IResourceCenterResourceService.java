/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.crossover.IResourceCenterResourceCrossoverService;
import codedriver.framework.cmdb.dto.ci.CiVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import codedriver.framework.cmdb.dto.resourcecenter.ResourceVo;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.util.List;


/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
public interface IResourceCenterResourceService extends IResourceCenterResourceCrossoverService {

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj);

    /**
     * 补充资产的账号信息
     *
     * @param idList
     * @param resourceVoList
     */
    void addResourceAccount(List<Long> idList, List<ResourceVo> resourceVoList);

    /**
     * 补充资产的标签信息
     *
     * @param idList
     * @param resourceVoList
     */
    void addResourceTag(List<Long> idList, List<ResourceVo> resourceVoList);


    /**
     * 获取对应模块的应用清单列表
     * 其中清单列表有 系统 存储设备 网络设备 应用实例 应用实例集群 DB实例 DB实例集群 访问入口
     *
     * @param searchVo
     * @return
     */
    JSONArray getAppModuleResourceList(ResourceSearchVo searchVo);

    /**
     * 获取对应的资产类型名称
     *
     * @param resourceCiVoList
     * @param resourceCiVo
     * @return
     */
    public String getResourceTypeName(List<CiVo> resourceCiVoList, CiVo resourceCiVo);
}
