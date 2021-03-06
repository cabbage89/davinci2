/*
 * <<
 *  Davinci
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package edp.davinci.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.JsonPath;
import edp.core.consts.Consts;
import edp.core.enums.HttpCodeEnum;
import edp.core.enums.MailContentTypeEnum;
import edp.core.exception.NotFoundException;
import edp.core.exception.ServerException;
import edp.core.model.MailContent;
import edp.core.utils.*;
import edp.davinci.core.common.Constants;
import edp.davinci.core.common.ErrorMsg;
import edp.davinci.core.common.ResultMap;
import edp.davinci.core.enums.CheckEntityEnum;
import edp.davinci.core.enums.LockType;
import edp.davinci.core.enums.UserDistinctType;
import edp.davinci.core.enums.UserOrgRoleEnum;
import edp.davinci.dao.OrganizationMapper;
import edp.davinci.dao.RelUserOrganizationMapper;
import edp.davinci.dao.UserMapper;
import edp.davinci.dto.organizationDto.OrganizationInfo;
import edp.davinci.dto.userDto.*;
import edp.davinci.model.LdapPerson;
import edp.davinci.model.Organization;
import edp.davinci.model.RelUserOrganization;
import edp.davinci.model.User;
import edp.davinci.service.LdapService;
import edp.davinci.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.regex.Matcher;


@Slf4j
@Service("userService")
public class UserServiceImpl extends BaseEntityService implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private RelUserOrganizationMapper relUserOrganizationMapper;

    @Autowired
    private TokenUtils tokenUtils;

    @Autowired
    private MailUtils mailUtils;


    @Autowired
    private FileUtils fileUtils;

    @Autowired
    private ServerUtils serverUtils;

    @Autowired
    private LdapService ldapService;

    @Autowired
    private Environment environment;

    private static final CheckEntityEnum entity = CheckEntityEnum.USER;


    private static final Long TOKEN_TIMEOUT_MILLIS = 10 * 60 * 1000L;

    /**
     * ??????????????????
     *
     * @param name
     * @param scopeId
     * @return
     */
    @Override

    public boolean isExist(String name, Long id, Long scopeId) {
        Long userId = userMapper.getIdByName(name);
        if (null != id && null != userId) {
            return !id.equals(userId);
        }
        return null != userId && userId.longValue() > 0L;
    }

    /**
     * ??????????????????
     *
     * @param userRegist
     * @return
     */
    @Override
    @Transactional
    public User regist(UserRegist userRegist) throws ServerException {

        String username = userRegist.getUsername();
        //???????????????????????????
        if (isExist(username, null, null)) {
            log.info("The username({}) has been registered", username);
            throw new ServerException("The username:" + username + " has been registered");
        }

        String email = userRegist.getEmail();
        //????????????????????????
        if (isExist(email, null, null)) {
            log.info("The email({}) has been registered", email);
            throw new ServerException("The email:" + email + " has been registered");
        }

        BaseLock usernameLock = getLock(entity, username, null);
        if (usernameLock != null && !usernameLock.getLock()) {
            alertNameTaken(entity, username);
        }

        BaseLock emailLock = null;
        if (!username.toLowerCase().equals(email.toLowerCase())) {
            emailLock = getLock(entity, email, null);
        }

        if (emailLock != null && !emailLock.getLock()) {
            alertNameTaken(entity, email);
        }

        try {
            User user = new User();
            //????????????
            userRegist.setPassword(BCrypt.hashpw(userRegist.getPassword(), BCrypt.gensalt()));
            BeanUtils.copyProperties(userRegist, user);
            //????????????
            if (userMapper.insert(user) <= 0) {
                log.info("Regist fail, userRegist:{}", userRegist.toString());
                throw new ServerException("Regist fail, unspecified error");
            }
            //?????????????????????????????????
            sendMail(user.getEmail(), user);
            return user;
        } finally {
            releaseLock(usernameLock);
            releaseLock(emailLock);
        }
    }

    @Override
    public User externalRegist(OAuth2AuthenticationToken oauthAuthToken) throws ServerException {
        OAuth2User oauthUser = oauthAuthToken.getPrincipal();

        User user = getByUsername(oauthUser.getName());
        if (user != null) {
            return user;
        }
        user = new User();

        String emailMapping = environment.getProperty(String.format("spring.security.oauth2.client.provider.%s.userMapping.email", oauthAuthToken.getAuthorizedClientRegistrationId()));
        String nameMapping = environment.getProperty(String.format("spring.security.oauth2.client.provider.%s.userMapping.name", oauthAuthToken.getAuthorizedClientRegistrationId()));
        String avatarMapping = environment.getProperty(String.format("spring.security.oauth2.client.provider.%s.userMapping.avatar", oauthAuthToken.getAuthorizedClientRegistrationId()));
        JSONObject jsonObj = new JSONObject(oauthUser.getAttributes());

        user.setName(JsonPath.read(jsonObj, nameMapping));
        user.setUsername(oauthUser.getName());
        user.setPassword("OAuth2");
        user.setEmail(JsonPath.read(jsonObj, emailMapping));
        user.setAvatar(JsonPath.read(jsonObj, avatarMapping));
        int insert = userMapper.insert(user);
        if (insert > 0) {
            return user;
        } else {
            log.info("Regist fail, username:{}", oauthUser.getName());
            throw new ServerException("Regist fail, unspecified error");
        }
    }

    protected void alertNameTaken(CheckEntityEnum entity, String name) throws ServerException {
        log.warn("The {} username or email {} has been registered", entity.getSource(), name);
        throw new ServerException("The " + entity.getSource() + " username or email has been registered");
    }

    /**
     * ???????????????????????????
     *
     * @param username
     * @return
     */
    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    /**
     * ????????????
     *
     * @param userLogin
     * @return
     */
    @Override
    public User userLogin(UserLogin userLogin) throws ServerException {

        String username = userLogin.getUsername();
        String password = userLogin.getPassword();

        User user = getByUsername(username);
        if (user != null) {
            // ????????????
            boolean checkpw = false;
            try {
                checkpw = BCrypt.checkpw(password, user.getPassword());
            } catch (Exception e) {

            }

            if (checkpw) {
                return user;
            }

            if (ldapLogin(username, password)) {
                return user;
            }

            log.info("Username({}) password is wrong", username);
            throw new ServerException("Username or password is wrong");
        }

        user = ldapAutoRegist(username, password);
        if (user == null) {
            throw new ServerException("Username or password is wrong");
        }
        return user;
    }

    private boolean ldapLogin(String username, String password) {
        if (!ldapService.existLdapServer()) {
            return false;
        }

        LdapPerson ldapPerson = ldapService.findByUsername(username, password);
        if (null == ldapPerson) {
            return false;
        }

        return true;
    }

    private User ldapAutoRegist(String username, String password) {

        if (!ldapService.existLdapServer()) {
            return null;
        }

        LdapPerson ldapPerson = ldapService.findByUsername(username, password);
        if (null == ldapPerson) {
            throw new ServerException("Username or password is wrong");
        }

        String email = ldapPerson.getEmail();
        if (userMapper.existEmail(ldapPerson.getEmail())) {
            log.info("Ldap auto regist fail, the email {} has been registered", email);
            throw new ServerException("Ldap auto regist fail: the email " + email + " has been registered");
        }

        if (userMapper.existUsername(ldapPerson.getSAMAccountName())) {
            ldapPerson.setSAMAccountName(email);
        }

        return ldapService.registPerson(ldapPerson);
    }

    /**
     * ????????????
     *
     * @param keyword
     * @param user
     * @param orgId
     * @param includeSelf
     * @return
     */
    @Override
    public List<UserBaseInfo> getUsersByKeyword(String keyword, User user, Long orgId, Boolean includeSelf) {
        List<UserBaseInfo> users = userMapper.getUsersByKeyword(keyword, orgId);
        if (includeSelf) {
            return users;
        }

        Iterator<UserBaseInfo> iterator = users.iterator();
        while (iterator.hasNext()) {
            UserBaseInfo userBaseInfo = iterator.next();
            if (userBaseInfo.getId().equals(user.getId())) {
                iterator.remove();
            }
        }

        return users;
    }

    /**
     * ????????????
     *
     * @param user
     * @return
     */
    @Override
    @Transactional
    public boolean updateUser(User user) throws ServerException {
        if (userMapper.updateBaseInfo(user) <= 0) {
            log.info("Update user fail, username:{}", user.getUsername());
            throw new ServerException("Update user fail");
        }
        return true;
    }

    @Override
    @Transactional
    public ResultMap activateUserNoLogin(String token, HttpServletRequest request) {
        ResultMap resultMap = new ResultMap(tokenUtils);

        token = AESUtils.decrypt(token, null);
        String username = tokenUtils.getUsername(token);
        if (null == username) {
            return resultMap.fail().message("The activate toke is invalid");
        }

        User user = getByUsername(username);
        if (null == user) {
            return resultMap.fail().message("The activate toke is invalid");
        }

        // ????????????????????????????????????
        if (user.getActive()) {
            return resultMap.fail().message("The current user is activated and doesn't need to be reactivated");
        }

        BaseLock lock = LockFactory.getLock("ACTIVATE" + Consts.AT_SYMBOL + username.toUpperCase(), 5, LockType.REDIS);
        if (lock != null && !lock.getLock()) {
            return resultMap.fail().message("The current user is activating");
        }

        try {
            // ????????????token
            if (tokenUtils.validateToken(token, user)) {
                user.setActive(true);
                user.setUpdateTime(new Date());
                userMapper.activeUser(user);

                String orgName = user.getUsername() + "'s Organization";
                // ???????????????????????????Organization
                Organization organization = new Organization(orgName, null, user.getId());
                organizationMapper.insert(organization);

                // ?????????????????????????????????????????????owner
                RelUserOrganization relUserOrganization = new RelUserOrganization(organization.getId(), user.getId(),
                        UserOrgRoleEnum.OWNER.getRole());
                relUserOrganization.createdBy(user.getId());
                relUserOrganizationMapper.insert(relUserOrganization);

                UserLoginResult userLoginResult = new UserLoginResult();
                BeanUtils.copyProperties(user, userLoginResult);
                return resultMap.success(tokenUtils.generateToken(user)).payload(userLoginResult);
            }

            return resultMap.fail().message("The activate toke is invalid");

        } finally {
            releaseLock(lock);
        }
    }

    /**
     * ????????????
     *
     * @param email
     * @param user
     * @return
     */
    @Override
    public boolean sendMail(String email, User user) throws ServerException {
        //????????????
        if (!email.equals(user.getEmail())) {
            throw new ServerException("The current email address is not match user email address");
        }

        Map<String, Object> content = new HashMap<String, Object>();
        content.put("username", user.getUsername());
        content.put("host", serverUtils.getHost());
        content.put("token", AESUtils.encrypt(tokenUtils.generateContinuousToken(user), null));

        MailContent mailContent = MailContent.MailContentBuilder.builder()
                .withSubject(Constants.USER_ACTIVATE_EMAIL_SUBJECT)
                .withTo(user.getEmail())
                .withMainContent(MailContentTypeEnum.TEMPLATE)
                .withTemplate(Constants.USER_ACTIVATE_EMAIL_TEMPLATE)
                .withTemplateContent(content)
                .build();

        mailUtils.sendMail(mailContent, null);
        return true;
    }

    /**
     * ??????????????????
     *
     * @param user
     * @param oldPassword
     * @param password
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ResultMap changeUserPassword(User user, String oldPassword, String password, HttpServletRequest request) {
        ResultMap resultMap = new ResultMap(tokenUtils);

        //???????????????
        if (!BCrypt.checkpw(oldPassword, user.getPassword())) {
            return resultMap.failAndRefreshToken(request).message("Incorrect original password");
        }
        //???????????????
        user.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        user.setUpdateTime(new Date());
        if (userMapper.changePassword(user) > 0) {
            return resultMap.success().message("Successful password modification");
        }

        return resultMap.failAndRefreshToken(request);
    }

    /**
     * ????????????
     *
     * @param user
     * @param file
     * @param request
     * @return
     */
    @Override
    @Transactional
    public ResultMap uploadAvatar(User user, MultipartFile file, HttpServletRequest request) {
        ResultMap resultMap = new ResultMap(tokenUtils);

        //????????????????????????
        if (!fileUtils.isImage(file)) {
            return resultMap.failAndRefreshToken(request).message("File format error");
        }

        //????????????
        String fileName = user.getUsername() + "_" + UUID.randomUUID();
        String avatar = null;
        try {
            avatar = fileUtils.upload(file, Constants.USER_AVATAR_PATH, fileName);
            if (StringUtils.isEmpty(avatar)) {
                return resultMap.failAndRefreshToken(request).message("User avatar upload error");
            }
        } catch (Exception e) {
            log.error("User avatar upload error, username:{}", user.getUsername(), e);
            return resultMap.failAndRefreshToken(request).message("User avatar upload error");
        }

        //???????????????
        if (!StringUtils.isEmpty(user.getAvatar())) {
            fileUtils.remove(user.getAvatar());
        }

        //??????????????????
        user.setAvatar(avatar);
        user.setUpdateTime(new Date());
        if (userMapper.updateAvatar(user) > 0) {
            Map<String, String> map = new HashMap<>();
            map.put("avatar", avatar);
            return resultMap.successAndRefreshToken(request).payload(map);
        }

        return resultMap.failAndRefreshToken(request).message("Server error, user avatar update fail");
    }


    /**
     * ??????????????????
     *
     * @param id
     * @param user
     * @param request
     * @return
     */
    @Override
    public ResultMap getUserProfile(Long id, User user, HttpServletRequest request) {
        ResultMap resultMap = new ResultMap(tokenUtils);

        User tempUser = userMapper.getById(id);
        if (null == tempUser) {
            return resultMap.failAndRefreshToken(request).message("User not found");
        }

        UserProfile userProfile = new UserProfile();
        BeanUtils.copyProperties(tempUser, userProfile);
        if (id.equals(user.getId())) {
            List<OrganizationInfo> organizationInfos = organizationMapper.getOrganizationByUser(user.getId());
            userProfile.setOrganizations(organizationInfos);
            return resultMap.successAndRefreshToken(request).payload(userProfile);
        }

        Long[] userIds = {user.getId(), id};
        List<OrganizationInfo> jointlyOrganization = organizationMapper.getJointlyOrganization(Arrays.asList(userIds), id);
        if (!CollectionUtils.isEmpty(jointlyOrganization)) {
            BeanUtils.copyProperties(tempUser, userProfile);
            userProfile.setOrganizations(jointlyOrganization);
            return resultMap.successAndRefreshToken(request).payload(userProfile);
        }

        return resultMap.failAndRefreshToken(request, HttpCodeEnum.UNAUTHORIZED).message("You have not permission to view the user's information because you don't have any organizations that join together");
    }

    @Override
    public ResultMap getUserProfileFromToken(String token) {
        String username = tokenUtils.getUsername(Constants.TOKEN_PREFIX + Constants.SPACE + token);
        User user = getByUsername(username);
        if (null == user) {
            return new ResultMap().fail(HttpCodeEnum.FORBIDDEN.getCode()).message(ErrorMsg.ERR_MSG_PERMISSION);
        }
        if (!tokenUtils.validateToken(token, user)) {
            return new ResultMap().fail(HttpCodeEnum.FORBIDDEN.getCode()).message(ErrorMsg.ERR_MSG_PERMISSION);
        }
        UserProfile userProfile = new UserProfile();
        BeanUtils.copyProperties(user, userProfile);
        List<OrganizationInfo> organizationInfos = organizationMapper.getOrganizationByUser(user.getId());
        userProfile.setOrganizations(organizationInfos);
        return new ResultMap().success(tokenUtils.generateToken(user)).payload(userProfile);
    }

    @Override
    public String forgetPassword(UserDistinctType userDistinctType, UserDistinctTicket ticket) {
        User user = null;
        switch (userDistinctType) {
            case EMAIL:
                String email = ticket.getTicket();
                if (StringUtils.isEmpty(email)) {
                    throw new ServerException("Email cannot be empty!");
                }
                Matcher matcher = Constants.PATTERN_EMAIL_FORMAT.matcher(email);
                if (!matcher.find()) {
                    throw new ServerException("Invalid email format!");
                }
                user = userMapper.selectByUsername(email);
                if (user == null) {
                    throw new ServerException("The current email is not registered in Davinci");
                }
                break;
            case USERNAME:
                String username = ticket.getTicket();
                if (StringUtils.isEmpty(username)) {
                    throw new ServerException("Username cannot be EMPTY!");
                }
                user = userMapper.selectByUsername(username);
                if (user == null) {
                    throw new ServerException("The current username is not registered in Davinci");
                }
                break;
            default:
                throw new NotFoundException("Unknown request uri");
        }

        String checkCode = TokenUtils.randomPassword();
        user.setPassword(checkCode);
        String checkToken = tokenUtils.generateToken(user, TOKEN_TIMEOUT_MILLIS);

        Map<String, Object> content = new HashMap<>(3);
        content.put("ticket", ticket.getTicket());
        content.put("checkCode", checkCode);


        MailContent mailContent = MailContent.MailContentBuilder.builder()
                .withSubject(Constants.USER_REST_PASSWORD_EMAIL_SUBJECT)
                .withTo(user.getEmail())
                .withMainContent(MailContentTypeEnum.TEMPLATE)
                .withTemplate(Constants.USER_REST_PASSWORD_EMAIL_TEMPLATE)
                .withTemplateContent(content)
                .build();

        mailUtils.sendMail(mailContent, null);
        return StringZipUtil.compress(checkToken);
    }

    @Override
    @Transactional
    public boolean resetPassword(UserDistinctType userDistinctType, String token, UserDistinctTicket ticket) {
        User user = null;
        switch (userDistinctType) {
            case EMAIL:
                String email = ticket.getTicket();
                if (StringUtils.isEmpty(email)) {
                    throw new ServerException("Email cannot be EMPTY!");
                }
                Matcher matcher = Constants.PATTERN_EMAIL_FORMAT.matcher(email);
                if (!matcher.find()) {
                    throw new ServerException("Invalid email format!");
                }
                user = userMapper.selectByUsername(email);
                if (user == null) {
                    throw new ServerException("The current email is not registered in Davinci");
                }
                break;
            case USERNAME:
                String username = ticket.getTicket();
                if (StringUtils.isEmpty(username)) {
                    throw new ServerException("Username cannot be EMPTY!");
                }
                user = userMapper.selectByUsername(username);
                if (user == null) {
                    throw new ServerException("The current username is not registered in Davinci");
                }
                break;
            default:
                throw new NotFoundException("Unknown request uri");
        }

        if (StringUtils.isEmpty(ticket.getCheckCode())) {
            throw new ServerException("Check code cannot be Empty");
        }
        if (StringUtils.isEmpty(ticket.getPassword())) {
            throw new ServerException("Password cannot be Empty");
        }

        String decompress = StringZipUtil.decompress(token);
        user.setPassword(ticket.getCheckCode());
        if (!tokenUtils.validateToken(decompress, user)) {
            throw new ServerException("Invalid check code, check code is wrong or has expired");
        }
        user.setPassword(BCrypt.hashpw(ticket.getPassword(), BCrypt.gensalt()));
        return userMapper.changePassword(user) > 0;
    }
}
