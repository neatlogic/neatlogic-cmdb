/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.cientity;

import codedriver.framework.cmdb.dto.cientity.CiEntityVo;
import codedriver.framework.cmdb.dto.transaction.CiEntityTransactionVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CiEntityService {
    /**
     * 根据配置项id列表返回配置项
     *
     * @param ciId           模型id
     * @param ciEntityIdList 配置项id列表
     * @return 配置项列表
     */
    List<CiEntityVo> getCiEntityByIdList(Long ciId, List<Long> ciEntityIdList);

    List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

    /**
     * 保存配置项
     *
     * @param ciEntityTransactionVo 配置项事务
     * @return 事务id
     */
    @Transactional
    Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * 获取单个配置项详细信息
     *
     * @param ciEntityId 配置项id
     * @return ciEntityVo
     */
    CiEntityVo getCiEntityById(Long ciEntityId);

    void createCiEntityName(CiEntityTransactionVo ciEntityTransactionVo);

    void createSnapshot(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * 提交事务
     *
     * @param transactionId 事务id
     * @return 配置项id
     */
    Long commitTransaction(Long transactionId);

    /**
     * 删除整个配置项
     *
     * @param ciEntityId 配置项id
     * @return 事务id
     */
    Long deleteCiEntity(Long ciEntityId);

    /**
     * 根据配置项id和视图配置查询配置项
     *
     * @param ciEntityIdList 配置项id列表
     * @param ciEntityVo     配置项
     * @return 配置项列表
     */
    List<CiEntityVo> searchCiEntityByIds(List<Long> ciEntityIdList, CiEntityVo ciEntityVo);

    /**
     * 验证配置项是否合法
     *
     * @param ciEntityTransactionVo 事务
     * @return 是否合法
     */
    boolean validateCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * 批量保存多个配置项
     *
     * @param ciEntityTransactionList 事务列表
     * @return 事务组id
     */
    Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList);

}
