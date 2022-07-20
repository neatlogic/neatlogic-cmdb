/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.api.resourcecenter;

import codedriver.framework.asynchronization.threadlocal.TenantContext;
import codedriver.framework.auth.core.AuthAction;
import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceCenterConfigVo;
import codedriver.framework.cmdb.dto.resourcecenter.customview.ICustomView;
import codedriver.framework.cmdb.dto.resourcecenter.customview.ResourceCustomViewFactory;
import codedriver.framework.cmdb.exception.resourcecenter.ResourceCenterConfigNotFoundException;
import codedriver.framework.dao.mapper.SchemaMapper;
import codedriver.framework.restful.annotation.Description;
import codedriver.framework.restful.annotation.OperationType;
import codedriver.framework.restful.constvalue.OperationTypeEnum;
import codedriver.framework.restful.core.privateapi.PrivateApiComponentBase;
import codedriver.module.cmdb.auth.label.RESOURCECENTER_MODIFY;
import codedriver.module.cmdb.dao.mapper.resourcecenter.ResourceCenterConfigMapper;
import codedriver.module.cmdb.utils.ResourceEntityViewBuilder;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@AuthAction(action = RESOURCECENTER_MODIFY.class)
@OperationType(type = OperationTypeEnum.OPERATE)
@Transactional
public class InitResourceCenterApi extends PrivateApiComponentBase {
    static Logger logger = LoggerFactory.getLogger(InitResourceCenterApi.class);

    @Resource
    private ResourceCenterConfigMapper resourceCenterConfigMapper;

    @Resource
    private SchemaMapper schemaMapper;

    @Override
    public String getToken() {
        return "/cmdb/resourcecenter/init";
    }

    @Override
    public String getName() {
        return "初始化资源中心配置";
    }

    @Override
    public String getConfig() {
        return null;
    }

    @Description(desc = "初始化资源中心配置接口")
    @Override
    public Object myDoService(JSONObject jsonObj) throws Exception {
//        ResourceCenterConfigVo configVo = resourceCenterConfigMapper.getResourceCenterConfig();
//        if (configVo == null) {
//            throw new ResourceCenterConfigNotFoundException();
//        }
//        ResourceEntityViewBuilder builder = new ResourceEntityViewBuilder(configVo.getConfig());
//        builder.buildView();
        // 创建自定义视图
//        List<ICustomView> custonViewList = ResourceCustomViewFactory.getCustomViewList();
//        for (ICustomView custonView : custonViewList) {
//            String selectBody = custonView.getSelectBody();
//            if (StringUtils.isNotBlank(selectBody)) {
//                try {
//                    String sql = "CREATE OR REPLACE VIEW " + TenantContext.get().getDataDbName() + "." + custonView.getName() + " AS " + selectBody;
//                    if (logger.isDebugEnabled()) {
//                        logger.debug(sql);
//                    }
//                    schemaMapper.insertView(sql);
//                } catch (Exception ex) {
//                    logger.error(ex.getMessage(), ex);
//                }
//            }
//        }
        return null;
    }

}
