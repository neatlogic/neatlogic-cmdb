package codedriver.module.cmdb.cischema;

import codedriver.framework.asynchronization.thread.CodeDriverThread;
import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.batch.BatchJob;
import codedriver.framework.batch.BatchRunner;
import codedriver.framework.common.config.Config;
import codedriver.framework.exception.database.DataBaseNotFoundException;
import codedriver.framework.transaction.core.AfterTransactionJob;
import codedriver.framework.transaction.core.ICommitted;
import codedriver.module.cmdb.dao.mapper.ci.AttrMapper;
import codedriver.module.cmdb.dao.mapper.ci.CiMapper;
import codedriver.module.cmdb.dao.mapper.cischema.CiSchemaMapper;
import codedriver.module.cmdb.dto.ci.AttrVo;
import codedriver.module.cmdb.dto.ci.CiVo;
import codedriver.module.cmdb.dto.ci.RelVo;
import codedriver.module.cmdb.dto.cientity.AttrEntityVo;
import codedriver.module.cmdb.dto.cientity.CiEntityVo;
import codedriver.module.cmdb.dto.cientity.RelEntityVo;
import codedriver.module.cmdb.dto.schema.SchemaAuditVo;
import codedriver.module.cmdb.enums.SchemaActionType;
import codedriver.module.cmdb.enums.SchemaTargetType;
import codedriver.module.cmdb.service.cientity.CiEntityService;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class CiSchemaHandler {
    private static final ThreadLocal<List<SchemaAuditVo>> SCHEMAAUDIT_THREADLOCAL = new ThreadLocal<>();
    private static CiSchemaMapper ciSchemaMapper;
    private static CiEntityService ciEntityService;
    private static AttrMapper attrMapper;
    private static CiMapper ciMapper;
    protected static CodeDriverThread schemaHandlerThread = null;
    private static final int parallel = 10;
    private static final Object lock = new Object();
    private static String currentTenantUuid = null;

    public static void notifyWorker() {
        synchronized (lock) {
            currentTenantUuid = TenantContext.get().getTenantUuid();
            lock.notifyAll();
        }
    }

    @PostConstruct
    public void init() {
        //启动schema线程，每个实例只有一个
        schemaHandlerThread = new CodeDriverThread() {
            @Override
            protected void execute() {
                while (true) {
                    if (StringUtils.isNotBlank(currentTenantUuid)) {
                        //切换租户数据库
                        TenantContext.init(currentTenantUuid).setUseDefaultDatasource(false);
                        SchemaAuditVo auditVo = ciSchemaMapper.getLatestSchemaAudit(Config.SCHEDULE_SERVER_ID);
                        if (auditVo != null) {
                            String oldName = Thread.currentThread().getName();
                            Thread.currentThread().setName(oldName + "-" + auditVo.getTargetId());
                            try {
                                if (auditVo.getTargetType().equals(SchemaTargetType.CI.toString())) {
                                    if (auditVo.getAction().equals(SchemaActionType.INSERT.toString())) {
                                        doInitCiSchema(auditVo);
                                    } else if (auditVo.getAction().equals(SchemaActionType.DELETE.toString())) {
                                        doDeleteCiSchema(auditVo);
                                    }
                                } else if (auditVo.getTargetType().equals(SchemaTargetType.ATTR.toString())) {
                                    if (auditVo.getAction().equals(SchemaActionType.INSERT.toString())) {
                                        doInsertAttr(auditVo);
                                    } else if (auditVo.getAction().equals(SchemaActionType.DELETE.toString())) {
                                        doDeleteAttr(auditVo);
                                    }
                                } else if (auditVo.getTargetType().equals(SchemaTargetType.CIENTITY.toString())) {
                                    if (auditVo.getAction().equals(SchemaActionType.UPDATE.toString())) {
                                        doUpdateCiEntity(auditVo);
                                    } else if (auditVo.getAction().equals(SchemaActionType.DELETE.toString())) {
                                        doDeleteCiEntity(auditVo);
                                    } else if (auditVo.getAction().equals(SchemaActionType.INSERT.toString())) {
                                        doInsertCiEntity(auditVo);
                                    }
                                }
                                ciSchemaMapper.deleteSchemaAuditById(auditVo.getId());
                            } catch (Exception ex) {
                                ciSchemaMapper.updateSchemaAuditIsFailed(auditVo.getId());
                            }
                            Thread.currentThread().setName(oldName);
                        } else {
                            try {
                                synchronized (lock) {
                                    //释放当前租户
                                    TenantContext.get().release();
                                    currentTenantUuid = null;
                                    lock.wait();
                                }
                            } catch (InterruptedException ignored) {
                            }
                        }
                    } else {
                        try {
                            synchronized (lock) {
                                lock.wait();
                            }
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
            }
        };
        Thread t = new Thread(schemaHandlerThread);
        t.setName("CI_SCHEMA_HANDLER");
        t.start();
    }

    @Autowired
    public CiSchemaHandler(CiSchemaMapper _ciSchemaMapper, AttrMapper _attrMapper, CiEntityService _ciEntityService, CiMapper _ciMapper) {
        ciSchemaMapper = _ciSchemaMapper;
        attrMapper = _attrMapper;
        ciEntityService = _ciEntityService;
        ciMapper = _ciMapper;
    }

    public static void insertCiEntity(CiEntityVo ciEntityVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.CIENTITY.toString());
        auditVo.setAction(SchemaActionType.INSERT.toString());
        auditVo.setTargetId(ciEntityVo.getId());
        saveAudit(auditVo);
    }

    public static void updateCiEntity(CiEntityVo ciEntityVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.CIENTITY.toString());
        auditVo.setAction(SchemaActionType.UPDATE.toString());
        auditVo.setTargetId(ciEntityVo.getId());
        saveAudit(auditVo);
    }

    public static void deleteCiEntity(CiEntityVo ciEntityVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.CIENTITY.toString());
        auditVo.setAction(SchemaActionType.DELETE.toString());
        auditVo.setTargetId(ciEntityVo.getId());
        auditVo.setDataStr(JSONObject.toJSONString(ciEntityVo));
        saveAudit(auditVo);
    }

    public static void deleteCi(CiVo ciVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.CI.toString());
        auditVo.setAction(SchemaActionType.DELETE.toString());
        auditVo.setTargetId(ciVo.getId());
        auditVo.setDataStr(JSONObject.toJSONString(ciVo));
        saveAudit(auditVo);
    }

    public static void deleteRel(RelVo relVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.REL.toString());
        auditVo.setAction(SchemaActionType.DELETE.toString());
        auditVo.setTargetId(relVo.getId());
        auditVo.setDataStr(JSONObject.toJSONString(relVo));
        saveAudit(auditVo);
    }

    public static void deleteAttr(AttrVo attrVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.ATTR.toString());
        auditVo.setAction(SchemaActionType.DELETE.toString());
        auditVo.setTargetId(attrVo.getId());
        auditVo.setDataStr(JSONObject.toJSONString(attrVo));
        saveAudit(auditVo);
    }

    /**
     * @Description: 重建模型schema，一般在新建模型或修改模型时调用
     * @Author: chenqiwei
     * @Date: 2020/12/31 5:00 下午
     * @Params: [ciId]
     * @Returns: void
     **/
    public static void initCiSchema(CiVo ciVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.CI.toString());
        auditVo.setAction(SchemaActionType.INSERT.toString());
        auditVo.setTargetId(ciVo.getId());
        saveAudit(auditVo);
    }

    /**
     * @Description: 模型表中增加列
     * @Author: chenqiwei
     * @Date: 2020/12/31 6:06 下午
     * @Params: [attrId]
     * @Returns: void
     **/
    public static void insertAttr(AttrVo attrVo) {
        SchemaAuditVo auditVo = new SchemaAuditVo();
        auditVo.setTargetType(SchemaTargetType.ATTR.toString());
        auditVo.setAction(SchemaActionType.INSERT.toString());
        auditVo.setTargetId(attrVo.getId());
        auditVo.setDataStr(JSONObject.toJSONString(attrVo));
        saveAudit(auditVo);
    }


    private static void saveAudit(SchemaAuditVo auditVo) {
        AfterTransactionJob<SchemaAuditVo> committer = new AfterTransactionJob<>();
        committer.execute(auditVo, new ICommitted<SchemaAuditVo>() {
            @Override
            public void execute(SchemaAuditVo schemaAuditVo) {
                ciSchemaMapper.replaceSchemaAudit(schemaAuditVo);
                notifyWorker();
            }
        });

        /*
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            ciSchemaMapper.replaceSchemaAudit(auditVo);
            notifyWorker();
        } else {
            List<SchemaAuditVo> schemaAuditList = SCHEMAAUDIT_THREADLOCAL.get();
            if (schemaAuditList == null) {
                schemaAuditList = new ArrayList<>();
                SCHEMAAUDIT_THREADLOCAL.set(schemaAuditList);
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCommit() {
                        //新起线程执行，避免当前线程事务没提交完毕导致更新视图不及时
                        List<SchemaAuditVo> schemaAuditList = SCHEMAAUDIT_THREADLOCAL.get();
                        CachedThreadPool.execute(new CodeDriverThread() {
                            @Override
                            protected void execute() {
                                for (SchemaAuditVo audit : schemaAuditList) {
                                    ciSchemaMapper.replaceSchemaAudit(audit);
                                }
                                notifyWorker();
                            }
                        });
                    }

                    @Override
                    public void afterCompletion(int status) {
                        SCHEMAAUDIT_THREADLOCAL.remove();
                    }
                });
            }
            schemaAuditList.add(auditVo);
        }*/
    }


    private static void doDeleteAttr(SchemaAuditVo auditVo) {
        if (auditVo != null && auditVo.getData() != null) {
            AttrVo attrVo = JSONObject.toJavaObject(auditVo.getData(), AttrVo.class);
            if (attrVo != null) {
                CiVo ciVo = ciMapper.getCiById(attrVo.getCiId());
                final String SCHEMA_DB_NAME = "codedriver_" + TenantContext.get().getTenantUuid() + "_data";
                final String CI_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_ci_";
                if (ciSchemaMapper.checkDatabaseIsExists(SCHEMA_DB_NAME) > 0) {
                    ciSchemaMapper.deleteAttr(CI_SCHEMA_NAME_PREFIX + ciVo.getName(), attrVo.getName());
                } else {
                    throw new DataBaseNotFoundException();
                }
            }
        }
    }

    private static void doInsertCiEntity(SchemaAuditVo auditVo) {
        if (auditVo != null) {
            final String SCHEMA_DB_NAME = "codedriver_" + TenantContext.get().getTenantUuid() + "_data";
            final String CI_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_ci_";
            final String REL_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_rel_";
            Long ciEntityId = auditVo.getTargetId();
            CiEntityVo ciEntity = ciEntityService.getCiEntityDetailById(ciEntityId);
            if (ciEntity != null) {
                CiVo ciVo = ciMapper.getCiById(ciEntity.getCiId());
                List<RelEntityVo> relEntityList = ciEntity.getRelEntityList();
                ciSchemaMapper.insertCiEntity(CI_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntity);

                if (CollectionUtils.isNotEmpty(relEntityList)) {
                    ciSchemaMapper.insertRelEntity(REL_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntity);
                }
            }
        }
    }

    private static void doUpdateCiEntity(SchemaAuditVo auditVo) {
        if (auditVo != null) {
            final String SCHEMA_DB_NAME = "codedriver_" + TenantContext.get().getTenantUuid() + "_data";
            final String CI_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_ci_";
            final String REL_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_rel_";
            Long ciEntityId = auditVo.getTargetId();
            CiEntityVo ciEntity = ciEntityService.getCiEntityDetailById(ciEntityId);
            if (ciEntity != null) {
                CiVo ciVo = ciMapper.getCiById(ciEntity.getCiId());
                //先清空数据
                ciSchemaMapper.deleteCiEntityById(CI_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntityId);
                ciSchemaMapper.deleteCiEntityRelByCiEntityId(REL_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntityId);

                List<RelEntityVo> relEntityList = ciEntity.getRelEntityList();
                ciSchemaMapper.insertCiEntity(CI_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntity);

                if (CollectionUtils.isNotEmpty(relEntityList)) {
                    ciSchemaMapper.insertRelEntity(REL_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntity);
                }
            }
        }
    }

    private static void doDeleteCiEntity(SchemaAuditVo auditVo) {
        if (auditVo != null && auditVo.getData() != null) {
            Long ciEntityId = auditVo.getTargetId();
            CiEntityVo ciEntityVo = JSONObject.toJavaObject(auditVo.getData(), CiEntityVo.class);
            if (ciEntityVo != null && ciEntityVo.getCiId() != null) {
                final String SCHEMA_DB_NAME = "codedriver_" + TenantContext.get().getTenantUuid() + "_data";
                final String CI_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_ci_";
                final String REL_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_rel_";
                CiVo ciVo = ciMapper.getCiById(ciEntityVo.getCiId());
                ciSchemaMapper.deleteCiEntityById(CI_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntityId);
                ciSchemaMapper.deleteCiEntityRelByCiEntityId(REL_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntityId);
            }
        }
    }

    private static void doInsertAttr(SchemaAuditVo auditVo) {
        if (auditVo != null && auditVo.getData() != null) {
            AttrVo attrVo = JSONObject.toJavaObject(auditVo.getData(), AttrVo.class);
            if (attrVo != null) {
                CiVo ciVo = ciMapper.getCiById(attrVo.getCiId());
                final String SCHEMA_DB_NAME = "codedriver_" + TenantContext.get().getTenantUuid() + "_data";
                final String CI_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_ci_";
                if (ciSchemaMapper.checkDatabaseIsExists(SCHEMA_DB_NAME) > 0) {
                    ciSchemaMapper.insertAttr(CI_SCHEMA_NAME_PREFIX + ciVo.getName(), attrVo);
                } else {
                    throw new DataBaseNotFoundException();
                }
            }
        }
    }

    /*
     * @Description: 删除配置项模型，相关表会被删除
     * @Author: chenqiwei
     * @Date: 2021/1/5 5:03 下午
     * @Params: [auditVo]
     * @Returns: void
     **/
    private static void doDeleteCiSchema(SchemaAuditVo auditVo) {
        if (auditVo != null && auditVo.getData() != null) {
            CiVo ciVo = JSONObject.toJavaObject(auditVo.getData(), CiVo.class);
            if (ciVo != null) {
                final String SCHEMA_DB_NAME = "codedriver_" + TenantContext.get().getTenantUuid() + "_data";
                final String CI_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_ci_";
                final String REL_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_rel_";

                ciSchemaMapper.deleteCiSchema(CI_SCHEMA_NAME_PREFIX + ciVo.getName());
                ciSchemaMapper.deleteRelSchema(REL_SCHEMA_NAME_PREFIX + ciVo.getName());
            }
        }
    }

    /*
     * @Description: 初始化整个模型，数据会删除并重新导入
     * @Author: chenqiwei
     * @Date: 2021/1/5 5:04 下午
     * @Params: [auditVo]
     * @Returns: void
     **/
    private static void doInitCiSchema(SchemaAuditVo auditVo) {
        if (auditVo != null) {
            CiVo ciVo = ciMapper.getCiById(auditVo.getTargetId());
            if (ciVo != null) {
                final String SCHEMA_DB_NAME = "codedriver_" + TenantContext.get().getTenantUuid() + "_data";
                final String CI_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_ci_";
                final String REL_SCHEMA_NAME_PREFIX = SCHEMA_DB_NAME + ".cmdb_rel_";
                if (ciSchemaMapper.checkDatabaseIsExists(SCHEMA_DB_NAME) > 0) {
                    //删除旧表
                    ciSchemaMapper.deleteCiSchema(CI_SCHEMA_NAME_PREFIX + ciVo.getName());
                    ciSchemaMapper.deleteRelSchema(REL_SCHEMA_NAME_PREFIX + ciVo.getName());
                    //创建新表
                    List<AttrVo> attrList = attrMapper.getAttrByCiId(ciVo.getId());
                    ciSchemaMapper.insertCiSchema(CI_SCHEMA_NAME_PREFIX + ciVo.getName());
                    ciSchemaMapper.insertRelSchema(REL_SCHEMA_NAME_PREFIX + ciVo.getName());

                    //同步数据，1000条数据一个循环
                    CiEntityVo ciEntityVo = new CiEntityVo();
                    ciEntityVo.setPageSize(1000);
                    ciEntityVo.setCiId(ciVo.getId());
                    List<CiEntityVo> ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
                    while (CollectionUtils.isNotEmpty(ciEntityList)) {
                        BatchRunner<CiEntityVo> runner = new BatchRunner<>();
                        runner.execute(ciEntityList, parallel, new BatchJob<CiEntityVo>() {
                            @Override
                            public void execute(CiEntityVo ciEntity) {
                                List<AttrEntityVo> attrEntityList = ciEntity.getAttrEntityList();
                                List<RelEntityVo> relEntityList = ciEntity.getRelEntityList();
                                // if (CollectionUtils.isNotEmpty(attrEntityList)) {
                                ciSchemaMapper.insertCiEntity(CI_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntity);
                                //}

                                if (CollectionUtils.isNotEmpty(relEntityList)) {
                                    ciSchemaMapper.insertRelEntity(REL_SCHEMA_NAME_PREFIX + ciVo.getName(), ciEntity);
                                }
                            }
                        });
                        ciEntityVo.setCurrentPage(ciEntityVo.getCurrentPage() + 1);
                        ciEntityList = ciEntityService.searchCiEntity(ciEntityVo);
                    }

                } else {
                    throw new DataBaseNotFoundException();
                }
            }
        }
    }

}
