package codedriver.module.cmdb.service.ci;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.CiVo;

@Service
public class CiServiceImpl implements CiService {

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Override
    public int saveCi(CiVo ciVo) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public int deleteCi(Long ciId) {
        // 删除配置项相关信息
        ciEntityMapper.deleteCiEntityByCiId(ciId);
        // 删除事务相关信息
        transactionMapper.deleteTransactionByCiId(ciId);
        // 删除模型相关信息
        ciMapper.deleteCiById(ciId);
        return 1;
    }

}
