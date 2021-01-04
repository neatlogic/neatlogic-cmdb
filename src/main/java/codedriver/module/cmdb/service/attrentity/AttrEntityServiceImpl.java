package codedriver.module.cmdb.service.attrentity;

import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AttrEntityServiceImpl implements AttrEntityService {
    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;


}
