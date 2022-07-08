/*
 * Copyright(c) 2021 TechSureCo.,Ltd.AllRightsReserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.dto.resourcecenter.config.ResourceInfo;
import codedriver.framework.cmdb.utils.ResourceSearchGenerateSqlUtil;
import com.alibaba.fastjson.JSONObject;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiConsumer;

public interface ResourceCenterCustomGenerateSqlService {

    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByProtocolIdList(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList);

    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByTagIdList(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList);

    BiConsumer<ResourceSearchGenerateSqlUtil, PlainSelect> getBiConsumerByKeyword(JSONObject paramObj, List<ResourceInfo> unavailableResourceInfoList);

}
