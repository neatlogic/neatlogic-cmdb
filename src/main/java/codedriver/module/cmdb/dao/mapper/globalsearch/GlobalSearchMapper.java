/*
 * Copyright(c) 2021 TechSure Co., Ltd. All Rights Reserved.
 * 本内容仅限于深圳市赞悦科技有限公司内部传阅，禁止外泄以及用于其他的商业项目。
 */

package codedriver.module.cmdb.dao.mapper.globalsearch;

import codedriver.framework.cmdb.dto.ci.CiVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface GlobalSearchMapper {
    List<CiVo> searchCiEntityCountByWord(@Param("wordList") List<String> wordList);
}
