package com.olacabs.jackhammer.utilities;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import com.olacabs.jackhammer.configuration.JackhammerConfiguration;
import com.olacabs.jackhammer.models.*;
import com.olacabs.jackhammer.security.AES;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;


import com.olacabs.jackhammer.db.*;
import com.olacabs.jackhammer.common.Constants;
import org.apache.commons.lang3.StringUtils;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import javax.xml.bind.DatatypeConverter;

import java.io.UnsupportedEncodingException;
import java.util.*;
import org.json.JSONObject;

@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class GitUtil {

    @Inject
    @Named(Constants.GROUP_DAO)
    GroupDAO groupDAO;


    @Inject
    @Named(Constants.REPO_DAO)
    RepoDAO repoDAO;

    @Inject
    @Named(Constants.SCAN_TYPE_DAO)
    ScanTypeDAO scanTypeDAO;

    @Inject
    @Named(Constants.OWNER_TYPE_DAO)
    OwnerTypeDAO ownerTypeDAO;

    @Inject
    @Named(Constants.GIT_DAO)
    GitDAO gitDAO;

    @Inject
    @Named(Constants.SCHEDULE_TYPE_DAO)
    ScheduleTypeDAO scheduleTypeDAO;

    @Inject
    @Named(Constants.SCAN_DAO)
    ScanDAO scanDAO;

    @Inject
    JackhammerConfiguration jackhammerConfiguration;

    public void pullGitRepos() {
        try {
            Git git = gitDAO.get();
            if (StringUtils.equals(git.getGitType().toLowerCase(), Constants.GITHUB)) {
                pullGitHubInfo();
                log.info("Repo Type==> {}...{}", "Github");
            } else if (StringUtils.equals(git.getGitType().toLowerCase(), Constants.GITLAB)) {
                log.info("Repo Type==> {}...{}", "Gitlb");
                pullGitLabInfo();
            } else if (StringUtils.equals(git.getGitType().toLowerCase(), Constants.BITBUCKET)) {
                log.info("Repo Type==> {}...{}", "BitBucket");
                pullBitBucketInfo();
            }
        } catch (Throwable t) {
            log.info("Exception while getting repos ", t);
        }
    }

    public void pullBitBucketInfo() {
        List<BitBucketGroup> bitBucketGroups = getBitBucketGroups();
        try {
            OwnerType ownerType = ownerTypeDAO.getDefaultOwnerType();
            ScanType scanType = scanTypeDAO.getStaticScanType();
            log.info("Total bitbucket groups=> {} {}", bitBucketGroups.size());
            for (BitBucketGroup bitBucketGroup : bitBucketGroups) {
                Group group = groupDAO.findGroupByName(bitBucketGroup.getName());
                long groupId = group == null ? 0 : group.getId();
                if (group == null) {
                    Group newGroup = new Group();
                    newGroup.setName(bitBucketGroup.getName());
                    groupId = groupDAO.insert(newGroup);
                }
                for (BitBucketProject bitBucketProject : bitBucketGroup.getBitBucketProjects()) {
                    Repo repo = repoDAO.findRepoByName(bitBucketProject.getName());
                    try {
                        if (repo == null) {
                            log.info("Adding new repo with ....{}",bitBucketProject.getName());
                            Repo newRepo = new Repo();
                            newRepo.setName(bitBucketProject.getName());
                            newRepo.setGroupId(groupId);
                            newRepo.setOwnerTypeId(ownerType.getId());
                            newRepo.setScanTypeId(scanType.getId());
                            newRepo.setTarget(bitBucketProject.getLinks_html());
                            int repId = repoDAO.insert(newRepo);

                            //insert scan
                            log.info("Creating new scan .......");
                            ScheduleType scheduleType = scheduleTypeDAO.findScheduleByName(Constants.WEEKLY);
                            Scan scan = new Scan();
                            scan.setName(newRepo.getName());
                            scan.setGroupId(newRepo.getGroupId());
                            scan.setOwnerTypeId(newRepo.getOwnerTypeId());
                            scan.setScanTypeId(newRepo.getScanTypeId());
                            scan.setStatus(Constants.SCAN_QUEUED_STATUS);
                            scan.setTarget(newRepo.getTarget());
                            scan.setScheduleTypeId(scheduleType.getId());
                            scan.setRepoId(repId);
                            scanDAO.insert(scan);
                        }
                    } catch (Exception e) {
                        log.error("Error while creating new repo/ new scan...{}..{}", e);
                    }
                }
            }
        } catch (Throwable t) {
            log.error("Exception while reading fetched groups", t);
        }
    }

    public void pullGitLabInfo() {
        List<GitLabGroup> gitlabGroups = getGitLabGroups();
        try {
            OwnerType ownerType = ownerTypeDAO.getDefaultOwnerType();
            ScanType scanType = scanTypeDAO.getStaticScanType();
            log.info("Total gitlab groups=> {} {}", gitlabGroups.size());
            for (GitLabGroup gitLabGroup : gitlabGroups) {
                Group group = groupDAO.findGroupByName(gitLabGroup.getName());
                long groupId = group == null ? 0 : group.getId();
                if (group == null) {
                    Group newGroup = new Group();
                    newGroup.setName(gitLabGroup.getName());
                    groupId = groupDAO.insert(newGroup);
                }
                for (GitLabProject gitLabProject : gitLabGroup.getGitLabProjects()) {
                    Repo repo = repoDAO.findRepoByName(gitLabProject.getName());
                    try {
                        if (repo == null) {
                            log.info("Adding new repo with ....{}",gitLabProject.getName());
                            Repo newRepo = new Repo();
                            newRepo.setName(gitLabProject.getName());
                            newRepo.setGroupId(groupId);
                            newRepo.setOwnerTypeId(ownerType.getId());
                            newRepo.setScanTypeId(scanType.getId());
                            newRepo.setTarget(gitLabProject.getHttp_url_to_repo());
                            int repId = repoDAO.insert(newRepo);

                            //insert scan
                            log.info("Creating new scan .......");
                            ScheduleType scheduleType = scheduleTypeDAO.findScheduleByName(Constants.WEEKLY);
                            Scan scan = new Scan();
                            scan.setName(newRepo.getName());
                            scan.setGroupId(newRepo.getGroupId());
                            scan.setOwnerTypeId(newRepo.getOwnerTypeId());
                            scan.setScanTypeId(newRepo.getScanTypeId());
                            scan.setStatus(Constants.SCAN_QUEUED_STATUS);
                            scan.setTarget(newRepo.getTarget());
                            scan.setScheduleTypeId(scheduleType.getId());
                            scan.setRepoId(repId);
                            scanDAO.insert(scan);
                        }
                    } catch (Exception e) {
                        log.error("Error while creating new repo/ new scan...{}..{}", e);
                    }
                }
            }
        } catch (Throwable t) {
            log.error("Exception while reading fetched groups", t);
        }
    }

    public void pullGitHubInfo() {
        List<GitHubGroup> gitHubGroups = getGiHubGroups();
        try {
            OwnerType ownerType = ownerTypeDAO.getDefaultOwnerType();
            ScanType scanType = scanTypeDAO.getStaticScanType();
            for (GitHubGroup gitHubGroup : gitHubGroups) {
                Group group = groupDAO.findGroupByName(gitHubGroup.getName());
                long groupId = group == null ? 0 : group.getId();
                if (group == null) {
                    Group newGroup = new Group();
                    newGroup.setName(gitHubGroup.getName());
                    groupId = groupDAO.insert(newGroup);
                }
                for (GitHubProject gitHubProject : gitHubGroup.getGitHubProjects()) {
                    Repo repo = repoDAO.findRepoByName(gitHubProject.getName());
                    if (repo == null) {
                        Repo newRepo = new Repo();
                        newRepo.setName(gitHubProject.getName());
                        newRepo.setGroupId(groupId);
                        newRepo.setOwnerTypeId(ownerType.getId());
                        newRepo.setScanTypeId(scanType.getId());
                        newRepo.setTarget(gitHubProject.getHtml_url());
                        repoDAO.insert(newRepo);
                    }
                }
            }
        } catch (Throwable t) {
            log.error("Exception while reading fetched groups", t);
        }
    }

    private List<BitBucketGroup> getBitBucketGroups() {
        List<BitBucketGroup> bitBucketGroups = new ArrayList<BitBucketGroup>();
        Git git = gitDAO.get();
        if (git != null) {
            try {
                String privateToken = AES.decrypt(git.getApiAccessToken(), jackhammerConfiguration.getJwtConfiguration().getTokenSigningKey());
                String accessToken = getBitBucketAccessToken(git.getUserName(),privateToken);

                StringBuilder targetBuilder = new StringBuilder();
                targetBuilder.append(Constants.BITBUCKET_API_URL);
                targetBuilder.append(Constants.BIT_BUCKET_GROUPS_END_POINT);
                targetBuilder.append(git.getOrganizationName());
                targetBuilder.append(Constants.GIT_PROJECTS_END_POINT);

                while (true) {                                        
                    WebTarget webTarget = ClientBuilder.newClient()
                            .target(targetBuilder.toString())
                            .queryParam(Constants.ACCESS_TOKEN, accessToken);
                    Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
                    String jsonString = response.readEntity(String.class);
                    JSONObject jsonResponse = new JSONObject(jsonString);
                    response.close();
                    
                    if (jsonResponse.has(Constants.VALUES)) {
                        for (Object record:jsonResponse.getJSONArray(Constants.VALUES)) {
                            BitBucketGroup bitBucketGroup = new BitBucketGroup();
                            JSONObject group = (JSONObject) record;

                            String name = group.getString(Constants.NAME);
                            String links_repositories = group.getJSONObject(Constants.LINKS).getJSONObject(Constants.REPOSITORIES).getString(Constants.HREF);

                            bitBucketGroup.setName(name);
                            bitBucketGroup.setLinks_repositories(links_repositories);
                            bitBucketGroups.add(bitBucketGroup);
                        }
                    }

                    if (jsonResponse.has(Constants.NEXT)) {
                        webTarget = ClientBuilder.newClient().target(jsonResponse.getString(Constants.NEXT));
                    } else {
                        break;
                    }
                }

                for (BitBucketGroup bitBucketGroup : bitBucketGroups) {
                    List<BitBucketProject> bitBucketProjects = new ArrayList<BitBucketProject>();

                    while (true) {
                        WebTarget webTarget = ClientBuilder.newClient()
                                .target(bitBucketGroup.getLinks_repositories())
                                .queryParam(Constants.ACCESS_TOKEN, privateToken);
                        
                        Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
                        String JsonString = response.readEntity(String.class);
                        JSONObject jsonResponse = new JSONObject(JsonString);
                        response.close();

                        if (jsonResponse.has(Constants.VALUES)) {
                            for (Object record:jsonResponse.getJSONArray(Constants.VALUES)) {
                                BitBucketProject bitBucketProject = new BitBucketProject();
                                JSONObject project = (JSONObject) record;

                                String name = project.getString(Constants.NAME);
                                String links_html = project.getJSONObject(Constants.LINKS).getJSONObject(Constants.HTML).getString(Constants.HREF);
                                
                                bitBucketProject.setName(name);
                                bitBucketProject.setLinks_html(links_html);
                                bitBucketProjects.add(bitBucketProject);
                            }
                        }

                        if (jsonResponse.has(Constants.NEXT)) {
                            webTarget = ClientBuilder.newClient().target(jsonResponse.getString(Constants.NEXT));
                        } else {
                            break;
                        }
                    }
                    bitBucketGroup.setBitBucketProjects(bitBucketProjects);
                }
            } catch (Throwable ex) {
                log.error("Throwable Exception while fetching groups ", ex);
            }
        }
        return bitBucketGroups;
    }

    private List<GitLabGroup> getGitLabGroups() {
        List<GitLabGroup> gitLabGroups = new ArrayList<GitLabGroup>();
        Git git = gitDAO.get();
        if (git != null) {
            int page = 1;
            try {
                String privateToken = AES.decrypt(git.getApiAccessToken(), jackhammerConfiguration.getJwtConfiguration().getTokenSigningKey());
                while (true) {
                    WebTarget webTarget = ClientBuilder.newClient()
                            .target(git.getGitEndPoint() + Constants.GIT_LAB_GROUPS_END_POINT)
                            .queryParam(Constants.PRIVATE_TOKEN, privateToken)
                            .queryParam(Constants.ALL_AVAILABLE, true)
                            .queryParam(Constants.PAGE, page);
                    Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
                    List<GitLabGroup> fetchedGroups = response.readEntity(new GenericType<List<GitLabGroup>>() {
                    });
                    if (fetchedGroups.size() == 0) break;
                    gitLabGroups.addAll(fetchedGroups);
                    page += 1;
                }

                for (GitLabGroup gitLabGroup : gitLabGroups) {
                    StringBuilder endPointBuilder = new StringBuilder();

                    endPointBuilder.append(git.getGitEndPoint());
                    endPointBuilder.append(Constants.GIT_LAB_GROUPS_END_POINT);
                    endPointBuilder.append(gitLabGroup.getId());
                    endPointBuilder.append(Constants.GIT_PROJECTS_END_POINT);
                    List<GitLabProject> gitLabProjects = new ArrayList<GitLabProject>();
                    page = 1;
                    while (true) {
                        WebTarget webTarget = ClientBuilder.newClient()
                                .target(endPointBuilder.toString())
                                .queryParam(Constants.PRIVATE_TOKEN, privateToken)
                                .queryParam(Constants.PAGE, page);
                        Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
                        List<GitLabProject> fetchedProjects = response.readEntity(new GenericType<List<GitLabProject>>() {
                        });
                        if (fetchedProjects.size() == 0) break;
                        gitLabProjects.addAll(fetchedProjects);
                        page += 1;
                    }
                    gitLabGroup.setGitLabProjects(gitLabProjects);
                }
            } catch (Throwable ex) {
                log.error("Throwable Exception while fetching groups ", ex);
            }
        }
        return gitLabGroups;
    }


    private List<GitHubGroup> getGiHubGroups() {
        List<GitHubGroup> gitHubGroups = new ArrayList<GitHubGroup>();
        Git git = gitDAO.get();
        if (git != null) {
            int page = 1;
            try {
                String privateToken = AES.decrypt(git.getApiAccessToken(), jackhammerConfiguration.getJwtConfiguration().getTokenSigningKey());
                while (true) {
                    StringBuilder webTargetBuilder = new StringBuilder();

                    webTargetBuilder.append(Constants.GITHUB_API_URL);
                    webTargetBuilder.append(Constants.ORGS);
                    webTargetBuilder.append(Constants.URL_SEPARATOR);
                    webTargetBuilder.append(git.getOrganizationName());
                    webTargetBuilder.append(Constants.URL_SEPARATOR);
                    webTargetBuilder.append(Constants.TEAMS);

                    WebTarget webTarget = ClientBuilder.newClient()
                            .target(webTargetBuilder.toString())
                            .queryParam(Constants.ACCESS_TOKEN, privateToken)
                            .queryParam(Constants.PAGE, page);
                    Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
                    List<GitHubGroup> fetchedGroups = response.readEntity(new GenericType<List<GitHubGroup>>() {
                    });
                    if (fetchedGroups.size() == 0) break;
                    gitHubGroups.addAll(fetchedGroups);
                    page += 1;
                }

                for (GitHubGroup gitHubGroup : gitHubGroups) {
                    StringBuilder repoEndPointBuilder = new StringBuilder();

                    repoEndPointBuilder.append(Constants.GITHUB_API_URL);
                    repoEndPointBuilder.append(Constants.TEAMS);
                    repoEndPointBuilder.append(Constants.URL_SEPARATOR);
                    repoEndPointBuilder.append(gitHubGroup.getId());
                    repoEndPointBuilder.append(Constants.URL_SEPARATOR);
                    repoEndPointBuilder.append(Constants.REPOS);

                    WebTarget webTarget = ClientBuilder.newClient()
                            .target(repoEndPointBuilder.toString())
                            .queryParam(Constants.ACCESS_TOKEN, privateToken);
                    Response response = webTarget.request(MediaType.APPLICATION_JSON).get();
                    List<GitHubProject> fetchedProjects = response.readEntity(new GenericType<List<GitHubProject>>() {
                    });
                    gitHubGroup.setGitHubProjects(fetchedProjects);
                }
            } catch (Throwable ex) {
                log.error("Throwable Exception while fetching groups ", ex);
            }
        }
        return gitHubGroups;
    }

    public String getBitBucketAccessTokenStatic(String userName, String password){
        String access_token = "<replace with a live access_token obtained via curl>";
        return access_token;
    }

    public String getBitBucketAccessToken(String userName, String password) {
        String access_token = null;

        try {
            StringBuilder endpointBuilder = new StringBuilder();
            endpointBuilder.append(Constants.BITBUCKET_API_URL);
            endpointBuilder.append(Constants.BIT_BUCKET_ACCESS_TOKEN_END_POINT);

            WebTarget webTarget = ClientBuilder.newClient()
                    .target(endpointBuilder.toString());

            log.info("Token Endpoint....{}:",endpointBuilder.toString());
            log.info("Authentication Token....{}:",getBasicAuthentication(userName,password));
            MultivaluedMap<String, String> formData = new MultivaluedHashMap<String, String>();
            formData.putSingle(Constants.GRANT_TYPE,Constants.CLIENT_CREDENTIALS);

            Response response = webTarget.request(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION,getBasicAuthentication(userName,password))
                    .post(Entity.form(formData));

            String jsonString = response.readEntity(String.class);            
            log.info("Token Response received....{}:",jsonString);
            JSONObject jsonResponse = new JSONObject(jsonString);

            access_token = jsonResponse.get(Constants.ACCESS_TOKEN).toString();
        } catch (Throwable ex) {
            System.out.println("Throwable Exception while fetching groups "+ ex.toString());
            ex.printStackTrace();
        }
        return access_token;
    }

    private String getBasicAuthentication(final String user, final String password) {
        String token = user + ":" + password;
        try {
            return Constants.BASIC +Constants.STRING_SPACER + DatatypeConverter.printBase64Binary(token.getBytes(Constants.UTF_8));
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException("Cannot encode with UTF-8", ex);
        }
    }
}
