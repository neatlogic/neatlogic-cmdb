package codedriver.module.cmdb.service.cientity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import codedriver.framework.cmdb.constvalue.AttrType;
import codedriver.framework.cmdb.constvalue.TransactionActionType;
import codedriver.framework.cmdb.constvalue.TransactionStatus;
import codedriver.framework.cmdb.prop.core.IPropertyHandler;
import codedriver.framework.cmdb.prop.core.PropertyHandlerFactory;
import codedriver.framework.common.util.PageUtil;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.RelMapper;
import codedriver.module.cmdb.dao.mapper.cientity.AttrEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.CiEntityMapper;
import codedriver.module.cmdb.dao.mapper.cientity.RelEntityMapper;
import codedriver.module.cmdb.dao.mapper.transaction.TransactionMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.dto.transaction.AttrEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.CiEntityTransactionVo;
import codedriver.module.cmdb.dto.transaction.TransactionVo;
import codedriver.module.cmdb.exception.cientity.AttrEntityDuplicateException;
import codedriver.module.cmdb.exception.cientity.AttrEntityNotFoundException;
import codedriver.module.cmdb.exception.cientity.CiEntityIsLockedException;

@Service
public class CiEntityServiceImpl implements CiEntityService {

    @Autowired
    private CiEntityMapper ciEntityMapper;

    @Autowired
    private AttrEntityMapper attrEntityMapper;

    @Autowired
    private RelEntityMapper relEntityMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private AttrMapper attrMapper;

    @Autowired
    private RelMapper relMapper;

    @Override
    public List<CiEntityVo> searchCiEntity(CiEntityVo ciEntityVo) {
        List<Long> ciEntityIdList = ciEntityMapper.searchCiEntityId(ciEntityVo);
        List<CiEntityVo> ciEntityList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ciEntityIdList)) {
            if (ciEntityVo.getNeedPage()) {
                int rowNum = ciEntityMapper.searchCiEntityIdCount(ciEntityVo);
                ciEntityVo.setRowNum(rowNum);
                ciEntityVo.setPageCount(PageUtil.getPageCount(rowNum, ciEntityVo.getPageSize()));
            }
            ciEntityList = ciEntityMapper.searchCiEntityByIdList(ciEntityIdList);
        }
        return ciEntityList;
    }

    @Transactional
    @Override
    public Long saveCiEntity(CiEntityVo ciEntityVo, TransactionActionType action) {
        if (action.equals(TransactionActionType.UPDATE)) {
            CiEntityVo checkCiEntityVo = ciEntityMapper.getCiEntityById(ciEntityVo.getId());
            if (checkCiEntityVo != null && checkCiEntityVo.getIsLocked().equals(1)) {
                throw new CiEntityIsLockedException(ciEntityVo.getId());
            }
        }
        List<RelEntityVo> oldRelEntityList = null;

        TransactionVo transactionVo = new TransactionVo();
        transactionVo.setCiId(ciEntityVo.getCiId());

        CiEntityTransactionVo ciEntityTransactionVo = new CiEntityTransactionVo(ciEntityVo);
        ciEntityTransactionVo.setAction(action.getValue());
        ciEntityTransactionVo.setTransactionId(transactionVo.getId());

        transactionVo.setCiEntityTransactionVo(ciEntityTransactionVo);

        boolean hasChange = validateCiEntity(ciEntityVo);

        if (hasChange) {
            // 写入事务
            transactionMapper.insertTransaction(transactionVo);
            // 写入配置项事务
            transactionMapper.insertCiEntityTransaction(ciEntityTransactionVo);
            // 写入属性事务
            if (CollectionUtils.isNotEmpty(ciEntityVo.getAttrEntityList())) {
                List<AttrEntityTransactionVo> attrEntityTransactionList = new ArrayList<>();
                for (AttrEntityVo attrEntityVo : ciEntityVo.getAttrEntityList()) {
                    AttrEntityTransactionVo attrEntityTransactionVo = new AttrEntityTransactionVo(attrEntityVo);
                    attrEntityTransactionVo.setTransactionId(transactionVo.getId());
                    transactionMapper.insertAttrEntityTransaction(attrEntityTransactionVo);
                    attrEntityTransactionList.add(attrEntityTransactionVo);
                }
                ciEntityTransactionVo.setAttrEntityTransactionList(attrEntityTransactionList);
            }

            commitTransaction(transactionVo);
            return transactionVo.getId();
        } else {
            // 没有任何变化则返回零
            return 0L;
        }
    }

    private boolean checkCiEntityAllowUpdate(CiEntityVo ciEntityVo) {

        return true;
    }

    @SuppressWarnings("serial")
    private boolean validateCiEntity(CiEntityVo ciEntityVo) {
        List<AttrVo> attrList = attrMapper.getAttrByCiId(ciEntityVo.getCiId());
        List<RelVo> relList = relMapper.getRelByCiId(ciEntityVo.getCiId());

        List<AttrEntityVo> oldAttrEntityList = attrEntityMapper.getAttrEntityByCiEntityId(ciEntityVo.getId());

        // 清除和模型属性不匹配的属性
        if (CollectionUtils.isNotEmpty(ciEntityVo.getAttrEntityList())) {
            Iterator<AttrEntityVo> itAttr = ciEntityVo.getAttrEntityList().iterator();
            while (itAttr.hasNext()) {
                boolean isExists = false;
                AttrEntityVo attrEntityVo = itAttr.next();
                for (AttrVo attrVo : attrList) {
                    if (attrVo.getId().equals(attrEntityVo.getAttrId())) {
                        isExists = true;
                        break;
                    }
                }
                if (!isExists) {
                    itAttr.remove();
                }
            }
        }

        // 把attrEntity中的value拆解到valueList里
        /*if (CollectionUtils.isNotEmpty(ciEntityVo.getAttrEntityList())) {
            for (AttrEntityVo attrEntityVo : ciEntityVo.getAttrEntityList()) {
                for (AttrVo attrVo : attrList) {
                    if (attrVo.getId().equals(attrEntityVo.getAttrId())) {
                        if (attrVo.getType().equals(AttrType.PROPERTY.getValue())) {
                            IPropertyHandler propHandler = PropertyHandlerFactory.getHandler(attrVo.getPropHandler());
                            attrEntityVo.setValueList(propHandler.transferValue(attrEntityVo.getValue()));
                            break;
                        } else if (attrVo.getType().equals(AttrType.CUSTOM.getValue())) {
                            attrEntityVo.setValueList(new ArrayList<String>() {
                                {
                                    this.add(attrEntityVo.getValue().toString());
                                }
                            });
                        }
                    }
                }
            }
        }*/

        for (AttrVo attrVo : attrList) {
            // 属性值类型是自定义或属性定义的才需要进行值校验
            if (attrVo.getType().equals(AttrType.CUSTOM.getValue())
                || attrVo.getType().equals(AttrType.PROPERTY.getValue())) {
                AttrEntityVo attrEntityVo = ciEntityVo.getAttrEntityByAttrId(attrVo.getId());
                // 属性必填校验
                if (attrVo.getIsRequired().equals(1)) {
                    if (attrEntityVo == null || CollectionUtils.isEmpty(attrEntityVo.getValueList())) {
                        // 如果新属性没有值，从老属性中查找
                        if (CollectionUtils.isNotEmpty(oldAttrEntityList)) {
                            boolean isExists = false;
                            for (AttrEntityVo oldAttrEntityVo : oldAttrEntityList) {
                                if (oldAttrEntityVo.getAttrId().equals(attrVo.getId())) {
                                    isExists = true;
                                    if (CollectionUtils.isEmpty(oldAttrEntityVo.getValueList())) {
                                        throw new AttrEntityNotFoundException(attrVo.getLabel());
                                    }
                                    break;
                                }
                            }
                            if (!isExists) {
                                throw new AttrEntityNotFoundException(attrVo.getLabel());
                            }
                        } else {
                            throw new AttrEntityNotFoundException(attrVo.getLabel());
                        }

                    }
                }
                // 检查属性是否唯一
                if (attrVo.getIsUnique().equals(1)) {
                    if (attrEntityVo != null) {
                        if (CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                            if (attrEntityMapper.checkAttrEntityValueIsExists(attrEntityVo) > 0) {
                                throw new AttrEntityDuplicateException(attrVo.getLabel(), attrEntityVo.getValueList());
                            }
                        }
                    } else {
                        for (AttrEntityVo oldAttrEntityVo : oldAttrEntityList) {
                            if (oldAttrEntityVo.getAttrId().equals(attrVo.getId())) {
                                if (CollectionUtils.isNotEmpty(oldAttrEntityVo.getValueList())) {
                                    if (attrEntityMapper.checkAttrEntityValueIsExists(oldAttrEntityVo) > 0) {
                                        throw new AttrEntityDuplicateException(attrVo.getLabel(),
                                            oldAttrEntityVo.getValueList());
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        // 判断属性值是否有变化
        boolean hasChange = false;
        if (CollectionUtils.isNotEmpty(ciEntityVo.getAttrEntityList())) {
            // 去掉没变化的修改
            ciEntityVo.getAttrEntityList().removeAll(oldAttrEntityList);
        }
        if (CollectionUtils.isNotEmpty(ciEntityVo.getAttrEntityList())) {
            hasChange = true;
        }

        return hasChange;
    }

    /**
     * @Author: chenqiwei
     * @Time:Sep 2, 2020
     * @Description: 提交事务
     * @param @param
     *            transactionVo
     * @param @return
     * @return Long 配置项id
     */
    private Long commitTransaction(TransactionVo transactionVo) {
        CiEntityTransactionVo ciEntityTransactionVo = transactionVo.getCiEntityTransactionVo();
        CiEntityVo ciEntityVo = new CiEntityVo(ciEntityTransactionVo);
        // 写入配置项
        if (ciEntityTransactionVo.getAction().equals(TransactionActionType.INSERT.getValue())) {
            ciEntityMapper.insertCiEntity(ciEntityVo);
        } else if (ciEntityTransactionVo.getAction().equals(TransactionActionType.UPDATE.getValue())) {
            ciEntityVo.setIsLocked(0);
            ciEntityMapper.updateCiEntityLockById(ciEntityVo);
        }
        // 写入属性
        List<AttrEntityTransactionVo> attrEntityTransactionList = ciEntityTransactionVo.getAttrEntityTransactionList();
        if (CollectionUtils.isNotEmpty(attrEntityTransactionList)) {
            for (AttrEntityTransactionVo attrEntityTransactionVo : attrEntityTransactionList) {
                AttrEntityVo attrEntityVo = new AttrEntityVo(attrEntityTransactionVo);
                if (CollectionUtils.isNotEmpty(attrEntityVo.getValueList())) {
                    attrEntityMapper.insertAttrEntity(attrEntityVo);
                } else {
                    attrEntityMapper.deleteAttrEntity(attrEntityVo);
                }
            }
        }
        transactionVo.setStatus(TransactionStatus.COMMITED.getValue());
        transactionMapper.updateTransactionStatus(transactionVo);
        return ciEntityVo.getId();
    }

    @Override
    public Long commitTransaction(Long transactionId) {
        return null;
    }

    public static void main(String[] arg) {
        List<AttrEntityVo> newAttrList = new ArrayList<>();
        AttrEntityVo attr1 = new AttrEntityVo();
        attr1.setAttrId(1L);
        attr1.setValueList(new ArrayList<String>() {
            {
                this.add("a");
                this.add("c");
                this.add("b");
            }
        });

        AttrEntityVo attr2 = new AttrEntityVo();
        attr2.setAttrId(2L);
        attr2.setValueList(new ArrayList<String>() {
            {
                this.add("a");
            }
        });
        newAttrList.add(attr1);
        newAttrList.add(attr2);

        List<AttrEntityVo> oldAttrList = new ArrayList<>();
        AttrEntityVo attr3 = new AttrEntityVo();
        attr3.setAttrId(1L);
        attr3.setValueList(new ArrayList<String>() {
            {
                this.add("a");
                this.add("b");
                this.add("c");
            }
        });
        AttrEntityVo attr4 = new AttrEntityVo();
        attr4.setAttrId(2L);
        attr4.setValueList(new ArrayList<String>() {
            {
                this.add("a");
            }
        });
        oldAttrList.add(attr3);
        oldAttrList.add(attr4);

        newAttrList.removeAll(oldAttrList);
        System.out.println(newAttrList.size());
    }

}
