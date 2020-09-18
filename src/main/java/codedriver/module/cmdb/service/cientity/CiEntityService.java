package codedriver.module.cmdb.service.cientity;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;

import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;

public interface CiEntityService {
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo);

    @Transactional
    /**
     * @Author: chenqiwei
     * @Time:Sep 2, 2020
     * @Description: 保存配置项
     * @param @param
     *            ciEntityVo
     * @param @param
     *            action
     * @param @return
     * @return Long 事务id
     */
    public Long saveCiEntity(CiEntityTransactionVo ciEntityTransactionVo, TransactionActionType action);

    public Long commitTransaction(Long transactionId);

}
