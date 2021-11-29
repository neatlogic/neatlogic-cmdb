/*
 * Copyright (c)  2021 TechSure Co.,Ltd.  All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.service.resourcecenter.resource;

import codedriver.framework.cmdb.dto.resourcecenter.ResourceSearchVo;
import com.alibaba.fastjson.JSONObject;


/**
 * @author laiwt
 * @since 2021/11/22 14:41
 **/
public interface ResourceCenterResourceService {

    ResourceSearchVo assembleResourceSearchVo(JSONObject jsonObj);


}
