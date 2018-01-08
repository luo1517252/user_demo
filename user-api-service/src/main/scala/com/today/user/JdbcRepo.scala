package com.today.user.repository

import java.text.SimpleDateFormat
import java.util.Date
import javax.sql.DataSource

import com.today.user._
import com.today.api.user.enums.{IntegralTypeEnum, UserStatusEnum}
import com.today.api.user.request._
import com.today.api.user.response._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import wangzx.scala_commons.sql._
@Component
class JdbcRepo{

 //val simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss.S")
  @Autowired
  var dataSource: DataSource = _

  /**
    * 检查 username 是否已经存在
    * @param userName
    * @return
    */
  def checkUserName(userName: String): Boolean = {
    val row : Option[Row] = dataSource.row[Row](
      sql" SELECT * FROM users WHERE USER_NAME = ${userName} ")
    assert(row.isEmpty,"该用户已存在")
    true
  }

  /**
    * 添加用户
    * @param request
    * @return
    */
  def addUser(request: RegisterUserRequest): Int = {
    dataSource.executeUpdate(
      sql"""INSERT INTO users set user_name = ${request.userName},
                                    password = ${request.passWord},
                                    telephone = ${request.telephone},
                                    integral = 0,
                                    user_status = 1,
                                    created_at = now(),
                                    created_by = 1,
                                    updated_by = 1""")
  }

  /**
    * 返回注册信息
    * @param username
    * @return
    */
  def registerUserResponse(username: String): RegisterUserResponse = {

    val registerRes :Option[RegisterResponse] = dataSource.row[RegisterResponse](
      sql"SELECT * FROM users WHERE USER_NAME = $username")

    val res = registerRes.map(x => {
      RegisterUserResponse(userName = x.user_name,
                          telephone = x.telephone,
                          status = UserStatusEnum.findByValue(x.user_status),
                          createdAt = x.created_at.getTime)
    }).get
    res
  }


  def activationUser(userName: String){
    val res = dataSource.executeUpdate(
      sql"""UPDATE users SET user_status = 1,
                              integral = integral + 5
                          WHERE user_name = ${userName}""")
    println(res)
  }
  /**
    * 根据手机号和密码查询用户
    * @param request
    * @return
    */
  def selectUserByTelAndPassword(request: LoginUserRequest): Option[LoginUserResponse] = {
    val loginRes :Option[LoginResponse] = dataSource.row[LoginResponse](
      sql"SELECT * FROM users WHERE telephone = ${request.telephone} AND password =${request.passWord}")
    val res = loginRes.map(x=> {
      LoginUserResponse(userName = x.user_name,
                        telephone = x.telephone,
                        status = UserStatusEnum.findByValue(x.user_status),
                        integral = x.integral,
                        createdAt = x.created_at.getTime,
                        updatedAt = x.updated_at.getTime,
                        email = x.email,
                        qq = x.qq)
    })
    res
  }


  def modifyUserByUserName(request: ModifyUserRequest): Option[ModifyUserResponse] = {

    val resp  = dataSource.executeUpdate(sql"update users set email = ${request.email} , qq = ${request.qq} where id = ${request.userId}")

    val modifyRes: Option[Users] = dataSource.row[Users](sql"""SELECT * FROM users WHERE id = ${request.userId}""")

    val pri  = dataSource.row[String](
      sql"SELECT user_name FROM users WHERE id = ${request.userId}")


    val res = modifyRes.map(x=>{
      ModifyUserResponse(userName = x.user_name,
        telephone =  x.telephone,
        status = UserStatusEnum.findByValue(x.user_status),
        updatedAt = x.updated_at.getTime,
        email = x.email,
        qq = x.qq)
    })
    res

  }

  /**
    * 查看是否被冻结,是否已拉黑
    *
    * @param userId
    * @return
    */
  def checkUserIsFreeze(userId: String): Boolean = {
    val status: Option[Int] = dataSource.row[Int](
      sql"SELECT user_status FROM users WHERE id = ${userId} AND is_delete = 0")
    val statusFlag = UserStatusEnum.findByValue(status.get) match {
      case UserStatusEnum.FREEZED => false
      case UserStatusEnum.BLACK => false
      case _ => true
    }
    statusFlag
  }


  /**
    * 冻结用户 拉黑用户
    * @param request
    * @return
    */
  def freezeUserByUserId(request: FreezeUserRequest, status:Int):FreezeUserResponse = {
    dataSource.executeUpdate(
      sql"""update users set user_status = ${status},
                              remark = ${request.remark}
                          where id = ${request.userId} AND is_delete = 0""")

    val res = dataSource.row[Users](
      sql"""SELECT * FROM users WHERE id = ${request.userId}""")

    val freeResp: Option[FreezeUserResponse] = res.map(x=>{
      FreezeUserResponse(x.id.toString,UserStatusEnum.findByValue(x.user_status.toString.toInt),x.remark)
    })
    freeResp.get
  }

  /**
    * 拉黑用户 清空积分
    * @param request
    * @return
    */
  def blackUserByUserId(request: BlackUserRequest, status:Int):BlackUserResponse = {
    dataSource.executeUpdate(
      sql"""update users set user_status = ${status} ,
                              remark = ${request.remark}
                        where id = ${request.userId} AND is_delete = 0""")

    val res :Option[Users] = dataSource.row[Users](
      sql"""SELECT * FROM users WHERE id = ${request.userId}""")

    val blackResp = res.map(x=>{
      BlackUserResponse(x.id.toString,UserStatusEnum.findByValue(x.user_status),x.remark)
    })
    blackResp.get
  }

  /**
    * 查询用户现有积分
    * @param userId
    * @return
    */
  def selectPresentIntegralById(userId: String):Int = {
    dataSource.row[Int](
      sql"SELECT integral FROM users WHERE id = ${userId} ").get
  }



  /**
    * 改变积分流水
    *
    *  1. update user set integral = ?  where id = ${userId}
    *  2. insert into integral_journal() values()
    * @param request
    * @return
    */
  def changeUserIntegral(request: ChangeIntegralRequest):Int= {

    val integralPrice = request.integralType match {
      case IntegralTypeEnum.ADD => request.integralPrice
      case IntegralTypeEnum.MINUS => (-request.integralPrice.toInt).toString
    }
    dataSource.executeUpdate(
      sql"""update users set integral = integral + ${integralPrice}  where id = ${request.userId}""")

    val presentIntegral = selectPresentIntegralById(request.userId.toString)

    /**
      * 改变积分流水
      */
    var id: Option[Int] = None
    dataSource.executeUpdateWithGenerateKey(
      sql"""INSERT INTO integral_journal set user_id = ${request.userId},
                                              integral_type = ${request.integralType.id},
                                              integral_price = ${request.integralPrice},
                                              integral_source = ${request.integralSource.id},
                                              integral = ${presentIntegral},
                                              created_by = 1,
                                              updated_at = now(),
                                              updated_by = 1,
                                              remark = ${request.integralSource.name}""")(rs=>{
      while (rs.next()) {
        id = Some(rs.getInt(1))
      }})
    id.get
  }
}