package codedriver.module.cmdb.service.ci;

import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.CiVo;

import java.util.List;

@Service
public class CiServiceImpl implements CiService {

    @Autowired
    private CiMapper ciMapper;

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private RelMapper relMapper;

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

    @Override
    public CiVo getCiById(Long id) {
        CiVo ciVo = ciMapper.getCiById(id);
        List<AttrVo> attrList = attrMapper.getAttrByCiId(id);
        List<RelVo> relList = relMapper.getRelByCiId(id);
        ciVo.setRelList(relList);
        ciVo.setAttrList(attrList);
//        ciVo.setOperateList(operateMapper.getOperateListByCiId(ciVo.getId()));
        return ciVo;
    }
}
