/*Copyright (C) 2024  深圳极向量科技有限公司 All Rights Reserved.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.*/

package neatlogic.module.cmdb.service.resourcecenter.resource;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.resourcecenter.AccountVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import neatlogic.framework.cmdb.dto.resourcecenter.ResourceVo;
import neatlogic.framework.cmdb.dto.tag.TagVo;

import java.util.List;
import java.util.Map;


/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
public interface IResourceCenterResourceService {

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj);

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj,boolean isIncludeSon);

    void assembleResourceSearchVo(ResourceSearchVo searchVo, boolean isIncludeSon);

    /**
     * 处理批量搜索关键字
     * @param resourceSearchVo
     */
    void handleBatchSearchList(ResourceSearchVo resourceSearchVo);

    /**
     * 设置ipFieldAttrId字段和nameFieldAttrId字段
     * @param resourceSearchVo
     */
    void setIpFieldAttrIdAndNameFieldAttrId(ResourceSearchVo resourceSearchVo);

    /**
     * 设置isIpFieldSort字段和isNameFieldSort字段
     * @param resourceSearchVo
     */
    void setIsIpFieldSortAndIsNameFieldSort(ResourceSearchVo resourceSearchVo);

    /**
     * 查询所有后代模型的id列表
     *
     * @param idList
     * @return
     */
    List<Long> getDownwardCiIdListByCiIdList(List<Long> idList);

    /**
     * 获取资产的账号信息
     *
     * @param idList
     */
    Map<Long, List<AccountVo>> getResourceAccountByResourceIdList(List<Long> idList);

    /**
     * 获取资产的标签信息
     *
     * @param idList 资产id列表
     * @return map<资产id ， 标签列表>
     */
    Map<Long, List<TagVo>> getResourceTagByResourceIdList(List<Long> idList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addTagAndAccountInformation(List<ResourceVo> resourceList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addTagInformation(List<ResourceVo> resourceList);

    /**
     * 添加标签和账号信息
     *
     * @param resourceList
     */
    void addAccountInformation(List<ResourceVo> resourceList);

}
