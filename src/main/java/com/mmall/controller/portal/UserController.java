package com.mmall.controller.portal;

import com.mmall.common.Const;
import com.mmall.common.ResponseCode;
import com.mmall.common.ServerResponse;
import com.mmall.pojo.User;
import com.mmall.service.IUserService;
import com.mmall.util.CookieUtil;
import com.mmall.util.JsonUtil;
import com.mmall.util.RedisShardedPoolUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @program: mmall
 * @ClassName UserController
 * @description
 * @author: Yangh
 * @create: 2020-02-04 12:00
 * @Version 1.0
 **/
@Controller
@RequestMapping("/user/")
public class UserController {

    @Autowired
    private IUserService iUserService;

    /**
      *@Author Yangh
      *@Description //TODO 用户登录接口
      *@Date 2020/2/12 11:12
      *@param username
      *@param password
      *@param session
      *@return com.mmall.common.ServerResponse<com.mmall.pojo.User>
    **/
    @RequestMapping(value = "login.do",method = RequestMethod.GET)
    @ResponseBody
    public ServerResponse<User> login(String username, String password, HttpSession session, HttpServletResponse httpServletResponse) {
        ServerResponse<User> response = iUserService.login(username, password);
        if (response.isSuccess()){
//            session.setAttribute(Const.CURRENT_USER,response.getData());
            CookieUtil.writeLoginToken(httpServletResponse, session.getId());
//            CookieUtil.readLoginToken(httpServletRequest);
//            CookieUtil.delLoginToken(httpServletRequest, httpServletResponse);
            RedisShardedPoolUtil.setEx(session.getId(), JsonUtil.obj2String(response.getData()),Const.RedisCacheExtime.REDIS_SESSION_EXTIME);
        }
        return response;
    }

    /**
     * @Author Yangjh
     * @Description //TODO 登出接口
     * @Date 17:14 2020-2-4
     * @Param [session]
     * @return com.mmall.common.ServerResponse<java.lang.String>
     **/
    @RequestMapping(value = "logout.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
//        session.removeAttribute(Const.CURRENT_USER);
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        CookieUtil.delLoginToken(httpServletRequest, httpServletResponse);
        RedisShardedPoolUtil.del(loginToken);
        return ServerResponse.createBySuccess();
    }

    /**
     * @Author Yangjh
     * @Description //TODO 用户注册接口
     * @Date 17:18 2020-2-4
     * @Param user
     * @return com.mmall.common.ServerResponse<java.lang.String>
     **/
    @RequestMapping(value = "register.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> register(User user){
        return iUserService.register(user);
    }

    /**
      *@Author Yangh
      *@Description //TODO 用户名、邮箱实时校验接口
      *@Date 2020/2/12 11:12
      *@param str
      *@param type 根据是username还是email来判断str使用哪个sql
      *@return com.mmall.common.ServerResponse<java.lang.String>
    **/
    @RequestMapping(value = "check_valid.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> checkValid(String str, String type) {
        return iUserService.checkValid(str, type);
    }

    /**
      *@Author Yangh
      *@Description //TODO 获取登录用户信息接口
      *@Date 2020/2/12 16:54
      *@param httpServletRequest
      *@return com.mmall.common.ServerResponse<com.mmall.pojo.User>
    **/
    @RequestMapping(value = "get_user_info.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> getUserInfo(HttpServletRequest httpServletRequest) {
//        User user = (User) session.getAttribute(Const.CURRENT_USER);
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);
        if (user != null) {
            return ServerResponse.createBySuccess(user);
        }
        return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
    }

    /**
      *@Author Yangh
      *@Description //TODO 忘记密码找回问题接口
      *@Date 2020/2/12 16:57
      *@param username
      *@return com.mmall.common.ServerResponse<java.lang.String>
    **/
    @RequestMapping(value = "forget_get_question.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetGetQuestion(String username) {
        return iUserService.selectQuestion(username);
    }

    /**
      *@Author Yangh
      *@Description //TODO 使用本地缓存提交并检查问题答案接口
      *@Date 2020/2/12 20:46
      *@param username
      *@param question
      *@param answer
      *@return com.mmall.common.ServerResponse<java.lang.String>
    **/
    @RequestMapping(value = "forget_check_answer.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetCheckAnswer(String username, String question, String answer) {
        return iUserService.checkAnswer(username,question,answer);
    }

    /**
      *@Author Yangh
      *@Description //TODO 忘记密码的重设密码接口
      *@Date 2020/2/12 20:51
      *@param username
      *@param passwordNew
      *@param forgetToken
      *@return com.mmall.common.ServerResponse<java.lang.String>
    **/
    @RequestMapping(value = "forget_reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> forgetResetPassword(String username, String passwordNew, String forgetToken) {
        return iUserService.forgetResetPassword(username, passwordNew, forgetToken);
    }

    /**
      *@Author Yangh
      *@Description //TODO 登录中状态重置密码
      *@Date 2020/2/12 22:20
      *@param httpServletRequest
      *@param passwordOld
      *@param passwordNew
      *@return com.mmall.common.ServerResponse<java.lang.String>
    **/
    @RequestMapping(value = "reset_password.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<String> resetPassword(HttpServletRequest httpServletRequest, String passwordOld, String passwordNew) {
//        User user = (User) session.getAttribute(Const.CURRENT_USER);
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User user = JsonUtil.string2Obj(userJsonStr, User.class);
        if (user == null) {
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        return iUserService.resetPassword(passwordOld,passwordNew,user);
    }

    /**
      *@Author Yangh
      *@Description //TODO 登录状态更新个人信息
      *@Date 2020/2/12 23:15
      *@param httpServletRequest
      *@param user
      *@return com.mmall.common.ServerResponse<com.mmall.pojo.User>
    **/
    @RequestMapping(value = "update_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> update_information(HttpServletRequest httpServletRequest, User user) {
//        User currentUser = (User) session.getAttribute(Const.CURRENT_USER);
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User currentUser = JsonUtil.string2Obj(userJsonStr, User.class);
        if (currentUser == null) {
            return ServerResponse.createByErrorMessage("用户未登录");
        }
        user.setId(currentUser.getId());
        user.setUsername(currentUser.getUsername());
        ServerResponse<User> response = iUserService.updateInformation(user);
        if(response.isSuccess()){
            response.getData().setUsername(currentUser.getUsername());
//            session.setAttribute(Const.CURRENT_USER,response.getData());
            RedisShardedPoolUtil.setEx(loginToken, JsonUtil.obj2String(response.getData()),Const.RedisCacheExtime.REDIS_SESSION_EXTIME);

        }
        return response;
    }

    /**
      *@Author Yangh
      *@Description //TODO 获取当前登录用户的详细信息，并强制登录
      *@Date 2020/2/12 23:27
      *@param httpServletRequest
      *@return com.mmall.common.ServerResponse<com.mmall.pojo.User>
    **/
    @RequestMapping(value = "get_information.do",method = RequestMethod.POST)
    @ResponseBody
    public ServerResponse<User> get_information(HttpServletRequest httpServletRequest){
//        User currentUser = (User)session.getAttribute(Const.CURRENT_USER);
        String loginToken = CookieUtil.readLoginToken(httpServletRequest);
        if (StringUtils.isEmpty(loginToken)) {
            return ServerResponse.createByErrorMessage("用户未登录，无法获取当前用户的信息");
        }
        String userJsonStr = RedisShardedPoolUtil.get(loginToken);
        User currentUser = JsonUtil.string2Obj(userJsonStr, User.class);
        if(currentUser == null){
            return ServerResponse.createByErrorCodeMessage(ResponseCode.NEED_LOGIN.getCode(),"未登录,需要强制登录status=10");
        }
        return iUserService.getInformation(currentUser.getId());
    }


}
