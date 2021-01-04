package codedriver.module.cmdb.service.cientity;

import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.TransactionVo;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CiEntityService {
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

    /**
     * @param @param  ciEntityTransactionVo
     * @param @return
     * @return Long 事务id
     * @Author: chenqiwei
     * @Time: 2020年11月17日
     * @Description: 保存配置项
     */
    @Transactional
    public Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * @Author: chenqiwei
     * @Time: 2020年11月11日
     * @Description: 获取配置项详细信息
     * @param @param
     *            ciEntityId
     * @param @return
     * @return CiEntityVo
     */
    public CiEntityVo getCiEntityDetailById(Long ciEntityId);

    void createCiEntityName(CiEntityTransactionVo ciEntityTransactionVo);

    void createSnapshot(CiEntityTransactionVo ciEntityTransactionVo);

    Long commitTransaction(TransactionVo transactionVo);

    public Long commitTransaction(Long transactionId);

    /**
     * @param @param  ciEntityId
     * @param @return
     * @return Long
     * @Author: chenqiwei
     * @Time: 2020年11月11日
     * @Description: 删除单个配置项
     */
    public Long deleteCiEntity(Long ciEntityId);

    /**
     * @Author: chenqiwei
     * @Time: 2020年11月11日
     * @Description: 根据配置项id和视图配置查询配置项
     * @param @param
     *            ciEntityIdList
     * @param @param
     *            ciEntityVo
     * @param @return
     * @return List<CiEntityVo>
     */
    public List<CiEntityVo> searchCiEntityByIds(List<Long> ciEntityIdList, CiEntityVo ciEntityVo);

    /**
     * @Author: chenqiwei
     * @Time: 2020年11月17日
     * @Description: 验证配置项是否合法
     * @param @param
     *            ciEntityTransactionVo
     * @param @param
     *            action
     * @param @return
     * @return boolean
     */
    public boolean validateCiEntity(CiEntityTransactionVo ciEntityTransactionVo);

    /**
     * @Author: chenqiwei
     * @Time: 2020年11月17日
     * @Description: 批量保存多个配置项
     * @param @param
     *            ciEntityTransactionList
     * @param @return
     * @return Long 事务组id
     */
    public Long saveCiEntity(List<CiEntityTransactionVo> ciEntityTransactionList);

}
