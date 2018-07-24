package io.choerodon.wiki.domain.application.repository;

import java.util.List;

import io.choerodon.core.domain.Page;
import io.choerodon.mybatis.pagehelper.domain.PageRequest;
import io.choerodon.wiki.domain.application.entity.WikiSpaceE;

/**
 * Created by Zenger on 2018/7/2.
 */
public interface WikiSpaceRepository {

    List<WikiSpaceE> getWikiSpaceList(Long resourceId, String resourceType);

    Boolean deleteSpaceById(Long id);

    WikiSpaceE insert(WikiSpaceE wikiSpaceE);

    WikiSpaceE insertIfNotExist(WikiSpaceE wikiSpaceE);

    Page<WikiSpaceE> listWikiSpaceByPage(Long resourceId, String type,
                                         PageRequest pageRequest, String searchParam);

    WikiSpaceE selectById(Long id);

    Boolean checkName(Long projectId, String name, String type);

    WikiSpaceE update(WikiSpaceE wikiSpaceE);
}
