package io.choerodon.wiki.domain.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;

import io.choerodon.core.exception.CommonException;
import io.choerodon.wiki.domain.service.IWikiGroupService;
import io.choerodon.wiki.infra.common.FileUtil;
import io.choerodon.wiki.infra.feign.WikiClient;

/**
 * Created by Ernst on 2018/7/6.
 */
@Service
public class IWikiGroupServiceImpl implements IWikiGroupService {

    private static final Logger logger = LoggerFactory.getLogger(IWikiGroupServiceImpl.class);

    @Value("${wiki.client}")
    private String client;

    private WikiClient wikiClient;

    private IWikiUserServiceImpl iWikiUserService;

    public IWikiGroupServiceImpl(WikiClient wikiClient, IWikiUserServiceImpl iWikiUserService) {
        this.wikiClient = wikiClient;
        this.iWikiUserService = iWikiUserService;
    }

    @Override
    public Boolean createGroup(String groupName, String username) {
        try {
            RequestBody requestBody = RequestBody.create(MediaType.parse("Content-Type, application/xml"), getGroupXml());
            Call<ResponseBody> call = wikiClient.createGroup(username,
                    client, groupName, requestBody);
            Response response = call.execute();
            if (response.code() == 201 || response.code() == 202) {
                return true;
            } else {
                throw new CommonException("error.create.group");
            }
        } catch (IOException e) {
            logger.error(e.getMessage());
            throw new CommonException("error.create.group");
        }
    }

    @Override
    public Boolean createGroupUsers(String groupName, String loginName, String username) {
        try {
            //如果组不存在则新建组
            Boolean falg = iWikiUserService.checkDocExsist(username, groupName);
            if (!falg) {
                this.createGroup(groupName,username);
            }

            FormBody body = new FormBody.Builder().add("className", "XWiki.XWikiGroups").add("property#member", "XWiki." + loginName).build();
            Call<ResponseBody> call = wikiClient.createGroupUsers(username, client, groupName, body);
            Response response = call.execute();
            if (response.code() == 201) {
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean disableOrgGroupView(String organizationCode, String organizationName, String username) {
        try {
            String groupName = "O-" + organizationCode + "UserGroup";
            String param = "O-" + organizationName;
            Boolean falg = iWikiUserService.checkDocExsist(username, groupName);
            if (!falg) {
                throw new CommonException("error.query.group");
            }

            FormBody body = new FormBody.Builder().add("className", "XWiki.XWikiGlobalRights").add("property#allow", "0")
                    .add("property#groups", "XWiki." + groupName).add("property#levels", "view").build();

            Call<ResponseBody> call = wikiClient.offerRightToOrgGroupView(username, client, param, body);
            Response response = call.execute();
            if (response.code() == 201) {
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean disableProjectGroupView(String projectName, String projectCode, String organizationName, String username) {
        try {
            String groupName = "P-" + projectCode + "UserGroup";
            Boolean falg = iWikiUserService.checkDocExsist(username, groupName);
            if (!falg) {
                throw new CommonException("error.query.group");
            }

            FormBody body = new FormBody.Builder().add("className", "XWiki.XWikiGlobalRights").add("property#allow", "0")
                    .add("property#groups", "XWiki." + groupName).add("property#levels", "view").build();

            URLEncoder.encode(organizationName,"UTF-8");
            Call<ResponseBody> call = wikiClient.offerRightToProjectGroupView(username, client, organizationName, projectName, body);
            Response response = call.execute();
            if (response.code() == 201) {
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean addRightsToOrg(String organizationCode, String organizationName, List<String> rights,Boolean isAdmin, String username) {
        try {
            String groupName = "O-" + organizationCode + (isAdmin? "AdminGroup":"UserGroup");
            Boolean falg = iWikiUserService.checkDocExsist(username, groupName);
            if (!falg) {
                throw new CommonException("error.query.group");
            }
            StringBuffer stringBuffer = new StringBuffer();
            for(String right:rights){
                stringBuffer.append(right);
                stringBuffer.append(",");
            }
            String levels = stringBuffer.toString();
            levels = levels.substring(0,levels.length()-1);

            FormBody body = new FormBody.Builder().add("className", "XWiki.XWikiGlobalRights").add("property#allow", "1")
                    .add("property#groups", "XWiki." + groupName).add("property#levels", levels).build();

            String encodeStr = "O-"+organizationName;
            URLEncoder.encode(encodeStr,"UTF-8");
            Call<ResponseBody> call = wikiClient.offerRightToOrgGroupView(username, client, encodeStr, body);
            Response response = call.execute();
            logger.info("u:"+username);
            logger.info("client"+client);
            logger.info("O-"+organizationName+":"+response.code());
            if (response.code() == 201) {
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    @Override
    public Boolean addRightsToProject(String projectName, String projectCode, String organizationName,List<String> rights,Boolean isAdmin, String username) {
        try {
            String groupName = "P-" + projectCode + (isAdmin?"AdminGroup":"UserGroup");
            Boolean falg = iWikiUserService.checkDocExsist(username, groupName);
            if (!falg) {
                throw new CommonException("error.query.group");
            }

            StringBuffer stringBuffer = new StringBuffer();
            for(String right:rights){
                stringBuffer.append(right);
                stringBuffer.append(",");
            }
            String levels = stringBuffer.toString();
            levels = levels.substring(0,levels.length()-1);

            FormBody body = new FormBody.Builder().add("className", "XWiki.XWikiGlobalRights").add("property#allow", "1")
                    .add("property#groups", "XWiki." + groupName).add("property#levels", levels).build();

            String encodeStr = "O-"+organizationName;
            URLEncoder.encode(encodeStr,"UTF-8");
            Call<ResponseBody> call = wikiClient.offerRightToProjectGroupView(username, client, encodeStr, "P-"+projectName, body);
            Response response = call.execute();
            if (response.code() == 201) {
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error(e.getMessage());
            return false;
        }
    }

    private String getGroupXml() {
        InputStream inputStream = this.getClass().getResourceAsStream("/xml/group.xml");
        Map<String, String> params = new HashMap<>();
        return FileUtil.replaceReturnString(inputStream, params);
    }

}