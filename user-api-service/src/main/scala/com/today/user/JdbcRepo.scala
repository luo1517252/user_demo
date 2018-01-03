package com.today.user.repository

import java.util.Date
import javax.sql.DataSource
import org.springframework.beans.factory.annotation.{Autowired, Qualifier}
import org.springframework.stereotype.Component
import wangzx.scala_commons.sql._
@Component
class JdbcRepo{

  @Autowired
  @Qualifier("dataSource")
  var dataSource: DataSource = _

  /**增加一条记录*/
  def registerUser(username:String,password:String,telephone:String){

    dataSource.executeUpdate(sql"""INSERT INTO users(user_name,password,telephone,integral,created_at,created_by,updated_at,updated_by,remark) VALUES (${username},${password},${telephone},0,${new Date},"","",1,1)""")
  }
  /**查找手机号有没有存在*/
  def findTelephone(telephone:String) ={
    val s = dataSource.row[Int](sql"""SELECT COUNT(*) FROM users WHERE telephone=${telephone}""")
    s.get
  }


}