package com.today.user


import java.util.Date

import com.today.api.user.enums.{IntegralSourceEnum, IntegralTypeEnum, UserStatusEnum}
import com.today.api.user.request._
import com.today.api.user.response._
import com.today.api.user.service.UserService
import com.today.user.repository.JdbcRepo
import org.springframework.beans.factory.annotation.Autowired

object UserServiceImpl {
  val telephoneRegex = "^1(3[0-9]|4[57]|5[0-35-9]|7[01678]|8[0-9])\\d{8}$".r
  val passwordRegex = """(?=.*\d)(?=.*[A-z])^[0-9A-z]{8,}$""".r

  val isEmail = """^(\w)+(\.\w+)*@(\w)+((\.\w{2,3}){1,3})$""".r
  val isQQ = """\d{4,12}""".r
}

class UserServiceImpl extends UserService {

  import UserServiceImpl._

  @Autowired
  var jdbcRepo: JdbcRepo = _

  /**
    *
    * *
    * ### 用户注册
    * *
    * #### 业务描述
    * 用户注册账户，用户密码需要加盐之后存储(加盐方案还么确定,小伙伴可以自己随意设计个简单的加解密方案)
    * *
    * #### 接口依赖
    * 无
    * #### 边界异常说明
    * 无
    * *
    * #### 输入
    *1.user_request.RegisterUserRequest
    * *
    * #### 前置检查
    *1. 手机号码规则验证
    *2. 手机号未被使用验证
    *3. 密码规则,字母数字八位混合
    * *
    * ####  逻辑处理
    *1.密码加盐处理
    *2.新增一条user记录
    *3.返回结果 user_response.RegisterUserResponse
    * *
    * #### 数据库变更
    *1. insert into user() values()
    * *
    * ####  事务处理
    * 无
    * *
    * ####  输出
    *1.user_response.RegisterUserResponse
    *
    **/


  override def registerUser(request: RegisterUserRequest): RegisterUserResponse = {

    assert(registerPreCheck(request))

    jdbcRepo.addUser(request)
    jdbcRepo.registerUserResponse(request.userName)
    //RegisterUserResponse("xy","13871353138",UserStatusEnum.ACTIVATED,new Date().getTime)
  }

  /**
    * registerPreCheck 注册前置检查
    * see  @http://blog.csdn.net/pp_fzp/article/details/50764600
    * @param request
    * @return
    */
  def registerPreCheck(request: RegisterUserRequest): Boolean ={
    assert(telephoneRegex.findAllIn(request.telephone ).nonEmpty , "手机号码输入不正确")
    assert(passwordRegex.findFirstIn(request.passWord ).nonEmpty , "密码输入格式不正确")
    assert(jdbcRepo.checkUserName(request.userName ) , "用户名已经存在")
    true
  }


  /**
    *
    * *
    * ### 用户登录
    * *
    * #### 业务描述
    * 用户登录
    * *
    * #### 接口依赖
    * 无
    * #### 边界异常说明
    * 无
    * *
    * #### 输入
    *1.user_request.LoginUserRequest
    * *
    * #### 前置检查
    *1.手机号码规则验证
    *2.密码规则,字母数字八位混合
    * *
    * ####  逻辑处理
    *1. 根据手机号码和密码查询用户记录
    *2. 异常用户状态的用户登录返回 Exception
    * *
    * #### 数据库变更
    *1. select *  from user where telphone = ? and password = ?
    * *
    * ####  事务处理
    * 无
    * *
    * ####  输出
    *1.user_response.LoginUserResponse
    *
    **/
  override def login(request: LoginUserRequest): LoginUserResponse = {

    assert(loginPreCheck(request))

    jdbcRepo.selectUserByTelAndPassword(request).get
  }

  def loginPreCheck(request: LoginUserRequest):Boolean = {
    assert(telephoneRegex.findFirstIn(request.telephone).nonEmpty,"手机号码输入不正确")
    assert(passwordRegex.findFirstIn(request.passWord).nonEmpty,"密码输入格式不正确")
    true
  }
  /**
    *
    **
    * ### 用户修改个人资料
    **
    * #### 业务描述
    * 用户再注册之后完善个人资料,完善资料增加积分5
    **
    * #### 接口依赖
    * 无
    * #### 边界异常说明
    * 无
    **
    * #### 输入
    *1.user_request.ModifyUserRequest
    **
    * #### 前置检查
    *1. 邮箱规则验证
    *2. qq 规则验证
    *3. 用户状态判断只有用户状态为
    **
    * ####  逻辑处理
    *1. 根据输入的参数计算用户积分
    *2. 修改用户 email qq
    *2. 修改完成之后调用积分action增加用户积分(完善资料增加积分5) ChangeUserIntegralAction
    **
    * #### 数据库变更
    *1. update user set email = ? , qq = ? where id = ${userId}
    **
    * ####  事务处理
    *1. 无
    **
    * ####  输出
    *1.user_response.ModifyUserAction
    *
    **/
  override def modifyUser(request: ModifyUserRequest): ModifyUserResponse = {

    assert(modifyPreCheck(request))

    val res:Option[ModifyUserResponse] = jdbcRepo.modifyUserByUserName(request)
    val presentIntegral:Int = jdbcRepo.selectPresentIntegralById(request.userId)
    changeUserIntegral(ChangeIntegralRequest(request.userId,presentIntegral.toString,IntegralTypeEnum.ADD,IntegralSourceEnum.PREFECT_INFORMATION))

    res.get
  }


  def modifyPreCheck(request: ModifyUserRequest) = {
    assert(isEmail.findFirstIn(request.email).nonEmpty,"邮箱格式不正确")
    assert(isQQ.findFirstIn(request.qq).nonEmpty,"QQ格式不正确")
    assert(jdbcRepo.checkUserIsFreeze(request.userId),"用户已被冻结或拉黑")
    true
  }

  /**
    *
    **
    * ### 冻结用户接口
    **
    * #### 业务描述
    * 用户因为触犯一些游戏规则,后台自检程序或者管理员会冻结该用户
    **
    * #### 接口依赖
    * 无
    * #### 边界异常说明
    * 无
    **
    * #### 输入
    *1.user_request.FreezeUserRequest
    **
    * #### 前置检查
    *1.用户状态检查(已冻结,已拉黑,已逻辑删除的用户不能冻结)
    **
    * ####  逻辑处理
    *1. 设置用户状态为 FREEZE
    **
    * #### 数据库变更
    *1. update user set status = ? , remark = ? where id = ${userId}
    **
    * ####  事务处理
    *1. 无
    **
    * ####  输出
    *1.user_response.FreezeUserResponse
    *
    **/
  override def freezeUser(request: FreezeUserRequest): FreezeUserResponse = {

   assert(jdbcRepo.checkUserIsFreeze(request.userId),"冻结的用户状态不正确!")

    jdbcRepo.freezeUserByUserId(request,UserStatusEnum.FREEZED.id)
  }

  /**
    *
    **
    * ### 拉黑用户接口
    **
    * #### 业务描述
    * 用户因为触犯一些游戏规则,后台自检程序或者管理员会拉黑该用户,拉黑用户把用户的积分置为0
    **
    *
    * #### 接口依赖
    * 无
    * #### 边界异常说明
    * 无
    **
    * #### 输入
    *1.user_request.BlackUserRequest
    **
    * #### 前置检查
    *1.用户状态检查(已冻结,已拉黑,已逻辑删除的用户不能拉黑)
    **
    * ####  逻辑处理
    *1. 设置用户状态为  BLACK
    *2. 调用积分修改接口 ChangeUserIntegralAction
    **
    * #### 数据库变更
    *1. update user set status = ? , remark = ? where id = ${userId}
    **
    * ####  事务处理
    *1. 无
    **
    * ####  输出
    *1.user_response.BlackUserResponse
    *
    **/
  override def blackUser(request: BlackUserRequest): BlackUserResponse = {

    assert(jdbcRepo.checkUserIsFreeze(request.userId), "拉黑的用户状态不正确!!!")

    jdbcRepo.blackUserByUserId(request,UserStatusEnum.BLACK.id)
  }

  /**
    *
    **
    * ### 记录积分改变流水
    **
    * #### 业务描述
    * 用户因为完成一些游戏规则或者触犯游戏规则导致积分减少或者增加,调用该接口修改用户积分
    **
    * #### 接口依赖
    * 无
    * #### 边界异常说明
    * 无
    **
    * #### 输入
    *1.user_request.ChangeIntegralRequest
    **
    * #### 前置检查
    *1.用户状态检查(已冻结,已拉黑,已逻辑删除的用户不能冻结)
    **
    * ####  逻辑处理
    *1. 设置用户状态为 FREEZE
    **
    * #### 数据库变更
    *1. update user set integral = ?  where id = ${userId}
    *2. insert into integral_journal() values()
    **
  *####  事务处理
    *1. 无
    **
  *####  输出
    *1. i32 流水 Id
    *
    **/
  override def changeUserIntegral(request: ChangeIntegralRequest): Int = {

    assert(jdbcRepo.checkUserIsFreeze(request.userId), "用户状态不正确!!!")

    jdbcRepo.changeUserIntegral(request)
  }
}
