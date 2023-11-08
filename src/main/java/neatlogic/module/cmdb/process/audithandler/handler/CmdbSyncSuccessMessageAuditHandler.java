package neatlogic.module.cmdb.process.audithandler.handler;

import com.alibaba.fastjson.JSONObject;
import neatlogic.framework.cmdb.dto.transaction.TransactionVo;
import neatlogic.framework.process.audithandler.core.IProcessTaskStepAuditDetailHandler;
import neatlogic.framework.process.dto.ProcessTaskStepAuditDetailVo;
import neatlogic.framework.util.TableResultUtil;
import neatlogic.module.cmdb.dao.mapper.transaction.TransactionMapper;
import neatlogic.module.cmdb.process.constvalue.CmdbAuditDetailType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

@Component
public class CmdbSyncSuccessMessageAuditHandler implements IProcessTaskStepAuditDetailHandler {

    @Resource
    private TransactionMapper transactionMapper;

    @Override
    public String getType() {
        return CmdbAuditDetailType.CMDBSYNCMESSAGE.getValue();
    }

    @Override
    public int handle(ProcessTaskStepAuditDetailVo processTaskStepAuditDetailVo) {
        String newContent = processTaskStepAuditDetailVo.getNewContent();
        if (StringUtils.isBlank(newContent)) {
            return 0;
        }
        Long transactionGroupId;
        try {
            transactionGroupId = Long.valueOf(newContent);
        } catch (Exception e) {
            return 1;
        }
        if (transactionGroupId == null) {
            return 0;
        }
        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setTransactionGroupId(transactionGroupId);
        List<TransactionVo> transactionList = transactionMapper.searchTransaction(transactionVo);
        if (CollectionUtils.isEmpty(transactionList)) {
            return 0;
        }
        processTaskStepAuditDetailVo.setNewContent(JSONObject.toJSONString(TableResultUtil.getResult(transactionList)));
        return 1;
    }
}
