/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.legalvalid;

import codedriver.framework.cmdb.dto.legalvalid.IllegalCiEntityVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface IllegalCiEntityMapper {
    /**
     * 获取不合规配置项数量
     *
     * @param ciId 模型id
     * @return 数量
     */
    int getIllegalCiEntityCountByCiId(Long ciId);

    List<IllegalCiEntityVo> searchIllegalCiEntity(IllegalCiEntityVo illegalCiEntityVo);

    int searchIllegalCiEntityCount(IllegalCiEntityVo illegalCiEntityVo);

    void insertCiEntityIllegal(IllegalCiEntityVo illegalCiEntityVo);

    void deleteCiEntityIllegal(@Param("ciEntityId") Long ciEntityId, @Param("legalValidId") Long legalValidId);


}
