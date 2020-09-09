package codedriver.module.cmdb.service.ci;

import java.util.List;

import codedriver.framework.cmdb.constvalue.GroupType;

public interface CiAuthService {
    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 是否拥有模型管理权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasCiManagePrivilege(Long ciId);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 是否拥有配置项查询权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasCiEntityQueryPrivilege(Long ciId);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 指定配置项是否拥有查询权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasCiEntityQueryPrivilege(Long ciId, Long ciEntityId);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 是否拥有配置项插入权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasCiEntityInsertPrivilege(Long ciId);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 指定配置项是否拥有插入权限
     * @param @param
     *            ciId
     * @param @param
     *            ciEntityId
     * @param @return
     * @return boolean
     */
    public boolean hasCiEntityInsertPrivilege(Long ciId, Long ciEntityId);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 是否拥有配置项更新权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasCiEntityUpdatePrivilege(Long ciId);

    /**
    * @Author: chenqiwei
    * @Time:Sep 8, 2020
    * @Description:  
    * @param @param ciEntityId
    * @param @param groupType
    * @param @return 
    * @return boolean
     */
    public boolean isInGroup(Long ciEntityId, GroupType... groupType);

    /**
    * @Author: chenqiwei
    * @Time:Sep 8, 2020
    * @Description: TODO 
    * @param @param ciEntityIdList
    * @param @param groupType
    * @param @return 
    * @return List<Long> 返回有权限的id
     */
    public List<Long> isInGroup(List<Long> ciEntityIdList, GroupType... groupType);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 是否拥有配置项删除权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasCiEntityDeletePrivilege(Long ciId);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 是否拥有事务管理权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasTransactionPrivilege(Long ciId);

    /**
     * @Author: chenqiwei
     * @Time:Sep 8, 2020
     * @Description: 是否拥有密码查看权限
     * @param @param
     *            ciId
     * @param @return
     * @return boolean
     */
    public boolean hasPasswordPrivilege(Long ciId);

}
