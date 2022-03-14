package codedriver.module.cmdb.dependency;

import codedriver.framework.cmdb.enums.CmdbFromType;
import codedriver.framework.dependency.core.CustomTableDependencyHandlerBase;
import codedriver.framework.dependency.core.IFromType;
import codedriver.framework.dependency.dto.DependencyInfoVo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author longrf
 * @date 2022/3/11 5:14 下午
 */
@Service
public class ResourceAccountDependencyHandler extends CustomTableDependencyHandlerBase {

    @Override
    protected String getTableName() {
        return "cmdb_resourcecenter_resource_account";
    }

    @Override
    protected String getFromField() {
        return "account_id";
    }

    @Override
    protected String getToField() {
        return "resource_id";
    }

    @Override
    protected List<String> getToFieldList() {
        return null;
    }

    @Override
    protected DependencyInfoVo parse(Object dependencyObj) {
        return null;
    }

    @Override
    public IFromType getFromType() {
        return CmdbFromType.RESOURCE_ACCOUNT;
    }
}
